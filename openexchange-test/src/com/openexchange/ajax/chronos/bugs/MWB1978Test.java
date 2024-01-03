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

import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractSecondUserChronosTest;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.UpdateEventBody;

/**
 * Users can change arbitrary appointments by ID confusion
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.org">Tobias Friedrich</a>
 */
public class MWB1978Test extends AbstractSecondUserChronosTest {

    @Test
    public void testUpdateForeignEvent() throws Throwable {
        /*
         * as user a, create appointment in personal calendar
         */
        EventData eventDataA = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), UUIDs.getUnformattedStringFromRandom(), defaultFolderId);
        EventData createdEventA = eventManager.createEvent(eventDataA, true);
        /*
         * as user b, create appointment in personal calendar, too
         */
        EventData eventDataB = EventFactory.createSingleTwoHourEvent(testUser2.getUserId(), UUIDs.getUnformattedStringFromRandom(), defaultFolderId2);
        EventData createdEventB = eventManager2.createEvent(eventDataB, true);
        /*
         * as user b, update 'own' appointment created by user b 
         */
        EventData eventUpdateB = new EventData()
            .summary(createdEventB.getSummary() + " changed")
            .id(createdEventB.getId())
            .folder(createdEventB.getFolder())
        ;
        ChronosCalendarResultResponse updateResponseB = userApi2.getChronosApi().updateEventBuilder()
            .withFolder(defaultFolderId2)
            .withId(createdEventB.getId())
            .withTimestamp(L(2116800000000L))
            .withUpdateEventBody(new UpdateEventBody().event(eventUpdateB))
        .execute();
        eventManager2.handleUpdate(updateResponseB, false);
        /*
         * check appointment was updated as expected
         */
        EventData updatedEventB = eventManager2.getEvent(defaultFolderId2, createdEventB.getId());
        assertEquals(eventUpdateB.getSummary(), updatedEventB.getSummary());
        /*
         * as user b, try and update 'foreign' appointment created by user a by exchanging id in body
         */
        EventData eventUpdateA = new EventData()
            .summary(createdEventA.getSummary() + " changed")
            .id(createdEventA.getId())
            .folder(createdEventA.getFolder())
        ;
        userApi2.getChronosApi().updateEventBuilder()
            .withFolder(defaultFolderId2)
            .withId(createdEventB.getId())
            .withTimestamp(L(2116800000000L))
            .withUpdateEventBody(new UpdateEventBody().event(eventUpdateA))
        .execute();
        /*
         * check user A's appointment was not updated
         */
        EventData updatedEventA = eventManager.getEvent(defaultFolderId, createdEventA.getId());
        assertEquals(eventDataA.getSummary(), updatedEventA.getSummary());
    }
    
}
