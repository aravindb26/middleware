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

package com.openexchange.cluster.serialization.session;

import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONCoercion;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.slf4j.Logger;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.Origin;
import com.openexchange.session.Session;
import com.openexchange.session.SessionVersionService;
import com.openexchange.session.VersionMismatchException;

/**
 * {@link SessionCodec} - The codec for session serialization in cluster.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public final class SessionCodec {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SessionCodec.class);
    }

    private static final AtomicReference<ObfuscatorService> OBFUSCATOR_REFERENCE = new AtomicReference<>();

    /**
     * Sets the obfuscator service.
     *
     * @param obfuscatorService The obfuscator service
     */
    public static void setObfuscatorService(ObfuscatorService obfuscatorService) {
        OBFUSCATOR_REFERENCE.set(obfuscatorService);
    }

    private static final AtomicReference<SessionVersionService> SESSION_VERSION_REFERENCE = new AtomicReference<>();

    /**
     * Sets the session version service.
     *
     * @param obfuscatorService The session version service
     */
    public static void setSessionVersionService(SessionVersionService sessionVersionService) {
        SESSION_VERSION_REFERENCE.set(sessionVersionService);
    }

    /**
     * Initializes a new {@link SessionCodec}.
     */
    private SessionCodec() {
        super();
    }

    private static JSONObject stream2json(InputStream jsonData) throws JSONException {
        try {
            return JSONServices.parseObject(jsonData);
        } finally {
            Streams.close(jsonData);
        }
    }

    /**
     * Gets the session from given JSON stream.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: Session's password is unobfuscated from JSON representation.
     * </div>
     *
     * @param jsonData The JSON stream
     * @return The session
     * @throws JSONException If conversion fails
     * @throws VersionMismatchException If version of stored session data does not match the expected version of the application
     * @throws IllegalStateException If tracked obfuscator or session version service is absent
     */
    public static ClusterSession stream2Session(InputStream jsonData) throws JSONException, VersionMismatchException {
        if (null == jsonData) {
            return null;
        }

        return json2Session(stream2json(jsonData));
    }

    /**
     * Gets the session from given JSON stream.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: Session's password is unobfuscated from JSON representation.
     * </div>
     *
     * @param jsonData The JSON stream
     * @param obfuscatorService The obfuscator to use
     * @param versionService The version service to obtain expected application version from
     * @return The session
     * throws JSONException If conversion fails
     * @throws VersionMismatchException If version of stored session data does not match the expected version of the application
     * @throws IllegalArgumentException If passed obfuscator or session version service is <code>null</code>
     */
    public static ClusterSession stream2Session(InputStream jsonData, ObfuscatorService obfuscatorService, SessionVersionService versionService) throws JSONException, VersionMismatchException {
        if (null == jsonData) {
            return null;
        }

        return json2Session(stream2json(jsonData), obfuscatorService, versionService);
    }

    /**
     * Gets the session from given JSON representation.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: Session's password is unobfuscated from JSON representation.
     * </div>
     *
     * @param jSession The JSON representation to convert
     * @return The session
     * @throws JSONException If conversion fails
     * @throws VersionMismatchException If version of stored session data does not match the expected version of the application
     * @throws IllegalStateException If tracked obfuscator or session version service is absent
     */
    public static ClusterSession json2Session(JSONObject jSession) throws JSONException, VersionMismatchException {
        if (null == jSession) {
            return null;
        }

        ObfuscatorService obfuscatorService = OBFUSCATOR_REFERENCE.get();
        if (obfuscatorService == null) {
            throw new IllegalStateException("Obfuscator is absent");
        }

        SessionVersionService versionService = SESSION_VERSION_REFERENCE.get();
        if (versionService == null) {
            throw new IllegalStateException("Session version service is absent");
        }

        return json2Session(jSession, obfuscatorService, versionService);
    }

    /**
     * Gets the session from given JSON representation.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: Session's password is unobfuscated from JSON representation.
     * </div>
     *
     * @param jSession The JSON representation to convert
     * @param obfuscatorService The obfuscator to use
     * @param versionService The version service to obtain expected application version from
     * @return The session
     * @throws JSONException If conversion fails
     * @throws VersionMismatchException If version of stored session data does not match the expected version of the application
     * @throws IllegalArgumentException If passed obfuscator or session version service is <code>null</code>
     */
    public static ClusterSession json2Session(JSONObject jSession, ObfuscatorService obfuscatorService, SessionVersionService versionService) throws JSONException, VersionMismatchException {
        if (jSession == null) {
            return null;
        }
        if (obfuscatorService == null) {
            throw new IllegalArgumentException("Obfuscator is absent");
        }
        if (versionService == null) {
            throw new IllegalArgumentException("Session version service is absent");
        }

        if (versionService.getVersion() > SessionVersionService.DEFAULT_VERSION) {
            int version = jSession.optInt("version", 1);
            if (version > SessionVersionService.DEFAULT_VERSION && version != versionService.getVersion()) {
                // Stored session data does not match expected version
                throw new VersionMismatchException(version, versionService.getVersion());
            }
        }

        String sessionId = jSession.optString("sessionId", null);
        int contextId = jSession.optInt("contextId", 0);
        int userId = jSession.optInt("userId", 0);
        String loginName = jSession.optString("loginName", null);
        String obfuscatedPassword = jSession.optString("password", null);
        String secret = jSession.optString("secret", null);
        String login = jSession.optString("login", null);
        String randomToken = jSession.optString("randomToken", null);
        String localIp = jSession.optString("localIp", null);
        String authId = jSession.optString("authId", null);
        String hash = jSession.optString("hash", null);
        String client = jSession.optString("client", null);
        String userLogin = jSession.optString("userLogin", null);
        Origin origin = Origin.originFor(jSession.optString("origin", null));
        boolean staySignedIn = jSession.optBoolean("staySignedIn", false);

        Map<String, Object> parameters = null;
        String optString = jSession.optString("alternativeId", null);
        if (optString != null) {
            parameters = new LinkedHashMap<String, Object>();
            parameters.put(Session.PARAM_ALTERNATIVE_ID, optString);
        }

        optString = jSession.optString("userAgent", null);
        if (optString != null) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>();
            }
            parameters.put(Session.PARAM_USER_AGENT, optString);
        }

        long loginTime = jSession.optLong("loginTime", -1);
        if (loginTime > 0) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>();
            }
            parameters.put(Session.PARAM_LOGIN_TIME, Long.valueOf(loginTime));
        }

        optString = jSession.optString("oauthAccessToken", null);
        if (optString != null) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>();
            }
            parameters.put(Session.PARAM_OAUTH_ACCESS_TOKEN, optString);
        }

        optString = jSession.optString("oauthRefreshToken", null);
        if (optString != null) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>();
            }
            parameters.put(Session.PARAM_OAUTH_REFRESH_TOKEN, optString);
        }

        optString = jSession.optString("oauthAccessTokenExpiryDate", null);
        if (optString != null) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>();
            }
            parameters.put(Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE, optString);
        }

        optString = jSession.optString("oauthIdToken", null);
        if (optString != null) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>();
            }
            parameters.put(Session.PARAM_OAUTH_ID_TOKEN, optString);
        }

        optString = jSession.optString("brand", null);
        if (optString != null) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>();
            }
            parameters.put(Session.PARAM_BRAND, optString);
        }

        optString = jSession.optString("userSchema", null);
        if (optString != null) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>();
            }
            parameters.put(Session.PARAM_USER_SCHEMA, optString);
        }

        JSONObject jParameters = jSession.optJSONObject("parameters");
        if (jParameters != null) {
            if (parameters == null) {
                parameters = new LinkedHashMap<String, Object>(jParameters.length());
            }
            for (Map.Entry<String, Object> entry : jParameters.entrySet()) {
                parameters.put(entry.getKey(), JSONCoercion.coerceToNative(entry.getValue()));
            }
        }

        return new ClusterSession(sessionId, loginName, obfuscatedPassword == null ? null : obfuscatorService.unobfuscate(obfuscatedPassword), contextId, userId, secret, login, randomToken, localIp, authId, hash, client, userLogin, staySignedIn, origin, parameters);
    }

    /**
     * Gets the JSON representation for given session.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: Session's password is obfuscated in JSON representation.
     * </div>
     *
     * @param session The session to convert
     * @return The session's JSON representation
     * @throws JSONException If conversion fails
     * @throws IllegalStateException If tracked obfuscator or session version service is absent
     */
    public static JSONObject session2Json(Session session) throws JSONException {
        if (null == session) {
            return null;
        }

        ObfuscatorService obfuscatorService = OBFUSCATOR_REFERENCE.get();
        if (obfuscatorService == null) {
            throw new IllegalStateException("Obfuscator is absent");
        }

        SessionVersionService versionService = SESSION_VERSION_REFERENCE.get();
        if (versionService == null) {
            throw new IllegalStateException("Session version service is absent");
        }

        return session2Json(session, obfuscatorService, versionService);
    }

    private static final Set<String> IGNOREES = Set.of(Session.PARAM_ALTERNATIVE_ID, Session.PARAM_USER_AGENT, Session.PARAM_LOGIN_TIME,
        Session.PARAM_OAUTH_ACCESS_TOKEN, Session.PARAM_OAUTH_REFRESH_TOKEN, Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE,
        Session.PARAM_BRAND, Session.PARAM_USER_SCHEMA, Session.PARAM_OAUTH_ID_TOKEN);

    /**
     * Gets the JSON representation for given session.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: Session's password is obfuscated in JSON representation.
     * </div>
     *
     * @param session The session to convert
     * @param obfuscatorService The obfuscator to use
     * @param versionService The version service to obtain expected application version from
     * @return The session's JSON representation
     * @throws JSONException If conversion fails
     * @throws IllegalArgumentException If passed obfuscator or session version service is <code>null</code>
     */
    public static JSONObject session2Json(Session session, ObfuscatorService obfuscatorService, SessionVersionService versionService) throws JSONException {
        if (session == null) {
            return null;
        }
        if (obfuscatorService == null) {
            throw new IllegalArgumentException("Obfuscator is absent");
        }
        if (versionService == null) {
            throw new IllegalArgumentException("Session version service is absent");
        }

        JSONObject jSession = new JSONObject(16);

        jSession.putOpt("sessionId", session.getSessionID());
        jSession.put("contextId", session.getContextId());
        jSession.put("userId", session.getUserId());
        jSession.put("version", versionService.getVersion());
        jSession.putOpt("loginName", session.getLoginName());
        jSession.putOpt("password", session.getPassword() == null ? null : obfuscatorService.obfuscate(session.getPassword()));
        jSession.putOpt("secret", session.getSecret());
        jSession.putOpt("login", session.getLogin());
        jSession.putOpt("randomToken", session.getRandomToken());
        jSession.putOpt("localIp", session.getLocalIp());
        jSession.putOpt("authId", session.getAuthId());
        jSession.putOpt("hash", session.getHash());
        jSession.putOpt("client", session.getClient());
        jSession.putOpt("userLogin", session.getUserlogin());
        jSession.putOpt("origin", session.getOrigin() == null ? null : session.getOrigin().name());
        jSession.put("staySignedIn", session.isStaySignedIn());

        Object optParameter;
        {
            optParameter = session.getParameter(Session.PARAM_ALTERNATIVE_ID);
            jSession.putOpt("alternativeId", optParameter == null ? null : optParameter.toString());
        }
        {
            optParameter = session.getParameter(Session.PARAM_USER_AGENT);
            jSession.putOpt("userAgent", optParameter == null ? null : optParameter.toString());
        }
        {
            optParameter = session.getParameter(Session.PARAM_LOGIN_TIME);
            jSession.put("loginTime", optParameter == null ? -1L : ((Long) optParameter).longValue());
        }

        {
            optParameter = session.getParameter(Session.PARAM_OAUTH_ACCESS_TOKEN);
            jSession.putOpt("oauthAccessToken", optParameter == null ? null : optParameter.toString());
        }
        {
            optParameter = session.getParameter(Session.PARAM_OAUTH_REFRESH_TOKEN);
            jSession.putOpt("oauthRefreshToken", optParameter == null ? null : optParameter.toString());
        }
        {
            optParameter = session.getParameter(Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE);
            jSession.putOpt("oauthAccessTokenExpiryDate", optParameter == null ? null : optParameter.toString());
        }
        {
            optParameter = session.getParameter(Session.PARAM_OAUTH_ID_TOKEN);
            jSession.putOpt("oauthIdToken", optParameter == null ? null : optParameter.toString());
        }
        {
            optParameter = session.getParameter(Session.PARAM_BRAND);
            jSession.putOpt("brand", optParameter == null ? null : optParameter.toString());
        }
        {
            optParameter = session.getParameter(Session.PARAM_USER_SCHEMA);
            jSession.putOpt("userSchema", optParameter == null ? null : optParameter.toString());
        }
        optParameter = null; // NOSONARLINT Help GC

        JSONObject jRemoteParameters = null;
        for (String parameterName : session.getParameterNames()) {
            if (!IGNOREES.contains(parameterName)) {
                optParameter = session.getParameter(parameterName);
                if (isSerializablePojo(optParameter)) {
                    if (jRemoteParameters == null) {
                        jRemoteParameters = new JSONObject();
                    }
                    try {
                        jRemoteParameters.putSafe(parameterName, JSONCoercion.coerceToJSON(optParameter));
                    } catch (java.lang.IllegalStateException e) {
                        // Max. size for JSON object exceeded
                        LoggerHolder.LOG.error("JSON representation for session {} could not be created. Too many (remote) parameters:{}{}", session.getSessionID(), Strings.getLineSeparator(), jRemoteParameters, e);
                        throw new JSONException("JSON representation for session " + session.getSessionID() + " could not be created", e);
                    }
                }
            }
        }
        jSession.putOpt("parameters", jRemoteParameters);

        return jSession;
    }

    private static final String POJO_PACKAGE = "java.lang.";

    private static boolean isSerializablePojo(Object obj) {
        return null == obj ? false : ((obj instanceof Serializable) && obj.getClass().getName().startsWith(POJO_PACKAGE));
    }

}
