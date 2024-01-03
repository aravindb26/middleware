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

package com.openexchange.dav.carddav.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.UserAgents;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MWB661Test}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.5
 */
public class MWB661Test extends CardDAVTest {

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.CALDAV_SYNCHRONIZER;
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUidOnlyReferences(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String collection = String.valueOf(getDefaultFolderID());
        /*
         * prepare contact vCard
         */
        String contactUid = randomUID();
        String contactVCard = // @formatter:off
            "BEGIN:VCARD" + "\r\n" +
            "VERSION:3.0" + "\r\n" +
            "FN:Test" + "\r\n" +
            "EMAIL:test@example.com" + "\r\n" +
            "PRODID:-//Microsoft Corporation//Outlook 16.0 MIMEDIR//EN" + "\r\n" +
            "REV:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + contactUid + "\r\n" +
            "END:VCARD" + "\r\n"
        ; // @formatter:on
        /*
         * create contact vCard resource on server
         */
        assertEquals(StatusCodes.SC_CREATED, putVCard(contactUid, contactVCard, collection), "response code wrong");
        /*
         * prepare distribution list vCard referencing this contact
         */
        String listUid = randomUID();
        String listVCard = // @formatter:off
            "BEGIN:VCARD" + "\r\n" +
            "VERSION:3.0" + "\r\n" +
            "FN:List" + "\r\n" +
            "X-ADDRESSBOOKSERVER-KIND:group" + "\r\n" +
            "X-ADDRESSBOOKSERVER-MEMBER:urn:uuid:" + contactUid + "\r\n" +
            "PRODID:-//Microsoft Corporation//Outlook 16.0 MIMEDIR//EN" + "\r\n" +
            "REV:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + listUid + "\r\n" +
            "END:VCARD" + "\r\n"
        ; // @formatter:on
        /*
         * create list vCard resource on server
         */
        assertEquals(StatusCodes.SC_CREATED, putVCard(listUid, listVCard, collection), "response code wrong");
        /*
         * get & verify created contact & list on server
         */
        Contact contact = getContact(contactUid, getDefaultFolderID());
        assertNotNull(contact);
        rememberForCleanUp(contact);
        Contact distList = getContact(listUid, getDefaultFolderID());
        assertNotNull(distList);
        rememberForCleanUp(distList);
        assertTrue(distList.getMarkAsDistribtuionlist(), "no distribution list");
        assertNotNull(distList.getDistributionList(), "no distribution list");
        assertEquals(1, distList.getDistributionList().length, "unexpected number of members");
        DistributionListEntryObject entry = distList.getDistributionList()[0];
        assertEquals(contact.getId(true), entry.getEntryID(), "entry id wrong");
    }

}
