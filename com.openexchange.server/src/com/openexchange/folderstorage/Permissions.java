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

package com.openexchange.folderstorage;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import com.openexchange.deputy.DeputyService;
import com.openexchange.java.Strings;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import gnu.trove.map.hash.TIntIntHashMap;


/**
 * A helper class with useful methods for folder permissions.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class Permissions {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Permissions.class);
    }

    /**
     * The actual max permission that can be transfered in field 'bits' or JSON's permission object
     */
    private static final int MAX_PERMISSION = 64;

    private static final int[] MAPPING_0 = { 0, 2, 4, -1, 8 };

    private static final TIntIntHashMap MAPPING_1;
    static {
        TIntIntHashMap m = new TIntIntHashMap(6);
        m.put(Permission.MAX_PERMISSION, MAX_PERMISSION);
        m.put(MAX_PERMISSION, MAX_PERMISSION);
        m.put(0, 0);
        m.put(2, 1);
        m.put(4, 2);
        m.put(8, 4);
        MAPPING_1 = m;
    }

    /**
     * Parses the given permission bit mask into an array with 5 elements:
     * <ul>
     * <li>0: folder permission</li>
     * <li>1: read permission</li>
     * <li>2: write permission</li>
     * <li>3: delete permission</li>
     * <li>4: admin permission (<code>true</code> if > 0)</li>
     * </ul>
     *
     * @param permissionBits The bit mask
     * @return An array containing the partial permissions.
     */
    public static final int[] parsePermissionBits(final int permissionBits) {
        int bits = permissionBits;
        final int[] retval = new int[5];
        for (int i = retval.length - 1; i >= 0; i--) {
            final int shiftVal = (i * 7); // Number of bits to be shifted
            retval[i] = bits >> shiftVal;
            bits -= (retval[i] << shiftVal);
            if (retval[i] == MAX_PERMISSION) {
                retval[i] = Permission.MAX_PERMISSION;
            } else if (i < (retval.length - 1)) {
                retval[i] = MAPPING_0[retval[i]];
            } else {
                retval[i] = retval[i];
            }
        }
        return retval;
    }

    /**
     * Creates a permission bit mask from the given {@link Permission} instance.
     *
     * @param permission The permission
     * @return The bit mask
     */
    public static int createPermissionBits(final Permission permission) {
        return createPermissionBits(
            permission.getFolderPermission(),
            permission.getReadPermission(),
            permission.getWritePermission(),
            permission.getDeletePermission(),
            permission.isAdmin());
    }

    /**
     * Creates a read-only permission bit mask.
     *
     * @return The bit mask
     */
    public static int createReadOnlyPermissionBits() {
        return createPermissionBits(OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, false);
    }

    /**
     * Creates a permission bit mask from the given partial permissions.
     *
     * @param fp The folder permission
     * @param rp The read permission
     * @param wp The write permission
     * @param dp The delete permission
     * @param adminFlag The folder admin flag
     * @return The bit mask
     */
    public static int createPermissionBits(int fp, int rp, int wp, int dp, boolean adminFlag) {
        int retval = 0;
        int i = 4;
        retval += (adminFlag ? 1 : 0) << (i-- * 7)/* Number of bits to be shifted */;
        retval += MAPPING_1.get(dp) << (i-- * 7);
        retval += MAPPING_1.get(wp) << (i-- * 7);
        retval += MAPPING_1.get(rp) << (i-- * 7);
        retval += MAPPING_1.get(fp) << (i * 7);
        return retval;
    }

    /**
     * Creates a new {@link Permission} instance based on the given entity and permission bit mask.
     *
     * @param entity The entity
     * @param isGroup Whether the entity is a group (<code>true</code>) or a user (<code>false</code>)
     * @param permissionBits The permission bit mask
     * @return The permission instance
     */
    public static Permission createPermission(int entity, boolean isGroup, int permissionBits) {
        return new BasicPermission(entity, isGroup, permissionBits);
    }

    /**
     * Checks if given permission is a deputy permission.
     *
     * @param permission The permission to check
     * @param folder The folder possessing the permission
     * @param session The session
     * @return <code>true</code> if given permission is a deputy permission; otherwise <code>false</code>
     */
    public static boolean isDeputyPermission(Permission permission, Folder folder, Session session) {
        Boolean isDeputyPermission = null;
        if (permission instanceof DeputyAwarePermission) {
            DeputyAwarePermission deputyAwarePermission = (DeputyAwarePermission) permission;
            if (deputyAwarePermission.isDeputyPermissionSupported()) {
                isDeputyPermission = Boolean.valueOf(deputyAwarePermission.isDeputyPermission());
            }
        }
        if (isDeputyPermission == null) {
            isDeputyPermission = Boolean.FALSE;
            if (isDeputyServiceAvailable(session)) {
                // Try to determine via folder meta-data
                Map<String, Object> meta = folder.getMeta();
                if (meta != null && !meta.isEmpty()) {
                    for (Iterator<Entry<String, Object>> it = meta.entrySet().iterator(); isDeputyPermission.booleanValue() == false && it.hasNext();) {
                        Object value = it.next().getValue();
                        if ((value instanceof Map)) {
                            @SuppressWarnings("unchecked") Map<String, Object> deputyMetadata = (Map<String, Object>) value;
                            if ("deputy".equals(deputyMetadata.get("type"))) {
                                int entityId = getInt("entity", -1, deputyMetadata);
                                if (entityId >= 0 && entityId == permission.getEntity()) {
                                    isDeputyPermission = Boolean.TRUE;
                                }
                            }
                        }
                    }
                }
            }
        }
        return isDeputyPermission.booleanValue();
    }

    private static boolean isDeputyServiceAvailable(Session session) {
        try {
            DeputyService deputyService = ServerServiceRegistry.getInstance().getService(DeputyService.class);
            return deputyService != null && deputyService.isAvailable(session);
        } catch (Exception e) {
            LoggerHolder.LOG.warn("Failed to check availability of deputy service. Assuming as absent.", e);
            return false;
        }
    }

    private static int getInt(String key, int defaultValue, Map<String, Object> deputyMetadata) {
        Object value = deputyMetadata.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        int i = Strings.parsePositiveInt(value.toString());
        return i >= 0 ? i : defaultValue;
    }

}
