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

package com.openexchange.ajax.manifests.actions;

import org.json.JSONObject;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractAJAXParser;

/**
 * {@link ConfigRequestParser}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since 7.6.0
 */
public class ConfigRequestParser extends AbstractAJAXParser<ConfigResponse> {

    protected ConfigRequestParser(boolean failOnError) {
        super(failOnError);
    }

    @Override
    protected ConfigResponse createResponse(Response response) {
        ConfigResponse retval = new ConfigResponse(response);
        JSONObject json = (JSONObject) response.getData();
        if (isFailOnError()) {
            assertFalse(response.hasError(), response.getErrorMessage());
        }
        if (!response.hasError()) {
            retval.setConfig(json);
        }
        return retval;
    }

}
