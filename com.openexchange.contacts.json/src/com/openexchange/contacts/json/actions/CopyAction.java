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

package com.openexchange.contacts.json.actions;

import static com.openexchange.java.Strings.parseInt;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.contact.common.AccountAwareContactsFolder;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contacts.json.ContactRequest;
import com.openexchange.contacts.json.mapping.ContactMapper;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.attach.AttachmentMetadataFactory;
import com.openexchange.groupware.attach.Attachments;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.java.Streams;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link CopyAction}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
@RestrictedAction(module = IDBasedContactAction.MODULE_NAME, type = RestrictedAction.Type.WRITE)
public class CopyAction extends IDBasedContactAction {

    /**
     * Initializes a new {@link CopyAction}.
     *
     * @param serviceLookup A service lookup reference
     * @param internalAccessOnly <code>true</code> to use the <i>internal</i> contacts access directly, <code>false</code> to use the compositing access interfaces
     */
    public CopyAction(ServiceLookup serviceLookup, boolean internalAccessOnly) {
        super(serviceLookup, internalAccessOnly);
    }

    @Override
    protected AJAXRequestResult perform(IDBasedContactsAccess access, ContactRequest request) throws OXException {
        /*
         * prepare copy of original contact
         */
        Contact originalContact = access.getContact(getContactID(request.getFolderID(), request.getObjectID()));
        Contact contact = ContactMapper.getInstance().copy(originalContact, null, (ContactField[]) null);
        contact.removeId(true);
        contact.removeFolderId(true);
        contact.removeUid();
        contact.removeNumberOfAttachments();
        /*
         * create copy in target folder & prepare result
         */
        String destFolderId = request.getFolderIDFromData();
        access.createContact(destFolderId, contact);
        String newId = contact.getId();
        AJAXRequestResult result = new AJAXRequestResult(new JSONObject().putSafe("id", newId), contact.getLastModified(), "json");
        /*
         * copy over attachments as needed, if possible
         */
        if (0 < originalContact.getNumberOfAttachments()) {
            AccountAwareContactsFolder sourceFolder = access.getFolder(request.getFolderID());
            AccountAwareContactsFolder targetFolder = access.getFolder(destFolderId);
            if (ContactsAccount.DEFAULT_ACCOUNT.getAccountId() == targetFolder.getAccount().getAccountId() && 
                ContactsAccount.DEFAULT_ACCOUNT.getAccountId() == sourceFolder.getAccount().getAccountId()) {
                Optional<String> adjustedSourceFolderId = Attachments.adjustFolderId(sourceFolder.getId());
                int sourceFolderId = parseInt(adjustedSourceFolderId.isPresent() ? adjustedSourceFolderId.get() : sourceFolder.getId());
                Optional<String> adjustedTargetFolderId = Attachments.adjustFolderId(targetFolder.getId());
                int targetFolderId = parseInt(adjustedTargetFolderId.isPresent() ? adjustedTargetFolderId.get() : targetFolder.getId());
                copyAttachments(request.getSession(), sourceFolderId, parseInt(originalContact.getId(true)), targetFolderId, parseInt(newId));
            } else {
                //
                result.addWarnings(Collections.singletonList(OXException.general("Attachments cannot be stored in account " + targetFolder.getAccount().getAccountId())));
            }
        }
        return result;
    }

    /**
     * Copies the attachments of the contact
     *
     * @param session The session
     * @param folderId The source folder identifier
     * @param objectId The source object identifier
     * @param targetFolderId The target folder identifier
     * @param targetObjectId The target object identifier
     */
    private static void copyAttachments(ServerSession session, int folderId, int objectId, int targetFolderId, int targetObjectId) throws OXException {
        /*
         * Copy attachments
         */
        AttachmentBase attachmentBase = Attachments.getInstance();
        SearchIterator<?> iterator = attachmentBase.getAttachments(session, folderId, objectId, Types.CONTACT, session.getContext(), session.getUser(), session.getUserConfiguration()).results();
        try {
            if (iterator.hasNext()) {
                AttachmentMetadataFactory factory = new AttachmentMetadataFactory();
                try {
                    attachmentBase.startTransaction();
                    do {
                        AttachmentMetadata orig = (AttachmentMetadata) iterator.next();
                        AttachmentMetadata copy = factory.newAttachmentMetadata(orig);
                        copy.setFolderId(targetFolderId);
                        copy.setAttachedId(targetObjectId);
                        copy.setId(AttachmentBase.NEW);
                        InputStream file = null;
                        try {
                            file = attachmentBase.getAttachedFile(session, folderId, objectId, Types.CONTACT, orig.getId(), session.getContext(), session.getUser(), session.getUserConfiguration());
                            attachmentBase.attachToObject(copy, file, session, session.getContext(), session.getUser(), session.getUserConfiguration());
                        } finally {
                            Streams.close(file);
                        }
                    } while (iterator.hasNext());
                    attachmentBase.commit();
                } catch (SearchIteratorException e) {
                    try {
                        attachmentBase.rollback();
                    } catch (OXException e1) {
                        LOG.error("Attachment transaction rollback failed", e1);
                    }
                    throw e;
                } catch (OXException e) {
                    try {
                        attachmentBase.rollback();
                    } catch (OXException e1) {
                        LOG.error("Attachment transaction rollback failed", e1);
                    }
                    throw e;
                } finally {
                    try {
                        attachmentBase.finish();
                    } catch (OXException e) {
                        LOG.error("Attachment transaction finish failed", e);
                    }
                }
            }
        } finally {
            SearchIterators.close(iterator);
        }
    }

}
