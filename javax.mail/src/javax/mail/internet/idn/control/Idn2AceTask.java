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

package javax.mail.internet.idn.control;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import gnu.inet.encoding.IDNAException;

/**
 * {@link Idn2AceTask} - Performs the IDN to ACE conversion through invoking:
 * <pre>
 * gnu.inet.encoding.IDNA.toASCII(domain, true);
 * </pre>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class Idn2AceTask implements Callable<String>, Delayed {

    /** The special poison task to stop taking from queue */
    public static final Idn2AceTask POISON = new Idn2AceTask(null, 0) {

        @Override
        public int compareTo(Delayed o) {
            return -1;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0L;
        }
    };

    // ---------------------------------------------------------------------------------------------------------------------------

    private final String domain;
    private final int timeoutSec;
    private volatile long stamp;
    private volatile Thread worker;

    /**
     * Initializes a new {@link Idn2AceTask}.
     *
     * @param domain The domain
     * @param timeoutSec The timeout seconds
     */
    public Idn2AceTask(String domain, int timeoutSec) {
        super();
        this.domain = domain;
        this.timeoutSec = timeoutSec;
    }

    @Override
    public int compareTo(Delayed o) {
        long thisStamp = this.stamp;
        long otherStamp = ((Idn2AceTask) o).stamp;
        return (thisStamp < otherStamp ? -1 : (thisStamp == otherStamp ? 0 : 1));
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long toGo = stamp - System.currentTimeMillis();
        return unit.convert(toGo, TimeUnit.MILLISECONDS);
    }

    @Override
    public String call() throws IDNAException {
        Idn2AceControl control = Idn2AceControl.getInstance();
        stamp = System.currentTimeMillis() + (timeoutSec * 1000L);
        worker = Thread.currentThread();
        control.add(this);
        try {
            return gnu.inet.encoding.IDNA.toASCII(domain, true);
        } finally {
            control.remove(this);
        }
    }

    /**
     * Interrupts this task (if currently processed).
     */
    public void interrupt() {
        Thread worker = this.worker;
        if (null != worker) {
            worker.interrupt();
        }
    }

}
