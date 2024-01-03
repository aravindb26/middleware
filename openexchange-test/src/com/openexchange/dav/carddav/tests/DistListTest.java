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

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import com.google.api.client.util.Objects;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.UserAgents;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.dav.reports.SyncCollectionResponse;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link DistListTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.4
 */
public class DistListTest extends CardDAVTest {

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.EM_CLIENT_FOR_APP_SUITE;
    }

    //@Test - not yet supported, as long as UID references are only resolved within the current collection
    public void noTestContactReferencesFromMultipleFolders() throws Exception {
        /*
         * fetch initial sync token
         */
        String collection = String.valueOf(getDefaultFolderID());
        SyncToken syncToken = new SyncToken(fetchSyncToken(collection));
        /*
         * create contact on server in default folder
         */
        String uid1 = randomUID();
        String firstName = "John";
        String lastName = "Doe";
        String email = firstName.toLowerCase() + '.' + lastName.toLowerCase() + "@example.org";
        Contact contact1 = new Contact();
        contact1.setSurName(lastName);
        contact1.setGivenName(firstName);
        contact1.setDisplayName(firstName + " " + lastName);
        contact1.setUid(uid1);
        contact1.setEmail1(email);
        rememberForCleanUp(create(contact1));
        /*
         * lookup "own" user contact in global addressbook
         */
        Contact userContact = com.openexchange.ajax.user.UserTools.getUserContact(getClient(), getClient().getValues().getUserId());
        /*
         * sync client
         */
        SyncCollectionResponse syncCollectionResponse = syncCollection(syncToken, "/carddav/" + collection + "/");
        Map<String, String> eTags = syncCollectionResponse.getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = addressbookMultiget(collection, eTags.keySet());
        assertContains(uid1, addressData);
        /*
         * prepare list vCard referencing both contacts on client
         */
        String uid = randomUID();
        String vCard = // @formatter:off
            "BEGIN:VCARD" + "\r\n" +
            "VERSION:3.0" + "\r\n" +
            "N:list;;;;" + "\r\n" +
            "FN:list" + "\r\n" +
            "UID:" + uid + "\r\n" +
            "X-ADDRESSBOOKSERVER-KIND:GROUP" + "\r\n" +
            "X-ADDRESSBOOKSERVER-MEMBER;X-EMAIL=\"" + contact1.getEmail1() + "\";X-CN=\"" + contact1.getDisplayName() + "\":urn:uuid:" + contact1.getUid() + "\r\n" +
            "X-ADDRESSBOOKSERVER-MEMBER;X-EMAIL=\"" + userContact.getEmail1() + "\";X-CN=\"" + userContact.getDisplayName() + "\":urn:uuid:" + userContact.getUid() + "\r\n" +
            "END:VCARD" + "\r\n"
        ; // @formatter:on
        /*
         * create vCard resource on server
         */
        assertEquals(StatusCodes.SC_CREATED, putVCard(uid, vCard, collection), "response code wrong");
        /*
         * get & verify created contact on server
         */
        Contact createdContact = getContact(uid);
        rememberForCleanUp(createdContact);
        assertEquals(uid, createdContact.getUid(), "uid wrong");
        assertTrue(createdContact.getMarkAsDistribtuionlist(), "no distribution list");
        assertNotNull(createdContact.getDistributionList(), "no distribution list");
        assertEquals(2, createdContact.getDistributionList().length, "unexpected number of members");
        DistributionListEntryObject entry1 = null;
        DistributionListEntryObject entryUser = null;
        for (DistributionListEntryObject entry : createdContact.getDistributionList()) {
            if (Objects.equal(entry.getEmailaddress(), contact1.getEmail1())) {
                entry1 = entry;
            }
            if (Objects.equal(entry.getEmailaddress(), userContact.getEmail1())) {
                entryUser = entry;
            }
        }
        assertNotNull(entry1, "contact not found in list");
        assertEquals(I(contact1.getObjectID()), entry1.getEntryID(), "entry id wrong");
        assertNotNull(userContact, "user contact not found in list");
        assertNotNull(entryUser);
        assertEquals(I(userContact.getObjectID()), entryUser.getEntryID(), "entry id wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testContactReferencesFromMultipleDuringUpdate(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch initial sync token
         */
        String collection = String.valueOf(getDefaultFolderID());
        SyncToken syncToken = new SyncToken(fetchSyncToken(collection));
        /*
         * create contact on server in default folder
         */
        String uid1 = randomUID();
        String firstName = "John";
        String lastName = "Doe";
        String email = firstName.toLowerCase() + '.' + lastName.toLowerCase() + "@example.org";
        Contact contact1 = new Contact();
        contact1.setSurName(lastName);
        contact1.setGivenName(firstName);
        contact1.setDisplayName(firstName + " " + lastName);
        contact1.setUid(uid1);
        contact1.setEmail1(email);
        rememberForCleanUp(create(contact1));
        /*
         * lookup "own" user contact in global addressbook
         */
        Contact userContact = com.openexchange.ajax.user.UserTools.getUserContact(getClient(), getClient().getValues().getUserId());
        /*
         * create distribution list on server containing of these contacts
         */
        Contact distList = new Contact();
        distList.setUid(randomUID());
        distList.setDisplayName("listnameeditme");
        distList.setSurName("listnameeditme");
        DistributionListEntryObject[] dleo = new DistributionListEntryObject[2];
        dleo[0] = new DistributionListEntryObject(contact1.getDisplayName(), contact1.getEmail1(), DistributionListEntryObject.EMAILFIELD1);
        dleo[0].setEntryID(contact1.getId(true));
        dleo[0].setFolderID(contact1.getFolderId(true));
        dleo[1] = new DistributionListEntryObject(userContact.getDisplayName(), userContact.getEmail1(), DistributionListEntryObject.EMAILFIELD1);
        dleo[1].setEntryID(userContact.getId(true));
        dleo[1].setFolderID(userContact.getFolderId(true));
        distList.setDistributionList(dleo);
        rememberForCleanUp(create(distList));
        /*
         * sync client
         */
        SyncCollectionResponse syncCollectionResponse = syncCollection(syncToken, "/carddav/" + collection + "/");
        Map<String, String> eTags = syncCollectionResponse.getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = addressbookMultiget(collection, eTags.keySet());
        assertContains(uid1, addressData);
        VCardResource distlistResource = assertContains(distList.getUid(), addressData);
        /*
         * update name of dist list on client
         */
        String updatedVCard = distlistResource.getVCardString().replaceAll("listnameeditme", "listnameedited");
        assertEquals(StatusCodes.SC_CREATED, putVCardUpdate(distlistResource.getUID(), updatedVCard, collection, distlistResource.getETag()), "response code wrong");
        /*
         * verify updated list on server
         */
        Contact updatedDistlist = getContact(distList.getUid());
        rememberForCleanUp(updatedDistlist);
        assertTrue(updatedDistlist.getMarkAsDistribtuionlist(), "no distribution list");
        assertNotNull(updatedDistlist.getDistributionList(), "no distribution list");
        assertEquals(2, updatedDistlist.getDistributionList().length, "unexpected number of members");
        DistributionListEntryObject entry1 = null;
        DistributionListEntryObject entryUser = null;
        for (DistributionListEntryObject entry : updatedDistlist.getDistributionList()) {
            if (Objects.equal(entry.getEmailaddress(), contact1.getEmail1())) {
                entry1 = entry;
            }
            if (Objects.equal(entry.getEmailaddress(), userContact.getEmail1())) {
                entryUser = entry;
            }
        }
        assertNotNull(entry1, "contact not found in list");
        assertEquals(contact1.getId(true), entry1.getEntryID(), "entry id wrong");
        assertNotNull(userContact, "user contact not found in list");
        assertNotNull(entryUser);
        assertEquals(userContact.getId(true), entryUser.getEntryID(), "entry id wrong");
    }

}
