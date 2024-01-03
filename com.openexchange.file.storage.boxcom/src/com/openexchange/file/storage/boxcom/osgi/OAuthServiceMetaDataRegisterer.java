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

package com.openexchange.file.storage.boxcom.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.file.storage.FileStorageAccountManagerProvider;
import com.openexchange.file.storage.oauth.osgi.AbstractCloudStorageOAuthServiceMetaDataRegisterer;
import com.openexchange.oauth.KnownApi;
import com.openexchange.server.ServiceLookup;

/**
 * {@link OAuthServiceMetaDataRegisterer}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class OAuthServiceMetaDataRegisterer extends AbstractCloudStorageOAuthServiceMetaDataRegisterer {

    /**
     * Initializes a new {@link OAuthServiceMetaDataRegisterer}.
     *
     * @param context The bundle context
     */
    public OAuthServiceMetaDataRegisterer(BundleContext context, ServiceLookup services) {
        super(context, services, KnownApi.BOX_COM.getServiceId());
    }

    @Override
    protected ServiceTrackerCustomizer<FileStorageAccountManagerProvider, FileStorageAccountManagerProvider> getRegisterer() {
        return new BoxServiceRegisterer(getContext(), getServiceLookup());
    }
}
