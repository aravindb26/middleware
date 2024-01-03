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

import java.util.List;

/**
 * {@link SessionEvent} - A session event.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class SessionEvent {

    private final SessionOperation operation;
    private final List<String> sessionIds;

    /**
     * Initializes a new {@link SessionEvent}.
     *
     * @param operation The operation
     * @param sessionIds The session identifiers
     */
    SessionEvent(SessionOperation operation, List<String> sessionIds) {
        super();
        this.operation = operation;
        this.sessionIds = sessionIds;
    }

    /**
     * Gets the operation.
     *
     * @return The operation
     */
    public SessionOperation getOperation() {
        return operation;
    }

    /**
     * Gets the session identifiers.
     *
     * @return The session identifiers
     */
    public List<String> getSessionIds() {
        return sessionIds;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        builder.append('{');
        if (operation != null) {
            builder.append("operation=").append(operation).append(", ");
        }
        if (sessionIds != null) {
            builder.append("sessionIds=").append(sessionIds).append(", ");
        }
        builder.append('}');
        return builder.toString();
    }

}
