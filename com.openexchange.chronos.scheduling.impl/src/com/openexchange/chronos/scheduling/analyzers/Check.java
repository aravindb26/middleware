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

package com.openexchange.chronos.scheduling.analyzers;

import static com.openexchange.folderstorage.Permission.CREATE_OBJECTS_IN_FOLDER;
import static com.openexchange.folderstorage.Permission.DELETE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.DELETE_OWN_OBJECTS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.WRITE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.WRITE_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.tools.arrays.Collections;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * 
 * {@link Check}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class Check {

    private static final Logger LOGGER = LoggerFactory.getLogger(Check.class);

    private Check() {}

    /**
     * Gets a value indicating whether the current session user is allowed to perform any action on behalf of the calendar user
     * 
     * @param services To get the context service from
     * @param session The users session
     * @param calendarUserId The identifier of the calendar user
     * @param originalResource The original calendar resource, can be <code>null</code>
     * @return <code>true</code> if the user has access, <code>false</code> if not
     */
    public static boolean hasAccess(ServiceLookup services, CalendarSession session, int calendarUserId, CalendarObjectResource originalResource) {
        try {
            checkAccess(services, session, calendarUserId, originalResource);
            return true;
        } catch (OXException e) {
            LOGGER.debug("No access", e);
        }
        return false;
    }

    /**
     * Checks if the current session user is allowed to perform any action on behalf of the calendar user
     * 
     * @param services To get the context service from
     * @param session The users session
     * @param calendarUserId The identifier of the calendar user
     * @param originalResource The original calendar resource, can be <code>null</code>
     * @throws OXException In case session user is not allowed to perform actions
     */
    @SuppressWarnings("null") // Check is within boolean "isUnknown"
    public static void checkAccess(ServiceLookup services, CalendarSession session, int calendarUserId, CalendarObjectResource originalResource) throws OXException {
        if (session.getUserId() == calendarUserId) {
            return;
        }
        /*
         * Get folder
         */
        Integer folderId;
        boolean isUnkown = null == originalResource || Collections.isNullOrEmpty(originalResource.getEvents());
        try {
            if (isUnkown) {
                /*
                 * Get standard calendar folder of the calendar user
                 */
                folderId = Integer.valueOf(session.getConfig().getDefaultFolderId(calendarUserId));
            } else {
                folderId = Integer.valueOf(originalResource.getFirstEvent().getFolderId());
            }
        } catch (NumberFormatException e) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create(e);
        }
        /*
         * Get folder permissions
         */
        Context context = services.getServiceSafe(ContextService.class).getContext(session.getContextId());
        FolderObject folder = new OXFolderAccess(context).getFolderObject(i(folderId));
        UserPermissionBits permissionBits = ServerSessionAdapter.valueOf(session.getSession()).getUserPermissionBits();
        EffectivePermission permission = folder.getEffectiveUserPermission(session.getUserId(), permissionBits);
        /*
         * Check permissions
         */
        if (permission.getReadPermission() < READ_FOLDER) {
            throw CalendarExceptionCodes.NO_READ_PERMISSION.create(folderId.toString());
        }
        if (isUnkown) {
            /*
             * Check for general write access
             */
            if (permission.getFolderPermission() < CREATE_OBJECTS_IN_FOLDER) {
                throw CalendarExceptionCodes.NO_READ_PERMISSION.create(folderId.toString());
            }
            if (permission.getWritePermission() < WRITE_ALL_OBJECTS) {
                throw CalendarExceptionCodes.NO_WRITE_PERMISSION.create(folderId.toString());
            }
            if (permission.getDeletePermission() < DELETE_ALL_OBJECTS) {
                throw CalendarExceptionCodes.NO_DELETE_PERMISSION.create(folderId.toString());
            }
        } else {
            /*
             * Check if at least own objects can be modified
             */
            if (permission.getFolderPermission() < CREATE_OBJECTS_IN_FOLDER) {
                throw CalendarExceptionCodes.NO_READ_PERMISSION.create(folderId.toString());
            }
            if (permission.getWritePermission() < WRITE_OWN_OBJECTS) {
                throw CalendarExceptionCodes.NO_WRITE_PERMISSION.create(folderId.toString());
            }
            if (permission.getDeletePermission() < DELETE_OWN_OBJECTS) {
                throw CalendarExceptionCodes.NO_DELETE_PERMISSION.create(folderId.toString());
            }
            /*
             * Check if events are managed by the session user, if needed
             */
            if ((permission.getWritePermission() == WRITE_OWN_OBJECTS || permission.getDeletePermission() == DELETE_OWN_OBJECTS)//
                && false == isUserMangedResource(originalResource, session)) {
                throw CalendarExceptionCodes.NO_WRITE_PERMISSION.create(folderId);
            }
        }
    }

    /**
     * Gets a value indicating whether the current session user is managing the resource or not.
     *
     * @param resource The resource to check
     * @param session The user session
     * @return <code>true</code> if the resource is managed by the user, either if he is the organizer
     *         or if he created the resource on behalf of
     *         <code>false</code> otherwise
     */
    private static boolean isUserMangedResource(CalendarObjectResource resource, CalendarSession session) {
        /*
         * Check if event was created by session user
         */
        if (resource.getEvents().size() == 1) {
            return session.getUserId() == resource.getFirstEvent().getCreatedBy().getEntity();
        }
        /*
         * Check if series was created by the session user, implicit allow access to exceptions too
         */
        Event seriesMaster = resource.getSeriesMaster();
        if (null != seriesMaster) {
            return session.getUserId() == seriesMaster.getCreatedBy().getEntity();
        }
        /*
         * Single event instances, check if at least one event was created by session user
         */
        for (Event event : resource.getEvents()) {
            if (session.getUserId() == event.getCreatedBy().getEntity()) {
                return true;
            }
        }
        return false;
    }

}
