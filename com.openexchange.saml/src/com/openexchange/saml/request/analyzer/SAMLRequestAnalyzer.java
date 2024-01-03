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
package com.openexchange.saml.request.analyzer;

import java.util.Optional;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.database.DatabaseService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.exception.OXException;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.UserInfo;
import com.openexchange.saml.tools.SAMLLoginTools;
import com.openexchange.segment.SegmentMarker;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.reservation.Reservation;
import com.openexchange.session.reservation.SessionReservationService;

/**
 * {@link SAMLRequestAnalyzer} is a {@link RequestAnalyzer} which uses the session reservation token
 * of a saml request to determine the marker for the request
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class SAMLRequestAnalyzer implements RequestAnalyzer {
    
    private final ServiceLookup services;
    private final String loginPrefix;
    private final String loginActionName;

    /**
     * Initializes a new {@link OIDCRequestAnalyzer}.
     *
     * @param services A service lookup reference
     * @param loginActionName The actual name of the login action to analyze the request for
     * @throws OXException If initialization fails
     */
    public SAMLRequestAnalyzer(ServiceLookup services, String loginActionName) throws OXException {
        super();
        this.services = services;
        this.loginActionName = loginActionName;
        this.loginPrefix = services.getServiceSafe(DispatcherPrefixService.class).getPrefix() + LoginServlet.SERVLET_PATH_APPENDIX;
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        /*
         * check if applicable
         */
        Optional<String> path = data.getParsedURL().getPath();
        if (path.isEmpty() || false == path.get().startsWith(loginPrefix)) {
            return Optional.empty(); // other servlet
        }
        Optional<String> actionParameter = data.getParsedURL().optParameter(LoginServlet.PARAMETER_ACTION);
        if (actionParameter.isEmpty() || false == actionParameter.get().equals(loginActionName)) {
            return Optional.empty(); // other login action
        }
        /*
         * extract & lookup reservation for session token
         */
        Optional<String> sessionTokenParameter = data.getParsedURL().optParameter(SAMLLoginTools.PARAM_TOKEN);
        if (sessionTokenParameter.isEmpty()) {
            return Optional.of(AnalyzeResult.UNKNOWN);   
        }
        Reservation reservation = services.getServiceSafe(SessionReservationService.class).getReservation(sessionTokenParameter.get());
        if (null == reservation) {
            return Optional.of(AnalyzeResult.UNKNOWN);
        }
        /*
         * construct & return analyze result for reserved session
         */
        String schemaName = services.getServiceSafe(DatabaseService.class).getSchemaName(reservation.getContextId());
        UserInfo userInfo = UserInfo.builder(reservation.getContextId()).withUserId(reservation.getUserId()).build();
        return Optional.of(new AnalyzeResult(SegmentMarker.of(schemaName), userInfo));
    }

}
