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

package com.openexchange.file.storage;

import com.openexchange.share.recipient.ShareRecipient;

/**
 * {@link DefaultFileStorageGuestObjectPermission}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class DefaultFileStorageGuestObjectPermission extends DefaultFileStorageObjectPermission implements FileStorageGuestObjectPermission {

    private ShareRecipient recipient;

    /**
     * Initializes an empty {@link DefaultFileStorageGuestObjectPermission}.
     */
    public DefaultFileStorageGuestObjectPermission() {
        super();
    }

    @Override
    public ShareRecipient getRecipient() {
        return recipient;
    }

    /**
     * Sets the specified recipient
     *
     * @param recipient the recipient to set
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

        if (obj instanceof DefaultFileStorageGuestObjectPermission) {
            DefaultFileStorageGuestObjectPermission other = (DefaultFileStorageGuestObjectPermission) obj;
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
