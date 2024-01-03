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

package com.openexchange.chronos.scheduling;

import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;

/**
 * 
 * {@link SchedulingAnalyzer}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public interface SchedulingAnalyzer {

    /**
     * Get the {@link SchedulingMethod} the analyzer is responsible for
     *
     * @return The supported method
     */
    SchedulingMethod getMethod();

    /**
     * Analyzes the {@link IncomingSchedulingMessage}
     *
     * @param message The message
     * @param session The calendar session
     * @return The analysis
     * @throws OXException In case of error during analysis
     */
    ITipAnalysis analyze(IncomingSchedulingMessage message, CalendarSession session) throws OXException;

}
