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

package com.openexchange.ldap.common.config.auth;

import com.openexchange.session.Session;

/**
 * {@link AuthType}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public enum AuthType {

    /**
     * Anonymous authentication.
     */
    ANONYMOUS,

    /**
     * Authentication using static administrative credentials.
     */
    ADMINDN,

    /**
     * Authentication using each session user's individual credentials. The user's distinguished name is dynamically resolved through a
     * search that matches the actual value of the defined <i>loginSource</i>.
     */
    USERDN_RESOLVED,

    /**
     * Authentication using each session user's individual credentials. The user's distinguished name is constructed using a static
     * template, where the name part is injected dynamically for each user.
     */
    USERDN_TEMPLATE,

    /**
     * Authentication using the oauth bearer access token contained in the user's session. See {@link Session#PARAM_OAUTH_ACCESS_TOKEN}
     */
    OAUTHBEARER,
    
    /**
     * Authentication using a custom implementation of {@link com.openexchange.ldap.common.BindRequestFactory}
     */
    CUSTOM

    ;

}
