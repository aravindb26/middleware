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

package com.openexchange.server.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.mail.mime.crypto.CryptoMailRecognizer;
import com.openexchange.mail.mime.crypto.impl.CryptoMailRecognizerRegistry;

/**
 * {@link CryptoRecognizerTracker}
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.6
 */
public class CryptoRecognizerTracker implements ServiceTrackerCustomizer<CryptoMailRecognizer, CryptoMailRecognizer> {

    private final BundleContext context;

    public CryptoRecognizerTracker(final BundleContext context) {
        this.context = context;
    }

    @Override
    public CryptoMailRecognizer addingService(ServiceReference<CryptoMailRecognizer> serviceReference) {
        final CryptoMailRecognizer service = context.getService(serviceReference);
        if (CryptoMailRecognizerRegistry.getInstance().addRecognizer(service)) {
            return service;
        }
        // Nothing to track
        context.ungetService(serviceReference);
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<CryptoMailRecognizer> reference, CryptoMailRecognizer service) {
        // Nothing to do

    }

    @Override
    public void removedService(ServiceReference<CryptoMailRecognizer> serviceReference, CryptoMailRecognizer service) {
        if (null != service) {
            CryptoMailRecognizerRegistry.getInstance().removeRecognizer(service);
            context.ungetService(serviceReference);
        }

    }

}
