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

package com.openexchange.http.liveness;

import java.nio.charset.StandardCharsets;

/**
 * {@link ResponseConstants} - Constants for liveness responses.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ResponseConstants {

    /**
     * Initializes a new {@link ResponseConstants}.
     */
    private ResponseConstants() {
        super();
    }

    private static final String CRLF = "\r\n";

    /** The "OK" response */
    public static final byte[] RESPONSE_OK = new StringBuilder(128) // NOSONARLINT
        .append("HTTP/1.1 200 OK").append(CRLF)
        .append("Connection: close").append(CRLF)
        .append("Expires: Sat, 06 May 1995 12:00:00 GMT").append(CRLF)
        .append("Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0").append(CRLF)
        .append("Pragma: no-cache").append(CRLF)
        .append("Content-Type: text/plain").append(CRLF)
        .append("Content-Length: 2").append(CRLF)
        .append(CRLF)
        .append("OK").append(CRLF)
        .toString().getBytes(StandardCharsets.US_ASCII);

    /** The "NOK" response */
    public static final byte[] RESPONSE_NOK = new StringBuilder(128) // NOSONARLINT
        .append("HTTP/1.1 404 Not Found").append(CRLF)
        .append("Connection: close").append(CRLF)
        .append("Expires: Sat, 06 May 1995 12:00:00 GMT").append(CRLF)
        .append("Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0").append(CRLF)
        .append("Pragma: no-cache").append(CRLF)
        .append("Content-Type: text/plain").append(CRLF)
        .append("Content-Length: 3").append(CRLF)
        .append(CRLF)
        .append("NOK").append(CRLF)
        .toString().getBytes(StandardCharsets.US_ASCII);

    /** The expected request line prefix: <code>"GET /live "</code> */
    public static final byte[] EXPECTED_REQUEST_LINE_PREFIX = "GET /live ".getBytes(StandardCharsets.US_ASCII); // NOSONARLINT

}
