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

package com.openexchange.tools.webdav;

import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.context.ContextService;
import com.openexchange.login.listener.internal.LoginListenerRegistryImpl;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.tools.webdav.request.analyzer.DAVRequestAnalyzer;
import com.openexchange.user.UserService;

/**
 * {@link WebDAVUtilsActivator}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class WebDAVUtilsActivator extends HousekeepingActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { AuthenticationServiceRegistry.class, ContextService.class, UserService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        DAVRequestAnalyzer davRequestAnalyzer = new DAVRequestAnalyzer(context, this, WebDAVSessionStore.getInstance(), LoginListenerRegistryImpl.getInstance());
        track(DAVServletPathProvider.class, davRequestAnalyzer);
        openTrackers();
        registerService(RequestAnalyzer.class, davRequestAnalyzer);
    }

}
