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

import java.net.URISyntaxException;
import java.rmi.Naming;
import java.util.ArrayList;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.rmi.OXSecondaryAccountInterface;
import com.openexchange.admin.rmi.dataobjects.Account;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.tools.net.URITools;

/**
 * {@link ListAccount}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class ListAccount extends SecondaryAccountAbstraction {

    public static void main(final String args[]) {
        new ListAccount().execute(args);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link ListAccount}.
     */
    public ListAccount() {
        super();
    }

    public void execute(final String[] args2) {

        final AdminParser parser = new AdminParser("listsecondaryaccount");

        setOptions(parser);

        try {
            parser.ownparse(args2);

            final Credentials auth = credentialsparsing(parser);

            // get rmi ref
            OXSecondaryAccountInterface oxacc = (OXSecondaryAccountInterface) Naming.lookup(RMI_HOSTNAME + OXSecondaryAccountInterface.RMI_NAME);

            Context ctx = contextparsing(parser);
            User[] users = parseUsers(parser);
            Group[] groups = parseGroups(parser);

            Account[] accounts = oxacc.list(ctx, users, groups, auth);

            if (null != parser.getOptionValue(this.csvOutputOption)) {
                precsvinfos(accounts);
            } else {
                sysoutOutput(accounts);
            }

            sysexit(0);
        } catch (Exception e) {
            printErrors(null, ctxid, e, parser);
        }

    }

    private void setOptions(final AdminParser parser) {
        setDefaultCommandLineOptionsWithoutContextID(parser);
        setContextOption(parser, NeededQuadState.needed);
        setUsersOption(parser, NeededQuadState.notneeded);
        setGroupsOption(parser, NeededQuadState.notneeded);
        setCSVOutputOption(parser);
    }

    private void sysoutOutput(Account[] accounts) throws InvalidDataException {
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        for (Account account : accounts) {
            data.add(makeStandardData(account, false));
        }

        doOutput(new String[] { "r", "r", "r", "l", "l", "l", "l", "l", "l" },
                 new String[] { "context-id", "user-id", "account-id", "primary-address", "name", "login", "mail-server-URI", "transport-login", "transport-server-URI" }, data);
    }

    private void precsvinfos(Account[] accounts) throws InvalidDataException {
        // needed for csv output, KEEP AN EYE ON ORDER!!!
        final ArrayList<String> columns = new ArrayList<>(22);
        columns.add("context-id");
        columns.add("user-id");
        columns.add("account-id");
        columns.add("primary-address");
        columns.add("name");
        columns.add("login");
        columns.add("password");
        columns.add("personal");
        columns.add("reply-to");
        columns.add("mail-server-URL");
        columns.add("mail-starttls");
        columns.add("transport-login");
        columns.add("transport-password");
        columns.add("transport-server-URL");
        columns.add("transport-starttls");
        columns.add("archive-fullname");
        columns.add("drafts-fullname");
        columns.add("sent-fullname");
        columns.add("spam-fullname");
        columns.add("trash-fullname");
        columns.add("confirmed-spam-fullname");
        columns.add("confirmed-ham-fullname");

        ArrayList<ArrayList<String>> data = new ArrayList<>(accounts.length);
        for (Account account : accounts) {
            data.add(makeStandardData(account, true));
        }

        doCSVOutput(columns, data);
    }

    private ArrayList<String> makeStandardData(Account account, final boolean csv) {
        final ArrayList<String> rea_data = new ArrayList<>();

        // Context, user & id

        rea_data.add(account.getContextId().toString());
        rea_data.add(account.getUserId().toString());
        rea_data.add(account.getId().toString());

        // Primary address, name, loging, password, personal & reply-to

        if (null != account.getPrimaryAddress()) {
            rea_data.add(account.getPrimaryAddress());
        } else {
            rea_data.add(null);
        }

        if (null != account.getName()) {
            rea_data.add(account.getName());
        } else {
            rea_data.add(null);
        }

        if (null != account.getLogin()) {
            rea_data.add(account.getLogin());
        } else {
            rea_data.add(null);
        }

        if (csv) {
            if (null != account.getPassword()) {
                rea_data.add(account.getPassword());
            } else {
                rea_data.add(null);
            }
        }

        if (csv) {
            if (null != account.getPersonal()) {
                rea_data.add(account.getPersonal());
            } else {
                rea_data.add(null);
            }

            if (null != account.getReplyTo()) {
                rea_data.add(account.getReplyTo());
            } else {
                rea_data.add(null);
            }
        }

        // Mail server data

        String mailServerUri = generateMailServerURL(account);
        if (null != mailServerUri) {
            rea_data.add(mailServerUri);
        } else {
            rea_data.add(null);
        }

        if (csv) {
            rea_data.add(account.isMailStartTls() ? "STARTTLS" : "");
        }

        // Transport auth & server data

        if (null != account.getTransportLogin()) {
            rea_data.add(account.getTransportLogin());
        } else {
            rea_data.add(null);
        }

        if (csv) {
            if (null != account.getTransportPassword()) {
                rea_data.add(account.getTransportPassword());
            } else {
                rea_data.add(null);
            }
        }

        String transportServerUri = generateTransportServerURL(account);
        if (null != transportServerUri) {
            rea_data.add(transportServerUri);
        } else {
            rea_data.add(null);
        }

        if (csv) {
            rea_data.add(account.isTransportStartTls() ? "STARTTLS" : "");
        }

        // Standard folders

        if (csv) {
            if (null != account.getArchiveFullname()) {
                rea_data.add(account.getArchiveFullname());
            } else {
                rea_data.add(null);
            }

            if (null != account.getDraftsFullname()) {
                rea_data.add(account.getDraftsFullname());
            } else {
                rea_data.add(null);
            }

            if (null != account.getSentFullname()) {
                rea_data.add(account.getSentFullname());
            } else {
                rea_data.add(null);
            }

            if (null != account.getSpamFullname()) {
                rea_data.add(account.getSpamFullname());
            } else {
                rea_data.add(null);
            }

            if (null != account.getTrashFullname()) {
                rea_data.add(account.getTrashFullname());
            } else {
                rea_data.add(null);
            }

            if (null != account.getConfirmedSpamFullname()) {
                rea_data.add(account.getConfirmedSpamFullname());
            } else {
                rea_data.add(null);
            }

            if (null != account.getConfirmedHamFullname()) {
                rea_data.add(account.getConfirmedHamFullname());
            } else {
                rea_data.add(null);
            }
        }

        return rea_data;
    }

    private String generateMailServerURL(Account account) {
        if (com.openexchange.java.Strings.isEmpty(account.getMailServer())) {
            return null;
        }
        final String protocol = account.isMailSecure() ? account.getMailProtocol() + 's' : account.getMailProtocol();
        try {
            return URITools.generateURI(protocol, account.getMailServer(), account.getMailPort()).toString();
        } catch (URISyntaxException e) {
            // Old implementation is not capable of handling IPv6 addresses.
            final StringBuilder sb = new StringBuilder(32);
            sb.append(account.getMailProtocol());
            if (account.isMailSecure()) {
                sb.append('s');
            }
            return sb.append("://").append(account.getMailServer()).append(':').append(account.getMailPort()).toString();
        }
    }

    private String generateTransportServerURL(Account account) {
        if (com.openexchange.java.Strings.isEmpty(account.getTransportServer())) {
            return null;
        }
        final String protocol = account.isTransportSecure() ? account.getTransportProtocol() + 's' : account.getTransportProtocol();
        try {
            return URITools.generateURI(protocol, account.getTransportServer(), account.getTransportPort()).toString();
        } catch (URISyntaxException e) {
            // Old implementation is not capable of handling IPv6 addresses.
            final StringBuilder sb = new StringBuilder(32);
            sb.append(account.getTransportProtocol());
            if (account.isTransportSecure()) {
                sb.append('s');
            }
            return sb.append("://").append(account.getTransportServer()).append(':').append(account.getTransportPort()).toString();
        }
    }

    @Override
    protected final String getObjectName() {
        return "accounts";
    }

}
