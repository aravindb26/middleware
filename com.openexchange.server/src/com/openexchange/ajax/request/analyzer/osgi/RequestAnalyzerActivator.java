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

package com.openexchange.ajax.request.analyzer.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.request.analyzer.AutoLoginRequestAnalyzer;
import com.openexchange.ajax.request.analyzer.CookieRequestAnalyzer;
import com.openexchange.ajax.request.analyzer.LoginRequestAnalyzer;
import com.openexchange.ajax.request.analyzer.SessionRequestAnalyzer;
import com.openexchange.ajax.requesthandler.Dispatcher;
import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.login.listener.internal.LoginListenerRegistryImpl;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.user.UserService;

/**
 * {@link RequestAnalyzerActivator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class RequestAnalyzerActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAnalyzerActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class, DispatcherPrefixService.class, SessiondService.class, Dispatcher.class, ObfuscatorService.class, UserService.class, ContextService.class, AuthenticationServiceRegistry.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LOG.info("starting bundle {}", context.getBundle());
            LoginListenerRegistryImpl loginListenerRegistry = LoginListenerRegistryImpl.getInstance();
            registerService(RequestAnalyzer.class, new SessionRequestAnalyzer(this), 1);
            registerService(RequestAnalyzer.class, new CookieRequestAnalyzer(this));
            registerService(RequestAnalyzer.class, new AutoLoginRequestAnalyzer(this));
            registerService(RequestAnalyzer.class, new LoginRequestAnalyzer(this, loginListenerRegistry), 1);
        } catch (Exception e) {
            LOG.error("error starting {}", context.getBundle(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("stopping bundle {}", context.getBundle());
        super.stopBundle();
    }

}
