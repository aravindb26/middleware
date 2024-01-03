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

package com.openexchange.contact.provider.composition;

import static com.openexchange.contact.ContactIDUtil.createContactID;
import static com.openexchange.contact.common.ContactsAccount.DEFAULT_ACCOUNT;
import static com.openexchange.contact.common.ContactsAccount.ID_PREFIX;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.contact.ContactID;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.java.Strings;

/**
 * {@link IDMangling}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class IDMangling {

    /** The virtual folder identifier used for basic contact providers */
    public static final String BASIC_FOLDER_ID = "0";

    // @formatter:off
    /** A set of fixed root folder identifiers excluded from ID mangling for the default account */
    protected static final Set<String> ROOT_FOLDER_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        null, // no parent
        "0",  // com.openexchange.folderstorage.FolderStorage.ROOT_ID
        "1",  // com.openexchange.folderstorage.FolderStorage.PRIVATE_ID
        "2",  // com.openexchange.folderstorage.FolderStorage.PUBLIC_ID
        "3"  // com.openexchange.folderstorage.FolderStorage.SHARED_ID
    )));
    // @formatter:on

    /** The prefix indicating a the virtual <i>shared</i> root (com.openexchange.groupware.container.FolderObject.SHARED_PREFIX) */
    protected static final String SHARED_PREFIX = "u:";

    /**
     * Gets the relative representation of a specific unique composite folder identifier.
     * <p/>
     * {@link IDMangling#ROOT_FOLDER_IDS} are passed as-is implicitly, same goes for identifiers starting with {@link AccountAwareIDMangler#SHARED_PREFIX}.
     *
     * @param uniqueFolderId The unique composite folder identifier, e.g. <code>con://11/38</code>
     * @return The extracted relative folder identifier
     * @throws OXException {@link ContactExceptionCodes.ID_PARSING_FAILED} if passed identifier can't be unmangled to its relative representation
     */
    public static String getRelativeFolderId(String uniqueFolderId) throws OXException {
        if (ROOT_FOLDER_IDS.contains(uniqueFolderId)) {
            return uniqueFolderId;
        }
        if (-1 != Strings.parsePositiveInt(uniqueFolderId)) {
            return uniqueFolderId; // use numerical folder id as-is
        }
        try {
            return unmangleFolderId(uniqueFolderId).get(2);
        } catch (IllegalArgumentException e) {
            throw ContactExceptionCodes.ID_PARSING_FAILED.create(e, uniqueFolderId);
        }
    }

    /**
     * Gets the relative representation of a specific unique full contact identifier consisting of composite parts.
     *
     * @param uniqueId The unique full contact identifier
     * @return The relative full contact identifier
     */
    public static ContactID getRelativeId(ContactID uniqueContactID) throws OXException {
        if (null == uniqueContactID) {
            return uniqueContactID;
        }
        return createContactID(getRelativeFolderId(uniqueContactID.getFolderID()), uniqueContactID.getObjectID());
    }

    /**
     * Gets the fully qualified composite representation of a specific relative folder identifier.
     * <p/>
     * {@link IDMangling#ROOT_FOLDER_IDS} as well as identifiers starting with {@link IDMangling#SHARED_PREFIX} are passed as-is implicitly.
     *
     * @param accountId The identifier of the account the folder originates in
     * @param relativeFolderId The relative folder identifier
     * @return The unique folder identifier
     */
    public static String getUniqueFolderId(int accountId, String relativeFolderId) {
        return getUniqueFolderId(accountId, relativeFolderId, DEFAULT_ACCOUNT.getAccountId() == accountId);
    }

    /**
     * Gets the fully qualified composite representation of a specific relative folder identifier.
     * <p/>
     * {@link IDMangling#ROOT_FOLDER_IDS} as well as identifiers starting with {@link IDMangling#SHARED_PREFIX} are passed as-is implicitly,
     * in case a <i>groupware</i> contacts access is indicated.
     *
     * @param accountId The identifier of the account the folder originates in
     * @param relativeFolderId The relative folder identifier
     * @param groupwareAccess <code>true</code> if the identifier originates from a <i>groupware</i> contacts access, <code>false</code>, otherwise
     * @return The unique folder identifier
     */
    public static String getUniqueFolderId(int accountId, String relativeFolderId, boolean groupwareAccess) {
        if (groupwareAccess || DEFAULT_ACCOUNT.getAccountId() == accountId) {
            if (ROOT_FOLDER_IDS.contains(relativeFolderId) || relativeFolderId.startsWith(SHARED_PREFIX)) {
                return relativeFolderId;
            }
        } else if (null == relativeFolderId) {
            return mangleFolderId(accountId, BASIC_FOLDER_ID);
        }
        return mangleFolderId(accountId, relativeFolderId);
    }

    /**
     * Gets the account identifier of a specific unique composite folder identifier.
     * <p/>
     * {@link IDMangling#ROOT_FOLDER_IDS} as well as identifiers starting with {@link IDMangling#SHARED_PREFIX} will always yield the
     * identifier of the default account.
     *
     * @param uniqueFolderId The unique composite folder identifier, e.g. <code>con://11/38</code>
     * @return The extracted account identifier
     * @throws OXException {@link ContactExceptionCodes.ID_PARSING_FAILED} if the account identifier can't be extracted from the passed composite identifier
     */
    public static int getAccountId(String uniqueFolderId) throws OXException {
        if (ROOT_FOLDER_IDS.contains(uniqueFolderId) || uniqueFolderId.startsWith(SHARED_PREFIX)) {
            return DEFAULT_ACCOUNT.getAccountId();
        }
        if (Strings.parsePositiveInt(uniqueFolderId) >= 0) {
            return DEFAULT_ACCOUNT.getAccountId();
        }
        // Carry on with unmangling
        try {
            return Integer.parseInt(unmangleFolderId(uniqueFolderId).get(1));
        } catch (IllegalArgumentException e) {
            throw ContactExceptionCodes.ID_PARSING_FAILED.create(e, uniqueFolderId);
        }
    }

    /**
     * <i>Mangles</i> the supplied relative folder identifier, together with its corresponding account information.
     *
     * @param accountId The identifier of the account the folder originates in
     * @param relativeFolderId The relative folder identifier
     * @return The mangled folder identifier
     */
    protected static String mangleFolderId(int accountId, String relativeFolderId) {
        return com.openexchange.tools.id.IDMangler.mangle(ID_PREFIX, String.valueOf(accountId), relativeFolderId);
    }

    /**
     * <i>Unmangles</i> the supplied unique folder identifier into its distinct components.
     *
     * @param uniqueFolderId The unique composite folder identifier, e.g. <code>con://11/38</code>
     * @return The unmangled components of the folder identifier
     * @throws IllegalArgumentException If passed identifier can't be unmangled into its distinct components
     */
    protected static List<String> unmangleFolderId(String uniqueFolderId) {
        if (null == uniqueFolderId || false == uniqueFolderId.startsWith(ID_PREFIX)) {
            throw new IllegalArgumentException(uniqueFolderId);
        }
        List<String> unmangled = com.openexchange.tools.id.IDMangler.unmangle(uniqueFolderId);
        if (null == unmangled || 3 > unmangled.size() || false == ID_PREFIX.equals(unmangled.get(0))) {
            throw new IllegalArgumentException(uniqueFolderId);
        }
        return unmangled;
    }

}
