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

package com.openexchange.admin.rmi.dataobjects;

/**
 * This class represents an account.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class Account extends AccountData implements NameAndIdObject {

    private static final long serialVersionUID = -5142262036163363767L;

    private Integer contextId;
    private boolean contextIdset;

    private Integer userId;
    private boolean userIdset;

    private Integer id;
    private boolean idset;

    /**
     * Initializes a new {@link Account}.
     */
    public Account() {
        super();
    }

    /**
     * Gets the context identifier
     *
     * @return The context identifier
     */
    public Integer getContextId() {
        return contextId;
    }

    /**
     * Sets the context identifier
     *
     * @param contextId The context identifier
     */
    public void setContextId(Integer contextId) {
        this.contextId = contextId;
        this.contextIdset = true;
    }

    /**
     * Gets the user identifier
     *
     * @return The user identifier
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier
     *
     * @param userId The user identifier
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
        this.userIdset = true;
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public void setId(final Integer val) {
        this.id = val;
        this.idset = true;
    }

    /**
     * Checks whether context identifier is set
     *
     * @return <code>true</code> if context identifier is set; otherwise <code>false</code>
     */
    public boolean isContextIdset() {
        return contextIdset;
    }

    /**
     * Checks whether user identifier is set
     *
     * @return <code>true</code> if user identifier is set; otherwise <code>false</code>
     */
    public boolean isUserIdset() {
        return userIdset;
    }

    /**
     * Checks whether identifier is set
     *
     * @return <code>true</code> if identifier is set; otherwise <code>false</code>
     */
    public boolean isIdset() {
        return idset;
    }

}
