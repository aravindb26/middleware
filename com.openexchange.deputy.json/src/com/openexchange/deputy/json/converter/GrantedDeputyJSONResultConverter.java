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

package com.openexchange.deputy.json.converter;

import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.deputy.ActiveDeputyPermission;
import com.openexchange.deputy.GrantedDeputyPermissions;
import com.openexchange.deputy.Grantee;
import com.openexchange.exception.OXException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link GrantedDeputyJSONResultConverter}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class GrantedDeputyJSONResultConverter extends DeputyJSONResultConverter {

    /**
     * Initializes a new {@link GrantedDeputyJSONResultConverter}.
     */
    public GrantedDeputyJSONResultConverter() {
        super();
    }

    @Override
    public String getInputFormat() {
        return "granteddeputypermission";
    }

    @Override
    public void convert(final AJAXRequestData requestData, final AJAXRequestResult result, final ServerSession session, final Converter converter) throws OXException {
        try {
            final Object resultObject = result.getResultObject();
            if (resultObject instanceof GrantedDeputyPermissions) {
                result.setResultObject(convertGrantedDeputyPermissions((GrantedDeputyPermissions) resultObject), "json");
                return;
            }
            throw new UnsupportedOperationException(GrantedDeputyJSONResultConverter.class.getName() + " does not support conversion of type: " + (resultObject == null ? "null" : resultObject.getClass().getName()));
        } catch (final JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private JSONArray convertGrantedDeputyPermissions(GrantedDeputyPermissions grantedDeputyPermissions) throws JSONException {
        JSONArray jPermissions = new JSONArray(grantedDeputyPermissions.size());
        for (Map.Entry<Grantee, List<ActiveDeputyPermission>> entry : grantedDeputyPermissions.entrySet()) {
            List<ActiveDeputyPermission> deputyPermissions = entry.getValue();
            JSONArray jAddresses = null;
            for (ActiveDeputyPermission deputyPermission : deputyPermissions) {
                JSONObject jDeputyPermission = convertDeputyPermission(deputyPermission);
                if (deputyPermission.isSendOnBehalfOf()) {
                    if (jAddresses == null) {
                        jAddresses = new JSONArray(entry.getKey().getAliases());
                    }
                    jDeputyPermission.put("granteeAddresses", jAddresses);
                }
                jPermissions.put(jDeputyPermission);
            }
        }
        return jPermissions;
    }

}
