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

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.resource.ResourcePermissionUtility.DEFAULT_PERMISSIONS;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.mail.internet.AddressException;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.exception.OXException;
import com.openexchange.group.GroupService;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.util.Pair;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.resource.ResourcePermission;
import com.openexchange.resource.SchedulingPrivilege;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.user.User;
import com.openexchange.user.UserService;

/**
 * {@link ResourceTools} - Utility methods for resource module
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ResourceTools {

    /**
     * Property to enable or disable a special mode to control which permission combinations are possible. If enabled, only two
     * alternatives for permissions are allowed to cover the basic managed/unmanaged resource semantics: either 'book_directly' for all
     * users (group 0), or, a combination of 'ask_to_book' for group 0, plus one or more entities who act as 'delegate'.
     * Note: This simple mode is enabled by default, and must remain 'true', as long as resources are managed in App Suite by users with
     * 'editresource' module permission.
     */
    private static final Property PROPERTY_SIMPLE_PERMISSION_MODE = DefaultProperty.valueOf("com.openexchange.resource.simplePermissionMode", Boolean.TRUE);

    /**
     * Initializes a new {@link ResourceTools}
     */
    private ResourceTools() {
        super();
    }

    private static final Pattern PATTERN_ALLOWED_CHARS = Pattern.compile("[\\S ]+");

    /**
     * Checks if specified resource identifier contains invalid characters.
     * <p>
     * Valid characters are:<br>
     * &quot;<i>&nbsp; abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_ -+.%$@<i>&quot;
     *
     * @param identifier The resource identifier to check
     * @return <code>true</code> if specified resource identifier only consists of valid characters; otherwise <code>false</code>
     */
    public static boolean validateResourceIdentifier(final String identifier) {
        /*
         * Check for allowed chars: abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 _-+.%$@
         */
        return PATTERN_ALLOWED_CHARS.matcher(identifier).matches();
    }

    // private static final Pattern PATTERN_VALID_EMAIL =
    // Pattern.compile("[$%\\.+a-zA-Z0-9_-]+@[\\.a-zA-Z0-9_-]+");

    /**
     * Checks if specified resource email address' notation is valid
     *
     * @param emailAddress The resource email address to check
     * @return <code>true</code> if specified resource email address is valid; otherwise <code>false</code>
     */
    public static boolean validateResourceEmail(final String emailAddress) {
        /*
         * Validate e-mail with InternetAddress class from JavaMail API
         */
        try {
            new QuotedInternetAddress(emailAddress, true).validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }

    /**
     * Validates the assigned resource permissions, throwing an exception if they're invalid.
     * 
     * @param context The context
     * @param permissions The permissions to validate
     * @throws OXException If validation fails
     */
    public static void validatePermissions(Context context, ResourcePermission[] permissions) throws OXException {
        if (null == permissions || 0 == permissions.length || Arrays.equals(DEFAULT_PERMISSIONS, permissions)) {
            return;
        }
        Map<SchedulingPrivilege, List<ResourcePermission>> permissionsByPrivilege = new HashMap<SchedulingPrivilege, List<ResourcePermission>>();
        Set<Pair<Integer, Boolean>> usedEntities = new HashSet<Pair<Integer, Boolean>>();
        for (ResourcePermission permission : permissions) {
            /*
             * check each entity is only defined once, it actually exists & doesn't refer to a guest entity
             */
            if (false == usedEntities.add(new Pair<Integer, Boolean>(I(permission.getEntity()), B(permission.isGroup())))) {
                throw ResourceExceptionCode.INVALID_SCHEDULING_PRIVILEGES.create("Duplicate permission entity " + permission.getEntity());
            }
            if (permission.isGroup()) {
                if (GroupStorage.GUEST_GROUP_IDENTIFIER == permission.getEntity()) {
                    throw ResourceExceptionCode.NO_GUEST_PRIVILEGES.create(I(permission.getEntity()));
                }
                ServerServiceRegistry.getServize(GroupService.class, true).getGroup(context, permission.getEntity());
            } else {
                User user = ServerServiceRegistry.getServize(UserService.class, true).getUser(permission.getEntity(), context);
                if (user.isGuest()) {
                    throw ResourceExceptionCode.NO_GUEST_PRIVILEGES.create(I(permission.getEntity()));
                }
            }
            /*
             * collect permission per privilege for subsequent checks
             */
            if (null == permission.getSchedulingPrivilege() || SchedulingPrivilege.NONE.equals(permission.getSchedulingPrivilege())) {
                throw ResourceExceptionCode.INVALID_SCHEDULING_PRIVILEGES.create("Invalid privilege for entity " + permission.getEntity());
            }
            com.openexchange.tools.arrays.Collections.put(permissionsByPrivilege, permission.getSchedulingPrivilege(), permission);
        }
        /*
         * if there is an 'ask_to_book' privilege, ensure that there's at least one 'delegate' defined as well
         */
        if (permissionsByPrivilege.containsKey(SchedulingPrivilege.ASK_TO_BOOK) && false == permissionsByPrivilege.containsKey(SchedulingPrivilege.DELEGATE)) {
            throw ResourceExceptionCode.NO_BOOKING_DELEGATE.create();
        }
        /*
         * in special simple mode, only allow certain constellations
         */
        if (isSimplePermissionMode(context.getContextId())) {
            List<ResourcePermission> delegatePermissions = permissionsByPrivilege.get(SchedulingPrivilege.DELEGATE);
            if (null == delegatePermissions || 0 == delegatePermissions.size()) {
                /*
                 * no 'delegate' defined, so only 'book_directly' for group 0 is possible 
                 */
                if (false == Arrays.equals(DEFAULT_PERMISSIONS, permissions)) {
                    throw ResourceExceptionCode.INVALID_SCHEDULING_PRIVILEGES.create("Only 'book_directly' for group 0 allowed without 'delegate' in simple permission mode");
                }
            } else {
                /*
                 * at least one 'delegate' defined, so only a remaining 'ask_to_book' for group 0 is possible
                 */
                if (permissionsByPrivilege.containsKey(SchedulingPrivilege.BOOK_DIRECTLY) || permissionsByPrivilege.containsKey(SchedulingPrivilege.NONE)) {
                    throw ResourceExceptionCode.INVALID_SCHEDULING_PRIVILEGES.create("Only 'ask_to_book' for group 0 allowed with 'delegate' in simple permission mode");
                }
                List<ResourcePermission> askToBookPermissions = permissionsByPrivilege.get(SchedulingPrivilege.ASK_TO_BOOK);
                if (null == askToBookPermissions || 1 != askToBookPermissions.size() || 
                    false == new ResourcePermission(GroupStorage.GROUP_ZERO_IDENTIFIER, true, SchedulingPrivilege.ASK_TO_BOOK).equals(askToBookPermissions.get(0))) {
                    throw ResourceExceptionCode.INVALID_SCHEDULING_PRIVILEGES.create("Only 'ask_to_book' for group 0 allowed with 'delegate' in simple permission mode");
                }
            }
        }
    }
    
    private static boolean isSimplePermissionMode(int contextId) throws OXException {
        return ServerServiceRegistry.getServize(LeanConfigurationService.class, true).getBooleanProperty(-1, contextId, PROPERTY_SIMPLE_PERMISSION_MODE);
    }

}
