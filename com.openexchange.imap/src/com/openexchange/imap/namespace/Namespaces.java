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

package com.openexchange.imap.namespace;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.openexchange.java.Strings;

/**
 * {@link Namespaces} - Represent the response to the <code>NAMESPACE</code> command.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class Namespaces {

    /**
     * Extracts the full names from given list of namespaces.
     *
     * @param namespaces The namespaces
     * @return The full names
     */
    public static List<String> getFullNamesFrom(List<Namespace> namespaces) {
        if (namespaces == null || namespaces.isEmpty()) {
            return Collections.emptyList();
        }
        return namespaces.stream().map(Namespace::getFullName).toList();
    }

    /**
     * Extracts the full names from given list of namespaces.
     *
     * @param namespaces The namespaces
     * @return The full names as a String array
     */
    public static String[] getFullNamesAsArrayFrom(List<Namespace> namespaces) {
        List<String> fullNames = getFullNamesFrom(namespaces);
        return fullNames.isEmpty() ? Strings.getEmptyStrings() : fullNames.toArray(new String[fullNames.size()]);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final List<Namespace> personal;
    private final List<Namespace> otherUsers;
    private final List<Namespace> shared;

    private volatile List<String> otherUsersFullNames;
    private volatile List<String> sharedFullNames;

    /**
     * Initializes a new {@link Namespaces}.
     *
     * @param personal The personal namespaces
     * @param otherUsers The namespaces for other users
     * @param shared The shared namespace
     */
    public Namespaces(Namespace[] personal, Namespace[] otherUsers, Namespace[] shared) {
        super();
        this.personal = personal == null ? null : List.copyOf(Arrays.stream(personal).filter(Objects::nonNull).toList());
        this.otherUsers = otherUsers == null ? null : List.copyOf(Arrays.stream(otherUsers).filter(Objects::nonNull).toList());
        this.shared = shared == null ? null : List.copyOf(Arrays.stream(shared).filter(Objects::nonNull).toList());
    }

    /**
     * Initializes a new {@link Namespaces}.
     *
     * @param personal The personal namespaces
     * @param otherUsers The namespaces for other users
     * @param shared The shared namespace
     */
    public Namespaces(List<Namespace> personal, List<Namespace> otherUsers, List<Namespace> shared) {
        super();
        this.personal = personal == null ? List.of() : List.copyOf(personal.stream().filter(Objects::nonNull).toList());
        this.otherUsers = otherUsers == null ? List.of() : List.copyOf(otherUsers.stream().filter(Objects::nonNull).toList());
        this.shared = shared == null ? List.of() : List.copyOf(shared.stream().filter(Objects::nonNull).toList());
    }

    /**
     * Gets the personal namespaces.
     *
     * @return The personal namespaces
     */
    public List<Namespace> getPersonal() {
        return personal;
    }

    /**
     * Gets the namespaces for other users.
     *
     * @return The namespaces for other users
     */
    public List<Namespace> getOtherUsers() {
        return otherUsers;
    }

    /**
     * Gets the full names from namespaces for other users.
     *
     * @return The full names
     */
    public String[] getOtherUsersFullNames() {
        List<String> fullNames = this.otherUsersFullNames;
        if (fullNames == null) {
            synchronized (this) {
                fullNames = this.otherUsersFullNames;
                if (fullNames == null) {
                    fullNames = List.copyOf(getFullNamesFrom(otherUsers));
                    this.otherUsersFullNames = fullNames;
                }
            }
        }
        return fullNames.toArray(new String[fullNames.size()]);
    }

    /**
     * Gets the shared (aka public) namespaces.
     *
     * @return The shared/public namespaces
     */
    public List<Namespace> getShared() {
        return shared;
    }

    /**
     * Gets the full names from shared/public namespaces.
     *
     * @return The full names
     */
    public String[] getSharedFullNames() {
        List<String> fullNames = this.sharedFullNames;
        if (fullNames == null) {
            synchronized (this) {
                fullNames = this.sharedFullNames;
                if (fullNames == null) {
                    fullNames = List.copyOf(getFullNamesFrom(shared));
                    this.sharedFullNames = fullNames;
                }
            }
        }
        return fullNames.toArray(new String[fullNames.size()]);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (personal != null) {
            builder.append("personal=").append(personal).append(", ");
        }
        if (otherUsers != null) {
            builder.append("otherUsers=").append(otherUsers).append(", ");
        }
        if (shared != null) {
            builder.append("shared=").append(shared);
        }
        builder.append(']');
        return builder.toString();
    }

}
