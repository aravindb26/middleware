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

import static com.openexchange.imap.IMAPMessageStorage.allowSORTDISPLAY;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import com.google.common.collect.ImmutableMap;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.exception.OXException;
import com.openexchange.imap.IMAPCapabilities;
import com.openexchange.imap.acl.ACLExtension;
import com.openexchange.imap.acl.ACLExtensionFactory;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.namespace.Namespace;
import com.openexchange.imap.services.Services;
import com.openexchange.java.Strings;
import com.openexchange.mail.PreviewMode;
import com.openexchange.mail.api.MailConfig.BoolCapVal;
import com.openexchange.mail.cache.SessionMailCache;
import com.openexchange.mail.cache.SessionMailCacheEntry;
import com.openexchange.session.Session;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link CapabilitiesCache} - A cache to check for capabilities for a certain IMAP server.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CapabilitiesCache {

    /**
     * Initializes a new {@link CapabilitiesCache}.
     */
    private CapabilitiesCache() {
        super();
    }

    /**
     * The parsed/examined IMAP capabilities.
     */
    public static final class CapabilitiesResponse {

        private final ACLExtension aclExtension;
        private final IMAPCapabilities imapCapabilities;
        private final Map<String, String> map;

        /**
         * Initializes a new {@link CapabilitiesResponse}.
         *
         * @param aclExtension The ACL extension to use
         * @param imapCapabilities The IMAP capabilities
         * @param map The capabilities' map representation
         */
        CapabilitiesResponse(ACLExtension aclExtension, IMAPCapabilities imapCapabilities, Map<String, String> map) {
            super();
            this.aclExtension = aclExtension;
            this.imapCapabilities = imapCapabilities;
            this.map = map == null ? null : ImmutableMap.copyOf(map);
        }

        /**
         * Gets the ACL extension.
         *
         * @return The ACL extension
         */
        public ACLExtension getAclExtension() {
            return aclExtension;
        }

        /**
         * Gets the IMAP capabilities.
         *
         * @return The IMAP capabilities
         */
        public IMAPCapabilities getImapCapabilities() {
            return imapCapabilities;
        }

        /**
         * Gets the capabilities' map representation.
         *
         * @return The map
         */
        public Map<String, String> getMap() {
            return map;
        }
    }

    private static final class CapsCacheEntry implements SessionMailCacheEntry<CapabilitiesResponse> {

        private final int user;

        private volatile CapabilitiesResponse capRes;

        private volatile CacheKey key;

        public CapsCacheEntry(int user) {
            this(null, user);
        }

        public CapsCacheEntry(CapabilitiesResponse capRes, int user) {
            super();
            this.user = user;
            this.capRes = capRes;
        }

        private CacheKey getKeyInternal() {
            CacheKey tmp = key;
            if (null == tmp) {
                final CacheService service = Services.getService(CacheService.class);
                key = tmp = null == service ? null : service.newCacheKey(MailCacheCode.CAPS.getCode(), user);
            }
            return tmp;
        }

        @Override
        public CacheKey getKey() {
            return getKeyInternal();
        }

        @Override
        public CapabilitiesResponse getValue() {
            return capRes;
        }

        @Override
        public void setValue(CapabilitiesResponse value) {
            capRes = value;
        }

        @Override
        public Class<CapabilitiesResponse> getEntryClass() {
            return CapabilitiesResponse.class;
        }
    }

    /**
     * Gets cached capabilities for given IMAP store.
     *
     * @param imapStore The IMAP store
     * @param imapConfig The IMAP configuration
     * @param session The session providing the session-bound cache
     * @param accontId The account ID
     * @return The cached capabilities or <code>null</code>
     * @throws MessagingException If <code>MYRIGHTS</code> command fails
     * @throws OXException If an Open-Xchange error occurs
     */
    public static CapabilitiesResponse getCapabilitiesResponse(IMAPStore imapStore, IMAPConfig imapConfig, Session session, int accontId) throws MessagingException, OXException {
        final CapsCacheEntry entry = new CapsCacheEntry(session.getUserId());
        final SessionMailCache mailCache = SessionMailCache.getInstance(session, accontId);
        mailCache.get(entry);
        if (null == entry.getValue()) {
            final IMAPCapabilities imapCaps = new IMAPCapabilities();
            /*
             * Get as map
             */
            @SuppressWarnings("unchecked") Map<String, String> map = imapStore.getCapabilities();
            map = new LinkedHashMap<>(map);
            {
                BoolCapVal supportsACLs = imapConfig.getIMAPProperties().getSupportsACLs();
                if (BoolCapVal.AUTO.equals(supportsACLs)) {
                    imapCaps.setACL(map.containsKey(IMAPCapabilities.CAP_ACL));
                } else {
                    imapCaps.setACL(BoolCapVal.TRUE.equals(supportsACLs));
                }
            }
            imapCaps.setThreadReferences(map.containsKey(IMAPCapabilities.CAP_THREAD_REFERENCES));
            imapCaps.setThreadOrderedSubject(map.containsKey(IMAPCapabilities.CAP_THREAD_ORDEREDSUBJECT));
            imapCaps.setQuota(map.containsKey(IMAPCapabilities.CAP_QUOTA));
            boolean hasSort = map.containsKey(IMAPCapabilities.CAP_SORT);
            imapCaps.setSort(hasSort);
            imapCaps.setIMAP4(map.containsKey(IMAPCapabilities.CAP_IMAP4));
            imapCaps.setIMAP4rev1(map.containsKey(IMAPCapabilities.CAP_IMAP4_REV1));
            imapCaps.setUIDPlus(map.containsKey(IMAPCapabilities.CAP_UIDPLUS));
            imapCaps.setNamespace(map.containsKey(IMAPCapabilities.CAP_NAMESPACE));
            imapCaps.setIdle(map.containsKey(IMAPCapabilities.CAP_IDLE));
            imapCaps.setChildren(map.containsKey(IMAPCapabilities.CAP_CHILDREN));
            imapCaps.setHasSubscription(!imapConfig.getIMAPProperties().isIgnoreSubscription());
            {
                // Check for file name search capability
                boolean hasFileNameSearchCapability = map.containsKey(IMAPCapabilities.CAP_SEARCH_FILENAME);
                if (hasFileNameSearchCapability == false) {
                    hasFileNameSearchCapability = FileNameSearchSupportedCache.isFileNameSearchSupported(imapConfig, imapStore);
                    if (hasFileNameSearchCapability) {
                        map.put(IMAPCapabilities.CAP_SEARCH_FILENAME, IMAPCapabilities.CAP_SEARCH_FILENAME);
                    }
                }
                imapCaps.setFileNameSearch(hasFileNameSearchCapability);
            }
            {
                // Check if any preview capability is supported
                PreviewMode previewMode = PreviewMode.NONE;
                for (PreviewMode pm : PreviewMode.values()) {
                    String capabilityName = pm.getCapabilityName();
                    if (capabilityName != null && map.containsKey(capabilityName)) {
                        previewMode = pm;
                        break;
                    }
                }
                imapCaps.setTextPreview(previewMode != PreviewMode.NONE);
            }
            imapCaps.setMailFilterApplication(map.containsKey(IMAPCapabilities.CAP_FILTER_SIEVE));
            imapCaps.setMetadata(map.containsKey(IMAPCapabilities.CAP_METADATA));
            if (hasSort && imapConfig.getIMAPProperties().isImapSort()) {
                // IMAP sort supported & enabled
                try {
                    imapCaps.setSortDisplay(map.containsKey(IMAPCapabilities.CAP_SORT_DISPLAY) && allowSORTDISPLAY(session, accontId));
                } catch (OXException e) {
                    throw new MessagingException("Failed to determine if SORT-DISPLAY extension is allowed", e);
                }
            } else {
                // The in-memory sorting does sort with primary respect to display name, the actual address
                imapCaps.setSortDisplay(true);
            }
            imapCaps.setAttachmentSearchEnabled(imapConfig.getIMAPProperties().isAttachmentMarkerEnabled());
            {
                // Check for Dovecot server
                boolean hasXDovecotCapability = map.containsKey(IMAPCapabilities.CAP_XDOVECOT);
                if (hasXDovecotCapability == false) {
                    hasXDovecotCapability = map.containsKey("ID") && XDovecotCache.isDovecotIMAPServer(imapConfig, imapStore);
                    if (hasXDovecotCapability) {
                        map.put(IMAPCapabilities.CAP_XDOVECOT, IMAPCapabilities.CAP_XDOVECOT);
                    }
                }
                imapCaps.setDovecotServer(hasXDovecotCapability);
            }
            if (imapCaps.hasNamespace()) {
                List<Namespace> namespaces = NamespacesCache.getUserNamespaces(imapStore, true, session, accontId);
                boolean found = false;
                for (Namespace ns : namespaces) {
                    found = !Strings.isEmpty(ns.getFullName());
                }
                imapCaps.setSharedFolders(found);

                namespaces = NamespacesCache.getSharedNamespaces(imapStore, true, session, accontId);
                found = false;
                for (Namespace ns : namespaces) {
                    found = !Strings.isEmpty(ns.getFullName());
                }
                imapCaps.setPublicFolders(found);
            }
            /*
             * ACL extension
             */
            final ACLExtension aclExtension = ACLExtensionFactory.getInstance().getACLExtension(map, imapConfig);
            /*
             * Set value
             */
            entry.setValue(new CapabilitiesResponse(aclExtension, imapCaps, map));
            mailCache.put(entry);
        }
        return entry.getValue();
    }

    /**
     * Removes cached capabilities.
     *
     * @param user The user identifier
     * @param session The session providing the session-bound cache
     * @param accontId The account ID
     */
    public static void removeCachedRights(int user, Session session, int accontId) {
        SessionMailCache.getInstance(session, accontId).remove(new CapsCacheEntry(user));
    }

}
