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

package com.openexchange.ajax.simple;

import static com.openexchange.ajax.framework.ClientCommons.X_OX_HTTP_TEST_HEADER_NAME;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.tools.JSONCoercion;
import com.openexchange.test.common.configuration.AJAXConfig;
import okhttp3.CookieJar;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.FormBody.Builder;
import okhttp3.HttpUrl;
import okhttp3.JavaNetCookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * {@link SimpleOXClient}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class SimpleOXClient {

    private static final String USER_AGENT = "SimpleOXClient";
    private static final String BASE = AJAXConfig.getProperty(AJAXConfig.Property.BASE_PATH);

    private boolean debug = "true".equals(System.getProperty("debug"));

    private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }
    }
    };
    private static final SSLContext trustAllSslContext;
    static {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
    private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();

    private final OkHttpClient client;
    private String sessionID;
    private final HttpUrl host;
    private final CookieManager cookieManager;

    public SimpleOXClient(String host, boolean secure) {
        String scheme = secure ? "https" : "http";
        int port = secure ? 443 : 80;
        this.host = new HttpUrl.Builder().host(host).port(port).scheme(scheme).build();
        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieJar jar = new JavaNetCookieJar(cookieManager);
        if (secure) {
            // @formatter:off
            client = new OkHttpClient.Builder()
                                     .hostnameVerifier((hostname, session) -> true)
                                     .sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0])
                                     .cookieJar(jar)
                                     .connectionPool(new ConnectionPool(10,5, TimeUnit.SECONDS))
                                     .build();
            // @formatter:on
        } else {
            client = new OkHttpClient.Builder().connectionPool(new ConnectionPool(10,5, TimeUnit.SECONDS)).cookieJar(jar).build();
        }
    }

    public SimpleOXClient(String host) {
        this(host, false);
    }

    public String login(String login, String password) throws JSONException, IOException {
        JSONObject obj = raw("login", "login", "name", login, "password", password);
        if (obj.has("error")) {
            throw new RuntimeException("Unexpected Repsonse: " + obj.toString());
        }
        return sessionID = obj.getString("session");
    }

    public boolean isLoggedIn() {
        return sessionID != null;
    }

    public SimpleOXModule getModule(String moduleName) {
        return new SimpleOXModule(this, moduleName);
    }

    public JSONObject execute(String module, String action, Map<String, Object> params) throws JSONException, IOException {
        params.put("action", action);
        if (!params.containsKey("session") && isLoggedIn()) {
            params.put("session", sessionID);
        }
        Request.Builder builder = new Request.Builder()
                .header("User-Agent", USER_AGENT)
                .header(X_OX_HTTP_TEST_HEADER_NAME, USER_AGENT);

        if (params.containsKey("body")) {
            String bodyStr = JSONCoercion.coerceToJSON(params.remove("body")).toString();
            builder.put(RequestBody.create(bodyStr, MediaType.parse("application/json; charset=UTF-8")));
            okhttp3.HttpUrl.Builder urlBuilder = host.newBuilder(BASE + module);
            params.forEach((k, v) -> urlBuilder.addQueryParameter(k, v == null ? null : v.toString()));
            builder.url(urlBuilder.build());
        } else {
            Builder formBodyBuilder = new FormBody.Builder();
            params.forEach((k, v) -> formBodyBuilder.add(k, v == null ? null : v.toString()));
            builder.post(formBodyBuilder.build()).url(host.newBuilder(BASE + module).build());
        }

        Response resp = client.newCall(builder.build()).execute();
        String response = resp.body().string();

        return response.isEmpty() ? JSONObject.EMPTY_OBJECT : new JSONObject(response);
    }

    public SimpleResponse call(String module, String action, Object... parameters) throws JSONException, IOException {
        return new SimpleResponse(raw(module, action, parameters));
    }

    public JSONObject raw(String module, String action, Object... parameters) throws JSONException, IOException {
        Response resp = rawMethod(module, action, parameters);
        if (resp.code() != 200) {
            resp.body().close();
            throw new IllegalStateException("Expected a return code of 200 but was " + resp.code());
        }
        String response = resp.body().string();
        if (debug) {
            System.out.println("Response: " + response);
        }
        return response.isEmpty() ? JSONObject.EMPTY_OBJECT : new JSONObject(response);
    }

    public Response rawMethod(String module, String action, Object... parameters) throws JSONException, IOException {
        Map<String, Object> params = M(parameters);
        params.put("action", action);
        if (!params.containsKey("session") && isLoggedIn()) {
            params.put("session", sessionID);
        }
        Request.Builder builder = new Request.Builder()
                .header("User-Agent", USER_AGENT)
                .header(X_OX_HTTP_TEST_HEADER_NAME, USER_AGENT);

        if (params.containsKey("body")) {
            String bodyStr = JSONCoercion.coerceToJSON(params.remove("body")).toString();
            builder.put(RequestBody.create(bodyStr, MediaType.parse("application/json; charset=UTF-8")));
            okhttp3.HttpUrl.Builder urlBuilder = host.newBuilder(BASE + module);
            params.forEach((k, v) -> urlBuilder.addQueryParameter(k, v == null ? null : v.toString()));
            builder.url(urlBuilder.build());
        } else {
            Builder formBodyBuilder = new FormBody.Builder();
            params.forEach((k, v) -> formBodyBuilder.add(k, v == null ? null : v.toString()));
            builder.post(formBodyBuilder.build()).url(host.newBuilder(BASE + module).build());
        }

        return client.newCall(builder.build()).execute();
    }

    private Map<String, Object> M(Object... parameters) {
        HashMap<String, Object> map = new HashMap<String, Object>();

        for (int i = 0; i < parameters.length; i++) {
            map.put(parameters[i++].toString(), parameters[i]);
        }

        return map;
    }

    public String getUserAgent() {
        return USER_AGENT;
    }

    public CookieStore getCookieStore() {
        return cookieManager.getCookieStore();
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Response executeGet(String uri) throws IOException {
        return executeGet(uri, Optional.empty());
    }

    public Response executeGet(String uri, Optional<Map<String, String>> optHeader) throws IOException {
        okhttp3.Request.Builder builder = new Request.Builder().get().url(host + uri);
        optHeader.ifPresent(headerMap -> headerMap.forEach((k, v) -> builder.addHeader(k, v)));
        return client.newCall(builder.build()).execute();
    }

}
