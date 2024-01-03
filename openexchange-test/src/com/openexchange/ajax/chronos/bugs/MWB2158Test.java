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

package com.openexchange.ajax.chronos.bugs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractICalCalendarProviderTest;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.testing.httpclient.models.FolderCalendarConfig;
import com.openexchange.testing.httpclient.models.FolderCalendarExtendedProperties;
import com.openexchange.testing.httpclient.models.FolderCalendarExtendedPropertiesDescription;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;

/**
 * {@link MWB2158Test}
 *
 * @author <a href="mailto:daniel.becker-xchange.com">Daniel Becker</a>
 */
public class MWB2158Test extends AbstractICalCalendarProviderTest {

    @Test
    public void testMWB2158() throws Exception {
        String mockediCalFeed = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" +
            "VERSION:2.0" + "\r\n" +
            "PRODID:-//Open-Xchange//7.10.6-Rev0//EN" + "\r\n" +
            "METHOD:PUBLISH" + "\r\n" +
            "X-WR-CALNAME:Kalender" + "\r\n" +
            "BEGIN:VTIMEZONE" + "\r\n" +
            "TZID:Europe/Berlin" + "\r\n" +
            "LAST-MODIFIED:20201011T015911Z" + "\r\n" +
            "TZURL:http://tzurl.org/zoneinfo-outlook/Europe/Berlin" + "\r\n" +
            "X-LIC-LOCATION:Europe/Berlin" + "\r\n" +
            "BEGIN:DAYLIGHT" + "\r\n" +
            "TZNAME:CEST" + "\r\n" +
            "TZOFFSETFROM:+0100" + "\r\n" +
            "TZOFFSETTO:+0200" + "\r\n" +
            "DTSTART:19700329T020000" + "\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" +
            "END:DAYLIGHT" + "\r\n" +
            "BEGIN:STANDARD" + "\r\n" +
            "TZNAME:CET" + "\r\n" +
            "TZOFFSETFROM:+0200" + "\r\n" +
            "TZOFFSETTO:+0100" + "\r\n" +
            "DTSTART:19701025T030000" + "\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" +
            "END:STANDARD" + "\r\n" +
            "END:VTIMEZONE" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "DTSTAMP:20210614T110907Z" + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" +
            "CREATED:20210614T110819Z" + "\r\n" +
            "DTEND;VALUE=DATE:20210615" + "\r\n" +
            "DTSTART;VALUE=DATE:20210614" + "\r\n" +
            "LAST-MODIFIED:20210614T110907Z" + "\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=6;BYMONTHDAY=14" + "\r\n" +
            "SEQUENCE:0" + "\r\n" +
            "SUMMARY:Geburtstag" + "\r\n" +
            "TRANSP:OPAQUE" + "\r\n" +
            "UID:89f9eb90-ce98-46f1-95e5-d372acc6c0bd" + "\r\n" +
            "X-MICROSOFT-CDO-ALLDAYEVENT:TRUE" + "\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY" + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "DTSTAMP:20210614T110907Z" + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" +
            "CREATED:20210614T110907Z" + "\r\n" +
            "DTEND;TZID=Europe/Berlin:20220614T080000" + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:20220614T060000" + "\r\n" +
            "LAST-MODIFIED:20210614T110907Z" + "\r\n" +
            "RECURRENCE-ID;VALUE=DATE:20220614" + "\r\n" +
            "SEQUENCE:1" + "\r\n" +
            "SUMMARY:Geburtstag" + "\r\n" +
            "TRANSP:OPAQUE" + "\r\n" +
            "UID:89f9eb90-ce98-46f1-95e5-d372acc6c0bd" + "\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY" + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR" + "\r\n"
        ; // @formatter:off
        String path = "/files/" + UUID.randomUUID().toString() + ".ics";
        String hostname = AJAXConfig.getProperty(AJAXConfig.Property.MOCK_HOSTNAME);
        String mockedFeedUri = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL) + "://" + hostname + "/" + path;
        mock(path, mockediCalFeed, HttpStatus.SC_OK);

        FolderCalendarConfig config = new FolderCalendarConfig();
        config.setUri(mockedFeedUri);
        config.setEnabled(Boolean.TRUE);
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setModule(CalendarFolderManager.EVENT_MODULE);
        folder.setComOpenexchangeCalendarConfig(config);
        folder.setSubscribed(Boolean.TRUE);
        String title = "Calendar";
        folder.setTitle(title);
        folder.setComOpenexchangeCalendarProvider(CalendarFolderManager.ICAL_ACCOUNT_PROVIDER_ID);
        FolderCalendarExtendedProperties properties = new FolderCalendarExtendedProperties();
        properties.description(new FolderCalendarExtendedPropertiesDescription().value("MWB-2158 with title Calendar with URI: " + mockedFeedUri));
        folder.setComOpenexchangeCalendarExtendedProperties(properties);
        addPermissions(folder);

        NewFolderBody body = new NewFolderBody();
        body.setFolder(folder);
        /*
         * Folder with iCA feed should be created, also it has the reserved name "Calendar" 
         */
        FolderUpdateResponse response = foldersApi.createFolder(CalendarFolderManager.DEFAULT_FOLDER_ID, body, CalendarFolderManager.TREE_ID, CalendarFolderManager.MODULE, null, Boolean.TRUE);
        String account = checkResponse(response.getError(), response.getErrorDesc(), response.getCategories(), response).getData();
        assertTrue(null != account);
        assertFalse(account.startsWith("cal://0"));
        FolderResponse folderResponse = foldersApi.getFolder(account, CalendarFolderManager.DEFAULT_FOLDER_ID, null, null, null);
        FolderData folderData = checkResponse(folderResponse.getError(), folderResponse.getErrorDesc(), folderResponse.getCategories(), folderResponse).getData();
        assertTrue(title.equalsIgnoreCase(folderData.getTitle()));
    }

}
