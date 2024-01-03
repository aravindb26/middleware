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

package com.openexchange.gdpr.dataexport.impl;

import static com.openexchange.gdpr.dataexport.impl.DataExportUtility.deleteQuietly;
import static com.openexchange.gdpr.dataexport.impl.DataExportUtility.stringFor;
import static com.openexchange.gdpr.dataexport.impl.notification.DataExportNotificationSender.sendNotificationAndSetMarker;
import static com.openexchange.java.Autoboxing.I;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.ResponseFields;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorage;
import com.openexchange.filestore.FileStorageCodes;
import com.openexchange.gdpr.dataexport.DataExportAbortedException;
import com.openexchange.gdpr.dataexport.DataExportConfig;
import com.openexchange.gdpr.dataexport.DataExportDiagnosticsReport;
import com.openexchange.gdpr.dataexport.DataExportExceptionCode;
import com.openexchange.gdpr.dataexport.DataExportJob;
import com.openexchange.gdpr.dataexport.DataExportProvider;
import com.openexchange.gdpr.dataexport.DataExportProviderRegistry;
import com.openexchange.gdpr.dataexport.DataExportSavepoint;
import com.openexchange.gdpr.dataexport.DataExportStatus;
import com.openexchange.gdpr.dataexport.DataExportStorageService;
import com.openexchange.gdpr.dataexport.DataExportTask;
import com.openexchange.gdpr.dataexport.DataExportWorkItem;
import com.openexchange.gdpr.dataexport.DiagnosticsReportOptions;
import com.openexchange.gdpr.dataexport.ExportResult;
import com.openexchange.gdpr.dataexport.Message;
import com.openexchange.gdpr.dataexport.PauseResult;
import com.openexchange.gdpr.dataexport.impl.notification.Reason;
import com.openexchange.gdpr.dataexport.impl.utils.ChunkedZippedOutputStream;
import com.openexchange.java.ISO8601Utils;
import com.openexchange.java.Streams;
import com.openexchange.java.util.UUIDs;
import com.openexchange.log.LogProperties;
import com.openexchange.server.ServiceLookup;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadRenamer;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import com.openexchange.user.UserService;

