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
import java.util.List;
import java.util.TimeZone;
import org.apache.jackrabbit.webdav.client.methods.OptionsMethod;
import org.junit.internal.AssumptionViolatedException;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link AvailabilityTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class AvailabilityTest extends CalDAVTest {

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        OptionsMethod options = null;
        try {
            options = new OptionsMethod(getBaseUri());
            assertEquals(StatusCodes.SC_OK, super.webDAVClient.executeMethod(options), "unexpected http status");
            assertResponseHeaders(new String[] { "calendar-availability" }, "DAV", options);
        } catch (AssertionError e) {
            throw new AssumptionViolatedException("\"calendar-availability\" not supported.", e);
        } finally {
            release(options);
        }
    }

    /**
     * Simple test that simulates the creation of an availability block
     * with a single available slot on the client and the immediate sync
     * on the server.
     */
    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void setAvailabilityFromClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String uid = randomUID();
        String summary = "test";
        String location = "testcity";
        Date start = TimeTools.D("tomorrow at 3pm");
        Date end = TimeTools.D("tomorrow at 4pm");

        // Generate and set
        String iCal = generateVAvailability(start, end, uid, summary, location);
        assertEquals(207, propPatchICal(iCal), "response code wrong");

        // Get from client and assert
        List<ICalResource> iCalResource = propFind("calendar-availability");
        assertNotNull(iCalResource, "The expected availability resource is null");
        assertEquals(1, iCalResource.size(), "Expected only one availability resource");

        ICalResource resource = iCalResource.get(0);
        List<Component> availabilities = resource.getAvailabilities();
        assertNotNull(availabilities, "The availabilities list is null");
        assertEquals(1, availabilities.size(), "Expected only one availability block");

        Component availability = availabilities.get(0);
        assertEquals(1, availability.getComponents().size(), "Expected one sub-component");
        assertEquals(uid, availability.getProperty("UID").getValue(), "The uid property does not match");
        assertEquals(summary, availability.getProperty("SUMMARY").getValue(), "The summary property does not match");
        assertEquals(location, availability.getProperty("LOCATION").getValue(), "The location property does not match");

        Component available = availability.getComponents().get(0);
        assertEquals(start, TimeTools.D(available.getProperty("DTSTART").getValue(), TimeZone.getTimeZone("Europe/Berlin")), "The start date does not match");
        assertEquals(end, TimeTools.D(available.getProperty("DTEND").getValue(), TimeZone.getTimeZone("Europe/Berlin")), "The end date does not match");
    }
}
