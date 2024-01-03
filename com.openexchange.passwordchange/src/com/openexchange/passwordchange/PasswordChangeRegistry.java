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

package com.openexchange.passwordchange;

import com.openexchange.exception.OXException;

/**
 * {@link PasswordChangeRegistry} - Performs changing a user's password to the fitting service
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.6
 */
public interface PasswordChangeRegistry {

    /**
     * Returns <code>true</code> if is a password change service is enabled for the specified user in he specified context
     *
     * @param contextId THe context identifier
     * @param userId The user identifier
     * @return <code>true</code> if a service is enabled for the user, <code>false</code> otherwise
     */
    boolean isEnabled(int contextId, int userId);

    /**
     * Performs the password update.
     *
     * @param event The event containing the session of the user whose password shall be changed, the context, the new password, and the old
     * password (needed for verification)
     * @throws OXException If password update fails
     */
    void perform(PasswordChangeEvent event) throws OXException;

}
