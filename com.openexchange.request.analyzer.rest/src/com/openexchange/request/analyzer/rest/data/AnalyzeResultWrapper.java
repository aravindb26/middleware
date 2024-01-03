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
package com.openexchange.request.analyzer.rest.data;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.Type;
import com.openexchange.segment.SegmentMarker;

/**
 * {@link AnalyzeResultWrapper} is a wrapper for analyze results
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class AnalyzeResultWrapper {

    private final String marker;
    private final Headers headers;
    private final Type type;

    /**
     * Initializes a new {@link AnalyzeResultWrapper}.
     *
     * @param delegate The {@link AnalyzeResult} to wrap
     */
    public AnalyzeResultWrapper(AnalyzeResult delegate) {
        super();
        this.type = delegate.getType();
        this.headers = delegate.optUserInfo()
                               .map(info -> new Headers(I(info.getContextId()), info.getUserId().orElse(null), info.getLogin().orElse(null)))
                               .orElse(null);
        this.marker = delegate.optMarker()
                              .map(SegmentMarker::encode)
                              .orElse(null);
    }

    /**
     * Gets the marker
     *
     * @return The marker
     */
    public String getMarker() {
        return marker;
    }

    /**
     * Gets the headers
     *
     * @return The headers
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Gets the type
     *
     * @return The type
     */
    public Type getType() {
        return type;
    }

}
