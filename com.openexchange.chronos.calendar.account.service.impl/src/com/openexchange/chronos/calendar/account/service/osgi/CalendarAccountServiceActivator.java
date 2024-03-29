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

package com.openexchange.chronos.calendar.account.service.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.chronos.calendar.account.service.groupware.CalendarAccountDeleteListener;
import com.openexchange.chronos.calendar.account.service.impl.CalendarAccountServiceImpl;
import com.openexchange.chronos.provider.CalendarProviderRegistry;
import com.openexchange.chronos.provider.account.AdministrativeCalendarAccountService;
import com.openexchange.chronos.provider.account.CalendarAccountService;
import com.openexchange.chronos.storage.CalendarStorageFactory;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.context.ContextService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.database.DatabaseService;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.secret.SecretEncryptionFactoryService;
import com.openexchange.secret.SecretService;
import com.openexchange.secret.recovery.EncryptedItemCleanUpService;
import com.openexchange.secret.recovery.EncryptedItemDetectorService;
import com.openexchange.secret.recovery.SecretMigrator;
import com.openexchange.threadpool.ThreadPoolService;

/**
 * {@link CalendarAccountServiceActivator}
 *
 * @author <a href="mailto:Jan-Oliver.Huhn@open-xchange.com">Jan-Oliver Huhn</a>
 * @since v7.10.0
 */
public class CalendarAccountServiceActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(CalendarAccountServiceActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ContextService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class<?>[] { CalendarProviderRegistry.class, DatabaseService.class, CalendarStorageFactory.class, CapabilityService.class, ConfigViewFactory.class, 
            SecretEncryptionFactoryService.class, CryptoService.class, SecretService.class, ThreadPoolService.class };
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LOG.info("starting bundle {}", context.getBundle());
            CalendarAccountServiceImpl accountService = new CalendarAccountServiceImpl(this);
            registerService(CalendarAccountService.class, accountService);
            registerService(AdministrativeCalendarAccountService.class, accountService);
            registerService(SecretMigrator.class, accountService);
            registerService(EncryptedItemDetectorService.class, accountService);
            registerService(EncryptedItemCleanUpService.class, accountService);
            registerService(DeleteListener.class, new CalendarAccountDeleteListener(this));
            openTrackers();
        } catch (Exception e) {
            LOG.error("error starting {}", context.getBundle(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("stopping bundle {}", context.getBundle());
        super.stopBundle();
    }

}
