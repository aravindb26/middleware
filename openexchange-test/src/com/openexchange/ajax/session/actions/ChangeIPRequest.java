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

package com.openexchange.ajax.session.actions;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.fields.LoginFields;

/**
 * {@link ChangeIPRequest}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ChangeIPRequest extends AbstractRequest<ChangeIPResponse> {

    private final boolean failOnError;

    public ChangeIPRequest(final String newIP, final boolean failOnError) {
        super(new Parameter[] { new URLParameter(AJAXServlet.PARAMETER_ACTION, LoginServlet.ACTION_CHANGEIP), new FieldParameter(LoginFields.CLIENT_IP_PARAM, newIP)
        });
        this.failOnError = failOnError;
    }

    @Override
    public ChangeIPParser getParser() {
        return new ChangeIPParser(failOnError);
    }
}
