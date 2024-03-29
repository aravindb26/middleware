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

import com.openexchange.contact.provider.ContactsProviderRegistry;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link CompositingIDBasedContactsAccessFactory}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class CompositingIDBasedContactsAccessFactory implements IDBasedContactsAccessFactory {

    private final ContactsProviderRegistry providerRegistry;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link CompositingIDBasedContactsAccessFactory}.
     * 
     * @param providerRegistry The provider contacts provider registry to use
     * @param services A service lookup reference
     */
    public CompositingIDBasedContactsAccessFactory(ContactsProviderRegistry providerRegistry, ServiceLookup services) {
        super();
        this.providerRegistry = providerRegistry;
        this.services = services;
    }

    @Override
    public IDBasedContactsAccess createAccess(Session session) throws OXException {
        return new CompositingIDBasedContactsAccess(session, providerRegistry, services);
    }

    @Override
    public IDBasedContactsAccess createInternalAccess(Session session) throws OXException {
        return new InternalIDBasedContactsAccess(session, providerRegistry, services);
    }

}
