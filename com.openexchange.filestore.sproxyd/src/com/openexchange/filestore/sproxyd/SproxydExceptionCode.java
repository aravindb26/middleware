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

package com.openexchange.filestore.sproxyd;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link SproxydExceptionCode} - Enumeration of all {@link OXException}s known in Sproxyd module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum SproxydExceptionCode implements DisplayableOXExceptionCode {

    /**
     * An error occurred: %1$s
     */
    UNEXPECTED_ERROR("An error occurred: %1$s", CATEGORY_ERROR, 1),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR("An I/O error occurred: %1$s", CATEGORY_ERROR, 2),
    /**
     * An SQL error occurred: %1$s
     */
    SQL_ERROR("An SQL error occurred: %1$s", CATEGORY_ERROR, 3),
    /**
     * No such document: %1$s
     */
    NO_SUCH_DOCUMENT("No such document: %1$s", CATEGORY_ERROR, 4, SproxydExceptionMessages.NO_SUCH_DOCUMENT_MSG),
    /**
     * No such chunk: %1$s
     */
    NO_SUCH_CHUNK("No such chunk: %1$s", CATEGORY_ERROR, 5, SproxydExceptionMessages.NO_SUCH_CHUNK_MSG),
    /**
     * No next chunk for chunk: %1$s
     */
    NO_NEXT_CHUNK("No next chunk for chunk: %1$s", CATEGORY_ERROR, 6, SproxydExceptionMessages.NO_NEXT_CHUNK_MSG),
    /**
     * All configured sproxyd endpoints are currently unavailable.
     */
    STORAGE_UNAVAILABLE("All configured sproxyd endpoints are currently unavailable.", Category.CATEGORY_SERVICE_DOWN, 7),
    /**
     * Storage object "%1$s" is locked: %2$s
     */
    LOCKED("Storage object \"%1$s\" is locked: %2$s", Category.CATEGORY_ERROR, 8),
    ;

    private static final String PREFIX = "SPROXYD";

    /**
     * Gets the <code>"SPROXYD"</code> prefix
     *
     * @return The prefix
     */
    public static String prefix() {
        return PREFIX;
    }

    private final Category category;
    private final int detailNumber;
    private final String message;
    private final String displayMessage;

    private SproxydExceptionCode(String message, Category category, int detailNumber, String displayMessage) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
    }

    private SproxydExceptionCode(String message, Category category, int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.displayMessage = OXExceptionStrings.MESSAGE;
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
    public int getNumber() {
        return detailNumber;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage;
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
