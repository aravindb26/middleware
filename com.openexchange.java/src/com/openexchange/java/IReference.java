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

package com.openexchange.java;

/**
 * {@link IReference} - A simple reference.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 * @param <V> The type of object referred to by this reference
 */
public interface IReference<V> {

    /**
     * Gets the referenced value
     *
     * @return The value; might be <code>null</code>
     */
    V getValue();

    /**
     * Checks if this reference holds a non-null value.
     *
     * @return <code>true</code> if this reference holds a non-null value; otherwise <code>false</code> if value is <code>null</code>
     */
    default boolean isPresent() {
        return getValue() != null;
    }

    /**
     * Checks if this reference holds a <code>null</code> value.
     *
     * @return <code>true</code> if this reference holds a <code>null</code> value; otherwise <code>false</code> if value is NOT <code>null</code>
     */
    default boolean isEmpty() {
        return getValue() == null;
    }

}
