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

package com.openexchange.dataretention.csv.osgi;

import com.openexchange.config.ConfigurationService;
import com.openexchange.dataretention.DataRetentionService;
import com.openexchange.dataretention.csv.CSVDataRetentionConfig;
import com.openexchange.dataretention.csv.CSVDataRetentionService;
import com.openexchange.dataretention.csv.CSVWriter;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link CSVDataRetentionActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CSVDataRetentionActivator extends HousekeepingActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CSVDataRetentionActivator.class);

    /**
     * Initializes a new {@link CSVDataRetentionActivator}.
     */
    public CSVDataRetentionActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        try {
            CSVDataRetentionConfig.getInstance().init((ConfigurationService) getService(clazz));
        } catch (OXException e) {
            LOG.error("", e);
        }
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        // Nothing to do. Wait for re-availability.
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            CSVDataRetentionConfig.getInstance().init(getService(ConfigurationService.class));
            registerService(DataRetentionService.class, new CSVDataRetentionService());
        } catch (Exception e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
		CSVWriter.releaseInstance();
		CSVDataRetentionConfig.releaseInstance();
		super.stopBundle();
    }

}
