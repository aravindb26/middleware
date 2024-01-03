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

package com.openexchange.ajax.chronos.bugs;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractSecondUserChronosTest;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.models.ChronosRecurrenceInfoResponse;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * Confirmation buttons not working when inviting a person to a series exception
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.org">Tobias Friedrich</a>
 */
public class MWB2231Test extends AbstractSecondUserChronosTest {

    @Test
    public void testGetRecurrenceInfoForOrphanedInstance() throws Throwable {
        /*
         * as user a, create appointment series in personal calendar
         */
        EventData seriesEventData = EventFactory.createSeriesEvent(testUser.getUserId(), UUIDs.getUnformattedStringFromRandom(), 2, defaultFolderId);
        EventData createdEventSeries = eventManager.createEvent(seriesEventData, true);
        /*
         * as user a, invite user b to first occurrence of series
         */
        List<EventData> allEvents = eventManager.getAllEvents(defaultFolderId, new Date(), CalendarUtils.add(new Date(), Calendar.DATE, 10), true);
        List<EventData> eventOccurrences = getEventsByUid(allEvents, createdEventSeries.getUid());
        assertEquals(2, eventOccurrences.size());
        EventData firstOccurrence = eventOccurrences.get(0);
        EventData occurrenceUpdate = new EventData();
        occurrenceUpdate.setFolder(firstOccurrence.getFolder());
        occurrenceUpdate.setId(firstOccurrence.getId());
        occurrenceUpdate.setRecurrenceId(firstOccurrence.getRecurrenceId());
        occurrenceUpdate.setAttendees(Arrays.asList(getUserAttendee(testUser2), getUserAttendee(testUser)));
        EventData updatedOccurence = eventManager.updateOccurenceEvent(occurrenceUpdate, occurrenceUpdate.getRecurrenceId(), true);
        /*
         * as user b, try and resolve orphaned occurrence
         */
        ChronosRecurrenceInfoResponse recurrenceInfoResponse = userApi2.getChronosApi().getRecurrenceBuilder()
            .withFolder(defaultFolderId2).withId(updatedOccurence.getId()).withRecurrenceId(updatedOccurence.getRecurrenceId()).execute();
        assertNull(recurrenceInfoResponse.getError());
    }
    
}
