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

import com.openexchange.annotation.Nullable;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;

/**
 * A service interface which can be registered to perform operations based on an {@link IncomingSchedulingMessage}.
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.6
 */
public interface ITipProcessor {

    /**
     * Processes a given {@link IncomingSchedulingMessage}
     *
     * @param message The {@link IncomingSchedulingMessage}
     * @param session The {@link CalendarSession}
     * @return The {@link CalendarResult} of the processing or <code>null</code> if not applicable
     * @throws OXException In case of an error while processing. However, implementations should only
     *             throw exceptions in cases of missing services, etc. If the data is not applicable
     *             for whatever reason (permission, concurrency, etc.) a <code>null</code> shall be returned
     */
    @Nullable
    CalendarResult process(IncomingSchedulingMessage message, CalendarSession session) throws OXException;
}
