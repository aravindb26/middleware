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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.DelayQueue;

/**
 * {@link Idn2AceControl} - Collects ongoing IDN to ACE conversions.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class Idn2AceControl {

    private static final Idn2AceControl INSTANCE = new Idn2AceControl();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static Idn2AceControl getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    private final DelayQueue<Idn2AceTask> queue;

    /**
     * Initializes a new {@link Idn2AceControl}.
     */
    private Idn2AceControl() {
        super();
        queue = new DelayQueue<>();
    }

    /**
     * Adds the specified task.
     *
     * @param task The task
     * @return <tt>true</tt>
     */
    public boolean add(Idn2AceTask task) {
        return queue.offer(task);
    }

    /**
     * Removes the specified task.
     *
     * @param task The task to remove
     * @return <code>true</code> if such a task was removed; otherwise <code>false</code>
     */
    public boolean remove(Idn2AceTask task) {
        return queue.remove(task);
    }

    /**
     * Await expired parse tasks from this control.
     *
     * @return The expired parse tasks
     * @throws InterruptedException If interrupted while waiting
     */
    List<Idn2AceTask> awaitExpired() throws InterruptedException {
        Idn2AceTask expired = queue.take();
        List<Idn2AceTask> expirees = new LinkedList<Idn2AceTask>();
        expirees.add(expired);
        queue.drainTo(expirees);
        return expirees;
    }

    /**
     * Removes expired parse tasks from this control.
     *
     * @return The expired parse tasks
     */
    List<Idn2AceTask> removeExpired() {
        List<Idn2AceTask> expirees = new LinkedList<Idn2AceTask>();
        queue.drainTo(expirees);
        return expirees;
    }

}
