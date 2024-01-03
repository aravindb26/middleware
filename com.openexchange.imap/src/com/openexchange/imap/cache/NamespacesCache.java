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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.mail.MessagingException;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.imap.namespace.Namespace;
import com.openexchange.imap.namespace.Namespaces;
import com.openexchange.imap.services.Services;
import com.openexchange.mail.cache.SessionMailCache;
import com.openexchange.mail.cache.SessionMailCacheEntry;
import com.openexchange.session.Session;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link NamespacesCache} - The session-bound cache for NAMESPACE command.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class NamespacesCache {

    /**
     * No instance
     */
    private NamespacesCache() {
        super();
    }

    /**
     * Gets cached namespaces from given IMAP store.
     *
     * @param imapStore The IMAP store on which <code>NAMESPACE</code> command is invoked
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return The namespaces
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static Namespaces getNamespaces(IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        final NamespacesCacheEntry entry = new NamespacesCacheEntry();
        final SessionMailCache mailCache = SessionMailCache.getInstance(session, accountId);
        mailCache.get(entry);
        if (load && (null == entry.getValue())) {
            com.sun.mail.imap.protocol.Namespaces tmp = imapStore.getNamespaces();
            List<Namespace> personal = tmp.personal == null ? null : Arrays.stream(tmp.personal).filter(Objects::nonNull).map(ns -> new Namespace(ns.prefix, ns.delimiter)).toList();
            List<Namespace> otherUsers = tmp.otherUsers == null ? null : Arrays.stream(tmp.otherUsers).filter(Objects::nonNull).map(ns -> new Namespace(ns.prefix, ns.delimiter)).toList();
            List<Namespace> shared = tmp.shared == null ? null : Arrays.stream(tmp.shared).filter(Objects::nonNull).map(ns -> new Namespace(ns.prefix, ns.delimiter)).toList();
            Namespaces namespaces = new Namespaces(personal, otherUsers, shared);
            entry.setValue(namespaces);
            mailCache.put(entry);
        }
        return entry.getValue();
    }

    /**
     * Gets cached personal namespaces from given IMAP store.
     *
     * @param imapStore The IMAP store on which <code>NAMESPACE</code> command is invoked
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return The personal namespaces
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static List<Namespace> getPersonalNamespaces(IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        return getNamespaces(imapStore, load, session, accountId).getPersonal();
    }

    /**
     * Gets cached prefix for personal namespace.
     *
     * @param imapStore The IMAP store on which <code>NAMESPACE</code> command is invoked
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return The personal namespace folder or <code>null</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static Namespace getPersonalNamespace(IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        List<Namespace> personal = getPersonalNamespaces(imapStore, load, session, accountId);
        return personal.isEmpty() ? null : personal.get(0);
    }

    /**
     * Checks if personal namespaces contain the specified full name
     *
     * @param fullname The full name to check
     * @param imapStore The IMAP store
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return <code>true</code> if personal namespaces contain the specified full name; otherwise <code>false</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static boolean containedInPersonalNamespaces(String fullname, IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        for (Namespace namespace : getPersonalNamespaces(imapStore, load, session, accountId)) {
            if ((namespace.getFullName()).equals(fullname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets cached user namespaces when invoking <code>NAMESPACE</code> command on given IMAP store
     *
     * @param imapStore The IMAP store on which <code>NAMESPACE</code> command is invoked
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return The <b>binary-sorted</b> user namespace folders
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static List<Namespace> getUserNamespaces(IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        return getNamespaces(imapStore, load, session, accountId).getOtherUsers();
    }

    /**
     * Checks if user namespaces contain the specified full name
     *
     * @param fullname The full name to check
     * @param imapStore The IMAP store
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return <code>true</code> if user namespaces contain the specified fullname; otherwise <code>false</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static boolean containedInUserNamespaces(String fullname, IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        for (Namespace namespace : getUserNamespaces(imapStore, load, session, accountId)) {
            if ((namespace.getFullName()).equals(fullname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if provided full name starts with any of user namespaces.
     *
     * @param fullname The full name to check
     * @param imapStore The IMAP store
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return <code>true</code> if provided full name starts with any of user namespaces; otherwise <code>false</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static boolean startsWithAnyOfUserNamespaces(String fullname, IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        for (Namespace userNamespace : getUserNamespaces(imapStore, load, session, accountId)) {
            if (fullname.equals(userNamespace.getFullName()) || fullname.startsWith(userNamespace.getPrefix())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets cached shared namespaces when invoking <code>NAMESPACE</code> command on given IMAP store
     *
     * @param imapStore The IMAP store on which <code>NAMESPACE</code> command is invoked
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return The <b>binary-sorted</b> shared namespace folders
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static List<Namespace> getSharedNamespaces(IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        return getNamespaces(imapStore, load, session, accountId).getShared();
    }

    /**
     * Checks if shared namespaces contain the specified full name.
     *
     * @param fullname The full name to check
     * @param imapStore The IMAP store
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return <code>true</code> if shared namespaces contain the specified full name; otherwise <code>false</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static boolean containedInSharedNamespaces(String fullname, IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        for (Namespace namespace : getSharedNamespaces(imapStore, load, session, accountId)) {
            if ((namespace.getFullName()).equals(fullname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if provided full name starts with any of shared namespaces.
     *
     * @param fullname The full name to check
     * @param imapStore The IMAP store
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return <code>true</code> if provided full name starts with any of shared namespaces; otherwise <code>false</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static boolean startsWithAnyOfSharedNamespaces(String fullname, IMAPStore imapStore, boolean load, Session session, int accountId) throws MessagingException {
        for (Namespace sharedNamespace : getSharedNamespaces(imapStore, load, session, accountId)) {
            if (fullname.equals(sharedNamespace.getFullName()) || fullname.startsWith(sharedNamespace.getPrefix())) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final class NamespacesCacheEntry implements SessionMailCacheEntry<Namespaces> {

        private final AtomicReference<Namespaces> namespaces;
        private final CacheKey key;

        NamespacesCacheEntry() {
            this(null);
        }

        NamespacesCacheEntry(Namespaces namespaces) {
            super();
            this.namespaces = new AtomicReference<Namespaces>(namespaces);
            this.key = Services.getService(CacheService.class).newCacheKey(MailCacheCode.NAMESPACES.getCode(), "");
        }

        @Override
        public CacheKey getKey() {
            return key;
        }

        @Override
        public Namespaces getValue() {
            return namespaces.get();
        }

        @Override
        public void setValue(Namespaces namespaces) {
            this.namespaces.set(namespaces);
        }

        @Override
        public Class<Namespaces> getEntryClass() {
            return Namespaces.class;
        }

    }
}
