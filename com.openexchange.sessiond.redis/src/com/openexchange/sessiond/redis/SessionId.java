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

package com.openexchange.sessiond.redis;

/**
 * {@link SessionId} - Tiny helper class for session identifier.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class SessionId {

    /**
     * Creates a new instance for given session identifier.
     *
     * @param sessionId The session identifier
     * @return The new instance
     */
    public static SessionId newSessionId(String sessionId) {
        return new SessionId(sessionId, false);
    }

    /**
     * Creates a new instance for given alternative session identifier.
     *
     * @param sessionId The alternative session identifier
     * @return The new instance
     */
    public static SessionId newAlternativeSessionId(String sessionId) {
        return new SessionId(sessionId, true);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String identifier;
    private final boolean alternativeId;

    /**
     * Initializes a new {@link SessionId}.
     *
     * @param identifier The session identifier
     * @param alternativeId Whether identifier is the alternative identifier or not
     */
    private SessionId(String identifier, boolean alternativeId) {
        super();
        this.identifier = identifier;
        this.alternativeId = alternativeId;
    }

    /**
     * Gets either the session identifier or alternative session identifier.
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Checks whether identifier denotes the alternative session identifier.
     *
     * @return <code>true</code> if alternative session identifier; otherwise <code>false</code>
     */
    public boolean isAlternativeId() {
        return alternativeId;
    }

    /**
     * Gets the alternative session identifier.
     *
     * @return The alternative identifier
     * @throws IllegalStateException If identifier is not an alternative session identifier
     */
    public String getAlternativeIdentifier() {
        if (alternativeId) {
            return identifier;
        }
        throw new IllegalStateException("Identifier is not an alternative session identifier");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        if (alternativeId) {
            builder.append("alternative ");
        }
        builder.append("session identifier ");
        builder.append(identifier);
        return builder.toString();
    }
} // End of class SessionId