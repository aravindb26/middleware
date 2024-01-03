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

package com.openexchange.osgi;

/**
 * {@link RankingAwareServiceListing} extends {@link ServiceListing} with ranking informations
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @param <S> The type of the service
 */
public interface RankingAwareServiceListing<S> extends ServiceListing<S> {

    /**
     * Checks if this service list has no services listed
     *
     * @return <code>true</code> if no service exists, otherwise <code>false</code>
     */
    default boolean isEmpty() {
        return hasAnyServices() == false;
    }

    /**
     * Checks if this service list has any services
     *
     * @return <code>true</code> if at least one service exists; otherwise <code>false</code>
     */
    boolean hasAnyServices();

    /**
     * Gets the currently highest-ranked service from this service listing
     *
     * @return The highest-ranked service or <code>null</code> (if service listing is empty)
     */
    S getHighestRanked();

}
