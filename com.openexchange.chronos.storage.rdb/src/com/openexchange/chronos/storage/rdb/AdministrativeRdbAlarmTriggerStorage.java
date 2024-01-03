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

package com.openexchange.chronos.storage.rdb;

import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;
import com.openexchange.chronos.AcquiredAlarmTriggerLock;
import com.openexchange.chronos.AlarmAction;
import com.openexchange.chronos.DefaultLockedAlarmTrigger;
import com.openexchange.chronos.AlarmTriggerField;
import com.openexchange.chronos.AlarmTrigger;
import com.openexchange.chronos.LockedAlarmTrigger;
import com.openexchange.chronos.alarm.message.MessageAlarmConfig;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.storage.AdministrativeAlarmTriggerStorage;
import com.openexchange.chronos.storage.ContextAndAccountId;
import com.openexchange.chronos.storage.rdb.osgi.Services;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.java.Sets;
import com.openexchange.java.util.UUIDs;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link AdministrativeRdbAlarmTriggerStorage}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.1
 */
public class AdministrativeRdbAlarmTriggerStorage implements AdministrativeAlarmTriggerStorage {

    protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AdministrativeRdbAlarmTriggerStorage.class);

    private static final AlarmTriggerDBMapper MAPPER = AlarmTriggerDBMapper.getInstance();

    @Override
    public Map<ContextAndAccountId, List<LockedAlarmTrigger>> getAndLockTriggers(Connection con, Date until, Date overdueTime, boolean lock, AlarmAction... actions) throws OXException {
        if (until == null) {
            return Collections.emptyMap();
        }
        if (actions == null || actions.length == 0) {
            return Collections.emptyMap();
        }

        Date overdueTimeToUse = overdueTime;
        if (overdueTimeToUse == null) {
            Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            instance.add(Calendar.MINUTE, -5);
            overdueTimeToUse = instance.getTime();
        }
        return getAndLockTriggers(con, until.getTime(), overdueTimeToUse.getTime(), lock, actions);
    }

    private static final Function<? super ContextAndAccountId, ? extends List<AlarmTrigger>> F_NEW_ARRAYLIST = p -> new ArrayList<>();
    private static final Function<? super ContextAndAccountId, ? extends List<LockedAlarmTrigger>> F_NEW_ARRAYLIST2 = p -> new ArrayList<>();

    private static Map<ContextAndAccountId, List<LockedAlarmTrigger>> getAndLockTriggers(Connection con, long until, long overdueTime, boolean lock, AlarmAction[] actions) throws OXException {
        try {
            AlarmTriggerField[] mappedFields = new AlarmTriggerField[] { AlarmTriggerField.ALARM_ID, AlarmTriggerField.TIME, AlarmTriggerField.EVENT_ID, AlarmTriggerField.USER_ID, AlarmTriggerField.RECURRENCE_ID};
            StringBuilder stringBuilder = new StringBuilder().append("SELECT `cid`, `account`, ").append(MAPPER.getColumns(mappedFields)).append(" FROM `calendar_alarm_trigger` WHERE ");
            addAlarmActions(stringBuilder, actions.length);
            stringBuilder.append(" AND `triggerDate`<?");
            stringBuilder.append(" AND (`processed`=0 OR (`triggerDate`<? AND `processed`<?))");

            Map<ContextAndAccountId, List<AlarmTrigger>> result = null;
            PreparedStatement stmt = null;
            ResultSet resultSet = null;
            try {
                stmt = con.prepareStatement(stringBuilder.toString());
                stringBuilder = null;

                int pos = 1;
                for (AlarmAction action : actions) {
                    stmt.setString(pos++, action.getValue());
                }
                stmt.setLong(pos++, until);
                stmt.setLong(pos++, overdueTime);
                stmt.setLong(pos++, overdueTime);
                resultSet = logExecuteQuery(stmt);
                if (resultSet.next()) {
                    result = new HashMap<>();
                    do {
                        result.computeIfAbsent(newPair(resultSet), F_NEW_ARRAYLIST).add(readTrigger(resultSet, mappedFields));
                    } while (resultSet.next());
                }
            } finally {
                Databases.closeSQLStuff(resultSet, stmt);
            }

            if (result == null) {
                return Collections.emptyMap();
            }

            return yieldLockedTriggersFor(result, lock, Long.valueOf(overdueTime), con);
        } catch (SQLException e) {
            throw CalendarExceptionCodes.DB_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Yields the locked alarm triggers for specified alarm trigger.
     * <p>
     * Trying to acquire the processing lock for each given alarm trigger.
     *
     * @param queriedTriggers The alarm triggers for which the processing lock should be acquired
     * @param lock Whether the processing lock should really be acquired or not (dry run)
     * @param optOverdueTime The optional date when an already acquired processing lock is considered as expired
     * @param con The read-write connection to use
     * @return The locked alarm trigger
     * @throws OXException If acquiring locked alarm triggers fails
     */
    private static Map<ContextAndAccountId, List<LockedAlarmTrigger>> yieldLockedTriggersFor(Map<ContextAndAccountId, List<AlarmTrigger>> queriedTriggers, boolean lock, Long optOverdueTime, Connection con) throws OXException {
        if (!lock) {
            // No locking. Return with dummy lock.
            Map<ContextAndAccountId, List<LockedAlarmTrigger>> dummyLockedAlarmTriggers = new HashMap<>();
            for (Map.Entry<ContextAndAccountId, List<AlarmTrigger>> e : queriedTriggers.entrySet()) {
                List<AlarmTrigger> triggers = e.getValue();
                dummyLockedAlarmTriggers.put(e.getKey(), triggers.stream().map(DefaultLockedAlarmTrigger::createUnlockedAlarmTriggerFor).collect(Collectors.toList()));
            }
            return dummyLockedAlarmTriggers;
        }

        int representativeContextId = queriedTriggers.keySet().iterator().next().getContextId();
        ConcurrentMap<AlarmKey, UUID> markedAsLocked = new ConcurrentHashMap<>();

        Map<ContextAndAccountId, List<LockedAlarmTrigger>> cleanUp = null;
        ScheduledTimerTask timerTask = null;
        try {
            Map<ContextAndAccountId, List<LockedAlarmTrigger>> lockedAlarmTriggers = new HashMap<>();
            cleanUp = lockedAlarmTriggers;
            for (Map.Entry<ContextAndAccountId, List<AlarmTrigger>> e : queriedTriggers.entrySet()) {
                ContextAndAccountId caa = e.getKey();
                for (AlarmTrigger alarmTrigger : e.getValue()) {
                    int alarmId = alarmTrigger.getAlarm().intValue();
                    UUID claim = acquireProcessingLock(caa, alarmId, optOverdueTime, con);
                    if (claim != null) {
                        // Lock acquired...
                        AlarmKey alarmKey = new AlarmKey(alarmId, caa.getAccountId(), caa.getContextId());
                        markedAsLocked.put(alarmKey, claim);
                        if (timerTask == null) {
                            // Timer task not yet initialized
                            timerTask = initLockRefresher(representativeContextId, markedAsLocked);
                        }

                        AcquiredAlarmTriggerLock acquiredLock = new RemoveFromCollectionAcquiredLock(alarmKey, markedAsLocked);
                        lockedAlarmTriggers.computeIfAbsent(caa, F_NEW_ARRAYLIST2).add(new DefaultLockedAlarmTrigger(alarmTrigger, acquiredLock, claim));
                    }
                }
            }

            cleanUp = null;
            timerTask = null;
            return lockedAlarmTriggers;
        } finally {
            if (timerTask != null) {
                timerTask.cancel();
            }
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
     * Initializes the refresher of processing lock for the alarm keys contained in given collection.
     *
     * @param representativeContextId A representative context identifier pointing to affected database schema
     * @param markedAsLocked The collection of alarms for which the processing lock needs to be refreshed
     * @return The timer task performing the lock refresh
     * @throws OXException If initializing lock refresher fails
     */
    private static ScheduledTimerTask initLockRefresher(int representativeContextId, ConcurrentMap<AlarmKey, UUID> markedAsLocked) throws OXException {
        TimerService timerService = Services.get().getServiceSafe(TimerService.class);

        AtomicReference<ScheduledTimerTask> timerTaskReference = new AtomicReference<>(null);
        Runnable task = new Runnable() {

            @Override
            public void run() {
                boolean somethingDone = updateLockEntries();
                if (!somethingDone) {
                    // Self-terminate...
                    ScheduledTimerTask stt = timerTaskReference.getAndSet(null);
                    if (stt != null) {
                        stt.cancel();
                        LOG.debug("Terminated lock refresher for schema associated with context {}", Integer.valueOf(representativeContextId));
                    }
                }
            }

            private boolean updateLockEntries() {
                if (markedAsLocked.isEmpty()) {
                    return false;
                }

                DatabaseService databaseService = null;
                Connection c = null;
                PreparedStatement stmt = null;
                try {
                    databaseService = Services.get().getServiceSafe(DatabaseService.class);
                    c = databaseService.getWritable(representativeContextId);

                    /*-
                     * Build something like:
                     *
                     * UPDATE `calendar_alarm_trigger` SET `processed`=1234 WHERE CONCAT(`cid`, `account`, `alarm`, `claim`) IN (CONCAT(1, 4, 2, ...), CONCAT(1, 6, 2. ...), ... )
                     */
                    StringBuilder sqlBuilder = new StringBuilder("UPDATE `calendar_alarm_trigger` SET `processed`=? WHERE CONCAT(`cid`, `account`, `alarm`, `claim`) IN (");
                    int reslen = sqlBuilder.length();
                    for (Set<Map.Entry<AlarmKey, UUID>> partition : Sets.partition(markedAsLocked.entrySet(), 100)) {
                        sqlBuilder.setLength(reslen);
                        Databases.appendConcatIN(sqlBuilder, 4, partition.size());
                        stmt = c.prepareStatement(sqlBuilder.toString());
                        int pos = 1;
                        stmt.setLong(pos++, System.currentTimeMillis());
                        for (Map.Entry<AlarmKey, UUID> entry : partition) {
                            AlarmKey alarmKey = entry.getKey();
                            stmt.setInt(pos++, alarmKey.contextId);
                            stmt.setInt(pos++, alarmKey.accountId);
                            stmt.setInt(pos++, alarmKey.alarmId);
                            stmt.setBytes(pos++, UUIDs.toByteArray(entry.getValue()));
                        }
                        stmt.executeUpdate();
                        closeSQLStuff(stmt);
                        stmt = null;
                    }
                    LOG.debug("Refreshed locks for schema associated with context {}", Integer.valueOf(representativeContextId));
                } catch (Exception e) {
                    LOG.warn("Failed to refresh locks for schema associated with context {}", Integer.valueOf(representativeContextId), e);
                } finally {
                    closeSQLStuff(stmt);
                    if (databaseService != null && c != null) {
                        databaseService.backWritable(representativeContextId, c);
                    }
                }
                return true;
            }
        };

        ScheduledTimerTask timerTask = timerService.scheduleWithFixedDelay(task, 1, 1, TimeUnit.MINUTES);
        timerTaskReference.set(timerTask);
        return timerTask;
    }

    /**
     * Tries to acquire the processing lock for specified alarm.
     *
     * @param caa The contact and account identifier tuple
     * @param alarmId The alarm identifier
     * @param optLockExpiredTime The optional date when and already acquired processing lock is considered as expired
     * @param con The connection to use
     * @return The claim if processing lock could be successfully acquired; otherwise <code>null</code>
     * @throws OXException If operation fails
     */
    private static UUID acquireProcessingLock(ContextAndAccountId caa, int alarmId, Long optLockExpiredTime, Connection con) throws OXException {
        long lockExpiredTime;
        if (optLockExpiredTime == null) {
            LeanConfigurationService configService = Services.get().getServiceSafe(LeanConfigurationService.class);
            int overDue = configService.getIntProperty(MessageAlarmConfig.OVERDUE);
            lockExpiredTime = System.currentTimeMillis() - overDue;
        } else {
            lockExpiredTime = optLockExpiredTime.longValue();
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `processed` FROM `calendar_alarm_trigger` WHERE `cid`=? AND `account`=? AND `alarm`=?");
            stmt.setInt(1, caa.getContextId());
            stmt.setInt(2, caa.getAccountId());
            stmt.setInt(3, alarmId);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                // No such entry...?
                return null;
            }

            long stamp = rs.getLong(1);
            Databases.closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;

            if (stamp != 0 && (stamp >= lockExpiredTime)) {
                // Already locked and not yet expired
                return null;
            }

            // Not locked or lock is expired
            UUID claim = UUID.randomUUID();
            stmt = con.prepareStatement("UPDATE `calendar_alarm_trigger` SET `processed`=?, `claim`=? WHERE `cid`=? AND `account`=? AND `alarm`=? AND `processed`=?");
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setBytes(2, UUIDs.toByteArray(claim));
            stmt.setInt(3, caa.getContextId());
            stmt.setInt(4, caa.getAccountId());
            stmt.setInt(5, alarmId);
            stmt.setLong(6, stamp);
            boolean acquired = stmt.executeUpdate() > 0;
            Databases.closeSQLStuff(stmt);
            stmt = null;

            return acquired ? claim : null;
        } catch (SQLException e) {
            throw CalendarExceptionCodes.DB_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    /**
     * Creates a new contact and account identifier tuple from current result set row.
     *
     * @param resultSet The result set to read from
     * @return The newly created contact and account identifier tuple
     * @throws SQLException If reading from result set fails
     */
    private static ContextAndAccountId newPair(ResultSet resultSet) throws SQLException {
        return new ContextAndAccountId(resultSet.getInt(2), resultSet.getInt(1));
    }

    /**
     * Modifies given string builder having specified alarm actions in <code>WHERE</code> clause.
     *
     * @param stringBuilder The string builder to append to
     * @param actions The actions to add
     */
    private static void addAlarmActions(StringBuilder stringBuilder, int numberOfActions) {
        if (numberOfActions > 0) {
            if (numberOfActions == 1) {
                stringBuilder.append("(`action`=?)");
            } else {
                stringBuilder.append("(`action` IN (?");
                for (int i = numberOfActions - 1; i-- > 0;) {
                    stringBuilder.append(", ?");
                }
                stringBuilder.append("))");
            }
        }
    }

    @Override
    public void dropProcessingStatus(Connection con, Map<ContextAndAccountId, List<AlarmTrigger>> triggers) throws OXException {
        if (triggers.isEmpty()) {
            return;
        }

        try {
            // Create flat list for triggers to update
            List<TriggerInfo> infos = new ArrayList<>();
            for (Map.Entry<ContextAndAccountId, List<AlarmTrigger>> entry : triggers.entrySet()) {
                ContextAndAccountId caa = entry.getKey();
                for (AlarmTrigger trigger : entry.getValue()) {
                    infos.add(new TriggerInfo(caa, trigger));
                }
            }

            // Check size
            int numberOfTriggers = infos.size();
            if (numberOfTriggers == 0) {
                return;
            }

            // Drop processing status for each trigger
            if (numberOfTriggers == 1) {
                dropProcessingForSingleAlarmTrigger(con, infos.get(0));
            } else {
                dropProcessingForMultipleAlarmTrigger(con, infos);
            }
        } catch (SQLException e) {
            throw CalendarExceptionCodes.DB_ERROR.create(e, e.getMessage());
        }
    }

    private void dropProcessingForMultipleAlarmTrigger(Connection con, List<TriggerInfo> infos) throws SQLException {
        PreparedStatement stmt = null;
        try {
            /*-
             * Build something like:
             *
             * UPDATE `calendar_alarm_trigger` SET `processed`=0, `claim`=NULL WHERE CONCAT(`cid`, `account`, `alarm`) IN ((1, 4, 2), (1, 6, 2), ... )
             */
            StringBuilder sqlBuilder = new StringBuilder("UPDATE `calendar_alarm_trigger` SET `processed`=?, `claim`=? WHERE CONCAT(`cid`, `account`, `alarm`) IN (");
            int reslen = sqlBuilder.length();
            for (List<TriggerInfo> partition : Lists.partition(infos, 100)) {
                sqlBuilder.setLength(reslen);
                Databases.appendConcatIN(sqlBuilder, 3, partition.size());
                stmt = con.prepareStatement(sqlBuilder.toString());
                int pos = 1;
                stmt.setLong(pos++, 0);
                stmt.setNull(pos++, Types.BINARY);
                for (TriggerInfo triggerInfo : partition) {
                    stmt.setInt(pos++, triggerInfo.caa().getContextId());
                    stmt.setInt(pos++, triggerInfo.caa().getAccountId());
                    stmt.setInt(pos++, i(triggerInfo.trigger().getAlarm()));
                }
                stmt.executeUpdate();
                closeSQLStuff(stmt);
                stmt = null;
            }
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    private void dropProcessingForSingleAlarmTrigger(Connection con, TriggerInfo triggerInfo) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("UPDATE `calendar_alarm_trigger` SET `processed`=?, `claim`=? WHERE `cid`=? AND `account`=? AND `alarm`=?");
            int pos = 1;
            stmt.setLong(pos++, 0);
            stmt.setNull(pos++, Types.BINARY);
            stmt.setInt(pos++, triggerInfo.caa().getContextId());
            stmt.setInt(pos++, triggerInfo.caa().getAccountId());
            stmt.setInt(pos++, i(triggerInfo.trigger().getAlarm()));
            stmt.executeUpdate();
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    /**
     * Retrieves an {@link AlarmTrigger} by reading the given {@link AlarmTriggerField}s from the result set.
     *
     * @param resultSet The {@link ResultSet}
     * @param fields The fields to read
     * @return The {@link AlarmTrigger}
     * @throws SQLException
     * @throws OXException
     */
    private static AlarmTrigger readTrigger(ResultSet resultSet, AlarmTriggerField... fields) throws SQLException, OXException {
        return MAPPER.fromResultSet(resultSet, fields);
    }

    @Override
    public Map<ContextAndAccountId, List<LockedAlarmTrigger>> getMessageAlarmTriggers(Connection con, int cid, int account, String eventId, boolean lock, AlarmAction... actions) throws OXException {
        if (actions == null || actions.length == 0) {
            return Collections.emptyMap();
        }

        try {
            AlarmTriggerField[] mappedFields = new AlarmTriggerField[] { AlarmTriggerField.ALARM_ID, AlarmTriggerField.TIME, AlarmTriggerField.EVENT_ID, AlarmTriggerField.USER_ID, AlarmTriggerField.RECURRENCE_ID };
            StringBuilder stringBuilder = new StringBuilder().append("SELECT `cid`, `account`, ").append(MAPPER.getColumns(mappedFields)).append(" FROM ").append("`calendar_alarm_trigger` WHERE ");
            stringBuilder.append("`cid`=? AND `account`=? AND ");
            addAlarmActions(stringBuilder, actions.length);
            stringBuilder.append(" AND `eventId`=? AND `processed`=0");

            Map<ContextAndAccountId, List<AlarmTrigger>> result = null;
            PreparedStatement stmt = null;
            ResultSet resultSet = null;
            try {
                stmt = con.prepareStatement(stringBuilder.toString());
                stringBuilder = null;

                int parameterIndex = 1;
                stmt.setInt(parameterIndex++, cid);
                stmt.setInt(parameterIndex++, account);
                for (AlarmAction action: actions) {
                    stmt.setString(parameterIndex++, action.getValue());
                }
                stmt.setString(parameterIndex++, eventId);
                resultSet = logExecuteQuery(stmt);
                if (resultSet.next()) {
                    result = new HashMap<>();
                    do {
                        result.computeIfAbsent(newPair(resultSet), F_NEW_ARRAYLIST).add(readTrigger(resultSet, mappedFields));
                    } while (resultSet.next());
                }
            } finally {
                Databases.closeSQLStuff(resultSet, stmt);
            }

            if (result == null) {
                return Collections.emptyMap();
            }

            return yieldLockedTriggersFor(result, lock, null, con);
        } catch (SQLException e) {
            throw CalendarExceptionCodes.DB_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Logs & executes a prepared statement's SQL query.
     *
     * @param stmt The statement to execute the SQL query from
     * @return The result set
     */
    protected static ResultSet logExecuteQuery(PreparedStatement stmt) throws SQLException {
        if (false == LOG.isDebugEnabled()) {
            return stmt.executeQuery();
        }
        String statementString = String.valueOf(stmt);
        long start = System.currentTimeMillis();
        ResultSet resultSet = stmt.executeQuery();
        LOG.debug("executeQuery: {} - {} ms elapsed.", statementString, L(System.currentTimeMillis() - start));
        return resultSet;
    }

    /**
     * Logs & executes a prepared statement's SQL update.
     *
     * @param stmt The statement to execute the SQL update from
     * @return The number of affected rows
     */
    protected static int logExecuteUpdate(PreparedStatement stmt) throws SQLException {
        if (false == LOG.isDebugEnabled()) {
            return stmt.executeUpdate();
        }
        String statementString = String.valueOf(stmt);
        long start = System.currentTimeMillis();
        int rowCount = stmt.executeUpdate();
        LOG.debug("executeUpdate: {} - {} rows affected, {} ms elapsed.", statementString, I(rowCount), L(System.currentTimeMillis() - start));
        return rowCount;
    }

    private static final class RemoveFromCollectionAcquiredLock implements AcquiredAlarmTriggerLock {

        private final ConcurrentMap<AlarmKey, UUID> markedAsLocked;
        private final AlarmKey alarmKey;

        RemoveFromCollectionAcquiredLock(AlarmKey alarmKey, ConcurrentMap<AlarmKey, UUID> markedAsLocked) {
            super();
            this.markedAsLocked = markedAsLocked;
            this.alarmKey = alarmKey;
        }

        @Override
        public void unlock() {
            markedAsLocked.remove(alarmKey);
        }
    }

    private static final class AlarmKey implements Comparable<AlarmKey> {

        private final int alarmId;
        private final int accountId;
        private final int contextId;
        private final int hash;

        AlarmKey(int alarmId, int accountId, int contextId) {
            this.alarmId = alarmId;
            this.accountId = accountId;
            this.contextId = contextId;

            int prime = 31;
            int result = 1;
            result = prime * result + contextId;
            result = prime * result + accountId;
            result = prime * result + alarmId;
            this.hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AlarmKey other = (AlarmKey) obj;
            if (contextId != other.contextId) {
                return false;
            }
            if (accountId != other.accountId) {
                return false;
            }
            if (alarmId != other.alarmId) { // NOSONARLINT
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(AlarmKey o) {
            int c = Integer.compare(contextId, o.contextId);
            if (c == 0) {
                c = Integer.compare(accountId, o.accountId);
            }
            if (c == 0) {
                c = Integer.compare(alarmId, o.alarmId);
            }
            return c;
        }
    } // End of class AlarmKey

    /**
     * Simple tuple for context/account identifier and alarm trigger.
     */
    private static record TriggerInfo(ContextAndAccountId caa, AlarmTrigger trigger) {
        // Nothing
    }

}
