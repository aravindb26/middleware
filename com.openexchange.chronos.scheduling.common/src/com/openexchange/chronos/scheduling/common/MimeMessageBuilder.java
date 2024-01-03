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

package com.openexchange.chronos.scheduling.common;

import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getFolderView;
import static com.openexchange.chronos.common.CalendarUtils.optEMailAddress;
import static com.openexchange.chronos.scheduling.common.Constants.HEADER_AUTO_SUBMITTED;
import static com.openexchange.chronos.scheduling.common.Constants.HEADER_DATE;
import static com.openexchange.chronos.scheduling.common.Constants.HEADER_DISPNOTTO;
import static com.openexchange.chronos.scheduling.common.Constants.HEADER_ORGANIZATION;
import static com.openexchange.chronos.scheduling.common.Constants.HEADER_XPRIORITY;
import static com.openexchange.chronos.scheduling.common.Constants.HEADER_X_MAILER;
import static com.openexchange.chronos.scheduling.common.Constants.VALUE_AUTO_GENERATED;
import static com.openexchange.chronos.scheduling.common.Constants.VALUE_PRIORITYNORM;
import static com.openexchange.chronos.scheduling.common.Constants.VALUE_X_MAILER;
import static com.openexchange.chronos.scheduling.common.MailUtils.generateHeaderValue;
import static com.openexchange.chronos.scheduling.common.Utils.getQuotedAddress;
import static com.openexchange.chronos.scheduling.common.Utils.isResource;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.mime.utils.MimeMessageUtility.parseAddressList;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.scheduling.changes.ChangeAction;
import com.openexchange.contact.ContactService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.session.Session;
import com.openexchange.version.VersionService;
import com.sun.mail.smtp.SMTPMessage;

