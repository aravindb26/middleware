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

package com.openexchange.resource;

import java.io.Serializable;

/**
 * {@link ResourcePermission}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class ResourcePermission implements Serializable, Cloneable {

    private static final long serialVersionUID = 6117514739336040689L;

    private final int entity;
    private final boolean group;
    private final SchedulingPrivilege schedulingPrivilege;

    /**
     * Initializes a new resource permission.
     *
     * @param entity The entity identifier
     * @param group <code>true</code> if the entity refers to a group, <code>false</code> if it refers to a user
     * @param schedulingPrivilege The scheduling privilege
     */
    public ResourcePermission(int entity, boolean group, SchedulingPrivilege schedulingPrivilege) {
        super();
        this.entity = entity;
        this.group = group;
        this.schedulingPrivilege = schedulingPrivilege;
    }

    /**
     * Gets the entity identifier.
     *
     * @return The entity identifier
     */
    public int getEntity() {
        return entity;
    }

    /**
     * Gets a value indicating whether the entity refers to a group or not.
     *
     * @return <code>true</code> if the entity refers to a group, <code>false</code> if it refers to a user
     */
    public boolean isGroup() {
        return group;
    }

    /**
     * Gets the scheduling privilege.
     *
     * @return The scheduling privilege
     */
    public SchedulingPrivilege getSchedulingPrivilege() {
        return schedulingPrivilege;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + entity;
        result = prime * result + (group ? 1231 : 1237);
        result = prime * result + ((schedulingPrivilege == null) ? 0 : schedulingPrivilege.hashCode());
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
        ResourcePermission other = (ResourcePermission) obj;
        if (entity != other.entity) {
            return false;
        }
        if (group != other.group) {
            return false;
        }
        if (schedulingPrivilege != other.schedulingPrivilege) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ResourcePermission [entity=" + entity + ", group=" + group + ", schedulingPrivilege=" + schedulingPrivilege + "]";
    }

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError("CloneNotSupportedException although Cloneable.", e);
        }
    }

}
