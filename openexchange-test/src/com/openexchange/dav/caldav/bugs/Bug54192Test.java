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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.oauth.provider.protocol.Grant;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.WebDAVClient;
import com.openexchange.dav.WebDAVTest;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.test.common.test.pool.TestUser;

/**
 * {@link Bug54192Test}
 *
 * Sync Conflict with emclient when dismissing appointment notification
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug54192Test extends CalDAVTest {

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.EM_CLIENT_6_0;
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateConcurrently(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create appointment on server
         */
        String uid = randomUID();
        Date start = TimeTools.D("next sunday at 12:30");
        Date end = TimeTools.D("next sunday at 14:30");
        Appointment appointment = create(generateAppointment(start, end, uid, "test", "location"));
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(appointment.getTitle(), iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        /*
         * update appointment on client concurrently & verify response codes
         */
        iCalResource.getVEvent().setSummary("test_edit");
        int[] responses = updateEventConcurrently(iCalResource, 10);
        for (int response : responses) {
            assertTrue(HttpServletResponse.SC_CREATED == response || HttpServletResponse.SC_PRECONDITION_FAILED == response, "Unexpected response: HTTP " + response
            );
        }
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertEquals("test_edit", appointment.getTitle(), "Title wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals("test_edit", iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
    }

    private int[] updateEventConcurrently(final ICalResource iCalResource, int numThreads) throws Exception {
        Thread[] threads = new Thread[numThreads];
        final int[] responses = new int[threads.length];
        TestUser user = this.testUser;
        Grant grant = oAuthGrant;
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        WebDAVClient localDavClient = new WebDAVClient(user, getDefaultUserAgent(), grant);
                        responses[index] = putICalUpdate(localDavClient, iCalResource);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                }
            });
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        return responses;
    }

}
