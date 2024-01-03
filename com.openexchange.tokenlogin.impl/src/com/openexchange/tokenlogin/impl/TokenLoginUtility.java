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

package com.openexchange.tokenlogin.impl;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.tokenlogin.TokenLoginExceptionCodes;

/**
 * {@link TokenLoginUtility}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class TokenLoginUtility {

    /**
     * Extracts the session identifier from the given token.
     * 
     * @param obfuscatorService A reference to the obfuscator service
     * @param token The token to extract the session id from, as passed by the client
     * @return The extracted token
     * @throws OXException - {@link TokenLoginExceptionCodes#NO_SUCH_TOKEN} if the session couldn't be extracted
     */
    public static String extractSessionId(ObfuscatorService obfuscatorService, String token) throws OXException {
        String[] splitToken = Strings.splitBy(token, '-', true);
        if (splitToken.length != 2) {
            // Token is of invalid format; expected: <obfuscated-session-id> + "-" + <UUID>
            throw TokenLoginExceptionCodes.NO_SUCH_TOKEN.create(token);
        }

        // Get session identifier
        return obfuscatorService.unobfuscate(new String(Strings.unhex(splitToken[0]), StandardCharsets.UTF_8));
    }

    /**
     * Creates a new token.
     *
     * @param sessionId The session identifier
     * @param obfuscatorService The obfuscator service to use
     * @return The new token
     */
    public static String createNewToken(ObfuscatorService obfuscatorService, String sessionId) {
        byte[] bSessionId = obfuscatorService.obfuscate(sessionId).getBytes(StandardCharsets.UTF_8);
        return new StringBuilder(Strings.asHex(bSessionId)).append('-').append(UUIDs.getUnformattedString(UUID.randomUUID())).toString();
    }

}
