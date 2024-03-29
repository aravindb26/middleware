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

package com.openexchange.mail.dataobjects.compose;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.activation.DataHandler;
import javax.mail.internet.InternetAddress;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.dataobjects.MailAuthenticityResult;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.SecurityInfo;
import com.openexchange.mail.dataobjects.SecurityResult;
import com.openexchange.mail.dataobjects.SecuritySettings;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.HeaderCollection;
import com.openexchange.mail.mime.filler.MimeMessageFiller;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.session.Session;

/**
 * {@link DelegatingComposedMailMessage}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.2
 */
public class DelegatingComposedMailMessage extends ComposedMailMessage {

    private static final long serialVersionUID = -9123857937074916237L;

    private final ComposedMailMessage delegate;
    private Boolean appendToSentFolder;
    private Boolean transportToRecipients;
    private ComposeType overwritingSendType;
    private UserSettingMail overwritingMailSettings;

    /**
     * Initializes a new {@link DelegatingComposedMailMessage}.
     *
     * @param delegate The compose message to delegate to
     * @throws NullPointerException If specified delegate is <code>null</code>
     */
    public DelegatingComposedMailMessage(ComposedMailMessage delegate) {
        super(delegate.getSession(), delegate.getContext());
        this.delegate = delegate;
    }

    @Override
    public void setMailSettings(UserSettingMail mailSettings) {
        this.overwritingMailSettings = mailSettings;
    }

    @Override
    public UserSettingMail getMailSettings() {
        UserSettingMail mailSettings = this.overwritingMailSettings;
        return null == mailSettings ? delegate.getMailSettings() : mailSettings;
    }

    @Override
    public boolean isAppendToSentFolder() {
        Boolean appendToSentFolder = this.appendToSentFolder;
        return null == appendToSentFolder ? delegate.isAppendToSentFolder() : appendToSentFolder.booleanValue();
    }

    @Override
    public void setAppendToSentFolder(boolean appendToSentFolder) {
        this.appendToSentFolder = Boolean.valueOf(appendToSentFolder);
    }

    @Override
    public boolean isTransportToRecipients() {
        Boolean transportToRecipients = this.transportToRecipients;
        return null == transportToRecipients ? delegate.isTransportToRecipients() : transportToRecipients.booleanValue();
    }

    @Override
    public void setTransportToRecipients(boolean transportToRecipients) {
        this.transportToRecipients = Boolean.valueOf(transportToRecipients);
    }

    @Override
    public void setSendType(ComposeType sendType) {
        this.overwritingSendType = sendType;
    }

    @Override
    public ContentType getContentType() {
        return delegate.getContentType();
    }

    @Override
    public ComposeType getSendType() {
        ComposeType sendType = this.overwritingSendType;
        return null == sendType ? delegate.getSendType() : sendType;
    }

    @Override
    public boolean containsContentType() {
        return delegate.containsContentType();
    }

    @Override
    public void removeContentType() {
        delegate.removeContentType();
    }

    @Override
    public void setContentType(ContentType contentType) {
        delegate.setContentType(contentType);
    }

    @Override
    public void setContentType(String contentType) throws OXException {
        delegate.setContentType(contentType);
    }

    @Override
    public ContentDisposition getContentDisposition() {
        return delegate.getContentDisposition();
    }

    @Override
    public boolean hasRecipients() {
        return delegate.hasRecipients();
    }

    @Override
    public boolean containsContentDisposition() {
        return delegate.containsContentDisposition();
    }

    @Override
    public void removeContentDisposition() {
        delegate.removeContentDisposition();
    }

    @Override
    public InternetAddress[] getRecipients() {
        return delegate.getRecipients();
    }

    @Override
    public void setContentDisposition(String disposition) throws OXException {
        delegate.setContentDisposition(disposition);
    }

    @Override
    public void addRecipient(InternetAddress recipient) {
        delegate.addRecipient(recipient);
    }

