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

package com.openexchange.conversion.datahandler;

import com.openexchange.ajax.parser.ResponseParser;
import com.openexchange.conversion.ConversionResult;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataExceptionCodes;
import com.openexchange.conversion.DataHandler;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link Json2OXExceptionDataHandler}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class Json2OXExceptionDataHandler implements DataHandler {

    /**
     * Initializes a new {@link Json2OXExceptionDataHandler}.
     */
    public Json2OXExceptionDataHandler() {
        super();
    }

    @Override
    public String[] getRequiredArguments() {
        return Strings.getEmptyStrings();
    }

    @Override
    public Class<?>[] getTypes() {
        return new Class<?>[] { JSONObject.class };
    }

    @Override
    public ConversionResult processData(Data<? extends Object> data, DataArguments dataArguments, Session session) throws OXException {
        ConversionResult result = new ConversionResult();
        Object sourceData = data.getData();
        try {
            if (null == sourceData) {
                result.setData(null);
            } else if ((sourceData instanceof JSONObject)) {
                result.setData(ResponseParser.parseException((JSONObject) sourceData));
            } else {
                throw DataExceptionCodes.TYPE_NOT_SUPPORTED.create(sourceData.getClass().toString());
            }
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
        return result;
    }

}
