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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MoveTest} - Tests various move operations via the CardDAV interface
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MoveTest extends CardDAVTest {

    public MoveTest() {
        super();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMoveContactToSubfolderOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create subfolder on server
         */
        String subFolderName = "testfolder_" + randomUID();
        FolderObject subFolder = super.createFolder(subFolderName);
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "jaqueline";
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
         * move contact on server
         */
        contact.setParentFolderID(subFolder.getObjectID());
        super.update(super.getDefaultFolder().getObjectID(), contact);
        /*
         * verify contact on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        addressData = super.addressbookMultiget(eTags.keySet());
        contactCard = assertContains(uid, addressData);
        assertEquals(firstName, contactCard.getGivenName(), "N wrong");
        assertEquals(lastName, contactCard.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, contactCard.getFN(), "FN wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMoveContactToDefaultFolderOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create subfolder on server
         */
        String subFolderName = "testfolder_" + randomUID();
        FolderObject subFolder = super.createFolder(subFolderName);
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "jaqueline";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact, subFolder.getObjectID()));
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
         * move contact on server
         */
        contact.setParentFolderID(super.getDefaultFolder().getObjectID());
        super.update(subFolder.getObjectID(), contact);
        contact.setParentFolderID(subFolder.getObjectID());
        /*
         * verify contact on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        addressData = super.addressbookMultiget(eTags.keySet());
        contactCard = assertContains(uid, addressData);
        assertEquals(firstName, contactCard.getGivenName(), "N wrong");
        assertEquals(lastName, contactCard.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, contactCard.getFN(), "FN wrong");
    }

}
