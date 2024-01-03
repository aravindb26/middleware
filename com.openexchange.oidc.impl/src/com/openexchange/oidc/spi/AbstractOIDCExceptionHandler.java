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

package com.openexchange.oidc.spi;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.impl.ContextExceptionCodes;
import com.openexchange.groupware.upgrade.SegmentedUpdateService;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;
import com.openexchange.oidc.OIDCBackendConfig;
import com.openexchange.oidc.OIDCExceptionCode;
import com.openexchange.oidc.OIDCExceptionHandler;
import com.openexchange.tools.servlet.http.Tools;

public abstract class AbstractOIDCExceptionHandler implements OIDCExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractOIDCExceptionHandler.class);
    private final SegmentedUpdateService segmentedUpdateService;
    private OIDCBackendConfig config;

    public AbstractOIDCExceptionHandler(OIDCBackendConfig config) {
        this(config, null);
    }
    
    /**
     * 
     * Initializes a new {@link AbstractOIDCExceptionHandler}
     * with the possibility to use segmented updates / upgrading without downtimes.
     *
     * @param config The oidc backend config.
     * @param segmentedUpdateService The segmented update service.
     */
    public AbstractOIDCExceptionHandler(OIDCBackendConfig config, SegmentedUpdateService segmentedUpdateService) {
        this.config = config;
        this.segmentedUpdateService = segmentedUpdateService;
    }

    protected String getDefaultErrorResponsePage(OXException exception, HttpServletRequest request) {
        String result = "";

        boolean isOauthFailure = exception == null;
        String headTitle = "";
        String category = "error_category: ";
        String errorType = "error_type: ";
        String errorDescription = "error_description: ";
        if (isOauthFailure) {
            headTitle = "Authentication provider error";
            category += "oauth-error";
            String error = request.getParameter("error");
            errorType += error != null ? StringEscapeUtils.escapeHtml4(error) : "";
            String errorDesc = request.getParameter("error_description");
            errorDescription += errorDesc != null ? StringEscapeUtils.escapeHtml4(errorDesc) : "";
        } else {
            headTitle = "500 - Internal Server Error";
            category += "ox-error";
            errorType += "internal server error";
            errorDescription += "An internal error occurred, please try again later.";
        }

        result += "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "  <head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <title>"+ headTitle + "</title>\n" +
            "  </head>\n" +
            "<body>\n" +
            "<h1>An error occured</h1>\n" +
            "<p>" + category + "</p>\n" +
            "<p>" + errorType + "</p>\n" +
            "<p>" + errorDescription + "</p>\n" +
            "</body></html>\n";
        return result;
    }

    @Override
    public void handleAuthenticationFailed(HttpServletRequest request, HttpServletResponse response, OXException exception) throws IOException {
        LOG.trace("handleAuthenticationFailed(request: {}, HttpServletResponse response, OXException: {})", request.getRequestURI(), exception.getExceptionCode());
        String failureRedirectLocation = this.config.getFailureRedirect();
        if (!failureRedirectLocation.isEmpty() && exception.getCode() == OIDCExceptionCode.INVALID_AUTHENTICATION_STATE_NO_USER.getNumber()) {
            response.sendRedirect(failureRedirectLocation);
            return;
        }
        // Redirect for segmented updates
        if (ContextExceptionCodes.LOCATED_IN_ANOTHER_SERVER.equals(exception)) {
            if (segmentedUpdateService == null) {
                LOG.error("Cannot redirect. Segmented Update Service is unavailable.");
            } else {
                try {
                    String migrationRedirectURL = segmentedUpdateService.getMigrationRedirectURL(request.getServerName());
                    if (Strings.isEmpty(migrationRedirectURL)) {
                        LOG.error("Cannot redirect. The property 'com.openexchange.server.migrationRedirectURL' is not set.");
                    } else {
                        sendRedirect(response, exception, migrationRedirectURL);
                        return;
                    }
                } catch (Exception e) {
                    LOG.error("Caught error while trying to get migration redirect URL", e);
                }

                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } catch (IOException | IllegalStateException x) {
                    LOG.error("Caught error while trying to send error after failing to send redirect", x);
                }
            }
        }
        handleResponseException(request, response, exception);
    }

    @Override
    public void handleLogoutFailed(HttpServletRequest request, HttpServletResponse response, OXException exception) throws IOException {
        LOG.trace("handleLogoutFailed(request: {}, HttpServletResponse response, OXException: {})", request.getRequestURI(), exception.getExceptionCode());
        this.handleResponseException(request, response, exception);
    }

    @Override
    public void handleResponseException(HttpServletRequest request, HttpServletResponse response, OXException exception) throws IOException {
        LOG.trace("handleException(request: {}, HttpServletResponse response, OXException: {})", request.getRequestURI(), exception.getMessage());
        LOG.error(exception.getMessage(), exception);
        String content = this.getDefaultErrorResponsePage(exception, request);
        if (Strings.isEmpty(content)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Tools.disableCaching(response);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setCharacterEncoding(Charsets.UTF_8_NAME);
        response.setContentType("text/html");
        response.setContentLength(content.getBytes(Charsets.UTF_8).length);
        response.getWriter().write(content);
    }

    private void sendRedirect(HttpServletResponse httpResponse, OXException exception, String redirectUri) {
        try {
            httpResponse.sendRedirect(redirectUri);
            LOG.debug("Sent redirect to {} due to {}", redirectUri, exception.getMessage());
        } catch (IOException e) {
            LOG.error("Caught error while trying to send redirect", e);
            try {
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException | IllegalStateException x) {
                // nothing to do here
                LOG.error("Caught error while trying to send error after failing to send redirect", x);
            }
        }
    }

}
