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

package com.openexchange.conference.webhook.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.service.CalendarHandler;
import com.openexchange.chronos.service.CalendarInterceptor;
import com.openexchange.conference.webhook.impl.JitsiInterceptor;
import com.openexchange.conference.webhook.impl.WebhookConferenceHandler;
import com.openexchange.conference.webhook.impl.ZoomInterceptor;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.conversion.ConversionService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.HttpClientService;

/**
 * 
 * {@link Activator}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.4
 */
public class Activator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { HttpClientService.class, ConversionService.class, LeanConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("starting bundle {}", context.getBundle());
        try {
            /**
             * Register CalendarInterceptors
             */
            registerService(CalendarInterceptor.class, new ZoomInterceptor(this));
            registerService(CalendarInterceptor.class, new JitsiInterceptor(this));
            /**
             * Register CalendarHandler
             */
            registerService(CalendarHandler.class, new WebhookConferenceHandler(this));
        } catch (Exception e) {
            LOG.error("error starting {}", context.getBundle(), e);
            throw e;
        }
    }

}
