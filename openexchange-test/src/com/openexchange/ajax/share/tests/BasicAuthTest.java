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

package com.openexchange.ajax.share.tests;

import static com.openexchange.test.common.groupware.calendar.TimeTools.D;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.java.Strings;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.CalendarTestManager;

/**
 * {@link BasicAuthTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@SuppressWarnings("deprecation")
public class BasicAuthTest extends ShareTest {

    private CalendarTestManager calendarManager;

    /**
     * Initializes a new {@link BasicAuthTest}.
     *
     * @param name The test name
     */
    public BasicAuthTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        calendarManager = new CalendarTestManager(getClient());
    }

    @Test
    public void testBasicAuth() throws Exception {
        EnumAPI api = EnumAPI.OUTLOOK;
        int module = FolderObject.CALENDAR;
        OCLGuestPermission guestPermission = createNamedGuestPermission();

        // Create a calendar folder having a share permission
        FolderObject folder = insertSharedFolder(api, module, getDefaultFolder(module), guestPermission);

        // Check permissions
        OCLPermission matchingPermission = null;
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created folder found");
        checkPermissions(guestPermission, matchingPermission);

        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(api, module, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);

        // Create some appointments for that folder
        {
            int n = 1;
            {
                Appointment appointment = new Appointment();
                appointment.setStartDate(D("11.11.2014 08:00"));
                appointment.setEndDate(D("11.11.2014 09:00"));
                appointment.setTitle("Appointment #" + Integer.toString(n++));
                appointment.setParentFolderID(folder.getObjectID());
                appointment.setIgnoreConflicts(true);

                UserParticipant user = new UserParticipant(getClient().getValues().getUserId());
                user.setConfirm(Appointment.NONE);
                UserParticipant guestParticipant = new UserParticipant(guestPermission.getEntity());
                user.setConfirm(Appointment.NONE);

                appointment.setParticipants(new Participant[] { user, guestParticipant });
                appointment.setUsers(new UserParticipant[] { user, guestParticipant });

                calendarManager.insert(appointment);
            }

            {
                Appointment appointment = new Appointment();
                appointment.setStartDate(D("11.11.2014 10:00"));
                appointment.setEndDate(D("11.11.2014 11:00"));
                appointment.setTitle("Appointment #" + Integer.toString(n++));
                appointment.setParentFolderID(folder.getObjectID());
                appointment.setIgnoreConflicts(true);

                UserParticipant user = new UserParticipant(getClient().getValues().getUserId());
                user.setConfirm(Appointment.NONE);
                UserParticipant guestParticipant = new UserParticipant(guestPermission.getEntity());
                user.setConfirm(Appointment.NONE);

                appointment.setParticipants(new Participant[] { user, guestParticipant });
                appointment.setUsers(new UserParticipant[] { user, guestParticipant });

                calendarManager.insert(appointment);
            }

            {
                Appointment appointment = new Appointment();
                appointment.setStartDate(D("12.11.2014 08:00"));
                appointment.setEndDate(D("12.11.2014 09:00"));
                appointment.setTitle("Appointment #" + Integer.toString(n++));
                appointment.setParentFolderID(folder.getObjectID());
                appointment.setIgnoreConflicts(true);

                UserParticipant user = new UserParticipant(getClient().getValues().getUserId());
                user.setConfirm(Appointment.NONE);
                UserParticipant guestParticipant = new UserParticipant(guestPermission.getEntity());
                user.setConfirm(Appointment.NONE);

                appointment.setParticipants(new Participant[] { user, guestParticipant });
                appointment.setUsers(new UserParticipant[] { user, guestParticipant });

                calendarManager.insert(appointment);
            }

            {
                Appointment appointment = new Appointment();
                appointment.setStartDate(D("12.11.2014 10:00"));
                appointment.setEndDate(D("12.11.2014 11:00"));
                appointment.setTitle("Appointment #" + Integer.toString(n++));
                appointment.setParentFolderID(folder.getObjectID());
                appointment.setIgnoreConflicts(true);

                UserParticipant user = new UserParticipant(getClient().getValues().getUserId());
                user.setConfirm(Appointment.NONE);
                UserParticipant guestParticipant = new UserParticipant(guestPermission.getEntity());
                user.setConfirm(Appointment.NONE);

                appointment.setParticipants(new Participant[] { user, guestParticipant });
                appointment.setUsers(new UserParticipant[] { user, guestParticipant });

                calendarManager.insert(appointment);
            }
        }

        // Check access to share (via guest client)
        String shareURL = discoverShareURL(guestPermission.getApiClient(), guest);
        GuestClient guestClient = resolveShare(shareURL, guestPermission.getRecipient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission);

        // Build appropriate HttpClient
        HttpClientBuilder clientBuilder = HttpClientBuilder.create().setUserAgent("Microsoft Outlook");
        String password = ShareTest.getPassword(guestPermission.getRecipient());
        if (Strings.isNotEmpty(password)) {
            String username = ShareTest.getUsername(guestPermission.getRecipient());
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            UsernamePasswordCredentials anonymousCredentials = new UsernamePasswordCredentials(username, password);
            credentialsProvider.setCredentials(org.apache.http.auth.AuthScope.ANY, anonymousCredentials);
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        // Access that folder through ICal
        try (CloseableHttpClient httpClient = clientBuilder.build()) {
            HttpGet httpGet = new HttpGet(shareURL);
            httpGet.setHeader("Accept", "text/calendar");
            httpGet.setHeader("User-Agent", "Microsoft Outlook");
            httpGet.setHeader("X-Marker", "test");

            HttpResponse httpResponse = httpClient.execute(httpGet);
            assertEquals(200, httpResponse.getStatusLine().getStatusCode(), "Wrong HTTP status");

            Header contentTypeHeader = httpResponse.getFirstHeader("Content-Type");
            assertNotNull(contentTypeHeader, "missing content-type header");
            assertTrue(contentTypeHeader.getValue().startsWith("text/calendar"), "Unexpected Content-Type header: "+contentTypeHeader.getValue());

            HttpEntity entity = httpResponse.getEntity();
            assertNotNull(entity, "No file downloaded");
            byte[] downloadedFile = EntityUtils.toByteArray(entity);
            Assertions.assertNotNull(downloadedFile);

            String ical = new String(downloadedFile, "UTF-8");
            assertTrue(ical.startsWith("BEGIN:VCALENDAR"), "Received content seems not be an ICal: " + ical);
        }
    }

}


