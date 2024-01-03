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

package com.openexchange.chronos.service;

import com.openexchange.chronos.scheduling.ITipProcessorService;
import com.openexchange.chronos.scheduling.IncomingSchedulingMailFactory;
import com.openexchange.chronos.scheduling.SchedulingBroker;
import com.openexchange.exception.OXException;

/**
 * 
 * {@link SchedulingServices}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public interface SchedulingServices {

    /**
     * Provides access to additional utilities for scheduling.
     * 
     * @return The scheduling utilities
     * @see com.openexchange.chronos.scheduling.SchedulingBroker
     */
    SchedulingUtilities getSchedulingUtilities();

    /**
     * Get the processor service
     *
     * @return The service
     * @throws OXException If service is absent
     */
    ITipProcessorService getITipProcessorService() throws OXException;

    /**
     * Get the scheduling broker
     *
     * @return The broker
     * @throws OXException If broker is absent
     */
    SchedulingBroker getSchedulingBroker() throws OXException;

    /**
     * Get the mail factory for incoming scheduling mails
     *
     * @return The factory
     * @throws OXException if factory is absent
     */
    IncomingSchedulingMailFactory getIncomingSchedulingMailFactory() throws OXException;

}
