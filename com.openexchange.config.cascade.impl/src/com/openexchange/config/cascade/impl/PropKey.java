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

package com.openexchange.config.cascade.impl;

/**
 * {@link PropKey} - The key for a user-associated property.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class PropKey implements Comparable<PropKey> {

    /** The context identifier */
    public final int contextId;

    /** The user identifier */
    public final int userId;

    /** The name of the property */
    public final String propertyName;

    private final int hash;

    /**
     * Initializes a new {@link PropKey}.
     *
     * @param propertyName The name of the property
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public PropKey(String propertyName, int userId, int contextId) {
        super();
        this.contextId = contextId;
        this.userId = userId;
        this.propertyName = propertyName;
        int prime = 31;
        int result = prime * 1 + contextId;
        result = prime * result + userId;
        result = prime * result + propertyName.hashCode();
        this.hash = result;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PropKey)) {
            return false;
        }
        PropKey other = (PropKey) obj;
        if (contextId != other.contextId) {
            return false;
        }
        if (userId != other.userId) {
            return false;
        }
        if (propertyName == null) {
            if (other.propertyName != null) {
                return false;
            }
        } else if (!propertyName.equals(other.propertyName)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(PropKey o) {
        int c = Integer.compare(contextId, o.contextId);
        return c == 0 ? Integer.compare(userId, o.userId) : c;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(48);
        builder.append("[contextId=").append(contextId).append(", userId=").append(userId).append(", ");
        if (propertyName != null) {
            builder.append("propertyName=").append(propertyName);
        }
        builder.append(']');
        return builder.toString();
    }
}