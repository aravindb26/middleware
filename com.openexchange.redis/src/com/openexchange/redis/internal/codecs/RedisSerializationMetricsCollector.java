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

package com.openexchange.redis.internal.codecs;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

/**
 * {@link RedisSerializationMetricsCollector}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisSerializationMetricsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisSerializationMetricsCollector.class);

    private final Cache<String, Meter> meters = CacheBuilder.newBuilder().build();

    /**
     * Initializes a new {@link RedisSerializationMetricsCollector}.
     */
    public RedisSerializationMetricsCollector() {
        super();
    }

    public void timeSerialization(Class<?> clazz, long time) {
        try {
            // @formatter:off
            Timer timer = (Timer) meters.get(clazz.getName() + "SER", () -> Timer.builder("appsuite.redis.cache.serialization")
                .description("Serialization time")
                .tag("type", clazz.getSimpleName())
                .serviceLevelObjectives(registerSLOBoundaries())
                .register(Metrics.globalRegistry));
            timer.record(time, TimeUnit.NANOSECONDS);
            // @formatter:on
        } catch (ExecutionException e) {
            LOGGER.debug("Error while collecting metric for serialization of '{}'", clazz.getName(), e);
        }
    }

    public void timeDeserialization(Class<?> clazz, long time) {
        try {
            // @formatter:off
            Timer timer = (Timer) meters.get(clazz.getName() + "DESER", () -> Timer.builder("appsuite.redis.cache.deserialization")
                .description("Deserialization time")
                .tag("type", clazz.getSimpleName())
                .serviceLevelObjectives(registerSLOBoundaries())
                .register(Metrics.globalRegistry));
            timer.record(time, TimeUnit.NANOSECONDS);
            // @formatter:on
        } catch (ExecutionException e) {
            LOGGER.debug("Error while collecting metric for deserialization of '{}'", clazz.getName(), e);
        }
    }

    private Duration[] registerSLOBoundaries() {
        // @formatter:off
        return new Duration[] {
            Duration.ofNanos(50000),
            Duration.ofNanos(100000),
            Duration.ofNanos(120000),
            Duration.ofNanos(125000),
            Duration.ofNanos(130000),
            Duration.ofNanos(140000),
            Duration.ofNanos(150000),
            Duration.ofNanos(160000),
            Duration.ofNanos(170000),
            Duration.ofNanos(180000),
            Duration.ofNanos(200000),
            Duration.ofNanos(250000),
            Duration.ofNanos(500000),
            Duration.ofNanos(1000000)
        };
        // @formatter:on
    }
}
