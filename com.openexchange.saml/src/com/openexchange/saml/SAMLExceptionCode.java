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

package com.openexchange.saml;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link SAMLExceptionCode}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.6.1
 */
public enum SAMLExceptionCode implements DisplayableOXExceptionCode {
    /**
     * An error regarding the SAML credential provider occurred: %1$s
     */
    CREDENTIAL_PROBLEM("An error regarding the SAML credential provider occurred: %1$s", Category.CATEGORY_CONFIGURATION, 1),
    /**
     * An error occurred while trying to encode a SAML message for transport: %1$s
     */
    ENCODING_ERROR("An error occurred while trying to encode a SAML message for transport: %1$s", Category.CATEGORY_ERROR, 2),
    /**
     * An error occurred while decoding a SAML message: %1$s
     */
    DECODING_ERROR("An error occurred while decoding a SAML message: %1$s", Category.CATEGORY_ERROR, 3),
    /**
     * An error occurred while trying to marshall a SAML object: %1$s
     */
    MARSHALLING_PROBLEM("An error occurred while trying to marshall a SAML object: %1$s", Category.CATEGORY_ERROR, 4),
    /**
     * An error occurred while trying to unmarshall a SAML object: %1$s
     */
    UNMARSHALLING_ERROR("An error occurred while trying to unmarshall a SAML object: %1$s", Category.CATEGORY_ERROR, 5),
    /**
     * The binding '%1$s' is not supported for this operation
     */
    UNSUPPORTED_BINDING("The binding '%1$s' is not supported for this operation", Category.CATEGORY_ERROR, 6),
    /**
     * SAML message validation failed: %1$s (%2$s)
     */
    VALIDATION_FAILED("SAML message validation failed: %1$s (%2$s)", Category.CATEGORY_ERROR, 7),
    /**
     * A SAML request was considered invalid. Reason: %1$s
     */
    INVALID_REQUEST("A SAML request was considered invalid. Reason: %1$s", Category.CATEGORY_ERROR, 8),
    /**
     * An internal error occurred: %1$s
     */
    INTERNAL_ERROR("An internal error occurred: %1$s", Category.CATEGORY_ERROR, 9),
    /**
     * No user could be determined for the assertions subject: %1$s
     */
    UNKNOWN_USER("No user could be determined for the assertions subject: %1$s", Category.CATEGORY_ERROR, 10)
    ;

    private final Category category;

    private final int detailNumber;

    private final String message;

    private final String displayMessage;

    private SAMLExceptionCode(final String message, final Category category, final int detailNumber) {
        this(message, null, category, detailNumber);
    }

    private SAMLExceptionCode(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.category = category;
        this.detailNumber = detailNumber;
        this.displayMessage = (displayMessage == null) ? OXExceptionStrings.MESSAGE : displayMessage;
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
        return detailNumber;
    }

    @Override
    public String getPrefix() {
        return "SAML";
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

}
