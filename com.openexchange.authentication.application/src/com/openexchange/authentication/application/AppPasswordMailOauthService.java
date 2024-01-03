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

package com.openexchange.authentication.application;

import com.openexchange.exception.OXException;
import com.openexchange.session.Session;

/**
 * Interface used to obtain OAuth tokens for user access to IMAP
 * 
 * {@link AppPasswordMailOauthService}
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.4
 */
public interface AppPasswordMailOauthService {

    /**
     * Gets an oauth token for a user, using username and password, then attaches to the session
     *
     * @param session The session
     * @param login The login
     * @param password The password
     * @throws OXException if an error is occurred
     */
    void getAndApplyToken(Session session, String login, String password) throws OXException;
}
