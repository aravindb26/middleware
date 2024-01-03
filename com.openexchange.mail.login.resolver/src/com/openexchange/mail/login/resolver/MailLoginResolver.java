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
package com.openexchange.mail.login.resolver;

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.Service;
import com.openexchange.session.UserAndContext;

/**
 * {@link MailLoginResolver} - Knows how to resolve mail logins to a context and user identifier and vice versa.
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
@Service
public interface MailLoginResolver {

    /**
     * Resolves given mail login.
     *
     * @param contextId The user's contextId if available, or <code>-1</code> to use the system-wide defaults
     * @param mailLogin The mail login to resolve
     * @return The resolved mail login
     * @throws OXException If resolve operation fails
     */
    ResolverResult resolveMailLogin(int contextId, String mailLogin) throws OXException;

    /**
     * Resolves given userId/contextId entity.
     *
     * @param contextId The user's contextId if available, or <code>-1</code> to use the system-wide defaults
     * @param entity Pair containing userId and contextId
     * @return The resolved entity
     * @throws OXException If resolve operation fails
     */
    ResolverResult resolveEntity(int contextId, UserAndContext entity) throws OXException;

    /**
     * Resolves multiple mail logins.
     *
     * @param contextId The user's contextId if available, or <code>-1</code> to use the system-wide defaults
     * @param mailLogins The mail logins to resolve
     * @return List of resolved mail logins
     * @throws OXException If resolve operation fails
     */
    List<ResolverResult> resolveMultipleMailLogins(int contextId, List<String> mailLogins) throws OXException;

    /**
     * Resolves multiple userId/contextId entities.
     *
     * @param contextId The user's contextId if available, or <code>-1</code> to use the system-wide defaults
     * @param entities List containing userId/contextId entities
     * @return List of resolved userId/contextId entities
     * @throws OXException If resolve operation fails
     */
    List<ResolverResult> resolveMultipleEntities(int contextId, List<UserAndContext> entities) throws OXException;

}