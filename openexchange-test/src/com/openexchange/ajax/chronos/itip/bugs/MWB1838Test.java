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

import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.google.gson.Gson;
import com.openexchange.ajax.chronos.itip.AbstractITipAnalyzeTest;
import com.openexchange.ajax.chronos.itip.ITipUtil;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailReplyData;
import com.openexchange.testing.httpclient.models.MailReplyResponse;
import com.openexchange.testing.httpclient.models.UserResponse;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.UserApi;

/**
 * {@link MWB1838Test}
 * 
 * Forwarded invitation cannot be added to calendar
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MWB1838Test extends AbstractITipAnalyzeTest {

    public static List<String> data() {
        return Arrays.asList("always", "never", "known");
    }

    private UserResponse userResponse2;
    private MailApi mailApi2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        userResponse2 = new UserApi(testUser2.getApiClient()).getUser(String.valueOf(testUser2.getUserId()));
        mailApi2 = new MailApi(testUser2.getApiClient());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testApplyForwardedInvitation(String data) throws Exception {

        ITipUtil.setAutoProcessing(new JSlobApi(apiClient), data);
        /*
         * in context 2, create event and add user B from context 1
         */
        EventData eventData = new EventData();
        eventData.setFolder(folderIdC2);
        eventData.setSummary(UUIDs.getUnformattedStringFromRandom());
        TimeZone timeZone = TimeZone.getTimeZone("Australia/Darwin");
        Date start = com.openexchange.time.TimeTools.D("next thursday afternoon", timeZone);
        Date end = CalendarUtils.add(start, Calendar.HOUR, 1);
        eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
        eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
        CalendarUser organizer = new CalendarUser();
        organizer.setEntity(I(testUserC2.getUserId()));
        eventData.setOrganizer(organizer);
        List<Attendee> attendees = new ArrayList<Attendee>();
        Attendee attendee1 = new Attendee();
        attendee1.setCuType(CuTypeEnum.INDIVIDUAL);
        attendee1.setEntity(I(testUserC2.getUserId()));
        attendees.add(attendee1);
        Attendee attendee2 = new Attendee();
        attendee2.setCuType(CuTypeEnum.INDIVIDUAL);
        attendee2.setUri(CalendarUtils.getURI(userResponse2.getData().getEmail1()));
        attendees.add(attendee2);
        eventData.setAttendees(attendees);
        /*
         * create event
         */
        EventData createdEvent = eventManagerC2.createEvent(eventData, true);
        createdEvent = eventManagerC2.getEvent(folderIdC2, createdEvent.getId());
        /*
         * as user B in context 1, receive & verify imip
         */
        MailData iMip2 = receiveIMip(testUser2.getApiClient(), userResponseC2.getData().getEmail1(), createdEvent.getSummary(), 0, SchedulingMethod.REQUEST);
        assertSingleChange(analyze(testUser2.getApiClient(), iMip2)).getNewEvent();
        /*
         * as user B, forward received mail to user A
         */
        MailReplyResponse replyResponse = mailApi2.forwardMailBuilder().withFolder(iMip2.getFolderId()).withId(iMip2.getId()).execute();
        MailReplyData replyData = replyResponse.getData();
        replyData.setTo(Collections.singletonList(java.util.Arrays.asList((String) null, userResponseC1.getData().getEmail1())));
        String result = mailApi2.sendMailBuilder().withJson0(new Gson().toJson(replyData)).execute();
        assertNotNull(result);
        /*
         * as user A, receive forwarded mail & apply the creation
         */
        MailData iMip = receiveIMip(testUser.getApiClient(), userResponse2.getData().getEmail1(), createdEvent.getSummary(), 0, SchedulingMethod.REQUEST);
        assertSingleChange(analyze(testUser2.getApiClient(), iMip)).getNewEvent();
        CalendarResult applyResult = applyCreate(testUser.getApiClient(), constructBody(iMip));
        assertTrue(null != applyResult.getCreated() && 1 == applyResult.getCreated().size());
        /*
         * lookup event in calendar & check stored data
         */
        EventData appliedEvent = eventManager.getEvent(applyResult.getCreated().get(0).getFolder(), applyResult.getCreated().get(0).getId());
        assertNotNull(appliedEvent);
        assertEquals(eventData.getSummary(), appliedEvent.getSummary());
        assertEquals(CalendarUtils.getURI(userResponseC2.getData().getEmail1()), appliedEvent.getOrganizer().getUri());
        assertTrue(null != appliedEvent.getAttendees() && 3 == appliedEvent.getAttendees().size());
        for (String expected : new String[] { userResponseC2.getData().getEmail1(), userResponse2.getData().getEmail1(), userResponseC1.getData().getEmail1() }) {
            assertNotNull(lookupAttendee(appliedEvent.getAttendees(), expected));
        }
    }

    private static Attendee lookupAttendee(List<Attendee> attendees, String email) {
        if (null != attendees) {
            String uri = CalendarUtils.getURI(email);
            for (Attendee attendee : attendees) {
                if (uri.equals(attendee.getUri())) {
                    return attendee;
                }
            }
        }
        return null;
    }

}
