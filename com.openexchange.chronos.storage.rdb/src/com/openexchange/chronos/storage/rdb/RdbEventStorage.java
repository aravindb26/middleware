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

import static com.openexchange.chronos.common.CalendarUtils.add;
import static com.openexchange.chronos.common.CalendarUtils.compare;
import static com.openexchange.chronos.common.CalendarUtils.isAllDay;
import static com.openexchange.chronos.common.CalendarUtils.isFloating;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import static com.openexchange.osgi.Tools.requireService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;
import com.google.common.collect.Lists;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.exception.ProblemSeverity;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.EventStorage;
import com.openexchange.chronos.storage.rdb.osgi.Services;
import com.openexchange.config.ConfigurationService;
import com.openexchange.database.Databases;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.Enums;
import com.openexchange.java.util.TimeZones;
import com.openexchange.search.SearchTerm;

/**
 * {@link RdbEventStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class RdbEventStorage extends RdbStorage implements EventStorage {

    private static final int INSERT_CHUNK_SIZE = 100;
    private static final int DELETE_CHUNK_SIZE = 200;
    private static final EventMapper MAPPER = EventMapper.getInstance();

    private final int accountId;
    private final EntityProcessor entityProcessor;

    /**
     * Initializes a new {@link RdbEventStorage}.
     *
     * @param context The context
     * @param accountId The account identifier
     * @param entityResolver The entity resolver to use, or <code>null</code> if not available
     * @param dbProvider The database provider to use
     * @param txPolicy The transaction policy
     */
    public RdbEventStorage(Context context, int accountId, EntityResolver entityResolver, DBProvider dbProvider, DBTransactionPolicy txPolicy) {
        super(context, dbProvider, txPolicy);
        this.accountId = accountId;
        this.entityProcessor = new EntityProcessor(context.getContextId(), entityResolver, this);
    }

    @Override
    public String nextId() throws OXException {
        String value = null;
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            value = asString(nextId(connection, accountId, "calendar_event_sequence"));
            updated = 1;
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            release(connection, updated);
        }
        return value;
    }

    @Override
    public long countEvents() throws OXException {
        return countEvents(null);
    }

    @Override
    public long countEvents(SearchTerm<?> searchTerm) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return countEvents(connection, false, searchTerm);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public long countEventTombstones(SearchTerm<?> searchTerm) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return countEvents(connection, true, searchTerm);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public List<Event> searchEvents(SearchTerm<?> searchTerm, SearchOptions searchOptions, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvents(connection, false, searchTerm, searchOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Event loadEvent(String eventId, EventField[] fields) throws OXException {
        List<Event> events = loadEvents(Collections.singletonList(eventId), fields);
        return 0 < events.size() ? events.get(0) : null;
    }

    @Override
    public List<Event> loadEvents(List<String> eventIds, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvents(connection, eventIds, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Event loadException(String seriesId, RecurrenceId recurrenceId, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectException(connection, seriesId, recurrenceId, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public List<Event> loadExceptions(String seriesId, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectExceptions(connection, seriesId, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public void insertEvent(Event event) throws OXException {
        insertEvents(Collections.singletonList(event));
    }

    @Override
    public void insertEvents(List<Event> events) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            for (List<Event> chunk : Lists.partition(events, INSERT_CHUNK_SIZE)) {
                updated += insertEvents(connection, chunk);
            }
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, events, connection, "calendar_event");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void updateEvent(Event event) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            if (needsRangeUpdate(event)) {
                updated = updateEvent(connection, event.getId(), event, getRangeFrom(event), getRangeUntil(event));
            } else {
                updated = updateEvent(connection, event.getId(), event);
            }
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, event, connection, "calendar_event");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void deleteEvent(String eventId) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated = deleteEvent(connection, eventId);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, (Event) null, connection, "calendar_event");
        } finally {
            release(connection, updated);
        }
    }


    @Override
    public void deleteEvents(List<String> eventIds) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            for (List<String> chunk : Lists.partition(eventIds, DELETE_CHUNK_SIZE)) {
                updated += deleteEvents(connection, chunk);
            }
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, (Event) null, connection, "calendar_event");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public boolean deleteAllEvents() throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated += deleteEvents(connection);
            updated += deleteTombstones(connection);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, (Event) null, connection, "calendar_event");
        } finally {
            release(connection, updated);
        }
        return 0 < updated;
    }

    private int deleteEvents(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM calendar_event WHERE cid=? AND account=?;")) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, accountId);
            return logExecuteUpdate(stmt);
        }
    }

    private int deleteTombstones(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM calendar_event_tombstone WHERE cid=? AND account=?;")) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, accountId);
            return logExecuteUpdate(stmt);
        }
    }

    @Override
    public List<Event> searchEventTombstones(SearchTerm<?> searchTerm, SearchOptions searchOptions, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvents(connection, true, searchTerm, searchOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public List<Event> searchOverlappingEvents(List<Attendee> attendees, boolean includeTransparent, SearchOptions searchOptions, EventField[] fields) throws OXException {
        Set<Integer> entities = new HashSet<Integer>();
        for (Attendee attendee : attendees) {
            if (CalendarUtils.isInternal(attendee)) {
                entities.add(I(attendee.getEntity()));
            }
        }
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectOverlappingEvents(connection, I2i(entities), includeTransparent, searchOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public void insertEventTombstone(Event event) throws OXException {
        insertEventTombstones(Collections.singletonList(event));
    }

    @Override
    public void insertEventTombstones(List<Event> events) throws OXException {
        if (null == events || 0 == events.size()) {
            return;
        }
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            for (List<Event> chunk : Lists.partition(events, INSERT_CHUNK_SIZE)) {
                updated += replaceTombstoneEvents(connection, chunk);
            }
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, events, connection, "calendar_event_tombstone");
        } finally {
            release(connection, updated);
        }
    }

    private int deleteEvent(Connection connection, String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM calendar_event WHERE cid=? AND account=? AND id=?;")) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, accountId);
            stmt.setInt(3, asInt(id));
            return logExecuteUpdate(stmt);
        }
    }

    private int deleteEvents(Connection connection, List<String> ids) throws SQLException {
        if (null == ids || 0 == ids.size()) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder()
            .append("DELETE FROM calendar_event WHERE cid=? AND account=? AND id")
            .append(Databases.getPlaceholders(ids.size())).append(';')
        ;
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            for (String id : ids) {
                stmt.setInt(parameterIndex++, asInt(id));
            }
            return logExecuteUpdate(stmt);
        }
    }

    private int insertEvents(Connection connection, List<Event> events) throws SQLException, OXException {
        if (null == events || 0 == events.size()) {
            return 0;
        }
        EventField[] mappedFields = MAPPER.getMappedFields();
        StringBuilder stringBuilder = new StringBuilder()
            .append("INSERT INTO calendar_event ")
            .append("(cid,account,").append(MAPPER.getColumns(mappedFields)).append(",rangeFrom,rangeUntil) ")
            .append("VALUES (?,?,").append(MAPPER.getParameters(mappedFields)).append(",?,?)")
        ;
        for (int i = 1; i < events.size(); i++) {
            stringBuilder.append(",(?,?,").append(MAPPER.getParameters(mappedFields)).append(",?,?)");
        }
        stringBuilder.append(';');
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            for (Event event : events) {
                validateEvent(event);
                stmt.setInt(parameterIndex++, context.getContextId());
                stmt.setInt(parameterIndex++, accountId);
                parameterIndex = MAPPER.setParameters(stmt, parameterIndex, entityProcessor.adjustPriorSave(event), mappedFields);
                stmt.setLong(parameterIndex++, getRangeFrom(event));
                stmt.setLong(parameterIndex++, getRangeUntil(event));
            }
            return logExecuteUpdate(stmt);
        }
    }

    private int replaceTombstoneEvents(Connection connection, List<Event> events) throws SQLException, OXException {
        if (null == events || 0 == events.size()) {
            return 0;
        }
        EventField[] mappedFields = MAPPER.getMappedFields();
        StringBuilder stringBuilder = new StringBuilder()
            .append("REPLACE INTO calendar_event_tombstone ")
            .append("(cid,account,").append(MAPPER.getColumns(mappedFields)).append(",rangeFrom,rangeUntil) ")
            .append("VALUES (?,?,").append(MAPPER.getParameters(mappedFields)).append(",?,?)")
        ;
        for (int i = 1; i < events.size(); i++) {
            stringBuilder.append(",(?,?,").append(MAPPER.getParameters(mappedFields)).append(",?,?)");
        }
        stringBuilder.append(';');
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            for (Event event : events) {
                stmt.setInt(parameterIndex++, context.getContextId());
                stmt.setInt(parameterIndex++, accountId);
                validateEvent(event);
                parameterIndex = MAPPER.setParameters(stmt, parameterIndex, entityProcessor.adjustPriorSave(event), mappedFields);
                stmt.setLong(parameterIndex++, getRangeFrom(event));
                stmt.setLong(parameterIndex++, getRangeUntil(event));
            }
            return logExecuteUpdate(stmt);
        }
    }

    private int updateEvent(Connection connection, String id, Event event) throws SQLException, OXException {
        EventField[] assignedfields = MAPPER.getAssignedFields(event);
        if (0 == assignedfields.length) {
            return 0;
        }
        String sql = new StringBuilder()
            .append("UPDATE calendar_event SET ").append(MAPPER.getAssignments(assignedfields))
            .append(" WHERE cid=? AND account=? AND id=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            validateEvent(event);
            parameterIndex = MAPPER.setParameters(stmt, parameterIndex, entityProcessor.adjustPriorSave(event), assignedfields);
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            stmt.setInt(parameterIndex++, asInt(id));
            return logExecuteUpdate(stmt);
        }
    }

    private int updateEvent(Connection connection, String id, Event event, long rangeFrom, long rangeUntil) throws SQLException, OXException {
        EventField[] assignedfields = MAPPER.getAssignedFields(event);
        if (0 == assignedfields.length) {
            return 0;
        }
        String sql = new StringBuilder()
            .append("UPDATE calendar_event SET ").append(MAPPER.getAssignments(assignedfields))
            .append(",rangeFrom=?,rangeUntil=? WHERE cid=? AND account=? AND id=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            validateEvent(event);
            parameterIndex = MAPPER.setParameters(stmt, parameterIndex, entityProcessor.adjustPriorSave(event), assignedfields);
            stmt.setLong(parameterIndex++, rangeFrom);
            stmt.setLong(parameterIndex++, rangeUntil);
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            stmt.setInt(parameterIndex++, asInt(id));
            return logExecuteUpdate(stmt);
        }
    }

    private List<Event> selectEvents(Connection connection, List<String> ids, EventField[] fields) throws SQLException, OXException {
        if (null == ids || 0 == ids.size()) {
            return Collections.emptyList();
        }
        List<Event> events = new ArrayList<Event>(ids.size());
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        String sql = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields)).append(" FROM calendar_event ")
            .append("WHERE cid=? AND account=? AND id").append(Databases.getPlaceholders(ids.size())).append(';')
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            for (String id : ids) {
                stmt.setInt(parameterIndex++, asInt(id));
            }
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    events.add(readEvent(connection, resultSet, mappedFields, null));
                }
            }
        }
        return events;
    }

    private Event selectException(Connection connection, String seriesId, RecurrenceId recurrenceId, EventField[] fields) throws SQLException, OXException {
        /*
         * lookup event with matching encoded recurrence identifier
         */
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        String sql = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields)).append(" FROM calendar_event ")
            .append("WHERE cid=? AND account=? AND series=? AND recurrence=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, accountId);
            stmt.setInt(3, asInt(seriesId));
            stmt.setString(4, CalendarUtils.encode(recurrenceId.getValue()));
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                if (resultSet.next()) {
                    return readEvent(connection, resultSet, mappedFields, null);
                }
            }
        }
        /*
         * lookup event with matching UTC recurrence identifier as fallback, if applicable
         */
        if (null != recurrenceId.getValue().getTimeZone() && false == "UTC".equals(recurrenceId.getValue().getTimeZone().getID())) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, context.getContextId());
                stmt.setInt(2, accountId);
                stmt.setInt(3, asInt(seriesId));
                stmt.setString(4, CalendarUtils.encode(recurrenceId.getValue().shiftTimeZone(TimeZones.UTC)));
                try (ResultSet resultSet = logExecuteQuery(stmt)) {
                    if (resultSet.next()) {
                        return readEvent(connection, resultSet, mappedFields, null);
                    }
                }
            }
        }
        /*
         * lookup matching recurrence manually as last resort
         */
        List<Event> changeExceptions = selectExceptions(connection, seriesId, new EventField[] { EventField.ID, EventField.RECURRENCE_ID });
        for (Event changeException : changeExceptions) {
            if (recurrenceId.matches(changeException.getRecurrenceId())) {
                List<Event> events = selectEvents(connection, Collections.singletonList(changeException.getId()), fields);
                return 0 < events.size() ? events.get(0) : null;
            }
        }
        /*
         * not found, otherwise
         */
        return null;
    }

    private List<Event> selectExceptions(Connection connection, String seriesId, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        String sql = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields)).append(" FROM calendar_event ")
            .append("WHERE cid=? AND account=? AND series=? AND id<>series;")
        .toString();
        List<Event> events = new ArrayList<Event>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, accountId);
            stmt.setInt(3, asInt(seriesId));
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    events.add(readEvent(connection, resultSet, mappedFields, null));
                }
            }
        }
        return events;
    }

    private List<Event> selectEvents(Connection connection, boolean deleted, SearchTerm<?> searchTerm, SearchOptions searchOptions, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        SearchAdapter adapter = new SearchAdapter(context.getContextId(), null, "e.", "a.").append(searchTerm);
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields, "e."))
            .append(" FROM ").append(deleted ? "calendar_event_tombstone" : "calendar_event").append(" AS e ")
        ;
        if (null != searchOptions && null != searchOptions.getFrom() && null != searchOptions.getUntil()) {
            IndexHint rangeIndexHint = getRangeIndexHint();
            if (null != rangeIndexHint && false == IndexHint.NONE.equals(rangeIndexHint)) {
                stringBuilder.append(rangeIndexHint.name()).append(" INDEX(`range`) ");
            }
        }
        if (adapter.usesAttendees()) {
            stringBuilder.append(" LEFT JOIN ").append(deleted ? "calendar_attendee_tombstone" : "calendar_attendee").append(" AS a ")
            .append("ON e.cid=a.cid AND e.account=a.account AND e.id=a.event");
        }
        stringBuilder.append(" WHERE e.cid=? AND e.account=?");
        if (null != searchOptions && null != searchOptions.getFrom()) {
            stringBuilder.append(" AND e.rangeUntil>?");
        }
        if (null != searchOptions && null != searchOptions.getUntil()) {
            stringBuilder.append(" AND e.rangeFrom<?");
        }
        stringBuilder.append(" AND ").append(adapter.getClause()).append(getSortOptions(MAPPER, searchOptions, "e.")).append(';');
        Set<String> ids = new HashSet<String>();
        List<Event> events = new ArrayList<Event>();
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            if (null != searchOptions && null != searchOptions.getFrom()) {
                stmt.setLong(parameterIndex++, searchOptions.getFrom().getTime());
            }
            if (null != searchOptions && null != searchOptions.getUntil()) {
                stmt.setLong(parameterIndex++, searchOptions.getUntil().getTime());
            }
            adapter.setParameters(stmt, parameterIndex++);
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    Event event = readEvent(connection, resultSet, mappedFields, "e.");
                    if (null == event.getId() || ids.add(event.getId())) {
                        events.add(event);
                    }
                }
            }
        }
        return events;
    }

    private long countEvents(Connection connection, boolean deleted, SearchTerm<?> searchTerm) throws SQLException, OXException {
        StringBuilder stringBuilder = new StringBuilder();
        SearchAdapter adapter = new SearchAdapter(context.getContextId(), null, "e.", "a.").append(searchTerm);
        if (false == adapter.usesEvents()) {
            if (false == adapter.usesAttendees()) {
                /*
                 * neither restrictions by event, nor by attendee table, so count all events in account
                 */
                stringBuilder
                    .append("SELECT COUNT(*) FROM ").append(deleted ? "calendar_event_tombstone" : "calendar_event")
                    .append(" WHERE cid=? AND account=?;")
                ;
            } else {
                /*
                 * no restrictions by event, so count via attendee table solely
                 */
                stringBuilder
                    .append("SELECT COUNT(DISTINCT a.event) FROM ").append(deleted ? "calendar_attendee_tombstone" : "calendar_attendee").append(" AS a")
                    .append(" WHERE a.cid=? AND a.account=? AND ").append(adapter.getClause()).append(';')
                ;
            }
        } else if (false == adapter.usesAttendees()) {
            /*
             * no restrictions by attendee, so count via event table solely
             */
            stringBuilder
                .append("SELECT COUNT(*) FROM ").append(deleted ? "calendar_event_tombstone" : "calendar_event").append(" AS e")
                .append(" WHERE e.cid=? AND e.account=? AND ").append(adapter.getClause()).append(';')
            ;
        } else {
            /*
             * restrictions by both event and attendee, so count joined result
             */
            stringBuilder
                .append("SELECT COUNT(DISTINCT e.id) FROM ").append(deleted ? "calendar_event_tombstone" : "calendar_event").append(" AS e")
                .append(" LEFT JOIN ").append(deleted ? "calendar_attendee_tombstone" : "calendar_attendee").append(" AS a")
                .append(" ON e.cid=a.cid AND e.account=a.account AND e.id=a.event")
                .append(" WHERE e.cid=? AND e.account=? AND ").append(adapter.getClause()).append(';')
            ;
        }
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            adapter.setParameters(stmt, parameterIndex++);
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        }
    }

    private List<Event> selectOverlappingEvents(Connection connection, int[] entities, boolean includeTransparent, SearchOptions searchOptions, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields, "e."))
            .append(" FROM calendar_event AS e")
        ;
        if (null != entities && 0 < entities.length) {
            stringBuilder.append(" LEFT JOIN calendar_attendee AS a ON e.cid=a.cid AND e.account=a.account AND e.id=a.event");
        }
        stringBuilder.append(" WHERE e.cid=? AND e.account=?");
        if (null != searchOptions && null != searchOptions.getFrom()) {
            stringBuilder.append(" AND e.rangeUntil>?");
        }
        if (null != searchOptions && null != searchOptions.getUntil()) {
            stringBuilder.append(" AND e.rangeFrom<?");
        }
        if (false == includeTransparent) {
            stringBuilder.append(" AND e.transp<>0");
        }
        if (null != entities && 0 < entities.length) {
            if (1 == entities.length) {
                stringBuilder.append(" AND (a.entity=? OR (e.folder IS NOT NULL AND e.user=?))");
            } else {
                stringBuilder.append(" AND (a.entity IN (").append(EventMapper.getParameters(entities.length)).append(") OR ")
                    .append("(e.folder IS NOT NULL AND e.user IN (").append(EventMapper.getParameters(entities.length)).append(")))");
            }
        }
        stringBuilder.append(getSortOptions(MAPPER, searchOptions, "e.")).append(';');
        Set<String> ids = new HashSet<String>();
        List<Event> events = new ArrayList<Event>();
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            if (null != searchOptions && null != searchOptions.getFrom()) {
                stmt.setLong(parameterIndex++, searchOptions.getFrom().getTime());
            }
            if (null != searchOptions && null != searchOptions.getUntil()) {
                stmt.setLong(parameterIndex++, searchOptions.getUntil().getTime());
            }
            if (null != entities && 0 < entities.length) {
                for (int entity : entities) {
                    stmt.setInt(parameterIndex++, entity);
                }
                for (int entity : entities) {
                    stmt.setInt(parameterIndex++, entity);
                }
            }
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    Event event = readEvent(connection, resultSet, mappedFields, "e.");
                    if (null == event.getId() || ids.add(event.getId())) {
                        events.add(event);
                    }
                }
            }
        }
        return events;
    }

    private Event readEvent(Connection connection, ResultSet resultSet, EventField[] fields, String columnLabelPrefix) throws SQLException, OXException {
        Event event = MAPPER.fromResultSet(resultSet, fields, columnLabelPrefix);
        if (event.containsEndDate() && null != event.getEndDate() && null != event.getStartDate()) {
            /*
             * take over 'all-day' nature from start-date
             */
            if (event.getStartDate().isAllDay()) {
                event.setEndDate(event.getEndDate().toAllDay());
            }
        }
        return entityProcessor.adjustAfterLoad(event, connection);
    }

    private void validateEvent(Event event) throws OXException {
        try {
            MAPPER.validateAll(event);
        } catch (OXException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException) {
                OXException oxe = asOXException((SQLException) cause, MAPPER, event, null, "calendar_event");
                LOG.warn("Validation failed for event {}", event.getId(), oxe);
                throw oxe;
            }

            LOG.warn("Validation failed for event {}", event.getId(), e);
            throw e;
        }
    }

    /**
     * Gets a value indicating whether the supplied updated event data also requires updating the <code>rangeFrom</code> and
     * <code>rangeUntil</code> columns, depending on which properties were changed.
     *
     * @param event The event to check
     * @return <code>true</code> if the range needs to be updated, <code>false</code>, otherwise
     */
    private static boolean needsRangeUpdate(Event event) {
        return event.containsStartDate() || event.containsEndDate() || event.containsRecurrenceRule();
    }

    /**
     * Calculates the start time of the effective range of an event, i.e. the start of the period the event spans. The range is always
     * the maximum, timezone-independent range for any possible occurrence of the event, i.e. the range for <i>floating</i> events is
     * expanded with the minimum/maximum timezone offsets, and the period for recurring event series will span from the first until the
     * last possible occurrence (or {@link Long#MAX_VALUE} for never ending series).
     * <p/>
     * For overridden instances (<i>change exceptions</i>), the start of the effective range is determined as the minimum of the event's
     * actual, and the original start date (as per its recurrence identifier).
     * <p/>
     * If no start- and end-date are set in the event, the "from" value for the maximum range {@link Long#MIN_VALUE} is returned.
     *
     * @param event The event to get the range for
     * @return The start time of the effective range of an event
     */
    private long getRangeFrom(Event event) {
        DateTime rangeFrom = event.getStartDate();
        if (null == rangeFrom) {
            /*
             * (legacy) tombstone event without persisted start-/end-date
             */
            return Long.MIN_VALUE;
        }
        /*
         * extend range to include original event start date (based on recurrence id) for overridden instances
         */
        if (isSeriesException(event) && null != event.getRecurrenceId() && 0 > compare(event.getRecurrenceId().getValue(), rangeFrom, TimeZones.UTC)) {
            rangeFrom = event.getRecurrenceId().getValue();
        }
        if (isFloating(event)) {
            /*
             * add easternmost offset (GMT +14:00)
             */
            return add(new Date(rangeFrom.getTimestamp()), Calendar.HOUR_OF_DAY, -14).getTime();
        }
        return rangeFrom.getTimestamp();
    }

    /**
     * Calculates the end time of the effective range of an event, i.e. the end of the period the event spans. The range is always
     * the maximum, timezone-independent range for any possible occurrence of the event, i.e. the range for <i>floating</i> events is
     * expanded with the minimum/maximum timezone offsets, and the period for recurring event series will span from the first until the
     * last possible occurrence (or {@link Long#MAX_VALUE} for never ending series).
     * <p/>
     * For overridden instances (<i>change exceptions</i>), the end of the effective range is determined as the maximum of the event's
     * actual end date, and the original start date (as per its recurrence identifier).
     * <p/>
     * If no start- and end-date are set in the event, the "until" valid for the maximum range {@link Long#MAX_VALUE} is returned.
     *
     * @param event The event to get the range for
     * @return The start time of the effective range of an event
     */
    private long getRangeUntil(Event event) {
        DateTime rangeUntil = event.getEndDate();
        if (null == rangeUntil) {
            if (null == event.getStartDate()) {
                /*
                 * (legacy) tombstone event without persisted start-/end-date
                 */
                return Long.MAX_VALUE;
            }
            if (isAllDay(event)) {
                /*
                 * assume a duration of one day for all-day events w/o defined end-date
                 */
                rangeUntil = event.getStartDate().addDuration(new Duration(1, 1, 0));
            } else {
                /*
                 * assume 'instant' event that ends on its start-date, otherwise
                 */
                rangeUntil = event.getStartDate();
            }
        }
        if (isSeriesMaster(event)) {
            /*
             * take over end-date of last occurrence
             */
            try {
                DefaultRecurrenceData recurrenceData = new DefaultRecurrenceData(event.getRecurrenceRule(), event.getStartDate(), null);
                RecurrenceId lastRecurrenceId = requireService(RecurrenceService.class, Services.get()).getLastOccurrence(recurrenceData);
                if (null == lastRecurrenceId) {
                    return Long.MAX_VALUE; // never ending series
                }
                long eventDuration = event.getEndDate().getTimestamp() - event.getStartDate().getTimestamp();
                rangeUntil = new DateTime(rangeUntil.getTimeZone(), lastRecurrenceId.getValue().getTimestamp() + eventDuration);
            } catch (OXException e) {
                addInvalidDataWarning(event.getId(), EventField.RECURRENCE_RULE, ProblemSeverity.NORMAL, "Unable to determine effective range for series master event", e);
            }
        } else if (isSeriesException(event) && null != event.getRecurrenceId() && 0 < compare(event.getRecurrenceId().getValue(), rangeUntil, TimeZones.UTC)) {
            /*
             * extend range to include original event start date (based on recurrence id) for overridden instances
             */
            rangeUntil = event.getRecurrenceId().getValue();
        }
        if (isFloating(event)) {
            /*
             * add offset of 'westernmost' timezone offset (GMT-12:00)
             */
            return add(new Date(rangeUntil.getTimestamp()), Calendar.HOUR_OF_DAY, 12).getTime();
        }
        return rangeUntil.getTimestamp();
    }

    /**
     * Gets the configured range index hint as per <code>com.openexchange.calendar.storage.rangeIndexHint</code>.
     * 
     * @return The configured range index hint, or its default {@link IndexHint#NONE} if not configured differently
     */
    private static IndexHint getRangeIndexHint() throws OXException {
        String value = Services.getService(ConfigurationService.class, true)
            .getProperty("com.openexchange.calendar.storage.rangeIndexHint", IndexHint.NONE.name());
        return Enums.parse(IndexHint.class, value, IndexHint.NONE);
    }

}
