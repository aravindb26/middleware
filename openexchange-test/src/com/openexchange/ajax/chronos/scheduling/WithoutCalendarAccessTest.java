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

package com.openexchange.ajax.chronos.scheduling;

import static com.openexchange.ajax.chronos.factory.AttendeeFactory.createAsExternals;
import static com.openexchange.ajax.chronos.factory.AttendeeFactory.createIndividual;
import static com.openexchange.ajax.chronos.factory.AttendeeFactory.createIndividuals;
import static com.openexchange.ajax.chronos.factory.AttendeeFactory.createOrganizerFrom;
import static com.openexchange.ajax.chronos.factory.EventFactory.deltaOf;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertNoCalendarAccess;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.changedSummary;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.convertToAttendee;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveNotification;
import static com.openexchange.ajax.chronos.util.DateTimeUtil.getDateTime;
import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.java.Lists.combine;
import static com.openexchange.test.common.groupware.calendar.TimeTools.D;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.ICalFactories;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.exception.OXException;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.modules.SessionmanagementApi;

/**
 * {@link WithoutCalendarAccessTest} - Tests about calendar processing with an internal user that hasn't calendar access
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.0.0
 */
public class WithoutCalendarAccessTest extends AbstractKnownProcessingTest {

    private TestUser testUser3;

