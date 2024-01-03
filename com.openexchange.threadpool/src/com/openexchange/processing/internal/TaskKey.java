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

package com.openexchange.processing.internal;

import java.util.Objects;

/**
 * {@link TaskKey} - The key for a submitted processor task.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class TaskKey implements Comparable<TaskKey> {

    private final Object key;
    private final int hash;

    /**
     * Initializes a new {@link TaskKey}.
     *
     * @param key The key object
     */
    public TaskKey(Object key) {
        super();
        this.key = key;
        this.hash = Objects.hash(key);
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
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(key, ((TaskKey) obj).key);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int compareTo(TaskKey o) {
        if (key.getClass() == o.key.getClass() && (key instanceof Comparable)) {
            // Keys are of equal type and implement java.lang.Comparable
            return ((Comparable) key).compareTo(o.key);
        }
        return Integer.compare(hash, o.hash);
    }

}
