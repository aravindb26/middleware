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

package com.openexchange.imap.entity2acl;

import static com.openexchange.java.Autoboxing.I;
import java.util.Objects;
import java.util.Optional;
import com.openexchange.java.Strings;

/**
 * {@link UserInfo} - User information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class UserInfo {

    private final int userId;
    private final int contextId;
    private final Optional<String> optionalDisplayName;
    private int hash;

    /**
     * Initializes a new {@link UserInfo}.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param displayName The display name (if <code>null</code> or empty, the display name is returned as empty)
     */
    UserInfo(int userId, int contextId, String displayName) {
        super();
        this.userId = userId;
        this.contextId = contextId;
        this.optionalDisplayName = Strings.isEmpty(displayName) ? Optional.empty() : Optional.of(displayName);
        hash = 0;
    }

    /**
     * Gets the user identifier.
     *
     * @return The user identifier
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the context identifier.
     *
     * @return The context identifier
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Gets the optional display name.
     *
     * @return The optional display name
     */
    public Optional<String> getOptionalDisplayName() {
        return optionalDisplayName;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = Objects.hash(I(userId), I(contextId), optionalDisplayName);
            hash = h;
        }
        return h;
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
        UserInfo other = (UserInfo) obj;
        return userId == other.userId && contextId == other.contextId && Objects.equals(optionalDisplayName, other.optionalDisplayName);
    }

}
