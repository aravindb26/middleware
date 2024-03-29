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

package com.openexchange.contact;

/**
 * {@link ContactID}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class ContactID {

    private final String folderID;
    private final String objectID;

    /**
     * Initializes a new {@link ContactID}.
     *
     * @param folderID The folder ID
     * @param objectID The object ID
     */
    public ContactID(String folderID, String objectID) {
        super();
        this.folderID = folderID;
        this.objectID = objectID;
    }

    /**
     * Gets the folderID
     *
     * @return The folderID
     */
    public String getFolderID() {
        return folderID;
    }

    /**
     * Gets the objectID
     *
     * @return The objectID
     */
    public String getObjectID() {
        return objectID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((folderID == null) ? 0 : folderID.hashCode());
        result = prime * result + ((objectID == null) ? 0 : objectID.hashCode());
        return result;
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
        ContactID other = (ContactID) obj;
        if (folderID == null) {
            if (other.folderID != null) {
                return false;
            }
        } else if (!folderID.equals(other.folderID)) {
            return false;
        }
        if (objectID == null) {
            if (other.objectID != null) {
                return false;
            }
        } else if (!objectID.equals(other.objectID)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ContactID [folderID=").append(folderID).append(", objectID=").append(objectID).append(']');
        return builder.toString();
    }
}
