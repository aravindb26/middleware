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

package com.openexchange.groupware.infostore.validation;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import static org.slf4j.LoggerFactory.getLogger;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.capabilities.CapabilitySet;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.exception.OXException;
import com.openexchange.group.Group;
import com.openexchange.group.GroupService;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.container.ObjectPermission;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreExceptionCodes;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfigurationCodes;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.groupware.userconfiguration.UserPermissionBitsStorage;
import com.openexchange.java.Strings;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.share.GuestInfo;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.ShareService;
import com.openexchange.share.recipient.RecipientType;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;


/**
 * {@link ObjectPermissionValidator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ObjectPermissionValidator implements InfostoreValidator {

    private final DBProvider dbProvider;

    /**
     * Wraps added/removed/changed objects permissions
     */
    private static class TouchedPermissions {

        /**
         * A list of changed and new permissions
         */
        List<ObjectPermission> changedAndNewPermissions = new ArrayList<>();
        /**
         * A list of removed object permissions
         */
        List<ObjectPermission> removedPermissions = new ArrayList<>();

        /**
         *
         * Gets a list of all touched(new, changed, removed) permissions
         *
         * @return A list of all touched(new, changed, removed) permissions
         */
        List<ObjectPermission> getTouchedPermissions() {
            return Stream.concat(changedAndNewPermissions.stream(), removedPermissions.stream()).collect(Collectors.toList());
        }
    }

    /**
     * Initializes a new {@link ObjectPermissionValidator}.
     *
     * @param dbProvider
     */
    public ObjectPermissionValidator(DBProvider dbProvider) {
        super();
        this.dbProvider = dbProvider;
    }

    @Override
    public String getName() {
        return ObjectPermissionValidator.class.getSimpleName();
    }

    @Override
    public DocumentMetadataValidation validate(ServerSession session, DocumentMetadata metadata, DocumentMetadata originalDocument, Set<Metadata> updatedColumns) {
        DocumentMetadataValidation validation = new DocumentMetadataValidation();
        TouchedPermissions touchedPermissions = getTouchedPermissions(metadata, originalDocument, updatedColumns);
        if (null == touchedPermissions || 0 == touchedPermissions.getTouchedPermissions().size()) {
            return validation; // no object permissions set or changed
        }
        /*
         * check applied permission bits
         */
        if (false == checkPermissionBits(touchedPermissions.getTouchedPermissions(), validation)) {
            return validation;
        }
        /*
         * check group IDs
         */
        if (false == checkGrouptEntities(session, touchedPermissions.getTouchedPermissions(), validation)) {
            return validation;
        }
        /*
         * check targeted permission entities
         */
        if (false == touchedPermissions.changedAndNewPermissions.isEmpty() && false == checkPermissionEntities(session, touchedPermissions.changedAndNewPermissions, validation)) {
            return validation;
        }
        /*
         * perform further checks based on each permission entity kind
         */
        if (false == checkCapabilities(session, touchedPermissions.getTouchedPermissions(), validation)) {
            return validation;
        }
        return validation;
    }

    /**
     * Checks the session user's capabilities and module permissions needed to apply the permission changes.
     *
     * @param session The session
     * @param touchedPermissions The added/changed/removed permissions
     * @param validation The document validation
     * @return <code>true</code> if checks were passed, <code>false</code>, otherwise
     */
    private boolean checkCapabilities(ServerSession session, List<ObjectPermission> touchedPermissions, DocumentMetadataValidation validation) {
        ShareService shareService = ServerServiceRegistry.getServize(ShareService.class);
        CapabilitySet capabilities = null;
        try {
            capabilities = ServerServiceRegistry.getServize(CapabilityService.class).getCapabilities(session);
        } catch (OXException e) {
            getLogger(ObjectPermissionValidator.class).warn("Error getting capabilities for user {}", I(session.getUserId()), e);
            validation.setFatalException(e);
        }
        for (ObjectPermission permission : touchedPermissions) {
            GuestInfo guestInfo = null;
            if (false == permission.isGroup()) {
                try {
                    guestInfo = shareService.getGuestInfo(session, permission.getEntity());
                } catch (OXException e) {
                    getLogger(ObjectPermissionValidator.class).warn("Error getting guest info for permission entity {}", I(permission.getEntity()), e);
                    validation.setFatalException(e);
                }
            }
            if (null == guestInfo) {
                /*
                 * internal permission entity, okay
                 */
            } else if (RecipientType.ANONYMOUS.equals(guestInfo.getRecipientType())) {
                /*
                 * anonymous link permission entity, check "share_links" capability
                 */
                if (null == capabilities || false == capabilities.contains("share_links")) {
                    OXException e = ShareExceptionCodes.NO_SHARE_LINK_PERMISSION.create();
                    validation.setError(Metadata.OBJECT_PERMISSIONS_LITERAL, e.getDisplayMessage(session.getUser().getLocale()));
                    validation.setException(e);
                    return false;
                }
            } else if (RecipientType.GUEST.equals(guestInfo.getRecipientType())) {
                /*
                 * external guest permission entity, check "invite_guests" capability
                 */
                if (null == capabilities || false == capabilities.contains("invite_guests")) {
                    OXException e = ShareExceptionCodes.NO_INVITE_GUEST_PERMISSION.create();
                    validation.setError(Metadata.OBJECT_PERMISSIONS_LITERAL, e.getDisplayMessage(session.getUser().getLocale()));
                    validation.setException(e);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks the applied bits of each permission.
     *
     * @param touchedPermissions The added/changed/removed permissions
     * @param validation The document validation
     * @return <code>true</code> if checks were passed, <code>false</code>, otherwise
     */
    private boolean checkPermissionBits(List<ObjectPermission> touchedPermissions, DocumentMetadataValidation validation) {
        for (ObjectPermission permission : touchedPermissions) {
            int bits = permission.getPermissions();
            if (ObjectPermission.DELETE == bits) {
                validation.setError(Metadata.OBJECT_PERMISSIONS_LITERAL, "DELETE object permission is not allowed.");
                validation.setException(InfostoreExceptionCodes.VALIDATION_FAILED_INAPPLICABLE_PERMISSIONS.create(I(permission.getEntity())));
                return false;
            }
            if (ObjectPermission.WRITE != bits && ObjectPermission.READ != bits) {
                validation.setError(Metadata.OBJECT_PERMISSIONS_LITERAL, "Invalid permission bits: " + bits);
                validation.setException(InfostoreExceptionCodes.VALIDATION_FAILED_INAPPLICABLE_PERMISSIONS.create(I(permission.getEntity())));
                return false;
            }
        }
        return true;
    }

    /**
     * Checks IDs for group permissions
     *
     * @param session The session
     * @param touchedPermissions The added/changed/removed permissions
     * @param validation The document validation
     * @return <code>true</code> if checks were passed, <code>false</code>, otherwise
     */
    private boolean checkGrouptEntities(ServerSession session, List<ObjectPermission> touchedPermissions, DocumentMetadataValidation validation) {
        /*
         * check existence of each group permission entity & collect group members
         */
        int[] groupIDs = getGroupEntities(touchedPermissions);
        if (null != groupIDs) {
            for (int groupID : groupIDs) {
                try {
                    Group group = ServerServiceRegistry.getServize(GroupService.class).getGroup(session.getContext(), groupID);
                    if (GroupStorage.GUEST_GROUP_IDENTIFIER == group.getIdentifier()) {
                        // invalid group
                        validation.setError(Metadata.OBJECT_PERMISSIONS_LITERAL, "Group " + group.getDisplayName() + " can't be used for object permissions.");
                        validation.setException(InfostoreExceptionCodes.VALIDATION_FAILED_INAPPLICABLE_PERMISSIONS_GUEST_GROUP.create(group.getDisplayName()));
                        return false;
                    }

                    // TODO: also validate capabilities of each group member (not done during folder checks, so deactivated for now here, too)
                    // userIDs.addAll(Arrays.asList(Autoboxing.i2I(group.getMember())));
                } catch (OXException e) {
                    if ("GRP-0017".equals(e.getErrorCode())) {
                        // group not found
                        validation.setError(Metadata.OBJECT_PERMISSIONS_LITERAL, e.getDisplayMessage(session.getUser().getLocale()));
                        validation.setException(InfostoreExceptionCodes.VALIDATION_FAILED_INAPPLICABLE_PERMISSIONS.create(I(groupID)));
                        return false;
                    }
                    getLogger(ObjectPermissionValidator.class).warn("Error getting group for permission entity {}", I(groupID), e);
                    validation.setFatalException(e);
                }
            }
        }
        return true;
    }

    /**
     * Checks the capabilities and module permissions of each targeted permission entity.
     *
     * @param session The session
     * @param touchedPermissions The added/changed/removed permissions
     * @param validation The document validation
     * @return <code>true</code> if checks were passed, <code>false</code>, otherwise
     */
    @SuppressWarnings("resource")
    private boolean checkPermissionEntities(ServerSession session, List<ObjectPermission> touchedPermissions, DocumentMetadataValidation validation) {
        /*
         * check capabilities of each user permission entity, too, as well as their existence
         */
        List<Integer> userIDs = new ArrayList<Integer>();
        userIDs.addAll(getUserEntities(touchedPermissions));
        UserPermissionBitsStorage permissionBitsStorage = UserPermissionBitsStorage.getInstance();
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(session.getContext());
            for (int userID : userIDs) {
                try {
                    UserPermissionBits permissionBits = permissionBitsStorage.getUserPermissionBits(connection, userID, session.getContext());
                    if (false == permissionBits.hasInfostore()) {
                        validation.setError(Metadata.OBJECT_PERMISSIONS_LITERAL, "User " + userID + " has no permissons for module infostore.");
                        validation.setException(InfostoreExceptionCodes.VALIDATION_FAILED_INAPPLICABLE_PERMISSIONS.create(getDisplayNameSafe(session.getContext(), userID)));
                        return false;
                    }
                } catch (OXException e) {
                    if (UserConfigurationCodes.NOT_FOUND.equals(e)) {
                        // user not found
                        validation.setError(Metadata.OBJECT_PERMISSIONS_LITERAL, e.getDisplayMessage(session.getUser().getLocale()));
                        validation.setException(InfostoreExceptionCodes.VALIDATION_FAILED_INAPPLICABLE_PERMISSIONS.create(getDisplayNameSafe(session.getContext(), userID)));
                        return false;
                    }
                    getLogger(ObjectPermissionValidator.class).warn("Error getting user configuration for permission entity {}", I(userID), e);
                    validation.setFatalException(e);
                }
            }
        } catch (OXException e) {
            getLogger(ObjectPermissionValidator.class).warn("Error getting user configuration for permission entities", e);
            validation.setFatalException(e);
        } finally {
            if (null != connection) {
                dbProvider.releaseReadConnection(session.getContext(), connection);
            }
        }
        return true;
    }

    private static int[] getGroupEntities(List<ObjectPermission> objectPermissions) {
        if (null != objectPermissions && 0 < objectPermissions.size()) {
            List<Integer> groupIDs = new ArrayList<Integer>();
            for (ObjectPermission objectPermission : objectPermissions) {
                if (objectPermission.isGroup()) {
                    groupIDs.add(I(objectPermission.getEntity()));
                }
            }
            return 0 < groupIDs.size() ? I2i(groupIDs) : null;
        }
        return null;
    }

    private static List<Integer> getUserEntities(List<ObjectPermission> objectPermissions) {
        List<Integer> userIDs = new ArrayList<Integer>();
        if (null != objectPermissions && 0 < objectPermissions.size()) {
            for (ObjectPermission objectPermission : objectPermissions) {
                if (false == objectPermission.isGroup()) {
                    userIDs.add(I(objectPermission.getEntity()));
                }
            }
        }
        return userIDs;
    }

    private static TouchedPermissions getTouchedPermissions(DocumentMetadata metadata, DocumentMetadata originalDocument, Set<Metadata> updatedColumns) {
        if (null == originalDocument) {
            /*
             * for new documents, check all applied object permissions
             */
            TouchedPermissions touchedPermissions = new TouchedPermissions();
            if (null != metadata.getObjectPermissions()) {
                touchedPermissions.changedAndNewPermissions.addAll(metadata.getObjectPermissions());
            }
            return touchedPermissions;
        } else if (null != updatedColumns && false == updatedColumns.contains(Metadata.OBJECT_PERMISSIONS_LITERAL)) {
            /*
             * object permissions not updated, so no checks needed
             */
            return null;
        } else {
            /*
             * for documents with updated object permissions, check for added, removed or modified permissions
             */
            TouchedPermissions touchedPermissions = new TouchedPermissions();
            List<ObjectPermission> originalPermissions = originalDocument.getObjectPermissions();
            List<ObjectPermission> newPermissions = metadata.getObjectPermissions();
            if (null != newPermissions) {
                for (ObjectPermission permission : newPermissions) {
                    ObjectPermission originalPermission = getPermissionByEntity(originalPermissions, permission.getEntity());
                    if (null == originalPermission || false == originalPermission.equals(permission)) {
                        touchedPermissions.changedAndNewPermissions.add(permission);
                    }
                }
            }
            if (null != originalPermissions) {
                for (ObjectPermission permission : originalPermissions) {
                    if (null == getPermissionByEntity(newPermissions, permission.getEntity())) {
                        touchedPermissions.removedPermissions.add(permission);
                    }
                }
            }
            return touchedPermissions;
        }
    }

    /**
     * Searches an array of permissions for a specific entity.
     *
     * @param permissions The permissions to search in
     * @param entity The identifier of the entity to lookup
     * @return The entity's permission, or <code>null</code> if not present in the supplied permissions
     */
    private static ObjectPermission getPermissionByEntity(List<ObjectPermission> permissions, int entity) {
        if (null != permissions) {
            for (ObjectPermission permission : permissions) {
                if (permission.getEntity() == entity) {
                    return permission;
                }
            }
        }
        return null;
    }

    /**
     * Safely gets a display name for a user entity to use in exception messages.
     * 
     * @param context The context the user is located in
     * @param userId The identifier of the user to get a proper display name
     * @return The display name
     */
    private static String getDisplayNameSafe(Context context, int userId) {
        try {
            User user = UserStorage.getInstance().getUser(userId, context);
            if (Strings.isNotEmpty(user.getDisplayName())) {
                return user.getDisplayName();
            }
            if (Strings.isNotEmpty(user.getMail())) {
                return user.getMail();
            }
        } catch (OXException e) {
            getLogger(ObjectPermissionValidator.class).warn("Unable to get display name for user {}", I(userId), e);
        }
        return "User " + userId;
    }

}
