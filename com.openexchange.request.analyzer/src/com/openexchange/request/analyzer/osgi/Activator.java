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

package com.openexchange.request.analyzer.osgi;

import org.slf4j.LoggerFactory;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestAnalyzerService;
import com.openexchange.request.analyzer.impl.RequestAnalyzerServiceImpl;

/**
 * {@link Activator}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class Activator extends HousekeepingActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected void startBundle() throws Exception {
        LoggerFactory.getLogger(Activator.class).info("Starting bundle com.openexchange.request.analyzer");
        RequestAnalyzerServiceImpl service = new RequestAnalyzerServiceImpl(context);
        track(RequestAnalyzer.class, service);
        openTrackers();
        registerService(RequestAnalyzerService.class, service);
    }

    @Override
    protected void stopBundle() throws Exception {
        LoggerFactory.getLogger(Activator.class).info("Stopping bundle com.openexchange.request.analyzer");
        super.stopBundle();
    }

}
