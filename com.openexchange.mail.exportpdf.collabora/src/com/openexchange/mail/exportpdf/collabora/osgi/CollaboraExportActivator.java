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

package com.openexchange.mail.exportpdf.collabora.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.filemanagement.DistributedFileManagement;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.mail.exportpdf.collabora.impl.CollaboraHtmlBodyConverter;
import com.openexchange.mail.exportpdf.collabora.impl.CollaboraMailExportConverter;
import com.openexchange.mail.exportpdf.converter.MailExportConverter;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverter;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.user.UserService;

/**
 * {@link CollaboraExportActivator}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CollaboraExportActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboraExportActivator.class);
    private static final int CONVERTER_RANKING = 100;
    private static final int BODY_CONVERTER_RANKING = 100;

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { LeanConfigurationService.class, ConfigurationService.class, UserService.class, ManagedFileManagement.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            CollaboraMailExportConverter collaboraConverter = new CollaboraMailExportConverter(this);
            registerService(MailExportConverter.class, collaboraConverter, CONVERTER_RANKING);
            registerService(MailExportHtmlBodyConverter.class, new CollaboraHtmlBodyConverter(collaboraConverter, this), BODY_CONVERTER_RANKING);
            trackService(DistributedFileManagement.class);
            openTrackers();
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
