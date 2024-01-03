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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.deputy.ActiveDeputyPermission;
import com.openexchange.deputy.ModulePermission;
import com.openexchange.deputy.Permissions;
import com.openexchange.exception.OXException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link DeputyJSONResultConverter}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyJSONResultConverter implements ResultConverter {

    /**
     * Initializes a new {@link DeputyJSONResultConverter}.
     */
    public DeputyJSONResultConverter() {
        super();
    }

    @Override
    public String getInputFormat() {
        return "deputypermission";
    }

    @Override
    public String getOutputFormat() {
        return "json";
    }

    @Override
    public Quality getQuality() {
        return Quality.GOOD;
    }

    @Override
    public void convert(final AJAXRequestData requestData, final AJAXRequestResult result, final ServerSession session, final Converter converter) throws OXException {
        try {
            final Object resultObject = result.getResultObject();
            if (resultObject instanceof ActiveDeputyPermission) {
                result.setResultObject(convertDeputyPermission((ActiveDeputyPermission) resultObject), "json");
                return;
            }
            /*
             * Collection of permissions
             */
            if (resultObject == null) {
                return;
            }
            @SuppressWarnings("unchecked") Collection<ActiveDeputyPermission> deputyPermissions = (Collection<ActiveDeputyPermission>) resultObject;
            final JSONArray jArray = new JSONArray(deputyPermissions.size());
            for (ActiveDeputyPermission deputyPermission : deputyPermissions) {
                jArray.put(convertDeputyPermission(deputyPermission));
            }
            result.setResultObject(jArray, "json");
        } catch (final JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Converts given deputy permission to its JSON representation
     *
     * @param deputyPermission The deputy permission to convert
     * @return The resulting JSON representation
     * @throws JSONException If JSON representation cannot be generated
     */
    protected JSONObject convertDeputyPermission(ActiveDeputyPermission deputyPermission) throws JSONException {
        JSONObject json = new JSONObject(6);

        json.put("deputyId", deputyPermission.getDeputyId());
        json.put("granteeId", deputyPermission.getUserId());
        json.put("userId", deputyPermission.getEntityId());
        json.put("sendOnBehalfOf", deputyPermission.isSendOnBehalfOf());

        List<ModulePermission> modulePermissions = deputyPermission.getModulePermissions();
        JSONObject jModulePermissions = new JSONObject(modulePermissions.size());
        for (ModulePermission modulePermission : modulePermissions) {
            jModulePermissions.put(modulePermission.getModuleId(), convertModulePermission(modulePermission));
        }
        json.put("modulePermissions", jModulePermissions);

        return json;
    }

    private JSONObject convertModulePermission(ModulePermission modulePermission) throws JSONException {
        JSONObject json = new JSONObject(3);

        Optional<List<String>> optionalFolderIds = modulePermission.getOptionalFolderIds();
        if (optionalFolderIds.isPresent()) {
            json.put("folderIds", new JSONArray(optionalFolderIds.get()));
        }

        json.put("permission", Permissions.createPermissionBits(modulePermission.getPermission()));

        return json;
    }

}
