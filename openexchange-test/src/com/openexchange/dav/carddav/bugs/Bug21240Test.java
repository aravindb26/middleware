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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.UserAgents;
import com.openexchange.groupware.container.Contact;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug21240Test}
 *
 * Contact can't be deleted with Addressbook client on Mac OS 10.6.8
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug21240Test extends CardDAVTest {

    public Bug21240Test() {
        super();
    }

    @BeforeEach
    public void setUserAgent() throws Exception {
        super.webDAVClient.setUserAgent(UserAgents.MACOS_10_6_8);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDeleteContact(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create contact
         */
        final String uid = randomUID() + "-ABSPlugin";
        final String pathUid = randomUID() + "-ABSPlugin";
        final String firstName = "test";
        final String lastName = "hannes";
        final String vCard = "BEGIN:VCARD" + "\r\n" + "VERSION:3.0" + "\r\n" + "N:" + lastName + ";" + firstName + ";;;" + "\r\n" + "FN:" + firstName + " " + lastName + "\r\n" + "CATEGORIES:Kontakte" + "\r\n" + "X-ABUID:A33920F3-656F-47B7-A335-2C603DA3F324\\:ABPerson" + "\r\n" + "UID:" + uid + "\r\n" + "REV:" + super.formatAsUTC(new Date()) + "\r\n" + "END:VCARD" + "\r\n";
        assertEquals(StatusCodes.SC_CREATED, super.putVCard(pathUid, vCard), "response code wrong");
        /*
         * verify contact on server
         */
        final Contact contact = super.getContact(uid);
        super.rememberForCleanUp(contact);
        assertEquals(uid, contact.getUid(), "uid wrong");
        assertEquals(firstName, contact.getGivenName(), "firstname wrong");
        assertEquals(lastName, contact.getSurName(), "lastname wrong");
        /*
         * delete contact
         */
        assertEquals(StatusCodes.SC_NO_CONTENT, super.delete(pathUid), "response code wrong");
        /*
         * verify deletion on server
         */
        assertNull(super.getContact(uid), "contact not deleted");
        /*
         * verify deletion on client
         */
        Map<String, String> allETags = super.getAllETags();
        for (String href : allETags.values()) {
            assertFalse(href.contains(pathUid), "resource still present");
        }
    }
}
