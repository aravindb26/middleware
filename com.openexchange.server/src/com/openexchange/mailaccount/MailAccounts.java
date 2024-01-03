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

package com.openexchange.mailaccount;

import static com.openexchange.java.Strings.isEmpty;
import static com.openexchange.mailaccount.Constants.MAIL_PROTOCOL_GUARD_GUEST;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.mail.internet.idn.IDNA;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;


/**
 * {@link MailAccounts} - Utility class for mail account.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.6.1
 */
public final class MailAccounts {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(MailAccounts.class);
    }

    /**
     * Initializes a new {@link MailAccounts}.
     */
    private MailAccounts() {
        super();
    }

    /**
     * Checks whether the specified transport account uses special Gmail Send API to transport messages.
     *
     * @param transportAccount The transport account to check
     * @return <code>true</code> for a Gmail Send API; otherwise <code>false</code>
     */
    public static boolean isGmailTransport(TransportAccount transportAccount) {
        return null == transportAccount ? false : "gmailsend".equals(transportAccount.getTransportProtocol());
    }

    /**
     * Gets the transport authentication information from given mail account.
     *
     * @param mailAccount The mail account
     * @param fallback The fall-back value
     * @return The transport authentication information or <code>fallback</code>
     */
    public static TransportAuth getTransportAuthFrom(MailAccount mailAccount, TransportAuth fallback) {
        if (null == mailAccount) {
            return fallback;
        }
        Map<String, String> properties = mailAccount.getProperties();
        if (null == properties) {
            return fallback;
        }
        TransportAuth transportAuth = TransportAuth.transportAuthFor(properties.get("transport.auth"));
        return null == transportAuth ? fallback : transportAuth;
    }

    /**
     * Checks for a guest session
     *
     * @param session The session to check
     * @return <code>true</code> for a guest session; otherwise <code>false</code>
     * @deprecated Use {@link com.openexchange.session.Sessions#isGuest(Session)} instead.
     */
    @Deprecated
    public static boolean isGuest(Session session) {
        return null != session && Boolean.TRUE.equals(session.getParameter(Session.PARAM_GUEST));
    }

    /**
     * Checks if account is a guest account
     *
     * @param account The mail account to check
     * @return <code>true</code> for a guest account; otherwise <code>false</code>
     */
    public static boolean isGuestAccount(MailAccount account) {
        return null == account ? false : MAIL_PROTOCOL_GUARD_GUEST.equals(account.getMailProtocol());
    }

    /**
     * Checks if account is a guest account
     *
     * @param account The account to check
     * @return <code>true</code> for a guest account; otherwise <code>false</code>
     */
    public static boolean isGuestAccount(Account account) {
        return null == account ? false : Constants.NAME_GUARD_GUEST.equals(account.getName());
    }

    /**
     * Checks if specified mail account is an IMAP account having given host and port (optional).
     *
     * @param account The mail account to check against
     * @param host The host name
     * @param port The port or <code>-1</code> to ignore
     * @return <code>true</code> if given mail account matches; otherwise <code>false</code>
     */
    public static boolean isEqualImapAccount(MailAccount account, String host, int port) {
        if (account == null) {
            return false;
        }

        // Check if mail protocol advertises IMAP
        String mailProtocol = account.getMailProtocol();
        if (mailProtocol == null || !Strings.asciiLowerCase(mailProtocol).startsWith("imap")) {
            return false;
        }

        // Check if port is equal to given one (if any)
        if (port > 0) {
            int mailPort = account.getMailPort();
            if (mailPort <= 0 || port != mailPort) {
                return false;
            }
        }

        // Check if host is (ASCII-wise) equal to given one
        if (false == IDNA.toASCII(host).equals(IDNA.toASCII(account.getMailServer()))) {
            return false;
        }

        // Appears to be matching IMAP account
        return true;
    }

    /**
     * Tries to get session from either given properties or from current thread.
     *
     * @param properties The properties possibly providing a session
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The optional session
     */
    public static Optional<Session> tryGetSession(Map<String, Object> properties, int userId, int contextId) {
        Session session = (Session) properties.get("com.openexchange.mailaccount.session");
        if (session != null) {
            return Optional.of(session);
        }

        return com.openexchange.session.Sessions.getValidatedSessionForCurrentThread(userId, contextId);
    }

    /**
     * Checks if specified account is a secondary one.
     *
     * @param accountId The account identifier
     * @param session The session providing user information
     * @return <code>true</code> if specified account is a secondary one; otherwise <code>false</code>
     * @throws OXException If check fails
     * @throws IllegalArgumentException If session is <code>null</code>
     */
    public static boolean isSecondaryAccount(int accountId, Session session) throws OXException {
        if (accountId < 0 || accountId == Account.DEFAULT_ID) {
            return false;
        }
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }
        return isSecondaryAccount(accountId, session.getUserId(), session.getContextId());
    }

    /**
     * Checks if specified account is a secondary one.
     *
     * @param accountId The account identifier
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> if specified account is a secondary one; otherwise <code>false</code>
     * @throws OXException If check fails
     */
    public static boolean isSecondaryAccount(int accountId, int userId, int contextId) throws OXException {
        if (accountId < 0 || accountId == Account.DEFAULT_ID) {
            return false;
        }
        MailAccountStorageService service = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class);
        if (service == null) {
            throw ServiceExceptionCode.absentService(MailAccountStorageService.class);
        }
        return service.isSecondaryMailAccount(accountId, userId, contextId);
    }

    /**
     * Removes deactivated mail accounts from given accounts array.
     *
     * @param accounts The accounts array
     * @return The accounts array with deactivated ones removed
     */
    public static MailAccount[] removeDeactivatedAccountsFrom(MailAccount[] accounts) {
        if (accounts == null) {
            return accounts;
        }

        int length = accounts.length;
        if (length <= 0) {
            return accounts;
        }

        List<MailAccount> tmp = null;
        for (int i = 0; i < length; i++) {
            MailAccount account = accounts[i];
            if (account.isDeactivated()) {
                if (tmp == null) {
                    tmp = new ArrayList<MailAccount>(length);
                    if (i > 0) {
                        for (int k = 0; k < i; k++) {
                            tmp.add(accounts[k]);
                        }
                    }
                }
            } else {
                if (tmp != null) {
                    tmp.add(account);
                }
            }
        }
        return tmp == null ? accounts : tmp.toArray(new MailAccount[tmp.size()]);
    }

    /**
     * Removes deactivated mail accounts from given accounts listing.
     *
     * @param accounts The accounts listing
     * @return The accounts listing with deactivated ones removed
     */
    public static <L extends List<MailAccount>> List<MailAccount> removeDeactivatedAccountsFrom(L accounts) {
        if (accounts == null) {
            return accounts;
        }

        int length = accounts.size();
        if (length <= 0) {
            return accounts;
        }

        try {
            for (Iterator<MailAccount> it = accounts.iterator(); it.hasNext();) {
                if (it.next().isDeactivated()) {
                    it.remove();
                }
            }
            return accounts;
        } catch (UnsupportedOperationException e) {
            // Apparently 'java.util.Iterator.remove()' is not supported
            List<MailAccount> tmp = new ArrayList<MailAccount>(length);
            for (MailAccount account : accounts) {
                if (account.isDeactivated() == false) {
                    tmp.add(account);
                }
            }
            return tmp;
        }
    }

    /**
     * Checks if the specified mail server is equal to the other one.
     *
     * @param mailServer The mail server's string representation; either host name or textual representation of its IP address
     * @param addr The mail server's Internet Protocol (IP) address or <code>null</code> to compare by string representation
     * @param otherMailServer The other mail server's string representation; either host name or textual representation of its IP address
     * @return <code>true</code> if both mail servers are equal; otherwise <code>false</code>
     */
    public static boolean checkMailServer(final String mailServer, final InetAddress addr, final String otherMailServer) {
        if (isEmpty(otherMailServer)) {
            return false;
        }
        if (null == addr) {
            /*
             * Check by server string
             */
            return mailServer.equalsIgnoreCase(otherMailServer);
        }
        try {
            return addr.equals(InetAddress.getByName(IDNA.toASCII(otherMailServer)));
        } catch (UnknownHostException e) {
            LoggerHolder.LOG.warn("", e);
            /*
             * Check by server string
             */
            return mailServer.equalsIgnoreCase(otherMailServer);
        }
    }

    /**
     * Checks if both protocols are equal.
     *
     * @param protocol1 The fist protocol's string representation
     * @param protocol2 The second protocol's string representation
     * @return <code>true</code> if both protocols are equal; otherwise <code>false</code>
     */
    public static boolean checkProtocol(final String protocol1, final String protocol2) {
        if (isEmpty(protocol1) || isEmpty(protocol2)) {
            return false;
        }
        return protocol1.equalsIgnoreCase(protocol2);
    }

}
