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
package com.openexchange.chronos.scheduling.impl.incoming;

import static com.openexchange.chronos.common.CalendarUtils.extractEMailAddress;
import static com.openexchange.chronos.common.CalendarUtils.optExtendedPropertyValue;
import static com.openexchange.chronos.scheduling.impl.incoming.MailUtils.closeMailAccess;
import static com.openexchange.chronos.scheduling.impl.incoming.MailUtils.getAttachmentPart;
import static com.openexchange.chronos.scheduling.impl.incoming.MailUtils.getMailAccess;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Calendar;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.IncomingCalendarObjectResource;
import com.openexchange.chronos.common.IncomingSchedulingMessageBuilder;
import com.openexchange.chronos.common.IncomingSchedulingObjectBuilder;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.ical.ICalParameters;
import com.openexchange.chronos.ical.ICalService;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.scheduling.IncomingSchedulingMailFactory;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.common.ChronosITipData;
import com.openexchange.chronos.scheduling.impl.ITipMailFlag;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.tools.alias.UserAliasUtility;
import com.openexchange.java.Enums;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.crypto.CryptographicAwareMailAccessFactory;
import com.openexchange.mail.dataobjects.IDMailMessage;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.mailaccount.UnifiedInboxUID;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Origin;
import com.openexchange.session.Session;
import com.openexchange.tools.arrays.Arrays;
import com.openexchange.tools.arrays.Collections;
import com.openexchange.user.User;
import com.openexchange.user.UserService;

