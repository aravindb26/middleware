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

package com.openexchange.carddav.resources;

import static com.openexchange.dav.DAVProtocol.protocolException;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.tx.TransactionAwares.finishSafe;
import static com.openexchange.tx.TransactionAwares.rollbackSafe;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.jdom2.Element;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.carddav.CardDAVProperty;
import com.openexchange.carddav.GroupwareCarddavFactory;
import com.openexchange.carddav.Tools;
import com.openexchange.carddav.photos.PhotoUtils;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.ContactIDUtil;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.ContactsAccessCapability;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDMangling;
import com.openexchange.contact.storage.ContactTombstoneStorage;
import com.openexchange.contact.vcard.DistributionListMode;
import com.openexchange.contact.vcard.VCardExport;
import com.openexchange.contact.vcard.VCardImport;
import com.openexchange.contact.vcard.VCardParameters;
import com.openexchange.contact.vcard.VCardService;
import com.openexchange.contact.vcard.storage.VCardStorageService;
import com.openexchange.dav.DAVProtocol;
import com.openexchange.dav.DAVUserAgent;
import com.openexchange.dav.PreconditionException;
import com.openexchange.dav.Privilege;
import com.openexchange.dav.resources.CommonResource;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.tools.mappings.MappedIncorrectString;
import com.openexchange.groupware.tools.mappings.MappedTruncation;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.policy.retry.NoTimeoutRetryPolicy;
import com.openexchange.policy.retry.RetryPolicy;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.webdav.WebDAVRequestContext;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProperty;
import com.openexchange.webdav.protocol.WebdavProtocolException;

