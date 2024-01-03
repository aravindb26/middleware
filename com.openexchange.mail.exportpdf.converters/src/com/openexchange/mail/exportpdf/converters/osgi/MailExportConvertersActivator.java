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

package com.openexchange.mail.exportpdf.converters.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.mail.exportpdf.MailExportService;
import com.openexchange.mail.exportpdf.converter.MailExportConverter;
import com.openexchange.mail.exportpdf.converters.ImageMailExportConverter;
import com.openexchange.mail.exportpdf.converters.PDFMailExportConverter;
import com.openexchange.mail.exportpdf.converters.TextMailExportConverter;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.regional.RegionalSettingsService;

/**
 * {@link MailExportConvertersActivator}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailExportConvertersActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(MailExportConvertersActivator.class);

    /**
     * Initialises a new {@link MailExportConvertersActivator}.
     */
    public MailExportConvertersActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { MailExportService.class, LeanConfigurationService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class<?>[] { RegionalSettingsService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LeanConfigurationService leanConfigurationService = getServiceSafe(LeanConfigurationService.class);
            registerService(MailExportConverter.class, new ImageMailExportConverter(leanConfigurationService), Integer.MAX_VALUE);
            registerService(MailExportConverter.class, new PDFMailExportConverter(leanConfigurationService), Integer.MAX_VALUE);
            registerService(MailExportConverter.class, new TextMailExportConverter(leanConfigurationService), Integer.MAX_VALUE);
        } catch (Exception e) {
            LOG.error("Failed to start bundle {}", context.getBundle().getSymbolicName(), e);
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("Stopping bundle '{}'", context.getBundle().getSymbolicName());
        super.stopBundle();
    }
}
