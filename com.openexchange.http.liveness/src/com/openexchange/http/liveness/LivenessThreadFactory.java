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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LivenessThreadFactory} - The thread factory for HTTP liveness end-point.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class LivenessThreadFactory implements ThreadFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LivenessThreadFactory.class);

    private final AtomicInteger threadNumber;
    private final ExecutorService threadCreatorService;

    /**
     * Initializes a new {@link LivenessThreadFactory}.
     */
    public LivenessThreadFactory() {
        super();
        threadNumber = new AtomicInteger();
        threadCreatorService = Executors.newSingleThreadExecutor(new LivenessMasterThreadFactory("Liveness-"));
    }

    @Override
    public Thread newThread(Runnable r) {
        // Ensure a positive thread number
        int threadNum = threadNumber.incrementAndGet();
        if (threadNum <= 0) {
            do {
                if (threadNumber.compareAndSet(threadNum, 1)) {
                    threadNum = 1;
                } else {
                    threadNum = threadNumber.incrementAndGet();
                }
            } while (threadNum <= 0);
        }
        try {
            return createThreadWithMaster(r, threadNum);
        } catch (InterruptedException e) {
            LOG.error("Single thread pool for creating threads was interrupted.", e);
            return null;
        } catch (ExecutionException e) {
            LOG.error("Single thread pool for creating threads catched an exception while creating one.", e);
            return null;
        }
    }

    /**
     * Shuts-down this thread factory.
     */
    public void shutDown() {
        threadCreatorService.shutdownNow();
    }

    private static String getThreadName(String namePrefix, int threadNumber) {
        return new StringBuilder(namePrefix.length() + 3).append(namePrefix).append(String.format("%03d", Integer.valueOf(threadNumber))).toString();
    }

    private Thread createThreadWithMaster(Runnable r, int threadNum) throws InterruptedException, ExecutionException {
        ThreadCreateCallable callable = new ThreadCreateCallable(r, getThreadName("Liveness-", threadNum));
        Future<Thread> future = threadCreatorService.submit(callable);
        return future.get();
    }

    private static class ThreadCreateCallable implements Callable<Thread> {

        private final Runnable runnable;
        private final String threadName;

        ThreadCreateCallable(Runnable runnable, String threadName) {
            super();
            this.runnable = runnable;
            this.threadName = threadName;
        }

        @Override
        public Thread call() {
            return LivenessMasterThreadFactory.newCustomThread(runnable, threadName);
        }
    }

}
