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

package com.openexchange.oauth.scope;

import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.API;
import com.openexchange.oauth.DefaultAPI;

/**
 * {@link OAuthScopeRegistry} - Central access point for all available {@link OAuthScope}s for all supported
 * OAuth {@link DefaultAPI}s
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface OAuthScopeRegistry {

    /**
     * Registers the specified {@link OAuthScope} to the specified OAuth {@link DefaultAPI}
     *
     * @param api The OAuth {@link DefaultAPI}
     * @param scope The {@link OAuthScope} to register
     */
    void registerScope(API api, OAuthScope scope);

    /**
     * Registers the specified {@link OAuthScope}s to the specified OAuth {@link DefaultAPI}
     *
     * @param api The OAuth {@link DefaultAPI}
     * @param scopes The {@link OAuthScope}s to register
     */
    void registerScopes(API api, OAuthScope... scopes);

    /**
     * Unregisters the {@link OAuthScope} that is associated with the specified {@link OXScope} and OAuth {@link DefaultAPI}
     *
     * @param api The OAuth {@link DefaultAPI}
     * @param module The {@link OXScope}
     */
    void unregisterScope(API api, OXScope module);

    /**
     * Unregisters all {@link OAuthScope}s that are associated with the specified OAuth {@link DefaultAPI}
     *
     * @param api The OAuth {@link DefaultAPI}
     */
    void unregisterScopes(API api);

    /**
     * Purges the registry
     */
    void purge();

    /**
     * Returns an unmodifiable {@link Set} with all available {@link OAuthScope}s the specified OAuth {@link DefaultAPI} provides
     *
     * @param api The OAuth {@link DefaultAPI} for which to get all available {@link OAuthScope}s
     * @return a unmodifiable {@link Set} with all available {@link OAuthScope}s
     * @throws OXException if there is no such OAuth {@link DefaultAPI} known to the registry
     */
    Set<OAuthScope> getAvailableScopes(API api) throws OXException;

    /**
     * <p>Returns an unmodifiable {@link Set} with all available legacy {@link OAuthScope}s of the specified OAuth {@link DefaultAPI}.</p>
     * <p>The legacy scopes include:</p>
     * <ul>
     * <li>{@link OXScope#drive}</li>
     * <li>{@link OXScope#calendar_ro}</li>
     * <li>{@link OXScope#contacts_ro}</li>
     * <li>{@link OXScope#generic}</li>
     * </ul>
     *
     * @param api The OAuth {@link DefaultAPI} for which to get all available {@link OAuthScope}s
     * @return a unmodifiable {@link Set} with all available {@link OAuthScope}s
     * @throws OXException if there is no such OAuth {@link DefaultAPI} known to the registry
     */
    Set<OAuthScope> getLegacyScopes(API api) throws OXException;

    /**
     * Returns an unmodifiable {@link Set} with all available {@link OAuthScope}s that are associated with the specified
     * OAuth {@link DefaultAPI} and {@link OXScope}. If there is no {@link OAuthScope} associated with one of the specified
     * OAuth {@link DefaultAPI} and {@link OXScope}s, an {@link OXException} will be thrown
     *
     * @param api The OAuth {@link DefaultAPI}
     * @param modules The {@link OXScope}s
     * @return An unmodifiable {@link Set} with all available {@link OAuthScope}s
     * @throws OXException if there is no {@link OAuthScope} associated with the specified OAuth {@link DefaultAPI} and {@link OXScope}
     */
    Set<OAuthScope> getAvailableScopes(API api, OXScope... modules) throws OXException;

    /**
     * Returns the {@link OAuthScope} associated with the specified {@link DefaultAPI} and {@link OXScope}
     *
     * @param api The {@link DefaultAPI}
     * @param module The {@link OXScope}
     * @return the {@link OAuthScope} associated with the specified {@link DefaultAPI} and {@link OXScope}
     * @throws OXException if there is no {@link OAuthScope} associated with the specified OAuth {@link DefaultAPI} and {@link OXScope}
     */
    OAuthScope getScope(API api, OXScope module) throws OXException;
}
