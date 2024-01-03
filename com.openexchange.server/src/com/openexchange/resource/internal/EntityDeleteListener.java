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
package com.openexchange.resource.internal;

import static com.openexchange.java.Autoboxing.i;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourcePermission;
import com.openexchange.resource.SchedulingPrivilege;

/**
 * {@link EntityDeleteListener} listens to delete events are removes related resources
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8
 */
public class EntityDeleteListener implements DeleteListener {

    private final ExtendedResourceStorage storage;
    private final Optional<CachingAwareResourceStorage> optCacheStorage;

    /**
     * Initializes a new {@link EntityDeleteListener}.
     *
     * @param storage The {@link ExtendedResourceStorage} to use
     */
    public EntityDeleteListener(ExtendedResourceStorage storage) {
        super();
        this.storage = storage;
        if (storage instanceof CachingAwareResourceStorage ca) {
            optCacheStorage = Optional.of(ca);
        } else {
            optCacheStorage = Optional.empty();
        }
    }

    @Override
    public void deletePerformed(DeleteEvent event, Connection readCon, Connection writeCon) throws OXException {
        if (DeleteEvent.TYPE_RESOURCE == event.getType()) {
            if (optCacheStorage.isPresent()) {
                optCacheStorage.get().invalidate(event.getContext(), event.getId());
            }
            return;
        }
        if (DeleteEvent.TYPE_USER != event.getType() && DeleteEvent.TYPE_GROUP != event.getType()) {
            return; // nothing to do; deleted context implicitly handled via OXContextMySQLStorage#deleteTableData
        }
        int fallbackBookingDelegate = null == event.getDestinationUserID() || 0 == i(event.getDestinationUserID()) ? event.getContext().getMailadmin() : i(event.getDestinationUserID());
        handleDeletedEntity(event.getContext(), readCon, writeCon, event.getId(), DeleteEvent.TYPE_GROUP == event.getType(), fallbackBookingDelegate);
    }

    /**
     * Handles a group or user deletion by looking up any references to the entity within the stored resource permissions and adjusting
     * those permissions by removing the deleted entity and adding a fallback booking delegate as needed.
     *
     * @param context The context
     * @param readCon The 'read' connection to use
     * @param writeCon The 'write' connection to use
     * @param entity The identifier of the deleted entity
     * @param group <code>true</code> if the entity refers to a group, <code>false</code>, otherwise
     * @param fallbackBookingDelegate The user identifier to insert as booking delegate if one is required
     */
    private void handleDeletedEntity(Context context, Connection readCon, Connection writeCon, int entity, boolean group, int fallbackBookingDelegate) throws OXException {
        /*
         * lookup resources that reference the deleted entity in their permissions
         */
        List<Resource> resources = storage.getResourceIdsWithPermissionsForEntity(context, readCon, entity, group);
        for (Resource resource : resources) {
            /*
             * adjust the permissions by removing the deleted entity and adding a fallback booking delegate if required
             */
            ResourcePermission[] adjustedPermissions = adjustPermissionsAfterDeletedEntity(resource.getPermissions(), entity, group, fallbackBookingDelegate);
            /*
             * write back adjusted permissions
             */
            storage.deletePermissions(context, writeCon, resource.getIdentifier());
            storage.insertPermissions(context, writeCon, resource.getIdentifier(), adjustedPermissions);
        }
    }

    /**
     * Adjusts a resource permissions upon user or group deletion, by removing the deleted entity and adding a fallback booking delegate
     * if required.
     *
     * @param permissions The permissions to adjust
     * @param entity The identifier of the deleted entity
     * @param group <code>true</code> if the entity refers to a group, <code>false</code>, otherwise
     * @param fallbackBookingDelegate The user identifier to insert as booking delegate if one is required
     * @return The adjusted permissions
     */
    private static ResourcePermission[] adjustPermissionsAfterDeletedEntity(ResourcePermission[] permissions, int entity, boolean group, int fallbackBookingDelegate) {
        /*
         * construct new list of resource permissions w/o removed entity, ensuring that a booking delegate is present as soon as
         * there's another 'ask_to_book' privilege present
         */
        boolean needsBookingDelegate = false;
        boolean stillHasBookingDelegate = false;
        List<ResourcePermission> adjustedPermissions = new ArrayList<>();
        for (ResourcePermission permission : permissions) {
            if (permission.getEntity() == entity && permission.isGroup() == group) {
                continue; // this one is removed
            }
            adjustedPermissions.add(permission);
            if (SchedulingPrivilege.DELEGATE.equals(permission.getSchedulingPrivilege())) {
                stillHasBookingDelegate = true;
                continue;
            }
            if (SchedulingPrivilege.ASK_TO_BOOK.equals(permission.getSchedulingPrivilege())) {
                needsBookingDelegate = true;
            }
        }
        if (needsBookingDelegate && false == stillHasBookingDelegate) {
            adjustedPermissions.add(new ResourcePermission(fallbackBookingDelegate, false, SchedulingPrivilege.DELEGATE));
        }
        return adjustedPermissions.toArray(new ResourcePermission[adjustedPermissions.size()]);
    }

}
