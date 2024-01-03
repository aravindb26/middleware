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

import java.util.List;

/**
 * {@link Idn2AceWatcher} - Responsible for interrupting expired threads currently performing IDN to ACE conversion.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class Idn2AceWatcher implements Runnable {

    /**
     * Initializes a new {@link Idn2AceWatcher}.
     */
    public Idn2AceWatcher() {
        super();
    }

    @Override
    public void run() {
        try {
            Thread runner = Thread.currentThread();
            Idn2AceControl control = Idn2AceControl.getInstance();
            while (!runner.isInterrupted()) {
                List<Idn2AceTask> expired = control.awaitExpired();
                boolean poisoned = expired.remove(Idn2AceTask.POISON);
                for (Idn2AceTask task : expired) {
                    // Parsing for too long
                    task.interrupt();
                }
                if (poisoned) {
                    return;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

}
