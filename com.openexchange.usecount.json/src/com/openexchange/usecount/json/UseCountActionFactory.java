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

package com.openexchange.usecount.json;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXActionServiceFactory;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.usecount.json.action.AbstractUseCountAction;
import com.openexchange.usecount.json.action.IncrementUseCountAction;

/**
 * {@link UseCountActionFactory}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class UseCountActionFactory implements AJAXActionServiceFactory {

    private static final String MODULE = "usecount";

    /**
     * Gets the <code>"usecount"</code> module identifier for use-count action factory.
     *
     * @return The module identifier
     */
    public static String getModule() {
        return MODULE;
    }

    // ------------------------------------------------------------------------------------------

    private final Map<String, AbstractUseCountAction> actions;

    /**
     * Initializes a new {@link UseCountActionFactory}.
     */
    public UseCountActionFactory(ServiceLookup services) {
        super();
        ImmutableMap.Builder<String, AbstractUseCountAction> actions = ImmutableMap.builderWithExpectedSize(2);
        actions.put("increment", new IncrementUseCountAction(services));
        this.actions = actions.build();
    }

    @Override
    public AJAXActionService createActionService(String action) throws OXException {
        return actions.get(action);
    }

}
