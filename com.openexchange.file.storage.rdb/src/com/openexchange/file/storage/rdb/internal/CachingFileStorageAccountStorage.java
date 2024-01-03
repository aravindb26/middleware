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

package com.openexchange.file.storage.rdb.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.rdb.Services;
import com.openexchange.folderstorage.cache.service.FolderCacheInvalidationService;
import com.openexchange.java.Strings;
import com.openexchange.lock.LockService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;

/**
 * {@link CachingFileStorageAccountStorage} - The messaging account manager backed by {@link CacheService}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.18.2
 */
public final class CachingFileStorageAccountStorage implements FileStorageAccountStorage {

    private static final CachingFileStorageAccountStorage INSTANCE = new CachingFileStorageAccountStorage();

    private static final Logger LOG = LoggerFactory.getLogger(CachingFileStorageAccountStorage.class);

    private static final String REGION_NAME = "FileStorageAccount";

    /**
     * Gets the cache region name.
     *
     * @return The cache region name
     */
    public static String getRegionName() {
        return REGION_NAME;
    }

    /**
     * Gets the cache-backed instance.
     *
     * @return The cache-backed instance
     */
    public static CachingFileStorageAccountStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Generates a new cache key.
     *
     * @return The new cache key
     */
    protected static CacheKey newCacheKey(final CacheService cacheService, final String serviceId, final int id, final int user, final int cid) {
        return cacheService.newCacheKey(cid, serviceId, String.valueOf(id), String.valueOf(user));
    }

    /**
     * Generates a new cache key for storing/looking up all identifiers a certain user has accounts for.
     *
     * @param cacheService A reference to the cache service
     * @param contextId The context identifier
     * @param userId The user identifier
     * @param serviceId The serice identifier
     * @return The cache key
     */
    private static CacheKey newCacheKeyForAccountIds(CacheService cacheService, int contextId, int userId, String serviceId) {
        return cacheService.newCacheKey(contextId, String.valueOf(userId), serviceId);
    }

    /*-
     * ------------------------------ Member section ------------------------------
     */

    /**
     * The database-backed delegatee.
     */
    private final RdbFileStorageAccountStorage delegatee;

    /**
     * The service registry.
     */
    private final ServiceLookup serviceRegistry;

    /**
     * Initializes a new {@link CachingFileStorageAccountStorage}.
     */
    private CachingFileStorageAccountStorage() {
        super();
        delegatee = RdbFileStorageAccountStorage.getInstance();
        serviceRegistry = Services.getServices();
    }

    /**
     * Invalidates specified account.
     *
     * @param serviceId The service identifier
     * @param id The account identifier
     * @param user The user identifier
     * @param contextId The context identifier
     * @throws OXException If invalidation fails
     */
    public void invalidate(final String serviceId, final int id, final int user, final int contextId) throws OXException {
        invalidateFileStorageAccount(serviceId, id, user, contextId);
    }

    private void invalidateFileStorageAccount(final String serviceId, final int id, final int user, final int cid) throws OXException {
        final CacheService cacheService = serviceRegistry.getService(CacheService.class);
        if (null != cacheService) {
            final Cache cache = cacheService.getCache(REGION_NAME);
            cache.remove(newCacheKey(cacheService, serviceId, id, user, cid));
        }
    }

    /**
     * Invalidates the list of cached account identifiers in a specific service for the supplied session user.
     *
     * @param session The session
     * @param serviceId The service identifier
     */
    private void invalidateFileStorageAccountIds(Session session, String serviceId) throws OXException {
        final CacheService cacheService = serviceRegistry.getService(CacheService.class);
        if (null != cacheService) {
            Cache cache = cacheService.getCache(REGION_NAME);
            cache.remove(newCacheKeyForAccountIds(cacheService, session.getContextId(), session.getUserId(), serviceId));
        }
    }

    /**
     * Gets the first account matching specified account identifier.
     *
     * @param accountId The account identifier
     * @param session The session
     * @return The matching account or <code>null</code>
     * @throws OXException If look-up fails
     */
    public FileStorageAccount getAccount(final int accountId, final Session session) throws OXException {
        return delegatee.getAccount(accountId, session);
    }

    @Override
    public int addAccount(final String serviceId, final FileStorageAccount account, final Session session) throws OXException {
        int accountId = delegatee.addAccount(serviceId, account, session);
        invalidateFileStorageAccountIds(session, serviceId);
        return accountId;
    }

    @Override
    public void deleteAccount(final String serviceId, final FileStorageAccount account, final Session session) throws OXException {
        delegatee.deleteAccount(serviceId, account, session);
        invalidateFileStorageAccount(serviceId, Integer.parseInt(account.getId()), session.getUserId(), session.getContextId());
        invalidateFileStorageAccountIds(session, serviceId);
    }

