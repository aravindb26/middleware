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

package com.openexchange.chronos.provider.composition;

import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.session.Session;

/**
 * {@link IDBasedCalendarAccessFactory}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
@SingletonService
public interface IDBasedCalendarAccessFactory {

    /**
     * Initializes a new ID-based calendar access for the supplied session.
     * <p/>
     * Supplementary calendar parameters may be configured in the returned reference.
     *
     * @param session The session to create the access for
     * @return The calendar access
     * @see CalendarParameters
     */
    IDBasedCalendarAccess createAccess(Session session) throws OXException;

    /**
     * Initializes a new ID-based calendar access for the supplied session.
     * <p/>
     * Supplementary calendar parameters may also be configured in the returned reference.
     *
     * @param session The session to create the access for
     * @param parameters The calendar parameters to take over, or <code>null</code> if there are none
     * @return The calendar access
     * @see CalendarParameters
     */
    IDBasedCalendarAccess createAccess(Session session, CalendarParameters parameters) throws OXException;

}