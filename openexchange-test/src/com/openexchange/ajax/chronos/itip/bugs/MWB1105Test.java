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

package com.openexchange.ajax.chronos.itip.bugs;

import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertAttendeePartStat;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.parseICalAttachment;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.dmfs.rfc5545.DateTime;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.itip.AbstractITipTest;
import com.openexchange.ajax.chronos.itip.IMipReceiver;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.testing.httpclient.models.ActionResponse;
import com.openexchange.testing.httpclient.models.AnalysisChangeDeletedEvent;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.ChronosMultipleCalendarResultResponse;
import com.openexchange.testing.httpclient.models.DeleteEventBody;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.UserData;

/**
 * {@link MWB1105Test}
 *
 * Deleting external organized event creates delete exception
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class MWB1105Test extends AbstractITipTest {

    @Test
    public void testDeleteOccurrenceAsAttendee() throws Exception {
        /*
         * as user b from context 2, create event series and invite user a from context 1
         */
        UserData userB = userResponseC2.getData();
        UserData userA = userResponseC1.getData();
        String uid = UUID.randomUUID().toString();
        String summary = UUID.randomUUID().toString();
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        Date start = com.openexchange.time.TimeTools.D("next monday afternoon", timeZone);
        Date end = CalendarUtils.add(start, Calendar.HOUR, 1);
        EventData eventDataC2 = new EventData();
        eventDataC2.setUid(uid);
        eventDataC2.setSummary(summary);
        eventDataC2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
        eventDataC2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
        eventDataC2.setRrule("FREQ=DAILY;COUNT=6");
        eventDataC2.setAttendees(Arrays.asList(AttendeeFactory.createIndividual(userB.getUserId()), AttendeeFactory.createIndividual(userA.getEmail1())));
        eventDataC2.setOrganizer(AttendeeFactory.createOrganizerFrom(AttendeeFactory.createIndividual(userB.getUserId())));
        eventDataC2 = eventManagerC2.createEvent(eventDataC2);
        /*
         * receive & analyze iMIP request as user a
         */
        MailData iMipRequestData = receiveIMip(apiClient, userB.getEmail1(), summary, 0, uid, null, SchedulingMethod.REQUEST);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(uid, newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), userA.getEmail1(), "NEEDS-ACTION");
        /*
         * reply with "accepted" for the event series
         */
        EventData eventData = assertSingleEvent(accept(constructBody(iMipRequestData), null));
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * check event in calendar
         */
        EventResponse eventResponse = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), eventData.getRecurrenceId(), null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        eventData = eventResponse.getData();
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * receive & analyze iMIP reply as user b
         */
        MailData iMipReplyData = receiveIMip(apiClientC2, userA.getEmail1(), summary, 0, uid, null, SchedulingMethod.REPLY);
        assertNotNull(iMipReplyData);
        ImportedCalendar iTipReply = parseICalAttachment(apiClientC2, iMipReplyData);
        assertEquals("REPLY", iTipReply.getMethod());
        assertTrue(null != iTipReply.getEvents() && 1 == iTipReply.getEvents().size());
        Event replyEvent = iTipReply.getEvents().get(0);
        assertAttendeePartStat(replyEvent.getAttendees(), userA.getEmail1(), ParticipationStatus.ACCEPTED);
        /*
         * as invited user a, delete single occurrence of the series
         */
        Date startDate = DateTimeUtil.parseDateTime(eventData.getStartDate());
        DateTime recurrenceId = new DateTime(TimeZone.getTimeZone("Europe/Berlin"), CalendarUtils.add(startDate, Calendar.DATE, 2).getTime());
        DeleteEventBody deleteBody = new DeleteEventBody();
        deleteBody.addEventsItem(new EventId().folder(eventData.getFolder()).id(eventData.getId()).recurrenceId(CalendarUtils.encode(recurrenceId)));
        ChronosMultipleCalendarResultResponse deleteResponse = chronosApi.deleteEvent(eventData.getTimestamp(), deleteBody, null, null, null, null, null, null, null);
        assertNull(deleteResponse.getError(), deleteResponse.getError());
        /*
         * check deleted occurrence appears as delete exception in event series
         */
        eventData = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), null, null, null).getData();
        List<String> deleteExceptionDates = eventData.getDeleteExceptionDates();
        assertTrue(null != deleteExceptionDates && 1 == deleteExceptionDates.size());
        assertEquals(CalendarUtils.encode(recurrenceId), deleteExceptionDates.get(0));
        /*
         * receive & analyze iMIP reply as user b
         */
        iMipReplyData = receiveIMip(apiClientC2, userA.getEmail1(), summary, -1, uid, new DefaultRecurrenceId(recurrenceId), SchedulingMethod.REPLY);
        assertNotNull(iMipReplyData);
        iTipReply = parseICalAttachment(apiClientC2, iMipReplyData);
        assertEquals("REPLY", iTipReply.getMethod());
        assertTrue(null != iTipReply.getEvents() && 1 == iTipReply.getEvents().size());
        replyEvent = iTipReply.getEvents().get(0);
        assertAttendeePartStat(replyEvent.getAttendees(), userA.getEmail1(), ParticipationStatus.DECLINED);
        /*
         * as user b, re-schedule the declined occurrence
         */
        EventData eventOccurrenceC2 = eventManagerC2.getEvent(eventDataC2.getFolder(), eventDataC2.getId(), CalendarUtils.encode(recurrenceId), false);
        EventData updatedOccurrenceC2 = new EventData();
        updatedOccurrenceC2.setId(eventDataC2.getId());
        updatedOccurrenceC2.setRecurrenceId(CalendarUtils.encode(recurrenceId));
        updatedOccurrenceC2.setAttendees(null); //TODO: neutral defaults
        Date updatedStart = CalendarUtils.add(DateTimeUtil.parseDateTime(eventOccurrenceC2.getStartDate()), Calendar.HOUR, 1);
        Date updatedEnd = CalendarUtils.add(DateTimeUtil.parseDateTime(eventOccurrenceC2.getEndDate()), Calendar.HOUR, 1);
        updatedOccurrenceC2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), updatedStart.getTime()));
        updatedOccurrenceC2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), updatedEnd.getTime()));
        updatedOccurrenceC2 = eventManagerC2.updateOccurenceEvent(updatedOccurrenceC2, CalendarUtils.encode(recurrenceId), true);
        /*
         * receive & analyze iMIP request as user a
         */
        iMipRequestData = receiveIMip(apiClient, userB.getEmail1(), summary, -1, uid, new DefaultRecurrenceId(recurrenceId), SchedulingMethod.REQUEST);
        newEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(uid, newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), userA.getEmail1(), "NEEDS-ACTION");
        /*
         * reply with "accepted" for the occurrence
         */
        ActionResponse actionResponse = accept(constructBody(iMipRequestData), null);
        EventData updatedOccurrence = null;
        for (EventData updatedEventData : actionResponse.getData()) {
            if (uid.equals(updatedEventData.getUid()) && CalendarUtils.encode(recurrenceId).equals(updatedEventData.getRecurrenceId())) {
                updatedOccurrence = updatedEventData;
                break;
            }
        }
        assertNotNull(updatedOccurrence);
        assertAttendeePartStat(updatedOccurrence.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * verify the re-instantiated event occurrence appears again properly in the calendar
         */
        updatedOccurrence = chronosApi.getEvent(updatedOccurrence.getId(), updatedOccurrence.getFolder(), updatedOccurrence.getRecurrenceId(), null, null).getData();
        assertNotNull(updatedOccurrence);
        assertEquals(CalendarUtils.encode(recurrenceId), updatedOccurrence.getRecurrenceId());
        assertAttendeePartStat(updatedOccurrence.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * check deleted occurrence appears as change exception again in event series
         */
        eventData = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), null, null, null).getData();
        deleteExceptionDates = eventData.getDeleteExceptionDates();
        assertTrue(null == deleteExceptionDates || 0 == deleteExceptionDates.size());
        List<String> changeExceptionDates = eventData.getChangeExceptionDates();
        assertTrue(null != changeExceptionDates && 1 == changeExceptionDates.size());
        assertEquals(CalendarUtils.encode(recurrenceId), changeExceptionDates.get(0));
    }

    @Test
    public void testDeleteChangeExceptionAsAttendee() throws Exception {
        /*
         * as user b from context 2, create event series and invite user a from context 1
         */
        UserData userB = userResponseC2.getData();
        UserData userA = userResponseC1.getData();
        String uid = UUID.randomUUID().toString();
        String summary = UUID.randomUUID().toString();
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        Date start = com.openexchange.time.TimeTools.D("next monday afternoon", timeZone);
        Date end = CalendarUtils.add(start, Calendar.HOUR, 1);
        EventData eventDataC2 = new EventData();
        eventDataC2.setUid(uid);
        eventDataC2.setSummary(summary);
        eventDataC2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
        eventDataC2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
        eventDataC2.setRrule("FREQ=DAILY;COUNT=6");
        eventDataC2.setAttendees(Arrays.asList(AttendeeFactory.createIndividual(userB.getUserId()), AttendeeFactory.createIndividual(userA.getEmail1())));
        eventDataC2.setOrganizer(AttendeeFactory.createOrganizerFrom(AttendeeFactory.createIndividual(userB.getUserId())));
        eventDataC2 = eventManagerC2.createEvent(eventDataC2);
        /*
         * receive & analyze iMIP request as user a
         */
        MailData iMipRequestData = receiveIMip(apiClient, userB.getEmail1(), summary, 0, uid, null, SchedulingMethod.REQUEST);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(uid, newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), userA.getEmail1(), "NEEDS-ACTION");
        /*
         * reply with "accepted" for the event series
         */
        EventData eventData = assertSingleEvent(accept(constructBody(iMipRequestData), null));
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * check event in calendar
         */
        EventResponse eventResponse = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), eventData.getRecurrenceId(), null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        eventData = eventResponse.getData();
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * receive & analyze iMIP reply as user b
         */
        MailData iMipReplyData = receiveIMip(apiClientC2, userA.getEmail1(), summary, 0, uid, null, SchedulingMethod.REPLY);
        assertNotNull(iMipReplyData);
        ImportedCalendar iTipReply = parseICalAttachment(apiClientC2, iMipReplyData);
        assertEquals("REPLY", iTipReply.getMethod());
        assertTrue(null != iTipReply.getEvents() && 1 == iTipReply.getEvents().size());
        Event replyEvent = iTipReply.getEvents().get(0);
        assertAttendeePartStat(replyEvent.getAttendees(), userA.getEmail1(), ParticipationStatus.ACCEPTED);
        /*
         * create change exception as user b
         */
        Date startDate = DateTimeUtil.parseDateTime(eventData.getStartDate());
        DateTime recurrenceId = new DateTime(TimeZone.getTimeZone("Europe/Berlin"), CalendarUtils.add(startDate, Calendar.DATE, 2).getTime());
        EventData eventOccurrenceC2 = eventManagerC2.getEvent(eventDataC2.getFolder(), eventDataC2.getId(), CalendarUtils.encode(recurrenceId), false);
        EventData updatedOccurrenceC2 = new EventData();
        updatedOccurrenceC2.setId(eventDataC2.getId());
        updatedOccurrenceC2.setFolder(eventDataC2.getFolder());
        updatedOccurrenceC2.setRecurrenceId(CalendarUtils.encode(recurrenceId));
        updatedOccurrenceC2.setAttendees(null); //TODO: neutral defaults
        Date updatedStart = CalendarUtils.add(DateTimeUtil.parseDateTime(eventOccurrenceC2.getStartDate()), Calendar.HOUR, 1);
        Date updatedEnd = CalendarUtils.add(DateTimeUtil.parseDateTime(eventOccurrenceC2.getEndDate()), Calendar.HOUR, 1);
        updatedOccurrenceC2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), updatedStart.getTime()));
        updatedOccurrenceC2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), updatedEnd.getTime()));
        updatedOccurrenceC2 = eventManagerC2.updateOccurenceEvent(updatedOccurrenceC2, CalendarUtils.encode(recurrenceId), true);
        updatedOccurrenceC2 = eventManagerC2.getEvent(eventDataC2.getFolder(), eventDataC2.getId(), CalendarUtils.encode(recurrenceId), false);
        /*
         * receive & analyze iMIP request as user a
         */
        iMipRequestData = receiveIMip(apiClient, userB.getEmail1(), summary, updatedOccurrenceC2.getSequence().intValue(), uid, new DefaultRecurrenceId(recurrenceId), SchedulingMethod.REQUEST);
        newEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(uid, newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), userA.getEmail1(), "NEEDS-ACTION");
        /*
         * reply with "accepted" for the change exception
         */
        ActionResponse actionResponse = accept(constructBody(iMipRequestData), null);
        EventData updatedOccurrence = null;
        for (EventData updatedEventData : actionResponse.getData()) {
            if (uid.equals(updatedEventData.getUid()) && CalendarUtils.encode(recurrenceId).equals(updatedEventData.getRecurrenceId())) {
                updatedOccurrence = updatedEventData;
                break;
            }
        }
        assertNotNull(updatedOccurrence);
        assertAttendeePartStat(updatedOccurrence.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * check change exception event in calendar
         */
        updatedOccurrence = chronosApi.getEvent(updatedOccurrence.getId(), updatedOccurrence.getFolder(), updatedOccurrence.getRecurrenceId(), null, null).getData();
        assertNotNull(updatedOccurrence);
        assertEquals(CalendarUtils.encode(recurrenceId), updatedOccurrence.getRecurrenceId());
        assertAttendeePartStat(updatedOccurrence.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * receive & analyze iMIP reply as user b
         */
        iMipReplyData = receiveIMip(apiClientC2, userA.getEmail1(), summary, updatedOccurrenceC2.getSequence().intValue(), uid, new DefaultRecurrenceId(recurrenceId), SchedulingMethod.REPLY);
        assertNotNull(iMipReplyData);
        iTipReply = parseICalAttachment(apiClientC2, iMipReplyData);
        assertEquals("REPLY", iTipReply.getMethod());
        assertTrue(null != iTipReply.getEvents() && 1 == iTipReply.getEvents().size());
        replyEvent = iTipReply.getEvents().get(0);
        assertAttendeePartStat(replyEvent.getAttendees(), userA.getEmail1(), ParticipationStatus.ACCEPTED);
        /*
         * as invited user a, delete the change exception
         */
        DeleteEventBody deleteBody = new DeleteEventBody();
        deleteBody.addEventsItem(new EventId().folder(updatedOccurrence.getFolder()).id(updatedOccurrence.getId()).recurrenceId(updatedOccurrence.getRecurrenceId()));
        ChronosMultipleCalendarResultResponse deleteResponse = chronosApi.deleteEvent(updatedOccurrence.getTimestamp(), deleteBody, null, null, null, null, null, null, null);
        assertNull(deleteResponse.getError(), deleteResponse.getError());
        /*
         * check deleted occurrence appears as delete exception in event series
         */
        eventData = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), null, null, null).getData();
        List<String> deleteExceptionDates = eventData.getDeleteExceptionDates();
        assertTrue(null != deleteExceptionDates && 1 == deleteExceptionDates.size());
        assertEquals(CalendarUtils.encode(recurrenceId), deleteExceptionDates.get(0));
        /*
         * receive & analyze iMIP reply as user b
         */
        // @formatter:off
        iMipReplyData = new IMipReceiver(apiClientC2).from(userA.getEmail1())
                                                     .subject(summary)
                                                     .sequence(updatedOccurrenceC2.getSequence())
                                                     .uid(uid)
                                                     .recurrenceId(new DefaultRecurrenceId(recurrenceId))
                                                     .method(SchedulingMethod.REPLY)
                                                     .partStat(ParticipationStatus.DECLINED)
                                                     .receive();
        // @formatter:on
        assertNotNull(iMipReplyData);
        iTipReply = parseICalAttachment(apiClientC2, iMipReplyData);
        assertEquals("REPLY", iTipReply.getMethod());
        assertTrue(null != iTipReply.getEvents() && 1 == iTipReply.getEvents().size());
        replyEvent = iTipReply.getEvents().get(0);
        assertAttendeePartStat(replyEvent.getAttendees(), userA.getEmail1(), ParticipationStatus.DECLINED);
        /*
         * as user b, re-schedule the declined occurrence again
         */
        eventOccurrenceC2 = eventManagerC2.getEvent(eventDataC2.getFolder(), eventDataC2.getId(), CalendarUtils.encode(recurrenceId), false);
        updatedOccurrenceC2 = new EventData();
        updatedOccurrenceC2.setFolder(eventOccurrenceC2.getFolder());
        updatedOccurrenceC2.setId(eventOccurrenceC2.getId());
        updatedOccurrenceC2.setRecurrenceId(eventOccurrenceC2.getRecurrenceId());
        updatedOccurrenceC2.setAttendees(null); //TODO: neutral defaults
        updatedStart = CalendarUtils.add(DateTimeUtil.parseDateTime(eventOccurrenceC2.getStartDate()), Calendar.MINUTE, 30);
        updatedEnd = CalendarUtils.add(DateTimeUtil.parseDateTime(eventOccurrenceC2.getEndDate()), Calendar.MINUTE, 30);
        updatedOccurrenceC2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), updatedStart.getTime()));
        updatedOccurrenceC2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), updatedEnd.getTime()));
        updatedOccurrenceC2 = eventManagerC2.updateOccurenceEvent(updatedOccurrenceC2, eventOccurrenceC2.getRecurrenceId(), true);
        updatedOccurrenceC2 = eventManagerC2.getEvent(eventOccurrenceC2.getFolder(), eventOccurrenceC2.getId(), eventOccurrenceC2.getRecurrenceId(), false);
        /*
         * receive & analyze iMIP request as user a
         */
        iMipRequestData = receiveIMip(apiClient, userB.getEmail1(), summary, updatedOccurrenceC2.getSequence().intValue(), uid, new DefaultRecurrenceId(recurrenceId), SchedulingMethod.REQUEST);
        newEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(uid, newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), userA.getEmail1(), "NEEDS-ACTION");
        /*
         * reply with "accepted" for the occurrence
         */
        actionResponse = accept(constructBody(iMipRequestData), null);
        updatedOccurrence = null;
        for (EventData updatedEventData : actionResponse.getData()) {
            if (uid.equals(updatedEventData.getUid()) && CalendarUtils.encode(recurrenceId).equals(updatedEventData.getRecurrenceId())) {
                updatedOccurrence = updatedEventData;
                break;
            }
        }
        assertNotNull(updatedOccurrence);
        assertAttendeePartStat(updatedOccurrence.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * verify the re-instantiated event occurrence appears again properly in the calendar
         */
        updatedOccurrence = chronosApi.getEvent(updatedOccurrence.getId(), updatedOccurrence.getFolder(), updatedOccurrence.getRecurrenceId(), null, null).getData();
        assertNotNull(updatedOccurrence);
        assertEquals(CalendarUtils.encode(recurrenceId), updatedOccurrence.getRecurrenceId());
        assertAttendeePartStat(updatedOccurrence.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * check deleted occurrence appears as change exception again in event series
         */
        eventData = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), null, null, null).getData();
        deleteExceptionDates = eventData.getDeleteExceptionDates();
        assertTrue(null == deleteExceptionDates || 0 == deleteExceptionDates.size());
        List<String> changeExceptionDates = eventData.getChangeExceptionDates();
        assertTrue(null != changeExceptionDates && 1 == changeExceptionDates.size());
        assertEquals(CalendarUtils.encode(recurrenceId), changeExceptionDates.get(0));
    }

    @Test
    public void testDeleteDeletedOccurrenceAsAttendee() throws Exception {
        /*
         * as user b from context 2, create event series and invite user a from context 1
         */
        UserData userB = userResponseC2.getData();
        UserData userA = userResponseC1.getData();
        String uid = UUID.randomUUID().toString();
        String summary = UUID.randomUUID().toString();
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        Date start = com.openexchange.time.TimeTools.D("next monday afternoon", timeZone);
        Date end = CalendarUtils.add(start, Calendar.HOUR, 1);
        EventData eventDataC2 = new EventData();
        eventDataC2.setUid(uid);
        eventDataC2.setSummary(summary);
        eventDataC2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
        eventDataC2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
        eventDataC2.setRrule("FREQ=DAILY;COUNT=6");
        eventDataC2.setAttendees(Arrays.asList(AttendeeFactory.createIndividual(userB.getUserId()), AttendeeFactory.createIndividual(userA.getEmail1())));
        eventDataC2.setOrganizer(AttendeeFactory.createOrganizerFrom(AttendeeFactory.createIndividual(userB.getUserId())));
        eventDataC2 = eventManagerC2.createEvent(eventDataC2);
        /*
         * receive & analyze iMIP request as user a
         */
        MailData iMipRequestData = receiveIMip(apiClient, userB.getEmail1(), summary, 0, uid, null, SchedulingMethod.REQUEST);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(uid, newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), userA.getEmail1(), "NEEDS-ACTION");
        /*
         * reply with "accepted" for the event series
         */
        EventData eventData = assertSingleEvent(accept(constructBody(iMipRequestData), null));
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * check event in calendar
         */
        EventResponse eventResponse = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), eventData.getRecurrenceId(), null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        eventData = eventResponse.getData();
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), userA.getEmail1(), "ACCEPTED");
        /*
         * receive & analyze iMIP reply as user b
         */
        MailData iMipReplyData = receiveIMip(apiClientC2, userA.getEmail1(), summary, 0, uid, null, SchedulingMethod.REPLY);
        assertNotNull(iMipReplyData);
        ImportedCalendar iTipReply = parseICalAttachment(apiClientC2, iMipReplyData);
        assertEquals("REPLY", iTipReply.getMethod());
        assertTrue(null != iTipReply.getEvents() && 1 == iTipReply.getEvents().size());
        Event replyEvent = iTipReply.getEvents().get(0);
        assertAttendeePartStat(replyEvent.getAttendees(), userA.getEmail1(), ParticipationStatus.ACCEPTED);
        /*
         * as invited user a, delete single occurrence of the series
         */
        Date startDate = DateTimeUtil.parseDateTime(eventData.getStartDate());
        DateTime recurrenceId = new DateTime(TimeZone.getTimeZone("Europe/Berlin"), CalendarUtils.add(startDate, Calendar.DATE, 2).getTime());
        DeleteEventBody deleteBody = new DeleteEventBody();
        deleteBody.addEventsItem(new EventId().folder(eventData.getFolder()).id(eventData.getId()).recurrenceId(CalendarUtils.encode(recurrenceId)));
        ChronosMultipleCalendarResultResponse deleteResponse = chronosApi.deleteEvent(eventData.getTimestamp(), deleteBody, null, null, null, null, null, null, null);
        assertNull(deleteResponse.getError(), deleteResponse.getError());
        /*
         * check deleted occurrence appears as delete exception in event series
         */
        eventData = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), null, null, null).getData();
        List<String> deleteExceptionDates = eventData.getDeleteExceptionDates();
        assertTrue(null != deleteExceptionDates && 1 == deleteExceptionDates.size());
        assertEquals(CalendarUtils.encode(recurrenceId), deleteExceptionDates.get(0));
        /*
         * receive & analyze iMIP reply as user b
         */
        iMipReplyData = receiveIMip(apiClientC2, userA.getEmail1(), summary, -1, uid, new DefaultRecurrenceId(recurrenceId), SchedulingMethod.REPLY);
        assertNotNull(iMipReplyData);
        iTipReply = parseICalAttachment(apiClientC2, iMipReplyData);
        assertEquals("REPLY", iTipReply.getMethod());
        assertTrue(null != iTipReply.getEvents() && 1 == iTipReply.getEvents().size());
        replyEvent = iTipReply.getEvents().get(0);
        assertAttendeePartStat(replyEvent.getAttendees(), userA.getEmail1(), ParticipationStatus.DECLINED);
        /*
         * as user b, also delete the declined occurrence
         */
        EventId eventId = new EventId().folder(eventDataC2.getFolder()).id(eventDataC2.getId()).recurrenceId(CalendarUtils.encode(recurrenceId));
        eventManagerC2.deleteEvent(eventId);
        /*
         * receive & analyze iMIP request as user a
         */
        iMipRequestData = receiveIMip(apiClient, userB.getEmail1(), summary, -1, uid, new DefaultRecurrenceId(recurrenceId), SchedulingMethod.CANCEL);
        AnalysisChangeDeletedEvent deletedEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getDeletedEvent();
        assertNotNull(deletedEvent);
        assertEquals(uid, deletedEvent.getUid());
        cancel(apiClient, constructBody(iMipRequestData), null, true);
        /*
         * verify the event occurrence appears is deleted
         */
        eventData = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), null, null, null).getData();
        deleteExceptionDates = eventData.getDeleteExceptionDates();
        assertTrue(null != deleteExceptionDates && 1 == deleteExceptionDates.size());
        assertEquals(CalendarUtils.encode(recurrenceId), deleteExceptionDates.get(0));
    }

}
