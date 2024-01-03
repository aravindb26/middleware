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

package com.openexchange.api.client.common.calls.system;

/**
 * {@link WhoamiInformation}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.5
 */
public class WhoamiInformation {


    private final String sessionId;
    private final String user;
    private final int userId;
    private final int contextId;
    private final String locale;
    /**
     * Initializes a new {@link WhoamiInformation}.
     *
     * @param sessionId The session ID
     * @param user The user
     * @param userId The user ID
     * @param contextId The context ID
     * @param locale The locale
     */
    public WhoamiInformation(String sessionId, String user, int userId, int contextId, String locale) {
        super();
        this.sessionId = sessionId;
        this.user = user;
        this.userId = userId;
        this.contextId = contextId;
        this.locale = locale;
    }

    /**
     * Gets the sessionId
     *
     * @return The sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the user
     *
     * @return The user
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the userId
     *
     * @return The userId
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the contextId
     *
     * @return The contextId
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Gets the locale
     *
     * @return The locale
     */
    public String getLocale() {
        return locale;
    }


}
