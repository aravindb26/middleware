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
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.java.Strings;

/**
 * {@link Bug32897Test}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug32897Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDefaultAlarms(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * discover default alarms
         */
        final DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.DEFAULT_ALARM_VEVENT_DATE);
        props.add(PropertyNames.DEFAULT_ALARM_VEVENT_DATETIME);
        props.add(PropertyNames.SUPPORTED_CALENDAR_COMPONENT_SET);
        final PropFindMethod propFind = new PropFindMethod(webDAVClient.getBaseURI() + Config.getPathPrefix() + "/caldav/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_1);
        final MultiStatusResponse[] responses = webDAVClient.doPropFind(propFind);
        assertNotNull(responses, "got no response");
        assertTrue(0 < responses.length, "got no responses");
        for (final MultiStatusResponse response : responses) {
            if (response.getProperties(StatusCodes.SC_OK).contains(PropertyNames.SUPPORTED_CALENDAR_COMPONENT_SET)) {
                final Node node = extractNodeValue(PropertyNames.SUPPORTED_CALENDAR_COMPONENT_SET, response);
                if (null != node && null != node.getAttributes() && null != node.getAttributes().getNamedItem("name") && "VEVENT".equals(node.getAttributes().getNamedItem("name").getTextContent())) {
                    Object defaultAlarmVEventDate = response.getProperties(StatusCodes.SC_OK).get(PropertyNames.DEFAULT_ALARM_VEVENT_DATE).getValue();
                    assertTrue(null == defaultAlarmVEventDate || Strings.isEmpty(String.valueOf(defaultAlarmVEventDate)), "wrong default alarm");
                    Object defaultAlarmVEventDatetime = response.getProperties(StatusCodes.SC_OK).get(PropertyNames.DEFAULT_ALARM_VEVENT_DATETIME).getValue();
                    assertTrue(null == defaultAlarmVEventDatetime || Strings.isEmpty(String.valueOf(defaultAlarmVEventDatetime)), "wrong default alarm");
                }
            }
        }

    }

}
