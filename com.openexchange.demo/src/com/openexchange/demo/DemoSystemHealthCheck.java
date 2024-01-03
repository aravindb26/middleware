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

import java.util.LinkedHashMap;
import java.util.Map;
import com.openexchange.health.DefaultMWHealthCheckResponse;
import com.openexchange.health.MWHealthCheck;
import com.openexchange.health.MWHealthCheckResponse;

/**
 * {@link DemoSystemHealthCheck}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class DemoSystemHealthCheck implements MWHealthCheck {

    private static final String NAME = "demoSystem";

    private final InitializationPerformer initializationPerformer;

    /**
     * Initializes a new {@link DemoSystemHealthCheck}.
     */
    public DemoSystemHealthCheck(InitializationPerformer initializationPerformer) {
        super();
        this.initializationPerformer = initializationPerformer;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public MWHealthCheckResponse call() {
        InitializationState initializationState = initializationPerformer.getInitializationState();

        Map<String, Object> data = new LinkedHashMap<String, Object>(6);
        data.put("serverRegistered", Boolean.valueOf(initializationState.isServerRegistered()));
        data.put("filestoreRegistered", Boolean.valueOf(initializationState.isFilestoreRegistered()));
        data.put("databaseRegistered", Boolean.valueOf(initializationState.isDatabaseRegistered()));
        data.put("numberOfCreatedContexts", initializationState.getNumberOfCreatedContexts() + " of " + initializationState.getTotalNumberOfContexts());
        data.put("info", initializationState.getStateMessage());

        return new DefaultMWHealthCheckResponse(NAME, data, initializationState.isFinished());
    }

}
