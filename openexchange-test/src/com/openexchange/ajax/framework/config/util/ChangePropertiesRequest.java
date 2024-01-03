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

package com.openexchange.ajax.framework.config.util;

import java.util.Map;
import org.json.JSONObject;
import com.openexchange.ajax.framework.AJAXRequest;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.ajax.framework.Header;

/**
 * {@link ChangePropertiesRequest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.1
 */
public class ChangePropertiesRequest implements AJAXRequest<ChangePropertiesResponse> {

    private Map<String, String> properties;
    private static final String CONFIG_URL = "/ajax/changeConfigForTest";
    private String scope;
    private String reloadables;

    /**
     * Initializes a new {@link ChangePropertiesRequest}.
     */
    public ChangePropertiesRequest(Map<String, String> properties, String scope, String reloadables) {
        super();
        this.properties = properties;
        this.scope = scope;
        this.reloadables = reloadables;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Method getMethod() {
        return Method.PUT;
    }

    @Override
    public String getServletPath() {
        return CONFIG_URL;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Parameter[] getParameters() {
        if (reloadables != null) {
            return new Parameter[] { new Parameter("scope", scope), new Parameter("reload", reloadables) };
        }
        return new Parameter[] { new Parameter("scope", scope) };
    }

    @Override
    public AbstractAJAXParser<ChangePropertiesResponse> getParser() {
        return new ChangePropertiesParser(true);
    }

    @Override
    public Object getBody() {
        return new JSONObject(properties);
    }

    @Override
    public Header[] getHeaders() {
        return NO_HEADER;
    }

}
