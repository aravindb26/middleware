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

package com.openexchange.conference.webhook.impl;

import static com.openexchange.chronos.common.CalendarUtils.hasExternalOrganizer;
import static com.openexchange.conference.webhook.impl.Utils.getConferences;
import java.util.List;
import java.util.Set;
import com.openexchange.chronos.Conference;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.service.CalendarInterceptor;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link AbstractConferenceInterceptor}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public abstract class AbstractConferenceInterceptor implements CalendarInterceptor {

    private static final Set<EventField> RELEVANT_FIELDS = com.openexchange.tools.arrays.Collections.unmodifiableSet(EventField.CONFERENCES, EventField.START_DATE, EventField.END_DATE);

    protected final ServiceLookup services;
    protected final String conferenceType;

    /**
     * Initializes a new {@link AbstractConferenceInterceptor}.
     *
     * @param services A service lookup reference
     * @param conferenceType The underlying conference type
     */
    protected AbstractConferenceInterceptor(ServiceLookup services, String conferenceType) {
        super();
        this.services = services;
        this.conferenceType = conferenceType;
    }

    @Override
    public Set<EventField> getRelevantFields() {
        return RELEVANT_FIELDS;
    }

    @Override
    public void onBeforeUpdate(CalendarSession session, String folderId, Event originalEvent, Event updatedEvent) throws OXException {
        if (false == isEnabled(session) || hasExternalOrganizer(updatedEvent)) {
            return;
        }
        /*
         * check general integrity of updated event if applicable
         */
        List<Conference> updatedConferences = getConferences(updatedEvent, conferenceType);
        if (updatedConferences.isEmpty() || EventMapper.getInstance().equalsByFields(originalEvent, updatedEvent, EventField.SERIES_ID, EventField.START_DATE, EventField.END_DATE, EventField.RECURRENCE_RULE)) {
            return;
        }
        checkIntegrity(session, originalEvent, updatedEvent, updatedConferences);
    }

    @Override
    public void onBeforeCreate(CalendarSession session, String folderId, Event newEvent) throws OXException {
        if (false == isEnabled(session) || hasExternalOrganizer(newEvent)) {
            return;
        }
        /*
         * check integrity if applicable
         */
        List<Conference> conferences = getConferences(newEvent, conferenceType);
        if (conferences.isEmpty()) {
            return;
        }
        checkIntegrity(session, null, newEvent, conferences);
    }

    @Override
    public void onBeforeDelete(CalendarSession session, String folderId, Event deletedEvent) throws OXException {
        // nothing to do
    }

    /**
     * Gets a value indicating whether the interceptor is enabled or not.
     * 
     * @param session The calendar session
     * @return <code>true</code> if the interceptor is enabled, <code>false</code>, otherwise
     */
    protected abstract boolean isEnabled(CalendarSession session) throws OXException;

    /**
     * Checks the integrity of a new or updated event that is being stored, along with conferences of this interceptor's conference type,
     * throwing an appropriate exception if the event contains unsupported conference data.
     * 
     * @param session The calendar session
     * @param originalEvent The original event, or <code>null</code> for newly created events
     * @param updatedEvent The new or updated event
     * @param conferences The conferences of this interceptor's type within the new or updated event
     */
    protected abstract void checkIntegrity(CalendarSession session, Event originalEvent, Event updatedEvent, List<Conference> conferences) throws OXException;

}
