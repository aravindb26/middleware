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


package com.openexchange.processing.internal.watcher;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import com.openexchange.java.AbstractOperationsWatcher;

/**
 * {@link ProcessorTaskInfo} - Information about a processor task.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ProcessorTaskInfo extends AbstractOperationsWatcher.Operation {

    /** The constant dummy instance */
    public static final ProcessorTaskInfo POISON = new ProcessorTaskInfo() {

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

    /**
     * Dummy constructor
     */
    private ProcessorTaskInfo() {
        super();
    }

    /**
     * Initializes a new {@link InputStreamInfo}.
     *
     * @param reader The thread reading from stream
     * @param in The stream the gets read out
     * @param timeoutMillis The timeout in milliseconds
     */
    public ProcessorTaskInfo(Thread reader, long timeoutMillis) {
        super(reader, timeoutMillis);
    }

}
