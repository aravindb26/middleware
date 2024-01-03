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

package com.openexchange.deputy.json.action;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.deputy.DefaultDeputyPermission;
import com.openexchange.deputy.DefaultModulePermission;
import com.openexchange.deputy.DefaultPermission;
import com.openexchange.deputy.DeputyPermission;
import com.openexchange.deputy.DeputyService;
import com.openexchange.deputy.ModulePermission;
import com.openexchange.deputy.Permissions;
import com.openexchange.deputy.json.DeputyActionFactory;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractDeputyAction} - Abstract GDPR data export action.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractDeputyAction implements AJAXActionService {

    /**
     * The service look-up
     */
    protected final ServiceLookup services;

    /**
     * Initializes a new {@link AbstractDeputyAction}.
     *
     * @param services The service look-up
     */
    protected AbstractDeputyAction(ServiceLookup services) {
        super();
        this.services = services;
    }

    /**
     * Gets the deputy service.
     *
     * @return The deputy service
     * @throws OXException If deputy service cannot be returned
     */
    protected DeputyService getDeputyService() throws OXException {
        return services.getServiceSafe(DeputyService.class);
    }

    private static final String JSON_FIELD_MODULE_PERMISSIONS = "modulePermissions";
    private static final String JSON_FIELD_USER_ID = "userId";
    private static final String JSON_FIELD_SEND_ON_BEHALF_OF = "sendOnBehalfOf";
    private static final String JSON_FIELD_FOLDER_IDS = "folderIds";
    private static final String JSON_FIELD_PERMISSION = "permission";

    /**
     * Parses specified JSON representation of deputy permission.
     *
     * @param jDeputyPermission The JSON representation of deputy permission
     * @return The parsed deputy permission
     * @throws JSONException If reading from JSON fails
     */
    protected static DeputyPermission parseDeputyPermission(JSONObject jDeputyPermission) throws JSONException {
        DefaultDeputyPermission.Builder deputyPermission = DefaultDeputyPermission.builder();

        int userId = jDeputyPermission.getInt(JSON_FIELD_USER_ID);
        deputyPermission.withEntityId(userId);
        deputyPermission.withGroup(false);
        deputyPermission.withSendOnBehalfOf(jDeputyPermission.optBoolean(JSON_FIELD_SEND_ON_BEHALF_OF, false));

        JSONObject jModulePermissions = jDeputyPermission.getJSONObject(JSON_FIELD_MODULE_PERMISSIONS);
        int length = jModulePermissions.length();
        List<ModulePermission> modulePermissions = new ArrayList<>(length);
        for (String moduleId : jModulePermissions.keySet()) {
            JSONObject jModulePermission = jModulePermissions.getJSONObject(moduleId);
            modulePermissions.add(parseModulePermission(userId, moduleId, jModulePermission));
        }
        deputyPermission.withModulePermissions(modulePermissions);
        return deputyPermission.build();
    }

    private static ModulePermission parseModulePermission(int userId, String moduleId, JSONObject jModulePermission) throws JSONException {
        DefaultModulePermission.Builder modulePermission = DefaultModulePermission.builder();
        modulePermission.withModuleId(moduleId);

        JSONArray jFolderIds = jModulePermission.optJSONArray(JSON_FIELD_FOLDER_IDS);
        if (jFolderIds != null) {
            modulePermission.withFolderIds(jFolderIds.stream().map(o -> o.toString()).collect(Collectors.toList()));
        }

        int[] permissionBits = Permissions.parsePermissionBits(jModulePermission.getInt(JSON_FIELD_PERMISSION));
        DefaultPermission.Builder permission = DefaultPermission.builder();
        permission.withEntity(userId).withGroup(false);
        permission.withFolderPermission(permissionBits[0]);
        permission.withReadPermission(permissionBits[1]);
        permission.withWritePermission(permissionBits[2]);
        permission.withDeletePermission(permissionBits[3]);
        permission.withAdmin(permissionBits[4] > 0);
        modulePermission.withPermission(permission.build());

        return modulePermission.build();
    }

    /**
     * Requires a JSON content in given request's data.
     *
     * @param requestData The request data to read from
     * @return The JSON content
     * @throws OXException If JSON content cannot be returned
     */
    protected JSONValue requireJSONBody(AJAXRequestData requestData) throws OXException {
        Object data = requestData.getData();
        if (null == data) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
        }
        JSONValue jBody = requestData.getData(JSONValue.class);
        if (null == jBody) {
            throw AjaxExceptionCodes.INVALID_REQUEST_BODY.create(JSONValue.class, data.getClass());
        }
        return (JSONValue) data;
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        if (!services.getService(CapabilityService.class).getCapabilities(session).contains(DeputyService.CAPABILITY_DEPUTY)) {
            throw AjaxExceptionCodes.NO_PERMISSION_FOR_MODULE.create(DeputyActionFactory.getModule());
        }
        try {
            return doPerform(getDeputyService(), requestData, session);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Performs given GDPR data export request.
     *
     * @param deputyService The deputy service
     * @param requestData The request data
     * @param session The session providing user information
     * @return The AJAX result
     * @throws OXException If performing request fails
     */
    protected abstract AJAXRequestResult doPerform(DeputyService deputyService, AJAXRequestData requestData, ServerSession session) throws OXException, JSONException;

}
