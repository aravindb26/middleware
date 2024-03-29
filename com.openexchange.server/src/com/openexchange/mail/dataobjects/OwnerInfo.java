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

package com.openexchange.mail.dataobjects;

import static com.openexchange.java.Autoboxing.I;
import java.util.Objects;

/**
 * {@link OwnerInfo} - Owner information for a shared mail folder.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class OwnerInfo {

    private final int userId;
    private final int contextId;
    private final String login;
    private int hash;

    /**
     * Initializes a new {@link OwnerInfo}.
     *
     * @param userId The user identifier of the owner
     * @param contextId The context identifier of the owner
     * @param login The login name of the owner
     */
    public OwnerInfo(int userId, int contextId, String login) {
        super();
        this.userId = userId;
        this.contextId = contextId;
        this.login = login;
        hash = 0;
    }

    /**
     * Gets the user identifier of the owner.
     *
     * @return The user identifier of the owner
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the context identifier of the owner.
     *
     * @return The context identifier of the owner
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Gets the login name of the owner.
     *
     * @return The login name of the owner
     */
    public String getLogin() {
        return login;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = Objects.hash(I(userId), I(contextId), login);
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
        OwnerInfo other = (OwnerInfo) obj;
        return userId == other.userId && contextId == other.contextId && Objects.equals(login, other.login);
    }

}
