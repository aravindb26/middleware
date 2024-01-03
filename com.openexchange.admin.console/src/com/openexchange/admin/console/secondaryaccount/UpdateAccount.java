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

package com.openexchange.admin.console.secondaryaccount;

import java.rmi.Naming;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.rmi.OXSecondaryAccountInterface;
import com.openexchange.admin.rmi.dataobjects.AccountData;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;

/**
 * {@link UpdateAccount}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class UpdateAccount extends SecondaryAccountAbstraction {

    public static void main(final String args[]) {
        new UpdateAccount().execute(args);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link UpdateAccount}.
     */
    public UpdateAccount() {
        super();
    }

    public void execute(final String[] args2) {

        final AdminParser parser = new AdminParser("updatesecondaryaccount");

        setOptions(parser);

        try {
            parser.ownparse(args2);

            final Credentials auth = credentialsparsing(parser);

            // get rmi ref
            OXSecondaryAccountInterface oxacc = (OXSecondaryAccountInterface) Naming.lookup(RMI_HOSTNAME + OXSecondaryAccountInterface.RMI_NAME);

            String primaryAddress = parsePrimaryAddress(parser);
            Context ctx = contextparsing(parser);
            User[] users = parseUsers(parser);
            Group[] groups = parseGroups(parser);
            AccountData accountData = parseAccountData(parser);
            boolean updated = oxacc.update(primaryAddress, accountData, ctx, users, groups, auth);

            if (updated) {
                if (users != null || groups != null) {
                    System.out.println("Secondary account with primary address " + primaryAddress + " successfully updated from specified users/groups in context " + ctxid.intValue());
                } else {
                    System.out.println("Secondary account with primary address " + primaryAddress + " successfully updated from context " + ctxid.intValue());
                }
            } else {
                if (users != null || groups != null) {
                    System.out.println("No such secondary account with primary address " + primaryAddress + " for specified users/groups in context " + ctxid.intValue());
                } else {
                    System.out.println("No such secondary account with primary address " + primaryAddress + " in context " + ctxid.intValue());
                }
            }

            sysexit(0);
        } catch (Exception e) {
            printErrors(null, ctxid, e, parser);
        }

    }

    private void setOptions(final AdminParser parser) {
        setDefaultCommandLineOptionsWithoutContextID(parser);
        setPrimaryAddressOption(parser, NeededQuadState.needed);
        setContextOption(parser, NeededQuadState.needed);
        setUsersOption(parser, NeededQuadState.eitheror);
        setGroupsOption(parser, NeededQuadState.eitheror);
        setAccountDataOptions(parser, false);
    }

    @Override
    protected final String getObjectName() {
        return "account";
    }

}
