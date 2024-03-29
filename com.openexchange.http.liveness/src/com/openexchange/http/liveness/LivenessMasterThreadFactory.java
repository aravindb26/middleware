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

package com.openexchange.http.liveness;

import java.util.concurrent.ThreadFactory;

/**
 * Thread factory creating the single master thread that is then used in a single thread executor service to create new threads for the
 * thread pool.
 *
 * This is necessary to ensure that threads for the thread pool are always created with the correct OSGi thread context class loader. See
 * bug 26072 for threads in the thread pool created with the wrong thread context class loader.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
final class LivenessMasterThreadFactory implements ThreadFactory {

    private final String namePrefix;

    /**
     * Initializes a new {@link LivenessMasterThreadFactory}.
     *
     * @param namePrefix The prefix of the thread name
     */
    LivenessMasterThreadFactory(String namePrefix) {
        super();
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return newCustomThread(r, namePrefix + "LivenessThreadCreator");
    }

    public static Thread newCustomThread(Runnable runnable, String threadName) {
        return new Thread(runnable, threadName);
    }
}
