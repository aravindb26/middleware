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

package com.openexchange.mailaccount.utils;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.collect.ImmutableSet;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailProperty;
import com.openexchange.mail.config.MailReloadable;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.net.HostList;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;

/**
 * {@link MailAccountUtils} - Utility class for mail account module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.2
 */
public final class MailAccountUtils {

    /**
     * Initializes a new {@link MailAccountUtils}.
     */
    private MailAccountUtils() {
        super();
    }

    private static final AtomicReference<HostList> blacklistedHosts = new AtomicReference<>();

    private static HostList blacklistedHosts() {
        HostList tmp = blacklistedHosts.get();
        if (null == tmp) {
            synchronized (MailAccountUtils.class) {
                tmp = blacklistedHosts.get();
                if (null == tmp) {
                    ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return HostList.EMPTY;
                    }
                    String prop = service.getProperty("com.openexchange.mail.account.blacklist", "127.0.0.1-127.255.255.255,localhost").trim();
                    tmp = HostList.valueOf(prop);
                    blacklistedHosts.set(tmp);
                }
            }
        }
        return tmp;
    }

    private static final AtomicReference<Set<Integer>> allowedPorts = new AtomicReference<>();

    private static Set<Integer> allowedPorts() {
        Set<Integer> tmp = allowedPorts.get();
        if (null == tmp) {
            synchronized (MailAccountUtils.class) {
                tmp = allowedPorts.get();
                if (null == tmp) {
                    ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return Collections.emptySet();
                    }
                    String prop = service.getProperty("com.openexchange.mail.account.whitelist.ports", "143,993, 25,465,587, 110,995").trim();
                    if (Strings.isEmpty(prop)) {
                        tmp = Collections.<Integer> emptySet();
                    } else {
                        String[] tokens = Strings.splitByComma(prop);
                        tmp = new HashSet<Integer>(tokens.length);
                        for (String token : tokens) {
                            if (Strings.isNotEmpty(token)) {
                                try {
                                    tmp.add(Integer.valueOf(token.trim()));
                                } catch (NumberFormatException e) {
                                    // Ignore
                                }
                            }
                        }
                    }
                    tmp = ImmutableSet.copyOf(tmp);
                    allowedPorts.set(tmp);
                }
            }
        }
        return tmp;
    }

    static {
        MailReloadable.getInstance().addReloadable(new Reloadable() {

            @Override
            public void reloadConfiguration(ConfigurationService configService) {
                blacklistedHosts.set(null);
                allowedPorts.set(null);
            }

            @Override
            public Interests getInterests() {
                return Reloadables.interestsForProperties("com.openexchange.mail.account.blacklist", "com.openexchange.mail.account.whitelist.ports");
            }
        });
    }

    /**
     * Checks if specified host name and port are denied to connect against.
     * <p>
     * The host name can either be a machine name, such as "<code>java.sun.com</code>", or a textual representation of its IP address.
     *
     * @param hostName The host name; either a machine name or a textual representation of its IP address
     * @param port The port number
     * @return <code>true</code> if denied; otherwise <code>false</code>
     * @throws OXException in case the host is blacklisted
     */
    public static boolean isDenied(String hostName, int port) throws OXException {
        if (isBlacklisted(hostName)) {
            throw MailAccountExceptionCodes.BLACKLISTED_SERVER.create(hostName);
        }
        return false == isAllowed(port);
    }

    /**
     * Checks if the transport of the given account is disabled or not
     *
     * @param accountId The account id
     * @param session The users session
     * @throws OXException in case mail transport is disabled
     */
    public static void checkTransport(int accountId, Session session) throws OXException {
        if (Account.DEFAULT_ID == accountId) {
            return;
        }
        LeanConfigurationService configService = ServerServiceRegistry.getServize(LeanConfigurationService.class, true);
        if (false == configService.getBooleanProperty(session.getUserId(), session.getContextId(), MailProperty.SMTP_ALLOW_EXTERNAL)) {
            throw MailAccountExceptionCodes.EXTERNAL_ACCOUNTS_DISABLED.create(I(session.getUserId()), I(session.getContextId()));
        }
    }

    /**
     * Checks if specified host name is black-listed.
     * <p>
     * The host name can either be a machine name, such as "<code>java.sun.com</code>", or a textual representation of its IP address.
     *
     * @param hostName The host name; either a machine name or a textual representation of its IP address
     * @return <code>true</code> if black-listed; otherwise <code>false</code>
     */
    public static boolean isBlacklisted(String hostName) {
        if (Strings.isEmpty(hostName)) {
            return false;
        }

        return blacklistedHosts().contains(hostName);
    }

    /**
     * Gets the blocked hosts as per configuration.
     *
     * @return The blocked hosts
     */
    public static HostList getBlockedHosts() {
        return blacklistedHosts();
    }

    /**
     * Checks if specified port is not allowed.
     *
     * @param port The port to check
     * @return <code>true</code> if not allowed; otherwise <code>false</code>
     */
    public static boolean isNotAllowed(int port) {
        return !isAllowed(port);
    }

    /**
     * Checks if specified port is allowed.
     *
     * @param port The port to check
     * @return <code>true</code> if allowed; otherwise <code>false</code>
     */
    public static boolean isAllowed(int port) {
        if (port < 0) {
            // Not set; always allow
            return true;
        }

        if (port > 65535) {
            // Invalid port
            return false;
        }

        Set<Integer> allowedPorts = allowedPorts();
        return allowedPorts.isEmpty() || allowedPorts.contains(Integer.valueOf(port));
    }

    /**
     * Checks if specified mail account represents the Unified Mail account
     *
     * @param mailAccount The mail account to check
     * @return <code>true</code> if specified mail account represents the Unified Mail account; otherwise <code>false</code>
     */
    public static boolean isUnifiedINBOXAccount(final MailAccount mailAccount) {
        return isUnifiedINBOXAccount(mailAccount.getMailProtocol());
    }

    /**
     * Checks if specified mail protocol denotes the Unified Mail protocol identifier
     *
     * @param mailProtocol The mail protocol to check
     * @return <code>true</code> if specified mail protocol denotes the Unified Mail protocol identifier; otherwise <code>false</code>
     */
    public static boolean isUnifiedINBOXAccount(final String mailProtocol) {
        return UnifiedInboxManagement.PROTOCOL_UNIFIED_INBOX.equals(mailProtocol);
    }
}