/**
 * {@link ContactResource}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactResource extends CommonResource<Contact> {

    /**
     * The file extension used for vCard resources.
     */
    public static final String EXTENSION_VCF = ".vcf";

    /**
     * The content type used for vCard resources.
     */
    public static final String CONTENT_TYPE = "text/vcard; charset=utf-8";

    private static final int MAX_RETRIES = 3;

    /** The contact fields that are considered when resolving distribution list references */
    private static final ContactField CONTACT_FIELDS_TO_LOAD[] = { ContactField.OBJECT_ID, ContactField.FOLDER_ID, ContactField.UID, ContactField.DISPLAY_NAME, ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3, ContactField.MARK_AS_DISTRIBUTIONLIST, ContactField.DISTRIBUTIONLIST };

    private final GroupwareCarddavFactory factory;
    private final CardDAVCollection parent;
    private VCardImport vCardImport;
    private VCardExport vCardExport;

    /**
     * Initializes a new {@link ContactResource}.
     *
     * @param factory The CardDAV factory
     * @param parent The parent folder collection
     * @param object An existing groupware object represented by this resource, or <code>null</code> if a placeholder resource should be created
     * @param url The resource url
     */
    public ContactResource(GroupwareCarddavFactory factory, CardDAVCollection parent, Contact object, WebdavPath url) {
        super(parent, object, url);
        this.factory = factory;
        this.parent = parent;
    }

    /**
     * Creates a new contact resource from a vCard import.
     *
     * @param factory The CardDAV factory
     * @param parent The parent folder collection
     * @param url The target resource URL
     * @param vCardImport The vCard import to apply
     * @return The new contact resource
     */
    static ContactResource fromImport(GroupwareCarddavFactory factory, CardDAVCollection parent, WebdavPath url, VCardImport vCardImport) {
        ContactResource contactResource = new ContactResource(factory, parent, null, url);
        contactResource.vCardImport = vCardImport;
        return contactResource;
    }

    @Override
    protected String getFileExtension() {
        return EXTENSION_VCF;
    }

    @Override
    public String getContentType() throws WebdavProtocolException {
        return CONTENT_TYPE;
    }

    @Override
    public Long getLength() throws WebdavProtocolException {
        if (!exists()) {
            return L(0L);
        }
        VCardExport vCardResource = getVCardResource(false);
        if (null != vCardResource && null != vCardResource.getVCard()) {
            return Long.valueOf(vCardResource.getVCard().getLength());
        }
        return L(0L);
    }

    @Override
    public InputStream getBody() throws WebdavProtocolException {
        if (!exists()) {
            throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
        }
        VCardExport vCardResource = getVCardResource(true);
        if (null == vCardResource) {
            throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
        }
        try {
            return vCardResource.getClosingStream();
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        }
    }

    @Override
    public void save() throws WebdavProtocolException {
        if (false == parent.supports(ContactsAccessCapability.READ_WRITE.getName())) {
            parent.requirePrivilege(Privilege.WRITE);
        }
        IFileHolder vCardFileHolder = null;
        String vCardID = null;
        String previousVCardID = null;
        boolean saved = false;
        Session session = factory.getSession();
        IDBasedContactsAccess contactsAccess = null;
        try {
            if (!exists()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_CONFLICT);
            } else if (null == vCardImport || null == vCardImport.getContact()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
            }
            /*
             * store original vCard if possible
             */
            previousVCardID = object.getVCardId();
            Contact contact = vCardImport.getContact();
            vCardFileHolder = vCardImport.getVCard();
            vCardID = storeVCard(session.getContextId(), vCardFileHolder);
            contact.setVCardId(vCardID);
            ContactID contactID = ContactIDUtil.createContactID(object.getFolderId(), object.getId());
            /*
             * update contact, trying again in case of recoverable errors
             */
            contactsAccess = factory.createContactsAccess();
            restoreDistributionListReferences(contactsAccess, contact, Integer.toString(contact.getParentFolderID())); //TODO: abstract from numerical folder ids
            RetryPolicy policy = new NoTimeoutRetryPolicy(MAX_RETRIES);
            do {
                try {
                    contactsAccess.updateContact(contactID, contact, contact.getLastModified().getTime());
                    LOG.debug("{}: saved.", getUrl());
                    saved = true;
                } catch (OXException e) {
                    if (false == handle(e)) {
                        break;
                    }
                }
            } while (policy.isRetryAllowed() && false == saved);
            /*
             * process attachments
             */
            if (saved) {
                handleAttachments(object, contact);
            }
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            Streams.close(vCardFileHolder);
            closeVCardImport();
            if (saved) {
                deleteVCard(session.getContextId(), previousVCardID);
            } else {
                rollbackSafe(contactsAccess);
                if (null != vCardID) {
                    deleteVCard(session.getContextId(), vCardID);
                }
            }
            finishSafe(contactsAccess);
        }
    }

    @Override
    public void delete() throws WebdavProtocolException {
        if (false == parent.supports(ContactsAccessCapability.READ_WRITE.getName())) {
            parent.requirePrivilege(Privilege.UNBIND);
        }
        boolean deleted = false;
        Contact object = this.object;
        String vCardID = null == object ? null : object.getVCardId();
        Session session = factory.getSession();
        IDBasedContactsAccess contactsAccess = null;
        try {
            if (false == exists()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
            }
            /*
             * delete contact, trying again in case of recoverable errors
             */
            if (null != object) {
                ContactID contactID = ContactIDUtil.createContactID(object.getFolderId(), object.getId());
                contactsAccess = factory.createContactsAccess();
                RetryPolicy policy = new NoTimeoutRetryPolicy(MAX_RETRIES);
                do {
                    try {
                        contactsAccess.deleteContact(contactID, object.getLastModified().getTime());
                        LOG.debug("{}: deleted.", getUrl());
                        deleted = true;
                        this.object = null;
                    } catch (OXException e) {
                        if (false == handle(e)) {
                            break;
                        }
                    }
                } while (policy.isRetryAllowed() && false == deleted);
            }
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            if (deleted) {
                if (null != vCardID) {
                    deleteVCard(session.getContextId(), vCardID);
                }
                rollbackSafe(contactsAccess);
            }
            finishSafe(contactsAccess);
        }
    }

    @Override
    public void create() throws WebdavProtocolException {
        if (false == parent.supports(ContactsAccessCapability.READ_WRITE.getName())) {
            parent.requirePrivilege(Privilege.BIND);
        }
        String vCardID = null;
        IFileHolder vCardFileHolder = null;
        boolean created = false;
        Session session = factory.getSession();
        IDBasedContactsAccess contactsAccess = null;
        try {
            if (exists()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_CONFLICT);
            } else if (null == vCardImport || null == vCardImport.getContact()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
            }
            /*
             * import vCard as new contact
             */
            contactsAccess = factory.createContactsAccess();
            Contact contact = vCardImport.getContact();
            if (null != url) {
                String extractedUID = Tools.extractUID(url);
                if (null != extractedUID && false == extractedUID.equals(contact.getUid())) {
                    /*
                     * Always extract the UID from the URL; the Addressbook client in MacOS 10.6 uses different UIDs in
                     * the WebDAV path and the UID field in the vCard, so we need to store this UID in the contact
                     * resource, too, to recognize later updates on the resource.
                     */
                    LOG.debug("{}: Storing WebDAV resource name in filename.", getUrl());
                    contact.setFilename(extractedUID);
                }
            }
            contact.setContextId(session.getContextId());
            String parentFolderID = parent.getFolder().getID();
            if (DAVUserAgent.IOS.equals(getUserAgent()) && false == parentFolderID.equals(factory.getState().getDefaultFolder().getID())) {
                /*
                 * for iOS, set initial parent to the default contacts folder & insert tombstone record for automatic cleanup during next sync
                 */
                ContactTombstoneStorage tombstoneStorage = factory.getOptionalService(ContactTombstoneStorage.class);
                if (null != tombstoneStorage && tombstoneStorage.supports(session, parentFolderID)) {
                    LOG.debug("{}: Re-routing contact creation to default folder for iOS client, inserting tombstone in targeted folder for client recovery.", getUrl());
                    tombstoneStorage.insertTombstone(session, IDMangling.getRelativeFolderId(parentFolderID), contact);
                }
                parentFolderID = factory.getState().getDefaultFolder().getID();
            }
            if (contact.getMarkAsDistribtuionlist() && false == parent.isSyncDistributionLists()) {
                /*
                 * insert & delete not supported contact group (next sync cleans up the client)
                 */
                try {
                    LOG.warn("{}: contact groups not supported, performing immediate deletion of this resource.", this.getUrl());
                    contact.removeDistributionLists();
                    contact.removeNumberOfDistributionLists();
                    contactsAccess.createContact(parentFolderID, contact);
                    contactsAccess.deleteContact(ContactIDUtil.createContactID(contact.getFolderId(), contact.getId()), contact.getLastModified().getTime());
                } catch (OXException e) {
                    throw protocolException(getUrl(), e);
                }
                return;
            }

            restoreDistributionListReferences(contactsAccess, contact, parentFolderID);
            /*
             * store original vCard if possible
             */
            vCardFileHolder = vCardImport.getVCard();
            vCardID = storeVCard(session.getContextId(), vCardFileHolder);
            contact.setVCardId(vCardID);
            /*
             * save contact, trying again in case of recoverable errors
             */
            object = contact;
            RetryPolicy policy = new NoTimeoutRetryPolicy(MAX_RETRIES);
            do {
                try {
                    contactsAccess.createContact(parentFolderID, object);
                    LOG.debug("{}: created.", getUrl());
                    created = true;
                } catch (OXException e) {
                    if (false == handle(e)) {
                        break;
                    }
                }
            } while (policy.isRetryAllowed() && false == created);
            /*
             * process indicated attachments
             */
            if (created) {
                handleAttachments(null, object);
            }
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            Streams.close(vCardFileHolder);
            closeVCardImport();
            if (false == created) {
                if (null != vCardID) {
                    deleteVCard(session.getContextId(), vCardID);
                }
                rollbackSafe(contactsAccess);
            }
            finishSafe(contactsAccess);
        }
    }

    private void restoreDistributionListReferences(IDBasedContactsAccess idBasedContactsAccess, Contact contact, String parentFolderID) throws OXException {
        if (contact == null || !contact.getMarkAsDistribtuionlist()) {
            return;
        }
        // try to search for known contacts
        SearchTerm<?> searchTerm = createSearchTerm(contact.getDistributionList(), contact.getUid());
        if (null == searchTerm) {
            return;
        }
        SearchIterator<Contact> contacts = null;
        try {
            idBasedContactsAccess.set(IDBasedContactsAccess.PARAMETER_FIELDS, CONTACT_FIELDS_TO_LOAD);
            List<Contact> dbContacts = idBasedContactsAccess.searchContacts(Collections.singletonList(parentFolderID), searchTerm);
            Optional<Contact> originalContact = dbContacts.stream().filter(x -> x.getUid().equals(contact.getUid())).findFirst();
            List<DistributionListEntryObject> prepared = restoreDistributionListReferences0(originalContact, Arrays.asList(contact.getDistributionList()), dbContacts);
            contact.setDistributionList(prepared.stream().toArray(DistributionListEntryObject[]::new));
        } finally {
            SearchIterators.close(contacts);
        }
    }

    private SearchTerm<?> createSearchTerm(DistributionListEntryObject[] newDistList, String originalContactUID) {
        List<SingleSearchTerm> uidTerms = new ArrayList<SingleSearchTerm>();
        if (Strings.isNotEmpty(originalContactUID)) {
            uidTerms.add(new SingleSearchTerm(SingleOperation.EQUALS)
                .addOperand(new ContactFieldOperand(ContactField.UID)).addOperand(new ConstantOperand<String>(originalContactUID)));
        }
        if (null != newDistList) {
            for (DistributionListEntryObject entry : newDistList) {
                if (Strings.isNotEmpty(entry.getContactUid())) {
                    uidTerms.add(new SingleSearchTerm(SingleOperation.EQUALS)
                        .addOperand(new ContactFieldOperand(ContactField.UID)).addOperand(new ConstantOperand<String>(entry.getContactUid())));
                }
            }
        }
        if (uidTerms.isEmpty()) {
            return null;
        }
        if (1 == uidTerms.size()) {
            return uidTerms.get(0);
        }
        CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
        for (SingleSearchTerm uidTerm : uidTerms) {
            orTerm.addSearchTerm(uidTerm);
        }
        return orTerm;
    }

    private List<DistributionListEntryObject> restoreDistributionListReferences0(Optional<Contact> originalContact, List<DistributionListEntryObject> provided, List<Contact> dbContacts) {
        for (DistributionListEntryObject entry : provided) {
            if (entry.containsEntryID() && entry.containsFolderld() && entry.containsEmailfield()) { //skip the appropriate set ones
                continue;
            }
            Optional<Contact> findFirst = dbContacts.stream().filter(x -> x.getUid().equals(entry.getContactUid())).findFirst();
            if (findFirst.isPresent()) {
                Contact dbContact = findFirst.get();
                if (!entry.containsDisplayname()) {
                    entry.setDisplayname(dbContact.getDisplayName());
                }
                if (entry.containsEmailaddress() && Strings.isNotEmpty(entry.getEmailaddress())) {
                    String providedMailAddress = entry.getEmailaddress();
                    if (providedMailAddress.equals(dbContact.getEmail1())) {
                        entry.setEmailfield(DistributionListEntryObject.EMAILFIELD1);
                    } else if (providedMailAddress.equals(dbContact.getEmail2())) {
                        entry.setEmailfield(DistributionListEntryObject.EMAILFIELD2);
                    } else if (providedMailAddress.equals(dbContact.getEmail3())) {
                        entry.setEmailfield(DistributionListEntryObject.EMAILFIELD3);
                    }
                } else {
                    /*
                     * insert reference to first non-empty email address of contact as fallback
                     */
                    try {
                        if (Strings.isNotEmpty(dbContact.getEmail1())) {
                            entry.setEmailfield(DistributionListEntryObject.EMAILFIELD1);
                            entry.setEmailaddress(dbContact.getEmail1(), false);
                        } else if (Strings.isNotEmpty(dbContact.getEmail2())) {
                            entry.setEmailfield(DistributionListEntryObject.EMAILFIELD2);
                            entry.setEmailaddress(dbContact.getEmail2(), false);
                        } else if (Strings.isNotEmpty(dbContact.getEmail3())) {
                            entry.setEmailfield(DistributionListEntryObject.EMAILFIELD3);
                            entry.setEmailaddress(dbContact.getEmail3(), false);
                        }
                    } catch (OXException e) {
                        LOG.warn("Unexpected error assigning default mail address from reference contact", e);
                    }
                }
                if (!entry.containsEntryID() || entry.getEntryID() == null) {
                    entry.setEntryID(dbContact.getId(true));
                }
                if (!entry.containsFolderld() || entry.getFolderID() == null) {
                    entry.setFolderID(dbContact.getFolderId(true));
                }
            } else if (originalContact.isPresent()) {
                // try to match dleo from original list
                DistributionListEntryObject[] origDistList = originalContact.get().getDistributionList();
                if (origDistList == null) {
                    continue;
                }
                // try matching uid...
                Optional<DistributionListEntryObject> findOriginalDistListEntry = Arrays.asList(origDistList).stream().filter(x -> x.getContactUid() != null && x.getContactUid().equals(entry.getContactUid())).findFirst();
                if (false == findOriginalDistListEntry.isPresent()) {
                    // try match by mail als last resort
                    findOriginalDistListEntry = Arrays.asList(origDistList).stream().filter(x -> x.getEmailaddress() != null && x.getEmailaddress().equals(entry.getEmailaddress())).findFirst();
                }
                if (findOriginalDistListEntry.isPresent()) { // internal user or one-off => set appropriate to not overwrite NULL values
                    DistributionListEntryObject relatedDbEntry = findOriginalDistListEntry.get();
                    if (relatedDbEntry.containsFolderld()) {
                        entry.setFolderID(relatedDbEntry.getFolderID());
                    }
                    if (relatedDbEntry.containsEntryID()) {
                        entry.setEntryID(relatedDbEntry.getEntryID());
                    }
                    if (relatedDbEntry.containsSortName()) {
                        entry.setSortName(relatedDbEntry.getSortName());
                    }
                    if (relatedDbEntry.containsEmailfield()) {
                        entry.setEmailfield(relatedDbEntry.getEmailfield());
                    }
                    if (relatedDbEntry.containsContactUid()) {
                        entry.setContactUid(relatedDbEntry.getContactUid());
                    }
                    continue;
                }
                continue;
            }
        }
        return provided;
    }

    @Override
    protected void deserialize(InputStream inputStream) throws OXException, IOException {
        VCardService vCardService = factory.requireService(VCardService.class);
        VCardParameters parameters = vCardService.createParameters(factory.getSession()).setKeepOriginalVCard(parent.isStoreOriginalVCard()).setImportAttachments(true).setRemoveAttachmentsFromKeptVCard(true).setDistributionListMode(getDistributionListMode());
        if (!exists()) {
            /*
             * import vCard as new contact
             */
            vCardImport = vCardService.importVCard(inputStream, null, parameters);
            if (null == vCardImport || null == vCardImport.getContact()) {
                throw new PreconditionException(DAVProtocol.CARD_NS.getURI(), "valid-address-data", getUrl(), HttpServletResponse.SC_FORBIDDEN);
            }
        } else {
            /*
             * import vCard and merge with existing contact, ensuring that some important properties don't change
             */
            Contact contact;
            IDBasedContactsAccess contactsAccess = null;
            try {
                contactsAccess = factory.createContactsAccess();
                contact = contactsAccess.getContact(ContactIDUtil.createContactID(object.getFolderId(), object.getId()));
            } finally {
                finishSafe(contactsAccess);
            }
            if (null != contact) {
                String uid = contact.getUid();
                int parentFolderID = contact.getParentFolderID();
                int contextID = contact.getContextId();
                Date lastModified = contact.getLastModified();
                int objectID = contact.getObjectID();
                String vCardID = contact.getVCardId();
                contact.setProperty("com.openexchange.contact.vcard.photo.uri", PhotoUtils.buildURI(factory.getServiceSafe(ConfigViewFactory.class), getHostData(), contact));
                contact.setProperty("com.openexchange.contact.vcard.photo.contentType", contact.getImageContentType());
                vCardImport = factory.requireService(VCardService.class).importVCard(inputStream, contact, parameters);
                if (null == vCardImport || null == vCardImport.getContact()) {
                    throw new PreconditionException(DAVProtocol.CARD_NS.getURI(), "valid-address-data", getUrl(), HttpServletResponse.SC_FORBIDDEN);
                }
                vCardImport.getContact().setUid(uid);
                vCardImport.getContact().setParentFolderID(parentFolderID);
                vCardImport.getContact().setContextId(contextID);
                vCardImport.getContact().setLastModified(lastModified);
                vCardImport.getContact().setObjectID(objectID);
                vCardImport.getContact().setVCardId(vCardID);
            }
        }
    }

    @Override
    protected WebdavProperty internalGetProperty(WebdavProperty property) throws WebdavProtocolException {
        if (exists() && DAVProtocol.CARD_NS.getURI().equals(property.getNamespace()) && "address-data".equals(property.getName())) {
            String value;
            Set<String> propertyNames = extractRequestedProperties(property);
            if (null != propertyNames && 0 < propertyNames.size()) {
                try (VCardExport vCardExport = generateVCardResource(propertyNames); InputStream inputStream = vCardExport.getClosingStream()) {
                    value = Streams.stream2string(inputStream, Charsets.UTF_8_NAME);
                } catch (IOException | OXException e) {
                    throw protocolException(getUrl(), e);
                }
            } else {
                try (InputStream inputStream = getBody()) {
                    value = Streams.stream2string(inputStream, Charsets.UTF_8_NAME);
                } catch (IOException e) {
                    throw protocolException(getUrl(), e);
                }
            }
            WebdavProperty result = new WebdavProperty(property.getNamespace(), property.getName());
            result.setXML(true);
            result.setValue("<![CDATA[" + value + "]]>");
            return result;
        }
        return null;
    }

    /**
     * Silently closes the body file holder if set.
     */
    private void closeVCardImport() {
        if (null != vCardImport) {
            Streams.close(vCardImport);
            vCardImport = null;
        }
    }

    /**
     * Tries to handle an exception.
     *
     * @param e the exception to handle
     * @return <code>true</code>, if the operation should be retried, <code>false</code>, otherwise.
     */
    private boolean handle(OXException e) throws WebdavProtocolException {
        LOG.debug("Trying to handle exception: {}", e.getMessage(), e);
        if (Tools.isImageProblem(e)) {
            /*
             * image problem, handle by create without image
             */
            if (object != null) {
                LOG.warn("{}: {} - removing image and trying again.", getUrl(), e.getMessage());
                object.removeImage1();
            }
            return true;
        } else if (Tools.isDataTruncation(e)) {
            /*
             * handle by trimming truncated fields
             */
            if (trimTruncatedAttributes(e)) {
                LOG.warn("{}: {} - trimming fields and trying again.", getUrl(), e.getMessage());
                return true;
            }
        } else if (Tools.isIncorrectString(e)) {
            /*
             * handle by removing incorrect characters
             */
            if (replaceIncorrectStrings(e, "")) {
                LOG.warn("{}: {} - removing incorrect characters and trying again.", getUrl(), e.getMessage());
                return true;
            }
        } else if (Category.CATEGORY_PERMISSION_DENIED.equals(e.getCategory())) {
            /*
             * handle by overriding sync-token
             */
            LOG.debug("{}: {}", this.getUrl(), e.getMessage());
            LOG.debug("{}: overriding next sync token for client recovery.", this.getUrl());
            factory.setOverrideNextSyncToken(parent.getFolder().getID(), "0");
        } else if (Category.CATEGORY_CONFLICT.equals(e.getCategory())) {
            throw protocolException(getUrl(), e, HttpServletResponse.SC_CONFLICT);
        } else if (Category.CATEGORY_SERVICE_DOWN.equals(e.getCategory())) {
            /*
             * throw appropriate protocol exception
             */
            throw protocolException(getUrl(), e, HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            throw protocolException(getUrl(), e);
        }
        return false;
    }

    private boolean trimTruncatedAttributes(OXException e) {
        try {
            return MappedTruncation.truncate(e.getProblematics(), object);
        } catch (OXException x) {
            LOG.warn("{}: error trying to handle truncated attributes", getUrl(), x);
            return false;
        }
    }

    private boolean replaceIncorrectStrings(OXException e, String replacement) {
        try {
            return MappedIncorrectString.replace(e.getProblematics(), object, replacement);
        } catch (OXException x) {
            LOG.warn("{}: error trying to handle truncated attributes", getUrl(), x);
            return false;
        }
    }

    private VCardExport getVCardResource(boolean reset) throws WebdavProtocolException {
        VCardExport vCardResource = this.vCardExport;
        if (null == vCardResource) {
            try {
                vCardResource = generateVCardResource(null);
            } catch (OXException e) {
                throw protocolException(getUrl(), e);
            }
        }
        this.vCardExport = reset ? null : vCardResource;
        return vCardResource;
    }

    private VCardExport generateVCardResource(Set<String> propertyNames) throws OXException {
        /*
         * determine required contact fields for the export
         */
        VCardService vCardService = factory.requireService(VCardService.class);
        VCardParameters parameters = vCardService.createParameters(factory.getSession()).setDistributionListMode(getDistributionListMode());
        ContactField[] contactFields;
        if (null != propertyNames && 0 < propertyNames.size()) {
            parameters.setPropertyNames(propertyNames);
            contactFields = vCardService.getContactFields(propertyNames);
        } else {
            contactFields = null;
        }
        /*
         * load required contact data from storage
         */
        Contact contact;
        IDBasedContactsAccess contactsAccess = null;
        try {
            contactsAccess = factory.createContactsAccess();
            contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, contactFields);
            contact = contactsAccess.getContact(ContactIDUtil.createContactID(object.getFolderId(true), object.getId(true)));
        } finally {
            finishSafe(contactsAccess);
        }
        applyAttachments(contact);
        if (isExportPhotoAsURI() && 0 < contact.getNumberOfImages()) {
            contact.setProperty("com.openexchange.contact.vcard.photo.uri", PhotoUtils.buildURI(factory.getServiceSafe(ConfigViewFactory.class), getHostData(), contact));
            contact.setProperty("com.openexchange.contact.vcard.photo.contentType", contact.getImageContentType());
        }
        /*
         * export contact data & return resulting vCard stream
         */
        InputStream originalVCard = null;
        try {
            Session session = factory.getSession();
            originalVCard = optVCard(session.getContextId(), object.getVCardId());
            return vCardService.exportContact(contact, originalVCard, parameters);
        } finally {
            Streams.close(originalVCard);
        }
    }

    /**
     * Stores a vCard.
     *
     * @param contextID The context identifier
     * @param fileHolder The file holder carrying the vCard data, or <code>null</code> to do nothing
     * @return The identifier of the stored vCard
     */
    private String storeVCard(int contextID, IFileHolder fileHolder) {
        if (null != fileHolder) {
            VCardStorageService vCardStorageService = factory.getVCardStorageService(contextID);
            if (null != vCardStorageService) {
                try (InputStream inputStream = fileHolder.getStream()) {
                    String vCardID = vCardStorageService.saveVCard(inputStream, contextID);
                    LOG.debug("{}: saved vCard in '{}'.", getUrl(), vCardID);
                    return vCardID;
                } catch (OXException | IOException e) {
                    LOG.warn("Error storing vCard in context {}.", I(contextID), e);
                }
            }
        }
        return null;
    }

    /**
     * Optionally gets the input stream for a stored vCard.
     *
     * @param contextID The context identifier
     * @param vCardID The identifier of the vCard to get, or <code>null</code> to do nothing
     * @return The vCard, or <code>null</code> if not available
     */
    private InputStream optVCard(int contextID, String vCardID) {
        if (null != vCardID) {
            VCardStorageService vCardStorage = factory.getVCardStorageService(contextID);
            if (null != vCardStorage) {
                try {
                    return vCardStorage.getVCard(vCardID, contextID);
                } catch (OXException e) {
                    LOG.warn("Error retrieving vCard with id {} in context {} from storage.", vCardID, I(contextID), e);
                }
            }
        }
        return null;
    }

    /**
     * Deletes a vCard silently.
     *
     * @param contextID The context identifier
     * @param vCardID The identifier of the vCard to delete, or <code>null</code> to do nothing
     * @return <code>true</code> if a vCard file actually has been deleted, <code>false</code>, otherwise
     */
    private boolean deleteVCard(int contextID, String vCardID) {
        if (null != vCardID) {
            VCardStorageService vCardStorage = factory.getVCardStorageService(contextID);
            if (null != vCardStorage) {
                try {
                    return vCardStorage.deleteVCard(vCardID, contextID);
                } catch (OXException e) {
                    if ("FLS-0017".equals(e.getErrorCode())) {
                        LOG.debug("vCard file with id {} in context {} no longer found in storage.", vCardID, I(contextID), e);
                    } else {
                        LOG.warn("Error while deleting vCard with id {} in context {} from storage.", vCardID, I(contextID), e);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Extracts a set of vCard property names as specified in the child nodes of the supplied <code>CARDDAV:address-data</code> property.
     *
     * @param addressDataProperty The <code>CARDDAV:address-data</code> property to get the requested vCard properties from
     * @return The requested vCard properties, or <code>null</code> if no specific properties are requested
     */
    private static Set<String> extractRequestedProperties(WebdavProperty addressDataProperty) {
        List<Element> childElements = addressDataProperty.getChildren();
        if (null != childElements && 0 < childElements.size()) {
            Set<String> propertyNames = new HashSet<String>(childElements.size());
            for (Element childElement : childElements) {
                if (DAVProtocol.CARD_NS.equals(childElement.getNamespace()) && "prop".equals(childElement.getName())) {
                    String name = childElement.getAttributeValue("name");
                    if (null != name) {
                        propertyNames.add(name);
                    }
                }
            }
            return propertyNames;
        }
        return null;
    }

    /**
     * Gets the distribution list mode to use for serialization to vCards, based on the client's user agent.
     *
     * @return The distribution list mode
     */
    private DistributionListMode getDistributionListMode() {
        return DistributionListMode.ADDRESSBOOKSERVER; // only mode as of now
    }

    /**
     * Gets a value indicating whether the value of the <code>PHOTO</code>-property in vCards should be exported as URI or not, based on
     * the <code>Prefer</code>-header sent by the client.
     *
     * @return <code>true</code> if the photo should be exported as URI, <code>false</code>, otherwise
     */
    private boolean isExportPhotoAsURI() {
        /*
         * evaluate "Prefer" header first
         */
        WebDAVRequestContext requestContext = DAVProtocol.getRequestContext();
        if (null != requestContext) {
            Enumeration<?> preferHeaders = requestContext.getHeaders("Prefer");
            if (null != preferHeaders && preferHeaders.hasMoreElements()) {
                do {
                    String value = String.valueOf(preferHeaders.nextElement());
                    if ("photo=uri".equalsIgnoreCase(value)) {
                        return true;
                    }
                    if ("photo=binary".equalsIgnoreCase(value)) {
                        return false;
                    }
                } while (preferHeaders.hasMoreElements());
            }
        }
        /*
         * default to configuration
         */
        try {
            return "uri".equals(factory.getServiceSafe(LeanConfigurationService.class).getProperty(CardDAVProperty.PREFERRED_PHOTO_ENCODING));
        } catch (OXException e) {
            LOG.warn("Error getting \"{}\", falling back 'binary'.", CardDAVProperty.PREFERRED_PHOTO_ENCODING, e);
        }
        return false;
    }

}
