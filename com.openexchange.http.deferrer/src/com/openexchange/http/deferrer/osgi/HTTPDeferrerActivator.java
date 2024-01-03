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

package com.openexchange.http.deferrer.osgi;

import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.http.deferrer.CustomRedirectURLDetermination;
import com.openexchange.http.deferrer.DeferringURLService;
import com.openexchange.http.deferrer.impl.AbstractDeferringURLService;
import com.openexchange.http.deferrer.impl.PropertyReadingDeferringURLService;
import com.openexchange.http.deferrer.servlet.DeferrerServlet;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.SimpleRegistryListener;
import com.openexchange.osgi.service.http.HttpServices;

/**
 * {@link HTTPDeferrerActivator}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class HTTPDeferrerActivator extends HousekeepingActivator {

    private String alias;

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { LeanConfigurationService.class, ConfigViewFactory.class, ConfigurationService.class, HttpService.class, DispatcherPrefixService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        final Logger logger = org.slf4j.LoggerFactory.getLogger(HTTPDeferrerActivator.class);
        logger.info("Starting bundle {}", context.getBundle().getSymbolicName());

        final DispatcherPrefixService prefixService = getService(DispatcherPrefixService.class);
        AbstractDeferringURLService.PREFIX.set(prefixService);
        String alias = prefixService.getPrefix() + "defer";
        getService(HttpService.class).registerServlet(alias, new DeferrerServlet(getServiceSafe(LeanConfigurationService.class)), null, null);
        this.alias = alias;

        registerService(DeferringURLService.class, new PropertyReadingDeferringURLService(this));

        track(CustomRedirectURLDetermination.class, new SimpleRegistryListener<CustomRedirectURLDetermination>() {

			@Override
			public void added(
					ServiceReference<CustomRedirectURLDetermination> ref,
					CustomRedirectURLDetermination service) {
				DeferrerServlet.CUSTOM_HANDLERS.add(service);
			}

			@Override
			public void removed(
					ServiceReference<CustomRedirectURLDetermination> ref,
					CustomRedirectURLDetermination service) {
				DeferrerServlet.CUSTOM_HANDLERS.remove(service);
			}
		});

        openTrackers();
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        final Logger logger = org.slf4j.LoggerFactory.getLogger(HTTPDeferrerActivator.class);
        logger.info("Stopping bundle {}", context.getBundle().getSymbolicName());

        HttpService service = getService(HttpService.class);
        if (null != service) {
            String alias = this.alias;
            if (null != alias) {
                this.alias = null;
                HttpServices.unregister(alias, service);
            }
        }
        super.stopBundle();
    }

}
