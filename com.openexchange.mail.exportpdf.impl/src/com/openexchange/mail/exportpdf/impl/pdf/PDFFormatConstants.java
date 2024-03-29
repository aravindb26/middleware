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

package com.openexchange.mail.exportpdf.impl.pdf;

/**
 * {@link PDFFormatConstants} - Defines format constants
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class PDFFormatConstants {

    /**
     * Defines the extra space after the headers' labels
     */
    public static final short EXTRA_SPACE = 5;

    /**
     * User space units per inch
     */
    private static final float POINTS_PER_INCH = 72;

    /**
     * User space units per millimetre
     */
    public static final float POINTS_PER_MM = 1 / (10 * 2.54f) * POINTS_PER_INCH;

    /**
     * Initialises a new {@link PDFFormatConstants}.
     */
    private PDFFormatConstants() {
        super();
    }
}
