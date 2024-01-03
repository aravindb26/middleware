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

package com.openexchange.groupware.tasks;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.sql.DBUtils.forSQLCommand;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.openexchange.annotation.Nullable;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.container.FolderChildObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.search.Order;
import com.openexchange.groupware.search.SearchObject;
import com.openexchange.groupware.search.TaskSearchObject;
import com.openexchange.java.Strings;
import com.openexchange.tools.StringCollection;

/**
 * This class contains methods for building the sql query for searches.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class SQL {

    /**
     * SQL statement selecting all fields for a task.
     */
    private static final String ALL_FIELDS;

    static final String COUNT_TASKS_IN_CONTEXT = "SELECT count(id) FROM task WHERE cid=?";

    /**
     * Tables for tasks.
     */
    public static final Map<StorageType, String> TASK_TABLES =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * Tables for participants.
     */
    static final Map<StorageType, String> PARTS_TABLES =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * Tables for external participants. No table anymore for deleted external participants because of bug 29809, US #53461243.
     */
    static final Map<StorageType, String> EPARTS_TABLES = new EnumMap<StorageType, String>(StorageType.class);

    /**
     * Tables for task folder mapping.
     */
    static final Map<StorageType, String> FOLDER_TABLES =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * Search for all tasks of a user.
     */
    static final Map<StorageType, String> SEARCH_USER_TASKS = new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for inserting participants.
     */
    static final Map<StorageType, String> INSERT_PARTS =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for selecting participants.
     */
    static final Map<StorageType, String> SELECT_PARTS =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for updating participants.
     */
    static final Map<StorageType, String> UPDATE_PARTS =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for deleting participants.
     */
    static final Map<StorageType, String> DELETE_PARTS =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * Search for tasks with a user as participant.
     */
    static final Map<StorageType, String> FIND_PARTICIPANT =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for finding group participants.
     */
    static final Map<StorageType, String> FIND_GROUP =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statement for inserting external participants.
     */
    static final Map<StorageType, String> INSERT_EXTERNAL =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statement for selecting external participants.
     */
    static final Map<StorageType, String> SELECT_EXTERNAL =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statement for deleting external participants.
     */
    static final Map<StorageType, String> DELETE_EXTERNAL =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for inserting folder mappings.
     */
    static final Map<StorageType, String> INSERT_FOLDER =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for deleting folder mappings.
     */
    static final Map<StorageType, String> DELETE_FOLDER =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for selecting task folder mappings.
     */
    static final Map<StorageType, String> SELECT_FOLDER =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for selecting a task folder mapping by the user.
     */
    static final Map<StorageType, String> FOLDER_BY_USER =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for selecting a task folder mapping by the folder
     * identifier.
     */
    static final Map<StorageType, String> FOLDER_BY_ID =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * selects the tasks identifier in a folder.
     */
    static final Map<StorageType, String> TASK_IN_FOLDER =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * SQL statements for searching all task folder mapping by the user.
     */
    static final Map<StorageType, String> SEARCH_FOLDER_BY_USER =
        new EnumMap<StorageType, String>(StorageType.class);

    /**
     * Prevent instantiation
     */
    private SQL() {
        super();
    }

    /**
     * @return all fields colon separated for using in SELECT and INSERT
     * statements.
     */
    static String getAllFields() {
        return ALL_FIELDS;
    }

    /**
     * @param columns attributes of a task that should be selected.
     * @param folder <code>true</code> if the folder must be selected in
     * searches.
     * @return all fields that are specified in the columns colon separated for
     * using in SELECT and INSERT statements.
     * @throws OXException if a mapping for a column isn't implemented.
     */
    static String getFields(final int[] columns, final boolean folder) throws OXException {
        return getFields(columns, folder, null);
    }

    /**
     * @param columns attributes of a task that should be selected.
     * @param folder <code>true</code> if the folder must be selected in
     * searches.
     * @param prefix reference table prefix for column names
     * @return all fields that are specified in the columns colon separated for
     * using in SELECT and INSERT statements.
     * @throws OXException if a mapping for a column isn't implemented.
     */
    static String getFields(int[] columns, boolean folder, String prefix) throws OXException {
        return getFields(columns, folder, false, prefix);
    }

    /**
     * @param columns attributes of a task that should be selected.
     * @param folder <code>true</code> if the folder must be selected in searches.
     * @param withGroupBy <code>true</code> If query uses a <code>"GROUP BY"</code> clause; otherwise <code>false</code>
     * @param prefix reference table prefix for column names
     * @return all fields that are specified in the columns colon separated for using in SELECT and INSERT statements.
     * @throws OXException if a mapping for a column isn't implemented.
     */
    static String getFields(int[] columns, boolean folder, boolean withGroupBy, String prefix) throws OXException {
        String prfix = (prefix == null) ? "" : prefix + ".";

        final StringBuilder builder = new StringBuilder();
        for (final int i : columns) {
            final Mapper<?> mapper = Mapping.getMapping(i);
            if (null == mapper) {
                switch (i) {
                case CommonObject.LAST_MODIFIED_OF_NEWEST_ATTACHMENT:
                case CalendarObject.PARTICIPANTS:
                case CalendarObject.USERS:
                case CalendarObject.ALARM:
                    break;
                case FolderChildObject.FOLDER_ID:
                    if (folder) {
                        if (withGroupBy) {
                            builder.append("MIN(folder) AS folder,");
                        } else {
                            builder.append("folder,");
                        }
                    }
                    break;
                default:
                    throw TaskExceptionCode.UNKNOWN_ATTRIBUTE.create(I(i));
                }
            } else {
                builder.append(prfix);
                builder.append(mapper.getDBColumnName());
                builder.append(',');
            }
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    /**
     * @param all folder identifier where all tasks can be seen.
     * @param own folder identifier where own tasks can be seen.
     * @param shared folder identifier of shared task folders that can be seen.
     * @return the sql where condition for limiting the tasks to all readable
     * objects in folders.
     */
    static String allFoldersWhere(final List<Integer> all,
        final List<Integer> own, final List<Integer> shared) {
        final StringBuilder sql = new StringBuilder();
        if (all.size() + own.size() + shared.size() > 0) {
            sql.append('(');
            if (all.size() > 0) {
                sql.append("(folder in (");
                for (int i = 0; i < all.size(); i++) {
                    sql.append("?,");
                }
                sql.setLength(sql.length() - 1);
                sql.append("))");
            }
            if (own.size() > 0) {
                if (sql.length() > 1) {
                    sql.append(" OR ");
                }
                sql.append("(folder in (");
                for (int i = 0; i < own.size(); i++) {
                    sql.append("?,");
                }
                sql.setLength(sql.length() - 1);
                sql.append(") AND created_from=?)");
            }
            if (shared.size() > 0) {
                if (sql.length() > 1) {
                    sql.append(" OR ");
                }
                sql.append("(folder in (");
                for (int i = 0; i < shared.size(); i++) {
                    sql.append("?,");
                }
                sql.setLength(sql.length() - 1);
                sql.append(") AND private=false)");
            }
            sql.append(')');
        } else {
            sql.append("false");
        }
        return sql.toString();
    }

    /**
     * @param search task search object.
     * @return SQL condition checking the end of tasks to be in range.
     * @throws OXException if the range is not defined properly in the task
     * search object.
     */
    static String getRangeWhere(final TaskSearchObject search)
        throws OXException {
        final StringBuilder sql = new StringBuilder();
        final Date[] range = search.getRange();
        if (null != range) {
            if (range.length < 1 || range.length > 2) {
                throw TaskExceptionCode.WRONG_DATE_RANGE.create(Integer.valueOf(range.length));
            }
            if (range.length >= 1) {
                sql.append("(end >= ?");
            }
            if (range.length == 2) {
                sql.append(" AND end < ?");
            }
            sql.append(')');
        }
        return sql.toString();
    }

    static String getPatternWhere(final TaskSearchObject search) {
        final StringBuilder sql = new StringBuilder();
        // This compare is correct. NO_PATTERN is null and cannot be compared with Object.equals()
        if (SearchObject.NO_PATTERN != search.getPattern()) {
            sql.append('(');
            sql.append(Mapping.getMapping(CalendarObject.TITLE).getDBColumnName());
            sql.append(" LIKE ? OR ");
            sql.append(Mapping.getMapping(CalendarObject.NOTE).getDBColumnName());
            sql.append(" LIKE ? OR ");
            sql.append(Mapping.getMapping(CommonObject.CATEGORIES).getDBColumnName());
            sql.append(" LIKE ?)");
        }
        return sql.toString();
    }

    static String getOnlyOwn(final String table) {
        final StringBuilder sql = new StringBuilder();
        sql.append(table);
        sql.append(".created_from=?");
        return sql.toString();
    }

    static String getNoPrivate(final String table) {
        final StringBuilder sql = new StringBuilder();
        sql.append(table);
        sql.append(".private=false");
        return sql.toString();
    }

    /**
     * @param orderBy attribute identifier that should be used for sorting.
     * @param order defining the order direction.
     * @return SQL order by expression.
     */
    static String getOrder(final int orderBy, final Order order) {
        final StringBuilder sql = new StringBuilder();
        if (0 != orderBy && !Order.NO_ORDER.equals(order)) {
            sql.append(" ORDER BY ");
            sql.append(Mapping.getMapping(orderBy).getDBColumnName());
            sql.append(' ');
            sql.append(forSQLCommand(order));
        }
        return sql.toString();
    }

    /**
     * Creates the row limit for a SQL select command.
     * @param from start of limit.
     * @param to end of limit.
     * @return string to append to a SQL select command.
     */
    static String getLimit(final int from, final int to) {
        final StringBuilder sql = new StringBuilder();
        if (-1 != to) {
            sql.append(" LIMIT ");
            sql.append(from);
            sql.append(',');
            sql.append(to - from);
        }
        return sql.toString();
    }

    /**
     * Appends conditions for the SQL <code>WHERE</code>-clause to match against the supplied internal user or external participants.
     * 
     * @param stringBuilder The {@link StringBuilder} instance to append the conditions to
     * @param participants The participants to append conditions for
     * @return The used parameters to set in the prepared statement
     */
    static Object[] appendParticipants(StringBuilder stringBuilder, Participant[] participants) {
        if (null == participants || 0 == participants.length) {
            return new Object[0];
        }
        List<Object> parameters = new ArrayList<Object>(participants.length);
        for (Participant participant : participants) {
            if (Participant.USER == participant.getType()) {
                stringBuilder.append(" AND EXISTS (SELECT 1 FROM task_participant AS tp WHERE task.cid=tp.cid AND task.id=tp.task AND tp.user=?)");
                parameters.add(I(participant.getIdentifier()));
            } else if (participant.getType() == Participant.EXTERNAL_USER && Strings.isNotEmpty(participant.getEmailAddress())) {
                stringBuilder.append(" AND EXISTS (SELECT 1 FROM task_eparticipant AS tp WHERE task.cid=tp.cid AND task.id=tp.task AND tp.mail=?)");
                parameters.add(StringCollection.prepareForSearch(Strings.replaceSurrogatePairs(participant.getEmailAddress(), '@'), false, false));
            }
        }
        return parameters.toArray(new Object[parameters.size()]);
    }

    /**
     * Maps a list of column names to the according database column mapper implementations.
     * @param fields string array with database column names.
     * @return the database column mapper implementation.
     */
    public static Mapper<?>[] mapColumns(final String[] fields) {
        final List<Mapper<?>> tmp = new ArrayList<Mapper<?>>();
        for (final String field : fields) {
            Mapper<?> mapper = mapColumn(field);
            if (null != mapper) {
                tmp.add(mapper);
                break;
            }
        }
        return tmp.toArray(new Mapper[tmp.size()]);
    }

    /**
     * Maps the column name to the according database column mapper implementation.
     * @param columnName name of a database column.
     * @return the database column mapper implementation or <code>null</code> if the column can not be mapped.
     */
    @Nullable
    public static Mapper<?> mapColumn(String columnName) {
        for (final Mapper<?> mapper : Mapping.MAPPERS) {
            if (mapper.getDBColumnName().equals(columnName)) {
                return mapper;
            }
        }
        return null;
    }
    

    @Nullable
    public static Mapper<?> mapFromExternalParticipantsColumn(String columnName) {
        for (final Mapper<?> mapper : ParticipantMapping.EXTERNAL_MAPPERS) {
            if (mapper.getDBColumnName().equals(columnName)) {
                return mapper;
            }
        }
        return null;
    }

    static {
        final StringBuilder selectAll = new StringBuilder();
        for (final Mapper<?> mapper : Mapping.MAPPERS) {
            selectAll.append(mapper.getDBColumnName());
            selectAll.append(',');
        }
        selectAll.setLength(selectAll.length() - 1);
        ALL_FIELDS = selectAll.toString();

        final StorageType[] active = new StorageType[] { StorageType.ACTIVE };
        final StorageType[] activeDelete = new StorageType[] { StorageType.ACTIVE, StorageType.DELETED };
        final String tableName = "@tableName@";

        TASK_TABLES.put(StorageType.ACTIVE, "task");
        TASK_TABLES.put(StorageType.DELETED, "del_task");
        PARTS_TABLES.put(StorageType.ACTIVE, "task_participant");
        PARTS_TABLES.put(StorageType.REMOVED, "task_removedparticipant");
        PARTS_TABLES.put(StorageType.DELETED, "del_task_participant");
        EPARTS_TABLES.put(StorageType.ACTIVE, "task_eparticipant");
        // Table del_task_eparticipant was removed with fix for bug 35992 because of US #53461243 and bug 29809.
        FOLDER_TABLES.put(StorageType.ACTIVE, "task_folder");
        FOLDER_TABLES.put(StorageType.DELETED, "del_task_folder");

        String sql = "SELECT id FROM @taskTable@ LEFT JOIN @participantTable@ ON @taskTable@.cid=@participantTable@.cid "
            + "AND @taskTable@.id=@participantTable@.task WHERE @taskTable@.cid=? AND "
            + "(@taskTable@.created_from=? OR @taskTable@.changed_from=?)";
        for (final StorageType type : activeDelete) {
            SEARCH_USER_TASKS.put(type, sql.replace("@taskTable@", TASK_TABLES.get(type))
                .replace("@participantTable@", PARTS_TABLES.get(type)));
        }

        sql = "INSERT INTO " + tableName + " (cid,task,user,group_id,accepted,"
            + "description) VALUES (?,?,?,?,?,?)";
        for (final StorageType type : activeDelete) {
            INSERT_PARTS.put(type, sql.replace(tableName, PARTS_TABLES.get(type)));
        }
        INSERT_PARTS.put(StorageType.REMOVED,
            "INSERT INTO task_removedparticipant (cid,task,user,group_id,"
            + "accepted,description,folder) VALUES (?,?,?,?,?,?,?)");
        SELECT_PARTS.put(StorageType.ACTIVE,
            "SELECT task,user,group_id,accepted,description "
            + "FROM task_participant WHERE cid=? AND task IN (");
        SELECT_PARTS.put(StorageType.REMOVED,
            "SELECT task,user,group_id,accepted,description,folder "
            + "FROM task_removedparticipant WHERE cid=? AND task IN (");
        SELECT_PARTS.put(StorageType.DELETED,
            "SELECT task,user,group_id,accepted,description "
            + "FROM del_task_participant WHERE cid=? AND task IN (");
        sql = "UPDATE " + tableName + " SET group_id=?, accepted=?, "
            + "description=? WHERE cid=? AND task=? AND user=?";
        for (final StorageType type : StorageType.values()) {
            UPDATE_PARTS.put(type, sql.replace(tableName, PARTS_TABLES
                .get(type)));
        }
        sql = "DELETE FROM " + tableName
            + " WHERE cid=? AND task=? AND user IN (";
        for (final StorageType type : StorageType.values()) {
            DELETE_PARTS.put(type, sql.replace(tableName, PARTS_TABLES
                .get(type)));
        }
        sql = "SELECT task FROM " + tableName + " WHERE cid=? AND user=?";
        for (final StorageType type : StorageType.values()) {
            FIND_PARTICIPANT.put(type, sql.replace(tableName,
                PARTS_TABLES.get(type)));
        }
        sql = "SELECT task FROM " + tableName + " WHERE cid=? AND group_id=?";
        for (final StorageType type : StorageType.values()) {
            FIND_GROUP.put(type, sql.replace(tableName, PARTS_TABLES
                .get(type)));
        }
        sql = "INSERT INTO " + tableName
            + " (cid,task,mail,display_name) VALUES (?,?,?,?)";
        for (final StorageType type : active) {
            INSERT_EXTERNAL.put(type, sql.replace(tableName, EPARTS_TABLES.get(type)));
        }
        sql = "DELETE FROM " + tableName
            + " WHERE cid=? AND task=? AND mail IN (";
        for (final StorageType type : active) {
            DELETE_EXTERNAL.put(type, sql.replace(tableName, EPARTS_TABLES.get(type)));
        }
        sql = "SELECT task,mail,display_name FROM " + tableName
            + " WHERE cid=? AND task IN (";
        for (final StorageType type : active) {
            SELECT_EXTERNAL.put(type, sql.replace(tableName, EPARTS_TABLES.get(type)));
        }

        sql = "INSERT INTO " + tableName + " (cid, id, folder, user) "
            + "VALUES (?,?,?,?)";
        for (final StorageType type : activeDelete) {
            INSERT_FOLDER.put(type, sql.replace(tableName, FOLDER_TABLES
                .get(type)));
        }
        sql = "DELETE FROM " + tableName
            + " WHERE cid=? AND id=? AND folder IN (";
        for (final StorageType type : activeDelete) {
            DELETE_FOLDER.put(type, sql.replace(tableName, FOLDER_TABLES
                .get(type)));
        }
        sql = "SELECT folder,user FROM " + tableName + " WHERE cid=? AND id=?";
        for (final StorageType type : activeDelete) {
            SELECT_FOLDER.put(type, sql.replace(tableName, FOLDER_TABLES
                .get(type)));
        }
        sql = "SELECT folder FROM " + tableName
            + " WHERE cid=? AND id=? AND user=?";
        for (final StorageType type : activeDelete) {
            FOLDER_BY_USER.put(type, sql.replace(tableName, FOLDER_TABLES
                .get(type)));
        }
        sql = "SELECT user FROM " + tableName
            + " WHERE cid=? AND id=? AND folder=?";
        for (final StorageType type : activeDelete) {
            FOLDER_BY_ID.put(type, sql.replace(tableName, FOLDER_TABLES
                .get(type)));
        }
        sql = "SELECT id FROM " + tableName + " WHERE cid=? AND folder=?";
        for (final StorageType type : activeDelete) {
            TASK_IN_FOLDER.put(type, sql.replace(tableName, FOLDER_TABLES
                .get(type)));
        }
        sql = "SELECT folder,id FROM " + tableName + " WHERE cid=? AND user=?";
        for (final StorageType type : activeDelete) {
            SEARCH_FOLDER_BY_USER.put(type, sql.replace(tableName, FOLDER_TABLES
                .get(type)));
        }
    }
}
