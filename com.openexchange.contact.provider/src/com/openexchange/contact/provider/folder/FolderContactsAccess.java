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

package com.openexchange.contact.provider.folder;

import java.util.Collections;
import java.util.List;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.common.ContactsFolder;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.ContactsAccess;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;

/**
 * {@link FolderContactsAccess}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public interface FolderContactsAccess extends ContactsAccess {

    /**
     * Gets a specific contacts folder.
     *
     * @param folderId The identifier of the contacts folder to get
     * @return The contacts folder
     */
    ContactsFolder getFolder(String folderId) throws OXException;

    /**
     * Returns a list with all visible contacts folders for the user
     *
     * @return a list with all visible contacts folders for the user
     */
    List<? extends ContactsFolder> getVisibleFolders() throws OXException;

    /**
     * Gets a specific contact from a specific folder
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param folderId The identifier of the folder representing the current user's contacts
     * @param contactId The identifier of the contact to get
     * @return The contact
     */
    default Contact getContact(String folderId, String contactId) throws OXException {
        List<Contact> contacts = getContacts(Collections.singletonList(new ContactID(folderId, contactId)));
        if (null == contacts || contacts.isEmpty()) {
            throw ContactsProviderExceptionCodes.CONTACT_NOT_FOUND_IN_FOLDER.create(folderId, contactId);
        }
        Contact contact = contacts.get(0);
        if (null == contact) {
            throw ContactsProviderExceptionCodes.CONTACT_NOT_FOUND_IN_FOLDER.create(folderId, contactId);
        }
        return contact;
    }

    /**
     * Gets a list of contacts with the specified identifiers.
     *
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param contactIDs A list of the identifiers of the contacts to get
     * @return The contacts
     */
    List<Contact> getContacts(List<ContactID> contactIDs) throws OXException;

    /**
     * Gets all contacts in a specific contacts folder.
     *
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     * </ul>
     *
     * @param folderId The identifier of the folder to get the contacts from
     * @return The contacts
     */
    List<Contact> getContacts(String folderId) throws OXException;

}
