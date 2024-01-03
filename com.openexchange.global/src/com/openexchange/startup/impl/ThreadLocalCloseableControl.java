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

package com.openexchange.startup.impl;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.openexchange.java.Streams;
import com.openexchange.marker.KnownThreadLocalValue;
import com.openexchange.marker.OXThreadMarker;
import com.openexchange.startup.CloseableControlService;


/**
 * {@link ThreadLocalCloseableControl} - The singleton Closeable control.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ThreadLocalCloseableControl implements CloseableControlService {

    private static final ThreadLocalCloseableControl INSTANCE = new ThreadLocalCloseableControl();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static ThreadLocalCloseableControl getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------------ //

    /** The current closeables */
    final ThreadLocal<Queue<Closeable>> closeables;

    /**
     * Initializes a new {@link ThreadLocalCloseableControl}.
     */
    private ThreadLocalCloseableControl() {
        super();
        closeables = new ThreadLocal<Queue<Closeable>>();
    }

    private Queue<Closeable> getQueue() {
        Thread t = Thread.currentThread();
        return t instanceof OXThreadMarker ? ((OXThreadMarker) t).getThreadLocalValue(KnownThreadLocalValue.CLOSABLES.getIdentifier()) : closeables.get();
    }

    private Queue<Closeable> requireQueue() {
        Queue<Closeable> queue;
        Thread t = Thread.currentThread();
        if (t instanceof OXThreadMarker) {
            OXThreadMarker oxt = (OXThreadMarker) t;
            queue = oxt.getThreadLocalValue(KnownThreadLocalValue.CLOSABLES.getIdentifier());
            if (null == queue) {
                Queue<Closeable> nq = new ConcurrentLinkedQueue<Closeable>();
                oxt.putThreadLocalValue(KnownThreadLocalValue.CLOSABLES.getIdentifier(), nq);
                queue = nq;
            }
        } else {
            queue = closeables.get();
            if (null == queue) {
                Queue<Closeable> nq = new ConcurrentLinkedQueue<Closeable>();
                closeables.set(nq);
                queue = nq;
            }
        }
        return queue;
    }

    @Override
    public boolean addCloseable(Closeable closeable) {
        return null == closeable ? false : requireQueue().offer(closeable);
    }

    @Override
    public boolean removeCloseable(Closeable closeable) {
        if (null == closeable) {
            return false;
        }

        Queue<Closeable> queue = getQueue();
        return null == queue ? false : queue.remove(closeable);
    }

    @Override
    public Collection<Closeable> getCurrentCloseables() {
        Queue<Closeable> queue = getQueue();
        return null == queue ? Collections.<Closeable> emptyList() : Collections.<Closeable> unmodifiableCollection(queue);
    }

    @Override
    public void closeAll() {
        Queue<Closeable> queue = getQueue();
        if (null != queue) {
            for (Closeable closeable; (closeable = queue.poll()) != null;) {
                Streams.close(closeable);
            }
        }
    }

}
