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

package com.openexchange.java.security.internal;

/**
 * {@link TypeAndAlgorithm} - The tuple for type and algorithm.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
class TypeAndAlgorithm {

    private final String type;
    private final String algorithm;
    private int hash;

    TypeAndAlgorithm(String type, String algorithm) {
        super();
        this.type = type;
        this.algorithm = algorithm;
        hash = 0;
    }

    /**
     * Gets the type
     *
     * @return The type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the algorithm
     *
     * @return The algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
            h = result;
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypeAndAlgorithm other = (TypeAndAlgorithm) obj;
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (algorithm == null) {
            if (other.algorithm != null) {
                return false;
            }
        } else if (!algorithm.equals(other.algorithm)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(24);
        builder.append('[');
        if (type != null) {
            builder.append("type=").append(type).append(", ");
        }
        if (algorithm != null) {
            builder.append("algorithm=").append(algorithm);
        }
        builder.append(']');
        return builder.toString();
    }
}
