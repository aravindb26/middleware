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

package com.openexchange.drive.client.windows.rest.service.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.utils.URIBuilder;
import com.openexchange.drive.client.windows.rest.DriveClientProvider;
import com.openexchange.drive.client.windows.rest.DriveManifest;
import com.openexchange.drive.client.windows.rest.service.DriveClientWindowsUpdateService;
import com.openexchange.exception.OXException;
import com.openexchange.framework.request.RequestContextHolder;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.groupware.notify.hostname.HostnameService;
import com.openexchange.session.Session;
import com.openexchange.templating.OXTemplate;

/**
 * {@link DriveUpdateServiceKubernetesImpl}
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */
public class DriveClientWindowsUpdateServiceImpl implements DriveClientWindowsUpdateService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DriveClientWindowsUpdateServiceImpl.class);

    private final DriveClientProvider provider;

    public DriveClientWindowsUpdateServiceImpl(DriveClientProvider provider) {
        super();
        this.provider = provider;
    }

    @Override
    public String getInstallerDownloadUrl(Session session) throws OXException {
        DriveManifest manifest = provider.getManifest(session.getUserId(), session.getContextId());
        return prefixWithHostIfNeeded(session, getDownloadPath(session, manifest));
    }

    /**
     * Get the required capabilities
     *
     * @return The needed capability id`s
     */
    public List<String> getRequiredCapabilities() {
        return Arrays.asList("drive");
    }

    /**
     * Retrieves the values for the templates placeholder's.
     *
     * @param session The session
     *
     * @return A Map containing the values for the {@link OXTemplate}.
     * @throws OXException if branding isn't valid or if the values couldn't be retrieved
     */
    public Map<String, Object> getTemplateValues(Session session) throws OXException {

        Map<String, Object> values = new HashMap<String, Object>(12);

        DriveManifest manifest = provider.getManifest(session.getUserId(), session.getContextId());
        String downloadPath = getDownloadPath(session, manifest);

        values.put("MSI_URL", StringEscapeUtils.escapeXml(downloadPath));
        values.put("URL", StringEscapeUtils.escapeXml(downloadPath));
        values.put("NAME", StringEscapeUtils.escapeXml(manifest.getBrand()));
        values.put("VERSION", StringEscapeUtils.escapeXml(manifest.getVersion()));
        values.put("UPGRADECODE", StringEscapeUtils.escapeXml(manifest.getUpgradeCode()));

        return values;
    }

    private String getDownloadPath(Session session, DriveManifest manifest) {
        return prefixWithHostIfNeeded(session, manifest.getDownloadPath());
    }

    private static HostData getHostData(Session session) throws OXException {
        /*
         * get host data from request context or session parameter
         */
        com.openexchange.framework.request.RequestContext requestContext = RequestContextHolder.get();
        if (null != requestContext) {
            return requestContext.getHostData();
        }
        HostData hostData = (HostData) session.getParameter(HostnameService.PARAM_HOST_DATA);
        if (null != hostData) {
            return hostData;
        }
        throw OXException.general("No hostdata available");
    }

    private static String prefixWithHostIfNeeded(Session session, String downloadPath) {
        try {
            URI uri = new URI(downloadPath);
            if (uri.isAbsolute()) {
                LOG.debug("Download path \"{}\" from drive manifest is already in absolute format, using provided path as-is.", downloadPath);
                return downloadPath;
            }
            LOG.debug("Download path \"{}\" from drive manifest not in absolute format, attempting to prefix with scheme/host from request context.", downloadPath);
            HostData hostData = getHostData(session);
            String value = new URIBuilder().setScheme(hostData.isSecure() ? "https" : "http").setHost(hostData.getHost()).setPath(downloadPath).build().toString();
            LOG.debug("Successfully prefixed download path \"{}\" from drive manifest with scheme/host from request context to \"{}\".", downloadPath, value);
            return value;
        } catch (Exception e) {
            LOG.warn("Unexpected injecting hostname into download path \"{}\" from drive manifest, using provided path as-is.", downloadPath, e);
            return downloadPath;
        }
    }

}
