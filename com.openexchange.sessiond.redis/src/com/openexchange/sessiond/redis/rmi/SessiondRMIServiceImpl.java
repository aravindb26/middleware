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

package com.openexchange.sessiond.redis.rmi;

import static com.openexchange.java.Autoboxing.I;
import java.rmi.RemoteException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.auth.AbstractAuthenticatorRMIService;
import com.openexchange.auth.Authenticator;
import com.openexchange.auth.Credentials;
import com.openexchange.exception.OXException;
import com.openexchange.sessiond.redis.RedisSessiondService;
import com.openexchange.sessiond.redis.osgi.Services;
import com.openexchange.sessiond.rmi.SessiondRMIService;

/**
 * {@link SessiondRMIServiceImpl} - The sessiond RMI interface implementation using Redis session storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class SessiondRMIServiceImpl extends AbstractAuthenticatorRMIService implements SessiondRMIService {

    private static final Logger LOG = LoggerFactory.getLogger(SessiondRMIServiceImpl.class);

    private final RedisSessiondService sessiondService;

    /**
     * Initializes a new {@link SessiondRMIServiceImpl}.
     *
     * @param sessiondService The sessiond service
     */
    public SessiondRMIServiceImpl(RedisSessiondService sessiondService) {
        super();
        this.sessiondService = sessiondService;
    }

    @Override
    public boolean clearUserSession(String sessionId, Credentials auth) throws RemoteException {
        return clearUserSession(sessionId, false, auth);
    }

    @Override
    public boolean clearUserSession(String sessionId, boolean global, Credentials auth) throws RemoteException {
        Authenticator authenticator = Services.optService(Authenticator.class);
        authenticate(authenticator, auth);

        return sessiondService.removeSession(sessionId); // always global
    }

    @Override
    public void clearContextSessions(int contextId, Credentials auth) throws RemoteException {
        Authenticator authenticator = Services.optService(Authenticator.class);
        authenticate(authenticator, auth);

        sessiondService.removeContextSessions(contextId); // always global
    }

    @Override
    public void clearContextSessionsGlobally(int contextId, Credentials auth) throws RemoteException {
        Authenticator authenticator = Services.optService(Authenticator.class);
        authenticate(authenticator, auth);

        sessiondService.removeContextSessions(contextId); // always global
    }

    @Override
    public void clearContextSessionsGlobally(Set<Integer> contextIds, Credentials auth) throws RemoteException {
        Authenticator authenticator = Services.optService(Authenticator.class);
        authenticate(authenticator, auth);

        try {
            sessiondService.removeContextSessionsGlobal(contextIds); // always global
        } catch (OXException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public int clearUserSessions(int userId, int contextId, Credentials auth) throws RemoteException {
        Authenticator authenticator = Services.optService(Authenticator.class);
        authenticate(authenticator, auth);

        try {
            return sessiondService.removeAndReturnUserSessions(userId, contextId, true).size();
        } catch (OXException e) {
            LOG.warn("Failed to remove sessions for user {} in context {} from Redis session storage", I(userId), I(contextId), e);
            return 0;
        }
    }

    @Override
    public void clearUserSessionsGlobally(int userId, int contextId, Credentials auth) throws RemoteException {
        Authenticator authenticator = Services.optService(Authenticator.class);
        authenticate(authenticator, auth);

        try {
            sessiondService.removeUserSessionsGlobally(userId, contextId);
        } catch (Exception e) {
            LOG.error("", e);
            String message = e.getMessage();
            throw new RemoteException(message, new Exception(message));
        }
    }

    @Override
    public void clearSessionStorage(Credentials auth) throws RemoteException {
        Authenticator authenticator = Services.optService(Authenticator.class);
        authenticate(authenticator, auth);

        sessiondService.removeAllSessions();
    }
}
