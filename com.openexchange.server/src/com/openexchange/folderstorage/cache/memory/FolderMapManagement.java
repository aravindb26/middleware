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

package com.openexchange.folderstorage.cache.memory;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.jctools.maps.NonBlockingHashMap;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.caching.CacheService;
import com.openexchange.caching.events.CacheEvent;
import com.openexchange.caching.events.CacheEventService;
import com.openexchange.folderstorage.cache.CacheServiceRegistry;
import com.openexchange.java.util.Tools;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;

/**
 * {@link FolderMapManagement}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FolderMapManagement {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FolderMapManagement.class);

    /** The cache region name */
    public static final String REGION = FolderMapManagement.class.getSimpleName();

    private static final FolderMapManagement INSTANCE = new FolderMapManagement();

    /**
     * Gets the {@link FolderMapManagement management} instance.
     *
     * @return The management instance
     */
    public static FolderMapManagement getInstance() {
        return INSTANCE;
    }

    private static final Callable<ConcurrentMap<Integer,FolderMap>> LOADER = new Callable<ConcurrentMap<Integer,FolderMap>>() {

        @Override
        public ConcurrentMap<Integer, FolderMap> call() {
            return new NonBlockingHashMap<Integer, FolderMap>();
        }
    };

    // -------------------------------------------------------------------------------------------------------

    private final Cache<Integer, ConcurrentMap<Integer, FolderMap>> cache;

    /**
     * Initializes a new {@link FolderMapManagement}.
     */
    private FolderMapManagement() {
        super();
        cache = CacheBuilder.newBuilder().initialCapacity(64).expireAfterAccess(30, TimeUnit.MINUTES).build();
    }

    /**
     * Clears the folder management.
     */
    public void clear() {
        cache.invalidateAll();
    }

    /**
     * Drop caches for given context.
     *
     * @param contextId The context identifier
     */
    public void dropFor(int contextId) {
        dropFor(contextId, true);
    }

    /**
     * Drop caches for given context.
     *
     * @param contextId The context identifier
     * @param notify Whether to post notification or not
     */
    public void dropFor(int contextId, boolean notify) {
        cache.invalidate(Integer.valueOf(contextId));
        if (notify) {
            fireInvalidateCacheEvent(contextId);
        }
        LOG.debug("Cleaned user-sensitive folder cache for context {}", I(contextId));
    }

    /**
     * Drop caches for given session's user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public void dropFor(int userId, int contextId) {
        dropFor(userId, contextId, true);
    }

    /**
     * Drop caches for given session's user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param notify Whether to post notification or not
     */
    public void dropFor(int userId, int contextId, boolean notify) {
        ConcurrentMap<Integer, FolderMap> contextMap = cache.getIfPresent(Integer.valueOf(contextId));
        if (null != contextMap) {
            contextMap.remove(Integer.valueOf(userId));
        }
        if (notify) {
            fireInvalidateCacheEvent(userId, contextId);
        }
        LOG.debug("Cleaned user-sensitive folder cache for user {} in context {}", I(userId), I(contextId));
    }

    /**
     * Gets the folder map for specified session.
     *
     * @param contextId The context ID
     * @param userId The user ID
     * @return The folder map
     */
    public FolderMap getFor(int contextId, int userId) {
        try {
            ConcurrentMap<Integer, FolderMap> contextMap = cache.get(Integer.valueOf(contextId), LOADER);

            final Integer us = Integer.valueOf(userId);
            FolderMap folderMap = contextMap.get(us);
            if (null == folderMap) {
                final FolderMap newFolderMap = new FolderMap(300, TimeUnit.SECONDS, userId, contextId);
                folderMap = contextMap.putIfAbsent(us, newFolderMap);
                if (null == folderMap) {
                    folderMap = newFolderMap;
                }
            }
            return folderMap;
        } catch (ExecutionException e) {
            // Cannot occur
            throw new IllegalStateException(e.getCause());
        }
    }

    /**
     * Optionally gets the folder map for specified session.
     *
     * @param session The session
     * @return The folder map or <code>null</code> if absent
     */
    public FolderMap optFor(Session session) {
        final ConcurrentMap<Integer, FolderMap> contextMap = cache.getIfPresent(Integer.valueOf(session.getContextId()));
        if (null == contextMap) {
            return null;
        }
        return contextMap.get(Integer.valueOf(session.getUserId()));
    }

    /**
     * Optionally gets the folder map for specified user in given context.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The folder map or <code>null</code> if absent
     */
    public FolderMap optFor(int userId, int contextId) {
        final ConcurrentMap<Integer, FolderMap> contextMap = cache.getIfPresent(Integer.valueOf(contextId));
        if (null == contextMap) {
            return null;
        }
        return contextMap.get(Integer.valueOf(userId));
    }

    /**
     * Drop folder from all user caches for given context.
     *
     * @param folderId The folder id
     * @param treeId The tree id
     * @param optUser The optional user identifier
     * @param contextId The context identifier
     */
    public void dropFor(String folderId, String treeId, int optUser, int contextId) {
        dropFor(folderId, treeId, optUser, contextId, null);
    }

    /**
     * Drop folder from all user caches for given context.
     *
     * @param folderId The folder id
     * @param treeId The tree id
     * @param optUser The optional user identifier
     * @param contextId The context identifier
     * @param optSession The optional session
     */
    public void dropFor(String folderId, String treeId, int optUser, int contextId, Session optSession) {
        dropFor(folderId, treeId, optUser, contextId, optSession, true);
    }

    /**
     * Drop folders from all user caches for given context.
     *
     * @param folderIds The folder identifiers
     * @param treeId The tree id
     * @param optUser The optional user identifier
     * @param contextId The context identifier
     * @param optSession The optional session
     */
    public void dropFor(List<String> folderIds, String treeId, int optUser, int contextId, Session optSession) {
        dropFor(folderIds, treeId, optUser, contextId, optSession, true);
    }

    /**
     * Drop folders from all user caches for given context.
     *
     * @param folderIds The folder identifiers
     * @param treeId The tree id
     * @param optUser The optional user identifier
     * @param contextId The context identifier
     * @param optSession The optional session
     * @param notify Whether to post notification or not
     */
    public void dropFor(List<String> folderIds, String treeId, int optUser, int contextId, Session optSession, boolean notify) {
        if ((null == folderIds) || (null == treeId)) {
            return;
        }
        ConcurrentMap<Integer, FolderMap> contextMap = cache.getIfPresent(Integer.valueOf(contextId));
        if (null == contextMap) {
            return;
        }
        for (String folderId : folderIds) {
            if (optUser > 0 && Tools.getUnsignedInteger(folderId) < 0) {
                final FolderMap folderMap = contextMap.get(Integer.valueOf(optUser));
                if (null != folderMap) {
                    folderMap.remove(folderId, treeId, optSession);
                }
            } else {
                // Delete all known
                for (final FolderMap folderMap : contextMap.values()) {
                    if (null == optSession) {
                        folderMap.remove(folderId, treeId);
                    } else {
                        folderMap.remove(folderId, treeId, optSession);
                    }
                }
            }
        }
        if (notify) {
            fireInvalidateCacheEvent(folderIds, treeId, optUser, contextId);
        }
    }

    /**
     * Drop folders hierarchies from all user caches for given context.
     *
     * @param folderIds The folder identifiers
     * @param treeId The tree id
     * @param optUser The optional user identifier
     * @param contextId The context identifier
     * @param notify Whether to post notification or not
     */
    public void dropHierarchyFor(Collection<String> folderIds, String treeId, int optUser, int contextId) {
        dropHierarchyFor(folderIds, treeId, optUser, contextId, true);
    }

    /**
     * Drop folders hierarchies from all user caches for given context.
     *
     * @param folderIds The folder identifiers
     * @param treeId The tree id
     * @param optUser The optional user identifier
     * @param contextId The context identifier
     * @param notify Whether to post notification or not
     */
    public void dropHierarchyFor(Collection<String> folderIds, String treeId, int optUser, int contextId, boolean notify) {
        if ((null == folderIds) || (null == treeId)) {
            return;
        }
        ConcurrentMap<Integer, FolderMap> contextMap = cache.getIfPresent(Integer.valueOf(contextId));
        if (null == contextMap) {
            return;
        }
        Set<String> ids = notify ? new HashSet<String>(16, 0.9f) : null;
        for (String folderId : folderIds) {
            if (optUser > 0 && Tools.getUnsignedInteger(folderId) < 0) {
                FolderMap folderMap = contextMap.get(Integer.valueOf(optUser));
                if (null != folderMap) {
                    folderMap.removeHierarchy(folderId, treeId, ids);
                }
            } else {
                // Delete all known
                for (FolderMap folderMap : contextMap.values()) {
                    folderMap.removeHierarchy(folderId, treeId, ids);
                }
            }
        }
        if (notify && (null != ids && !ids.isEmpty())) {
            fireInvalidateCacheEvent(new ArrayList<String>(ids), treeId, optUser, contextId);
        }
    }

    /**
     * Drop folder from all user caches for given context.
     *
     * @param folderId The folder id
     * @param treeId The tree id
     * @param optUser The optional user identifier
     * @param contextId The context identifier
     * @param optSession The optional session
     * @param notify Whether to post notification or not
     */
    public void dropFor(String folderId, String treeId, int optUser, int contextId, Session optSession, boolean notify) {
        if ((null == folderId) || (null == treeId)) {
            return;
        }
        final ConcurrentMap<Integer, FolderMap> contextMap = cache.getIfPresent(Integer.valueOf(contextId));
        if (null == contextMap) {
            return;
        }
        //  If folder identifier is not a number AND user identifier is valid
        //  (because numbers hint to former global folders; e.g. database folders)
        //  Then it is sufficient to clean in user-associated map only
        if (optUser > 0 && Tools.getUnsignedInteger(folderId) < 0) {
            final FolderMap folderMap = contextMap.get(Integer.valueOf(optUser));
            if (null != folderMap) {
                folderMap.remove(folderId, treeId, optSession);
            }
        } else {
            // Delete all known
            for (final FolderMap folderMap : contextMap.values()) {
                if (null == optSession) {
                    folderMap.remove(folderId, treeId);
                } else {
                    folderMap.remove(folderId, treeId, optSession);
                }
            }
        }
        if (notify) {
            fireInvalidateCacheEvent(folderId, treeId, optUser, contextId);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    private static void fireInvalidateCacheEvent(int contextId) {
        fireInvalidateCacheEvent(-1, contextId);
    }

    private static void fireInvalidateCacheEvent(int userId, int contextId) {
        fireInvalidateCacheEvent((String) null, null, userId, contextId);
    }

    private static void fireInvalidateCacheEvent(String folderId, String treeId, int optUser, int contextId) {
        fireInvalidateCacheEvent(Collections.singletonList(folderId), treeId, optUser, contextId);
    }

    private static void fireInvalidateCacheEvent(List<String> folderIds, String treeId, int optUser, int contextId) {
        CacheEventService cacheEventService = CacheServiceRegistry.getServiceRegistry().getOptionalService(CacheEventService.class);
        if (null != cacheEventService && cacheEventService.getConfiguration().remoteInvalidationForPersonalFolders()) {
            CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
            if (null == cacheService) {
                return;
            }
            if ((null == folderIds || folderIds.isEmpty()) || (1 == folderIds.size() && null == folderIds.get(0))) {
                /*
                 * Context-/user-wide invalidation
                 */
                CacheEvent event = CacheEvent.INVALIDATE(REGION, String.valueOf(contextId), cacheService.newCacheKey(optUser));
                cacheEventService.notify(INSTANCE, event, false);
            } else {
                /*
                 * Explicit invalidation of one or more folders
                 */
                List<String> keys = new ArrayList<String>();
                keys.add(treeId);
                for (String folderId : folderIds) {
                    if (false == com.openexchange.folderstorage.internal.Tools.isGlobalId(folderId)) {
                        keys.add(folderId);
                    }
                }
                if (false == keys.isEmpty()) {
                    CacheEvent event = CacheEvent.INVALIDATE(REGION, String.valueOf(contextId), cacheService.newCacheKey(optUser, keys.toArray(new String[keys.size()])));
                    cacheEventService.notify(INSTANCE, event, false);
                }
            }
        }
    }

}
