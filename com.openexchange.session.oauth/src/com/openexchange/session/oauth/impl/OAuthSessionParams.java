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

package com.openexchange.session.oauth.impl;

import com.openexchange.session.Session;

/**
 * {@link OAuthSessionParams} - Simple wrapper for session parameters holding OAuth information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class OAuthSessionParams {

    private final String accessToken;
    private final String expiryString;
    private final String refreshToken;
    private final String idToken;

    /**
     * Initializes a new {@link OAuthSessionParams}.
     *
     * @param session The session to grab parameters from
     */
    public OAuthSessionParams(Session session) {
        this((String) session.getParameter(Session.PARAM_OAUTH_ACCESS_TOKEN), (String) session.getParameter(Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE), (String) session.getParameter(Session.PARAM_OAUTH_REFRESH_TOKEN), (String) session.getParameter(Session.PARAM_OAUTH_ID_TOKEN));
    }

    /**
     * Initializes a new {@link OAuthSessionParams}.
     *
     * @param accessToken The parameter value for OAuth access token
     * @param expiryString The parameter value for OAuth expiry string
     * @param refreshToken The parameter value for OAuth refresh token
     * @param idToken The parameter value for OIDC id token
     */
    public OAuthSessionParams(String accessToken, String expiryString, String refreshToken, String idToken) {
        super();
        this.accessToken = accessToken;
        this.expiryString = expiryString;
        this.refreshToken = refreshToken;
        this.idToken = idToken;
    }

    /**
     * Gets the parameter value for OAuth access token.
     *
     * @return The parameter value for OAuth access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Gets the parameter value for OAuth expiry string.
     *
     * @return The parameter value for OAuth expiry string
     */
    public String getExpiryString() {
        return expiryString;
    }

    /**
     * Gets the parameter value for OAuth refresh token.
     *
     * @return The parameter value for OAuth refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Gets the parameter value for OIDC id token.
     *
     * @return The parameter value for OIDC id token, or <code>null</code> if not set
     */
    public String getIDToken() {
        return idToken;
    }

    /**
     * Applies wrapped the OAuth session parameters wrapped by this instance to given session.
     *
     * @param session The session to apply to
     * @return The session
     */
    public Session applyTo(Session session) {
        if (session != null) {
            session.setParameter(Session.PARAM_OAUTH_ACCESS_TOKEN, accessToken);
            session.setParameter(Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE, expiryString);
            session.setParameter(Session.PARAM_OAUTH_REFRESH_TOKEN, refreshToken);
            session.setParameter(Session.PARAM_OAUTH_ID_TOKEN, idToken);
        }
        return session;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (accessToken != null) {
            builder.append("access_token=").append(Integer.toHexString(accessToken.hashCode())).append(", ");
        }
        if (expiryString != null) {
            builder.append("expires_in=").append(expiryString).append(", ");
        }
        if (refreshToken != null) {
            builder.append("refresh_token=").append(Integer.toHexString(refreshToken.hashCode())).append(", ");
        }
        if (idToken != null) {
            builder.append("id_token=").append(Integer.toHexString(idToken.hashCode()));
        }
        builder.append(']');
        return builder.toString();
    }



}
