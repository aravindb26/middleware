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

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.EventData;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MWB104Test}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.4
 */
public class MWB104Test extends AbstractChronosTest {

    private UserApi userApi2;
    private String defaultFolderId2;
    private EventManager eventManager2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        SessionAwareClient apiClient2 = testUser2.getApiClient();
        userApi2 = new UserApi(apiClient2, testUser2);
        defaultFolderId2 = getDefaultFolder(userApi2.getFoldersApi());
        eventManager2 = new EventManager(userApi2, defaultFolderId2);
    }

    @Test
    public void testResetPartstat() throws Exception {
        EventData eventData = EventFactory.createSingleTwoHourEvent(getCalendaruser(), "MWB-104 Test", folderId);
        Integer secondUserId = I(testUser2.getUserId());
        Attendee attendee = AttendeeFactory.createIndividual(secondUserId);
        eventData.addAttendeesItem(attendee);
        EventData createdEvent = eventManager.createEvent(eventData, true);

        AttendeeAndAlarm attendeeAndAlarm = new AttendeeAndAlarm();
        attendee.setPartStat("ACCEPTED");
        attendee.setComment("Comment");
        attendeeAndAlarm.setAttendee(attendee);
        eventManager2.setLastTimeStamp(eventManager.getLastTimeStamp());
        eventManager2.updateAttendee(createdEvent.getId(), attendeeAndAlarm, false);

        for (Attendee a : eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId()).getAttendees()) {
            if (a.getEntity() == secondUserId) {
                assertEquals("ACCEPTED", a.getPartStat(), "Expected correct partstat");
                assertEquals("Comment", a.getComment(), "Expected correct comment.");
            }
        }

        createdEvent.setStartDate(DateTimeUtil.incrementDateTimeData(createdEvent.getStartDate(), 3600000));
        createdEvent.setEndDate(DateTimeUtil.incrementDateTimeData(createdEvent.getEndDate(), 3600000));
        createdEvent.setAttendees(null);

        eventManager.setLastTimeStamp(eventManager2.getLastTimeStamp());
        eventManager.updateEvent(createdEvent, false, false);

        for (Attendee a : eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId()).getAttendees()) {
            if (a.getEntity() == secondUserId) {
                assertEquals("NEEDS-ACTION", a.getPartStat(), "Expected correct partstat");
                assertEquals("Comment", a.getComment(), "Expected correct comment.");
            }
        }
    }

}
