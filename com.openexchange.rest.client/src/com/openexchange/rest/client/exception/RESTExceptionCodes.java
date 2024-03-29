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

package com.openexchange.rest.client.exception;

import static com.openexchange.exception.OXExceptionStrings.MESSAGE;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.API;

/**
 * {@link RESTExceptionCodes}
 * 
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public enum RESTExceptionCodes implements DisplayableOXExceptionCode {
    /**
     * Session must not be null.
     */
    SESSION_NULL("Session must not be null.", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 1),
    /**
     * The HTTP method \"%1$s\" is unsupported.
     */
    UNSUPPORTED_METHOD("The HTTP method \"%1$s\" is unsupported.", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 2),
    /**
     * The specified encoding \"%1$s\" is unsupported.
     */
    UNSUPPORTED_ENCODING("The specified encoding \"%1$s\" is unsupported.", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 3),
    /**
     * The "key" must not be null.
     */
    KEY_NULL("The \"key\" must not be null.", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 4),
    /**
     * The "secret" must not be null.
     */
    SECRET_NULL("The \"secret\" must not be null.", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 5),
    /**
     * The character "%1$s" is illegal for "%2$s".
     */
    ILLEGAL_CHARACTER("The character \"%1$s\" is illegal for key: \"%2$s\"", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 6),
    /**
     * An SSL exception occurred: "%1$s"
     */
    SSL_EXCEPTION("An SSL exception occurred: \"%1$s\"", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 7),
    /**
     * An IO exception occurred: "%1$s"
     */
    IO_EXCEPTION("An IO exception occurred: \"%1$s\"", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 8),
    /**
     * An OOM exception occurred: "%1$s"
     */
    OOM_EXCEPTION("An OOM exception occurred: \"%1$s\"", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 9),
    /**
     * An error occurred: "%1$s"
     */
    ERROR("An error occurred: \"%1$s\"", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 10),
    /**
     * Unauthorized
     */
    UNAUTHORIZED("Unauthorized", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 11),
    /**
     * Error parsing following response body: "%1$s".
     */
    PARSE_ERROR("Error parsing following response body: \"%1$s\".", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 12),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>An HTTP client protocol error occurred: %1$s</li>
     */
    CLIENT_PROTOCOL_ERROR("An HTTP client protocol error occurred: %1$s", OXExceptionStrings.MESSAGE, CATEGORY_ERROR, 13),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>The URI path '%1$s' is not valid.</li>
     */
    INVALID_URI_PATH("The URI path '%1$s' is not valid.", OXExceptionStrings.MESSAGE, CATEGORY_ERROR, 14),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>The requested page was not found.</li>
     * TODO introduced user friendly message
     */
    PAGE_NOT_FOUND("The requested page was not found.", OXExceptionStrings.MESSAGE, CATEGORY_ERROR, 15),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>An unexpected error occurred: %1$s</li>
     */
    UNEXPECTED_ERROR("An unexpected error occurred: %1$s", MESSAGE, CATEGORY_ERROR, 16),
    /**
     * <li>The remote %2$s service is unavailable at the moment. There is nothing we can do about it. Please try again later.</li>
     * <li>The remote service is unavailable at the moment: %1$s. Please try again later.</li>
     */
    REMOTE_SERVICE_UNAVAILABLE("The remote service is unavailable at the moment: %1$s. Please try again later.", RESTExceptionMessages.REMOTE_SERVICE_UNAVAILABLE_MSG, CATEGORY_SERVICE_DOWN, 17),
    /**
     * <li>An internal server error occurred on %2$s side. There is nothing we can do about it.</li>
     * <li>A remote internal server error occurred: %1$s</li>
     */
    REMOTE_INTERNAL_SERVER_ERROR("A remote internal server error occurred: %1$s", RESTExceptionMessages.REMOTE_INTERNAL_SERVER_ERROR_MSG, CATEGORY_SERVICE_DOWN, 18),
    /**
     * <li>A remote server error occurred on %2$s side. There is nothing we can do about it.</li>
     * <li>A remote server error occurred: %1$s</li>
     */
    REMOTE_SERVER_ERROR("A remote server error occurred: %1$s", RESTExceptionMessages.REMOTE_SERVER_ERROR_MSG, CATEGORY_ERROR, 19),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>An I/O error occurred: %1$s</li>
     */
    IO_ERROR("An I/O error occurred: %1$s", MESSAGE, CATEGORY_ERROR, 20),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>A JSON error occurred: %1$s</li>
     */
    JSON_ERROR("A JSON error occurred: %1$s", MESSAGE, CATEGORY_ERROR, 21),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>No stream parser found for the specified content type '%1$s'</li>
     */
    NO_STREAM_PARSER("No stream parser found for the specified content type '%1$s'", MESSAGE, CATEGORY_ERROR, 22),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>Bad request: %1$s</li>
     */
    BAD_REQUEST("Bad request: '%1$s'", MESSAGE, CATEGORY_ERROR, 23),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>Forbidden: %1$s</li>
     */
    FORBIDDEN("Forbidden: '%1$s'", MESSAGE, CATEGORY_ERROR, 23),

    ;

    private static final String PREFIX = "OX-REST";

    /**
     * (Log) Message of the exception.
     */
    private final String message;

    /**
     * Display message of the exception.
     */
    private final String displayMessage;

    /**
     * Category of the exception.
     */
    private final Category category;

    /**
     * Detail number of the exception.
     */
    private final int number;

    /**
     * Default constructor.
     * 
     * @param message message.
     * @param displayMessage The display message
     * @param category category.
     * @param detailNumber detail number.
     */
    private RESTExceptionCodes(final String message, String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage;
        this.category = category;
        number = detailNumber;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     * 
     * @return The newly created {@link OXException} instance
     */
    public OXException create() {
        return OXExceptionFactory.getInstance().create(this);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     * 
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Object... args) {
        return OXExceptionFactory.getInstance().create(this, (Throwable) null, args);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     * 
     * @param cause The optional initial cause
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Throwable cause, final Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, args);
    }

    /**
     * Creates a new {@link RESTOXException} from the specified {@link HttpResponse}
     * 
     * @param response The {@link HttpResponse} object
     * @return The newly created {@link RESTOXException} instance
     */
    public OXException create(final HttpResponse response) {
        return new RESTOXException(response);
    }

    /**
     * Creates a new {@link RESTOXException} from the specified {@link HttpResponse} and the result object
     * 
     * @param response The {@link HttpResponse} object
     * @param rest A Map of String to Object.
     * @return The newly created {@link RESTOXException} instance
     */
    public OXException create(final HttpResponse response, final Object rest) {
        return new RESTOXException(response, rest);
    }

    /**
     * Stringifies a response. Usually called when the REST client fails to parse a response from the server
     * 
     * @param reader
     * @return
     */
    public static String stringifyBody(BufferedReader reader) {
        String inputLine = null;

        try {
            if (reader != null) {
                reader.reset();
            }
        } catch (IOException ioe) {
        }
        StringBuffer result = new StringBuffer();
        try {
            if (reader != null) {
                while ((inputLine = reader.readLine()) != null) {
                    result.append(inputLine);
                }
            }
        } catch (IOException e) {
        }

        return result.toString();
    }

    /**
     * Whether the given response is valid when it has no body (only some error codes are allowed without a reason, currently 302 and 304).
     * 
     * @param response The {@link HttpResponse} object
     * @return true if the specified response is valid; false otherwise.
     */
    public static boolean isValidWithNullBody(final HttpResponse response) {
        int code = response.getStatusLine().getStatusCode();
        switch (code) {
            case HTTPResponseCodes._304_NOT_MODIFIED:
                return true;
            case HTTPResponseCodes._302_FOUND:
                String location = getHeader(response, "location");
                if (Strings.isEmpty(location)) {
                    break;
                }
                int loc = location.indexOf("://");
                if (loc < 0) {
                    break;
                }
                location = location.substring(loc + 3);
                loc = location.indexOf('/');
                if (loc < 0) {
                    break;
                }
                location = location.substring(0, loc);
                if (location.toLowerCase().contains(API.getServer())) {
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

    /**
     * Get the specified header value from the the specified {@link HttpResponse}
     * 
     * @param response The {@link HttpResponse}
     * @param name The header name
     * @return The header value
     */
    private static String getHeader(final HttpResponse response, final String name) {
        String value = null;
        Header serverheader = response.getFirstHeader(name);
        if (serverheader != null) {
            value = serverheader.getValue();
        }
        return value;
    }

    /**
     * Wraps any non-200 HTTP responses from an API call. See the constants in the {@HTTPResponseCodes} class for the
     * meaning of each error code.
     */
    private static final class RESTOXException extends OXException {

        private static class Error {

            /** English version of the error. */
            public String error;

            /** The error in the user's locale, if intended to be displayed to the user. */
            public String userError;

            public Map<String, Object> fields;

            @SuppressWarnings("unchecked")
            public Error(Map<String, Object> map) {
                fields = map;
                Object err = map.get("error");
                if (err instanceof String) {
                    error = (String) err;
                } else if (err instanceof Map<?, ?>) {
                    Map<String, Object> detail = (Map<String, Object>) err;
                    for (Object val : detail.values()) {
                        if (val instanceof String) {
                            error = (String) val;
                        }
                    }
                }
                Object uerr = map.get("user_error");
                if (uerr instanceof String) {
                    userError = (String) uerr;
                }

            }
        }

        private static final long serialVersionUID = -4971138851658897323L;

        private int error;

        private String reason;

        private String server;

        private String location;

        private Map<String, Object> parsedResponse;

        private Error body;

        /**
         * Initializes a new {@link RESTOXException}.
         * 
         * @param response The {@link HttpResponse}
         */
        protected RESTOXException(final HttpResponse response) {
            this.fillInStackTrace();
            StatusLine status = response.getStatusLine();
            error = status.getStatusCode();
            reason = status.getReasonPhrase();
            server = getHeader(response, "server");
            location = getHeader(response, "location");
        }

        /**
         * Initializes a new {@link RESTOXException}.
         * 
         * @param response The {@link HttpResponse}
         * @param rest A Map of String to Object.
         */
        @SuppressWarnings("unchecked")
        protected RESTOXException(final HttpResponse response, final Object rest) {
            this(response);

            if (rest != null && rest instanceof Map<?, ?>) {
                parsedResponse = (Map<String, Object>) rest;
                body = new Error(parsedResponse);
            }
        }

        @Override
        public String toString() {
            return "RESTOXException (" + server + "): " + error + " " + reason + " (" + (body == null ? "" : body.error) + ")";
        }

        @Override
        public String getMessage() {
            return toString();
        }

        /**
         * Gets the HTTP error code
         * 
         * @return The HTTP error code
         */
        public int getError() {
            return error;
        }
    }
}
