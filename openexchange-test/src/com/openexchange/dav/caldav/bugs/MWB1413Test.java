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

package com.openexchange.dav.caldav.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MWB1413Test}
 *
 * birthday calendar name is changeable via DAV but not in Web UI.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MWB1413Test extends CalDAVTest {

    protected MultiStatusResponse[] propPatch(String href, DavPropertySet setProps, DavPropertyNameSet removeProps) throws Exception {
        PropPatchMethod propPatch = null;
        try {
            propPatch = new PropPatchMethod(getBaseUri() + href, setProps, removeProps);
            assertEquals(webDAVClient.executeMethod(propPatch), StatusCodes.SC_MULTISTATUS, "unexpected response status");
            return propPatch.getResponseBodyAsMultiStatus().getResponses();
        } finally {
            release(propPatch);
        }
    }

    protected MultiStatusResponse[] propFind(String href, DavPropertyNameSet props, int depth) throws Exception {
        PropFindMethod propFind = null;
        try {
            propFind = new PropFindMethod(getBaseUri() + href, props, depth);
            assertEquals(webDAVClient.executeMethod(propFind), StatusCodes.SC_MULTISTATUS, "unexpected response status");
            return propFind.getResponseBodyAsMultiStatus().getResponses();
        } finally {
            release(propFind);
        }
    }

    private static DavPropertySet extractSinglePropertySet(MultiStatusResponse[] responses, int statusCode) {
        assertNotNull(responses);
        assertEquals(1, responses.length);
        return responses[0].getProperties(statusCode);
    }

    private String discoverBirthdaysHref() throws Exception {
        String href = Config.getPathPrefix() + "/caldav/";
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.DISPLAYNAME);
        MultiStatusResponse[] responses = propFind(href, props, DavConstants.DEPTH_1);
        for (MultiStatusResponse response : responses) {
            String displayName = extractTextContent(PropertyNames.DISPLAYNAME, response);
            if ("Birthdays".equals(displayName)) {
                return response.getHref();
            }
        }
        return null;
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRenameBirthdaysCalendar(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String href = discoverBirthdaysHref();
        assertNotNull(href);
        testForbiddenCollectionRename(href);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRenameDefaultCalendar(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testForbiddenCollectionRename(Config.getPathPrefix() + "/caldav/" + encodeFolderID(getDefaultFolderID()) + "/");
    }

    private void testForbiddenCollectionRename(String href) throws Exception {
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.DISPLAYNAME);
        MultiStatusResponse[] responses = propFind(href, props, DavConstants.DEPTH_0);
        DavPropertySet okProps = extractSinglePropertySet(responses, StatusCodes.SC_OK);
        assertTrue(okProps.contains(PropertyNames.DISPLAYNAME));
        String displayName = String.valueOf(okProps.get(DavPropertyName.DISPLAYNAME).getValue());
        /*
         * attempt to change the displayname property of the user's default calendar
         */
        DavPropertySet setProps = new DavPropertySet();
        setProps.add(new DefaultDavProperty<String>(PropertyNames.DISPLAYNAME, "renamed", false));
        responses = propPatch(href, setProps, new DavPropertyNameSet());
        DavPropertySet rejectedProps = extractSinglePropertySet(responses, StatusCodes.SC_FORBIDDEN);
        assertTrue(rejectedProps.contains(PropertyNames.DISPLAYNAME));
        /*
         * check that the displayname was not changed
         */
        responses = propFind(href, props, DavConstants.DEPTH_0);
        okProps = extractSinglePropertySet(responses, StatusCodes.SC_OK);
        assertTrue(okProps.contains(PropertyNames.DISPLAYNAME));
        assertEquals(displayName, String.valueOf(okProps.get(DavPropertyName.DISPLAYNAME).getValue()));
    }

}
