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


package com.openexchange.http.liveness;

import com.openexchange.config.ConfigurationService;

/**
 * {@link SocketLivenessServerStarter} - Tracks configuration service & starts HTTP liveness end-point.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SocketLivenessServerStarter { // NOSONARLINT

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SocketLivenessServerStarter.class);

    private static final SocketLivenessServerStarter INSTANCE = new SocketLivenessServerStarter();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static SocketLivenessServerStarter getInstance() {
        return INSTANCE;
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    private SocketLivenessServer server;

    /**
     * Initializes a new {@link SocketLivenessServerStarter}.
     */
    private SocketLivenessServerStarter() {
        super();
    }

    /**
     * (Re-)Starts the HTTP liveness end-point using given configuration service.
     *
     * @param configService The configuration service to use
     * @return <code>true</code> if successfully started; otherwise <code>false</code>
     */
    public synchronized boolean startSocketLivenessServer(ConfigurationService configService) {
        stopSocketLivenessServer();

        boolean grizzlyLivenessEnabled = configService.getBoolProperty("com.openexchange.http.grizzly.livenessEnabled", false);
        if (grizzlyLivenessEnabled) {
            // Liveness end-point is offered by embedded Grizzly HTTP Web Server
            LOG.info("Liveness end-point is offered by embedded Grizzly HTTP Web Server. Therefore denying start-up of this dedicated liveness end-point.");
            return false;
        }

        String httpHost = configService.getProperty("com.openexchange.connector.networkListenerHost", "127.0.0.1");
        if ("*".equals(httpHost)) {
            httpHost = "0.0.0.0";
        }
        int livenessPort = configService.getIntProperty("com.openexchange.connector.livenessPort", 8016);

        SocketLivenessServer cleanUp = null;
        try {
            SocketLivenessServer server = new SocketLivenessServer(httpHost, livenessPort);
            cleanUp = server;
            server.start();
            LOG.info("Started HTTP liveness end-point on host {} at port {}", httpHost, Integer.valueOf(livenessPort));
            cleanUp = null;
            this.server = server;
            return true;
        } catch (Exception e) {
            LOG.error("Failed to start HTTP liveness end-point on host {} at port {}", httpHost, Integer.valueOf(livenessPort), e);
            return false;
        } finally {
            if (cleanUp != null) {
                cleanUp.stop();
            }
        }
    }

    /**
     * Stops the HTTP liveness end-point.
     */
    public synchronized void stopSocketLivenessServer() {
        SocketLivenessServer server = this.server;
        if (server != null) {
            this.server = null;
            server.stop();
            LOG.info("Stopped HTTP liveness end-point");
        }
    }

}
