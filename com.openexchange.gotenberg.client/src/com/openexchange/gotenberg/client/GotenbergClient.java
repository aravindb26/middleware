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

package com.openexchange.gotenberg.client;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.exception.OXException;
import com.openexchange.gotenberg.client.exception.GotenbergExceptionCodes;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;

/**
 * {@link GotenbergClient} - A client for communicating with a "gotenberg.dev"-server
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @see <a href="https://gotenberg.dev">More information</a>
 */
public class GotenbergClient {

    /**
     * The header for controlling the filename of the returned data
     */
    private static final String GOTENBERG_OUTPUT_FILENAME_HEADER = "Gotenberg-Output-Filename";

    /*
     * The API endpoint for converting HTML to PDF
     */
    private static final String BASE_CHROME_CONVERSION_ENDPOINT = "/forms/chromium/convert/html";

    /**
     * THe API endpoint for converting documents to PDF
     */
    private static final String BASE_LIBREOFFICE_CONVERSION_ENDPOINT = "/forms/libreoffice/convert";

    /*
     * The name of the HTML form field
     */
    private static final String HTML_FILE_NAME = "index.html";

    /*
     * The names of the form parameters
     */
    private static final String PDF_FORMAT = "pdfFormat";
    private static final String PAPER_WITH = "paperWidth";
    private static final String PAPER_HEIGHT = "paperHeight";
    private static final String MARGIN_TOP = "marginTop";
    private static final String MARGIN_BOTTOM = "marginBottom";
    private static final String MARGIN_LEFT = "marginLeft";
    private static final String MARGIN_RIGHT = "marginRight";

    /**
     * The fallback content-type
     */
    private static final String FALLBACK_CONTENT_TYPE = "application/pdf";

    private static final int BUFFER_SIZE = 65536;

    /**
     * A logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(GotenbergClient.class);

    //----------------------------------------------------------------------------------------------------

    /**
     * {@link Document} - A document which will be converted to PDF
     */
    public interface Document {

        /**
         * Gets the name
         *
         * @return The name
         */
        String getName();

        /**
         * Gets the contentType
         *
         * @return The contentType
         */
        String getContentType();

        /**
         * Gets the data
         *
         * @return The data
         */
        InputStream getData() throws IOException;

    }

    /**
     * {@link Document} - A HTML document which will be converted to PDF
     */
    public interface HTMLDocument extends Document {

        @Override
        default String getName() {
            return HTML_FILE_NAME;
        }

        @Override
        default String getContentType() {
            return ContentType.TEXT_HTML.getMimeType();

        }
    }

    static class DocumentContentBody extends AbstractContentBody {

        private final Document document;

        /**
         * Initializes a new {@link DocumentContentBody}.
         *
         * @param document The underlying document
         */
        public DocumentContentBody(Document document) {
            super(ContentType.create(document.getContentType()));
            this.document = document;
        }

        @Override
        public String getFilename() {
            return document.getName();
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            try (InputStream inputStream = document.getData()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                for (int read; (read = inputStream.read(buffer)) > 0;) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
        }

        @Override
        public String getTransferEncoding() {
            return MIME.ENC_BINARY;
        }

        @Override
        public long getContentLength() {
            return -1L;
        }

    }

    //----------------------------------------------------------------------------------------------------

    private final URI baseURI;
    private final ManagedHttpClient client;

    /**
     * Initializes a new {@link GotenbergClient}.
     *
     * @param baseURI The base URI used to connect to the "gotenberg" service
     * @param client The {@link ManagedHttpClient} to use for connecting to Gotenberg
     */
    public GotenbergClient(URI baseURI, ManagedHttpClient client) {
        this.baseURI = baseURI;
        this.client = client;
    }

    //----------------------------------------------------------------------------------------------------

    /**
     * Adds values of the given {@link PageProperties} to the entity
     *
     * @param entityBuilder The entity builder to add the values to
     * @param pageProperties The {@link PageProperties} to add
     */
    private static void addPageProperties(MultipartEntityBuilder entityBuilder, PageProperties pageProperties) {
        pageProperties.getPaperWidth().ifPresent(width -> entityBuilder.addTextBody(PAPER_WITH, width.toString()));
        pageProperties.getPaperHeight().ifPresent(height -> entityBuilder.addTextBody(PAPER_HEIGHT, height.toString()));
        pageProperties.getMarginTop().ifPresent(margin -> entityBuilder.addTextBody(MARGIN_TOP, margin.toString()));
        pageProperties.getMarginBottom().ifPresent(margin -> entityBuilder.addTextBody(MARGIN_BOTTOM, margin.toString()));
        pageProperties.getMarginLeft().ifPresent(margin -> entityBuilder.addTextBody(MARGIN_LEFT, margin.toString()));
        pageProperties.getMarginRight().ifPresent(margin -> entityBuilder.addTextBody(MARGIN_RIGHT, margin.toString()));
    }

    private static HttpPost createConversionRequest(String uri, Collection<Document> documents, ConversionOptions options, boolean merge) {
        StringBuilder parameterLogger = new StringBuilder();
        HttpPost request = new HttpPost(uri);
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();

        //Adding the PDF format if defined and not the default non "PDF-A" format
        if (options.getPdfFormat() != null && options.getPdfFormat() != PDFFormat.PDF) {
            entityBuilder.addTextBody(PDF_FORMAT, options.getPdfFormat().getName());
            parameterLogger.append("[").append(PDF_FORMAT).append("|").append(options.getPdfFormat().getName()).append("]");
        }

        //Adding page properties if present
        options.getPageProperties().ifPresent(properties -> addPageProperties(entityBuilder, properties));

        //Controlling the document's output filename, if present at all
        options.getOutputFilename().ifPresent(filename -> request.addHeader(GOTENBERG_OUTPUT_FILENAME_HEADER, filename));

        //Adding all the data to convert
        for (Document document : documents) {
            entityBuilder.addPart(document.getName(), new DocumentContentBody(document));
        }

        //merge to a single PDF if multiple documents are being converted
        if (merge) {
            entityBuilder.addTextBody("merge", Boolean.TRUE.toString());
        }

        request.setEntity(entityBuilder.build());
        LOG.debug("Created Gotenberg conversion request: {} - {}", request, parameterLogger);
        return request;
    }

