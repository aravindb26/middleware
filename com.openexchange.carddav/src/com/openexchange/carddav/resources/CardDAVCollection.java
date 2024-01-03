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

import static com.openexchange.carddav.Tools.getSupportedCapabilities;
import static com.openexchange.dav.DAVProtocol.protocolException;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tx.TransactionAwares.finishSafe;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.carddav.CarddavProtocol;
import com.openexchange.carddav.GroupwareCarddavFactory;
import com.openexchange.carddav.mixins.BulkRequests;
import com.openexchange.carddav.mixins.MaxImageSize;
import com.openexchange.carddav.mixins.MaxResourceSize;
import com.openexchange.carddav.mixins.SupportedAddressData;
import com.openexchange.carddav.mixins.SupportedReportSet;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.ContactsAccessCapability;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.similarity.ContactSimilarityService;
import com.openexchange.contact.vcard.VCardImport;
import com.openexchange.contact.vcard.VCardParameters;
import com.openexchange.contact.vcard.VCardService;
import com.openexchange.contact.vcard.storage.VCardStorageService;
import com.openexchange.dav.DAVProtocol;
import com.openexchange.dav.DAVUserAgent;
import com.openexchange.dav.PreconditionException;
import com.openexchange.dav.SimilarityException;
import com.openexchange.dav.mixins.CTag;
import com.openexchange.dav.mixins.CurrentUserPrivilegeSet;
import com.openexchange.dav.reports.SyncStatus;
import com.openexchange.dav.resources.FolderCollection;
import com.openexchange.dav.resources.SyncToken;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.results.SequenceResult;
import com.openexchange.groupware.results.UpdatesResult;
import com.openexchange.groupware.search.Order;
import com.openexchange.java.Strings;
import com.openexchange.login.Interface;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;
import com.openexchange.webdav.protocol.WebdavStatusImpl;
import com.openexchange.webdav.protocol.helpers.AbstractResource;

