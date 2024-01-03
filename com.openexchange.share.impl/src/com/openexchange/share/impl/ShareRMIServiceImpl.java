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

package com.openexchange.share.impl;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import com.openexchange.auth.AbstractAuthenticatorRMIService;
import com.openexchange.auth.Authenticator;
import com.openexchange.auth.Credentials;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;
import com.openexchange.share.ShareInfo;
import com.openexchange.share.impl.rmi.ShareRMIService;

/**
 * {@link ShareRMIServiceImpl}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public class ShareRMIServiceImpl extends AbstractAuthenticatorRMIService implements ShareRMIService {

    private static final String EMPTY_STRING = "";
    private final DefaultShareService shareService;
    private ServiceLookup serviceLookup;

    /**
     * Initialises a new {@link ShareRMIServiceImpl}.
     */
    public ShareRMIServiceImpl(DefaultShareService shareService, ServiceLookup serviceLookup) {
        super();
        this.shareService = shareService;
        this.serviceLookup = serviceLookup;
    }

    @Override
    public String listShares(int contextId) throws RemoteException {
        try {
            return formatForCLT(shareService.getAllShares(contextId));
        } catch (OXException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public String listShares(int contextId, int guestId) throws RemoteException {
        try {
            return formatForCLT(shareService.getAllShares(contextId, guestId));
        } catch (OXException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public String listShares(String token) throws RemoteException {
        String path = null;
        if (Strings.isNotEmpty(token) && token.indexOf('/') >= 0) {
            String[] split = Strings.splitBy(token, '/', true);
            path = split[1];
            token = split[0];
        }
        try {
            return formatForCLT(shareService.getShares(token, path));
        } catch (OXException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public int removeShare(String token, String path, Credentials auth) throws RemoteException {
        Authenticator authenticator = serviceLookup.getOptionalService(Authenticator.class);
        authenticate(authenticator, auth);

        if (null != path && !path.isEmpty() && !EMPTY_STRING.equals(path)) {
            token = token + "/" + path;
        }
        try {
            return shareService.removeShares(Collections.singletonList(token));
        } catch (OXException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public int removeShare(String shareToken, String targetPath, int contextId, Credentials auth) throws RemoteException {
        Authenticator authenticator = serviceLookup.getOptionalService(Authenticator.class);
        authenticate(authenticator, auth, contextId);

        if (null != targetPath && !targetPath.isEmpty() && !EMPTY_STRING.equals(targetPath)) {
            shareToken = shareToken + "/" + targetPath;
        }
        try {
            return shareService.removeShares(Collections.singletonList(shareToken), contextId);
        } catch (OXException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public int removeShares(int contextId, Credentials auth) throws RemoteException {
        Authenticator authenticator = serviceLookup.getOptionalService(Authenticator.class);
        authenticate(authenticator, auth, contextId);

        try {
            return shareService.removeShares(contextId);
        } catch (OXException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public int removeShares(int contextId, int guestId, Credentials auth) throws RemoteException {
        Authenticator authenticator = serviceLookup.getOptionalService(Authenticator.class);
        authenticate(authenticator, auth, contextId);

        try {
            return shareService.removeShares(contextId, guestId);
        } catch (OXException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    /**
     * Formats the specified share information for the command line tools
     *
     * @param shareInfos A {@link List} with the {@link ShareInfo}
     * @return A formatted string with the share information
     */
    private String formatForCLT(List<ShareInfo> shareInfos) {
        if (null == shareInfos || shareInfos.isEmpty()) {
            return "No shares found.";
        }
        StringBuilder sb = new StringBuilder();
        for (ShareInfo info : shareInfos) {
            sb.append(info).append('\n');
        }
        return sb.toString();
    }
}
