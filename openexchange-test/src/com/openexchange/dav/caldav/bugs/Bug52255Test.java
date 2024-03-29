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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.test.common.test.PermissionTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug52255Test}
 *
 * "Private" appointment details readable via CalDAV "GET" request
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.4
 */
public class Bug52255Test extends Abstract2UserCalDAVTest {

    private CalendarTestManager manager2;
    private FolderObject sharedFolder;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        /*
         * as user b, create subfolder shared to user a
         */
        manager2 = new CalendarTestManager(client2);
        manager2.setFailOnError(true);
        sharedFolder = new FolderObject();
        sharedFolder.setModule(FolderObject.CALENDAR);
        sharedFolder.setParentFolderID(manager2.getPrivateFolder());
        sharedFolder.setPermissions(
            PermissionTools.P(Integer.valueOf(client2.getValues().getUserId()),
            PermissionTools.ADMIN, Integer.valueOf(getClient().getValues().getUserId()), "vr")
        );
        sharedFolder.setFolderName(randomUID());
        ftm.setClient(client2);
        sharedFolder = ftm.insertFolderOnServer(sharedFolder);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testGetPrivateInSharedFolder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("tomorrow at noon", TimeZone.getTimeZone("Europe/Berlin")));
        Date startTime = calendar.getTime();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date endTime = calendar.getTime();
        /*
         * create private appointment in shared folder for user b on server
         */
        String uid = randomUID();
        Appointment appointment = new Appointment();
        appointment.setUid(uid);
        appointment.setTitle("Geheim");
        appointment.setIgnoreConflicts(true);
        appointment.setStartDate(startTime);
        appointment.setEndDate(endTime);
        appointment.setParentFolderID(sharedFolder.getObjectID());
        appointment.setPrivateFlag(true);
        manager2.insert(appointment);
        /*
         * verify appointment on client as user a
         */
        ICalResource iCalResource = get(String.valueOf(sharedFolder.getObjectID()), uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        String classification = iCalResource.getVEvent().getPropertyValue("CLASS");
        assertTrue("PRIVATE".equals(classification) || "CONFIDENTIAL".equals(classification), "CLASS wrong");
        assertNotEquals(appointment.getTitle(), iCalResource.getVEvent().getSummary(), "SUMMARY is readable");
    }

}
