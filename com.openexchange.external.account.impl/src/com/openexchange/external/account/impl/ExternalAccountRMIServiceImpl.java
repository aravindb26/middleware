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

package com.openexchange.external.account.impl;

import java.rmi.RemoteException;
import java.util.List;
import com.openexchange.auth.AbstractAuthenticatorRMIService;
import com.openexchange.auth.Authenticator;
import com.openexchange.auth.Credentials;
import com.openexchange.exception.OXException;
import com.openexchange.external.account.ExternalAccount;
import com.openexchange.external.account.ExternalAccountModule;
import com.openexchange.external.account.ExternalAccountRMIService;
import com.openexchange.external.account.ExternalAccountService;
import com.openexchange.external.account.impl.osgi.ExternalAccountActivator;
import com.openexchange.server.ServiceLookup;

/**
 * {@link ExternalAccountRMIServiceImpl}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.4
 */
public class ExternalAccountRMIServiceImpl extends AbstractAuthenticatorRMIService implements ExternalAccountRMIService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ExternalAccountRMIServiceImpl.class);

    private final ExternalAccountService externalAccountService;

    private final ServiceLookup services;

    /**
     * Initializes a new {@link ExternalAccountRMIServiceImpl}.
     *
     * @param service The external account service
     * @param externalAccountActivator
     */
    public ExternalAccountRMIServiceImpl(ExternalAccountService externalAccountService, ExternalAccountActivator services) {
        super();
        this.externalAccountService = externalAccountService;
        this.services = services;
    }

    @Override
    public List<ExternalAccount> list(int contextId) throws RemoteException {
        try {
            return externalAccountService.list(contextId);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    public List<ExternalAccount> list(int contextId, int userId) throws RemoteException {
        try {
            return externalAccountService.list(contextId, userId);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    public List<ExternalAccount> list(int contextId, String providerId) throws RemoteException {
        try {
            return externalAccountService.list(contextId, providerId);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    public List<ExternalAccount> list(int contextId, ExternalAccountModule module) throws RemoteException {
        try {
            return externalAccountService.list(contextId, module);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    public List<ExternalAccount> list(int contextId, int userId, ExternalAccountModule module) throws RemoteException {
        try {
            return externalAccountService.list(contextId, userId, module);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    public List<ExternalAccount> list(int contextId, int userId, String providerId) throws RemoteException {
        try {
            return externalAccountService.list(contextId, userId, providerId);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    public List<ExternalAccount> list(int contextId, String providerId, ExternalAccountModule module) throws RemoteException {
        try {
            return externalAccountService.list(contextId, providerId, module);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    public List<ExternalAccount> list(int contextId, int userId, String providerId, ExternalAccountModule module) throws RemoteException {
        try {
            return externalAccountService.list(contextId, userId, providerId, module);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    public boolean delete(int id, int contextId, int userId, ExternalAccountModule module, Credentials auth) throws RemoteException {
        Authenticator authenticator = services.getOptionalService(Authenticator.class);
        authenticate(authenticator, auth, contextId);

        try {
            return externalAccountService.delete(id, contextId, userId, module);
        } catch (OXException e) {
            LOG.error("", e);
            throw new RemoteException(e.getPlainLogMessage(), e);
        } catch (RuntimeException | Error e) {
            LOG.error("", e);
            throw e;
        }
    }
}
