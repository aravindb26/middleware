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

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.config.MailAccountProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.UUEncodedAttachmentMailPart;
import com.openexchange.mail.exportpdf.MailExportContentInformation;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;
import com.openexchange.mail.exportpdf.converters.header.MailHeader;
import com.openexchange.mail.exportpdf.impl.DefaultMailExportAttachmentInformation;
import com.openexchange.mail.exportpdf.impl.DefaultMailExportMailPartContainer;
import com.openexchange.mail.exportpdf.impl.pdf.dao.PDFMailExportContentInformation;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.mail.parser.ContentProvider;
import com.openexchange.mail.parser.MailMessageHandler;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mail.uuencode.UUEncodedPart;
import com.openexchange.session.Session;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;

/**
 * {@link PDFExportMailMessageHandler}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class PDFExportMailMessageHandler implements MailMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PDFExportMailMessageHandler.class);

    private final PDFMailExportContentInformation contentInformation;
    private final MailAccountProperties mailProperties;

    /**
     * Initializes a new {@link PDFExportMailMessageHandler}.
     *
     * @param session The session providing user information
     */
    public PDFExportMailMessageHandler(Session session) {
        super();
        mailProperties = new MailAccountProperties(null, session.getUserId(), session.getContextId());
        contentInformation = new PDFMailExportContentInformation();
    }

    @Override
    public boolean handleFrom(InternetAddress[] fromAddrs) throws OXException {
        contentInformation.addHeader(MailHeader.FROM.getMailHeaderName(), formatAddresses(fromAddrs));
        return true;
    }

    @Override
    public boolean handleSender(InternetAddress[] senderAddrs) throws OXException {
        contentInformation.addHeader(MailHeader.SENDER.getMailHeaderName(), formatAddresses(senderAddrs));
        return true;
    }

    @Override
    public boolean handleToRecipient(InternetAddress[] recipientAddrs) throws OXException {
        contentInformation.addHeader(MailHeader.TO.getMailHeaderName(), formatAddresses(recipientAddrs));
        return true;
    }

    @Override
    public boolean handleCcRecipient(InternetAddress[] recipientAddrs) throws OXException {
        contentInformation.addHeader(MailHeader.CC.getMailHeaderName(), formatAddresses(recipientAddrs));
        return true;
    }

    @Override
    public boolean handleBccRecipient(InternetAddress[] recipientAddrs) throws OXException {
        contentInformation.addHeader(MailHeader.BCC.getMailHeaderName(), formatAddresses(recipientAddrs));
        return true;
    }

    @Override
    public boolean handleSubject(String subject) throws OXException {
        contentInformation.addHeader(MailHeader.SUBJECT.getMailHeaderName(), subject);
        return true;
    }

    @Override
    public boolean handleSentDate(Date sentDate) throws OXException {
        contentInformation.setSentDate(sentDate);
        return true;
    }

    @Override
    public boolean handleReceivedDate(Date receivedDate) throws OXException {
        contentInformation.setReceivedDate(receivedDate);
        return true;
    }

    @Override
    public boolean handleInlinePlainText(String plainTextContent, ContentType contentType, long size, String fileName, String id) throws OXException {
        contentInformation.setTextBody(plainTextContent);
        return true;
    }

    @Override
    public boolean handleInlineHtml(ContentProvider htmlContent, ContentType contentType, long size, String fileName, String id) throws OXException {
        contentInformation.setRichTextBody(htmlContent.getContent());
        return true;
    }

    @Override
    public boolean handleAttachment(MailPart part, boolean isInline, String baseContentType, String fileName, String id) throws OXException {
        return trackAttachment(new DefaultMailExportAttachmentInformation(createContainer(part, isInline, id, null), baseContentType, fileName, id, isInline));
    }

    @Override
    public boolean handleSpecialPart(MailPart part, String baseContentType, String fileName, String id) throws OXException {
        return trackAttachment(new DefaultMailExportAttachmentInformation(createContainer(part, false, id, null), baseContentType, fileName, id, false));
    }

    @Override
    public boolean handleImagePart(MailPart part, String imageCID, String baseContentType, boolean isInline, String fileName, String id) throws OXException {
        if (mailProperties.hideInlineImages() && MessageUtility.seemsInlineImage(part, true)) {
            return true;
        }
        return trackAttachment(new DefaultMailExportAttachmentInformation(createContainer(part, isInline, id, imageCID), baseContentType, fileName, id, isInline, imageCID));
    }

    public MailExportContentInformation getInformation() {
        return contentInformation;
    }

    /**
     * Tracks the specified attachment
     *
     * @param attachmentInfo the attachment info
     * @return {@code true} if the attachment is tracked
     */
    private boolean trackAttachment(DefaultMailExportAttachmentInformation attachmentInfo) {
        LOG.trace("Tracking {}", attachmentInfo);
        contentInformation.addAttachmentInformation(attachmentInfo);
        return true;
    }

    /**
     * Formats the specified addresses, i.e. creates a comma separated list
     *
     * @param addresses The addresses to format
     * @return The formatted addresses
     */
    private static String formatAddresses(InternetAddress[] addresses) {
        if (null == addresses || 0 == addresses.length) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(formatAddress(addresses[0]));
        for (int i = 1; i < addresses.length; i++) {
            stringBuilder.append(", ").append(formatAddress(addresses[i]));
        }
        return stringBuilder.toString();
    }

    /**
     * Formats a single address
     *
     * @param address The single address to format
     * @return The formatted address
     */
    private static String formatAddress(InternetAddress address) {
        if (null == address) {
            return "";
        }
        if (Strings.isEmpty(address.getPersonal())) {
            return address.getAddress();
        }
        if (Strings.isEmpty(address.getAddress())) {
            return address.getPersonal();
        }
        return new StringBuilder().append(address.getPersonal()).append(" <").append(address.getAddress()).append('>').toString();
    }

    /**
     * Creates a mail part container for the specified mail part
     *
     * @param part The mail part
     * @return The container
     */
    private static MailExportMailPartContainer createContainer(MailPart part, boolean inline, String id, String imageCID) {
        return new DefaultMailExportMailPartContainer(part, inline, id, imageCID);
    }

    @Override
    public boolean handleInlineUUEncodedPlainText(String decodedTextContent, ContentType contentType, int size, String fileName, String id) throws OXException {
        return handleInlinePlainText(decodedTextContent, contentType, size, fileName, id);
    }

    @Override
    public boolean handleInlineUUEncodedAttachment(UUEncodedPart part, String id) throws OXException {
        String contentType = MimeTypes.MIME_APPL_OCTET;
        String fileName = part.getFileName();
        try {
            TCharSet separators = new TCharHashSet(new char[] { '/', '\\', File.separatorChar });
            String fn = fileName;
            boolean containsSeparatorChar = !separators.forEach(separator -> fn.indexOf(separator) < 0);

            File file = new File(fileName);
            if (containsSeparatorChar) {
                fileName = file.getName();
                file = new File(fileName);
            }

            contentType = Strings.asciiLowerCase(MimeType2ExtMap.getContentType(file.getName()));
        } catch (Exception e) {
            Throwable t = new Throwable(new StringBuilder("Unable to fetch content/type for '").append(fileName).append("': ").append(e).toString());
            LOG.warn("", t);
        }

        MailPart mailPart = createUUEncodedAttachmentMailPart(part, contentType, fileName, id);
        return trackAttachment(new DefaultMailExportAttachmentInformation(createContainer(mailPart, false, id, null), contentType, fileName, id, false));
    }

    /**
     * Creates a UUEncoded attachment from the specified {@link UUEncodedPart}
     * 
     * @param part The {@link UUEncodedPart}
     * @param contentType The content type of the part
     * @param fileName The filename
     * @param id The id
     * @return The {@link UUEncodedAttachmentMailPart}
     * @throws OXException if an error is occurred
     */
    private static MailPart createUUEncodedAttachmentMailPart(UUEncodedPart part, String contentType, String fileName, String id) throws OXException {
        MailPart mailPart = new UUEncodedAttachmentMailPart(part);
        mailPart.setContentType(contentType);
        mailPart.setSize(part.getFileSize());
        mailPart.setFileName(fileName);
        mailPart.setSequenceId(id);
        return mailPart;
    }

    //<editor-fold desc="NOP">
    ///////////////////////////////// NOOP////////////////////////////////////////

    @Override
    public boolean handleHeaders(int size, Iterator<Entry<String, String>> iter) throws OXException {
        return true;
    }

    @Override
    public boolean handlePriority(int priority) throws OXException {
        return true;
    }

    @Override
    public boolean handleMsgRef(String msgRef) throws OXException {
        return true;
    }

    @Override
    public boolean handleDispositionNotification(InternetAddress dispositionNotificationTo, boolean acknowledged) throws OXException {
        return true;
    }

    @Override
    public boolean handleContentId(String contentId) throws OXException {
        return true;
    }

    @Override
    public boolean handleSystemFlags(int flags) throws OXException {
        return true;
    }

    @Override
    public boolean handleUserFlags(String[] userFlags) throws OXException {
        return true;
    }

    @Override
    public boolean handleColorLabel(int colorLabel) throws OXException {
        return true;
    }

    @Override
    public boolean handleMultipart(MailPart mp, int bodyPartCount, String id) throws OXException {
        // no-op
        return true;
    }

    @Override
    public boolean handleMultipartEnd(MailPart mp, String id) throws OXException {
        // no-op
        return true;
    }

    @Override
    public boolean handleNestedMessage(MailPart mailPart, String id) throws OXException {
        // no-op
        return true;
    }

    @Override
    public void handleMessageEnd(MailMessage mail) throws OXException {
        // no-op
    }

    @Override
    public boolean handleReplyTo(InternetAddress[] replyToAddrs) throws OXException {
        return false;
    }
    //</editor-fold>
}
