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

package org.glassfish.grizzly.http.server;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@link EmptyWriteLock}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public class EmptyWriteLock extends java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock {

    private static final long serialVersionUID = -7671295270015164735L;

    private static final EmptyWriteLock INSTANCE = new EmptyWriteLock();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static EmptyWriteLock getInstance() {
        return INSTANCE;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link EmptyWriteLock}.
     */
    private EmptyWriteLock() {
        super(new ReentrantReadWriteLock());
    }

    @Override
    public void unlock() {
        // ignore
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public Condition newCondition() {
        return EmptyReadLock.EMPTY_CONDITION;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        // ignore
    }

    @Override
    public void lock() {
        // ignore
    }

}
