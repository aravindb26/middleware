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

package com.openexchange.chronos.provider.internal;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.provider.FreeBusyProvider;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.FreeBusyResult;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link InternalFreeBusyProvider}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class InternalFreeBusyProvider implements FreeBusyProvider {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link InternalFreeBusyProvider}.
     *
     * @param services A service lookup reference
     */
    public InternalFreeBusyProvider(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public Map<Attendee, Map<Integer, FreeBusyResult>> query(Session session, List<Attendee> attendees, Date from, Date until, boolean merge, CalendarParameters parameters) throws OXException {
        CalendarSession calendarSession = services.getService(CalendarService.class).init(session, parameters);
        Map<Attendee, FreeBusyResult> results = calendarSession.getFreeBusyService().getFreeBusy(calendarSession, attendees, from, until, merge);
        if (null == results || results.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Attendee, Map<Integer, FreeBusyResult>> resultsPerAccountId = new LinkedHashMap<Attendee, Map<Integer, FreeBusyResult>>(results.size());
        for (Entry<Attendee, FreeBusyResult> entry : results.entrySet()) {
            resultsPerAccountId.put(entry.getKey(), Collections.singletonMap(I(Constants.ACCOUNT_ID), entry.getValue()));
        }
        return resultsPerAccountId;
    }

}
