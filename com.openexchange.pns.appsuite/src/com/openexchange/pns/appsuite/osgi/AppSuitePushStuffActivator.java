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

package com.openexchange.pns.appsuite.osgi;

import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.pns.PushMessageGenerator;
import com.openexchange.pns.appsuite.AppSuiteMessageGenerator;
import com.openexchange.pns.appsuite.AppSuiteWebSocketToClientResolver;
import com.openexchange.pns.transport.websocket.WebSocketToClientResolver;
import com.openexchange.user.UserService;

/**
 * {@link AppSuitePushStuffActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class AppSuitePushStuffActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link AppSuitePushStuffActivator}.
     */
    public AppSuitePushStuffActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {};
    }

    @Override
    protected void startBundle() throws Exception {
        trackService(UserService.class);
        openTrackers();
        registerService(PushMessageGenerator.class, new AppSuiteMessageGenerator(this));
        registerService(WebSocketToClientResolver.class, new AppSuiteWebSocketToClientResolver());
    }

}
