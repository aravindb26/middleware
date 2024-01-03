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

import javax.servlet.http.HttpServletResponse;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.junit.jupiter.api.Assertions;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.methods.MkCalendarMethod;
import com.openexchange.dav.caldav.properties.SupportedCalendarComponentSetProperty;
import com.openexchange.groupware.container.FolderObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug66837Test}
 *
 * Sync fail on creating Task folder
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.3
 */
public class Bug66837Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateTaskCollection(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * prepare target resource
         */
        String collectionName = randomUID();
        String name = randomUID();
        DavPropertySet setProperties = new DavPropertySet();
        setProperties.add(new SupportedCalendarComponentSetProperty(SupportedCalendarComponentSetProperty.Comp.VTODO));
        setProperties.add(new DefaultDavProperty<String>(PropertyNames.DISPLAYNAME, name));
        /*
         * create collection via MKCALENDAR
         */
        String targetHref = "/caldav/" + collectionName + '/';
        MkCalendarMethod mkCalendar = null;
        try {
            mkCalendar = new MkCalendarMethod(getBaseUri() + targetHref, setProperties);
            Assertions.assertEquals(StatusCodes.SC_CREATED, webDAVClient.executeMethod(mkCalendar), "response code wrong");
        } finally {
            release(mkCalendar);
        }
        /*
         * verify task folder on server
         */
        FolderObject folder = getTaskFolder(name);
        assertNotNull(folder, "folder not found on server");
        assertEquals(name, folder.getFolderName(), "folder name wrong");
        /*
         * verify task collection on client
         */
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        props.add(PropertyNames.DISPLAYNAME);
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.SUPPORTED_CALENDAR_COMPONENT_SET);
        props.add(PropertyNames.XMPP_URI);
        props.add(PropertyNames.CALENDAR_COLOR);
        PropFindMethod propFind = new PropFindMethod(getBaseUri() + targetHref, DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        MultiStatusResponse[] responses = webDAVClient.doPropFind(propFind);
        assertNotNull(responses, "got no response");
        assertTrue(0 < responses.length, "got no response");
        MultiStatusResponse folderResponse = null;
        for (MultiStatusResponse response : responses) {
            if (response.getPropertyNames(HttpServletResponse.SC_OK).contains(PropertyNames.DISPLAYNAME)) {
                if (name.equals(super.extractTextContent(PropertyNames.DISPLAYNAME, response))) {
                    folderResponse = response;
                    break;
                }
            }
        }
        assertNotNull(folderResponse, "no response for new folder");
    }

}
