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

package com.openexchange.file.storage;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.apache.commons.lang3.math.Fraction;

/**
 * {@link FileStorageMediaUtility} - Utility methods for media information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class FileStorageMediaUtility {

    /**
     * Initializes a new {@link FileStorageMediaUtility}.
     */
    private FileStorageMediaUtility() {
        super();
    }

    /**
     * Gets the description for a given camera focal length in millimeter; e.g. <code>"38mm"</code>.
     *
     * @param focalLength The focal length in millimeter
     * @param locale The locale to use
     * @return The focal length description
     */
    public static String getCameraFocalLengthDescription(double focalLength, Locale locale) {
        Locale loc = null == locale ? Locale.US : locale;

        DecimalFormat format = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(loc));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(focalLength) + "mm";
    }

    /**
     * Gets the description for a given camera lens aperture; e.g. <code>"f/1,8"</code>.
     *
     * @param aperture The camera lens aperture
     * @param locale The locale to use
     * @return The lens aperture description
     */
    public static String getCameraApertureDescription(double aperture, Locale locale) {
        Locale loc = null == locale ? Locale.US : locale;

        DecimalFormat format = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(loc));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return "f/" + format.format(aperture);
    }

    /**
     * Gets the description for a given camera exposure time; e.g. <code>"1/4s"</code>.
     *
     * @param exposureTime The exposure time as APEX value
     * @param locale The locale to use
     * @return The exposure time description
     */
    public static String getCameraExposureTimeDescription(double exposureTime, Locale locale) {
        if (exposureTime >= 1) {
            return getDecimalCameraExposureTime(exposureTime, locale);
        }
        try {
            return Fraction.getFraction(exposureTime).toString() + "s";
        } catch (ArithmeticException e) {
            return getDecimalCameraExposureTime(exposureTime, locale);
        }
    }

    /**
     * Gets the camera exposure time as decimal number; e.g. <code>"0,25s"</code>.
     *
     * @param exposureTime The exposure time as APEX value
     * @param locale The locale to use
     * @return The exposure time description
     */
    private static String getDecimalCameraExposureTime(double exposureTime, Locale locale) {
        Locale loc = null == locale ? Locale.US : locale;
        DecimalFormat format = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(loc));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(exposureTime) + "s";
    }

}
