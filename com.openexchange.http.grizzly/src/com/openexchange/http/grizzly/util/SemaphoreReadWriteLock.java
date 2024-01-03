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

package com.openexchange.http.grizzly.util;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * {@link SemaphoreReadWriteLock}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class SemaphoreReadWriteLock {

    private final Lock readLock;
    private final Lock writeLock;

    /**
     * Initializes a new {@link SemaphoreReadWriteLock}.
     */
    public SemaphoreReadWriteLock() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Initializes a new {@link SemaphoreReadWriteLock}.
     *
     * @param numOfConcurrentRequests The number of concurrently permitted requests
     */
    public SemaphoreReadWriteLock(final int numOfConcurrentRequests) {
        super();

        // A semaphore, which does support fairness
        final Semaphore semaphore = new Semaphore(numOfConcurrentRequests, true);

        readLock = new Lock() {

            @Override
            public void unlock() {
                semaphore.release();
            }

            @Override
            public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
                return semaphore.tryAcquire(time, unit);
            }

            @Override
            public boolean tryLock() {
                return semaphore.tryAcquire();
            }

            @Override
            public Condition newCondition() {
                throw new UnsupportedOperationException("SemaphoreReadWriteLock.readLock.newCondition()");
            }

            @Override
            public void lockInterruptibly() throws InterruptedException {
                semaphore.acquire();
            }

            @Override
            public void lock() {
                semaphore.acquireUninterruptibly();
            }
        };

        writeLock = new Lock() {

            @Override
            public void unlock() {
                semaphore.release(numOfConcurrentRequests);
            }

            @Override
            public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
                return semaphore.tryAcquire(numOfConcurrentRequests, time, unit);
            }

            @Override
            public boolean tryLock() {
                return semaphore.tryAcquire(numOfConcurrentRequests);
            }

            @Override
            public Condition newCondition() {
                throw new UnsupportedOperationException("SemaphoreReadWriteLock.writeLock.newCondition()");
            }

            @Override
            public void lockInterruptibly() throws InterruptedException {
                semaphore.acquire(numOfConcurrentRequests);
            }

            @Override
            public void lock() {
                semaphore.acquireUninterruptibly(numOfConcurrentRequests);
            }
        };
    }

    /**
     * Gets the read lock
     *
     * @return The read lock
     */
    public Lock getReadLock() {
        return readLock;
    }

    /**
     * Gets the write lock
     *
     * @return The write lock
     */
    public Lock getWriteLock() {
        return writeLock;
    }

}
