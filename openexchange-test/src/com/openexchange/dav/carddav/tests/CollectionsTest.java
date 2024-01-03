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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Node;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.UserAgents;

/**
 * {@link CollectionsTest}
 *
 * Tests discovery of addressbook collections below the root collection.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class CollectionsTest extends CardDAVTest {

    public CollectionsTest() {
        super();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMacOSClients(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String aggregatedCollectionName = getDefaultCollectionName();
        for (String userAgent : UserAgents.MACOS_ALL) {
            super.webDAVClient.setUserAgent(userAgent);
            discoverRoot();
            discoverAggregatedCollection(aggregatedCollectionName, true);
            discoverContactsCollection(false);
            discoverGABCollection(false);
        }
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testIOSClients(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String aggregatedCollectionName = getDefaultCollectionName();
        for (String userAgent : UserAgents.IOS_ALL) {
            super.webDAVClient.setUserAgent(userAgent);
            discoverRoot();
            discoverAggregatedCollection(aggregatedCollectionName, false);
            discoverContactsCollection(true);
            discoverGABCollection(true);
        }
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testOtherClients(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        String aggregatedCollectionName = getDefaultCollectionName();
        for (String userAgent : UserAgents.OTHER_ALL) {
            super.webDAVClient.setUserAgent(userAgent);
            discoverRoot();
            discoverAggregatedCollection(aggregatedCollectionName, false);
            discoverContactsCollection(true);
            discoverGABCollection(true);
        }
    }

    private void discoverRoot() throws Exception {

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.CURRENT_USER_PRINCIPAL);
        props.add(PropertyNames.PRINCIPAL_URL);
        props.add(PropertyNames.RESOURCETYPE);
        PropFindMethod propFind = new PropFindMethod(super.webDAVClient.getBaseURI() + Config.getPathPrefix() + "/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        MultiStatusResponse response = assertSingleResponse(super.webDAVClient.doPropFind(propFind));
        Node node = super.extractNodeValue(PropertyNames.RESOURCETYPE, response);
        assertMatches(PropertyNames.COLLECTION, node);
    }

    private void discoverAggregatedCollection(String aggregatedCollectionName, boolean shouldExists) throws Exception {
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.ADD_MEMBER);
        props.add(PropertyNames.CURRENT_USER_PRIVILEGE_SET);
        props.add(PropertyNames.DISPLAYNAME);
        props.add(PropertyNames.MAX_IMAGE_SIZE);
        props.add(PropertyNames.MAX_RESOURCE_SIZE);
        props.add(PropertyNames.ME_CARD);
        props.add(PropertyNames.OWNER);
        props.add(PropertyNames.PUSH_TRANSPORTS);
        props.add(PropertyNames.PUSHKEY);
        props.add(PropertyNames.QUOTA_AVAILABLE_BYTES);
        props.add(PropertyNames.QUOTA_USED_BYTES);
        props.add(PropertyNames.RESOURCE_ID);
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.SUPPORTED_REPORT_SET);
        props.add(PropertyNames.SYNC_TOKEN);
        PropFindMethod propFind = new PropFindMethod(super.webDAVClient.getBaseURI() + Config.getPathPrefix() + "/carddav/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_1);
        MultiStatusResponse aggregatedCollectionResponse = null;
        for (MultiStatusResponse response : super.webDAVClient.doPropFind(propFind)) {
            if (response.getHref().equals(buildCollectionHref(aggregatedCollectionName))) {
                aggregatedCollectionResponse = response;
                break;
            }
        }
        if (shouldExists) {
            assertNotNull(aggregatedCollectionResponse, "Aggregated collection not found at /carddav/Contacts");
            List<Node> nodeList = super.extractNodeListValue(PropertyNames.RESOURCETYPE, aggregatedCollectionResponse);
            assertContains(PropertyNames.COLLECTION, nodeList);
            assertContains(PropertyNames.ADDRESSBOOK, nodeList);
        } else {
            assertNull(aggregatedCollectionResponse, "Aggregated collection found at /carddav/Contacts");
        }
    }

    private void discoverContactsCollection(boolean shouldExist) throws Exception {
        String folderName = super.getDefaultFolder().getFolderName();
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.ADD_MEMBER);
        props.add(PropertyNames.CURRENT_USER_PRIVILEGE_SET);
        props.add(PropertyNames.DISPLAYNAME);
        props.add(PropertyNames.MAX_IMAGE_SIZE);
        props.add(PropertyNames.MAX_RESOURCE_SIZE);
        props.add(PropertyNames.ME_CARD);
        props.add(PropertyNames.OWNER);
        props.add(PropertyNames.PUSH_TRANSPORTS);
        props.add(PropertyNames.PUSHKEY);
        props.add(PropertyNames.QUOTA_AVAILABLE_BYTES);
        props.add(PropertyNames.QUOTA_USED_BYTES);
        props.add(PropertyNames.RESOURCE_ID);
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.SUPPORTED_REPORT_SET);
        props.add(PropertyNames.SYNC_TOKEN);
        PropFindMethod propFind = new PropFindMethod(super.webDAVClient.getBaseURI() + Config.getPathPrefix() + "/carddav/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_1);
        boolean found = false;
        for (MultiStatusResponse response : super.webDAVClient.doPropFind(propFind)) {
            String displayName = super.extractTextContent(DavPropertyName.DISPLAYNAME, response);
            if (null != displayName && 0 < displayName.length() && "\u200A".equals(displayName.substring(0, 1))) {
                displayName = displayName.substring(1);
            }
            if (folderName.equals(displayName)) {
                found = true;
                break;
            }
        }
        if (shouldExist) {
            assertTrue(found, "Default contact folder collection not found below /carddav/");
        } else {
            assertFalse(found, "Default contact folder collection found below /carddav/");
        }
    }

    private void discoverGABCollection(boolean shouldExist) throws Exception {
        String folderName = super.getGABFolder().getFolderName();
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.ADD_MEMBER);
        props.add(PropertyNames.CURRENT_USER_PRIVILEGE_SET);
        props.add(PropertyNames.DISPLAYNAME);
        props.add(PropertyNames.MAX_IMAGE_SIZE);
        props.add(PropertyNames.MAX_RESOURCE_SIZE);
        props.add(PropertyNames.ME_CARD);
        props.add(PropertyNames.OWNER);
        props.add(PropertyNames.PUSH_TRANSPORTS);
        props.add(PropertyNames.PUSHKEY);
        props.add(PropertyNames.QUOTA_AVAILABLE_BYTES);
        props.add(PropertyNames.QUOTA_USED_BYTES);
        props.add(PropertyNames.RESOURCE_ID);
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.SUPPORTED_REPORT_SET);
        props.add(PropertyNames.SYNC_TOKEN);
        PropFindMethod propFind = new PropFindMethod(super.webDAVClient.getBaseURI() + Config.getPathPrefix() + "/carddav/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_1);
        boolean found = false;
        for (MultiStatusResponse response : super.webDAVClient.doPropFind(propFind)) {
            String displayName = super.extractTextContent(DavPropertyName.DISPLAYNAME, response);
            if (folderName.equals(displayName)) {
                found = true;
                break;
            }
        }
        if (shouldExist) {
            assertTrue(found, "GAB folder collection not found below /carddav/");
        } else {
            assertFalse(found, "GAB folder collection found below /carddav/");
        }
    }

}
