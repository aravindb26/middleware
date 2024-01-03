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
 * {@link MailExportAttachmentInformation}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface MailExportAttachmentInformation {

    /**
     * Returns the {@link MailExportMailPartContainer} instance associated with this object.
     *
     * @return the {@link MailExportMailPartContainer} instance associated with this object
     */
    MailExportMailPartContainer getMailPart();

    /**
     * Returns the base content type of this object.
     *
     * @return the base content type of this object
     */
    String getBaseContentType();

    /**
     * Returns the filename of this object.
     *
     * @return the filename of this object
     */
    String getFileName();

    /**
     * Returns the ID of this object.
     *
     * @return the ID of this object
     */
    String getId();

    /**
     * Returns whether or not this object is inline.
     *
     * @return {@code true} if this object is inline, {@code false} otherwise
     */
    boolean isInline();

    /**
     * Returns the image CID of this object.
     *
     * @return the image CID of this object
     */
    String getImageCID();
}
