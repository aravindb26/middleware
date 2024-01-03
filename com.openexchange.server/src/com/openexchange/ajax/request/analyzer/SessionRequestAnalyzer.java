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
package com.openexchange.ajax.request.analyzer;

import static com.openexchange.request.analyzer.utils.RequestAnalyzerUtils.createAnalyzeResult;
import java.util.Optional;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.exception.OXException;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link SessionRequestAnalyzer} is a {@link RequestAnalyzer} which uses the session id
 * to determine the marker for the request
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class SessionRequestAnalyzer extends AbstractPrefixAwareRequestAnalyzer {

    /**
     * Initializes a new {@link SessionRequestAnalyzer}.
     *
     * @param services The {@link ServiceLookup} to use
     * @throws OXException If initialization fails
     */
    public SessionRequestAnalyzer(ServiceLookup services) throws OXException {
        super(services);
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        if (false == hasValidPrefix(data)) {
            return Optional.empty();
        }
        // parse session parameter
        Optional<String> optSessionId = data.getParsedURL().optParameter(AJAXServlet.PARAMETER_SESSION);
        if (optSessionId.isEmpty()) {
            // analyzer is not responsible for this request
            return Optional.empty();
        }
        // Get session for id
        Session session = sessiondService.get().peekSession(optSessionId.get());
        return Optional.of(createAnalyzeResult(session));
    }

}
