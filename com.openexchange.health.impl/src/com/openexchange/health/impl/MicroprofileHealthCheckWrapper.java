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

package com.openexchange.health.impl;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import com.openexchange.health.AbstractCachingMWHealthCheck;
import com.openexchange.health.DefaultMWHealthCheckResponse;
import com.openexchange.health.MWHealthCheckResponse;

/**
 * {@link MicroprofileHealthCheckWrapper} - Wraps a Microprofile health check.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.1
 */
public class MicroprofileHealthCheckWrapper extends AbstractCachingMWHealthCheck {

    private final HealthCheck healthCheck;

    /**
     * Initializes a new {@link MicroprofileHealthCheckWrapper}.
     *
     * @param healthCheck The Microprofile health check to wrap
     * @param timeToLiveMillis The time-to-live in milliseconds for a cached response, if elapsed the response is considered as expired/invalid
     */
    public MicroprofileHealthCheckWrapper(HealthCheck healthCheck, long timeToLiveMillis) {
        super(timeToLiveMillis);
        this.healthCheck = healthCheck;
    }

    @Override
    public String getName() {
        return healthCheck.getClass().getName();
    }

    @Override
    protected MWHealthCheckResponse doCall() {
        HealthCheckResponse response = healthCheck.call();
        return new DefaultMWHealthCheckResponse(getName(), response.getData().isPresent() ? response.getData().get() : null, Status.UP.equals(response.getStatus()));
    }

}
