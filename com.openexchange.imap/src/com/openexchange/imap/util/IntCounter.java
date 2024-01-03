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

package com.openexchange.imap.util;

/**
 * {@link IntCounter} - A simple counter.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class IntCounter {

    private int count;

    /**
     * Initializes a new {@link IntCounter}.
     *
     * @param initialCount The initial count to set
     */
    public IntCounter(int initialCount) {
        super();
        count = initialCount;
    }

    /**
     * Increments count by one.
     *
     * @return This instance
     */
    public IntCounter increment() {
        count++;
        return this;
    }

    /**
     * Increments count by specified argument.
     *
     * @param amount The amount to increment by
     * @return This instance
     */
    public IntCounter incrementBy(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must not be less than 0 (zero)");
        }
        count += amount;
        return this;
    }

    /**
     * Decrements count by one.
     *
     * @return This instance
     */
    public IntCounter decrement() {
        count--;
        return this;
    }

    /**
     * Decrements count by specified argument.
     *
     * @param amount The amount to decrement by
     * @return This instance
     */
    public IntCounter decrementBy(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must not be less than 0 (zero)");
        }
        count -= amount;
        return this;
    }

    /**
     * Gets the current count.
     *
     * @return The count
     */
    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return Integer.toString(count);
    }

}
