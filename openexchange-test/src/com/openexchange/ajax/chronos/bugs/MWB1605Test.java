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

import static com.openexchange.ajax.chronos.factory.AttendeeFactory.createIndividuals;
import static com.openexchange.ajax.chronos.util.CalendarAssertUtil.isCalendarFolder;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.testing.httpclient.models.EventData.FlagsEnum.ORGANIZER_ON_BEHALF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.freebusy.FreeBusyUtils;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponseData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FreeBusyTime;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MWB1605Test} - Test if a viewable event is correctly displayed in Free/Busy view
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.3
 */
public class MWB1605Test extends AbstractChronosTest {

    private String privateEventId;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        /*
         * Create event for the second user, that shows up without details later
         * to ensure no additional data is leaked
         */
        UserApi userApi = new UserApi(testUser2.getApiClient(), testUser2);
        EventManager manager = new EventManager(userApi, getDefaultFolder(testUser2.getApiClient()));
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        start.setTimeInMillis(end.getTimeInMillis() - TimeUnit.HOURS.toMillis(4));
        end.setTimeInMillis(end.getTimeInMillis() - TimeUnit.HOURS.toMillis(2));
        privateEventId = manager.createEvent(//
            EventFactory.createSingleEvent(testUser2.getUserId(), "Event with only " + testUser2.getLogin(), null,//
                DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end), null))//
            .getId();
        assertThat(privateEventId, is(not(emptyOrNullString())));
    }

    @Test
    public void testPrivateCalendar() throws Exception {
        /*
         * Create event in folder
         */
        String summary = "EventInPrivateFolder";
        EventData createdEvent = createEvent(defaultFolderId, summary, testUser, testUser2);
        assertThat(createdEvent.getAttendees().stream()//
            .filter(a -> a.getEntity().equals(I(testUser.getUserId()))).findAny().orElseThrow(() -> new AssertionError("Unable to find acting attendee"))//
            .getFolder(), isCalendarFolder(defaultFolderId));
        checkFreeBusyData(summary);
    }

    @Test
    public void testSharedCalendar() throws Exception {
        /*
         * Create folder
         */
        String calendarFolderId = folderManager.createCalendarFolder("sharedFolder", testUser.getUserId());
        FolderData folder = folderManager.getFolder(calendarFolderId);
        folderManager.shareFolder(folder, testUser2);
        /*
         * Create event in folder, response will contain created event twice, once in private folder of attendee and once in shared folder
         */
        String summary = "EventInSharedFolder";
        EventData createdEvent = createEvent(calendarFolderId, "EventInSharedFolder", testUser2, testUser, testUser2);
        if (createdEvent.getFlags().contains(ORGANIZER_ON_BEHALF)) {
            assertThat(createdEvent.getFolder(), is(calendarFolderId));
        } else {
            assertThat(createdEvent.getFolder(), is(getDefaultFolder(testUser2.getApiClient())));
        }
        checkFreeBusyData(summary);
    }

    @Test
    public void testPublicCalendar() throws Exception {
        /*
         * Create folder
         */
        String calendarFolderId = folderManager.createPublicCalendarFolder("publicCalendarFolder");
        FolderData folder = folderManager.getFolder(calendarFolderId);
        folderManager.shareFolder(folder, testUser2);
        /*
         * Create event in folder as another user which is still viewable by the owner of the public calendar
         */
        String summary = "EventInPublicFolder";
        EventData createdEvent = createEvent(calendarFolderId, summary, testUser2);
        assertThat(createdEvent.getFolder(), is(calendarFolderId));
        checkFreeBusyData(summary);
    }

    /**
     * Creates an event in the given folder
     *
     * @param calendarFolderId The folder to create the event in
     * @param summary The summary of the event
     * @param acting The acting user, that creates the event
     * @param attendee The optional attendee of the event
     * @throws Exception In case of error
     */
    private static EventData createEvent(String calendarFolderId, String summary, TestUser acting, TestUser... attendees) throws Exception {
        UserApi userApi = new UserApi(acting.getApiClient(), acting);
        EventManager manager = new EventManager(userApi, calendarFolderId);
        EventData eventData = EventFactory.createSingleTwoHourEvent(acting.getUserId(), summary);
        eventData.setFolder(calendarFolderId);
        eventData.setAttendees(createIndividuals(attendees));
        EventData createdEvent = manager.createEvent(eventData);
        assertThat(createdEvent.getSummary(), is(summary));
        return createdEvent;
    }

    /**
     * Checks if free/busy data is available with details
     *
     * @param summary The summary the conflicting event should have
     * @throws Exception In case of error
     */
    private void checkFreeBusyData(String summary) throws Exception {
        List<ChronosFreeBusyResponseData> data = FreeBusyUtils.getData(chronosApi, testUser.getContextId(), testUser2);
        assertTrue(data.size() == 1);
        ChronosFreeBusyResponseData freeBusyData = data.get(0);
        assertTrue(freeBusyData.getFreeBusyTime().size() == 2);
        for (FreeBusyTime busyTime : freeBusyData.getFreeBusyTime()) {
            String freeBusySummary = busyTime.getEvent().getSummary();
            if (privateEventId.equals(busyTime.getEvent().getId())) {
                /*
                 * Private non-group-scheduled event by second user, not visible for the acting user
                 */
                assertThat(busyTime.getEvent().getSummary(), is(emptyOrNullString()));
            } else {
                /*
                 * Check that the summary is set, acting user can view the event
                 */
                assertThat(freeBusySummary, is(summary));
            }
        }
    }

}
