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

package com.openexchange.mail.exportpdf;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.F;
import static com.openexchange.java.Autoboxing.I;
import com.openexchange.config.lean.Property;

/**
 * {@link MailExportProperty}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public enum MailExportProperty implements Property {

    /**
     * Defines the maximum concurrent mail exports that the server is allowed
     * to process. If the limit is reached an error will be returned to the client,
     * advising it to retry again in a while.
     * <p>
     * Defaults to 10.
     */
    concurrentExports(I(10)),

    /**
     * Defines the top margin (in millimeters) of the exported pages.
     * <p>
     * Defaults to 12.7 (0.5 inches).
     */
    pageMarginTop(F(12.7f)),

    /**
     * Defines the bottom margin (in millimeters) of the exported pages.
     * <p>
     * Defaults to 12.7 (0.5 inches).
     */
    pageMarginBottom(F(12.7f)),

    /**
     * Defines the left margin (in millimeters) of the exported pages.
     * <p>
     * Defaults to 12.7 (0.5 inches).
     */
    pageMarginLeft(F(12.7f)),

    /**
     * Defines the right margin (in millimeters) of the exported pages.
     * <p>
     * Defaults to 12.7 (0.5 inches).
     */
    pageMarginRight(F(12.7f)),

    /**
     * Defines the headers' font size
     * <p>
     * Defaults to 12
     * </p>
     */
    headersFontSize(I(12)),

    /**
     * Defines the body font size
     * <p>
     * Defaults to 12
     * </p>
     */
    bodyFontSize(I(12)),

    /**
     * Defines whether PDF pages will be auto-oriented in
     * landscape mode whenever a full page appended image
     * is in landscape mode.
     * <p>
     * Defaults to <code>false</code>
     * </p>
     */
    autoPageOrientation(B(false))

    ;

    private final Object defaultValue;

    private static final String PREFIX = "com.openexchange.mail.exportpdf.";

    /**
     * Initializes a new {@link MailExportProperty}.
     */
    MailExportProperty(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the fully qualified name for the property
     *
     * @return the fully qualified name for the property
     */
    @Override
    public String getFQPropertyName() {
        return PREFIX + name();
    }

    /**
     * Returns the default value of this property
     *
     * @return the default value of this property
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
}
