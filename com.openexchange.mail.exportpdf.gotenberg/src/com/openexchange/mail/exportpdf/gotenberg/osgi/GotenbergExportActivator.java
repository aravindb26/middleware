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

package com.openexchange.mail.exportpdf.gotenberg.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.mail.exportpdf.converter.MailExportConverter;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverter;
import com.openexchange.mail.exportpdf.gotenberg.impl.GotenbergHtmlBodyConverter;
import com.openexchange.mail.exportpdf.gotenberg.impl.GotenbergMailExportConverter;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link GotenbergExportActivator}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class GotenbergExportActivator extends HousekeepingActivator {

    private static final int CONVERTER_RANKING = 500;
    private static final int BODY_CONVERTER_RANKING = 500;

    private static final Logger LOG = LoggerFactory.getLogger(GotenbergExportActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { LeanConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            GotenbergMailExportConverter converter = new GotenbergMailExportConverter(this);
            registerService(MailExportConverter.class, converter, CONVERTER_RANKING);
            registerService(MailExportHtmlBodyConverter.class, new GotenbergHtmlBodyConverter(converter), BODY_CONVERTER_RANKING);
            LOG.info("Starting bundle '{}'", context.getBundle().getSymbolicName());
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
