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

package com.openexchange.session;

/**
 * {@link VersionMismatchException} - Signals that version of stored session data does not match the application's expected version.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.12.0
 */
public class VersionMismatchException extends Exception {

    private static final long serialVersionUID = 4465149585192146130L;

    private final int version;
    private final int expectedVersion;

    /**
     * Initializes a new {@link VersionMismatchException} with default detail message; e.g.<br>
     * <pre>
     * "Session data's version (1) does not match the application's expected version (2)"
     * </pre>
     *
     * @param version The version of stored session data that does not match the expected version of the application
     * @param expectedVersion The version expected by application
     */
    public VersionMismatchException(int version, int expectedVersion) {
        super(new StringBuilder("Session data's version (").append(version).append(") does not match the application's expected version (").append(expectedVersion).append(')').toString());
        this.version = version;
        this.expectedVersion = expectedVersion;
    }

    /**
     * Gets the version of stored session data that does not match the application's expected version.
     *
     * @return The version of stored session data
     */
    public int getVersion() {
        return version;
    }

    /**
     * Gets the application's expected version.
     *
     * @return The application's expected version
     */
    public int getExpectedVersion() {
        return expectedVersion;
    }

}
