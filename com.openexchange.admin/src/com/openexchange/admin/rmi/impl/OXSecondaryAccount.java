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

package com.openexchange.admin.rmi.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.plugins.OXSecondaryAccountPluginInterface;
import com.openexchange.admin.properties.AdminProperties;
import com.openexchange.admin.rmi.OXSecondaryAccountInterface;
import com.openexchange.admin.rmi.dataobjects.Account;
import com.openexchange.admin.rmi.dataobjects.AccountData;
import com.openexchange.admin.rmi.dataobjects.AccountDataOnCreate;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.AbstractAdminRmiException;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchObjectException;
import com.openexchange.admin.rmi.exceptions.NoSuchUserException;
import com.openexchange.admin.rmi.exceptions.RemoteExceptionUtils;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.services.PluginInterfaces;
import com.openexchange.admin.storage.interfaces.OXSecondaryAccountStorageInterface;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.admin.tools.PropertyHandler;
import com.openexchange.group.GroupStorage;
import com.openexchange.java.Strings;


/**
 * {@link OXSecondaryAccount}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class OXSecondaryAccount extends OXCommonImpl implements OXSecondaryAccountInterface {

    /** The logger */
    final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OXSecondaryAccount.class);

    private static final String EMPTY_STRING = "";

    private final BasicAuthenticator basicauth;
    private final AdminCache cache;
    private final PropertyHandler prop;
    private final OXSecondaryAccountStorageInterface oxsa;

    /**
     * Initializes a new {@link OXSecondaryAccount}.
     *
     * @throws StorageException If initialization fails
     */
    public OXSecondaryAccount() throws StorageException {
        super();
        this.cache = ClientAdminThread.cache;
        this.prop = this.cache.getProperties();
        basicauth = BasicAuthenticator.createPluginAwareAuthenticator();
        oxsa = OXSecondaryAccountStorageInterface.getInstance();
    }

    @Override
    public void create(AccountDataOnCreate accountData, Context context, User[] usersArg, Group[] groupsArg, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException {
        try {
            if (accountData == null) {
                throw new InvalidDataException("Account data is missing.");
            }
            if (null == context) {
                throw new InvalidDataException("Missing context.");
            }
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }
            basicauth.doAuthentication(auth, context);
            checkContextAndSchema(context);
            User[] users = prepareUsers(usersArg, context);
            Group[] groups = prepareGroups(groupsArg, context);
            if ((null == users || 0 == users.length) && (null == groups || 0 == groups.length)) {
                throw new InvalidDataException("Missing users/groups in account data");
            }
            oxsa.create(accountData, context, users, groups);

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXSecondaryAccountPluginInterface oxsecondaryaccpount : pluginInterfaces.getSecondaryaccountPlugins().getServiceList()) {
                        oxsecondaryaccpount.create(accountData, context, users, groups, auth);
                    }
                }
            }
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, context, null);
            throw e;
        }
    }

    @Override
    public boolean update(String primaryAddress, AccountData accountData, Context context, User[] usersArg, Group[] groupsArg, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException {
        try {
            if (Strings.isEmpty(primaryAddress)) {
                throw new InvalidDataException("Primary address is empty.");
            }
            if (null == context) {
                throw new InvalidDataException("Missing context.");
            }
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }
            basicauth.doAuthentication(auth, context);
            checkContextAndSchema(context);
            User[] users = prepareUsers(usersArg, context);
            Group[] groups = prepareGroups(groupsArg, context);
            if ((null == users || 0 == users.length) && (null == groups || 0 == groups.length)) {
                throw new InvalidDataException("Missing users/groups in account data");
            }
            boolean updated = oxsa.update(primaryAddress, accountData, context, users, groups);

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXSecondaryAccountPluginInterface oxsecondaryaccpount : pluginInterfaces.getSecondaryaccountPlugins().getServiceList()) {
                        oxsecondaryaccpount.update(primaryAddress, accountData, context, users, groups, auth);
                    }
                }
            }

            return updated;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, context, null);
            throw e;
        }
    }

    @Override
    public boolean delete(String primaryAddress, Context context, User[] usersArg, Group[] groupsArg, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException {
        try {
            if (Strings.isEmpty(primaryAddress)) {
                throw new InvalidDataException("Primary address is empty.");
            }
            if (null == context) {
                throw new InvalidDataException("Missing context.");
            }
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }
            basicauth.doAuthentication(auth, context);
            checkContextAndSchema(context);
            User[] users = prepareUsers(usersArg, context);
            Group[] groups = prepareGroups(groupsArg, context);
            if ((null == users || 0 == users.length) && (null == groups || 0 == groups.length)) {
                throw new InvalidDataException("Missing users/groups in account data");
            }
            boolean deleted = oxsa.delete(primaryAddress, context, users, groups);

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXSecondaryAccountPluginInterface oxsecondaryaccpount : pluginInterfaces.getSecondaryaccountPlugins().getServiceList()) {
                        oxsecondaryaccpount.delete(primaryAddress, context, users, groups, auth);
                    }
                }
            }

            return deleted;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, context, null);
            throw e;
        }
    }

    @Override
    public Account[] list(Context context, User[] usersArg, Group[] groupsArg, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException {
        try {
            if (null == context) {
                throw new InvalidDataException("Missing context.");
            }
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }
            basicauth.doAuthentication(auth, context);
            checkContextAndSchema(context);
            User[] users = removeNullElementsFrom(usersArg);
            if (users != null) {
                try {
                    for (User user : users) {
                        setIdOrGetIDFromNameAndIdObject(context, user);
                    }
                } catch (NoSuchObjectException e) {
                    throw new NoSuchUserException(e);
                }
                for (User user : users) {
                    int userId = user.getId().intValue();
                    if (!tool.existsUser(context, userId)) {
                        throw new NoSuchUserException("No such user " + userId + " in context " + context.getId());
                    }
                }
            }
            Group[] groups = prepareGroups(groupsArg, context);
            Account[] accounts = oxsa.list(context, users, groups);

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXSecondaryAccountPluginInterface oxsecondaryaccpount : pluginInterfaces.getSecondaryaccountPlugins().getServiceList()) {
                        accounts = oxsecondaryaccpount.list(context, accounts, users, groups, auth);
                    }
                }
            }

            return accounts;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, context, null);
            throw e;
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private void logAndEnhanceException(Throwable t, final Credentials credentials, final Context ctx, final User usr) {
        logAndEnhanceException(t, credentials, null != ctx ? ctx.getIdAsString() : null, null != usr ? String.valueOf(usr.getId()) : null);
    }

    private void logAndEnhanceException(Throwable t, final Credentials credentials, final String contextId, String userId) {
        if (t instanceof AbstractAdminRmiException) {
            logAndReturnException(LOGGER, ((AbstractAdminRmiException) t), credentials, contextId, userId);
        } else if (t instanceof RemoteException) {
            RemoteException remoteException = (RemoteException) t;
            String exceptionId = AbstractAdminRmiException.generateExceptionId();
            RemoteExceptionUtils.enhanceRemoteException(remoteException, exceptionId);
            logAndReturnException(LOGGER, remoteException, exceptionId, credentials, contextId, userId);
        } else if (t instanceof Exception) {
            RemoteException remoteException = RemoteExceptionUtils.convertException((Exception) t);
            String exceptionId = AbstractAdminRmiException.generateExceptionId();
            RemoteExceptionUtils.enhanceRemoteException(remoteException, exceptionId);
            logAndReturnException(LOGGER, remoteException, exceptionId, credentials, contextId, userId);
        }
    }

    private User[] prepareUsers(User[] usersArg, Context context) throws StorageException, InvalidDataException, NoSuchUserException {
        User[] users = removeNullElementsFrom(usersArg);
        if (users != null) {
            try {
                for (User user : users) {
                    setIdOrGetIDFromNameAndIdObject(context, user);
                }
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            for (User user : users) {
                int userId = user.getId().intValue();
                if (!tool.existsUser(context, userId)) {
                    throw new NoSuchUserException("No such user " + userId + " in context " + context.getId());
                }
            }
        }
        return users;
    }

    private Group[] prepareGroups(Group[] groupsArg, Context context) throws StorageException, InvalidDataException, NoSuchUserException {
        Group[] groups = removeNullElementsFrom(groupsArg);
        if (groups != null) {
            try {
                for (Group group : groups) {
                    Integer id = group.getId();
                    if (null == id || 0 > id.intValue()) {
                        setIdOrGetIDFromNameAndIdObject(context, group);
                    }
                }
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            for (Group group : groups) {
                int groupId = group.getId().intValue();
                if (GroupStorage.GROUP_ZERO_IDENTIFIER != groupId && !tool.existsGroup(context, groupId)) {
                    throw new NoSuchUserException("No such group " + groupId + " in context " + context.getId());
                }
            }
        }
        return groups;
    }

    private static User[] removeNullElementsFrom(User[] users) {
        if (users == null) {
            return users;
        }

        int length = users.length;
        if (length <= 0) {
            return users;
        }

        List<User> tmp = null;
        for (int i = 0; i < length; i++) {
            User user = users[i];
            if (user == null) {
                if (tmp == null) {
                    tmp = new ArrayList<>(length);
                    if (i > 0) {
                        for (int k = 0; k < i; k++) {
                            tmp.add(users[k]);
                        }
                    }
                }
            } else {
                if (tmp != null) {
                    tmp.add(user);
                }
            }
        }
        return tmp == null ? users : tmp.toArray(new User[tmp.size()]);
    }

    private static Group[] removeNullElementsFrom(Group[] groups) {
        if (groups == null) {
            return groups;
        }

        int length = groups.length;
        if (length <= 0) {
            return groups;
        }

        List<Group> tmp = null;
        for (int i = 0; i < length; i++) {
            Group group = groups[i];
            if (group == null) {
                if (tmp == null) {
                    tmp = new ArrayList<>(length);
                    if (i > 0) {
                        for (int k = 0; k < i; k++) {
                            tmp.add(groups[k]);
                        }
                    }
                }
            } else {
                if (tmp != null) {
                    tmp.add(group);
                }
            }
        }
        return tmp == null ? groups : tmp.toArray(new Group[tmp.size()]);
    }

}
