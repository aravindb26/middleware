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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.attach.actions.AttachRequest;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug55068Test}
 *
 * accepting an appointment using a mobile (android) phone deletes the assigned resource
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.4
 */
public class Bug55068Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testManagedAttachmentDisposition(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create appointment on server & attach a file attachment
         */
        String uid = randomUID();
        Appointment appointment = new Appointment();
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setUid(uid);
        appointment.setTitle("Bug55068Test");
        appointment.setIgnoreConflicts(true);
        appointment.setStartDate(TimeTools.D("next friday at 2 pm", TimeZone.getTimeZone("Europe/Berlin")));
        appointment.setEndDate(TimeTools.D("next friday at 3 pm", TimeZone.getTimeZone("Europe/Berlin")));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        appointment = catm.insert(appointment);
        getClient().execute(new AttachRequest(appointment, "test.txt", new ByteArrayInputStream(randomUID().getBytes()), "text/plain")).getId();
        /*
         * verify appointment on client as user a
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        Property attachProperty = iCalResource.getVEvent().getProperty("ATTACH");
        assertNotNull(attachProperty);
        /*
         * check plain GET response on attachment resource
         */
        GetMethod get = null;
        try {
            get = new GetMethod(attachProperty.getValue());
            Assertions.assertEquals(StatusCodes.SC_OK, webDAVClient.executeMethod(get), "response code wrong");
            Header responseHeader = get.getResponseHeader("Content-Disposition");
            assertNotNull(responseHeader);
            assertTrue(responseHeader.getValue().toLowerCase().startsWith("attachment"));
        } finally {
            release(get);
        }
    }

}
