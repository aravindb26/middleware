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

package com.openexchange.contact.account.service.impl.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.contact.account.service.impl.ContactsAccountServiceImpl;
import com.openexchange.contact.account.service.impl.groupware.ContactsAccountDeleteListener;
import com.openexchange.contact.provider.ContactsAccountService;
import com.openexchange.contact.provider.ContactsProviderRegistry;
import com.openexchange.contact.storage.ContactsStorageFactory;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.lock.LockService;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link ContactsAccountServiceActivator}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class ContactsAccountServiceActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(ContactsAccountServiceActivator.class);

    /**
     * Initializes a new {@link ContactsAccountServiceActivator}.
     */
    public ContactsAccountServiceActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {};
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class<?>[] { ContactsProviderRegistry.class, ContactsStorageFactory.class, ContextService.class, CapabilityService.class, DatabaseService.class, LockService.class };
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("Starting bundle {}", context.getBundle());
        trackService(ObjectUseCountService.class);
        openTrackers();
        registerService(ContactsAccountService.class, new ContactsAccountServiceImpl(this));
        registerService(DeleteListener.class, new ContactsAccountDeleteListener(this));
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("Stopping bundle {}", context.getBundle());
        super.stopBundle();
    }
}
