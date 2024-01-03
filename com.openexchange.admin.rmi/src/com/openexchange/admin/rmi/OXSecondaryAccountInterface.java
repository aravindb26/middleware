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
package com.openexchange.admin.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import com.openexchange.admin.rmi.dataobjects.Account;
import com.openexchange.admin.rmi.dataobjects.AccountData;
import com.openexchange.admin.rmi.dataobjects.AccountDataOnCreate;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchUserException;
import com.openexchange.admin.rmi.exceptions.StorageException;

/**
 * This interface defines the methods of the secondary account management.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.10.6
 */
public interface OXSecondaryAccountInterface extends Remote {

    /**
     * RMI name to be used in the naming lookup.
     */
    public static final String RMI_NAME = "OXSecondaryAccount";

    /**
     * Creates given secondary account and makes it available to given users in specified context.
     * <p>
     * Note: If both are absent - users and groups - secondary account is assumed being made available to all users in specified context.
     *
     * @param accountData The account data
     * @param context The context to which the user belong
     * @param users One or more users to which the secondary account is made available
     * @param groups One or more groups to which the secondary account is made available
     * @param auth The credentials
     * @throws RemoteException If a general remote exception occurs
     * @throws StorageException If a general storage exception occurs
     * @throws InvalidCredentialsException If provided credentials are invalid
     * @throws NoSuchContextException If specified context does not exist
     * @throws NoSuchUserException If specified user does not exist
     * @throws InvalidDataException If given account data is invalid
     * @throws DatabaseUpdateException If database is currently updating
     */
    void create(AccountDataOnCreate accountData, Context context, User[] users, Group[] groups, Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException;

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
     * @param auth The credentials
     * @return <code>true</code> if such a secondary account has been deleted; otherwise <code>false</code>
     * @throws RemoteException If a general remote exception occurs
     * @throws StorageException If a general storage exception occurs
     * @throws InvalidCredentialsException If provided credentials are invalid
     * @throws NoSuchContextException If specified context does not exist
     * @throws NoSuchUserException If specified user does not exist
     * @throws InvalidDataException If given account data is invalid
     * @throws DatabaseUpdateException If database is currently updating
     */
    boolean update(String primaryAddress, AccountData accountData, Context context, User[] users, Group[] groups, Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException;

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Deletes the account identified through given primary E-Mail address from given users in specified context.
     * <p>
     * Note: If both are absent - users and groups - secondary account is assumed being deleted for all users in specified context.
     *
     * @param primaryAddress The primary E-Mail address identifying the account to delete
     * @param context The context to which the user belong
     * @param users One or more users from which the secondary account is removed
     * @param groups One or more groups to which the secondary account is removed
     * @param auth The credentials
     * @return <code>true</code> if such a secondary account has been deleted; otherwise <code>false</code>
     * @throws RemoteException If a general remote exception occurs
     * @throws StorageException If a general storage exception occurs
     * @throws InvalidCredentialsException If provided credentials are invalid
     * @throws NoSuchContextException If specified context does not exist
     * @throws NoSuchUserException If specified user does not exist
     * @throws InvalidDataException If given account data is invalid
     * @throws DatabaseUpdateException If database is currently updating
     */
    boolean delete(String primaryAddress, Context context, User[] users, Group[] groups, Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException;

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Lists the accounts identified for given users in specified context.
     * <p>
     * Note: If both are absent - users and groups - secondary account is listed for all users in specified context.
     *
     * @param context The context to which the user belong
     * @param users One or more users for which secondary accounts shall be listed
     * @param groups One or more groups for which secondary accounts shall be listed
     * @param auth The credentials
     * @throws RemoteException If a general remote exception occurs
     * @throws StorageException If a general storage exception occurs
     * @throws InvalidCredentialsException If provided credentials are invalid
     * @throws NoSuchContextException If specified context does not exist
     * @throws NoSuchUserException If specified user does not exist
     * @throws InvalidDataException If given account data is invalid
     * @throws DatabaseUpdateException If database is currently updating
     */
    Account[] list(Context context, User[] users, Group[] groups, Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException;

}
