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

package com.openexchange.chronos.itip.json.actions;

import java.util.EnumSet;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXActionServiceFactory;
import com.openexchange.chronos.scheduling.ITipAction;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link ITipActionFactory}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class ITipActionFactory implements AJAXActionServiceFactory {

    private final Map<String, AJAXActionService> actions;

    /**
     * Initializes a new {@link ITipActionFactory}.
     * 
     * @param services The service lookup
     */
    public ITipActionFactory(ServiceLookup services) {
        ImmutableMap.Builder<String, AJAXActionService> actions = ImmutableMap.builder();
        EnumSet<ITipAction> iTipActions = EnumSet.allOf(ITipAction.class);
        /*
         * ignoring isn't supported for the old module, so remove from supported actions
         */
        iTipActions.remove(ITipAction.IGNORE);
        /*
         * apply actions aren't supported for the old module, so remove them from supported actions
         */
        for (ITipAction action : new ITipAction[] { ITipAction.APPLY_CREATE, ITipAction.APPLY_REMOVE, ITipAction.APPLY_REMOVE, ITipAction.APPLY_RESPONSE, ITipAction.APPLY_CHANGE }) {
            iTipActions.remove(action);
        }
        /*
         * use generic handler for certain actions
         */
        LegacySchedulingAction genericAction = new LegacySchedulingAction(services, SchedulingMethod.ADD, SchedulingMethod.CANCEL, SchedulingMethod.REPLY, SchedulingMethod.REQUEST, SchedulingMethod.REFRESH);
        for (ITipAction action : iTipActions) {
            actions.put(action.name().toLowerCase(), genericAction);
        }
        /*
         * also use generic handler for legacy 'create', 'update' and 'delete' actions
         */
        for (String legacyActionName : new String[] { "create", "update", "delete" }) {
            actions.put(legacyActionName, genericAction);
        }
        /*
         * prepare and enroll the initial 'analyze' action for the itip module, too
         */
        actions.put("analyze", new LegacyAnalyzeAction(services));
        this.actions = actions.build();
    }

    @Override
    public AJAXActionService createActionService(String action) throws OXException {
        return actions.get(action);
    }

}
