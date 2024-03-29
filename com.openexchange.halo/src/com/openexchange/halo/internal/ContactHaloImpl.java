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

package com.openexchange.halo.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.halo.HaloContactDataSource;
import com.openexchange.contact.halo.HaloContactQuery;
import com.openexchange.contact.picture.ContactPicture;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ParsedDisplayName;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.contact.helpers.ContactMerger;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.halo.ContactHalo;
import com.openexchange.halo.HaloContactImageSource;
import com.openexchange.halo.HaloExceptionCodes;
import com.openexchange.java.Strings;
import com.openexchange.server.ExceptionOnAbsenceServiceLookup;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link ContactHaloImpl} - The <code>ContactHalo</code> implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ContactHaloImpl implements ContactHalo {

    private final Map<String, HaloContactDataSource> contactDataSources;
    private final List<HaloContactImageSource> imageSources;
    private final Lock imageSourcesLock = new ReentrantLock();
    private final ServiceLookup services;
    private final Cache<ContactHaloQueryKey, HaloContactQuery> queryCache;

    /**
     * Initializes a new {@link ContactHaloImpl}.
     *
     * @param services The service look-up
     */
    public ContactHaloImpl(final ServiceLookup services) {
        super();
        contactDataSources = new ConcurrentHashMap<String, HaloContactDataSource>(8, 0.9f, 1);
        imageSources = new ArrayList<HaloContactImageSource>();
        this.services = ExceptionOnAbsenceServiceLookup.valueOf(services);
        queryCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).build();
    }

    @Override
    public AJAXRequestResult investigate(final String provider, final Contact contact, final AJAXRequestData req, final ServerSession session) throws OXException {
        final HaloContactDataSource dataSource = contactDataSources.get(provider);
        if (dataSource == null) {
            throw HaloExceptionCodes.UNKNOWN_PROVIDER.create(provider);
        }
        if (!dataSource.isAvailable(session)) {
            throw HaloExceptionCodes.UNAVAILABLE_PROVIDER.create(provider);
        }
        if (!(contact.getInternalUserId() > 0) && !contact.containsEmail1() && !contact.containsEmail2() && !contact.containsEmail3()) {
            throw HaloExceptionCodes.INVALID_CONTACT.create();
        }
        return dataSource.investigate(buildQuery(contact, session, true), req, session);
    }

    @Override
    public ContactPicture getPicture(Contact contact, ServerSession session) throws OXException {
        HaloContactQuery contactQuery = buildQuery(contact, session, true);

        for (HaloContactImageSource source : imageSources) {
            if (!source.isAvailable(session)) {
                continue;
            }
            ContactPicture picture = source.getPicture(contactQuery, session);
            IFileHolder fileHolder = picture.getFileHolder();
            if (fileHolder != null) {
                StringBuilder etagBuilder = new StringBuilder();
                etagBuilder.append(source.getClass().getName()).append("://").append(picture.getETag());
                return new ContactPicture(etagBuilder.toString(), fileHolder, picture.getLastModified());
            }
        }
        return null;
    }

    @Override
    public String getPictureETag(Contact contact, ServerSession session) throws OXException {
        HaloContactQuery contactQuery = buildQuery(contact, session, false);
        for (HaloContactImageSource source : imageSources) {
            if (!source.isAvailable(session)) {
                continue;
            }
            String eTag = source.getPictureETag(contactQuery, session);
            if (eTag != null) {
                StringBuilder etagBuilder = new StringBuilder();
                etagBuilder.append(source.getClass().getName()).append("://").append(eTag);
                return etagBuilder.toString();
            }
        }
        return null;
    }

    // Friendly for testing
    HaloContactQuery buildQuery(final Contact contact, final ServerSession session) throws OXException {
        return buildQuery(contact, session, true);
    }

    // Friendly for testing
    HaloContactQuery buildQuery(final Contact contact, final ServerSession session, final boolean withBytes) throws OXException {
        ContactHaloQueryKey key = new ContactHaloQueryKey(session.getSessionID(), contact, withBytes);
        try {
            return queryCache.get(key, new Callable<HaloContactQuery>() {

                @Override
                public HaloContactQuery call() throws Exception {
                    return createQuery(contact, session, withBytes);
                }
            });
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (null != cause && (e.getCause() instanceof OXException)) {
                throw (OXException)cause;
            }
            throw HaloExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    HaloContactQuery createQuery(final Contact contact, final ServerSession session, final boolean withBytes) throws OXException {
        final UserService userService = services.getService(UserService.class);
        final UserPermissionService userPermissionService = services.getService(UserPermissionService.class);
        final IDBasedContactsAccessFactory contactAccessFactory = services.getService(IDBasedContactsAccessFactory.class);
        final HaloContactQuery.Builder contactQueryBuilder = HaloContactQuery.builder();
        final ContactField[] fields = withBytes ? null : new ContactField[] { ContactField.OBJECT_ID, ContactField.LAST_MODIFIED, ContactField.FOLDER_ID };

        Contact resultContact = contact;

        // Look-up associated user...
        User user = null;

        // ... if global address book is accessible
        if (userPermissionService.getUserPermissionBits(session.getUserId(), session.getContext()).isGlobalAddressBookEnabled()) {
            // Prefer look-up by user identifier
            {
                final int userId = resultContact.getInternalUserId();
                if (userId > 0) {
                    user = userService.getUser(userId, session.getContext());
                }
            }

            // Check by object/folder identifier
            if (null == user) {
                if (null != resultContact.getId(true) && null != resultContact.getFolderId(true)) {
                    Contact loaded;
                    IDBasedContactsAccess contactsAccess = contactAccessFactory.createAccess(session);
                    try {
                        contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, fields);
                        loaded = contactsAccess.getContact(new ContactID(resultContact.getFolderId(true), resultContact.getId(true)));
                    } finally {
                        contactsAccess.finish();
                    }
                    return contactQueryBuilder.withContact(loaded).withMergedContacts(Arrays.asList(loaded)).build();
                }
            }

            // Try to find a user with a given eMail address
            if (user == null && resultContact.containsEmail1()) {
                try {
                    user = userService.searchUser(resultContact.getEmail1(), session.getContext(), false);
                } catch (OXException x) {
                    // Don't care. This is all best effort anyway.
                }
            }

            if (user == null && resultContact.containsEmail2()) {
                try {
                    user = userService.searchUser(resultContact.getEmail2(), session.getContext(), false);
                } catch (OXException x) {
                    // Don't care. This is all best effort anyway.
                }
            }

            if (user == null && resultContact.containsEmail3()) {
                try {
                    user = userService.searchUser(resultContact.getEmail3(), session.getContext(), false);
                } catch (OXException x) {
                    // Don't care. This is all best effort anyway.
                }
            }
        } else {
            // Global address book is NOT accessible
            int userId = resultContact.getInternalUserId();
            if (userId > 0 && userId == session.getUserId()) {
                // Requests his own contact picture
                user = userService.getUser(userId, session.getContext());
            }

            // Try to find a user with a given eMail address and verify that matching user is session-associated user
            if (user == null && resultContact.containsEmail1()) {
                try {
                    User match = userService.searchUser(resultContact.getEmail1(), session.getContext(), false);
                    if (match != null && match.getId() == session.getUserId()) {
                        user = match;
                    }
                } catch (OXException x) {
                    // Don't care. This is all best effort anyway.
                }
            }

            if (user == null && resultContact.containsEmail2()) {
                try {
                    User match = userService.searchUser(resultContact.getEmail2(), session.getContext(), false);
                    if (match != null && match.getId() == session.getUserId()) {
                        user = match;
                    }
                } catch (OXException x) {
                    // Don't care. This is all best effort anyway.
                }
            }

            if (user == null && resultContact.containsEmail3()) {
                try {
                    User match = userService.searchUser(resultContact.getEmail3(), session.getContext(), false);
                    if (match != null && match.getId() == session.getUserId()) {
                        user = match;
                    }
                } catch (OXException x) {
                    // Don't care. This is all best effort anyway.
                }
            }
        }

        contactQueryBuilder.withUser(user);
        List<Contact> contactsToMerge = new ArrayList<Contact>(4);
        if (user != null) {
            // Load the associated contact
            IDBasedContactsAccess contactsAccess = contactAccessFactory.createAccess(session);
            try {
                contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, fields);
                resultContact = contactsAccess.getUserAccess().getUserContact(user.getId());
            } finally {
                contactsAccess.finish();
            }
            contactsToMerge.add(resultContact);
        } else if (Strings.isNotEmpty(resultContact.getEmail1())) {
            // Try to find a contact
            ContactsSearchObject contactSearch = new ContactsSearchObject();
            String email = resultContact.getEmail1();
            contactSearch.setEmail1(email);
            contactSearch.setEmail2(email);
            contactSearch.setEmail3(email);
            contactSearch.setOrSearch(true);
            contactSearch.setExactMatch(true);
            List<Contact> foundContacts;
            IDBasedContactsAccess contactsAccess = contactAccessFactory.createAccess(session);
            try {
                contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, fields);
                foundContacts = contactsAccess.searchContacts(contactSearch);
            } finally {
                contactsAccess.finish();
            }
            for (Contact foundContact : foundContacts) {
                if (checkEmails(foundContact, email)) {
                    contactsToMerge.add(foundContact);
                }
            }
        }
        contactQueryBuilder.withMergedContacts(contactsToMerge);

        int contactsToMergeSize = contactsToMerge.size();
        if (1 == contactsToMergeSize) {
            // There is only one
            resultContact = contactsToMerge.get(0);
        } else if (contactsToMergeSize > 0) {
            // Need to merge...
            ContactMerger contactMerger = new ContactMerger(false);
            for (final Contact c : contactsToMerge) {
                resultContact = contactMerger.merge(resultContact, c);
            }
        }

        /*
         * try to decompose display name if no other "name" properties are already set and the contact is "new"
         */
        if (false == resultContact.containsObjectID() &&
            resultContact.containsDisplayName() && Strings.isNotEmpty(resultContact.getDisplayName()) &&
            false == resultContact.containsGivenName() && false == resultContact.containsSurName() &&
            false == resultContact.containsNickname() && false == resultContact.containsCompany()) {
            new ParsedDisplayName(resultContact.getDisplayName()).applyTo(resultContact);
        }
        contactQueryBuilder.withContact(resultContact);
        return contactQueryBuilder.build();
    }

    private boolean checkEmails(Contact c, String email1) {
        if (email1.equalsIgnoreCase(c.getEmail1())) {
            return true;
        }
        if (email1.equalsIgnoreCase(c.getEmail2())) {
            return true;
        }
        if (email1.equalsIgnoreCase(c.getEmail3())) {
            return true;
        }

        return false;
    }

    @Override
    public List<String> getProviders(final ServerSession session) throws OXException {
        final ConfigViewFactory configViews = services.getService(ConfigViewFactory.class);
        final ConfigView view = configViews.getView(session.getUserId(), session.getContextId());
        final List<String> providers = new ArrayList<String>();
        for (final Entry<String, HaloContactDataSource> entry : contactDataSources.entrySet()) {
            if (entry.getValue().isAvailable(session)) {
                final String provider = entry.getKey();
                final ComposedConfigProperty<Boolean> property = view.property(provider, boolean.class);
                Boolean value = property.get();
                if (value == null || value.booleanValue()) {
                    providers.add(provider);
                }
            }
        }
        return providers;
    }

    public void addContactDataSource(final HaloContactDataSource ds) {
        contactDataSources.put(ds.getId(), ds);
    }

    public void removeContactDataSource(final HaloContactDataSource ds) {
        contactDataSources.remove(ds.getId());
    }

    public void addContactImageSource(final HaloContactImageSource is) {
        try {
            imageSourcesLock.lock();
            imageSources.add(is);
            Collections.sort(imageSources, (o1, o2) -> o2.getPriority() - o1.getPriority());
        } finally {
            imageSourcesLock.unlock();
        }
    }

    public void removeContactImageSource(final HaloContactImageSource is) {
        try {
            imageSourcesLock.lock();
            imageSources.remove(is);

        } finally {
            imageSourcesLock.unlock();
        }
    }

    private static class ContactHaloQueryKey {

        private final String sessionID;
        private final Contact contact;
        private final boolean withBytes;
        private final int hashCode;

        /**
         * Initializes a new {@link ContactHaloQueryKey}.
         *
         * @param sessionID The session ID
         * @param contact The contact
         * @param withBytes The withBytes flag
         */
        public ContactHaloQueryKey(String sessionID, Contact contact, boolean withBytes) {
            super();
            this.sessionID = sessionID;
            this.contact = contact;
            this.withBytes = withBytes;
            final int prime = 31;
            int hash = 1;
            hash = prime * hash + ((contact == null) ? 0 : contact.hashCode());
            hash = prime * hash + ((sessionID == null) ? 0 : sessionID.hashCode());
            hash = prime * hash + (withBytes ? 1231 : 1237);
            this.hashCode = hash;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ContactHaloQueryKey other = (ContactHaloQueryKey) obj;
            if (contact == null) {
                if (other.contact != null) {
                    return false;
                }
            } else if (!contact.equals(other.contact)) {
                return false;
            }
            if (sessionID == null) {
                if (other.sessionID != null) {
                    return false;
                }
            } else if (!sessionID.equals(other.sessionID)) {
                return false;
            }
            if (withBytes != other.withBytes) {
                return false;
            }
            return true;
        }

    }

}
