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

package com.openexchange.ajax.requesthandler.responseRenderers;

import static com.openexchange.java.Autoboxing.b;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;
import javax.servlet.http.sim.SimHttpServletRequest;
import javax.servlet.http.sim.SimHttpServletResponse;
import javax.servlet.sim.ByteArrayServletOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.container.ByteArrayFileHolder;
import com.openexchange.ajax.container.FileHolder;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.cache.CachedResource;
import com.openexchange.ajax.requesthandler.cache.ResourceCache;
import com.openexchange.ajax.requesthandler.cache.ResourceCaches;
import com.openexchange.ajax.requesthandler.responseRenderers.FileResponseRendererTools.Delivery;
import com.openexchange.ajax.requesthandler.responseRenderers.FileResponseRendererTools.Disposition;
import com.openexchange.config.SimConfigurationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.SimLeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.html.HtmlService;
import com.openexchange.html.SimHtmlService;
import com.openexchange.imagetransformation.BasicTransformedImage;
import com.openexchange.imagetransformation.ImageTransformationService;
import com.openexchange.imagetransformation.ImageTransformations;
import com.openexchange.imagetransformation.ScaleType;
import com.openexchange.imagetransformation.TransformedImage;
import com.openexchange.imagetransformation.java.impl.JavaImageTransformationProvider;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.tools.image.WrappingImageTransformationService;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.SimServerSession;
import com.openexchange.tools.strings.BasicTypesStringParser;
import com.openexchange.tools.strings.StringParser;

