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

package com.openexchange.logging;

import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * {@link Markers} - Utility class for markers.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class Markers {

    /**
     * Initializes a new {@link Markers}.
     */
    private Markers() {
        super();
    }

    /**
     * Gets the resulting marker for given arguments.
     *
     * @param marker The marker
     * @param references The optional references
     * @return The marker
     */
    public static Marker getMarkerWithReferences(Marker marker, Marker... references) {
        if (marker == null) {
            throw new IllegalArgumentException("Marker must not be null");
        }

        if (references == null || references.length <= 0 || Arrays.stream(references).noneMatch(Objects::nonNull)) {
            return marker;
        }

        Marker retval = MarkerFactory.getMarker(marker.getName());
        for (Marker reference : references) {
            if (reference != null) {
                retval.add(reference);
            }
        }
        return retval;
    }

}
