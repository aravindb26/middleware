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

package com.openexchange.imap.entity2acl;

import com.openexchange.exception.OXException;
import com.openexchange.imap.namespace.Namespaces;
import com.openexchange.mail.login.resolver.MailLoginResolverService;
import com.openexchange.session.Session;


/**
 * {@link Entity2ACLArgsImpl} - The default implementation of <code>Entity2ACLArgs</code.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class Entity2ACLArgsImpl implements Entity2ACLArgs {

    private final Session session;
    private final int accountId;
    private final String serverUrl;
    private final int sessionUser;
    private final String fullName;
    private final char separator;
    private final String[] otherUserNamespaces;
    private final String[] publicNamespaces;
    private final boolean secondaryAccount;
    private final boolean resolve;

    /**
     * Initializes a new {@link Entity2ACLArgsImpl}.
     *
     * @param session The user's session
     * @param accountId The account identifier
     * @param imapServerAddress The IMAP server address
     * @param sessionUser The session user ID
     * @param fullName The IMAP folder's full name
     * @param separator The separator character
     * @param namespaces The namespaces
     * @param secondaryAccount Whether denoted account is secondary one
     * @param resolve Whether ACL names or entities are resolved by {@link MailLoginResolverService}
     */
    public Entity2ACLArgsImpl(Session session, int accountId, String serverUrl, int sessionUser, String fullName, char separator, Namespaces namespaces, boolean secondaryAccount, boolean resolve) {
        this(session, accountId, serverUrl, sessionUser, fullName, separator, namespaces.getOtherUsersFullNames(), namespaces.getSharedFullNames(), secondaryAccount, resolve);
    }

    /**
     * Initializes a new {@link Entity2ACLArgsImpl}.
     *
     * @param session The user's session
     * @param accountId The account identifier
     * @param imapServerAddress The IMAP server address
     * @param sessionUser The session user ID
     * @param fullName The IMAP folder's full name
     * @param separator The separator character
     * @param otherUserNamespaces The namespaces for other users
     * @param publicNamespaces The namespaces for public folders
     * @param secondaryAccount Whether denoted account is secondary one
     * @param resolve Whether ACL names or entities are resolved by {@link MailLoginResolverService}
     */
    public Entity2ACLArgsImpl(Session session, int accountId, String serverUrl, int sessionUser, String fullName, char separator, String[] otherUserNamespaces, String[] publicNamespaces, boolean secondaryAccount, boolean resolve) {
        super();
        this.session = session;
        this.accountId = accountId;
        this.serverUrl = serverUrl;
        this.sessionUser = sessionUser;
        this.fullName = fullName;
        this.separator = separator;
        this.otherUserNamespaces = null == otherUserNamespaces || otherUserNamespaces.length == 0 ? null : otherUserNamespaces;
        this.publicNamespaces = null == publicNamespaces || publicNamespaces.length == 0 ? null : publicNamespaces;
        this.secondaryAccount = secondaryAccount;
        this.resolve = resolve;
    }

    @Override
    public Object[] getArguments(IMAPServer imapServer) throws OXException {
        return imapServer.getArguments(accountId, serverUrl, sessionUser, fullName, separator, otherUserNamespaces, publicNamespaces, secondaryAccount, resolve);
    }

    @Override
    public Session getSession() {
        return session;
    }

}
