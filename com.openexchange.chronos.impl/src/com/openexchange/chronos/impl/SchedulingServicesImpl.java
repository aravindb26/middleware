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

package com.openexchange.chronos.impl;

import com.openexchange.chronos.scheduling.ITipProcessorService;
import com.openexchange.chronos.scheduling.IncomingSchedulingMailFactory;
import com.openexchange.chronos.scheduling.SchedulingBroker;
import com.openexchange.chronos.service.SchedulingServices;
import com.openexchange.chronos.service.SchedulingUtilities;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;

/**
 * 
 * {@link SchedulingServicesImpl}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class SchedulingServicesImpl implements SchedulingServices {

    private final ServiceLookup services;
    private final SchedulingUtilitiesImpl schedulingUtils;

    /**
     * Initializes a new {@link SchedulingServicesImpl}.
     * 
     * @param services The service lookup
     *
     */
    public SchedulingServicesImpl(ServiceLookup services) {
        super();
        this.services = services;
        this.schedulingUtils = new SchedulingUtilitiesImpl(services);
    }

    @Override
    public SchedulingUtilities getSchedulingUtilities() {
        return schedulingUtils;
    }

    @Override
    public IncomingSchedulingMailFactory getIncomingSchedulingMailFactory() throws OXException {
        return services.ofOptionalService(IncomingSchedulingMailFactory.class)
            .orElseThrow(() -> ServiceExceptionCode.SERVICE_UNAVAILABLE.create(IncomingSchedulingMailFactory.class.getName()));
    }

    @Override
    public ITipProcessorService getITipProcessorService() throws OXException {
        return services.ofOptionalService(ITipProcessorService.class)
            .orElseThrow(() -> ServiceExceptionCode.SERVICE_UNAVAILABLE.create(ITipProcessorService.class.getName()));
    }

    @Override
    public SchedulingBroker getSchedulingBroker() throws OXException {
        return services.ofOptionalService(SchedulingBroker.class)
            .orElseThrow(() -> ServiceExceptionCode.SERVICE_UNAVAILABLE.create(SchedulingBroker.class.getName()));
    }

}
