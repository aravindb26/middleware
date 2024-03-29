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

package com.openexchange.ajax.helper;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Strings.toLowerCase;
import static com.openexchange.java.Strings.toUpperCase;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.activation.MimetypesFileTypeMap;
import com.google.common.net.PercentEscaper;
import com.openexchange.ajax.SessionServlet;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.ajax.fileholder.ByteArrayRandomAccess;
import com.openexchange.ajax.fileholder.IFileHolder.RandomAccess;
import com.openexchange.ajax.fileholder.Readable;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadException.UploadCode;
import com.openexchange.groupware.upload.impl.UploadImageSizeExceededException;
import com.openexchange.html.HtmlService;
import com.openexchange.html.HtmlServices;
import com.openexchange.imagetransformation.ImageTransformationConfig;
import com.openexchange.imagetransformation.NoSuchImageReader;
import com.openexchange.imagetransformation.Utility;
import com.openexchange.java.CharsetDetector;
import com.openexchange.java.Charsets;
import com.openexchange.java.HTMLDetector;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.ImageTypeDetector;
import com.openexchange.tools.encoding.Helper;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link DownloadUtility} - Utility class for download.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DownloadUtility {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DownloadUtility.class);

    /**
     * Initializes a new {@link DownloadUtility}.
     */
    private DownloadUtility() {
        super();
    }

    /**
     * Checks specified input stream intended for inline display for harmful data if its Content-Type indicates image content.
     *
     * @param inputStream The input stream
     * @param fileName The file name
     * @param contentTypeStr The content-type string
     * @param userAgent The user agent
     * @param session The associated session
     * @return The checked download providing input stream, content type, and content disposition to use
     * @throws OXException If checking download fails
     */
    public static CheckedDownload checkInlineDownload(Readable inputStream, String fileName, String contentTypeStr, String userAgent, ServerSession session) throws OXException {
        return checkInlineDownload(inputStream, fileName, contentTypeStr, null, userAgent, session);
    }

    /**
     * Checks specified input stream intended for inline display for harmful data if its Content-Type indicates image content.
     *
     * @param inputStream The input stream
     * @param fileName The file name
     * @param sContentType The <i>Content-Type</i> string
     * @param overridingDisposition Optionally overrides the <i>Content-Disposition</i> header
     * @param userAgent The <i>User-Agent</i>
     * @param session The associated session
     * @return The checked download providing input stream, content type, and content disposition to use
     * @throws OXException If checking download fails
     */
    public static CheckedDownload checkInlineDownload(Readable inputStream, String fileName, String sContentType, String overridingDisposition, String userAgent, ServerSession session) throws OXException {
        return checkInlineDownload(inputStream, -1L, fileName, sContentType, overridingDisposition, userAgent, false, session);
    }

    /**
     * Checks specified input stream intended for inline display for harmful data if its Content-Type indicates image content.
     *
     * @param inputStream The input stream
     * @param size The size of the passed stream
     * @param fileName The file name
     * @param sContentType The <i>Content-Type</i> string
     * @param overridingDisposition Optionally overrides the <i>Content-Disposition</i> header
     * @param userAgent The <i>User-Agent</i>
     * @param considerAsSafe Whether content is considered as safe
     * @param session The associated session
     * @return The checked download providing input stream, content type, and content disposition to use
     * @throws OXException If checking download fails
     */
    public static CheckedDownload checkInlineDownload(Readable inputStream, long size, String fileName, String sContentType, String overridingDisposition, String userAgent, boolean considerAsSafe, ServerSession session) throws OXException {
        return checkInlineDownload(inputStream, size, fileName, sContentType, overridingDisposition, userAgent, null == session ? Locale.US : session.getUser().getLocale(), considerAsSafe);
    }

    private static final String MIME_APPL_OCTET = MimeTypes.MIME_APPL_OCTET;

    /**
     * Checks specified input stream intended for inline display for harmful data if its Content-Type indicates image content.
     *
     * @param inputStream The input stream
     * @param size The size of the passed stream
     * @param fileName The file name
     * @param sContentType The <i>Content-Type</i> string
     * @param overridingDisposition Optionally overrides the <i>Content-Disposition</i> header
     * @param userAgent The <i>User-Agent</i>
     * @param locale The locale to use
     * @param considerAsSafe Whether content is considered as safe
     * @return The checked download providing input stream, content type, and content disposition to use
     * @throws OXException If checking download fails
     */
    public static CheckedDownload checkInlineDownload(Readable inputStream, long size, String fileName, String sContentType, String overridingDisposition, String userAgent, Locale locale, boolean considerAsSafe) throws OXException {
        ThresholdFileHolder sink = null;
        try {
            /*
             * We are supposed to let the client display the attachment. Therefore set attachment's Content-Type and inline disposition to let
             * the client decide if it's able to display.
             */
            final ContentType contentType = new ContentType(sContentType);
            if ((null != fileName) && contentType.startsWith(MIME_APPL_OCTET)) {
                /*
                 * Try to determine MIME type
                 */
                final String ct = MimeType2ExtMap.getContentType(fileName);
                final int pos = ct.indexOf('/');
                contentType.setPrimaryType(ct.substring(0, pos));
                contentType.setSubType(ct.substring(pos + 1));
            }
            boolean harmful = false;
            boolean indicatesInline = false;
            String sContentDisposition = overridingDisposition;
            if (sContentDisposition != null) {
                // Content-Disposition is given. Check for valid/known value
                String lccd = toLowerCase(sContentDisposition).trim();
                if (lccd.startsWith("inline")) {
                    sContentDisposition = "inline";
                    indicatesInline = true;
                } else if (lccd.startsWith("attachment")) {
                    sContentDisposition = "attachment";
                } else {
                    // Unknown value. Assume "inline" as default disposition to trigger client's (Browser) internal viewer.
                    sContentDisposition = "inline";
                    indicatesInline = true;
                }
            }
            long sz = size;
            Readable in = inputStream;
            // Some variables
            String fn = fileName;
            boolean modifiedContent = false;
            if (considerAsSafe == false) {
                // Check by Content-Type and file name
                if (Strings.containsAny(toLowerCase(contentType.getSubType()), "htm", "xhtm")) {
                    /*
                     * HTML content requested for download...
                     */
                    if (null == sContentDisposition) {
                        sContentDisposition = "attachment";
                    } else if (indicatesInline) {
                        if (contentType.contains("application/")) {
                            sContentDisposition = "attachment";
                        } else {
                            /*
                             * Sanitizing of HTML content needed
                             */
                            sink = new ThresholdFileHolder();
                            sink.write(in);
                            in = null;
                            String cs = contentType.getCharsetParameter();
                            if (!CharsetDetector.isValid(cs)) {
                                cs = CharsetDetector.detectCharset(sink.getStream());
                                if ("US-ASCII".equalsIgnoreCase(cs)) {
                                    cs = "ISO-8859-1";
                                }
                            }
                            // Check size
                            String htmlContent;
                            if (sink.getLength() > HtmlServices.htmlThreshold()) {
                                // HTML cannot be sanitized as it exceeds the threshold for HTML parsing
                                OXException oxe = AjaxExceptionCodes.HTML_TOO_BIG.create();
                                htmlContent = SessionServlet.getErrorPage(200, oxe.getDisplayMessage(locale), "");
                            } else {
                                htmlContent = new String(sink.toByteArray(), Charsets.forName(cs));
                                HtmlService htmlService = ServerServiceRegistry.getInstance().getService(HtmlService.class);
                                htmlContent = htmlService.sanitize(htmlContent, null, true, null, null);
                            }
                            sink.close();
                            sink = null; // Null'ify as not needed anymore
                            contentType.setCharsetParameter("UTF-8");
                            byte[] tmp = htmlContent.getBytes(Charsets.UTF_8);
                            sz = tmp.length;
                            in = new ByteArrayRandomAccess(tmp);
                            modifiedContent = true;
                        }
                    }
                } else if (Strings.containsAny(toLowerCase(contentType.getSubType()), "script") || fileNameImpliesJavascript(fileName)) {
                    // Treat all script content as harmful
                    harmful = true;
                    sContentDisposition = "attachment";
                } else if (Strings.containsAny(toLowerCase(contentType.getSubType()), "rdf") || fileNameImpliesRdf(fileName)) {
                    harmful = true;
                    sContentDisposition = "attachment";
                } else if (Strings.containsAny(toLowerCase(contentType.getSubType()), "svg") || fileNameImpliesSvg(fileName)) {
                    // Treat all SVG content as harmful
                    harmful = true;
                    sContentDisposition = "attachment";
                } else if (Strings.containsAny(toLowerCase(contentType.getSubType()), "xcf") || fileNameImpliesXcf(fileName)) {
                    sContentDisposition = "attachment";
                } else if (Strings.containsAny(toLowerCase(contentType.getSubType()), "niff", "nif") || fileNameImpliesNiff(fileName)) {
                    sContentDisposition = "attachment";
                } else if (Strings.containsAny(toLowerCase(contentType.getSubType()), "freehand") || fileNameImpliesFreehand(fileName)) {
                    sContentDisposition = "attachment";
                } else if (Strings.startsWithAny(toLowerCase(contentType.getSubType()), "xsl") || fileNameImpliesExcel(fileName)) {
                    sContentDisposition = "attachment";
                } else if (Strings.containsAny(toLowerCase(contentType.getSubType()), "xml") || fileNameImpliesXml(fileName)) {
                    /*
                     * XML content requested for download...
                     */
                    if (null == sContentDisposition) {
                        sContentDisposition = "attachment";
                    } else if (indicatesInline) {
                        if (contentType.contains("application/")) {
                            sContentDisposition = "attachment";
                        } else {
                            /*
                             * Escaping of XML content needed
                             */
                            sink = new ThresholdFileHolder();
                            sink.write(in);
                            in = null;
                            String cs = contentType.getCharsetParameter();
                            if (!CharsetDetector.isValid(cs)) {
                                cs = CharsetDetector.detectCharset(sink.getStream());
                                if ("US-ASCII".equalsIgnoreCase(cs)) {
                                    cs = "ISO-8859-1";
                                }
                            }
                            // Escape of XML content
                            {
                                ThresholdFileHolder copy = null;
                                OutputStreamWriter w = null;
                                Reader r = null;
                                try {
                                    copy = new ThresholdFileHolder();
                                    r = new InputStreamReader(sink.getClosingStream(), Charsets.forName(cs));
                                    w = new OutputStreamWriter(copy.asOutputStream(), Charsets.UTF_8);
                                    HtmlService htmlService = ServerServiceRegistry.getInstance().getService(HtmlService.class);
                                    final int buflen = 8192;
                                    final char[] cbuf = new char[buflen];
                                    for (int read; (read = r.read(cbuf, 0, buflen)) > 0;) {
                                        String xmlContent = new String(cbuf, 0, read);
                                        xmlContent = htmlService.htmlFormat(xmlContent);
                                        w.write(xmlContent);
                                    }
                                    w.flush();
                                    sink = copy;
                                    copy = null; // Avoid premature closing
                                } finally {
                                    Streams.close(r, w, copy);
                                }
                            }
                            contentType.setSubType("html");
                            contentType.setCharsetParameter("UTF-8");
                            sz = sink.getLength();
                            in = sink.getClosingRandomAccess();
                            modifiedContent = true;
                            sink = null; // Set to null to avoid premature closing at the end of try-finally clause
                        }
                    }
                } else if (contentType.startsWith("text/plain")) {
                    /*-
                     * Text content requested for download...
                     *
                     * Check for possibly missing charset parameter
                     */
                    if (null == contentType.getCharsetParameter()) {
                        /*
                         * Try and detect charset for plain text files
                         */
                        sink = new ThresholdFileHolder();
                        sink.write(in);
                        in = null;
                        String cs = CharsetDetector.detectCharset(sink.getStream());
                        if ("US-ASCII".equalsIgnoreCase(cs)) {
                            cs = "ISO-8859-1";
                        }
                        contentType.setCharsetParameter(cs);
                        sz = sink.getLength();
                    }
                    /*
                     * Safe reading of content if appropriate
                     */
                    if (null == sContentDisposition) {
                        if (null != sink) {
                            sz = sink.getLength();
                            in = sink.getClosingRandomAccess();
                            modifiedContent = true;
                            sink = null; // Set to null to avoid premature closing at the end of try-finally clause
                        }
                        sContentDisposition = "attachment";
                    } else if (indicatesInline) {
                        /*
                         * Sanitizing of text content needed
                         */
                        if (null == sink) {
                            sink = new ThresholdFileHolder();
                            sink.write(in);
                            in = null;
                        }
                        String cs = contentType.getCharsetParameter();
                        if (!CharsetDetector.isValid(cs)) {
                            cs = CharsetDetector.detectCharset(sink.getStream());
                            if ("US-ASCII".equalsIgnoreCase(cs)) {
                                cs = "ISO-8859-1";
                            }
                        }
                        // Convert to UTF-8
                        {
                            ThresholdFileHolder utf8Copy = null;
                            OutputStreamWriter w = null;
                            Reader r = null;
                            try {
                                utf8Copy = new ThresholdFileHolder();
                                r = new InputStreamReader(sink.getClosingStream(), Charsets.forName(cs));
                                w = new OutputStreamWriter(utf8Copy.asOutputStream(), Charsets.UTF_8);
                                final int buflen = 8192;
                                final char[] cbuf = new char[buflen];
                                for (int read; (read = r.read(cbuf, 0, buflen)) > 0;) {
                                    w.write(cbuf, 0, read);
                                }
                                w.flush();
                                sink = utf8Copy;
                                utf8Copy = null; // Avoid premature closing
                            } finally {
                                Streams.close(r, w, utf8Copy);
                            }
                        }
                        contentType.setCharsetParameter("UTF-8");
                        sz = sink.getLength();
                        in = sink.getClosingRandomAccess();
                        modifiedContent = true;
                        sink = null; // Set to null to avoid premature closing at the end of try-finally clause
                    } else {
                        if (null != sink) {
                            sz = sink.getLength();
                            in = sink.getClosingRandomAccess();
                            modifiedContent = true;
                            sink = null; // Set to null to avoid premature closing at the end of try-finally clause
                        }
                    }
                } else if (contentType.startsWith("image/") || fileNameImpliesImage(fileName)) {
                    /*
                     * Image content requested for download...
                     */
                    final BrowserDetector browserDetector = BrowserDetector.detectorFor(userAgent);
                    final boolean msieOnWindows = null != browserDetector && (browserDetector.isMSIE() && browserDetector.isWindows());
                    {
                        /*-
                         * Image content requested
                         *
                         * Get image bytes
                         */
                        sink = new ThresholdFileHolder();
                        sink.write(in);
                        in = null;
                        /*
                         * Check consistency of content-type, file extension and magic bytes
                         */
                        String preparedFileName = getSaveAsFileName(fileName, msieOnWindows, sContentType);
                        String fileExtension = getFileExtension(fn);
                        if (Strings.isEmpty(fileExtension)) {
                            /*
                             * Check for HTML since no corresponding file extension is known
                             */
                            if (HTMLDetector.containsHTMLTags(sink.getStream(), false, HtmlServices.getGlobalEventHandlerIdentifiers())) {
                                final CheckedDownload ret = asAttachment(sink.getClosingRandomAccess(), preparedFileName, sink.getLength(), true);
                                sink = null; // Set to null to avoid premature closing at the end of try-finally clause
                                return ret;
                            }
                        } else {
                            if ('.' == fileExtension.charAt(0)) {
                                // ".png" --> "png"
                                fileExtension = fileExtension.substring(1);
                            }
                            final Set<String> extensions = new HashSet<String>(MimeType2ExtMap.getFileExtensions(contentType.getBaseType()));
                            if (extensions.isEmpty() || (extensions.size() == 1 && extensions.contains("dat"))) {
                                /*
                                 * Content type determined by file name extension is unknown
                                 */
                                final String ct = MimeType2ExtMap.getContentType(fn);
                                if (MIME_APPL_OCTET.equals(ct)) {
                                    /*
                                     * No content type known
                                     */
                                    if (HTMLDetector.containsHTMLTags(sink.getStream(), false, HtmlServices.getGlobalEventHandlerIdentifiers())) {
                                        final CheckedDownload ret = asAttachment(sink.getClosingRandomAccess(), preparedFileName, sink.getLength(), true);
                                        sink = null; // Set to null to avoid premature closing at the end of try-finally clause
                                        return ret;
                                    }
                                } else {
                                    final int pos = ct.indexOf('/');
                                    contentType.setPrimaryType(ct.substring(0, pos));
                                    contentType.setSubType(ct.substring(pos + 1));
                                }
                            } else if (!extensions.contains(fileExtension)) {
                                /*
                                 * File extension does not fit to MIME type. Reset file name.
                                 */
                                fn = addFileExtension(fn, extensions.iterator().next());
                                preparedFileName = getSaveAsFileName(fn, msieOnWindows, contentType.getBaseType());
                            }
                            final String detectedCT = ImageTypeDetector.getMimeType(sink.getStream());
                            if (MIME_APPL_OCTET.equals(detectedCT)) {
                                /*
                                 * Unknown magic bytes. Check for HTML.
                                 */
                                if (HTMLDetector.containsHTMLTags(sink.getStream(), false, HtmlServices.getGlobalEventHandlerIdentifiers())) {
                                    final CheckedDownload ret = asAttachment(sink.getClosingRandomAccess(), preparedFileName, sink.getLength(), true);
                                    sink = null; // Set to null to avoid premature closing at the end of try-finally clause
                                    return ret;
                                }
                            } else if (!contentType.isMimeType(detectedCT)) {
                                /*
                                 * Image's magic bytes indicate another content type
                                 */
                                contentType.setBaseType(detectedCT);
                            }
                        }
                        /*
                         * New combined input stream (with original size)
                         */
                        in = sink.getClosingRandomAccess();
                        modifiedContent = true;
                        sink = null; // Set to null to avoid premature closing at the end of try-finally clause
                    }
                } else if (contentType.containsAny("shockwave", "flash") || fileNameImpliesFlash(fileName)) {
                    sContentDisposition = "attachment";
                } else if (fileNameImpliesHtml(fileName)) {
                    /*
                     * HTML content requested for download...
                     */
                    if (null == sContentDisposition) {
                        sContentDisposition = "attachment";
                    } else if (indicatesInline) {
                        /*
                         * Sanitizing of HTML content needed
                         */
                        sink = new ThresholdFileHolder();
                        sink.write(in);
                        in = null;
                        if (HTMLDetector.containsHTMLTags(sink.getStream(), true, HtmlServices.getGlobalEventHandlerIdentifiers())) {
                            String cs = contentType.getCharsetParameter();
                            if (!CharsetDetector.isValid(cs)) {
                                cs = CharsetDetector.detectCharset(sink.getStream());
                                if ("US-ASCII".equalsIgnoreCase(cs)) {
                                    cs = "ISO-8859-1";
                                }
                            }
                            // Check size
                            String htmlContent;
                            if (sink.getLength() > HtmlServices.htmlThreshold()) {
                                // HTML cannot be sanitized as it exceeds the threshold for HTML parsing
                                OXException oxe = AjaxExceptionCodes.HTML_TOO_BIG.create();
                                htmlContent = SessionServlet.getErrorPage(200, oxe.getDisplayMessage(locale), "");
                            } else {
                                htmlContent = new String(sink.toByteArray(), Charsets.forName(cs));
                                HtmlService htmlService = ServerServiceRegistry.getInstance().getService(HtmlService.class);
                                htmlContent = htmlService.sanitize(htmlContent, null, true, null, null);
                            }
                            sink.close();
                            sink = null; // Null'ify as not needed anymore
                            contentType.setCharsetParameter("UTF-8");
                            byte[] tmp = htmlContent.getBytes(Charsets.UTF_8);
                            sz = tmp.length;
                            in = new ByteArrayRandomAccess(tmp);
                            modifiedContent = true;
                        } else {
                            in = sink.getClosingRandomAccess();
                        }
                    }
                }
            }
            /*
             * Create return value
             */
            final CheckedDownload retval;
            if (sContentDisposition == null) {
                // Assume "inline" as default disposition to trigger client's (Browser) internal viewer.
                final StringBuilder builder = new StringBuilder(32).append("inline");
                appendFilenameParameter(fn, contentType.isBaseType("application", "octet-stream") ? null : contentType.toString(), userAgent, builder);
                contentType.removeParameter("name");
                retval = new CheckedDownload(contentType.toString(), builder.toString(), fn, in, sz, harmful, modifiedContent);
            } else if (sContentDisposition.indexOf(';') < 0) {
                final StringBuilder builder = new StringBuilder(32).append(sContentDisposition);
                appendFilenameParameter(fn, contentType.isBaseType("application", "octet-stream") ? null : contentType.toString(), userAgent, builder);
                contentType.removeParameter("name");
                retval = new CheckedDownload(contentType.toString(), builder.toString(), fn, in, sz, harmful, modifiedContent);
            } else {
                contentType.removeParameter("name");
                retval = new CheckedDownload(contentType.toString(), sContentDisposition, fn, in, sz, harmful, modifiedContent);
            }
            return retval;
        } catch (UnsupportedEncodingException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            Streams.close(sink);
        }
    }

    private static boolean fileNameImpliesHtml(final String fileName) {
        return null != fileName && checkFileNameFor(fileName, testStartsWith("text/htm"), "html", "htm");
    }

    private static boolean fileNameImpliesImage(final String fileName) {
        return null != fileName && MimeType2ExtMap.getContentType(fileName).startsWith("image/");
    }

    private static boolean fileNameImpliesFlash(final String fileName) {
        return null != fileName && MimeType2ExtMap.getContentType(fileName).indexOf("flash") >= 0;
    }

    private static boolean fileNameImpliesSvg(final String fileName) {
        return null != fileName && checkFileNameFor(fileName, testContains("svg"), "svg");
    }

    private static boolean fileNameImpliesXcf(final String fileName) {
        return null != fileName && checkFileNameFor(fileName, testContains("xcf"), "xcf");
    }

    private static boolean fileNameImpliesNiff(final String fileName) {
        return null != fileName && checkFileNameFor(fileName, testContains("niff"), "niff", "nif");
    }

    private static boolean fileNameImpliesFreehand(final String fileName) {
        return null != fileName && checkFileNameFor(fileName, testContains("freehand"), "fh4", "fh5", "fhc");
    }

    private static boolean fileNameImpliesExcel(final String fileName) {
        return null != fileName && checkFileNameFor(fileName, testContains("excel"), "xls", "xlt", "xlm", "xlsx", "xlsm", "xltx", "xltm", "xlsb", "xla", "xlam", "xll", "xlw");
    }

    private static boolean fileNameImpliesJavascript(final String fileName) {
        return null != fileName && MimeType2ExtMap.getContentType(fileName).indexOf("script") >= 0;
    }

    private static boolean fileNameImpliesXml(final String fileName) {
        return null != fileName && MimeType2ExtMap.getContentType(fileName).indexOf("xml") >= 0;
    }

    private static boolean fileNameImpliesRdf(final String fileName) {
        return null != fileName && MimeType2ExtMap.getContentType(fileName).indexOf("rdf") >= 0;
    }

    private static boolean checkFileNameFor(String fileName, ContentTypeTest contentTypeTest, String... fileNameTests) {
        if (contentTypeTest.matches(fileName)) {
            return true;
        }

        int pos = fileName.indexOf('.');
        String toCheck = Strings.asciiLowerCase(pos < 0 ? fileName : fileName.substring(pos));
        for (String test : fileNameTests) {
            if (toCheck.endsWith(test)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Appends the <tt>"filename"</tt> parameter to specified {@link StringBuilder} instance; e.g.
     *
     * <pre>
     * "attachment; filename="readme.txt"
     *            ^---------------------^
     * </pre>
     *
     * @param fileName The file name
     * @param userAgent The user agent identifier
     * @param appendTo The {@link StringBuilder} instance to append to
     */
    public static void appendFilenameParameter(final String fileName, final String userAgent, final StringBuilder appendTo) {
        appendFilenameParameter(fileName, null, userAgent, appendTo);
    }

    private static final PercentEscaper encoder = new PercentEscaper("", false);

    /**
     * Appends the <tt>"filename"</tt> parameter to specified {@link StringBuilder} instance; e.g.
     *
     * <pre>
     * "attachment; filename="readme.txt"
     *            ^---------------------^
     * </pre>
     *
     * @param fileName The file name
     * @param baseCT The base content type; e.g <tt>"application/octet-stream"</tt> or <tt>"text/plain"</tt>
     * @param userAgent The user agent identifier
     * @param appendTo The {@link StringBuilder} instance to append to
     */
    public static void appendFilenameParameter(final String fileName, final String baseCT, final String userAgent, final StringBuilder appendTo) {
        if (null == fileName) {
            appendTo.append("; filename=\"").append(DEFAULT_FILENAME).append('"');
            return;
        }
        String fn = fileName;
        if ((null != baseCT) && (null == getFileExtension(fn))) {
            if (baseCT.regionMatches(true, 0, MIME_TEXT_PLAIN, 0, MIME_TEXT_PLAIN.length()) && !fileName.toLowerCase(Locale.US).endsWith(".txt")) {
                fn += ".txt";
            } else if (baseCT.regionMatches(true, 0, MIME_TEXT_HTML, 0, MIME_TEXT_HTML.length()) && !fileName.toLowerCase(Locale.US).endsWith(".html")) {
                fn += ".html";
            }
        }
        fn = escapeBackslashAndQuote(fn);
        if (null != userAgent && BrowserDetector.detectorFor(userAgent).isMSIE()) {
            // InternetExplorer
            appendTo.append("; filename=\"").append(Helper.encodeFilenameForIE(fn, Charsets.UTF_8)).append('"');
            return;
        }
        /*-
         * On socket layer characters are casted to byte values.
         *
         * sink.write((byte) chars[i]);
         *
         * Therefore ensure we have a one-character-per-byte charset, as it is with ISO-8859-1
         */
        String isoFileName = new String(fn.getBytes(Charsets.UTF_8), Charsets.ISO_8859_1);
        final boolean isAndroid = (null != userAgent && toLowerCase(userAgent).indexOf("android") >= 0);
        if (isAndroid) {
            // myfile.dat => myfile.DAT
            final int pos = isoFileName.lastIndexOf('.');
            if (pos >= 0) {
                isoFileName = isoFileName.substring(0, pos) + toUpperCase(isoFileName.substring(pos));
            }
        }
        String encoded = encoder.escape(fn);
        appendTo.append("; filename*=UTF-8''").append(encoded);
        appendTo.append("; filename=\"").append(isoFileName).append('"');
    }

    private static final Pattern PAT_BSLASH = Pattern.compile("\\\\");

    private static final Pattern PAT_QUOTE = Pattern.compile("\"");

    private static String escapeBackslashAndQuote(final String str) {
        return PAT_QUOTE.matcher(PAT_BSLASH.matcher(str).replaceAll("\\\\\\\\")).replaceAll("\\\\\\\"");
    }

    private static CheckedDownload asAttachment(RandomAccess randomAccess, String preparedFileName, long size, boolean harmful) {
        /*
         * We are supposed to offer attachment for download. Therefore enforce application/octet-stream and attachment disposition.
         */
        return new CheckedDownload(MIME_APPL_OCTET, new StringBuilder(64).append("attachment; filename=\"").append(preparedFileName).append('"').toString(), preparedFileName, randomAccess, size, harmful, false);
    }

    // private static final Pattern P = Pattern.compile("^[\\w\\d\\:\\/\\.]+(\\.\\w{3,4})$");

    /**
     * Checks if specified file name has a trailing file extension.
     *
     * @param fileName The file name
     * @return The extension (e.g. <code>".txt"</code>) or <code>null</code>
     */
    private static String getFileExtension(final String fileName) {
        if (null == fileName) {
            return null;
        }
        final int pos = fileName.lastIndexOf('.');
        if ((pos <= 0) || (pos >= fileName.length())) {
            return null;
        }
        return fileName.substring(pos);
    }

    private static String addFileExtension(final String fileName, final String ext) {
        if (null == fileName) {
            return null;
        }
        final int pos = fileName.indexOf('.');
        if (-1 == pos) {
            return new StringBuilder(fileName).append('.').append(ext).toString();
        }
        return new StringBuilder(fileName.substring(0, pos)).append('.').append(ext).toString();
    }

    private static final String DEFAULT_FILENAME = "file.dat";

    private static final String MIME_TEXT_PLAIN = "text/plain";

    private static final String MIME_TEXT_HTML = "text/htm";

    /**
     * Gets a safe form (as per RFC 2047) for specified file name.
     * <p>
     * {@link BrowserDetector} may be used to parse browser and/or platform identifier from <i>"user-agent"</i> header.
     *
     * @param fileName The file name
     * @param internetExplorer <code>true</code> if <i>"user-agent"</i> header indicates to be Internet Explorer on a Windows platform;
     *            otherwise <code>false</code>
     * @param baseCT The (optional) base content type
     * @return A safe form (as per RFC 2047) for specified file name
     * @see BrowserDetector
     */
    public static final String getSaveAsFileName(final String fileName, final boolean internetExplorer, final String baseCT) {
        if (null == fileName) {
            return DEFAULT_FILENAME;
        }
        final StringBuilder tmp = new StringBuilder(32);
        try {
            if (fileName.indexOf(' ') >= 0) {
                tmp.append(Helper.encodeFilename(fileName.replaceAll(" ", "_"), "UTF-8", internetExplorer));
            } else {
                tmp.append(Helper.encodeFilename(fileName, "UTF-8", internetExplorer));
            }
        } catch (UnsupportedEncodingException e) {
            LOG.error("", e);
            return fileName;
        }
        if ((null != baseCT) && (null == getFileExtension(fileName))) {
            if (baseCT.regionMatches(true, 0, MIME_TEXT_PLAIN, 0, MIME_TEXT_PLAIN.length()) && !fileName.toLowerCase(Locale.US).endsWith(".txt")) {
                tmp.append(".txt");
            } else if (baseCT.regionMatches(true, 0, MIME_TEXT_HTML, 0, MIME_TEXT_HTML.length()) && !fileName.toLowerCase(Locale.US).endsWith(".html")) {
                tmp.append(".html");
            }
        }
        return tmp.toString();
    }

    /**
     * {@link CheckedDownload} - Represents a checked download as a result of <tt>DownloadUtility.checkInlineDownload()</tt>.
     */
    public static final class CheckedDownload {

        private final String contentType;
        private final String contentDisposition;
        private final String fileName;
        private final Readable inputStream;
        private final long size;
        private final boolean consideredHarmful;
        private final boolean modifiedContent;

        CheckedDownload(String contentType, String contentDisposition, String fileName, Readable inputStream, long size, boolean consideredHarmful, boolean modifiedContent) {
            super();
            this.contentType = contentType;
            this.contentDisposition = contentDisposition;
            this.fileName = fileName;
            this.inputStream = inputStream;
            this.size = size;
            this.consideredHarmful = consideredHarmful;
            this.modifiedContent = modifiedContent;
        }

        /**
         * Whether content has been modified.
         *
         * @return <code>true</code> if content has been modified; otherwise <code>false</code>
         */
        public boolean hasModifiedContent() {
            return modifiedContent;
        }

        /**
         * Whether the checked download's content is consired as harmful and no sanitizing could be applied.
         *
         * @return <code>true</code> if harmful; otherwise <code>false</code>
         */
        public boolean isConsideredHarmful() {
            return consideredHarmful;
        }

        /**
         * Gets the file name
         *
         * @return The file name
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Gets the size
         *
         * @return The size
         */
        public long getSize() {
            return size;
        }

        /**
         * Gets the content type.
         *
         * @return The content type
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Gets the content disposition.
         *
         * @return The content disposition
         */
        public String getContentDisposition() {
            return contentDisposition;
        }

        /**
         * Checks if Content-Disposition indicates an attachment.
         *
         * @return <code>true</code> if attachment; otherwise <code>false</code>
         */
        public boolean isAttachment() {
            return null != contentDisposition && Strings.toLowerCase(contentDisposition).startsWith("attachment");
        }

        /**
         * Gets the input stream.
         *
         * @return The input stream
         */
        public Readable getInputStream() {
            return inputStream;
        }
    }

    /**
     * Checks if specified uploaded file is a illegal and/or possibly harmful.
     *
     * @param file The file to check
     * @return <code>true</code> if specified uploaded file is illegal/harmful; otherwise <code>false</code>
     * @throws OXException If uploaded file cannot be checked
     * @throws IOException If an I/O error occurs or upload is denied
     */
    public static boolean isIllegalUpload(UploadFile file) throws OXException, IOException {
        ContentType contentType = new ContentType(file.getContentType());
        String fileName = file.getPreparedFileName();
        if ((null != fileName) && contentType.startsWith(MIME_APPL_OCTET)) {
            /*
             * Try to determine MIME type
             */
            final String ct = MimeType2ExtMap.getContentType(fileName);
            final int pos = ct.indexOf('/');
            contentType.setPrimaryType(ct.substring(0, pos));
            contentType.setSubType(ct.substring(pos + 1));
        }

        if (Strings.startsWithAny(toLowerCase(contentType.getSubType()), "svg") || fileNameImpliesSvg(fileName)) {
            if (HTMLDetector.containsHTMLTags(file.openStream(), false, HtmlServices.getGlobalEventHandlerIdentifiers())) {
                // Illegal
                return true;
            }
            return false;
        }

        if (isIllegalImage(file)) {
            return true;
        }

        if (contentType.containsAny("shockwave", "flash") || fileNameImpliesFlash(fileName)) {
            return true;
        }
        if (Strings.startsWithAny(toLowerCase(contentType.getSubType()), "htm", "xhtm", "xml") || fileNameImpliesHtml(fileName)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if specified uploaded file is an illegal image.
     *
     * @param file The file to check
     * @return <code>true</code> if specified uploaded file is an illegal image; otherwise <code>false</code>
     * @throws IOException If uploaded file cannot be checked
     * @throws OXException if the image is too big or the resolution is too high
     */
    public static boolean isIllegalImage(UploadFile file) throws IOException, OXException {
        String contentType = file.getContentType();
        if (isImageContentType(contentType)) {
            return isIllegalImageData(file);
        }
        if (null != file.getPreparedFileName()) {
            contentType = new MimetypesFileTypeMap().getContentType(file.getPreparedFileName());
            if (isImageContentType(contentType)) {
                return isIllegalImageData(file);
            }
        }
        contentType = com.openexchange.java.ImageTypeDetector.getMimeType(file.openStream());
        if (com.openexchange.java.Strings.toLowerCase(contentType).startsWith("image/")) {
            return isIllegalImageData(file);
        }
        return false;
    }

    private static boolean isImageContentType(final String contentType) {
        return null != contentType && com.openexchange.java.Strings.toLowerCase(contentType).startsWith("image/");
    }

    private static boolean isIllegalImageData(UploadFile imageFile) throws IOException, OXException {
        if (!isValidImage(imageFile)) {
            // Invalid
            return true;
        }

        if (HTMLDetector.containsHTMLTags(imageFile.openStream(), false, HtmlServices.getGlobalEventHandlerIdentifiers())) {
            // Illegal
            return true;
        }
        return false;
    }

    private static boolean isValidImage(UploadFile imageFile) throws IOException, OXException {
        return isValidImage(imageFile.openStream(), imageFile.getSize(), imageFile.getContentType(), imageFile.getPreparedFileName());
    }

    /**
     * Checks for valid image according to configured image restrictions.
     *
     * @param imageStream The stream providing image data
     * @param size The size of the image
     * @param contentType The image's MIME type
     * @param fileName The name of the image file
     * @return <code>true</code> for valid image data and no configured restriction does apply; otherwise <code>false</code> if data is no valid image data
     * @throws IOException If an I/O error occurs
     * @throws OXException If any of configured restrictions does apply (resolution too high, size too big, etc.)
     */
    public static boolean isValidImage(InputStream imageStream, long size, String contentType, String fileName) throws IOException, OXException {
        try {
            Dimension dimension = Utility.getImageDimensionFor(imageStream, contentType, fileName);
            if (dimension == null || dimension.getHeight() <= 0 || dimension.getWidth() <= 0) {
                return false;
            }

            // Check size
            {
                long maxSize = ImageTransformationConfig.getConfig().getTransformationMaxSize();
                if (0 < maxSize && maxSize < size) {
                    // Too big
                    throw UploadImageSizeExceededException.create(size, maxSize, true);
                }
            }

            // Check resolution
            {
                long maxResolution = ImageTransformationConfig.getConfig().getTransformationMaxResolution();
                if (0 < maxResolution) {
                    int resolution = dimension.height * dimension.width;
                    if (resolution > maxResolution) {
                        // Resolution too high
                        throw UploadCode.IMAGE_RESOLUTION_TOO_HIGH.create(I(resolution), L(maxResolution));
                    }
                }
            }

            return true;
        } catch (NoSuchImageReader e) {
            // Missing image reader. Thus assume false.
            return false;
        }
    }

    // ----------------------------------------------------------------------------------------------------------------

    private static interface ContentTypeTest {

        boolean matches(String fileName);
    }

    private static ContainsContentTypeTest testContains(String test) {
        return new ContainsContentTypeTest(test);
    }

    private static class ContainsContentTypeTest implements ContentTypeTest {

        private final String test;

        ContainsContentTypeTest(String test) {
            super();
            this.test = test;
        }

        @Override
        public boolean matches(String fileName) {
            String contentTypeByFileName = MimeType2ExtMap.getContentType(fileName);
            return contentTypeByFileName.indexOf(test) >= 0;
        }

    }

    private static StartsWithContentTypeTest testStartsWith(String test) {
        return new StartsWithContentTypeTest(test);
    }

    private static class StartsWithContentTypeTest implements ContentTypeTest {

        private final String test;

        StartsWithContentTypeTest(String test) {
            super();
            this.test = test;
        }

        @Override
        public boolean matches(String fileName) {
            String contentTypeByFileName = MimeType2ExtMap.getContentType(fileName);
            return contentTypeByFileName.startsWith(test);
        }

    }

}
