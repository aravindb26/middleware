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

package com.openexchange.admin.console.user;

import java.rmi.RemoteException;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.rmi.OXUserInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.DuplicateExtensionException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchUserException;
import com.openexchange.admin.rmi.exceptions.StorageException;

public abstract class ListCoreExtended extends ListCore {

    @Override
    protected void setOptions(final AdminParser parser) {
        super.setOptions(parser);
        this.searchOption = setShortLongOpt(parser, OPT_NAME_SEARCHPATTERN, OPT_NAME_SEARCHPATTERN_LONG, "The search pattern which is used for listing. This applies to name.", true, NeededQuadState.notneeded);
        this.ignoreCaseOption = setShortLongOpt(parser, OPT_NAME_IGNORECASE, OPT_NAME_IGNORECASE_LONG, "Whether to perform look-up case-insensitive", false, NeededQuadState.notneeded);
    }

    @Override
    protected User[] maincall(final AdminParser parser, final OXUserInterface oxusr, final Context ctx, final Credentials auth, final Integer length, final Integer offset) throws Exception {
        String pattern = (String) parser.getOptionValue(this.searchOption);
        if (null == pattern) {
            pattern = "*";
        }

        Boolean ignoreCase = (Boolean) parser.getOptionValue(this.ignoreCaseOption);
        if (null == ignoreCase) {
            ignoreCase = Boolean.FALSE;
        }

        Boolean includeGuests = (Boolean) parser.getOptionValue(this.includeGuestsOption);
        if (null == includeGuests) {
            includeGuests = Boolean.FALSE;
        }

        Boolean excludeUsers = (Boolean) parser.getOptionValue(this.excludeUsersOption);
        if (null == excludeUsers) {
            excludeUsers = Boolean.FALSE;
        }

        return maincall(parser, oxusr, pattern, ignoreCase.booleanValue(), ctx, auth, includeGuests.booleanValue(), excludeUsers.booleanValue(), length, offset);
    }

    protected abstract User[] maincall(final AdminParser parser, final OXUserInterface oxusr, final String search_pattern, final boolean ignoreCase, final Context ctx, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException, DuplicateExtensionException;

    protected abstract User[] maincall(final AdminParser parser, final OXUserInterface oxusr, final String search_pattern, final boolean ignoreCase, final Context ctx, final Credentials auth, final boolean includeGuests, final boolean excludeUsers, final Integer length, final Integer offset) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException, DuplicateExtensionException;
}
