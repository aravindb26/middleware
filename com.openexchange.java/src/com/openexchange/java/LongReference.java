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

import javax.annotation.concurrent.NotThreadSafe;


/**
 * {@link LongReference} - A simple reference class for a <code>long</code> value.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.6.1
 */
@NotThreadSafe
public final class LongReference {

    /** The value */
    private long value;

    /**
     * Initializes a new {@link LongReference}.
     */
    public LongReference() {
        this(0L);
    }

    /**
     * Initializes a new {@link LongReference}.
     *
     * @param value The value to set
     */
    public LongReference(long value) {
        super();
        this.value = value;
    }

    /**
     * Gets the value
     *
     * @return The value
     */
    public long getValue() {
        return value;
    }

    /**
     * Sets the value
     *
     * @param value The value to set
     * @return This reference with new value applied
     */
    public LongReference setValue(long value) {
        this.value = value;
        return this;
    }

    /**
     * Increments the value
     *
     * @param increment The value to increment by
     * @return This reference with new value incremented
     */
    public LongReference incrementValue(long increment) {
        if (increment < 0) {
            throw new IllegalArgumentException("Increment must not be less than zero");
        }
        this.value += increment;
        return this;
    }

    /**
     * Decrements the value
     *
     * @param decrement The value to decrement by
     * @return This reference with new value decremented
     */
    public LongReference decrementValue(long decrement) {
        if (decrement < 0) {
            throw new IllegalArgumentException("Decrement must not be less than zero");
        }
        this.value -= decrement;
        return this;
    }

}