/**
 * {@link CardDAVCollection} - CardDAV collection for contact folders.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class CardDAVCollection extends FolderCollection<Contact> {

    /** A list of basic contact fields that are fetched when getting contacts from the storage */
    protected static final ContactField[] BASIC_FIELDS = {
        ContactField.OBJECT_ID, ContactField.LAST_MODIFIED, ContactField.CREATION_DATE, ContactField.UID,
        ContactField.FILENAME, ContactField.FOLDER_ID, ContactField.VCARD_ID, ContactField.MARK_AS_DISTRIBUTIONLIST
    };

    protected final GroupwareCarddavFactory factory;

    private Boolean isStoreOriginalVCard;

    /**
     * Initializes a new {@link CardDAVCollection}.
     *
     * @param factory The factory
     * @param url The WebDAV path
     * @param folder The underlying folder
     */
    public CardDAVCollection(GroupwareCarddavFactory factory, WebdavPath url, UserizedFolder folder) throws OXException {
        this(factory, url, folder, folder.getOwnPermission(), getSupportedCapabilities(folder));
    }
    
    /**
     * Initializes a new {@link CardDAVCollection}.
     *
     * @param factory The factory
     * @param url The WebDAV path
     * @param folder The underlying folder
     * @param ownPermissions The own permissions on the collection to indicate
     * @param supportedCapabilities The supported contacts access capabilities
     */
    protected CardDAVCollection(GroupwareCarddavFactory factory, WebdavPath url, UserizedFolder folder, Permission ownPermissions, EnumSet<ContactsAccessCapability> supportedCapabilities) throws OXException {
        super(factory, url, folder);
        this.factory = factory;
        includeProperties(
            new CurrentUserPrivilegeSet(ownPermissions),
            new SupportedReportSet(supportedCapabilities),
            new MaxResourceSize(factory),
            new MaxImageSize(factory),
            new SupportedAddressData(),
            new BulkRequests(factory)
        );
        if (supportedCapabilities.contains(ContactsAccessCapability.SYNC)) {
            includeProperties(new CTag(this), new com.openexchange.dav.mixins.SyncToken(this));
        } else if (supportedCapabilities.contains(ContactsAccessCapability.CTAG)) {
            includeProperties(new CTag(this));
        } 
    }
    
    @Override
    public GroupwareCarddavFactory getFactory() {
        return factory;
    }

    @Override
    public String getPushTopic() {
        return null != folder ? "ox:" + Interface.CARDDAV.toString().toLowerCase() + ":" + folder.getID() : null;
    }

    /**
     * Parses and imports all vCards from the supplied input stream.
     *
     * @param inputStream The input stream to parse and import from
     * @return The import results
     */
    public List<BulkImportResult> bulkImport(InputStream inputStream, float maxSimilarity) throws OXException {
        List<BulkImportResult> importResults = new ArrayList<BulkImportResult>();
        VCardService vCardService = factory.requireService(VCardService.class);
        VCardParameters parameters = vCardService.createParameters(factory.getSession()).setKeepOriginalVCard(isStoreOriginalVCard())
            .setImportAttachments(true).setRemoveAttachmentsFromKeptVCard(true);
        SearchIterator<VCardImport> searchIterator = null;
        try {
            searchIterator = vCardService.importVCards(inputStream, parameters);
            while (searchIterator.hasNext()) {
                importResults.add(bulkImport(searchIterator.next(), maxSimilarity));
            }
        } finally {
            SearchIterators.close(searchIterator);
        }
        return importResults;
    }

    private BulkImportResult bulkImport(VCardImport vCardImport, float maxSimilarity) throws OXException {
        BulkImportResult importResult = new BulkImportResult();
        if (null == vCardImport || null == vCardImport.getContact()) {
            importResult.setError(new PreconditionException(DAVProtocol.CARD_NS.getURI(), "valid-address-data", getUrl(), HttpServletResponse.SC_FORBIDDEN));
            return importResult;
        }
        Contact contact = vCardImport.getContact();
        if (Strings.isEmpty(contact.getUid())) {
            importResult.setError(new PreconditionException(DAVProtocol.CARD_NS.getURI(), "no-uid-conflict", getUrl(), HttpServletResponse.SC_FORBIDDEN));
            return importResult;
        }

        importResult.setUid(contact.getUid());
        WebdavPath url = null;
        if (contact.containsFilename() || contact.containsUid()) {
            url = constructPathForChildResource(contact);
            importResult.setHref(url);
        }
        try {
            checkImportError(contact);
            checkMaxResourceSize(vCardImport);
            checkUidConflict(contact.getUid());
            checkSimilarityConflict(maxSimilarity, contact, importResult);
            ContactResource.fromImport(factory, this, url, vCardImport).create();
            if (importResult.getHref() == null) {
                url = constructPathForChildResource(contact);
                importResult.setHref(url);
            }
        } catch (SimilarityException | PreconditionException e) {
            importResult.setError(e);
        } catch (WebdavProtocolException e) {
            importResult.setError(new PreconditionException(DAVProtocol.CARD_NS.getURI(), "valid-address-data", getUrl(), HttpServletResponse.SC_FORBIDDEN));
        }
        return importResult;
    }

    /**
     * Tests if the given contact is too similar to another contact in the same folder
     *
     * @param maxSimilarity The maximum accepted similarity
     * @param contact The contact to test
     * @param result The result object
     * @throws OXException if the ContactSimilarityService is not available or if the contact is too similar to another contact
     */
    private void checkSimilarityConflict(float maxSimilarity, Contact contact, BulkImportResult result) throws OXException {
        if (maxSimilarity > 0) {
            // test if contact is too similar to other contacts
            ContactSimilarityService service = this.factory.getService(ContactSimilarityService.class);
            if (service == null) {
                throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(ContactSimilarityService.class.getSimpleName());
            }
            Contact duplicate = service.getSimilar(factory.getSession(), contact, maxSimilarity);
            if (duplicate != null) {
                result.setUid(duplicate.getUid());
                throw new SimilarityException(constructPathForChildResource(duplicate).toString(), contact.getUid(), HttpServletResponse.SC_CONFLICT);
            }
        }
    }

    /**
     * Checks the vCard import's size against the maximum allowed vCard size.
     *
     * @param vCardImport The vCard import to check
     * @throws PreconditionException <code>(CARDDAV:max-resource-size)</code> if the maximum size is exceeded
     */
    private void checkMaxResourceSize(VCardImport vCardImport) throws PreconditionException {
        long maxSize = factory.getState().getMaxVCardSize();
        if (0 < maxSize) {
            IFileHolder vCard = vCardImport.getVCard();
            if (null != vCard && maxSize < vCard.getLength()) {
                throw new PreconditionException(DAVProtocol.CARD_NS.getURI(), "max-resource-size", getUrl(), HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }

    /**
     * Checks an imported contact regarding a possible import error.
     *
     * @param contact The imported contact to check
     * @throws PreconditionException if there was an import error
     */
    private void checkImportError(Contact contact) throws PreconditionException {
        OXException importError = (OXException) contact.getProperty("com.openexchange.contact.vcard.importError");
        if (null == importError) {
            return;
        }
        if ("VCARD-0006".equals(importError.getErrorCode())) {
            throw new PreconditionException(importError, DAVProtocol.CARD_NS.getURI(), "max-resource-size", getUrl(), HttpServletResponse.SC_FORBIDDEN);
        }
        throw new PreconditionException(importError, DAVProtocol.CARD_NS.getURI(), "valid-address-data", getUrl(), HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Checks for an existing resource in this collection conflicting with a specific UID.
     *
     * @param uid The UID to check
     * @throws OXException If the check fails
     * @throws PreconditionException <code>(CARDDAV:no-uid-conflict)</code> if the UID conflicts with an existing resource
     */
    private void checkUidConflict(String uid) throws OXException, PreconditionException {
        Contact existingContact = getObject(uid);
        if (null != existingContact) {
            throw new PreconditionException(DAVProtocol.CARD_NS.getURI(), "no-uid-conflict", constructPathForChildResource(existingContact), HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Gets a value indicating whether the underlying storage supports storing the original vCard or not.
     *
     * @return <code>true</code> if storing the original vCard is possible, <code>false</code>, otherwise
     */
    public boolean isStoreOriginalVCard() {
        if (null != isStoreOriginalVCard) {
            return isStoreOriginalVCard.booleanValue();
        }
        VCardStorageService vCardStorageService = factory.getVCardStorageService(factory.getSession().getContextId());
        if (null == vCardStorageService) {
            return Boolean.FALSE;
        }
        IDBasedContactsAccess contactsAccess = null;
        try {
            contactsAccess = factory.createContactsAccess();
            isStoreOriginalVCard = Boolean.valueOf(contactsAccess.supports(folder.getID(), ContactField.VCARD_ID));
        } catch (OXException e) {
            LOG.warn("Error checking if storing the vCard ID is supported, assuming \"false\".", e);
            isStoreOriginalVCard = Boolean.FALSE;
        } finally {
            finishSafe(contactsAccess);
        }
        return isStoreOriginalVCard.booleanValue();
    }

    /**
     * Gets a value indicating whether synchronization of distribution lists is enabled for the used client or not.
     *
     * @return <code>true</code> if distribution lists can be synchronized with the used client, <code>false</code>, otherwise
     */
    public boolean isSyncDistributionLists() {
        DAVUserAgent davUserAgent = getUserAgent();
        return davUserAgent.equals(DAVUserAgent.EM_CLIENT) || davUserAgent.equals(DAVUserAgent.OUTLOOK_CALDAV_SYNCHRONIZER);
    }

    /**
     * Gets a list of one or more folders represented by the collection.
     *
     * @return The folder identifiers
     */
    protected List<UserizedFolder> getFolders() {
        return Collections.singletonList(folder);
    }

    /**
     * Gets a list of the identifiers of the folder(s) represented by the collection.
     *
     * @return The folder identifiers
     */
    protected List<String> getFolderIds() {
        return getFolders().stream().map(folder -> folder.getID()).collect(Collectors.toList());
    }

    @Override
    public String getResourceType() throws WebdavProtocolException {
        return super.getResourceType() + CarddavProtocol.ADDRESSBOOK;
    }

    /**
     * Gets a list of contact resources matching the supplied search term.
     *
     * @param term The search term to use
     * @return The contact resources, or an empty list if no contacts were found
     */
    public List<WebdavResource> getFilteredObjects(SearchTerm<?> term) throws WebdavProtocolException {
        /*
         * construct search term & issue search in this collection's folder(s)
         */
        SearchTerm<?> searchTerm;
        if (isSyncDistributionLists()) {
            searchTerm = term;
        } else {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(term).addSearchTerm(getExcludeDistributionlistTerm());
        }
        List<Contact> foundContacts;
        IDBasedContactsAccess contactsAccess = null;
        try {
            contactsAccess = factory.createContactsAccess();
            contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, BASIC_FIELDS);
            foundContacts = contactsAccess.searchContacts(getFolderIds(), searchTerm);
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            finishSafe(contactsAccess);
        }
        /*
         * transform into WebDAV resources and return results
         */
        List<WebdavResource> resources = new ArrayList<>(foundContacts.size());
        for (Contact found : foundContacts) {
            if (isSynchronized(found)) {
                resources.add(createResource(found, constructPathForChildResource(found)));
            }
        }
        return resources;
    }

    @Override
    protected WebdavPath constructPathForChildResource(Contact object) {
        String fileName = object.getFilename();
        if (null == fileName || 0 == fileName.length()) {
            fileName = object.getUid();
        }
        String fileExtension = getFileExtension().toLowerCase();
        if (false == fileExtension.startsWith(".")) {
            fileExtension = "." + fileExtension;
        }
        return constructPathForChildResource(fileName + fileExtension);
    }

    @Override
    protected SyncStatus<WebdavResource> getSyncStatus(SyncToken syncToken) throws OXException {
        SyncStatus<WebdavResource> syncStatus = new SyncStatus<>();
        String[] ignore = syncToken.isInitial() ? new String[] { "deleted" } : null;
        Date since = new Date(syncToken.getTimestamp());
        long timestamp = syncToken.getTimestamp();
        /*
         * get updates result(s) from contacts access
         */
        Map<String, UpdatesResult<Contact>> updatesResults;
        IDBasedContactsAccess contactsAccess = null;
        try {
            contactsAccess = factory.createContactsAccess();
            contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, BASIC_FIELDS);
            contactsAccess.set(ContactsParameters.PARAMETER_IGNORE, ignore);
            updatesResults = contactsAccess.getUpdatedContacts(getFolderIds(), since);
        } finally {
            finishSafe(contactsAccess);
        }
        /*
         * add new /modified contacts to multistatus if synchronized, remember UID, increment total count & take over maximum timestamp
         */
        Set<String> newAndModifiedUids = new HashSet<String>();
        long totalCount = 0L;
        for (UpdatesResult<Contact> updates : updatesResults.values()) {
            if (null != updates.getNewAndModifiedObjects()) {
                for (Contact object : updates.getNewAndModifiedObjects()) {
                    if (isSynchronized(object)) {
                        newAndModifiedUids.add(object.getUid());
                        WebdavResource resource = createResource(object, constructPathForChildResource(object));
                        int status = null != object.getCreationDate() && object.getCreationDate().after(since) ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK;
                        syncStatus.addStatus(new WebdavStatusImpl<WebdavResource>(status, resource.getUrl(), resource));
                    }
                }
            }
            if (0 < updates.getTotalCount()) {
                totalCount += updates.getTotalCount();
            }
            timestamp = Math.max(timestamp, updates.getTimestamp());
        }
        /*
         * add deleted contacts to multistatus unless already contained (due to move operations)
         */
        for (UpdatesResult<Contact> updates : updatesResults.values()) {
            if (null != updates.getDeletedObjects()) {
                for (Contact object : updates.getDeletedObjects()) {
                    if (null != object.getUid() && false == newAndModifiedUids.contains(object.getUid())) {
                        WebdavResource resource = createResource(object, constructPathForChildResource(object));
                        syncStatus.addStatus(new WebdavStatusImpl<WebdavResource>(HttpServletResponse.SC_NOT_FOUND, resource.getUrl(), resource));
                    }
                }
            }
        }
        /*
         * set next sync-token in result, encoding the total number of contacts in folder(s) as 'additional' info to recognize stale deletions
         */
        syncStatus.setToken(new SyncToken(timestamp, Long.toString(totalCount)).toString());
        return syncStatus;
    }

    @Override
    protected Collection<Contact> getObjects() throws OXException {
        /*
         * get contacts, either including or excluding distribution lists
         */
        List<Contact> foundContacts;
        IDBasedContactsAccess contactsAccess = null;
        try {
            contactsAccess = factory.createContactsAccess(BASIC_FIELDS, null, I(factory.getState().getContactLimit()), ContactField.OBJECT_ID, Order.ASCENDING);
            foundContacts = new LinkedList<>();
            for (String folderId : getFolderIds()) {
                foundContacts.addAll(contactsAccess.getContacts(folderId));
            }
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            finishSafe(contactsAccess);
        }
        return filterSynchronizedContacts(foundContacts);
    }

    @Override
    protected Contact getObject(String resourceName) throws OXException {
        SearchTerm<?> searchTerm = getResourceNameTerm(resourceName);
        if (false == isSyncDistributionLists()) {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(searchTerm).addSearchTerm(getExcludeDistributionlistTerm());
        }
        List<Contact> foundContacts;
        IDBasedContactsAccess contactsAccess = null;
        try {
            contactsAccess = factory.createContactsAccess(BASIC_FIELDS, null, I(1), ContactField.OBJECT_ID, Order.ASCENDING);
            foundContacts = contactsAccess.searchContacts(getFolderIds(), searchTerm);
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            finishSafe(contactsAccess);
        }
        List<Contact> contacts = filterSynchronizedContacts(foundContacts);
        return contacts.isEmpty() ? null : contacts.get(0);
    }

    @Override
    protected AbstractResource createResource(Contact object, WebdavPath url) {
        return new ContactResource(factory, this, object, url);
    }

    @Override
    protected String getFileExtension() {
        return ContactResource.EXTENSION_VCF;
    }

    @Override
    public Date getLastModified() throws WebdavProtocolException {
        Map<String, SequenceResult> sequenceNumbers;
        IDBasedContactsAccess contactsAccess = null;
        try {
            contactsAccess = factory.createContactsAccess();
            contactsAccess.set(ContactsParameters.PARAMETER_IGNORE, new String[] {"count"});
            sequenceNumbers = contactsAccess.getSequenceNumbers(getFolderIds());
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            finishSafe(contactsAccess);
        }
        long timestamp = 0L;
        for (SequenceResult sequenceResult : sequenceNumbers.values()) {
            timestamp = Math.max(timestamp, sequenceResult.getTimestamp());
        }
        return new Date(timestamp);
    }

    protected SyncToken fetchSyncToken() throws WebdavProtocolException {
        /*
         * get sequence numbers from contacts access
         */
        Map<String, SequenceResult> sequenceNumbers;
        IDBasedContactsAccess contactsAccess = null;
        try {
            contactsAccess = factory.createContactsAccess();
            sequenceNumbers = contactsAccess.getSequenceNumbers(getFolderIds());
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            finishSafe(contactsAccess);
        }
        /*
         * derive resulting sync token
         */
        long timestamp = 0L;
        long totalCount = 0L;
        for (SequenceResult result : sequenceNumbers.values()) {
            timestamp = Math.max(timestamp, result.getTimestamp());
            if (0 < result.getTotalCount()) {
                totalCount += result.getTotalCount();
            }
        }
        return new SyncToken(timestamp, Long.toString(totalCount));
    }

    @Override
    public String getSyncToken() throws WebdavProtocolException {
        return fetchSyncToken().toString();
    }

    @Override
    public SyncStatus<WebdavResource> getSyncStatus(String token, int limit) throws WebdavProtocolException {
        if (null != token && 0 < token.length()) {
            /*
             * check for overridden sync-token for this client
             */
            String overrrideSyncToken = factory.getOverrideNextSyncToken(folder.getID());
            if (null != overrrideSyncToken && 0 < overrrideSyncToken.length()) {
                factory.setOverrideNextSyncToken(folder.getID(), null);
                token = overrrideSyncToken;
                LOG.debug("Overriding sync token to '{}' for collection '{}'.", token, getUrl());
            }
        }
        return super.getSyncStatus(token, limit);
    }

    @Override
    public String getCTag() throws WebdavProtocolException {
        /*
         * prefer direct CTag if supported
         */
        if (hasSupport(ContactsAccessCapability.CTAG)) {
            IDBasedContactsAccess contactsAccess = null;
            try {
                contactsAccess = factory.createContactsAccess();
                StringBuilder stringBuilder = new StringBuilder();
                for (String folderId : getFolderIds()) {
                    stringBuilder.append(contactsAccess.getCTag(folderId));
                }
                return stringBuilder.toString();
            } catch (OXException e) {
                throw protocolException(getUrl(), e);
            } finally {
                finishSafe(contactsAccess);
            }
        }
        /*
         * check for overridden sync-token for this client
         */
        String overrideSyncToken = factory.getOverrideNextSyncToken(folder.getID());
        if (null != overrideSyncToken && 0 < overrideSyncToken.length()) {
            factory.setOverrideNextSyncToken(folder.getID(), null);
            String value = "http://www.open-xchange.com/ctags/" + folder.getID() + "-" + overrideSyncToken;
            LOG.debug("Overriding CTag property to '{}' for collection '{}'.", value, getUrl());
            return value;
        }
        return super.getCTag();
    }

    /**
     * Gets a value indicating whether the underlying folder(s) have support for a specific contacts access capability.
     * 
     * @param capability The capability to check
     * @return <code>true</code> if the capability is supported, <code>false</code>, otherwise
     */
    protected boolean hasSupport(ContactsAccessCapability capability) {
        return getSupportedCapabilities(getFolders()).contains(capability);
    }

    /**
     * Gets a value indicating whether a contact is synchronized via CardDAV or not.
     *
     * @param contact The contact to check
     * @return <code>true</code> if the contact is synchronized, <code>false</code>, otherwise
     */
    protected boolean isSynchronized(Contact contact) {
        if (contact.getMarkAsDistribtuionlist() && false == isSyncDistributionLists()) {
            return false;
        }
        if (Strings.isEmpty(contact.getUid())) {
            return false;
        }
        return true;
    }

    /**
     * Checks contacts in list and filters for unsynchronized contacts
     *
     * @param contacts The contact list to filder
     * @return The filtered contact list
     */
    protected List<Contact> filterSynchronizedContacts(List<Contact> contacts) {
        List<Contact> filtered = new ArrayList<>(contacts.size());
        for (Contact contact : contacts) {
            if (isSynchronized(contact)) {
                filtered.add(contact);
            }
        }
        return filtered;
    }

    /**
     * Gets a search term to exclude contacts marked as distribution list from the results.
     *
     * @return The search term
     */
    protected static SearchTerm<?> getExcludeDistributionlistTerm() {
        CompositeSearchTerm noDistListTerm = new CompositeSearchTerm(CompositeOperation.OR);
        SingleSearchTerm term1 = new SingleSearchTerm(SingleOperation.EQUALS);
        term1.addOperand(new ContactFieldOperand(ContactField.NUMBER_OF_DISTRIBUTIONLIST));
        term1.addOperand(new ConstantOperand<>(Integer.valueOf(0)));
        noDistListTerm.addSearchTerm(term1);
        SingleSearchTerm term2 = new SingleSearchTerm(SingleOperation.ISNULL);
        term2.addOperand(new ContactFieldOperand(ContactField.NUMBER_OF_DISTRIBUTIONLIST));
        noDistListTerm.addSearchTerm(term2);
        return noDistListTerm;
    }

    /**
     * Gets a search term restricting the contacts to the supplied resource name, matching either the {@link ContactField#UID} or
     * {@link ContactField#FILENAME} properties.
     *
     * @param resourceName The resource name to get the search term for
     * @return The search term
     */
    protected static SearchTerm<?> getResourceNameTerm(String resourceName) {
        CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
        SingleSearchTerm uidTerm = new SingleSearchTerm(SingleOperation.EQUALS);
        uidTerm.addOperand(new ContactFieldOperand(ContactField.UID));
        uidTerm.addOperand(new ConstantOperand<String>(resourceName));
        orTerm.addSearchTerm(uidTerm);
        SingleSearchTerm filenameTerm = new SingleSearchTerm(SingleOperation.EQUALS);
        filenameTerm.addOperand(new ContactFieldOperand(ContactField.FILENAME));
        filenameTerm.addOperand(new ConstantOperand<String>(resourceName));
        orTerm.addSearchTerm(filenameTerm);
        return orTerm;
    }

}
