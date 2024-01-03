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

package com.openexchange.tools.update;

import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.java.Strings;

/**
 * This class contains some tools to ease update of database.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Tools {

    /**
     * Prevent instantiation
     */
    private Tools() {
        super();
    }

    /**
     * Checks if specified column is nullable (<code>NULL</code> is allowed).
     *
     * @param con The connection to use
     * @param table The table
     * @param column The column
     * @return <code>true</code> if nullable; otherwise <code>false</code>
     * @throws SQLException If column cannot be checked if nullable
     */
    public static final boolean isNullable(final Connection con, final String table, final String column) throws SQLException {
        return Databases.isNullable(con, table, column);
    }

    /**
     * Checks if denoted table has its PRIMARY KEY set to specified columns.
     *
     * @param con The connection to use
     * @param table The tanle to check
     * @param columns The expected PRIMARY KEY
     * @return <code>true</code> it denoted table has such a PRIMARY KEY; otherwise <code>false</code>
     * @throws SQLException If PRIMARY KEY check fails
     */
    public static final boolean existsPrimaryKey(final Connection con, final String table, final String[] columns) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        final List<String> foundColumns = new ArrayList<String>();
        ResultSet result = null;
        try {
            result = metaData.getPrimaryKeys(con.getCatalog(), null, table);
            while (result.next()) {
                final String columnName = result.getString(4);
                final int columnPos = result.getInt(5);
                while (foundColumns.size() < columnPos) {
                    foundColumns.add(null);
                }
                foundColumns.set(columnPos - 1, columnName);
            }
        } finally {
            closeSQLStuff(result);
        }
        boolean matches = columns.length == foundColumns.size();
        for (int i = 0; matches && i < columns.length; i++) {
            matches = columns[i].equalsIgnoreCase(foundColumns.get(i));
        }
        return matches;
    }

    /**
     * Lists the names of available indexes for specified table.
     *
     * @param con The connection to use
     * @param table The table name
     * @return The names of available indexes
     * @throws SQLException If listing the names of available indexes fails
     */
    public static final String[] listIndexes(Connection con, String table) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        ResultSet result = null;
        try {
            result = metaData.getIndexInfo(con.getCatalog(), null, table, false, false);
            Set<String> names = new LinkedHashSet<String>();
            while (result.next()) {
                String indexName = result.getString(6);
                names.add(indexName);
            }
            return names.toArray(new String[names.size()]);
        } finally {
            closeSQLStuff(result);
        }
    }

    /**
     * @param con readable database connection.
     * @param table table name that indexes should be tested.
     * @param columns column names that the index must cover.
     * @return the name of an index that matches the given columns or <code>null</code> if no matching index is found.
     * @throws SQLException if some SQL problem occurs.
     * @throws NullPointerException if one of the columns is <code>null</code>.
     */
    public static final String existsIndex(final Connection con, final String table, final String[] columns) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        final Map<String, ArrayList<String>> indexes = new HashMap<String, ArrayList<String>>();
        ResultSet result = null;
        try {
            result = metaData.getIndexInfo(con.getCatalog(), null, table, false, false);
            while (result.next()) {
                final String indexName = result.getString(6);
                final int columnPos = result.getInt(8);
                final String columnName = result.getString(9);
                ArrayList<String> foundColumns = indexes.get(indexName);
                if (null == foundColumns) {
                    foundColumns = new ArrayList<String>();
                    indexes.put(indexName, foundColumns);
                }
                while (foundColumns.size() < columnPos) {
                    foundColumns.add(null);
                }
                foundColumns.set(columnPos - 1, columnName);
            }
        } finally {
            closeSQLStuff(result);
        }
        String foundIndex = null;
        final Iterator<Entry<String, ArrayList<String>>> iter = indexes.entrySet().iterator();
        while (null == foundIndex && iter.hasNext()) {
            final Entry<String, ArrayList<String>> entry = iter.next();
            final ArrayList<String> foundColumns = entry.getValue();
            if (columns.length != foundColumns.size()) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; matches && i < columns.length; i++) {
                matches = columns[i].equalsIgnoreCase(foundColumns.get(i));
            }
            if (matches) {
                foundIndex = entry.getKey();
            }
        }
        return foundIndex;
    }

    public static final String existsForeignKey(final Connection con, final String primaryTable, final String[] primaryColumns, final String foreignTable, final String[] foreignColumns) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        final Set<ForeignKey> keys = new HashSet<ForeignKey>();
        ResultSet result = null;
        try {
            result = metaData.getImportedKeys(con.getCatalog(), null, foreignTable);
            ForeignKey key = null;
            while (result.next()) {
                final String foundPrimaryTable = result.getString("PKTABLE_NAME");
                final String foundForeignTable = result.getString("FKTABLE_NAME");
                final String keyName = result.getString("FK_NAME");
                final ForeignKey tmp = new ForeignKey(keyName, foundPrimaryTable, foundForeignTable);
                if (null == key || !key.isSame(tmp)) {
                    key = tmp;
                    keys.add(key);
                }
                final String primaryColumn = result.getString("PKCOLUMN_NAME");
                final String foreignColumn = result.getString("FKCOLUMN_NAME");
                final int columnPos = result.getInt("KEY_SEQ");
                key.setPrimaryColumn(columnPos - 1, primaryColumn);
                key.setForeignColumn(columnPos - 1, foreignColumn);
            }
        } finally {
            closeSQLStuff(result);
        }
        for (final ForeignKey key : keys) {
            if (key.getPrimaryTable().equalsIgnoreCase(primaryTable) && key.getForeignTable().equalsIgnoreCase(foreignTable) && key.matches(primaryColumns, foreignColumns)) {
                return key.getName();
            }
        }
        return null;
    }

    public static final List<String> allForeignKey(final Connection con, final String foreignTable) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet result = null;
        try {
            result = metaData.getImportedKeys(con.getCatalog(), null, foreignTable);
            if (!result.next()) {
                return Collections.emptyList();
            }

            Set<String> set = new HashSet<String>();
            do {
                final String keyName = result.getString("FK_NAME");
                if (null != keyName) {
                    set.add(keyName);
                }
            } while (result.next());
            return set.isEmpty() ? Collections.<String> emptyList() : new ArrayList<String>(set);
        } finally {
            closeSQLStuff(result);
        }
    }

    /**
     * This method drops the primary key on the table. Beware, this method is vulnerable to SQL injection because table and index name can
     * not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table table name that primary key should be dropped.
     * @throws SQLExceptionif some SQL problem occurs.
     */
    public static final void dropPrimaryKey(final Connection con, final String table) throws SQLException {
        final String sql = "ALTER TABLE " + surroundWithBackticksIfNeeded(table) + " DROP PRIMARY KEY";
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * This method drops an index with the given name. Beware, this method is vulnerable to SQL injection because table and index name can
     * not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table table name that index should be dropped.
     * @param index name of the index to drop.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void dropIndex(final Connection con, final String table, final String index) throws SQLException {
        final String sql = "ALTER TABLE " + surroundWithBackticksIfNeeded(table) + " DROP INDEX " + surroundWithBackticksIfNeeded(index);
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * This method drops an index with the given foreign key. Beware, this method is vulnerable to SQL injection because table and foreign key name can
     * not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table table name that index should be dropped.
     * @param foreignKey name of the foreign key to drop.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void dropForeignKey(final Connection con, final String table, final String foreignKey) throws SQLException {
        final String sql = "ALTER TABLE " + surroundWithBackticksIfNeeded(table) + " DROP FOREIGN KEY " + surroundWithBackticksIfNeeded(foreignKey);
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * Drops the key with the specified name. Beware, this method is vulnerable to SQL injection because table and key name can
     * not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table table name that index should be dropped.
     * @param key name of the key to drop.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void dropKey(Connection con, String table, String key) throws SQLException {
        String sql = "ALTER TABLE " + surroundWithBackticksIfNeeded(table) + " DROP KEY " + surroundWithBackticksIfNeeded(key);
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * This method creates a new primary key on a table. Beware, this method is vulnerable to SQL injection because table and column names
     * can not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table name of the table that should get a new primary key.
     * @param columns names of the columns the primary key should cover.
     * @param lengths The column lengths; <code>-1</code> for full column
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createPrimaryKey(final Connection con, final String table, final String[] columns, final int[] lengths) throws SQLException {
        createKey(con, table, columns, lengths, true, null);
    }

    /**
     * This method creates a new (primary) key on a table. Beware, this method is vulnerable to SQL injection because table and column names
     * can not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table name of the table that should get a new primary key.
     * @param columns names of the columns the key should cover.
     * @param lengths The column lengths; <code>-1</code> for full column
     * @param primary <code>true</code> if a <code>PRIMARY KEY</code> is to be created; <code>false</code> for a <code>KEY</code>
     * @param name The name of the <code>KEY</code>. In case of a <code>PRIMARY KEY</code> the name will simply be ignored.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createKey(final Connection con, final String table, final String[] columns, final int[] lengths, boolean primary, String name) throws SQLException {
        final StringBuilder sql = new StringBuilder("ALTER TABLE ");
        sql.append(surroundWithBackticksIfNeeded(table));
        sql.append(" ADD ");
        if (primary) {
            sql.append("PRIMARY ");
        }
        sql.append("KEY ");
        if (!primary && Strings.isNotEmpty(name)) {
            sql.append(surroundWithBackticksIfNeeded(name));
        }
        sql.append(" (");
        {
            final String column = columns[0];
            sql.append(surroundWithBackticksIfNeeded(column));
            final int len = lengths[0];
            if (len > 0) {
                sql.append('(').append(len).append(')');
            }
        }
        for (int i = 1; i < columns.length; i++) {
            final String column = columns[i];
            sql.append(',');
            sql.append(surroundWithBackticksIfNeeded(column));
            final int len = lengths[i];
            if (len > 0) {
                sql.append('(').append(len).append(')');
            }
        }
        sql.append(')');
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * This method creates a new primary key on a table. Beware, this method is vulnerable to SQL injection because table and column names
     * can not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table name of the table that should get a new primary key.
     * @param columns names of the columns the primary key should cover.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createPrimaryKeyIfAbsent(final Connection con, final String table, final String[] columns) throws SQLException {
        if (!existsPrimaryKey(con, table, columns)) {
            if (hasPrimaryKey(con, table)) {
                dropPrimaryKey(con, table);
            }
            createPrimaryKey(con, table, columns);
        }
    }

    /**
     * This method creates a new primary key on a table. Beware, this method is vulnerable to SQL injection because table and column names
     * can not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table name of the table that should get a new primary key.
     * @param columns names of the columns the primary key should cover.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createPrimaryKey(final Connection con, final String table, final String[] columns) throws SQLException {
        final int[] lengths = new int[columns.length];
        Arrays.fill(lengths, -1);
        createPrimaryKey(con, table, columns, lengths);
    }

    /**
     * This method creates a new index on a table. Beware, this method is vulnerable to SQL injection because table and column names can not
     * be set through a {@link PreparedStatement}
     *
     * @param con writable database connection.
     * @param table name of the table that should get a new index.
     * @param name name of the index or <code>null</code> to let the database define the name.
     * @param columns names of the columns the index should cover.
     * @param unique if this should be a unique index.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createIndex(final Connection con, final String table, final String name, final String[] columns, final boolean unique) throws SQLException {
        final StringBuilder sql = new StringBuilder("ALTER TABLE ");
        sql.append(surroundWithBackticksIfNeeded(table));
        sql.append(" ADD ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ");
        if (null != name) {
            sql.append(surroundWithBackticksIfNeeded(name));
            sql.append(' ');
        }
        sql.append('(');
        for (final String column : columns) {
            sql.append(surroundWithBackticksIfNeeded(column));
            sql.append(',');
        }
        sql.setLength(sql.length() - 1);
        sql.append(')');
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * Surrounds specified column name with back-tick characters (<code>'`'</code>) if needed.
     * <p>
     * Examples:
     * <table>
     * <tr><td align="right"><code>"name"</code></td><td>--&gt;</td><td><code>"`name`"</code></td></tr>
     * <tr><td align="right"><code>"`name"</code></td><td>--&gt;</td><td><code>"`name`"</code></td></tr>
     * <tr><td align="right"><code>"name`"</code></td><td>--&gt;</td><td><code>"`name`"</code></td></tr>
     * <tr><td align="right"><code>"`name`"</code></td><td>--&gt;</td><td><code>"`name`"</code></td></tr>
     * <tr><td align="right"><code>"name(191)"</code></td><td>--&gt;</td><td><code>"`name`(191)"</code></td></tr>
     * <tr><td align="right"><code>"`name(191)"</code></td><td>--&gt;</td><td><code>"`name`(191)"</code></td></tr>
     * <tr><td align="right"><code>"name`(191)"</code></td><td>--&gt;</td><td><code>"`name`(191)"</code></td></tr>
     * <tr><td align="right"><code>"`name`(191)"</code></td><td>--&gt;</td><td><code>"`name`(191)"</code></td></tr>
     * </table>
     *
     * @param name The column name to examine
     * @return The column surrounded with back-ticks
     */
    private static String surroundWithBackticksIfNeeded(String name) {
        if (null == name) {
            return name;
        }

        // `name`(191)
        String toExamine = name;
        boolean startingBacktick = toExamine.startsWith("`");
        boolean endingBacktick = toExamine.endsWith("`");
        if (startingBacktick && endingBacktick) {
            return name;
        }

        int openingParenthesis = toExamine.indexOf('(');
        if (openingParenthesis == 0) {
            throw new IllegalArgumentException("Invalid column name: " + name);
        }

        String tail = null;
        if (openingParenthesis > 0) {
            tail = toExamine.substring(openingParenthesis);
            toExamine = toExamine.substring(0, openingParenthesis);
        }

        endingBacktick = toExamine.endsWith("`");
        if (startingBacktick && endingBacktick) {
            return name;
        }

        StringBuilder sb = new StringBuilder(name.length() + 2);
        if (startingBacktick == false) {
            sb.append('`');
        }
        sb.append(toExamine);
        if (endingBacktick == false) {
            sb.append('`');
        }
        if (tail != null) {
            sb.append(tail);
        }
        return sb.toString();
    }

    /**
     * This method creates a new index on a table. Beware, this method is vulnerable to SQL injection because table and column names can not
     * be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table name of the table that should get a new index.
     * @param columns names of the columns the index should cover.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createIndex(final Connection con, final String table, final String[] columns) throws SQLException {
        createIndex(con, table, null, columns, false);
    }

    public static void createForeignKey(final Connection con, final String table, final String[] columns, final String referencedTable, final String[] referencedColumns) throws SQLException {
        createForeignKey(con, null, table, columns, referencedTable, referencedColumns);
    }

    public static void createForeignKey(final Connection con, final String name, final String table, final String[] columns, final String referencedTable, final String[] referencedColumns) throws SQLException {
        final StringBuilder sql = new StringBuilder("ALTER TABLE ");
        sql.append(surroundWithBackticksIfNeeded(table));
        sql.append(" ADD FOREIGN KEY ");
        if (null != name) {
            sql.append(surroundWithBackticksIfNeeded(name));
            sql.append(' ');
        }
        sql.append('(');
        for (final String column : columns) {
            sql.append(surroundWithBackticksIfNeeded(column));
            sql.append(',');
        }
        sql.setLength(sql.length() - 1);
        sql.append(") REFERENCES ");
        sql.append(surroundWithBackticksIfNeeded(referencedTable));
        sql.append('(');
        for (final String column : referencedColumns) {
            sql.append(surroundWithBackticksIfNeeded(column));
            sql.append(',');
        }
        sql.setLength(sql.length() - 1);
        sql.append(')');
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * Checks if denoted table has any primary key set.
     *
     * @param con The connection
     * @param table The table name
     * @return <code>true</code> if denoted table has any primary key set; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static final boolean hasPrimaryKey(final Connection con, final String table) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        // Get primary keys
        final ResultSet primaryKeys = metaData.getPrimaryKeys(con.getCatalog(), null, table);
        try {
            return primaryKeys.next();
        } finally {
            closeSQLStuff(primaryKeys);
        }
    }

    /**
     * Checks if denoted column in given table is of type {@link java.sql.Types#VARCHAR}.
     *
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return <code>true</code> if denoted column in given table is of type {@link java.sql.Types#VARCHAR}; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static final boolean isVARCHAR(final Connection con, final String table, final String column) throws SQLException {
        return isType(con, table, column, java.sql.Types.VARCHAR);
    }

    /**
     * Checks if denoted column in given table is of specified type from {@link java.sql.Types}.
     *
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @param type The type to check against
     * @return <code>true</code> if denoted column in given table is of specified type; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static final boolean isType(final Connection con, final String table, final String column, final int type) throws SQLException {
        return type == getColumnType(con, table, column);
    }

    /**
     * Gets the type of specified column in given table from {@link java.sql.Types}.
     *
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return The type of specified column in given table from {@link java.sql.Types} or <code>-1</code> if column does not exist
     * @throws SQLException If a SQL error occurs
     */
    public static final int getColumnType(final Connection con, final String table, final String column) throws SQLException {
        if (!columnExists(con, table, column)) {
            return -1;
        }
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        int type = -1;
        try {
            rs = metaData.getColumns(con.getCatalog(), null, table, column);
            while (rs.next()) {
                type = rs.getInt(5);
            }
        } finally {
            closeSQLStuff(rs);
        }
        return type;
    }

    /**
     * Checks if specified column in given table has a default value set.
     *
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return <code>true</code> if that column has a default value; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static final boolean hasDefaultValue(final Connection con, final String table, final String column) throws SQLException {
        if (!columnExists(con, table, column)) {
            throw new SQLException("Column '" + column + "' does not exist in table '" + table + "'");
        }

        return null != getDefaultValue(con, table, column);
    }

    /**
     * Gets the default value (if any) for specified column in given table.
     *
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return <code>true</code> if that column has a default value; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static final String getDefaultValue(final Connection con, final String table, final String column) throws SQLException {
        if (!columnExists(con, table, column)) {
            throw new SQLException("Column '" + column + "' does not exist in table '" + table + "'");
        }
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        try {
            rs = metaData.getColumns(con.getCatalog(), null, table, column);
            if (rs.next()) {
                return rs.getString("COLUMN_DEF");
            }
            return null;
        } finally {
            closeSQLStuff(rs);
        }
    }

    /**
     * Gets the type of the denoted column in given table.
     *
     * @param con The connection to use
     * @param table The table name
     * @param column The column name
     * @return The type or <code>null</code> if such a column does not exist
     * @throws SQLException If an SQL error occurs
     */
    public static final String getColumnTypeName(final Connection con, final String table, final String column) throws SQLException {
        if (!columnExists(con, table, column)) {
            return null;
        }
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        String typeName = null;
        try {
            rs = metaData.getColumns(con.getCatalog(), null, table, column);
            while (rs.next()) {
                // TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified
                typeName = rs.getString(6);
            }
        } finally {
            closeSQLStuff(rs);
        }
        return typeName;
    }

    /**
     * Checks for existence of denoted table.
     *
     * @param con The connection to use
     * @param table The table to check
     * @return <code>true</code> if such a table exists; otherwise <code>false</code>
     * @throws SQLException If an SQL error occurs
     */
    public static final boolean tableExists(final Connection con, final String table) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        boolean retval = false;
        try {
            rs = metaData.getTables(con.getCatalog(), null, table, new String[] { TABLE });
            retval = (rs.next() && rs.getString("TABLE_NAME").equalsIgnoreCase(table));
        } finally {
            closeSQLStuff(rs);
        }
        return retval;
    }

    /**
     * Checks if specified column does <b>not</b> exist.
     *
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return <code>true</code> if specified column does <b>not</b> exist; otherwise <code>false</code> if existent
     * @throws SQLException If an SQL error occurs
     */
    public static boolean columnNotExists(final Connection con, final String table, final String column) throws SQLException {
        return columnExists(con, table, column) == false;
    }

    /**
     * Checks if specified column exists.
     *
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return <code>true</code> if specified column exists; otherwise <code>false</code>
     * @throws SQLException If an SQL error occurs
     */
    public static boolean columnExists(final Connection con, final String table, final String column) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        boolean retval = false;
        try {
            rs = metaData.getColumns(con.getCatalog(), null, table, column);
            while (rs.next()) {
                retval = rs.getString(4).equalsIgnoreCase(column);
            }
        } finally {
            closeSQLStuff(rs);
        }
        return retval;
    }

    private static final String TABLE = "TABLE";

    public static boolean hasSequenceEntry(final String sequenceTable, final Connection con, final int ctxId) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT 1 FROM " + sequenceTable + " WHERE cid=?");
            stmt.setInt(1, ctxId);
            rs = stmt.executeQuery();
            return rs.next();
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    public static List<Integer> getContextIDs(final Connection con) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT cid FROM user");
            rs = stmt.executeQuery();
            if (!rs.next()) {
                return Collections.emptyList();
            }

            Set<Integer> contextIds = new LinkedHashSet<>();
            do {
                contextIds.add(I(rs.getInt(1)));
            } while (rs.next());
            return new ArrayList<>(contextIds);
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    public static void exec(final Connection con, final String sql, final Object... args) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(sql);
            int i = 1;
            for (final Object arg : args) {
                stmt.setObject(i++, arg);
            }
            stmt.execute();
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Drops specified table.
     *
     * @param con The connection to use
     * @param tableName The table name
     * @throws SQLException If dropping columns fails
     */
    public static boolean dropTable(final Connection con, final String tableName) throws SQLException {
        if (Strings.isEmpty(tableName)) {
            return false;
        }

        StringBuilder sql = new StringBuilder("DROP TABLE ").append(tableName);

        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
            return true;
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Adds specified columns to denoted table.
     * <p>
     * Please prefer {@link #checkAndAddColumns(Connection, String, Column...)} to ensure update task execution is idempotent.
     *
     * @param con The connection to use for altering the table
     * @param tableName The name of the table to modify
     * @param cols The columns to add
     * @throws SQLException If altering table fails
     * @see #checkAndAddColumns(Connection, String, Column...)
     */
    public static void addColumns(final Connection con, final String tableName, final Column... cols) throws SQLException {
        if (cols == null || cols.length <= 0) {
            return;
        }

        StringBuilder sql = new StringBuilder(cols.length << 5);
        sql.append("ALTER TABLE ").append(surroundWithBackticksIfNeeded(tableName));
        sql.append(" ADD ").append(surroundWithBackticksIfNeeded(cols[0].getName())).append(' ').append(cols[0].getDefinition());
        for (int i = 1; i < cols.length; i++) {
            sql.append(", ADD ").append(surroundWithBackticksIfNeeded(cols[i].getName())).append(' ').append(cols[i].getDefinition());
        }

        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Drops specified columns from given table.
     *
     * @param con The connection to use
     * @param tableName The table name
     * @param cols The columns to drop
     * @throws SQLException If dropping columns fails
     */
    public static void dropColumns(final Connection con, final String tableName, final Column... cols) throws SQLException {
        if (null == cols || 0 == cols.length) {
            return;
        }

        StringBuilder sql = new StringBuilder(cols.length << 4);
        sql.append("ALTER TABLE ").append(surroundWithBackticksIfNeeded(tableName));
        sql.append(" DROP ").append(surroundWithBackticksIfNeeded(cols[0].getName()));
        for (int i = 1; i < cols.length; i++) {
            sql.append(", DROP ").append(surroundWithBackticksIfNeeded(cols[i].getName()));
        }

        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Checks absence of specified columns and adds them to table.
     *
     * @param con The connection to use
     * @param tableName The table name
     * @param cols The columns to add
     * @return <code>true</code> if any of given columns was added; otherwise <code>false</code> if none was added
     * @throws SQLException If operation fails
     */
    public static boolean checkAndAddColumns(final Connection con, final String tableName, final Column... cols) throws SQLException {
        List<Column> notExisting = null;
        for (Column col : cols) {
            if (!columnExists(con, tableName, col.getName())) {
                if (notExisting == null) {
                    notExisting = new LinkedList<Column>();
                }
                notExisting.add(col);
            }
        }

        if (notExisting == null) {
            return false;
        }

        addColumns(con, tableName, notExisting.toArray(new Column[notExisting.size()]));
        return true;
    }

    /**
     * Checks existence of specified columns and drops them from table.
     *
     * @param con The connection to use
     * @param tableName The table name
     * @param cols The columns to drop
     * @return <code>true</code> if any of given columns was dropped; otherwise <code>false</code> if none was dropped
     * @throws SQLException If operation fails
     */
    public static boolean checkAndDropColumns(final Connection con, final String tableName, final Column... cols) throws SQLException {
        final List<Column> existing = new LinkedList<Column>();
        for (final Column col : cols) {
            if (columnExists(con, tableName, col.getName())) {
                existing.add(col);
            }
        }

        int size = existing.size();
        if (size <= 0) {
            return false;
        }

        dropColumns(con, tableName, existing.toArray(new Column[size]));
        return true;
    }

    public static void modifyColumns(final Connection con, final String tableName, final Collection<Column> columns) throws SQLException {
        modifyColumns(con, tableName, columns.toArray(new Column[columns.size()]));
    }

    /**
     * Modifies specified columns in given table.
     *
     * @param con The connection to use
     * @param tableName The table name
     * @param cols The new column definitions to change to
     * @throws SQLException If operation fails
     */
    public static void modifyColumns(final Connection con, final String tableName, final Column... cols) throws SQLException {
        modifyColumns(con, tableName, false, cols);
    }

    /**
     * Modifies specified columns in given table.
     *
     * @param con The connection to use
     * @param tableName The table name
     * @param ignore Whether to add the <code>"IGNORE"</code> keyword to the SQL statement; e.g. to ignore data truncation.
     * @param cols The new column definitions to change to
     * @throws SQLException If operation fails
     */
    public static void modifyColumns(final Connection con, final String tableName, boolean ignore, final Column... cols) throws SQLException {
        if (null == cols || cols.length == 0) {
            return;
        }

        StringBuilder sql = new StringBuilder(cols.length << 5);
        sql.append(ignore ? "ALTER IGNORE TABLE " : "ALTER TABLE ").append(surroundWithBackticksIfNeeded(tableName));
        sql.append(" MODIFY COLUMN ").append(surroundWithBackticksIfNeeded(cols[0].getName())).append(' ').append(cols[0].getDefinition());
        for (int i = 1; i < cols.length; i++) {
            sql.append(", MODIFY COLUMN ").append(surroundWithBackticksIfNeeded(cols[i].getName())).append(' ').append(cols[i].getDefinition());
        }

        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Checks existence of specified columns and modifies them in table.
     *
     * @param con The connection to use
     * @param tableName The table name
     * @param cols The columns to modify
     * @throws SQLException If operation fails
     */
    public static void checkAndModifyColumns(final Connection con, final String tableName, final Column... cols) throws SQLException {
        checkAndModifyColumns(con, tableName, false, cols);
    }

    /**
     * Checks existence of specified columns and modifies them in table.
     *
     * @param con The connection to use
     * @param tableName The table name
     * @param ignore adds the keyword IGNORE to the SQL statement to ignore e.g. data truncation.
     * @param cols The columns to modify
     * @throws SQLException If operation fails
     */
    public static void checkAndModifyColumns(final Connection con, final String tableName, boolean ignore, final Column... cols) throws SQLException {
        final List<Column> toDo = new ArrayList<Column>(cols.length);
        for (final Column col : cols) {
            String columnTypeName = getColumnTypeName(con, tableName, col.getName());
            if ((null != columnTypeName) && (false == col.getDefinition().contains(columnTypeName))) {
                toDo.add(col);
            }
        }
        if (!toDo.isEmpty()) {
            modifyColumns(con, tableName, ignore, toDo.toArray(new Column[toDo.size()]));
        }
    }

    /**
     * Changes the denoted VARCHAR column to have a new size.
     *
     * @param colName The column to enlarge
     * @param newSize The new size to set the column to
     * @param tableName The table name
     * @param con The connection to use
     * @throws OXException If size of the denoted VARCHAR column cannot be changed
     */
    public static void changeVarcharColumnSize(final String colName, final int newSize, final String tableName, final Connection con) throws OXException {
        ResultSet rsColumns = null;
        try {
            DatabaseMetaData meta = con.getMetaData();
            rsColumns = meta.getColumns(con.getCatalog(), null, tableName, null);

            boolean doAlterTable = false;
            while (rsColumns.next()) {
                String columnName = rsColumns.getString("COLUMN_NAME");
                if (colName.equals(columnName)) {
                    int dataType = rsColumns.getInt("DATA_TYPE");
                    if (java.sql.Types.VARCHAR != dataType) {
                        // Not a VARCHAR column
                        Databases.closeSQLStuff(rsColumns);
                        rsColumns = null;
                        throw new SQLException("Column \"" + colName + "\" in table \"" + tableName + "\" is not of type VARCHAR");
                    }

                    int currentSize = rsColumns.getInt("COLUMN_SIZE");
                    if (currentSize != newSize) {
                        doAlterTable = true;
                    }

                    break;
                }
            }
            Databases.closeSQLStuff(rsColumns);
            rsColumns = null;

            if (doAlterTable) {
                modifyColumns(con, tableName, new Column(colName, "VARCHAR(" + newSize + ")"));
            }
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rsColumns);
        }
    }

    /**
     * Gets the size of the denoted VARCHAR column in specified table.
     *
     * @param colName The name of the VARCHAR column
     * @param tableName The name of the table to look-up
     * @param con The connection to use
     * @return The size of the denoted VARCHAR column or <code>-1</code> if such a VARCHAR column does not exist
     * @throws OXException If retrieving size of the VARCHAR column fails
     */
    public static int getVarcharColumnSize(String colName, String tableName, Connection con) throws OXException {
        ResultSet rsColumns = null;
        try {
            DatabaseMetaData meta = con.getMetaData();
            rsColumns = meta.getColumns(con.getCatalog(), null, tableName, null);
            while (rsColumns.next()) {
                String columnName = rsColumns.getString("COLUMN_NAME");
                if (colName.equals(columnName)) {
                    int dataType = rsColumns.getInt("DATA_TYPE");
                    if (java.sql.Types.VARCHAR == dataType) {
                        return rsColumns.getInt("COLUMN_SIZE");
                    }
                }
            }

            // No such VARCHAR column
            return -1;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rsColumns);
        }
    }

}