/**
 * {@link FileResponseRendererTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class FileResponseRendererTest {

    private SimConfigurationService simConfigurationService;

    /**
     * Initializes a new {@link FileResponseRendererTest}.
     */
    public FileResponseRendererTest() {
        super();
    }

    @BeforeEach
    public void setUp() throws Exception {
        ServerServiceRegistry.getInstance().addService(HtmlService.class, new SimHtmlService());
        simConfigurationService = new SimConfigurationService();
        SimLeanConfigurationService simLeanConfigurationService = new SimLeanConfigurationService(simConfigurationService);
        ServerServiceRegistry.getInstance().addService(LeanConfigurationService.class, simLeanConfigurationService);
        simConfigurationService.stringProperties.put("UPLOAD_DIRECTORY", "/tmp/");
        AJAXConfig.init();
    }

    @AfterEach
    public void tearDown() {
        ServerServiceRegistry.getInstance().removeService(HtmlService.class);
    }

    public static Stream<Boolean> propertySetting() {
        return Stream.of(Boolean.TRUE,
                         Boolean.FALSE);
    }

    @ParameterizedTest
    @MethodSource("propertySetting")
    public void testProperContentLength(Boolean allowContentDispositionInline) {
        setContentDispositionInlineConfig(allowContentDispositionInline);
        try {
            final String html = "foo\n" + "<object/data=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgiWFNTIFNjaHdhY2hzdGVsbGUiKTwvc2NyaXB0Pg==\"></object>\n" + "bar";
            final byte[] bytes = html.getBytes("ISO-8859-1");
            final int length = bytes.length;
            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(bytes, "text/html; charset=ISO-8859-1", Delivery.view, Disposition.inline, "document.html");

            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();

            final AJAXRequestData requestData = new AJAXRequestData();
            final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            final SimHttpServletRequest req = new SimHttpServletRequest();
            final SimHttpServletResponse resp = new SimHttpServletResponse();
            final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
            resp.setOutputStream(servletOutputStream);
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

            final long contentLength = resp.getContentLength();
            assertTrue(contentLength > 0, "Unexpected Content-Length: " + contentLength);
            if (b(allowContentDispositionInline)) {
                assertTrue(length > contentLength, "Unexpected Content-Length: " + contentLength + ", but should be less than " + length);
            }
            final int size = servletOutputStream.size();
            assertEquals(size, contentLength, "Unexpected Content-Length.");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testApplicationHtml_Bug35512() {
        setContentDispositionInlineConfig(Boolean.TRUE);
        try {
            String html = "foo\n" + "<object/data=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgiWFNTIFNjaHdhY2hzdGVsbGUiKTwvc2NyaXB0Pg==\"></object>\n" + "bar";
            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(html.getBytes(), "application/xhtml+xml", Delivery.view, Disposition.inline, "evil.html");

            AJAXRequestData requestData = new AJAXRequestData();
            {
                requestData.setSession(new SimServerSession(1, 1));
                requestData.putParameter("width", "10");
                requestData.putParameter("height", "10");
            }
            AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            result.setExpires(Tools.getDefaultImageExpiry());
            SimHttpServletRequest req = new SimHttpServletRequest();
            SimHttpServletResponse resp = new SimHttpServletResponse();
            ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
            resp.setOutputStream(servletOutputStream);
            FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

            String s = new String(servletOutputStream.toByteArray());
            assertTrue(s.indexOf("<object/data") < 0, "HTML content not sanitized: " + s);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @ParameterizedTest
    @MethodSource("propertySetting")
    public void testMaxAgeHeader_Bug33441(Boolean allowContentDispositionInline) {
        setContentDispositionInlineConfig(allowContentDispositionInline);
        try {
            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("28082.jpg", "image/jpeg", Delivery.view, Disposition.inline, "28082.jpg");
            final AJAXRequestData requestData = new AJAXRequestData();
            {
                requestData.setSession(new SimServerSession(1, 1));
                requestData.putParameter("width", "10");
                requestData.putParameter("height", "10");
            }
            final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            result.setExpires(Tools.getDefaultImageExpiry());
            final SimHttpServletRequest req = new SimHttpServletRequest();
            final SimHttpServletResponse resp = new SimHttpServletResponse();
            final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
            resp.setOutputStream(servletOutputStream);
            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
            fileResponseRenderer.setScaler(new TestableImageTransformationService(IOUtils.toByteArray(fileHolder.getStream()), ImageTransformations.HIGH_EXPENSE));
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

            assertTrue(resp.containsHeader("cache-control"), "HTTP header \"cache-control\" is missing");

            String sCacheControl = resp.getHeaders().get("cache-control");
            assertTrue(Strings.isNotEmpty(sCacheControl), "HTTP header \"cache-control\" is missing");

            if (b(allowContentDispositionInline)) {
                assertTrue(sCacheControl.indexOf("max-age=3600") > 0, "Invalid HTTP header \"cache-control\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    @Test
    public void testZeroByteTransformation_Bug28429() {
        try {
            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("28429.jpg", "image/jpeg", Delivery.view, Disposition.inline, "28429.jpg");
            final AJAXRequestData requestData = new AJAXRequestData();
            {
                requestData.setSession(new SimServerSession(1, 1));
                requestData.putParameter("width", "10");
                requestData.putParameter("height", "10");
            }
            final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            final SimHttpServletRequest req = new SimHttpServletRequest();
            final SimHttpServletResponse resp = new SimHttpServletResponse();
            {
                final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
                resp.setOutputStream(servletOutputStream);
            }
            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
            fileResponseRenderer.setScaler(new TestableImageTransformationService(IOUtils.toByteArray(fileHolder.getStream()), ImageTransformations.HIGH_EXPENSE));
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
            assertFalse(resp.getStatus() >= 400, "Got an error status: " + resp.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testContentTypeByFileName_Bug31648() {
        try {
            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("31648.png", "application/binary", Delivery.view, Disposition.inline, "31648.png");
            final AJAXRequestData requestData = new AJAXRequestData();
            {
                requestData.setSession(new SimServerSession(1, 1));
                requestData.putParameter("width", "10");
                requestData.putParameter("height", "10");
            }
            final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            final SimHttpServletRequest req = new SimHttpServletRequest();
            final SimHttpServletResponse resp = new SimHttpServletResponse();
            final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
            resp.setOutputStream(servletOutputStream);

            MimeType2ExtMap.addMimeType("image/png", "png");

            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
            fileResponseRenderer.setScaler(new TestableImageTransformationService(IOUtils.toByteArray(fileHolder.getStream()), ImageTransformations.HIGH_EXPENSE));
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
            final String expectedContentType = "image/png";
            final String currentContentType = resp.getContentType();
            assertEquals(expectedContentType, currentContentType, "Unexpected content-type.");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testContentTypeByFileName_Bug67097() {
        try {
            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("67097.png", "image/png", Delivery.view, Disposition.inline, "oxout.html");
            final AJAXRequestData requestData = new AJAXRequestData();
            {
                requestData.setSession(new SimServerSession(1, 1));
            }
            final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            final SimHttpServletRequest req = new SimHttpServletRequest();
            final SimHttpServletResponse resp = new SimHttpServletResponse();
            final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
            resp.setOutputStream(servletOutputStream);

            MimeType2ExtMap.addMimeType("image/png", "png");

            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
            fileResponseRenderer.setScaler(new TestableImageTransformationService(IOUtils.toByteArray(fileHolder.getStream()), ImageTransformations.HIGH_EXPENSE));
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
            final String expectedContentType = "image/png";
            final String currentContentType = resp.getContentType();
            assertEquals(expectedContentType, currentContentType, "Unexpected content-type.");
            final String expectedFileName = "oxout.png";
            final String currentContentDisposition = resp.getHeader("Content-Disposition");
            assertTrue(currentContentDisposition.indexOf(expectedFileName) > 0, "Unexpected content-disposition: " + currentContentDisposition);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testRangeHeader_Bug27394() {
        try {
            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("26926_27394.pdf", "application/pdf", Delivery.view, Disposition.inline, "26926_27394.pdf");
            final AJAXRequestData requestData = new AJAXRequestData();
            {
                requestData.setSession(new SimServerSession(1, 1));
                requestData.putParameter("width", "10");
                requestData.putParameter("height", "10");
            }
            final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            final SimHttpServletRequest req = new SimHttpServletRequest();
            {
                req.setHeader("Range", "bytes=0-50");
            }
            final SimHttpServletResponse resp = new SimHttpServletResponse();
            final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
            resp.setOutputStream(servletOutputStream);
            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
            fileResponseRenderer.setScaler(new TestableImageTransformationService(IOUtils.toByteArray(fileHolder.getStream()), ImageTransformations.HIGH_EXPENSE));
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
            assertTrue(resp.containsHeader("accept-ranges"), "HTTP header \"accept-ranges\" is missing");
            assertTrue(resp.containsHeader("content-range"), "HTTP header \"content-range\" is missing");
            assertEquals("bytes", resp.getHeaders().get("accept-ranges"));
            assertEquals("bytes 0-50/3852226", resp.getHeaders().get("content-range"));
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void test404_Bug26848() {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();

        // test with null file
        AJAXRequestData requestData = new AJAXRequestData();
        AJAXRequestResult result = new AJAXRequestResult();
        SimHttpServletRequest req = new SimHttpServletRequest();
        SimHttpServletResponse resp = new SimHttpServletResponse();
        ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.write(requestData, result, req, resp);
        assertEquals(404, resp.getStatus(), "Wrong status code");

        // test with empty filename
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(new byte[0], "application/octet-stream", Delivery.download, Disposition.attachment, "");

        resp = new SimHttpServletResponse();
        servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);

        result = new AJAXRequestResult(fileHolder, "file");
        fileResponseRenderer.write(requestData, result, req, resp);
        assertEquals(404, resp.getStatus(), "Wrong status code");

        // test with empty content type and empty filename
        fileHolder.setContentType("");

        resp = new SimHttpServletResponse();
        servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);

        result = new AJAXRequestResult(fileHolder, "file");
        fileResponseRenderer.write(requestData, result, req, resp);
        assertEquals(404, resp.getStatus(), "Wrong status code");
    }

    @Test
    public void testXSSVuln_Bug26244() throws IOException {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();

        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("xss_utf16.html");

        final AJAXRequestData requestData = new AJAXRequestData();
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_type", "text/html; charset=UTF-16");
        req.setParameter("content_disposition", "inline");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        final String expectedCT = "text/html";
        assertEquals(expectedCT, resp.getContentType(), "Wrong Content-Type");
    }

    @Test
    public void testXSSVuln_Bug29147() throws IOException {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("29147.svg", "image/svg+xml", Delivery.view, Disposition.inline, "29147.svg");
        final AJAXRequestData requestData = new AJAXRequestData();
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        assertTrue(resp.getStatus() == 403, "Wrong status code");
        assertTrue(resp.getStatusMessage().equals("Denied to output possibly harmful content"), "Wrong status message");
    }

    @Test
    public void testXSSVuln_Bug26373_view() throws IOException {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();

        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("26237.html");

        final AJAXRequestData requestData = new AJAXRequestData();
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_disposition", "view");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

        assertEquals("application/octet-stream", resp.getContentType(), "Wrong Content-Type");
    }

    
    public void testXSSVuln_Bug26237_inline() throws IOException {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();

        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("26237.html");

        final AJAXRequestData requestData = new AJAXRequestData();
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_disposition", "inline");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

        assertEquals("application/octet-stream", resp.getContentType(), "Wrong Content-Type");
    }

    @Test
    public void testXSSVuln_Bug26243() throws IOException {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();

        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("31714.jpg");

        final AJAXRequestData requestData = new AJAXRequestData();
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_disposition", "inline");
        req.setParameter("content_type", "text/html%0d%0a%0d%0a<script>alert(\"XSS\")</script>");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        final String expectedContentType = "image/jpeg";
        assertEquals(expectedContentType, resp.getContentType(), "Wrong Content-Type");
    }

    @Test
    public void testBug31714() throws IOException {
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("31714.jpg", "image/jpeg", Delivery.view, Disposition.inline, "31714.jpg");

        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.putParameter("width", "1000000");
        requestData.putParameter("height", "1000000");
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        fileResponseRenderer.setScaler(new TestableImageTransformationService(IOUtils.toByteArray(fileHolder.getStream()), ImageTransformations.HIGH_EXPENSE));
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        assertEquals(500, resp.getStatus(), "Unexpected status code.");
    }

    @Test
    public void testBug26995() throws IOException {
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("26995", "application/octet-stream", Delivery.view, Disposition.attachment, "26995");

        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.setScaler(new TestableImageTransformationService(IOUtils.toByteArray(fileHolder.getStream()), ImageTransformations.HIGH_EXPENSE));
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        final String expectedContentType = "image/jpeg";
        assertEquals(expectedContentType, resp.getContentType(), "Wrong Content-Type");
    }

    @Test
    public void testBug25133() throws IOException {
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("25133.eml", null, Delivery.download, Disposition.attachment, "25133.eml");

        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.putParameter("transformationNeeded", "true");
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.setScaler(new WrappingImageTransformationService(new JavaImageTransformationProvider()));
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        assertNotNull(resp.getContentType(), "Header content-type not found");
        assertEquals("application/octet-stream", resp.getContentType(), "Wrong Content-Type");
    }

    @Test
    public void testBug48559() throws IOException {
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("48559.svg", "application/xml", Delivery.view, Disposition.inline, "fake.pdf");

        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.setScaler(new WrappingImageTransformationService(new JavaImageTransformationProvider()));
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        assertNotNull(resp.getContentType(), "Header content-type not found");
        assertTrue(resp.getContentType().startsWith("application/xml"), "Wrong Content-Type");
        assertTrue(resp.getHeader("content-disposition").startsWith("attachment"), "Wrong content-disposition");
    }

    @Test
    public void testTikaShouldDetectCorrectContenType_Bug26153() throws IOException {
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("Rotate_90CW.jpg", "application/octet-stream", Delivery.view, Disposition.inline, "Rotate");

        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.setScaler(new WrappingImageTransformationService(new JavaImageTransformationProvider()));
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        requestData.setSession(new SimServerSession(1, 1));
        final String expectedContentType = "image/jpeg";
        assertEquals(expectedContentType, resp.getContentType(), "Wrong Content-Type");
    }

    @Test
    public void testUnquoteContentTypeAndDisposition_Bug26153() throws IOException {
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("Rotate_90CW.jpg", "\"image/jpeg\"", Delivery.view, Disposition.inline, "Rotate_90CW.jpg");
        fileHolder.setDisposition("\"inline\"");
        fileHolder.setDelivery("\"view\"");
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.setScaler(new WrappingImageTransformationService(new JavaImageTransformationProvider()));
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        requestData.setSession(new SimServerSession(1, 1));
        assertEquals("image/jpeg", resp.getContentType(), "Wrong Content-Type");
        assertNotNull(resp.getHeaders().get("content-disposition"), "Header content-disposition not present");
        String contentDisposition = resp.getHeaders().get("content-disposition");
        contentDisposition = contentDisposition.substring(0, contentDisposition.indexOf(';'));
        final String expectedContentDisposition = "inline";
        assertEquals(expectedContentDisposition, contentDisposition, "Wrong Content-Disposition");
    }

    @Test
    public void testSanitizingUrlParameter() throws IOException {
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("XSSFile.html", null, Delivery.view, Disposition.inline, "XSSFile.html");
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_type", "text/html&amp;lt;script&amp;gt;x=/xss/;alert&amp;#40;x.source&amp;#41;&amp;lt;/script&amp;gt;");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.setScaler(new WrappingImageTransformationService(new JavaImageTransformationProvider()));
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        requestData.setSession(new SimServerSession(1, 1));
        assertTrue(resp.getContentType().startsWith("text/html"), "Wrong Content-Type");
    }

    @Test
    public void testSanitizingFileArguments() throws Exception {
        ByteArrayFileHolder fileHolder = new ByteArrayFileHolder(new byte[] { 1, 1, 1, 1 });
        fileHolder.setContentType("<pwn/ny");
        fileHolder.setDelivery(Delivery.view.name());
        fileHolder.setDisposition(Disposition.inline.name());
        fileHolder.setName("<svg onload=alert(1)>");
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        resp.setCharacterEncoding("UTF-8");
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.setScaler(new WrappingImageTransformationService(new JavaImageTransformationProvider()));
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

        String response = new String(servletOutputStream.toByteArray(), "UTF-8");
        assertFalse(response.indexOf("<svg onload=alert") >= 0, "Response contains malicious content");
    }

    @Test
    public void testContentLengthMailAttachments_Bug26926() {
        String testDataDir = AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR);
        try {
            InputStream is = new FileInputStream(new File(testDataDir + "26926_27394.pdf"));
            FileHolder fileHolder = new FileHolder(is, 3852226, "application/pdf", "26926_27394.pdf");
            fileHolder.setDelivery("download");
            final AJAXRequestData requestData = new AJAXRequestData();
            requestData.putParameter("delivery", "download");
            requestData.setAction("attachment");
            requestData.setSession(new SimServerSession(1, 1));
            final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            final SimHttpServletRequest req = new SimHttpServletRequest();
            final SimHttpServletResponse resp = new SimHttpServletResponse();
            {
                final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
                resp.setOutputStream(servletOutputStream);
            }
            requestData.setHttpServletResponse(resp);
            requestData.setHttpServletRequest(req);
            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
            final long contentLength = resp.getContentLength();
            assertTrue(contentLength == 3852226, "Content-Length should be 3852226, but is " + contentLength);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testChunkRead() {
        try {
            byte[] bytes = FileResponseRendererTools.newByteArray(2048);
            bytes[256] = 120;

            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(bytes, "application/octet-stream", Delivery.download, Disposition.attachment, "bin.data");

            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();

            {
                final AJAXRequestData requestData = new AJAXRequestData();
                final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
                final SimHttpServletRequest req = new SimHttpServletRequest();
                req.setParameter("off", "0");
                req.setParameter("len", "256");
                final SimHttpServletResponse resp = new SimHttpServletResponse();
                final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
                resp.setOutputStream(servletOutputStream);
                fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

                final byte[] writtenBytes = servletOutputStream.toByteArray();
                assertEquals(256, writtenBytes.length, "Unexpected number of written bytes.");
            }

            {
                final AJAXRequestData requestData = new AJAXRequestData();
                final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
                final SimHttpServletRequest req = new SimHttpServletRequest();
                req.setParameter("off", "256");
                req.setParameter("len", "256");
                final SimHttpServletResponse resp = new SimHttpServletResponse();
                final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
                resp.setOutputStream(servletOutputStream);
                fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

                final byte[] writtenBytes = servletOutputStream.toByteArray();
                assertEquals(256, writtenBytes.length, "Unexpected number of written bytes.");
                assertEquals(120, writtenBytes[0], "Unexpected starting byte.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testChunkReadOutOfRange() {
        try {
            byte[] bytes = FileResponseRendererTools.newByteArray(2048);

            ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(bytes, "application/octet-stream", Delivery.download, Disposition.attachment, "bin.data");

            final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();

            final AJAXRequestData requestData = new AJAXRequestData();
            final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
            final SimHttpServletRequest req = new SimHttpServletRequest();
            req.setParameter("off", "2049");
            req.setParameter("len", "256");
            final SimHttpServletResponse resp = new SimHttpServletResponse();
            final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
            resp.setOutputStream(servletOutputStream);
            fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

            assertEquals(416, resp.getStatus(), "Unexpected status code.");
            assertEquals("bytes */2048", resp.getHeaders().get("content-range"), "Unexpected 'Content-Range' header.");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testResourceCacheIsDisabled() {
        byte[] bytes = FileResponseRendererTools.newByteArray(2048);

        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(bytes, "image/jpeg", Delivery.view, Disposition.inline, "someimage.jpg");

        final TestableResourceCache resourceCache = new TestableResourceCache(false);
        ResourceCaches.setResourceCache(resourceCache);
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        fileResponseRenderer.setScaler(new TestableImageTransformationService(bytes, ImageTransformations.HIGH_EXPENSE));
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        requestData.putParameter("width", "80");
        requestData.putParameter("height", "80");
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        result.setHeader("ETag", "1323jjlksldfsdkfms");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        assertEquals(1, resourceCache.callsToIsEnabledFor, "isEnabled() not called");
        assertEquals(0, resourceCache.callsToGet, "get() called");
        assertEquals(0, resourceCache.callsToSave, "save() called");
    }

    @Test
    public void testResourceCacheIsDisabled2() {
        byte[] bytes = FileResponseRendererTools.newByteArray(2048);

        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(bytes, "image/jpeg", Delivery.view, Disposition.inline, "someimage.jpg");

        ServerServiceRegistry.getInstance().addService(StringParser.class, new BasicTypesStringParser());
        final TestableResourceCache resourceCache = new TestableResourceCache(true);
        ResourceCaches.setResourceCache(resourceCache);
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        fileResponseRenderer.setScaler(new TestableImageTransformationService(bytes, ImageTransformations.HIGH_EXPENSE));
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        requestData.putParameter("width", "10");
        requestData.putParameter("height", "10");
        requestData.putParameter("cache", "false");
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        assertEquals(0, resourceCache.callsToGet, "get() not called");
        assertEquals(0, resourceCache.callsToSave, "save() called");
    }

    @Test
    public void testNoCachingOnCheapTransformations() {
        byte[] bytes = FileResponseRendererTools.newByteArray(2048);

        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(bytes, "image/jpeg", Delivery.view, Disposition.inline, "someimage.jpg");

        final TestableResourceCache resourceCache = new TestableResourceCache(true);
        ResourceCaches.setResourceCache(resourceCache);
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        fileResponseRenderer.setScaler(new TestableImageTransformationService(bytes, ImageTransformations.LOW_EXPENSE));
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        requestData.putParameter("width", "80");
        requestData.putParameter("height", "80");
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        result.setHeader("ETag", "1323jjlksldfsdkfms");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        assertEquals(1, resourceCache.callsToGet, "get() not called");
        assertEquals(0, resourceCache.callsToSave, "save() called");
    }

    @Test
    public void testCachingOnExpensiveTransformations() {
        byte[] bytes = FileResponseRendererTools.newByteArray(2048);

        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder(bytes, "image/jpeg", Delivery.view, Disposition.inline, "someimage.jpg");

        ServerServiceRegistry.getInstance().addService(StringParser.class, new BasicTypesStringParser());
        final TestableResourceCache resourceCache = new TestableResourceCache(true);
        ResourceCaches.setResourceCache(resourceCache);
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        fileResponseRenderer.setScaler(new TestableImageTransformationService(bytes, ImageTransformations.HIGH_EXPENSE));
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.putParameter("width", "10");
        requestData.putParameter("height", "10");
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        result.setHeader("ETag", "1323jjlksldfsdkfms");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        assertEquals(1, resourceCache.callsToGet, "get() not called");
        assertEquals(1, resourceCache.callsToSave, "save() called");
    }

    @Test
    public void testXSSVuln_Bug49159() throws IOException {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("ox.html.txt", "text/plain", Delivery.view, Disposition.inline, "ox.html.txt");
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.putParameter("content_type", "text/html");
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_type", "text/html");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        final String expectedCT = "text/plain";
        assertTrue(resp.getContentType().startsWith(expectedCT), "Wrong Content-Type");
    }

    @Test
    public void testXSSVuln_Bug58742() throws IOException {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("html-xml", "application/xml", Delivery.view, Disposition.inline, "html-xml");
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.putParameter("content_disposition", "inline");
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_disposition", "inline");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        final String expectedDisposition = "attachment";
        assertTrue(resp.getHeader("Content-Disposition").startsWith(expectedDisposition), "Wrong Content-Type");
    }

    @Test
    public void testContentTypeXJavaScriptView_MWB70() throws IOException {
        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("MWB70.html", "text/x-javascript", Delivery.view, Disposition.inline, "oxout.html");
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.putParameter("content_type", "text/x-javascript");
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");
        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_type", "text/x-javascript");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);

        final int expectedStatus = 403;
        final int currentStatus = resp.getStatus();
        assertEquals(expectedStatus, currentStatus, "Unexpected status code");
        final String expectedContentType = null;
        final String currentContentType = resp.getContentType();
        assertEquals(expectedContentType, currentContentType, "Unexpected content-type");
    }

    @ParameterizedTest
    @MethodSource("propertySetting")
    public void testViewAndInline(Boolean allowContentDispositionInline) throws IOException {
        setContentDispositionInlineConfig(allowContentDispositionInline);
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("Rotate_90CW.jpg", "image/jpeg", Delivery.download, Disposition.attachment, "Rotate");

        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_disposition", "inline");
        req.setParameter("delivery", "view");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        requestData.setSession(new SimServerSession(1, 1));
        final String expectedContentDisposition = b(allowContentDispositionInline) ? "inline" : "attachment";
        final String expectedContentType = b(allowContentDispositionInline) ? "image/jpeg" : "application/octet-stream";
        assertEquals(expectedContentType, resp.getContentType(), "Wrong Content-Type");
        String contentDispo = resp.getHeader("content-disposition");
        assertTrue(contentDispo.startsWith(expectedContentDisposition), "Wrong content-disposition: " + contentDispo);
    }
    
    @ParameterizedTest
    @MethodSource("propertySetting")
    public void testView(Boolean allowContentDispositionInline) throws IOException {
        setContentDispositionInlineConfig(allowContentDispositionInline);
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("Rotate_90CW.jpg", "image/jpeg", Delivery.download, Disposition.attachment, "Rotate");

        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("delivery", "view");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        requestData.setSession(new SimServerSession(1, 1));
        final String expectedContentDisposition = b(allowContentDispositionInline) ? "inline" : "attachment";
        final String expectedContentType = b(allowContentDispositionInline) ? "image/jpeg" : "application/octet-stream";
        assertEquals(expectedContentType, resp.getContentType(), "Wrong Content-Type");
        String contentDispo = resp.getHeader("content-disposition");
        assertTrue(contentDispo.startsWith(expectedContentDisposition), "Wrong content-disposition: " + contentDispo);
    }
    
    @ParameterizedTest
    @MethodSource("propertySetting")
    public void testInline(Boolean allowContentDispositionInline) throws IOException {
        setContentDispositionInlineConfig(allowContentDispositionInline);
        ByteArrayFileHolder fileHolder = FileResponseRendererTools.getFileHolder("Rotate_90CW.jpg", "image/jpeg", Delivery.download, Disposition.attachment, "Rotate");

        final FileResponseRenderer fileResponseRenderer = new FileResponseRenderer();
        final AJAXRequestData requestData = new AJAXRequestData();
        requestData.setSession(new SimServerSession(1, 1));
        final AJAXRequestResult result = new AJAXRequestResult(fileHolder, "file");

        final SimHttpServletRequest req = new SimHttpServletRequest();
        req.setParameter("content_disposition", "inline");
        final SimHttpServletResponse resp = new SimHttpServletResponse();
        final ByteArrayServletOutputStream servletOutputStream = new ByteArrayServletOutputStream();
        resp.setOutputStream(servletOutputStream);
        fileResponseRenderer.writeFileHolder(fileHolder, requestData, result, req, resp);
        requestData.setSession(new SimServerSession(1, 1));
        final String expectedContentDisposition = b(allowContentDispositionInline) ? "inline" : "attachment";
        final String expectedContentType = "application/octet-stream";
        assertEquals(expectedContentType, resp.getContentType(), "Wrong Content-Type");
        String contentDispo = resp.getHeader("content-disposition");
        assertTrue(contentDispo.startsWith(expectedContentDisposition), "Wrong content-disposition: " + contentDispo);
    }

    
    
    private void setContentDispositionInlineConfig(Boolean allowContentDispositionInline) {
        simConfigurationService.stringProperties.put(FileResponseRendererProperties.ALLOW_CONTENT_DISPOSITION_INLINE.getFQPropertyName(), allowContentDispositionInline.toString());
    }

    private static final class TestableResourceCache implements ResourceCache {

        private final boolean isEnabled;

        int callsToIsEnabledFor = 0;

        int callsToGet = 0;

        int callsToSave = 0;

        public TestableResourceCache(final boolean isEnabled) {
            super();
            this.isEnabled = isEnabled;
        }

        @Override
        public boolean isEnabledFor(int contextId, int userId) throws OXException {
            callsToIsEnabledFor++;
            return isEnabled;
        }

        @Override
        public boolean save(String id, CachedResource resource, int userId, int contextId) throws OXException {
            callsToSave++;
            return false;
        }

        @Override
        public CachedResource get(String id, int userId, int contextId) throws OXException {
            callsToGet++;
            return null;
        }

        @Override
        public void remove(int userId, int contextId) throws OXException {

        }

        @Override
        public void removeAlikes(String id, int userId, int contextId) throws OXException {

        }

        @Override
        public void clearFor(int contextId) throws OXException {

        }

        @Override
        public boolean exists(String id, int userId, int contextId) throws OXException {
            return false;
        }
    }

    private static final class TestableImageTransformations implements ImageTransformations {

        final byte[] imageData;

        final int expenses;

        public TestableImageTransformations(byte[] imageData, int expenses) {
            super();
            this.imageData = imageData;
            this.expenses = expenses;
        }

        @Override
        public ImageTransformations rotate() {
            return this;
        }

        @Override
        public ImageTransformations scale(int maxWidth, int maxHeight, ScaleType scaleType) {
            return this;
        }

        @Override
        public ImageTransformations scale(int maxWidth, int maxHeight, ScaleType scaleType, boolean shrinkOnly) {
            return this;
        }

        @Override
        public ImageTransformations crop(int x, int y, int width, int height) {
            return this;
        }

        @Override
        public ImageTransformations compress() {
            return this;
        }

        @Override
        public BufferedImage getImage() throws IOException {
            return null;
        }

        @Override
        public byte[] getBytes(String formatName) throws IOException {
            return null;
        }

        @Override
        public InputStream getInputStream(String formatName) throws IOException {
            return null;
        }

        @Override
        public BasicTransformedImage getTransformedImage(String formatName) throws IOException {
            return new BasicTransformedImage() {

                @Override
                public int getTransformationExpenses() {
                    return expenses;
                }

                @Override
                public long getSize() {
                    return 0;
                }

                @Override
                public byte[] getImageData() {
                    return imageData;
                }

                @Override
                public InputStream getImageStream() throws OXException {
                    return new ByteArrayInputStream(imageData);
                }

                @Override
                public IFileHolder getImageFile() {
                    return null;
                }

                @Override
                public String getFormatName() {
                    return null;
                }

                @Override
                public void close() {
                    // Nothing
                }
            };
        }

        @Override
        public TransformedImage getFullTransformedImage(String formatName) throws IOException {
            return new TransformedImage() {

                @Override
                public int getWidth() {
                    return 0;
                }

                @Override
                public int getTransformationExpenses() {
                    return expenses;
                }

                @Override
                public long getSize() {
                    return 0;
                }

                @Override
                public byte[] getMD5() {
                    return null;
                }

                @Override
                public byte[] getImageData() {
                    return imageData;
                }

                @Override
                public InputStream getImageStream() throws OXException {
                    return new ByteArrayInputStream(imageData);
                }

                @Override
                public IFileHolder getImageFile() {
                    return null;
                }

                @Override
                public int getHeight() {
                    return 0;
                }

                @Override
                public String getFormatName() {
                    return null;
                }

                @Override
                public void close() {
                    // Nothing
                }
            };
        }

        @Override
        public Optional<TransformedImage> optFullTransformedImage(String formatName) throws IOException {
            return Optional.of(new TransformedImage() {

                @Override
                public int getWidth() {
                    return 0;
                }

                @Override
                public int getTransformationExpenses() {
                    return expenses;
                }

                @Override
                public long getSize() {
                    return 0;
                }

                @Override
                public byte[] getMD5() {
                    return null;
                }

                @Override
                public byte[] getImageData() {
                    return imageData;
                }

                @Override
                public InputStream getImageStream() throws OXException {
                    return new ByteArrayInputStream(imageData);
                }

                @Override
                public IFileHolder getImageFile() {
                    return null;
                }

                @Override
                public int getHeight() {
                    return 0;
                }

                @Override
                public String getFormatName() {
                    return null;
                }

                @Override
                public void close() {
                    // Nothing
                }
            });
        }

        @Override
        public Optional<BasicTransformedImage> optTransformedImage(String formatName) throws IOException {
            return Optional.of(new BasicTransformedImage() {

                @Override
                public int getTransformationExpenses() {
                    return expenses;
                }

                @Override
                public long getSize() {
                    return 0;
                }

                @Override
                public byte[] getImageData() {
                    return imageData;
                }

                @Override
                public InputStream getImageStream() throws OXException {
                    return new ByteArrayInputStream(imageData);
                }

                @Override
                public IFileHolder getImageFile() {
                    return null;
                }

                @Override
                public String getFormatName() {
                    return null;
                }

                @Override
                public void close() {
                    // Nothing
                }
            });
        }
    }

    private static final class TestableImageTransformationService implements ImageTransformationService {

        private final byte[] imageData;
        private final int expenses;

        TestableImageTransformationService(byte[] imageData, int expenses) {
            super();
            this.imageData = imageData;
            this.expenses = expenses;
        }

        @Override
        public ImageTransformations transfom(BufferedImage sourceImage) {
            return new TestableImageTransformations(imageData, expenses);
        }

        @Override
        public ImageTransformations transfom(BufferedImage sourceImage, Object source) {
            return new TestableImageTransformations(imageData, expenses);
        }

        @Override
        public ImageTransformations transfom(InputStream imageStream) throws IOException {
            return new TestableImageTransformations(imageData, expenses);
        }

        @Override
        public ImageTransformations transfom(InputStream imageStream, Object source) throws IOException {
            return new TestableImageTransformations(imageData, expenses);
        }

        @Override
        public ImageTransformations transfom(IFileHolder imageFile, Object source) throws IOException {
            return new TestableImageTransformations(imageData, expenses);
        }

        @Override
        public ImageTransformations transfom(byte[] imageData) throws IOException {
            return new TestableImageTransformations(imageData, expenses);
        }

        @Override
        public ImageTransformations transfom(byte[] imageData, Object source) throws IOException {
            return new TestableImageTransformations(imageData, expenses);
        }
    }

}
