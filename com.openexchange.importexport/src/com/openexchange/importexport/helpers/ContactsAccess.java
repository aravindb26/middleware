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
package com.openexchange.importexport.helpers;

import static com.openexchange.java.Autoboxing.I;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.ContactIDUtil;
import com.openexchange.contact.common.AccountAwareContactsFolder;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.common.ContactsPermission;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ContactsAccess}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class ContactsAccess {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link ContactsAccess}.
     *
     * @param services The {@link ServiceLookup}
     */
    public ContactsAccess(ServiceLookup services) {
        this.services = services;
    }

    /**
     * Creates a new {@link IDBasedContactsAccess} for the given {@link Session}
     *
     * @param session The {@link Session} to get the {@link IDBasedContactsAccess} for.
     * @return The {@link IDBasedContactsAccess} for the given {@link Session}.
     * @throws OXException
     */
    private IDBasedContactsAccess getContactsAccess(Session session) throws OXException {
        IDBasedContactsAccessFactory factory = services.getServiceSafe(IDBasedContactsAccessFactory.class);
        return factory.createAccess(session);
    }

    /**
     * Gets a contacts folder
     *
     * @param session The {@link Session}
     * @param folderId The ID of the folder
     * @return The folder with the given ID
     * @throws OXException
     */
    public AccountAwareContactsFolder getFolder(Session session, String folderId) throws OXException {
        IDBasedContactsAccess contactsAccess = getContactsAccess(session);
        try {
            return contactsAccess.getFolder(folderId);
        } finally {
            contactsAccess.finish();
        }
    }

    /**
     * Checks if the given contacts folder can be read by the user associated with the given {@link Session}
     *
     * @param session The {@link Session}
     * @param folderId The ID of the folder
     * @return <code>True</code> if the associated user can read all objects within the given folder, <code>false</code> otherwise
     * @throws OXException
     */
    public boolean canReadFolder(ServerSession session, String folderId) throws OXException {
        // check read access to folder
        AccountAwareContactsFolder folder = getFolder(session, folderId);
        if (folder != null) {
            List<ContactsPermission> permissions = folder.getPermissions();
            List<Integer> userGroups = Arrays.stream(session.getUser().getGroups()).boxed().collect(Collectors.toList());
            if (permissions != null) {
                for (ContactsPermission permission : permissions) {
                    //@formatter:off
                    if ( ((permission.getEntity() == session.getUserId()) || (permission.isGroup() && userGroups.contains(I(permission.getEntity())))) &&
                          permission.getReadPermission() >= ContactsPermission.READ_ALL_OBJECTS) {
                        return true;
                    }
                    //@formatter:on
                }
            }
        }
        return false;
    }

    /**
     * Checks if the user associated with the given {@link Session} can create new objects in the given contacts folder
     *
     * @param session The {@link Session}
     * @param folderId The ID of the folder
     * @return <code>True</code> if the associated user can read all objects within the given folder, <code>false</code> otherwise
     * @throws OXException
     */
    public boolean canCreateObjectsInFolder(ServerSession session, String folderId) throws OXException {
        // check read access to folder
        AccountAwareContactsFolder folder = getFolder(session, folderId);
        if (folder != null) {
            List<ContactsPermission> permissions = folder.getPermissions();
            List<Integer> userGroups = Arrays.stream(session.getUser().getGroups()).boxed().collect(Collectors.toList());
            if (permissions != null) {
                for (ContactsPermission permission : permissions) {
                    //@formatter:off
                    if ( ((permission.getEntity() == session.getUserId()) || (permission.isGroup() && userGroups.contains(I(permission.getEntity())))) &&
                         permission.getFolderPermission() >= ContactsPermission.CREATE_OBJECTS_IN_FOLDER){
                        return true;
                    }
                    //@formatter:on
                }
            }
        }
        return false;
    }

    /**
     * Gets all contacts from the specified folder
     *
     * @param session The {@link Session}
     * @param folderId The ID of the folder to get the contacts for
     * @param fields The fields to load
     * @return A list of all contacts of the given folder
     * @throws OXException
     */
    public List<Contact> getContacts(Session session, String folderId, ContactField[] fields) throws OXException {
        IDBasedContactsAccess contactsAccess = getContactsAccess(session);
        try {
            contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, fields);
            return contactsAccess.getContacts(folderId);
        } finally {
            contactsAccess.finish();
        }
    }

    /**
     * Gets specific contacts from a given folder.
     *
     * @param session The {@link Session}
     * @param folderId The ID of the folder
     * @param ids The IDs of the contacts to get
     * @param fields The fields to load
     * @return The list of contacts from the given folder
     * @throws OXException
     */
    public List<Contact> getContacts(Session session, String folderId, List<String> ids, ContactField[] fields) throws OXException {
        IDBasedContactsAccess contactsAccess = getContactsAccess(session);
        try {
            contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, fields);
            List<ContactID> contactIDs = ids.stream().map(id -> ContactIDUtil.createContactID(folderId, id)).collect(Collectors.toList());
            return contactsAccess.getContacts(contactIDs);
        } finally {
            contactsAccess.finish();
        }
    }

    /**
     * Gets a single contact
     *
     * @param session The {@link Session}
     * @param folderId The ID of the folder
     * @param contactId The ID of the contact
     * @param fields The fields to load
     * @return The contact with the given ID
     * @throws OXException
     */
    public Contact getContact(Session session, String folderId, String contactId, ContactField[] fields) throws OXException {
        IDBasedContactsAccess contactsAccess = getContactsAccess(session);
        try {
            contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, fields);
            return contactsAccess.getContact(ContactIDUtil.createContactID(folderId, contactId));
        } finally {
            contactsAccess.finish();
        }
    }

    /**
     * Creates a new contact in the given folder
     *
     * @param session The {@link Session}
     * @param folderId The ID of the folder to create the contact in
     * @param contact The contact to create
     * @throws OXException
     */
    public void createContact(Session session, String folderId, Contact contact) throws OXException {
        IDBasedContactsAccess contactsAccess = getContactsAccess(session);
        try {
            contactsAccess.createContact(folderId, contact);
        } finally {
            contactsAccess.finish();
        }
    }

    /**
     * Gets a user's contact with all fields.<p>
     *
     * @param session The session
     * @param userId The ID of the user
     * @return The contact
     * @throws OXException
     * @see {@link com.openexchange.contact.provider.composition.IDBasedUserAccess#getUserContact(int)}
     */
    public Contact getUserContact(Session session, int userId) throws OXException {
        IDBasedContactsAccess contactsAccess = getContactsAccess(session);
        try {
            return contactsAccess.getUserAccess().getUserContact(userId);
        } finally {
            contactsAccess.finish();
        }
    }

    /**
     * Returns if the provided contact fields are supported by the storage
     *
     * @param session The session
     * @param folderId The ID of the folder to check
     * @param fields The fields to check for support
     * @return <code>true</code> if all fields are supported, <code>false</code> if at least one field is not supported
     * @throws OXException
     */
    public boolean supports(Session session, String folderId, ContactField... fields) throws OXException {
        IDBasedContactsAccess contactsAccess = getContactsAccess(session);
        try {
            return contactsAccess.supports(folderId, fields);
        } finally {
            contactsAccess.finish();
        }
    }
}
