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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.chronos.storage.operation.OSGiCalendarStorageOperation;
import com.openexchange.exception.OXException;
import com.openexchange.mailmapping.MailResolverService;
import com.openexchange.mailmapping.ResolveReply;
import com.openexchange.mailmapping.ResolvedMail;
import com.openexchange.server.ServiceLookup;
import com.openexchange.threadpool.AbstractTask;

/**
 * {@link AbstractCrossContextLookupTask}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @param <T> The return type
 * @since v7.10.6
 */
public abstract class AbstractCrossContextLookupTask<T> extends AbstractTask<T> {

    final ServiceLookup services;
    final Map<String, Attendee> attendeesByMailAdress;
    final Date from;
    final Date until;

    /**
     * Initializes a new {@link AbstractCrossContextLookupTask}.
     *
     * @param services The service lookup
     * @param attendeesByMailAdress External attendees mapped by their mail addresses
     * @param from The start of the requested time range
     * @param until The end of the requested time range
     */
    public AbstractCrossContextLookupTask(ServiceLookup services, Map<String, Attendee> attendeesByMailAdress, Date from, Date until) {
        super();
        this.services = services;
        this.attendeesByMailAdress = attendeesByMailAdress;
        this.from = from;
        this.until = until;
    }

    /*
     * ============================== Cross context lookup ==============================
     */

    protected Map<Attendee, List<Event>> getOverlappingEventsPerAttendee(int contextId, Collection<Attendee> attendees, BiPredicate<Event, Attendee> includePredicate) throws OXException {
        return new OSGiCalendarStorageOperation<Map<Attendee, List<Event>>>(services, contextId, Utils.ACCOUNT_ID) {

            @Override
            protected Map<Attendee, List<Event>> call(CalendarStorage storage) throws OXException {
                return new OverlappingEventsLoader(storage).load(attendees, from, until, includePredicate);
            }
        }.executeQuery();
    }

    protected List<Event> getOverlappingEvents(int contextId, Collection<Attendee> attendees) throws OXException {
        return new OSGiCalendarStorageOperation<List<Event>>(services, contextId, Utils.ACCOUNT_ID) {

            @Override
            protected List<Event> call(CalendarStorage storage) throws OXException {
                return new OverlappingEventsLoader(storage).loadEvents(attendees, from, until, true);
            }
        }.executeQuery();
    }

    protected Map<Integer, Map<Attendee, Attendee>> resolveToContexts() throws OXException {
        MailResolverService mailResolver = services.getOptionalService(MailResolverService.class);
        if (null == mailResolver || null == attendeesByMailAdress || attendeesByMailAdress.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Map<Attendee, Attendee>> resolvedUsersPerContext = new HashMap<Integer, Map<Attendee, Attendee>>();
        String[] mailAddresses = attendeesByMailAdress.keySet().toArray(new String[attendeesByMailAdress.size()]);
        ResolvedMail[] resolvedMails = mailResolver.resolveMultiple(mailAddresses);
        if (null == resolvedMails || resolvedMails.length != mailAddresses.length) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Unexpected result from mail resolver service");
        }
        for (int i = 0; i < resolvedMails.length; i++) {
            ResolvedMail resolvedMail = resolvedMails[i];
            if (null != resolvedMail && ResolveReply.ACCEPT.equals(resolvedMail.getResolveReply()) && 0 < resolvedMail.getContextID() && 0 < resolvedMail.getUserID()) {
                Attendee attendee = attendeesByMailAdress.get(mailAddresses[i]);
                if (null == attendee) {
                    continue;
                }
                Attendee resolvedAttendee = AttendeeMapper.getInstance().copy(attendee, null, (AttendeeField[]) null);
                resolvedAttendee.setEntity(resolvedMail.getUserID());
                resolvedAttendee.setCuType(CalendarUserType.INDIVIDUAL);
                Map<Attendee, Attendee> users = resolvedUsersPerContext.get(I(resolvedMail.getContextID()));
                if (null == users) {
                    users = new HashMap<Attendee, Attendee>();
                    resolvedUsersPerContext.put(I(resolvedMail.getContextID()), users);
                }
                users.put(resolvedAttendee, attendee);
            }
        }
        return resolvedUsersPerContext;
    }

}
