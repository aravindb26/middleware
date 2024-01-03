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

package com.openexchange.usecount.json.osgi;

import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.contact.ContactService;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.contactcollector.ContactCollectorService;
import com.openexchange.group.GroupService;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.principalusecount.PrincipalUseCountService;
import com.openexchange.resource.ResourceService;
import com.openexchange.usecount.json.UseCountActionFactory;
import com.openexchange.user.UserService;

/**
 * {@link UseCountJSONActivator} - Activator for the use-count JSON interface.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class UseCountJSONActivator extends AJAXModuleActivator {

    /**
     * Initializes a new {@link UseCountJSONActivator}.
     */
    public UseCountJSONActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected void startBundle() throws Exception {
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UseCountJSONActivator.class);
        trackService(PrincipalUseCountService.class);
        trackService(ObjectUseCountService.class);
        trackService(ContactCollectorService.class);
        trackService(ContactService.class);
        trackService(UserService.class);
        trackService(ResourceService.class);
        trackService(GroupService.class);
        trackService(IDBasedContactsAccessFactory.class);
        openTrackers();
        registerModule(new UseCountActionFactory(this), UseCountActionFactory.getModule());
        log.info("Bundle successfully started: {}", context.getBundle().getSymbolicName());
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        org.slf4j.LoggerFactory.getLogger(UseCountJSONActivator.class).info("Bundle stopped: {}", context.getBundle().getSymbolicName());
    }

}
