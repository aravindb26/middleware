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
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.conference.webhook.impl.Utils.TYPE_JITSI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import com.openexchange.chronos.Conference;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.RecurrenceIterator;
import com.openexchange.conference.webhook.ConferenceWebhookProperties;
import com.openexchange.conference.webhook.exception.JitsitExceptionCodes;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link JitsiInterceptor}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class JitsiInterceptor extends AbstractConferenceInterceptor {

    /**
     * Initializes a new {@link JitsiInterceptor}.
     *
     * @param services A service lookup reference
     */
    public JitsiInterceptor(ServiceLookup services) {
        super(services, TYPE_JITSI);
    }

    @Override
    protected boolean isEnabled(CalendarSession session) throws OXException {
        return services.getServiceSafe(LeanConfigurationService.class).getBooleanProperty(
            session.getUserId(), session.getContextId(), ConferenceWebhookProperties.enableJitsiInterceptor);
    }

    @Override
    protected void checkIntegrity(CalendarSession session, Event originalEvent, Event updatedEvent, List<Conference> conferences) throws OXException {
        if (null == updatedEvent || null == conferences || conferences.isEmpty()) {
            return;
        }
        /*
         * check that the event's end date is not scheduled more than one year in advance
         */
        DateTime endDate = updatedEvent.getEndDate();
        if (null == endDate) {
            endDate = updatedEvent.getStartDate();
        }
        if (System.currentTimeMillis() < endDate.addDuration(new Duration(-1, 365, 0)).getTimestamp()) {
            throw JitsitExceptionCodes.NO_MORE_THAN_ONE_YEAR_IN_ADVANCE.create(updatedEvent.getId());
        }
        /*
         * check effective end of recurring event series as well
         */
        if (isSeriesMaster(updatedEvent)) {
            DefaultRecurrenceData recurrenceData = new DefaultRecurrenceData(updatedEvent);
            RecurrenceRule rule = initRecurrenceRule(recurrenceData.getRecurrenceRule());
            if (null == rule.getCount() && null == rule.getUntil()) {
                throw JitsitExceptionCodes.NO_MORE_THAN_ONE_YEAR_IN_ADVANCE.create(updatedEvent.getId());
            }
            DateTime rangeStart = updatedEvent.getStartDate();
            RecurrenceIterator<RecurrenceId> iterator = session.getRecurrenceService().iterateRecurrenceIds(recurrenceData);
            long maxDuration = TimeUnit.DAYS.toMillis(365L);
            DateTime rangeEnd;
            while (iterator.hasNext()) {
                rangeEnd = calculateEnd(updatedEvent, iterator.next());
                if (maxDuration < rangeEnd.getTimestamp() - rangeStart.getTimestamp()) {
                    throw JitsitExceptionCodes.NO_MORE_THAN_ONE_YEAR_IN_ADVANCE.create(updatedEvent.getId());
                }
            }
        }
    }

}
