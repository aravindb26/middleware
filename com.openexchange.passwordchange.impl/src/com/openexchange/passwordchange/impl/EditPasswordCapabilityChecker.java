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

package com.openexchange.passwordchange.impl;

import com.openexchange.capabilities.CapabilityChecker;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.userconfiguration.Permission;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.java.Strings;
import com.openexchange.passwordchange.PasswordChangeService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.User;
import com.openexchange.userconf.UserPermissionService;

/**
 * Checks if the user with provided session does have the capability to change the password ('edit_password').
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.1
 */
public class EditPasswordCapabilityChecker implements CapabilityChecker {

    /** The capability to set */
    public final static String EDIT_PASSWORD_CAP = "edit_password";
    private final ServiceLookup serviceLookup;
    private final PasswordChangeRegistryImpl registry;

    /**
     * Initializes a new {@link EditPasswordCapabilityChecker}.
     *
     * @param serviceLookup The service lookup
     * @param registry The registry for {@link PasswordChangeService}
     */
    public EditPasswordCapabilityChecker(ServiceLookup serviceLookup, PasswordChangeRegistryImpl registry) {
        this.serviceLookup = serviceLookup;
        this.registry = registry;
    }

    @Override
    public boolean isEnabled(String capability, Session ses) throws OXException {
        if (false == EDIT_PASSWORD_CAP.equals(capability)) {
            return false;
        }
        if ((Strings.isEmpty(capability)) || (ses == null)) {
            return false;
        }

        final ServerSession session = ServerSessionAdapter.valueOf(ses);
        if ((session == null) || (session.isAnonymous())) {
            return false;
        }
        int contextId = session.getContextId();
        int userId = session.getUserId();
        if (contextId <= 0 && userId <= 0) {
            return false;
        }
        User user = session.getUser();
        if ((user != null) && (user.isGuest()) && Strings.isNotEmpty(user.getMail())) {
            return true;
        }
        /* Check if some service is available in general */
        if (registry.getServiceList().isEmpty() || false == registry.isEnabled(contextId, userId)) {
            return false;
        }
        /* Check if permission is set for the user */
        final UserPermissionService userPermissionService = serviceLookup.getService(UserPermissionService.class);
        if (userPermissionService == null) {
            return false;
        }
        UserPermissionBits userPermissionBits = userPermissionService.getUserPermissionBits(userId, session.getContext());
        return userPermissionBits != null && userPermissionBits.hasPermission(Permission.EDIT_PASSWORD);
    }
}
