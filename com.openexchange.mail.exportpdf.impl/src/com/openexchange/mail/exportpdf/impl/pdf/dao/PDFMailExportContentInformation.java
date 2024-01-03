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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.MailExportAttachmentInformation;
import com.openexchange.mail.exportpdf.MailExportContentInformation;
import com.openexchange.mail.exportpdf.converters.header.MailHeader;
import com.openexchange.mail.exportpdf.impl.DefaultMailExportAttachmentInformation;

/**
 * {@link PDFMailExportContentInformation}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class PDFMailExportContentInformation implements MailExportContentInformation {

    private final Map<String, String> headers;
    private final List<MailExportAttachmentInformation> attachmentInfos;
    private Date received;
    private Date sent;
    private String textBody;
    private String richTextBody;

    /**
     * Initialises a new {@link PDFMailExportContentInformation}
     */
    public PDFMailExportContentInformation() {
        super();
        attachmentInfos = new LinkedList<>();
        headers = new HashMap<>(8);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getSubject() {
        return getHeader(MailHeader.SUBJECT);
    }

    @Override
    public String getTo() {
        return getHeader(MailHeader.TO);
    }

    @Override
    public String getFrom() {
        return getHeader(MailHeader.FROM);
    }

    @Override
    public String getSender() {
        return getHeader(MailHeader.SENDER);
    }

    @Override
    public String getCC() {
        return getHeader(MailHeader.CC);
    }

    @Override
    public String getBCC() {
        return getHeader(MailHeader.BCC);
    }

    @Override
    public Date getSentDate() {
        return sent;
    }

    @Override
    public Date getReceivedDate() {
        return received;
    }

    @Override
    public List<MailExportAttachmentInformation> getAttachmentInformation() {
        return attachmentInfos;
    }

    @Override
    public MailExportAttachmentInformation getInlineImage(String imageCID) {
        return null == attachmentInfos ? null : attachmentInfos.stream().filter(info -> imageCID.equals(info.getImageCID())).findFirst().orElse(null);
    }

    @Override
    public List<MailExportAttachmentInformation> getInlineImages() {
        return null == attachmentInfos ? Collections.emptyList() : attachmentInfos.stream().filter(attachment -> Strings.isNotEmpty(attachment.getImageCID())).toList();
    }

    @Override
    public String getBody() {
        return richTextBody;
    }

    @Override
    public String getTextBody() {
        return textBody;
    }

    @Override
    public List<OXException> getWarnings() {
        return Collections.emptyList();
    }

    /**
     * Adds the specified header
     *
     * @param key The name of the header
     * @param value The value of the header
     */
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * Adds attachment information
     *
     * @param attachmentInfo The attachment information to add
     */
    public void addAttachmentInformation(DefaultMailExportAttachmentInformation attachmentInfo) {
        attachmentInfos.add(attachmentInfo);
    }

    /**
     * Sets the text body
     *
     * @param textBody the text body to set
     */
    public void setTextBody(String textBody) {
        String thisTextBody = this.textBody;
        if (thisTextBody == null) {
            this.textBody = textBody;
            return;
        }
        if (thisTextBody.endsWith("\n") || textBody.startsWith("\n")) {
            this.textBody = thisTextBody + textBody;
            return;
        }
        this.textBody = thisTextBody + "\r\n" + textBody;
    }

    /**
     * Sets the rich text body (i.e. the HTML version)
     *
     * @param richTextBody The rich text body
     */
    public void setRichTextBody(String richTextBody) {
        this.richTextBody = richTextBody;
    }

    /**
     * Sets the sent date
     *
     * @param sent The date to set
     */
    public void setSentDate(Date sent) {
        this.sent = sent;
    }

    /**
     * Sets the received date
     *
     * @param received the received date to set
     */
    public void setReceivedDate(Date received) {
        this.received = received;
    }

    /**
     * Retrieves the value of the specified mail header
     *
     * @param mailHeader The mail header
     * @return The value
     */
    private String getHeader(MailHeader mailHeader) {
        return headers.get(mailHeader.getDisplayName());
    }
}
