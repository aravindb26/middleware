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
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.api.Assertions;
import com.openexchange.dav.Config;
import com.openexchange.dav.Headers;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug42104Test}
 *
 * "Shown as" status-change is lost after updating appointment in iCal client
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug42104Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testGetWithIfMatch(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment on server
         */
        String uid = randomUID();
        String summary = "Bug42104Test";
        String location = "da";
        Date start = TimeTools.D("next friday at 02:00");
        Date end = TimeTools.D("next friday at 04:00");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        rememberForCleanUp(create(appointment));
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        String originalETag = iCalResource.getETag();
        /*
         * change something in the client
         */
        iCalResource.getVEvent().setSummary(summary + "_edit");
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        /*
         * get appointment with original eTag on client
         */
        GetMethod get = null;
        try {
            String href = Config.getPathPrefix() + "/caldav/" + encodeFolderID(getDefaultFolderID()) + "/" + uid + ".ics";
            get = new GetMethod(getBaseUri() + href);
            get.addRequestHeader(Headers.IF_MATCH, originalETag);
            Assertions.assertEquals(304, webDAVClient.executeMethod(get), "response code wrong");
        } finally {
            release(get);
        }
    }

}
