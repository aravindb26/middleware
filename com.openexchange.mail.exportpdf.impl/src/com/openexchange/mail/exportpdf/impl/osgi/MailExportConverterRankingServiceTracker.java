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
import com.openexchange.mail.exportpdf.converter.MailExportConverter;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;

/**
 * {@link MailExportConverterRankingServiceTracker} - Tracks ranked {@link MailExportConverter}s
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailExportConverterRankingServiceTracker extends RankingAwareNearRegistryServiceTracker<MailExportConverter> {

    private static final Logger LOG = LoggerFactory.getLogger(MailExportConverterRankingServiceTracker.class);

    /**
     * Initialises a new {@link MailExportConverterRankingServiceTracker}.
     *
     * @param context The bundle context
     */
    public MailExportConverterRankingServiceTracker(BundleContext context) {
        super(context, MailExportConverter.class);
    }

    @Override
    protected void onServiceAdded(MailExportConverter service) {
        LOG.info("{} added to registry", service.getClass());
    }

    @Override
    protected void onServiceRemoved(MailExportConverter service) {
        LOG.info("{} removed from registry", service.getClass());
    }
}
