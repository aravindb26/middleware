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

package com.openexchange.resource.internal.osgi;

import com.openexchange.caching.CacheService;
import com.openexchange.database.CreateTableService;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.groupware.update.DefaultUpdateTaskProviderService;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.resource.ResourceService;
import com.openexchange.resource.internal.CachingResourceStorage;
import com.openexchange.resource.internal.CreateResourcePermissionsTable;
import com.openexchange.resource.internal.CreateResourcePermissionsTableTask;
import com.openexchange.resource.internal.EntityDeleteListener;
import com.openexchange.resource.internal.ExtendedResourceStorage;
import com.openexchange.resource.internal.RdbResourceStorage;
import com.openexchange.resource.internal.ResourceServiceImpl;
import com.openexchange.resource.storage.ResourceStorage;

/**
 * {@link ResourceActivator}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.2
 */
public class ResourceActivator extends HousekeepingActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { CacheService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        registerService(CreateTableService.class, new CreateResourcePermissionsTable());
        registerService(UpdateTaskProviderService.class, new DefaultUpdateTaskProviderService(new CreateResourcePermissionsTableTask()));
        ResourceServiceImpl service = new ResourceServiceImpl();
        registerService(ResourceService.class, service);
        RdbResourceStorage resourceStorage = new RdbResourceStorage();
        ExtendedResourceStorage cachingResourceStorage = new CachingResourceStorage(resourceStorage, getServiceSafe(CacheService.class));
        registerService(ResourceStorage.class, cachingResourceStorage);
        registerService(DeleteListener.class, new EntityDeleteListener(cachingResourceStorage));
    }

}
