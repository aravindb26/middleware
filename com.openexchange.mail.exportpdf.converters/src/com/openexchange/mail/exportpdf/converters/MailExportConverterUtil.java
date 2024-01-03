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

package com.openexchange.mail.exportpdf.converters;

import java.util.List;
import java.util.Set;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;
import com.openexchange.mail.mime.MimeType2ExtMap;

/**
 * {@link MailExportConverterUtil}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailExportConverterUtil {

    /**
     * Initialises a new {@link MailExportConverterUtil}.
     */
    private MailExportConverterUtil() {
        super();
    }

    /**
     * Checks if the specified mail part can be handled according to the specified enabled file extensions
     *
     * @param mailPart The mail part
     * @param enabledFileExtensions the enabled file extensions
     * @return <code>true</code> if it can be handled; <code>false</code> otherwise
     */
    public static boolean handles(MailExportMailPartContainer mailPart, Set<String> enabledFileExtensions) {
        if (MailExportConverterUtil.handlesContentType(mailPart.getBaseContentType(), enabledFileExtensions)) {
            return true;
        }
        return MailExportConverterUtil.handlesFileName(mailPart.getFileName(), enabledFileExtensions);
    }

    /**
     * Checks if the specified mail part can be handled according to the specified enabled file extensions
     * 
     * @param contentType The content type
     * @param filename The filename
     * @param enabledFileExtensions the enabled file extensions
     *
     * @return <code>true</code> if it can be handled; <code>false</code> otherwise
     */
    public static boolean handles(String contentType, String filename, Set<String> enabledFileExtensions) {
        if (MailExportConverterUtil.handlesContentType(contentType, enabledFileExtensions)) {
            return true;
        }
        return MailExportConverterUtil.handlesFileName(filename, enabledFileExtensions);

    }

    /**
     * Checks if the given content type is configured and handled by Collabora
     * 
     * @param contentType The content type to check
     *
     * @return <code>true</code> if the given content type is handled by configuration, <code>false</code> otherwise
     */
    public static boolean handlesContentType(String contentType, Set<String> enabledFileExtensions) {
        if (Strings.isEmpty(contentType)) {
            return false;
        }

        /* Check if the file extension, related to the content type, is enabled by configuration */
        List<String> fileExtensions = MimeType2ExtMap.getFileExtensions(contentType);
        for (String extension : fileExtensions) {
            if (handlesFileExtension(extension, enabledFileExtensions)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the extension of the given filename is configured and handled by Collabora
     * 
     * @param filename The filename to check
     *
     * @return <code>true</code> if the given filename extension is handled by configuration, <code>false</code> otherwise
     */
    public static boolean handlesFileName(String filename, Set<String> enabledFileExtensions) {
        if (Strings.isEmpty(filename)) {
            return false;
        }
        if (filename.contains(".")) {
            return handlesFileExtension(filename.substring(filename.lastIndexOf(".") + 1), enabledFileExtensions);
        }
        return false;
    }

    /**
     * Checks if the given file extension is configured and handled by Collabora
     *
     * @param extension The extension without a leading dot (e.g. "html")
     * @return <code>true</code> if the given filename extension is handled by configuration, <code>false</code> otherwise
     */
    public static boolean handlesFileExtension(String extension, Set<String> enabledFileExtensions) {
        if (Strings.isEmpty(extension)) {
            return false;
        }
        return Strings.isNotEmpty(extension) && enabledFileExtensions.contains(extension);
    }
}
