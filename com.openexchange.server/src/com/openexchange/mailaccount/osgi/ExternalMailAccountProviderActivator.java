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

package com.openexchange.mailaccount.osgi;

import com.openexchange.external.account.ExternalAccountProvider;
import com.openexchange.jslob.JSlobEntry;
import com.openexchange.mailaccount.AllowExternalSMTPJSlobEntry;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.internal.MailExternalAccountProvider;
import com.openexchange.oauth.OAuthAccountStorage;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link ExternalMailAccountProviderActivator}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.4
 */
public class ExternalMailAccountProviderActivator extends HousekeepingActivator {

    /**
     * Initialises a new {@link ExternalMailAccountProviderActivator}.
     */
    public ExternalMailAccountProviderActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { MailAccountStorageService.class, OAuthAccountStorage.class };
    }

    @Override
    protected void startBundle() throws Exception {
        registerService(JSlobEntry.class, new AllowExternalSMTPJSlobEntry());
        registerService(ExternalAccountProvider.class, new MailExternalAccountProvider(this));
    }
}
