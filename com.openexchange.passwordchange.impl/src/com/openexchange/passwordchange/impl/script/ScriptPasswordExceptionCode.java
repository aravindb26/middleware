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


package com.openexchange.passwordchange.impl.script;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link ScriptPasswordExceptionCode}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public enum ScriptPasswordExceptionCode implements DisplayableOXExceptionCode {
    /**
     * Cannot change password for any reason.
     */
    PASSWORD_FAILED("Cannot change password < %s >, see log files for details.", ScriptPasswordExceptionMessage.PASSWORD_FAILED_MSG, CATEGORY_PERMISSION_DENIED, 1),
    /**
     * Cannot change password: %1$s
     */
    PASSWORD_FAILED_WITH_MSG("Cannot change password: %1$s", ScriptPasswordExceptionMessage.PASSWORD_FAILED_MSG, CATEGORY_PERMISSION_DENIED, 1),
    /**
     * New password too short.
     */
    PASSWORD_SHORT("New password is too short.", ScriptPasswordExceptionMessage.PASSWORD_SHORT_MSG, CATEGORY_USER_INPUT, 2),
    /**
     * New password too weak.
     */
    PASSWORD_WEAK("New password is too weak.", ScriptPasswordExceptionMessage.PASSWORD_WEAK_MSG, CATEGORY_USER_INPUT, 3),
    /**
     * User not found.
     */
    PASSWORD_NOUSER("Cannot find user.", OXExceptionStrings.MESSAGE, CATEGORY_CONFIGURATION, 4),
    /**
     * User not found.
     */
    LDAP_ERROR("LDAP error.", OXExceptionStrings.MESSAGE, CATEGORY_CONFIGURATION, 5),

    ;

    /**
     * Message of the exception.
     */
    private final String message;

    /**
     * (Display-) Message of the exception.
     */
    private final String displayMessage;

    /**
     * Category of the exception.
     */
    private final Category category;

    /**
     * Detail number of the exception.
     */
    private final int detailNumber;

    /**
     * Default constructor.
     *
     * @param message message.
     * @param category category.
     * @param detailNumber detail number.
     */
    private ScriptPasswordExceptionCode(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage;
        this.category = category;
        this.detailNumber = detailNumber;
    }

    @Override
    public String getPrefix() {
        return "PSW";
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
