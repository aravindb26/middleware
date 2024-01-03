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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

/**
 * {@link Bug29554Test}
 *
 * Mountain Lion: The server responded: &quot;403&quot; to operation CalDAVWriteEntityQueueableOperation.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug29554Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSupportedComponentSets(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * discover supported component sets of root collection
         */
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.SUPPORTED_CALENDAR_COMPONENT_SETS);
        PropFindMethod propFind = new PropFindMethod(webDAVClient.getBaseURI() + Config.getPathPrefix() + "/caldav/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        MultiStatusResponse[] responses = webDAVClient.doPropFind(propFind);
        assertNotNull(responses, "got no response");
        assertTrue(0 < responses.length, "got no responses");
        Set<String> comps = new HashSet<String>();
        for (MultiStatusResponse response : responses) {
            if (response.getProperties(StatusCodes.SC_OK).contains(PropertyNames.SUPPORTED_CALENDAR_COMPONENT_SETS)) {
                List<Node> nodes = extractNodeListValue(PropertyNames.SUPPORTED_CALENDAR_COMPONENT_SETS, response);
                assertNotNull(nodes);
                for (Node node : nodes) {
                    comps.add(removeWhitspaceNodes(node.getChildNodes()).item(0).getAttributes().getNamedItem("name").getTextContent());
                }
            } else {
                fail("no multistatus response");
            }
        }
        assertTrue(comps.contains("VEVENT"), "no VEVENT found");
        assertTrue(comps.contains("VTODO"), "no VTODO found");
    }

}
