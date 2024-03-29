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

package com.openexchange.microsoft.graph.api.exception;

import static com.openexchange.exception.OXExceptionStrings.MESSAGE;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;

/**
 * {@link MicrosoftGraphContactClientExceptionCodes} - Defines the client exceptions codes
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public enum MicrosoftGraphContactClientExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * <li>Unable to retrieve the contact photo for contact with id '%1$s'. There is nothing we can do about it. Try again later.</li>
     * <li>Unable to retrieve contact photo. The Content-Type header of the response was either empty or missing.</li>
     */
    NO_CONTACT_PHOTO_EMPTY_CONTENT_TYPE("Unable to retrieve contact photo. The Content-Type header of the response was either empty or missing.", MicrosoftGraphContactClientExceptionMessages.NO_CONTACT_PHOTO, CATEGORY_ERROR, 1),
    /**
     * <li>Unable to retrieve the contact photo for contact with id '%1$s'. There is nothing we can do about it. Try again later.</li>
     * <li>Unable to retrieve contact photo. The Content-Type header of the response does not indicate the existence of an image in the response body: '%2$s'</li>
     */
    NO_CONTACT_PHOTO_WRONG_CONTENT_TYPE("Unable to retrieve contact photo. The Content-Type header of the response does not indicate the existence of an image in the response body: '%2$s'.", MicrosoftGraphContactClientExceptionMessages.NO_CONTACT_PHOTO, CATEGORY_ERROR, 1),
    /**
     * <li>Unable to retrieve the contact photo for contact with id '%1$s'. There is nothing we can do about it. Try again later.</li>
     * <li>Unable to retrieve contact photo. The response body is either empty or missing.</li>
     */
    NO_CONTACT_PHOTO_EMPTY_RESPONSE_BODY("Unable to retrieve contact photo. The response body is either empty or missing.", MicrosoftGraphContactClientExceptionMessages.NO_CONTACT_PHOTO, CATEGORY_ERROR, 1),
    /**
     * <li>Unable to retrieve the contact photo for contact with id '%1$s'. There is nothing we can do about it. Try again later.</li>
     * <li>Unable to retrieve contact photo. The response body is not a byte array but an instance of '%1$s'.</li>
     */
    NO_CONTACT_PHOTO_WRONG_RESPONSE_BODY("Unable to retrieve contact photo. The response body is not a byte array but an instance of '%1$s'.", MicrosoftGraphContactClientExceptionMessages.NO_CONTACT_PHOTO, CATEGORY_ERROR, 1),
    ;

    public static final String PREFIX = "MICROSOFT-GRAPH-CONTACT-CLIENT";

    private final String message;
    private final String displayMessage;
    private final Category category;
    private final int number;

    /**
     * Initialises a new {@link MicrosoftGraphContactClientExceptionCodes}.
     * 
     * @param message The exception message
     * @param displayMessage The display message
     * @param category The {@link Category}
     * @param number The error number
     */
    private MicrosoftGraphContactClientExceptionCodes(String message, Category category, int number) {
        this(message, null, category, number);
    }

    /**
     * Initialises a new {@link MicrosoftGraphContactClientExceptionCodes}.
     * 
     * @param message The exception message
     * @param displayMessage The display message
     * @param category The {@link Category}
     * @param number The error number
     */
    private MicrosoftGraphContactClientExceptionCodes(String message, String displayMessage, Category category, int number) {
        this.message = message;
        this.displayMessage = null != displayMessage ? displayMessage : MESSAGE;
        this.category = category;
        this.number = number;
    }

    @Override
    public int getNumber() {
        return number;
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
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public boolean equals(OXException e) {
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
}
