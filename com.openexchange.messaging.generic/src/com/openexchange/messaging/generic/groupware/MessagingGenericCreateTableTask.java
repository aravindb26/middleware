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

package com.openexchange.messaging.generic.groupware;

import com.openexchange.database.AbstractCreateTableImpl;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateTaskV2;
import com.openexchange.java.Strings;

/**
 * {@link MessagingGenericCreateTableTask}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MessagingGenericCreateTableTask extends AbstractCreateTableImpl implements UpdateTaskV2 {

    /**
     * Initializes a new {@link MessagingGenericCreateTableTask}.
     */
    public MessagingGenericCreateTableTask() {
        super();
    }

    private String getMessagingAccountTable() {
        return "CREATE TABLE messagingAccount (" +
        " cid INT4 unsigned NOT NULL," +
        " user INT4 unsigned NOT NULL," +
        " serviceId VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL," +
        " account INT4 unsigned NOT NULL," +
        " confId INT4 unsigned NOT NULL," +
        " displayName VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL," +
        " PRIMARY KEY (cid, user, serviceId, account)" +
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    }

    @Override
    protected String[] getCreateStatements() {
        return new String[] { getMessagingAccountTable() };
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes();
    }

    @Override
    public String[] getDependencies() {
        return Strings.getEmptyStrings();
    }

    @Override
    public void perform(final PerformParameters params) throws com.openexchange.exception.OXException {
        createTable("messagingAccount", getMessagingAccountTable(), params.getConnectionProvider().getConnection());
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessagingGenericCreateTableTask.class);
        logger.info("UpdateTask ''{}'' successfully performed!", MessagingGenericCreateTableTask.class.getSimpleName());
    }

    @Override
    public String[] requiredTables() {
        return new String[] { "user" };
    }

    @Override
    public String[] tablesToCreate() {
        return new String[] { "messagingAccount" };
    }

}
