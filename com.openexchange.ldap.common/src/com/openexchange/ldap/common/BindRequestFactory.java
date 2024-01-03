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

package com.openexchange.ldap.common;

import com.openexchange.exception.OXException;
import com.openexchange.session.Session;
import com.unboundid.ldap.sdk.BindRequest;

/**
 * {@link BindRequestFactory} provides {@link BindRequest}s for ldap connection with require individual bind operations.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public interface BindRequestFactory {

    /**
     * Creates a bind request for the given session
     *
     * @param session The user session
     * @return The bind request
     * @throws OXException in case the {@link BindRequest} couldn't be created
     */
    BindRequest createBindRequest(Session session) throws OXException;
    
    /**
     * Gets the {@link BindRequestFactory} identifier
     *
     * @return The identifier
     */
    String getId();

}
