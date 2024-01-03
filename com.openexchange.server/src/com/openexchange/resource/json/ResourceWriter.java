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

package com.openexchange.resource.json;

import static com.openexchange.ajax.writer.DataWriter.writeParameter;
import static com.openexchange.resource.ResourcePermissionUtility.getEffectivePrivilege;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.java.util.TimeZones;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourcePermission;
import com.openexchange.resource.SchedulingPrivilege;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link ResourceWriter} - Writes a {@link Resource resource} to a JSON object
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ResourceWriter {

    /**
     * Initializes a new {@link ResourceWriter}
     */
    private ResourceWriter() {
        super();
    }

    /**
     * Writes specified {@link Resource resource} to a JSON object
     *
     * @param session The requesting user's session
     * @param resource The resource to write
     * @return The written JSON object
     * @throws JSONException If writing to JSON object fails
     */
    public static JSONObject writeResource(ServerSession session, Resource resource) throws JSONException {
        return writeResource(session.getUser(), resource);
    }

    /**
     * Writes specified {@link Resource resource} to a JSON object
     *
     * @param user The requesting user
     * @param resource The resource to write
     * @return The written JSON object
     * @throws JSONException If writing to JSON object fails
     */
    public static JSONObject writeResource(User user, Resource resource) throws JSONException {
        return writeResource(resource, getEffectivePrivilege(resource, user), TimeZone.getTimeZone(user.getTimeZone()));
    }

    /**
     * Writes specified {@link Resource resource} to a JSON object.
     * <p/>
     * To include a user's 'own' scheduling permission for the resource, use {@link #writeResource(Resource, SchedulingPrivilege)}.
     *
     * @param resource The resource to write
     * @return The written JSON object
     * @throws JSONException If writing to JSON object fails
     */
    public static JSONObject writeResource(Resource resource) throws JSONException {
        return writeResource(resource, null);
    }

    /**
     * Writes specified {@link Resource resource} to a JSON object
     * <p/>
     * To respect the user/request timezone when serializing the 'last modified' field, use {@link #writeResource(Resource, SchedulingPrivilege, TimeZone)}.
     *
     * @param resource The resource to write
     * @param ownPermission The 'own' permission to indicate, or <code>null</code> to omit
     * @return The written JSON object
     * @throws JSONException If writing to JSON object fails
     */
    public static JSONObject writeResource(Resource resource, SchedulingPrivilege ownPermission) throws JSONException {
        return writeResource(resource, ownPermission, null);
    }

    /**
     * Writes specified {@link Resource resource} to a JSON object
     *
     * @param resource The resource to write
     * @param ownPermission The 'own' permission to indicate, or <code>null</code> to omit
     * @param timeZone The use/request timezone to consider, or <code>null</code> to fall back to UTC
     * @return The written JSON object
     * @throws JSONException If writing to JSON object fails
     */
    public static JSONObject writeResource(Resource resource, SchedulingPrivilege ownPermission, TimeZone timeZone) throws JSONException {
        final JSONObject retval = new JSONObject(10);
        retval.put(ResourceFields.ID, resource.getIdentifier() == -1 ? JSONObject.NULL : Integer.valueOf(resource.getIdentifier()));
        retval.put(ResourceFields.NAME, resource.getSimpleName() == null ? JSONObject.NULL : resource.getSimpleName());
        retval.put(ResourceFields.DISPLAY_NAME, resource.getDisplayName() == null ? JSONObject.NULL : resource.getDisplayName());
        retval.put(ResourceFields.MAIL, resource.getMail() == null ? JSONObject.NULL : resource.getMail());
        retval.put(ResourceFields.AVAILABILITY, resource.isAvailable());
        retval.put(ResourceFields.DESCRIPTION, resource.getDescription() == null ? JSONObject.NULL : resource.getDescription());
        writeParameter(DataFields.LAST_MODIFIED, resource.getLastModified(), null == timeZone ? TimeZones.UTC : timeZone, retval);
        writeParameter(DataFields.LAST_MODIFIED_UTC, resource.getLastModified(), TimeZones.UTC, retval);
        retval.put(ResourceFields.PERMISSIONS, null == resource.getPermissions() ? JSONObject.NULL : writeResourcePermissions(resource.getPermissions()));
        retval.putOpt(ResourceFields.OWN_PRIVILEGE, writeSchedulingPrivilege(ownPermission));
        return retval;
    }

    /**
     * Serializes a resource permission array to JSON.
     *
     * @param permissions The permissions to serialize
     * @return The serialized permissions as JSON array
     * @throws JSONException If writing to the JSON object fails
     */
    private static JSONArray writeResourcePermissions(ResourcePermission[] permissions) throws JSONException {
        JSONArray jsonArray = new JSONArray(permissions.length);
        for (int i = 0; i < permissions.length; i++) {
            jsonArray.put(i, writeResourcePermission(permissions[i]));
        }
        return jsonArray;
    }

    /**
     * Serializes a resource permission to JSON.
     *
     * @param permission The permission to serialize
     * @return The serialized permission as JSON object
     * @throws JSONException If writing to the JSON object fails
     */
    private static JSONObject writeResourcePermission(ResourcePermission permission) throws JSONException {
        JSONObject jsonObject = new JSONObject(3);
        jsonObject.put(ResourcePermissionFields.ENTITY, permission.getEntity());
        jsonObject.put(ResourcePermissionFields.GROUP, permission.isGroup());
        jsonObject.put(ResourcePermissionFields.PRIVILEGE, writeSchedulingPrivilege(permission.getSchedulingPrivilege()));
        return jsonObject;
    }

    /**
     * Serializes a scheduling privilege to its name as used in the HTTP API.
     *
     * @param privilege The privilege to serialize
     * @return The serialized privilege, or <code>null</code> if passed argument was <code>null</code>
     */
    private static String writeSchedulingPrivilege(SchedulingPrivilege privilege) {
        return null != privilege ? privilege.name().toLowerCase() : null;
    }

}
