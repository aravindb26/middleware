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

package com.openexchange.streamcontrol.internal;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import com.openexchange.java.AbstractOperationsWatcher;
import com.openexchange.java.InterruptibleInputStream;

/**
 * {@link InputStreamInfo} - The information about a controlled input stream.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class InputStreamInfo extends AbstractOperationsWatcher.Operation {

    /** The constant dummy instance */
    public static final InputStreamInfo POISON = new InputStreamInfo() {

        @Override
        public int compareTo(Delayed o) {
            return -1;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }
    };

    // -------------------------------------------------------------------------------------------------------------------------------------

    /** The stream the gets read out */
    private final InterruptibleInputStream in;

    /**
     * Dummy constructor
     */
    private InputStreamInfo() {
        super();
        in = null;
    }

    /**
     * Initializes a new {@link InputStreamInfo}.
     *
     * @param reader The thread reading from stream
     * @param in The stream the gets read out
     * @param timeoutMillis The timeout in milliseconds
     */
    public InputStreamInfo(Thread reader, InterruptibleInputStream in, int timeoutMillis) {
        super(reader, timeoutMillis);
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeout must not be less than or equal to 0 (zero)");
        }
        this.in = in;
    }

    /**
     * Gets the stream the gets read out
     *
     * @return The stream
     */
    public InterruptibleInputStream getIn() {
        return in;
    }

}
