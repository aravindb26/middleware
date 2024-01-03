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

package com.openexchange.mail.filter.json.v2.json.mapper.parser.action;

import java.util.ArrayList;
import org.apache.jsieve.SieveException;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.jsieve.commands.ActionCommand;
import com.openexchange.jsieve.commands.ActionCommand.Commands;
import com.openexchange.mail.filter.json.v2.json.fields.GeneralField;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link KeepActionCommandParser}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class KeepActionCommandParser extends AbstractActionCommandParser {

    /**
     * Initializes a new {@link KeepActionCommandParser}.
     */
    public KeepActionCommandParser(ServiceLookup services) {
        super(services, Commands.KEEP);
    }

    @Override
    public ActionCommand parse(JSONObject jsonObject, ServerSession session) throws JSONException, SieveException {
        return new ActionCommand(Commands.KEEP, new ArrayList<Object>(0));
    }

    @Override
    public void parse(JSONObject jsonObject, ActionCommand actionCommand) throws JSONException, OXException {
        jsonObject.put(GeneralField.id.name(), Commands.KEEP.getJsonName());
    }
}
