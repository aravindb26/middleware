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

package com.openexchange.filestore.sproxyd.impl;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.metrics.micrometer.Micrometer;
import com.openexchange.osgi.ExceptionUtils;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

/**
 * A {@link EndpointPool} manages a set of endpoints for the sproxyd client. The available
 * endpoints are returned in a round-robin manner. If endpoints become unavailable they can
 * be blacklisted. Every host on the blacklist is periodically checked by a heartbeat for
 * availability. If a formerly blacklisted host becomes available again, it is removed from
 * the blacklist and returned to the pool of available hosts. The process of blacklisting
 * an endpoint is up to the client.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
@ThreadSafe
public class EndpointPool {

    static final Logger LOG = LoggerFactory.getLogger(EndpointPool.class);

    private static final String GROUP_ID = "appsuite.sproxyd.";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<String> available;
    private final List<String> blacklist;
    private final AtomicInteger counter;
    private final String filestoreId;
    private final int numberOfEndpoints;
    private final boolean blacklistEndpoints;
    private final ServiceLookup services;
    private ScheduledTimerTask heartbeat;

    /**
     * Initializes a new {@link EndpointPool}.
     *
     * @param filestoreId The filestore ID
     * @param endpointUrls A list of endpoint URLs to manage; must not be empty; URLs must always end with a trailing slash
     * @param heartbeatInterval
     * @param blacklistEndpoints Whether to black-list end-points if considered as unavailable
     * @param services
     * @throws OXException
     */
    public EndpointPool(String filestoreId, List<String> endpointUrls, int heartbeatInterval, boolean blacklistEndpoints, ServiceLookup services) throws OXException {
        super();
        this.filestoreId = filestoreId;
        this.blacklistEndpoints = blacklistEndpoints;
        this.services = services;
        int size = endpointUrls.size();
        numberOfEndpoints = size;
        available = new ArrayList<>(endpointUrls);
        blacklist = new ArrayList<>(size);
        counter = new AtomicInteger(size);
        if (endpointUrls.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'endpointUrls' must not be empty");
        }

        LOG.debug("Sproxyd endpoint pool [{}]: Scheduling heartbeat timer task", filestoreId);
        heartbeat = services.getServiceSafe(TimerService.class).scheduleWithFixedDelay(new Heartbeat(filestoreId, this, services), heartbeatInterval, heartbeatInterval);
        initMetrics();
    }

    /**
     * Initializes the metrics for this {@link EndpointPool}
     */
    private void initMetrics() {
        String noUnit = null;
        Tags tags = Tags.of("filestore", filestoreId);
        Micrometer.registerOrUpdateGauge(Metrics.globalRegistry,
            GROUP_ID + "endpoints.total",
            tags,
            "Number of configured sproxyd endpoints",
            noUnit,
            this,
            (p) -> (double) p.getNumberOfEndpoints());

        Micrometer.registerOrUpdateGauge(Metrics.globalRegistry,
            GROUP_ID + "endpoints.available",
            tags,
            "Number of available sproxyd endpoints",
            noUnit,
            this,
            (p) -> (double) p.getStats().getAvailableEndpoints());

        Micrometer.registerOrUpdateGauge(Metrics.globalRegistry,
            GROUP_ID + "endpoints.unavailable",
            tags,
            "Number of unavailable (blacklisted) sproxyd endpoints",
            noUnit,
            this,
            (p) -> (double) p.getStats().getBlacklistedEndpoints());
    }

    /**
     * Gets the number of end-points
     *
     * @return The number of end-points
     */
    public int getNumberOfEndpoints() {
        return numberOfEndpoints;
    }

    /**
     * Checks whether to black-list end-points if considered as unavailable.
     *
     * @return <code>true</code> for black-listing; otherwise <code>false</code>
     */
    public boolean isBlacklistEndpoints() {
        return blacklistEndpoints;
    }

    /**
     * Gets an available end-point.
     *
     * @param prefix The prefix
     * @return The end-point or <code>null</code> if all end-points have been blacklisted
     */
    public Endpoint get(String prefix) {
        lock.readLock().lock();
        try {
            if (available.isEmpty()) {
                return null;
            }

            int next = counter.incrementAndGet();
            if (next < 0) {
                int newNext = available.size();
                counter.compareAndSet(next, newNext);
                next = newNext;
            }
            Endpoint endpoint = new Endpoint(available.get(next % available.size()), prefix);
            LOG.debug("Sproxyd endpoint pool [{}]: Returning endpoint {}", filestoreId, endpoint);
            return endpoint;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes an end-point from the list of available ones and adds it to the blacklist.
     *
     * @param url The base URL of the end-point
     */
    public void blacklist(String url) {
        if (!blacklistEndpoints) {
            LOG.warn("Sproxyd endpoint pool [{}]: Endpoint {} is NOT added to blacklist since disabled via configuration", filestoreId, url);
            return;
        }

        lock.writeLock().lock();
        try {
            if (available.remove(url)) {
                LOG.warn("Sproxyd endpoint pool [{}]: Endpoint {} is added to blacklist", filestoreId, url);
                blacklist.add(url);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes an endpoint from the blacklist and adds it back to list of available ones.
     *
     * @param url The base URL of the endpoint
     */
    public void unblacklist(String url) {
        lock.writeLock().lock();
        try {
            if (blacklist.remove(url)) {
                LOG.info("Sproxyd endpoint pool [{}]: Endpoint {} is removed from blacklist", filestoreId, url);
                available.add(url);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Closes this endpoint pool instance. The blacklist heartbeat task is cancelled.
     */
    public synchronized void close() {
        if (heartbeat != null) {
            heartbeat.cancel();
            heartbeat = null;
        }
    }

    List<String> getBlacklist() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(blacklist);
        } finally {
            lock.readLock().unlock();
        }
    }

    EndpointPoolStats getStats() {
        lock.readLock().lock();
        try {
            return new EndpointPoolStats(getNumberOfEndpoints(), getBlacklist());
        } finally {
            lock.readLock().unlock();
        }
    }

    private static class Heartbeat implements Runnable {

        @SuppressWarnings("hiding")
        private final String filestoreId;
        private final EndpointPool endpoints;
        private final ServiceLookup services;

        Heartbeat(String filestoreId, EndpointPool endpoints, ServiceLookup services) {
            super();
            this.filestoreId = filestoreId;
            this.endpoints = endpoints;
            this.services = services;
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public void run() {
            try {
                List<String> blacklist = endpoints.getBlacklist();
                if (blacklist.isEmpty()) {
                    LOG.debug("Sproxyd endpoint pool [{}]: Heartbeat - blacklist is empty, nothing to do", filestoreId);
                    return;
                }

                LOG.debug("Sproxyd endpoint pool [{}]: Heartbeat - blacklist contains {} endpoints", filestoreId, I(blacklist.size()));
                for (String endpoint : blacklist) {
                    HttpClient httpClient = services.getServiceSafe(HttpClientService.class).getHttpClient(filestoreId);
                    if (Utils.endpointUnavailable(endpoint, httpClient)) {
                        LOG.warn("Sproxyd endpoint pool [{}]: Endpoint {} is still unavailable", filestoreId, endpoint);
                    } else {
                        endpoints.unblacklist(endpoint);
                    }
                }
            } catch (Throwable t) {
                LOG.error("Sproxyd endpoint pool [{}]: Error during heartbeat execution", filestoreId, t);
                ExceptionUtils.handleThrowable(t);
            }
        }

    }

}