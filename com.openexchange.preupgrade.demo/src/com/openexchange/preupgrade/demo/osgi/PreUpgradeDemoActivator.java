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

package com.openexchange.preupgrade.demo.osgi;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.preupgrade.PreUpgradeTask;
import com.openexchange.preupgrade.demo.DummyTask;

/**
 * {@link PreUpgradeDemoActivator}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class PreUpgradeDemoActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link PreUpgradeDemoActivator}.
     */
    public PreUpgradeDemoActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {};
    }

    @Override
    protected void startBundle() throws Exception {
        getDummyTasks(10).stream().forEach(task -> registerService(PreUpgradeTask.class, task, 0));
    }

    /**
     * Creates a random amount of dummy tasks
     * 
     * @param the maximum amount of tasks
     *
     * @return a list with dummy tasks
     */
    private List<PreUpgradeTask> getDummyTasks(int amount) {
        SecureRandom random = new SecureRandom();
        int tasks = Math.max(random.nextInt(amount), 1);
        List<PreUpgradeTask> t = new ArrayList<>(tasks);
        for (int i = 0; i < tasks; i++) {
            t.add(new DummyTask(Math.max(random.nextInt(20), 1)));
        }
        return t;
    }

}
