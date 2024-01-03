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

package com.openexchange.contact.provider.composition.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.openexchange.contact.provider.AutoProvisioningContactsProvider;
import com.openexchange.contact.provider.ContactsProvider;
import com.openexchange.contact.provider.ContactsProviderRegistry;
import com.openexchange.osgi.ServiceListing;

/**
 * {@link ContactsProviderRegistryImpl}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class ContactsProviderRegistryImpl implements ContactsProviderRegistry {

    private final ServiceListing<ContactsProvider> contactProviders;

    /**
     * Initializes a new {@link ContactsProviderRegistryImpl}.
     * 
     * @param contactProviders The contact providers service list
     */
    public ContactsProviderRegistryImpl(ServiceListing<ContactsProvider> contactProviders) {
        super();
        this.contactProviders = contactProviders;
    }

    @Override
    public Optional<ContactsProvider> getContactProvider(String id) {
        return contactProviders.getServiceList().stream().filter(provider -> id.equals(provider.getId())).findFirst();
    }

    @Override
    public List<ContactsProvider> getContactProviders() {
        return contactProviders.getServiceList();
    }

    @Override
    public List<AutoProvisioningContactsProvider> getAutoProvisioningContactsProviders() {
        return getContactProviders().stream().filter(provider -> (provider instanceof AutoProvisioningContactsProvider)).map(AutoProvisioningContactsProvider.class::cast).collect(Collectors.toList());
    }
}
