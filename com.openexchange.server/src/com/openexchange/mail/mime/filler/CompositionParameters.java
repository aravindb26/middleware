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

package com.openexchange.mail.mime.filler;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.openexchange.exception.OXException;
import com.openexchange.image.ImageDataSource;
import com.openexchange.image.ImageLocation;
import com.openexchange.mail.mime.filler.MimeMessageFiller.ImageProvider;

public interface CompositionParameters {

    /**
     * Gets the value for the {@link #HDR_ORGANIZATION} header.
     *
     * @return The organization or <code>null</code> if not present
     * @throws OXException
     */
    String getOrganization() throws OXException;

    /**
     * Gets the value for the {@link #HDR_X_ORIGINATING_CLIENT} header.
     *
     * @return The client identifier or <code>null</code> if not present
     * @throws OXException
     */
    String getClient() throws OXException;

    /**
     * Gets the IP to be set as value for the 'X-Originating-IP'.
     *
     * @return The IP address or <code>null</code> if the header shall not be set.
     */
    String getOriginatingIP() throws OXException;

    /**
     * Gets the ASCII-encoded value for the 'MAIL FROM' command resp. the 'Envelope-From' header.
     *
     * @return The header value, not <code>null</code>
     * @throws OXException
     */
    String getEnvelopeFrom() throws OXException;

    /**
     * Gets the address to be set for the 'Sender' header if it differs from the 'From' header.
     *
     * @param from The value of the 'From' header
     * @return The 'Sender' header or <code>null</code> if it shall not be set
     * @throws OXException
     * @throws AddressException
     */
    InternetAddress getSenderAddress(InternetAddress from) throws OXException, AddressException;

    /**
     * Gets the ID of the time zone used to format date headers.
     *
     * @return The time zone ID, not <code>null</code>
     * @throws OXException
     */
    String getTimeZoneID() throws OXException;

    /**
     * Gets the address to be set for the 'Reply-To' header, if it has not already been set.
     *
     * @return The address or <code>null</code>
     * @throws OXException
     */
    String getReplyToAddress() throws OXException;

    /**
     * Returns if the 'Reply-To' header shall be set or not.
     *
     * @return <code>true</code> if the header shall be set
     */
    boolean setReplyTo();

    /**
     * Gets the locale used for string translations.
     *
     * @return The locale, not <code>null</code>
     * @throws OXException
     */
    Locale getLocale() throws OXException;

    /**
     * Gets the file name for the senders VCard, if it shall be attached.
     *
     * @return The file name or <code>null</code> to skip attaching the VCard.
     * @throws UnsupportedEncodingException
     * @throws OXException
     */
    String getUserVCardFileName() throws OXException;

    /**
     * Gets the session user's vCard.
     * 
     * @return The vCard as byte array
     * @throws OXException
     */
    byte[] getUserVCard() throws OXException;

    /**
     * Gets the character count after which a line break is added in <code>text/plain</code> messages
     *
     * @return The character count after which a line break is added. If < 0, no line breaks are applied.
     */
    int getAutoLinebreak();

    /**
     * Checks if a forwarded message is supposed to be added as an attachment; otherwise it is added inline.
     *
     * @return <code>true</code> if a forwarded message is supposed to be added as an attachment; otherwise <code>false</code> if it is
     *         added inline.
     */
    boolean isForwardAsAttachment();

    /**
     * Creates an {@link ImageProvider} for the given data source and location.
     *
     * @param dataSource The image data source
     * @param imageLocation The image location
     * @return The image provider
     * @throws OXException
     */
    ImageProvider createImageProvider(ImageDataSource dataSource, ImageLocation imageLocation) throws OXException;

}