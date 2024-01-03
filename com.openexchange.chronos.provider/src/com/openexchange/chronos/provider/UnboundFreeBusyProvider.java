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

package com.openexchange.chronos.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.FreeBusyResult;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;

/**
 * {@link UnboundFreeBusyProvider}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public abstract class UnboundFreeBusyProvider implements FreeBusyProvider {

    /**
     * Initializes a new {@link UnboundFreeBusyProvider}.
     */
    protected UnboundFreeBusyProvider() {
        super();
    }

    @Override
    public Map<Attendee, Map<Integer, FreeBusyResult>> query(Session session, List<Attendee> attendees, Date from, Date until, boolean merge, CalendarParameters parameters) throws OXException {
        List<Attendee> supportedAttendees = filterUnsupported(attendees);
        if (null == supportedAttendees || supportedAttendees.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Attendee, FreeBusyResult> results = getFreeBusy(session, supportedAttendees, from, until, merge, parameters);
        if (null == results || results.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Attendee, Map<Integer, FreeBusyResult>> freeBusyResults = new LinkedHashMap<Attendee, Map<Integer, FreeBusyResult>>(results.size());
        for (Entry<Attendee, FreeBusyResult> entry : results.entrySet()) {
            freeBusyResults.put(entry.getKey(), Collections.singletonMap(NO_ACCOUNT, entry.getValue()));
        }
        return freeBusyResults;
    }

    /**
     * Filters attendees that are not considered as supported by this provider from the given attendee list, as per {@link #isSupported(Attendee)}.
     *
     * @param attendees The attendees to filter
     * @return The filtered attendees
     */
    protected List<Attendee> filterUnsupported(List<Attendee> attendees) {
        if (null == attendees || attendees.isEmpty()) {
            return attendees;
        }
        List<Attendee> supportedAttendees = new ArrayList<Attendee>(attendees.size());
        for (Attendee attendee : attendees) {
            if (isSupported(attendee)) {
                supportedAttendees.add(attendee);
            }
        }
        return supportedAttendees;
    }

    /**
     * Gets a value indicating whether a specific attendee supported and should be considered during free/busy lookups or not.
     * <p/>
     * In this default implementation, all <i>external</i> attendees with a valid <code>mailto:</code>-URI are considered as supported.
     * Override if applicable.
     *
     * @param attendees The attendee to check
     * @return <code>true</code> if the attendee is supported, <code>false</code>, otherwise
     */
    protected boolean isSupported(Attendee attendee) {
        return null != attendee && false == CalendarUtils.isInternal(attendee) && null != CalendarUtils.optEMailAddress(attendee.getUri());
    }

    /**
     * Queries the free/busy time for a list of attendees.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_MASK_UID}</li>
     * </ul>
     *
     * @param session The session of the user requesting the free/busy data
     * @param attendees The queried attendees, previously filtered via {@link #filterUnsupported(List)}
     * @param from The start of the requested time range
     * @param until The end of the requested time range
     * @param parameters Additional arbitrary parameters, or <code>null</code> if not used
     * @param merge <code>true</code> to merge the resulting free/busy-times, <code>false</code>, otherwise
     * @return The free/busy times for each of the attendees
     */
    protected abstract Map<Attendee, FreeBusyResult> getFreeBusy(Session session, List<Attendee> attendees, Date from, Date until, boolean merge, CalendarParameters parameters) throws OXException;

}
