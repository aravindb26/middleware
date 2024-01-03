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

import java.util.List;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;

/**
 * 
 * {@link ITipProcessorService}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.6
 */
@SingletonService
public interface ITipProcessorService {

    /**
     * Analyzes an incoming {@link IncomingSchedulingMessage}.
     *
     * @param messages A list of {@link IncomingSchedulingMessage}s to analyze
     * @param session The {@link CalendarSession}
     * @return A List of {@link ITipAnalysis}' for the given messages
     * @throws OXException In case of error during the analyzing
     */
    List<ITipAnalysis> analyze(List<IncomingSchedulingMessage> messages, CalendarSession session) throws OXException;

    /**
     * Analyzes an {@link IncomingSchedulingMessage}.
     *
     * @param message The {@link IncomingSchedulingMessage} to analyze
     * @param session The {@link CalendarSession}
     * @return An {@link ITipAnalysis} for the given message
     * @throws OXException In case of error during the analyzing
     */
    ITipAnalysis analyze(IncomingSchedulingMessage message, CalendarSession session) throws OXException;

    /**
     * Processes a List of {@link IncomingSchedulingMessage}s.
     * This may contain several CRUD operations on the underlying calendar and a comprehensive analysis.
     *
     * @param messages The List of {@link IncomingSchedulingMessage}s to process
     * @param session The {@link CalendarSession}
     * @return A List of {@link ProcessResult}s containing all operations and {@link ITipAnalysis}' based on the Messages itself and the operations.
     * @throws OXException In case of error during the processing
     */
    List<ProcessResult> process(List<IncomingSchedulingMessage> messages, CalendarSession session) throws OXException;

    /**
     * Processes an {@link IncomingSchedulingMessage}.
     * This may contain several CRUD operations on the underlying calendar and a comprehensive analysis.
     *
     * @param message The {@link IncomingSchedulingMessage} to process
     * @param session The {@link CalendarSession}
     * @return A {@link ProcessResult} containing all performed operations and the new message status after the operations
     * @throws OXException In case of error during the processing
     */
    ProcessResult process(IncomingSchedulingMessage message, CalendarSession session) throws OXException;
}
