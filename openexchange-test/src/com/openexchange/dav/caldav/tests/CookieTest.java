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

package com.openexchange.dav.caldav.tests;

import org.apache.commons.httpclient.Header;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.junit.jupiter.api.Assertions;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * {@link CookieTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class CookieTest extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testNoSessionCookieForCalDAV(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * execute simple propfind
         */
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.PRINCIPAL_URL);
        PropFindMethod propFind = new PropFindMethod(webDAVClient.getBaseURI() + Config.getPathPrefix() + "/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        try {
            Assertions.assertEquals(StatusCodes.SC_MULTISTATUS, webDAVClient.executeMethod(propFind), "unexpected http status");
        } finally {
            release(propFind);
        }
        /*
         * check for Set-Cookie header
         */
        Header[] headers = propFind.getResponseHeaders("Set-Cookie");
        if (null != headers) {
            for (Header header : headers) {
                if (header.getValue().contains("JSESSIONID")) {
                    Assertions.fail(header.getName());
                }
            }
        }
    }

}
