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

package com.openexchange.ajax.mail.filter.api.request;

import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.mail.filter.api.parser.ConfigParser;
import com.openexchange.ajax.mail.filter.api.response.ConfigResponse;

/**
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ConfigRequest extends AbstractMailFilterRequest<ConfigResponse> {

    @SuppressWarnings("hiding")
    private final boolean failOnError;

    /**
     * Initialises a new {@link ConfigRequest}. Does NOT fail in case of an error
     */
    public ConfigRequest() {
        this(false);
    }

    /**
     * Initialises a new {@link ConfigRequest}.
     *
     * @param failOnError the fail on error flag
     */
    public ConfigRequest(final boolean failOnError) {
        super();
        this.failOnError = failOnError;
    }

    @Override
    public Object getBody() throws JSONException {
        return new JSONObject();
    }

    @Override
    public Method getMethod() {
        return Method.GET;
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[] { new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_CONFIG),
        };
    }

    @Override
    public ConfigParser getParser() {
        return new ConfigParser(failOnError);
    }
}
