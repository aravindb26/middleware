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

package com.openexchange.imap;

import java.util.LinkedHashMap;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.imap.services.Services;
import com.openexchange.java.Strings;
import com.openexchange.log.LogProperties;
import com.openexchange.mail.utils.ClientParameterUtility;
import com.openexchange.session.Session;
import com.openexchange.version.VersionService;
import com.sun.mail.imap.IMAPStore;


/**
 * {@link IMAPClientParameters} - An enumeration for IMAP client parameters passed to IMAP store using <code>"ID"</code> command (if supported)
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum IMAPClientParameters {

    /**
     * The parameter for the client's originating IP address.
     */
    ORIGINATING_IP("x-originating-ip"),
    /**
     * The parameter for the client's session identifier.
     */
    SESSION_ID("x-session-ext-id"),
    /**
     * The parameter for the client's name.
     */
    NAME("name"),
    /**
     * The parameter for the client's version identifier.
     */
    VERSION("xversion"),
    ;

    private final String paramName;

    private IMAPClientParameters(String paramName) {
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

    /**
     * Generates the session information.
     * <pre>
     *  &lt;session-id&gt; + "-" &lt;user-id&gt; + "-" + &lt;context-id&gt; + "-" + &lt;random&gt;
     *
     *  Example:
     *  6ceec6585485458eb27456ad6ec97b62-17-1337-1356782
     * </pre>
     *
     * @param session The user-associated session
     * @param imapStore The IMAP store
     * @return The session information
     */
    public static String generateSessionInformation(Session session, @SuppressWarnings("unused") IMAPStore imapStore) {
        return ClientParameterUtility.generateSessionInformation(session);
    }

    private static final class Generator implements com.sun.mail.imap.ExternalIdGenerator {

        private final Session session;

        Generator(Session session) {
            super();
            this.session = session;
        }

        @Override
        public String generateExternalId() {
            String imapSessionId = ClientParameterUtility.generateSessionInformation(session);
            LogProperties.put(LogProperties.Name.MAIL_SESSION, imapSessionId);
            return imapSessionId;
        }
    }

    /**
     * Sets the default client parameters.
     *
     * @param imapStore The IMAP store to connect to
     * @param session The associated Groupware session
     * @throws OXException
     */
    public static void setDefaultClientParameters(IMAPStore imapStore, Session session) throws OXException {
        // Set generator
        imapStore.setExternalIdGenerator(new Generator(session));

        // Generate & set client parameters
        Map<String, String> clientParams = new LinkedHashMap<String, String>(6);
        String localIp = session.getLocalIp();
        clientParams.put(IMAPClientParameters.ORIGINATING_IP.getParamName(), Strings.isEmpty(localIp) ? ClientParameterUtility.getLocalHost() : localIp);
        clientParams.put(IMAPClientParameters.NAME.getParamName(), "Open-Xchange");
        clientParams.put(IMAPClientParameters.VERSION.getParamName(), Services.getServiceSafe(VersionService.class).getVersion().toString());
        imapStore.setClientParameters(clientParams);
    }

}
