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

import static com.openexchange.ajax.chronos.LinkedAttachmentsTest.assertAttachments;
import static com.openexchange.ajax.chronos.LinkedAttachmentsTest.prepareInlineAttachment;
import static com.openexchange.ajax.chronos.LinkedAttachmentsTest.prepareRandomLinkedAttachment;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.asset.Asset;
import com.openexchange.test.common.asset.AssetType;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.ChronosAttachment;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;

/**
 * {@link ITipLinkedAttachmentsTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.5
 */
public class ITipLinkedAttachmentsTest extends AbstractITipAnalyzeTest {

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).build();
    }
    
    @Test
    public void testCreateWithMultipleLinkedAttachments() throws Exception {
        /*
         * prepare event with linked attachments, organized from user in context 2
         */
        List<ChronosAttachment> chronosAttachments = new ArrayList<ChronosAttachment>();
        for (int i = 0; i < 3; i++) {
            chronosAttachments.add(prepareRandomLinkedAttachment());
        }
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Tbilisi");
        Date start = TimeTools.D("Next week in the afternoon", timeZone);
        Date end = CalendarUtils.add(start, Calendar.HOUR, 4, timeZone);
        EventData eventData = new EventData();
        eventData.setSummary(UUIDs.getUnformattedStringFromRandom());
        eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
        eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
        eventData.setAttachments(chronosAttachments);
        eventData.setOrganizer(
            new CalendarUser().entity(userResponseC2.getData().getUserId()).uri(CalendarUtils.getURI(userResponseC2.getData().getEmail1()))
        );
        eventData.setAttendees(Arrays.asList(
            new Attendee().cuType(CuTypeEnum.INDIVIDUAL).uri(CalendarUtils.getURI(userResponseC1.getData().getEmail1())), 
            new Attendee().cuType(CuTypeEnum.INDIVIDUAL).entity(userResponseC2.getData().getUserId()).uri(CalendarUtils.getURI(userResponseC2.getData().getEmail1())).partStat("ACCEPTED")
        ));
        /*
         * create event as user in context 2 & check stored attachment metadata
         */
        EventData createdEvent = eventManagerC2.createEvent(eventData, true);
        assertAttachments(chronosAttachments, createdEvent);
        /*
         * receive iMIP as user in context 1 & apply the changes into the user's calendar
         */
        MailData iMipMail = receiveIMip(apiClient, userResponseC2.getData().getEmail1(), createdEvent.getSummary(), createdEvent.getSequence().intValue(), SchedulingMethod.REQUEST);
        CalendarResult createResult = applyCreate(apiClient, constructBody(iMipMail));
        assertTrue(null != createResult.getCreated() && 1 == createResult.getCreated().size());
        /*
         * lookup event in calendar & check stored attachment metadata
         */
        createdEvent = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertAttachments(chronosAttachments, createdEvent);
    }

    @Test
    public void testCreateWithMixedModeAttachments() throws Exception {
        /*
         * prepare event with linked attachments, organized from user in context 2
         */
        List<ChronosAttachment> chronosAttachments = new ArrayList<ChronosAttachment>();
        chronosAttachments.add(prepareRandomLinkedAttachment());
        Asset assetA = assetManager.getRandomAsset(AssetType.jpg);
        chronosAttachments.add(prepareInlineAttachment(assetA, 0));
        chronosAttachments.add(prepareRandomLinkedAttachment());
        Asset assetB = assetManager.getRandomAsset(AssetType.png);
        chronosAttachments.add(prepareInlineAttachment(assetB, 1));
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Tbilisi");
        Date start = TimeTools.D("Next week in the afternoon", timeZone);
        Date end = CalendarUtils.add(start, Calendar.HOUR, 4, timeZone);
        EventData eventData = new EventData();
        eventData.setSummary(UUIDs.getUnformattedStringFromRandom());
        eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
        eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
        eventData.setAttachments(chronosAttachments);
        eventData.setOrganizer(
            new CalendarUser().entity(userResponseC2.getData().getUserId()).uri(CalendarUtils.getURI(userResponseC2.getData().getEmail1()))
        );
        eventData.setAttendees(Arrays.asList(
            new Attendee().cuType(CuTypeEnum.INDIVIDUAL).uri(CalendarUtils.getURI(userResponseC1.getData().getEmail1())), 
            new Attendee().cuType(CuTypeEnum.INDIVIDUAL).entity(userResponseC2.getData().getUserId()).uri(CalendarUtils.getURI(userResponseC2.getData().getEmail1())).partStat("ACCEPTED")
        ));
        /*
         * create event as user in context 2 & check stored attachment metadata
         */
        JSONObject response = eventManagerC2.createEventWithAttachments(eventData, Arrays.asList(assetA, assetB));
        EventData createdEvent = eventManagerC2.getEvent(response.getString("folder"), response.getString("id"));
        assertAttachments(chronosAttachments, createdEvent);
        /*
         * receive iMIP as user in context 1 & apply the changes into the user's calendar
         */
        MailData iMipMail = receiveIMip(apiClient, userResponseC2.getData().getEmail1(), createdEvent.getSummary(), createdEvent.getSequence().intValue(), SchedulingMethod.REQUEST);
        CalendarResult createResult = applyCreate(apiClient, constructBody(iMipMail));
        assertTrue(null != createResult.getCreated() && 1 == createResult.getCreated().size());
        /*
         * lookup event in calendar & check stored attachment metadata
         */
        createdEvent = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertAttachments(chronosAttachments, createdEvent);
    }

}
