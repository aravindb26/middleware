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


package com.openexchange.lock;


/**
 * {@link AccessControls} - Utility class for {@code AccessControl}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class AccessControls {

    /**
     * Initializes a new {@link AccessControls}.
     */
    private AccessControls() {
        super();
    }

    /**
     * Release specified {@code AccessControl} instance safely.
     *
     * @param accessControl The instance to release or <code>null</code>
     * @param aquired <code>true</code> if a grant has been acquired; otherwise <code>false</code>
     */
    public static void release(AccessControl accessControl, boolean aquired) {
        if (null != accessControl) {
            try {
                accessControl.release(aquired);
            } catch (@SuppressWarnings("unused") Exception e) {
                // Ignore
            }
        }
    }

}
