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

package com.openexchange.resource.json.manage.osgi;

import org.osgi.framework.BundleActivator;
import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.resource.ResourceService;
import com.openexchange.resource.json.manage.preferences.Module;
import com.openexchange.resource.json.manage.request.ResourceActionFactory;
import com.openexchange.server.ExceptionOnAbsenceServiceLookup;
import com.openexchange.user.UserService;

/**
 * {@link ResourceRequestActivator} - {@link BundleActivator Activator} for resource servlet.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ResourceRequestActivator extends AJAXModuleActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ResourceRequestActivator.class);

    /**
     * Initializes a new {@link ResourceRequestActivator}
     */
    public ResourceRequestActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ResourceService.class, UserService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            registerRequestHandler();
        } catch (Exception e) {
            LOG.error("", e);
            throw e;
        }
    }

    private void registerRequestHandler() {
        /*
         * Register request handler
         */
        registerModule(new ResourceActionFactory(new ExceptionOnAbsenceServiceLookup(this)), "resource");
        registerService(PreferencesItemService.class, new Module());
    }
}
