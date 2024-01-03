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

import java.util.Map;

/**
 * {@link DefaultMWHealthCheckResponse} - The default implementation of {@link MWHealthCheckResponse}.
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.1
 */
public class DefaultMWHealthCheckResponse implements MWHealthCheckResponse {

    private final String name;
    private final Map<String, Object> data;
    private final MWHealthState state;

    /**
     * Initializes a new {@link DefaultMWHealthCheckResponse}.
     *
     * @param name The name/identifier for the health check
     * @param data The data
     * @param state The state; <code>true</code> for {@link MWHealthState#UP} and <code>false</code> for {@link MWHealthState#DOWN}
     */
    public DefaultMWHealthCheckResponse(String name, Map<String, Object> data, boolean state) {
        this(name, data, state ? MWHealthState.UP : MWHealthState.DOWN);
    }

    /**
     * Initializes a new {@link DefaultMWHealthCheckResponse}.
     *
     * @param name The name/identifier for the health check
     * @param data The data
     * @param state The state
     */
    public DefaultMWHealthCheckResponse(String name, Map<String, Object> data, MWHealthState state) {
        super();
        this.name = name;
        this.data = data;
        this.state = state;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public MWHealthState getState() {
        return state;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(MWHealthState.UP.equals(state) ? "UP" : "DOWN");
        // Don't include detailed data for now
        //        if (null != data && data.size() > 0) {
        //            sb.append(", ").append("data: [");
        //            for (Map.Entry<String, Object> entry : data.entrySet()) {
        //                sb.append(entry.getKey()).append('=').append(entry.getValue()).append(',');
        //            }
        //            sb.deleteCharAt(sb.length() - 1);
        //            sb.append(']');
        //        }
        return sb.toString();
    }

}
