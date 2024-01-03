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

package com.openexchange.filemanagement.json;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXActionServiceFactory;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link ManagedFileActionFactory}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ManagedFileActionFactory implements AJAXActionServiceFactory {

    private final Map<String, AJAXActionService> actions;

    /**
     * Initializes a new {@link ManagedFileActionFactory}.
     *
     * @param services The service look-up
     */
    public ManagedFileActionFactory(final ServiceLookup services) {
        super();
        ImmutableMap.Builder<String, AJAXActionService> actions = ImmutableMap.builder();
        actions.put("keepalive", new com.openexchange.filemanagement.json.actions.KeepaliveAction());
        actions.put("get", new com.openexchange.filemanagement.json.actions.GetAction());
        actions.put("new", new com.openexchange.filemanagement.json.actions.NewAction());
        actions.put("range", new com.openexchange.filemanagement.json.actions.RangeAction());
        this.actions = actions.build();
    }

    @Override
    public AJAXActionService createActionService(final String action) throws OXException {
        return actions.get(action);
    }

}
