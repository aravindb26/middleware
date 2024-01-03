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

package org.glassfish.grizzly.http.server;

import static org.glassfish.grizzly.http.util.HttpCodecUtils.put;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.KeepAlive;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.Constants;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.DelayedExecutor;
import com.openexchange.java.Strings;

/**
 * {@link CustomHttpCodecFilter}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public class CustomHttpCodecFilter extends org.glassfish.grizzly.http.HttpServerFilter {

    /**
     * Initializes a new {@link CustomHttpCodecFilter}.
     *
     * @deprecated
     */
    @Deprecated
    public CustomHttpCodecFilter(boolean chunkingEnabled, int maxHeadersSize, String defaultResponseContentType, KeepAlive keepAlive, DelayedExecutor executor, int maxRequestHeaders, int maxResponseHeaders) {
        super(chunkingEnabled, maxHeadersSize, defaultResponseContentType, keepAlive, executor, maxRequestHeaders, maxResponseHeaders);
    }

    /**
     * CRLF bytes.
     */
    private static final byte[] CRLF_BYTES = {(byte) '\r', (byte) '\n'};

    @Override
    protected boolean onHttpHeaderParsed(HttpHeader httpHeader, Buffer buffer, FilterChainContext ctx) {
        if (super.onHttpHeaderParsed(httpHeader, buffer, ctx)) { // Call to super...
            // Error occurred while processing HTTP headers
            return true;
        }

        // No error while processing HTTP headers. Continue...
        if (httpHeader.isExpectContent()) {
            /*-
             * This is true if there is a HTTP method that provides request payload (POST, PUT, PATCH, ...)
             * and HTTP headers signal payload availability (e.g. Transfer-Encoding: chunked or Content Length greater than 0)
             *
             * See: o.g.grizzly.http.HttpServerFilter.prepareRequest(ServerHttpRequestImpl, boolean)
             */
            HttpRequestPacket requestPacket = (HttpRequestPacket) httpHeader;
            if (requestPacket.requiresAcknowledgement()) {
                /*-
                 * This is true if HTTP headers contain the "Expect: 100-continue" header line, protocol is "HTTP/1.1" and there is no
                 * ready payload data, yet
                 *
                 * See: o.g.grizzly.http.HttpServerFilter.prepareRequest(ServerHttpRequestImpl, boolean)
                 *
                 * ----------------------------------------------------------------------------------------------------------------
                 *
                 * If Grizzly expects content, the "100 Continue" acknowledgement is not sent to client, which in turns waits for that
                 * interim response until actually sending data. The request then starves. Therefore that "100 Continue" interim response
                 * is sent here.
                 */
                MemoryManager<?> memoryManager = ctx.getMemoryManager();
                Buffer output = memoryManager.allocate(128);
                output = put(memoryManager, output, Protocol.HTTP_1_1.getProtocolBytes());
                output = put(memoryManager, output, Constants.SP);
                output = put(memoryManager, output, HttpStatus.CONINTUE_100.getStatusBytes());
                output = put(memoryManager, output, Constants.SP);
                output = put(memoryManager, output, HttpStatus.CONINTUE_100.getReasonPhraseBytes());
                output = put(memoryManager, output, CRLF_BYTES);
                output = put(memoryManager, output, CRLF_BYTES);
                output.trim();
                output.allowBufferDispose(true);
                requestPacket.getResponse().acknowledged();
                ctx.write(output);
            }
        }

        // Signal no error
        return false;
    }

    @Override
    protected void onHttpHeaderError(HttpHeader httpHeader, FilterChainContext ctx, Throwable t) throws IOException {
        if (t instanceof IllegalStateException && "HTTP packet header is too large".equals(t.getMessage())) {
            /*-
             * Handle special exception when HTTP packet header is too large:
             *
             * java.lang.IllegalStateException: HTTP packet header is too large
             *    at org.glassfish.grizzly.http.HttpCodecFilter$HeaderParsingState.checkOverflow(HttpCodecFilter.java:2060)
             *    at org.glassfish.grizzly.http.HttpCodecFilter.decodeHttpPacketFromBytes(HttpCodecFilter.java:748)
             *    at org.glassfish.grizzly.http.HttpCodecFilter.decodeHttpPacket(HttpCodecFilter.java:717)
             *    at org.glassfish.grizzly.http.HttpCodecFilter.handleRead(HttpCodecFilter.java:565)
             *    ...
             */
            HttpResponsePacket response = ((HttpRequestPacket) httpHeader).getResponse();
            sendHttpPacketHeaderTooLargeResponse(ctx, response);
        } else {
            super.onHttpHeaderError(httpHeader, ctx, t);
        }
    }

    private void sendHttpPacketHeaderTooLargeResponse(final FilterChainContext ctx, final HttpResponsePacket response) {
        if (response.getHttpStatus().getStatusCode() < 400) {
            // 431 - Request Header Fields Too Large
            HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.setValues(response);
        }

        byte[] errorPage = getHttpPacketHeaderTooLargeErrorPage().getBytes(StandardCharsets.UTF_8);
        response.setContentLength(errorPage.length);
        response.setContentType("text/html; charset=UTF-8");
        HttpContent errorHttpResponse = HttpContent.builder(response).content(new ByteBufferWrapper(ByteBuffer.wrap(errorPage))).last(true).build();
        errorPage = null;

        Buffer resBuf = encodeHttpPacket(ctx, errorHttpResponse);
        ctx.write(resBuf);
        response.getProcessingState().getHttpContext().close();
    }

    /**
     * Generates a simple error page for given arguments.
     *
     * @param statusCode The status code; e.g. <code>404</code>
     * @param msg The optional status message; e.g. <code>"Not Found"</code>
     * @param desc The optional status description; e.g. <code>"The requested URL was not found on this server."</code>
     * @return A simple error page
     */
    private static String getHttpPacketHeaderTooLargeErrorPage() {
        int statusCode = HttpStatus.BAD_REQUEST_400.getStatusCode();
        String msg = "Request Header Or Cookie Too Large";

        StringBuilder sb = new StringBuilder(2300);
        String lineSep = Strings.getLineSeparator();
        sb.append("<!DOCTYPE html>").append(lineSep);
        sb.append("<html><head>").append(lineSep);

        sb.append("<title>").append(statusCode);
        sb.append(' ').append(msg);
        sb.append("</title>").append(lineSep);

        sb.append("</head><body>").append(lineSep);

        sb.append("<h1>");
        sb.append(msg);
        sb.append("</h1>").append(lineSep);

        sb.append("<p>").append(lineSep);
        sb.append("The error might occur when the server detects that the size of cookies for the domain you are visiting is too large.").append(lineSep);
        sb.append("It might also occur when the server finds that some of the cookies are corrupted.").append(lineSep);
        sb.append("</p>").append(lineSep);

        sb.append("<p>").append(lineSep);
        sb.append("To clear the error message you can try clearing the cookies for that particular domain.").append(lineSep);
        sb.append("Each browser has unique instructions for clearing the cache and cookies. Instructions for commonly used browser are listed below.").append(lineSep);
        sb.append("</p>").append(lineSep);

        sb.append("<ul>").append(lineSep);
        sb.append("    <li>").append(lineSep);
        sb.append("    <p><a href=\"https://support.google.com/accounts/answer/32050?co=GENIE.Platform=Desktop&amp;hl=en\">Chrome</a><a href=\"https://support.google.com/accounts/answer/32050?co=GENIE.Platform=Desktop&amp;hl=en\">: Clear Cache and Cookies (Google Support)</a></p>").append(lineSep);
        sb.append("    </li>").append(lineSep);
        sb.append("    <li>").append(lineSep);
        sb.append("    <p><a href=\"http://www.wikihow.com/Clear-Cache-and-Cookies#Mozilla_Firefox_sub\">Firefox</a><a href=\"https://support.mozilla.org/en-US/kb/how-clear-firefox-cache\">: How to Clear the Firefox Cache (Mozilla Support)</a></p>").append(lineSep);
        sb.append("    </li>").append(lineSep);
        sb.append("    <li>").append(lineSep);
        sb.append("    <p id=\"s-lg-content-20145857\"><a href=\"http://www.wikihow.com/Clear-Cache-and-Cookies#Internet_Explorer_.28IE.29_sub\">Internet Explorer</a><a href=\"https://support.microsoft.com/en-us/help/17438/windows-internet-explorer-view-delete-browsing-history\">: View and Delete your Browsing History in Internet Explorer (Microsoft Support)</a></p>").append(lineSep);
        sb.append("    </li>").append(lineSep);
        sb.append("    <li>").append(lineSep);
        sb.append("    <p><a href=\"http://windows.microsoft.com/en-us/windows-10/view-delete-browsing-history-microsoft-edge\">Microsoft Edge</a><a href=\"https://support.microsoft.com/en-us/help/10607/microsoft-edge-view-delete-browser-history\">: View and Delete Browsing History in Microsoft Edge (Microsoft Edge Support)</a></p>").append(lineSep);
        sb.append("    </li>").append(lineSep);
        sb.append("    <li>").append(lineSep);
        sb.append("    <p><a href=\"http://www.wikihow.com/Clear-Cache-and-Cookies#Apple_Safari_sub\">Safari</a><a href=\"https://support.apple.com/guide/safari/clear-your-browsing-history-sfri47acf5d6/mac\">: Clear your browsing history (Apple Support)</a></p>").append(lineSep);
        sb.append("    </li>").append(lineSep);
        sb.append("    <li>").append(lineSep);
        sb.append("    <p><a href=\"http://www.wikihow.com/Clear-Cache-and-Cookies#iOS_sub\">A</a><a href=\"https://support.apple.com/en-us/HT201265\">pple iOS: Delete history. cache, and cookies (Apple Support)</a></p>").append(lineSep);
        sb.append("    </li>").append(lineSep);
        sb.append("</ul>").append(lineSep);

        sb.append("</body></html>").append(lineSep);
        return sb.toString();
    }

}
