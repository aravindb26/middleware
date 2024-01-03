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
package com.openexchange.segmenter.client.impl;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.segmenter.client.impl.SegmenterProperties.BASE_URL;
import static com.openexchange.segmenter.client.impl.SegmenterProperties.LOCAL_SITE_ID;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.segment.SegmentMarker;
import com.openexchange.segmenter.client.SegmenterService;
import com.openexchange.segmenter.client.Site;
import com.openexchange.server.ServiceLookup;

/**
 * {@link SegmenterServiceImpl} - The implementation for the {@link SegmenterService}.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class SegmenterServiceImpl implements SegmenterService, Reloadable {

    /** The HTTP client identifier as used towards {@link HttpClientService} */
    public static final String HTTP_CLIENT_ID = "segmenterService";

    /** Static path segments pointing to the segmenter service endpoint */
    private static final List<String> PATH_SEGMENTS = Arrays.asList("segmenter", "v1", "segments");

    private static final Logger LOG = LoggerFactory.getLogger(SegmenterServiceImpl.class);

    private final ServiceLookup services;
    private final AtomicReference<String> baseUrlReference;
    private final AtomicReference<Site> localSiteReference;

    /**
     * Initializes a new {@link SegmenterServiceImpl}.
     *
     * @param services The service look-up
     * @throws OXException If initialization fails
     */
    public SegmenterServiceImpl(ServiceLookup services) throws OXException {
        super();
        this.services = services;
        this.baseUrlReference = new AtomicReference<>();
        this.localSiteReference = new AtomicReference<>();
        init(services.getServiceSafe(ConfigurationService.class));
    }

    @Override
    public Site getLocalSite() {
        return localSiteReference.get();
    }

    @Override
    public Site getSite(SegmentMarker marker) throws OXException {
        if (null == marker) {
            return null;
        }
        String baseUrl = baseUrlReference.get();
        if (baseUrl == null) {
            /*
             * statically use 'local' site in non-sharded environments
             */
            Site localSite = localSiteReference.get();
            LOG.trace("No URL to segmenter service configured, falling back to local site '{}' for segment marker '{}'", localSite, marker);
            return localSite;
        }

        HttpGet request = null;
        HttpResponse response = null;
        try {
            /*
             * perform GET request to query a single marker
             */
            request = new HttpGet(buildSegmenterServiceURI(baseUrl, new BasicNameValuePair("marker", marker.encode())));
            request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            /*
             * read response & convert to site
             */
            response = examineStatusCode(getHttpClient().execute(request));
            return response == null ? null : deserializeSite(readJSONResponse(response.getEntity()));
        } catch (Exception e) {
            throw OXException.general("Unable to load site for marker", e);
        } finally {
            HttpClients.close(request, response);
        }
    }

    @Override
    public List<Site> getSites(List<SegmentMarker> markers) throws OXException {
        if (null == markers || markers.isEmpty()) {
            return Collections.emptyList();
        }

        String baseUrl = baseUrlReference.get();
        if (baseUrl == null) {
            /*
             * statically use 'local' site in non-sharded environments
             */
            Site localSite = localSiteReference.get();
            LOG.trace("No URL to segmenter service configured, falling back to local site '{}' for segment markers '{}'", localSite, markers);
            return Collections.nCopies(markers.size(), localSite);
        }

        HttpPut request = null;
        HttpResponse response = null;
        try {
            /*
             * perform PUT request to query multiple markers
             */
            request = new HttpPut(buildSegmenterServiceURI(baseUrl));
            request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            request.setEntity(new StringEntity(toJson(markers), ContentType.APPLICATION_JSON));
            /*
             * read response & convert sites
             */
            response = examineStatusCode(getHttpClient().execute(request));
            JSONArray jSites = response == null ? JSONArray.EMPTY_ARRAY : readJSONResponse(response.getEntity());
            List<Site> sites = new ArrayList<>(jSites.length());
            for (Object jSite : jSites) {
                sites.add(deserializeSite(((JSONObject) jSite)));
            }
            return sites;
        } catch (Exception e) {
            throw OXException.general("Unable to load site for markers", e);
        } finally {
            HttpClients.close(request, response);
        }
    }

    // -------------------- reloadable methods -------------------

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        init(configService);
    }

    @Override
    public Interests getInterests() {
        return DefaultInterests.builder().propertiesOfInterest(BASE_URL.getFQPropertyName(), LOCAL_SITE_ID.getFQPropertyName()).build();
    }

    // ---------------------- private methods -------------------

    /**
     * Initializes the segmenter configuration.
     *
     * @param configService A reference to the configuration service
     */
    private void init(ConfigurationService configService) {
        String baseUrl = configService.getProperty(BASE_URL.getFQPropertyName(), BASE_URL.getDefaultValue(String.class));
        String localSiteId = configService.getProperty(LOCAL_SITE_ID.getFQPropertyName(), LOCAL_SITE_ID.getDefaultValue(String.class));
        baseUrlReference.set(baseUrl);
        localSiteReference.set(new SiteImpl(localSiteId, 1.0f));
        LOG.info("Using '{}' as base URI to the segmenter service, using '{}' as local site id.", baseUrl, localSiteId);
    }

    /**
     * Gets the HTTP client.
     *
     * @return The HTTP client
     * @throws OXException If HTTP client cannot be returned
     */
    private HttpClient getHttpClient() throws OXException {
        return services.getServiceSafe(HttpClientService.class).getHttpClient(HTTP_CLIENT_ID);
    }

    /**
     * Converts the list of markers to a JSON array.
     * <p>
     * Example
     * <pre>
     *  "[{"marker": "eyJzY2hlbWEiOiAiZGF0YWJzZV81In0="}, ..., {"marker": "eyJzY2hlbWEiOiAiZGF0YWJhc2VfNDUifQ=="}]"
     * </pre>
     *
     * @param markers The markers
     * @return The JSON string
     */
    private static String toJson(List<SegmentMarker> markers) {
        JSONArray array = new JSONArray(markers.size());
        markers.stream()
               .filter(m -> m != null)
               .map(m -> toMarkerObject(m))
               .forEach(array::put);
        return array.toString();
    }

    /**
     * Converts the encoded marker string to a JSON object.
     * <p>
     * Example
     * <pre>
     *  {"marker": "eyJzY2hlbWEiOiAiZGF0YWJzZV81In0="}
     * </pre>
     *
     * @param m The marker
     * @return The marker JSON object
     */
    private static JSONObject toMarkerObject(SegmentMarker m) {
        return new JSONObject(1).putSafe("marker", m.encode());
    }

    /**
     * Checks if specified HTTP response's status code is is range of success responses (<code>200</code> – <code>299</code>).
     *
     * @param resp The HTTP response to examine
     * @throws OXException If status code does <b>not</b> fall in success responses
     */
    private static HttpResponse examineStatusCode(HttpResponse resp) throws OXException {
        if (resp == null) {
            return resp;
        }

        int statusCode = resp.getStatusLine().getStatusCode();
        if (statusCode < 200 || statusCode > 299) {
            LOG.error("Segmenter service responds with unexpected response code ({}): '{}'", I(statusCode), resp.getStatusLine().getReasonPhrase());
            throw OXException.general("Unable to load site for marker");
        }
        return resp;
    }

    /**
     * Builds the URI to the segmenter service end-point, based on the provided base URI and optional request parameters.
     *
     * @param baseUrl The base URL of the segmenter service
     * @param parameters The parameters to add to the constructed URI
     * @return The URI
     * @throws OXException If URI cannot be built
     */
    private static URI buildSegmenterServiceURI(String baseUrl, NameValuePair... parameters) throws OXException {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl).setPathSegments(PATH_SEGMENTS);
            if (null != parameters && 0 < parameters.length) {
                uriBuilder.addParameters(Arrays.asList(parameters));
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw OXException.general("Invalid configured base URL", e);
        }
    }

    /**
     * Reads specified entnty's content and parses it to a JSON value.
     *
     * @param <V> The JSON value type; either object or array
     * @param responseEntity The response entity to read from
     * @return The JSON value
     * @throws JSONException If a JSON error occurs
     * @throws IOException If an I/O error occurs
     */
    private static <V extends JSONValue> V readJSONResponse(HttpEntity responseEntity) throws JSONException, IOException {
        InputStream content = null;
        try {
            content = responseEntity.getContent();
            return (V) JSONServices.parse(content);
        } finally {
            Streams.close(content);
        }
    }

    /**
     * Deserializes specified site JSON representation to a site instance.
     *
     * @param jSite The JSON representation to parse
     * @return The resulting site
     * @throws JSONException If a JSON error occurs
     */
    private static Site deserializeSite(JSONObject jSite) throws JSONException {
        if (null == jSite) {
            return null;
        }
        return new SiteImpl(jSite.getString("id"), (float) jSite.optDouble("availability", 1.0f));
    }

}
