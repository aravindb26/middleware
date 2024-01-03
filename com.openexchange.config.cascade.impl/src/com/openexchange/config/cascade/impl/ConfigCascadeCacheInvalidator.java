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

package com.openexchange.config.cascade.impl;

import java.io.Serializable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.events.CacheEvent;
import com.openexchange.caching.events.CacheEventService;
import com.openexchange.caching.events.CacheListener;
import com.openexchange.caching.events.CacheOperation;
import com.openexchange.java.Strings;

/**
 * {@link ConfigCascadeCacheInvalidator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ConfigCascadeCacheInvalidator implements CacheListener, ServiceTrackerCustomizer<CacheEventService, CacheEventService> {

    private final BundleContext context;

    /**
     * Initializes a new {@link ConfigCascadeCacheInvalidator}.
     */
    public ConfigCascadeCacheInvalidator(BundleContext context) {
        super();
        this.context = context;
    }

    // -------------------------------------------------------------------------------------------------------------------------- //

    @Override
    public void onEvent(Object sender, CacheEvent cacheEvent, boolean fromRemote) {
        if ("User".equals(cacheEvent.getRegion())) {
            CacheOperation operation = cacheEvent.getOperation();
            if (operation == CacheOperation.CLEAR) {
                ConfigCascade.clearCachedValues();
                return;
            }
            if (null != cacheEvent.getKeys()) {
                for (Serializable key : cacheEvent.getKeys()) {
                    if ((key instanceof CacheKey)) {
                        CacheKey cacheKey = (CacheKey) key;
                        if (CacheOperation.INVALIDATE == operation) {
                            String[] keys = cacheKey.getKeys();
                            if (null != keys && 0 < keys.length && null != keys[0]) {
                                int userId = Strings.parseUnsignedInt(keys[0]);
                                if (userId > 0) {
                                    ConfigCascade.clearCachedValuesOfUser(userId, cacheKey.getContextId());
                                }
                            }
                        } else if (CacheOperation.INVALIDATE_GROUP == operation) {
                            ConfigCascade.clearCachedValuesOfContext(cacheKey.getContextId());
                        }
                    }
                }
            }
        } else if ("Context".equals(cacheEvent.getRegion())) {
            CacheOperation operation = cacheEvent.getOperation();
            if (operation == CacheOperation.CLEAR) {
                ConfigCascade.clearCachedValues();
                return;
            }
            if (null != cacheEvent.getKeys()) {
                for (Serializable key : cacheEvent.getKeys()) {
                    if ((key instanceof Integer)) {
                        Integer iContextId = (Integer) key;
                        if (CacheOperation.INVALIDATE == operation) {
                            ConfigCascade.clearCachedValuesOfContext(iContextId.intValue());
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------- //

    @Override
    public CacheEventService addingService(ServiceReference<CacheEventService> reference) {
        CacheEventService service = context.getService(reference);
        service.addListener("User", this);
        service.addListener("Context", this);
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<CacheEventService> reference, CacheEventService service) {
        // Nothing to do
    }

    @Override
    public void removedService(ServiceReference<CacheEventService> reference, CacheEventService service) {
        service.removeListener("Context", this);
        service.removeListener("User", this);
    }
}
