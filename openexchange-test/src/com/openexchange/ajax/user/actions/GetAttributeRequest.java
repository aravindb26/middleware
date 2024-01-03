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

package com.openexchange.ajax.user.actions;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.user.json.actions.GetAttributeAction;

/**
 * {@link GetAttributeRequest}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public final class GetAttributeRequest extends AbstractUserRequest<GetAttributeResponse> {

    private final int userId;
    private final String name;

    public GetAttributeRequest(int userId, String name) {
        super();
        this.userId = userId;
        this.name = name;
    }

    @Override
    public Method getMethod() {
        return Method.GET;
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[] { new URLParameter(AJAXServlet.PARAMETER_ACTION, GetAttributeAction.ACTION), new URLParameter(AJAXServlet.PARAMETER_ID, userId), new URLParameter("name", name)
        };
    }

    @Override
    public GetAttributeParser getParser() {
        return new GetAttributeParser(name, true);
    }

    @Override
    public Object getBody() {
        return null;
    }
}
