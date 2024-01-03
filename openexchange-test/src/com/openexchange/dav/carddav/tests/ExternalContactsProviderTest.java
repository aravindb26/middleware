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
package com.openexchange.dav.carddav.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.UserAgents;
import com.openexchange.java.Strings;


/**
 * {@link ExternalContactsProviderTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.x
 */
public class ExternalContactsProviderTest extends CardDAVTest {

    /* Reference to providers in com.openexchange.contact.provider.test bundle */
    private static final String BASIC_PROVIDER_DISPLAY_NAME = "c.o.contact.prov.test.basic";
    private static final String TEST_PROVIDER_DISPLAY_NAME = "c.o.contact.provider.test";

    private static String TEST_VCARD = "BEGIN:VCARD\n"
        + "VERSION:3.0\n"
        + "N:Mustermann;Erika;;Dr.;\n"
        + "FN:Dr. Erika Mustermann\n"
        + "ORG:Wikimedia\n"
        + "ROLE:Kommunikation\n"
        + "TITLE:Redaktion & Gestaltung\n"
        + "PHOTO;VALUE=URL;TYPE=JPEG:http://commons.wikimedia.org/wiki/File:Erika_Mustermann_2010.jpg\n"
        + "TEL;TYPE=WORK,VOICE:+49 221 9999123\n"
        + "TEL;TYPE=HOME,VOICE:+49 221 1234567\n"
        + "ADR;TYPE=HOME:;;Heidestraße 17;Köln;;51147;Germany\n"
        + "EMAIL;TYPE=PREF,INTERNET:erika@mustermann.de\n"
        + "URL:http://de.wikipedia.org/\n"
        + "REV:2014-03-01T22:11:10Z\n"
        + "END:VCARD";

    /*
    private static String BASIC_VCARD = "BEGIN:VCARD\n"
        + "VERSION:3.0\n"
        + "PRODID:-//Open-Xchange//8.0.0//EN\n"
        + "UID:6d8daa63-67a9-4054-973f-c6f89014f8b0\n"
        + "FN:Max ich nix Mustermann alter!\n"
        + "N:Mustermann;Max;;;\n"
        + "EMAIL;TYPE=work:max.mustermann@example.org\n"
        + "REV:2022-09-07T09:09:22Z\n"
        + "END:VCARD";
    */

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
    }

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.THUNDERBIRD_CARDBOOK;
    }

    @Test
    public void testDiscoverCollections() throws Exception {
        List<String> collections = discoverCollections();
        assertFalse(collections.isEmpty(), "No collections found.");
    }

    @Test
    public void testCollectionsContainExternalStorages() throws Exception {
        List<String> collections = discoverCollections();
        assertTrue(collections.contains(BASIC_PROVIDER_DISPLAY_NAME), "Basic test contacts provider missing.");
        assertTrue(collections.contains(TEST_PROVIDER_DISPLAY_NAME), "Test contacts provider missing.");
    }

    @Test
    public void testSyncToken() throws Exception {
        String encodedId = discoverCollectionByDisplayName(TEST_PROVIDER_DISPLAY_NAME, true);
        String syncToken = fetchSyncToken(encodedId);
        assertFalse(Strings.isEmpty(syncToken), "No sync token for test contacts provider.");
        String uuid = UUID.randomUUID().toString();
        int status = putVCard(uuid, TEST_VCARD, encodedId);
        assertTrue(StatusCodes.SC_CREATED == status, "No contact created in test contacts provider.");
        String syncToken2 = fetchSyncToken(encodedId);
        assertFalse(syncToken.equals(syncToken2), "Sync token unchanged.");
    }

    @Test
    public void testCTag() throws Exception {
        String encodedId = discoverCollectionByDisplayName(BASIC_PROVIDER_DISPLAY_NAME, true);
        String ctag = getCTag(encodedId);
        assertFalse(Strings.isEmpty(ctag), "No CTag for basic test contacts provider.");
        /* Basic test contacts provider does not support vcard updates
        int status = putVCardUpdate("6d8daa63-67a9-4054-973f-c6f89014f8b0", BASIC_VCARD, encodedId, null);
        assertTrue("No contact created in basic test contacts provider.", StatusCodes.SC_CREATED == status);
        String ctag2 = getCTag(encodedId);
        assertFalse("CTag unchanged.", ctag.equals(ctag2));
        */
    }

}
