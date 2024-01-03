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

package com.openexchange.mail.exportpdf.impl;

import com.openexchange.mail.exportpdf.MailExportAttachmentInformation;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;

/**
 * {@link DefaultMailExportAttachmentInformation} - Provides metadata information about
 * the attachment, and a MailPart container for the actual attachment data.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DefaultMailExportAttachmentInformation implements MailExportAttachmentInformation {

    private final MailExportMailPartContainer mailPart;
    private final String baseContentType;
    private final String fileName;
    private final String id;
    private final boolean isInline;
    private final String imageCID;

    /**
     * Initializes a new {@link DefaultMailExportAttachmentInformation}.
     *
     * @param part The corresponding mail part
     * @param baseContentType The attachment's base content type
     * @param fileName The attachment's file name
     * @param id The attachment's identifier
     * @param isInline <code>true</code> if this is an <i>inline</i> attachment, <code>false</code>, otherwise
     */
    public DefaultMailExportAttachmentInformation(MailExportMailPartContainer part, String baseContentType, String fileName, String id, boolean isInline) {
        this(part, baseContentType, fileName, id, isInline, null);
    }

    /**
     * Initializes a new {@link DefaultMailExportAttachmentInformation}.
     *
     * @param part The corresponding mail part
     * @param baseContentType The attachment's base content type
     * @param fileName The attachment's file name
     * @param id The attachment's identifier
     * @param isInline <code>true</code> if this is an <i>inline</i> attachment, <code>false</code>, otherwise
     * @param imageCID The image's content identifier
     */
    public DefaultMailExportAttachmentInformation(MailExportMailPartContainer part, String baseContentType, String fileName, String id, boolean isInline, String imageCID) {
        super();
        this.mailPart = part;
        this.imageCID = imageCID;
        this.baseContentType = baseContentType;
        this.isInline = isInline;
        this.fileName = fileName;
        this.id = id;
    }

    @Override
    public MailExportMailPartContainer getMailPart() {
        return mailPart;
    }

    @Override
    public String getBaseContentType() {
        return baseContentType;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isInline() {
        return isInline;
    }

    @Override
    public String getImageCID() {
        return imageCID;
    }

    @Override
    public String toString() {
        return "MailAttachmentInfo [mailPart=" + mailPart + ", baseContentType=" + baseContentType + ", fileName=" + fileName + ", id=" + id + ", isInline=" + isInline + ", imageCID=" + imageCID + "]";
    }
}
