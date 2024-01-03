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

package com.openexchange.mail.exportpdf.impl.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.file.storage.composition.IDBasedFolderAccessFactory;
import com.openexchange.html.HtmlService;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.mail.exportpdf.MailExportService;
import com.openexchange.mail.exportpdf.impl.MailExportConverterRegistry;
import com.openexchange.mail.exportpdf.impl.MailExportServiceImpl;
import com.openexchange.mail.exportpdf.impl.ThrottlingMailExportService;
import com.openexchange.mail.exportpdf.pdfa.PDFAService;
import com.openexchange.mail.service.MailService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.regional.RegionalSettingsService;
import com.openexchange.serverconfig.ServerConfigService;

/**
 * {@link MailExportActivator}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailExportActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(MailExportActivator.class);

    /**
     * Initialises a new {@link MailExportActivator}.
     */
    public MailExportActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { MailService.class, IDBasedFileAccessFactory.class, IDBasedFolderAccessFactory.class, //
            LeanConfigurationService.class, ServerConfigService.class, TranslatorFactory.class, PDFAService.class, HtmlService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class<?>[] { RegionalSettingsService.class };
    }

    @Override
    protected void startBundle() {
        try {
            LOG.info("Starting bundle '{}'", context.getBundle().getSymbolicName());
            MailExportConverterRankingServiceTracker converterTracker = new MailExportConverterRankingServiceTracker(context);
            MailExportHtmlBodyConverterRankingServiceTracker bodyConverterTracker = new MailExportHtmlBodyConverterRankingServiceTracker(context);
            rememberTracker(converterTracker);
            rememberTracker(bodyConverterTracker);
            openTrackers();
            MailExportServiceImpl mailExportService = new MailExportServiceImpl(getServiceSafe(ServerConfigService.class), getServiceSafe(MailService.class), getServiceSafe(IDBasedFileAccessFactory.class), getServiceSafe(IDBasedFolderAccessFactory.class), new MailExportConverterRegistry(this, bodyConverterTracker, converterTracker), this);
            registerService(MailExportService.class, new ThrottlingMailExportService(getServiceSafe(LeanConfigurationService.class), mailExportService));
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
