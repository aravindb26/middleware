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

package com.openexchange.collabora.online.client;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.collabora.online.client.conversion.ConversionFormat;
import com.openexchange.collabora.online.client.conversion.ConversionFormat.ConversionFormatParameter;
import com.openexchange.collabora.online.client.exception.CollaboraExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;

/**
 * {@link CollaboraClient}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CollaboraClient {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboraClient.class);

    /**
     * The base endpoint of the Collabora CODE Conversion API
     */
    private static final String BASE_CONVERSION_ENDPOINT = "cool/convert-to/";

    /**
     * The name the file parameter added to the conversion request
     */
    private static final String PARAMETER_FILE = "File";

    /**
     * The name the language parameter added to the conversion request
     */
    private static final String PARAMETER_LANGUAGE = "lang";

    /**
     * The fallback content-type
     */
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private final ManagedHttpClient client;
    private final URI baseUri;

    /**
     * Initializes a new {@link CollaboraClient}.
     *
     * @param baseUri The base URI being used to connect to the "Collabora online" service
     * @param client The {@link ManagedHttpClient} to use for connecting to Collabora
     */
    public CollaboraClient(URI baseUri, ManagedHttpClient client) {
        super();
        this.baseUri = baseUri;
        this.client = client;
    }

    //----------------------------------------------------------------------------------------------------

    /**
     * Internal method to create a {@link ContentType} instance from the given content-type-string
     *
     * @param contentType The string to create the {@link ContentType} for
     * @return The {@link ContentType} for the given string, The {@link ContentType} for "application/octet-stream",
     * in case the string is <code>null</code> or empty.
     */
    private ContentType createContentType(String contentType) {
        return Strings.isNotEmpty(contentType) ? ContentType.create(contentType) : ContentType.APPLICATION_OCTET_STREAM;
    }

    /**
     * Internal helper method create a log entry for the given parameter/value pair and add it to the given {@link StringBuilder}
     *
     * @param name The name of the parameter to log
     * @param value The value of the parameter to log
     * @param builder The builder to add the logging string to
     */
    private void logParameter(String name, String value, StringBuilder builder) {
        builder.append("[").append(name).append("|").append(value).append("]");
    }

    /**
     * Creates the HTTP request sent to the Collabora CODE server in order to convert the given data
     *
     * @param data The data to convert
     * @param contentType The contentType of the data to convert
     * @param format The format to convert the given data into
     * @param locale The locale to consider for the conversion request, or <code>null</code> to omit the locale
     * @return The conversion request ready to be executed against a Collabora CODE server
     * @throws OXException if an invalid url is provided, or an unexpected error is occurred
     */
    private HttpRequestBase createConversionRequest(InputStream data, ContentType contentType, ConversionFormat format, Locale locale) throws OXException {
        try {
            StringBuilder parameterLogger = new StringBuilder();
            URIBuilder uriBuilder = new URIBuilder(baseUri).setPath(BASE_CONVERSION_ENDPOINT + format.getName());
            HttpPost request = new HttpPost(uriBuilder.build().toString());
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();

            //Adding the locale, if provided
            if (locale != null) {
                logParameter(PARAMETER_LANGUAGE, locale.toLanguageTag(), parameterLogger);
                entityBuilder.addTextBody(PARAMETER_LANGUAGE, locale.toLanguageTag());
            }

            //Adding sub-parameters of the given conversion format to the request
            for (ConversionFormatParameter formatParameter : format.getParameters()) {
                logParameter(formatParameter.name(), formatParameter.value(), parameterLogger);
                entityBuilder.addTextBody(formatParameter.name(), formatParameter.value());
            }

            //Adding the file data to convert
            entityBuilder.addBinaryBody(PARAMETER_FILE, data, contentType, PARAMETER_FILE);

            request.setEntity(entityBuilder.build());
            LOG.debug("Created Collabora conversion request: {} - {}", request, parameterLogger);
            return request;
        } catch (URISyntaxException e) {
            throw CollaboraExceptionCodes.INVALID_URL.create(e, baseUri, e.getMessage());
        }
    }

    /**
     * Executes a request against a Collabora CODE server
     *
     * @param request The request to execute
     * @return The response The response to the executed request
     * @throws OXException due an error executing the given request
     */
    private HttpResponse executeRequest(HttpRequestBase request) throws OXException {
        boolean success = false;
        HttpResponse response = null;
        try {
            response = client.execute(request);
            assertResponse(response);
            LOG.debug("Successfully executed Collabora conversion request {}", request);
            success = true;
            return response;
        } catch (IOException e) {
            throw CollaboraExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            //Release resources in case of any error (incl. possible, not caught RuntimeExceptions)
            //Do not clean up in case of success, because the caller will close the resources if finally consumed
            if (!success) {
                HttpClients.close(request, response);
            }
        }
    }

    /**
     * Internal method to check the response from a Collabora CODE server
     *
     * @param response The response to check
     * @throws OXException in case the response was not HTTP 200 OK
     */
    private void assertResponse(HttpResponse response) throws OXException {
        if (response.getStatusLine() == null) {
            throw CollaboraExceptionCodes.UNEXPECTED_ERROR.create("Unable to parse response code");
        }
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return;
        }

        final String msg = "(HTTP-Status %s) %s".formatted(I(response.getStatusLine().getStatusCode()), getErrorMessage(response));
        LOG.debug(msg);
        throw CollaboraExceptionCodes.REMOTE_SERVER_ERROR.create(msg);
    }

    /**
     * Parses and consumes the entity of the given response as error message
     *
     * @param response The response to get the error body from
     * @return The error as String
     */
    private String getErrorMessage(HttpResponse response) {
        try {
            if (response.getEntity() != null) {
                try (InputStream content = response.getEntity().getContent()) {
                    return IOUtils.toString(content, StandardCharsets.UTF_8);
                }
            }
        } catch (UnsupportedOperationException | IOException e) {
            LOG.error("Failed to obtaining error message from response {}", e.getMessage());
        }
        return null;
    }

    /**
     * Internal method to obtain the content, as {@link InputStream}, from the given {@link HttpResponse}
     *
     * @param response The response to obtain the InputStream for
     * @return The InputStream from the given response
     * @throws OXException if an error is occurred when getting the InputStream from the given response or receiving an empty response
     */
    private InputStream getContent(HttpResponse response) throws OXException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw CollaboraExceptionCodes.UNEXPECTED_ERROR.create("Received an empty response");
        }
        try {
            return entity.getContent();
        } catch (UnsupportedOperationException e) {
            throw CollaboraExceptionCodes.UNEXPECTED_ERROR.create(e, "Unable to parse response: " + e.getMessage());
        } catch (IOException e) {
            throw CollaboraExceptionCodes.IO_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Getting the Content-Type header value of given {@link HttpResponse}
     *
     * @param response The response to get the Content-Type header for
     * @return The Content-Type value of the given {@link HttpResponse}, or <code>null</code> if the header is missing or empty.
     */
    private String getContentType(HttpResponse response) {
        Header resultContentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        if (resultContentTypeHeader == null) {
            LOG.debug("Missing Content-Type header for converted document.");
            return null;
        }

        String contentType = resultContentTypeHeader.getValue();
        LOG.debug("Content-Type header for converted document: {}", contentType);
        return Strings.isNotEmpty(contentType) ? contentType.trim() : null;
    }

    /**
     * Parses the filename from the Content-Disposition header of the given response, if present
     *
     * @param response The response to parse the filename from
     * @return The filename of the response's Content-Disposition header, or <code>null</code>
     */
    private String getFileName(HttpResponse response) {
        Header contentDisposition = response.getFirstHeader("Content-Disposition");
        if (contentDisposition == null || !Strings.isNotEmpty(contentDisposition.getValue())) {
            return null;
        }
        Pattern pattern = Pattern.compile("filename=['\"]?([^'\"]+)['\"]?");
        Matcher matcher = pattern.matcher(contentDisposition.getValue());
        if (!matcher.find()) {
            return null;
        }
        String fileName = matcher.group(1);
        if (fileName == null) {
            return null;
        }
        //remove everything before and including a path segment
        return fileName.replaceAll(".*[/\\\\]", "");
    }

    //----------------------------------------------------------------------------------------------------

    /**
     * Converts the given data into the specified format by using the "Collabora online" conversion API
     *
     * @param data The data to convert
     * @param contentType The content type of the given data
     * @param format The format to convert the data to
     * @param locale The locale to consider during the conversation, or <code>null</code> in order to use Collabora's default language
     * @return The converted data as {@link InputStream}
     * @throws OXException if a conversion error is occurred
     */
    public ThresholdFileHolder getConversionResult(InputStream data, String contentType, ConversionFormat format, Locale locale) throws OXException {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(format, "format must not be null");

        ThresholdFileHolder fileHolder = null;
        boolean success = false;
        HttpRequestBase request = null;
        HttpResponse response = null;
        try {
            request = createConversionRequest(data, createContentType(contentType), format, locale);
            response = executeRequest(request);

            /* Getting the content-type of the result if present, or just guess based on the input format if missing */
            String resultContentType = getContentType(response);
            if (resultContentType == null || APPLICATION_OCTET_STREAM.equalsIgnoreCase(resultContentType)) {
                resultContentType = Strings.isNotEmpty(format.getContentType()) ? format.getContentType() : APPLICATION_OCTET_STREAM;
                LOG.debug("Adopted Content-Type header for converted document to {}", resultContentType);
            }

            /* Getting the filename from the Content-Disposition Header, if present */
            String fileName = getFileName(response);

            /* Store response data into file holder and return it */
            fileHolder = new ThresholdFileHolder();
            fileHolder.setContentType(resultContentType);
            fileHolder.setName(fileName);
            try (InputStream responseContentStream = getContent(response)) {
                fileHolder.write(responseContentStream);
            }
            success = true;
            return fileHolder;
        } catch (IOException e) {
            throw CollaboraExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            HttpClients.close(request, response);
            if (!success) {
                Streams.close(fileHolder);
            }
        }
    }

}
