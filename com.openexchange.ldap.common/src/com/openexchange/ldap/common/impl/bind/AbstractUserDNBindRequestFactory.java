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

package com.openexchange.ldap.common.impl.bind;

import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.BindRequestFactory;
import com.openexchange.ldap.common.config.auth.UserNameSource;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link AbstractUserDNBindRequestFactory} is an abstract user dn bind request factory
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public abstract class AbstractUserDNBindRequestFactory implements BindRequestFactory {

    static final String VARIABLE_NAME = "[value]";

    /**
     * Gets the user name of the defined source from the given session
     *
     * @param session The user session
     * @param source The source of the name
     * @return The name
     * @throws OXException
     */
    String getUserName(Session session, UserNameSource source) throws OXException {
        ServerSession serverSession = ServerSessionAdapter.valueOf(session);
        switch(source) {
            case MAIL:
                return serverSession.getUser().getMail();
            default:
            case SESSION:
                return session.getLoginName();
            case USERNAME:
                return serverSession.getUser().getLoginInfo();
        }
    }

}
