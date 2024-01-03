package com.openexchange.ajax.chronos.itip.bugs;

import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.itip.AbstractITipAnalyzeTest;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.models.Analysis;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;

/**
 * {@link MWB2227Test}
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MWB2227Test extends AbstractITipAnalyzeTest {

	@Override
	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		super.setUp(testInfo);
	}

	@Test
    public void testReInviteToOccurrence() throws Exception {
		/*
         * in context 2, create event series and invite user from context 1
         */
		EventData eventData = new EventData();
		eventData.setFolder(folderIdC2);
		eventData.setSummary(UUIDs.getUnformattedStringFromRandom());
        TimeZone timeZone = TimeZone.getTimeZone("America/Sao_Paulo");
        Date start = com.openexchange.time.TimeTools.D("next monday evening", timeZone);
		Date end = CalendarUtils.add(start, Calendar.HOUR, 1);
		eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
		eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
		eventData.setRrule("FREQ=DAILY;COUNT=10");
		CalendarUser organizerC2 = new CalendarUser();
		organizerC2.setEntity(I(testUserC2.getUserId()));
		eventData.setOrganizer(organizerC2);
		List<Attendee> attendees = new ArrayList<Attendee>();
		Attendee attendeeC2 = new Attendee();
		attendeeC2.setCuType(CuTypeEnum.INDIVIDUAL);
		attendeeC2.setEntity(I(testUserC2.getUserId()));
		attendees.add(attendeeC2);
        Attendee attendeeC1 = new Attendee();
        attendeeC1.setCuType(CuTypeEnum.INDIVIDUAL);
        attendeeC1.setUri(CalendarUtils.getURI(userResponseC1.getData().getEmail1()));
        attendees.add(attendeeC1);
		eventData.setAttendees(attendees);
		EventData createdEvent = eventManagerC2.createEvent(eventData, true);
		createdEvent = eventManagerC2.getEvent(folderIdC2, createdEvent.getId());
        /*
         * as attendee in context 1, receive & apply invitation to series
         */
        MailData iMip = receiveIMip(testUser.getApiClient(), userResponseC2.getData().getEmail1(), createdEvent.getSummary(), 
            createdEvent.getSequence().intValue(), createdEvent.getUid(), null, SchedulingMethod.REQUEST);
        AnalyzeResponse analyzeResponse = analyze(testUser.getApiClient(), iMip);
        Analysis analysis = analyzeResponse.getData().get(0);
        assertTrue(null != analysis.getChanges() && 0 < analysis.getChanges().size());
        CalendarResult applyResult = applyCreate(testUser.getApiClient(), constructBody(iMip));
        assertTrue(null != applyResult.getCreated() && 0 < applyResult.getCreated().size() || null != applyResult.getUpdated() && 0 < applyResult.getUpdated().size());
        /*
         * verify appointment series in calendar of user on context 1
         */
        List<EventData> eventOccurrencesC1 = new ArrayList<EventData>();
        List<EventData> allEventsC1 = eventManager.getAllEvents(defaultFolderId, new Date(), CalendarUtils.add(new Date(), Calendar.MONTH, 2), true);
        for (EventData eventC1 : allEventsC1) {
            if (createdEvent.getUid().equals(eventC1.getUid())) {
                eventOccurrencesC1.add(eventC1);
            }
        }
        assertEquals(10, eventOccurrencesC1.size());
		/*
         * as organizer in context 2, update third occurrence of event and remove user from context 1
         */
        String recurrenceId = eventOccurrencesC1.get(2).getRecurrenceId();
        EventData occurrenceUpdate = new EventData();
        occurrenceUpdate.setFolder(createdEvent.getFolder());
        occurrenceUpdate.setId(createdEvent.getId());
        occurrenceUpdate.setRecurrenceId(recurrenceId);
        occurrenceUpdate.setAttendees(Arrays.asList(attendeeC2));
        EventData updatedOccurence = eventManagerC2.updateOccurenceEvent(occurrenceUpdate, recurrenceId, true);
        /*
         * as attendee in context 1, receive & apply cancellation of occurrence
         */
        iMip = receiveIMip(testUser.getApiClient(), userResponseC2.getData().getEmail1(), createdEvent.getSummary(), 
            updatedOccurence.getSequence().intValue(), createdEvent.getUid(), new DefaultRecurrenceId(CalendarUtils.decode(recurrenceId)), SchedulingMethod.CANCEL);
        analyzeResponse = analyze(testUser.getApiClient(), iMip);
        analysis = analyzeResponse.getData().get(0);
        assertTrue(null != analysis.getChanges() && 0 < analysis.getChanges().size());
        applyResult = applyRemove(testUser.getApiClient(), constructBody(iMip));
        assertTrue(null != applyResult.getUpdated() && 0 < applyResult.getUpdated().size());
        /*
         * verify appointment series in calendar of user on context 1
         */
        eventOccurrencesC1 = new ArrayList<EventData>();
        allEventsC1 = eventManager.getAllEvents(defaultFolderId, new Date(), CalendarUtils.add(new Date(), Calendar.MONTH, 2), true);
        for (EventData eventC1 : allEventsC1) {
            if (createdEvent.getUid().equals(eventC1.getUid())) {
                eventOccurrencesC1.add(eventC1);
            }
        }
        assertEquals(9, eventOccurrencesC1.size());
        /*
         * as organizer in context 2, update third occurrence of event and add user from context 1 again
         */
        occurrenceUpdate = new EventData();
        occurrenceUpdate.setFolder(createdEvent.getFolder());
        occurrenceUpdate.setId(updatedOccurence.getId());
        occurrenceUpdate.setRecurrenceId(recurrenceId);
        occurrenceUpdate.setAttendees(Arrays.asList(attendeeC2, attendeeC1 ));
        updatedOccurence = eventManagerC2.updateOccurenceEvent(occurrenceUpdate, recurrenceId, true);
        /*
         * as attendee in context 1, receive & apply request for new occurrence
         */
        iMip = receiveIMip(testUser.getApiClient(), userResponseC2.getData().getEmail1(), createdEvent.getSummary(), 
            updatedOccurence.getSequence().intValue(), createdEvent.getUid(), new DefaultRecurrenceId(CalendarUtils.decode(recurrenceId)), SchedulingMethod.REQUEST);
        analyzeResponse = analyze(testUser.getApiClient(), iMip);
        analysis = analyzeResponse.getData().get(0);
        assertTrue(null != analysis.getChanges() && 0 < analysis.getChanges().size());
        applyResult = applyCreate(testUser.getApiClient(), constructBody(iMip));
        assertTrue(null != applyResult.getCreated() && 0 < applyResult.getCreated().size() || null != applyResult.getUpdated() && 0 < applyResult.getUpdated().size());
        /*
         * verify appointment series in calendar of user on context 1
         */
        eventOccurrencesC1 = new ArrayList<EventData>();
        allEventsC1 = eventManager.getAllEvents(defaultFolderId, new Date(), CalendarUtils.add(new Date(), Calendar.MONTH, 2), true);
        for (EventData eventC1 : allEventsC1) {
            if (createdEvent.getUid().equals(eventC1.getUid())) {
                eventOccurrencesC1.add(eventC1);
            }
        }
        assertEquals(10, eventOccurrencesC1.size());
	}
}