/**
 * {@link IncomingSchedulingMailFactoryImpl}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class IncomingSchedulingMailFactoryImpl implements IncomingSchedulingMailFactory {

    protected static final Logger LOGGER = LoggerFactory.getLogger(IncomingSchedulingMailFactoryImpl.class);

    protected final ServiceLookup services;

    /**
     * Initializes a new {@link IncomingSchedulingMailFactoryImpl}.
     *
     * @param services The service lookup
     */
    public IncomingSchedulingMailFactoryImpl(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public IncomingSchedulingMessage create(CalendarSession session, int accountId, String folderId, String mailId, String sequenceId) throws OXException {
        return create(session, accountId, folderId, mailId, sequenceId, false);
    }

    @Override
    public IncomingSchedulingMessage createPatched(CalendarSession session, int accountId, String folderId, String mailId, String sequenceId) throws OXException {
        return create(session, accountId, folderId, mailId, sequenceId, true);
    }

    private IncomingSchedulingMessage create(CalendarSession session, int accountId, String folderId, String mailId, String sequenceId, boolean patchEvents) throws OXException {
        MailAccess<?, ?> mailAccess = null;
        MailMessage mail;
        try {
            /*
             * Load mail and gather meta data
             */
            mailAccess = getMailAccess(services.getOptionalService(CryptographicAwareMailAccessFactory.class), session.getSession(), accountId);
            mail = mailAccess.getMessageStorage().getMessage(folderId, mailId, false);
            return create(mailAccess, mail, sequenceId, session.getSession(), patchEvents);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    /**
     * Creates an {@link IncomingSchedulingMessage} based on the given mail and purifies specific
     * fields in the calendar object(s) to work with our internal model.
     *
     * @param mailAccess The access to use
     * @param mail The mail to get the data from
     * @param sequenceId The optional sequence ID of the iCAL file to process
     * @param session The user session
     * @return The mail parsed to an {@link IncomingSchedulingMessage}
     * @throws OXException In case the message can't be created
     */
    public IncomingSchedulingMessage createPatched(MailAccess<?, ?> mailAccess, MailMessage mail, String sequenceId, Session session) throws OXException {
        return create(mailAccess, checkAndFetchMail(mailAccess, mail), sequenceId, session, true);
    }

    private static MailMessage checkAndFetchMail(MailAccess<?, ?> mailAccess, MailMessage mail) throws OXException {
        if (IDMailMessage.class.isAssignableFrom(mail.getClass())) {
            /*
             * Enclosing types can't be loaded, re-load mail
             */
            return mailAccess.getMessageStorage().getMessage(mail.getFolder(), mail.getMailId(), false);
        }
        return mail;
    }

    /**
     * Creates an {@link IncomingSchedulingMessage} based on the given mail and purifies specific
     * fields in the calendar object(s) to work with our internal model.
     * <p>
     * Assumes that the {@link Account#DEFAULT_ID} is used
     * <p>
     * The target user for the scheduling message will always be the session user.
     *
     * @param mail The mail to get the data from
     * @param session The underlying session; may be a synthetic one for auto-processed messages
     * @return The mail parsed to an {@link IncomingSchedulingMessage}
     * @throws OXException In case the message can't be created
     */
    public IncomingSchedulingMessage createPatched(MailMessage mail, Session session) throws OXException {
        return create(null, mail, mail.getMailId(), session, true, -1, Account.DEFAULT_ID);
    }

    private IncomingSchedulingMessage create(MailAccess<?, ?> mailAccess, MailMessage mail, String sequenceId, Session session, boolean patchEvents) throws OXException {
        String folderId = mail.getFolder();
        MailFolder folder = mailAccess.getFolderStorage().getFolder(folderId);
        int userId = -1;
        /*
         * Check if the session user is working on behalf of another user
         */
        if (folder.isShared() && folder.getOwnerInfo() != null) {
            /*
             * Lookup if the folder is shared to the user
             */
            userId = folder.getOwnerInfo().getUserId();
            if (0 >= userId) {
                throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Unable to determine shared mail folder owner, unable to generate scheduling message");
            }
        } else if (Account.DEFAULT_ID != mailAccess.getMailConfig().getAccount().getId()) {
            /*
             * Look-up to whom the mail address belongs to
             */
            String mailAddress = mailAccess.getMailConfig().getAccount().getPrimaryAddress();
            if (mailAddress.indexOf(UnifiedInboxManagement.MAIL_ADDRESS_DOMAIN_PART) > 0) { // NOSONARLINT
                try {
                    UnifiedInboxUID uid = new UnifiedInboxUID(mail.getMailId());
                    int realAccountId = uid.getAccountId();
                    if (Account.DEFAULT_ID == realAccountId) {
                        // Nullify to have session user being considered as target user
                        mailAddress = null;
                    } else {
                        MailAccountStorageService mass = services.getOptionalService(MailAccountStorageService.class);
                        mailAddress = mass == null ? null : mass.getMailAccount(realAccountId, session.getUserId(), session.getContextId()).getPrimaryAddress();
                    }
                } catch (Exception e) {
                    // No Unified Mail identifier
                }
            }
            if (mailAddress != null) {
                ContextService contextService = services.getServiceSafe(ContextService.class);
                UserService userService = services.getServiceSafe(UserService.class);
                Context context = contextService.getContext(session.getContextId());
                User user = userService.getUser(session.getUserId(), context);
                if (false == UserAliasUtility.isAlias(mailAddress, user.getAliases())) {
                    /*
                     * Mail doesn't belong to current user, search for the correct user
                     */
                    User searchUser = userService.searchUser(mailAddress, context, false, false, false);
                    if (null != searchUser && searchUser.getId() != session.getUserId()) {
                        userId = searchUser.getId();
                    } else {
                        /*
                         * Probably doesn't belong to our calendar system at all, abort
                         */
                        OXException exception = CalendarExceptionCodes.UNEXPECTED_ERROR.create("Unable to find target calendar user");
                        LOGGER.warn("Unable to find target calendar user", exception);
                        throw exception;
                    }
                }
            }
        }
        return create(mailAccess, mail, sequenceId, session, patchEvents, userId, mailAccess.getAccountId());
    }

    private IncomingSchedulingMessage create(MailAccess<?, ?> mailAccess, MailMessage mail, String sequenceId, Session session, boolean patchEvents, int targetUser, int accountId) throws OXException {
        IncomingSchedulingObjectBuilder iMipBuilder = IncomingSchedulingObjectBuilder.newBuilder();
        iMipBuilder.setMailAccountId(String.valueOf(accountId));
        iMipBuilder.setMailId(mail.getMailId());
        iMipBuilder.setMailFolderId(mail.getFolder());
        convertFlags(mail, iMipBuilder);
        /*
         * Load & parse data from mail, link referenced attachments as needed
         */
        ImportedCalendar calendar = getCalendar(services, mailAccess, mail, sequenceId);
        if (patchEvents) {
            calendar = ITipPatches.applyAll(calendar);
        }
        List<Event> events = linkBinaryAttachments(mail, calendar.getEvents(), Origin.SYNTHETIC.equals(session.getOrigin()) ? null :
            new ShortLivingMailAccess(services, session, mail.getFolder(), mail.getMailId(), accountId));
        /*
         * Gather further meta data & build scheduling message
         */
        Map<String, Object> additionals = new HashMap<>();
        additionals.put("PRODID", calendar.getProdId());
        String itipData = optExtendedPropertyValue(calendar.getExtendedProperties(), ChronosITipData.PROPERTY_NAME, String.class);
        if (Strings.isNotEmpty(itipData)) {
            additionals.put(ChronosITipData.PROPERTY_NAME, ChronosITipData.decode(itipData));
        }
        return IncomingSchedulingMessageBuilder.newBuilder()
            .setTargetUser(0 < targetUser ? targetUser : session.getUserId())
            .setMethod(SchedulingMethod.valueOf(calendar.getMethod()))
            .setAdditionals(additionals)
            .setResource(new IncomingCalendarObjectResource(events))
            .setSchedulingObject(iMipBuilder.setOriginator(getOriginator(calendar, mail)).setMessageId(mail.getFirstHeader(MessageHeaders.HDR_MESSAGE_ID)).build())
            .build();
    }

    /*
     * ============================== HELPERS ==============================
     */

    /**
     * Extracts the <i>originator</i> from a calendar representing an iTIP message, optionally making use of additional address hints if
     * the message was received in an email.
     * <p/>
     * Depending on the <code>METHOD</code> property of the calendar, the originator is either the organizer of the scheduling object
     * resource, or the attendee specified within the resource.
     *
     * @param calendar The calendar to get the originator from
     * @param mail An email to gather additional hints from to choose the organizer if there are multiple possibilities
     * @return The extracted originator
     * @throws OXException If no originator could be extracted
     */
    private static CalendarUser getOriginator(Calendar calendar, MailMessage mail) throws OXException {
        ArrayList<InternetAddress> addressHints = new ArrayList<>(2);
        if (null != mail.getReplyTo()) {
            addressHints.addAll(java.util.Arrays.asList(mail.getReplyTo()));
        }
        if (null != mail.getFrom()) {
            addressHints.addAll(java.util.Arrays.asList(mail.getFrom()));
        }
        CalendarUser originator = getOriginator(calendar, addressHints.toArray(new InternetAddress[addressHints.size()]));
        if (null == originator) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Can't find originator.");
        }
        if (Strings.isEmpty(originator.getCn()) || null != originator.getSentBy() && Strings.isEmpty(originator.getSentBy().getCn())) {
            if (null != mail.getSender()) {
                addressHints.addAll(java.util.Arrays.asList(mail.getSender()));
            }
            return injectCNs(originator, addressHints);
        }
        return originator;
    }

    /**
     * Tries to inject display names for the calendar user (and the optionally set user in <code>SENT-BY</code>) using the personal part
     * from a matching internet address.
     *
     * @param calendarUser The calendar user to inject display names into
     * @param addresses The addresses to extract possible display names from
     * @return The supplied calendar user reference, enhanced with common names where possible
     */
    private static CalendarUser injectCNs(CalendarUser calendarUser, List<InternetAddress> addresses) {
        if (null != calendarUser && null != addresses && !addresses.isEmpty()) {
            if (Strings.isEmpty(calendarUser.getCn())) {
                String emailAddress = extractEMailAddress(calendarUser.getUri());
                if (null != emailAddress) {
                    for (InternetAddress address : addresses) {
                        if (null != address.getAddress() && address.getAddress().equalsIgnoreCase(emailAddress)) {
                            String personal = address.getPersonal();
                            if (Strings.isNotEmpty(personal)) {
                                calendarUser.setCn(personal);
                                break;
                            }
                        }
                    }
                }
            }
            if (null != calendarUser.getSentBy()) {
                calendarUser.setSentBy(injectCNs(calendarUser.getSentBy(), addresses));
            }
        }
        return calendarUser;
    }

    /**
     * Extracts the <i>originator</i> from a calendar representing an iTIP message, optionally making use of additional address hints if
     * the message was received in an email.
     * <p/>
     * Depending on the <code>METHOD</code> property of the calendar, the originator is either the organizer of the scheduling object
     * resource, or the attendee specified within the resource.
     *
     * @param calendar The calendar to get the originator from
     * @param addressHints Additional hints to choose the organizer if there are multiple possibilities
     * @return The extracted originator, or <code>null</code> if no applicable originator could be extracted
     */
    private static CalendarUser getOriginator(Calendar calendar, InternetAddress... addressHints) {
        SchedulingMethod method = Enums.parse(SchedulingMethod.class, calendar.getMethod(), null);
        if (null == method) {
            return null;
        }
        switch (method) {
            case PUBLISH:
            case REQUEST:
            case ADD:
            case CANCEL:
            case DECLINECOUNTER:
                return extractOrganizer(calendar);
            case REPLY:
            case REFRESH:
            case COUNTER:
                return extractAttendee(calendar, addressHints);
            default:
                return null;
        }
    }

    /**
     * Extracts the first organizer found in the events of the calendar.
     *
     * @param calendar The calendar to get the organizer from
     * @return The organizer, or <code>null</code> if there is none
     */
    private static Organizer extractOrganizer(Calendar calendar) {
        if (null != calendar && null != calendar.getEvents()) {
            for (Event event : calendar.getEvents()) {
                if (null != event.getOrganizer()) {
                    return event.getOrganizer();
                }
            }
        }
        return null;
    }

    /**
     * Extracts the first attendee found in the events of the calendar.
     *
     * @param calendar The calendar to get the attendee from
     * @param addressHints Additional hints to choose the attendee if there are multiple possibilities
     * @return The attendee, or <code>null</code> if there is none
     */
    private static Attendee extractAttendee(Calendar calendar, InternetAddress... addressHints) {
        if (null != calendar && null != calendar.getEvents()) {
            for (Event event : calendar.getEvents()) {
                List<Attendee> attendees = event.getAttendees();
                if (null == attendees || attendees.isEmpty()) {
                    continue;
                }
                /*
                 * select attendee based on address hints if applicable
                 */
                if (1 < attendees.size() && null != addressHints && 0 < addressHints.length) {
                    for (InternetAddress address : addressHints) {
                        if (null == address.getAddress()) {
                            continue;
                        }
                        for (Attendee attendee : attendees) {
                            if (address.getAddress().equalsIgnoreCase(CalendarUtils.extractEMailAddress(attendee.getUri()))) {
                                return attendee;
                            }
                        }
                    }
                }
                /*
                 * use first / only attendee, otherwise
                 */
                return attendees.get(0);
            }
        }
        return null;
    }

    /**
     * Get the iCAL file from the mail as-is and parses it into an {@link Calendar} object
     *
     * @param services The service lookup
     * @param mailAccess The access to mails, or <code>null</code> if not available
     * @param mail The loaded mail to process
     * @param sequenceId The attachments sequence identifier of the iCAL, or <code>null</code> for dynamic discovery
     * @return A {@link Calendar} containing the iCAL from the mail
     * @throws OXException If service is missing, iCAL can't be obtained or importing fails
     */
    private static ImportedCalendar getCalendar(ServiceLookup services, MailAccess<?, ?> mailAccess, MailMessage mail, String sequenceId) throws OXException {
        MailPart icalMailPart = discoverIcalMailPart(mailAccess, mail, sequenceId);
        if (null != icalMailPart) {
            /*
             * parse iTIP attachment from discovered mail part
             */
            return parseICalAttachment(services, icalMailPart);
        }
        /*
         * fail if not found
         */
        throw MailExceptionCode.ATTACHMENT_NOT_FOUND.create(sequenceId, mail.getMailId(), mail.getFolder());
    }

    private static ImportedCalendar parseICalAttachment(ServiceLookup services, MailPart iCalMailPart) throws OXException {
        InputStream iCal = null;
        try {
            iCalMailPart.loadContent();
            iCal = iCalMailPart.getInputStream();
            ICalService iCalService = services.getServiceSafe(ICalService.class);
            /*
             * Don't ignore unset fields explicitly. E.g. unset attachment field in incoming REQUEST method indicates removed attachments.
             */
            ICalParameters parameters = iCalService.initParameters();
            parameters.set(ICalParameters.IGNORE_UNSET_PROPERTIES, Boolean.FALSE);
            parameters.set(ICalParameters.IGNORE_ALARM, Boolean.TRUE);
            return iCalService.importICal(iCal, parameters);
        } finally {
            Streams.close(iCal);
        }
    }

    private static MailPart discoverIcalMailPart(MailAccess<?, ?> mailAccess, MailMessage mail, String sequenceId) throws OXException {
        if (Strings.isNotEmpty(sequenceId) && null != mailAccess) {
            return mailAccess.getMessageStorage().getAttachment(mail.getFolder(), mail.getMailId(), sequenceId);
        }
        return MailUtils.getIcalAttachmentPart(mail);
    }

    /*
     * ============================== Attachments ==============================
     */

    /**
     * Prepares attachment data by linking binary attachments to their representation in the mail
     *
     * @param mail The mail to get the attachments from
     * @param events The calendar data as events
     * @param access The mail access to use when lazy loading attachments, or <code>null</code> to fall-back to in-memory handling
     * @return The calendar data enriched with the initialized attachments
     */
    private List<Event> linkBinaryAttachments(MailMessage mail, List<Event> events, ShortLivingMailAccess access) {
        List<Event> modified = new ArrayList<>(events.size());
        for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
            Event event = iterator.next();
            if (Collections.isNotEmpty(event.getAttachments())) {
                try {
                    Event copy = EventMapper.getInstance().copy(event, null, (EventField[]) null);
                    copy.setAttachments(prepareBinaryAttachments(mail, copy.getAttachments(), access));
                    modified.add(copy);
                } catch (OXException e) {
                    LOGGER.debug("Unable to copy", e);
                }
            } else {
                modified.add(event);
            }
        }
        return modified;
    }

    /**
     * Prepares attachments transmitted with the incoming message
     *
     * @param mail The mail
     * @param originalAttachments The attachments to find
     * @param in The object to get the (binary) attachments from
     * @param access The mail access to use when lazy loading attachments, or <code>null</code> to fall-back to in-memory handling
     * @return The filtered and existing attachments
     */
    private static List<Attachment> prepareBinaryAttachments(MailMessage mail, List<Attachment> originalAttachments, ShortLivingMailAccess access) throws OXException {
        List<Attachment> attachments = new ArrayList<>(originalAttachments.size());
        for (Attachment attachment : originalAttachments) {
            MailPart part = getAttachmentPart(mail, attachment.getUri());
            if (null != part) {
                attachments.add(prepareAttachment(part, attachment, access));
            } else {
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    /**
     * Get an attachment based on the supplied part
     *
     * @param part The mail part aka. the attachment
     * @param ref The attachment transmitted in the as iCAL
     * @param access The access to perform lazy.laoding with, or <code>null</code> to load the attachment in memory
     * @return An adjusted attachment
     */
    private static Attachment prepareAttachment(MailPart part, Attachment ref, ShortLivingMailAccess access) {
        Attachment attachment = new Attachment();
        attachment.setUri(part.getContentId()); // use part, avoids adjusting
        attachment.setChecksum(ref.getChecksum());
        if (Strings.isNotEmpty(part.getFileName())) {
            // Prefer mail announced name, see Bug65533
            attachment.setFilename(part.getFileName());
        } else {
            attachment.setFilename(ref.getFilename());
        }
        if (Strings.isNotEmpty(part.getContentType().getBaseType())) {
            attachment.setFormatType(part.getContentType().getBaseType());
        } else if (Strings.isNotEmpty(ref.getFormatType())) {
            attachment.setFormatType(ref.getFormatType());
        }
        attachment.setSize(ref.getSize());
        if (null == access) {
            /*
             * No access for lazy loading, load into memory
             */
            ThresholdFileHolder fileHolder = new ThresholdFileHolder();
            try {
                fileHolder.write(part.getInputStream());
                attachment.setData(fileHolder);
            } catch (OXException e) {
                LOGGER.info("Unable to parse attachment: {}", e.getMessage(), e);
            } finally {
                if (fileHolder.getCount() <= 0) {
                    fileHolder.close();
                }
            }
        } else {
            /*
             * Lazy-load attachments since presence alone doens't mean the file is updated.
             */
            attachment.setData(new MailAttachmentFileHolder(access, part.getContentId(), attachment.getFilename(), attachment.getFormatType(), attachment.getSize()));
        }
        return attachment;
    }

    /**
     * Converts mail flags to the fitting iMIP state
     *
     * @param mail The iTIP mail
     * @param iMipBuilder The builder
     */
    private static void convertFlags(MailMessage mail, IncomingSchedulingObjectBuilder iMipBuilder) {
        String[] userFlags = mail.getUserFlags();
        if (Arrays.isNullOrEmpty(userFlags)) {
            return;
        }
        for (ITipMailFlag flag : ITipMailFlag.values()) {
            if (Arrays.contains(userFlags, flag.getFlag())) {
                iMipBuilder.setState(flag.convert());
                break;
            }
        }
    }

}
