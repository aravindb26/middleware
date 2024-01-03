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

package com.openexchange.importexport.importers;

import static com.openexchange.java.Autoboxing.I;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.mail.internet.AddressException;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.contact.vcard.storage.VCardStorageService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.tools.mappings.MappedIncorrectString;
import com.openexchange.groupware.tools.mappings.MappedTruncation;
import com.openexchange.importexport.helpers.ContactsAccess;
import com.openexchange.importexport.osgi.ImportExportServices;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link ContactImporter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class ContactImporter extends AbstractImporter {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContactImporter.class);

    /**
     * Defines the maximum number of implicit retries in case of truncation errors.
     */
    protected static final int MAX_RETRIES = 10;

    protected ContactImporter(ServiceLookup services) {
        super(services);
    }

    /**
     * Creates a new contact, implicitly trying again with trimmed values in
     * case of truncation errors.
     *
     * @param session the current session
     * @param contact the contact to create
     * @param folderID the target folder ID
     * @throws OXException
     */
    protected void createContact(Session session, Contact contact, String folderID) throws OXException {
        this.createContact(session, contact, folderID, (InputStream) null);
    }

    /**
     * Creates a new contact, implicitly trying again with trimmed values in
     * case of truncation errors.
     *
     * @param session the current session
     * @param contact the contact to create
     * @param folderID the target folder ID
     * @param vCard the FileHolder containing the original vCard
     * @throws OXException
     */
    protected void createContact(Session session, Contact contact, String folderID, IFileHolder vCard) throws OXException {
        this.createContact(session, contact, folderID, vCard == null ? null : vCard.getStream());
    }

    /**
     * Creates a new contact, implicitly trying again with trimmed values in
     * case of truncation errors.
     *
     * @param session the current session
     * @param contact the contact to create
     * @param folderID the target folder ID
     * @param vCard the VCard to persist or null if not available
     * @throws OXException
     */
    protected void createContact(Session session, Contact contact, String folderID, String vCard) throws OXException {
        this.createContact(session, contact, folderID, vCard == null ? null : new ByteArrayInputStream(vCard.getBytes(com.openexchange.java.Charsets.UTF_8)));
    }

    /**
     * Creates a new contact, implicitly trying again with trimmed values in
     * case of truncation errors.
     *
     * @param session the current session
     * @param contact the contact to create
     * @param folderID the target folder ID
     * @param vCard the InputStream providing the original vCard
     * @throws OXException
     */
    protected void createContact(Session session, Contact contact, String folderID, InputStream vCard) throws OXException {
        VCardStorageService vCardStorage = ImportExportServices.getVCardStorageService(session.getContextId());
        ContactsAccess contactsAccess = ImportExportServices.getContactsAccess();

        if (vCard != null && vCardStorage != null && contactsAccess.supports(session, folderID, ContactField.VCARD_ID)) {
            String vCardId = vCardStorage.saveVCard(vCard, session.getContextId());
            if (vCardId != null) {
                contact.setVCardId(vCardId);
            }
        }

        for (int retryCount = 1; retryCount <= MAX_RETRIES; retryCount++) {
            try {
                contactsAccess.createContact(session, folderID, contact);
                return;
            } catch (OXException e) {
                if (retryCount < MAX_RETRIES && handle(e, contact)) {
                    // try again
                    LOG.debug("{} - trying again ({}/{})", e.getMessage(), I(retryCount), I(MAX_RETRIES), e);
                    continue;
                }
                // re-throw
                throw e;
            } finally {
                if (vCardStorage != null && contact.getVCardId() != null && contact.getObjectID() == 0) {
                    vCardStorage.deleteVCard(contact.getVCardId(), session.getContextId());
                    contact.removeVCardId();
                }
            }
        }
    }

    protected boolean handle(OXException e, Contact contact) {
        if (ContactExceptionCodes.DATA_TRUNCATION.equals(e)) {
            return null != e.getProblematics() && trimTruncatedAttributes(e, contact);
        }
        if (ContactExceptionCodes.INCORRECT_STRING.equals(e)) {
            return null != e.getProblematics() && removeIncorrectStrings(e, contact);
        }

        if (ContactExceptionCodes.INVALID_EMAIL.equals(e)) {
            if (contact.containsEmail1()) {
                String value = contact.getEmail1();
                if (Strings.isNotEmpty(value)) {
                    try {
                        QuotedInternetAddress.validate(value);
                    } catch (@SuppressWarnings("unused") AddressException x) {
                        contact.setEmail1("");
                        return true;
                    }
                }
            }
            if (contact.containsEmail2()) {
                String value = contact.getEmail2();
                if (Strings.isNotEmpty(value)) {
                    try {
                        new QuotedInternetAddress(value).validate();
                    } catch (@SuppressWarnings("unused") AddressException x) {
                        contact.setEmail2("");
                        return true;
                    }
                }
            }
            if (contact.containsEmail3()) {
                String value = contact.getEmail3();
                if (Strings.isNotEmpty(value)) {
                    try {
                        new QuotedInternetAddress(value).validate();
                    } catch (@SuppressWarnings("unused") AddressException x) {
                        contact.setEmail3("");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean trimTruncatedAttributes(OXException e, Contact contact) {
        try {
            return MappedTruncation.truncate(e.getProblematics(), contact);
        } catch (OXException x) {
            LOG.warn("error trying to handle truncated attributes", x);
            return false;
        }
    }

    private static boolean removeIncorrectStrings(OXException e, Contact contact) {
        try {
            return MappedIncorrectString.replace(e.getProblematics(), contact, "");
        } catch (OXException x) {
            LOG.warn("error trying to handle incorrect strings", x);
            return false;
        }
    }

}
