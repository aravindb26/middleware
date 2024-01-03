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

package com.openexchange.contacts.json;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXActionServiceFactory;
import com.openexchange.contacts.json.actions.AdvancedSearchAction;
import com.openexchange.contacts.json.actions.AllAction;
import com.openexchange.contacts.json.actions.AnniversariesAction;
import com.openexchange.contacts.json.actions.AutocompleteAction;
import com.openexchange.contacts.json.actions.BirthdaysAction;
import com.openexchange.contacts.json.actions.ContactAction;
import com.openexchange.contacts.json.actions.CopyAction;
import com.openexchange.contacts.json.actions.DeleteAction;
import com.openexchange.contacts.json.actions.GetAction;
import com.openexchange.contacts.json.actions.GetUserAction;
import com.openexchange.contacts.json.actions.GetVCardAction;
import com.openexchange.contacts.json.actions.ListAction;
import com.openexchange.contacts.json.actions.ListUserAction;
import com.openexchange.contacts.json.actions.NewAction;
import com.openexchange.contacts.json.actions.SearchAction;
import com.openexchange.contacts.json.actions.UpdateAction;
import com.openexchange.contacts.json.actions.UpdatesAction;
import com.openexchange.server.ServiceLookup;


/**
 * {@link ContactActionFactory}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactActionFactory implements AJAXActionServiceFactory {

    /** The (legacy) HTTP API module name for accessing <i>internal</i> contacts access interface directly */
    public static final String MODULE = "contacts";

    private final Map<String, ContactAction> actions;

    /**
     * Initializes a new {@link ContactActionFactory}.
     * 
     * @param serviceLookup A service lookup reference
     */
    public ContactActionFactory(ServiceLookup serviceLookup) {
        super();
        this.actions = ImmutableMap.<String, ContactAction> builder()
            .put("get", new GetAction(serviceLookup, true))
            .put("all", new AllAction(serviceLookup, true))
            .put("list", new ListAction(serviceLookup, true))
            .put("new", new NewAction(serviceLookup, true))
            .put("delete", new DeleteAction(serviceLookup, true))
            .put("update", new UpdateAction(serviceLookup, true))
            .put("updates", new UpdatesAction(serviceLookup, true))
            .put("listuser", new ListUserAction(serviceLookup))
            .put("getuser", new GetUserAction(serviceLookup))
            .put("copy", new CopyAction(serviceLookup, true))
            .put("search", new SearchAction(serviceLookup, true))
            .put("advancedSearch", new AdvancedSearchAction(serviceLookup, true))
            .put("birthdays", new BirthdaysAction(serviceLookup, true))
            .put("anniversaries", new AnniversariesAction(serviceLookup, true))
            .put("autocomplete", new AutocompleteAction(serviceLookup, true))
            .put("getVcard", new GetVCardAction(serviceLookup))
        .build();
    }

    @Override
    public AJAXActionService createActionService(String action) {
        return actions.get(action);
    }
}
