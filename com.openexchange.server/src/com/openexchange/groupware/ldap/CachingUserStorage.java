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

package com.openexchange.groupware.ldap;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.RdbUserStorage.ValuePair;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.lock.LockService;
import com.openexchange.log.LogProperties;
import com.openexchange.password.mechanism.PasswordMech;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.user.User;
import com.openexchange.user.UserExceptionCode;
import com.openexchange.user.internal.mapping.UserMapper;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * This class implements the user storage using a cache to store once read objects.
 */
public class CachingUserStorage extends UserStorage {

    /**
     * Logger.
     */
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CachingUserStorage.class);

    static final String REGION_NAME = "User";

    /**
     * Proxy attribute for the object implementing the persistent methods.
     */
    private final RdbUserStorage delegate;

    /**
     * Default constructor.
     */
    public CachingUserStorage(RdbUserStorage delegate) {
        super();
        this.delegate = delegate;
    }

    private void handleOXException(OXException e, int contextId, Cache cache) {
        if (UserExceptionCode.USER_NOT_FOUND.equals(e)) {
            String uid = LogProperties.get(LogProperties.Name.LOGIN_RESOLVED_LOGIN);
            if (null != uid) {
                try {
                    cache.remove(cache.newCacheKey(contextId, uid));
                } catch (Exception x) {
                    LOG.trace("Failed to remove from cache", x);
                }
            }
        }
    }

    @Override
    public boolean isGuest(int userId, int contextId) throws OXException {
        CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.isGuest(userId, ContextStorage.getInstance().getContext(contextId));
        }

        Cache cache = cacheService.getCache(REGION_NAME);
        CacheKey key = cacheService.newCacheKey(contextId, userId);
        Object object = cache.get(key);
        if (object instanceof User user) {
            return user.isGuest();
        }

        LockService lockService = ServerServiceRegistry.getInstance().getService(LockService.class);
        Lock lock = null == lockService ? LockService.EMPTY_LOCK : lockService.getSelfCleaningLockFor(new StringBuilder(32).append("User-").append(contextId).append('-').append(userId).toString());
        lock.lock();
        try {
            object = cache.get(key);
            if (object instanceof User user) {
                return user.isGuest();
            }

            User user = delegate.getUser(userId, contextId);
            cache.put(key, user, false);
            return user.isGuest();
        } catch (OXException e) {
            handleOXException(e, contextId, cache);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isGuest(int userId, Context context) throws OXException {
        CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.isGuest(userId, context);
        }

        Cache cache = cacheService.getCache(REGION_NAME);
        CacheKey key = cacheService.newCacheKey(context.getContextId(), userId);
        Object object = cache.get(key);
        if (object instanceof User user) {
            return user.isGuest();
        }

        LockService lockService = ServerServiceRegistry.getInstance().getService(LockService.class);
        Lock lock = null == lockService ? LockService.EMPTY_LOCK : lockService.getSelfCleaningLockFor(new StringBuilder(32).append("User-").append(context.getContextId()).append('-').append(userId).toString());
        lock.lock();
        try {
            object = cache.get(key);
            if (object instanceof User user) {
                return user.isGuest();
            }

            User user = delegate.getUser(userId, context);
            cache.put(key, user, false);
            return user.isGuest();
        } catch (OXException e) {
            handleOXException(e, context.getContextId(), cache);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean exists(int userId, int contextId) throws OXException {
        CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.exists(userId, contextId);
        }

        Cache cache = cacheService.getCache(REGION_NAME);
        CacheKey key = cacheService.newCacheKey(contextId, userId);
        Object object = cache.get(key);
        if (object instanceof User) {
            return true;
        }

        return delegate.exists(userId, contextId);
    }

    @Override
    public User getUser(int uid, Context context) throws OXException {
        CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.getUser(uid, context);
        }

        Cache cache = cacheService.getCache(REGION_NAME);
        CacheKey key = cacheService.newCacheKey(context.getContextId(), uid);
        Object object = cache.get(key);
        if (object instanceof User user) {
            return user;
        }

        LockService lockService = ServerServiceRegistry.getInstance().getService(LockService.class);
        Lock lock = null == lockService ? LockService.EMPTY_LOCK : lockService.getSelfCleaningLockFor(new StringBuilder(32).append("User-").append(context.getContextId()).append('-').append(uid).toString());
        lock.lock();
        try {
            object = cache.get(key);
            if (object instanceof User user) {
                return user;
            }

            User user = delegate.getUser(uid, context);
            cache.put(key, user, false);
            return user;
        } catch (OXException e) {
            handleOXException(e, context.getContextId(), cache);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public User loadIfAbsent(int userId, Context context, Connection con) throws OXException {
        CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.getUser(userId, context);
        }

        Cache cache = cacheService.getCache(REGION_NAME);
        CacheKey key = cacheService.newCacheKey(context.getContextId(), userId);
        Object object = cache.get(key);
        if (object instanceof User user) {
            return user;
        }

        LockService lockService = ServerServiceRegistry.getInstance().getService(LockService.class);
        Lock lock = null == lockService ? LockService.EMPTY_LOCK : lockService.getSelfCleaningLockFor(new StringBuilder(32).append("User-").append(context.getContextId()).append('-').append(userId).toString());
        lock.lock();
        try {
            object = cache.get(key);
            if (object instanceof User user) {
                return user;
            }

            User user = delegate.getUser(context, userId, con);
            cache.put(key, user, false);
            return user;
        } catch (OXException e) {
            handleOXException(e, context.getContextId(), cache);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int createUser(final Connection con, final Context context, final User user) throws OXException {
        return delegate.createUser(con, context, user);
    }

    @Override
    public void deleteUser(Context context, int userId) throws OXException {
        delegate.deleteUser(context, userId);
        invalidateUser(context, userId);
    }

    @Override
    public void deleteUser(final Connection con, final Context context, int userId) throws OXException {
        delegate.deleteUser(con, context, userId);
        invalidateUser(context, userId);
    }

    @Override
    public User getUser(int userId, int contextId) throws OXException {
        CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.getUser(userId, ContextStorage.getInstance().getContext(contextId));
        }

        Cache cache = cacheService.getCache(REGION_NAME);
        Object object = cache.get(cacheService.newCacheKey(contextId, userId));
        if (object instanceof User user) {
            return user;
        }

        LockService lockService = ServerServiceRegistry.getInstance().getService(LockService.class);
        Lock lock = null == lockService ? LockService.EMPTY_LOCK : lockService.getSelfCleaningLockFor(new StringBuilder(32).append("User-").append(contextId).append('-').append(userId).toString());
        lock.lock();
        try {
            object = cache.get(cacheService.newCacheKey(contextId, userId));
            if (object instanceof User user) {
                return user;
            }

            Context context = ContextStorage.getInstance().getContext(contextId);
            User user = delegate.getUser(userId, context);
            cache.put(cacheService.newCacheKey(contextId, userId), user, false);
            return user;
        } catch (OXException e) {
            handleOXException(e, contextId, cache);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public User getUser(final Context ctx, final int userId, final Connection con) throws OXException {
        CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.getUser(ctx, userId, con);
        }
        final User user = delegate.getUser(ctx, userId, con);
        cacheService.getCache(REGION_NAME).put(cacheService.newCacheKey(ctx.getContextId(), user.getId()), user, false);
        return user;
    }

    @Override
    public User[] getUser(Context ctx, boolean includeGuests, boolean excludeUsers) throws OXException {
        return getUser(ctx, listAllUser(null, ctx, includeGuests, excludeUsers));
    }

    @Override
    public User[] getUser(Connection con, Context ctx, boolean includeGuests, boolean excludeUsers) throws OXException {
        return getUser(ctx, listAllUser(con, ctx, includeGuests, excludeUsers), con);
    }

    @Override
    public User[] getUser(final Context ctx, final int[] userIds, Connection con) throws OXException {
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.getUser(ctx, userIds, con);
        }
        final Cache cache = cacheService.getCache(REGION_NAME);
        final TIntObjectMap<User> map = new TIntObjectHashMap<>(userIds.length, 1);
        final List<Integer> toLoad = new ArrayList<>(userIds.length);
        final int contextId = ctx.getContextId();
        for (final int userId : userIds) {
            final Object object = cache.get(cacheService.newCacheKey(contextId, userId));
            if (object instanceof User user) {
                map.put(userId, user);
            } else {
                toLoad.add(I(userId));
            }
        }
        final User[] loaded = delegate.getUser(ctx, I2i(toLoad), con);
        for (final User user : loaded) {
            cache.put(cacheService.newCacheKey(contextId, user.getId()), user, false);
            map.put(user.getId(), user);
        }
        final List<User> retval = new ArrayList<>(userIds.length);
        for (final int userId : userIds) {
            retval.add(map.get(userId));
        }
        return retval.toArray(new User[retval.size()]);
    }

    @Override
    public User[] getUser(final Context ctx, final int[] userIds) throws OXException {
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.getUser(ctx, userIds);
        }
        final Cache cache = cacheService.getCache(REGION_NAME);
        final TIntObjectMap<User> map = new TIntObjectHashMap<>(userIds.length, 1);
        final List<Integer> toLoad = new ArrayList<>(userIds.length);
        final int contextId = ctx.getContextId();
        for (final int userId : userIds) {
            final Object object = cache.get(cacheService.newCacheKey(contextId, userId));
            if (object instanceof User user) {
                map.put(userId, user);
            } else {
                toLoad.add(I(userId));
            }
        }
        final User[] loaded = delegate.getUser(ctx, I2i(toLoad));
        for (final User user : loaded) {
            cache.put(cacheService.newCacheKey(contextId, user.getId()), user, false);
            map.put(user.getId(), user);
        }
        final List<User> retval = new ArrayList<>(userIds.length);
        for (final int userId : userIds) {
            retval.add(map.get(userId));
        }
        return retval.toArray(new User[retval.size()]);
    }

    @Override
    public User[] getGuestsCreatedBy(Connection connection, Context context, int userId) throws OXException {
        User[] loaded = delegate.getGuestsCreatedBy(connection, context, userId);
        CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService != null) {
            Cache cache = cacheService.getCache(REGION_NAME);
            for (User user : loaded) {
                cache.put(cacheService.newCacheKey(context.getContextId(), user.getId()), user, false);
            }
        }
        return loaded;
    }

    @Override
    protected void updateUserInternal(final Connection con, final User user, final Context context) throws OXException {
        // First try to detect some lousy client writing the same values all the time.
        boolean doUpdate = false;
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService != null) {
            final Cache cache = cacheService.getCache(REGION_NAME);
            final Object object = cache.get(cacheService.newCacheKey(context.getContextId(), user.getId()));
            if (object instanceof User oldUser) {
                // Okay, we still have that user in the cache. Now compare attributes.
                User differences = new UserMapper().getDifferences(oldUser, user);
                doUpdate = new UserMapper().getAssignedFields(differences).length != 0;
                // All attributes the same? Then check for changed user attributes.
                if (!doUpdate && null != user.getAttributes()) {
                    final Map<String, String> oldAttributes = oldUser.getAttributes();
                    final Map<String, String> attributes = user.getAttributes();
                    final Map<String, String> added = new HashMap<>();
                    final Map<String, String> removed = new HashMap<>();
                    final Map<String, ValuePair> changed = new HashMap<>();
                    RdbUserStorage.calculateDifferences(oldAttributes, attributes, added, removed, changed);
                    doUpdate = !added.isEmpty() || !removed.isEmpty() || !changed.isEmpty();
                }
                if (!doUpdate && oldUser.isGuest()) {
                    doUpdate |= null != user.getPasswordMech() && !user.getPasswordMech().equals(oldUser.getPasswordMech());
                    doUpdate |= null != user.getUserPassword() && !user.getUserPassword().equals(oldUser.getUserPassword());
                }
            } else {
                doUpdate = true;
            }
        } else {
            doUpdate = true;
        }
        if (doUpdate) {
            // Only update the user in the database if it differs from the one in the cache, the user is not cached or the service is not available.
            if (con == null) {
                delegate.updateUser(user, context);
            } else {
                delegate.updateUser(con, user, context);
            }
            invalidateUser(context, user.getId());
        }
    }

    @Override
    public String getUserAttribute(final String name, final int userId, final Context context) throws OXException {
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService == null) {
            return delegate.getUserAttribute(name, userId, context);
        }
        return getUser(userId, context).getAttributes().get(new StringBuilder("attr_").append(name).toString());
    }

    @Override
    public void setUserAttribute(final String name, final String value, final int userId, final Context context) throws OXException {
        delegate.setUserAttribute(name, value, userId, context);
        invalidateUserCache(context, userId);
    }

    @Override
    public void setAttribute(String name, String value, int userId, Context context) throws OXException {
        setAttribute(null, name, value, userId, context);
    }

    @Override
    public void setAttribute(Connection con, String name, String value, int userId, Context context) throws OXException {
        setAttribute(con, name, value, userId, context, true);
    }

    @Override
    public void setAttribute(Connection con, String name, String value, int userId, Context context, boolean invalidate) throws OXException {
        if (null == name) {
            throw LdapExceptionCode.UNEXPECTED_ERROR.create("Attribute name is null.").setPrefix("USR");
        }
        if (invalidate) {
            // Regular way
            if (null == con) {
                delegate.setAttribute(name, value, userId, context);
            } else {
                delegate.setAttribute(con, name, value, userId, context);
            }
            invalidateUserCache(context, userId);
        } else {
            // Special handling to avoid invalidations
            CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
            if (cacheService == null) {
                // Huh...?
                if (null == con) {
                    delegate.setAttribute(name, value, userId, context);
                } else {
                    delegate.setAttribute(con, name, value, userId, context);
                }
            } else {
                if (null == con) {
                    // Set attribute and obtain updated user
                    User updatedUser = delegate.setAttributeAndReturnUser(name, value, userId, context, true);

                    // Put into cache
                    Cache cache = cacheService.getCache(REGION_NAME);
                    CacheKey key = cacheService.newCacheKey(context.getContextId(), userId);
                    cache.put(key, updatedUser, false);
                } else {
                    // Set attribute
                    delegate.setAttribute(con, name, value, userId, context);

                    // Reload & put into cache
                    User updatedUser = delegate.getUser(context, userId, con);
                    Cache cache = cacheService.getCache(REGION_NAME);
                    CacheKey key = cacheService.newCacheKey(context.getContextId(), userId);
                    cache.put(key, updatedUser, false);
                }
            }
        }
    }

    @Override
    public int getUserId(final String uid, final Context context) throws OXException {
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (null == cacheService) {
            return delegate.getUserId(uid, context);
        }

        final Cache cache = cacheService.getCache(REGION_NAME);
        final CacheKey key = cache.newCacheKey(context.getContextId(), uid);
        int identifier = -1;
        Integer tmp;
        try {
            tmp = (Integer) cache.get(key);
        } catch (@SuppressWarnings("unused") final ClassCastException e) {
            tmp = null;
        }
        if (null == tmp) {
            LOG.trace("Cache MISS. Context: {} User: {}", I(context.getContextId()), uid);
            identifier = delegate.getUserId(uid, context);
            try {
                cache.put(key, Integer.valueOf(identifier), false);
            } catch (OXException e) {
                throw LdapExceptionCode.CACHE_PROBLEM.create(e, new Object[0]).setPrefix("USR");
            }
        } else {
            LOG.trace("Cache HIT. Context: {} User: {}", I(context.getContextId()), uid);
            identifier = tmp.intValue();
        }
        LogProperties.put(LogProperties.Name.LOGIN_RESOLVED_LOGIN, uid);
        return identifier;
    }

    @Override
    public int[] listModifiedUser(final Date modifiedSince, final Context context) throws OXException {
        // Caching doesn't make any sense here.
        return delegate.listModifiedUser(modifiedSince, context);
    }

    @Override
    public User searchUser(String email, Context context, boolean considerAliases, boolean includeGuests, boolean excludeUsers) throws OXException {
        return delegate.searchUser(email, context, considerAliases, includeGuests, excludeUsers);
    }

    @Override
    public User[] searchUserByMailLogin(final String login, final Context context) throws OXException {
        // Caching doesn't make any sense here.
        return delegate.searchUserByMailLogin(login, context);
    }

    @Override
    public User[] searchUserByName(final String name, final Context context, final int searchType) throws OXException {
        // Caching doesn't make any sense here.
        return delegate.searchUserByName(name, context, searchType);
    }

    @Override
    public int[] listAllUser(Connection con, Context context, boolean includeGuests, boolean excludeUsers) throws OXException {
        return delegate.listAllUser(con, context, includeGuests, excludeUsers);
    }

    @Override
    public int[] listAllUser(Connection con, int contextID, boolean includeGuests, boolean excludeUsers) throws OXException {
        return delegate.listAllUser(con, contextID, includeGuests, excludeUsers);
    }

    @Override
    public int[] resolveIMAPLogin(final String imapLogin, final Context context) throws OXException {
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (null == cacheService) {
            return delegate.resolveIMAPLogin(imapLogin, context);
        }
        try {
            final Cache cache = cacheService.getCache(REGION_NAME);
            final CacheKey key = cache.newCacheKey(context.getContextId(), new StringBuilder(imapLogin.length() + 1).append('~').append(imapLogin).toString());
            final int[] identifiers;
            int[] tmp;
            try {
                tmp = (int[]) cache.get(key);
            } catch (@SuppressWarnings("unused") final ClassCastException e) {
                tmp = null;
            }
            if (null == tmp) {
                identifiers = delegate.resolveIMAPLogin(imapLogin, context);
                try {
                    if (null != cache.get(key)) {
                        cache.remove(key);
                    }
                    cache.put(key, identifiers, false);
                } catch (OXException e) {
                    throw UserExceptionCode.CACHE_PROBLEM.create(e);
                }
            } else {
                identifiers = tmp;
            }
            return identifiers;
        } catch (OXException e) {
            throw UserExceptionCode.CACHE_PROBLEM.create(e);
        }
    }

    @Override
    public void invalidateUser(final Context ctx, final int userId) throws OXException {
        invalidateUserCache(ctx, userId);
        try {
            UserConfigurationStorage.getInstance().invalidateCache(userId, ctx);
        } catch (@SuppressWarnings("unused") final Exception e) {
            // Ignore
        }
    }

    private void invalidateUserCache(final Context ctx, final int userId) throws OXException {
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (null != cacheService) {
            try {
                final Cache cache = cacheService.getCache(REGION_NAME);
                cache.remove(cache.newCacheKey(ctx.getContextId(), userId));
            } catch (OXException e) {
                throw UserExceptionCode.CACHE_PROBLEM.create(e);
            }
        }
    }

    @Override
    protected void startInternal() {
        // Nothing to start
    }

    @Override
    protected void stopInternal() throws OXException {
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService != null) {
            cacheService.freeCache(REGION_NAME);
        }
    }

    UserStorage getDelegate() {
        return delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updatePasswordInternal(Connection connection, Context context, int userId, PasswordMech mech, String password, byte[] salt) throws OXException {
        delegate.updatePasswordInternal(connection, context, userId, mech, password, salt);
    }
}
