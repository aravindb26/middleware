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

package com.openexchange.subscribe.yahoo.osgi;

import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.context.ContextService;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthServiceMetaData;
import com.openexchange.oauth.yahoo.YahooService;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link Activator}
 *
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class Activator extends HousekeepingActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { OAuthService.class, ContextService.class, YahooService.class, FolderService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        // react dynamically to the appearance/disappearance of OAuthMetaDataService for MSN
        ServiceTracker<OAuthServiceMetaData, OAuthServiceMetaData> metaDataTracker = new ServiceTracker<OAuthServiceMetaData, OAuthServiceMetaData>(context, OAuthServiceMetaData.class, new OAuthServiceMetaDataRegisterer(context, this));
        rememberTracker(metaDataTracker);
        openTrackers();
    }
}
