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
package com.openexchange.mail.login.resolver.ldap.impl;

/**
 * {@link MailLoginCacheKey}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class MailLoginCacheKey {

    private final String mailLogin;
    private final String clientId;

    /**
     * Initialises a new {@link MailLoginCacheKey}.
     */
    public MailLoginCacheKey(String mailLogin, String clientId) {
        super();
        this.mailLogin = mailLogin;
        this.clientId = clientId;
    }

    /**
     * Gets the mailLogin
     *
     * @return The mailLogin
     */
    public String getMailLogin() {
        return mailLogin;
    }

    /**
     * Gets the clientId
     *
     * @return The clientId
     */
    public String getClientId() {
        return clientId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mailLogin == null) ? 0 : mailLogin.hashCode());
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MailLoginCacheKey other = (MailLoginCacheKey) obj;
        if (mailLogin == null) {
            if (other.mailLogin != null) {
                return false;
            }
        } else if (!mailLogin.equals(other.mailLogin)) {
            return false;
        }
        if (clientId == null) {
            if (other.clientId != null) {
                return false;
            }
        } else if (!clientId.equals(other.clientId)) {
            return false;
        }
        return true;
    }

}