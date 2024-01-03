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
package com.openexchange.contact.common;

import java.util.Collections;

/**
 * {@link ContactsFolderProperty}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public enum ContactsFolderProperty {
    ;

    /**
     * Initializes a new calendar folder property for the <code>usedInPicker</code> property.
     * <p/>
     * This flag may be used by clients when searching for contacts.
     * <p/>
     * The value is defined as a <code>CSS3</code> color value.
     *
     * @param value The value to take over
     * @return The extended property
     */
    public static ExtendedProperty USED_IN_PICKER(String value) {
        return USED_IN_PICKER(value, false);
    }

    /**
     * Initializes a new calendar folder property for the <code>usedInPicker</code> property.
     * <p/>
     * This flag may be used by clients when searching for contacts.
     *
     * @param value The value to take over
     * @param isProtected <code>true</code> if the property should be protected, <code>false</code>, otherwise
     * @return The extended property
     */
    public static ExtendedProperty USED_IN_PICKER(String value, boolean isProtected) {
        return createProperty(USED_IN_PICKER_LITERAL, value, isProtected);
    }

    /** The literal used for the {@link ContactsFolderProperty#USED_IN_PICKER} property. */
    public static final String USED_IN_PICKER_LITERAL = "usedInPicker";

    /**
     * Gets a value indicating whether a specific property is marked as <i>protected</i> or not.
     *
     * @param property The property to check
     * @return <code>true</code> if the property is protected, <code>false</code>, otherwise
     */
    public static boolean isProtected(ExtendedProperty property) {
        ExtendedPropertyParameter parameter = property.getParameter(PROTECTED_PARAMETER.getName());
        return null != parameter && Boolean.parseBoolean(parameter.getValue());
    }

    private static final ExtendedPropertyParameter PROTECTED_PARAMETER = new ExtendedPropertyParameter("protected", "true");

    private static ExtendedProperty createProperty(String name, String value, boolean isProtected) {
        return isProtected ? new ExtendedProperty(name, value, Collections.singletonList(PROTECTED_PARAMETER)) : new ExtendedProperty(name, value);
    }

}
