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

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * {@link Bug63360Test}
 *
 * joplin app not working with appsuite
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.2
 */
public class Bug63360Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testIfNoneMatchOnCollection(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETLASTMODIFIED);
        props.add(PropertyNames.RESOURCETYPE);
        PropFindMethod propFind = null;
        try {
            propFind = new PropFindMethod(getBaseUri() + Config.getPathPrefix(), DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
            propFind.addRequestHeader("If-None-Match", "JoplinIgnore-42130");
            webDAVClient.doPropFind(propFind, StatusCodes.SC_MULTISTATUS);
        } finally {
            release(propFind);
        }
    }

}
