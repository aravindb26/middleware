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

package com.openexchange.database.cleanup.impl.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * {@link IncreaseThreadsBeforeQueueingThreadPoolExecutor}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class IncreaseThreadsBeforeQueueingThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     * Initializes a new {@link IncreaseThreadsBeforeQueueingThreadPoolExecutor}.
     */
    public IncreaseThreadsBeforeQueueingThreadPoolExecutor(int nThreads, int nMaxThreads, ThreadFactory threadFactory) {
        // See java.util.concurrent.Executors.newFixedThreadPool(int, ThreadFactory)
        super(nThreads, nMaxThreads, 0L, TimeUnit.MILLISECONDS, new OfferIfEmptyLinkedBlockingQueue(), threadFactory, new QueuePuttingRejectedExecutionHandler());
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static final class QueuePuttingRejectedExecutionHandler implements RejectedExecutionHandler {

        /**
         * Initializes a new {@link QueuePuttingRejectedExecutionHandler}.
         */
        public QueuePuttingRejectedExecutionHandler() {
            super();
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                // This does the actual put into the queue. Once the max. number of threads have been reached, the tasks will be queued
                executor.getQueue().put(r);
                // Do this after the put() to prevent from race conditions
                if (executor.isShutdown()) {
                    throw new RejectedExecutionException("Task " + r + " rejected from " + executor);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class OfferIfEmptyLinkedBlockingQueue extends LinkedBlockingQueue<Runnable> {

        private static final long serialVersionUID = 7705646616250115417L;

        /**
         * Initializes a new {@link OfferIfEmptyLinkedBlockingQueue}.
         */
        public OfferIfEmptyLinkedBlockingQueue() {
            super();
        }

        @Override
        public boolean offer(Runnable e) {
            /*-
             * Offer it to the queue if there is 0 items already queued, else return false so the ThreadPoolExecutor
             * will add another thread.
             *
             * If we return false and max threads have been reached then the RejectedExecutionHandler will be called
             * which will do the put into the queue.
             */
            return isEmpty() && super.offer(e);
        }
    }

}
