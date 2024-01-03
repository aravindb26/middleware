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

import java.util.Optional;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.Ranked;
import com.openexchange.osgi.annotation.Service;
import com.openexchange.session.Session;

/**
 * {@link DeputyModuleProvider} - Manages deputy permissions for a certain module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
@Service
public interface DeputyModuleProvider extends Ranked {

    /** The default ranking of <code>0</code> (zero). */
    public static final int DEFAULT_RANKING = 0;

    /**
     * Gets the ranking of this provider. By default, the {@link #DEFAULT_RANKING default ranking} is returned.
     * <p>
     * This higher the ranking, the more likely this provider will be chosen.
     *
     * @return The ranking
     */
    @Override
    default int getRanking() {
        return DEFAULT_RANKING;
    }

    /**
     * Gets the module identifier.
     *
     * @return The module identifier
     */
    String getModuleId();

    /**
     * Checks if deputy permission can be applied to this provider for session-associated user.
     *
     * @param optionalModulePermission The optional deputy module permission to examine
     * @param session The session providing user information
     * @return <code>true</code> if applicable; otherwise <code>false</code> if denied
     * @throws OXException If applicability check fails
     */
    boolean isApplicable(Optional<ModulePermission> optionalModulePermission, Session session) throws OXException;

    /**
     * Grants given deputy module permission.
     *
     * @param deputyInfo The basic information for a deputy permission
     * @param modulePermission The deputy module permission to grant
     * @param session The session providing user information
     * @throws OXException If deputy module permission cannot be granted
     */
    void grantDeputyPermission(DeputyInfo deputyInfo, ModulePermission modulePermission, Session session) throws OXException;

    /**
     * Updates given deputy module permission.
     *
     * @param deputyInfo The basic information for a deputy permission
     * @param modulePermission The new deputy module permission to apply
     * @param session The session providing user information
     * @throws OXException If updating the deputy module permission fails
     */
    void updateDeputyPermission(DeputyInfo deputyInfo, ModulePermission modulePermission, Session session) throws OXException;

    /**
     * Revokes the deputy module permission associated with given identifier.
     *
     * @param deputyInfo The basic information for a deputy permission
     * @param session The session providing user information
     * @throws OXException If deputy module permission cannot be revoked
     */
    void revokeDeputyPermission(DeputyInfo deputyInfo, Session session) throws OXException;

    /**
     * Gets the deputy module permission associated with given identifier.
     *
     * @param deputyInfo TThe basic information for a deputy permission
     * @param session The session providing user information
     * @return The deputy module permission or empty if there is no such deputy module permission
     * @throws OXException If the deputy module permission cannot be returned
     */
    Optional<ModulePermission> getDeputyPermission(DeputyInfo deputyInfo, Session session) throws OXException;

}
