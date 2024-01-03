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

/**
 * {@link Namespace} - A single namespace entry.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class Namespace {

    /** Prefix string for the namespace. */
    private final String prefix;

    /** The full name for the namespace. */
    private final String fullName;

    /** Delimiter between names in this namespace. */
    private final char delimiter;

    /**
     * Initializes a new {@link Namespace}.
     */
    public Namespace(String prefix, char delimiter) {
        super();
        this.prefix = prefix;
        this.delimiter = delimiter;
        int len = prefix.length();
        this.fullName = len > 0 ? prefix.substring(0, len - 1) : prefix;
    }

    /**
     * Gets the prefix string for the namespace; e.g. <code>"INBOX/"</code>.
     *
     * @return The prefix (e.g. <code>"INBOX/"</code>)
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Gets the full name (prefix string for the namespace without possibly trailing delimiter character); e.g. <code>"INBOX"</code>.
     *
     * @return The full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Gets the delimiter between names in this namespace.
     *
     * @return The delimiter
     */
    public char getDelimiter() {
        return delimiter;
    }

    @Override
    public String toString() {
        return new StringBuilder().append('(').append('"').append(prefix).append("\" ").append(delimiter).append(')').toString();
    }

}
