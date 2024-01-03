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

import java.util.ArrayList;
import java.util.List;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.console.CLIOption;
import com.openexchange.admin.console.ObjectNamingAbstraction;
import com.openexchange.admin.rmi.dataobjects.AccountData;
import com.openexchange.admin.rmi.dataobjects.AccountDataOnCreate;
import com.openexchange.admin.rmi.dataobjects.EndpointSource;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.java.Strings;


/**
 * {@link SecondaryAccountAbstraction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public abstract class SecondaryAccountAbstraction extends ObjectNamingAbstraction {

    protected static final char OPT_NAME_ACCOUNT_PRIMARY_ADDRESS_SHORT = 'e';
    protected static final String OPT_NAME_ACCOUNT_PRIMARY_ADDRESS_LONG = "primary-address";

    protected static final String OPT_NAME_USERS_LONG = "users";

    protected static final String OPT_NAME_GOUPS_LONG = "groups";

    protected CLIOption primaryAddressOption = null;
    protected CLIOption usersOption = null;
    protected CLIOption groupsOption = null;

    protected CLIOption loginOption = null;
    protected CLIOption passwordOption = null;
    protected CLIOption nameOption = null;
    protected CLIOption personalOption = null;
    protected CLIOption replyToOption = null;

    protected CLIOption mailEndpointSourceOption = null;
    protected CLIOption transportEndpointSourceOption = null;

    protected CLIOption mailServerOption = null;
    protected CLIOption mailPortOption = null;
    protected CLIOption mailProtocolOption = null;
    protected CLIOption mailSecureOption = null;
    protected CLIOption mailStartTlsOption = null;

    protected CLIOption transportLoginOption = null;
    protected CLIOption transportPasswordOption = null;
    protected CLIOption transportServerOption = null;
    protected CLIOption transportPortOption = null;
    protected CLIOption transportProtocolOption = null;
    protected CLIOption transportSecureOption = null;
    protected CLIOption transportStartTlsOption = null;

    protected CLIOption archiveFullNameOption = null;
    protected CLIOption draftsFullNameOption = null;
    protected CLIOption sentFullNameOption = null;
    protected CLIOption spamFullNameOption = null;
    protected CLIOption trashFullNameOption = null;
    protected CLIOption confirmedSpamFullNameOption = null;
    protected CLIOption confirmedHamFullNameOption = null;

    /**
     * Initializes a new {@link SecondaryAccountAbstraction}.
     */
    protected SecondaryAccountAbstraction() {
        super();
    }

    @Override
    protected String getObjectName() {
        return "account";
    }

    protected void setPrimaryAddressOption(final AdminParser parser, NeededQuadState state) {
        this.primaryAddressOption = setShortLongOpt(parser, OPT_NAME_ACCOUNT_PRIMARY_ADDRESS_SHORT, OPT_NAME_ACCOUNT_PRIMARY_ADDRESS_LONG, "The primary address of the account", true, state);
    }

    protected void setUsersOption(final AdminParser parser, NeededQuadState state) {
        String desc;
        if (NeededQuadState.eitheror.equals(state)) {
            desc = "The user identifiers. Required if no 'groups' are set.";
        } else {
            desc = "The user identifiers. Note: If both are absent - users and groups - the operation is assumed being applied for all users in specified context.";
        }
        this.usersOption = setLongOpt(parser, OPT_NAME_USERS_LONG, desc, true, state);
    }

    protected void setGroupsOption(final AdminParser parser, NeededQuadState state) {
        String desc;
        if (NeededQuadState.eitheror.equals(state)) {
            desc = "The user identifiers. Required if no 'users' are set.";
        } else {
            desc = "The group identifiers. Note: If both are absent - users and groups - the operation is assumed being applied for all users in specified context.";
        }
        this.groupsOption = setLongOpt(parser, OPT_NAME_GOUPS_LONG, desc, true, state);
    }

    protected void setAccountDataOptions(AdminParser parser, boolean forCreate) {
        if (forCreate) {
            setPrimaryAddressOption(parser, NeededQuadState.needed);
        }
        if (forCreate) {
            this.loginOption = setLongOpt(parser, "login", "The account login informaton", true, true);
        } else {
            this.loginOption = setLongOpt(parser, "login", "The account login informaton", true, false);
        }
        if (forCreate) {
            this.passwordOption = setLongOpt(parser, "password", "The account password or secret; if not set password information is used as for primary account", true, false);
        } else {
            this.passwordOption = setLongOpt(parser, "password", "The account password or secret; if set to empty string, password is assumed being cleared, which means to use the same as for primary account", true, false);
        }
        this.nameOption = setShortLongOpt(parser, 'a', "name", "The account name", true, forCreate ? NeededQuadState.needed : NeededQuadState.notneeded);
        this.personalOption = setShortLongOpt(parser, 'r', "personal", "The personal for the account's E-Mail address; e.g. Jane Doe <jane.doe@example.com>", true, NeededQuadState.notneeded);
        this.replyToOption = setLongOpt(parser, "reply-to", "The reply-to address to use for the account", true, false);

        if (forCreate) {
            this.mailEndpointSourceOption = setLongOpt(parser, "mail-endpoint-source", "The mail end-point source; either primary, localhost or none. \"primary\" uses same end-point settings as primary account. \"localhost\" uses localhost as end-point. \"none\" to expect end-point data from appropriate options ('mail-server', 'mail-port', 'mail-protocol', 'mail-secure', and 'mail-starttls')", true, false);
        }

        this.mailServerOption = setLongOpt(parser, "mail-server", "The mail server host name or IP address" + (forCreate ? "; see also option 'mail-endpoint-source'" : ""), true, false);
        this.mailPortOption = setLongOpt(parser, "mail-port", "The mail server port (e.g. 143)" + (forCreate ? "; see also option 'mail-endpoint-source'" : ""), true, false);
        this.mailProtocolOption = setLongOpt(parser, "mail-protocol", "The mail server protocol (e.g. imap)" + (forCreate ? "; see also option 'mail-endpoint-source'" : ""), true, false);
        this.mailSecureOption = setLongOpt(parser, "mail-secure", "Whether SSL is supposed to be used when connecting against mail server" + (forCreate ? "; see also option 'mail-endpoint-source'" : ""), true, false);
        this.mailStartTlsOption = setLongOpt(parser, "mail-starttls", "Whether STARTTLS is enforced when connecting plain against mail server" + (forCreate ? "; see also option 'mail-endpoint-source'" : ""), true, false);

        if (forCreate) {
            this.transportEndpointSourceOption = setLongOpt(parser, "transport-endpoint-source", "The transport end-point source; either primary, localhost or none. \"primary\" uses same end-point settings as primary account. \"localhost\" uses localhost as end-point. \"none\" to expect end-point data from appropriate options ('transport-server', 'transport-port', 'transport-protocol', 'transport-secure', and 'transport-starttls')", true, false);
        }

        this.transportLoginOption = setLongOpt(parser, "transport-login", "The transport server login informaton (if any). If absent the mail server one is used", true, false);
        this.transportPasswordOption = setLongOpt(parser, "transport-password", "The transport server password (if any). If absent the mail server one is used", true, false);
        this.transportServerOption = setLongOpt(parser, "transport-server", "The transport server host name or IP address" + (forCreate ? "; see also option 'transport-endpoint-source'" : ""), true, false);
        this.transportPortOption = setLongOpt(parser, "transport-port", "The transport server port (e.g. 25)" + (forCreate ? "; see also option 'transport-endpoint-source'" : ""), true, false);
        this.transportProtocolOption = setLongOpt(parser, "transport-protocol", "The transport server protocol (e.g. smtp)" + (forCreate ? "; see also option 'transport-endpoint-source'" : ""), true, false);
        this.transportSecureOption = setLongOpt(parser, "transport-secure", "Whether SSL is supposed to be used when connecting against transport server" + (forCreate ? "; see also option 'transport-endpoint-source'" : ""), true, false);
        this.transportStartTlsOption = setLongOpt(parser, "transport-starttls", "Whether STARTTLS is enforced when connecting plain against transport server" + (forCreate ? "; see also option 'transport-endpoint-source'" : ""), true, false);

        this.archiveFullNameOption = setLongOpt(parser, "archive-fullname", "The full name for the standard archive folder", true, false);
        this.draftsFullNameOption = setLongOpt(parser, "drafts-fullname", "The full name for the standard drafts folder", true, false);
        this.sentFullNameOption = setLongOpt(parser, "sent-fullname", "The full name for the standard sent folder", true, false);
        this.spamFullNameOption = setLongOpt(parser, "spam-fullname", "The full name for the standard spam folder", true, false);
        this.trashFullNameOption = setLongOpt(parser, "trash-fullname", "The full name for the standard trash folder", true, false);
        this.confirmedSpamFullNameOption = setLongOpt(parser, "confirmed-spam-fullname", "The full name for the standard confirmed-spam folder", true, false);
        this.confirmedHamFullNameOption = setLongOpt(parser, "confirmed-ham-fullname", "The full name for the standard confirmed-ham folder", true, false);
    }

    protected String parsePrimaryAddress(final AdminParser parser) {
        if (parser.getOptionValue(this.primaryAddressOption) == null) {
            return null;
        }

        String primaryAddress = (String) parser.getOptionValue(this.primaryAddressOption);
        if (Strings.isEmpty(primaryAddress)) {
            return null;
        }
        return primaryAddress.trim();
    }

    protected User[] parseUsers(final AdminParser parser) throws InvalidDataException {
        if (parser.getOptionValue(this.usersOption) == null) {
            return null;
        }

        String sUsers = (String) parser.getOptionValue(this.usersOption);
        if (Strings.isEmpty(sUsers)) {
            return null;
        }

        String[] tmp = Strings.splitByComma(sUsers);
        List<User> l = new ArrayList<User>(tmp.length);
        for (String sId : tmp) {
            try {
                int userId = Integer.parseInt(sId);
                if (userId <= 0) {
                    throw new InvalidDataException("Invalid user identifier: " + sId);
                }
                l.add(new User(userId));
            } catch (NumberFormatException e) {
                throw new InvalidDataException("Invalid user identifier: " + sId, e);
            }
        }
        return l.toArray(new User[l.size()]);
    }

    protected Group[] parseGroups(final AdminParser parser) throws InvalidDataException {
        if (parser.getOptionValue(this.groupsOption) == null) {
            return null;
        }

        String sGroups = (String) parser.getOptionValue(this.groupsOption);
        if (Strings.isEmpty(sGroups)) {
            return null;
        }

        String[] tmp = Strings.splitByComma(sGroups);
        List<Group> l = new ArrayList<Group>(tmp.length);
        for (String sId : tmp) {
            try {
                int groupId = Integer.parseInt(sId);
                if (groupId < 0) {
                    throw new InvalidDataException("Invalid group identifier: " + sId);
                }
                l.add(new Group(Integer.valueOf(groupId)));
            } catch (NumberFormatException e) {
                throw new InvalidDataException("Invalid group identifier: " + sId, e);
            }
        }
        return l.toArray(new Group[l.size()]);
    }

    protected AccountDataOnCreate parseAccountDataForCreate(final AdminParser parser) {
        AccountDataOnCreate accountData = new AccountDataOnCreate();
        doParseAccountData(accountData, parser);

        if (parser.getOptionValue(this.mailEndpointSourceOption) != null) {
            accountData.setMailEndpointSource(EndpointSource.endpointSourceFor(parser.getOptionValue(this.mailEndpointSourceOption).toString()));
        }

        if (parser.getOptionValue(this.transportEndpointSourceOption) != null) {
            accountData.setMailEndpointSource(EndpointSource.endpointSourceFor(parser.getOptionValue(this.transportEndpointSourceOption).toString()));
        }

        return accountData;
    }

    protected AccountData parseAccountData(final AdminParser parser) {
        AccountData accountData = new AccountData();
        doParseAccountData(accountData, parser);
        return accountData;
    }

    private void doParseAccountData(AccountData accountData, AdminParser parser) {
        if (parser.getOptionValue(this.primaryAddressOption) != null) {
            accountData.setPrimaryAddress(parser.getOptionValue(this.primaryAddressOption).toString());
        }
        if (parser.getOptionValue(this.loginOption) != null) {
            accountData.setLogin(parser.getOptionValue(this.loginOption).toString());
        }
        if (parser.getOptionValue(this.passwordOption) != null) {
            String pw = parser.getOptionValue(this.passwordOption).toString();
            if (Strings.isNotEmpty(pw)) {
                accountData.setPassword(pw);
            } else {
                accountData.setPassword(null);
            }
        }
        if (parser.getOptionValue(this.nameOption) != null) {
            accountData.setName(parser.getOptionValue(this.nameOption).toString());
        }
        if (parser.getOptionValue(this.personalOption) != null) {
            accountData.setPersonal(parser.getOptionValue(this.personalOption).toString());
        }
        if (parser.getOptionValue(this.replyToOption) != null) {
            accountData.setReplyTo(parser.getOptionValue(this.replyToOption).toString());
        }

        if (parser.getOptionValue(this.mailServerOption) != null) {
            accountData.setMailServer(parser.getOptionValue(this.mailServerOption).toString());
        }
        if (parser.getOptionValue(this.mailProtocolOption) != null) {
            accountData.setMailProtocol(parser.getOptionValue(this.mailProtocolOption).toString());
        }
        if (parser.getOptionValue(this.mailPortOption) != null) {
            accountData.setMailPort(Integer.parseInt(parser.getOptionValue(this.mailPortOption).toString()));
        }
        if (parser.getOptionValue(this.mailSecureOption) != null) {
            accountData.setMailSecure(Boolean.parseBoolean(parser.getOptionValue(this.mailSecureOption).toString()));
        }
        if (parser.getOptionValue(this.mailStartTlsOption) != null) {
            accountData.setMailStartTls(Boolean.parseBoolean(parser.getOptionValue(this.mailStartTlsOption).toString()));
        }

        if (parser.getOptionValue(this.transportLoginOption) != null) {
            accountData.setTransportLogin(parser.getOptionValue(this.transportLoginOption).toString());
        }
        if (parser.getOptionValue(this.transportPasswordOption) != null) {
            accountData.setTransportPassword(parser.getOptionValue(this.transportPasswordOption).toString());
        }
        if (parser.getOptionValue(this.transportServerOption) != null) {
            accountData.setTransportServer(parser.getOptionValue(this.transportServerOption).toString());
        }
        if (parser.getOptionValue(this.transportProtocolOption) != null) {
            accountData.setTransportProtocol(parser.getOptionValue(this.transportProtocolOption).toString());
        }
        if (parser.getOptionValue(this.transportPortOption) != null) {
            accountData.setTransportPort(Integer.parseInt(parser.getOptionValue(this.transportPortOption).toString()));
        }
        if (parser.getOptionValue(this.transportSecureOption) != null) {
            accountData.setTransportSecure(Boolean.parseBoolean(parser.getOptionValue(this.transportSecureOption).toString()));
        }
        if (parser.getOptionValue(this.transportStartTlsOption) != null) {
            accountData.setTransportStartTls(Boolean.parseBoolean(parser.getOptionValue(this.transportStartTlsOption).toString()));
        }

        if (parser.getOptionValue(this.archiveFullNameOption) != null) {
            accountData.setArchiveFullname(parser.getOptionValue(this.archiveFullNameOption).toString());
        }
        if (parser.getOptionValue(this.draftsFullNameOption) != null) {
            accountData.setDraftsFullname(parser.getOptionValue(this.draftsFullNameOption).toString());
        }
        if (parser.getOptionValue(this.sentFullNameOption) != null) {
            accountData.setSentFullname(parser.getOptionValue(this.sentFullNameOption).toString());
        }
        if (parser.getOptionValue(this.spamFullNameOption) != null) {
            accountData.setSpamFullname(parser.getOptionValue(this.spamFullNameOption).toString());
        }
        if (parser.getOptionValue(this.trashFullNameOption) != null) {
            accountData.setTrashFullname(parser.getOptionValue(this.trashFullNameOption).toString());
        }
        if (parser.getOptionValue(this.confirmedSpamFullNameOption) != null) {
            accountData.setConfirmedSpamFullname(parser.getOptionValue(this.confirmedSpamFullNameOption).toString());
        }
        if (parser.getOptionValue(this.confirmedHamFullNameOption) != null) {
            accountData.setConfirmedHamFullname(parser.getOptionValue(this.confirmedHamFullNameOption).toString());
        }
    }

}
