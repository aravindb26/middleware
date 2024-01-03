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

/**
 * {@link SchedulingPrivilege}
 * 
 * The different resource scheduling privileges that can be granted for users and groups.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public enum SchedulingPrivilege {

    /** No privileges to book the resource */
    NONE,
    
    /** May submit a request to book the resource, if it is available */
    ASK_TO_BOOK,
    
    /** May book the resource directly, if it is available */
    BOOK_DIRECTLY,
    
    /** Act as delegate of the resource and manage bookings */
    DELEGATE,
    
    ;

    /**
     * Gets a value indicating whether this scheduling privilege also implies another privilege.
     * <p/>
     * E.g., {@link SchedulingPrivilege#DELEGATE} also implies {@link SchedulingPrivilege#BOOK_DIRECTLY} etc.
     * 
     * @param privilege The privilege to check whether it is implied by this privilege
     * @return <code>true</code> if this scheduling privilege also implies the supplied one, <code>false</code>, otherwise
     */
    public boolean implies(SchedulingPrivilege privilege) {
        return ordinal() >= privilege.ordinal();
    }

}
