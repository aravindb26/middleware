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

package com.openexchange.drive.client.windows.rest;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.openexchange.annotation.NonNull;
import com.openexchange.drive.client.windows.rest.service.internal.DriveClientWindowsExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;
import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Function;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

/**
 * {@link DriveClientWindowsRestHttpClient}
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */
public class DriveClientWindowsRestHttpClient {

    private static final Gson GSON = new Gson();

    /** Identifier to get a KubernetesDrive associated HTTP client */
    public static final String HTTP_CLIENT_ID = "DriveWindows";

    private final ManagedHttpClient httpClient;

    /**
     * Initializes a new {@link DriveClientWindowsRestHttpClient}.
     *
     * @param httpClient The {@link ManagedHttpClient}
     * @param host The base path
     */
    public DriveClientWindowsRestHttpClient(@NonNull ManagedHttpClient httpClient) {
        super();
        this.httpClient = httpClient;
    }

    /**
     * Retrieves the manifest from the given manifest URL
     *
     * @param url The URL to the manifest Endpoint
     * @return The {@link DriveManifest}
     * @throws OXException If URL is not reachable, malformed or response status is not equal to 200
     */
    public DriveManifest getManifest(URL url) throws OXException {
        return performGet(url, manifest -> { return manifest;} );
    }

    /**
     * Retrieves the download URL from the given manifest URL
     *
     * @param url The URL to the manifest Endpoint
     * @return The redirect url as String
     * @throws OXException If URL is not reachable, malformed or response status is not equal to 200
     */
    public String getRedirectUrl(URL url) throws OXException {
        return performGet(url, driveData -> { return driveData.getDownloadPath(); } );
    }

    private <T> T performGet(URL url, Function<DriveManifest, T> handler) throws OXException {
        HttpGet httpGet;
        try {
            httpGet = new HttpGet(url.toURI());
            return performRequest(httpGet, handler);
        } catch (URISyntaxException urie) {
            throw new OXException(urie);
        }
    }

    /**
     * Perform a HTTP request and create a {@link DriveManifest} from drive client service`s response
     *
     * @param <T> The {@link DriveManifest}
     * @param request The http request
     * @param handler The function handler creating the {@link DriveManifest}
     * @return A {@link DriveManifest} instance
     * @throws OXException In case the HTTP request can not be performed or response status is unexpected
     */
    private <T> T performRequest(HttpRequestBase request, Function<DriveManifest, T> handler) throws OXException {
        HttpResponse response = null;
        try {
            response = httpClient.execute(request);
            int responseStatus = response.getStatusLine().getStatusCode();
            if (responseStatus >= HttpStatus.SC_OK && responseStatus < HttpStatus.SC_MULTIPLE_CHOICES) {
                String json = EntityUtils.toString(response.getEntity());
                DriveManifest driveManifest = GSON.fromJson(json, DriveManifest.class);
                if (driveManifest == null) {
                    throw DriveClientWindowsExceptionCodes.INVALID_REQUEST_DATA.create();
                }
                return handler.apply( driveManifest );
            } else if (responseStatus >= HttpStatus.SC_BAD_REQUEST && responseStatus <= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                if (responseStatus == 404) {
                    throw DriveClientWindowsExceptionCodes.MISSING_RESOURCE.create(I(responseStatus));
                }
                throw DriveClientWindowsExceptionCodes.REMOTE_CLIENT_ERROR.create(I(responseStatus));
            } else if (responseStatus > HttpStatus.SC_INTERNAL_SERVER_ERROR ) {
                throw DriveClientWindowsExceptionCodes.REMOTE_SERVER_ERROR.create(I(responseStatus));
            }
            throw DriveClientWindowsExceptionCodes.UNEXPECTED_STATUS.create(I(responseStatus));
        } catch (ClientProtocolException e) {
            throw new OXException(e);
        } catch (IOException e) {
            throw DriveClientWindowsExceptionCodes.SERVICE_REQUEST_FAILED.create(request.getRequestLine().getUri());
        } catch (JsonSyntaxException e) {
            throw new OXException(e);
        } finally {
            HttpClients.close(request, response);
        }
    }

}
