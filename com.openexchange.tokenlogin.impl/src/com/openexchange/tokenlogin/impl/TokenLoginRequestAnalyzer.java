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
package com.openexchange.tokenlogin.impl;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.request.analyzer.utils.RequestAnalyzerUtils.createAnalyzeResult;
import static com.openexchange.tokenlogin.impl.TokenLoginUtility.extractSessionId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.BodyData;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.RequestURL;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link TokenLoginRequestAnalyzer} is a {@link RequestAnalyzer} which uses a login token
 * to determine the marker for the request.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class TokenLoginRequestAnalyzer implements RequestAnalyzer {

    /** The name of the token parameter as used in the redeem action */
    private static final String PARAMETER_TOKEN = "token"; // com.openexchange.ajax.fields.LoginFields.TOKEN

    /** The value of the action parameter hinting to the redeem action */
    private static final String ACTION_REDEEM_TOKEN = "redeemToken"; // com.openexchange.ajax.LoginServlet.ACTION_REDEEM_TOKEN

    private final ServiceLookup services;
    
    /**
     * Initializes a new {@link TokenLoginRequestAnalyzer}.
     *
     * @param services The {@link ServiceLookup} to use
     */
    public TokenLoginRequestAnalyzer(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        // Check if applicable
        if (false == isRedeemTokenParameter(data.getParsedURL())) {
            // analyzer is not responsible for this request
            return Optional.empty();
        }
        
        // Require body to be present
        Optional<BodyData> optBody = data.optBody();
        if (optBody.isEmpty()) {
            return Optional.of(AnalyzeResult.MISSING_BODY);
        }
        
        // Get token
        String token = optBodyParameterValue(optBody.get(), PARAMETER_TOKEN);
        if (Strings.isEmpty(token)) {
            return Optional.of(AnalyzeResult.UNKNOWN);
        }

        // Parse token to session id
        String sessionId;
        try {
            sessionId = extractSessionId(services.getServiceSafe(ObfuscatorService.class), token);
        } catch (Exception e) {
            // unparsable token, let real request fail anywhere
            org.slf4j.LoggerFactory.getLogger(TokenLoginRequestAnalyzer.class).debug("Error extracting session id from token {}: {}", token, e.getMessage(), e);
            return Optional.of(AnalyzeResult.UNKNOWN);
        }
        
        // Get session for id & create appropriate result
        Session session = services.getServiceSafe(SessiondService.class).peekSession(sessionId);
        return Optional.of(createAnalyzeResult(session));
    }

    /**
     * Parses the parameters from the given <code>application/application/x-www-form-urlencoded</code> request body data as name-value-pairs.
     * 
     * @param bodyData The body data to parse
     * @return The parsed form data parameters
     */
    private static List<NameValuePair> parseBodyParameters(BodyData bodyData) {
        if (null != bodyData) {
            try {
                return URLEncodedUtils.parse(bodyData.getDataAsString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(TokenLoginRequestAnalyzer.class).debug("Error parsing form parameters from body: {}", e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Parses the supplied form data body and gets the value of a specific parameter if present.
     * 
     * @param bodyData The body data to parse and get the parameter value from
     * @param parameterName The name of the parameter to get the value for
     * @return The parameter value, or <code>null</code> if not set
     */
    private static String optBodyParameterValue(BodyData bodyData, String parameterName) {
        List<NameValuePair> parameters = parseBodyParameters(bodyData);
        if (null != parameters) {
            for (NameValuePair parameter : parameters) {
                if (parameterName.equals(parameter.getName())) {
                    return parameter.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Checks if the given url contains the {@value #ACTION_REDEEM_TOKEN} action
     *
     * @param url The url to check
     * @return <code>true</code> if the url contains the action, <code>false</code> otherwise
     * @throws OXException In case the URL cannot be evaluated
     */
    private static boolean isRedeemTokenParameter(RequestURL url) throws OXException {
        return b(url.optParameter("action")
                    .map(action -> B(ACTION_REDEEM_TOKEN.equals(action)))
                    .orElse(Boolean.FALSE));
    }

}
