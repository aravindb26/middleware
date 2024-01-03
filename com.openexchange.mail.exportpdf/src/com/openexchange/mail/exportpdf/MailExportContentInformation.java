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

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.openexchange.exception.OXException;

/**
 * {@link MailExportContentInformation} - Holds export information about the mail content
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface MailExportContentInformation {

    /**
     * Returns the headers
     *
     * @return the headers
     */
    Map<String, String> getHeaders();

    /**
     * Helper method for retrieving the mail's subject (if available) from the headers
     *
     * @return the value of the <code>Subject</code> header or <code>null</code>
     */
    String getSubject();

    /**
     * Helper method for retrieving the mail's to (if available) from the headers
     *
     * @return the value of the <code>To</code> header or <code>null</code>
     */

    String getTo();

    /**
     * Helper method for retrieving the mail's <code>From</code>> header (if available) from the headers
     *
     * @return the value of the <code>From</code> header or <code>null</code>
     */
    String getFrom();

    /**
     * Helper method for retrieving the mail's <code>Sender</code> header (if available) from the headers
     *
     * @return the value of the <code>Sender</code> header or <code>null</code>
     */
    String getSender();

    /**
     * Helper method for retrieving the mail's <code>Cc</code> header (if available) from the headers
     *
     * @return the value of the <code>Cc</code> header or <code>null</code>
     */
    String getCC();

    /**
     * Helper method for retrieving the mail's <code>Bcc</code> header (if available) from the headers
     *
     * @return the value of the <code>Bcc</code> header or <code>null</code>
     */
    String getBCC();

    /**
     * Gets the date the email was sent.
     *
     * @return The date, or <code>null</code> if unknown
     */
    Date getSentDate();

    /**
     * Gets the date the email was received.
     *
     * @return The date, or <code>null</code> if unknown
     */
    Date getReceivedDate();

    /**
     * Returns the attachment information
     *
     * @return the attachment information
     */
    List<MailExportAttachmentInformation> getAttachmentInformation();

    /**
     * Returns information about the inline image with the specified cid
     *
     * @param imageCID The image cid
     * @return The information about the inline image
     */
    MailExportAttachmentInformation getInlineImage(String imageCID);

    /**
     * Returns a list of known inline images
     *
     * @return A list of known inline images
     */
    List<MailExportAttachmentInformation> getInlineImages();

    /**
     * Returns the main, rich-text, body
     *
     * @return the main body
     */
    String getBody();

    /**
     * Return the main text body
     *
     * @return the main text body
     */
    String getTextBody();

    /**
     * Returns any conversion warnings
     *
     * @return any conversion warnings
     */
    List<OXException> getWarnings();
}
