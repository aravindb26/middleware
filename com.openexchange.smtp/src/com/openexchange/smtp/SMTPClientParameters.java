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

package com.openexchange.smtp;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.mail.Transport;
import com.openexchange.java.Strings;
import com.openexchange.mail.utils.ClientParameterUtility;
import com.openexchange.session.Session;
import com.sun.mail.smtp.SMTPTransport;

/**
 * {@link SMTPClientParameters} - An enumeration for SMTP client parameters passed to SMTP store using <code>"XCLIENT"</code> command (if supported)
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public enum SMTPClientParameters {

    /**
     * The protocol parameter
     */
    PROTOCOL("PROTO"),
    /**
     * The client's IP address parameter
     */
    ADDRESS("ADDR"),
    /**
     * The client's session identifier
     */
    SESSION("SESSION"),
    ;

    private final String paramName;

    private SMTPClientParameters(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Gets the parameter name
     *
     * @return The parameter name
     */
    public String getParamName() {
        return paramName;
    }

    // --------------------------------------------------------------------------------------------------------------------

    private static final class Generator implements com.sun.mail.smtp.XClientParametersGenerator {

        private final Session session;

        Generator(Session session) {
            super();
            this.session = session;
        }

        @Override
        public Map<String, String> getClientParameters() {
            // Generate & set client parameters
            Map<String, String> clientParams = new LinkedHashMap<String, String>(6);
            clientParams.put(SMTPClientParameters.PROTOCOL.getParamName(), "ESMTP");
            String localIp = session.getLocalIp();
            clientParams.put(SMTPClientParameters.ADDRESS.getParamName(), Strings.isEmpty(localIp) ? ClientParameterUtility.getLocalHost() : localIp);
            clientParams.put(SMTPClientParameters.SESSION.getParamName(), ClientParameterUtility.generateSessionInformation(session));
            return clientParams;
        }

    }

    /**
     * Sets the default client parameters.
     *
     * @param transport The SMTP transport to connect to
     * @param session The associated Groupware session
     */
    public static void setDefaultClientParameters(Transport transport, Session session) {
        if (transport instanceof SMTPTransport) {
            ((SMTPTransport) transport).setClientParametersGenerator(new Generator(session));
        }
    }

}
