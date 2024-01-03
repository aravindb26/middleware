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

package com.openexchange.folderstorage;

import com.openexchange.share.recipient.ShareRecipient;

/**
 * {@link BasicGuestPermission}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public class BasicGuestPermission extends BasicPermission implements GuestPermission {

    private static final long serialVersionUID = -1806831555286164309L;

    private ShareRecipient recipient;

    /**
     * Initializes a new {@link ParsedGuestPermission}.
     */
    public BasicGuestPermission() {
        super();
    }

    /**
     * 
     * Initializes a new {@link BasicGuestPermission} with the given permission.
     *
     * @param permission The permission
     */
    public BasicGuestPermission(BasicGuestPermission permission) {
        super(permission);
        recipient = permission.getRecipient();
    }

    @Override
    public ShareRecipient getRecipient() {
        return recipient;
    }

    /**
     * Sets the share recipient.
     *
     * @param recipient The share recipient to set
     */
    public void setRecipient(ShareRecipient recipient) {
        this.recipient = recipient;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        return prime * result + ((recipient == null) ? 0 : recipient.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        boolean superEquals = super.equals(obj);
        if (superEquals == false) {
            return superEquals;
        }

        if (obj instanceof BasicGuestPermission) {
            BasicGuestPermission other = (BasicGuestPermission) obj;
            ShareRecipient otherRecipient = other.getRecipient();
            if (recipient == null && otherRecipient != null) {
                return false;
            }
            if (recipient != null && recipient.equals(otherRecipient) == false) {
                return false;
            }
        } else if (recipient != null) {
            return false;
        }

        return true;
    }
}