    @Override
    public FileStorageAccount getAccount(final String serviceId, final int id, final Session session) throws OXException {
        if (Strings.isEmpty(serviceId) || id < 0 || session == null) {
            throw FileStorageExceptionCodes.ACCOUNT_NOT_FOUND.create(Integer.valueOf(id), serviceId, Integer.valueOf(session == null ? -1 : session.getUserId()), Integer.valueOf(session == null ? -1 : session.getContextId()));
        }
        CacheService cacheService = serviceRegistry.getService(CacheService.class);
        if (cacheService == null) {
            return delegatee.getAccount(serviceId, id, session);
        }

        Cache cache = cacheService.getCache(REGION_NAME);
        Object object = cache.get(newCacheKey(cacheService, serviceId, id, session.getUserId(), session.getContextId()));
        if (object instanceof FileStorageAccount) {
            return (FileStorageAccount) object;
        }

        LockService lockService = Services.getOptionalService(LockService.class);
        Lock lock = null == lockService ? LockService.EMPTY_LOCK : lockService.getSelfCleaningLockFor(new StringBuilder(32).append("fsaccount-").append(session.getContextId()).append('-').append(session.getUserId()).append('-').append(id).append('-').append(serviceId).toString());
        lock.lock();
        try {
            object = cache.get(newCacheKey(cacheService, serviceId, id, session.getUserId(), session.getContextId()));
            if (object instanceof FileStorageAccount) {
                return (FileStorageAccount) object;
            }

            FileStorageAccount account = delegatee.getAccount(serviceId, id, session);
            cache.put(newCacheKey(cacheService, serviceId, id, session.getUserId(), session.getContextId()), account, false);
            return account;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<FileStorageAccount> getAccounts(final String serviceId, final Session session) throws OXException {
        if (Strings.isEmpty(serviceId) || session == null) {
            return Collections.emptyList();
        }
        CacheService cacheService = serviceRegistry.getService(CacheService.class);
        if (null == cacheService) {
            return getAccounts(serviceId, delegatee.getAccountIDs(serviceId, session), session);
        }
        /*
         * get account identifiers from cache if possible
         */
        Cache cache = cacheService.getCache(REGION_NAME);
        CacheKey accountIdsKey = newCacheKeyForAccountIds(cacheService, session.getContextId(), session.getUserId(), serviceId);
        Object object = cache.get(accountIdsKey);
        if (object instanceof int[] accountIds) {
            return getAccounts(serviceId, new TIntArrayList(accountIds), session);
        }
        /*
         * lookup account ids from storage and put into cache, otherwise
         */
        TIntList accountIDList;
        LockService lockService = Services.getOptionalService(LockService.class);
        Lock lock = null == lockService ? LockService.EMPTY_LOCK : lockService.getSelfCleaningLockFor(new StringBuilder(32).append("fsaccount-").append(session.getContextId()).append('-').append(session.getUserId()).append('-').append(serviceId).toString());
        lock.lock();
        try {
            object = cache.get(accountIdsKey);
            if (object instanceof int[] accountIds) {
                accountIDList = new TIntArrayList(accountIds);
            } else {
                accountIDList = delegatee.getAccountIDs(serviceId, session);
                cache.put(accountIdsKey, null == accountIDList ? new int[0] : accountIDList.toArray(), false);
            }
        } finally {
            lock.unlock();
        }
        return getAccounts(serviceId, accountIDList, session);
    }

    private List<FileStorageAccount> getAccounts(String serviceId, TIntList ids, Session session) throws OXException {
        if (null == ids || ids.isEmpty()) {
            return Collections.emptyList();
        }
        final List<FileStorageAccount> accounts = new ArrayList<FileStorageAccount>(ids.size());
        class AdderProcedure implements TIntProcedure {

            OXException fsException;

            @Override
            public boolean execute(final int id) {
                try {
                    accounts.add(getAccount(serviceId, id, session));
                    return true;
                } catch (OXException e) {
                    fsException = e;
                    return false;
                }
            }

        }
        final AdderProcedure ap = new AdderProcedure();
        if (!ids.forEach(ap) && null != ap.fsException) {
            throw ap.fsException;
        }
        return accounts;
    }

    @Override
    public void updateAccount(final String serviceId, final FileStorageAccount account, final Session session) throws OXException {
        delegatee.updateAccount(serviceId, account, session);
        invalidateFileStorageAccount(serviceId, Integer.parseInt(account.getId()), session.getUserId(), session.getContextId());
        invalidateFileStorageAccountIds(session, serviceId);
        invalidateFolderCache(session, serviceId, account.getId());
    }

    private void invalidateFolderCache(Session session, String serviceId, String accountId) {
        FolderCacheInvalidationService folderCacheInvalidationService = serviceRegistry.getService(FolderCacheInvalidationService.class);
        try {
            folderCacheInvalidationService.invalidateSingle(serviceId+"://"+accountId+"/", "1", session);
        } catch (OXException e) {
            LOG.error("Unable to invalidate folder cache.", e);
        }
    }

    public boolean hasEncryptedItems(final FileStorageService service, final Session session) throws OXException {
        return delegatee.hasEncryptedItems(service, session);
    }

    public void migrateToNewSecret(final FileStorageService parentService, final String oldSecret, final String newSecret, final Session session) throws OXException {
        delegatee.migrateToNewSecret(parentService, oldSecret, newSecret, session);
    }

    public void cleanUp(final FileStorageService parentService, final String secret, final Session session) throws OXException {
        delegatee.cleanUp(parentService, secret, session);
    }

    public void removeUnrecoverableItems(final FileStorageService parentService, final String secret, final Session session) throws OXException {
        delegatee.removeUnrecoverableItems(parentService, secret, session);
    }

}
