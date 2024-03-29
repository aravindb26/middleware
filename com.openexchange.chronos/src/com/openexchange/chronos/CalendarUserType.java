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

package com.openexchange.chronos;

import com.openexchange.annotation.NonNull;
import com.openexchange.java.EnumeratedProperty;

/**
 * {@link CalendarUserType}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 * @see <a href="https://tools.ietf.org/html/rfc5545#section-3.2.3">RFC 5545, section 3.2.3</a>
 */
public class CalendarUserType extends EnumeratedProperty {

    /**
     * An individual.
     */
    @NonNull
    public static final CalendarUserType INDIVIDUAL = new CalendarUserType("INDIVIDUAL");

    /**
     * A group of individuals.
     */
    @NonNull
    public static final CalendarUserType GROUP = new CalendarUserType("GROUP");

    /**
     * A physical resource.
     */
    @NonNull
    public static final CalendarUserType RESOURCE = new CalendarUserType("RESOURCE");

    /**
     * A room resource.
     */
    @NonNull
    public static final CalendarUserType ROOM = new CalendarUserType("ROOM");

    /**
     * Otherwise not known.
     */
    @NonNull
    public static final CalendarUserType UNKNOWN = new CalendarUserType("UNKNOWN");

    /**
     * Initializes a new {@link CalendarUserType}.
     *
     * @param value The property value
     */
    public CalendarUserType(String value) {
        super(value);
    }

    @Override
    public String getDefaultValue() {
        return INDIVIDUAL.getValue();
    }

    @Override
    protected String[] getStandardValues() {
        return getValues(INDIVIDUAL, GROUP, RESOURCE, ROOM, UNKNOWN);
    }

}
