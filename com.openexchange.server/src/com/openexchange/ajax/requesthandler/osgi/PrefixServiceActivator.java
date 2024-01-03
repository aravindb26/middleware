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

package com.openexchange.ajax.requesthandler.osgi;

import com.openexchange.ajax.requesthandler.DefaultDispatcherPrefixService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.mail.mime.utils.ImageMatcher;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link PrefixServiceActivator} - The activator for dispatcher prefix service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class PrefixServiceActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link PrefixServiceActivator}.
     */
    public PrefixServiceActivator() {
        super();
    }

	@Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { ConfigurationService.class };
    }

	@Override
	protected void startBundle() throws Exception {
	    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PrefixServiceActivator.class);
	    log.info("Starting bundle: {}", context.getBundle().getSymbolicName());
		try {
		    // Load prefix from configuration
	        String prefix = loadPrefix();
	        log.info("Using dispatcher prefix: {}", prefix);

	        // Initialize dispatcher prefix service
            DispatcherPrefixService prefixService = new DefaultDispatcherPrefixService(prefix);
	        ServerServiceRegistry.getInstance().addService(DispatcherPrefixService.class, prefixService);
	        ImageMatcher.setPrefixService(prefixService);
	        registerService(DispatcherPrefixService.class, prefixService);
            log.info("Dispatcher prefix service initialized & registered");
		} catch (Exception e) {
            log.error("Dispatcher prefix service could not be registered", e);
            throw e;
        }
	}

	@Override
	protected void stopBundle() throws Exception {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PrefixServiceActivator.class);
        log.info("Stopping bundle: {}", context.getBundle().getSymbolicName());
	    super.stopBundle();
	}

	private String loadPrefix() {
	    String prefix = getService(ConfigurationService.class).getProperty("com.openexchange.dispatcher.prefix", "/ajax/").trim();
        if (prefix.charAt(0) != '/') {
            prefix = '/' + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + '/';
        }
        return prefix;
	}

}
