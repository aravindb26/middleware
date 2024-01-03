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

package com.openexchange.admin.storage.interfaces;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import org.slf4j.Logger;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.rmi.dataobjects.Account;
import com.openexchange.admin.rmi.dataobjects.AccountData;
import com.openexchange.admin.rmi.dataobjects.AccountDataOnCreate;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchUserException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.storage.mysqlStorage.OXSecondaryAccountMySQLStorage;
import com.openexchange.admin.tools.PropertyHandler;
import com.openexchange.java.Strings;

/**
 * {@link OXSecondaryAccountStorageInterface}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public abstract class OXSecondaryAccountStorageInterface {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OXSecondaryAccountStorageInterface.class);
    }

    private static volatile OXSecondaryAccountStorageInterface instance;

    /**
     * Creates a new instance implementing the secondary account storage interface.
     * @return an instance implementing the secondary account storage interface.
     * @throws com.openexchange.admin.rmi.exceptions.StorageException Storage exception
     */
    public static OXSecondaryAccountStorageInterface getInstance() throws StorageException {
        OXSecondaryAccountStorageInterface i = instance;
        if (null == i) {
            synchronized (OXSecondaryAccountStorageInterface.class) {
                i = instance;
                if (null == i) {
                    Class<? extends OXSecondaryAccountStorageInterface> implementingClass;
                    String className = ClientAdminThread.cache.getProperties().getProp(PropertyHandler.SECONDARY_ACCOUNT_STORAGE, OXSecondaryAccountMySQLStorage.class.getName());
                    if (Strings.isNotEmpty(className)) {
                        try {
                            implementingClass = Class.forName(className.trim()).asSubclass(OXSecondaryAccountStorageInterface.class);
                        } catch (ClassNotFoundException e) {
                            LoggerHolder.LOG.error("", e);
                            throw new StorageException(e);
                        }
                    } else {
                        StorageException storageException = new StorageException("Property for secondary account storage not defined");
                        LoggerHolder.LOG.error("", storageException);
                        throw storageException;
                    }

                    Constructor<? extends OXSecondaryAccountStorageInterface> cons;
                    try {
                        cons = implementingClass.getConstructor(new Class[] {});
                        i = cons.newInstance(new Object[] {});
                        instance = i;
                    } catch (SecurityException e) {
                        LoggerHolder.LOG.error("", e);
                        throw new StorageException(e);
                    } catch (NoSuchMethodException e) {
                        LoggerHolder.LOG.error("", e);
                        throw new StorageException(e);
                    } catch (IllegalArgumentException e) {
                        LoggerHolder.LOG.error("", e);
                        throw new StorageException(e);
                    } catch (InstantiationException e) {
                        LoggerHolder.LOG.error("", e);
                        throw new StorageException(e);
                    } catch (IllegalAccessException e) {
                        LoggerHolder.LOG.error("", e);
                        throw new StorageException(e);
                    } catch (InvocationTargetException e) {
                        LoggerHolder.LOG.error("", e);
                        throw new StorageException(e);
                    }
                }
            }
        }
        return i;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link OXSecondaryAccountStorageInterface}.
     */
    protected OXSecondaryAccountStorageInterface() {
        super();
    }

    /**
     * Creates given secondary account and makes it available to given users in specified context.
     *
     * @param accountData The account data
     * @param context The context to which the user belong
     * @param users One or more users to which the secondary account is made available
     * @param groups One or more groups to which the secondary account is made available
     * @throws RemoteException If a general remote exception occurs
     * @throws StorageException If a general storage exception occurs
     * @throws NoSuchContextException If specified context does not exist
     * @throws NoSuchUserException If specified user does not exist
     * @throws InvalidDataException If given account data is invalid
     * @throws DatabaseUpdateException If database is currently updating
     */
    public abstract void create(AccountDataOnCreate accountData, Context context, User[] users, Group[] groups) throws RemoteException, StorageException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException;

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Updates the account identified through given primary E-Mail address from given users in specified context.
     * <p>
     * Note: If both are absent - users and groups - secondary account is assumed being updated for all users in specified context.
     *
     * @param primaryAddress The primary E-Mail address identifying the account to delete
     * @param context The context to which the user belong
     * @param users One or more users from which the secondary account is removed
     * @param groups One or more groups to which the secondary account is removed
     * @return <code>true</code> if such a secondary account has been deleted; otherwise <code>false</code>
     * @throws RemoteException If a general remote exception occurs
     * @throws StorageException If a general storage exception occurs
     * @throws NoSuchContextException If specified context does not exist
     * @throws NoSuchUserException If specified user does not exist
     * @throws InvalidDataException If given account data is invalid
     * @throws DatabaseUpdateException If database is currently updating
     */
    public abstract boolean update(String primaryAddress, AccountData accountData, Context context, User[] users, Group[] groups) throws RemoteException, StorageException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException;

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Deletes the account identified through given primary E-Mail address from given users in specified context.
     *
     * @param primaryAddress The primary E-Mail address identifying the account to delete
     * @param context The context to which the user belong
     * @param users One or more users from which the secondary account is removed
     * @param groups One or more groups to which the secondary account is removed
     * @return <code>true</code> if such a secondary account has been deleted; otherwise <code>false</code>
     * @throws RemoteException If a general remote exception occurs
     * @throws StorageException If a general storage exception occurs
     * @throws NoSuchContextException If specified context does not exist
     * @throws NoSuchUserException If specified user does not exist
     * @throws InvalidDataException If given account data is invalid
     * @throws DatabaseUpdateException If database is currently updating
     */
    public abstract boolean delete(String primaryAddress, Context context, User[] users, Group[] groups) throws RemoteException, StorageException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException;

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Lists the accounts identified for given users in specified context.
     *
     * @param context The context to which the user belong
     * @param users One or more users for which secondary accounts shall be listed
     * @param groups One or more groups for which secondary accounts shall be listed
     * @throws RemoteException If a general remote exception occurs
     * @throws StorageException If a general storage exception occurs
     * @throws NoSuchContextException If specified context does not exist
     * @throws NoSuchUserException If specified user does not exist
     * @throws InvalidDataException If given account data is invalid
     * @throws DatabaseUpdateException If database is currently updating
     */
    public abstract Account[] list(Context context, User[] users, Group[] groups) throws RemoteException, StorageException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException;


}