    /**
     * Internal method to execute the HTTP request
     *
     * @param request The request to execute
     * @return The response
     * @throws OXException if an error is occurred
     */
    private HttpResponse executeRequest(HttpRequestBase request) throws OXException {
        boolean success = false;
        HttpResponse response = null;
        try {
            response = client.execute(request);
            assertResponse(response);
            LOG.debug("Successfully executed Gotenberg conversion request {}", request);
            success = true;
            return response;
        } catch (IOException e) {
            throw GotenbergExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            //Release resources in case of any error (incl. possible, not caught RuntimeExceptions)
            //Do not clean up in case of success, because the caller will close the resources if finally consumed
            if (!success) {
                HttpClients.close(request, response);
            }
        }
    }

    /**
     * Internal method to check the response from a Gotenberg server
     *
     * @param response The response to check
     * @throws OXException in case the response was not HTTP 200 OK
     */
    private void assertResponse(HttpResponse response) throws OXException {
        if (response.getStatusLine() == null) {
            throw GotenbergExceptionCodes.UNEXPECTED_ERROR.create("Unable to parse response code");
        }
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return;
        }

        final String msg = "(HTTP-Status %s) %s".formatted(I(response.getStatusLine().getStatusCode()), getErrorMessage(response));
        LOG.debug(msg);
        throw GotenbergExceptionCodes.REMOTE_SERVER_ERROR.create(msg);
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
     * @throws OXException due an error getting the InputStream from the given response or receiving an empty response
     */
    private InputStream getContent(HttpResponse response) throws OXException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw GotenbergExceptionCodes.UNEXPECTED_ERROR.create("Received an empty response");
        }
        try {
            return entity.getContent();
        } catch (UnsupportedOperationException e) {
            throw GotenbergExceptionCodes.UNEXPECTED_ERROR.create(e, "Unable to parse response: " + e.getMessage());
        } catch (IOException e) {
            throw GotenbergExceptionCodes.IO_ERROR.create(e, e.getMessage());
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

    private ThresholdFileHolder convert(String endpoint, Collection<Document> documents, ConversionOptions conversionOptions, boolean merge) throws OXException {
        ThresholdFileHolder fileHolder = null;
        boolean success = false;
        HttpRequestBase request = null;
        HttpResponse response = null;
        try {
            request = createConversionRequest(buildURI(endpoint), documents, conversionOptions, merge);
            response = executeRequest(request);
            String resultContentType = getContentType(response);
            if (Strings.isEmpty(resultContentType)) {
                /* Set the default content-type if not present in the response for some reason */
                resultContentType = FALLBACK_CONTENT_TYPE;
            }

            /* Store response data into file holder and return it */
            fileHolder = new ThresholdFileHolder();
            fileHolder.setContentType(resultContentType);
            fileHolder.setName(getFileName(response));
            try (InputStream responseContentStream = getContent(response)) {
                fileHolder = fileHolder.write(responseContentStream);
            }
            success = true;
            return fileHolder;
        } catch (IOException e) {
            throw GotenbergExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            HttpClients.close(request, response);
            if (!success) {
                Streams.close(fileHolder);
            }
        }
    }

    //----------------------------------------------------------------------------------------------------

    private String buildURI(String endpoint) throws OXException {
        try {
            return new URIBuilder(baseURI).setPath(endpoint).build().toString();
        } catch (URISyntaxException e) {
            throw GotenbergExceptionCodes.INVALID_URL.create(e, baseURI, e.getMessage());
        }
    }

    /**
     * Converts a HTML document and a collection of additional documents into PDF and returns it as a {@link ThresholdFileHolder}
     * <p>
     * The collection of documents is meant to be additional files, like images, fonts, stylesheets, and so on, referenced in the HTML body
     * </p>
     *
     * @param conversionOptions The conversion options which defines details of the conversion process
     * @param html The HTML content as {@link Document}
     * @param documents The documents to convert into PDF
     * @return The converted PDF as {@link ConversionResult}
     * @throws OXException if an error is occurred
     */
    public ThresholdFileHolder getHTMLConversionResult(ConversionOptions conversionOptions, Document html, Collection<Document> documents) throws OXException {
        /* convert a single HTML document without inline references, or with additional referenced inline documents */
        Collection<Document> allDocuments;
        if (null == documents || documents.isEmpty()) {
            allDocuments = Collections.singletonList(html);
        } else {
            allDocuments = new ArrayList<>(documents.size() + 1);
            allDocuments.add(html);
            allDocuments.addAll(documents);
        }
        return convert(BASE_CHROME_CONVERSION_ENDPOINT, allDocuments, conversionOptions, false);
    }

    /**
     * Converts a set of office documents into PDF and returns the result as {@link ThresholdFileHolder}
     *
     * @param conversionOptions The conversion options which defines details of the conversion process
     * @param documents A collection of office documents to convert into PDF
     * @return The converted PDF as {@link ThresholdFileHolder}
     * @throws OXException if an error is occurred
     */
    public ThresholdFileHolder getDocumentConversionResult(ConversionOptions conversionOptions, Collection<Document> documents) throws OXException {
        return convert(BASE_LIBREOFFICE_CONVERSION_ENDPOINT, documents, conversionOptions, 1 < documents.size());
    }

}
