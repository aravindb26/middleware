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

import static com.openexchange.gdpr.dataexport.impl.DataExportUtility.stringFor;
import static com.openexchange.groupware.upload.impl.UploadUtility.getSize;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.slf4j.Logger;
import com.google.common.collect.ImmutableList;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.gdpr.dataexport.DataExport;
import com.openexchange.gdpr.dataexport.DataExportArguments;
import com.openexchange.gdpr.dataexport.DataExportConfig;
import com.openexchange.gdpr.dataexport.DataExportConstants;
import com.openexchange.gdpr.dataexport.DataExportDownload;
import com.openexchange.gdpr.dataexport.DataExportExceptionCode;
import com.openexchange.gdpr.dataexport.DataExportJob;
import com.openexchange.gdpr.dataexport.DataExportProvider;
import com.openexchange.gdpr.dataexport.DataExportProviderRegistry;
import com.openexchange.gdpr.dataexport.DataExportResultFile;
import com.openexchange.gdpr.dataexport.DataExportService;
import com.openexchange.gdpr.dataexport.DataExportStatus;
import com.openexchange.gdpr.dataexport.DataExportStorageService;
import com.openexchange.gdpr.dataexport.DataExportTask;
import com.openexchange.gdpr.dataexport.DataExportTaskInfo;
import com.openexchange.gdpr.dataexport.DataExportWorkItem;
import com.openexchange.gdpr.dataexport.DefaultDataExport;
import com.openexchange.gdpr.dataexport.DefaultDataExportResultFile;
import com.openexchange.gdpr.dataexport.DiagnosticsReportOptions;
import com.openexchange.gdpr.dataexport.FileLocation;
import com.openexchange.gdpr.dataexport.FileLocations;
import com.openexchange.gdpr.dataexport.HostInfo;
import com.openexchange.gdpr.dataexport.Module;
import com.openexchange.gdpr.dataexport.impl.notification.DataExportNotificationSender;
import com.openexchange.gdpr.dataexport.impl.notification.Reason;
import com.openexchange.java.ExceptionCatchingRunnable;
import com.openexchange.java.ISO8601Utils;
import com.openexchange.java.MessageFormatter;
import com.openexchange.java.Strings;
import com.openexchange.schedule.TaskScheduler;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import com.openexchange.user.User;
import com.openexchange.user.UserService;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;


