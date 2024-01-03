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

package com.openexchange.chronos.scheduling.impl.transport;

import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.scheduling.common.Utils.getDisplayName;
import static com.openexchange.chronos.scheduling.common.Utils.getQuotedAddress;
import static com.openexchange.chronos.scheduling.common.Utils.isResource;
import static com.openexchange.chronos.scheduling.common.Utils.optResourceId;
import static com.openexchange.chronos.scheduling.common.Utils.selectDescribedEvent;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.mime.utils.MimeMessageUtility.parseAddressList;
import static java.lang.String.format;
import java.util.Locale;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.scheduling.ScheduleStatus;
import com.openexchange.chronos.scheduling.SchedulingMessage;
import com.openexchange.chronos.scheduling.changes.ChangeAction;
import com.openexchange.chronos.scheduling.changes.SentenceFactory;
import com.openexchange.chronos.scheduling.common.AbstractMailTransportProvider;
import com.openexchange.chronos.scheduling.common.ChronosITipData;
import com.openexchange.chronos.scheduling.common.DataSourceProvider;
import com.openexchange.chronos.scheduling.common.Messages;
import com.openexchange.chronos.scheduling.common.MimeMessageBuilder;
import com.openexchange.chronos.scheduling.common.MimePartFactory;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.contact.ContactService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.version.VersionService;

