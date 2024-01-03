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
import com.openexchange.admin.rmi.dataobjects.AccountDataOnCreate;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.EndpointSource;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.MissingOptionException;

/**
 * {@link CreateAccount}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class CreateAccount extends SecondaryAccountAbstraction {

    public static void main(final String args[]) {
        new CreateAccount().execute(args);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link CreateAccount}.
     */
    public CreateAccount() {
        super();
    }

    public void execute(final String[] args2) {

        final AdminParser parser = new AdminParser("createsecondaryaccount");

        setOptions(parser);

        try {
            parser.ownparse(args2);
            if (parser.getOptionValue(this.mailEndpointSourceOption) == null || EndpointSource.NONE.getId().equalsIgnoreCase(parser.getOptionValue(this.mailEndpointSourceOption).toString())) {
                StringBuilder sb = new StringBuilder();
                if (parser.getOptionValue(this.mailServerOption) == null) {
                    sb.append(this.mailServerOption.longForm()).append(',');
                }
                if (parser.getOptionValue(this.mailPortOption) == null) {
                    sb.append(this.mailPortOption.longForm()).append(',');
                }
                if (parser.getOptionValue(this.mailProtocolOption) == null) {
                    sb.append(this.mailProtocolOption.longForm()).append(',');
                }
                if (parser.getOptionValue(this.mailSecureOption) == null) {
                    sb.append(this.mailSecureOption.longForm()).append(',');
                }
                if (parser.getOptionValue(this.mailStartTlsOption) == null) {
                    sb.append(this.mailStartTlsOption.longForm()).append(',');
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                    sb.insert(0, this.mailEndpointSourceOption.longForm() + " OR ");
                    throw new MissingOptionException("Option(s) \"" + sb.toString() + "\" missing");
                }
            }

            final Credentials auth = credentialsparsing(parser);

            // get rmi ref
            OXSecondaryAccountInterface oxacc = (OXSecondaryAccountInterface) Naming.lookup(RMI_HOSTNAME + OXSecondaryAccountInterface.RMI_NAME);

            Context ctx = contextparsing(parser);
            User[] users = parseUsers(parser);
            Group[] groups = parseGroups(parser);
            AccountDataOnCreate accountData = parseAccountDataForCreate(parser);

            oxacc.create(accountData, ctx, users, groups, auth);

            if (users != null || groups != null) {
                System.out.println("Secondary account with primary address " + accountData.getPrimaryAddress() + " successfully created for specified users/groups in context " + ctxid.intValue());
            } else {
                System.out.println("Secondary account with primary address " + accountData.getPrimaryAddress() + " successfully created in context " + ctxid.intValue());
            }

            sysexit(0);
        } catch (Exception e) {
            printErrors(null, ctxid, e, parser);
        }

    }

    private void setOptions(final AdminParser parser) {
        setDefaultCommandLineOptionsWithoutContextID(parser);
        setContextOption(parser, NeededQuadState.needed);
        setUsersOption(parser, NeededQuadState.eitheror);
        setGroupsOption(parser, NeededQuadState.eitheror);
        setAccountDataOptions(parser, true);
    }

}
