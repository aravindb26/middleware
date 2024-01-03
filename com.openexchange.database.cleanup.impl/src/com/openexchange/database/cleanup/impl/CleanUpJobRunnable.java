/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.database.cleanup.impl;

import static com.eaio.util.text.HumanTime.exactly;
import static com.openexchange.database.cleanup.impl.DatabaseCleanUpServiceImpl.REFRESH_LAST_TOUCHED_STAMP_INTERVAL_MILLIS;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.context.PoolAndSchema;
import com.openexchange.database.cleanup.CleanUpJob;
import com.openexchange.database.cleanup.CleanUpJobType;
import com.openexchange.database.cleanup.DatabaseCleanUpExceptionCode;
import com.openexchange.database.cleanup.DatabaseCleanUpProperty;
import com.openexchange.database.cleanup.impl.storage.DatabaseCleanUpExecutionManagement;
import com.openexchange.exception.Category;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.update.UpdateStatus;
import com.openexchange.groupware.update.Updater;
import com.openexchange.server.ServiceLookup;
import com.openexchange.threadpool.BoundedCompletionService;
import com.openexchange.threadpool.ThreadPoolCompletionService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link CleanUpJobRunnable}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class CleanUpJobRunnable implements Runnable {

    /** The logger constant */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CleanUpJobRunnable.class);

    /** The random used for shuffle */
    private static final Random SHUFFLE_RANDOM = new Random();

    /** The decimal format to use when printing milliseconds */
    private static final NumberFormat MILLIS_FORMAT = newNumberFormat();

    /** The accompanying lock for shared decimal format */
    private static final Lock MILLIS_FORMAT_LOCK = new ReentrantLock();

    /**
     * Creates a new {@code DecimalFormat} instance.
     *
     * @return The format instance
     */
    private static NumberFormat newNumberFormat() {
        NumberFormat f = NumberFormat.getInstance(Locale.US);
        if (f instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) f;
            df.applyPattern("#,##0");
        }
        return f;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Integer optRepresentativeContextId;
    private final CleanUpJob job;
    private final boolean shuffle;
    private final DatabaseCleanUpExecutionManagement executionManagement;
    private final ServiceLookup services;
    private final AtomicReference<Thread> currentThreadReference;
    private final AtomicBoolean terminated;

    /**
     * Initializes a new {@link CleanUpJobRunnable}.
     *
     * @param job The wrapped job
     * @param executionManagement The storage service to use
     * @param services The service look-up
     */
    public CleanUpJobRunnable(CleanUpJob job,DatabaseCleanUpExecutionManagement executionManagement, ServiceLookup services) {
        this(job, false, executionManagement, services);
    }

    /**
     * Initializes a new {@link CleanUpJobRunnable}.
     *
     * @param job The wrapped job
     * @param shuffle <code>true</code> to shuffle the order, in which all schemas are iterated; otherwise <code>false</code>
     * @param executionManagement The storage service to use
     * @param services The service look-up
     */
    public CleanUpJobRunnable(CleanUpJob job, boolean shuffle, DatabaseCleanUpExecutionManagement executionManagement, ServiceLookup services) {
        this(null, job, shuffle, executionManagement, services);
    }

    /**
     * Initializes a new {@link CleanUpJobRunnable}.
     *
     * @param optRepresentativeContextId An optional representative context identifier to have the job only being executed for that schema; if <code>null</code> all schemas are considered
     * @param job The wrapped job
     * @param shuffle <code>true</code> to shuffle the order, in which all schemas are iterated; otherwise <code>false</code>
     * @param executionManagement The storage service to use
     * @param services The service look-up
     */
    public CleanUpJobRunnable(Integer optRepresentativeContextId, CleanUpJob job, boolean shuffle, DatabaseCleanUpExecutionManagement executionManagement, ServiceLookup services) {
        super();
        this.optRepresentativeContextId = optRepresentativeContextId;
        this.job = job;
        this.shuffle = shuffle;
        this.executionManagement = executionManagement;
        this.services = services;
        this.currentThreadReference = new AtomicReference<Thread>(null);
        this.terminated = new AtomicBoolean(false);
    }

    /**
     * Interrupts this runnable (if currently in execution).
     *
     * @param terminate Whether to terminate this runnable; e.g. prevent future executions of it
     * @return <code>true</code> if interrupted; otherwise <code>false</code> if not running at the time of calling
     */
    public boolean interrupt(boolean terminate) {
        if (terminate) {
            this.terminated.set(true);
        }
        Thread thread = currentThreadReference.get();
        if (thread == null) {
            return false;
        }
        thread.interrupt();
        return true;
    }

    @Override
    public void run() {
        if (terminated.get()) {
            LOG.info("Database clean up job '{}' has been terminated. Skipping...", job.getId().getIdentifier());
            return;
        }

        if (!isEnabled()) {
            LOG.info("Database clean up tasks are globally deactivated. Skipping job '{}'. To re-enable clean-up tasks set property '{}' to 'true'.", job.getId().getIdentifier(), DatabaseCleanUpProperty.ENABLED.getFQPropertyName());
            return;
        }

        Thread currentThread = Thread.currentThread();
        String prevName = currentThread.getName();
        currentThread.setName(job.getId().getIdentifier());
        currentThreadReference.set(currentThread);
        try {
            // Map to manage state
            Map<String, Object> state = new ConcurrentHashMap<>(4);

            // Prepare, clean-up and finish
            boolean prepared = false;
            try {
                prepared = job.getExecution().prepareCleanUp(state) ;
                if (prepared == false) {
                    LOG.info("Could not prepare clean-up of job '{}'.", job.getId());
                    return;
                }

                cleanUp(state, currentThread);
            } finally {
                if (prepared) {
                    finishSafe(state);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.info("Interrupting clean-up of job '{}'.", job.getId(), e);
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            LOG.warn("Failed to perform clean-up for: '{}'", job.getId(), t);
        } finally {
            currentThread.setName(prevName);
            currentThreadReference.set(null);
        }
    }

    private boolean isEnabled() {
        LeanConfigurationService configService = services.getOptionalService(LeanConfigurationService.class);
        return configService == null ? DatabaseCleanUpProperty.ENABLED.getDefaultValue(Boolean.class).booleanValue() : configService.getBooleanProperty(DatabaseCleanUpProperty.ENABLED);
    }

    private void cleanUp(Map<String, Object> state, Thread currentThread) throws OXException, InterruptedException {
        // Grab needed services
        ContextService contextService = services.getServiceSafe(ContextService.class);
        TimerService timerService = services.getServiceSafe(TimerService.class);
        ThreadPoolService threadPool = services.getServiceSafe(ThreadPoolService.class);
        Updater updater = Updater.getInstance();
        NeededServices neededServices = new NeededServices(executionManagement, contextService, timerService, updater, services);

        if (optRepresentativeContextId != null) {
            // Start time stamp
            long start = System.currentTimeMillis();

            // A certain schema is given
            cleanUpForSchemaWithRetry(job, optRepresentativeContextId, state, neededServices, currentThread);

            long duration = System.currentTimeMillis() - start;
            LOG.info("Clean-up by job '{}' took {}ms ({})", job.getId(), formatDuration(duration), exactly(duration, true)); // NOSONARLINT
            return;
        }

        ThreadPoolCompletionService<Void> completionService = null;
        try {
            // Iterate over representative context identifier per schema
            List<Integer> contextsIdInDifferentSchemas = ContextStorage.getInstance().getDistinctContextsPerSchema();
            int size = contextsIdInDifferentSchemas.size();
            if (size <= 0) {
                // No schemas available
                return;
            }

            // Only one schema available or clean-up job does not consider all database schemas
            if (size == 1 || !job.getExecution().considersAllDatabaseSchemas()) {
                // Start time stamp
                long start = System.currentTimeMillis();

                // Process that single schema
                cleanUpForSchemaWithRetry(job, contextsIdInDifferentSchemas.get(0), state, neededServices, currentThread);

                long duration = System.currentTimeMillis() - start;
                LOG.info("Clean-up by job '{}' took {}ms ({})", job.getId(), formatDuration(duration), exactly(duration, true)); // NOSONARLINT
                return;
            }

            // More than one schema available. Initiate concurrent execution.
            if (shuffle && size > 1) {
                Collections.shuffle(contextsIdInDifferentSchemas, SHUFFLE_RANDOM);
            }

            // Logging stuff for iterating all available schemas
            long start = System.currentTimeMillis();
            long logTimeDistance = TimeUnit.SECONDS.toMillis(10);
            AtomicLong lastLogTimeRef = new AtomicLong(start);
            AtomicInteger progressCounter = new AtomicInteger(0);

            // Schedule a task for each schema
            completionService = new BoundedCompletionService<Void>(threadPool, 10);
            for (Integer representativeContextId : contextsIdInDifferentSchemas) {
                completionService.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        // Process schema
                        cleanUpForSchemaWithRetry(job, representativeContextId, state, neededServices, currentThread);

                        // Progress logging
                        int numProcessed = progressCounter.incrementAndGet();
                        long now = System.currentTimeMillis();
                        long lastLogTime = lastLogTimeRef.get();
                        if (now > lastLogTime + logTimeDistance && lastLogTimeRef.compareAndSet(lastLogTime, now)) {
                            LOG.info("Clean-up job '{}' {}% finished ({}/{}).", job.getId(), I(numProcessed * 100 / size), I(numProcessed), I(size));
                        }
                        return null;
                    }
                });
            }

            // Await completion...
            for (int k = size; k-- > 0;) {
                try {
                    completionService.take().get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof OXException) {
                        throw (OXException) cause;
                    }
                    cause = cause == null ? e : cause;
                    throw OXException.general(cause.getMessage(), cause);
                }
            }
            completionService = null;

            long duration = System.currentTimeMillis() - start;
            LOG.info("Clean-up by job '{}' took {}ms ({})", job.getId(), formatDuration(duration), exactly(duration, true)); // NOSONARLINT
        } finally {
            if (completionService != null) {
                completionService.cancel(true);
            }
        }
    }

    private static void cleanUpForSchemaWithRetry(CleanUpJob job, Integer representativeContextId, Map<String, Object> state, NeededServices neededServices, Thread currentThread) throws OXException, InterruptedException {
        PoolAndSchema poolAndSchema = getSchema(representativeContextId, neededServices.contextService);
        if (!checkSchemaStatus(job, poolAndSchema, neededServices.updater)) {
            // Update running or pending. Abort clean-up run for that schema...
            return;
        }

        // Execute for schema
        boolean done = false;
        int retryCount = 3;
        for (int retry = retryCount; !done && retry-- > 0;) {
            // Check again if thread has been interrupted meanwhile
            if (currentThread.isInterrupted()) {
                LOG.info("Interrupting clean-up of job '{}'.", job.getId());
                return;
            }

            // Perform clean-up
            try {
                cleanUpForSchema(job, representativeContextId.intValue(), poolAndSchema.getSchema(), poolAndSchema.getPoolId(), state, neededServices, currentThread);
                done = true;
            } catch (OXException e) {
                if (retry > 0 && Category.CATEGORY_TRY_AGAIN.equals(e.getCategory())) {
                    long delay = ((retryCount - retry) * 1000) + ((long) (Math.random() * 1000));
                    LOG.debug("Failed clean-up of job '{}' for schema {}: {}; trying again in {}ms...", job.getId(), poolAndSchema.getSchema(), e.getMessage(), L(delay));
                    Thread.sleep(delay);
                } else {
                    LOG.warn("Failed clean-up of job '{}' for schema {}", job.getId(), poolAndSchema.getSchema(), e);
                    done = true;
                }
            } catch (Exception e) {
                LOG.warn("Failed clean-up of job '{}' for schema {}", job.getId(), poolAndSchema.getSchema(), e);
                done = true;
            }
        }
    }

    /**
     * Checks the update status of given schema.
     *
     * @param job The clean-up job to check for
     * @param poolAndSchema The pool and schema information
     * @param updater The update instance to use
     * @return <code>true</code> if schema update status is up-to-date, otherwise <code>false</code> if either currently updating or updates are pending
     * @throws OXException If update status cannot be checked
     */
    private static boolean checkSchemaStatus(CleanUpJob job, PoolAndSchema poolAndSchema, Updater updater) throws OXException {
        UpdateStatus status = updater.getStatus(poolAndSchema.getSchema(), poolAndSchema.getPoolId());
        if (status.blockingUpdatesRunning()) {
            // Context-associated schema is currently updated. Abort clean-up for that schema
            LOG.info("Update running: Skipping clean-up of job '{}' for schema {} since that schema is currently updated", job.getId(), poolAndSchema.getSchema());
            return false;
        }
        if ((status.needsBlockingUpdates() || status.needsBackgroundUpdates()) && !status.blockingUpdatesRunning() && !status.backgroundUpdatesRunning()) {
            // Context-associated schema needs an update. Abort clean-up for that schema
            LOG.info("Update needed: Skipping clean-up of job '{}' for schema {} since that schema needs an update", job.getId(), poolAndSchema.getSchema());
            return false;
        }
        return true;
    }

    private static void cleanUpForSchema(CleanUpJob job, int representativeContextId, String schema, int poolId, Map<String, Object> state, NeededServices neededServices, Thread currentThread) throws OXException {
        if (isNotApplicableFor(job, representativeContextId, schema, poolId, state, neededServices.services)) {
            // Not applicable for current schema
            LOG.debug("Clean-up job '{}' not applicable against schema {}. Therefore job is not executed.", job.getId(), schema);
            return;
        }

        if (job.isRunsExclusive() || job.getType() == CleanUpJobType.GENERAL) {
            // Job's execution needs to be coordinated among cluster nodes
            // Check for permission to execute it
            if (neededServices.executionManagement.checkExecutionPermission(job, representativeContextId)) {
                // Permission acquired...
                // Start timer task for periodic refresh of job's last-touched time stamp
                long refreshIntervalMillis = REFRESH_LAST_TOUCHED_STAMP_INTERVAL_MILLIS;
                ScheduledTimerTask timerTask = neededServices.timerService.scheduleWithFixedDelay(newRefreshTask(job, schema, representativeContextId, neededServices), refreshIntervalMillis, refreshIntervalMillis);
                try {
                    // Execute job
                    executeJobFor(job, representativeContextId, schema, poolId, state, currentThread, neededServices.services);
                    LOG.debug("Successfully executed clean-up job '{}' against schema {}", job.getId(), schema);
                } finally {
                    // Stop timer task
                    stopTimerTaskSafe(job, timerTask, schema, neededServices.timerService);
                    neededServices.executionManagement.markExecutionDone(job, representativeContextId);
                }
            } else {
                // No permission
                LOG.debug("No permission to execute clean-up job '{}' against schema {}; e.g. another process currently performs that job or job's delay has not yet elapsed.", job.getId(), schema);
            }
        } else {
            // May run at any time on any node, thus just execute it
            executeJobFor(job, representativeContextId, schema, poolId, state, currentThread, neededServices.services);
            LOG.debug("Successfully executed clean-up job '{}' against schema {}", job.getId(), schema);
        }
    }

    private static Runnable newRefreshTask(CleanUpJob job, String schema, int representativeContextId, NeededServices neededServices) {
        return new RefreshJobTimeStampTask(job, schema, representativeContextId, neededServices.executionManagement);
    }

    private static void stopTimerTaskSafe(CleanUpJob job, ScheduledTimerTask timerTask, String schema, TimerService timerService) {
        if (timerTask != null) {
            try {
                timerTask.cancel();
                timerService.purge();
            } catch (Exception e) {
                LOG.warn("Failed to stop stamp-refreshing timer task of clean-up of job '{}' for schema {}", job.getId(), schema, e);
            }
        }
    }

    private static boolean isNotApplicableFor(CleanUpJob job, int representativeContextId, String schema, int poolId, Map<String, Object> state, ServiceLookup services) throws OXException {
        return isApplicableFor(job, representativeContextId, schema, poolId, state, services) == false;
    }

    private static boolean isApplicableFor(CleanUpJob job, int representativeContextId, String schema, int poolId, Map<String, Object> state, ServiceLookup services) throws OXException {
        ReadOnlyCleanUpExecutionConnectionProvider connectionProvider = new ReadOnlyCleanUpExecutionConnectionProvider(representativeContextId, services);
        try {
            return job.getExecution().isApplicableFor(schema, representativeContextId, poolId, state, connectionProvider);
        } finally {
            connectionProvider.close();
        }
    }

    private static void executeJobFor(CleanUpJob job, int representativeContextId, String schema, int poolId, Map<String, Object> state, Thread currentThread, ServiceLookup services) throws OXException {
        // Create connection provider
        ReadWriteCleanUpExecutionConnectionProvider connectionProvider = new ReadWriteCleanUpExecutionConnectionProvider(representativeContextId, job.isPreferNoConnectionTimeout(), services);
        try {
            // Check if thread has been interrupted meanwhile
            if (currentThread.isInterrupted()) {
                LOG.info("Interrupting clean-up of job '{}'.", job.getId());
                return;
            }

            // Execute job
            job.getExecution().executeFor(schema, representativeContextId, poolId, state, connectionProvider);

            // Commit optional connection
            connectionProvider.commitAfterSuccess();
        } catch (SQLException e) {
            throw DatabaseCleanUpExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            // Roll-back optional connection
            connectionProvider.close();
        }
    }

    private void finishSafe(Map<String, Object> state) {
        try {
            job.getExecution().finishCleanUp(state);
        } catch (Exception e) {
            LOG.warn("Failed to finish clean-up of job '{}'", job.getId(), e);
        }
    }

    private static String formatDuration(long duration) {
        if (MILLIS_FORMAT_LOCK.tryLock()) {
            try {
                return MILLIS_FORMAT.format(duration);
            } finally {
                MILLIS_FORMAT_LOCK.unlock();
            }
        }

        // Use thread-specific DecimalFormat instance
        NumberFormat format = newNumberFormat();
        return format.format(duration);
    }

    private static PoolAndSchema getSchema(Integer representativeContextId, ContextService contextService) throws OXException {
        Map<PoolAndSchema, List<Integer>> associations = contextService.getSchemaAssociationsFor(Collections.singletonList(representativeContextId));
        if (associations.isEmpty()) {
            throw OXException.general("No such database pool and schema found for context " + representativeContextId);
        }
        return associations.keySet().iterator().next();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class RefreshJobTimeStampTask implements Runnable {

        private final DatabaseCleanUpExecutionManagement executionManagement;
        private final CleanUpJob job;
        private final int representativeContextId;
        private final String schema;

        RefreshJobTimeStampTask(CleanUpJob job, String schema, int representativeContextId, DatabaseCleanUpExecutionManagement executionManagement) {
            super();
            this.job = job;
            this.representativeContextId = representativeContextId;
            this.schema = schema;
            this.executionManagement = executionManagement;
        }

        @Override
        public void run() {
            try {
                if (executionManagement.refreshTimeStamp(job, representativeContextId)) {
                    LOG.debug("Successfully refreshed last-touched time stamp of clean-up job '{}' for schema {}", job.getId(), schema);
                }
            } catch (Exception e) {
                LOG.warn("Failed to refresh the last-touched time stamp for clean-up of job '{}' for schema {}", job.getId(), schema, e);
            }
        }
    }

    private static record NeededServices(DatabaseCleanUpExecutionManagement executionManagement, ContextService contextService , TimerService timerService, Updater updater, ServiceLookup services) {
        // Don't care
    }

}
