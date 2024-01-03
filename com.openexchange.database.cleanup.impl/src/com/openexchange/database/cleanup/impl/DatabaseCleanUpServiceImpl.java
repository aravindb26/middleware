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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.database.cleanup.CleanUpInfo;
import com.openexchange.database.cleanup.CleanUpJob;
import com.openexchange.database.cleanup.CleanUpJobId;
import com.openexchange.database.cleanup.CleanUpJobType;
import com.openexchange.database.cleanup.DatabaseCleanUpExceptionCode;
import com.openexchange.database.cleanup.DatabaseCleanUpService;
import com.openexchange.database.cleanup.impl.storage.DatabaseCleanUpExecutionManagement;
import com.openexchange.database.cleanup.impl.utils.IncreaseThreadsBeforeQueueingThreadPoolExecutor;
import com.openexchange.exception.OXException;
import com.openexchange.java.ExceptionCatchingRunnable;
import com.openexchange.schedule.TaskScheduler;
import com.openexchange.server.ServiceLookup;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;


/**
 * {@link DatabaseCleanUpServiceImpl} - The database clean-up service implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DatabaseCleanUpServiceImpl implements DatabaseCleanUpService {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DatabaseCleanUpServiceImpl.class);

    /** The interval in milliseconds for refreshing a job's last-touched time stamp */
    public static final long REFRESH_LAST_TOUCHED_STAMP_INTERVAL_MILLIS = 20000L;

    /** The expiration in milliseconds. If a task is idle for that long, is is assumed to be expired/stalled. */
    public static final long EXPIRATION_MILLIS = 60000L;

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final ConcurrentMap<CleanUpJobId, TimerTaskOrJob> submittedJobs;
    private final DatabaseCleanUpExecutionManagement executionManagement;
    private final ThreadPoolExecutor jobExecutorService;
    private final TaskScheduler taskScheduler;
    private final ServiceLookup services;
    private final boolean abortRunningGeneralJobs;
    private boolean stopped;

    /**
     * Initializes a new {@link DatabaseCleanUpServiceImpl}.
     *
     * @param scheduleExpression The schedule expression to use
     * @param frequencyMillis The frequency in milliseconds
     * @param concurrencyLevel The concurrency level determining how many threads may be used to execute scheduled clean-up jobs
     * @param abortRunningGeneralJobs <code>true</code> to abort running clean-up jobs of general type when specified schedule is elapsed; otherwise <code>false</code> to allow continuing execution
     * @param executionManagement The execution management to use
     * @param services The service look-up
     */
    public DatabaseCleanUpServiceImpl(String scheduleExpression, long frequencyMillis, int concurrencyLevel, boolean abortRunningGeneralJobs, DatabaseCleanUpExecutionManagement executionManagement, ServiceLookup services) {
        super();
        this.abortRunningGeneralJobs = abortRunningGeneralJobs;
        this.services = services;
        this.jobExecutorService = new IncreaseThreadsBeforeQueueingThreadPoolExecutor(0, concurrencyLevel, new DatabaseCleanUpThreadFactory());
        this.executionManagement = executionManagement;
        this.submittedJobs = new ConcurrentHashMap<>(32, 0.9F, 1);
        this.stopped = false;

        ExceptionCatchingRunnable periodicTask = this::checkDueJobs;
        ExceptionCatchingRunnable stopTask = this::dropQueuedAndStopRunningJobs;
        this.taskScheduler = TaskScheduler.builder()
            .withFrequencyMillis(frequencyMillis)
            .withPeriodicTask(periodicTask)
            .withScheduleExpression(scheduleExpression)
            .withStopProcessingListener(stopTask)
            .withTimerServiceFrom(services)
            .build();
    }

    /**
     * Checks for due clean-up jobs and submits them for execution.
     */
    private void checkDueJobs() {
        CleanUpJobAndRunnable jobAndRunnable;
        for (TimerTaskOrJob timerTaskOrJob : submittedJobs.values()) {
            jobAndRunnable = timerTaskOrJob.jobAndRunnable;
            if (jobAndRunnable != null) {
                CleanUpJobRunnable runnable = new CleanUpJobRunnable(jobAndRunnable.job, true, executionManagement, services);
                if (jobAndRunnable.currentRunnable.compareAndSet(null, runnable)) {
                    // Put into queue
                    jobExecutorService.execute(new UnreferencingRunnable(jobAndRunnable, runnable));
                }
            }
        }
    }

    /**
     * Drops queued and stops currently running clean-up jobs.
     */
    private void dropQueuedAndStopRunningJobs() {
        this.jobExecutorService.getQueue().clear();
        if (abortRunningGeneralJobs) {
            for (TimerTaskOrJob timerTaskOrJob : submittedJobs.values()) {
                if (timerTaskOrJob.jobAndRunnable != null) {
                    CleanUpJobRunnable runnable = timerTaskOrJob.jobAndRunnable.currentRunnable.get();
                    if (runnable != null) {
                        runnable.interrupt(true);
                    }
                }
            }
        }
    }

    /**
     * Starts scheduling general clean-up jobs.
     */
    public synchronized void startScheduling() {
        // Initialize scheduling stuff
        taskScheduler.start();
        LOGGER.info("Started scheduling of general clean-up jobs");
    }

    @Override
    public List<String> getCleanUpJobs() throws OXException {
        List<String> identifiers = new ArrayList<>(submittedJobs.size());
        for (CleanUpJobId jobId : submittedJobs.keySet()) {
            identifiers.add(jobId.getIdentifier());
        }
        return identifiers;
    }

    @Override
    public synchronized CleanUpInfo scheduleCleanUpJob(CleanUpJob job) throws OXException {
        if (stopped) {
            throw new RejectedExecutionException("Database clean-up framework already stopped");
        }

        CleanUpJobType type = job.getType();
        if (type == null) {
            throw OXException.general("Missing clean-up job type for " + job.getId());
        }

        switch (type) {
            case GENERAL: {
                // Try to exclusively put into map
                CleanUpJobAndRunnable jobAndRunnable = new CleanUpJobAndRunnable(job);
                if (submittedJobs.putIfAbsent(job.getId(), new TimerTaskOrJob(jobAndRunnable)) != null) {
                    // Such a task already exists
                    throw DatabaseCleanUpExceptionCode.DUPLICATE_CLEAN_UP_JOB.create(job.getId().getIdentifier());
                }

                // Successfully put into map.
                // Get & return clean-up info
                LOGGER.info("Successfully scheduled general clean-up job '{}'", job.getId().getIdentifier());
                return new ScheduledJobCleanUpInfo(job.getId(), jobAndRunnable.currentRunnable, submittedJobs);
            }
            case CUSTOM: {
                // Require timer service
                TimerService timerService = services.getServiceSafe(TimerService.class);

                // Try to exclusively put into map
                FutureTask<ScheduledTimerTask> timerTaskFuture = new FutureTask<>(new TimerTaskSpawningCallable(timerService, job, executionManagement, services));
                if (submittedJobs.putIfAbsent(job.getId(), new TimerTaskOrJob(timerTaskFuture)) != null) {
                    // Such a task already exists
                    throw DatabaseCleanUpExceptionCode.DUPLICATE_CLEAN_UP_JOB.create(job.getId().getIdentifier());
                }

                // Successfully put into map. Submit clean-up job.
                timerTaskFuture.run();

                // Get & return clean-up info
                LOGGER.info("Successfully scheduled custom clean-up job '{}'", job.getId().getIdentifier());
                return new CustomJobCleanUpInfo(job.getId(), getFrom(timerTaskFuture), submittedJobs);
            }
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        }
    }

    /**
     * Stops this clean-up service.
     *
     * @throws OXException If stopping fails
     */
    public synchronized void stop() throws OXException {
        stopped = true;
        taskScheduler.stop();
        jobExecutorService.getQueue().clear();
        jobExecutorService.shutdownNow();
        for (TimerTaskOrJob timerTaskOrJob : submittedJobs.values()) {
            if (timerTaskOrJob.timerTaskFuture != null) {
                getFrom(timerTaskOrJob.timerTaskFuture).cancel(true);
            } else {
                CleanUpJobRunnable runnable = timerTaskOrJob.jobAndRunnable.currentRunnable.get();
                if (runnable != null) {
                    runnable.interrupt(true);
                }
            }
        }
        submittedJobs.clear();
        LOGGER.info("Stopped database clean-up framework");
    }

    private static <V> V getFrom(Future<V> future) throws OXException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw DatabaseCleanUpExceptionCode.UNEXPECTED_ERROR.create(e, "Interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                throw (OXException) cause;
            }
            if (cause == null) {
                cause = e;
            }
            throw DatabaseCleanUpExceptionCode.UNEXPECTED_ERROR.create(cause, cause.getMessage());
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Executes specified runnable and clears reference after execution is done.
     */
    private static class UnreferencingRunnable implements Runnable {

        private final CleanUpJobAndRunnable jobAndRunnable;
        private final CleanUpJobRunnable runnable;

        /**
         * Initializes a new {@link UnreferencingRunnable}.
         *
         * @param jobAndRunnable The job and runnable instance to unset when execution is done
         * @param runnable The runnable to perform
         */
        UnreferencingRunnable(CleanUpJobAndRunnable jobAndRunnable, CleanUpJobRunnable runnable) {
            super();
            this.jobAndRunnable = jobAndRunnable;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } finally {
                jobAndRunnable.currentRunnable.set(null);
            }
        }
    }

    /**
     * Spawns new timer task for given custom clean-up job.
     */
    private static class TimerTaskSpawningCallable implements Callable<ScheduledTimerTask> {

        private final TimerService timerService;
        private final CleanUpJob job;
        private final DatabaseCleanUpExecutionManagement executionManagement;
        private final ServiceLookup services;

        TimerTaskSpawningCallable(TimerService timerService, CleanUpJob job, DatabaseCleanUpExecutionManagement executionManagement, ServiceLookup services) {
            super();
            this.timerService = timerService;
            this.job = job;
            this.executionManagement = executionManagement;
            this.services = services;
        }

        @Override
        public ScheduledTimerTask call() {
            CleanUpJobRunnable runnable = new CleanUpJobRunnable(job, executionManagement, services);
            return timerService.scheduleWithFixedDelay(runnable, job.getInitialDelay().toMillis(), job.getDelay().toMillis());
        }
    }

    /**
     * Simple helper class to have the currently executing runnable linked to its job.
     */
    private static class CleanUpJobAndRunnable {

        final CleanUpJob job;
        final AtomicReference<CleanUpJobRunnable> currentRunnable;

        CleanUpJobAndRunnable(CleanUpJob job) {
            super();
            this.job = job;
            this.currentRunnable = new AtomicReference<>(null);
        }
    }

    /**
     * Simple helper class.
     */
    private static class TimerTaskOrJob {

        final Future<ScheduledTimerTask> timerTaskFuture;
        final CleanUpJobAndRunnable jobAndRunnable;

        TimerTaskOrJob(Future<ScheduledTimerTask> timerTaskFuture) {
            super();
            this.timerTaskFuture = timerTaskFuture;
            this.jobAndRunnable = null;
        }

        TimerTaskOrJob(CleanUpJobAndRunnable jobAndRunnable) {
            super();
            this.jobAndRunnable = jobAndRunnable;
            this.timerTaskFuture = null;
        }
    }

}
