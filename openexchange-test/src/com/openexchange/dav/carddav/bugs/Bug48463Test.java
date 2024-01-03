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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.groupware.container.Contact;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug48463Test}
 *
 * carddav: multiple titles will be displayed with comma in iOS
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.3
 */
public class Bug48463Test extends CardDAVTest {

    /**
     * Initializes a new {@link Bug48463Test}.
     */
    public Bug48463Test() {
        super();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMultiplePositions(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create contact with multiple positions on server
         */
        String uid = randomUID();
        Contact contact = new Contact();
        contact.setGivenName("Otto");
        contact.setSurName("Tester");
        contact.setPosition("Senior Director Sales Italy and Switzerland");
        contact.setUid(uid);
        rememberForCleanUp(create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        VCardResource card = assertContains(uid, addressbookMultiget(eTags.keySet()));
        assertEquals(contact.getPosition(), card.getVCard().getTitle().getTitle(), "POSITION wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMultipleTitles(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create contact with multiple positions on server
         */
        String uid = randomUID();
        Contact contact = new Contact();
        contact.setGivenName("Otto");
        contact.setSurName("Tester");
        contact.setTitle("Prof. Dr. h. c.");
        contact.setUid(uid);
        rememberForCleanUp(create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        VCardResource card = assertContains(uid, addressbookMultiget(eTags.keySet()));
        List<String> values = card.getVCard().getN().getHonorificPrefixes();
        assertTrue(null != values && 1 == values.size(), "Title wrong");
        assertEquals(contact.getTitle(), values.get(0), "Title wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMultipleMiddleNames(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create contact with multiple positions on server
         */
        String uid = randomUID();
        Contact contact = new Contact();
        contact.setGivenName("Hadschi");
        contact.setSurName("al Gossarah");
        contact.setMiddleName("Halef Omar Ben Hadschi Abul Abbas Ibn Hadschi Dawuhd");
        contact.setUid(uid);
        rememberForCleanUp(create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        VCardResource card = assertContains(uid, addressbookMultiget(eTags.keySet()));
        List<String> values = card.getVCard().getN().getAdditionalNames();
        assertTrue(null != values && 1 == values.size(), "Middle name wrong");
        assertEquals(contact.getMiddleName(), values.get(0), "Middle name wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMultipleSuffixes(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create contact with multiple positions on server
         */
        String uid = randomUID();
        Contact contact = new Contact();
        contact.setGivenName("Otto");
        contact.setSurName("Tester");
        contact.setSuffix("the 2nd");
        contact.setUid(uid);
        rememberForCleanUp(create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        VCardResource card = assertContains(uid, addressbookMultiget(eTags.keySet()));
        List<String> values = card.getVCard().getN().getHonorificSuffixes();
        assertTrue(null != values && 1 == values.size(), "Suffix wrong");
        assertEquals(contact.getSuffix(), values.get(0), "Suffix wrong");
    }

}
