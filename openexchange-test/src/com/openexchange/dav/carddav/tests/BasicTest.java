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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Node;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.VisibleFoldersRequest;
import com.openexchange.ajax.folder.actions.VisibleFoldersResponse;
import com.openexchange.dav.Config;
import com.openexchange.dav.Headers;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.UserAgents;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.dav.reports.SyncCollectionResponse;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;

/**
 * {@link BasicTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.1
 */
public class BasicTest extends CardDAVTest {

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.IOS_8_4_0;
    }


    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testPutAndGet(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * prepare vCard
         */
        String collection = String.valueOf(getDefaultFolderID());
        String uid = randomUID();
        String firstName = "John";
        String lastName = "Doe";
        String email = firstName.toLowerCase() + '.' + lastName.toLowerCase() + "@example.org";
        String vCard = "BEGIN:VCARD" + "\r\n" + "PRODID:-//Example Inc.//Example Client 1.0//EN" + "\r\n" + "VERSION:3.0" + "\r\n" + "N:" + lastName + ";" + firstName + ";;;" + "\r\n" + "FN:" + firstName + " " + lastName + "\r\n" + "EMAIL;type=INTERNET;type=WORK;type=pref:" + email + "\r\n" + "UID:" + uid + "\r\n" + "REV:" + formatAsUTC(new Date()) + "\r\n" + "END:VCARD" + "\r\n";
        /*
         * create vCard resource on server
         */
        String href = Config.getPathPrefix() + "/carddav/" + collection + "/" + uid + ".vcf";
        PutMethod put = null;
        try {
            put = new PutMethod(getBaseUri() + href);
            put.addRequestHeader(Headers.IF_NONE_MATCH, "*");
            put.setRequestEntity(new StringRequestEntity(vCard, "text/vcard", "UTF-8"));
            assertEquals(StatusCodes.SC_CREATED, webDAVClient.executeMethod(put), "Response code wrong");
        } finally {
            release(put);
        }
        /*
         * get created vCard from server
         */
        VCardResource vCardResource;
        GetMethod get = null;
        try {
            get = new GetMethod(getBaseUri() + href);
            String reloadedVCard = webDAVClient.doGet(get);
            assertNotNull(reloadedVCard);
            Header eTagHeader = get.getResponseHeader("ETag");
            String eTag = null != eTagHeader ? eTagHeader.getValue() : null;
            vCardResource = new VCardResource(reloadedVCard, href, eTag);
        } finally {
            release(get);
        }
        /*
         * verify created contact
         */
        assertNotNull(vCardResource.getETag(), "No ETag");
        assertEquals(firstName, vCardResource.getGivenName(), "N wrong");
        assertEquals(lastName, vCardResource.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, vCardResource.getFN(), "FN wrong");
        /*
         * update contact on client
         */
        String updatedVCard = "BEGIN:VCARD" + "\r\n" + "PRODID:-//Example Inc.//Example Client 1.0//EN" + "\r\n" + "VERSION:3.0" + "\r\n" + "N:" + lastName + ";" + firstName + ";;;" + "\r\n" + "FN:" + firstName + " " + lastName + "\r\n" + "EMAIL;type=INTERNET;type=WORK;type=pref:" + email + "\r\n" + "UID:" + uid + "\r\n" + "REV:" + formatAsUTC(new Date()) + "\r\n" + "TEL;type=CELL:352-3534" + "\r\n" + "CATEGORIES:Family,Private" + "\r\n" + "END:VCARD" + "\r\n";
        try {
            put = new PutMethod(getBaseUri() + href);
            put.addRequestHeader(Headers.IF_MATCH, vCardResource.getETag());
            put.setRequestEntity(new StringRequestEntity(updatedVCard, "text/vcard", "UTF-8"));
            assertEquals(StatusCodes.SC_CREATED, webDAVClient.executeMethod(put), "Response code wrong");
        } finally {
            release(put);
        }
        /*
         * get updated vCard from server
         */
        try {
            get = new GetMethod(getBaseUri() + href);
            String reloadedVCard = webDAVClient.doGet(get);
            assertNotNull(reloadedVCard);
            Header eTagHeader = get.getResponseHeader("ETag");
            String eTag = null != eTagHeader ? eTagHeader.getValue() : null;
            vCardResource = new VCardResource(reloadedVCard, href, eTag);
        } finally {
            release(get);
        }
        /*
         * verify updated contact
         */
        assertNotNull(vCardResource.getETag(), "No ETag");
        assertEquals(firstName, vCardResource.getGivenName(), "N wrong");
        assertEquals(lastName, vCardResource.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, vCardResource.getFN(), "FN wrong");
        assertTrue(null != vCardResource.getVCard().getTels() && 1 == vCardResource.getVCard().getTels().size(), "No TELs found");
        assertEquals("352-3534", vCardResource.getVCard().getTels().get(0).getTelephone(), "TEL wrong");
        assertTrue(vCardResource.getVCard().getCategories().getCategories().contains("Family"), "CATEGORIES wrong");
        assertTrue(vCardResource.getVCard().getCategories().getCategories().contains("Private"), "CATEGORIES wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSyncCollection(String authMethod) throws Exception {
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
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "John";
        String lastName = "Doe";
        String email = firstName.toLowerCase() + '.' + lastName.toLowerCase() + "@example.org";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setUid(uid);
        contact.setEmail1(email);
        rememberForCleanUp(create(contact));
        /*
         * sync client
         */
        SyncCollectionResponse syncCollectionResponse = syncCollection(syncToken, "/carddav/" + collection + "/");
        Map<String, String> eTags = syncCollectionResponse.getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = addressbookMultiget(collection, eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        String href = card.getHref();
        /*
         * update contact on server
         */
        contact.setCellularTelephone1("352-3534");
        contact.setCategories("Family,Private");
        contact = update(contact);
        /*
         * sync client
         */
        syncCollectionResponse = syncCollection(syncToken, "/carddav/" + collection + "/");
        eTags = syncCollectionResponse.getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        addressData = addressbookMultiget(collection, eTags.keySet());
        card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        assertTrue(null != card.getVCard().getTels() && 1 == card.getVCard().getTels().size(), "No TELs found");
        assertEquals("352-3534", card.getVCard().getTels().get(0).getTelephone(), "TEL wrong");
        assertTrue(card.getVCard().getCategories().containsCategory("Family"), "CATEGORIES wrong");
        assertTrue(card.getVCard().getCategories().containsCategory("Private"), "CATEGORIES wrong");
        /*
         * delete contact on server
         */
        delete(contact);
        /*
         * sync client
         */
        syncCollectionResponse = syncCollection(syncToken, "/carddav/" + collection + "/");
        List<String> hrefsNotFound = syncCollectionResponse.getHrefsStatusNotFound();
        assertTrue(null != hrefsNotFound && 1 == hrefsNotFound.size(), "no resource deletions reported on sync collection");
        assertEquals(href, hrefsNotFound.get(0), "href not found");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDiscoverAddressbooks(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * retrieve expected contact collections from server
         */
        List<String> expectedCollections = new ArrayList<String>();
        VisibleFoldersRequest foldersRequest = new VisibleFoldersRequest(EnumAPI.OX_NEW, "contacts", new int[] { FolderObject.OBJECT_ID, FolderObject.FOLDER_NAME });
        VisibleFoldersResponse foldersResponse = getClient().execute(foldersRequest);
        Iterator<FolderObject> folders = foldersResponse.getPrivateFolders();
        while (folders.hasNext()) {
            expectedCollections.add("/carddav/" + folders.next().getObjectID() + '/');
        }
        folders = foldersResponse.getSharedFolders();
        while (folders.hasNext()) {
            expectedCollections.add("/carddav/" + folders.next().getObjectID() + '/');
        }
        folders = foldersResponse.getPublicFolders();
        while (folders.hasNext()) {
            expectedCollections.add("/carddav/" + folders.next().getObjectID() + '/');
        }
        /*
         * discover the current user principal
         */
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.CURRENT_USER_PRINCIPAL);
        props.add(PropertyNames.PRINCIPAL_URL);
        props.add(PropertyNames.RESOURCETYPE);
        PropFindMethod propFind = new PropFindMethod(getBaseUri() + Config.getPathPrefix() + "/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        MultiStatusResponse propFindResponse = assertSingleResponse(webDAVClient.doPropFind(propFind));
        String principalURL = extractHref(PropertyNames.CURRENT_USER_PRINCIPAL, propFindResponse);
        assertTrue(principalURL.contains("/" + getClient().getValues().getUserId()), "username not found in href child of " + PropertyNames.CURRENT_USER_PRINCIPAL);
        /*
         * discover the principal's addressbook home set
         */
        props = new DavPropertyNameSet();
        props.add(PropertyNames.ADDRESSBOOK_HOME_SET);
        props.add(PropertyNames.DISPLAYNAME);
        props.add(PropertyNames.EMAIL_ADDRESS_SET);
        props.add(PropertyNames.PRINCIPAL_COLLECTION_SET);
        props.add(PropertyNames.PRINCIPAL_URL);
        props.add(PropertyNames.RESOURCE_ID);
        props.add(PropertyNames.SUPPORTED_REPORT_SET);
        propFind = new PropFindMethod(getBaseUri() + principalURL, DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        propFindResponse = assertSingleResponse(webDAVClient.doPropFind(propFind));
        String addressbookHomeSet = extractHref(PropertyNames.ADDRESSBOOK_HOME_SET, propFindResponse);
        /*
         * do a depth 1 PROPFIND at the adressbook-home-set URL to get available collections
         */
        props = new DavPropertyNameSet();
        props.add(PropertyNames.CURRENT_USER_PRIVILEGE_SET);
        props.add(PropertyNames.DISPLAYNAME);
        props.add(PropertyNames.MAX_IMAGE_SIZE);
        props.add(PropertyNames.MAX_RESOURCE_SIZE);
        props.add(PropertyNames.OWNER);
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.SUPPORTED_REPORT_SET);
        List<String> actualCollections = new ArrayList<String>();
        propFind = new PropFindMethod(getBaseUri() + addressbookHomeSet, DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_1);
        for (MultiStatusResponse response : webDAVClient.doPropFind(propFind)) {
            DavProperty<?> property = response.getProperties(StatusCodes.SC_OK).get(PropertyNames.RESOURCETYPE);
            Object value = property.getValue();
            Collection<Node> resourceTypeNodes;
            if ((value instanceof Collection)) {
                resourceTypeNodes = (List<Node>) value;
            } else {
                resourceTypeNodes = Collections.singleton((Node) value);
            }
            for (Node resourceTypeNode : resourceTypeNodes) {
                if ("urn:ietf:params:xml:ns:carddav".equals(resourceTypeNode.getNamespaceURI()) && "addressbook".equals(resourceTypeNode.getNodeName())) {
                    actualCollections.add(response.getHref());
                }
            }
        }
        /*
         * verify that each collection was listed
         */
        for (String collection : actualCollections) {
            assertTrue(actualCollections.contains(collection), "Expected collection " + collection + " not found");
        }
        for (String collection : actualCollections) {
            assertTrue(expectedCollections.contains(collection), "Unexpected collection " + collection + " found");
        }
    }

}
