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

import org.slf4j.Marker;

/**
 * {@link Constants} - Useful constants for logging.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class Constants {

    /**
     * Initializes a new {@link Constants}.
     */
    private Constants() {
        super();
    }

    /**
     * The marker telling the Open-Xchange logging system to drop MDC for a certain log message
     * <p>
     * See <code>com.openexchange.logback.extensions.converters.LineMDCConverter</code>.
     */
    public static final Marker DROP_MDC_MARKER = ImmutableMarker.builder("DropMDC").build();

    /**
     * The marker telling the Open-Xchange logging system to output only the message w/o e.g. time stamp, class, etc.
     * <p>
     * <b>Does not work reliably by now!</b>
     * <p>
     * See <code>com.openexchange.logback.extensions.encoders.ExtendedPatternLayoutEncoder.OnlyMessageAwarePatternLayout</code>.
     */
    public static final Marker ONLY_MESSAGE_MARKER = ImmutableMarker.builder("OnlyMessage").build();

}
