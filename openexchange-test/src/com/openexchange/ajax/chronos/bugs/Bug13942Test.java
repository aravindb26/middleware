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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractSecondUserChronosTest;
import com.openexchange.ajax.chronos.factory.AlarmFactory;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.EventData;

/**
 *
 * {@link Bug13942Test}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.3
 */
public class Bug13942Test extends AbstractSecondUserChronosTest {

    @Test
    public void testBug13942() throws Exception {
        // Create event and invite other user
        EventData event = EventFactory.createSingleTwoHourEvent(getCalendaruser(), "testBug13942", folderId);
        Attendee attendee = AttendeeFactory.createAttendee(userApi2.getCalUser(), CuTypeEnum.INDIVIDUAL);
        event.getAttendees().add(attendee);
        EventData createEvent = eventManager.createEvent(event, true);

        // As user B first accept event and then add an alarm
        eventManager2.getEvent(defaultFolderId2, createEvent.getId());
        AttendeeAndAlarm attendeeAndAlarm = new AttendeeAndAlarm();
        attendee.setPartStat("ACCEPTED");
        attendeeAndAlarm.setAttendee(attendee);
        eventManager2.updateAttendee(createEvent.getId(), attendeeAndAlarm, false);

        attendeeAndAlarm.setAlarms(Collections.singletonList(AlarmFactory.createDisplayAlarm("-PT10M")));
        eventManager2.updateAttendee(createEvent.getId(), attendeeAndAlarm, false);


        EventData getEvent = eventManager.getEvent(folderId, createEvent.getId());

        for (Attendee att : getEvent.getAttendees()) {
            assertEquals("ACCEPTED", att.getPartStat());
        }

    }

}
