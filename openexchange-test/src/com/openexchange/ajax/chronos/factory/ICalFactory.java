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

package com.openexchange.ajax.chronos.factory;

import java.util.Collections;
import java.util.List;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.java.Lists;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * {@link ICalFactory} - Creates different iCals for a specific vendor
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.3
 */
public interface ICalFactory {

    /** Identifier for a single event template */
    static final String SINGLE = "single";

    /** Identifier for a series event template */
    static final String SERIES = "series";

    /**
     * Generates an iCAL file from a specific template
     *
     * @param method The method of the iCAL
     * @param id The identifier of the template
     * @param data The data to inject
     * @return The iCAL
     * @throws AssertionError In case preconditions of the iCAL aten't fulfilled
     * @throws Exception In case of general error
     */
    String generate(SchedulingMethod method, String id, List<EventData> data) throws AssertionError, Exception;

    /**
     * Generates an iCAL file from a specific template
     *
     * @param method The method of the iCAL
     * @param id The identifier of the template
     * @param data The data to inject
     * @return The iCAL
     * @throws AssertionError In case preconditions of the iCAL aten't fulfilled
     * @throws Exception In case of general error
     */
    default String generate(SchedulingMethod method, String id, EventData data) throws AssertionError, Exception {
        return generate(method, id, Collections.singletonList(data));
    }

    /**
     * Generates an iCAL file for a single event
     *
     * @param method The method of the iCAL
     * @param data The data to inject
     * @return The iCAL
     * @throws AssertionError In case preconditions of the iCAL aten't fulfilled
     * @throws Exception In case of general error
     */
    default String generateForSingleEvent(SchedulingMethod method, EventData data) throws AssertionError, Exception {
        return generate(method, SINGLE, data);
    }

    /**
     * Generates an iCAL file for a series event
     * 
     * @param method The method of the iCAL
     * @param master The master event
     * @param exceptions The optional exceptions
     * @return The iCAL
     * @throws AssertionError In case preconditions of the iCAL aten't fulfilled
     * @throws Exception In case of general error
     */
    default String generateForSeriesEvent(SchedulingMethod method, EventData master, List<EventData> exceptions) throws AssertionError, Exception {
        return generate(method, SERIES, Lists.combine(exceptions, master));
    }
}
