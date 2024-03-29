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

package com.openexchange.dav.caldav.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug39819Test}
 *
 * Deleted Umlaut \u00fc from appointment description
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug39819Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSpecialCharacters(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment on client
         */
        String uid = randomUID();
        Date start = TimeTools.D("next tuesday at 06:00");
        Date end = TimeTools.D("next tuesday at 07:00");
        String summary = "Test \u00fc_\u00f6_\u00e4_\u00fa_\u00ec_\u00f4_\u20ac_\u0160_\u0161_\u017d_\u017e_\u0152_\u0153_\u0178_" + "\u00a4_\u00a6_\u00a8_\u00b4_\u00b8_\u00bc_\u00bd_\u00be";
        String iCal = generateICal(start, end, uid, summary, "test");
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertEquals(summary, appointment.getTitle(), "Title wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getSummary(), "No SUMMARY in iCal found");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        /*
         * update appointment on client
         */
        iCalResource.getVEvent().setLocation("new location");
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify updated appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertEquals(summary, appointment.getTitle(), "Title wrong");
        assertEquals("new location", appointment.getLocation(), "Location wrong");
        /*
         * verify updated appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getSummary(), "No SUMMARY in iCal found");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvent().getLocation(), "No LOCATION in iCal found");
        assertEquals("new location", iCalResource.getVEvent().getLocation(), "LOCATION wrong");
    }

}