/**
 * {@link DataExportServiceImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class DataExportServiceImpl implements DataExportService {

    /** The logger */
    static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DataExportServiceImpl.class);

    private final DataExportStorageService storageService;
    private final DataExportConfig config;
    private final ServiceLookup services;
    private final ConcurrentMap<Future<Void>, DataExportTaskExecution> executions;
    private final DataExportProviderRegistry providerRegistry;

    private final TaskScheduler taskScheduler;
    private final ScheduledTimerTask checkAbortedTimerTask;


    /**
     * Initializes a new {@link DataExportServiceImpl}.
     *
     * @param config The data export configuration
     * @param storageService The storage service for data export
     * @param providerRegistry The provider registry for obtaining appropriate instances of <code>DataExportProvider</code>
     * @param services The service look-up
     * @throws OXException If initialization fails
     */
    public DataExportServiceImpl(DataExportConfig config, DataExportStorageService storageService, DataExportProviderRegistry providerRegistry, ServiceLookup services) throws OXException {
        super();
        this.config = config;
        this.storageService = storageService;
        this.providerRegistry = providerRegistry;
        this.services = services;
        ConcurrentMap<Future<Void>, DataExportTaskExecution> executions = new ConcurrentHashMap<>(config.getNumberOfConcurrentTasks(), 0.9F, 1);
        this.executions = executions;

        ExceptionCatchingRunnable periodicTask = this::startProcessingTasks;
        ExceptionCatchingRunnable stopTask = () -> stopProcessingTasks(false);
        taskScheduler = TaskScheduler.builder()
            .withFrequencyMillis(config.getCheckForTasksFrequency())
            .withPeriodicTask(periodicTask)
            .withRangesOfTheWeek(config.getRangesOfTheWeek())
            .withStopProcessingListener(stopTask)
            .withTimerServiceFrom(services)
            .build();

        TimerService timerService = services.getServiceSafe(TimerService.class);
        Runnable abortedChecker = new Runnable() {

            @Override
            public void run() {
                for (DataExportTaskExecution execution : executions.values()) {
                    Optional<DataExportTask> optionalTask = execution.getCurrentTask();
                    if (optionalTask.isPresent()) {
                        try {
                            DataExportTask task = optionalTask.get();
                            Optional<DataExportStatus> optionalStatus = storageService.getDataExportStatus(task.getUserId(), task.getContextId());
                            if (optionalStatus.isPresent() && optionalStatus.get().isAborted()) {
                                // Task has been aborted
                                execution.stop(MessageFormatter.format("Data export task {} of user {} in context {} has been marked as aborted", stringFor(task.getId()), I(task.getUserId()), I(task.getContextId())));
                            }
                        } catch (Exception e) {
                            LOG.warn("Failed to retrieve status for data export task.", e);
                        }
                    }
                }

                // Delete expired aborted tasks and completed tasks that exceed max. time-to-live.
                List<DataExportTaskInfo> withPendingNotification = null;
                try {
                    withPendingNotification = storageService.deleteCompletedOrAbortedTasksAndGetTasksWithPendingNotification();
                } catch (Exception e) {
                    LOG.warn("Failed to delete aborted or completed data export tasks", e);
                }

                // Check for tasks with pending notification
                if (withPendingNotification != null && !withPendingNotification.isEmpty()) {
                    for (DataExportTaskInfo taskInfo : withPendingNotification) {
                        Reason reason;
                        switch (taskInfo.getStatus()) {
                            case ABORTED:
                                reason = Reason.ABORTED;
                                break;
                            case DONE:
                                reason = Reason.SUCCESS;
                                break;
                            case FAILED:
                                reason = Reason.FAILED;
                                break;
                            default:
                                reason = null;
                                break;
                        }

                        if (reason != null) {
                            UUID taskId = taskInfo.getTaskId();
                            int userId = taskInfo.getUserId();
                            int contextId = taskInfo.getContextId();

                            HostInfo hostInfo = null;
                            Date expiryDate = null;
                            Date creationDate = null;
                            if (Reason.SUCCESS == reason) {
                                try {
                                    Optional<Date> lastAccessedTimeStamp = storageService.getLastAccessedTimeStamp(userId, contextId);
                                    expiryDate = new Date(lastAccessedTimeStamp.get().getTime() + config.getMaxTimeToLiveMillis());
                                } catch (Exception e) {
                                    expiryDate = new Date(System.currentTimeMillis() + config.getMaxTimeToLiveMillis());
                                    LOG.warn("Failed to query last-accessed time stamp from data export task {} of user {} in context {}. Assuming \"{}\" as expiration date.", stringFor(taskId), I(userId), I(contextId), ISO8601Utils.format(expiryDate), e);
                                }
                                try {
                                    Optional<DataExportTask> optionalTask = storageService.getDataExportTask(userId, contextId);
                                    if (optionalTask.isPresent()) {
                                        DataExportTask task = optionalTask.get();
                                        hostInfo = task.getArguments().getHostInfo();
                                        creationDate = task.getCreationTime();
                                    }
                                } catch (Exception e) {
                                    LOG.warn("Failed loading data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
                                }
                            }

                            try {
                                DataExportNotificationSender.sendNotificationAndSetMarker(reason, creationDate == null ? new Date() : creationDate, expiryDate, hostInfo, taskId, userId, contextId, true, services);
                            } catch (Exception e) {
                                LOG.warn("Cannot set notification-sent marker for data export task {} of user {} in context {}", stringFor(taskId), I(userId), I(contextId), e);
                            }
                        }
                    }
                }
            }
        };
        long delayMillis = config.getCheckForAbortedTasksFrequency();
        checkAbortedTimerTask = timerService.scheduleAtFixedRate(abortedChecker, delayMillis, delayMillis, TimeUnit.MILLISECONDS);

        LOG.info("Initiated data export service checking for tasks every {}msec utilizing:{}    {}", L(config.getCheckForTasksFrequency()), Strings.getLineSeparator(), config.getRangesOfTheWeek());
    }

    @Override
    public DataExportConfig getConfig() {
        return config;
    }

    @Override
    public List<DataExportTask> getRunningDataExportTasks() throws OXException {
        return storageService.getRunningDataExportTasks();
    }

    @Override
    public boolean hasRunningDataExportTasks() throws OXException {
        return storageService.hasRunningDataExportTasks();
    }

    @Override
    public List<DataExportTask> getDataExportTasks(boolean checkValidity) throws OXException {
        if (checkValidity == false) {
            return storageService.getDataExportTasks();
        }

        List<DataExportTask> dataExportTasks = storageService.getDataExportTasks();
        if (dataExportTasks.isEmpty()) {
            return dataExportTasks;
        }

        ContextService contextService = services.getOptionalService(ContextService.class);
        if (contextService != null) {
            UserService userService = services.getOptionalService(UserService.class);
            if (userService != null) {
                dataExportTasks = checkTasksValidity(dataExportTasks, userService, contextService);
            }
        }

        return dataExportTasks;
    }

    private List<DataExportTask> checkTasksValidity(List<DataExportTask> dataExportTasks, UserService userService, ContextService contextService) {
        List<DataExportTask> tasksToDelete = null;

        TIntObjectMap<Boolean> visitedContexts = new TIntObjectHashMap<>();
        for (Iterator<DataExportTask> it = dataExportTasks.iterator(); it.hasNext();) {
            DataExportTask dataExportTask = it.next();

            int contextId = dataExportTask.getContextId();
            Boolean exists = visitedContexts.get(contextId);
            if (exists == null) {
                exists = Boolean.valueOf(contextExists(contextId, contextService));
                visitedContexts.put(contextId, exists);
            }

            if (exists.booleanValue()) {
                if (userExists(dataExportTask.getUserId(), contextId, userService) == false) {
                    // Remove task
                    it.remove();
                    if (tasksToDelete == null) {
                        tasksToDelete = new ArrayList<>();
                    }
                    tasksToDelete.add(dataExportTask);
                }
            } else {
                // Remove task
                it.remove();
                if (tasksToDelete == null) {
                    tasksToDelete = new ArrayList<>();
                }
                tasksToDelete.add(dataExportTask);
            }
        }

        if (tasksToDelete != null) {
            List<DataExportTask> tasks = tasksToDelete;
            DataExportStorageService storageService = this.storageService;
            AbstractTask<Void> deleteTask = new AbstractTask<Void>() {

                @Override
                public Void call() throws Exception {
                    for (DataExportTask task : tasks) {
                        storageService.deleteDataExportTask(task.getId());
                        LOG.info("Deleted data export task {} of user {} in context {} since associated user does not exist.", stringFor(task.getId()), I(task.getUserId()), I(task.getContextId()));
                    }
                    return null;
                }
            };
            ThreadPools.submitElseExecute(deleteTask);
        }

        return dataExportTasks;
    }

    private boolean contextExists(int contextId, ContextService contextService) {
        try {
            return contextService.exists(contextId);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean userExists(int userId, int contextId, UserService userService) {
        try {
            return userService.exists(userId, contextId);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public Optional<UUID> submitDataExportTaskIfAbsent(DataExportArguments args, Session session) throws OXException {
        int userId = session.getUserId();
        int contextId = session.getContextId();

        if (args.getMaxFileSize() > 0 && args.getMaxFileSize() < DataExportConstants.MINIMUM_FILE_SIZE) {
            throw DataExportExceptionCode.INVALID_FILE_SIZE.create(getSize(DataExportConstants.MINIMUM_FILE_SIZE, 2, false, true));
        }

        List<Module> modulesToExport = args.getModules();
        if (modulesToExport == null) {
            throw DataExportExceptionCode.NO_MODULES_SPECIFIED.create();
        }

        int numberOfModulesToExport = modulesToExport.size();
        if (numberOfModulesToExport <= 0) {
            throw DataExportExceptionCode.NO_MODULES_SPECIFIED.create();
        }

        int fileStorageId;
        {
            ConfigViewFactory viewFactory = services.getServiceSafe(ConfigViewFactory.class);
            ConfigView view = viewFactory.getView(userId, contextId);

            fileStorageId = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.gdpr.dataexport.fileStorageId", -1, view);
            if (fileStorageId <= 0) {
                throw DataExportExceptionCode.NO_FILE_STORAGE_SPECIFIED.create();
            }

            // Check availability
            try {
                DataExportUtility.getFileStorageFor(fileStorageId, contextId);
            } catch (OXException e) {
                throw DataExportExceptionCode.FILE_STORAGE_INACCESSIBLE.create(e, I(fileStorageId), I(userId), I(contextId));
            }
        }

        List<DataExportWorkItem> workItems = new ArrayList<>(numberOfModulesToExport);
        for (Module module : modulesToExport) {
            String moduleId = module.getId();
            Optional<DataExportProvider> optionalProvider = providerRegistry.getHighestRankedProviderFor(moduleId);
            if (!optionalProvider.isPresent()) {
                throw DataExportExceptionCode.NO_SUCH_PROVIDER.create(moduleId);
            }

            boolean enabled = optionalProvider.get().checkArguments(args, session);
            if (enabled) {
                DataExportWorkItem workItem = new DataExportWorkItem();
                workItem.setId(UUID.randomUUID());
                workItem.setModuleId(moduleId);
                workItems.add(workItem);
            }
        }

        UUID taskId = UUID.randomUUID();

        DataExportTask task = new DataExportTask();
        task.setContextId(contextId);
        task.setUserId(userId);
        task.setFileStorageId(fileStorageId);
        task.setId(taskId);
        task.setWorkItems(workItems);
        task.setArguments(args);

        boolean keepgoing = true;
        while (keepgoing) {
            boolean created = storageService.createIfAbsent(task, userId, contextId);
            if (created) {
                LOG.info("Submitted data export task {} for user {} in context {}", stringFor(task.getId()), I(task.getUserId()), I(contextId));
                // startProcessingTasks(); // To immediately start...
                return Optional.of(taskId);
            }

            // Drop aborted ones
            keepgoing = false;
            try {
                Optional<DataExportTask> optExisting = storageService.getDataExportTask(userId, contextId);
                if (optExisting.isPresent()) {
                    DataExportTask existing = optExisting.get();
                    if (DataExportStatus.ABORTED == existing.getStatus()) {
                        boolean deleted = storageService.deleteDataExportTask(userId, contextId);
                        if (deleted) {
                            keepgoing = true;
                            LOG.info("Deleted aborted data export task {} (incl. all resources and artifacts) of user {} in context {} in order to submit a new task", stringFor(existing.getId()), I(task.getUserId()), I(contextId));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed checking for existent aborted data export task of user {} in context {}", I(task.getUserId()), I(contextId));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean cancelDataExportTask(int userId, int contextId) throws OXException {
        Optional<DataExportTask> optionalTask = storageService.getDataExportTask(userId, contextId);
        if (!optionalTask.isPresent()) {
            return false;
        }

        // Check status
        DataExportTask task = optionalTask.get();
        DataExportStatus status = task.getStatus();
        if (status.isDone() || status.isFailed()) {
            return false;
        }

        if (status.isAborted()) {
            // Already marked as aborted
            return true;
        }

        // Mark as aborted
        if (!storageService.markAborted(task.getId(), userId, contextId)) {
            return false;
        }

        // Check if this node is currently executing the task
        stopQuietly(task);
        LOG.info("Requested cancelation for data export task {} of user {} in context {}", stringFor(task.getId()), I(task.getUserId()), I(contextId));
        return true;
    }

    private void stopQuietly(DataExportTask task) {
        Optional<DataExportTask> optionalTask;
        try {
            Map<Future<Void>, DataExportTaskExecution> executions = this.executions;
            for (DataExportTaskExecution execution : executions.values()) {
                optionalTask = execution.getCurrentTask();
                if (optionalTask.isPresent() && equals(task, optionalTask.get())) {
                    execution.stop(MessageFormatter.format("Data export task {} of user {} in context {} has been requested for being canceled", stringFor(task.getId()), I(task.getUserId()), I(task.getContextId())));
                }
            }
        } catch (Exception e) {
            // Stop attempt failed
            if (task == null) {
                LOG.warn("Failed to locally stop task", e);
            } else {
                LOG.warn("Failed to locally stop task {} from user {} in context {}", stringFor(task.getId()), I(task.getUserId()), I(task.getContextId()), e);
            }
        }
    }

    @Override
    public List<UUID> cancelDataExportTasks(int contextId) throws OXException {
        List<DataExportTask> tasks = storageService.getDataExportTasks(contextId);
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> taskIds = new ArrayList<>(tasks.size());
        for (DataExportTask task : tasks) {
            DataExportStatus status = task.getStatus();
            if (!status.isDone() && !status.isFailed()) {
                if (status.isAborted()) {
                    // Already marked as aborted
                    taskIds.add(task.getId());
                } else {
                    // Mark as aborted
                    try {
                        if (storageService.markAborted(task.getId(), task.getUserId(), contextId)) {
                            stopQuietly(task);
                            taskIds.add(task.getId());
                            LOG.info("Requested cancelation for data export task {} of user {} in context {}", stringFor(task.getId()), I(task.getUserId()), I(contextId));
                        }
                    } catch (Exception e) {
                        LOG.warn("Failed to abort task {} from user {} in context {}", stringFor(task.getId()), I(task.getUserId()), I(contextId));
                    }
                }
            }
        }
        return taskIds;
    }

    @Override
    public boolean deleteDataExportTask(int userId, int contextId) throws OXException {
        Optional<DataExportTask> optionalTask = storageService.getDataExportTask(userId, contextId);
        if (!optionalTask.isPresent()) {
            return false;
        }

        // Check status
        DataExportTask task = optionalTask.get();
        DataExportStatus status = task.getStatus();
        if (!status.isDone() && !status.isFailed()) {
            return false;
        }

        boolean deleted = storageService.deleteDataExportTask(userId, contextId);
        if (deleted) {
            LOG.info("Deleted data export task {} (incl. all resources and artifacts) of user {} in context {}", stringFor(task.getId()), I(task.getUserId()), I(contextId));
        }
        return deleted;
    }

    @Override
    public Optional<DataExportTask> getDataExportTask(int userId, int contextId) throws OXException {
        return storageService.getDataExportTask(userId, contextId);
    }

    @Override
    public List<DataExportTask> getDataExportTasks(int contextId) throws OXException {
        return storageService.getDataExportTasks(contextId);
    }

    @Override
    public Optional<DataExport> getDataExport(Session session) throws OXException {
        int userId = session.getUserId();
        int contextId = session.getContextId();

        Optional<DataExportTask> optionalTask = storageService.getDataExportTask(userId, contextId);
        if (!optionalTask.isPresent()) {
            return Optional.empty();
        }

        // Check task's status
        DataExportTask task = optionalTask.get();
        checkAndRepairStatus(task);
        DataExportStatus status = task.getStatus();
        if (!status.isDone()) {
            return Optional.of(DefaultDataExport.builder().withTask(task).build());
        }

        // Task is done
        Optional<FileLocations> optionalLocations = storageService.getDataExportResultFiles(userId, contextId);
        if (!optionalLocations.isPresent()) {
            return Optional.of(DefaultDataExport.builder().withTask(task).build());
        }

        // Get locations
        FileLocations fileLocations = optionalLocations.get();
        List<FileLocation> locations = fileLocations.getLocations();

        // User
        UserService userService = services.getServiceSafe(UserService.class);
        User user = userService.getUser(userId, contextId);

        int total = locations.size();
        ImmutableList.Builder<DataExportResultFile> files = ImmutableList.builderWithExpectedSize(total);
        for (FileLocation fileLocation : locations) {
            DefaultDataExportResultFile resultFile = DefaultDataExportResultFile.builder()
                .withContentType(DataExportUtility.CONTENT_TYPE)
                .withFileName(DataExportUtility.generateFileNameFor("archive", ".zip", fileLocation.getNumber(), total, task.getCreationTime(), user))
                .withNumber(fileLocation.getNumber())
                .withTaskId(task.getId())
                .build();
            files.add(resultFile);
        }

        return Optional.of(DefaultDataExport.builder().withTask(task).withResultFiles(files.build()).withAvailableUntil(new Date(fileLocations.getLastAccessed() + config.getMaxTimeToLiveMillis())).build());
    }

    @Override
    public DataExportDownload getDataExportDownload(int number, int userId, int contextId) throws OXException {
        Optional<DataExportTask> optionalTask = storageService.getDataExportTask(userId, contextId);
        if (!optionalTask.isPresent()) {
            throw DataExportExceptionCode.NO_SUCH_TASK.create(I(userId), I(contextId));
        }

        // Check task's status
        DataExportTask task = optionalTask.get();
        DataExportStatus status = task.getStatus();
        if (!status.isDone()) {
            if (status.isFailed()) {
                throw DataExportExceptionCode.TASK_FAILED.create(I(userId), I(contextId));
            }
            if (status.isAborted()) {
                throw DataExportExceptionCode.TASK_ABORTED.create(I(userId), I(contextId));
            }
            throw DataExportExceptionCode.TASK_NOT_COMPLETED.create(I(userId), I(contextId));
        }

        // Task is done
        Optional<FileLocations> optionalResultFiles = storageService.getDataExportResultFiles(userId, contextId);
        if (!optionalResultFiles.isPresent()) {
            throw DataExportExceptionCode.TASK_NOT_COMPLETED.create(I(userId), I(contextId));
        }

        List<FileLocation> locations = optionalResultFiles.get().getLocations();
        FileLocation fileLocation = null;
        for (Iterator<FileLocation> it = locations.iterator(); fileLocation == null && it.hasNext(); ) {
            FileLocation loc = it.next();
            if (loc.getNumber() == number) {
                fileLocation = loc;
            }
        }

        if (fileLocation == null) {
            throw DataExportExceptionCode.NO_SUCH_RESULT_FILE.create(I(number), I(userId), I(contextId));
        }

        // User
        User user = services.getServiceSafe(UserService.class).getUser(userId, contextId);

        // File name
        String fileName = DataExportUtility.generateFileNameFor("archive", ".zip", number, locations.size(), task.getCreationTime(), user);

        return new FileStorageDataExportDownload(fileLocation, task, fileName);
    }

    @Override
    public List<Module> getAvailableModules(Session session) throws OXException {
        return providerRegistry.getAvailableModules(session);
    }

    @Override
    public boolean removeDataExport(int userId, int contextId) throws OXException {
        Optional<DataExportTask> optionalTask = storageService.getDataExportTask(userId, contextId);
        if (!optionalTask.isPresent()) {
            return false;
        }

        // Check status
        DataExportTask task = optionalTask.get();
        DataExportStatus status = task.getStatus();
        if (!status.isDone() && !status.isFailed()) {
            // Still in progress...
            return false;
        }

        // Delete it...
        storageService.deleteDataExportTask(userId, contextId);
        return true;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when service has been started.
     *
     * @throws OXException If trigger fails
     */
    public void onStartUp() throws OXException {
        planSchedule();
    }

    /**
     * Invoked when service has been stopped.
     */
    public void onStopped() {
        ScheduledTimerTask checkAbortedTimerTask = this.checkAbortedTimerTask;
        if (null != checkAbortedTimerTask) {
            checkAbortedTimerTask.cancel(false);
        }

        taskScheduler.stop();
        stopProcessingTasks(true);
    }

    @Override
    public void planSchedule() throws OXException {
        if (!config.isActive()) {
            // Not enabled on this node
            LOG.debug("Denied scheduling data export tasks on this node since deactivated per configuration");
            return;
        }

        try {
            taskScheduler.start();
            LOG.debug("Scheduled execution of data export tasks on this node");
        } catch (RuntimeException e) {
            throw DataExportExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    synchronized void startProcessingTasks() throws OXException {
        LOG.info("Checking for data export tasks to start");
        try {
            // Grab services
            ThreadPoolService threadPool = services.getServiceSafe(ThreadPoolService.class);
            DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);

            DataExportLock lock = DataExportLock.getInstance();
            DataExportLock.LockAcquisition acquisition = lock.acquireCleanUpTaskLock(false, databaseService);
            try {
                if (acquisition.isNotAcquired()) {
                    // Failed to acquire clean-up lock
                    LOG.info("Failed to acquire clean-up lock for data export files. Skipping job execution run...");
                    return;
                }

                // Obtain and check executions mapping
                LOG.info("Acquired clean-up lock for data export files. Initiating job execution run...");
                final Map<Future<Void>, DataExportTaskExecution> executions = this.executions;
                for (Iterator<Entry<Future<Void>, DataExportTaskExecution>> it = executions.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<Future<Void>, DataExportTaskExecution> executionEntry = it.next();
                    DataExportTaskExecution execution = executionEntry.getValue();
                    if (execution.isInvalid()) {
                        it.remove();
                        execution.stop("Processing thread no more alive");
                        executionEntry.getKey().cancel(true);

                        Optional<DataExportTask> optTask = executionEntry.getValue().getCurrentTask();
                        if (optTask.isPresent()) {
                            DataExportTask task = optTask.get();
                            LOG.debug("Canceled invalid job execution for data export task {} of user {} in context {}", stringFor(task.getId()), I(task.getUserId()), I(task.getContextId()));
                        }
                    } else {
                        Optional<DataExportTask> optTask = executionEntry.getValue().getCurrentTask();
                        if (optTask.isPresent()) {
                            DataExportTask task = optTask.get();
                            LOG.debug("Found running job execution for data export task {} of user {} in context {}", stringFor(task.getId()), I(task.getUserId()), I(task.getContextId()));
                        }
                    }
                }

                // Check if a new execution may be spawned
                if (executions.size() >= config.getNumberOfConcurrentTasks()) {
                    if (executions.size() == 1) {
                        LOG.debug("There is already a data export tasks in execution. Leaving since there may not be more than {} in concurrent execution", I(executions.size()), I(config.getNumberOfConcurrentTasks()));
                    } else {
                        LOG.debug("There are already {} data export tasks in execution. Leaving since there may not be more than {} in concurrent execution", I(executions.size()), I(config.getNumberOfConcurrentTasks()));
                    }
                    return;
                }

                // Start new executions as needed
                int count = 1;
                do {
                    Optional<DataExportJob> dataExportJob = storageService.getNextDataExportJob();
                    if (!dataExportJob.isPresent()) {
                        // No pending/paused/expired tasks available
                        LOG.debug("Currently there are no data export tasks to execute");
                        return;
                    }

                    // Get job
                    DataExportJob job = dataExportJob.get();
                    LOG.debug("Got next data export task {} of user {} in context {} for being executed", stringFor(job.getDataExportTask().getId()), I(job.getDataExportTask().getUserId()), I(job.getDataExportTask().getContextId()));

                    // Artificial delay
                    long nanosToWait = TimeUnit.NANOSECONDS.convert((count++ * 1000) + ((long) (Math.random() * 1000)), TimeUnit.MILLISECONDS);
                    LockSupport.parkNanos(nanosToWait);

                    // Diagnostics report
                    DiagnosticsReportOptions reportOptions = DiagnosticsReportOptions.builder()
                        .withAddDiagnosticsReport(isAddDiagnosticsReport(job))
                        .withConsiderPermissionDeniedErrors(isConsiderPermissionDeniedErrors(job))
                        .build();

                    // Some variables for clean-up
                    DataExportTaskExecution execution = null;
                    Future<Void> future = null;
                    boolean openedForProcessing = false;
                    try {
                        // Initialize execution & submit it to thread pool
                        execution = new DataExportTaskExecution(job, reportOptions, config, storageService, providerRegistry, services);
                        future = threadPool.submit(execution);
                        LOG.debug("Submitted execution for data export task {} of user {} in context {} with executor {}", stringFor(job.getDataExportTask().getId()), I(job.getDataExportTask().getUserId()), I(job.getDataExportTask().getContextId()), execution);

                        // Store execution in map. Then open it for being processed while registering a clean-up task that ensures execution is removed from map when finished
                        executions.put(future, execution);
                        execution.allowProcessing(new AllowProcessingRunnable(future, executions));
                        openedForProcessing = true;
                    } finally {
                        if (!openedForProcessing) {
                            if (execution != null) {
                                execution.stop("Failed to start execution");
                            }

                            if (future != null) {
                                executions.remove(future);
                                future.cancel(true);
                            }
                        }
                    }
                } while (executions.size() < config.getNumberOfConcurrentTasks());
            } finally {
                try {
                    lock.releaseCleanUpTaskLock(acquisition, databaseService);
                } catch (Exception e) {
                    LOG.warn("Failed to release clean-up lock for data export files", e);
                }
            }
        } catch (OXException e) { // NOSONARLINT
            LOG.warn("Exception while starting data export tasks", e);
            throw e;
        } catch (RuntimeException e) { // NOSONARLINT
            LOG.warn("Exception while starting data export tasks", e);
            throw DataExportExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private boolean isAddDiagnosticsReport(DataExportJob job) throws OXException {
        boolean addDiagnosticsReport = false;
        ConfigViewFactory viewFactory = services.getOptionalService(ConfigViewFactory.class);
        if (viewFactory != null) {
            DataExportTask task = job.getDataExportTask();
            ConfigView view = viewFactory.getView(task.getUserId(), job.getDataExportTask().getContextId());
            addDiagnosticsReport = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.gdpr.dataexport.addDiagnosticsReport", addDiagnosticsReport, view);
        }
        return addDiagnosticsReport;
    }

    private boolean isConsiderPermissionDeniedErrors(DataExportJob job) throws OXException {
        boolean considerPermissionDeniedErrors = false;
        ConfigViewFactory viewFactory = services.getOptionalService(ConfigViewFactory.class);
        if (viewFactory != null) {
            DataExportTask task = job.getDataExportTask();
            ConfigView view = viewFactory.getView(task.getUserId(), job.getDataExportTask().getContextId());
            considerPermissionDeniedErrors = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.gdpr.dataexport.considerPermissionDeniedErrors", considerPermissionDeniedErrors, view);
        }
        return considerPermissionDeniedErrors;
    }

    /**
     * Checks if the task's status is done only if the status of all work items are also done.<br>
     * Changes the task's status if some work items are still in a not done state while the task's status is set to done.
     *
     * @param task The task to check
     */
    private static void checkAndRepairStatus(DataExportTask task) {
        if (task == null) {
            return;
        }
        if (task.getStatus() != DataExportStatus.DONE) {
            return;
        }
        if (task.getWorkItems() == null || task.getWorkItems().isEmpty()) {
            return;
        }
        for (DataExportWorkItem item : task.getWorkItems()) {
            if (item.getStatus() != DataExportStatus.DONE) {
                task.setStatus(item.getStatus());
                break;
            }
        }
    }

    synchronized void stopProcessingTasks(boolean onShutDown) {
        LOG.info("Stopping to process data export tasks due to {}", onShutDown ? "shutdown" : "end of schedule");
        try {
            if (onShutDown || config.isAllowPausingRunningTasks()) {
                for (Map.Entry<Future<Void>, DataExportTaskExecution> executionEntry : executions.entrySet()) {
                    executionEntry.getValue().stop("Processing window (schedule) has passed");
                    executionEntry.getKey().cancel(true);
                }
            }
        } catch (Exception e) {
            LOG.warn("Exception while stopping data export tasks", e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static boolean equals(DataExportTask task1, DataExportTask task2) {
        if (task1 == null) {
            return task2 == null;
        }
        if (task2 == null) {
            return false;
        }
        if (task1.getContextId() != task2.getContextId()) {
            return false;
        }
        if (task1.getUserId() != task2.getUserId()) {
            return false;
        }
        if (!task1.getId().equals(task2.getId())) {
            return false;
        }
        return true;
    }

    private static final class AllowProcessingRunnable implements Runnable {

        private final Future<Void> futureToRemove;
        private final Map<Future<Void>, DataExportTaskExecution> executions;

        AllowProcessingRunnable(Future<Void> futureToRemove, Map<Future<Void>, DataExportTaskExecution> executions) {
            super();
            this.futureToRemove = futureToRemove;
            this.executions = executions;
        }

        @Override
        public void run() {
            executions.remove(futureToRemove);
        }
    }

}
