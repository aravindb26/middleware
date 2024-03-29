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

package com.openexchange.tools.oxfolder;

import java.rmi.RemoteException;
import com.openexchange.auth.AbstractAuthenticatorRMIService;
import com.openexchange.auth.Authenticator;
import com.openexchange.auth.Credentials;
import com.openexchange.exception.OXException;
import com.openexchange.gab.GABMode;
import com.openexchange.gab.GABRestorerRMIService;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link GABRestorerRMIServiceImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class GABRestorerRMIServiceImpl extends AbstractAuthenticatorRMIService implements GABRestorerRMIService {

    @Override
    public void restoreDefaultPermissions(final int cid, GABMode gabMode, Credentials auth) throws RemoteException {
        Authenticator authenticator = ServerServiceRegistry.getInstance().getService(Authenticator.class);
        authenticate(authenticator, auth);

        try {
            new OXFolderAdminHelper().restoreDefaultGlobalAddressBookPermissions(cid, gabMode, OXFolderProperties.isEnableInternalUsersEdit());
        } catch (OXException e) {
            final String message = e.getMessage();
            org.slf4j.LoggerFactory.getLogger(GABRestorerRMIServiceImpl.class).error(message, e);
            final Exception wrapMe = new Exception(message);
            wrapMe.setStackTrace(e.getStackTrace());
            throw new RemoteException(message, wrapMe);
        }
    }
}
