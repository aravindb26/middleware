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

package com.openexchange.imap.cache;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.caching.CacheService;
import com.openexchange.caching.events.CacheEvent;
import com.openexchange.caching.events.CacheEventService;
import com.openexchange.exception.OXException;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.imap.config.IIMAPProperties;
import com.openexchange.imap.namespace.Namespace;
import com.openexchange.imap.namespace.Namespaces;
import com.openexchange.imap.services.Services;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.session.Session;
import com.openexchange.session.UserAndContext;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.ListInfo;

/**
 * {@link ListLsubCache} - A user-bound cache for LIST/LSUB entries.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ListLsubCache {

    /**
     * The logger
     */
    protected static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ListLsubCache.class);

    private static final ListLsubCache INSTANCE = new ListLsubCache();

    /**
     * The region name.
     */
    public static final String REGION = "ListLsubCache";

    /** The default timeout for LIST/LSUB cache (6 minutes) */
    public static final long DEFAULT_TIMEOUT = 360000;

    private static final String INBOX = "INBOX";

    /** The cache */
    static final Cache<UserAndContext, Cache<Integer, ListLsubCollection>> CACHE = CacheBuilder.newBuilder().expireAfterAccess(java.time.Duration.ofHours(1)).build();

    /**
     * No instance
     */
    private ListLsubCache() {
        super();
    }

    /**
     * Drop caches for given session's user.
     *
     * @param session The session providing user information
     */
    public static void dropFor(Session session) {
        if (null != session) {
            dropFor(session.getUserId(), session.getContextId());
        }
    }

    /**
     * Drop caches for given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public static void dropFor(int userId, int contextId) {
        dropFor(userId, contextId, true, false);
    }

    /**
     * Drop caches for given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param notify Whether to notify
     * @param enforceNewConnection Whether a new connection is supposed to be used for cache initialization on subsequent calls
     */
    public static void dropFor(int userId, int contextId, boolean notify, boolean enforceNewConnection) {
        if (enforceNewConnection) {
            // Get the associated map
            Cache<Integer, ListLsubCollection> cache = CACHE.getIfPresent(UserAndContext.newInstance(userId, contextId));
            if (null != cache) {
                // Iterate keys and perform explicit getIfPresent() to touch last-accessed time stamp
                for (Integer accountId : cache.asMap().keySet()) {
                    ListLsubCollection collection = cache.getIfPresent(accountId);
                    if (collection != null) {
                        try {
                            synchronized (collection) {
                                collection.clear(enforceNewConnection);
                            }
                        } catch (@SuppressWarnings("unused") Exception e) {
                            // Ignore
                        }
                    }
                }

            }
        } else {
            CACHE.invalidate(UserAndContext.newInstance(userId, contextId));
        }

        if (notify) {
            fireInvalidateCacheEvent(userId, contextId);
        }

        LOG.debug("Cleaned user-sensitive LIST/LSUB cache for user {} in context {}", Integer.valueOf(userId), Integer.valueOf(contextId));
    }

    /**
     * Removes cached LIST/LSUB entry.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param session The session
     */
    public static void removeCachedEntry(String fullName, int accountId, Session session) {
        Cache<Integer, ListLsubCollection> cache = CACHE.getIfPresent(UserAndContext.newInstance(session));
        if (null == cache) {
            return;
        }
        ListLsubCollection collection = cache.getIfPresent(Integer.valueOf(accountId));
        if (null != collection) {
            synchronized (collection) {
                collection.remove(fullName);
            }

            fireInvalidateCacheEvent(session);
        }
    }

    /**
     * Checks if associated mailbox is considered as MBox format.
     *
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return {@link Boolean#TRUE} for MBox format, {@link Boolean#FALSE} for no MBOX format or <code>null</code> if undetermined
     * @throws OXException if a mail error occurs
     * @throws MessagingException If a messaging error occurs
     */
    public static Boolean consideredAsMBox(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.consideredAsMBox();
        }
    }

    /**
     * Clears the cache.
     *
     * @param accountId The account ID
     * @param session The session
     */
    public static void clearCache(int accountId, Session session) {
        clearCache(accountId, session.getUserId(), session.getContextId());
    }

    /**
     * Clears the cache.
     *
     * @param accountId The account ID
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public static void clearCache(int accountId, int userId, int contextId) {
        Cache<Integer, ListLsubCollection> cache = CACHE.getIfPresent(UserAndContext.newInstance(userId, contextId));
        if (null == cache) {
            return;
        }
        ListLsubCollection collection = cache.getIfPresent(Integer.valueOf(accountId));
        if (null != collection) {
            synchronized (collection) {
                collection.clear(false);
            }

            fireInvalidateCacheEvent(userId, contextId);
        }
    }

    /**
     * Adds single entry to cache. Replaces any existing entry.
     *
     * @param fullName The entry's full name
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing connected protocol
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @throws OXException If entry could not be added
     * @throws MessagingException If a messaging error occurs
     */
    public static void addSingle(String fullName, int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection, imapProperties)) {
                return;
            }
            collection.addSingle(fullName, imapFolder);

            fireInvalidateCacheEvent(session);
        }
    }

    /**
     * Adds single entry to cache. Replaces any existing entry.
     *
     * @param imapFolder The IMAP folder to add
     * @param subscribed Whether IMAP folder is subscribed
     * @param accountId The account ID
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @param fullName The entry's full name
     * @throws OXException If entry could not be added
     * @throws MessagingException If a messaging error occurs
     */
    public static void addSingle(IMAPFolder imapFolder, boolean subscribed, int accountId, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection, imapProperties)) {
                return;
            }
            collection.addSingle(imapFolder, subscribed);

            fireInvalidateCacheEvent(session);
        }
    }

    /**
     * Adds single entry to cache. Replaces any existing entry.
     *
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing connected protocol
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @throws OXException If entry could not be added
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry addSingleByFolder(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            boolean addIt = !checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.addSingleByFolder(imapFolder, addIt);
        }
    }

    /**
     * Adds single entry to cache. Replaces any existing entry.
     *
     * @param accountId The account ID
     * @param listInfo The LIST entry to add
     * @param imapFolder The IMAP folder providing connected protocol
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @throws OXException If entry could not be added
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry addSingleByFolder(int accountId, ListInfo listInfo, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            boolean addIt = !checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.addSingleByFolder(listInfo, addIt);
        }
    }

    /**
     * Gets the separator character.
     *
     * @param accountId The account ID
     * @param imapStore The connected IMAP store instance
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The separator
     * @throws OXException If a mail error occurs
     */
    public static char getSeparator(int accountId, IMAPStore imapStore, Session session, IIMAPProperties imapProperties) throws OXException {
        try {
            return getSeparator(accountId, (IMAPFolder) imapStore.getFolder(INBOX), session, imapProperties);
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the separator character.
     *
     * @param accountId The account ID
     * @param imapFolder An IMAP folder
     * @param session The session
     * @param ignoreSubscriptions Whether to ignore subscriptions
     * @return The separator
     * @throws OXException If a mail error occurs
     * @throws MessagingException If a messaging error occurs
     */
    public static char getSeparator(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        return getCachedLISTEntry(INBOX, accountId, imapFolder, session, imapProperties).getSeparator();
    }

    private static boolean seemsValid(ListLsubEntry entry) {
        return (null != entry) && (entry.canOpen() || entry.isNamespace() || entry.hasChildren());
    }

    /**
     * Gets cached LSUB entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The cached LSUB entry
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getCachedLSUBEntry(String fullName, int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        if (isAccessible(collection, imapProperties)) {
            ListLsubEntry entry = collection.getLsubIgnoreDeprecated(fullName);
            if (seemsValid(entry)) {
                return entry;
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection, imapProperties)) {
                ListLsubEntry entry = collection.getLsub(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
            /*
             * Return
             */
            ListLsubEntry entry = collection.getLsub(fullName);
            if (seemsValid(entry)) {
                return entry;
            }
            /*
             * Update & re-check
             */
            boolean exists = IMAPCommandsCollection.exists(fullName, imapFolder);
            if (false == exists) {
                return ListLsubCollection.emptyEntryFor(fullName);
            }
            collection.update(fullName, imapFolder, imapProperties.isIgnoreSubscription());
            fireInvalidateCacheEvent(session);
            entry = collection.getLsub(fullName);
            return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
        }
    }

    /**
     * Gets cached LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapStore The IMAP store
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The cached LIST entry
     * @throws OXException If loading the entry fails
     */
    public static ListLsubEntry getCachedLISTEntry(String fullName, int accountId, IMAPStore imapStore, Session session, IIMAPProperties imapProperties) throws OXException {
        try {
            IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
            if (isAccessible(collection, imapProperties)) {
                ListLsubEntry entry = collection.getListIgnoreDeprecated(fullName);
                if (seemsValid(entry)) {
                    return entry;
                }
            }
            synchronized (collection) {
                if (checkTimeStamp(imapFolder, collection, imapProperties)) {
                    ListLsubEntry entry = collection.getList(fullName);
                    return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
                }
                /*
                 * Return
                 */
                ListLsubEntry entry = collection.getList(fullName);
                if (seemsValid(entry)) {
                    return entry;
                }
                /*
                 * Update & re-check
                 */
                boolean exists = IMAPCommandsCollection.exists(fullName, imapFolder);
                if (false == exists) {
                    return ListLsubCollection.emptyEntryFor(fullName);
                }
                collection.update(fullName, imapFolder, imapProperties.isIgnoreSubscription());
                fireInvalidateCacheEvent(session);
                entry = collection.getList(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets up-to-date LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapStore The IMAP store
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The cached LIST entry
     * @throws MailException If loading the entry fails
     */
    public static ListLsubEntry getActualLISTEntry(String fullName, int accountId, IMAPStore imapStore, Session session, IIMAPProperties imapProperties) throws OXException {
        try {
            IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
            synchronized (collection) {
                return collection.getActualEntry(fullName, imapFolder);
            }
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the pretty-printed cache content
     *
     * @param accountId The account identifier
     * @param session The associated session
     * @return The pretty-printed content or <code>null</code>
     */
    public static String prettyPrintCache(int accountId, Session session) {
        return prettyPrintCache(accountId, session.getUserId(), session.getContextId());
    }

    /**
     * Gets the pretty-printed cache content
     *
     * @param accountId The account identifier
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The pretty-printed content or <code>null</code>
     */
    public static String prettyPrintCache(int accountId, int userId, int contextId) {
        try {
            // Get the associated map
            Cache<Integer, ListLsubCollection> cache = CACHE.getIfPresent(UserAndContext.newInstance(userId, contextId));
            if (null == cache) {
                return null;
            }

            // Submit task
            ListLsubCollection collection = cache.getIfPresent(Integer.valueOf(accountId));
            if (null == collection) {
                return null;
            }

            return collection.toString();
        } catch (@SuppressWarnings("unused") Exception e) {
            return null;
        }
    }

    /**
     * Tries to gets cached LIST entry for specified full name.
     * <p>
     * Performs no initializations if cache or entry is absent
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param session The session
     * @return The cached LIST entry or <code>null</code>
     */
    public static ListLsubEntry tryCachedLISTEntry(String fullName, int accountId, Session session) {
        // Get the associated map
        Cache<Integer, ListLsubCollection> cache = CACHE.getIfPresent(UserAndContext.newInstance(session));
        if (null == cache) {
            return null;
        }

        ListLsubCollection collection = cache.getIfPresent(Integer.valueOf(accountId));
        if (null == collection) {
            return null;
        }

        return collection.getListIgnoreDeprecated(fullName);
    }

    /**
     * Gets cached LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The cached LIST entry
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getCachedLISTEntry(String fullName, int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        return getCachedLISTEntry(fullName, accountId, imapFolder, session, false, imapProperties);
    }

    /**
     * Gets cached LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param reinitSpecialUseIfLoaded <code>true</code> to re-initialize SPECIAL-USE folders in case cache is already loaded; otherwise <code>false</code>
     * @param imapProperties The IMAP properties to use
     * @return The cached LIST entry
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getCachedLISTEntry(String fullName, int accountId, IMAPFolder imapFolder, Session session, boolean reinitSpecialUseIfLoaded, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        if (isAccessible(collection, imapProperties)) {
            ListLsubEntry entry = collection.getListIgnoreDeprecated(fullName);
            if (seemsValid(entry)) {
                if (reinitSpecialUseIfLoaded) {
                    collection.reinitSpecialUseFolders(imapFolder);
                }
                return entry;
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection, imapProperties)) {
                if (reinitSpecialUseIfLoaded) {
                    collection.reinitSpecialUseFolders(imapFolder);
                }
                ListLsubEntry entry = collection.getList(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
            /*
             * Return
             */
            ListLsubEntry entry = collection.getList(fullName);
            if (seemsValid(entry)) {
                if (reinitSpecialUseIfLoaded) {
                    collection.reinitSpecialUseFolders(imapFolder);
                }
                return entry;
            }
            /*
             * Update & re-check
             */
            boolean exists = IMAPCommandsCollection.exists(fullName, imapFolder);
            if (false == exists) {
                return ListLsubCollection.emptyEntryFor(fullName);
            }
            collection.update(fullName, imapFolder, imapProperties.isIgnoreSubscription());
            fireInvalidateCacheEvent(session);
            entry = collection.getList(fullName);
            return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
        }
    }

    /**
     * Gets cached LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The cached LIST entry or an empty entry
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry optCachedLISTEntry(String fullName, int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        if (isAccessible(collection, imapProperties)) {
            ListLsubEntry entry = collection.getListIgnoreDeprecated(fullName);
            return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
        }
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection, imapProperties);
            ListLsubEntry entry = collection.getList(fullName);
            return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
        }
    }

    private static boolean checkTimeStamp(IMAPFolder imapFolder, ListLsubCollection collection, IIMAPProperties imapProperties) throws MessagingException {
        /*
         * Check collection's deprecation status and stamp
         */
        if (collection.isDeprecated()) {
            collection.reinit(imapFolder, imapProperties.isIgnoreSubscription());
            return true;
        }
        if ((System.currentTimeMillis() - collection.getStamp()) > getTimeout(imapProperties)) {
            collection.reinit(imapFolder, imapProperties.isIgnoreSubscription());
            return true;
        }
        return false;
    }

    private static boolean isAccessible(ListLsubCollection collection, IIMAPProperties imapProperties) {
        return !collection.isDeprecated() && ((System.currentTimeMillis() - collection.getStamp()) <= getTimeout(imapProperties));
    }

    static long getTimeout(IIMAPProperties imapProperties) {
        return imapProperties.allowFolderCaches() ? imapProperties.getFolderCacheTimeoutMillis() : 20000L;
    }

    /**
     * Gets all LIST/LSUB entries.
     *
     * @param optParentFullName The optional full name of the parent
     * @param accountId The account identifier
     * @param subscribedOnly <code>false</code> for LIST entries; otherwise <code>true</code> for LSUB ones
     * @param imapStore The IMAP store
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return All LSUB entries
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static List<ListLsubEntry> getAllEntries(String optParentFullName, int accountId, boolean subscribedOnly, IMAPStore imapStore, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        IMAPFolder imapFolder = (IMAPFolder) imapStore.getDefaultFolder();
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        if (isAccessible(collection, imapProperties)) {
            if (null == optParentFullName) {
                return subscribedOnly ? collection.getLsubsIgnoreDeprecated() : collection.getLists();
            }

            ListLsubEntry entry = subscribedOnly ? collection.getLsubIgnoreDeprecated(optParentFullName) : collection.getListIgnoreDeprecated(optParentFullName);
            if (null != entry) {
                return entry.getChildren();
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection, imapProperties)) {
                if (null == optParentFullName) {
                    return subscribedOnly ? collection.getLsubs() : collection.getLists();
                }

                ListLsubEntry entry = subscribedOnly ? collection.getLsub(optParentFullName) : collection.getList(optParentFullName);
                if (null != entry) {
                    return entry.getChildren();
                }
            }
            /*
             * Update & re-check
             */
            collection.reinit(imapStore, imapProperties.isIgnoreSubscription());
            fireInvalidateCacheEvent(session);
            if (null == optParentFullName) {
                return subscribedOnly ? collection.getLsubs() : collection.getLists();
            }

            ListLsubEntry entry = subscribedOnly ? collection.getLsub(optParentFullName) : collection.getList(optParentFullName);
            if (null != entry) {
                return entry.getChildren();
            }
            return Collections.emptyList();
        }
    }

    /**
     * Gets cached LIST/LSUB entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The cached LIST/LSUB entry
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry[] getCachedEntries(String fullName, int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        if (isAccessible(collection, imapProperties)) {
            ListLsubEntry listEntry = collection.getListIgnoreDeprecated(fullName);
            if (seemsValid(listEntry)) {
                ListLsubEntry lsubEntry = collection.getLsubIgnoreDeprecated(fullName);
                ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
                return new ListLsubEntry[] { listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection, imapProperties)) {
                ListLsubEntry listEntry = collection.getList(fullName);
                ListLsubEntry lsubEntry = collection.getLsub(fullName);
                ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
                return new ListLsubEntry[] { listEntry == null ? emptyEntryFor : listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
            }
            /*
             * Return
             */
            ListLsubEntry listEntry = collection.getList(fullName);
            if (!seemsValid(listEntry)) {
                /*
                 * Update & re-check
                 */
                collection.update(fullName, imapFolder, imapProperties.isIgnoreSubscription());
                fireInvalidateCacheEvent(session);
                listEntry = collection.getList(fullName);
            }
            ListLsubEntry lsubEntry = collection.getLsub(fullName);
            ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
            return new ListLsubEntry[] { listEntry == null ? emptyEntryFor : listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
        }
    }

    /**
     * Re-Initializes the SPECIAL-USE folders (only if the IMAP store advertises support for <code>"SPECIAL-USE"</code> capability)
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @throws OXException If re-initialization fails
     * @throws MessagingException If a messaging error occurs
     */
    public static void reinitSpecialUseFolders(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            if (isAccessible(collection, imapProperties)) {
                collection.reinitSpecialUseFolders(imapFolder);
            } else {
                checkTimeStamp(imapFolder, collection, imapProperties);
                collection.reinitSpecialUseFolders(imapFolder);
            }
        }
    }

    /**
     * Gets the LIST entries marked with "\Drafts" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getDraftsEntry(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.getDraftsEntry();
        }
    }

    /**
     * Gets the LIST entries marked with "\Junk" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getJunkEntry(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.getJunkEntry();
        }
    }

    /**
     * Gets the LIST entries marked with "\Sent" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getSentEntry(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.getSentEntry();
        }
    }

    /**
     * Gets the LIST entries marked with "\Trash" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getTrashEntry(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.getTrashEntry();
        }
    }

    /**
     * Gets the LIST entries marked with "\Archive" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getArchiveEntry(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.getArchiveEntry();
        }
    }

    private static ListLsubCollection getCollection(int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        // Get the associated map
        Cache<Integer, ListLsubCollection> cache;
        {
            UserAndContext key = UserAndContext.newInstance(session);
            cache = CACHE.getIfPresent(key);
            if (cache == null) {
                try {
                    cache = CACHE.get(key, new Loader(imapProperties));
                } catch (ExecutionException | UncheckedExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause == null) {
                        cause = e;
                    }
                    throw OXException.general(cause.getMessage(), cause);
                }
            }
        }

        // Check if present
        ListLsubCollection optCollection = cache.getIfPresent(I(accountId));
        if (optCollection != null) {
            return optCollection;
        }

        // Create loader
        final AtomicBoolean caller = new AtomicBoolean(false);
        Callable<ListLsubCollection> loader = new Callable<ListLsubCollection>() {

            @Override
            public ListLsubCollection call() throws OXException, MessagingException {
                // Determine shared and user namespaces
                List<Namespace> shared;
                List<Namespace> user;
                try {
                    IMAPStore imapStore = (IMAPStore) imapFolder.getStore();
                    Namespaces namespaces = null;
                    try {
                        namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
                        shared = check(namespaces.getShared());
                    } catch (MessagingException e) {
                        if (imapStore.hasCapability("NAMESPACE")) {
                            LOG.warn("Couldn't get shared namespaces.", e);
                        } else {
                            LOG.debug("Couldn't get shared namespaces.", e);
                        }
                        shared = List.of();
                    } catch (RuntimeException e) {
                        LOG.warn("Couldn't get shared namespaces.", e);
                        shared = List.of();
                    }
                    try {
                        if (namespaces == null) {
                            namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
                        }
                        user = check(namespaces.getOtherUsers());
                    } catch (MessagingException e) {
                        if (imapStore.hasCapability("NAMESPACE")) {
                            LOG.warn("Couldn't get user namespaces.", e);
                        } else {
                            LOG.debug("Couldn't get user namespaces.", e);
                        }
                        user = List.of();
                    } catch (RuntimeException e) {
                        LOG.warn("Couldn't get user namespaces.", e);
                        user = List.of();
                    }
                } catch (MessagingException e) {
                    throw MimeMailException.handleMessagingException(e);
                }

                // Create collection instance
                ListLsubCollection collection = new ListLsubCollection(imapFolder, shared, user, imapProperties.isIgnoreSubscription());

                // Mark running thread as caller & return collection
                caller.set(true);
                return collection;
            }
        };

        try {
            return getFromCache(accountId, loader, cache);
        } catch (OXException e) {
            if (caller.get()) {
                CACHE.invalidate(UserAndContext.newInstance(session));
            }
            throw e;
        }
    }

    private static ListLsubCollection getFromCache(int accountId, Callable<ListLsubCollection> loader, Cache<Integer, ListLsubCollection> cache) throws OXException, MessagingException {
        try {
            return cache.get(I(accountId), loader);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof OXException) {
                throw (OXException) t;
            }
            if (t instanceof MessagingException) {
                throw (MessagingException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw MailExceptionCode.UNEXPECTED_ERROR.create(t, t.getMessage());
        }
    }

    static List<Namespace> check(List<Namespace> namespaces) {
        List<Namespace> retval = null;
        for (int i = 0; i < namespaces.size(); i++) {
            Namespace ns = namespaces.get(i);
            if (retval == null) {
                if (Strings.isEmpty(ns.getFullName())) {
                    retval = new ArrayList<Namespace>(namespaces.size());
                    if (i > 0) {
                        for (int j = 0; j < i; j++) {
                            retval.add(namespaces.get(j));
                        }
                    }
                }
            } else {
                if (Strings.isNotEmpty(ns.getFullName())) {
                    retval.add(ns);
                }
            }
        }
        return retval == null ? namespaces :  retval;
    }

    /**
     * Checks for any subscribed subfolder.
     *
     * @param fullName The full name
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder providing the protocol to use
     * @param session The session
     * @param imapProperties The IMAP properties to use
     * @return <code>true</code> if a subscribed subfolder exists; otherwise <code>false</code>
     * @throws OXException If a mail error occurs
     * @throws MessagingException If a messaging error occurs
     */
    public static boolean hasAnySubscribedSubfolder(String fullName, int accountId, IMAPFolder imapFolder, Session session, IIMAPProperties imapProperties) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session, imapProperties);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection, imapProperties);
            return collection.hasAnySubscribedSubfolder(fullName);
        }
    }

    private static void fireInvalidateCacheEvent(Session session) {
        fireInvalidateCacheEvent(session.getUserId(), session.getContextId());
    }

    private static void fireInvalidateCacheEvent(int userId, int contextId) {
        CacheEventService cacheEventService = Services.optService(CacheEventService.class);
        if (null != cacheEventService && cacheEventService.getConfiguration().remoteInvalidationForPersonalFolders()) {
            CacheEvent event = newCacheEventFor(userId, contextId);
            if (null != event) {
                cacheEventService.notify(INSTANCE, event, false);
            }
        }
    }

    /**
     * Creates a new cache event
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The cache event
     */
    private static CacheEvent newCacheEventFor(int userId, int contextId) {
        CacheService service = Services.optService(CacheService.class);
        return null == service ? null : CacheEvent.INVALIDATE(REGION, Integer.toString(contextId), service.newCacheKey(contextId, userId));
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final class Loader implements Callable<Cache<Integer, ListLsubCollection>> {

        private final IIMAPProperties imapProperties;

        /**
         * Initializes a new {@link Loader}.
         *
         * @param imapProperties The IMAP properties
         */
        Loader(IIMAPProperties imapProperties) {
            super();
            this.imapProperties = imapProperties;
        }

        @Override
        public Cache<Integer, ListLsubCollection> call() {
            return CacheBuilder.newBuilder().expireAfterAccess(java.time.Duration.ofMillis(getTimeout(imapProperties) << 1)).build();
        }
    }

}