    protected String folderId2;
    protected UserApi user2;
    protected EventManager eventManager2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        testUser3 = testContext.acquireUser();
    }

    @Test
    public void testUpgradeLifecycleAttendee() throws Exception {
        /*
         * Remove calendar access for second user
         */
        calendarAccess(false);
        /*
         * Create event with the user w/o calendar access
         */
        String summary = "Invite internal W/O access" + UUID.randomUUID().toString();
        EventData eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);
        eventData.setAttendees(createIndividuals(testUser, testUser2, testUser3));
        createdEvent = eventManager.createEvent(eventData);
        /*
         * Receive notification and iMIP
         */
        {
            receiveNotification(testUser3.getApiClient(), testUser.getLogin(), summary);
            MailData iMip = receiveIMip(testUser2.getApiClient(), testUser.getLogin(), summary, 0, createdEvent.getUid(), null, SchedulingMethod.REQUEST);
            assertNoCalendarAccess(analyze(testUser2.getApiClient(), iMip));
        }
        /*
         * Alter event and update, receive update notification and iMIP
         */
        createdEvent = eventManager.updateEvent(deltaOf(createdEvent).location("Olpe"));
        {
            receiveNotification(testUser3.getApiClient(), testUser.getLogin(), changedSummary(summary));
            MailData iMip = receiveIMip(testUser2.getApiClient(), testUser.getLogin(), summary, 1, createdEvent.getUid(), null, SchedulingMethod.REQUEST);
            assertNoCalendarAccess(analyze(testUser2.getApiClient(), iMip));
        }
        /*
         * Enable calendar access and check that user is *NOT* participating
         */
        calendarAccess(true);
        initializeEventManager2();
        {
            List<EventData> events = eventManager2.getAllEvents(yesterday(), tomorrow());
            assertThat("The user shouldn't be part of an existing event after upgrade", events.isEmpty());
        }
        /*
         * Update event and check that the internal user still gets an iMIP mail
         * <p>
         * The user is still saved as external participant, this should NOT be changed in case of an upgrade
         * <p>
         * Since the calendar is enabled now, analysis should return and notify about an event, that can't be found and can be added to the calendar.
         * Subsequent scheduling still triggers *iMIP mails*
         */
        createdEvent = eventManager.updateEvent(deltaOf(createdEvent).location("Dortmund"));
        {
            receiveNotification(testUser3.getApiClient(), testUser.getLogin(), changedSummary(summary));
            MailData iMip = receiveIMip(testUser2.getApiClient(), testUser.getLogin(), summary, 2, createdEvent.getUid(), null, SchedulingMethod.REQUEST);
            assertAnalyzeActions(analyze(testUser2.getApiClient(), iMip), ITipActionSet.ALL);
            EventData eventCopy = assertSingleEvent(accept(testUser2.getApiClient(), constructBody(iMip), null));
            assertThat(eventCopy.getId(), is(not(createdEvent.getId())));
        }
        /*
         * Remove the upgraded user
         */
        createdEvent = eventManager.updateEvent(deltaOf(createdEvent).attendees(createdEvent.getAttendees().stream().dropWhile(a -> null == a.getEntity() || i(a.getEntity()) <= 0).toList()));
        {
            MailData iMip = receiveIMip(testUser2.getApiClient(), testUser.getLogin(), summary, 3, createdEvent.getUid(), null, SchedulingMethod.CANCEL);
            assertAnalyzeActions(analyze(testUser2.getApiClient(), iMip), ITipActionSet.CANCEL);
            applyRemove(testUser2.getApiClient(), constructBody(iMip));
        }
        /*
         * Re-add the upgraded user, as internal attendee this time
         */
        {
            ArrayList<Attendee> attendees = new ArrayList<>(createdEvent.getAttendees());
            attendees.add(convertToAttendee(testUser2));
            createdEvent = eventManager.updateEvent(deltaOf(createdEvent).attendees(attendees));
            receiveNotification(testUser2.getApiClient(), testUser.getLogin(), summary);
            List<EventData> events = eventManager2.getAllEvents(yesterday(), tomorrow());
            assertThat("The user should be part of an existing event re-adding", I(events.size()), is(I(1)));
        }
    }

    @Test
    public void testUpgradeLifecycleOrganizer() throws Exception {
        /*
         * Remove calendar access for second user
         */
        calendarAccess(false);
        /*
         * Prepare initial iTIP REQUEST from Thunderbird
         */
        String organizerEmail = testUser2.getLogin();
        String attendeeEmail = testUser.getLogin();
        String summary = "Organized by interl w/o access" + randomUID();
        String iCal;
        {
            Calendar start = Calendar.getInstance(TimeZone.getTimeZone(ICalFactories.EUROPE_BERLIN));
            start.setTimeInMillis(System.currentTimeMillis());
            Calendar end = Calendar.getInstance(TimeZone.getTimeZone(ICalFactories.EUROPE_BERLIN));
            end.setTimeInMillis(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
            createdEvent = EventFactory.createSingleEvent(getUserId(), summary, getDateTime(start), getDateTime(end));
            createdEvent.setAttendees(createAsExternals(testUser)); // Thunderbird doens't send organizer as attendee
            createdEvent.setOrganizer(createOrganizerFrom(testUser2));
            iCal = ICalFactories.forThunderbird().generateForSingleEvent(SchedulingMethod.REQUEST, createdEvent);
        }
        /*
         * Wrap in iMIP message & send it from user b to user a
         */
        sendImip(testUser2.getApiClient(), generateImip(organizerEmail, attendeeEmail, null, summary, new Date(), SchedulingMethod.REQUEST, iCal));
        /*
         * Receive iMIP as attendee, event should be applied since the user is in the global address book
         */
        {
            MailData iMip = receiveIMip(testUser.getApiClient(), testUser2.getLogin(), summary, 0, createdEvent.getUid(), null, SchedulingMethod.REQUEST);
            assertAnalyzeActions(analyze(testUser.getApiClient(), iMip), ITipActionSet.ACTIONS);
        }
        /*
         * Alter event and update, receive update
         */
        {
            createdEvent.setSequence(I(1));
            iCal = ICalFactories.forThunderbird().generateForSingleEvent(SchedulingMethod.REQUEST, createdEvent);
            iCal = iCal.replace("BEGIN:VEVENT", "BEGIN:VEVENT\nLOCATION:Olpe");
            sendImip(testUser2.getApiClient(), generateImip(organizerEmail, attendeeEmail, null, summary, new Date(), SchedulingMethod.REQUEST, iCal));
        }
        MailData iMip = receiveIMip(testUser.getApiClient(), testUser2.getLogin(), summary, 1, createdEvent.getUid(), null, SchedulingMethod.REQUEST);
        assertAnalyzeActions(analyze(testUser.getApiClient(), iMip), ITipActionSet.ACTIONS);
        /*
         * Enable calendar access and check that there is *NO* event in the calendar
         */
        calendarAccess(true);
        initializeEventManager2();
        {
            List<EventData> events = eventManager2.getAllEvents(yesterday(), tomorrow());
            assertThat("The user shouldn't be part of an existing event after upgrade", events.isEmpty());
        }
        /*
         * Accept as attendee, mail should still be send to organizer as iMIP
         */
        accept(constructBody(iMip), null);
        receiveIMip(testUser2.getApiClient(), testUser.getLogin(), summary, 1, createdEvent.getUid(), null, SchedulingMethod.REPLY);
    }

    @Test
    public void testDowngradeLifecycleAttendee() throws Exception {
        /*
         * Create event with the internal users
         */
        String summary = "Normal appointment" + UUID.randomUUID().toString();
        EventData eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);
        eventData.setAttendees(createIndividuals(testUser, testUser2, testUser3));
        createdEvent = eventManager.createEvent(eventData);
        /*
         * Receive notifications and check within calendar
         */
        {
            receiveNotification(testUser2.getApiClient(), testUser.getLogin(), summary);
            receiveNotification(testUser3.getApiClient(), testUser.getLogin(), summary);
            initializeEventManager2();
            List<EventData> events = eventManager2.getAllEvents(yesterday(), tomorrow());
            assertThat("The user should be part of an event", I(events.size()), is(I(1)));
        }
        /*
         * Remove calendar access for second user and explicitly removed from the event
         * since downgrading works only on context level, sequence increased to 1
         */
        calendarAccess(false);
        eventManager2.tryCreateEvent(EventFactory.createSingleTwoHourEvent(testUser2.getUserId(), "Another appointment"), true);
        createdEvent = eventManager.updateEvent(deltaOf(createdEvent)//
            .attendees(new ArrayList<>(createdEvent.getAttendees()).stream()//
                .filter(a -> null != a.getEntity() && i(a.getEntity()) != testUser2.getUserId()).toList()));
        assertThat(I(createdEvent.getAttendees().size()), is(I(2)));
        /*
         * Alter event and update, receive update as external via iMIP mail
         */
        {
            createdEvent = eventManager.updateEvent(deltaOf(createdEvent).attendees(combine(createdEvent.getAttendees(), createIndividual(testUser2.getLogin()))));
            MailData iMip = receiveIMip(testUser2.getApiClient(), testUser.getLogin(), summary, 2, createdEvent.getUid(), null, SchedulingMethod.REQUEST);
            assertNoCalendarAccess(analyze(testUser2.getApiClient(), iMip));
        }
        /*
         * Enable calendar access and check that user is *NOT* participating
         */
        calendarAccess(true);
        {
            List<EventData> events = eventManager2.getAllEvents(yesterday(), tomorrow());
            assertThat("The user shouldn't be part of an existing event after upgrade", events.isEmpty());
        }
    }

    @Test
    public void testDowngradeLifecycleOrganizer() throws Exception {
        /*
         * Create event with the internal users
         */
        String summary = "Normal appointment" + UUID.randomUUID().toString();
        EventData eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);
        eventData.setAttendees(createIndividuals(testUser, testUser2, testUser3));
        createdEvent = eventManager.createEvent(eventData);
        /*
         * Receive notifications and check within calendar
         */
        {
            receiveNotification(testUser2.getApiClient(), testUser.getLogin(), summary);
            receiveNotification(testUser3.getApiClient(), testUser.getLogin(), summary);
            initializeEventManager2();
            List<EventData> events = eventManager2.getAllEvents(yesterday(), tomorrow());
            assertThat("The user should be part of an event", I(events.size()), is(I(1)));
        }
        /*
         * Remove calendar access for first user
         */
        calendarAccess(false, testUser);
        eventManager.tryCreateEvent(EventFactory.createSingleTwoHourEvent(testUser.getUserId(), "Another appointment"), true);
        /*
         * Create new event as "external organizer"
         */
        String organizerEmail = testUser.getLogin();
        String attendeeEmail = testUser2.getLogin();
        String summary2 = "Organized by internal w/o access" + randomUID();
        String iCal;
        EventData externalEvent;
        {
            Calendar start = Calendar.getInstance(TimeZone.getTimeZone(ICalFactories.EUROPE_BERLIN));
            start.setTimeInMillis(System.currentTimeMillis());
            Calendar end = Calendar.getInstance(TimeZone.getTimeZone(ICalFactories.EUROPE_BERLIN));
            end.setTimeInMillis(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
            externalEvent = EventFactory.createSingleEvent(getUserId(), summary2, getDateTime(start), getDateTime(end));
            externalEvent.setAttendees(createAsExternals(testUser2)); // Thunderbird doens't send organizer as attendee
            externalEvent.setOrganizer(createOrganizerFrom(testUser));
            iCal = ICalFactories.forThunderbird().generateForSingleEvent(SchedulingMethod.REQUEST, externalEvent);
        }
        /*
         * Wrap in iMIP message & send it from user b to user a
         */
        sendImip(testUser.getApiClient(), generateImip(organizerEmail, attendeeEmail, null, summary2, new Date(), SchedulingMethod.REQUEST, iCal));
        /*
         * Receive iMIP as attendee, event should be applied since the user is in the global address book
         */
        {
            MailData iMip = receiveIMip(testUser2.getApiClient(), testUser.getLogin(), summary2, 0, externalEvent.getUid(), null, SchedulingMethod.REQUEST);
            assertAnalyzeActions(analyze(testUser2.getApiClient(), iMip), ITipActionSet.ACTIONS);
        }
        /*
         * Enable calendar access and check that user is *NOT* organizing the second event
         */
        calendarAccess(true, testUser);
        {
            List<EventData> events = eventManager.getAllEvents(yesterday(), tomorrow());
            assertThat("The user shouldn't be part of an existing event after upgrade", I(events.size()), is(I(1)));
            assertThat(events.get(0).getSummary(), is(summary));
        }
        /*
         * Send update as external organizer
         */
        {
            externalEvent.setSequence(I(1));
            iCal = ICalFactories.forThunderbird().generateForSingleEvent(SchedulingMethod.REQUEST, externalEvent);
            iCal = iCal.replace("BEGIN:VEVENT", "BEGIN:VEVENT\nLOCATION:Olpe");
            sendImip(testUser.getApiClient(), generateImip(organizerEmail, attendeeEmail, null, summary2, new Date(), SchedulingMethod.REQUEST, iCal));
        }
        MailData iMip = receiveIMip(testUser2.getApiClient(), testUser.getLogin(), summary2, 1, externalEvent.getUid(), null, SchedulingMethod.REQUEST);
        assertAnalyzeActions(analyze(testUser2.getApiClient(), iMip), ITipActionSet.ACTIONS);
        /*
         * Re-check that no additional event is in the users calendar
         */
        {
            List<EventData> events = eventManager.getAllEvents(yesterday(), tomorrow());
            assertThat("The user shouldn't be part of an existing event after upgrade", I(events.size()), is(I(1)));
            assertThat(events.get(0).getSummary(), is(summary));
        }
    }

    /*
     * ============================== HELPERS ==============================
     */

    private void calendarAccess(boolean enable) throws OXException, ApiException {
        calendarAccess(enable, testUser2);
    }

    private void calendarAccess(boolean enable, TestUser user) throws OXException, ApiException {
        ConfigAwareProvisioningService.getService().changeModuleAccess( //@formatter:off
            user,
            UserModuleAccess.Builder.newInstance().calendar(B(enable)).build(),
            testContext.getUsedBy()); //@formatter:on
        /*
         * The server sometimes has problems with the changed module permissions, probably a caching issue
         * Re-login to clear caches
         */
        new SessionmanagementApi(user.getApiClient()).clear();
        user.performLogout();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        user.performLogin();
        /* Check access afterwards */
        Boolean lastValue = null;
        for (int i = 0; i < 10; i++) {
            try {
                UserModuleAccess moduleAccess = ConfigAwareProvisioningService.getService().getModuleAccess(user);
                lastValue = moduleAccess.getCalendar();
                if (null != lastValue && lastValue.booleanValue() == enable) {
                    return;
                }
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
        Assert.fail("Module access wasn't updated correctly. Desired module access for calendar was " + enable + " but still is " + lastValue);
    }

    private void initializeEventManager2() throws Exception, ApiException {
        user2 = new UserApi(testUser2.getApiClient(), testUser2);
        folderId2 = getDefaultFolder(testUser2.getApiClient());
        eventManager2 = new EventManager(user2, folderId2);
    }

    private Date yesterday() {
        return D("yesterday at 09:00", TimeZone.getTimeZone("UTC"));
    }

    private Date tomorrow() {
        return D("tomorrow at 17:00", TimeZone.getTimeZone("UTC"));
    }

}
