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

package com.openexchange.preupgrade.demo;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.preupgrade.AbstractPreUpgradeTask;
import com.openexchange.preupgrade.PreUpgradeTask;

/**
 * {@link DummyTask} - Simply dummy test for testing the progress monitor framework
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class DummyTask extends AbstractPreUpgradeTask implements PreUpgradeTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyTask.class);
    private static final SecureRandom random = new SecureRandom();
    private int totalSteps;

    /**
     * Initializes a new {@link DummyTask}.
     */
    public DummyTask(int totalSteps) {
        super();
        this.totalSteps = totalSteps;
    }

    @Override
    public int prepareUpgrade() throws OXException {
        setRequired(random.nextBoolean());
        StringBuilder builder = new StringBuilder("The following changes will be performed: \n");
        for (int i = 0; i < totalSteps; i++) {
            builder.append("  ").append(UUID.randomUUID()).append("\n");
        }
        LOGGER.info(builder.toString());
        return totalSteps;
    }

    @Override
    public void executeUpgrade() throws OXException {
        for (int i = 0; i < totalSteps; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(random.nextLong(1000));
                // 5% chance to finish a bunch right away.
                // 50% chance to finish random tasks at a time.
                double chance = random.nextDouble(1);
                if (chance <= 0.05f) {
                    i = totalSteps;
                    done();
                } else if (chance >= 0.5f) {
                    int progress = Math.max(random.nextInt(totalSteps - i), 1);
                    i += (progress - 1);
                    progress(progress);
                } else {
                    progress(1);
                }
            } catch (InterruptedException e) {
            }
        }
        done();
    }
}
