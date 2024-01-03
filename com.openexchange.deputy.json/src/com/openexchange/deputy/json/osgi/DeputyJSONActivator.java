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

package com.openexchange.deputy.json.osgi;

import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.deputy.DeputyService;
import com.openexchange.deputy.json.DeputyActionFactory;
import com.openexchange.deputy.json.converter.DeputyJSONResultConverter;
import com.openexchange.deputy.json.converter.GrantedDeputyJSONResultConverter;

/**
 * {@link DeputyJSONActivator} - Activator for the deputy JSON interface.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyJSONActivator extends AJAXModuleActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DeputyService.class, CapabilityService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeputyJSONActivator.class);
        registerModule(new DeputyActionFactory(this), DeputyActionFactory.getModule());
        registerService(ResultConverter.class, new DeputyJSONResultConverter());
        registerService(ResultConverter.class, new GrantedDeputyJSONResultConverter());
        log.info("Bundle successfully started: com.openexchange.deputy.json");
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        org.slf4j.LoggerFactory.getLogger(DeputyJSONActivator.class).info("Bundle stopped: com.openexchange.deputy.json");
    }
}
