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

package com.openexchange.dav.caldav.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.chronos.ResourceId;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Participant;
import com.openexchange.resource.Resource;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link ResourceAttendeeTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.3
 */
public class ResourceAttendeeTest extends CalDAVTest {

    private Resource resource;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        Integer res = testContext.acquireResource();
        Assertions.assertNotNull(res);
        resource = resTm.get(res.intValue());
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateViaResourceIdWithCorrectCUType(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        int contextId = getClient().getValues().getContextId();
        testCreate(resource.getIdentifier(), ResourceId.forResource(contextId, resource.getIdentifier()), "RESOURCE");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateViaMailWithCorrectCUType(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testCreate(resource.getIdentifier(), "mailto:" + resource.getMail(), "RESOURCE");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateViaResourceIdWithoutCUType(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        int contextId = getClient().getValues().getContextId();
        testCreate(resource.getIdentifier(), ResourceId.forResource(contextId, resource.getIdentifier()), null);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateViaMailWithoutCUType(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testCreate(resource.getIdentifier(), "mailto:" + resource.getMail(), null);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateViaResourceIdWithIncorrectCUType(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        int contextId = getClient().getValues().getContextId();
        testCreate(resource.getIdentifier(), ResourceId.forResource(contextId, resource.getIdentifier()), "INDIVIDUAL");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateViaMailWithIncorrectCUType(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testCreate(resource.getIdentifier(), "mailto:" + resource.getMail(), "INDIVIDUAL");
    }

    private void testCreate(int resourceId, String uri, String cuType) throws Exception {
        /*
         * create appointment on client
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 12:00");
        Date end = TimeTools.D("next monday at 13:00");
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" +
            "VERSION:2.0" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART:" + formatAsUTC(start) + "\r\n" +
            "DTEND:" + formatAsUTC(end) + "\r\n" +
            "SUMMARY:ResourceAttendeeTest"  + "\r\n" +
            "ORGANIZER:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=NEEDS-ACTION" + (null != cuType ? ";CUTYPE=" + cuType : "") + ":" + uri + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR" + "\r\n";
        // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "Appointment not found on server");
        rememberForCleanUp(appointment);
        Participant resourceParticipant = null;
        for (Participant participant : appointment.getParticipants()) {
            if (participant.getIdentifier() == resourceId) {
                resourceParticipant = participant;
                break;
            }
        }
        assertNotNull(resourceParticipant, "Resource participant not found");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");

        Property resoureceAttendee = iCalResource.getVEvent().getAttendee(uri);
        assertNotNull(resourceParticipant, "Resource attendee not found");
        assertEquals("RESOURCE", resoureceAttendee.getAttribute("CUTYPE"), "CUTYPE wrong");
        assertEquals("ACCEPTED", resoureceAttendee.getAttribute("PARTSTAT"), "PARTSTAT wrong");
    }

}
