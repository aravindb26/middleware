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

import java.util.Objects;
import java.util.Optional;
import com.openexchange.segment.SegmentMarker;

/**
 * {@link AnalyzeResult} - The analyze result for a request.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class AnalyzeResult {

    /** The constant for unknown analyzer result */
    public static final AnalyzeResult UNKNOWN = new AnalyzeResult(Type.UNKNOWN);

    /** The constant for missing body analyzer result */
    public static final AnalyzeResult MISSING_BODY = new AnalyzeResult(Type.MISSING_BODY);

    // ------------------------------------------------------------------------------------

    private final Type type;
    private final SegmentMarker marker;
    private final UserInfo userInfo;

    /**
     * Initializes a new {@link Type#SUCCESS} {@link AnalyzeResult}.
     *
     * @param marker The marker to return to client
     * @param userInfo Informations about the user associated with the request
     */
    public AnalyzeResult(SegmentMarker marker, UserInfo userInfo) {
        super();
        Objects.requireNonNull(marker);
        this.type = Type.SUCCESS;
        this.userInfo = userInfo;
        this.marker = marker;
    }

    /**
     * Initializes a new AnalyzeResult with the given type.
     *
     * @param type The type of the analyze result
     */
    private AnalyzeResult(Type type) {
        super();
        this.type = type;
        this.userInfo = null;
        this.marker = null;
    }

    /**
     * Gets the type
     *
     * @return The type
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the marker if the {@link AnalyzeResult} is of type {@link Type#SUCCESS}
     *
     * @return The optional marker
     */
    public Optional<SegmentMarker> optMarker() {
        return Optional.ofNullable(marker);
    }

    /**
     * Gets the user info if the {@link AnalyzeResult} is of type {@link Type#SUCCESS}
     *
     * @return The optional user info
     */
    public Optional<UserInfo> optUserInfo() {
        return Optional.ofNullable(userInfo);
    }

    @Override
    public String toString() {
        return "AnalyzeResult [type=" + type + ", marker=" + marker + ", userInfo=" + userInfo + "]";
    }

}
