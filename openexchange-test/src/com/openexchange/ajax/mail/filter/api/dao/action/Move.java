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

package com.openexchange.ajax.mail.filter.api.dao.action;

import com.openexchange.ajax.mail.filter.api.dao.ActionCommand;
import com.openexchange.ajax.mail.filter.api.dao.action.argument.MoveActionArgument;

/**
 * {@link Move}
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class Move extends AbstractAction implements Action<MoveActionArgument> {

    /**
     * Initialises a new {@link Move}.
     */
    public Move(String folder) {
        super(ActionCommand.move);
        addArgument(MoveActionArgument.into, folder);
    }

    @Override
    public ActionCommand getActionCommand() {
        return ActionCommand.move;
    }

    @Override
    public void setArgument(MoveActionArgument argument, Object value) {
        addArgument(argument, value);
    }

    @Override
    public Object getArgument(MoveActionArgument argument) {
        return getArguments().get(argument);
    }
}
