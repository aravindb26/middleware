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

package com.openexchange.drive.client.windows.rest.osgi;

import org.osgi.service.http.HttpService;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.drive.client.windows.rest.Constants;
import com.openexchange.drive.client.windows.rest.DriveClientProperty;
import com.openexchange.drive.client.windows.rest.DriveClientProvider;
import com.openexchange.drive.client.windows.rest.DriveClientRestProviderImpl;
import com.openexchange.drive.client.windows.rest.DriveClientWindowsRestHttpClient;
import com.openexchange.drive.client.windows.rest.service.DriveClientWindowsUpdateService;
import com.openexchange.drive.client.windows.rest.service.internal.DriveClientWindowsUpdateServiceImpl;
import com.openexchange.drive.client.windows.rest.servlet.InstallServlet;
import com.openexchange.drive.client.windows.rest.servlet.UpdatesXMLServlet;
import com.openexchange.groupware.notify.hostname.HostnameService;
import com.openexchange.login.Interface;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.service.http.HttpServices;
import com.openexchange.rest.client.httpclient.DefaultHttpClientConfigProvider;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.SpecificHttpClientConfigProvider;
import com.openexchange.templating.TemplateService;
import com.openexchange.userconf.UserConfigurationService;

/**
 * {@link DriveClientWindowsActivator}
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */
public class DriveClientWindowsActivator extends HousekeepingActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DriveClientWindowsActivator.class);

    private String v1UpdateXmlServletAlias;
    private String installServletAlias;

    /**
     * Initializes a new {@link DriveClientWindowsActivator}.
     */
    public DriveClientWindowsActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { UserConfigurationService.class, HttpService.class, DispatcherPrefixService.class,
            LeanConfigurationService.class, TemplateService.class, HttpClientService.class, CapabilityService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class<?>[] { HostnameService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        LOG.info("Starting bundle: {}", context.getBundle().getSymbolicName());

        Services.setServiceLookup(this);

        String prefix = getServiceSafe(DispatcherPrefixService.class).getPrefix();
        v1UpdateXmlServletAlias = prefix + Constants.UPDATE_XML_SERVLET;
        installServletAlias = prefix + Constants.INSTALL_SERVLET;

        LeanConfigurationService leanConfigService = getServiceSafe(LeanConfigurationService.class);
        LOG.info("Creating kubernetes provider running in cluster mode {}.", leanConfigService.getProperty(DriveClientProperty.CLUSTER_MODE));

        //register rest client
        registerService(SpecificHttpClientConfigProvider.class,
            new DefaultHttpClientConfigProvider(DriveClientWindowsRestHttpClient.HTTP_CLIENT_ID, "Open-Xchange DriveClientWindows HTTP client"));

        //register update service
        DriveClientProvider driveClientProvider = new DriveClientRestProviderImpl();
        DriveClientWindowsUpdateService updateService =
            new DriveClientWindowsUpdateServiceImpl(driveClientProvider);
        registerService(DriveClientWindowsUpdateService.class, updateService);

        //register servlets
        TemplateService templateService = getServiceSafe(TemplateService.class);
        HttpService httpService = getServiceSafe(HttpService.class);
        httpService.registerServlet(v1UpdateXmlServletAlias, new UpdatesXMLServlet(templateService, updateService), null, null);
        httpService.registerServlet(installServletAlias, new InstallServlet(updateService), null, null);

        LOG.info("Bundle started: {}", context.getBundle().getSymbolicName());
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        LOG.info("Stopping bundle: {}", context.getBundle().getSymbolicName());

        HttpService httpService = getServiceSafe(HttpService.class);
        if (v1UpdateXmlServletAlias != null) {
            HttpServices.unregister(v1UpdateXmlServletAlias, httpService);
            v1UpdateXmlServletAlias = null;
        }
        if (installServletAlias != null) {
            HttpServices.unregister(installServletAlias, httpService);
            installServletAlias = null;
        }
        super.stopBundle();
        Services.setServiceLookup(null);
        LOG.info("Bundle stopped: {}", context.getBundle().getSymbolicName());
    }

}
