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


package com.openexchange.gmail.send;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;
import com.openexchange.mail.MailExceptionCode;

/**
 * The Gmail Send error codes.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum GmailSendExceptionCode implements DisplayableOXExceptionCode {

    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR(MailExceptionCode.IO_ERROR.getMessage(), MailExceptionCode.IO_ERROR.getCategory(), MailExceptionCode.IO_ERROR.getNumber()),
    /**
     * Unsupported charset-encoding: %1$s
     */
    ENCODING_ERROR(MailExceptionCode.ENCODING_ERROR.getMessage(), MailExceptionCode.ENCODING_ERROR.getCategory(), MailExceptionCode.ENCODING_ERROR.getNumber()),
    /**
     * The message part with sequence ID %1$s could not be found in message %2$s in folder %3$s
     */
    PART_NOT_FOUND(GmailSendExceptionCode.PART_NOT_FOUND_MSG, CATEGORY_ERROR, 3003),
    /**
     * Html-2-Text conversion failed: %1$s
     */
    HTML2TEXT_CONVERTER_ERROR(GmailSendExceptionCode.HTML2TEXT_CONVERTER_ERROR_MSG, CATEGORY_ERROR, 3004),
    /**
     * An internal error occurred: %1$s
     */
    INTERNAL_ERROR(GmailSendExceptionCode.INTERNAL_ERROR_MSG, CATEGORY_ERROR, 3005),
    /**
     * No recipient(s) has been defined for new message
     */
    MISSING_RECIPIENTS(GmailSendExceptionMessage.MISSING_RECIPIENTS_MSG, CATEGORY_USER_INPUT, 3006, GmailSendExceptionMessage.MISSING_RECIPIENTS_MSG),
    /**
     * Message has been successfully sent, but a copy could not be placed in your sent folder
     */
    COPY_TO_SENT_FOLDER_FAILED(MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED.getMessage(), MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED.getCategory(), MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED.getNumber()),
    /**
     * Receipt acknowledgment cannot be sent: missing header %1$s in message %2$s
     */
    MISSING_NOTIFICATION_HEADER(GmailSendExceptionCode.MISSING_NOTIFICATION_HEADER_MSG, CATEGORY_ERROR, 3008),
    /**
     * No send address could be found in user configuration
     */
    NO_SEND_ADDRESS_FOUND(GmailSendExceptionCode.NO_SEND_ADDRESS_FOUND_MSG, CATEGORY_ERROR, 3009),
    /**
     * No content available in mail part
     */
    NO_CONTENT(GmailSendExceptionCode.NO_CONTENT_MSG, CATEGORY_ERROR, 3010),
    /**
     * Message has been successfully sent, but a copy could not be placed in your sent folder due to exceeded quota.
     */
    COPY_TO_SENT_FOLDER_FAILED_QUOTA(MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED_QUOTA.getMessage(), MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED_QUOTA.getCategory(), MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED_QUOTA.getNumber()),
    /**
     * No storage access because mail connection is not connected
     */
    NOT_CONNECTED(GmailSendExceptionCode.NOT_CONNECTED_MSG, CATEGORY_ERROR, 3012),
    /**
     * Unable to parse Gmail Send server URI "%1$s".
     */
    URI_PARSE_FAILED(GmailSendExceptionCode.URI_PARSE_FAILED_MSG, CATEGORY_CONFIGURATION, 3013),
    /**
     * The following recipient is not allowed: %1$s. Please remove associated address and try again.
     */
    RECIPIENT_NOT_ALLOWED(GmailSendExceptionMessage.RECIPIENT_NOT_ALLOWED, CATEGORY_USER_INPUT, 3014),
    /**
     * The Gmail Send server %1$s cannot be accessed using a secure SSL connection for user %2$s. Please change configuration accordingly.
     */
    SECURE_CONNECTION_NOT_POSSIBLE(GmailSendExceptionCode.SECURE_CONNECTION_NOT_POSSIBLE_MSG, CATEGORY_USER_INPUT, 3015),
    ;

    private final static String PART_NOT_FOUND_MSG = "The message part with sequence ID %1$s could not be found in message %2$s in folder %3$s";

    private final static String HTML2TEXT_CONVERTER_ERROR_MSG = "Html-2-Text conversion failed: %1$s";

    private final static String INTERNAL_ERROR_MSG = "An internal error occurred: %1$s";

    private final static String MISSING_NOTIFICATION_HEADER_MSG = "Receipt acknowledgment cannot be sent: missing header %1$s in message %2$s";

    private final static String NO_SEND_ADDRESS_FOUND_MSG = "No send address could be found in user configuration";

    private final static String NO_CONTENT_MSG = "No content available in mail part";

    private final static String NOT_CONNECTED_MSG = "No storage access because mail connection is not connected";

    private final static String URI_PARSE_FAILED_MSG = "Unable to parse Gmail Send server URI \"%1$s\".";

    private static final String SECURE_CONNECTION_NOT_POSSIBLE_MSG = "The Gmail Send server %1$s cannot be accessed using a secure SSL connection for user %2$s. Please change configuration accordingly.";

    private final String message;
    private final int detailNumber;
    private final Category category;
    private final String prefix;

    /**
     * Message displayed to the user
     */
    private String displayMessage;

    /**
     * Initializes a new {@link GmailSendExceptionCode}.
     *
     * @param message
     * @param category
     * @param detailNumber
     */
    private GmailSendExceptionCode(final String message, final Category category, final int detailNumber) {
        this(message, category, detailNumber, null);
    }

    /**
     * Initializes a new {@link GmailSendExceptionCode}.
     *
     * @param message
     * @param category
     * @param detailNumber
     * @param displayMessage
     */
    private GmailSendExceptionCode(final String message, final Category category, final int detailNumber, final String displayMessage) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.prefix = GmailSendProvider.PROTOCOL_GMAIL_SEND.getName();
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public int getNumber() {
        return detailNumber;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayMessage() {
        return this.displayMessage;
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
