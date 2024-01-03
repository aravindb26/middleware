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

import static com.openexchange.chronos.scheduling.common.Constants.ALTERNATIVE;
import static com.openexchange.chronos.scheduling.common.Constants.CANCLE_FILE_NAME;
import static com.openexchange.chronos.scheduling.common.Constants.INVITE_FILE_NAME;
import static com.openexchange.chronos.scheduling.common.Constants.MIXED;
import static com.openexchange.chronos.scheduling.common.Constants.RESPONSE_FILE_NAME;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ExtendedProperties;
import com.openexchange.chronos.ExtendedProperty;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.ical.CalendarExport;
import com.openexchange.chronos.ical.ICalService;
import com.openexchange.chronos.scheduling.RecipientSettings;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.changes.ScheduleChange;
import com.openexchange.chronos.scheduling.common.AbstractMimePartFactory;
import com.openexchange.chronos.scheduling.common.AttachmentDataSource;
import com.openexchange.chronos.scheduling.common.ChronosITipData;
import com.openexchange.chronos.scheduling.common.DataSourceProvider;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mime.MimeTypeMap;
import com.openexchange.server.ServiceLookup;

/**
 * {@link ExternalMimePartFactory} - Generates an iMIP mail
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 * @see <a href="https://tools.ietf.org/html/rfc6047">RFC6047 - iCalendar Message-Based Interoperability Protocol (<b>iMIP</b>)</a>
 */
