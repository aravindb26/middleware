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

package com.openexchange.mail.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.mail.MailAuthenticator;
import com.openexchange.mail.MailAuthenticatorRegistry;

/**
 * Service tracker for mail authenticators.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public final class MailAuthenticatorServiceTracker implements ServiceTrackerCustomizer<MailAuthenticator, MailAuthenticator> {

    private final BundleContext context;

    /**
     * Initializes a new {@link MailAuthenticatorServiceTracker}
     */
    public MailAuthenticatorServiceTracker(BundleContext context) {
        super();
        this.context = context;
    }

    @Override
    public MailAuthenticator addingService(ServiceReference<MailAuthenticator> reference) {
        MailAuthenticator authenticator = context.getService(reference);
        MailAuthenticatorRegistry.getInstance().addMailAuthenticator(authenticator);
        return authenticator;
    }

    @Override
    public void modifiedService(ServiceReference<MailAuthenticator> reference, MailAuthenticator authenticator) {
        // Nothing to do
    }

    @Override
    public void removedService(ServiceReference<MailAuthenticator> reference, MailAuthenticator authenticator) {
        if (null != authenticator) {
            MailAuthenticatorRegistry.getInstance().removeMailAuthenticator(authenticator);
            context.ungetService(reference);
        }
    }

}
