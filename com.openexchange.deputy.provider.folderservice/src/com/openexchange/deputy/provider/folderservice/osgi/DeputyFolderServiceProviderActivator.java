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

package com.openexchange.deputy.provider.folderservice.osgi;

import org.slf4j.Logger;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.deputy.DeputyModuleProvider;
import com.openexchange.deputy.KnownModuleId;
import com.openexchange.deputy.provider.folderservice.DeputyFolderServiceProvider;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.group.GroupService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.user.UserService;

/**
 * {@link DeputyFolderServiceProviderActivator} - Activator for deputy folder service provider.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyFolderServiceProviderActivator extends HousekeepingActivator {

    /** The logger constant */
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DeputyFolderServiceProviderActivator.class);

    /**
     * Initializes a new {@link DeputyFolderServiceProviderActivator}.
     */
    public DeputyFolderServiceProviderActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class, FolderService.class, ConfigurationService.class, ConfigViewFactory.class,
            ContextService.class, UserService.class, GroupService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("Starting bundle {}", context.getBundle().getSymbolicName());

        // Provider for calendar
        {
            DeputyFolderServiceProvider provider = new DeputyFolderServiceProvider(KnownModuleId.CALENDAR, this);
            registerService(DeputyModuleProvider.class, provider);
        }

        // Provider for contacts
        {
            DeputyFolderServiceProvider provider = new DeputyFolderServiceProvider(KnownModuleId.CONTACTS, this);
            registerService(DeputyModuleProvider.class, provider);
        }

        // Provider for tasks
        {
            DeputyFolderServiceProvider provider = new DeputyFolderServiceProvider(KnownModuleId.TASKS, this);
            registerService(DeputyModuleProvider.class, provider);
        }

        // Provider for Drive/InfoStore
        {
            DeputyFolderServiceProvider provider = new DeputyFolderServiceProvider(KnownModuleId.DRIVE, this);
            registerService(DeputyModuleProvider.class, provider);
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        LOG.info("Stopped bundle {}", context.getBundle().getSymbolicName());
    }

}