public class ExternalMimePartFactory extends AbstractMimePartFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalMimePartFactory.class);

    private static final String CHARSET = MailProperties.getInstance().getDefaultMimeCharset();

    private ICalService iCalService;

    private final SchedulingMethod method;

    private final CalendarObjectResource resource;

    private final CalendarUser originator;

    private final DataSourceProvider provider;

    private final ChronosITipData itipData;

    /**
     * Initializes a new {@link ExternalMimePartFactory}.
     * 
     * @param serviceLookup The {@link ServiceLookup}
     * @param itipData Chronos-specific ITip data to encode additionally
     * @param scheduleChange The change to add to the mail
     * @param recipientSettings The recipient settings
     * @param resource The {@link CalendarObjectResource} to append to the mail
     * @param provider The provider to get attachments, can be <code>null</code>. However, if the provider is <code>null</code>, attachment won't be appended to the mail and internal IDs will be kept in the iCAL
     * @param originator The originator of the message
     * @param method The {@link SchedulingMethod}
     * @throws OXException In case services are missing
     */
    public ExternalMimePartFactory(ServiceLookup serviceLookup, ChronosITipData itipData, ScheduleChange scheduleChange, RecipientSettings recipientSettings, CalendarObjectResource resource, DataSourceProvider provider, CalendarUser originator, SchedulingMethod method) throws OXException {
        super(serviceLookup, scheduleChange, recipientSettings);
        this.itipData = itipData;
        this.iCalService = serviceLookup.getServiceSafe(ICalService.class);
        this.resource = resource;
        this.provider = provider;
        this.originator = originator;
        this.method = method;
    }

    /**
     * Adds the mime parts based on the given message
     * 
     * @return The {@link MimeMultipart} to send
     * 
     * @throws OXException
     * @throws MessagingException
     */
    @Override
    public MimeMultipart create() throws OXException, MessagingException {
        boolean addAttachments = addAttachments();
        MimeMultipart multipart = new MimeMultipart(MIXED);
        /*
         * Add event's attachments
         */
        if (addAttachments) {
            multipart = generateAttachmentPart(multipart);
        }

        /*
         * Set text, HTML and embedded iCal part
         */
        MimeBodyPart part = new MimeBodyPart();
        {
            MimeMultipart alternative = new MimeMultipart(ALTERNATIVE);
            alternative.addBodyPart(generateTextPart());
            alternative.addBodyPart(generateHtmlPart());
            alternative.addBodyPart(generateIcalPart(addAttachments));
            MessageUtility.setContent(alternative, part);
        }
        multipart.addBodyPart(part);

        /*
         * Add the iCal file as attachment
         */
        multipart.addBodyPart(generateIcalAttachmentPart(addAttachments));

        return multipart;
    }

    /*
     * ----------------------------- HELPERS -----------------------------
     */

    /**
     * Check if the {@link CalendarObjectResource} has any attachments to add to the mail.
     * 
     * @return <code>true</code> if at least one internal attachment needs to be added to the mail, <code>false</code> otherwise
     */
    private boolean addAttachments() {
        if (null == provider) {
            return false;
        }
        switch (method) {
            case CANCEL:
            case REPLY:
            case DECLINECOUNTER:
                return false;
            default:
                break;
        }
        for (Event e : resource.getEvents()) {
            if (e.containsAttachments() && null != e.getAttachments() && false == e.getAttachments().isEmpty()) {
                for (Attachment a : e.getAttachments()) {
                    if (a.getManagedId() > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private MimeBodyPart generateIcalAttachmentPart(boolean addAttachments) throws MessagingException, OXException {
        String fileName = determineFileName();
        ContentType ct = new ContentType();
        ct.setPrimaryType("application");
        ct.setSubType("ics");
        ct.setNameParameter(fileName);
        ct.setParameter("method", method.name());
        ct.setCharsetParameter(CHARSET);

        MimeBodyPart part = generateIcal(ct, addAttachments, false);
        /*
         * Content-Disposition & Content-Transfer-Encoding
         */
        ContentDisposition cd = new ContentDisposition();
        cd.setAttachment();
        cd.setFilenameParameter(fileName);
        part.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, cd.toString());
        part.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, "base64");
        return part;

    }

    private MimeBodyPart generateIcalPart(boolean addAttachments) throws MessagingException, OXException {
        ContentType ct = new ContentType();
        ct.setPrimaryType(TEXT);
        ct.setSubType("calendar");
        ct.setParameter("method", method.name());
        ct.setCharsetParameter(CHARSET);

        return generateIcal(ct, addAttachments, true);
    }

    private MimeBodyPart generateIcal(ContentType contentType, boolean addAttachments, boolean checkASCII) throws MessagingException, OXException {
        MimeBodyPart icalPart = new MimeBodyPart();

        CalendarExport export = iCalService.exportICal(iCalService.initParameters());
        export.setMethod(method.name());
        if (null != itipData) {
            export.add(new ExtendedProperty(ChronosITipData.PROPERTY_NAME, ChronosITipData.encode(itipData)));
        }

        for (Event e : resource.getEvents()) {
            Event event = EventMapper.getInstance().copy(e, new Event(), (EventField[]) null);
            event = prepareEvent(event);
            switch (method) {
                case REPLY:
                    event = adjustForReply(event);
                    break;
                case CANCEL:
                    adjustSequence(event);
                    break;
                default:
                    // Do nothing special
                    break;
            }
            export.add(addAttachments ? adjustAttchmentReference(event) : event);
        }

        byte[] icalFile = export.toByteArray();
        logWarning(export.getWarnings());

        final String type = contentType.toString();
        icalPart.setDataHandler(new DataHandler(new MessageDataSource(icalFile, type)));
        icalPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MimeMessageUtility.foldContentType(type));
        if (checkASCII) {
            icalPart.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, isAscii(icalFile) ? "7bit" : "quoted-printable");
        }

        return icalPart;
    }

    /**
     * Generally prepares the event event prior export.
     *
     * @param event The event to export
     * @return The prepared event
     * @throws OXException In case of error
     */
    private Event prepareEvent(Event event) throws OXException {
        for (ListIterator<Attendee> iterator = event.getAttendees().listIterator(); iterator.hasNext();) {
            Attendee attendee = iterator.next();
            if (null == attendee.getPartStat()) {
                attendee = AttendeeMapper.getInstance().copy(attendee, null, (AttendeeField[]) null);
                attendee.setPartStat(ParticipationStatus.NEEDS_ACTION);
                iterator.set(attendee);
            }
        }

        return event;
    }

    /**
     * Adjusts (increments) the sequence for the CANCEL method
     *
     * @param event The event to adjust
     * @return The adjusted event
     * @see <a href="https://tools.ietf.org/html/rfc5546#section-2.1.4">RFC 5546</a>
     */
    private Event adjustSequence(Event event) {
        event.setSequence(event.getSequence() + 1);
        return event;
    }

    /**
     * Adjusts the event for a REPLY method by:
     * <li>Reducing the attendees list to the sending user</li>
     * <li>Set the sent-by field if the attendee isn't replying on its own</li>
     * <li>Setting the attendee's comment to the VEVENT</li>
     * <li>Changing the DTSAMP value of the VEVENT to the timestamp the attendee was last modified (e.g. changed participant status)</li>
     *
     * @param event The event to adjust
     * @return The adjusted event
     * @see <a href="https://tools.ietf.org/html/rfc5546#section-3.2.3">RFC 5546</a>
     */
    private Event adjustForReply(Event event) {
        Attendee attendee = CalendarUtils.find(event.getAttendees(), originator);
        try {
            attendee = AttendeeMapper.getInstance().copy(attendee, null, (AttendeeField[]) null);
            if (null != originator.getSentBy()) {
                attendee.setSentBy(originator.getSentBy());
            }
        } catch (OXException e) {
            LOGGER.warn("Unable to copy attendee and thus unable to set sent-by field", e);
        }
        addComment(event, attendee);
        event.setAttendees(Collections.singletonList(attendee));
        if (CalendarUtils.hasExternalOrganizer(event)) {
            event.setDtStamp(attendee.getTimestamp());
        }
        return event;
    }

    /**
     * Removes the comment from the attendee and adds it to the outgoing event
     *
     * @param event The outgoing event
     * @param attendee The attendee with the optional comment
     * @see <a href="hhttps://tools.ietf.org/html/rfc5546#section-3.2.3">RFC5546 - REPLY</a>
     */
    private void addComment(Event event, Attendee attendee) {
        if (Strings.isNotEmpty(attendee.getComment())) {
            ExtendedProperties props = event.getExtendedProperties();
            if (null == props) {
                props = new ExtendedProperties();
            }
            props.add(new ExtendedProperty("COMMENT", attendee.getComment()));
            event.setExtendedProperties(props);
        }
        attendee.removeComment();
    }

    /**
     * Adjust the URI parameter of attachments. New URI will be the reference
     * to the attachments added to the mail.
     * <p/>
     * Other attachments are taken over as-is.
     * 
     * @param event The {@link Event} to get the attachments from
     * @return The modified event with adjusted attachments URIs
     */
    private Event adjustAttchmentReference(Event event) {
        if (false == event.containsAttachments() || null == event.getAttachments()) {
            return event;
        }
        List<Attachment> attachments = new LinkedList<Attachment>();
        for (Attachment attachment : event.getAttachments()) {
            if (attachment.getManagedId() > 0) {
                Attachment a = new Attachment();
                a.setCreated(attachment.getCreated());
                a.setFilename(attachment.getFilename());
                setFormatType(a, attachment.getFormatType());
                a.setSize(attachment.getSize());
                a.setUri("cid:" + getAttachmentUri(attachment.getManagedId(), event.getUid()));
                a.setManagedId(0);
                attachments.add(a);
            } else {
                attachments.add(attachment);
            }
        }
        event.setAttachments(attachments);
        return event;
    }

    /**
     * Generates the {@link MimeBodyPart} for attachments related to {@link CalendarObjectResource#getEvent()}
     * Implicit assumption is that all attachments set in {@link CalendarObjectResource#getAttachemnts()} exclusively must
     * be added to the mail and are referenced in the event.
     * 
     * @return {@link MimeBodyPart} Containing the attachments
     * @throws OXException In case of error
     */
    private MimeMultipart generateAttachmentPart(MimeMultipart multipart) throws OXException {
        if (null == provider) {
            return multipart;
        }
        for (Event event : resource.getEvents()) {
            if (false == event.containsAttachments() || event.getAttachments().isEmpty()) {
                continue;
            }
            for (Attachment attachment : event.getAttachments()) {
                if (attachment.getManagedId() > 0) {
                    try {
                        generateAttachmentPart(multipart, attachment, event.getUid());
                    } catch (MessagingException e) {
                        LOGGER.error("Unexpected error while attaching attachments to iMIP mail", e);
                    }
                }
            }
        }
        return multipart;
    }

    /**
     * Creates an {@link MimeBodyPart} with the given attachment and appends it to the mail
     * 
     * @param multipart The mail as {@link MimeMultipart}
     * @param holder The attachment as {@link IFileHolder}
     * @param managedId The identifier of the attachment
     * @param uid The unique identifier of the event the attachment belongs to
     * @throws OXException If setting or getting attachment fails
     * @throws MessagingException If attachment can#t be set
     * @throws IOException If attachments stream can't be read
     */
    private void generateAttachmentPart(MimeMultipart multipart, Attachment attachment, String uid) throws OXException, MessagingException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        ContentType ct;
        String mimeType = attachment.getFormatType();
        if (Strings.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        ct = new ContentType(mimeType);

        bodyPart.setDataHandler(new DataHandler(new AttachmentDataSource(provider, attachment)));

        final String fileName = attachment.getFilename();
        if (Strings.isNotEmpty(fileName)) {
            ct.setNameParameter(fileName);
            final ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
            cd.setFilenameParameter(fileName);
            bodyPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, MimeMessageUtility.foldContentDisposition(cd.toString()));
        }

        bodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MimeMessageUtility.foldContentType(ct.toString()));
        bodyPart.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");

        bodyPart.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, "base64");
        bodyPart.setHeader(MessageHeaders.HDR_CONTENT_ID, "<" + getAttachmentUri(attachment.getManagedId(), uid) + ">");

        multipart.addBodyPart(bodyPart);
    }

    /**
     * Logs all given warnings
     * 
     * @param warnings The warnings to log
     */
    private void logWarning(List<OXException> warnings) {
        if (warnings != null && !warnings.isEmpty()) {
            for (OXException warning : warnings) {
                LOGGER.warn(warning.getMessage(), warning);
            }
        }
    }

    /**
     * Get the file name for the .ics file
     * 
     * @return The file name based on the {@link SchedulingMethod}
     */
    private String determineFileName() {
        switch (method) {
            case REQUEST:
                return INVITE_FILE_NAME;
            case CANCEL:
                return CANCLE_FILE_NAME;
            default:
                return RESPONSE_FILE_NAME;
        }
    }

    private boolean isAscii(final byte[] bytes) {
        boolean isAscci = true;
        for (int i = 0; isAscci && (i < bytes.length); i++) {
            isAscci = (bytes[i] >= 0);
        }
        return isAscci;
    }

    /**
     * Gets a unique identifier for attachments
     * 
     * @param managedId The managed ID of the attachment
     * @param uid The unique ID of the event
     * @return A unique identifier for the attachment
     */
    private String getAttachmentUri(int managedId, String uid) {
        return new StringBuilder().append(managedId).append('@').append(uid).toString();
    }

    /**
     * Sets the format type of an attachment
     *
     * @param attachment The attachment to set the type for
     * @param formatType The format. Can be <code>null</code>, then the type is looked up by its file name
     */
    private void setFormatType(Attachment attachment, String formatType) {
        String type = formatType;
        if (Strings.isEmpty(type)) {
            MimeTypeMap mimeTypeMap = services.getOptionalService(MimeTypeMap.class);
            if (null != mimeTypeMap) {
                type = mimeTypeMap.getContentType(attachment.getFilename());
            } else {
                type = com.openexchange.mail.mime.MimeTypes.MIME_APPL_OCTET;
            }
            LOGGER.trace("Auto detected MIME type {} for attchment {}", type, attachment.getFilename());
        }
        attachment.setFormatType(type);
    }
}
