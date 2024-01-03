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

package com.openexchange.contact.provider.internal.share;

import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.exception.OXException;

/**
 * {@link IDMangling}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class IDMangling {

    /**
     * Initializes a new {@link IDMangling}.
     */
    private IDMangling() {
        super();
    }

    protected static String optRelativeFolderId(String folderId) {
        try {
            return getRelativeFolderId(folderId);
        } catch (OXException e) {
            return null;
        }
    }

    protected static String getUniqueFolderId(String folderId) {
        if (folderId.startsWith(ContactsAccount.ID_PREFIX)) {
            return folderId;
        }
        return com.openexchange.contact.provider.composition.IDMangling.getUniqueFolderId(ContactsAccount.DEFAULT_ACCOUNT.getAccountId(), folderId, true);
    }

    protected static String getRelativeFolderId(String folderId) throws OXException {
        if (ContactsAccount.DEFAULT_ACCOUNT.getAccountId() == com.openexchange.contact.provider.composition.IDMangling.getAccountId(folderId)) {
            return com.openexchange.contact.provider.composition.IDMangling.getRelativeFolderId(folderId);
        }
        throw ContactsProviderExceptionCodes.UNSUPPORTED_FOLDER.create(folderId, "");
    }

}
