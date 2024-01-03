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

package com.openexchange.net.ssl.management.json.action;

import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.net.ssl.management.DefaultCertificate;
import com.openexchange.net.ssl.management.SSLCertificateManagementService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link UpdateSSLCertificateAction}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class UpdateSSLCertificateAction extends AbstractSSLCertificateManagementAction {

    /**
     * Initialises a new {@link UpdateSSLCertificateAction}.
     */
    public UpdateSSLCertificateAction(ServiceLookup services) {
        super(services);
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        String fingerprint = requestData.getParameter("fingerprint", String.class, false);
        String hostname = requestData.getParameter("hostname", String.class, false);
        Boolean trust = requestData.getParameter("trust", Boolean.class, false);

        DefaultCertificate.Builder certificate = DefaultCertificate.builder()
            .fingerprint(fingerprint)
            .hostName(hostname)
            .trusted(null != trust && trust.booleanValue());

        SSLCertificateManagementService managementService = getService(SSLCertificateManagementService.class);
        managementService.store(session.getUserId(), session.getContextId(), certificate.build());

        return new AJAXRequestResult();
    }
}
