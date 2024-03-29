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

package com.openexchange.subscribe.dav.gmx.de.osgi;

import com.openexchange.folderstorage.FolderService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.subscribe.SubscribeService;
import com.openexchange.subscribe.dav.gmx.de.GmxDeSubscribeService;

/**
 * {@link GmxDeSubscribeActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class GmxDeSubscribeActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link GmxDeSubscribeActivator}.
     */
    public GmxDeSubscribeActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { HttpClientService.class, FolderService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        GmxDeSubscribeService subscribeService = new GmxDeSubscribeService(this);
        registerService(SubscribeService.class, subscribeService);
    }

}
