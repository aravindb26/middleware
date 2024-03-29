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

package com.openexchange.tools.servlet;

import static com.openexchange.exception.OXExceptionStrings.MESSAGE;
import static com.openexchange.exception.OXExceptionStrings.MESSAGE_DENIED;
import static com.openexchange.exception.OXExceptionStrings.MESSAGE_RETRY;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * Error codes for AJAX-related servlet exceptions.
 */
public enum AjaxExceptionCodes implements DisplayableOXExceptionCode {
    /**
     * Unknown AJAX action.
     */
    UNKNOWN_ACTION("Unknown AJAX action.", MESSAGE, Category.CATEGORY_USER_INPUT, 1, false),
    /**
     * Missing the following request parameter: %1$s
     */
    MISSING_PARAMETER("Missing the following request parameter: %1$s", MESSAGE, Category.CATEGORY_USER_INPUT, 2),
    /**
     * Missing upload image.
     */
    NO_UPLOAD_IMAGE("Missing upload image.", MESSAGE, Category.CATEGORY_ERROR, 3),
    /**
     * Invalid parameter: %s
     */
    INVALID_PARAMETER("Invalid parameter: %s", MESSAGE, Category.CATEGORY_USER_INPUT, 4),
    /**
     * I/O error while writing to Writer object: %s
     */
    IO_ERROR("I/O error while writing to Writer object: %s", MESSAGE, Category.CATEGORY_ERROR, 5),
    /**
     * Missing AJAX request handler for module %s
     */
    MISSING_REQUEST_HANDLER("Missing AJAX request handler for module %s", MESSAGE, Category.CATEGORY_ERROR, 6),
    /**
     * Unknown module.
     */
    UNKNOWN_MODULE("Unknown module.", MESSAGE, Category.CATEGORY_USER_INPUT, 7, false),
    /**
     * JSON error: %s
     */
    JSON_ERROR("JSON error: %s", MESSAGE, Category.CATEGORY_ERROR, 9),
    /**
     * Invalid parameter "%1$s": %2$s
     */
    INVALID_PARAMETER_VALUE("Invalid parameter \"%1$s\": %2$s", MESSAGE, Category.CATEGORY_USER_INPUT, 10),
    /**
     * Unexpected error: %1$s
     */
    UNEXPECTED_ERROR("Unexpected error: %1$s", MESSAGE, Category.CATEGORY_ERROR, 11),
    /**
     * Action "%1$s" on request path "%2$s" is not permitted via a non-secure connection.
     */
    NON_SECURE_DENIED("Action \"%1$s\" on request path \"%2$s\" is not permitted via a non-secure connection.", AjaxExceptionMessages.NON_SECURE_DENIED_MSG, Category.CATEGORY_ERROR, 14),
    /**
     * The action "%1$s" is disabled due to server configuration
     */
    DISABLED_ACTION("The action \"%1$s\" is disabled due to server configuration", AjaxExceptionMessages.DISABLED_ACTION_MSG, Category.CATEGORY_PERMISSION_DENIED, 15),
    /**
     * No permission for module: %s.
     */
    NO_PERMISSION_FOR_MODULE("No permission for module: %1$s.", AjaxExceptionMessages.NO_PERMISSION_FOR_MODULE, Category.CATEGORY_PERMISSION_DENIED, 16),
    /**
     * Object has been changed in the meantime.
     */
    CONFLICT("Object has been changed in the meantime.", AjaxExceptionMessages.CONFLICT, Category.CATEGORY_CONFLICT, 17),
    /**
     * Unexpected result. Expected "%1$s", but is "%2$s".
     */
    UNEXPECTED_RESULT("Unexpected result. Expected \"%1$s\", but is \"%2$s\".", MESSAGE, Category.CATEGORY_ERROR, 18),
    /**
     * Too many concurrent requests. Please try again later.
     */
    TOO_MANY_REQUESTS("Too many concurrent requests. Please try again later.", MESSAGE_RETRY, Category.CATEGORY_TRY_AGAIN, 19),
    /**
     * Bad request. The server is unable to handle the request.
     */
    BAD_REQUEST("Bad request. The server is unable to handle the request.", AjaxExceptionMessages.BAD_REQUEST_MSG, Category.CATEGORY_ERROR, 20),
    /**
     * Bad request. %1$s
     */
    BAD_REQUEST_CUSTOM("Bad request. %1$s", AjaxExceptionMessages.BAD_REQUEST_CUSTOM_MSG, Category.CATEGORY_ERROR, 20),
    /**
     * Unknown AJAX action %1$s in module %2$s.
     */
    UNKNOWN_ACTION_IN_MODULE("Unknown AJAX action %1$s in module %2$s.", MESSAGE, Category.CATEGORY_ERROR, 21),
    /**
     * The file \"%1$s\" (\"%2$s\") can't be imported as image. Only image types (JPG, GIF, BMP or PNG) are supported.
     */
    NO_IMAGE_FILE("The file \"%1$s\" (\"%2$s\") can't be imported as image.", AjaxExceptionMessages.NO_IMAGE_FILE_MSG, Category.CATEGORY_USER_INPUT, 22),
    /**
     * Missing request body.
     */
    MISSING_REQUEST_BODY("Missing request body.", MESSAGE, Category.CATEGORY_ERROR, 23),
    /**
     * An HTTP error occurred. Status %1$s. Message %2$s.
     * <p>
     * Throws an HTTP error. Specify status code and status message (optional).
     */
    HTTP_ERROR("An HTTP error occurred. Status %1$s. Message %2$s.", MESSAGE, Category.CATEGORY_ERROR, 24),
    /**
     * Unsupported format: %1$s
     */
    UNSUPPORTED_FORMAT("Unsupported format: %1$s", MESSAGE, Category.CATEGORY_ERROR, 25),
    /**
     * Missing cookie: %s. Please re-login.
     */
    MISSING_COOKIE("Missing cookie: %1$s. Please re-login.", AjaxExceptionMessages.MISSING_COOKIE_MSG, Category.CATEGORY_ERROR, 2), // Yapp, the same error code
    /**
     * Cookie "%1$s" cannot be found because requests do not have any cookies. Please re-login.
     */
    MISSING_COOKIES("Cookie \"%1$s\" cannot be found because requests do not have any cookies. Please re-login.", AjaxExceptionMessages.MISSING_COOKIE_MSG, Category.CATEGORY_ERROR, 2), // Yapp, the same error code
    /**
     * Missing the following field in JSON data: %1$s
     */
    MISSING_FIELD("Missing the following field in JSON data: %1$s", AjaxExceptionMessages.MISSING_FIELD_MSG, Category.CATEGORY_ERROR, 2), // Yapp, the same error code
    /**
     * No such conversion path from "%1$s" to "%2$s" in module "%3$s" for action "%4$s".
     */
    NO_SUCH_CONVERSION_PATH("No such conversion path from \"%1$s\" to \"%2$s\" in module \"%3$s\" for action \"%4$s\".", null, Category.CATEGORY_ERROR, 26),
    /**
     * The HTML content is too big and therefore cannot be safely displayed. Please select to download its content if you want to see it.
     */
    HTML_TOO_BIG("The HTML content is too big and therefore cannot be safely displayed. Please select to download its content if you want to see it.", AjaxExceptionMessages.HTML_TOO_BIG_MSG, Category.CATEGORY_USER_INPUT, 27),
    /**
     * Invalid request body. Expect to be of type "%1$s", but is "%2$s".
     */
    INVALID_REQUEST_BODY("Invalid request body. Expect to be of type \"%1$s\", but is \"%2$s\".", MESSAGE_DENIED, Category.CATEGORY_ERROR, 28),
    /**
     * Client sent invalid JSON data in request body.
     */
    INVALID_JSON_REQUEST_BODY("Client sent invalid JSON data in request body.", AjaxExceptionMessages.INVALID_JSON_REQUEST_BODY, Category.CATEGORY_ERROR, 29),
    /**
     * Client sent not allowed request parameter \"%1$s\".
     */
    NOT_ALLOWED_URI_PARAM("Client sent not allowed request parameter \"%1$s\" within the URI.", AjaxExceptionMessages.NOT_ALLOWED_URI_PARAM_MSG, Category.CATEGORY_ERROR, 30),
    /**
     * Client sent illegal data in request body.
     */
    ILLEGAL_REQUEST_BODY("Client sent illegal data in request body.", MESSAGE_DENIED, Category.CATEGORY_ERROR, 31),
    /**
     * Client abruptly aborted the connection: %1$s
     */
    CONNECTION_RESET("Client abruptly aborted the connection: %1$s", MESSAGE, Category.CATEGORY_CONNECTIVITY, 32),
    /**
     * There is not enough space on the disk
     */
    DISK_FULL("There is not enough space on the disk", MESSAGE, Category.CATEGORY_ERROR, 33),
    /**
     * The requested action '%1$s' in module '%2$s' is deprecated, disabled and about to be removed.
     */
    DEPRECATED_ACION("The requested action '%1$s' in module '%2$s' is deprecated, disabled and about to be removed.", MESSAGE, Category.CATEGORY_ERROR, 34),
    /**
     * The request has been aborted by application.
     */
    REQUEST_ABORTED("The request has been aborted by application.", AjaxExceptionMessages.REQUEST_ABORTED, Category.CATEGORY_ERROR, 35),
    ;

    /** The prefix for this error code class: <code>"SVL"</code> */
    public static final String PREFIX = "SVL";

    /** (Log) Message of the exception. */
    private final String message;

    /** Display message of the exception. */
    private final String displayMessage;

    /** Category of the exception. */
    private final Category category;

    /** Detail number of the exception. */
    private final int number;

    /** Whether log/display arguments are allowed */
    private final boolean allowArguments;

    /** Constructor. */
    private AjaxExceptionCodes(String message, String displayMessage, Category category, int detailNumber) {
        this(message, displayMessage, category, detailNumber, true);
    }

    /** Constructor. */
    private AjaxExceptionCodes(String message, String displayMessage, Category category, int detailNumber, boolean allowArguments) {
        this.message = message;
        this.displayMessage = displayMessage == null ? OXExceptionStrings.MESSAGE : displayMessage;
        this.category = category;
        number = detailNumber;
        this.allowArguments = allowArguments;
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
        return OXExceptionFactory.getInstance().create(this, (Throwable) null, allowArguments ? args : MESSAGE_ARGS_EMPTY);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param cause The optional initial cause
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Throwable cause, final Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, allowArguments ? args : MESSAGE_ARGS_EMPTY);
    }

}
