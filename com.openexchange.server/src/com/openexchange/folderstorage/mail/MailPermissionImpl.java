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

package com.openexchange.folderstorage.mail;

import com.openexchange.folderstorage.BasicPermission;
import com.openexchange.folderstorage.DeputyAwarePermission;
import com.openexchange.mail.permission.MailPermission;

/**
 * {@link MailPermissionImpl} - A mail folder permission.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailPermissionImpl extends BasicPermission implements DeputyAwarePermission {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7950338717809340367L;

    private final boolean deputyPermission;

    /**
     * Initializes an empty {@link MailPermissionImpl}.
     */
    public MailPermissionImpl() {
        super();
        deputyPermission = false;
    }

    /**
     * Initializes a new {@link MailPermissionImpl}.
     */
    public MailPermissionImpl(final MailPermission mailPermission) {
        super();
        admin = mailPermission.isFolderAdmin();
        deletePermission = mailPermission.getDeletePermission();
        entity = mailPermission.getEntity();
        folderPermission = mailPermission.getFolderPermission();
        group = mailPermission.isGroupPermission();
        readPermission = mailPermission.getReadPermission();
        system = mailPermission.getSystem();
        type = mailPermission.getType();
        legator = mailPermission.getPermissionLegator();
        writePermission = mailPermission.getWritePermission();
        identifier = mailPermission.getIdentifier();
        deputyPermission = mailPermission.isDeputyPermission();
    }

    @Override
    public boolean isDeputyPermissionSupported() {
        return true;
    }

    @Override
    public boolean isDeputyPermission() {
        return deputyPermission;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (deputyPermission ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj) || (getClass() != obj.getClass())) {
            return false;
        }
        MailPermissionImpl other = (MailPermissionImpl) obj;
        if (deputyPermission != other.deputyPermission) {
            return false;
        }
        return true;
    }
}
