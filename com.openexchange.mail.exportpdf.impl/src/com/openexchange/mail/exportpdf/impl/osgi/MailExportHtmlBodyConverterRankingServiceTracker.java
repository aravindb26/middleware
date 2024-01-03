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

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverter;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;

/**
 * {@link MailExportHtmlBodyConverterRankingServiceTracker} - Tracks ranked {@link MailExportHtmlBodyConverter}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class MailExportHtmlBodyConverterRankingServiceTracker extends RankingAwareNearRegistryServiceTracker<MailExportHtmlBodyConverter> {

    private static final Logger LOG = LoggerFactory.getLogger(MailExportHtmlBodyConverterRankingServiceTracker.class);

    /**
     * Initializes a new {@link MailExportHtmlBodyConverterRankingServiceTracker}.
     *
     * @param context The bundle context
     */
    public MailExportHtmlBodyConverterRankingServiceTracker(BundleContext context) {
        super(context, MailExportHtmlBodyConverter.class);
    }

    @Override
    protected void onServiceAdded(MailExportHtmlBodyConverter service) {
        LOG.info("{} added to registry", service.getClass());
    }

    @Override
    protected void onServiceRemoved(MailExportHtmlBodyConverter service) {
        LOG.info("{} removed from registry", service.getClass());
    }
}
