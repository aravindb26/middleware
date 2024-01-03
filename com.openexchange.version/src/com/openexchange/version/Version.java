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

package com.openexchange.version;

import com.openexchange.java.Strings;

/**
 * {@link Version}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class Version implements Comparable<Version> {

    private static final String UNKNOWN = "<unknown version>";

    /**
     * Gets the fall-back string used for an unknown version.
     *
     * @return The unknown version identifier
     */
    public static String getUnknown() {
        return UNKNOWN;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final int major;
    private final int minor;
    private final int patch;
    private final int hashCode;
    private final String versionString;

    /**
     * Initialises a new {@link Version}.
     *
     * @param major The major
     * @param minor The minor
     * @param patch The patch
     */
    Version(int major, int minor, int patch) {
        super();
        this.major = major;
        this.minor = minor;
        this.patch = patch;

        // Version string
        this.versionString = (0 != major || 0 != minor || 0 != patch) ?
            new StringBuilder(16).append(major).append('.').append(minor).append('.').append(patch).toString() : UNKNOWN;

        // Hash code
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + minor;
        result = prime * result + patch;
        result = prime * result + ((versionString == null) ? 0 : versionString.hashCode());
        this.hashCode = result;
    }

    /**
     * Gets the major
     *
     * @return The major
     */
    public int getMajor() {
        return major;
    }

    /**
     * Gets the minor
     *
     * @return The minor
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Gets the patch
     *
     * @return The patch
     */
    public int getPatch() {
        return patch;
    }

    @Override
    public int hashCode() {
        return hashCode;
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
        Version other = (Version) obj;
        if (major != other.major) {
            return false;
        }
        if (minor != other.minor) {
            return false;
        }
        if (patch != other.patch) {
            return false;
        }
        if (versionString == null) {
            if (other.versionString != null) {
                return false;
            }
        } else if (!versionString.equals(other.versionString)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Version other) {
        if (major > other.major) {
            return 1;
        } else if (major < other.major) {
            return -1;
        }
        if (minor > other.minor) {
            return 1;
        } else if (minor < other.minor) {
            return -1;
        }
        if (patch > other.patch) {
            return 1;
        } else if (patch < other.patch) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return versionString;
    }

    /**
     * Parses the supplied server version string.
     * Note that past 8.0.0 the revision is dropped hence the 'rev' part is ignored
     *
     * @param versionString The version string to parse, e.g. <code>7.10.5</code>
     * @return The parsed server version
     * @throws IllegalArgumentException If string cannot be parsed
     */
    public static Version parse(String versionString) {
        String[] splitted = Strings.splitByDots(versionString);
        if (null == splitted || 0 == splitted.length) {
            throw new IllegalArgumentException("Version string '" + versionString + "' not in expected format");
        }
        int major = Strings.parsePositiveInt(splitted[0]);
        int minor = 1 < splitted.length ? Strings.parsePositiveInt(splitted[1]) : 0;
        int patch = 2 < splitted.length ? Strings.parsePositiveInt(Strings.splitBy(splitted[2], '-', true)[0]) : 0;
        if (-1 == major || -1 == patch || -1 == minor) {
            throw new IllegalArgumentException("Version string '" + versionString + "' not in expected format");
        }
        return new Version(major, minor, patch);
    }

    /**
     * Creates builder to build {@link Version}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link Version}.
     */
    public static final class Builder {

        private int major;
        private int minor;
        private int patch;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Sets the major part of the version
         *
         * @param major the major part of the version
         * @return this instance for chained calls
         */
        public Builder withMajor(int major) {
            this.major = major;
            return this;
        }

        /**
         * Sets the minor part of the version
         *
         * @param minor the minor part of the version
         * @return this instance for chained calls
         */
        public Builder withMinor(int minor) {
            this.minor = minor;
            return this;
        }

        /**
         * Sets the patch part of the version
         *
         * @param patch the patch part of the version
         * @return this instance for chained calls
         */
        public Builder withPatch(int patch) {
            this.patch = patch;
            return this;
        }

        /**
         * Builds the version
         *
         * @return the built version
         */
        public Version build() {
            return new Version(major, minor, patch);
        }
    }
}