/**
 * {@link MimeMessageBuilder} - builds the actual mail based on the given data
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class MimeMessageBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MimeMessageBuilder.class);

    private final MimeMessage mime;
    private final boolean isInternal;

    /**
     * Initializes a new {@link MimeMessageBuilder}.
     * @param isIntenal Whether the recipient is internal or not 
     */
    public MimeMessageBuilder(boolean isIntenal) {
        super();
        this.mime = new SMTPMessage(MimeDefaultSession.getDefaultSession());
        this.isInternal = isIntenal;
    }

    /**
     * Set the <code>FROM</code> header
     *
     * @param resource The resource to send, for looking up the correct sender address
     * @param originator The {@link CalendarUser} who the message originated from
     * @return This {@link MimeMessageBuilder} instance
     * @throws OXException If mail is missing
     */
    public MimeMessageBuilder setFrom(CalendarObjectResource resource, CalendarUser originator) throws OXException {
        String sender = null;
        try {
            for (Event event : resource.getEvents()) {
                /*
                 * Prefer address as persisted in the DB
                 */
                Attendee userAttendee = find(event.getAttendees(), originator);
                if (null != userAttendee) {
                    sender = getQuotedAddress(userAttendee.getCn(), userAttendee.getUri(), userAttendee.getEntity());
                    break;
                }
            }
            if (Strings.isEmpty(sender)) {
                sender = getQuotedAddress(originator);
            }
            InternetAddress[] addresses = parseAddressList(sender, false, false);
            checkAddress(addresses, originator);
            mime.setFrom(addresses[0]);
            mime.setReplyTo(addresses);
        } catch (MessagingException e) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create(e, "Originators email could not be set");
        }
        return this;
    }

    /**
     * Set the <code>TO</code> header
     *
     * @param recipient The {@link CalendarUser} which receives the message
     * @return This {@link MimeMessageBuilder} instance
     * @throws OXException If mail is missing
     */
    public MimeMessageBuilder setTo(CalendarUser recipient) throws OXException {
        return setTo(recipient, false);
    }

    /**
     * Set the <code>TO</code> header.
     * <p/>
     * Optionally falls back to the calendar user's email address, which might be an option for internal delivery of notification mails.
     *
     * @param recipient The {@link CalendarUser} which receives the message
     * @param fallbackToEMail <code>true</code> if the calendar user's email property may get used in case no <code>mailto:</code> URI
     *            is set, <code>false</code>, otherwise
     * @return This {@link MimeMessageBuilder} instance
     * @throws OXException If mail is missing
     */
    public MimeMessageBuilder setTo(CalendarUser recipient, boolean fallbackToEMail) throws OXException {
        try {
            InternetAddress[] addresses = parseAddressList(getQuotedAddress(recipient, fallbackToEMail), false, false);
            checkAddress(addresses, recipient);
            setTo(addresses);
        } catch (MessagingException e) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create(e, "Recipient email could not be set");
        }
        return this;
    }

    /**
     * Set the <code>TO</code> header to the given internet address.
     * 
     * @param to The address to set
     * @return This {@link MimeMessageBuilder} instance
     */
    public MimeMessageBuilder setTo(InternetAddress to) throws MessagingException {
        return setTo(new InternetAddress[] { to });
    }

    /**
     * Set the <code>TO</code> header to the given internet address(es).
     * 
     * @param to The address(es) to set
     * @return This {@link MimeMessageBuilder} instance
     */
    public MimeMessageBuilder setTo(InternetAddress[] to) throws MessagingException {
        mime.setRecipients(RecipientType.TO, to);
        return this;
    }

    /**
     * Set the <code>SENDER</code> header
     *
     * @param sender The {@link InternetAddress} who sends the message
     * @return This {@link MimeMessageBuilder} instance
     * @throws OXException If mail is missing
     */
    public MimeMessageBuilder setSender(InternetAddress sender) throws OXException {
        try {
            if (sender != null) {
                mime.setSender(sender);
            }
        } catch (MessagingException e) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create(e, "Sender email could not be set");
        }
        return this;
    }

    /**
     * Set the <code>SUBJECT</code> header
     *
     * @param subject The subject to set to the mail
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If subject can't be set
     */
    public MimeMessageBuilder setSubject(String subject) throws MessagingException {
        mime.setSubject(subject, MailProperties.getInstance().getDefaultMimeCharset());
        return this;
    }

    /**
     * Creates the payload aka the content. Text, HTML and iCal part will be appended to the mail.
     * Optional attachments of the scheduling event will be added too.
     *
     * @param factory An {@link MimePartFactory}
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If appending fails
     * @throws OXException If appending fails
     */
    public MimeMessageBuilder setContent(@NonNull MimePartFactory factory) throws MessagingException, OXException {
        MessageUtility.setContent(factory.create(), mime);
        return this;
    }

    /**
     * Adds additional header to set.
     *
     * @param additionalHeaders Additional headers as map
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If setting fails
     */
    public MimeMessageBuilder setAdditionalHeader(Map<String, String> additionalHeaders) throws MessagingException {
        if (null != additionalHeaders) {
            for (Map.Entry<String, String> header : additionalHeaders.entrySet()) {
                mime.setHeader(header.getKey(), header.getValue());
            }
        }
        return this;
    }

    /**
     * Adds additional header for a read receipt
     *
     * @param originator The {@link CalendarUser} to send the receipt to
     * @param additionalHeaders Additional headers as map
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If setting fails
     */
    public MimeMessageBuilder setReadReceiptHeader(CalendarUser originator, Map<String, String> additionalHeaders) throws MessagingException {
        if (null == additionalHeaders) {
            return this;
        }
        String readReceipt = additionalHeaders.get(Constants.ADDITIONAL_HEADER_READ_RECEIPT);
        if (Strings.isEmpty(readReceipt)) {
            return this;
        }
        return setReadReceiptHeader(originator, Boolean.valueOf(readReceipt));
    }

    /**
     * Adds additional header for a read receipt
     * 
     * @param originator The {@link CalendarUser} to send the receipt to
     * @param readReceipt The configured value for read receipt
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If setting fails
     */
    public MimeMessageBuilder setReadReceiptHeader(CalendarUser originator, Boolean readReceipt) throws MessagingException {
        if (null == readReceipt || Boolean.FALSE.equals(readReceipt)) {
            return this;
        }
        String email = optEMailAddress(originator.getUri());
        if (null != email) {
            mime.setHeader(HEADER_DISPNOTTO, email);
        }
        return this;
    }

    /**
     * Set the {@value #HEADER_XPRIORITY} header
     *
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If header can't be set
     */
    public MimeMessageBuilder setPriority() throws MessagingException {
        mime.setHeader(HEADER_XPRIORITY, VALUE_PRIORITYNORM);
        return this;
    }

    /**
     * Set the header necessary for processing notification mails in the UI
     *
     * @param recipient The recipient
     * @param action The {@link ChangeAction} to set as type
     * @param event The changed event
     * @param partStat The participant status of the originator
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If header can't be set
     */
    public MimeMessageBuilder setOXHeader(CalendarUser recipient, ChangeAction action, Event event, ParticipationStatus partStat) throws MessagingException {
        if (null == event || false == isInternal) {
            return this;
        }
        if (false == isResource(recipient.getSentBy(), event.getAttendees())) {
            try {
                String folderId = getFolderView(event, recipient.getEntity());
                mime.setHeader(Constants.HEADER_X_OX_REMINDER, new StringBuilder().append(event.getId()).append(',').append(folderId).append(',').append(1).toString());
            } catch (OXException e) {
                LOGGER.warn("Unable to get folder view for recipient {}, omitting {}-header.", recipient, Constants.HEADER_X_OX_REMINDER, e);
            }
        }
        mime.setHeader(Constants.HEADER_X_OX_MODULE, Constants.VALUE_X_OX_MODULE);
        mime.setHeader(Constants.HEADER_X_OX_TYPE, getTypeHeaderValue(action, partStat));
        mime.setHeader(Constants.HEADER_X_OX_OBJECT, event.getId());
        mime.setHeader(Constants.HEADER_X_OX_SEQUENCE, String.valueOf(event.getSequence()));
        mime.setHeader(Constants.HEADER_X_OX_UID, event.getUid());
        if (null != event.getRecurrenceId()) {
            mime.setHeader(Constants.HEADER_X_OX_RECURRENCE_DATE, event.getRecurrenceId().toString());
        }

        return this;
    }

    /**
     * Set the {@value #HEADER_X_MAILER} header
     *
     * @param versionService The {@link VersionService} to obtain the server version from
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If subject can't be set
     */
    public MimeMessageBuilder setMailerInfo(VersionService versionService) throws MessagingException {
        String mailerInfo;
        if (MailProperties.getInstance().isAppendVersionToMailerHeader() && null != versionService) {
            mailerInfo = new StringBuilder("Open-Xchange Mailer v").append(versionService.getVersion()).toString();
        } else {
            mailerInfo = VALUE_X_MAILER;
        }
        mime.setHeader(HEADER_X_MAILER, mailerInfo);
        return this;
    }

    /**
     * Set the {@link ChronosITipData#PROPERTY_NAME} header
     *
     * @param itipData The data to set, can be <code>null</code>
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If header can't be set
     */
    public MimeMessageBuilder setITipData(ChronosITipData itipData) throws MessagingException {
        if (null != itipData) {
            mime.setHeader(ChronosITipData.PROPERTY_NAME, ChronosITipData.encode(itipData));
        }
        return this;
    }

    /**
     * Set <code>MESSAGE_ID</code>, <code>IN_REPLY_TO</code> and the <code>REFERENCE</code>
     * header
     *
     * @param uid The unique ID of the calendar event
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException In case header can't be set
     */
    public MimeMessageBuilder setTracing(String uid) throws MessagingException {
        mime.setHeader(MessageHeaders.HDR_MESSAGE_ID, generateHeaderValue(uid, true));
        String reference = generateHeaderValue(uid, false);
        mime.setHeader(MessageHeaders.HDR_IN_REPLY_TO, reference);
        mime.setHeader(MessageHeaders.HDR_REFERENCES, reference);
        return this;
    }

    /**
     * Set the {@value #HEADER_ORGANIZATION} if the organization can be retrieved by the {@link ContactService}
     *
     * @param contactService The {@link ContactService}
     * @param session The {@link Session}
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If header can't be set
     */
    public MimeMessageBuilder setOrganization(ContactService contactService, Session session) throws MessagingException {
        if (null == contactService) {
            return this;
        }
        try {
            final String organization = contactService.getOrganization(session);
            if (null != organization && 0 < organization.length()) {
                mime.setHeader(HEADER_ORGANIZATION, organization);
            }
        } catch (final OXException e) {
            LOGGER.warn("Header \"Organization\" could not be set", e);
        }
        return this;
    }

    /**
     * Set the {@value #HEADER_DATE} header.
     *
     * @param timeZone The {@link TimeZone} to set as date header
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException If header can't be set
     * @throws OXException If date format can't be obtained
     */
    public MimeMessageBuilder setSentDate(TimeZone timeZone) throws MessagingException, OXException {
        /*
         * Set sent date in UTC time
         */
        if (mime.getSentDate() == null) {
            final MailDateFormat mdf = MimeMessageUtility.getMailDateFormat(timeZone.getID());
            synchronized (mdf) {
                mime.setHeader(HEADER_DATE, mdf.format(new Date()));
            }
        }
        return this;
    }

    /**
     * Set the auto generated flag via the {@value #HEADER_AUTO_SUBMITTED} header
     *
     * @return This {@link MimeMessageBuilder} instance
     * @throws MessagingException In case header can't be set
     */
    public MimeMessageBuilder setAutoGenerated() throws MessagingException {
        mime.setHeader(HEADER_AUTO_SUBMITTED, VALUE_AUTO_GENERATED);
        return this;
    }

    /**
     * Builds the {@link MimeMessage}
     *
     * @return The {@link MimeMessage}
     */
    public MimeMessage build() {
        return mime;
    }

    /*
     * ================ HELPERS ================
     */

    /**
     *
     * Checks if the given {@link InternetAddress} are not <code>null</code> or <code>empty</code>
     *
     * @param addresses The addresses to check
     * @param calendarUser For logging purpose
     * @throws OXException If address
     */
    private void checkAddress(InternetAddress[] addresses, CalendarUser calendarUser) throws OXException {
        if (null == addresses || 1 != addresses.length) {
            throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(calendarUser.getUri(), I(calendarUser.getEntity()), CalendarUserType.INDIVIDUAL);
        }
    }

    /**
     * Get the value for the header {@link Constants#HEADER_X_OX_TYPE}
     *
     * @param action The action that has been performed
     * @param partStat The participant status of the originator. Important in case of the <code>REPLY</code> action.
     * @return The value as String
     */
    private String getTypeHeaderValue(ChangeAction action, ParticipationStatus partStat) {
        switch (action) {
            case CREATE:
                return "New";
            case CANCEL:
                return "Deleted";
            case REPLY:
                if (ParticipationStatus.ACCEPTED.equals(partStat)) {
                    return "Accepted";
                }
                if (ParticipationStatus.DECLINED.equals(partStat)) {
                    return "Declined";
                }
                if (ParticipationStatus.TENTATIVE.equals(partStat)) {
                    return "Tentatively accepted";
                }
                return "Not yet accepted";
            case UPDATE:
                return "Modified";
            case NONE:
            default:
                return "Refresh";
        }
    }
}
