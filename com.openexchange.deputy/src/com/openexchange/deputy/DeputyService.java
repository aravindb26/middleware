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

package com.openexchange.deputy;

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.session.Session;

/**
 * {@link DeputyService} - Manages deputy permissions.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
@SingletonService
public interface DeputyService {

    /** The capability name to signal access to deputy permission(s) */
    public static final String CAPABILITY_DEPUTY = "deputy";

    /**
     * Checks if deputy service is available for session-associated user.
     *
     * @param session The session providing user information
     * @return <code>true</code> if available; otherwise <code>false</code> if denied
     * @throws OXException If availability check fails
     */
    boolean isAvailable(Session session) throws OXException;

    /**
     * Gets a listing of identifiers for those modules that are available for session-associated user.
     *
     * @param session The session providing user information
     * @return A listing of module identifiers
     * @throws OXException If listing of module identifiers cannot be returned
     */
    List<String> getAvailableModules(Session session) throws OXException;

    /**
     * Grants given deputy permission.
     *
     * @param deputyPermission The deputy permission
     * @param session The session providing user information
     * @return The identifier of the associated deputy permission
     * @throws OXException If deputy permission cannot be granted
     */
    String grantDeputyPermission(DeputyPermission deputyPermission, Session session) throws OXException;

    /**
     * Updates given deputy permission.
     *
     * @param deputyId The identifier of the deputy permission
     * @param deputyPermission The new deputy permission to apply
     * @param session The session providing user information
     * @throws OXException If updating the deputy permission fails
     */
    void updateDeputyPermission(String deputyId, DeputyPermission deputyPermission, Session session) throws OXException;

    /**
     * Revokes the deputy permission associated with given identifier.
     *
     * @param deputyId The identifier of the deputy permission
     * @param session The session providing user information
     * @throws OXException If deputy permission cannot be revoked
     */
    void revokeDeputyPermission(String deputyId, Session session) throws OXException;

    /**
     * Checks if there is such a deputy permission associated with given identifier in specified context.
     *
     * @param deputyId The identifier of the deputy permission
     * @param contextId The context identifier
     * @return <code>true</code> if such a deputy permission exists; otherwise <code>false</code>
     * @throws OXException If existence check fails
     */
    boolean existsDeputyPermission(String deputyId, int contextId) throws OXException;

    /**
     * Gets the deputy permission associated with given identifier.
     *
     * @param deputyId The identifier of the deputy permission
     * @param session The session providing user information
     * @return The deputy permission
     * @throws OXException If the deputy permission cannot be returned
     */
    ActiveDeputyPermission getDeputyPermission(String deputyId, Session session) throws OXException;

    /**
     * Gets the deputy permissions associated with given session's user.
     *
     * @param session The session providing user information
     * @return The deputy permissions granted by session-associated user to other users
     * @throws OXException If the deputy permissions cannot be returned
     */
    List<ActiveDeputyPermission> listDeputyPermissions(Session session) throws OXException;

    /**
     * Gets the deputy permissions, in which session-associated user is contained.
     *
     * @param session The session providing user information
     * @return The deputy permissions granted to session-associated user grouped by granting user
     * @throws OXException If the deputy permissions cannot be returned
     */
    GrantedDeputyPermissions listReverseDeputyPermissions(Session session) throws OXException;

    /**
     * Gets the deputy permissions, in which session-associated user is contained, granted by specified user.
     *
     * @param granteeId The identifier if the user that granted the permission
     * @param session The session providing user information
     * @return The deputy permissions granted to session-associated user grouped by granting user
     * @throws OXException If the deputy permissions cannot be returned
     */
    List<ActiveDeputyPermission> listReverseDeputyPermissions(int granteeId, Session session) throws OXException;

}
