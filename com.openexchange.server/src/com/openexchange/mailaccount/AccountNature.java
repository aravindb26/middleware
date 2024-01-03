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

package com.openexchange.mailaccount;


/**
 * {@link AccountNature} - Specifies the nature of a mail/transport account.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public enum AccountNature {

    /**
     * The primary account.
     */
    PRIMARY("primary"),
    /**
     * A secondary account.
     */
    SECONDARY("secondary"),
    /**
     * A regular account; neither primary nor secondary.
     */
    REGULAR("regular");

    private final String identifier;

    private AccountNature(String identifier) {
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

    @Override
    public String toString() {
        return identifier;
    }

    /**
     * Checks whether this account nature represents a primary or secondary account.
     *
     * @return <code>true</code> for primary or secondary account; otherwise <code>false</code>
     */
    public boolean isDefaultOrSecondaryAccount() {
        return this == PRIMARY || this == SECONDARY;
    }

    /**
     * Checks whether this account nature represents a primary account.
     *
     * @return <code>true</code> for primary; otherwise <code>false</code>
     */
    public boolean isDefaultAccount() {
        return this == PRIMARY;
    }

    /**
     * Checks whether this account nature represents a secondary account.
     *
     * @return <code>true</code> for secondary; otherwise <code>false</code>
     */
    public boolean isSecondaryAccount() {
        return this == SECONDARY;
    }

    /**
     * Checks whether this account nature represents neither a primary nor secondary account.
     *
     * @return <code>true</code> for neither primary nor secondary account; otherwise <code>false</code>
     */
    public boolean isNeitherDefaultNorSecondaryAccount() {
        return this == REGULAR;
    }

    /**
     * Gets the account nature for given flags.
     *
     * @param primary Whether account is the primary account
     * @param secondary Whether account is a secondary account
     * @return The account nature for given flags
     */
    public static AccountNature accountNatureFor(boolean primary, boolean secondary) {
        return primary ? PRIMARY : (secondary ? SECONDARY : REGULAR);
    }

}
