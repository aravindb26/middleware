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
package com.openexchange.ajax.metrics;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.services.ServerServiceRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;

/**
 * {@link HttpMetricUtil} provides util methods to track metrics of http requests
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8
 */
public class HttpMetricUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HttpMetricUtil.class);
    private static final HttpMetricUtil INSTANCE = new HttpMetricUtil();
    private volatile HttpMetricTagConfig config;

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static HttpMetricUtil getInstance() {
        return INSTANCE;
    }

    /**
     * Records the duration of a request
     *
     * @param module The name of the action's module
     * @param action The name of the action
     * @param status The status code of the request
     * @param durationMillis The duration in milliseconds
     */
    public void recordRequest(String module, String action, String status, long durationMillis) {
        if(module == null || action == null || status == null) {
            // Not record request with some unknown attributes
            return;
        }

        getTimer(module, action, status).record(durationMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the time for the given mode
     *
     * @param module The module name
     * @param action The action name
     * @param status The status of the request
     * @return The timer
     */
    private Timer getTimer(String module, String action, String status) {
        return getBuilderFor(module, action, status).description("HTTP API request times")
                                                    .serviceLevelObjectives(Duration.ofMillis(50),
                                                                            Duration.ofMillis(100),
                                                                            Duration.ofMillis(150),
                                                                            Duration.ofMillis(200),
                                                                            Duration.ofMillis(250),
                                                                            Duration.ofMillis(300),
                                                                            Duration.ofMillis(400),
                                                                            Duration.ofMillis(500),
                                                                            Duration.ofMillis(750),
                                                                            Duration.ofSeconds(1),
                                                                            Duration.ofSeconds(2),
                                                                            Duration.ofSeconds(5),
                                                                            Duration.ofSeconds(10),
                                                                            Duration.ofSeconds(30),
                                                                            Duration.ofMinutes(1))
                                                    .register(Metrics.globalRegistry);
    }

    /**
     * Prepares a builder for the given module mode
     *
     * @param module The module name
     * @param action The action name
     * @param status The status of the request
     * @return The builder
     */
    private Timer.Builder getBuilderFor(String module, String action, String status) {
        Builder result = Timer.builder("appsuite.httpapi.requests");
        HttpMetricTagConfig config = getConfig();
        if (config.useModule) {
            result.tag("module", module);
        }
        if (config.useAction) {
            result.tag("action", action);
        }
        if (config.useStatus) {
            result.tag("status", status);
        }
        return result;

    }

    private HttpMetricTagConfig getConfig() {
        if (config != null) {
            return config;
        }
        synchronized (this) {
            if (config != null) {
                return config;
            }
            LeanConfigurationService leanConfigurationService;
            try {
                leanConfigurationService = ServerServiceRegistry.getInstance().getService(LeanConfigurationService.class, true);
                String filter = leanConfigurationService.getProperty(MetricProperty.LABEL_FILTER);
                if (Strings.isEmpty(filter)) {
                    config = new HttpMetricTagConfig(true, true, true);
                    return config;
                }
                List<String> labelsToOmit = Strings.splitAndTrim(filter.toLowerCase(), ",");
                config = new HttpMetricTagConfig(labelsToOmit.contains("module") == false,
                                                 labelsToOmit.contains("action") == false,
                                                 labelsToOmit.contains("status") == false);
            } catch (OXException e) {
                LOG.warn("Unable to determine label fields. Falling back to all fields.", e);
                return new HttpMetricTagConfig(true, true, true);
            }
        }
        return config;
    }

    /**
     * {@link HttpMetricTags}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8
     */
    private record HttpMetricTagConfig(boolean useModule, boolean useAction, boolean useStatus) {
    }

}
