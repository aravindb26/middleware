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

package com.openexchange.tools.oxfolder.property.sql;

import com.openexchange.database.DatabaseService;
import com.openexchange.groupware.update.CreateTableUpdateTask;
import com.openexchange.java.Strings;

/**
 * {@link CreateFolderUserPropertyTask}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.0
 */
public class CreateFolderUserPropertyTask extends CreateTableUpdateTask {

    /**
     * Initializes a new {@link CreateUserPropertyFolderTask}.
     *
     * @param databaseService The {@link DatabaseService} to get the connection from
     */
    public CreateFolderUserPropertyTask(DatabaseService databaseService) {
        super(new CreateFolderUserPropertyTable(), Strings.getEmptyStrings(), databaseService);
    }

}
