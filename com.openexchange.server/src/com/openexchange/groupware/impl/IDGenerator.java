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

package com.openexchange.groupware.impl;

import static com.openexchange.database.Databases.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.impl.DBPool;
import gnu.trove.map.hash.ConcurrentTIntObjectHashMap;

/**
 * This class contains methods to generate unique identifier for all groupware
 * object types.
 * <p>
 * IDGenerator contains three different implementations to generate a unique
 * id stored into a table into the database.
 * <p>
 * To register an additional database sequence table, the method <code>IDGenerator.registerType()</code>
 * can be used, see {@link IDGenerator#registerType(String, int)}.
 *
 * TODO move this class to some package for mysql storage.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class IDGenerator {

    public static enum Implementations {
        NODBFUNCTION(new NoDBFunction()),
        MYSQLFUNCTION(new MySQLFunction()),
        COMPARE_AND_SET(new CompareAndSetFunction());

        private final Implementation impl;

        private Implementations(final Implementation impl) {
            this.impl = impl;
        }

        /**
         * Gets the implementation.
         *
         * @return The implementation
         */
        public Implementation getImpl() {
            return impl;
        }
    }

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IDGenerator.class);

    /**
     * Used implementation.
     */
    private static final Implementation GETID_IMPL = Implementations.MYSQLFUNCTION.getImpl();

    private IDGenerator() {
        super();
    }

    /**
     * This method generates a unique identifier for inserting new objects into
     * the database.
     * @param context The context that holds the object.
     * @param type the object type.
     * @return a unique identifier for the object.
     * @throws SQLException if the unique identifier can't be generated.
     */
    public static int getId(final Context context, final int type) throws SQLException {
        Connection con = null;
        try {
            con = DBPool.pickupWriteable(context);
        } catch (OXException e) {
            final SQLException sexp = new SQLException("Cannot get connection from dbpool.");
            sexp.initCause(e);
            throw sexp;
        }
        int newId = -1;
        try {
            con.setAutoCommit(false);
            newId = getId(context, type, con);
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
            DBPool.closeWriterSilent(context, con);
        }
        return newId;
    }

    /**
     * This method generates a unique identifier for inserting new objects into
     * the database.
     * @param context The context that holds the object.
     * @param type the object type. See com.openexchange.groupware.Types.
     * @param con a writable database connection must be given here.
     * @return a unique identifier for the object.
     * @throws SQLException if the unique identifier can't be generated.
     */
    public static int getId(final Context context, final int type, final Connection con) throws SQLException {
        return getId(context.getContextId(), type, con);
    }

    /**
     * This method generates a unique identifier for inserting new objects into
     * the database.
     * @param contextId unique identifier of the context that holds the object.
     * @param type the object type. See com.openexchange.groupware.Types.
     * @param con a writable database connection must be given here.
     * @return a unique identifier for the object.
     * @throws SQLException if the unique identifier can't be generated.
     */
    public static int getId(final int contextId, final int type, final Connection con) throws SQLException {
        if (con.getAutoCommit()) {
            throw new SQLException("Generating unique identifier is threadsafe if and only if it is executed in a transaction.");
        }
        return GETID_IMPL.getId(contextId, type, con);
    }

    /**
     * Gets an identifier for the config database.
     * @param con a writable database connection must be given here.
     * @return a unique identifier for config database objects.
     * @throws SQLException if the unique identifier can't be generated.
     */
    public static int getId(final Connection con) throws SQLException {
        return getId(con, -1);
    }

    /**
     * Gets an identifier for the config database.
     * @param con a writable database connection must be given here.
     * @param a type identifier, value must be < -1. A new type can be registered using {@link IDGenerator#registerType(String, int)}
     * @return a unique identifier for config database objects.
     * @throws SQLException if the unique identifier can't be generated.
     */
    public static int getId(final Connection con, final int type) throws SQLException {
        if (con.getAutoCommit()) {
            throw new SQLException("Generating unique identifier is threadsafe if and only if it is executed in a transaction.");
        }
        return GETID_IMPL.getId(-1, type, con);
    }

    /**
     * Interface for the different methods that get new unique identifier from
     * the database.
     */
    public static interface Implementation {

        /**
         * This method generates a unique identifier for inserting new objects
         * into the database.
         * @param contextId The unique identifier of the context.
         * @param type the object type. See com.openexchange.groupware.Types.
         * @param con Database connection.
         * @return a unique identifier for the object.
         * @throws SQLException if the unique identifier can't be generated.
         */
        int getId(final int contextId, final int type, final Connection con) throws SQLException;

        /**
         * Register a new type to the {@link IDGenerator}.
         * <p>
         * Depending on the implementation type, the parameter <code>sqlstring</code> must be as
         * following:
         * {@link Implementations#NODBFUNCTION}:
         * <code>configdb_sequence</code>
         * <p>
         * {@link Implementations#MYSQLFUNCTION}:
         * <code>configdb_sequence</code>
         * <p>
         *
         * @param sql
         * @param type negative integer less then -1
         * @throws SQLException
         */
        void registerType(final String sql, final int type) throws SQLException;
    }

    static abstract class AbstractImplementation implements Implementation {

        /**
         * Maps the basic groupware types to SQL functions.
         */
        private static final ConcurrentTIntObjectHashMap<String> BASIC_TABLES;

        static {
            final ConcurrentTIntObjectHashMap<String> tmp = new ConcurrentTIntObjectHashMap<String>(20);
            tmp.put(-1, "configdb_sequence");
            tmp.put(Types.APPOINTMENT, "sequence_calendar");
            tmp.put(Types.CONTACT, "sequence_contact");
            tmp.put(Types.FOLDER, "sequence_folder");
            tmp.put(Types.TASK, "sequence_task");
            tmp.put(Types.USER_SETTING, "sequence_gui_setting");
            tmp.put(Types.REMINDER, "sequence_reminder");
            tmp.put(Types.ICAL, "sequence_ical");
            tmp.put(Types.PRINCIPAL, "sequence_principal");
            tmp.put(Types.RESOURCE, "sequence_resource");
            tmp.put(Types.INFOSTORE, "sequence_infostore");
            tmp.put(Types.ATTACHMENT, "sequence_attachment");
            tmp.put(Types.WEBDAV, "sequence_webdav");
            tmp.put(Types.MAIL_SERVICE, "sequence_mail_service");
            tmp.put(Types.GENERIC_CONFIGURATION, "sequence_genconf");
            tmp.put(Types.SUBSCRIPTION, "sequence_subscriptions");
            tmp.put(Types.EAV_NODE, "sequence_uid_eav_node");
            BASIC_TABLES = tmp;
        }

        // ---------------------------------------------------------------------------------------------------------

        /** The map for additionally registered types */
        protected final ConcurrentTIntObjectHashMap<String> registeredTypes;

        /**
         * Initializes a new {@link AbstractImplementation}.
         */
        protected AbstractImplementation() {
            super();
            registeredTypes = new ConcurrentTIntObjectHashMap<String>(2);
        }

        @Override
        public void registerType(final String sql, final int type) throws SQLException {
            if (registeredTypes.putIfAbsent(type, sql) != null) {
                throw new SQLException("Type " + type + " already in use.");
            }
        }

        /**
         * Gets the appropriate unique identifier SQL function for specified type.
         *
         * @param type The type to look-up by
         * @return The SQL function generating the unique identifier
         * @throws SQLException If no such type is registered
         */
        protected String getSequenceTable(int type) throws SQLException {
            String retval = BASIC_TABLES.get(type);
            if (retval == null) {
                retval = registeredTypes.get(type);
            }
            if (retval == null) {
                throw new SQLException("No table defined for type: " + type);
            }
            return retval;
        }
    }

    /**
     * This implementation atomically updates sequence identifier through a compare-and-set.
     */
    static class CompareAndSetFunction extends AbstractImplementation {

        @Override
        public int getId(final int contextId, final int type, final Connection con) throws SQLException {
            final int retval;
            if (type < 0) {
                retval = getInternal(type, con);
            } else {
                retval = getInternal(contextId, type, con);
            }
            return retval;
        }

        private int getInternal(final int type, final Connection con) throws SQLException {
            String table = getSequenceTable(type);

            boolean set;
            int currentId;
            do {
                PreparedStatement stmt = null;
                ResultSet result = null;
                try {
                    stmt = con.prepareStatement("SELECT id FROM " + table);
                    result = stmt.executeQuery();
                    if (!result.next()) {
                        throw new SQLException("Table " + table + " contains no row.");
                    }
                    currentId = result.getInt(1);
                    result.close();
                    result = null;
                    stmt.close();
                    stmt = null;

                    stmt = con.prepareStatement("UPDATE " + table + " SET id=? WHERE id=?");
                    stmt.setInt(1, currentId + 1);
                    stmt.setInt(2, currentId);
                    set = stmt.executeUpdate() > 0;
                } catch (SQLException e) {
                    LOG.debug("SQL Problem: {}", stmt == null ? e.getMessage() : stmt.toString(), e);
                    throw e;
                } finally {
                    closeSQLStuff(result, stmt);
                }
            } while (!set);
            return currentId + 1;
        }

        private int getInternal(final int contextId, final int type, final Connection con) throws SQLException {
            String table = getSequenceTable(type);

            boolean set;
            int currentId;
            do {
                PreparedStatement stmt = null;
                ResultSet result = null;
                try {
                    stmt = con.prepareStatement("SELECT id FROM " + table + " WHERE cid=?");
                    stmt.setInt(1, contextId);
                    result = stmt.executeQuery();
                    if (!result.next()) {
                        throw new SQLException("Table " + table + " contains no row for context " + contextId);
                    }
                    currentId = result.getInt(1);
                    result.close();
                    result = null;
                    stmt.close();
                    stmt = null;

                    stmt = con.prepareStatement("UPDATE " + table + " SET id=? WHERE cid=? AND id=?");
                    stmt.setInt(1, currentId + 1);
                    stmt.setInt(2, contextId);
                    stmt.setInt(3, currentId);
                    set = stmt.executeUpdate() > 0;
                } catch (SQLException e) {
                    LOG.debug("SQL Problem: {}", stmt == null ? e.getMessage() : stmt.toString(), e);
                    throw e;
                } finally {
                    closeSQLStuff(result, stmt);
                }
            } while (!set);
            return currentId + 1;
        }

    }

    /**
     * This implementation uses only SELECT, INSERT and UPDATE sql commands to
     * generate unique identifier.
     */
    static class NoDBFunction extends AbstractImplementation {

        @Override
        public int getId(final int contextId, final int type, final Connection con) throws SQLException {
            final int retval;
            if (type < 0) {
                retval = getInternal(type, con);
            } else {
                retval = getInternal(contextId, type, con);
            }
            return retval;
        }

        private int getInternal(final int type, final Connection con) throws SQLException {
            final String table = getSequenceTable(type);
            int newId = -1;
            PreparedStatement stmt = null;
            ResultSet result = null;
            try {
                stmt = con.prepareStatement("UPDATE " + table + " SET id=id+1");
                stmt.execute();
                stmt.close();
                stmt = con.prepareStatement("SELECT id FROM " + table);
                result = stmt.executeQuery();
                if (result.next()) {
                    newId = result.getInt(1);
                }
            } catch (SQLException e) {
                LOG.debug("SQL Problem: {}", stmt);
                throw e;
            } finally {
                closeSQLStuff(result, stmt);
            }
            if (-1 == newId) {
                throw new SQLException("Table " + table + " contains no row.");
            }
            return newId;
        }

        private int getInternal(final int contextId, final int type, final Connection con) throws SQLException {
            final String table = getSequenceTable(type);
            int newId = -1;
            PreparedStatement stmt = null;
            ResultSet result = null;
            try {
                stmt = con.prepareStatement("UPDATE " + table + " SET id=id+1 WHERE cid=?");
                stmt.setInt(1, contextId);
                stmt.execute();
                stmt.close();
                stmt = con.prepareStatement("SELECT id FROM " + table + " WHERE cid=?");
                stmt.setInt(1, contextId);
                result = stmt.executeQuery();
                if (result.next()) {
                    newId = result.getInt(1);
                }
            } catch (SQLException e) {
                LOG.debug("SQL Problem: {}", stmt);
                throw e;
            } finally {
                closeSQLStuff(result, stmt);
            }
            if (-1 == newId) {
                throw new SQLException("Table " + table + " contains no row for context " + contextId);
            }
            return newId;
        }

    }

    /**
     * This implementation uses MySQL specific last_insert_id() function to generate unique identifier.
     * We discovered that we are not able to generate unique identifier with the {@link NoDBFunction} implementation on some MySQL
     * instances. The result of the SELECT contained multiple rows. All investigations did not show any possibility to fix this. Only the
     * last highest identifier was correct. Without sorting the first returned identifier was always a little slower than the last highest
     * and correct one. See bug 18788. An ugly fix is to select with the following command: SELECT id FROM sequence ORDER BY id DESC LIMIT 1
     */
    static class MySQLFunction extends AbstractImplementation {

        @Override
        public int getId(final int contextId, final int type, final Connection con) throws SQLException {
            final int retval;
            if (type < 0) {
                retval = getInternal(type, con);
            } else {
                retval = getInternal(contextId, type, con);
            }
            return retval;
        }

        private int getInternal(final int type, final Connection con) throws SQLException {
            final String table = getSequenceTable(type);
            int newId = -1;
            PreparedStatement stmt = null;
            ResultSet result = null;
            try {
                stmt = con.prepareStatement("UPDATE " + table + " SET id=last_insert_id(id+1)");
                stmt.execute();
                stmt.close();
                stmt = con.prepareStatement("SELECT last_insert_id()");
                result = stmt.executeQuery();
                if (result.next()) {
                    newId = result.getInt(1);
                }
            } catch (SQLException e) {
                LOG.debug("SQL Problem: {}", stmt);
                throw e;
            } finally {
                closeSQLStuff(result, stmt);
            }
            if (-1 == newId) {
                throw new SQLException("Table " + table + " contains no row.");
            }
            return newId;
        }

        private int getInternal(final int contextId, final int type, final Connection con) throws SQLException {
            final String table = getSequenceTable(type);
            int newId = -1;
            PreparedStatement stmt = null;
            ResultSet result = null;
            try {
                stmt = con.prepareStatement("UPDATE " + table + " SET id=last_insert_id(id+1) WHERE cid=?");
                stmt.setInt(1, contextId);
                stmt.execute();
                stmt.close();
                stmt = con.prepareStatement("SELECT last_insert_id()");
                result = stmt.executeQuery();
                if (result.next()) {
                    newId = result.getInt(1);
                }
            } catch (SQLException e) {
                LOG.debug("SQL Problem: {}", stmt);
                throw e;
            } finally {
                closeSQLStuff(result, stmt);
            }
            if (-1 == newId) {
                throw new SQLException("Table " + table + " contains no row for context " + contextId);
            }
            return newId;
        }

    }
}
