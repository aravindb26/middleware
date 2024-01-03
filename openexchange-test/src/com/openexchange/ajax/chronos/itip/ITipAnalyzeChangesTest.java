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

package com.openexchange.ajax.chronos.itip;

import static com.openexchange.ajax.chronos.factory.EventFactory.prepareDeltaEvent;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertAttendeePartStat;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.checkNoReplyMailReceived;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.prepareJsonForFileUpload;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.openexchange.ajax.chronos.factory.ConferenceBuilder;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.EventFactory.RecurringFrequency;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.factory.RRuleFactory;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.java.Strings;
import com.openexchange.test.common.asset.Asset;
import com.openexchange.test.common.asset.AssetType;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.AnalysisChange;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.ChronosAttachment;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.Conference;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventData.TranspEnum;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailListElement;
import com.openexchange.testing.httpclient.models.MailsCleanUpResponse;
import com.openexchange.testing.httpclient.modules.MailApi;

/**
 * {@link ITipAnalyzeChangesTest} - Updates different parts of an event and checks changes on attendee side.
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
@Execution(ExecutionMode.SAME_THREAD)
public class ITipAnalyzeChangesTest extends AbstractITipAnalyzeTest {

    private String summary;

    private EventData attendeeEvent = null;

    private Attendee replyingAttendee;

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).build();
    }

    /**
     * Creates an event as user A in context 1 with external attendee user B from context 2.
     * User B accepts the event and user A takes over the changes.
     */
    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);
        summary = this.getClass().getName() + " " + UUID.randomUUID().toString();
        /*
         * Create event
         */
        EventData eventToCreate = EventFactory.createSingleTwoHourEvent(0, summary);
        replyingAttendee = prepareCommonAttendees(eventToCreate);
        eventToCreate = prepareAttendeeConference(eventToCreate);
        eventToCreate = prepareModeratorConference(eventToCreate);
        eventToCreate.setLocation("SomeLocation");
        createdEvent = eventManager.createEvent(eventToCreate, true);

        /*
         * Receive mail as attendee
         */
        MailData iMip = receiveIMip(apiClientC2, userResponseC1.getData().getEmail1(), summary, 0, SchedulingMethod.REQUEST);
        MailListElement element = new MailListElement();
        element.setFolder(iMip.getFolderId());
        element.setId(iMip.getId());
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClientC2, iMip)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(createdEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION);

        /*
         * reply with "accepted"
         */
        attendeeEvent = assertSingleEvent(accept(apiClientC2, constructBody(iMip), null), createdEvent.getUid());
        assertAttendeePartStat(attendeeEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);
        // remove old mail, because automated deletion may be too slow and cause follow up issues
        MailsCleanUpResponse delResp = new MailApi(apiClientC2).deleteMails(Collections.singletonList(element), null, Boolean.TRUE, null);
        assertNull(delResp.getError(), delResp.getErrorDesc());

        /*
         * Receive mail as organizer and check actions
         */
        MailData reply = receiveIMip(testUser.getApiClient(), replyingAttendee.getEmail(), summary, 0, SchedulingMethod.REPLY);
        analyze(reply.getId());

        /*
         * Take over accept and check in calendar
         */
        assertSingleEvent(applyResponse(testUser.getApiClient(), constructBody(reply)), createdEvent.getUid());
        EventResponse eventResponse = chronosApi.getEvent(createdEvent.getId(), createdEvent.getFolder(), createdEvent.getRecurrenceId(), null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        createdEvent = eventResponse.getData();
        for (Attendee attendee : createdEvent.getAttendees()) {
            PartStat.ACCEPTED.assertStatus(attendee);
        }
        if (null != eventResponse.getTimestamp()) {
            eventManager.setLastTimeStamp(eventResponse.getTimestamp());
        }
    }

    @Test
    public void testSummaryChange() throws Exception {
        /*
         * Change summary as organizer
         */
        String changedSumamry = "New summary" + UUID.randomUUID();
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setSummary(changedSumamry);
        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that summary has been updated
         */
        MailData iMip = receiveMailAsAttendee(changedSumamry);
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        reCheckAnalyze(iMip);
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(attendeeEvent.getSummary().equals(changedSumamry), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, changedSumamry);
    }

    @Test
    public void testStartAndEndDateChange() throws Exception {
        /*
         * Shift start and end date by two hours as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        date.setTimeInMillis(date.getTimeInMillis() + TimeUnit.HOURS.toMillis(2));
        deltaEvent.setStartDate(DateTimeUtil.getDateTime(date));
        date.setTimeInMillis(date.getTimeInMillis() + TimeUnit.HOURS.toMillis(2));
        deltaEvent.setEndDate(DateTimeUtil.getDateTime(date));

        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that dates has been updated
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.NEEDS_ACTION, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event
         */
        accept(apiClientC2, constructBody(iMip), null);
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(deltaEvent.getEndDate().equals(attendeeEvent.getEndDate()), "Not changed!");
        reCheckAnalyze(iMip);

        /*
         * Receive mail as organizer and check actions
         */
        MailData reply = receiveIMip(testUser.getApiClient(), replyingAttendee.getEmail(), summary, 1, SchedulingMethod.REPLY);
        analyze(reply.getId());
    }

    @Test
    public void testStartDateChange() throws Exception {
        /*
         * Shift start by two hours as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        date.setTimeInMillis(date.getTimeInMillis() + TimeUnit.HOURS.toMillis(1));
        deltaEvent.setStartDate(DateTimeUtil.getDateTime(date));

        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that start date has been updated
         * Note: Due internal handling of a shortened Event, no rescheduling will happen. Thus the participant status is
         * unchanged. For details see com.openexchange.chronos.impl.Utils.coversDifferentTimePeriod(Event, Event) or
         * http://documentation.open-xchange.com/latest/middleware/calendar/implementation_details.html#reset-of-participation-status
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(deltaEvent.getStartDate().equals(attendeeEvent.getStartDate()), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    @Test
    public void testEndDateChange() throws Exception {
        /*
         * Shift end date by two hours as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        date.setTimeInMillis(date.getTimeInMillis() + TimeUnit.HOURS.toMillis(4));
        deltaEvent.setEndDate(DateTimeUtil.getDateTime(date));

        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that end date has been updated
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.NEEDS_ACTION, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event
         */
        accept(apiClientC2, constructBody(iMip), null);
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(deltaEvent.getEndDate().equals(attendeeEvent.getEndDate()), "Not changed!");
        reCheckAnalyze(iMip);
        /*
         * Receive mail as organizer and check actions
         */
        MailData reply = receiveIMip(testUser.getApiClient(), replyingAttendee.getEmail(), summary, 1, SchedulingMethod.REPLY);
        analyze(reply.getId());
    }

    @Test
    public void testStartDateTimeZoneChange() throws Exception {
        /*
         * Shift start and end date by two hours as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        Date date = DateTimeUtil.parseDateTime(createdEvent.getStartDate());
        String timeZone = "Europe/Isle_of_Man";
        deltaEvent.setStartDate(DateTimeUtil.getDateTime(timeZone, date.getTime()));

        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that time zone of start date has been updated
         * Note: Due internal handling of a shortened Event, no rescheduling will happen. Thus the participant status is
         * unchanged. For details see com.openexchange.chronos.impl.Utils.coversDifferentTimePeriod(Event, Event) or
         * http://documentation.open-xchange.com/latest/middleware/calendar/implementation_details.html#reset-of-participation-status
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        accept(apiClientC2, constructBody(iMip), null);
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        reCheckAnalyze(iMip);
        /*
         * Converted to"Europe/London" before sending
         * See also com.openexchange.chronos.ical.ical4j.mapping.AbstractICalMapping.toICalDate()
         */
        assertTrue("Europe/London".equals(attendeeEvent.getStartDate().getTzid()), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
    }

    @Test
    public void testLocationChange() throws Exception {
        /*
         * Change location as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        String location = "Olpe";
        deltaEvent.setLocation(location);

        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that location has been updated
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(location.equals(attendeeEvent.getLocation()), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    @Test
    public void testRemoveLocation() throws Exception {
        /*
         * Remove location as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setLocation("");

        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that location has been updated
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(Strings.isEmpty(attendeeEvent.getLocation()), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    @Test
    public void testTransparencyChange() throws Exception {
        /*
         * Shift start and end date by two hours as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setTransp(TranspEnum.TRANSPARENT.equals(createdEvent.getTransp()) ? TranspEnum.OPAQUE : TranspEnum.TRANSPARENT);

        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that the event is marked as "free"
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);
        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(deltaEvent.getTransp().equals(attendeeEvent.getTransp()), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    @Test
    public void testAddAndRemoveAttachment() throws Exception {
        /*
         * Prepare attachment and update it
         */
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);
        File file = new File(asset.getAbsolutePath());
        String callbackHtml = chronosApi.updateEventWithAttachments( //@formatter:off
            createdEvent.getFolder(),
            createdEvent.getId(),
            Long.valueOf(eventManager.getLastTimeStamp()),
            prepareJsonForFileUpload(createdEvent.getId(),
            null == createdEvent.getFolder() ? defaultFolderId : createdEvent.getFolder(),
            asset.getFilename()),
            file,
            null,
            null,
            null,
            null,
            null); //@formatter:on
        assertNotNull(callbackHtml);
        assertTrue(callbackHtml.contains("\"filename\":\"" + asset.getFilename() + "\""), "Should contain attachment name: " + asset.getFilename());
        createdEvent = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId());

        /*
         * Check constrains
         */
        int sequenceId = 0;
        MailData iMip = receiveMailAsAttendee("Appointment changed: " + summary, sequenceId);
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);

        /*
         * Accept changes and check if attachment has been added to the event
         */
        MailData mail = receiveIMip(apiClientC2, userResponseC1.getData().getEmail1(), summary, sequenceId, SchedulingMethod.REQUEST);
        MailListElement element = new MailListElement();
        element.setFolder(mail.getFolderId());
        element.setId(mail.getId());
        CalendarResult response = applyChange(apiClientC2, constructBody(mail));
        EventData current = assertSingleEvent(response);
        EventData eventData = eventManagerC2.getEvent(current.getFolder(), current.getId());

        assertEquals(createdEvent.getUid(), eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);
        /*
         * check if attachment was imported correctly
         */
        List<ChronosAttachment> attachments = eventData.getAttachments();
        assertTrue(null != attachments && 1 == attachments.size());
        ChronosAttachment attachment = attachments.get(0);
        assertEquals(asset.getFilename(), attachment.getFilename());
        assertEquals("image/jpeg", attachment.getFmtType());
        byte[] attachmentData = eventManagerC2.getAttachment(eventData.getId(), i(attachment.getManagedId()), eventData.getFolder());
        assertNotNull(attachmentData);

        /*
         * Remove attachment as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setAttachments(Collections.emptyList());
        // remove old mail
        MailsCleanUpResponse delResp = new MailApi(apiClientC2).deleteMails(Collections.singletonList(element), null, Boolean.TRUE, null);
        assertNull(delResp.getError(), delResp.getErrorDesc());
        updateEventAsOrganizer(deltaEvent);

        /*
         * Lookup that event has been removed
         */
        EventData updated = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertTrue(updated.getAttachments() == null || updated.getAttachments().isEmpty(), "Should not contain attachments");

        /*
         * Receive update as attendee and accept changes
         */
        iMip = receiveIMip(apiClientC2, userResponseC1.getData().getEmail1(), summary, 0, SchedulingMethod.REQUEST);
        analyzeResponse = analyze(apiClientC2, iMip);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);
        updated = assertSingleEvent(applyChange(apiClientC2, constructBody(iMip)));
        updated = eventManagerC2.getEvent(updated.getFolder(), updated.getId());
        assertTrue(updated.getAttachments() == null || updated.getAttachments().isEmpty(), "Should not contain attachments");
    }

    @Test
    public void testDescriptionChange() throws Exception {
        /*
         * Change description as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setDescription("New description");
        updateEventAsOrganizer(deltaEvent);

        /*
         * Check that end date has been updated
         */
        MailData iMip = receiveMailAsAttendee(0);
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(deltaEvent.getDescription().equals(attendeeEvent.getDescription()), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    @Test
    public void testRecuccrenceRuleChange() throws Exception {
        /*
         * Convert event to series as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        int occurences = 10;
        String rrule = RRuleFactory.getFrequencyWithOccurenceLimit(RecurringFrequency.DAILY, occurences);
        deltaEvent.setRrule(rrule);

        /*
         * Update as organizer
         */
        long now = System.currentTimeMillis();
        String fromStr = DateTimeUtil.getZuluDateTime(new Date(now - TimeUnit.DAYS.toMillis(1)).getTime()).getValue();
        String untilStr = DateTimeUtil.getZuluDateTime(new Date(now + TimeUnit.DAYS.toMillis(30)).getTime()).getValue();

        ChronosCalendarResultResponse calendarResultResponse = chronosApi.updateEvent(deltaEvent.getFolder(), deltaEvent.getId(), Long.valueOf(eventManager.getLastTimeStamp()), getUpdateBody(deltaEvent), deltaEvent.getRecurrenceId(), null, null, null, null, null, null, fromStr, untilStr, Boolean.TRUE, null);
        assertNull(calendarResultResponse.getError());
        assertTrue(calendarResultResponse.getData().getUpdated().size() == 0);
        assertTrue(calendarResultResponse.getData().getCreated().size() == occurences);
        assertTrue(calendarResultResponse.getData().getDeleted().size() == 1);

        /*
         * Check that end date has been updated
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.NEEDS_ACTION, ITipActionSet.ALL);
        AnalysisChange change = assertSingleChange(analyzeResponse);
        assertThat("Recurrence ID is not correct.", change.getNewEvent().getRrule(), is(rrule));

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        accept(apiClientC2, constructBody(iMip), null);
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertThat("Not changed!", attendeeEvent.getRrule(), is(rrule));
        reCheckAnalyze(iMip);
        /*
         * Receive mail as organizer and check actions
         */
        MailData reply = receiveIMip(testUser.getApiClient(), replyingAttendee.getEmail(), summary, 1, SchedulingMethod.REPLY);
        analyze(reply.getId());
    }

    @Test
    public void testAddAttendee() throws Exception {
        /*
         * Add an third attendee
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        TestUser testUser3 = context2.acquireUser();

        Attendee addedAttendee = ITipUtil.convertToAttendee(testUser3, Integer.valueOf(0));
        addedAttendee.setPartStat(PartStat.NEEDS_ACTION.getStatus());
        deltaEvent.getAttendees().add(addedAttendee);
        updateEventAsOrganizer(deltaEvent);
        assertTrue(deltaEvent.getAttendees().size() == createdEvent.getAttendees().size(), "Attendee was not added");

        /*
         * Check that the event has a new attendee
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(deltaEvent.getAttendees().size() == attendeeEvent.getAttendees().size(), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);

        /*
         * Check invite mail for new attendee
         */
		SessionAwareClient apiClient3 = testUser3.getApiClient();
        iMip = receiveIMip(apiClient3, userResponseC1.getData().getEmail1(), summary, 1, SchedulingMethod.REQUEST);
        analyzeResponse = analyze(apiClient3, iMip);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyzeResponse).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(attendeeEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), addedAttendee.getEmail(), PartStat.NEEDS_ACTION);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.ALL);
    }

    @Test
    public void testRemoveAttendee() throws Exception {
        /*
         * Remove attendee
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setAttendees(Collections.singletonList(createdEvent.getAttendees().stream().filter(a -> null != a.getEntity() && userResponseC1.getData().getId().equals(a.getEntity().toString())).findFirst().orElseThrow(() -> new Exception("Unable to find organizer"))));

        updateEventAsOrganizer(deltaEvent);

        MailData iMip = receiveIMip(apiClientC2, userResponseC1.getData().getEmail1(), "Appointment canceled: " + summary, 1, SchedulingMethod.CANCEL);
        AnalyzeResponse analyzeResponse = analyze(apiClientC2, iMip);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.CANCEL);
        assertSingleChange(analyzeResponse);

        /*
         * Delete attendee's event and check that no mail as been scheduled
         */
        cancel(testUserC2.getApiClient(), constructBody(iMip), null, false);
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip, ITipActionSet.EMPTY);
    }

    @Test
    public void testAddConference() throws Exception {
        /*
         * Add additional conference as organizer
         */
        ConferenceBuilder builder = ConferenceBuilder.newBuilder() //@formatter:off
            .setDefaultFeatures()
            .setLable("Random lable")
            .setVideoChatUri(); //@formatter:on
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setConferences(createdEvent.getConferences());
        deltaEvent.addConferencesItem(builder.build());
        updateEventAsOrganizer(deltaEvent);

        EventData updatedEvent = eventManager.getEvent(defaultFolderId, createdEvent.getId());
        assertThat("Should be three conferences!", I(updatedEvent.getConferences().size()), is(I(3)));

        /*
         * Check that the conference item has been added
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(deltaEvent.getConferences().size() == attendeeEvent.getConferences().size(), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    @Test
    public void testUpdateConference() throws Exception {
        /*
         * Change conference item as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        ArrayList<Conference> conferences = new ArrayList<Conference>(2);
        conferences.add(createdEvent.getConferences().get(1));
        Conference update = ConferenceBuilder.copy(createdEvent.getConferences().get(0));
        String label = "New lable";
        update.setLabel(label);
        conferences.add(update);
        deltaEvent.setConferences(conferences);
        updateEventAsOrganizer(deltaEvent);

        EventData updatedEvent = eventManager.getEvent(defaultFolderId, createdEvent.getId());
        assertThat("Should be two conferences!", I(updatedEvent.getConferences().size()), is(I(2)));

        /*
         * Check that conference has been updated
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        boolean changed = false;
        for (Conference conference : attendeeEvent.getConferences()) {
            if (label.equals(conference.getLabel())) {
                changed = true;
            }
        }
        assertTrue(changed, "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    @Test
    public void testUpdateExtendedPropertiesOfConference() throws Exception {
        /*
         * Change conference item as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        ArrayList<Conference> conferences = new ArrayList<Conference>(2);
        for (Conference conference : createdEvent.getConferences()) {
            Conference update = ConferenceBuilder.copy(conference);
            update.setExtendedParameters(Collections.emptyMap());
            conferences.add(update);
        }
        deltaEvent.setConferences(conferences);
        updateEventAsOrganizer(deltaEvent);

        EventData updatedEvent = eventManager.getEvent(defaultFolderId, createdEvent.getId());
        assertThat("Should be two conferences!", I(updatedEvent.getConferences().size()), is(I(2)));

        /*
         * Check that mails has been send (updates still needs to be propagated) without any description
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);
    }

    @Test
    public void testRemoveConference() throws Exception {
        /*
         * Remove conference item as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setConferences(Collections.emptyList());
        updateEventAsOrganizer(deltaEvent);

        EventData updatedEvent = eventManager.getEvent(defaultFolderId, createdEvent.getId());
        assertTrue(updatedEvent.getConferences() == null || updatedEvent.getConferences().isEmpty(), "Should be no conferences!");

        /*
         * Check that mails has been send
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(attendeeEvent.getConferences() == null || attendeeEvent.getConferences().isEmpty(), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    @Test
    public void testRemoveOnlyOneConference() throws Exception {
        /*
         * Remove conference item as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setConferences(Collections.singletonList(createdEvent.getConferences().get(0)));
        updateEventAsOrganizer(deltaEvent);

        EventData updatedEvent = eventManager.getEvent(defaultFolderId, createdEvent.getId());
        assertThat("Should be one conference!", I(updatedEvent.getConferences().size()), is(I(1)));

        /*
         * Check that mails has been send, but for changed conferences not removed all
         */
        MailData iMip = receiveMailAsAttendee();
        AnalyzeResponse analyzeResponse = analyzeUpdateAsAttendee(iMip, PartStat.ACCEPTED, ITipActionSet.ALL);
        assertSingleChange(analyzeResponse);

        /*
         * Update attendee's event and check that no mail as been scheduled
         */
        applyChange(apiClientC2, constructBody(iMip));
        attendeeEvent = eventManagerC2.getEvent(folderIdC2, attendeeEvent.getId());
        assertTrue(deltaEvent.getConferences().size() == attendeeEvent.getConferences().size(), "Not changed!");
        checkNoReplyMailReceived(testUser.getApiClient(), replyingAttendee, summary);
        reCheckAnalyze(iMip);
    }

    /*
     * ----------------------------- HELPERS ------------------------------
     */

    private void updateEventAsOrganizer(EventData deltaEvent) throws ApiException {
        long now = System.currentTimeMillis();
        String fromStr = DateTimeUtil.getZuluDateTime(new Date(now - TimeUnit.DAYS.toMillis(1)).getTime()).getValue();
        String untilStr = DateTimeUtil.getZuluDateTime(new Date(now + TimeUnit.DAYS.toMillis(30)).getTime()).getValue();
        // @formatter:off
        ChronosCalendarResultResponse calendarResultResponse = chronosApi.updateEventBuilder()
                                                                         .withFolder(deltaEvent.getFolder())
                                                                         .withId(deltaEvent.getId())
                                                                         .withTimestamp(Long.valueOf(eventManager.getLastTimeStamp()))
                                                                         .withUpdateEventBody(getUpdateBody(deltaEvent))
                                                                         .withRecurrenceId(deltaEvent.getRecurrenceId())
                                                                         .withRangeEnd(fromStr)
                                                                         .withRangeEnd(untilStr)
                                                                         .withExpand(Boolean.TRUE)
                                                                         .execute();
        // @formatter:on

        CalendarResult result = checkResponse(calendarResultResponse.getError(), calendarResultResponse.getErrorDesc(), calendarResultResponse.getData());
        assertTrue(result.getUpdated().size() == 1);
        createdEvent = result.getUpdated().get(0);
    }

    private AnalyzeResponse analyzeUpdateAsAttendee(MailData iMip, PartStat partStat, ITipActionSet consumer) throws Exception {
        AnalyzeResponse analyzeResponse = analyze(apiClientC2, iMip);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyzeResponse).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(attendeeEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), partStat);
        assertAnalyzeActions(analyzeResponse, consumer);
        return analyzeResponse;
    }

    private MailData receiveMailAsAttendee() throws Exception {
        return receiveMailAsAttendee("Appointment changed: " + summary);
    }

    private MailData receiveMailAsAttendee(int sequence) throws Exception {
        return receiveMailAsAttendee("Appointment changed: " + summary, sequence);
    }

    private MailData receiveMailAsAttendee(String summary) throws Exception {
        return receiveMailAsAttendee(summary, 1);
    }

    private MailData receiveMailAsAttendee(String summary, int sequnce) throws Exception {
        return receiveIMip(apiClientC2, userResponseC1.getData().getEmail1(), summary, sequnce, SchedulingMethod.REQUEST);
    }

    private void reCheckAnalyze(MailData iMip) throws Exception {
        reCheckAnalyze(iMip, ITipActionSet.ACTIONS);
    }

    private void reCheckAnalyze(MailData iMip, ITipActionSet consumer) throws Exception {
        AnalyzeResponse analyzeResponse = analyze(apiClientC2, iMip);
        assertNull(analyzeResponse.getCode(), "error during analysis: " + analyzeResponse.getError());
        assertAnalyzeActions(analyzeResponse, consumer);
    }

}
