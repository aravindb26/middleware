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


package com.openexchange.redis;

import java.util.Optional;
import com.openexchange.java.Strings;

/**
 * {@link OperationMode} - The operation mode of the Redis Server to connect against.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum OperationMode {

    /**
     * Redis Stand-Along operation mode.
     */
    STAND_ALONE("standalone"),
    /**
     * Redis Stand-Along operation mode.
     */
    CLUSTER("cluster"),
    /**
     * Redis Stand-Along operation mode.
     */
    SENTINEL("sentinel"),
    ;

    private final String identifier;

    private OperationMode(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the identifier.
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the operation mode for given identifier.
     *
     * @param operationMode The mode identifier to look-up by
     * @return The operation mode or empty
     */
    public static Optional<OperationMode> operationModeFor(String operationMode) {
        if (Strings.isEmpty(operationMode)) {
            return Optional.empty();
        }

        String tmp = Strings.asciiLowerCase(operationMode.trim());
        for (OperationMode om : OperationMode.values()) {
            if (om.identifier.equals(tmp)) {
                return Optional.of(om);
            }
        }
        return Optional.empty();
    }
}
