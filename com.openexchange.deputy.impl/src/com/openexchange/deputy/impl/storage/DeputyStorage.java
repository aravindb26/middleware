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

package com.openexchange.deputy.impl.storage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.openexchange.deputy.DeputyInfo;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;

/**
 * {@link DeputyStorage} - Stores basic deputy information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface DeputyStorage {

    /**
     * Stores deputy permission entry for given arguments.
     *
     * @param entityId The entity identifier
     * @param group <code>true</code> if entity is a group; otherwise <code>false</code>
     * @param sendOnBehalfOf Whether sending on behalf of is allowed
     * @param moduleIds The module identifiers
     * @param session The session providing user information
     * @return The deputy information
     * @throws OXException If deputy permission entry cannot be stored
     */
    DeputyInfo store(int entityId, boolean group, boolean sendOnBehalfOf, Collection<String> moduleIds, Session session) throws OXException;

    /**
     * Updates existent deputy permission entry.
     *
     * @param deputyId The deputy permission identifier
     * @param sendOnBehalfOf Whether sending on behalf of is allowed
     * @param moduleIds The module identifiers
     * @param session The session providing user information
     * @return The updated deputy information
     * @throws OXException If updating existent deputy permission entry fails
     */
    DeputyInfo update(String deputyId, boolean sendOnBehalfOf, Collection<String> moduleIds, Session session) throws OXException;

    /**
     * Deletes the deputy permission referenced by given identifier.
     *
     * @param deputyId The deputy permission identifier
     * @param session The session providing user information
     * @throws OXException If deletion fails
     */
    void delete(String deputyId, Session session) throws OXException;

    /**
     * Lists all deputies' information for the user associated with given session.
     *
     * @param session The session providing user information
     * @return All deputies' information
     * @throws OXException If deputies' information cannot be listed
     */
    List<DeputyInfo> list(Session session) throws OXException;

    /**
     * Lists all deputies' information, in which session-associated user is contained.
     *
     * @param session The session providing user information
     * @return All deputies' information, in which session-associated user is contained, grouped by identifier of granting user
     * @throws OXException If deputies' information cannot be listed
     */
    Map<Integer, List<DeputyInfo>> listReverse(Session session) throws OXException;

    /**
     * Lists all deputies' information, in which session-associated user is contained, granted by specified user.
     *
     * @param granteeId The identifier if the user that granted the permission
     * @param session The session providing user information
     * @return All deputies' information, in which session-associated user is contained
     * @throws OXException If deputies' information cannot be listed
     */
    List<DeputyInfo> listReverse(int granteeId, Session session) throws OXException;

    /**
     * Gets the deputy information for given identifier.
     *
     * @param deputyId The deputy permission identifier
     * @param session The session providing user information
     * @return The deputy information
     * @throws OXException If deputy information cannot be retrieved
     */
    DeputyInfo get(String deputyId, Session session) throws OXException;

    /**
     * Checks if there is such a deputy information for given identifier in specified context.
     *
     * @param deputyId The deputy permission identifier
     * @param contextId TThe context identifier
     * @return <code>true</code> if exists; otherwise <code>false</code>
     * @throws OXException If deputy information existence cannot be checked
     */
    boolean exists(String deputyId, int contextId) throws OXException;
}
