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
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Node;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.dav.carddav.CardDAVTest;

/**
 * {@link MWB1972Test}
 *
 * Thunderbird cannot find calendars
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MWB1972Test extends CardDAVTest {

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.THUNDERBIRD_102_6_1;
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDiscoverPrincipalResource(String authMethod) throws Exception {
        prepareOAuthClientIfNeeded(authMethod);
        /*
         * discover current user principal at root
         */
        // CalDAV: send (PROPFIND http://dav.ox.test/): <?xml version="1.0" encoding="UTF-8"?>
        // <D:propfind xmlns:D='DAV:' xmlns:A='http://apple.com/ns/ical/' xmlns:C='urn:ietf:params:xml:ns:caldav'><D:prop><D:resourcetype/><D:owner/><D:displayname/><D:current-user-principal/><D:current-user-privilege-set/><A:calendar-color/><C:calendar-home-set/></D:prop></D:propfind>
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.OWNER);
        props.add(PropertyNames.DISPLAYNAME);
        props.add(PropertyNames.CURRENT_USER_PRINCIPAL);
        props.add(PropertyNames.CURRENT_USER_PRIVILEGE_SET);
        props.add(PropertyNames.CALENDAR_COLOR);
        props.add(PropertyNames.CALENDAR_HOME_SET);
        PropFindMethod propFind = new PropFindMethod(webDAVClient.getBaseURI() + Config.getPathPrefix(), DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        MultiStatusResponse response = assertSingleResponse(webDAVClient.doPropFind(propFind));
        String currentUserPrincipal = extractHref(PropertyNames.CURRENT_USER_PRINCIPAL, response);
        assertNotNull(currentUserPrincipal);
        assertTrue(currentUserPrincipal.contains("/" + getClient().getValues().getUserId()));
        /*
         * check user principal resource type
         */
        // CalDAV: send (PROPFIND http://dav.ox.test/principals/users/13): 
        // <?xml version="1.0" encoding="UTF-8"?><D:propfind xmlns:D='DAV:' xmlns:C='urn:ietf:params:xml:ns:caldav'><D:prop><D:resourcetype/><C:calendar-home-set/></D:prop></D:propfind>
        props = new DavPropertyNameSet();
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.CALENDAR_HOME_SET);
        propFind = new PropFindMethod(webDAVClient.getBaseURI() + Config.getPathPrefix() + "/principals/users/" + getClient().getValues().getUserId() + "/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        response = assertSingleResponse(webDAVClient.doPropFind(propFind));
        Node resourceTypeNode = extractNodeValue(PropertyNames.RESOURCETYPE, response);
        assertNotNull(resourceTypeNode);
        assertEquals("DAV:", resourceTypeNode.getNamespaceURI());
        assertEquals("principal", resourceTypeNode.getLocalName());
    }
}
