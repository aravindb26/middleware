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
import java.util.Map;
import java.util.Map.Entry;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.Set;

/**
 * {@link DefaultGrantedDeputyPermissions} - A collection of active deputy permissions granted to a certain user grouped by granting user.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DefaultGrantedDeputyPermissions implements GrantedDeputyPermissions {

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder.
     *
     * @param expectedSize The expected size
     * @return The newly created builder
     */
    public static Builder builder(int expectedSize) {
        return new Builder(expectedSize);
    }

    /** The builder for an instance of <code>DefaultGrantedDeputyPermissions</code> */
    public static class Builder {

        private ImmutableMap.Builder<Grantee, List<ActiveDeputyPermission>> map;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        Builder(int expectedSize) {
            super();
            map = ImmutableMap.builderWithExpectedSize(expectedSize);
        }

        /**
         * Adds the listing of deputy permissions associated with granting user.
         *
         * @param grantee The grantee
         * @param permissions The listing of deputy permissions granted by given user
         * @return This builder
         */
        public Builder addEntry(Grantee grantee, List<ActiveDeputyPermission> permissions) {
            ImmutableMap.Builder<Grantee, List<ActiveDeputyPermission>> map = this.map;
            if (map == null) {
                this.map = ImmutableMap.builder();
                map = this.map;
            }
            map.put(grantee, permissions);
            return this;
        }

        /**
         * Creates the instance of <code>DefaultGrantedDeputyPermissions</code> from this builder's arguments.
         *
         * @return The instance of <code>DefaultGrantedDeputyPermissions</code>
         */
        public DefaultGrantedDeputyPermissions build() {
            return new DefaultGrantedDeputyPermissions(map == null ? ImmutableMap.of() : map.build());
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Map<Grantee, List<ActiveDeputyPermission>> map;

    /**
     * Initializes a new {@link DefaultGrantedDeputyPermissions}.
     *
     * @param map The mapping
     */
    DefaultGrantedDeputyPermissions(Map<Grantee, List<ActiveDeputyPermission>> map) {
        super();
        this.map = map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsGrantee(Grantee grantee) {
        return map.containsKey(grantee);
    }

    @Override
    public Optional<List<ActiveDeputyPermission>> get(Grantee grantee) {
        List<ActiveDeputyPermission> list = map.get(grantee);
        return Optional.ofNullable(list);
    }

    @Override
    public Set<Grantee> granteeSet() {
        return map.keySet();
    }

    @Override
    public Collection<List<ActiveDeputyPermission>> values() {
        return map.values();
    }

    @Override
    public Set<Entry<Grantee, List<ActiveDeputyPermission>>> entrySet() {
        return map.entrySet();
    }

}
