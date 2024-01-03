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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.carddav.CardDAVTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link AddressbookPropertiesTest}
 *
 * Tests discovery of additional properties for WebDAV address books,
 * simulating the steps happening during account creation of the Mac addressbook client.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class AddressbookPropertiesTest extends CardDAVTest {

    public AddressbookPropertiesTest() {
        super();
    }

    /**
     * Checks if the CardDAV server reports some information about the address book.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDiscoverAddressbookProperties(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        final DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.ADD_MEMBER);
        props.add(PropertyNames.BULK_REQUESTS);
        props.add(PropertyNames.CURRENT_USER_PRIVILEGE_SET);
        props.add(PropertyNames.DISPLAYNAME);
        props.add(PropertyNames.MAX_IMAGE_SIZE);
        props.add(PropertyNames.MAX_RESOURCE_SIZE);
        props.add(PropertyNames.OWNER);
        props.add(PropertyNames.PUSH_TRANSPORTS);
        props.add(PropertyNames.PUSHKEY);
        props.add(PropertyNames.QUOTA_AVAILABLE_BYTES);
        props.add(PropertyNames.QUOTA_USED_BYTES);
        props.add(PropertyNames.RESOURCE_ID);
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.SUPPORTED_REPORT_SET);
        props.add(PropertyNames.SYNC_TOKEN);
        final PropFindMethod propFind = new PropFindMethod(super.webDAVClient.getBaseURI() + Config.getPathPrefix() + "/carddav/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_1);
        final MultiStatusResponse[] responses = super.webDAVClient.doPropFind(propFind);
        assertNotNull(responses, "got no response");
        assertTrue(0 < responses.length, "got no responses");

    }
}
