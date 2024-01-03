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

package com.openexchange.mail.exportpdf.impl.pdf.dao;

import java.util.LinkedList;
import java.util.List;
import com.openexchange.mail.exportpdf.MailExportAttachmentInformation;
import com.openexchange.mail.exportpdf.MailExportBodyInformation;

/**
 * {@link PDFMailExportBodyInformation}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class PDFMailExportBodyInformation implements MailExportBodyInformation {

    private final List<MailExportAttachmentInformation> inlineImages = new LinkedList<>();
    private String richTextBody;

    /**
     * Sets the rich text body (i.e. the HTML version)
     *
     * @param richTextBody The rich text body
     * @return this
     */
    public PDFMailExportBodyInformation setRichTextBody(String richTextBody) {
        this.richTextBody = richTextBody;
        return this;
    }

    /**
     * Adds a list of inline images referenced in the rich-text mail  bod
     *
     * @param inlineImages A list of referenced images to add
     * @return this
     */
    public PDFMailExportBodyInformation addImages(List<MailExportAttachmentInformation> inlineImages) {
        this.inlineImages.addAll(inlineImages);
        return this;
    }

    @Override
    public String getBody() {
        return richTextBody;
    }

    @Override
    public MailExportAttachmentInformation getInlineImage(String imageCID) {
        return inlineImages.stream().filter(info -> imageCID.equals(info.getImageCID())).findFirst().orElse(null);
    }

    @Override
    public List<MailExportAttachmentInformation> getInlineImages() {
        return inlineImages;
    }
}