/**
 * {@link AllServingTransportProvider}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class AllServingTransportProvider extends AbstractMailTransportProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AllServingTransportProvider.class);

    private static final Property PREFER_NO_REPLY_FOR_IMIP = DefaultProperty.valueOf("com.openexchange.calendar.preferNoReplyForIMip", Boolean.FALSE);
    private static final Property PREFER_NO_REPLY_FOR_INTERNAL_USERS = DefaultProperty.valueOf("com.openexchange.calendar.preferNoReplyForNotifications", Boolean.FALSE);
    private static final Property USE_IMIP_FOR_INTERNAL_USERS = DefaultProperty.valueOf("com.openexchange.calendar.useIMipForInternalUsers", Boolean.FALSE);

    /**
     * Initializes a new {@link AllServingTransportProvider}.
     * 
     * @param serviceLookup The {@link ServiceLookup}
     */
    public AllServingTransportProvider(@NonNull ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    public int getRanking() {
        return 500;
    }

    @Override
    @NonNull
    public ScheduleStatus send(@NonNull Session session, @NonNull SchedulingMessage message) {
        /*
         * determine recipient
         */
        boolean internalRecipient = isInternalRecipient(message);
        InternetAddress recipientAddress;
        try {
            recipientAddress = getRecipientAddress(message, internalRecipient);
        } catch (Exception e) {
            LOG.warn("Unable to extract recipient mail address, unable to transport scheduling message {}", message, e);
            return ScheduleStatus.NO_TRANSPORT;
        }
        /*
         * build mime message
         */
        boolean preferNoReplyAccount;
        MimeMessage mimeMessage;
        try {
            preferNoReplyAccount = preferNoReplyAccount(session, internalRecipient);
            mimeMessage = buildMessage(session, message, recipientAddress, preferNoReplyAccount ? null : getUsersMail(session));
        } catch (Exception e) {
            LOG.warn("Unable to generate mail, unable to transport scheduling message {}", message, e);
            return ScheduleStatus.NOT_DELIVERED;
        }
        /*
         * send the generated mail
         */
        try {
            return transportMail(session, mimeMessage, preferNoReplyAccount);
        } catch (Exception e) {
            LOG.warn("Unable to generate mail, unable to transport scheduling message {}", message, e);
            return ScheduleStatus.NOT_DELIVERED;
        }
    }

    /**
     * Gets a value indicating whether the scheduling message is targeting an <i>internal</i> recipient or not.
     * 
     * @param message The scheduling message to check
     * @return <code>true</code> if the scheduling mesages's recipient is an <i>internal</i> entity, <code>false</code>, otherwise
     */
    private static boolean isInternalRecipient(SchedulingMessage message) {
        if (null != message.getRecipientSettings()) {
            return isInternal(message.getRecipientSettings().getRecipient(), message.getRecipientSettings().getRecipientType());
        }
        if (message.getRecipient() instanceof Attendee attendee) {
            return isInternal(attendee);
        }
        return isInternal(message.getRecipient(), CalendarUserType.INDIVIDUAL);
    }

    /**
     * Gets a value indicating whether the scheduling message is targeting recipient of a specific calendar user type or not.
     * 
     * @param message The scheduling message to check
     * @param type The type of the recipient to check
     * @return <code>true</code> if the scheduling mesages's recipient matches the given {@link CalendarUserType}, <code>false</code>, otherwise
     */
    private static boolean matchesRecipientType(SchedulingMessage message, CalendarUserType type) {
        if (null != message.getRecipientSettings()) {
            return CalendarUserType.INDIVIDUAL.matches(message.getRecipientSettings().getRecipientType());
        }
        if (message.getRecipient() instanceof Attendee attendee) {
            return CalendarUserType.INDIVIDUAL.matches(attendee.getCuType());
        }
        return true; // assume INDIVIDUAL by default
    }

    /**
     * Extracts the internet address of the recipient of the given scheduling message.
     * 
     * @param message The scheduling message to get the recipient's mail address for
     * @param fallbackToEMail <code>true</code> if the calendar user's email property may get used in case no <code>mailto:</code> URI
     *            is set, <code>false</code>, otherwise
     * @return The recipient's mail address
     */
    private static InternetAddress getRecipientAddress(SchedulingMessage message, boolean fallbackToEMail) throws AddressException, OXException {
        if (null == message) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("No recipient in scheduling message set");
        }
        InternetAddress[] addresses = parseAddressList(getQuotedAddress(message.getRecipient(), fallbackToEMail), false, false);
        if (null == addresses || 0 == addresses.length) {
            throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(message.getRecipient().getUri(), I(message.getRecipient().getEntity()), CalendarUserType.INDIVIDUAL);
        }
        return addresses[0];
    }

    /**
     * Constructs the MIME message for subsequent transport.
     * 
     * @param session The session
     * @param message The scheduling message
     * @param recipientAddress The recipient's address
     * @param senderAddress The sender's address
     * @return The MIME message
     */
    private MimeMessage buildMessage(Session session, SchedulingMessage message, InternetAddress recipientAddress, InternetAddress senderAddress) throws MessagingException, OXException {
        ChronosITipData itipData = getITipData(session, message);
        Map<String, String> additionals = getAdditionalHeaders(message);
        // @formatter:off
        MimeMessageBuilder builder = new MimeMessageBuilder(isInternalRecipient(message)) 
            .setSender(senderAddress)
            .setFrom(message.getResource(), message.getOriginator())
            .setTo(recipientAddress)
            .setSubject(getSubject(message))
            .setContent(prepareContent(session, itipData, message))
            .setITipData(itipData)
            .setAdditionalHeader(additionals)
            .setMailerInfo(serviceLookup.getOptionalService(VersionService.class))
            .setTracing(message.getResource().getUid())
            .setOrganization(serviceLookup.getOptionalService(ContactService.class), session)
            .setSentDate(message.getRecipientSettings().getTimeZone())
            .setAutoGenerated()
            .setOXHeader(message.getRecipient(), message.getScheduleChange().getAction(), selectDescribedEvent(message.getResource(), message.getScheduleChange().getChanges()), message.getScheduleChange().getOriginatorPartStat())
            .setReadReceiptHeader(message.getRecipient(), additionals)
            ;
        //@formatter:on
        return builder.build();
    }

    /**
     * Generates the iTIP data for the supplied scheduling message.
     * 
     * @param session The current session
     * @param message The scheduling message
     * @return The iITIP data
     */
    private ChronosITipData getITipData(Session session, SchedulingMessage message) throws OXException {
        int sentByResourceId = optResourceId(message.getRecipient().getSentBy(), message.getResource().getFirstEvent());
        String serverUid = serviceLookup.getServiceSafe(DatabaseService.class).getServerUid();
        return new ChronosITipData(serverUid, session.getContextId(), message.getScheduleChange().getAction(), sentByResourceId);
    }

    /**
     * Selects and initializes the appropriate {@link MimePartFactory} to generate the contents of the scheduling message.
     * 
     * @param session The session
     * @param itipData Chronos-specific ITip data to encode additionally
     * @param message The scheduling message
     * @return The prepared MIME part factory
     */
    private @NonNull MimePartFactory prepareContent(Session session, ChronosITipData iTipData, SchedulingMessage message) throws OXException {
        /*
         * check if full iMIP or notification message needs to be sent
         */
        if (isUseIMip(session, message)) {
            /*
             * prepare full iMIP parts, including attachments for 'external' recipients only
             */
            DataSourceProvider attachmentDataProvider = isInternalRecipient(message) ? null : (i) -> message.getAttachmentData(i);
            return new ExternalMimePartFactory(serviceLookup, iTipData, message.getScheduleChange(), message.getRecipientSettings(), message.getResource(), attachmentDataProvider, message.getOriginator(), message.getMethod());
        }
        /*
         * prepare informational notification parts, only
         */
        return new InternalMimePartFactory(serviceLookup, message.getScheduleChange(), message.getRecipientSettings());
    }

    /**
     * Gets a value indicating whether an <i>iMIP</i> message including iCalendar attachment, or a light-weight notification mail should
     * be generated, based on certain criteria of the message and configuration.
     *
     * @param session The session to decide the preference for
     * @param message The scheduling message
     * @return <code>true</code> if iMIP messages should be generated, <code>false</code> if notification mails are sufficient
     */
    private boolean isUseIMip(Session session, SchedulingMessage message) throws OXException {
        if (false == isInternalRecipient(message)) {
            return true; // iMIP for external recipients (always)
        }
        if (matchesRecipientType(message, CalendarUserType.INDIVIDUAL) && serviceLookup.getServiceSafe(LeanConfigurationService.class)
            .getBooleanProperty(message.getRecipient().getEntity(), session.getContextId(), USE_IMIP_FOR_INTERNAL_USERS)) {
            return true; // iMIP for internal user recipients (as configured)
        }
        if (null != message.getRecipient().getSentBy() && 0 < optResourceId(message.getRecipient().getSentBy(), message.getResource().getFirstEvent())) {
            return true; // iMIP for mails to booking delegates
        }
        if (0 < optResourceId(message.getOriginator(), message.getResource().getFirstEvent()) &&
            null != message.getOriginator().getSentBy() && isInternal(message.getOriginator().getSentBy(), CalendarUserType.INDIVIDUAL)) {
            return true;  // iMIP for mails from booking delegates
        }
        return false; // no iMIP necessary, otherwise
    }

    /**
     * Gets a value indicating whether to prefer the <i>no-reply</i> transport account when sending notification mails, or to stick to
     * the user's primary mail transport account instead.
     *
     * @param session The session to decide the preference for
     * @param isInternal <code>true</code> if an <i>internal</i> scheduling message is sent, <code>false</code>, otherwise
     * @return <code>true</code> if the no-reply account should be used, <code>false</code>, otherwise
     */
    private boolean preferNoReplyAccount(Session session, boolean isInternal) throws OXException {
        LeanConfigurationService configurationService = serviceLookup.getServiceSafe(LeanConfigurationService.class);
        if (isInternal) {
            return super.preferNoReplyAccount(session) || configurationService.getBooleanProperty(session.getUserId(), session.getContextId(), PREFER_NO_REPLY_FOR_INTERNAL_USERS);
        }
        boolean preferNoReply = configurationService.getBooleanProperty(session.getUserId(), session.getContextId(), PREFER_NO_REPLY_FOR_IMIP);
        return preferNoReply || super.preferNoReplyAccount(session);
    }

    private String getSubject(SchedulingMessage message) throws OXException {
        Event describedEvent = selectDescribedEvent(message.getResource(), message.getScheduleChange().getChanges());
        boolean isSentByResource = 0 < optResourceId(message.getRecipient().getSentBy(), describedEvent);
        Locale locale = message.getRecipientSettings().getLocale();
        StringHelper stringHelper = StringHelper.valueOf(locale);
        /*
         * determine subject based on scheduling method / change action, using alternative strings if a resource booking request is transported
         */
        String summary = describedEvent.getSummary();
        String resourceName = isSentByResource ? getDisplayName(message.getRecipient().getSentBy()) : null;
        switch (message.getMethod()) {
            case CANCEL:
                if (isSentByResource) {
                    return format(stringHelper.getString(Messages.RESOURCE_SUBJECT_CANCELLED_APPOINTMENT), resourceName, summary);
                }
                return format(stringHelper.getString(Messages.SUBJECT_CANCELLED_APPOINTMENT), summary);
            case COUNTER:
                return format(stringHelper.getString(Messages.SUBJECT_COUNTER_APPOINTMENT), summary);
            case DECLINECOUNTER:
                return format(stringHelper.getString(Messages.SUBJECT_DECLINECOUNTER), summary);
            case REFRESH:
                return format(stringHelper.getString(Messages.SUBJECT_REFRESH), summary);
            case REPLY:
                /*
                 * use special subjects for user or resource participation status
                 */
                ParticipationStatus partStat = message.getScheduleChange().getOriginatorPartStat();
                if (isResource(message.getOriginator(), describedEvent.getAttendees())) {
                    return format(stringHelper.getString(getResourcePartStatMessage(partStat)), getDisplayName(message.getOriginator()), summary);
                }
                return getPartStatSubject(serviceLookup.getServiceSafe(SentenceFactory.class), message.getOriginator(), partStat, locale, summary);
            case ADD:
            case PUBLISH:
            case REQUEST:
                /*
                 * prefer "updated" strings if indicated by change action
                 */
                if (ChangeAction.UPDATE.equals(message.getScheduleChange().getAction())) {
                    if (isSentByResource) {
                        return format(stringHelper.getString(Messages.RESOURCE_SUBJECT_CHANGED_APPOINTMENT), resourceName, summary);
                    }
                    return format(stringHelper.getString(Messages.SUBJECT_CHANGED_APPOINTMENT), summary);
                }
                if (isSentByResource) {
                    return format(stringHelper.getString(Messages.RESOURCE_SUBJECT_NEW_APPOINTMENT), resourceName, summary);
                }
                return format(stringHelper.getString(Messages.SUBJECT_NEW_APPOINTMENT), summary);
            default:
                LOG.warn("No specific subject for scheduling message with method \"{}\" found, falling back to generic summary.", message.getMethod());
                return summary;
        }
    }

    private static String getResourcePartStatMessage(ParticipationStatus partStat) {
        if (ParticipationStatus.ACCEPTED.matches(partStat)) {
            return Messages.RESOURCE_SUBJECT_ACCEPTED;
        }
        if (ParticipationStatus.TENTATIVE.matches(partStat)) {
            return Messages.RESOURCE_SUBJECT_TENTATIVE;
        }
        if (ParticipationStatus.DECLINED.matches(partStat)) {
            return Messages.RESOURCE_SUBJECT_DECLINED;
        }
        return Messages.RESOURCE_SUBJECT_NEEDS_ACTION;
    }

}