    @Override
    public void addRecipients(InternetAddress[] recipients) {
        delegate.addRecipients(recipients);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public void setContentDisposition(ContentDisposition disposition) {
        delegate.setContentDisposition(disposition);
    }

    @Override
    public Session getSession() {
        return delegate.getSession();
    }

    @Override
    public String getFileName() {
        return delegate.getFileName();
    }

    @Override
    public Context getContext() {
        return delegate.getContext();
    }

    @Override
    public void setFiller(MimeMessageFiller filler) {
        delegate.setFiller(filler);
    }

    @Override
    public boolean containsFileName() {
        return delegate.containsFileName();
    }

    @Override
    public void cleanUp() {
        delegate.cleanUp();
    }

    @Override
    public void removeFileName() {
        delegate.removeFileName();
    }

    @Override
    public void setFileName(String fileName) {
        delegate.setFileName(fileName);
    }

    @Override
    public void addHeader(String name, String value) {
        delegate.addHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value) {
        delegate.setHeader(name, value);
    }

    @Override
    public int getUnreadMessages() {
        return delegate.getUnreadMessages();
    }

    @Override
    public void addHeaders(HeaderCollection headers) {
        delegate.addHeaders(headers);
    }

    @Override
    public void setUnreadMessages(int unreadMessages) {
        delegate.setUnreadMessages(unreadMessages);
    }

    @Override
    public int getEnclosedCount() throws OXException {
        return delegate.getEnclosedCount();
    }

    @Override
    public boolean containsHeaders() {
        return delegate.containsHeaders();
    }

    @Override
    public void removeHeaders() {
        delegate.removeHeaders();
    }

    @Override
    public int getHeadersSize() {
        return delegate.getHeadersSize();
    }

    @Override
    public MailPart getEnclosedMailPart(int index) throws OXException {
        return delegate.getEnclosedMailPart(index);
    }

    @Override
    public Iterator<Entry<String, String>> getHeadersIterator() {
        return delegate.getHeadersIterator();
    }

    @Override
    public void setBodyPart(TextBodyMailPart mailPart) {
        delegate.setBodyPart(mailPart);
    }

    @Override
    public boolean containsHeader(String name) {
        return delegate.containsHeader(name);
    }

    @Override
    public TextBodyMailPart getBodyPart() {
        return delegate.getBodyPart();
    }

    @Override
    public String[] getHeader(String name) {
        return delegate.getHeader(name);
    }

    @Override
    public MailPart removeEnclosedPart(int index) {
        return delegate.removeEnclosedPart(index);
    }

    @Override
    public String getFirstHeader(String name) {
        return delegate.getFirstHeader(name);
    }

    @Override
    public void addEnclosedPart(MailPart part) {
        delegate.addEnclosedPart(part);
    }

    @Override
    public String getHeader(String name, String delimiter) {
        return delegate.getHeader(name, delimiter);
    }

    @Override
    public String getHeader(String name, char delimiter) {
        return delegate.getHeader(name, delimiter);
    }

    @Override
    public HeaderCollection getHeaders() {
        return delegate.getHeaders();
    }

    @Override
    public void addFrom(InternetAddress addr) {
        delegate.addFrom(addr);
    }

    @Override
    public Iterator<Entry<String, String>> getNonMatchingHeaders(String[] nonMatchingHeaders) {
        return delegate.getNonMatchingHeaders(nonMatchingHeaders);
    }

    @Override
    public void addFrom(InternetAddress[] addrs) {
        delegate.addFrom(addrs);
    }

    @Override
    public Iterator<Entry<String, String>> getMatchingHeaders(String[] matchingHeaders) {
        return delegate.getMatchingHeaders(matchingHeaders);
    }

    @Override
    public void addFrom(Collection<InternetAddress> addrs) {
        delegate.addFrom(addrs);
    }

    @Override
    public void removeHeader(String name) {
        delegate.removeHeader(name);
    }

    @Override
    public boolean containsFrom() {
        return delegate.containsFrom();
    }

    @Override
    public boolean hasHeaders(String... names) {
        return delegate.hasHeaders(names);
    }

    @Override
    public void removeFrom() {
        delegate.removeFrom();
    }

    @Override
    public InternetAddress[] getFrom() {
        return delegate.getFrom();
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    public void removeFromPersonals() {
        delegate.removeFromPersonals();
    }

    @Override
    public void addTo(InternetAddress addr) {
        delegate.addTo(addr);
    }

    @Override
    public boolean containsSize() {
        return delegate.containsSize();
    }

    @Override
    public void removeSize() {
        delegate.removeSize();
    }

    @Override
    public void setSize(long size) {
        delegate.setSize(size);
    }

    @Override
    public void addTo(InternetAddress[] addrs) {
        delegate.addTo(addrs);
    }

    @Override
    public String getContentId() {
        return delegate.getContentId();
    }

    @Override
    public void addTo(Collection<InternetAddress> addrs) {
        delegate.addTo(addrs);
    }

    @Override
    public boolean containsContentId() {
        return delegate.containsContentId();
    }

    @Override
    public void removeContentId() {
        delegate.removeContentId();
    }

    @Override
    public boolean containsTo() {
        return delegate.containsTo();
    }

    @Override
    public void setContentId(String contentId) {
        delegate.setContentId(contentId);
    }

    @Override
    public void removeTo() {
        delegate.removeTo();
    }

    @Override
    public String getSequenceId() {
        return delegate.getSequenceId();
    }

    @Override
    public InternetAddress[] getTo() {
        return delegate.getTo();
    }

    @Override
    public boolean containsSequenceId() {
        return delegate.containsSequenceId();
    }

    @Override
    public void removeSequenceId() {
        delegate.removeSequenceId();
    }

    @Override
    public void setSequenceId(String sequenceId) {
        delegate.setSequenceId(sequenceId);
    }

    @Override
    public void removeToPersonals() {
        delegate.removeToPersonals();
    }

    @Override
    public MailPath getMsgref() {
        return delegate.getMsgref();
    }

    @Override
    public void addCc(InternetAddress addr) {
        delegate.addCc(addr);
    }

    @Override
    public void addCc(InternetAddress[] addrs) {
        delegate.addCc(addrs);
    }

    @Override
    public boolean containsMsgref() {
        return delegate.containsMsgref();
    }

    @Override
    public void addCc(Collection<InternetAddress> addrs) {
        delegate.addCc(addrs);
    }

    @Override
    public void removeMsgref() {
        delegate.removeMsgref();
    }

    @Override
    public void setMsgref(MailPath msgref) {
        delegate.setMsgref(msgref);
    }

    @Override
    public boolean containsCc() {
        return delegate.containsCc();
    }

    @Override
    public void removeCc() {
        delegate.removeCc();
    }

    @Override
    public InternetAddress[] getCc() {
        return delegate.getCc();
    }

    @Override
    public boolean hasEnclosedParts() throws OXException {
        return delegate.hasEnclosedParts();
    }

    @Override
    public void removeCcPersonals() {
        delegate.removeCcPersonals();
    }

    @Override
    public Object getContent() throws OXException {
        return delegate.getContent();
    }

    @Override
    public void addBcc(InternetAddress addr) {
        delegate.addBcc(addr);
    }

    @Override
    public DataHandler getDataHandler() throws OXException {
        return delegate.getDataHandler();
    }

    @Override
    public void addBcc(InternetAddress[] addrs) {
        delegate.addBcc(addrs);
    }

    @Override
    public InputStream getInputStream() throws OXException {
        return delegate.getInputStream();
    }

    @Override
    public void addBcc(Collection<InternetAddress> addrs) {
        delegate.addBcc(addrs);
    }

    @Override
    public boolean containsBcc() {
        return delegate.containsBcc();
    }

    @Override
    public void removeBcc() {
        delegate.removeBcc();
    }

    @Override
    public InternetAddress[] getBcc() {
        return delegate.getBcc();
    }
    
    @Override
    public void addSender(Collection<InternetAddress> addrs) {
        delegate.addSender(addrs);
    }

    @Override
    public boolean containsSender() {
        return delegate.containsSender();
    }

    @Override
    public void removeSender() {
        delegate.removeSender();
    }

    @Override
    public InternetAddress[] getSender() {
        return delegate.getSender();
    }

    @Override
    public void loadContent() throws OXException {
        delegate.loadContent();
    }

    @Override
    public void removeBccPersonals() {
        delegate.removeBccPersonals();
    }

    @Override
    public InternetAddress[] getAllRecipients() {
        return delegate.getAllRecipients();
    }

    @Override
    public void writeTo(OutputStream out) throws OXException {
        delegate.writeTo(out);
    }

    @Override
    public void addReplyTo(InternetAddress addr) {
        delegate.addReplyTo(addr);
    }

    @Override
    public void addReplyTo(InternetAddress[] addrs) {
        delegate.addReplyTo(addrs);
    }

    @Override
    public String getSource() throws OXException {
        return delegate.getSource();
    }

    @Override
    public void addReplyTo(Collection<InternetAddress> addrs) {
        delegate.addReplyTo(addrs);
    }

    @Override
    public byte[] getSourceBytes() throws OXException {
        return delegate.getSourceBytes();
    }

    @Override
    public boolean containsReplyTo() {
        return delegate.containsReplyTo();
    }

    @Override
    public void prepareForCaching() {
        delegate.prepareForCaching();
    }

    @Override
    public void removeReplyTo() {
        delegate.removeReplyTo();
    }

    @Override
    public InternetAddress[] getReplyTo() {
        return delegate.getReplyTo();
    }

    @Override
    public int getFlags() {
        return delegate.getFlags();
    }

    @Override
    public boolean isAnswered() {
        return delegate.isAnswered();
    }

    @Override
    public boolean isDeleted() {
        return delegate.isDeleted();
    }

    @Override
    public boolean isDraft() {
        return delegate.isDraft();
    }

    @Override
    public boolean isFlagged() {
        return delegate.isFlagged();
    }

    @Override
    public boolean isRecent() {
        return delegate.isRecent();
    }

    @Override
    public boolean isSeen() {
        return delegate.isSeen();
    }

    @Override
    public boolean isUnseen() {
        return delegate.isUnseen();
    }

    @Override
    public boolean isSpam() {
        return delegate.isSpam();
    }

    @Override
    public boolean isForwarded() {
        return delegate.isForwarded();
    }

    @Override
    public boolean isReadAcknowledgment() {
        return delegate.isReadAcknowledgment();
    }

    @Override
    public boolean isUser() {
        return delegate.isUser();
    }

    @Override
    public boolean containsFlags() {
        return delegate.containsFlags();
    }

    @Override
    public void removeFlags() {
        delegate.removeFlags();
    }

    @Override
    public void setFlags(int flags) {
        delegate.setFlags(flags);
    }

    @Override
    public void setFlag(int flag, boolean enable) throws OXException {
        delegate.setFlag(flag, enable);
    }

    @Override
    public boolean isPrevSeen() {
        return delegate.isPrevSeen();
    }

    @Override
    public boolean containsPrevSeen() {
        return delegate.containsPrevSeen();
    }

    @Override
    public void removePrevSeen() {
        delegate.removePrevSeen();
    }

    @Override
    public void setPrevSeen(boolean prevSeen) {
        delegate.setPrevSeen(prevSeen);
    }

    @Override
    public int getThreadLevel() {
        return delegate.getThreadLevel();
    }

    @Override
    public boolean containsThreadLevel() {
        return delegate.containsThreadLevel();
    }

    @Override
    public void removeThreadLevel() {
        delegate.removeThreadLevel();
    }

    @Override
    public void setThreadLevel(int threadLevel) {
        delegate.setThreadLevel(threadLevel);
    }

    @Override
    public String getSubject() {
        return delegate.getSubject();
    }

    @Override
    public boolean containsSubject() {
        return delegate.containsSubject();
    }

    @Override
    public void removeSubject() {
        delegate.removeSubject();
    }

    @Override
    public void setSubject(String subject) {
        delegate.setSubject(subject);
    }

    @Override
    public void setSubject(String subject, boolean decoded) {
        delegate.setSubject(subject, decoded);
    }

    @Override
    public Date getSentDate() {
        return delegate.getSentDate();
    }

    @Override
    public boolean containsSentDate() {
        return delegate.containsSentDate();
    }

    @Override
    public void removeSentDate() {
        delegate.removeSentDate();
    }

    @Override
    public void setSentDate(Date sentDate) {
        delegate.setSentDate(sentDate);
    }

    @Override
    public Date getReceivedDate() {
        return delegate.getReceivedDate();
    }

    @Override
    public Date getReceivedDateDirect() {
        return delegate.getReceivedDateDirect();
    }

    @Override
    public boolean containsReceivedDate() {
        return delegate.containsReceivedDate();
    }

    @Override
    public void removeReceivedDate() {
        delegate.removeReceivedDate();
    }

    @Override
    public void setReceivedDate(Date receivedDate) {
        delegate.setReceivedDate(receivedDate);
    }

    @Override
    public void addUserFlag(String userFlag) {
        delegate.addUserFlag(userFlag);
    }

    @Override
    public void addUserFlags(String[] userFlags) {
        delegate.addUserFlags(userFlags);
    }

    @Override
    public void addUserFlags(Collection<String> userFlags) {
        delegate.addUserFlags(userFlags);
    }

    @Override
    public boolean containsUserFlags() {
        return delegate.containsUserFlags();
    }

    @Override
    public void removeUserFlags() {
        delegate.removeUserFlags();
    }

    @Override
    public String[] getUserFlags() {
        return delegate.getUserFlags();
    }

    @Override
    public int getColorLabel() {
        return delegate.getColorLabel();
    }

    @Override
    public boolean containsColorLabel() {
        return delegate.containsColorLabel();
    }

    @Override
    public void removeColorLabel() {
        delegate.removeColorLabel();
    }

    @Override
    public void setColorLabel(int colorLabel) {
        delegate.setColorLabel(colorLabel);
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }

    @Override
    public boolean containsPriority() {
        return delegate.containsPriority();
    }

    @Override
    public void removePriority() {
        delegate.removePriority();
    }

    @Override
    public void setPriority(int priority) {
        delegate.setPriority(priority);
    }

    @Override
    public InternetAddress getDispositionNotification() {
        return delegate.getDispositionNotification();
    }

    @Override
    public boolean containsDispositionNotification() {
        return delegate.containsDispositionNotification();
    }

    @Override
    public void removeDispositionNotification() {
        delegate.removeDispositionNotification();
    }

    @Override
    public void setDispositionNotification(InternetAddress dispositionNotification) {
        delegate.setDispositionNotification(dispositionNotification);
    }

    @Override
    public FullnameArgument getOriginalFolder() {
        return delegate.getOriginalFolder();
    }

    @Override
    public boolean containsOriginalFolder() {
        return delegate.containsOriginalFolder();
    }

    @Override
    public void removeOriginalFolder() {
        delegate.removeOriginalFolder();
    }

    @Override
    public void setOriginalFolder(FullnameArgument originalFolder) {
        delegate.setOriginalFolder(originalFolder);
    }

    @Override
    public String getOriginalId() {
        return delegate.getOriginalId();
    }

    @Override
    public boolean containsOriginalId() {
        return delegate.containsOriginalId();
    }

    @Override
    public void removeOriginalId() {
        delegate.removeOriginalId();
    }

    @Override
    public void setOriginalId(String originalId) {
        delegate.setOriginalId(originalId);
    }

    @Override
    public String getFolder() {
        return delegate.getFolder();
    }

    @Override
    public boolean containsFolder() {
        return delegate.containsFolder();
    }

    @Override
    public void removeFolder() {
        delegate.removeFolder();
    }

    @Override
    public void setFolder(String folder) {
        delegate.setFolder(folder);
    }

    @Override
    public int getAccountId() {
        return delegate.getAccountId();
    }

    @Override
    public boolean containsAccountId() {
        return delegate.containsAccountId();
    }

    @Override
    public void removeAccountId() {
        delegate.removeAccountId();
    }

    @Override
    public void setAccountId(int accountId) {
        delegate.setAccountId(accountId);
    }

    @Override
    public String getAccountName() {
        return delegate.getAccountName();
    }

    @Override
    public boolean containsAccountName() {
        return delegate.containsAccountName();
    }

    @Override
    public void removeAccountName() {
        delegate.removeAccountName();
    }

    @Override
    public void setAccountName(String accountName) {
        delegate.setAccountName(accountName);
    }

    @Override
    public boolean hasAttachment() {
        return delegate.hasAttachment();
    }

    @Override
    public boolean isHasAttachment() {
        return delegate.isHasAttachment();
    }

    @Override
    public boolean containsHasAttachment() {
        return delegate.containsHasAttachment();
    }

    @Override
    public void removeHasAttachment() {
        delegate.removeHasAttachment();
    }

    @Override
    public void setHasAttachment(boolean hasAttachment) {
        delegate.setHasAttachment(hasAttachment);
    }

    @Override
    public boolean isAlternativeHasAttachment() {
        return delegate.isAlternativeHasAttachment();
    }

    @Override
    public boolean containsAlternativeHasAttachment() {
        return delegate.containsAlternativeHasAttachment();
    }

    @Override
    public void removeAlternativeHasAttachment() {
        delegate.removeAlternativeHasAttachment();
    }

    @Override
    public void setAlternativeHasAttachment(boolean hasAttachment) {
        delegate.setAlternativeHasAttachment(hasAttachment);
    }

    @Override
    public Object clone() {
        ComposedMailMessage clonedDelegate = (ComposedMailMessage) delegate.clone();
        return new DelegatingComposedMailMessage(clonedDelegate);
    }

    @Override
    public boolean isAppendVCard() {
        return delegate.isAppendVCard();
    }

    @Override
    public boolean containsAppendVCard() {
        return delegate.containsAppendVCard();
    }

    @Override
    public void removeAppendVCard() {
        delegate.removeAppendVCard();
    }

    @Override
    public void setAppendVCard(boolean appendVCard) {
        delegate.setAppendVCard(appendVCard);
    }

    @Override
    public int getRecentCount() {
        return delegate.getRecentCount();
    }

    @Override
    public boolean containsRecentCount() {
        return delegate.containsRecentCount();
    }

    @Override
    public void removeRecentCount() {
        delegate.removeRecentCount();
    }

    @Override
    public void setRecentCount(int recentCount) {
        delegate.setRecentCount(recentCount);
    }

    @Override
    public MailPath getMailPath() {
        return delegate.getMailPath();
    }

    @Override
    public String getMessageId() {
        return delegate.getMessageId();
    }

    @Override
    public boolean containsMessageId() {
        return delegate.containsMessageId();
    }

    @Override
    public void removeMessageId() {
        delegate.removeMessageId();
    }

    @Override
    public void setMessageId(String messageId) {
        delegate.setMessageId(messageId);
    }

    @Override
    public String getInReplyTo() {
        return delegate.getInReplyTo();
    }

    @Override
    public String[] getReferences() {
        return delegate.getReferences();
    }

    @Override
    public String[] getReferencesOrInReplyTo() {
        return delegate.getReferencesOrInReplyTo();
    }

    @Override
    public boolean containsReferences() {
        return delegate.containsReferences();
    }

    @Override
    public void removeReferences() {
        delegate.removeReferences();
    }

    @Override
    public void setReferences(String sReferences) {
        delegate.setReferences(sReferences);
    }

    @Override
    public void setReferences(String[] references) {
        delegate.setReferences(references);
    }

    @Override
    public String getMailId() {
        return delegate.getMailId();
    }

    @Override
    public void setMailId(String id) {
        delegate.setMailId(id);
    }

    @Override
    public SecuritySettings getSecuritySettings() {
        return delegate.getSecuritySettings();
    }

    @Override
    public void setSecuritySettings(SecuritySettings securitySettings) {
        delegate.setSecuritySettings(securitySettings);
    }

    @Override
    public boolean isSubjectDecoded() {
        return delegate.isSubjectDecoded();
    }

    @Override
    public String getTextPreview() {
        return delegate.getTextPreview();
    }

    @Override
    public boolean containsTextPreview() {
        return delegate.containsTextPreview();
    }

    @Override
    public void removeTextPreview() {
        delegate.removeTextPreview();
    }

    @Override
    public void setTextPreview(String textPreview) {
        delegate.setTextPreview(textPreview);
    }

    @Override
    public void setSecurityInfo(SecurityInfo securityInfo) {
        delegate.setSecurityInfo(securityInfo);
    }

    @Override
    public SecurityInfo getSecurityInfo() {
        return delegate.getSecurityInfo();
    }

    @Override
    public boolean containsSecurityInfo() {
        return delegate.containsSecurityInfo();
    }

    @Override
    public void removeSecurityInfo() {
        delegate.removeSecurityInfo();
    }

    @Override
    public void setSecurityResult(SecurityResult result) {
        delegate.setSecurityResult(result);
    }

    @Override
    public SecurityResult getSecurityResult() {
        return delegate.getSecurityResult();
    }

    @Override
    public boolean hasSecurityResult() {
        return delegate.hasSecurityResult();
    }

    @Override
    public boolean containsSecurityResult() {
        return delegate.containsSecurityResult();
    }

    @Override
    public void removeSecurityResult() {
        delegate.removeSecurityResult();
    }

    @Override
    public void setAuthenticityResult(MailAuthenticityResult authenticationResult) {
        delegate.setAuthenticityResult(authenticationResult);
    }

    @Override
    public MailAuthenticityResult getAuthenticityResult() {
        return delegate.getAuthenticityResult();
    }

    @Override
    public boolean hasAuthenticityResult() {
        return delegate.hasAuthenticityResult();
    }

    @Override
    public boolean containsAuthenticityResult() {
        return delegate.containsAuthenticityResult();
    }

    @Override
    public void removeAuthenticityResult() {
        delegate.removeAuthenticityResult();
    }

}
