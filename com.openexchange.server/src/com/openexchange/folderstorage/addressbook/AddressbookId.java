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

package com.openexchange.folderstorage.addressbook;

import com.openexchange.folderstorage.SortableId;

/**
 * {@link AddressbookId}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class AddressbookId implements SortableId {

    private final String folderId;
    private final int ordinal;
    private final String name;
    private final int hashCode;

    /**
     * Initializes a new {@link AddressbookId}.
     *
     * @param folderId The folder identifier
     * @param ordinal The ordinal
     * @param name The name
     */
    public AddressbookId(final String folderId, final int ordinal, final String name) {
        super();
        this.folderId = folderId;
        this.ordinal = ordinal;
        this.name = name;

        final int prime = 31;
        int result = 1;
        result = prime * result + ordinal;
        hashCode = result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return folderId;
    }

    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    @Override
    public int compareTo(final SortableId o) {
        if (o instanceof AddressbookId) {
            final int thisVal = ordinal;
            final int anotherVal = ((AddressbookId) o).ordinal;
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }
        final int thisPrio = getPriority().ordinal();
        final int anotherPrio = (o).getPriority().ordinal();
        return (thisPrio < anotherPrio ? 1 : (thisPrio == anotherPrio ? 0 : -1));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AddressbookId)) {
            return false;
        }
        final AddressbookId other = (AddressbookId) obj;
        if (ordinal != other.ordinal) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(32).append("{folderId=").append(folderId).append(", ordinal=").append(ordinal).append('}').toString();
    }
}
