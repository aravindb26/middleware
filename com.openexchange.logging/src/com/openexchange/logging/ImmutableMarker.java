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

import java.util.Iterator;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import com.openexchange.java.Strings;

/**
 * {@link ImmutableMarker} - Represents an immutable marker.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ImmutableMarker implements Marker {

    private static final long serialVersionUID = -8976113860726073684L;

    /**
     * Creates a new builder for an instance of <code>ImmutableMarker</code>.
     *
     * @param name The name of the marker
     * @return The created builder
     */
    public static Builder builder(String name) {
        if (Strings.isEmpty(name)) {
            throw new IllegalArgumentException("Name must not be null or empty");
        }
        return new Builder(name);
    }

    /** The builder for an instance of <code>ImmutableMarker</code> */
    public static class Builder {

        private final Marker marker;

        private Builder(String name) {
            super();
            marker = MarkerFactory.getMarker(name);
        }

        /**
         * Adds a marker reference.
         *
         * @param reference The reference to another marker
         * @throws IllegalArgumentException If 'reference' is null
         * @return This builder
         */
        public Builder add(Marker reference) {
            marker.add(reference);
            return this;
        }

        /**
         * Removes a marker reference.
         *
         * @param reference The marker reference to remove
         * @return This builder
         */
        public Builder remove(Marker reference) {
            marker.remove(reference);
            return this;
        }

        /**
         * Builds the instance of <code>ImmutableMarker</code> from this builder's arguments.
         *
         * @return The instance of <code>ImmutableMarker</code>
         */
        public ImmutableMarker build() {
            return new ImmutableMarker(marker);
        }

    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    private final Marker marker;

    /**
     * Initializes a new {@link ImmutableMarker}.
     *
     * @param marker The underlying marker
     */
    private ImmutableMarker(Marker marker) {
        super();
        this.marker = marker;
    }

    @Override
    public String getName() {
        return marker.getName();
    }

    @Override
    public void add(Marker reference) {
        throw new UnsupportedOperationException("ImmutableMarker.add()");
    }

    @Override
    public boolean remove(Marker reference) {
        throw new UnsupportedOperationException("ImmutableMarker.remove()");
    }

    @Override
    public boolean hasChildren() {
        return marker.hasChildren();
    }

    @Override
    public boolean hasReferences() {
        return marker.hasReferences();
    }

    @Override
    public Iterator<Marker> iterator() {
        return marker.iterator();
    }

    @Override
    public boolean contains(Marker other) {
        return marker.contains(other);
    }

    @Override
    public boolean contains(String name) {
        return marker.contains(name);
    }

    @Override
    public boolean equals(Object o) {
        return marker.equals(o);
    }

    @Override
    public int hashCode() {
        return marker.hashCode();
    }

}
