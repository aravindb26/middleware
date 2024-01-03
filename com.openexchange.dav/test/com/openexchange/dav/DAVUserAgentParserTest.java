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

package com.openexchange.dav;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import com.openexchange.dav.useragent.DAVUserAgentParserImpl;

/**
 * {@link DAVUserAgentParserTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class DAVUserAgentParserTest {

    @Test
    public void testUserAgents() {
        DAVUserAgentParserImpl parser = new DAVUserAgentParserImpl(null, null); // Use with defaults

        assertThat("Wrong UserAgent", parser.parse("CalendarStore/5.0.2 (1166); iCal/5.0.2 (1571); Mac OS X/10.7.3 (11D50d)"), is(DAVUserAgent.MAC_CALENDAR));
        assertThat("Wrong UserAgent", parser.parse("Mac OS X/10.7.3 (11D50d); CalendarStore/5.0.2 (1166); iCal/5.0.2 (1571)"), is(DAVUserAgent.MAC_CALENDAR));
        assertThat("Wrong UserAgent", parser.parse("macOS/13.1 (22C65) dataaccessd/1.0"), is(DAVUserAgent.MAC_CALENDAR));
        assertThat("Wrong UserAgent", parser.parse("macOS/11.1 (20C69) AddressBookCore/2452.2"), is(DAVUserAgent.MAC_CONTACTS));
        assertThat("Wrong UserAgent", parser.parse("iOS/9.1 (13B143) dataaccessd/1.0"), is(DAVUserAgent.IOS));
        assertThat("Wrong UserAgent", parser.parse("Android iOS/9.1 (13B143) dataaccessd/1.0 Android"), is(DAVUserAgent.UNKNOWN));
        assertThat("Wrong UserAgent", parser.parse("iOS/15.0.1 (19A348) dataaccessd/1.0"), is(DAVUserAgent.IOS));
        assertThat("Wrong UserAgent", parser.parse("iOS/15.0.1 remindd/1.0"), is(DAVUserAgent.IOS_REMINDERS));
        assertThat("Wrong UserAgent", parser.parse("iOS/15.0.1 Android remindd/1.0"), is(DAVUserAgent.IOS_REMINDERS));
        assertThat("Wrong UserAgent", parser.parse("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:15.0) Gecko/20120907 Thunderbird/15.0.1 Lightning/1.7"), is(DAVUserAgent.THUNDERBIRD_LIGHTNING));
        assertThat("Wrong UserAgent", parser.parse("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:45.0) Gecko/20100101 Thunderbird/45.7.0 Lightning/4.7.7"), is(DAVUserAgent.THUNDERBIRD_LIGHTNING));
        assertThat("Wrong UserAgent", parser.parse("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:102.0) Gecko/20100101 Thunderbird/102.6.1"), is(DAVUserAgent.THUNDERBIRD_LIGHTNING));
        assertThat("Wrong UserAgent", parser.parse("Thunderbird CardBook/102.6.1"), is(DAVUserAgent.THUNDERBIRD_CARDBOOK));
        assertThat("Wrong UserAgent", parser.parse("eM Client/6.0.24144.0"), is(DAVUserAgent.EM_CLIENT));
        assertThat("Wrong UserAgent", parser.parse("eMClient/8.1.172.0"), is(DAVUserAgent.EM_CLIENT));
        assertThat("Wrong UserAgent", parser.parse("SomeInformation eMClient/8.1.172.0"), is(DAVUserAgent.EM_CLIENT));
        assertThat("Wrong UserAgent", parser.parse("com.openexchange.mobile.syncapp.enterprise"), is(DAVUserAgent.OX_SYNC));
        assertThat("Wrong UserAgent", parser.parse("org.dmfs.caldav.sync"), is(DAVUserAgent.CALDAV_SYNC));
        assertThat("Wrong UserAgent", parser.parse("org.dmfs.carddav.sync"), is(DAVUserAgent.CARDDAV_SYNC));
        assertThat("Wrong UserAgent", parser.parse("SmoothSync"), is(DAVUserAgent.SMOOTH_SYNC));
        assertThat("Wrong UserAgent", parser.parse("DAVdroid"), is(DAVUserAgent.DAVDROID));
        assertThat("Wrong UserAgent", parser.parse("DAVx5"), is(DAVUserAgent.DAVX5));
        assertThat("Wrong UserAgent", parser.parse("CalDavSynchronizer/3.6"), is(DAVUserAgent.OUTLOOK_CALDAV_SYNCHRONIZER));
        assertThat("Wrong UserAgent", parser.parse("MSFT-WP"), is(DAVUserAgent.WINDOWS_PHONE));
        assertThat("Wrong UserAgent", parser.parse("MSFT-WIN"), is(DAVUserAgent.WINDOWS));
    }

}
