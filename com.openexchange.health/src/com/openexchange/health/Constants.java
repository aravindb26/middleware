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

package com.openexchange.health;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * {@link Constants} - Constants for registered health checks.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class Constants {

    /**
     * Initializes a new {@link Constants}.
     */
    private Constants() {
        super();
    }

    /**
     * The property name for cache time-to-live when registering instances of <code>org.eclipse.microprofile.health.HealthCheck</code>.
     * <p>
     * Example:
     * <pre>
     *  HealthCheck myHealthCheck = new MyHealthCheck();
     *  Dictionary&lt;String, Object&gt; properties = new Hashtable&lt;&gt;(2);
     *  properties.put(Constants.SERVICE_CACHE_TIME_TO_LIVE, Long.valueOf(300000L)); // 5 minutes cache time-to-live
     *  registerService(HealthCheck.class, myHealthCheck, properties);
     * </pre>
     */
    public static final String SERVICE_CACHE_TIME_TO_LIVE = "com.openexchange.health.cache.timeToLive";

    /**
     * Creates a new dictionary containing the specified cache time-to-live to be used on service registration.
     *
     * @param timeToLive The time-to-live in milliseconds for a cached response, if elapsed the response is considered as expired/invalid
     * @return The newly created directory containing the specified time-to-live
     */
    public static Dictionary<String, Object> withTimeToLive(long timeToLive) {
        Dictionary<String, Object> properties = new Hashtable<>(2);
        properties.put(SERVICE_CACHE_TIME_TO_LIVE, Long.valueOf(timeToLive <= 0 ? 0L : timeToLive));
        return properties;
    }

}
