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
package com.openexchange.request.analyzer;

import static com.openexchange.java.Autoboxing.I;
import java.util.Optional;

/**
 * {@link UserInfo} - Contains informations about the user which is associated with the request.
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class UserInfo {

    /**
     * Creates a builder for {@link UserInfo}s
     *
     * @param contextId The required context identifier
     * @return The newly created builder
     */
    public static UserInfoBuilder builder(int contextId) {
        return new UserInfoBuilder(contextId);
    }

    /**
     * {@link UserInfoBuilder} - The builder for an instance of <code>UserInfo</code>.
     *
     * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
     */
    public static class UserInfoBuilder {

        private final int contextId;
        private int userId;
        private String login;

        /**
         * Initializes a new {@link UserInfoBuilder}.
         *
         * @param contextId The required context identifier
         */
        private UserInfoBuilder(int contextId) {
            if (contextId <= 0) {
                throw new IllegalArgumentException("Invalid context identifier");
            }
            this.contextId = contextId;
        }

        /**
         * Sets the user identifier.
         *
         * @param userId The user identifier to set; a value of less than/equal to <code>0</code> (zero) means no user identifier given
         * @return This builder
         */
        public UserInfoBuilder withUserId(int userId) {
            this.userId = userId <= 0 ? 0 : userId;
            return this;
        }

        /**
         * Sets the user login.
         *
         * @param login The user login; a value of <code>null</code> means no login string given
         * @return This builder
         */
        public UserInfoBuilder withLogin(String login) {
            this.login = login;
            return this;
        }

        /**
         * Builds the {@link UserInfo} from this builder's arguments.
         *
         * @return The {@link UserInfo}
         */
        public UserInfo build() {
            return new UserInfo(contextId, userId, login);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    private final int contextId;
    private final Optional<Integer> optUserId;
    private final Optional<String> optLogin;

    /**
     * Initializes a new {@link UserInfo}.
     *
     * @param contextId The context identifier
     * @param userId The user identifier
     * @param login The login string
     */
    private UserInfo(int contextId, int userId, String login) {
        super();
        this.contextId = contextId;
        this.optUserId = userId <= 0 ? Optional.empty() : Optional.of(I(userId));
        this.optLogin = Optional.ofNullable(login);
        if (contextId <= 0) {
            throw new IllegalArgumentException("Invalid context identifier");
        }
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
     * Gets the optional user identifier.
     *
     * @return The user identifier or empty (if not set)
     */
    public Optional<Integer> getUserId() {
        return optUserId;
    }

    /**
     * Gets the optional login string.
     *
     * @return The login string or empty (if not set)
     */
    public Optional<String> getLogin() {
        return optLogin;
    }


}
