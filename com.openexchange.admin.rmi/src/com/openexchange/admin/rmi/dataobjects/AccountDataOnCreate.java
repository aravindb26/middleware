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
 * This class represents account data required for creating an account.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class AccountDataOnCreate extends AccountData {

    private static final long serialVersionUID = 8393954040494701689L;

    private EndpointSource mailEndpointSource;
    private boolean mailEndpointSourceset;

    private EndpointSource transportEndpointSource;
    private boolean transportEndpointSourceset;

    /**
     * Initializes a new {@link AccountDataOnCreate}.
     */
    public AccountDataOnCreate() {
        super();
        mailEndpointSource = EndpointSource.NONE;
        transportEndpointSource = EndpointSource.NONE;
    }

    /**
     * Gets the mail end-point source.
     *
     * @return The end-point source
     */
    public EndpointSource getMailEndpointSource() {
        return this.mailEndpointSource;
    }

    /**
     * Sets the mail end-point source.
     *
     * @param mailEndpointSource The mail end-point source.
     */
    public void setMailEndpointSource(EndpointSource mailEndpointSource) {
        this.mailEndpointSource = mailEndpointSource;
        this.mailEndpointSourceset = true;
    }

    /**
     * Checks whether mail end-point source is set
     *
     * @return <code>true</code> if mail end-point source is set; otherwise <code>false</code>
     */
    public boolean isMailEndpointSourceset() {
        return mailEndpointSourceset;
    }

    /**
     * Gets the transport end-point source.
     *
     * @return The end-point source
     */
    public EndpointSource getTransportEndpointSource() {
        return this.transportEndpointSource;
    }

    /**
     * Sets the transport end-point source.
     *
     * @param transportEndpointSource The transport end-point source.
     */
    public void setTransportEndpointSource(EndpointSource transportEndpointSource) {
        this.transportEndpointSource = transportEndpointSource;
        this.transportEndpointSourceset = true;
    }

    /**
     * Checks whether transport end-point source is set
     *
     * @return <code>true</code> if transport end-point source is set; otherwise <code>false</code>
     */
    public boolean isTransportEndpointSourceset() {
        return transportEndpointSourceset;
    }

}
