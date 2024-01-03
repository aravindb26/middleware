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

package com.openexchange.deputy.json;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXActionServiceFactory;
import com.openexchange.deputy.json.action.AbstractDeputyAction;
import com.openexchange.deputy.json.action.AllDeputyAction;
import com.openexchange.deputy.json.action.AvailableModulesDeputyAction;
import com.openexchange.deputy.json.action.DeleteDeputyAction;
import com.openexchange.deputy.json.action.GetDeputyAction;
import com.openexchange.deputy.json.action.NewDeputyAction;
import com.openexchange.deputy.json.action.ReverseDeputyAction;
import com.openexchange.deputy.json.action.UpdateDeputyAction;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link DeputyActionFactory}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyActionFactory implements AJAXActionServiceFactory {

    private static final String MODULE = "deputy";

    /**
     * Gets the <code>"deputy"</code> module identifier for deputy permission action factory.
     *
     * @return The module identifier
     */
    public static String getModule() {
        return MODULE;
    }

    // ------------------------------------------------------------------------------------------

    private final Map<String, AbstractDeputyAction> actions;

    /**
     * Initializes a new {@link DeputyActionFactory}.
     */
    public DeputyActionFactory(ServiceLookup services) {
        super();
        ImmutableMap.Builder<String, AbstractDeputyAction> actions = ImmutableMap.builderWithExpectedSize(8);
        actions.put("available", new AvailableModulesDeputyAction(services));
        actions.put("get", new GetDeputyAction(services));
        actions.put("all", new AllDeputyAction(services));
        actions.put("reverse", new ReverseDeputyAction(services));
        actions.put("new", new NewDeputyAction(services));
        actions.put("delete", new DeleteDeputyAction(services));
        actions.put("update", new UpdateDeputyAction(services));
        this.actions = actions.build();
    }

    @Override
    public AJAXActionService createActionService(String action) throws OXException {
        return actions.get(action);
    }

}
