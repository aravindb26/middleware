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

package com.openexchange.ajax.chronos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.json.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openexchange.ajax.proxy.MockRequestMethod;
import com.openexchange.ajax.proxy.MockServiceHelperService;
import com.openexchange.testing.httpclient.models.FolderCalendarConfig;
import com.openexchange.testing.httpclient.models.FolderCalendarExtendedProperties;
import com.openexchange.testing.httpclient.models.FolderData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractExternalProviderChronosTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public abstract class AbstractExternalProviderChronosTest extends AbstractChronosTest {

    private final String providerId;

    /**
     * Initialises a new {@link AbstractExternalProviderChronosTest}.
     */
    public AbstractExternalProviderChronosTest(String providerId) {
        super();
        this.providerId = providerId;
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        setUpConfiguration();
    }

    /**
     * Mocks an external provider request with the specified URI, response content/payload, and status code
     *
     * @param uri The URI of the external source
     * @param responseContent The response content/payload
     * @param httpStatus The response status code
     */
    protected void mock(String uri, String responseContent, int httpStatus) {
        mock(uri, responseContent, httpStatus, Collections.emptyMap());
    }

    /**
     * Mocks an external provider request with the specified URI, response content/payload, status code and response headers
     *
     * @param uri The URI of the external source
     * @param responseContent The response content/payload
     * @param httpStatus The response status code
     * @param responseHeaders the response headers
     */
    protected void mock(String uri, String responseContent, int httpStatus, Map<String, String> responseHeaders) {
        mock(uri, responseContent, httpStatus, responseHeaders, Collections.emptyMap());
    }

    /**
     * Mocks an external provider request with the specified URI, response content/payload, status code, response headers and request parameter.
     *
     * @param uri The URI of the external source
     * @param responseContent The response content/payload
     * @param httpStatus The response status code
     * @param responseHeaders the response headers
     * @param params The request parameter
     */
    protected void mock(String uri, String responseContent, int httpStatus, Map<String, String> responseHeaders, Map<String, String> params) {
        mock(MockRequestMethod.GET, uri, responseContent, httpStatus, responseHeaders, params, 0);
    }

    /**
     * Mocks an external provider request with the specified URI, response content/payload, status code and response headers
     *
     * @param method The HTTP method
     * @param uri The URI of the external source
     * @param responseContent The response content/payload
     * @param httpStatus The response status code
     */
    protected void mock(MockRequestMethod method, String uri, String responseContent, int httpStatus) {
        mock(method, uri, responseContent, httpStatus, null, Collections.emptyMap(), Collections.emptyMap(), 0);
    }

    protected void mock(MockRequestMethod method, String path, String responseContent, int httpStatus, Map<String, String> responseHeaders, Map<String, String> requestParameter, int delay) {
        mock(method, path, responseContent, httpStatus, null, responseHeaders, requestParameter, delay);
    }

    protected void mock(MockRequestMethod method, String path, String responseContent, int httpStatus, String contentType, Map<String, String> responseHeaders, Map<String, String> requestParameter, int delay) {
        MockServiceHelperService.getInstance().mock(method, path, responseContent, httpStatus, contentType, responseHeaders, requestParameter, delay);
    }

    /**
     * Asserts the specified {@link FolderData}.
     *
     * @param actualFolderData The actual {@link FolderData} to assert
     * @param expectedTitle The expected title
     * @return The {@link FolderData}
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    protected FolderData assertFolderData(FolderData actualFolderData, String expectedTitle, JSONObject config, JSONObject extProperties) throws JsonParseException, JsonMappingException, IOException {
        assertNotNull(actualFolderData, "The folder data is 'null'");
        assertEquals(expectedTitle, actualFolderData.getTitle(), "The title does not match");
        assertEquals(actualFolderData.getComOpenexchangeCalendarProvider(), providerId, "The provider identifier does not match");
        assertNotNull(actualFolderData.getComOpenexchangeCalendarExtendedProperties(), "The extended properties configuration is 'null'");
        assertNotNull(actualFolderData.getComOpenexchangeCalendarConfig(), "The calendar configuration is 'null'");

        ObjectMapper objectMapper = new ObjectMapper();
        FolderCalendarConfig expectedConfig = objectMapper.readValue(config.toString(), FolderCalendarConfig.class);
        FolderCalendarExtendedProperties expectedProperties = objectMapper.readValue(extProperties.toString(), FolderCalendarExtendedProperties.class);
        assertEquals(expectedConfig, actualFolderData.getComOpenexchangeCalendarConfig());
        assertEquals(expectedProperties, actualFolderData.getComOpenexchangeCalendarExtendedProperties());
        return actualFolderData;
    }
}
