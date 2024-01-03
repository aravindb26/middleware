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

/**
 * {@link MailExportOptions} - Defines different mail export options
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface MailExportOptions {

    /**
     * Returns the drive folder id at which the exported mail should be saved
     *
     * @return The drive folder id
     */
    String getDestinationFolderId();

    /**
     * Returns the mail account id
     *
     * @return The mail account id
     */
    int getAccountId();

    /**
     * Returns the mail folder id
     *
     * @return the mail folder id
     */
    String getMailFolderId();

    /**
     * Returns the mail id
     *
     * @return the mail id
     */
    String getMailId();

    /**
     * Returns <code>true</code> if the mail is encrypted by Guard
     *
     * @return <code>true</code> if the mail is encrypted by Guard
     */
    boolean isEncrypted();

    /**
     * If the e-mail is encrypted by guard, it returns the decryption token
     *
     * @return the decryption token in case the mail is encrypted by guard
     */
    String getDecryptionToken();

    /**
     * Retrieves the hostname from the HTTP request
     *
     * @return the hostname
     */
    String getHostname();

    /**
     * Returns the desired page format.
     *
     * @return the desired page format
     */
    String getPageFormat();
    
    /**
     * Whether external images embedded in the html body shall be include in the PDF.
     *
     * @return <code>true</code> if they shall be included; <code>false</code> otherwise
     */
    Boolean includeExternalImages();

    /**
     * Indicates which version of a multipart email to prefer when exporting to PDF.
     * Defaults to <code>true</code>
     *
     * @return <code>true</code> if HTML (rich text) is preferable; <code>false</code> if the plain text is desired
     */
    default boolean preferRichText() {
        return true;
    }

    /**
     * Whether previews of convertible non-inline attachments shall be appended as
     * content in the PDF. Defaults to <code>true</code>
     *
     * @return <code>true</code> if they shall be included; <code>false</code> otherwise
     */
    default boolean appendAttachmentPreviews() {
        return true;
    }

    /**
     * Whether non-convertible non-inline attachments shall be included
     * in the PDF as embedded attachments. Defaults to <code>false</code>
     *
     * @return <code>true</code> if they shall be included; <code>false</code> otherwise
     */
    default boolean embedNonConvertibleAttachments() {
        return false;
    }

    /**
     * Whether previews of convertible non-inline attachments shall be included
     * as embedded attachments in the PDF. Defaults to <code>false</code>
     *
     * @return <code>true</code> if they shall be included; <code>false</code> otherwise
     */
    default boolean embedAttachmentPreviews() {
        return false;
    }

    /**
     * Whether convertible non-inline attachments shall be included
     * as embedded attachments in the PDF. Defaults to <code>true</code>
     *
     * @return <code>true</code> if they shall be included; <code>false</code> otherwise
     */
    default boolean embedRawAttachments() {
        return true;
    }
}
