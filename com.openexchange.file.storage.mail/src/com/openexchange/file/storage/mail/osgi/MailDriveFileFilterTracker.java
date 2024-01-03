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


package com.openexchange.file.storage.mail.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.file.storage.mail.MailDriveFileFilters;
import com.openexchange.file.storage.mail.filter.MailDriveFileFilter;


/**
 * {@link MailDriveFileFilterTracker} - The service tracker for mail drive file filter.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class MailDriveFileFilterTracker implements ServiceTrackerCustomizer<MailDriveFileFilter, MailDriveFileFilter> {

    private final BundleContext context;

    /**
     * Initializes a new {@link MailDriveFileFilterTracker}.
     *
     * @param context The bundle context
     */
    public MailDriveFileFilterTracker(BundleContext context) {
        super();
        this.context = context;
    }

    @Override
    public MailDriveFileFilter addingService(ServiceReference<MailDriveFileFilter> reference) {
        MailDriveFileFilter filter = context.getService(reference);
        if (MailDriveFileFilters.registerMailDriveFileFilter(filter)) {
            return filter;
        }

        context.ungetService(reference);
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<MailDriveFileFilter> reference, MailDriveFileFilter filter) {
        // Ignore
    }

    @Override
    public void removedService(ServiceReference<MailDriveFileFilter> reference, MailDriveFileFilter filter) {
        MailDriveFileFilters.unregisterMailDriveFileFilter(filter);
        context.ungetService(reference);
    }

}
