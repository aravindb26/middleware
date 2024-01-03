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


package com.openexchange.java;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * {@link AbstractOperationsWatcher} - An abstract watcher for operations that are supposed to be aborted after a certain timeout.
 *
 * @param <O> The type of the operation to watch
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public abstract class AbstractOperationsWatcher<O extends AbstractOperationsWatcher.Operation> {

    /**
     * The abstract operation class that is watched.
     */
    public abstract static class Operation implements Delayed {

        /** The processing thread */
        private final Thread processingThread;

        private final long stamp;

        /**
         * Initializes a new dummy {@link Operation}.
         */
        protected Operation() {
            super();
            this.processingThread = null;
            this.stamp = 0;
        }

        /**
         * Initializes a new {@link Operation}.
         *
         * @apram processingThread The processing thread
         * @param timeoutMillis The timeout in milliseconds
         * @throws IllegalArgumentException If timeout must not be less than or equal to <code>0</code> (zero)
         */
        protected Operation(Thread processingThread, long timeoutMillis) {
            super();
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("Timeout must not be less than or equal to 0 (zero)");
            }
            this.processingThread = processingThread;
            this.stamp = System.currentTimeMillis() + timeoutMillis;
        }

        /**
         * Gets the processing thread
         *
         * @return The processing thread
         */
        public Thread getProcessingThread() {
            return processingThread;
        }

        @Override
        public int compareTo(Delayed o) {
            long otherStamp = ((Operation) o).stamp;
            return (this.stamp < otherStamp ? -1 : (this.stamp == otherStamp ? 0 : 1));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long toGo = stamp - System.currentTimeMillis();
            return unit.convert(toGo, TimeUnit.MILLISECONDS);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String name;
    private final Duration waitForExpiredOperationsDuration;
    private final LockOfferingDelayQueue<O> queue;
    private boolean stopped;
    private Thread checker;

    /**
     * Initializes a new {@link AbstractOperationsWatcher}.
     *
     * @param name The name for this watcher
     * @param waitForExpiredOperationsDuration The duration to wait for expired operations before aborting watching thread
     */
    protected AbstractOperationsWatcher(String name, Duration waitForExpiredOperationsDuration) {
        super();
        this.name = name;
        this.waitForExpiredOperationsDuration = waitForExpiredOperationsDuration;
        this.queue = new LockOfferingDelayQueue<>();
        this.stopped = false;
    }

    /**
     * Gets the name
     *
     * @return The name
     */
    protected String getName() {
        return name;
    }

    /**
     * Gets the duration to wait for expired operations before aborting watching thread.
     *
     * @return The duration to wait for expired operations before aborting watching thread
     */
    protected Duration getWaitForExpiredOperationsDuration() {
        return waitForExpiredOperationsDuration;
    }

    private void startCheckerIfNeeded() { // Only invoked if lock is held
        if (checker != null) {
            // Nothing to do
            return;
        }

        Thread newChecker = new Thread(new CheckerTask<O>(this), name);
        newChecker.start();
        this.checker = newChecker;
    }

    private List<O> awaitExpired(long timeout, TimeUnit unit) throws InterruptedException {
        O expired = queue.poll(timeout, unit);
        if (expired == null) {
            // Timed out...
            Lock lock = queue.getLock();
            lock.lock();
            try {
                if (queue.isEmpty()) {
                    // No more elements in queue. Stop checker thread.
                    stopChecker();
                }
            } finally {
                lock.unlock();
            }
            return Collections.emptyList();
        }

        // Drain further expired elements from queue
        List<O> expirees = new LinkedList<>();
        expirees.add(expired);
        queue.drainTo(expirees);
        return expirees;
    }

    private void handleExpiredOperationSafely(O operation) {
        try {
            handleExpiredOperation(operation);
        } catch (Exception e) {
            getLogger().error("Failed to handle expired instance of {}", operation.getClass().getName(), e);
        }
    }

    private void handleUnwatchedOperationSafely(O operation, boolean removed) {
        try {
            handleUnwatchedOperation(operation, removed);
        } catch (Exception e) {
            getLogger().error("Failed to handle unwatched instance of {}", operation.getClass().getName(), e);
        }
    }

    private void stopChecker() { // Only invoked if lock is held
        if (checker == null) {
            // Nothing to do
            return;
        }

        if (!queue.offer(getPoisonElement())) {
            getLogger().info("Failed to add poison element into quere");
        }
        checker.interrupt();
        checker = null;
    }

    /**
     * Adds the specified operation instance to this watcher.
     *
     * @param operation The operation instance
     * @return <code>true</code> if added; otherwise <code>false</code>
     */
    public boolean add(O operation) {
        Lock lock = queue.getLock();
        lock.lock();
        try {
            if (stopped) {
                // Already shut down
                return false;
            }

            if (queue.offer(operation)) {
                // Offered to queue
                startCheckerIfNeeded();
                return true;
            }

            // Failed to offer to queue
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the specified operation instance from this watcher.
     *
     * @param operation The operation instance
     * @return <code>true</code> if such an operation was removed; otherwise <code>false</code>
     */
    public boolean remove(O operation) {
        if (operation == null) {
            // Huh...?
            return false;
        }
        boolean removed = queue.remove(operation);
        handleUnwatchedOperationSafely(operation, removed);
        return removed;
    }

    /**
     * Shuts-down this watcher instance.
     */
    public void shutDown() {
        Lock lock = queue.getLock();
        lock.lock();
        try {
            stopped = true;
            stopChecker();
            queue.clear();
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the poison element.
     *
     * @return The poison element
     */
    protected abstract O getPoisonElement();

    /**
     * Gets the logger to use.
     *
     * @return The logger
     */
    protected abstract org.slf4j.Logger getLogger();

    /**
     * Handles specified expired operation.
     *
     * @param operation The expired operation to handle
     * @throws Exception If handling expired operation fails
     */
    protected abstract void handleExpiredOperation(O operation) throws Exception;

    /**
     * Handles specified unwatched operation.
     *
     * @param operation The unwatched operation to handle
     * @param removed <code>true</code> if operation has been removed from watcher; otherwise <code>false</code> signaling removed by checker task
     * @throws Exception If handling unwatched operation fails
     */
    protected void handleUnwatchedOperation(O operation, boolean removed) throws Exception {
        // Nothing by default
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final class CheckerTask<O extends AbstractOperationsWatcher.Operation> implements Runnable {

        private final AbstractOperationsWatcher<O> watcher;

        /**
         * Initializes a new {@link CheckerTask}.
         *
         * @param watcher The watcher instance
         */
        private CheckerTask(AbstractOperationsWatcher<O> watcher) {
            super();
            this.watcher = watcher;
        }

        @Override
        public void run() {
            try {
                watcher.getLogger().info("Started watcher {}", watcher.getName());
                long waitMillis = watcher.getWaitForExpiredOperationsDuration().toMillis();
                boolean poisoned = false;
                while (!poisoned) {
                    for (O operation : watcher.awaitExpired(waitMillis, TimeUnit.MILLISECONDS)) {
                        if (operation == watcher.getPoisonElement()) {
                            poisoned = true;
                        } else {
                            watcher.handleExpiredOperationSafely(operation);
                        }
                    }
                }
                watcher.getLogger().info("Stopped watcher {}", watcher.getName());
            } catch (InterruptedException e) {
                // Likely by calling stopChecker(). Keep interrupted status
                Thread.currentThread().interrupt();
                watcher.getLogger().debug("Interrupted watcher {}", watcher.getName(), e);
            } catch (Exception e) {
                watcher.getLogger().error("Aborted watcher {}", watcher.getName(), e);
            }
        }
    }

}
