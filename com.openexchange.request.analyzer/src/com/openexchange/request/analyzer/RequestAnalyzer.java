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
package com.openexchange.request.analyzer;

import java.util.Optional;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.Ranked;

/**
 * {@link RequestAnalyzer}s are used by the {@link RequestAnalyzerService} to analyze the request.
 * Every {@link RequestAnalyzer} helps to map requests to their markers for a certain use case.
 * <p>
 * Implementations should be registered with a service ranking or extend {@link Ranked}.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public interface RequestAnalyzer {

    /**
     * Analyzes specified request data.
     * <p>
     * If request data is not applicable for this analyzer, an empty result is returned.
     *
     * @param data The request data to analyze
     * @return The analyze result or empty (if not applicable)
     * @throws OXException In case of errors
     */
    Optional<AnalyzeResult> analyze(RequestData data) throws OXException;

}
