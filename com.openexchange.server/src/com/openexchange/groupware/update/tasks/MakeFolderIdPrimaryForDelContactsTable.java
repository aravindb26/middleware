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
import org.apache.commons.lang.Validate;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.java.Strings;
import com.openexchange.tools.update.Tools;

/**
 * Adds the column fid to the primary key of table del_contacts.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.4
 */
public class MakeFolderIdPrimaryForDelContactsTable extends UpdateTaskAdapter {

    /**
     * Constant of the del_ table name to add the primary key
     */
    protected static final String DEL_CONTACTS = "del_contacts";

    /**
     * Constant of the prg_ table name to add the primary key
     */
    protected static final String PRG_CONTACTS = "prg_contacts";

    /**
     * Initializes a new {@link MakeFolderIdPrimaryForDelContactsTable}.
     */
    public MakeFolderIdPrimaryForDelContactsTable() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perform(PerformParameters params) throws OXException {
        Validate.notNull(params);

        Connection con = params.getConnection();
        int rollback = 0;
        try {
            con.setAutoCommit(false);
            rollback = 1;

            if (Tools.hasPrimaryKey(con, DEL_CONTACTS)) {
                Tools.dropPrimaryKey(con, DEL_CONTACTS);
            }
            Tools.createPrimaryKey(con, DEL_CONTACTS, new String[] { "cid", "intfield01", "fid" });

            if (Tools.hasPrimaryKey(con, PRG_CONTACTS)) {
                Tools.dropPrimaryKey(con, PRG_CONTACTS);
            }
            Tools.createPrimaryKey(con, PRG_CONTACTS, new String[] { "cid", "intfield01", "fid" });

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

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDependencies() {
        return Strings.getEmptyStrings();
    }
}