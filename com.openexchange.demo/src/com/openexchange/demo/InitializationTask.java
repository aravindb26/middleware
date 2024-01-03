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

package com.openexchange.demo;

import org.slf4j.Logger;
import com.openexchange.threadpool.AbstractTask;

/**
 * The task that performs initialization of demo system, which is triggered once all needed services are available.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class InitializationTask extends AbstractTask<Void> {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(InitializationTask.class);
    }

    private final ProvisioningInterfaces interfaces;
    private final InitializationPerformer initializer;

    /**
     * Initializes a new {@link InitializationTask}.
     *
     * @param interfaces The {@link ProvisioningInterfaces}
     * @param initializer The {@link InitializationPerformer}
     */
    public InitializationTask(ProvisioningInterfaces interfaces, InitializationPerformer initializer) {
        super();
        this.interfaces = interfaces;
        this.initializer = initializer;
    }

    @Override
    public Void call() {
        try {
            initializer.init(interfaces);
        } catch (Exception e) {
            LoggerHolder.LOG.error("Failed initialization of demo system.", e);
        }
        return null;
    }
}
