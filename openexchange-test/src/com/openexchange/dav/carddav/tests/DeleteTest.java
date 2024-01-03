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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.dav.reports.SyncCollectionResponse;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link DeleteTest} - Tests various delete operations via the CardDAV interface
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DeleteTest extends CardDAVTest {

    public DeleteTest() {
        super();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDeleteContactOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "banane";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource contactCard = assertContains(uid, addressData);
        assertEquals(firstName, contactCard.getGivenName(), "N wrong");
        assertEquals(lastName, contactCard.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, contactCard.getFN(), "FN wrong");
        /*
         * delete contact on server
         */
        super.delete(contact);
        /*
         * verify deletion on client
         */
        SyncCollectionResponse syncCollectionResponse = super.syncCollection(syncToken);
        assertTrue(0 < syncCollectionResponse.getHrefsStatusNotFound().size(), "no resource deletions reported on sync collection");
        eTags = syncCollectionResponse.getETagsStatusOK();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDeleteContactInSubfolderOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create folder on server
         */
        String folderName = "testfolder_" + randomUID();
        FolderObject folder = super.createFolder(folderName);
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "otto";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact, folder.getObjectID()));
        /*
         * verify contact and folder group on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource contactCard = assertContains(uid, addressData);
        assertEquals(firstName, contactCard.getGivenName(), "N wrong");
        assertEquals(lastName, contactCard.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, contactCard.getFN(), "FN wrong");
        /*
         * delete contact on server
         */
        super.delete(contact);
        /*
         * verify deletion on client
         */
        SyncCollectionResponse syncCollectionResponse = super.syncCollection(syncToken);
        assertTrue(0 < syncCollectionResponse.getHrefsStatusNotFound().size(), "no resource deletions reported on sync collection");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDeleteContactOnClient(String authMethod) throws Throwable {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "manfred";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact));
        /*
         * verify contact and folder group on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        /*
         * delete contact on client
         */
        assertEquals(StatusCodes.SC_NO_CONTENT, delete(uid), "response code wrong");
        /*
         * verify deletion on server
         */
        assertNull(super.getContact(uid), "contact not deleted on server");
        /*
         * verify deletion on client
         */
        SyncCollectionResponse syncCollectionResponse = super.syncCollection(syncToken);
        assertTrue(0 < syncCollectionResponse.getHrefsStatusNotFound().size(), "no resource deletions reported on sync collection");
        eTags = syncCollectionResponse.getETagsStatusOK();
    }

}
