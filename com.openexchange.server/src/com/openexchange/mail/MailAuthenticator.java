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

package com.openexchange.mail;

import com.openexchange.exception.OXException;
import com.openexchange.mail.api.AuthInfo;
import com.openexchange.mailaccount.Account;
import com.openexchange.session.Session;

/**
 * {@link MailAuthenticator} - Provides authentication information for primary/secondary accounts.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface MailAuthenticator {

    /**
     * Gets the ranking for this mail authenticator.
     * <p>
     * The higher the ranking the more likely this mail authenticator is chosen.
     *
     * @return The mail authenticator's ranking
     */
    default int getRanking() {
        return 0;
    }

    /**
     * Checks if this mail authenticator accepts given arguments.
     *
     * @param session The session
     * @param account The primary/secondary account
     * @param forMailAccess <code>true</code> if credentials are supposed to be set for mail access; otherwise <code>false</code> for mail transport
     * @return <code>true</code> if arguments are accepted; otherwise <code>false</code>
     * @throws OXException If examining given arguments fails
     */
    boolean accept(Session session, Account account, boolean forMailAccess) throws OXException;

    /**
     * Gets the login for session-associated user.
     *
     * @param session The session
     * @param account The primary/secondary account
     * @param forMailAccess <code>true</code> if login is supposed to be returned for mail access; otherwise <code>false</code> for mail transport
     * @return The login
     * @throws OXException If login cannot be determined
     */
    String getLogin(Session session, Account account, boolean forMailAccess) throws OXException;

    /**
     * Gets authentication information for given primary/secondary account.
     *
     * @param login The login
     * @param session The session
     * @param account The primary/secondary account
     * @param forMailAccess <code>true</code> if credentials are supposed to be set for mail access; otherwise <code>false</code> for mail transport
     * @return The authentication information
     * @throws OXException If authentication information cannot be returned
     */
    AuthInfo getAuthInfo(String login, Session session, Account account, boolean forMailAccess) throws OXException;

}
