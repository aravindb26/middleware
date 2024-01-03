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
package com.openexchange.resource.internal;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceGroup;
import com.openexchange.resource.ResourcePermission;
import com.openexchange.resource.SchedulingPrivilege;


/**
 * {@link CachingResourceStorage} is a {@link ExtendedResourceStorage} which caches the resources
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8
 */
public class CachingResourceStorage implements ExtendedResourceStorage, CachingAwareResourceStorage {

    private final ExtendedResourceStorage delegate;
    private final Cache cache;
    private final CacheService cacheService;

    /**
     * Initializes a new {@link CachingResourceStorage}.
     *
     * @param delegate The delegating resource storage
     * @param cacheService The cache service
     * @throws OXException
     */
    public CachingResourceStorage(ExtendedResourceStorage delegate, CacheService cacheService) throws OXException {
        super();
        this.delegate = delegate;
        this.cacheService = cacheService;
        cache = cacheService.getCache(CACHE_REGION_NAME);
    }

    @Override
    public ResourceGroup getGroup(int groupId, Context context) throws OXException {
        return delegate.getGroup(groupId, context);
    }

    @Override
    public ResourceGroup[] getGroups(Context context) throws OXException {
        return delegate.getGroups(context);
    }

    @Override
    public Resource getResource(int resourceId, Context context) throws OXException {
        CacheKey key = toKey(context, resourceId);
        Resource result = (Resource) cache.get(key);
        if (result != null) {
            return (Resource) result.clone();
        }
        result = delegate.getResource(resourceId, context);
        cache.put(key, (Resource) result.clone(), false);
        return result;
    }

    @Override
    public Resource getResource(int resourceId, Context context, Connection connection) throws OXException {
        CacheKey key = toKey(context, resourceId);
        Resource result = (Resource) cache.get(key);
        if (result != null) {
            return (Resource) result.clone();
        }
        result = delegate.getResource(resourceId, context, connection);
        cache.put(key, (Resource) result.clone(), false);
        return result;
    }

    @Override
    public ResourceGroup[] searchGroups(String pattern, Context context) throws OXException {
        return delegate.searchGroups(pattern, context);
    }

    @Override
    public Resource[] searchResources(String pattern, Context context) throws OXException {
        if (pattern.equals(SEARCH_PATTERN_ALL)) {
            // Use cache in case of all pattern
            CacheKey key = toAllKey(context);
            Integer[] ids = (Integer[]) cache.get(key);
            if (ids != null) {
                return loadResources(context, ids);
            }
            Resource[] result = delegate.searchResources(pattern, context);
            Integer[] cacheArray = Arrays.asList(result)
                                         .stream()
                                         .map(res -> I(res.getIdentifier()))
                                         .toArray(size -> new Integer[size]);
            cache.put(key, cacheArray, false);
            return result;
        }

        return delegate.searchResources(pattern, context);
    }

    @Override
    public Resource getByIdentifier(String identifier, Context context) throws OXException {
        return delegate.getByIdentifier(identifier, context);
    }

    @Override
    public Resource getByMail(String mail, Context context) throws OXException {
        return delegate.getByMail(mail, context);
    }

    @Override
    public Resource[] searchResourcesByMail(String pattern, Context context) throws OXException {
        return delegate.searchResourcesByMail(pattern, context);
    }

    @Override
    public Resource[] searchResourcesByPrivilege(int[] entities, SchedulingPrivilege privilege, Context context) throws OXException {
        return delegate.searchResourcesByPrivilege(entities, privilege, context);
    }

    @Override
    public Resource[] listModified(Date modifiedSince, Context context) throws OXException {
        return delegate.listModified(modifiedSince, context);
    }

    @Override
    public Resource[] listDeleted(Date modifiedSince, Context context) throws OXException {
        return delegate.listDeleted(modifiedSince, context);
    }

    @Override
    public void insertResource(Context ctx, Connection con, Resource resource, StorageType type) throws OXException {
        delegate.insertResource(ctx, con, resource, type);
        cache.remove(toKey(ctx, resource.getIdentifier()));
        invalidateAllKey(ctx);
    }

    @Override
    public void updateResource(Context ctx, Connection con, Resource resource) throws OXException {
        delegate.updateResource(ctx, con, resource);
        cache.remove(toKey(ctx, resource.getIdentifier()));
    }

    @Override
    public void deleteResourceById(Context ctx, Connection con, int resourceId) throws OXException {
        delegate.deleteResourceById(ctx, con, resourceId);
        cache.remove(toKey(ctx, resourceId));
        invalidateAllKey(ctx);
    }

    // ------------------------------ User based methods ------------------------

    @Override
    public Resource[] searchResources(String pattern, Context context, int userId) throws OXException {
        return delegate.searchResources(pattern, context, userId);
    }

    @Override
    public Resource[] searchResourcesByMail(String pattern, Context context, int userId) throws OXException {
        return delegate.searchResourcesByMail(pattern, context, userId);
    }

    // ----------------------------- Caching methods ---------------------------

    @Override
    public void invalidate(Context context, int resourceId) throws OXException {
        cache.remove(toKey(context, resourceId));
        invalidateAllKey(context);
    }

    /**
     * Creates a new cache key
     *
     * @param ctx The context
     * @param resourceId The resource id
     * @return The cache key
     */
    private CacheKey toKey(Context ctx, int resourceId) {
        return cacheService.newCacheKey(ctx.getContextId(), resourceId);
    }

    /**
     * Creates a new cache key for the all entry
     *
     * @param ctx The context
     * @return The cache key
     */
    private CacheKey toAllKey(Context ctx) {
        return cacheService.newCacheKey(ctx.getContextId(), SEARCH_PATTERN_ALL);
    }

    /**
     * Invalidates the all entry of the given context
     *
     * @param ctx The context
     * @throws OXException
     */
    private void invalidateAllKey(Context ctx) throws OXException {
        cache.remove(toAllKey(ctx));
    }

    // ------------------ extended methods --------------------

    @Override
    public List<Resource> getResourceIdsWithPermissionsForEntity(Context ctx, Connection con, int entity, boolean group) throws OXException {
        return delegate.getResourceIdsWithPermissionsForEntity(ctx, con, entity, group);
    }

    @Override
    public int deletePermissions(Context ctx, Connection connection, int resourceId) throws OXException {
        int result = delegate.deletePermissions(ctx, connection, resourceId);
        cache.remove(toKey(ctx, resourceId));
        invalidateAllKey(ctx);
        return result;
    }

    @Override
    public int[] insertPermissions(Context ctx, Connection connection, int resourceId, ResourcePermission[] permissions) throws OXException {
        int[] result = delegate.insertPermissions(ctx, connection, resourceId, permissions);
        cache.remove(toKey(ctx, resourceId));
        invalidateAllKey(ctx);
        return result;
    }

    // ---------------------------- helper methods -----------------

    /**
     * Loads the given resources
     *
     * @param context The context to load the resources from
     * @param ids The resource ids
     * @return The resources
     * @throws OXException
     */
    private Resource[] loadResources(Context context, Integer[] ids) throws OXException {
        Resource[] result = new Resource[ids.length];
        for (int x = 0; x < result.length; x++) {
            result[x] = getResource(i(ids[x]), context);
        }
        return result;
    }

}
