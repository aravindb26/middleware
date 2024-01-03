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

package com.openexchange.contact.provider.ldap;

import static com.openexchange.contact.provider.ldap.LdapContactsExceptionMessages.CANT_CHANGE_PROTECTED_FOLDER_PROPERTY_MSG;
import static com.openexchange.contact.provider.ldap.LdapContactsExceptionMessages.IGNORED_TERM_MSG;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link LdapContactsExceptionCodes}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public enum LdapContactsExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * <li>An internal LDAP error occurred: %1$s</li>
     */
    LDAP_ERROR("An internal LDAP error occurred: %1$s", CATEGORY_ERROR, 1),

    /**
     * <li>The configuration value '%1$s' is wrong or missing</li>
     */
    WRONG_OR_MISSING_CONFIG_VALUE("The configuration value '%1$s' is wrong or missing", CATEGORY_CONFIGURATION, 2),

    /**
     * <li>This property is protected and cannot be changed.</li>
     * <li>Can't change protected folder property [folder %1$s, property %2$s]</li>
     */
    CANT_CHANGE_PROTECTED_FOLDER_PROPERTY("Can't change protected folder property [folder %1$s, property %2$s]", CANT_CHANGE_PROTECTED_FOLDER_PROPERTY_MSG, CATEGORY_PERMISSION_DENIED, 3),

    /**
     * <li>An unexpected error occurred: %1$s</li>
     */
    UNEXPECTED_ERROR("An unexpected error occurred: %1$s", Category.CATEGORY_ERROR, 4),

    /**
     * <li>Entry %1$s can't be resolved</li>
     */
    CANT_RESOLVE_ENTRY("Entry %1$s can't be resolved", Category.CATEGORY_TRUNCATED, 5),

    /**
     * <li>The search term '%1$s' has been ignored in the search.</li>
     * <li>Search term ignored due to unmapped column operand [term '%1$s', message '%2$s']</li>
     */
    IGNORED_TERM("Search term ignored [term '%1$s', message '%2$s']", IGNORED_TERM_MSG, Category.CATEGORY_WARNING, 6),

    ;


    private static final String PREFIX = "CONLDAP";

    private final Category category;
    private final int number;
    private final String message;
    private final String displayMessage;

    private LdapContactsExceptionCodes(String message, Category category, int detailNumber) {
        this(message, null, category, detailNumber);
    }

    private LdapContactsExceptionCodes(String message, String displayMessage, Category category, int detailNumber) {
        this.message = message;
        this.number = detailNumber;
        this.category = category;
        this.displayMessage = null != displayMessage ? displayMessage : OXExceptionStrings.MESSAGE;
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
    public int getNumber() {
        return number;
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage;
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
    public OXException create(Object... args) {
        return OXExceptionFactory.getInstance().create(this, (Throwable) null, args);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param cause The optional initial cause
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(Throwable cause, final Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, args);
    }

}
