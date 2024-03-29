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

import static com.openexchange.database.Databases.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.tasks.TaskIterator2.StatementSetter;
import com.openexchange.tools.Collections;
import com.openexchange.tools.iterator.CombinedSearchIterator;
import com.openexchange.tools.iterator.SearchIterator;

/**
 * Implementation of search for tasks interface using a relational database
 * currently MySQL.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a> (find method)
 */
public class RdbTaskSearch extends TaskSearch {

    /**
     * Default constructor.
     */
    RdbTaskSearch() {
        super();
    }

    @Override
    int[] findUserTasks(final Context ctx, final Connection con, final int userId, final StorageType type) throws OXException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        final List<Integer> tasks = new ArrayList<Integer>();
        try {
            stmt = con.prepareStatement(SQL.SEARCH_USER_TASKS.get(type));
            int pos = 1;
            stmt.setInt(pos++, ctx.getContextId());
            stmt.setInt(pos++, userId);
            stmt.setInt(pos++, userId);
            result = stmt.executeQuery();
            while (result.next()) {
                tasks.add(Integer.valueOf(result.getInt(1)));
            }
        } catch (SQLException e) {
            throw TaskExceptionCode.SQL_ERROR.create(e);
        } finally {
            closeSQLStuff(result, stmt);
        }
        return Collections.toArray(tasks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    SearchIterator<Task> listModifiedTasks(final Context ctx,
        final int folderId, final StorageType type, final int[] columns,
        final Date since, final boolean onlyOwn, final int userId,
        final boolean noPrivate) throws OXException {
        final StringBuilder sql1 = new StringBuilder();
        sql1.append("SELECT ");
        sql1.append(SQL.getFields(columns, false));
        sql1.append(" FROM ");
        final String taskTable = SQL.TASK_TABLES.get(type);
        sql1.append(taskTable);
        sql1.append(" JOIN ");
        final String folderTable = SQL.FOLDER_TABLES.get(type);
        sql1.append(folderTable);
        sql1.append(" USING (cid,id) WHERE ");
        sql1.append(taskTable);
        sql1.append(".cid=? AND ");
        sql1.append(folderTable);
        sql1.append(".folder=? AND ");
        sql1.append(taskTable);
        sql1.append(".last_modified>?");
        if (onlyOwn) {
            sql1.append(" AND ");
            sql1.append(SQL.getOnlyOwn(taskTable));
        }
        if (noPrivate) {
            sql1.append(" AND ");
            sql1.append(SQL.getNoPrivate(taskTable));
        }
        final TaskIterator iter1 = new TaskIterator2(ctx, userId, sql1.toString(),
            new StatementSetter() {
                @Override
                public void perform(final PreparedStatement stmt)
                    throws SQLException {
                    int pos = 1;
                    stmt.setInt(pos++, ctx.getContextId());
                    stmt.setInt(pos++, folderId);
                    stmt.setLong(pos++, since.getTime());
                    if (onlyOwn) {
                        stmt.setInt(pos++, userId);
                    }
                }
            }, folderId, columns, false, type);
        if (StorageType.DELETED == type) {
            final StringBuilder sql2 = new StringBuilder();
            sql2.append("SELECT ");
            final String activeTaskTable = SQL.TASK_TABLES.get(StorageType.ACTIVE);
            sql2.append(SQL.getFields(columns, false, activeTaskTable));
            sql2.append(" FROM ");
            sql2.append(activeTaskTable);
            sql2.append(" JOIN ");
            final String removedPartsTable = SQL.PARTS_TABLES.get(StorageType.REMOVED);
            sql2.append(removedPartsTable);
            sql2.append(" ON ");
            sql2.append(activeTaskTable);
            sql2.append(".cid=");
            sql2.append(removedPartsTable);
            sql2.append(".cid AND ");
            sql2.append(activeTaskTable);
            sql2.append(".id=");
            sql2.append(removedPartsTable);
            sql2.append(".task ");
            sql2.append("WHERE ");
            sql2.append(activeTaskTable);
            sql2.append(".cid=? AND ");
            sql2.append(removedPartsTable);
            sql2.append(".folder=? AND ");
            sql2.append(activeTaskTable);
            sql2.append(".last_modified>?");
            if (onlyOwn) {
                sql2.append(" AND ");
                sql2.append(SQL.getOnlyOwn(activeTaskTable));
            }
            if (noPrivate) {
                sql2.append(" AND ");
                sql2.append(SQL.getNoPrivate(activeTaskTable));
            }
            final TaskIterator iter2 = new TaskIterator2(ctx, userId,
                sql2.toString(),
                new StatementSetter() {
                    @Override
                    public void perform(final PreparedStatement stmt)
                        throws SQLException {
                        int pos = 1;
                        stmt.setInt(pos++, ctx.getContextId());
                        stmt.setInt(pos++, folderId);
                        stmt.setLong(pos++, since.getTime());
                        if (onlyOwn) {
                            stmt.setInt(pos++, userId);
                        }
                    }
                }, folderId, columns, false, StorageType.REMOVED);
            return new CombinedSearchIterator<Task>(iter1, iter2);
        }
        return iter1;
    }
}
