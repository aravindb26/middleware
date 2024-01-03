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
 * {@link ImmutableReference} - A simple immutable reference class.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class ImmutableReference<V> implements IReference<V> {

    /**
     * Common instance for {@code empty()}.
     */
    private static final ImmutableReference<?> EMPTY = new ImmutableReference<>(null);

    /**
     * Returns an empty {@code IReference} instance. No value is present for this {@code IReference}.
     *
     * @param <V> The type of the non-existent value
     * @return An empty {@code IReference}
     */
    public static <V> IReference<V> empty() {
        @SuppressWarnings("unchecked") IReference<V> t = (ImmutableReference<V>) EMPTY;
        return t;
    }

    /**
     * Gets the immutable reference for given value.
     *
     * @param <V> The type
     * @param nullableValue The value; may be <code>null</code>
     * @return The reference
     */
    public static <V> IReference<V> immutableReferenceFor(V nullableValue) {
        return nullableValue == null ? empty() : new ImmutableReference<V>(nullableValue);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /** The value */
    private final V value;

    /**
     * Initializes a new {@link ImmutableReference}.
     *
     * @param value The value to set
     */
    public ImmutableReference(V value) {
        super();
        this.value = value;
    }

    /**
     * Gets the value
     *
     * @return The value
     */
    @Override
    public V getValue() {
        return value;
    }

}
