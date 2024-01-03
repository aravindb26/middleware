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

import static com.openexchange.tools.update.Tools.createIndex;
import static com.openexchange.tools.update.Tools.existsIndex;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import org.slf4j.Logger;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateConcurrency;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.groupware.update.WorkingLevel;

/**
 * {@link ContactsAddIndex4AutoCompleteSearchV2}
 *
 * (Re-)adds indexes in prg_contacts for "auto-complete" queries
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactsAddIndex4AutoCompleteSearchV2 extends UpdateTaskAdapter {

    /**
     * Initializes a new {@link ContactsAddIndex4AutoCompleteSearchV2}.
     */
    public ContactsAddIndex4AutoCompleteSearchV2() {
        super();
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes(UpdateConcurrency.BLOCKING, WorkingLevel.SCHEMA);
    }

    @Override
    public String[] getDependencies() {
        return new String[] { MakeFolderIdPrimaryForDelContactsTable.class.getName() };
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        Logger log = org.slf4j.LoggerFactory.getLogger(ContactsAddIndex4AutoCompleteSearchV2.class);
        log.info("Performing update task {}", ContactsAddIndex4AutoCompleteSearchV2.class.getSimpleName());

        Connection connection = params.getConnection();
        int rollback = 0;
        try {
            connection.setAutoCommit(false);
            rollback = 1;

            createIndexIfNeeded(log, connection, new String[] { "cid", "field03" }, "givenname");
            createIndexIfNeeded(log, connection, new String[] { "cid", "field02" }, "surname");
            createIndexIfNeeded(log, connection, new String[] { "cid", "field01" }, "displayname");
            createIndexIfNeeded(log, connection, new String[] { "cid", "field65" }, "email1");
            createIndexIfNeeded(log, connection, new String[] { "cid", "field66" }, "email2");
            createIndexIfNeeded(log, connection, new String[] { "cid", "field67" }, "email3");

            connection.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
            if (rollback == 1) {
                Databases.rollback(connection);
            }
            Databases.autocommit(connection);
            }
        }
        log.info("{} successfully performed.", ContactsAddIndex4AutoCompleteSearchV2.class.getSimpleName());
    }

    private static void createIndexIfNeeded(Logger log, Connection connection, String[] columns, String indexName) throws SQLException {
        String existingIndex = existsIndex(connection, "prg_contacts", columns);
        if (null == existingIndex) {
            log.info("Creating new index named \"{}\" with columns ({}) on table \"prg_contacts\".", indexName, Arrays.toString(columns));
            createIndex(connection, "prg_contacts", indexName, columns, false);
        } else {
            log.info("Found existing index named \"{}\" with columns ({}) on table \"prg_contacts\".", indexName, Arrays.toString(columns));
        }
    }

}
