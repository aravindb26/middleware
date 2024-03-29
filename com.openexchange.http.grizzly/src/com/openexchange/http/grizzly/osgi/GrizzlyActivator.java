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

package com.openexchange.http.grizzly.osgi;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import org.glassfish.grizzly.comet.CometAddOn;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.OXErrorPageGenerator;
import org.glassfish.grizzly.http.server.OXHttpServer;
import org.glassfish.grizzly.http.server.OXSessionManager;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.accesslog.AccessLogBuilder;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import com.google.common.collect.ImmutableList;
import com.openexchange.config.ConfigurationService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.exception.OXException;
import com.openexchange.http.grizzly.GrizzlyAccessLogConfig;
import com.openexchange.http.grizzly.GrizzlyAccessLogConfig.Format;
import com.openexchange.http.grizzly.GrizzlyAccessLogConfig.RotatePolicy;
import com.openexchange.http.grizzly.GrizzlyConfig;
import com.openexchange.http.grizzly.GrizzlyExceptionCode;
import com.openexchange.http.grizzly.service.comet.CometContextService;
import com.openexchange.http.grizzly.service.comet.impl.CometContextServiceImpl;
import com.openexchange.http.grizzly.service.http.FilterAndPath;
import com.openexchange.http.grizzly.service.http.HttpServiceFactory;
import com.openexchange.http.grizzly.service.utils.GrizzlyIPUtilImpl;
import com.openexchange.http.grizzly.service.websocket.WebApplicationService;
import com.openexchange.http.grizzly.service.websocket.impl.WebApplicationServiceImpl;
import com.openexchange.http.grizzly.servletfilter.RequestReportingFilter;
import com.openexchange.http.grizzly.servletfilter.WrappingFilter;
import com.openexchange.http.grizzly.threadpool.GrizzlyExecutorService;
import com.openexchange.http.requestwatcher.osgi.services.RequestWatcherService;
import com.openexchange.keystore.KeyStoreService;
import com.openexchange.net.ClientIPUtil;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.startup.SignalStartedService;
import com.openexchange.startup.ThreadControlService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;
import com.openexchange.version.VersionService;

