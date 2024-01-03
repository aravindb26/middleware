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

package com.openexchange.ajax.framework;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.framework.AJAXRequest.FieldParameter;
import com.openexchange.ajax.framework.AJAXRequest.FileParameter;
import com.openexchange.ajax.framework.AJAXRequest.Method;
import com.openexchange.ajax.framework.AJAXRequest.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.configuration.AJAXConfig.Property;
import com.openexchange.test.common.tools.URLParameter;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * Executes the {@link AJAXRequest}s, processes the response as defined through {@link AbstractAJAXParser} and returns an
 * {@link AbstractAJAXResponse}.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class Executor extends Assertions {

    private Executor() {
        super();
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXClient client, final AJAXRequest<T> request) throws OXException, IOException, JSONException {
        return execute(client.getSession(), request);
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXClient client, final AJAXRequest<T> request, final String protocol, final String hostname) throws OXException, IOException, JSONException {
        return execute(client.getSession(), request, protocol, hostname, getSleep());
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXSession session, final AJAXRequest<T> request) throws OXException, IOException, JSONException {
        return execute(session, request, AJAXConfig.getProperty(Property.PROTOCOL), AJAXConfig.getProperty(Property.SERVER_HOSTNAME), getSleep());
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXSession session, final AJAXRequest<T> request, final String hostname) throws OXException, IOException, JSONException {
        return execute(session, request, AJAXConfig.getProperty(Property.PROTOCOL), hostname, getSleep());
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXSession session, final AJAXRequest<T> request, final String protocol, final String hostname) throws OXException, IOException, JSONException {
        return execute(session, request, protocol, hostname, getSleep());
    }

    private static final AtomicLong COUNTER = new AtomicLong(1);

    public static <T extends AbstractAJAXResponse> T execute(final AJAXSession session, final AJAXRequest<T> request, final String protocol, final String hostname, final int sleep) throws OXException, IOException, JSONException {
        final String urlString;
        if (request instanceof PortAwareAjaxRequest) {
            urlString = protocol + "://" + hostname + ":" + ((PortAwareAjaxRequest<T>) request).getPort() + request.getServletPath();
        } else {
            urlString = protocol + "://" + hostname + request.getServletPath();
        }
        final HttpUriRequest httpRequest;
        final Method method = request.getMethod();
        switch (method) {
            case GET:
                httpRequest = new HttpGet(addQueryParamsToUri(urlString, getGETParameter(session, request)));
                break;
            case DELETE:
                httpRequest = new HttpDelete(addQueryParamsToUri(urlString, getGETParameter(session, request)));
                break;
            case POST:
                HttpEntity postEntity;
                String contentType = detectContentTypeHeader(request);
                if ("multipart/form-data".equals(contentType)) {
                    postEntity = buildMultipartEntity(request);
                } else {
                    postEntity = getBodyParameters(request);
                }

                HttpPost httpPost = new HttpPost(urlString + getURLParameter(session, request, true));
                httpPost.setEntity(postEntity);
                httpRequest = httpPost;
                break;
            case UPLOAD:
                final HttpPost httpUpload = new HttpPost(urlString + getURLParameter(session, request, false)); //TODO old request used to set "mimeEncoded" = true here
                addUPLOADParameter(httpUpload, request);
                httpRequest = httpUpload;
                break;
            case PUT:
                final HttpPut httpPut = new HttpPut(urlString + getURLParameter(session, request, false));
                Object body = request.getBody();
                if (null != body) {
                    final ByteArrayEntity entity = new ByteArrayEntity(createBodyBytes(body));
                    entity.setContentType(AJAXServlet.CONTENTTYPE_JSON);
                    httpPut.setEntity(entity);
                }
                httpRequest = httpPut;
                break;
            default:
                throw AjaxExceptionCodes.INVALID_PARAMETER.create(request.getMethod().name());
        }
        for (final Header header : request.getHeaders()) {
            if (method == Method.POST) {
                if (!"Content-Type".equalsIgnoreCase(header.getName())) {
                    httpRequest.addHeader(header.getName(), header.getValue());
                }
            } else {
                httpRequest.addHeader(header.getName(), header.getValue());
            }
        }
        // Test echo header
        final String echoHeaderName = AJAXConfig.getProperty(AJAXConfig.Property.ECHO_HEADER, "");
        String echoValue = null;
        if (!isEmpty(echoHeaderName)) {
            echoValue = "pingMeBack-" + COUNTER.getAndIncrement();
            httpRequest.addHeader(echoHeaderName, echoValue);
        }

        CloseableHttpClient httpClient = session.getHttpClient();
        HttpClientContext context = new HttpClientContext();
        long startRequest = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(httpRequest, context)) {
            final long requestDuration = System.currentTimeMillis() - startRequest;
            if (null != echoValue) {
                org.apache.http.Header header = response.getFirstHeader(echoHeaderName);
                if (null == header) {
                    fail("Missing echo header: " + echoHeaderName);
                } else {
                    assertEquals("Wrong echo header", echoValue, header.getValue());
                }
            }

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                // Restore the interrupted status; see http://www.ibm.com/developerworks/java/library/j-jtp05236/index.html
                Thread.currentThread().interrupt();
                System.out.println("InterruptedException while sleeping between test requests. Does that help?");
                e.printStackTrace();
            } //emulating HttpUnit to avoid the Apache bug that mixes package up

            AbstractAJAXParser<? extends T> parser = request.getParser();
            String responseBody = parser.checkResponse(response, httpRequest);

            long startParse = System.currentTimeMillis();
            T retval = parser.parse(responseBody);
            long parseDuration = System.currentTimeMillis() - startParse;

            retval.setRequestDuration(requestDuration);
            retval.setParseDuration(parseDuration);
            retval.setCookies(context.getCookieStore().getCookies());
            return retval;
        }
    }

    private static String detectContentTypeHeader(AJAXRequest<?> request) {
        for (final Header header : request.getHeaders()) {
            if ("Content-Type".equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }

        return null;
    }

    private static boolean isEmpty(final String string) {
        if (null == string) {
            return true;
        }
        final int len = string.length();
        boolean isWhitespace = true;
        for (int i = 0; isWhitespace && i < len; i++) {
            isWhitespace = Character.isWhitespace(string.charAt(i));
        }
        return isWhitespace;
    }


    /*************************************
     *** Rewrite for HttpClient: Start ***
     *************************************/

    private static String addQueryParamsToUri(String uri, final List<NameValuePair> queryParams) {

        java.util.Collections.sort(queryParams, new Comparator<NameValuePair>() {

            @Override
            public int compare(final NameValuePair o1, final NameValuePair o2) {
                return (o1.getName().compareTo(o2.getName()));
            }
        }); //sorting the query params alphabetically

        if (uri.contains("?")) {
            uri += "&";
        } else {
            uri += "?";
        }
        return uri + URLEncodedUtils.format(queryParams, "UTF-8");
    }

    private static List<NameValuePair> getGETParameter(final AJAXSession session, final AJAXRequest<?> ajaxRequest) throws IOException, JSONException { //new
        final List<NameValuePair> pairs = new LinkedList<>();

        if (session.getId() != null) {
            pairs.add(new BasicNameValuePair(AJAXServlet.PARAMETER_SESSION, session.getId()));
        }

        for (final Parameter param : ajaxRequest.getParameters()) {
            if (!(param instanceof FileParameter)) {
                pairs.add(new BasicNameValuePair(param.getName(), param.getValue()));
            }
        }

        return pairs;
    }

    private static void addUPLOADParameter(final HttpPost postMethod, final AJAXRequest<?> request) throws IOException, JSONException {
        final MultipartEntity parts = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

        for (final Parameter param : request.getParameters()) {
            if (param instanceof FieldParameter) {

                final FieldParameter fparam = (FieldParameter) param;
                final StringBody body = new StringBody(fparam.getFieldContent(), Charset.forName("UTF-8"));
                parts.addPart(new FormBodyPart(fparam.getFieldName(), body));
            }
            if (param instanceof FileParameter) {
                final FileParameter fparam = (FileParameter) param;
                final InputStream is = fparam.getInputStream();
                InputStreamBody body;
                if (null != fparam.getMimeType() && !"".equals(fparam.getMimeType())) {
                    body = new InputStreamBody(is, fparam.getMimeType(), fparam.getValue());
                } else {
                    body = new InputStreamBody(is, fparam.getValue());
                }
                parts.addPart(new FormBodyPart(fparam.getName(), body));
            }
        }
        postMethod.setEntity(parts);

    }

    private static HttpEntity getBodyParameters(final AJAXRequest<?> request) throws IOException, JSONException {
        final List<NameValuePair> pairs = new LinkedList<>();

        for (final Parameter param : request.getParameters()) {
            if (param instanceof FieldParameter) {
                final FieldParameter fparam = (FieldParameter) param;
                pairs.add(new BasicNameValuePair(fparam.getFieldName(), fparam.getFieldContent()));
            }
        }

        return new UrlEncodedFormEntity(pairs);
    }

    private static HttpEntity buildMultipartEntity(AJAXRequest<?> request) throws IOException, JSONException {
        MultipartEntity entity = new MultipartEntity();
        for (final Parameter param : request.getParameters()) {
            if (param instanceof FileParameter) {
                entity.addPart(param.getName(), new InputStreamBody(((FileParameter) param).getInputStream(), ((FileParameter) param).getMimeType(), ((FileParameter) param).getFileName()));
            } else if (param instanceof FieldParameter) {
                entity.addPart(((FieldParameter) param).getFieldName(), new StringBody(((FieldParameter) param).getFieldContent(), Charset.forName("UTF-8")));
            }
        }

        return entity;
    }

    /*************************************
     *** Rewrite for HttpClient: End ***
     *************************************/

    /**
     * @param strict <code>true</code> to only add URLParameters to the URL. This is needed for the POST request of the login method.
     *            Unfortunately breaks this a lot of other tests.
     */
    private static String getURLParameter(final AJAXSession session, final AJAXRequest<?> request, final boolean strict) throws IOException, JSONException {
        final URLParameter parameter = new URLParameter();
        if (null != session.getId()) {
            parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session.getId());
        }
        for (final Parameter param : request.getParameters()) {
            if (!strict && !(param instanceof FileParameter) && !(param instanceof FieldParameter)) {
                parameter.setParameter(param.getName(), param.getValue());
            }
            if (strict && param instanceof com.openexchange.ajax.framework.AJAXRequest.URLParameter) {
                parameter.setParameter(param.getName(), param.getValue());
            }
            // Don't throw error here because field and file parameters are added on POST with method addBodyParameter().
        }
        return parameter.getURLParameters();
    }

    private static byte[] createBodyBytes(final Object body) throws UnsupportedCharsetException {
        return body.toString().getBytes(Charsets.UTF_8);
    }

    private static int getSleep() {
        String sleepS;
        try {
            sleepS = AJAXConfig.getProperty(Property.SLEEP);
        } catch (NullPointerException e) {
            sleepS = null;
        }
        int sleep;
        try {
            sleep = Integer.parseInt(sleepS);
        } catch (NumberFormatException e) {
            sleep = 500;
        }
        return sleep;
    }
}