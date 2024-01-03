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

package com.openexchange.chronos.alarm.message.impl;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmAction;
import com.openexchange.chronos.AlarmTrigger;
import com.openexchange.chronos.DefaultAlarmTrigger;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.LockedAlarmTrigger;
import com.openexchange.chronos.alarm.message.AlarmNotificationService;
import com.openexchange.chronos.provider.CalendarProviderRegistry;
import com.openexchange.chronos.provider.account.AdministrativeCalendarAccountService;
import com.openexchange.chronos.service.CalendarUtilities;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.storage.AdministrativeAlarmTriggerStorage;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.chronos.storage.CalendarStorageFactory;
import com.openexchange.chronos.storage.ContextAndAccountId;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.database.cleanup.CleanUpExecution;
import com.openexchange.database.cleanup.CleanUpExecutionConnectionProvider;
import com.openexchange.database.cleanup.DatabaseCleanUpExceptionCode;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.database.provider.SimpleDBProvider;
import com.openexchange.exception.OXException;
import com.openexchange.ratelimit.RateLimiterFactory;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * The {@link MessageAlarmDeliveryWorker} checks if there are any message alarm triggers (e.g. email and sms) which needed to be executed within the next timeframe ({@link #lookAhead}).
 * It then marks those triggers as processed and schedules a {@link SingleMessageDeliveryTask} at the appropriate time (shifted forward by the {@link #shifts} value)
 * for each of them.
 *
 * It also picks up old triggers, which are already marked as processed by other threats, if they are overdue ({@link #overdueWaitTime}).
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.1
 */
public class MessageAlarmDeliveryWorker implements CleanUpExecution {

    protected static final Logger LOG = LoggerFactory.getLogger(MessageAlarmDeliveryWorker.class);
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final AdministrativeAlarmTriggerStorage storage;
    private final DatabaseService dbservice;
    private final ContextService ctxService;
    private final TimerService timerService;

    private final Map<Key, ScheduledTimerTask> scheduledTasks = new ConcurrentHashMap<>();
    private final CalendarStorageFactory factory;
    private final CalendarUtilities calUtil;

    private final AlarmNotificationServiceRegistry registry;
    private final CalendarProviderRegistry calendarProviderRegistry;
    private final AdministrativeCalendarAccountService administrativeCalendarAccountService;
    private final RecurrenceService recurrenceService;
    private final RateLimiterFactory rateLimitFactory;
    private final int lookAhead;
    private final int overdueWaitTime;

    /**
     * Initializes a new {@link MessageAlarmDeliveryWorker}.
     *
     * @param services The {@link ServiceLookup} to get various services from
     * @param registry The {@link AlarmNotificationServiceRegistry} used to send the notification
     * @param lookAhead The time value in minutes the worker is looking ahead.
     * @param overdueWaitTime The time in minutes to wait until an old trigger is picked up.
     * @throws OXException In case a service is missing
     */
    public MessageAlarmDeliveryWorker(ServiceLookup services, AlarmNotificationServiceRegistry registry, int lookAhead, int overdueWaitTime) throws OXException {
        this.storage = services.getServiceSafe(AdministrativeAlarmTriggerStorage.class);
        this.dbservice = services.getServiceSafe(DatabaseService.class);
        this.ctxService = services.getServiceSafe(ContextService.class);
        this.timerService = services.getServiceSafe(TimerService.class);
        this.factory = services.getServiceSafe(CalendarStorageFactory.class);
        this.calUtil = services.getServiceSafe(CalendarUtilities.class);
        this.calendarProviderRegistry = services.getServiceSafe(CalendarProviderRegistry.class);
        this.administrativeCalendarAccountService = services.getServiceSafe(AdministrativeCalendarAccountService.class);
        this.rateLimitFactory = services.getServiceSafe(RateLimiterFactory.class);
        this.recurrenceService = services.getServiceSafe(RecurrenceService.class);
        this.registry = registry;
        this.lookAhead = lookAhead;
        this.overdueWaitTime = overdueWaitTime;
    }

    @Override
    public boolean prepareCleanUp(Map<String, Object> state) throws OXException {
        LOG.info("Started alarm delivery worker run...");
        return true;
    }

    @Override
    public void finishCleanUp(Map<String, Object> state) throws OXException {
        LOG.info("Alarm delivery worker run finished!");
    }

    @Override
    public boolean isApplicableFor(String schema, int representativeContextId, int databasePoolId, Map<String, Object> state, CleanUpExecutionConnectionProvider connectionProvider) throws OXException {
        try {
            Boolean result = Databases.executeQuery(connectionProvider.getConnection(),
                rs -> Boolean.TRUE, // We have a result, so we are fine
                "SELECT 1 FROM updateTask WHERE taskName=?",
                s -> s.setString(1, MessageAlarmDeliveryWorkerUpdateTask.TASK_NAME));
            return result != null && result.booleanValue();
        } catch (SQLException e) {
            throw DatabaseCleanUpExceptionCode.SQL_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void executeFor(String schema, int representativeContextId, int databasePoolId, Map<String, Object> state, CleanUpExecutionConnectionProvider connectionProvider) throws OXException {
        Map<ContextAndAccountId, List<LockedAlarmTrigger>> cleanUp = null;
        Calendar until = Calendar.getInstance(UTC);
        until.add(Calendar.MINUTE, lookAhead);
        try {
            Connection connection = connectionProvider.getConnection();
            Calendar overdueTime = Calendar.getInstance(UTC);
            overdueTime.add(Calendar.MINUTE, -Math.abs(overdueWaitTime));

            Map<ContextAndAccountId, List<LockedAlarmTrigger>> lockedTriggers = storage.getAndLockTriggers(connection, until.getTime(), overdueTime.getTime(), false, registry.getActions());
            if (lockedTriggers.isEmpty()) {
                return;
            }

            lockedTriggers = storage.getAndLockTriggers(connection, until.getTime(), overdueTime.getTime(), true, registry.getActions());
            if (lockedTriggers.isEmpty()) {
                return;
            }

            cleanUp = lockedTriggers;
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            spawnDeliveryTaskForTriggers(connection, lockedTriggers);

            cleanUp = null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted alarm delivery worker run for schema " + schema, e);
        } catch (Exception e) {
            // Nothing that can be done here. Just retry it with the next run
            LOG.error("Failed alarm delivery worker run for schema " + schema, e);
        } finally {
            if (cleanUp != null) {
                for (List<LockedAlarmTrigger> lockedTriggers : cleanUp.values()) {
                    for (LockedAlarmTrigger lockedAlarmTrigger : lockedTriggers) {
                        lockedAlarmTrigger.unlock();
                    }
                }
            }
        }
    }

    /**
     * Spawns an delivery worker for the given triggers
     *
     * @param connection The connection to use
     * @param lockedTriggers The triggers to spawn a delivery task for
     * @throws OXException
     */
    private void spawnDeliveryTaskForTriggers(Connection connection, Map<ContextAndAccountId, List<LockedAlarmTrigger>> lockedTriggers) throws OXException {
        for (Map.Entry<ContextAndAccountId, List<LockedAlarmTrigger>> entry : lockedTriggers.entrySet()) {
            int cid = entry.getKey().getContextId();
            int account = entry.getKey().getAccountId();
            CalendarStorage calendarStorage = factory.create(ctxService.getContext(cid), account, optEntityResolver(cid), new SimpleDBProvider(connection, connection), DBTransactionPolicy.NO_TRANSACTIONS);
            for (LockedAlarmTrigger lockedTrigger : entry.getValue()) {
                LockedAlarmTrigger cleanUp = lockedTrigger;
                try {
                    Alarm alarm = calendarStorage.getAlarmStorage().loadAlarm(lockedTrigger.getAlarm().intValue());
                    Calendar calTriggerTime = Calendar.getInstance(UTC);
                    calTriggerTime.setTimeInMillis(lockedTrigger.getTime().longValue());
                    Calendar now = Calendar.getInstance(UTC);
                    AlarmNotificationService alarmNotificationService = registry.getService(alarm.getAction());
                    if (alarmNotificationService == null) {
                        LOG.error("Missing required AlarmNotificationService for alarm action \"{}\"", alarm.getAction().getValue());
                        throw ServiceExceptionCode.absentService(AlarmNotificationService.class);
                    }

                    Integer shift = I(alarmNotificationService.getShift());
                    long delay = (calTriggerTime.getTimeInMillis() - now.getTimeInMillis()) - (shift == null ? 0 : shift.intValue());
                    if (delay < 0) {
                        delay = 0;
                    }

                    SingleMessageDeliveryTask task = createTask(cid, account, alarm, lockedTrigger, alarmNotificationService);
                    ScheduledTimerTask timer = timerService.schedule(task, delay, TimeUnit.MILLISECONDS);
                    cleanUp = null;
                    Key key = key(cid, account, lockedTrigger.getEventId(), alarm.getId());
                    scheduledTasks.put(key, timer);
                    LOG.trace("Created a new alarm task for {}", key);
                } catch (UnsupportedOperationException e) {
                    LOG.error("Can't handle message alarms as long as the legacy storage is used.", e);
                } finally {
                    if (cleanUp != null) {
                        cleanUp.unlock();
                    }
                }
            }
        }
    }

    /**
     * Creates a new {@link SingleMessageDeliveryTask}
     *
     * @param cid The context id
     * @param account The account id
     * @param alarm The {@link Alarm}
     * @param trigger The alarm trigger
     * @param alarmNotificationService The {@link AlarmNotificationService}
     * @return The task
     * @throws OXException If the context couldn't be loaded or if no {@link AlarmNotificationService} is registered for the {@link AlarmAction} of the alarm
     */
    private SingleMessageDeliveryTask createTask(int cid, int account, Alarm alarm, LockedAlarmTrigger trigger, AlarmNotificationService alarmNotificationService) throws OXException {
        return new SingleMessageDeliveryTask.Builder() //@formatter:off
                                         .setDbservice(dbservice)
                                         .setStorage(storage)
                                         .setAlarmNotificationService(alarmNotificationService)
                                         .setFactory(factory)
                                         .setCalUtil(calUtil)
                                         .setCalendarProviderRegistry(calendarProviderRegistry)
                                         .setAdministrativeCalendarAccountService(administrativeCalendarAccountService)
                                         .setRecurrenceService(recurrenceService)
                                         .setCtx(ctxService.getContext(cid))
                                         .setAccount(account)
                                         .setAlarm(alarm)
                                         .setTrigger(trigger)
                                         .setCallback(this)
                                         .setRateLimitFactory(rateLimitFactory)
                                         .build(); //@formatter:on
    }

    /**
     * Creates a {@link Key}
     *
     * @param cid The context identifier
     * @param account The account identifier
     * @param eventId The event identifier
     * @param alarm The alarm identifier
     * @return the {@link Key}
     */
    Key key(int cid, int account, String eventId, int alarm) {
        return new Key(cid, account, eventId, alarm);
    }

    /**
     * Cancels all tasks for the given event identifier.
     *
     * @param cid The context id
     * @param accountId The account id
     * @param eventId The event id to cancel tasks for. E.g. because the event is deleted.
     */
    public void cancelAll(int cid, int accountId, String eventId) {
        Iterator<Entry<Key, ScheduledTimerTask>> iterator = scheduledTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Key, ScheduledTimerTask> entry = iterator.next();
            Key key = entry.getKey();
            if (key.getCid() == cid && key.getAccount() == accountId && key.getEventId().equals(eventId)) {
                LOG.trace("Canceled message alarm task for {}", key);
                entry.getValue().cancel();
                iterator.remove();
            }
        }
    }

    /**
     * Cancels all tasks for the given event identifiers.
     *
     * @param cid The context id
     * @param accountId The account id
     * @param eventIds The event ids to cancel tasks for. E.g. because those events are deleted.
     */
    public void cancelAll(int cid, int accountId, Collection<String> eventIds) {
        for (String eventId : eventIds) {
            cancelAll(cid, accountId, eventId);
        }
    }

    /**
     * Checks if the given events contain alarm trigger which must be triggered before the next run of the {@link MessageAlarmDeliveryWorker} and
     * schedules a task for each trigger.
     *
     * @param events A list of updated and newly created events
     * @param cid The context id
     * @param account The account id
     */
    public void checkAndScheduleTasksForEvents(List<Event> events, int cid, int account) {
        List<LockedAlarmTrigger> cleanUp = null;
        Connection readCon = null;
        Connection writeCon = null;
        try {
            readCon = dbservice.getReadOnly(cid);
            boolean successful = false;
            boolean readOnly = true;
            try {
                List<LockedAlarmTrigger> triggers = checkEvents(readCon, events, cid, account, false);
                if (triggers.isEmpty() == false) {
                    // If there are due alarm triggers get a writable connection and lock those triggers
                    dbservice.backReadOnly(cid, readCon);
                    readCon = null;
                    writeCon = dbservice.getWritable(cid);
                    triggers = checkEvents(writeCon, events, cid, account, true);
                    if (triggers.isEmpty() == false) {
                        cleanUp = triggers;
                        readOnly = false;
                        CalendarStorage calStorage = factory.create(ctxService.getContext(cid), account, optEntityResolver(cid), new SimpleDBProvider(writeCon, writeCon), DBTransactionPolicy.NO_TRANSACTIONS);
                        for (LockedAlarmTrigger trigger : triggers) {
                            scheduleTaskForEvent(calStorage, key(cid, account, trigger.getEventId(), trigger.getAlarm().intValue()), trigger);
                        }
                        cleanUp = null;
                    }
                    successful = true;
                }
            } finally {
                if (readCon != null) {
                    dbservice.backReadOnly(cid, readCon);
                }
                if (writeCon != null) {
                    if (successful == false) {
                        Databases.rollback(writeCon);
                    }
                    Databases.autocommit(writeCon);
                    if (readOnly) {
                        dbservice.backWritableAfterReading(cid, writeCon);
                    } else {
                        dbservice.backWritable(cid, writeCon);
                    }
                }
                if (cleanUp != null) {
                    for (LockedAlarmTrigger lockedAlarmTrigger : cleanUp) {
                        lockedAlarmTrigger.unlock();
                    }
                }
            }
        } catch (OXException e) {
            LOG.error("Error while trying to handle event: {}", e.getMessage(), e);
            // Can be ignored. Triggers are picked up with the next run of the MessageAlarmDeliveryWorker
        }
    }

    /**
     * Checks the given events for message alarms which need to be triggered soon
     *
     * @param con The connection to use
     * @param events The events to check
     * @param cid The id of the context the events belong to
     * @param account The id of the account the events belong to
     * @param isWriteCon The whether the given connection is a write connection or not
     * @return A list of AlarmTriggers which needs to be scheduled
     * @throws OXException
     */
    List<LockedAlarmTrigger> checkEvents(Connection con, List<Event> events, int cid, int account, boolean isWriteCon) throws OXException {
        Calendar cal = Calendar.getInstance(UTC);
        cal.add(Calendar.MINUTE, lookAhead);

        List<LockedAlarmTrigger> result = null;
        for (Event event : events) {
            Map<ContextAndAccountId, List<LockedAlarmTrigger>> triggerMap = storage.getMessageAlarmTriggers(con, cid, account, event.getId(), isWriteCon, registry.getActions());
            // Schedule a task for all triggers before the next usual interval
            for (Map.Entry<ContextAndAccountId, List<LockedAlarmTrigger>> entry : triggerMap.entrySet()) {
                for (LockedAlarmTrigger lockedTrigger : entry.getValue()) {
                    Key key = key(cid, account, event.getId(), lockedTrigger.getAlarm().intValue());
                    if (lockedTrigger.getTime().longValue() > cal.getTimeInMillis()) {
                        cancelTask(key);
                        continue;
                    }
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    result.add(lockedTrigger);
                }
            }
        }
        return result == null ? Collections.emptyList() : result;
    }

    /**
     * Schedules a task for the given alarm trigger
     *
     * @param storage The {@link CalendarStorage} to use
     * @param key The key
     * @param trigger The alarm trigger
     * @throws OXException
     */
    void scheduleTaskForEvent(CalendarStorage storage, Key key, LockedAlarmTrigger trigger) throws OXException {
        try {
            Alarm alarm = storage.getAlarmStorage().loadAlarm(trigger.getAlarm().intValue());
            scheduleTask(key, alarm, trigger);
        } catch (UnsupportedOperationException e) {
            LOG.error("Can't handle message alarms as long as the legacy storage is used.");
        }
    }

    /**
     * Schedules a {@link SingleMessageDeliveryTask} for the given {@link AlarmTriggerWrapper}
     *
     * @param key The {@link Key} to the {@link SingleMessageDeliveryTask}
     * @param alarm The {@link Alarm} of the {@link AlarmTriggerWrapper}
     * @param trigger The {@link AlarmTriggerWrapper}
     */
    private void scheduleTask(Key key, Alarm alarm, LockedAlarmTrigger trigger) {
        LockedAlarmTrigger cleanUp = trigger;
        cancelTask(key);
        try {
            Calendar calTriggerTime = Calendar.getInstance(UTC);
            calTriggerTime.setTimeInMillis(trigger.getTime().longValue());
            Calendar now = Calendar.getInstance(UTC);

            AlarmNotificationService alarmNotificationService = registry.getService(alarm.getAction());
            if (alarmNotificationService == null) {
                LOG.error("Missing required AlarmNotificationService for alarm action \"{}\"", alarm.getAction().getValue());
                throw ServiceExceptionCode.absentService(AlarmNotificationService.class);
            }
            Integer shift = I(alarmNotificationService.getShift());
            long delay = (calTriggerTime.getTimeInMillis() - now.getTimeInMillis()) - (shift == null ? 0 : shift.intValue());
            if (delay < 0) {
                delay = 0;
            }

            LOG.trace("Created new task for {}", key);
            SingleMessageDeliveryTask task = createTask(key.getCid(), key.getAccount(), alarm, trigger, alarmNotificationService);
            ScheduledTimerTask timer = timerService.schedule(task, delay, TimeUnit.MILLISECONDS);
            cleanUp = null;
            scheduledTasks.put(key, timer);
        } catch (OXException e) {
            LOG.error("Failed to schedule task", e);
        } finally {
            if (cleanUp != null) {
                cleanUp.unlock();
            }
        }
    }

    /**
     * Cancels the task specified by the key if one exists
     *
     * @param key The key
     */
    private void cancelTask(Key key) {
        ScheduledTimerTask scheduledTimerTask = scheduledTasks.remove(key);
        if (scheduledTimerTask != null) {
            LOG.trace("Canceled message alarm task for {}", key);
            scheduledTimerTask.cancel();
        }
    }

    private static final Function<? super ContextAndAccountId, ? extends List<AlarmTrigger>> F_NEW_ARRAYLIST = caa -> new ArrayList<>();

    /**
     * Cancels all running thread and tries to reset their processed values
     */
    public void cancel() {
        Map<Integer, List<Entry<Key, ScheduledTimerTask>>> entries = cancelAllScheduledTasks();
        for (Entry<Integer, List<Entry<Key, ScheduledTimerTask>>> cidEntry : entries.entrySet()) {
            Connection con = null;
            try {
                Map<ContextAndAccountId, List<AlarmTrigger>> triggers = new HashMap<>(cidEntry.getValue().size());
                for (Entry<Key, ScheduledTimerTask> entry : cidEntry.getValue()) {
                    Key key = entry.getKey();
                    AlarmTrigger trigger = new DefaultAlarmTrigger();
                    trigger.setAlarm(I(key.getId()));
                    ContextAndAccountId caa = new ContextAndAccountId(key.getAccount(), key.getCid());
                    triggers.computeIfAbsent(caa, F_NEW_ARRAYLIST).add(trigger);
                    LOG.trace("Try to reset the processed status of the alarm trigger for {}", key);
                }
                con = dbservice.getWritable(cidEntry.getKey().intValue());
                if (storage != null && con != null) {
                    storage.dropProcessingStatus(con, triggers);
                    LOG.trace("Successfully resetted the processed stati for context {}.", cidEntry.getKey());
                }
            } catch (OXException e1) {
                // ignore
            } finally {
                Databases.close(con);
            }
        }

        scheduledTasks.clear();
    }

    private static final Function<? super Integer, ? extends List<Entry<Key, ScheduledTimerTask>>> F_NEW_ARRAYLIST2 = c -> new ArrayList<>();

    /**
     * Cancels all scheduled tasks and returns mapping of cids to those tasks
     *
     * @return The cid / List of entries mapping
     */
    private Map<Integer, List<Entry<Key, ScheduledTimerTask>>> cancelAllScheduledTasks() {
        Map<Integer, List<Entry<Key, ScheduledTimerTask>>> entries = new HashMap<>();
        for (Entry<Key, ScheduledTimerTask> entry : scheduledTasks.entrySet()) {
            Key key = entry.getKey();
            entry.getValue().cancel();
            entries.computeIfAbsent(I(key.getCid()), F_NEW_ARRAYLIST2).add(entry);
            LOG.trace("Canceled message alarm delivery task for {}", key);
        }
        return entries;
    }

    /**
     * Optionally gets an entity resolver for the supplied context.
     *
     * @param contextId The identifier of the context to get the entity resolver for
     * @return The entity resolver, or <code>null</code> if not available
     */
    private EntityResolver optEntityResolver(int contextId) {
        try {
            return calUtil.getEntityResolver(contextId);
        } catch (OXException e) {
            LOG.trace("Error getting entity resolver for context: {}", Integer.valueOf(contextId), e);
        }
        return null;
    }

    /**
     * Removes the {@link SingleMessageDeliveryTask} defined by the given key from the local map.
     *
     * @param key The key to remove
     */
    public void remove(Key key) {
        if (key != null) {
            scheduledTasks.remove(key);
        }
    }

}
