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

package com.openexchange.groupware.update.tasks;

import static com.openexchange.database.Databases.autocommit;
import static com.openexchange.database.Databases.rollback;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static org.slf4j.LoggerFactory.getLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import com.google.common.collect.Lists;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateConcurrency;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskV2;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.UserAndContext;

/**
 * {@link Filestore2UserUpdateReferencesTask}
 * 
 * Updates the references to users with individual filestores in table filestore2user (config-db)
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Filestore2UserUpdateReferencesTask implements UpdateTaskV2 {

    private static final int INSERT_CHUNK_SIZE = 400;

    @Override
    public String[] getDependencies() {
        return new String[] { com.openexchange.groupware.update.tasks.ExtendUserFieldsTask.class.getName() };
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes(UpdateConcurrency.BLOCKING);
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        Connection connection = params.getConnection();
        try {
            /*
             * lookup all users that have an own filestore assigned
             */
            List<Entry<UserAndContext, Integer>> usersWithOwnFilestore = getUsersWithOwnFilestore(connection);
            if (usersWithOwnFilestore.isEmpty()) {
                getLogger(Filestore2UserUpdateReferencesTask.class).info("No users with own filestore found on schema {}.", connection.getCatalog());
                return;
            }
            /*
             * for each found user, insert a reference into table "filestore2user" of config db if missing
             */
            int updated = addFilestore2UserEntries(usersWithOwnFilestore);
            getLogger(Filestore2UserUpdateReferencesTask.class).info(
                "Updated {} references in config-db table 'filestore2user' for users with own filestore on schema {}.", I(updated), connection.getCatalog());
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        }
    }

    private static List<Entry<UserAndContext, Integer>> getUsersWithOwnFilestore(Connection connection) throws SQLException {
        List<Entry<UserAndContext, Integer>> usersWithOwnFilestore = new LinkedList<>();
        String sql = "SELECT `cid`,`id`,`filestore_id` FROM `user` WHERE `filestore_id` > 0 AND guestCreatedBy=0;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    usersWithOwnFilestore.add(new AbstractMap.SimpleEntry<UserAndContext, Integer>(
                        UserAndContext.newInstance(resultSet.getInt(2), resultSet.getInt(1)), I(resultSet.getInt(3))));
                }
            }
        }
        return usersWithOwnFilestore;
    }

    private static int addFilestore2UserEntries(List<Entry<UserAndContext, Integer>> usersWithOwnFilestore) throws OXException {
        if (null == usersWithOwnFilestore || usersWithOwnFilestore.isEmpty()) {
            return 0;
        }
        int updated = 0;
        ConfigDatabaseService configDbService = ServerServiceRegistry.getServize(DatabaseService.class, true);
        Connection connection = null;
        int rollback = 0;
        try {
            connection = configDbService.getForUpdateTask();
            connection.setAutoCommit(false);
            rollback = 1;
            for (List<Entry<UserAndContext, Integer>> chunk : Lists.partition(usersWithOwnFilestore, INSERT_CHUNK_SIZE)) {
                updated += addFilestore2UserEntries(connection, chunk);
            }
            connection.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            if (null != connection) {
                if (0 < rollback) {
                    if (1 == rollback) {
                        rollback(connection);
                    }
                    autocommit(connection);
                }
                if (0 < updated) {
                    configDbService.backForUpdateTask(connection);
                } else {
                    configDbService.backForUpdateTaskAfterReading(connection);

                }
            }
        }
        return updated;
    }

    private static int addFilestore2UserEntries(Connection connection, List<Entry<UserAndContext, Integer>> usersWithOwnFilestore) throws SQLException {
        if (null == usersWithOwnFilestore || usersWithOwnFilestore.isEmpty()) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder()
            .append("INSERT IGNORE INTO `filestore2user` (`cid`,`user`,`filestore_id`) VALUES (?,?,?)");
        for (int i = 1; i < usersWithOwnFilestore.size(); i++) {
            stringBuilder.append(",(?,?,?)");
        }
        stringBuilder.append(';');
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            for (Entry<UserAndContext, Integer> entry : usersWithOwnFilestore) {
                stmt.setInt(parameterIndex++, entry.getKey().getContextId());
                stmt.setInt(parameterIndex++, entry.getKey().getUserId());
                stmt.setInt(parameterIndex++, i(entry.getValue()));
            }
            return stmt.executeUpdate();
        }
    }

}
