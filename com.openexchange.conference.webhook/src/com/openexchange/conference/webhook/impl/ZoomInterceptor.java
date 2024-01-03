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

import static com.openexchange.chronos.common.CalendarUtils.calculateEnd;
import static com.openexchange.chronos.common.CalendarUtils.initRecurrenceRule;
import static com.openexchange.chronos.common.CalendarUtils.isAllDay;
import static com.openexchange.chronos.common.CalendarUtils.isFloating;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.conference.webhook.impl.Utils.getConferences;
import static com.openexchange.conference.webhook.impl.Utils.matches;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import com.openexchange.chronos.Conference;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.RecurrenceIterator;
import com.openexchange.conference.webhook.ConferenceWebhookProperties;
import com.openexchange.conference.webhook.exception.ZoomExceptionCodes;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link ZoomInterceptor}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.4
 */
public class ZoomInterceptor extends AbstractConferenceInterceptor {

    /**
     * Initializes a new {@link ZoomInterceptor}.
     *
     * @param services A service lookup reference
     */
    public ZoomInterceptor(ServiceLookup services) {
        super(services, Utils.TYPE_ZOOM);
    }

    @Override
    protected boolean isEnabled(CalendarSession session) throws OXException {
        return services.getServiceSafe(LeanConfigurationService.class).getBooleanProperty(session.getUserId(), session.getContextId(), ConferenceWebhookProperties.enableZoomInterceptor);
    }

    @Override
    protected void checkIntegrity(CalendarSession session, Event originalEvent, Event updatedEvent, List<Conference> conferences) throws OXException {
        if (null == conferences || conferences.isEmpty() || null == updatedEvent) {
            return;
        }
        /*
         * no zoom meetings with floating dates
         */
        if (isAllDay(updatedEvent)) {
            throw ZoomExceptionCodes.NO_ALL_DAY_APPOINTMENTS.create(updatedEvent.getId());
        } else if (isFloating(updatedEvent)) {
            throw ZoomExceptionCodes.NO_FLOATING_APPOINTMENTS.create(updatedEvent.getId());
        }
        /*
         * no recurring zoom meeting that spans over more than a year
         */
        if (isSeriesMaster(updatedEvent)) {
            DefaultRecurrenceData recurrenceData = new DefaultRecurrenceData(updatedEvent);
            RecurrenceRule rule = initRecurrenceRule(recurrenceData.getRecurrenceRule());
            if (null == rule.getCount() && null == rule.getUntil()) {
                throw ZoomExceptionCodes.NO_SERIES_LONGER_THAN_A_YEAR.create(updatedEvent.getId(), String.valueOf(recurrenceData));
            }
            DateTime rangeStart = updatedEvent.getStartDate();
            RecurrenceIterator<RecurrenceId> iterator = session.getRecurrenceService().iterateRecurrenceIds(recurrenceData);
            long maxDuration = TimeUnit.DAYS.toMillis(365L);
            DateTime rangeEnd;
            while (iterator.hasNext()) {
                rangeEnd = calculateEnd(updatedEvent, iterator.next());
                if (maxDuration < rangeEnd.getTimestamp() - rangeStart.getTimestamp()) {
                    throw ZoomExceptionCodes.NO_SERIES_LONGER_THAN_A_YEAR.create(updatedEvent.getId(), String.valueOf(recurrenceData));
                }
            }
        }
        /*
         * perform further checks based on conferences in original event
         */
        if (null != originalEvent && isSeriesMaster(originalEvent) != isSeriesMaster(updatedEvent)) {
            /*
             * series <-> single event, ensure that all original conferences are not re-used
             */
            for (Conference originalConference : getConferences(originalEvent, conferenceType)) {
                if (conferences.stream().anyMatch(c -> matches(c, originalConference))) {
                    throw ZoomExceptionCodes.NO_SWITCH_TO_OR_FROM_SERIES.create(originalEvent.getId());
                }
            }
        }
    }

}
