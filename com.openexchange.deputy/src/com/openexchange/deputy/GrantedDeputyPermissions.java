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

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * {@link GrantedDeputyPermissions} - A collection of active deputy permissions granted to a certain user grouped by granting user.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface GrantedDeputyPermissions {

    /**
     * Gets the size of this collection, which is the number of users that granted one or more deputy permissions.
     *
     * @return The size of this collection
     */
    int size();

    /**
     * Checks if this collection is empty.
     *
     * @return <code>true</code> if empty; otherwise <code>false</code>
     */
    boolean isEmpty();

    /**
     * Checks if this collection contains one or more deputy permissions granted by given user.
     *
     * @param grantee The grantee (the user that granted a deputy permission)
     * @return <code>true</code> if this collection contains one or more deputy permissions granted by given user; otherwise <code>false</code>
     */
    boolean containsGrantee(Grantee grantee);

    /**
     * Gets the deputy permissions granted by given user.
     *
     * @param grantee The grantee (the user that granted a deputy permission)
     * @return The deputy permissions or empty
     */
    Optional<List<ActiveDeputyPermission>> get(Grantee grantee);

    /**
     * Gets all grantees of this collection.
     *
     * @return All grantees
     */
    Set<Grantee> granteeSet();

    /**
     * Gets all granted deputy permissions of this collection.
     *
     * @return All granted deputy permissions
     */
    Collection<List<ActiveDeputyPermission>> values();

    /**
     * Gets a {@link Set} view of the grantee-to-permissions mappings contained in this collection.
     *
     * @return A {@link Set} view of the grantee-to-permissions mappings
     */
    Set<Entry<Grantee, List<ActiveDeputyPermission>>> entrySet();

}
