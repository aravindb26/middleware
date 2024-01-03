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

package com.openexchange.imap.debug;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.Marker;
import com.openexchange.logging.Constants;
import com.openexchange.logging.Markers;

/**
 * {@link SimpleLoggerCallingPrintStream} - A print stream writing passed bytes to an instance of <code>org.slf4j.Logger</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.4
 */
public class SimpleLoggerCallingPrintStream extends AbstractLoggerCallingPrintStream {

    private final org.slf4j.Logger logger;
    private final Marker marker;

    /**
     * Initializes a new {@link SimpleLoggerCallingPrintStream}.
     *
     * @param logger The logger to call
     */
    public SimpleLoggerCallingPrintStream(org.slf4j.Logger logger) {
        super();
        if (logger == null) {
            throw new IllegalArgumentException("Logger must not be null");
        }
        this.logger = logger;
        this.marker = Markers.getMarkerWithReferences(Constants.DROP_MDC_MARKER, Constants.ONLY_MESSAGE_MARKER);
    }

    @Override
    protected void logBufferContent(StringBuffer buf, Logger logger) {
        logger.info(marker, "{}", buf);
    }

    @Override
    protected Optional<Logger> createLogger() {
        return Optional.of(logger);
    }

}
