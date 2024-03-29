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

package com.openexchange.spamhandler.cloudmark;

import java.io.File;
import java.io.IOException;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.mail.util.SharedFileInputStream;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.datasource.FileHolderDataSource;
import com.openexchange.mail.mime.filler.MimeMessageFiller;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.MailTransport.SendRawProperties;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.spamhandler.SpamHandler;
import com.openexchange.spamhandler.cloudmark.util.ByteStream;
import com.openexchange.spamhandler.cloudmark.util.MailMessageByteStream;
import com.openexchange.spamhandler.cloudmark.util.MessageByteStream;

/**
 * Cloudmark spam handler
 *
 * @author <a href="mailto:benjamin.otterbach@open-xchange.com">Benjamin Otterbach</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CloudmarkSpamHandler extends SpamHandler {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CloudmarkSpamHandler.class);

    protected static final String TARGET_SPAM_ADDRESS = "com.openexchange.spamhandler.cloudmark.targetSpamEmailAddress";

    private static final String NAME = "CloudmarkSpamHandler";

    // -------------------------------------------------------------------------------------------

    private final ServiceLookup services;

    /**
     * Initializes a new {@link CloudmarkSpamHandler}.
     */
    public CloudmarkSpamHandler(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public String getSpamHandlerName() {
        return NAME;
    }

    private ThresholdFileHolder writeMessage(ByteStream byteStream) throws OXException {
        if (null == byteStream) {
            return null;
        }

        ThresholdFileHolder sink = new ThresholdFileHolder();
        boolean closeSink = true;
        try {
            byteStream.writeTo(sink.asOutputStream());
            closeSink = false;
            return sink;
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (closeSink) {
                Streams.close(sink);
            }
        }
    }

    private void getAndTransport(String mailId, InternetAddress targetAddress, InternetAddress senderAddress, WrapOptions wrapOptions, String fullName, final Session session, MailAccess<?, ?> mailAccess) throws OXException {
        ThresholdFileHolder sink = writeMessage(MailMessageByteStream.newInstanceFrom(mailAccess.getMessageStorage().getMessage(fullName, mailId, false)));
        if (null != sink) {
            try {
                // Initialize send properties
                SendRawProperties sendRawProperties = MailTransport.SendRawProperties.newInstance().addRecipient(targetAddress).setSender(senderAddress).setValidateAddressHeaders(false).setSanitizeHeaders(false);

                // Wrap if demanded
                if (wrapOptions.isWrap()) {
                    MimeMessage transportMessage = new MimeMessage(MimeDefaultSession.getDefaultSession());
                    {
                        File tempFile = sink.getTempFile();
                        if (null == tempFile) {
                            transportMessage.setDataHandler(new DataHandler(new ByteArrayDataSource(sink.toByteArray(), "message/rfc822")));
                        } else {
                            transportMessage.setDataHandler(new DataHandler(new FileHolderDataSource(sink, "message/rfc822")));
                        }
                    }
                    transportMessage.setHeader("Return-Path", senderAddress.getAddress());
                    transportMessage.setHeader("Content-Type", "message/rfc822");
                    transportMessage.setHeader("Content-Disposition", "attachment; filename=\"" + mailId + ".eml\"");
                    String subject = wrapOptions.optSubject();
                    if (Strings.isNotEmpty(subject)) {
                        transportMessage.setSubject(subject, MailProperties.getInstance().getDefaultMimeCharset());
                    }
                    if (MailProperties.getInstance().isAddClientIPAddress()) {
                        MimeMessageFiller.addClientIPAddress(transportMessage, session);
                    }

                    transportMessage.saveChanges();

                    ThresholdFileHolder tmp = writeMessage(new MessageByteStream(transportMessage));
                    Streams.close(sink);
                    sink = tmp;
                }

                // Transport message (either as-is or wrapped)
                MailTransport transport = MailTransport.getInstance(session);
                try {
                    File tempFile = sink.getTempFile();
                    if (null == tempFile) {
                        transport.sendRawMessage(sink.getStream(), sendRawProperties);
                    } else {
                        transport.sendRawMessage(new SharedFileInputStream(tempFile), sendRawProperties);
                    }
                } finally {
                    transport.close();
                }

            } catch (MessagingException e) {
                throw MimeMailException.handleMessagingException(e);
            } catch (IOException e) {
                throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
            } finally {
                Streams.close(sink);
            }
        }
    }

    @Override
    public void handleSpam(int accountId, String fullName, String[] mailIDs, boolean move, Session session) throws OXException {
        ConfigViewFactory factory = services.getService(ConfigViewFactory.class);
        ConfigView view = factory.getView(session.getUserId(), session.getContextId());

        String sTargetSpamEmailAddress = getPropertyFromView(view, TARGET_SPAM_ADDRESS, "", String.class).trim();

        MailAccess<?, ?> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect();

            if (Strings.isEmpty(sTargetSpamEmailAddress)) {
                LOG.warn("There is no value configured for 'com.openexchange.spamhandler.cloudmark.targetSpamEmailAddress', cannot process spam reporting to server.");
            } else {
                InternetAddress targetSpamAddress = null;
                try {
                    targetSpamAddress = new QuotedInternetAddress(sTargetSpamEmailAddress, true);
                } catch (AddressException e) {
                    LOG.error("The configured target eMail address is not valid", e);
                }

                // Get the wrap options
                WrapOptions wrapOptions = getWrapOptions(view, true);

                if (null != targetSpamAddress) {
                    InternetAddress senderAddress = getSenderAddress(session);
                    if (senderAddress == null) {
                        LOG.warn("Unable to transport spam mail. The sender address is missing.");
                    } else {
                        for (String mailId : mailIDs) {
                            getAndTransport(mailId, targetSpamAddress, senderAddress, wrapOptions, fullName, session, mailAccess);
                        }
                    }
                }
            }

            if (move) {
                final String targetSpamFolder = getPropertyFromView(view, "com.openexchange.spamhandler.cloudmark.targetSpamFolder", "1", String.class).trim();
                switch (targetSpamFolder) {
                    case "1":
                        mailAccess.getMessageStorage().moveMessages(fullName, mailAccess.getFolderStorage().getTrashFolder(), mailIDs, true);
                        break;
                    case "2":
                        mailAccess.getMessageStorage().moveMessages(fullName, mailAccess.getFolderStorage().getSpamFolder(), mailIDs, true);
                        break;
                    case "3":
                        mailAccess.getMessageStorage().moveMessages(fullName, mailAccess.getFolderStorage().getConfirmedSpamFolder(), mailIDs, true);
                        break;
                    case "0":
                        break;
                    default:
                        mailAccess.getMessageStorage().moveMessages(fullName, mailAccess.getFolderStorage().getTrashFolder(), mailIDs, true);
                        LOG.error("There is no valid 'com.openexchange.spamhandler.cloudmark.targetSpamFolder' configured. Moving spam to trash.");
                        break;
                }
            }
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public void handleHam(int accountId, String fullName, String[] mailIDs, boolean move, Session session) throws OXException {
        ConfigViewFactory factory = services.getService(ConfigViewFactory.class);
        ConfigView view = factory.getView(session.getUserId(), session.getContextId());

        String sTargetHamEmailAddress = getPropertyFromView(view, "com.openexchange.spamhandler.cloudmark.targetHamEmailAddress", "", String.class).trim();

        MailAccess<?, ?> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect();

            if (Strings.isEmpty(sTargetHamEmailAddress)) {
                LOG.warn("There is no value configured for 'com.openexchange.spamhandler.cloudmark.targetHamEmailAddress', cannot process ham reporting to server.");
            } else {
                InternetAddress targetHamAddress = null;
                try {
                    targetHamAddress = new QuotedInternetAddress(sTargetHamEmailAddress, true);
                } catch (AddressException e) {
                    LOG.error("The configured target eMail address is not valid", e);
                }

                // Get the wrap options
                WrapOptions wrapOptions = getWrapOptions(view, false);

                if (null != targetHamAddress) {
                    InternetAddress senderAddress = getSenderAddress(session);
                    if (senderAddress==null){
                        LOG.warn("Unable to transport ham mail. The sender address is missing.");
                    } else {
                        for (String mailId : mailIDs) {
                            getAndTransport(mailId, targetHamAddress, senderAddress, wrapOptions, fullName, session, mailAccess);
                        }
                    }
                }
            }

            if (move) {
                String targetSpamFolder = getPropertyFromView(view, "com.openexchange.spamhandler.cloudmark.targetSpamFolder", "1", String.class).trim();
                if (!targetSpamFolder.equals("0")) {
                    mailAccess.getMessageStorage().moveMessages(fullName, "INBOX", mailIDs, true);
                }
            }
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public boolean isCreateConfirmedSpam(Session session) throws OXException {
        ConfigViewFactory factory = services.getService(ConfigViewFactory.class);
        ConfigView view = factory.getView(session.getUserId(), session.getContextId());

        String targetSpamFolder = getPropertyFromView(view, "com.openexchange.spamhandler.cloudmark.targetSpamFolder", "1", String.class).trim();
        return "3".equals(targetSpamFolder);
    }

    @Override
    public boolean isCreateConfirmedHam(Session session) {
        return false;
    }

    private WrapOptions getWrapOptions(ConfigView view, boolean forSpam) throws OXException {
        // Check whether we are supposed to wrap the message
        boolean wrap = getPropertyFromView(view, "com.openexchange.spamhandler.cloudmark.wrapMessage", Boolean.FALSE, Boolean.class).booleanValue(); // <-- Call with 'false' as default to not change existing behavior

        String subject;
        if (forSpam) {
            subject = getPropertyFromView(view, "com.openexchange.spamhandler.cloudmark.spam.subjectForWrappingMessage", "", String.class).trim();
        } else {
            subject = getPropertyFromView(view, "com.openexchange.spamhandler.cloudmark.ham.subjectForWrappingMessage", "", String.class).trim();
        }

        return WrapOptions
            .builder()
            .withWrap(wrap)
            .withSubject(Strings.isEmpty(subject) ? null : subject)
            .build();
    }

    private static <V> V getPropertyFromView(ConfigView view, String propertyName, V defaultValue, Class<V> clazz) throws OXException {
        ComposedConfigProperty<V> property = view.property(propertyName, clazz);
        if (null == property) {
            return defaultValue;
        }

        V value = property.get();
        return value == null ? defaultValue : value;
    }

    /**
     * Gets the session users sender address.
     *
     * @return The address or <code>null</code> if not configured
     */
    private static InternetAddress getSenderAddress(Session session) throws OXException {
        UserSettingMail usm = UserSettingMailStorage.getInstance().getUserSettingMail(session);
        if (usm == null) {
            return null;
        }

        String sendAddr = usm.getSendAddr();
        return getAddress(sendAddr);
    }

    protected static InternetAddress getAddress(String sendAddr) {
        if (sendAddr == null) {
            return null;
        }

        try {
            return new QuotedInternetAddress(sendAddr, true);
        } catch (AddressException e) {
            LOG.error("Unable to parse provided email address {}", sendAddr, e);
            return null;
        }
    }
}
