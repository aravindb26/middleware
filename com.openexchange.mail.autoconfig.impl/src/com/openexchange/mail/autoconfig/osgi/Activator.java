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

package com.openexchange.mail.autoconfig.osgi;

import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.database.DatabaseService;
import com.openexchange.mail.autoconfig.AutoconfigService;
import com.openexchange.mail.autoconfig.httpclient.properties.AutoConfigHttpConfiguration;
import com.openexchange.mail.autoconfig.httpclient.properties.ISPDBHttpConfiguration;
import com.openexchange.mail.autoconfig.internal.AutoconfigServiceImpl;
import com.openexchange.mail.autoconfig.tools.Services;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.SpecificHttpClientConfigProvider;

/**
 * {@link Activator}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Activator extends HousekeepingActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        //@formatter:off
        return new Class<?>[] { ConfigViewFactory.class, DatabaseService.class, ConfigurationService.class,
            SSLSocketFactoryProvider.class, HttpClientService.class, LeanConfigurationService.class };
        //@formatter:on
    }

    @Override
    protected void startBundle() throws Exception {
        Services.setServiceLookup(this);
        trackService(MailAccountStorageService.class);
        trackService(SSLConfigurationService.class);
        openTrackers();
        registerService(AutoconfigService.class, new AutoconfigServiceImpl(this));
        registerService(SpecificHttpClientConfigProvider.class, new AutoConfigHttpConfiguration(this));
        registerService(SpecificHttpClientConfigProvider.class, new ISPDBHttpConfiguration(this));
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        Services.setServiceLookup(null);
    }

}