/**
 * {@link GrizzlyActivator}
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class GrizzlyActivator extends HousekeepingActivator {

    private OXSessionManager sessionManager;

    /**
     * Initializes a new {@link GrizzlyActivator}.
     */
    public GrizzlyActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { ConfigurationService.class, RequestWatcherService.class, ThreadPoolService.class, TimerService.class, VersionService.class, KeyStoreService.class };
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

    /**
     * Initialize server side SSL configuration.
     *
     * @return server side {@link SSLEngineConfigurator}.
     */
    private static SSLEngineConfigurator createSslConfiguration(GrizzlyConfig grizzlyConfig, KeyStoreService keyStoreService) {
        // Initialize SSLContext configuration
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();

        // Set key store
        // http://www.sslshopper.com/article-most-common-java-keytool-keystore-commands.html
        Optional<byte[]> optKeystore = keyStoreService.optSecret(grizzlyConfig.getKeystoreId());
        if (optKeystore.isPresent()) {
            sslContextConfig.setKeyStoreBytes(optKeystore.get());
        } else {
            sslContextConfig.setKeyStoreFile(grizzlyConfig.getKeystorePath());
        }
        sslContextConfig.setKeyStorePass(grizzlyConfig.getKeystorePassword());
        // Create SSLEngine configurator
        SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContextConfig.createSSLContext(false), false, false, false);
        List<String> enabledCipherSuites = grizzlyConfig.getEnabledCiphers();
        if (null != enabledCipherSuites && !enabledCipherSuites.isEmpty()) {
            sslEngineConfigurator.setEnabledCipherSuites(enabledCipherSuites.toArray(new String[enabledCipherSuites.size()]));
        }
        return sslEngineConfigurator;
    }

    @Override
    protected synchronized void startBundle() throws OXException {
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GrizzlyActivator.class);
        try {
            Services.setServiceLookup(this);
            ConfigurationService configurationService = getService(ConfigurationService.class);
            ThreadPoolService threadPool = getService(ThreadPoolService.class);
            trackService(DispatcherPrefixService.class);

            log.info("Starting Grizzly server.");

            // Initialize Grizzly configuration
            GrizzlyConfig grizzlyConfig;
            {
                GrizzlyConfig.Builder builder = GrizzlyConfig.builder();
                builder.initializeFrom(configurationService);
                grizzlyConfig = builder.build();
            }

            GrizzlyIPUtilImpl ipUtil = new GrizzlyIPUtilImpl(grizzlyConfig);
            registerService(ClientIPUtil.class, ipUtil);

            // Initialize HTTP session manager
            OXSessionManager sessionManager = new OXSessionManager(grizzlyConfig, getService(TimerService.class));
            this.sessionManager = sessionManager;

            // Create, configure and start server
            OXHttpServer grizzly = new OXHttpServer(grizzlyConfig);

            ServerConfiguration serverConfiguration = grizzly.getServerConfiguration();
            serverConfiguration.setMaxRequestParameters(grizzlyConfig.getMaxRequestParameters());
            serverConfiguration.setAllowPayloadForUndefinedHttpMethods(true);
            serverConfiguration.setDefaultErrorPageGenerator(OXErrorPageGenerator.getInstance());

            // Check for JMX
            if (grizzlyConfig.isJMXEnabled()) {
                serverConfiguration.setJmxEnabled(true);
                log.info("Enabled JMX for Grizzly server.");
            }

            // Check for access.log
            GrizzlyAccessLogConfig accessLogConfig = grizzlyConfig.getAccessLogConfig();
            if (accessLogConfig.isEnabled()) {
                AccessLogBuilder builder = new AccessLogBuilder(accessLogConfig.getFile());

                Format format = accessLogConfig.getFormat();
                if (null != format) {
                    switch (format) {
                        case AGENT:
                            builder.format(org.glassfish.grizzly.http.server.accesslog.ApacheLogFormat.AGENT);
                            break;
                        case COMBINED:
                            builder.format(org.glassfish.grizzly.http.server.accesslog.ApacheLogFormat.COMBINED);
                            break;
                        case COMMON:
                            builder.format(org.glassfish.grizzly.http.server.accesslog.ApacheLogFormat.COMMON);
                            break;
                        case REFERER:
                            builder.format(org.glassfish.grizzly.http.server.accesslog.ApacheLogFormat.REFERER);
                            break;
                        case VHOST_COMBINED:
                            builder.format(org.glassfish.grizzly.http.server.accesslog.ApacheLogFormat.VHOST_COMBINED);
                            break;
                        case VHOST_COMMON:
                            builder.format(org.glassfish.grizzly.http.server.accesslog.ApacheLogFormat.VHOST_COMMON);
                            break;
                        default:
                            break;
                    }
                }

                RotatePolicy rotatePolicy = accessLogConfig.getRotatePolicy();
                if (null != rotatePolicy) {
                    switch (rotatePolicy) {
                        case DAILY:
                            builder.rotationPattern("yyyyMMdd");
                            break;
                        case HOURLY:
                            builder.rotationPattern("yyyyMMddhh");
                            break;
                        case NONE: /* fall-through */
                        default:
                            break;
                    }
                }

                int statusThreshold = accessLogConfig.getStatusThreshold();
                if (statusThreshold > 0) {
                    builder.statusThreshold(statusThreshold);
                }

                TimeZone timeZone = accessLogConfig.getTimeZone();
                if (null != timeZone) {
                    builder.timeZone(timeZone);
                }

                builder.instrument(serverConfiguration);
            }

            // Check for Web-Sockets
            boolean websocketsEnabled = grizzlyConfig.isWebsocketsEnabled();
            if (websocketsEnabled) {
                registerService(WebApplicationService.class, new WebApplicationServiceImpl());
                log.info("Enabled WebSockets for Grizzly server.");
            }

            // Check for Comet
            boolean cometEnabled = grizzlyConfig.isCometEnabled();
            if (cometEnabled) {
                registerService(CometContextService.class, new CometContextServiceImpl());
                log.info("Enabled Comet for Grizzly server.");
            }

            // Initialize liveness network listener
            NetworkListener networkLivenessListener = null;
            if (grizzlyConfig.isLivenessEnabled()) {
                int livenessPort = grizzlyConfig.getLivenessPort();
                if (livenessPort > 0) {
                    networkLivenessListener = new NetworkListener("liveness-listener", grizzlyConfig.getHttpHost(), livenessPort);

                    // Set the maximum body/header size as well as HTTP session manager
                    networkLivenessListener.setMaxFormPostSize(1);
                    networkLivenessListener.setMaxBufferedPostSize(1);
                    networkLivenessListener.setMaxHttpHeaderSize(grizzlyConfig.getMaxHttpHeaderSize());

                    // Set the transport
                    networkLivenessListener.setTransport(buildTcpNioTransport(grizzlyConfig, threadPool));

                    // Add HTTP network listener to Grizzly server
                    grizzly.addListener(networkLivenessListener);
                    log.info("Prepared Grizzly liveness network listener on host: {} and port: {}, but not yet started...", grizzlyConfig.getHttpHost(), Integer.valueOf(livenessPort));
                }
            }

            // Initialize HTTP network listener
            {
                NetworkListener networkListener = new NetworkListener("http-listener", grizzlyConfig.getHttpHost(), grizzlyConfig.getHttpPort());

                // Set the maximum body/header size as well as HTTP session manager
                networkListener.setMaxFormPostSize((int) grizzlyConfig.getMaxFormPostSize());
                networkListener.setMaxBufferedPostSize((int) grizzlyConfig.getMaxFormPostSize());
                networkListener.setMaxHttpHeaderSize(grizzlyConfig.getMaxHttpHeaderSize());
                networkListener.setSessionManager(sessionManager);

                // Set the transport
                networkListener.setTransport(buildTcpNioTransport(grizzlyConfig, threadPool));

                // Web Socket and Comet
                if (websocketsEnabled) {
                    networkListener.registerAddOn(new WebSocketAddOn());
                }
                if (cometEnabled) {
                    networkListener.registerAddOn(new CometAddOn());
                }

                // Add HTTP network listener to Grizzly server
                grizzly.addListener(networkListener);
                log.info("Prepared Grizzly HTTP network listener on host: {} and port: {}, but not yet started...", grizzlyConfig.getHttpHost(), Integer.valueOf(grizzlyConfig.getHttpPort()));
            }


            // Initialize HTTPS network listener (if enabled)
            if (grizzlyConfig.isSslEnabled()) {
                NetworkListener networkSslListener = new NetworkListener("https-listener", grizzlyConfig.getHttpHost(), grizzlyConfig.getHttpsPort());

                // Set the maximum body/header size as well as HTTP session manager
                networkSslListener.setMaxFormPostSize((int) grizzlyConfig.getMaxFormPostSize());
                networkSslListener.setMaxBufferedPostSize((int) grizzlyConfig.getMaxFormPostSize());
                networkSslListener.setMaxHttpHeaderSize(grizzlyConfig.getMaxHttpHeaderSize());
                networkSslListener.setSessionManager(sessionManager);

                // Configure and enabled SSL
                networkSslListener.setSSLEngineConfig(createSslConfiguration(grizzlyConfig, getServiceSafe(KeyStoreService.class)));
                networkSslListener.setSecure(true);

                // Set the transport
                networkSslListener.setTransport(buildTcpNioTransport(grizzlyConfig, threadPool));

                // Web Socket and Comet
                if (websocketsEnabled) {
                    networkSslListener.registerAddOn(new WebSocketAddOn());
                }
                if (cometEnabled) {
                    networkSslListener.registerAddOn(new CometAddOn());
                }

                // Add HTTPS network listener to Grizzly server
                grizzly.addListener(networkSslListener);
                log.info("Prepared Grizzly HTTPS network listener on host: {} and port: {}, but not yet started...", grizzlyConfig.getHttpHost(), Integer.valueOf(grizzlyConfig.getHttpsPort()));
            }

            // Start Grizzly server / Liveness end-point
            grizzly.start();
            if (networkLivenessListener != null) {
                try {
                    networkLivenessListener.start();
                    log.info("Started Grizzly liveness network listener on host: {} and port: {}", grizzlyConfig.getHttpHost(), Integer.valueOf(grizzlyConfig.getLivenessPort()));
                } catch (Exception e) {
                    log.error("Failed to start Grizzly liveness network listener on host: {} and port: {}", grizzlyConfig.getHttpHost(), Integer.valueOf(grizzlyConfig.getLivenessPort()), e);
                }
            }

            // The HttpService factory
            HttpServiceFactory httpServiceFactory;
            {
                // Build default list of filters
                ImmutableList.Builder<FilterAndPath> builder = ImmutableList.builderWithExpectedSize(2);
                builder.add(new FilterAndPath(new WrappingFilter(grizzlyConfig, ipUtil), "/*"));
                boolean isFilterEnabled = configurationService.getBoolProperty("com.openexchange.requestwatcher.isEnabled", true);
                if (isFilterEnabled) {
                    builder.add(new FilterAndPath(new RequestReportingFilter(getService(RequestWatcherService.class), configurationService), "/*"));
                }

                // Create the HttpService factory. Each distinct bundle will get its own instance of HttpServiceImpl.
                httpServiceFactory = new HttpServiceFactory(grizzly, builder.build(), grizzlyConfig.isSupportHierachicalLookupOnNotFound(), grizzlyConfig.getCookieMaxInactivityInterval(), context.getBundle());
            }

            // Initialize the filter tracker
            {
                ServiceTracker<Filter, Filter> tracker = new ServiceTracker<>(context, Filter.class, new ServletFilterTracker(httpServiceFactory, context));
                rememberTracker(tracker);
            }

            if (grizzlyConfig.isShutdownFast()) {
                registerService(HttpService.class.getName(), httpServiceFactory);
                log.info("Registered OSGi HttpService for Grizzly server.");
            }

            // Track the thread control
            track(ThreadControlService.class, new ThreadControlTracker(context));

            // Finally start listeners if server start-up is completed
            track(SignalStartedService.class, new StartUpTracker(httpServiceFactory, grizzly, grizzlyConfig, context));
            openTrackers();
        } catch (Exception e) {
            throw GrizzlyExceptionCode.GRIZZLY_SERVER_NOT_STARTED.create(e, new Object[] {});
        }
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        org.slf4j.LoggerFactory.getLogger(GrizzlyActivator.class).info("Unregistering services.");
        super.stopBundle();

        OXSessionManager sessionManager = this.sessionManager;
        if (null != sessionManager) {
            this.sessionManager = null;
            sessionManager.destroy();
        }

        Services.setServiceLookup(null);
    }

    /**
     * Builds a TCPNIOTransport using {c.o}.threadpool
     *
     * @param grizzlyConfig The configuration to use to set settings for TCP NIO connections
     * @param threadPool The thread pool to use
     * @return The configured <code>TCPNIOTransport</code> instance
     * @throws OXException If the transport cannot be build
     */
    private TCPNIOTransport buildTcpNioTransport(GrizzlyConfig grizzlyConfig, ThreadPoolService threadPool) throws OXException {
        if (threadPool == null) {
            throw GrizzlyExceptionCode.NEEDED_SERVICE_MISSING.create(ThreadPoolService.class.getSimpleName());
        }

        // Determine settings for TCP NIO connections
        boolean keepAlive = grizzlyConfig.isKeepAlive();
        boolean tcpNoDelay = grizzlyConfig.isTcpNoDelay();
        int readTimeoutMillis = grizzlyConfig.getReadTimeoutMillis();
        int writeTimeoutMillis = grizzlyConfig.getWriteTimeoutMillis();

        // Build up the TCPNIOTransport to use
        TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
        TCPNIOTransport transport = builder
                                    .setIOStrategy(SameThreadIOStrategy.getInstance())
                                    .setMemoryManager(builder.getMemoryManager())

                                    .setKeepAlive(keepAlive)
                                    .setTcpNoDelay(tcpNoDelay)
                                    .setClientSocketSoTimeout(readTimeoutMillis)

                                    .setReadTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                                    .setWriteTimeout(writeTimeoutMillis, TimeUnit.MILLISECONDS)

                                    .build();

        // Apply ExecutorService backed by {c.o}.threadpool
        transport.setWorkerThreadPool(new GrizzlyExecutorService(threadPool));
        return transport;
    }

}
