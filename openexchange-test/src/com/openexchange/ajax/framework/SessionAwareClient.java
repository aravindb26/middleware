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

import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.TestUserConfig;
import com.openexchange.test.common.test.TestUserConfig.ClientTimeouts;
import com.openexchange.test.common.test.pool.TestUser;
import okhttp3.*;
import okhttp3.OkHttpClient.Builder;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.openexchange.testing.httpclient.invoker.ApiClient;

/**
 * {@link SessionAwareClient}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.5
 */
public class SessionAwareClient extends ApiClient {

    private String session;
    private String userAgent;
    private final TestUser user;
    private Integer userId;

    /**
     * Initializes a new {@link SessionAwareClient}.
     *
     * @param user The testuser
     */
    public SessionAwareClient(TestUser user) {
        this(user, Optional.empty(), Optional.empty());
    }

    /**
     * Initializes a new {@link SessionAwareClient}.
     *
     * @param user The testuser
     * @param optConfig The optional user config
     * @param userAgent The client's userAgent to use
     */
    public SessionAwareClient(TestUser user, Optional<TestUserConfig> optConfig, Optional<String> userAgent) {
        super();
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieJar jar = new JavaNetCookieJar(cookieManager);
        Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(jar);
        if(optConfig.isPresent() && optConfig.get().optTimeouts().isPresent()) {
            ClientTimeouts timeouts = optConfig.get().optTimeouts().get();
            timeouts.optConTimeout().ifPresent(duration -> builder.connectTimeout(duration.getSeconds(), TimeUnit.SECONDS));
            timeouts.optReadTimeout().ifPresent(duration -> builder.readTimeout(duration.getSeconds(), TimeUnit.SECONDS));
        } else {
            // add default timeouts
            builder.connectTimeout(Long.parseLong(AJAXConfig.getProperty(AJAXConfig.Property.CONNECTION_TIMOUT)), TimeUnit.MILLISECONDS);
            builder.readTimeout(Long.parseLong(AJAXConfig.getProperty(AJAXConfig.Property.READ_TIMOUT)), TimeUnit.MILLISECONDS);
        }
        if(userAgent.isPresent()) {
            this.userAgent = userAgent.get();
            setUserAgent(userAgent.get());
        }

        builder.hostnameVerifier((hostname, session) -> true);
        builder.addInterceptor(chain -> {
            Request request = chain.request();
            Request newRequest = request.newBuilder()
                    .addHeader(ClientCommons.X_OX_HTTP_TEST_HEADER_NAME, user.getCreatedBy())
                    .build();
            return chain.proceed(newRequest);
        });
        builder.connectionPool(new ConnectionPool(10,5, TimeUnit.SECONDS));
        OkHttpClient httpClient = builder.build();
        this.setHttpClient(httpClient);
        this.user = user;
    }

    /**
     * Gets the session id
     *
     * @return
     */
    public String getSession() {
        return session;
    }

    /**
     * Sets the session
     *
     * @param session The session to set
     */
    public void setSession(String session) {
        this.session = session;
        this.setApiKey(session);
    }

    /**
     * Gets the user
     *
     * @return The user
     */
    public TestUser getUser() {
        return user;
    }

    /**
     * getUserId
     *
     * @return
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * setUserId
     *
     * @param userId
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    /**
     * Sets the client's user agent
     *
     * @param userAgent The client's user agent
     */
    public void setClient(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Gets the client identifier
     *
     * @return The client identifier
     */
    public String getUserAgent() {
        return this.userAgent;
    }

    /**
     * Checks whether the client has the given user agent set
     *
     * @param userAgent The user agent to check
     * @return <code>True</code> if the given user agent matches the user agent of this {@link SessionAwareClient} instance
     */
    public boolean hasUserAgent(String userAgent) {
        return ((userAgent == null && this.userAgent == null) || (this.userAgent != null && this.userAgent.equals(userAgent)));
    }
}
