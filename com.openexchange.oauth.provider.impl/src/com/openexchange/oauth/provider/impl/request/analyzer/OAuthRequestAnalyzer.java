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

package com.openexchange.oauth.provider.impl.request.analyzer;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.provider.authorizationserver.spi.AuthorizationException;
import com.openexchange.oauth.provider.authorizationserver.spi.OAuthAuthorizationService;
import com.openexchange.oauth.provider.authorizationserver.spi.ValidationResponse;
import com.openexchange.oauth.provider.authorizationserver.spi.ValidationResponse.TokenStatus;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.UserInfo;
import com.openexchange.segment.SegmentMarker;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.webdav.AuthorizationHeader;

/**
 * {@link OAuthRequestAnalyzer} is a {@link RequestAnalyzer} which uses the oauth token
 * to determine the marker for the request
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class OAuthRequestAnalyzer implements RequestAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthRequestAnalyzer.class);

    private final ServiceLookup services;

    /**
     * Initializes a new {@link OAuthRequestAnalyzer}.
     *
     * @param services The {@link ServiceLookup}
     */
    public OAuthRequestAnalyzer(ServiceLookup services) {
        this.services = services;
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        AuthorizationHeader header = AuthorizationHeader.parseSafe(data.getHeaders().getFirstHeaderValue("Authorization"));
        if (header == null || false == "bearer".equalsIgnoreCase(header.getScheme())) {
            return Optional.empty();
        }
        Optional<ValidationResponse> optResponse = validateToken(header.getAuthString());
        if (optResponse.isEmpty()) {
            return Optional.of(AnalyzeResult.UNKNOWN);
        }
        ValidationResponse response = optResponse.get();
        if (false == TokenStatus.VALID.equals(response.getTokenStatus())) {
            return Optional.of(AnalyzeResult.UNKNOWN);
        }

        int userId = response.getUserId();
        int contextId = response.getContextId();
        String schemaName = services.getServiceSafe(DatabaseService.class).getSchemaName(contextId);
        UserInfo userInfo = UserInfo.builder(contextId)
                                    .withUserId(userId)
                                    .build();
        return Optional.of(new AnalyzeResult(SegmentMarker.of(schemaName), userInfo));
    }

    /**
     * Validates the token if possible
     *
     * @param token The token to validate
     * @return The optional {@link ValidationResponse}
     */
    private Optional<ValidationResponse> validateToken(String token) {
        return services.ofOptionalService(OAuthAuthorizationService.class).map(service -> validateToken(service, token));
    }

    /**
     * Validates the token with the help of the {@link OAuthAuthorizationService}
     *
     * @param service The service to use
     * @param token The token to validate
     * @return The {@link ValidationResponse} or null in case of errors
     */
    private ValidationResponse validateToken(OAuthAuthorizationService service, String token) {
        try {
            return service.validateAccessToken(token);
        } catch (AuthorizationException e) {
            LOG.debug("Encountered an error while validating oauth token", e);
            return null;
        }
    }

}
