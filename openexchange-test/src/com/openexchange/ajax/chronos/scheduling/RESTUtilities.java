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

package com.openexchange.ajax.chronos.scheduling;

import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.codec.binary.Base64;
import com.openexchange.ajax.framework.ClientCommons;
import com.openexchange.java.Charsets;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.restclient.invoker.ApiClient;

/**
 * 
 * {@link RESTUtilities}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class RESTUtilities {

    private static String hostname;

    private static String protocol;

    /**
     * Get the base path to the server using the port
     *
     * @return The base path
     */
    private static String getBasePath() {
        if (hostname == null) {
            hostname = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);
        }

        if (protocol == null) {
            protocol = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
            if (protocol == null) {
                protocol = "http";
            }
        }
        return protocol + "://" + hostname;
    }

    /**
     * Creates a rest client based on the configured REST user, which is server-wide configured
     * <p>
     * The client is configured to use <code>BASIC AUTH</code>
     *
     * @return The new REST client
     */
    public static ApiClient createRESTClient(String createdBy) {
        ApiClient restClient = new ApiClient();
        restClient.setBasePath(getBasePath());
        TestUser restUser = TestContextPool.getRestUser();
        restClient.setUsername(restUser.getUser());
        restClient.setPassword(restUser.getPassword());
        String authorizationHeaderValue = "Basic " + Base64.encodeBase64String((restUser.getUser() + ":" + restUser.getPassword()).getBytes(Charsets.UTF_8));
        restClient.addDefaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
        restClient.addDefaultHeader(ClientCommons.X_OX_HTTP_TEST_HEADER_NAME, createdBy);
        return restClient;
    }

}
