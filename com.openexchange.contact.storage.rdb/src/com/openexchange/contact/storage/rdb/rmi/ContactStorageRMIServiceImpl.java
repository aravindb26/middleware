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

package com.openexchange.contact.storage.rdb.rmi;

import static com.openexchange.java.Autoboxing.I;
import java.rmi.RemoteException;
import java.util.Collection;
import org.slf4j.Logger;
import com.openexchange.auth.AbstractAuthenticatorRMIService;
import com.openexchange.auth.Authenticator;
import com.openexchange.auth.Credentials;
import com.openexchange.contact.storage.rdb.internal.Deduplicator;
import com.openexchange.contact.storage.rdb.internal.RdbServiceLookup;
import com.openexchange.exception.OXException;
import com.openexchange.java.Autoboxing;

/**
 * {@link ContactStorageRMIServiceImpl}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public class ContactStorageRMIServiceImpl extends AbstractAuthenticatorRMIService implements ContactStorageRMIService {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {

        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ContactStorageRMIServiceImpl.class);
    }

    @Override
    public int[] deduplicateContacts(int contextID, int folderID, long limit, boolean dryRun, Credentials auth) throws RemoteException {
        Authenticator authenticator = RdbServiceLookup.optService(Authenticator.class);
        authenticate(authenticator, auth, contextID);

        Collection<Integer> objectIDs = null;
        try {
            objectIDs = Deduplicator.deduplicateContacts(contextID, folderID, limit, dryRun);
        } catch (OXException e) {
            LoggerHolder.LOG.error("Error de-duplicating contacts in folder {} of context {}{}: {}", I(folderID), I(contextID), dryRun ? " [dry-run]" : "", e.getMessage(), e);
        }
        return null != objectIDs ? Autoboxing.I2i(objectIDs) : null;
    }
}
