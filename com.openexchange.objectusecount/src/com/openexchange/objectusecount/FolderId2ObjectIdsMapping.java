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

package com.openexchange.objectusecount;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableMap;

/**
 * {@link FolderId2ObjectIdsMapping} - An immutable mapping for folder identifier to object identifiers.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class FolderId2ObjectIdsMapping {

    /**
     * Creates the singleton mapping for given folder and object identifier.
     *
     * @param folderId The identifier of the folder in which the object resides
     * @param objectId The identifier of the object
     * @return The mapping
     */
    public static FolderId2ObjectIdsMapping singletonMappingFor(String folderId, String objectId) {
        if (folderId == null || objectId == null) {
            throw new IllegalArgumentException("Folder and object identifier must not be null");
        }
        return new FolderId2ObjectIdsMapping(Collections.singletonMap(folderId, Collections.singletonList(objectId)));
    }

    /**
     * Creates a new builder.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>FolderId2ObjectIdsMapping</code> */
    public static class Builder {

        private final Map<String, List<String>> folderId2objectIds;

        Builder() {
            super();
            folderId2objectIds = new LinkedHashMap<String, List<String>>();
        }

        /**
         * Adds the mapping for given folder and object identifiers.
         *
         * @param folderId The identifier of the folder in which the object resides
         * @param objectIds The identifiers of the objects
         * @return This builder
         */
        public Builder addMappingFor(String folderId, String... objectIds) {
            if (folderId == null || objectIds == null || objectIds.length <= 0) {
                return this;
            }
            List<String> objectIdList = folderId2objectIds.get(folderId);
            if (objectIdList == null) {
                objectIdList = new ArrayList<String>(4);
                folderId2objectIds.put(folderId, objectIdList);
            }
            for (String objectId : objectIds) {
                if (objectId != null) {
                    objectIdList.add(objectId);
                }
            }
            return this;
        }

        /**
         * Adds the mapping for given folder and object identifiers.
         *
         * @param folderId The identifier of the folder in which the object resides
         * @param objectIds The identifiers of the objects
         * @return This builder
         */
        public <C extends Collection<String>> Builder addMappingFor(String folderId, C objectIds) {
            if (folderId == null || objectIds == null || objectIds.isEmpty()) {
                return this;
            }
            List<String> objectIdList = folderId2objectIds.get(folderId);
            if (objectIdList == null) {
                objectIdList = new ArrayList<String>(4);
                folderId2objectIds.put(folderId, objectIdList);
            }
            for (String objectId : objectIds) {
                if (objectId != null) {
                    objectIdList.add(objectId);
                }
            }
            return this;
        }

        /**
         * Builds the instance of <code>FolderId2ObjectIdsMapping</code> from this builder's arguments.
         *
         * @return The instance of <code>FolderId2ObjectIdsMapping</code>
         */
        public FolderId2ObjectIdsMapping build() {
            return new FolderId2ObjectIdsMapping(folderId2objectIds.isEmpty() ? ImmutableMap.of() : ImmutableMap.copyOf(folderId2objectIds));
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Map<String, List<String>> folderId2objectIds;

    /**
     * Initializes a new {@link FolderId2ObjectIdsMapping}.
     *
     * @param folderId2objectIds The object identifiers to retrieve use counts for mapped by folder identifier
     */
    FolderId2ObjectIdsMapping(Map<String, List<String>> folderId2objectIds) {
        super();
        this.folderId2objectIds = folderId2objectIds;
    }

    /**
     * Gets the folder-to-objects mapping.
     *
     * @return The mapping
     */
    public Map<String, List<String>> getFolderId2ObjectIds() {
        return folderId2objectIds;
    }

    /**
     * Checks if this mapping is empty.
     *
     * @return <code>true</code> if empty; otherwise <code>false</code>
     */
    public boolean isEmpty() {
        return folderId2objectIds.isEmpty();
    }

}
