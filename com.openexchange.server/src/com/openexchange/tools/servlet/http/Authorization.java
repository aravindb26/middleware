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

package com.openexchange.tools.servlet.http;

import java.nio.charset.UnsupportedCharsetException;
import org.slf4j.Logger;
import com.openexchange.auth.Authenticator;
import com.openexchange.exception.OXException;
import com.openexchange.tools.encoding.Base64;

/**
 * {@link Authorization}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public final class Authorization {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Authorization.class);
    }

    private static final String BASIC_AUTH = "basic";

    private static final String SPNEGO_AUTH = "negotiate";

    /**
     * Digest type for authorization.
     */
    private static final String DIGEST_AUTH = "digest";

    private Authorization() {
        super();
    }

    public static boolean checkForAuthorizationHeader(String authHeader) {
        final String authScheme = extractAuthScheme(authHeader);
        if (null == authScheme) {
            return false;
        }
        if (!(authScheme.equalsIgnoreCase(BASIC_AUTH) || authScheme.equalsIgnoreCase(SPNEGO_AUTH))) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the client sends a correct digest authorization header.
     *
     * @param auth Authorization header.
     * @return <code>true</code> if the client sent a correct authorization header.
     */
    public static boolean checkForDigestAuthorization(final String auth) {
        if (null == auth) {
            return false;
        }
        if (auth.length() <= DIGEST_AUTH.length()) {
            return false;
        }
        if (!auth.substring(0, DIGEST_AUTH.length()).equalsIgnoreCase(DIGEST_AUTH)) {
            return false;
        }
        return true;
    }

    public static String extractAuthScheme(String authHeader) {
        if (com.openexchange.java.Strings.isEmpty(authHeader)) {
            return null;
        }
        final int spacePos = authHeader.indexOf(' ');
        if (-1 == spacePos) {
            return null;
        }
        return authHeader.substring(0, spacePos);
    }

    /**
     * Checks if the client sends a correct basic authorization header.
     *
     * @param authHeader Authorization header.
     * @return <code>true</code> if the client sent a correct authorization header.
     */
    public static boolean checkForBasicAuthorization(final String authHeader) {
        final String authScheme = extractAuthScheme(authHeader);
        if (null == authScheme) {
            return false;
        }
        return authScheme.equalsIgnoreCase(BASIC_AUTH);
    }

    /**
     *
     * Checks if the client is authorized to access this admin rest endpoint
     *
     * Note: This method must only be used for rest endpoints which are only accessible for admins
     *
     * @param authenticator The authenticator to use
     * @param contextId The context id
     * @param auth The auth header
     * @return <code>true</code> if the client is authorized, <code>false</code> otherwise
     * @throws OXException
     */
    public static boolean checkAdminRestAuth(Authenticator authenticator, int contextId, String auth) throws OXException {
        if (Authorization.checkForBasicAuthorization(auth)) {
            // Check the provided credentials
            Credentials credentials = Authorization.decode(auth);
            if (Authorization.checkLogin(credentials.getPassword())) {
                try {
                    authenticator.doAuthentication(new com.openexchange.auth.Credentials(credentials.getLogin(), credentials.getPassword()), contextId, true);
                    // Access granted
                    return true;
                } catch (@SuppressWarnings("unused") OXException e) {
                    // Fall through
                }
            }
        } else {
            // No valid auth header provided by the client
            // Check if unauthorized access is allowed
            if (authenticator.isContextAuthenticationDisabled()) {
                // No auth required for the context admin
                LoggerHolder.LOG.debug("Granting access to admin rest api without basicAuth! 'CONTEXT_AUTHENTICATION_DISABLED' is set to 'true'.");
                return true;
            }

            if (authenticator.isMasterAuthenticationDisabled() && authenticator.isMasterAccountOverrideEnabled()) {
                // No auth required for master admin
                LoggerHolder.LOG.debug("Granting access to admin rest api without basicAuth! 'MASTER_AUTHENTICATION_DISABLED' and 'MASTER_ACCOUNT_OVERRIDE' are set to 'true' ");
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the client sends a correct kerberos authorization header.
     *
     * @param authHeader Authorization header.
     * @return <code>true</code> if the client sent a correct authorization header.
     */
    public static boolean checkForKerberosAuthorization(final String authHeader) {
        final String authScheme = extractAuthScheme(authHeader);
        if (null == authScheme) {
            return false;
        }
        if (!authScheme.equalsIgnoreCase(SPNEGO_AUTH)) {
            return false;
        }
        return true;
    }

    /**
     * The credentials providing login and password.
     */
    public static class Credentials {

        private final String login;
        private final String password;

        /**
         * Initializes a new {@link Credentials}.
         *
         * @param login The login
         * @param password The password
         */
        public Credentials(final String login, final String password) {
            super();
            this.login = login;
            this.password = password;
        }

        /**
         * Gets the login
         *
         * @return The login
         */
        public String getLogin() {
            return login;
        }

        /**
         * Gets the password
         *
         * @return The password
         */
        public String getPassword() {
            return password;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(24);
            builder.append("Credentials [");
            if (login != null) {
                builder.append("login=").append(login).append(", ");
            }
            if (password != null) {
                builder.append("password=").append(password);
            }
            builder.append(']');
            return builder.toString();
        }

    }

    /**
     * The unknown character: <code>'&#65533;'</code>
     */
    private static final char UNKNOWN = '\ufffd';

    public static Credentials decode(final String auth) throws UnsupportedCharsetException {
        final byte[] decoded = Base64.decode(auth.substring(BASIC_AUTH.length() + 1).trim());
        String userpass = new String(decoded, com.openexchange.java.Charsets.UTF_8);
        if (userpass.indexOf(UNKNOWN) >= 0) {
            userpass = new String(decoded, com.openexchange.java.Charsets.ISO_8859_1);
        }
        final int delimiter = userpass.indexOf(':');
        String login = "";
        String pass = "";
        if (-1 != delimiter) {
            login = userpass.substring(0, delimiter);
            pass = userpass.substring(delimiter + 1);
        }
        return new Credentials(login, pass);
    }

    /**
     * Checks if the login contains only valid values.
     *
     * @param pass password of the user
     * @return false if the login contains illegal values.
     */
    public static boolean checkLogin(final String pass) {
        // check if the user wants to login without password.
        // ldap bind doesn't fail with empty password. so check it here.
        return (pass != null && !com.openexchange.java.Strings.isEmpty(pass));
    }
}
