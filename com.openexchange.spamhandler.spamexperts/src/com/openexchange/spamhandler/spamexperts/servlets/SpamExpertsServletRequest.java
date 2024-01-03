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

package com.openexchange.spamhandler.spamexperts.servlets;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.common.io.BaseEncoding;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;
import com.openexchange.session.Session;
import com.openexchange.spamhandler.spamexperts.exceptions.SpamExpertsExceptionCode;
import com.openexchange.spamhandler.spamexperts.management.SpamExpertsConfig;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * This request handler currently know these actions:
 *
 *
 */
public final class SpamExpertsServletRequest {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SpamExpertsServletRequest.class);

    public static final String ACTION_GET_NEW_PANEL_SESSION = "generate_panel_session";

    // ------------------------------------------------------------------------------------------------------------

	private final Session session;
	private final User user;
    private final SpamExpertsConfig config;
    private final ManagedHttpClient httpClient;

	/**
	 * Initializes a new {@link SpamExpertsServletRequest}.
	 *
	 * @param session The session providing user data
	 * @param config The configuration
	 * @param httpClient The HTTP client to use
	 * @throws OXException If initialization fails
	 */
	public SpamExpertsServletRequest(Session session, SpamExpertsConfig config, ManagedHttpClient httpClient) throws OXException {
		super();
	    this.session = session;
        this.config = config;
        this.user = getUserFrom(session);
        this.httpClient = httpClient;
	}

	private static User getUserFrom(Session session) throws OXException {
	    if (session instanceof ServerSession) {
            return ((ServerSession) session).getUser();
        }

	    return UserStorage.getInstance().getUser(session.getUserId(), session.getContextId());
	}

    /**
     * Performs the specified action
     *
     * @param action The action identifier
     * @param jArgs The request's arguments
     * @return The result object
     * @throws OXException If request fails
     */
    public Object action(String action, JSONObject jArgs) throws OXException {
        try {
            if (action.equalsIgnoreCase(ACTION_GET_NEW_PANEL_SESSION)) {
                // create new panel session id and return it
                return actionGetNewPanelSession(jArgs);
            }

            throw AjaxExceptionCodes.UNKNOWN_ACTION.create(action);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

	/**
	 * Returns the spamexperts panel session ID<br>
	 * Needed by UI to redirect to Panel
	 *
	 * @param jArgs The request's arguments
	 * @return
	 * @throws AbstractOXException
	 * @throws JSONException
	 */
	private JSONObject actionGetNewPanelSession(final JSONObject jArgs) throws OXException, JSONException {
		try {
			// new parameter version to optionally determine the UI version
			String uiVersion = jArgs.optString("version", null);
			LOG.debug("trying to create new spamexperts panel session for user {} in context {}", getCurrentUserUsername(), I(getCurrentUserContextID()));

			// create complete new session identifier
			String sessionid = createPanelSessionID(uiVersion);
            if (sessionid == null) {
                throw SpamExpertsExceptionCode.SPAMEXPERTS_COMMUNICATION_ERROR.create("save and cache session", "invalid data for sessionid");
            }
            LOG.debug("new spamexperts panel session created for user {} in context {}", getCurrentUserUsername(), I(getCurrentUserContextID()));

			String panelWebUiUrl = config.getUriProperty(session, "com.openexchange.custom.spamexperts.panel.web_ui_url", "http://demo1.spambrand.com/?authticket=").toString();

			// add valid data to response
			// UI Plugin will format session and URL and redirect
			JSONObject jsonResponseObject = new JSONObject(2);
			jsonResponseObject.put("panel_session",sessionid); // send session id
			jsonResponseObject.put("panel_web_ui_url", panelWebUiUrl); // send UI URL
			return jsonResponseObject;
		} catch (ClientProtocolException e) {
			LOG.error("http communication error detected with spamexperts api interface", e);
			throw SpamExpertsExceptionCode.HTTP_COMMUNICATION_ERROR.create(e.getMessage());
		} catch (IOException e) {
			LOG.error("IO error occured while communicating with spamexperts api interface");
			throw SpamExpertsExceptionCode.HTTP_COMMUNICATION_ERROR.create(e.getMessage());
		}
	}

	private static String AUTH_ID_MAIL ="mail";
	private static String AUTH_ID_LOGIN ="login";
	private static String AUTH_ID_IMAP_LOGIN ="imaplogin";
	private static String AUTH_ID_USERNAME ="username";

	private String createPanelSessionID(final String uiVersion) throws OXException, IOException {
	    String admin = config.requireProperty(session, "com.openexchange.custom.spamexperts.panel.admin_user");
	    String password = config.requireProperty(session, "com.openexchange.custom.spamexperts.panel.admin_password").trim();

		String authid; // FALLBACK IS MAIL
        {
            String authid_attribute = config.getPropertyFor(session, "com.openexchange.custom.spamexperts.panel.api_auth_attribute", "mail", String.class).trim();
            if (authid_attribute.equals(AUTH_ID_IMAP_LOGIN)) {
                authid = this.user.getImapLogin();
            } else if (authid_attribute.equals(AUTH_ID_LOGIN)) {
                authid = this.session.getLogin();
            } else if (authid_attribute.equals(AUTH_ID_MAIL)) {
                authid = this.user.getMail();
            } else if (authid_attribute.equals(AUTH_ID_USERNAME)) {
                authid = this.session.getUserlogin();
            } else {
                authid = this.user.getMail();
            }
        }

		LOG.debug("Using {} as authID string from user {} in context {} to authenticate against panel API", authid, getCurrentUserUsername(), I(getCurrentUserContextID()));

		HttpGet getRequest = null;
		HttpResponse getResponse = null;
		try {
		    // call the API to retrieve the URL to access panel
		    List<NameValuePair> queryString = null;
		    if (null != uiVersion) {
		        queryString = new ArrayList<>(1);
		        queryString.add(new BasicNameValuePair("version", uiVersion));
		    }
            getRequest = new HttpGet(buildUri(getURIFor(authid), queryString, null));
		    setAuthorizationHeader(getRequest, admin, password);

            getResponse = httpClient.execute(getRequest);
            StatusLine statusLine = getResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                LOG.error("HTTP request to create new spamexperts panel session failed with status: {}", statusLine);
                throw SpamExpertsExceptionCode.SPAMEXPERTS_COMMUNICATION_ERROR.create("create panel authticket", statusLine);
            }

            HttpEntity entity = getResponse.getEntity();
            if (null == entity) {
                return null;
            }

            String resp = EntityUtils.toString(entity);
            LOG.debug("Got response for user {} in context {} from  panel API: \n{}", getCurrentUserUsername(), I(getCurrentUserContextID()), resp);

            if (Strings.isEmpty(resp)) {
                return null;
            }

            if (resp.indexOf("ERROR") != -1) {
                // ERROR DETECTED
                throw SpamExpertsExceptionCode.SPAMEXPERTS_COMMUNICATION_ERROR.create("create panel authticket", resp);
            }

            return resp;
        } catch (RuntimeException e) {
            LOG.error("runtime error", e);
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            HttpClients.close(getRequest, getResponse);
        }
    }

	private String getCurrentUserUsername(){
		return this.session.getLogin();
	}

	private int getCurrentUserContextID(){
		return this.session.getContextId();
	}

	private URI getURIFor(String authid) throws OXException {
	    String uriString = config.getUriProperty(session, "com.openexchange.custom.spamexperts.panel.api_interface_url", "http://demo1.spambrand.com/api/authticket/create/username/") + authid;
	    try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw OXException.general("Invalid URI: " + uriString, e);
        }
	}

	/**
     * Sets the Authorization header.
     *
     * @param request The HTTP request
     * @param login The login
     * @param password The password
     */
    private void setAuthorizationHeader(HttpRequestBase request, String login, String password) {
        String encodedCredentials = BaseEncoding.base64().encode((login + ":" + password).getBytes(Charsets.UTF_8));
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
    }

	/**
     * Builds the URI from given arguments
     *
     * @param baseUri The base URI
     * @param queryString The query string parameters
     * @return The built URI string
     * @throws IllegalArgumentException If the given string violates RFC 2396
     */
    @SuppressWarnings("deprecation")
    private URI buildUri(URI baseUri, List<NameValuePair> queryString, String optPath) {
        try {
            URIBuilder builder = new URIBuilder();
            builder.setScheme(baseUri.getScheme()).setHost(baseUri.getHost()).setPort(baseUri.getPort()).setPath(null == optPath ? baseUri.getPath() : optPath).setQuery(null == queryString ? null : URLEncodedUtils.format(queryString, "UTF-8"));
            return builder.build();
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException("Failed to build URI", x);
        }
    }
}