/**
 * {@link DataExportTaskExecution} - Continues executing data export tasks as long as
 * {@link DataExportStorageService#getNextDataExportJob() getNextDataExportTask()} returns a task to work on.
 * <p>
 * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
 * Note: Don't forget to invoke {@link #allowProcessing(Runnable)}
 * </div>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class DataExportTaskExecution extends AbstractTask<Void> {

    /** The logger constant */
    static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataExportTaskExecution.class);

    /** Counter for instances of <code>DataExportTaskExecution</code> */
    private static final AtomicLong COUNTER = new AtomicLong();

    private final DataExportProviderRegistry providerRegistry;
    private final DataExportStorageService storageService;
    private final long expirationTimeMillis;
    private final long maxProcessingTimeMillis;
    private final long maxTimeToLiveMillis;
    private final boolean useZip64;
    private final AtomicReference<JobAndReport> currentJob;
    private final AtomicReference<ProviderInfo> currentProviderInfo;
    private final ProcessingThreadReference processingThread;
    private final AtomicReference<ScheduledTimerTask> currentToucher;
    private final AtomicReference<ScheduledTimerTask> currentStopper;
    private final Lock stopLock;
    private final ServiceLookup services;
    private final AtomicReference<Runnable> cleanUpTask;
    private final CountDownLatch latch;
    private final AtomicLong startTime;
    private final DiagnosticsReportOptions diagnosticsReportOptions;
    private final long count;

    /**
     * Initializes a new {@link DataExportTaskExecution}.
     *
     * @param initialJob The initial job or <code>null</code>
     * @param addDiagnosticsReport code>true</code> to add diagnostics report; otherwise <code>false</code>
     * @param config The configuration
     * @param storageService The storage service to use
     * @param providerRegistry The listing of available data export providers
     * @param services The services
     */
    public DataExportTaskExecution(DataExportJob initialJob, DiagnosticsReportOptions diagnosticsReportOptions, DataExportConfig config, DataExportStorageService storageService, DataExportProviderRegistry providerRegistry, ServiceLookup services) {
        super();
        this.diagnosticsReportOptions = diagnosticsReportOptions;
        this.expirationTimeMillis = config.getExpirationTimeMillis();
        this.maxProcessingTimeMillis = config.getMaxProcessingTimeMillis();
        this.maxTimeToLiveMillis = config.getMaxTimeToLiveMillis();
        this.useZip64 = config.isUseZip64();
        currentJob = new AtomicReference<>(new JobAndReport(initialJob, null));
        currentToucher = new AtomicReference<>(null);
        currentStopper = new AtomicReference<>(null);
        stopLock = new ReentrantLock();
        this.storageService = storageService;
        this.providerRegistry = providerRegistry;
        this.services = services;
        currentProviderInfo = new AtomicReference<>(null);
        processingThread = new ProcessingThreadReference();
        cleanUpTask = new AtomicReference<>(null);
        latch = new CountDownLatch(1);
        startTime = new AtomicLong(0L);
        count = COUNTER.incrementAndGet();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getName()).append('-').append(count).toString();
    }

    @Override
    public void setThreadName(ThreadRenamer threadRenamer) {
        threadRenamer.renamePrefix(DataExportTaskExecution.class.getSimpleName());
    }

    /**
     * Gets the task that is currently executed.
     *
     * @return The optional task
     */
    public Optional<DataExportTask> getCurrentTask() {
        JobAndReport jobAndReport = currentJob.get();
        return jobAndReport == null ? Optional.empty() : Optional.of(jobAndReport.job.getDataExportTask());
    }

    /**
     * Allows processing for this execution.
     */
    public void allowProcessing(Runnable cleanUpTask) {
        this.cleanUpTask.set(cleanUpTask);
        latch.countDown();
    }

    /**
     * Gets the processing time in milliseconds.
     *
     * @return The processing time in milliseconds or <code>-1</code> if not yet started
     */
    public long getProcessingTimeMillis() {
        long startTimeMillis = startTime.get();
        return startTimeMillis <= 0 ? -1L : System.currentTimeMillis() - startTimeMillis;
    }

    /**
     * Checks whether this execution is considered as being valid.
     * <p>
     * Valid means, either processing is ahead or it is currently processed.
     *
     * @return <code>true</code> if processed; otherwise <code>false</code>
     */
    public boolean isValid() {
        return processingThread.isPendingOrRunning();
    }

    /**
     * Checks whether this execution is considered as being invalid.
     * <p>
     * Invalid means, processing has started, but there is no more a processing thread associated with it.
     *
     * @return <code>true</code> if <b>not</b> processed; otherwise <code>false</code>
     */
    public boolean isInvalid() {
        return !isValid();
    }

    @Override
    public Void call() throws OXException {
        this.processingThread.setCurrentThread(Thread.currentThread());
        try {
            process();
        } finally {
            this.processingThread.unsetCurrentThread();
        }
        return null;
    }

    /**
     * Processes this data export execution that is querying and handling pending/paused/expired data export tasks.
     *
     * @throws OXException If processing fails
     */
    private void process() throws OXException {
        try {
            // Check if thread is interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException("Interrupted prior to processing any task");
            }

            // Await until processing is allowed; see allowProcessing()
            latch.await();
            startTime.set(System.currentTimeMillis());

            Optional<DataExportJob> optionalJob;
            {
                JobAndReport initialJob = currentJob.getAndSet(null);
                if (initialJob == null) {
                    // Get next available task from storage
                    optionalJob = storageService.getNextDataExportJob();
                    if (optionalJob.isPresent()) {
                        DataExportJob job = optionalJob.get();
                        LOGGER.debug("Got next data export task {} of user {} in context {} for being executed by executor {}", stringFor(job.getDataExportTask().getId()), I(job.getDataExportTask().getUserId()), I(job.getDataExportTask().getContextId()), this);
                    }
                } else {
                    optionalJob = Optional.of(initialJob.job);
                }
            }

            if (!optionalJob.isPresent()) {
                // No further pending/paused/expired jobs available
                LOGGER.debug("Currently there are no data export tasks to execute. Stopping executor {}", this);
                return;
            }

            do {
                // Get the job to process
                DataExportJob job = optionalJob.get();

                // Check if thread is interrupted
                if (Thread.interrupted()) {
                    LOGGER.debug("Interrupted before starting execution of data export task {} of user {} in context {} with executor {}", stringFor(job.getDataExportTask().getId()), I(job.getDataExportTask().getUserId()), I(job.getDataExportTask().getContextId()), this);
                    StringBuilder msg = new StringBuilder("Interrupted while processing task \"");
                    msg.append(UUIDs.getUnformattedString(job.getDataExportTask().getId())).append('"');
                    throw new InterruptedException(msg.toString());
                }

                // Handle current job
                LOGGER.debug("Starting to execute data export task {} of user {} in context {} with executor {}", stringFor(job.getDataExportTask().getId()), I(job.getDataExportTask().getUserId()), I(job.getDataExportTask().getContextId()), this);
                handleJob(job);

                // Acquire next job... after some artificial delay
                long nanosToWait = TimeUnit.NANOSECONDS.convert(1000 + ((long) (Math.random() * 5000)), TimeUnit.MILLISECONDS);
                LockSupport.parkNanos(nanosToWait);
                optionalJob = storageService.getNextDataExportJob();
            } while (optionalJob.isPresent());

            // No further pending/paused/expired jobs available
        } catch (InterruptedException e) {
            // Keep interrupted status
            Thread.currentThread().interrupt();
            LOGGER.info("Data export execution interrupted for executor {}", this, e);
        } catch (Throwable t) {
            LOGGER.warn("Data export execution failed for executor {}", this, t);
            throw t;
        } finally {
            Runnable cleanUpTask = this.cleanUpTask.get();
            if (cleanUpTask != null) {
                try {
                    cleanUpTask.run();
                } catch (Exception e) {
                    LOGGER.warn("Failed to perform clean-up for executor {}", this, e);
                }
            }
        }
    }

    /**
     * Handles specified data export task job.
     *
     * @param job The job to handle
     */
    private void handleJob(DataExportJob job) {
        DataExportTask task = job.getDataExportTask();
        int userId = task.getUserId();
        int contextId = task.getContextId();
        UUID taskId = task.getId();
        Optional<DataExportDiagnosticsReport> optionalReport = diagnosticsReportOptions.isAddDiagnosticsReport() ? Optional.of(new DataExportDiagnosticsReport(diagnosticsReportOptions)) : Optional.empty();

        boolean error = true;
        currentJob.set(new JobAndReport(job, optionalReport));
        try {
            // Check task status (from loaded instance and live)
            if ((task.getStatus().isAborted() || storageService.getDataExportStatus(userId, contextId).orElse(null) == DataExportStatus.ABORTED)) {
                // Task has been aborted. Delete it...
                storageService.deleteDataExportTask(userId, contextId);

                // Trigger notification for user that data export has been aborted
                sendNotificationAndSetMarker(Reason.ABORTED, task.getCreationTime(), null, null, taskId, userId, contextId, false, services);
                return;
            }

            // User's locale
            Locale locale = getUserLocale(userId, contextId);

            // Determine the file storage to use
            FileStorage fileStorage = DataExportUtility.getFileStorageFor(task);

            // Fetch first work item
            Optional<DataExportWorkItem> optionalItem = job.getNextDataExportWorkItem();

            // Initialize toucher and stopper (if max. processing time is given)
            initTimerTasks(taskId, userId, contextId);

            LOGGER.debug("Starting data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this);

            // Process it (if present) and fetch remaining ones subsequently
            DataExportStatus currentStatus = DataExportStatus.RUNNING;
            boolean keepGoing = true;
            if (optionalItem.isPresent()) {
                boolean stillRunningAndHasNext = false;
                do {
                    try {
                        keepGoing = handleWorkItem(optionalItem.get(), task, locale, fileStorage, optionalReport);
                    } catch (DataExportAbortedException e) {
                        // Aborted...
                        LOGGER.debug("Aborted data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this);
                        keepGoing = false;
                        currentStatus = DataExportStatus.ABORTED;
                    }

                    if (keepGoing) {
                        currentStatus = storageService.getDataExportStatus(userId, contextId).orElse(null);
                        if (currentStatus != DataExportStatus.RUNNING) {
                            LOGGER.debug("Stopping data export task {} of user {} in context {} with executor {} since status changed to {}", stringFor(taskId), I(userId), I(contextId), this, currentStatus);
                            stillRunningAndHasNext = false;
                        } else {
                            optionalItem = job.getNextDataExportWorkItem();
                            if (optionalItem.isPresent()) {
                                stillRunningAndHasNext = true;
                            } else {
                                LOGGER.debug("Stopping data export task {} of user {} in context {} with executor {} since there are no more work items available", stringFor(taskId), I(userId), I(contextId), this);
                                stillRunningAndHasNext = false;
                            }
                        }
                    }
                } while (keepGoing && stillRunningAndHasNext);
            }

            // Check status
            if (currentStatus == null || currentStatus == DataExportStatus.ABORTED) {
                // Task has been aborted. Delete it...
                storageService.deleteDataExportTask(userId, contextId);

                // Trigger notification for user that data export has been aborted
                sendNotificationAndSetMarker(Reason.ABORTED, task.getCreationTime(), null, null, taskId, userId, contextId, false, services);
                LOGGER.debug("Data export task {} of user {} in context {} aborted", stringFor(taskId), I(userId), I(contextId));
            } else if (currentStatus == DataExportStatus.RUNNING) {
                if (keepGoing) {
                    // Consider as completed. Grab up-to-date version from storage
                    LOGGER.debug("Finished data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this);
                    DataExportTask reloaded = storageService.getDataExportTask(userId, contextId).orElse(null);
                    if (reloaded != null) {
                        // Finished because no more work items left and therefore task is completed. Generate resulting archive(s)
                        LOGGER.debug("Generating result files for data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this);
                        ChunkedZippedOutputStream zipOut = new ChunkedZippedOutputStream(reloaded, fileStorage, storageService, useZip64, services);
                        try {
                            generateResultFilesAndMarkAsDone(reloaded, fileStorage, zipOut, optionalReport, locale);
                        } finally {
                            zipOut.close();
                        }

                        // Drop work items' files to free space
                        try {
                            storageService.dropIntermediateFiles(taskId, userId, contextId);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to drop intermediate files from data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
                        }

                        // Determine expiration date
                        Date expiryDate;
                        try {
                            Optional<Date> lastAccessedTimeStamp = storageService.getLastAccessedTimeStamp(userId, contextId);
                            expiryDate = new Date(lastAccessedTimeStamp.get().getTime() + maxTimeToLiveMillis);
                        } catch (Exception e) {
                            expiryDate = new Date(System.currentTimeMillis() + maxTimeToLiveMillis);
                            LOGGER.warn("Failed to query last-accessed time stamp from data export task {} of user {} in context {}. Assuming \"{}\" as expiration date.", stringFor(taskId), I(userId), I(contextId), ISO8601Utils.format(expiryDate), e);
                        }

                        // Trigger notification for user that data export is available
                        sendNotificationAndSetMarker(Reason.SUCCESS, task.getCreationTime(), expiryDate, task.getArguments().getHostInfo(), taskId, userId, contextId, true, services);
                        LOGGER.info("Data export task {} of user {} in context {} completed", stringFor(taskId), I(userId), I(contextId));
                    } else {
                        LOGGER.debug("Could not find data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this);
                    }
                } else {
                    // Processing work items has been stopped

                    // TODO: Anything to do here?
                }
            }

            error = false;
        } catch (AlreadyLoggedError e) {
            // Re-throw error
            throw e.error;
        } catch (Error e) {
            LOGGER.warn("Data export task {} of user {} in context {} failed fatally", stringFor(taskId), I(userId), I(contextId), e);
            // Re-throw error
            throw e;
        } catch (AlreadyLoggedException t) {
            // Just caught here to avoid duplicate logging. Nothing to do...
        } catch (Throwable t) {
            LOGGER.warn("Data export task {} of user {} in context {} failed", stringFor(taskId), I(userId), I(contextId), t);
        } finally {
            cancelTimerTasks(taskId, userId, contextId, true);
            currentJob.set(null);
            if (error) {
                try {
                    storageService.markFailed(taskId, userId, contextId);
                } catch (Exception e) {
                    LOGGER.warn("Cannot set failed marker for data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
                }

                // Trigger notification for user that data export failed
                try {
                    sendNotificationAndSetMarker(Reason.FAILED, task.getCreationTime(), null, null, taskId, userId, contextId, true, services);
                } catch (Exception e) {
                    LOGGER.warn("Cannot set notification-sent marker for data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
                }
            }
        }
    }

    private boolean handleWorkItem(DataExportWorkItem item, DataExportTask task, Locale locale, FileStorage fileStorage, Optional<DataExportDiagnosticsReport> optionalReport) throws OXException, DataExportAbortedException {
        int userId = task.getUserId();
        int contextId = task.getContextId();
        UUID taskId = task.getId();
        String moduleId = item.getModuleId();
        boolean keepGoing = true;

        // Determine fitting provider with highest ranking
        Optional<DataExportProvider> optionalProvider = providerRegistry.getHighestRankedProviderFor(moduleId);
        if (!optionalProvider.isPresent()) {
            // No such provider
            OXException oxe = DataExportExceptionCode.NO_SUCH_PROVIDER.create(moduleId);
            storageService.markWorkItemFailed(Optional.ofNullable(toJson(oxe)), taskId, moduleId, userId, contextId);
            throw oxe;
        }
        DataExportProvider providerToUse = optionalProvider.get();

        // Get provider's path prefix
        String pathPrefix = providerToUse.getPathPrefix(locale);

        // Create the data sink
        String optPrevFileStorageLocation = item.getFileStorageLocation();
        DataExportSinkImpl sink = new DataExportSinkImpl(task, moduleId, pathPrefix, Optional.ofNullable(optPrevFileStorageLocation), fileStorage, useZip64, storageService, optionalReport, services);

        // Grab latest save-point (if any)
        Optional<JSONObject> optionalSavepoint;
        try {
            DataExportSavepoint savePoint = storageService.getSavePoint(taskId, moduleId, userId, contextId);
            optionalSavepoint = savePoint.getSavepoint();
            if (optionalReport.isPresent()) {
                Optional<DataExportDiagnosticsReport> optReport = savePoint.getReport();
                if (optReport.isPresent()) {
                    optionalReport.get().addAll(optReport.get());
                }
            }
        } catch (Exception e) {
            storageService.markWorkItemFailed(Optional.ofNullable(toJson(e)), taskId, moduleId, userId, contextId);
            return keepGoing;
        }

        // Generate a unique processing identifier for current provider's export
        UUID processingId = UUID.randomUUID();

        // Trigger provider export
        Throwable failure = null;
        currentProviderInfo.set(new ProviderInfo(processingId, providerToUse, sink, fileStorage));
        try {
            // Initiate/continue data export
            ExportResult exportResult;
            try {
                LOGGER.info("{} \"{}\" work item for data export task {} of user {} in context {} with executor {}", optionalSavepoint.isPresent() ? "Resuming" : "Starting", moduleId, stringFor(taskId), I(userId), I(contextId), this);
                exportResult = providerToUse.export(processingId, sink, optionalSavepoint, task, locale);
            } catch (InterruptedException e) {
                // Export interrupted
                Thread.currentThread().interrupt();
                LOGGER.warn("Interrupted \"{}\" work item for data export task {} of user {} in context {} with executor {}", moduleId, stringFor(taskId), I(userId), I(contextId), this, e);
                exportResult = ExportResult.interrupted();
            } finally {
                LogProperties.removeLogProperties();
            }

            if (exportResult.isCompleted()) {
                // Export completed successfully
                LOGGER.info("Completed \"{}\" work item for data export task {} of user {} in context {} with executor {}", moduleId, stringFor(taskId), I(userId), I(contextId), this);

                // Await file storage location
                String fileStorageLocation = sink.finish().orElse(null);

                boolean err = true;
                try {
                    // Mark work item as done
                    if (sink.wasExportCalled() || optPrevFileStorageLocation != null) {
                        storageService.markWorkItemDone(fileStorageLocation, taskId, moduleId, userId, contextId);
                    } else {
                        // Sink has not been initialized. Consequently, nothing was written to it
                        if (fileStorageLocation != null) {
                            fileStorage.deleteFile(fileStorageLocation);
                        }
                        storageService.markWorkItemDone(null, taskId, moduleId, userId, contextId);
                        if (optionalReport.isPresent()) {
                            optionalReport.get().add(Message.builder().appendToMessage("Data export provider \"").appendToMessage(moduleId).appendToMessage("\" exported no data.").withModuleId(moduleId).withTimeStamp(new Date()).build());
                        }
                    }
                    err = false;
                } finally {
                    if (err) {
                        deleteQuietly(fileStorageLocation, fileStorage);
                    }
                }
            } else {
                keepGoing = false;
                if (exportResult.isInterrupted()) {
                    // Manually paused; necessary actions take place in "stop(boolean)" method
                    LOGGER.info("Interrupted \"{}\" work item for data export task {} of user {} in context {}", moduleId, stringFor(taskId), I(userId), I(contextId));
                } else if (exportResult.isIncomplete()) {
                    // Incomplete result (paused)
                    Optional<Exception> incompleteReason = exportResult.getIncompleteReason();
                    if (incompleteReason.isPresent()) {
                        if (LOGGER.isDebugEnabled()) {
                            Exception pauseReason = incompleteReason.get();
                            LOGGER.info("Pausing \"{}\" work item for data export task {} of user {} in context {} due to (temporary) exception", moduleId, stringFor(taskId), I(userId), I(contextId), pauseReason);
                        } else {
                            LOGGER.info("Pausing \"{}\" work item for data export task {} of user {} in context {} due to (temporary) exception ``{}\u00b4\u00b4", moduleId, stringFor(taskId), I(userId), I(contextId), incompleteReason.get().getMessage());
                        }
                    } else {
                        LOGGER.info("Pausing \"{}\" work item for data export task {} of user {} in context {}", moduleId, stringFor(taskId), I(userId), I(contextId));
                    }
                    DataExportSavepoint savePoint = DataExportSavepoint.builder().withSavepoint(exportResult.getSavePoint().orElse(null)).withReport(optionalReport.orElse(null)).build();
                    storageService.setSavePoint(taskId, moduleId, savePoint, userId, contextId);
                    storageService.markPaused(taskId, userId, contextId);
                    Optional<String> fileStorageLocation = sink.finish();
                    if (fileStorageLocation.isPresent()) {
                        boolean err = true;
                        try {
                            storageService.markWorkItemPaused(fileStorageLocation.get(), taskId, moduleId, userId, contextId);
                            err = false;
                        } finally {
                            if (err) {
                                deleteQuietly(fileStorageLocation.get(), fileStorage);
                            }
                        }
                    }
                } else if (exportResult.isAborted()) {
                    // Manually aborted
                    LOGGER.info("Aborted \"{}\" work item for data export task {} of user {} in context {}", moduleId, stringFor(taskId), I(userId), I(contextId));
                    throw new DataExportAbortedException("\"" + moduleId + "\" data export task " + stringFor(taskId) + " of user " + userId + " in context " + contextId + " aborted");
                } else {
                    throw new IllegalArgumentException("Unknown export value: " + exportResult.getValue());
                }
            }
        } catch (DataExportAbortedException e) {
            // Just re-throw
            throw e;
        } catch (Exception e) {
            failure = e;
            LOGGER.warn("\"{}\" work item for data export task {} of user {} in context {} failed", moduleId, stringFor(taskId), I(userId), I(contextId), failure);
        } catch (Error e) {
            failure = e;
            LOGGER.warn("\"{}\" work item for data export task {} of user {} in context {} failed fatally", moduleId, stringFor(taskId), I(userId), I(contextId), failure);
            // Re-throw error
            throw new AlreadyLoggedError(e);
        } catch (Throwable t) {
            failure = t;
            LOGGER.warn("\"{}\" work item for data export task {} of user {} in context {} failed", moduleId, stringFor(taskId), I(userId), I(contextId), failure);
        } finally {
            currentProviderInfo.set(null);
            if (failure != null) {
                // An error occurred
                sink.revoke();
                storageService.markWorkItemFailed(Optional.ofNullable(toJson(failure)), taskId, moduleId, userId, contextId);
            }
        }
        return keepGoing;
    }

    /**
     * name of the encoding UTF-8
     */
    private static final String UTF8 = "UTF8";

    private void generateResultFilesAndMarkAsDone(DataExportTask task, FileStorage fileStorage, ChunkedZippedOutputStream zipOut, Optional<DataExportDiagnosticsReport> optionalReport, Locale locale) throws AlreadyLoggedError, OXException {
        int userId = task.getUserId();
        int contextId = task.getContextId();
        UUID taskId = task.getId();

        boolean error = true;
        try {
            // Add diagnostics report
            if (optionalReport.isPresent() && !optionalReport.get().isEmpty()) {
                zipOut.addDiagnostics(optionalReport.get(), locale);
            }
            LOGGER.debug("Generated statistics result file for data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this);

            // Add work items' artifacts
            boolean anyResult = false;
            for (DataExportWorkItem wo : task.getWorkItems()) {
                String fileStorageLocation = wo.getFileStorageLocation();
                if (fileStorageLocation != null) {
                    InputStream in = null;
                    ZipArchiveInputStream zipIn = null;
                    try {
                        in = fileStorage.getFile(fileStorageLocation);
                        zipIn = new ZipArchiveInputStream(in, UTF8);
                        zipOut.addEntriesFrom(zipIn);
                        anyResult = true;
                        LOGGER.debug("Added \"{}\" result file for data export task {} of user {} in context {} with executor {}", wo.getModuleId(), stringFor(taskId), I(userId), I(contextId), this);
                    } catch (OXException e) {
                        if (!FileStorageCodes.FILE_NOT_FOUND.equals(e)) {
                            throw e;
                        }
                        // Ignore unavailable file
                    } finally {
                        Streams.close(zipIn, in);
                    }
                } else {
                    LOGGER.warn("Missing result file from \"{}\" for data export task {} of user {} in context {} with executor {}", wo.getModuleId(), stringFor(taskId), I(userId), I(contextId), this);
                }
            }
            if (anyResult) {
                LOGGER.debug("Completed result files for data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this);
            } else {
                List<String> moduleIds = task.getWorkItems().stream().map(DataExportWorkItem::getModuleId).collect(Collectors.toList());
                LOGGER.error("No result file available from module(s) \"{}\" for data export task {} of user {} in context {} with executor {}", moduleIds, stringFor(taskId), I(userId), I(contextId), this);

                OXException oxe = DataExportExceptionCode.NO_RESULT_FILE_GENERATED.create(Integer.valueOf(userId), Integer.valueOf(contextId));
                for (String moduleId : moduleIds) {
                    storageService.markWorkItemFailed(Optional.ofNullable(toJson(oxe)), taskId, moduleId, userId, contextId);
                }
                throw oxe;
            }

            // Mark task as done
            storageService.markDone(task.getId(), task.getUserId(), task.getContextId());
            LOGGER.debug("Data export task {} of user {} in context {} marked as DONE with executor {}", stringFor(taskId), I(userId), I(contextId), this);

            error = false;
        } catch (Error e) {
            LOGGER.warn("Data export task {} of user {} in context {} failed fatally with executor {}", stringFor(taskId), I(userId), I(contextId), this, e);
            // Re-throw error
            throw new AlreadyLoggedError(e);
        } catch (Throwable t) {
            LOGGER.warn("Failed generating result files for data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this, t);
            throw new AlreadyLoggedException(t);
        } finally {
            if (error) {
                zipOut.cleanUp();
            }
        }
    }

    /**
     * Generates the JSON representation for given failure.
     *
     * @param failure The failure to convert to JSON
     * @return The JSON representation
     */
    private static JSONObject toJson(Throwable failure) {
        try {
            OXException oxe = failure instanceof OXException ? ((OXException) failure) : DataExportExceptionCode.UNEXPECTED_ERROR.create(failure, failure.getMessage());
            JSONObject jException = new JSONObject(10);
            ResponseWriter.addException(jException, oxe);
            jException.remove(ResponseFields.ERROR_STACK);
            return jException;
        } catch (JSONException e) {
            // Conversion to JSON failed
            LOGGER.info("Failed to convert exception to JSON", e);
        }
        return null;
    }

    /**
     * Stops this execution.
     *
     * @param reason The reason for stopping this execution
     */
    public void stop(String reason) {
        stop(false, reason);
    }

    /**
     * Stops this execution.
     *
     * @param internallyInvoked Whether this method has been invoked internally or externally
     * @param reason The reason for stopping this execution
     */
    void stop(boolean internallyInvoked, String reason) {
        LOGGER.info("Stopping executor {}: {}", this, reason);
        // Acquire stop lock
        stopLock.lock();
        try {
            // Grab currently executed job (if any)
            JobAndReport currentJob = this.currentJob.get();
            if (currentJob != null) {
                DataExportTask task = currentJob.job.getDataExportTask();
                int userId = task.getUserId();
                int contextId = task.getContextId();
                UUID taskId = task.getId();
                LOGGER.info("Stopping data export task {} of user {} in context {} with executor {}", stringFor(taskId), I(userId), I(contextId), this);

                // Grab the provider that is currently exporting
                boolean paused = false;
                {
                    ProviderInfo providerInfo = this.currentProviderInfo.get();
                    if (providerInfo != null) {
                        // Try to pause provider
                        DataExportProvider provider = providerInfo.provider;
                        String moduleId = provider.getId();
                        DataExportSinkImpl sink = providerInfo.sink;
                        try {
                            PauseResult pauseResult = provider.pause(providerInfo.processingId, sink, task);
                            if (pauseResult.isPaused()) {
                                paused = true;
                                DataExportSavepoint savePoint = DataExportSavepoint.builder().withSavepoint(pauseResult.getSavePoint().orElse(null)).withReport(currentJob.optionalReport.orElse(null)).build();
                                storageService.setSavePoint(taskId, moduleId, savePoint, userId, contextId);
                                storageService.markPaused(taskId, userId, contextId);
                                Optional<String> fileStorageLocation = sink.finish();
                                if (fileStorageLocation.isPresent()) {
                                    boolean err = true;
                                    try {
                                        storageService.markWorkItemPaused(fileStorageLocation.get(), taskId, moduleId, userId, contextId);
                                        err = false;
                                    } finally {
                                        if (err) {
                                            deleteQuietly(fileStorageLocation.get(), providerInfo.fileStorage);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.info("Failed to pause provider \"{}\" for data export task {} of user {} in context {} with executor {}", moduleId, stringFor(taskId), I(userId), I(contextId), this, e);
                        }
                    }
                }

                if (paused) {
                    cancelTimerTasks(taskId, userId, contextId, !internallyInvoked);
                }
            }
        } finally {
            stopLock.unlock();
        }
    }

    /**
     * Initializes timer tasks for stopping and touching.
     *
     * @param taskId The task identifier
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    private void initTimerTasks(UUID taskId, int userId, int contextId) throws OXException {
        DataExportStorageService storageService = this.storageService;
        TimerService timerService = services.getServiceSafe(TimerService.class);
        Runnable toucher = () -> {
            try {
                Optional<DataExportStatus> optionalStatus = storageService.getDataExportStatus(userId, contextId);
                if (optionalStatus.isPresent() && !optionalStatus.get().isTerminated()) {
                    storageService.touch(userId, contextId);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to touch time stamp of data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
            }
        };
        long delayMillis = expirationTimeMillis >> 1;
        currentToucher.set(timerService.scheduleWithFixedDelay(toucher, delayMillis, delayMillis));

        // Max. processing time
        if (maxProcessingTimeMillis > 0) {
            // Create one-shot timer task for stopping
            Runnable stopper = () -> {
                try {
                    stop(true, "Max. processing time (" + maxProcessingTimeMillis + ") is exceeded");
                } catch (Exception e) {
                    LOGGER.warn("Failed to stop execution of data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
                }
            };
            this.currentStopper.set(timerService.schedule(stopper, maxProcessingTimeMillis));
        }
    }

    /**
     * Cancels timer tasks for stopping and touching.
     *
     * @param taskId The task identifier
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param cancelStopperTask Whether stopper task needs to be canceled
     */
    private void cancelTimerTasks(UUID taskId, int userId, int contextId, boolean cancelStopperTask) {
        // Drop stopper timer task (as we are obviously stopping here)
        boolean somethingStopped = false;
        if (cancelStopperTask) {
            ScheduledTimerTask stopperTask = currentStopper.getAndSet(null);
            if (stopperTask != null) {
                try {
                    stopperTask.cancel();
                    somethingStopped = true;
                } catch (Exception e) {
                    LOGGER.warn("Failed to cancel one-time stopper task for data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
                }
            }
        }

        // Drop toucher timer task
        ScheduledTimerTask timerTask = currentToucher.getAndSet(null);
        if (timerTask != null) {
            try {
                timerTask.cancel();
                somethingStopped = true;
            } catch (Exception e) {
                LOGGER.warn("Failed to cancel periodic toucher for data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
            }
        }

        if (somethingStopped) {
            purgeCanceledTimerTasks();
        }
    }

    private void purgeCanceledTimerTasks() {
        TimerService timerService = services.getOptionalService(TimerService.class);
        if (timerService != null) {
            timerService.purge();
        }
    }

    private Locale getUserLocale(int userId, int contextId) throws OXException {
        UserService userService = services.getServiceSafe(UserService.class);
        return userService.getUser(userId, contextId).getLocale();
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    private static class ProviderInfo {

        final DataExportProvider provider;
        final UUID processingId;
        final DataExportSinkImpl sink;
        final FileStorage fileStorage;

        ProviderInfo(UUID processingId, DataExportProvider provider, DataExportSinkImpl sink, FileStorage fileStorage) {
            super();
            this.processingId = processingId;
            this.provider = provider;
            this.sink = sink;
            this.fileStorage = fileStorage;
        }
    }

    private static class JobAndReport {

        final DataExportJob job;
        final Optional<DataExportDiagnosticsReport> optionalReport;

        JobAndReport(DataExportJob job, Optional<DataExportDiagnosticsReport> optionalReport) {
            super();
            this.job = job;
            this.optionalReport = optionalReport;
        }
    }

    private static class AlreadyLoggedError extends Error {

        private static final long serialVersionUID = 6838393800639076591L;

        final Error error;

        /**
         * Initializes a new {@link AlreadyLoggedError}.
         *
         * @param cause The causing error
         */
        AlreadyLoggedError(Error cause) {
            super();
            this.error = cause;
        }
    }

    private static class AlreadyLoggedException extends RuntimeException {

        private static final long serialVersionUID = 7438393800639076591L;

        /**
         * Initializes a new {@link AlreadyLoggedException}.
         *
         * @param reason The reason
         */
        AlreadyLoggedException(Throwable reason) {
            super(reason);
        }
    }

    private static class ProcessingThreadReference {

        private boolean started;
        private Thread currentThread;

        ProcessingThreadReference() {
            super();
            started = false;
        }

        synchronized boolean isPendingOrRunning() {
            return !started || currentThread != null;
        }

        synchronized void setCurrentThread(Thread currentThread) {
            if (currentThread == null) {
                return;
            }
            this.currentThread = currentThread;
            started = true;
        }

        synchronized void unsetCurrentThread() {
            this.currentThread = null;
        }
    }

}