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

package com.openexchange.mail.exportpdf.collabora.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.DistributedFileManagement;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.converter.ReplacingInputStream.Replacement;

/**
 * {@link DistributedFileImageReplacement}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class DistributedFileImageReplacement implements Replacement {

    private static final String ENV_POD_IP = "POD_IP";
    private static final String NETWORK_LISTENER_PORT_CONFIG = "com.openexchange.connector.networkListenerPort";
    private static final Logger LOG = LoggerFactory.getLogger(DistributedFileImageReplacement.class);

    private final Map<String /* cid */ , String /* url */> imageURLs = new HashMap<>();
    private final ManagedFileManagement managedFileManagement;
    private final ConfigurationService configurationService;

    /**
     * Initializes a new {@link DistributedFileImageReplacement}.
     *
     * @param configurationService The {@link ConfigurationService}
     * @param managedFileManagement The {@link ManagedFileManagement}
     */
    public DistributedFileImageReplacement(ConfigurationService configurationService, ManagedFileManagement managedFileManagement) {
        this.configurationService = configurationService;
        this.managedFileManagement = managedFileManagement;
    }

    /**
     * Internal method to obtain the IP or hostname where this MW node is accessible
     *
     * @return The IP or hostname on which this MW is accessible
     */
    private String getIPOrHostname() {
        String hostname = null;

        /* Trying to obtain the IP/host via env. variable usually set in an k8s environment */
        try {
            hostname = System.getenv(ENV_POD_IP);
        } catch (Exception e) {
            LOG.trace("Unable to obtain hostname from environment variable %s: %s".formatted(ENV_POD_IP, e.getMessage()));
        }

        /* Trying to obtain the hostname via syscall */
        if (Strings.isEmpty(hostname)) {
            try {
                hostname = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                LOG.trace("Unable to obtain hostname from syscall: %s".formatted(e.getMessage()));
            }
        }

        /* Fallback, but most probably wrong */
        if (Strings.isEmpty(hostname)) {
            hostname = "localhost";
        }

        LOG.trace("hostname: {}", hostname);
        return hostname;
    }

    /**
     * Internal method to obtain the port number where this MW node is accessible
     *
     * @return The Port number on which this MW is accessible
     */
    private int getPort() {
        return configurationService.getIntProperty(NETWORK_LISTENER_PORT_CONFIG, 8009);
    }

    /**
     * Publishes a {@link InlineImage}s via the {@link DistributedFileManagement}
     *
     * @param inlineImage The image to publish
     * @throws OXException if an error is occurred
     */
    private void publishImages(InlineImage inlineImage) throws OXException {
        ManagedFile m = managedFileManagement.createManagedFile(inlineImage.getData(), true /* distributed */);
        String url = "http://%s:%s%s/%s".formatted(getIPOrHostname(), Integer.toString(getPort()), DistributedFileManagement.PATH, m.getID());
        imageURLs.putIfAbsent(inlineImage.getCid(), url);
    }

    /**
     * Publishes a collection of {@link InlineImage}s via the {@link DistributedFileManagement}
     *
     * @param inlineImages The collection of images to publish
     * @return this instance for chained calls
     * @throws OXException if an error is occurred
     */
    public DistributedFileImageReplacement publishImages(Collection<InlineImage> inlineImages) throws OXException {
        for (var inlineImage : inlineImages) {
            publishImages(inlineImage);
        }
        return this;
    }

    @Override
    public InputStream getReplacementData(int matchCount, MatchResult match) {
        if (match.groupCount() < 1 || !imageURLs.containsKey(match.group(1) /* cid */)) {
            return null;
        }

        /* Get the new URL by CID. This URL will actually replace the image in the body */
        final String url = imageURLs.get(match.group(1) /* cid */);
        return new ByteArrayInputStream(("src=\"" + url + "\"").getBytes(StandardCharsets.UTF_8));
    }
}
