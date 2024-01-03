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


package com.openexchange.sessiond.redis.metrics;

import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.exception.OXException;
import com.openexchange.sessiond.redis.RedisSessiondService;

/**
 * {@link RedisSessiondMetricsProvider} - The metrics provider for Redis session storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisSessiondMetricsProvider {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisSessiondMetricsProvider.class);

    private static final AtomicReference<RedisSessiondMetricsProvider> INSTANCE_REF = new AtomicReference<>();

    /**
     * Initializes the metrics provider instance.
     *
     * @param sessiondService The Redis SessiondD service
     */
    public static void initInstance(RedisSessiondService sessiondService) {
        INSTANCE_REF.set(new RedisSessiondMetricsProvider(sessiondService));
    }

    /**
     * Drops the metrics provider instance.
     */
    public static void dropInstance() {
        INSTANCE_REF.set(null);
    }

    /**
     * Gets the metrics provider instance.
     *
     * @return The metrics provider instance or <code>null</code>
     */
    public static RedisSessiondMetricsProvider getInstance() {
        return INSTANCE_REF.get();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final RedisSessiondService sessiondService;

    /**
     * Initializes a new {@link RedisSessiondMetricsProvider}.
     *
     * @param sessiondService The Redis SessiondD service
     */
    private RedisSessiondMetricsProvider(RedisSessiondService sessiondService) {
        super();
        this.sessiondService = sessiondService;
    }

    /**
     * Gets the total number of sessions.
     *
     * @return The total number of sessions
     */
    public int getMetricTotalSessions() {
        try {
            return (int) sessiondService.queryCounterForNumberOfSessions();
        } catch (Exception e) {
            LOG.error("Failed to acquire total number of sessions", e);
            return 0;
        }
    }

    /**
     * Gets the number of sessions for specified brand identifier.
     *
     * @param brandIdentifier The brand identifier
     * @return The total number of sessions
     */
    public int getMetricBrandSessions(String brandIdentifier) {
        try {
            return (int) sessiondService.queryNumberOfSessionsForBrand(brandIdentifier);
        } catch (Exception e) {
            LOG.error("Failed to acquire number of sessions for brand {}", brandIdentifier, e);
            return 0;
        }
    }

    /**
     * Gets the number of sessions having a long-term time-to-live (staySignedIn=true).
     * <p>
     * Short-term session have their lifetime set to <code>"com.openexchange.sessiond.sessionLongLifeTime"</code> property.
     *
     * @return The number of long-term sessions
     */
    public int getMetricLongSessions() {
        try {
            return (int) sessiondService.queryCounterForNumberOfLongSessions();
        } catch (Exception e) {
            LOG.error("Failed to acquire total number of sessions", e);
            return 0;
        }
    }

    /**
     * Gets the number of sessions having a short-term time-to-live (staySignedIn=false).
     * <p>
     * Short-term session have their lifetime set to <code>"com.openexchange.sessiond.sessionDefaultLifeTime"</code> property.
     *
     * @return The number of short-term sessions
     */
    public int getMetricShortSessions() {
        try {
            return (int) sessiondService.queryCounterForNumberOfShortSessions();
        } catch (Exception e) {
            LOG.error("Failed to acquire total number of sessions", e);
            return 0;
        }
    }

    /**
     * Gets the number of active sessions.
     *
     * @return The number of active sessions
     */
    public int getMetricActiveSessions() {
        try {
            return (int) sessiondService.queryCounterForNumberOfActiveSessions();
        } catch (OXException e) {
            LOG.error("Failed to acquire number of active sessions", e);
            return 0;
        }
    }

    /**
     * Gets the number of active sessions for specified brand identifier.
     *
     * @param brandIdentifier The brand identifier
     * @return The number of active sessions
     */
    public int getMetricBrandActiveSessions(String brandIdentifier) {
        try {
            return (int) sessiondService.queryCounterForNumberOfActiveSessionsForBrand(brandIdentifier);
        } catch (Exception e) {
            LOG.error("Failed to acquire number of sessions for brand {}", brandIdentifier, e);
            return 0;
        }
    }

    /**
     * getMaxNumberOfSessions
     *
     * @return
     */
    public int getMaxNumberOfSessions() {
        return sessiondService.getMaxNumberOfSessions();
    }

}
