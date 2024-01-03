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

import java.sql.Connection;
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.java.Strings;
import com.openexchange.tools.update.Tools;


/**
 * {@link AddSnippetAttachmentPrimaryKeyUpdateTask}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class AddSnippetAttachmentPrimaryKeyUpdateTask extends UpdateTaskAdapter {

    private static final String SNIPPET_ATTACHMENT_TABLE = "snippetAttachment";

    /**
     * Initializes a new {@link AddSnippetAttachmentPrimaryKeyUpdateTask}.
     */
    public AddSnippetAttachmentPrimaryKeyUpdateTask() {
        super();
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        Connection con = params.getConnection();
        int rollback = 0;
        try {
            if (false == Tools.tableExists(con, SNIPPET_ATTACHMENT_TABLE)) {
                // No such table
                return;
            }

            if (Tools.hasPrimaryKey(con, SNIPPET_ATTACHMENT_TABLE)) {
                // PK already existent
                return;
            }

            con.setAutoCommit(false);
            rollback = 1;

            Tools.dropIndex(con, SNIPPET_ATTACHMENT_TABLE, "cid");
            Tools.createPrimaryKey(con, SNIPPET_ATTACHMENT_TABLE, new String[] {"cid","user","id"});

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
        }
    }

    @Override
    public String[] getDependencies() {
        return Strings.getEmptyStrings();
    }

}
