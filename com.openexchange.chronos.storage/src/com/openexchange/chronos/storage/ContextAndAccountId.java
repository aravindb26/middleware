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

package com.openexchange.chronos.storage;

/**
 * {@link ContextAndAccountId} - A tuple consisting of context and account identifier.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public class ContextAndAccountId implements Comparable<ContextAndAccountId> {

    private final int accountId;
    private final int contextId;
    private int hash;

    /**
     * Initializes a new {@link ContextAndAccountId}.
     *
     * @param accountId The account identifier
     * @param contextId The context identifier
     */
    public ContextAndAccountId(int accountId, int contextId) {
        super();
        this.accountId = accountId;
        this.contextId = contextId;
    }

    /**
     * Gets the account identifier
     *
     * @return The account identifier
     */
    public int getAccountId() {
        return accountId;
    }

    /**
     * Gets the context identifier
     *
     * @return The context identifier
     */
    public int getContextId() {
        return contextId;
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            int prime = 31;
            result = 1;
            result = prime * result + contextId;
            result = prime * result + accountId;
            hash = result;
        }
        return result;
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
        ContextAndAccountId other = (ContextAndAccountId) obj;
        if (contextId != other.contextId) {
            return false;
        }
        if (accountId != other.accountId) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(ContextAndAccountId o) {
        int c = Integer.compare(contextId, o.contextId);
        if (c != 0) {
            return c;
        }
        return Integer.compare(accountId, o.accountId);
    }

}
