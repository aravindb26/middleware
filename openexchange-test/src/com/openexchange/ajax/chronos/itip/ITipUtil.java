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

package com.openexchange.ajax.chronos.itip;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.jms.IllegalStateException;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.ical.ical4j.mapping.ICalMapper;
import com.openexchange.chronos.ical.impl.ICalUtils;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.common.Messages;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.ConversionDataSource;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.JSlobData;
import com.openexchange.testing.httpclient.models.JSlobsResponse;
import com.openexchange.testing.httpclient.models.MailAttachment;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailDestinationData;
import com.openexchange.testing.httpclient.models.MailImportResponse;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import com.openexchange.testing.httpclient.modules.MailApi;
import net.fortuna.ical4j.util.CompatibilityHints;

/**
 * {@link ITipUtil}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.3
 */
public class ITipUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ITipUtil.class);

    private static final String NOTIFY_ACCEPTED_DECLINED_AS_CREATOR = "notifyAcceptedDeclinedAsCreator";
    private static final String NOTIFY_ACCEPTED_DECLINED_AS_PARTICIPANT = "notifyAcceptedDeclinedAsParticipant";
    private static final String NOTIFY_NEW_MODIFIED_DELETED = "notifyNewModifiedDeleted";
    private static final String DELETE_INVITATION_MAIL_AFTER_ACTION = "deleteInvitationMailAfterAction";
    private static final String AUTO_PROCESS_IMIP = "autoProcessIMip";

    /** Machine readable folder name of the INBOX */
    public static final String FOLDER_MACHINE_READABLE = "default0%2FINBOX";
    /** Human readable folder name of the INBOX */
    public static final String FOLDER_HUMAN_READABLE = "default0/INBOX";

    /**
     * Initializes a new {@link ITipUtil}.
     */
    private ITipUtil() {}

    /**
     * Uploads a mail to the INBOX
     *
     * @param apiClient The {@link SessionAwareClient}
     * @param eml The mail to upload
     * @return {@link MailDestinationData} with set mail ID and folder ID
     * @throws Exception In case of error
     */
    public static MailDestinationData createMailInInbox(SessionAwareClient apiClient, String eml) throws Exception {
        File tmpFile = File.createTempFile("test", ".eml");
        FileWriterWithEncoding writer = new FileWriterWithEncoding(tmpFile, "ASCII");
        writer.write(eml);
        writer.close();

        MailApi mailApi = new MailApi(apiClient);
        MailImportResponse importMail = mailApi.importMail(FOLDER_HUMAN_READABLE, tmpFile, null, Boolean.TRUE);
        return importMail.getData().get(0);
    }

    /**
     * Converts a test user to an attendee
     *
     * @param convertee The user to convert
     * @return An {@link Attendee}
     */
    public static Attendee convertToAttendee(TestUser convertee) {
        return convertToAttendee(convertee, I(convertee.getUserId()));
    }

    /**
     * Converts a test user to an attendee
     *
     * @param convertee The user to convert
     * @param userId The user identifier
     * @return An {@link Attendee}
     */
    public static Attendee convertToAttendee(TestUser convertee, Integer userId) {
        Attendee attendee = AttendeeFactory.createAttendee(userId, CuTypeEnum.INDIVIDUAL);
        attendee.cn(convertee.getUser());
        attendee.email(convertee.getLogin());
        attendee.setUri("mailto:" + convertee.getLogin());
        return attendee;
    }

    /**
     * Constructs a body
     *
     * @param mailId The mail identifier
     * @return A {@link ConversionDataSource} body
     */
    public static ConversionDataSource constructBody(String mailId) {
        return constructBody(mailId, "1.3");
    }

    /**
     * Constructs a body
     *
     * @param mailId The mail identifier
     * @param sequenceId The identifier of the attachment sequence
     * @return A {@link ConversionDataSource} body
     */
    public static ConversionDataSource constructBody(String mailId, String sequenceId) {
        return constructBody(mailId, sequenceId, FOLDER_HUMAN_READABLE);
    }

    /**
     * Constructs a body
     *
     * @param mailData The {@link MailData}
     * @return A {@link ConversionDataSource} body
     * @throws Exception In case of assertion error
     */
    public static ConversionDataSource constructBody(MailData mailData) throws Exception {
        assertNotNull(mailData);
        return constructBody(mailData.getId(), extractITipAttachmentId(mailData, null), mailData.getFolderId());
    }

    /**
     * Constructs a body
     *
     * @param mailId The mail identifier
     * @param sequenceId The identifier of the attachment sequence
     * @param folderName The folder name of the mail
     * @return A {@link ConversionDataSource} body
     */
    public static ConversionDataSource constructBody(String mailId, String sequenceId, String folderName) {
        ConversionDataSource body = new ConversionDataSource();
        body.setComOpenexchangeMailConversionFullname(folderName);
        body.setComOpenexchangeMailConversionMailid(mailId);
        body.setComOpenexchangeMailConversionSequenceid(sequenceId);
        return body;
    }

    /**
     * Constructs the mail subject of an iMIP message where the attendee
     * has accepted an event (series)
     * <p>
     * <code>anton accepted the invitation: Foo</code>
     *
     * @param from The attendee replying
     * @param summary The summary of the event
     * @return The mail subject
     */
    public static String acceptSummary(String from, String summary) {
        return constructActionSummary("accepted", from, summary);
    }

    /**
     * Constructs the mail subject of an iMIP message where the attendee
     * has tentatively accepted an event (series)
     * <p>
     * <code>anton tentatively accepted the invitation: Foo</code>
     *
     * @param from The attendee replying
     * @param summary The summary of the event
     * @return The mail subject
     */
    public static String tentativeSummary(String from, String summary) {
        return constructActionSummary("tentatively accepted", from, summary);
    }

    /**
     * Constructs the mail subject of an iMIP message where the attendee
     * has declined an event (series)
     * <p>
     * <code>anton declined the invitation: Foo</code>
     *
     * @param from The attendee replying
     * @param summary The summary of the event
     * @return The mail subject
     */
    public static String declineSummary(String from, String summary) {
        return constructActionSummary("declined", from, summary);
    }

    private static String constructActionSummary(String action, String from, String summary) {
        return String.format(Messages.SUBJECT_STATE_CHANGED, from, action, summary);
    }

    /**
     * Constructs the mail subject of an iMIP message where the
     * event generically has changed
     *
     * @param summary The summary of the event
     * @return The mail subject
     */
    public static String changedSummary(String summary) {
        return String.format(Messages.SUBJECT_CHANGED_APPOINTMENT, summary);
    }

    /**
     * Constructs the mail subject of an iMIP message where the
     * event was deleted
     *
     * @param summary The summary of the event
     * @return The mail subject
     */
    public static String deletedSummary(String summary) {
        return String.format(Messages.SUBJECT_CANCELLED_APPOINTMENT, summary);
    }

    /**
     * Get the name of a shared INBOX folder based on the
     * sharing users mail address
     *
     * @param mail The sharing users mail
     * @return THe folder identifier of the shared INBOX
     */
    public static String getSharedInboxName(String mail) {
        return "default0/shared/" + mail;
    }

    /**
     * Receive a calendar notification from the inbox
     *
     * @param apiClient The {@link SessionAwareClient} to use
     * @param fromToMatch The mail of the originator of the message
     * @param subjectToMatch The summary of the event
     * @return The mail as {@link MailData}
     * @throws Exception If the mail can't be found or something mismatches
     */
    public static MailData receiveNotification(SessionAwareClient apiClient, String fromToMatch, String subjectToMatch) throws Exception {
        MailData notification = receiveIMip(apiClient, fromToMatch, subjectToMatch, -1, null);
        assertThat("Notification mails should have attachments", notification.getAttachments(), is(not(oneOf(nullValue(), emptyCollectionOf(MailAttachment.class)))));
        for (MailAttachment attachment : notification.getAttachments()) {
            assertThat("iCAL should not be present", is(not(B(ContentType.isMimeType(attachment.getContentType(), "text/calendar")))));
            assertThat("iCAL should not be present", is(not(B(ContentType.isMimeType(attachment.getContentType(), "application/ics")))));
        }
        return notification;
    }

    /**
     * Receive the iMIP message from the inbox
     *
     * @param apiClient The {@link SessionAwareClient} to use
     * @param fromToMatch The mail of the originator of the message
     * @param subjectToMatch The summary of the event
     * @param sequenceToMatch The sequence identifier of event to match, or <code>-1</code> if not applicable
     * @param method The iTIP method that the mail must contain, or <code>null</code> to skip checking for the event data
     * @return The mail as {@link MailData}
     * @throws Exception If the mail can't be found or something mismatches
     */
    public static MailData receiveIMip(SessionAwareClient apiClient, String fromToMatch, String subjectToMatch, int sequenceToMatch, SchedulingMethod method) throws Exception {
        return receiveIMip(apiClient, fromToMatch, subjectToMatch, sequenceToMatch, null, null, method);
    }

    /**
     * Receive the iMIP message from the inbox
     *
     * @param apiClient The {@link SessionAwareClient} to use
     * @param fromToMatch The mail of the originator of the message
     * @param subjectToMatch The summary of the event
     * @param sequenceToMatch The sequence identifier of the first event to match, or <code>-1</code> if not applicable
     * @param uidToMatch The UID of the calendar object resource to match, or <code>null</code> if not applicable
     * @param recurrenceIdToMatch The recurrence identifier of the calendar object resource to match, or <code>null</code> if not applicable
     * @param method The iTIP method that the mail must contain, or <code>null</code> to skip checking for the event data
     * @return The mail as {@link MailData}
     * @throws Exception If the mail can't be found or something mismatches
     */
    public static MailData receiveIMip(SessionAwareClient apiClient, String fromToMatch, String subjectToMatch, int sequenceToMatch, String uidToMatch, RecurrenceId recurrenceIdToMatch, SchedulingMethod method) throws Exception {
        return receiveIMip(apiClient, FOLDER_HUMAN_READABLE, fromToMatch, subjectToMatch, sequenceToMatch, uidToMatch, recurrenceIdToMatch, method, -1);
    }

    /**
     * Receive the iMIP message from the given mail folder
     *
     * @param apiClient The {@link SessionAwareClient} to use
     * @param folder The mail folder to search in
     * @param fromToMatch The mail of the originator of the message
     * @param subjectToMatch The summary of the event
     * @param sequenceToMatch The sequence identifier of the first event to match, or <code>-1</code> if not applicable
     * @param uidToMatch The UID of the calendar object resource to match, or <code>null</code> if not applicable
     * @param recurrenceIdToMatch The recurrence identifier of the calendar object resource to match, or <code>null</code> if not applicable
     * @param method The iTIP method that the mail must contain, or <code>null</code> to skip checking for the event data
     * @param dtStamp The DTSTAMP of the first event to match, or <code>-1</code> if not applicable
     * @return The mail as {@link MailData}
     * @throws Exception If the mail can't be found or something mismatches
     */
    public static MailData receiveIMip(SessionAwareClient apiClient, String folder, String fromToMatch, String subjectToMatch, int sequenceToMatch, String uidToMatch, RecurrenceId recurrenceIdToMatch, SchedulingMethod method, long dtStamp) throws Exception {
    	return new IMipReceiver(apiClient, folder)
            .from(fromToMatch)
            .subject(subjectToMatch)
            .sequence(-1 < sequenceToMatch ? Integer.valueOf(sequenceToMatch) : null)
            .uid(uidToMatch)
            .recurrenceId(recurrenceIdToMatch)
            .method(method)
            .dtstamp(-1 < dtStamp ? L(dtStamp) : null)
        .receive();
    }

    /**
     * Extracts the attachment id of a <code>text/calendar</code> attachment, optionally also matching the supplied method name.
     * <p/>
     * Fails if no such attachment was found.
     * 
     * @param mailData The mail data to extract the attachment id from
     * @param expectedMethod The method to match, or <code>null</code> is not applicable
     * @return The identifier of the matching attachment
     * @throws OXException
     */
    public static String extractITipAttachmentId(MailData mailData, SchedulingMethod expectedMethod) throws OXException {
        assertNotNull(mailData.getAttachments());
        for (MailAttachment attachment : mailData.getAttachments()) {
            if (ContentType.isMimeType(attachment.getContentType(), "text/calendar")) {
                if (null != expectedMethod && false == attachment.getContentType().contains(expectedMethod.name())) {
                    continue;
                }
                return attachment.getId();
            }
        }
        throw new AssertionError("no itip attachment found");
    }

    /**
     * Checks that there is no REPLY mail received from given attendee
     *
     * @param client The client to use
     * @param replyingAttendee The attendee that replies
     * @param summary The summary
     * @throws Exception Error while fetching mail
     */
    public static void checkNoReplyMailReceived(SessionAwareClient client, Attendee replyingAttendee, String summary) throws Exception {
        Error error = null;
        try {
            MailData mail = receiveIMip(client, replyingAttendee.getEmail(), summary, 1, SchedulingMethod.REPLY);
            LOGGER.error("Found reply mail: {}", mail.toString());
        } catch (AssertionError ae) {
            error = ae;
        }
        Assertions.assertNotNull(error, "Excpected an error");
    }

    /**
     * Extracts the iCalendar file from the given mail into an readable object
     *
     * @param apiClient The {@link SessionAwareClient} to receive the iCAlndar file with
     * @param mailData The {@link MailData} to get the attachment from
     * @return The calendar information from the file as {@link ImportedCalendar}
     * @throws Exception In case of error
     */
    public static ImportedCalendar parseICalAttachment(SessionAwareClient apiClient, MailData mailData) throws Exception {
        return parseICalAttachment(apiClient, mailData.getFolderId(), mailData.getId(), extractITipAttachmentId(mailData, null));
    }

    /**
     * Extracts the iCalendar file from the given mail into an readable object
     *
     * @param apiClient The {@link SessionAwareClient} to receive the iCAlndar file with
     * @param mailData The {@link MailData} to get the attachment from
     * @param expectedMethod The expected iTIP method
     * @return The calendar information from the file as {@link ImportedCalendar}
     * @throws Exception In case of error
     */
    public static ImportedCalendar parseICalAttachment(SessionAwareClient apiClient, MailData mailData, SchedulingMethod expectedMethod) throws Exception {
        return parseICalAttachment(apiClient, mailData.getFolderId(), mailData.getId(), extractITipAttachmentId(mailData, expectedMethod));
    }

    private static ImportedCalendar parseICalAttachment(SessionAwareClient apiClient, String folder, String id, String attachmentId) throws Exception {
        MailApi mailApi = new MailApi(apiClient);
        byte[] attachment = mailApi.getMailAttachment(folder, id, attachmentId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
        return ICalUtils.importCalendar(Streams.newByteArrayInputStream(attachment), new ICalMapper(), null);
    }

    /**
     *
     * Prepares a JSON object for the upload of an JPG attachment with the chronos API.
     *
     * @param id The event id
     * @param folder The folder of the event
     * @param fileName The file name
     * @return A JSON as {@link String}
     * @throws Exception
     */
    public static String prepareJsonForFileUpload(String id, String folder, String fileName) throws Exception {
        return prepareJsonForFileUpload(id, folder, fileName, "image/jpeg");
    }

    /**
     *
     * Prepares a JSON object for the upload of an event.
     *
     * @param id The event id
     * @param folder The folder of the event
     * @param fileName The file name
     * @param fileType The file type
     * @return A JSON as {@link String}
     * @throws Exception
     */
    public static String prepareJsonForFileUpload(String id, String folder, String fileName, String fileType) throws Exception {
        JSONObject json = new JSONObject();
        JSONObject event = new JSONObject();

        event.put("id", id);
        event.put("folder", folder);
        event.put("timestamp", Long.valueOf(System.currentTimeMillis()));

        JSONArray array = new JSONArray();
        JSONObject attachment = new JSONObject();
        attachment.put("filename", fileName);
        attachment.put("fmtType", fileType);
        attachment.put("uri", "cid:file_0");
        array.add(0, attachment);

        event.put("attachments", array);
        json.put("event", event);

        return json.toString();
    }

    /**
     * prepares the given attendee with the given participant status for an update via the updateAttendee action
     *
     * @param event The event to get the attendee from
     * @param mailAddress The mail address of the desired attendee
     * @param participantStatus The participant status to set
     * @param comment The comment to set to the updated attendee
     * @return The attendee prepared for a update vie updateAttendee action
     * @throws IllegalStateException If the attendee can't be found
     */
    public static AttendeeAndAlarm prepareForAttendeeUpdate(EventData event, String mailAddress, String participantStatus, String comment) throws IllegalStateException {
        Optional<Attendee> matchingAttendee = event.getAttendees().stream().filter(a -> a.getEmail().equals(mailAddress)).findFirst();
        Attendee originalAttendee = matchingAttendee.orElseThrow(() -> new IllegalStateException("Attendee not found"));

        Attendee attendee = copyAtttendee(originalAttendee);
        attendee.setPartStat(participantStatus);
        attendee.setMember(null); // Hack to avoid this being recognized as change
        attendee.setComment(comment);

        AttendeeAndAlarm attendeeAndAlarm = new AttendeeAndAlarm();
        attendeeAndAlarm.attendee(attendee);
        return attendeeAndAlarm;
    }

    /**
     * Copies all values from the original attendee to a new attendee object
     *
     * @param originalAttendee The attendee to copy
     * @return A new Attendee object with the same values
     */
    public static Attendee copyAtttendee(Attendee originalAttendee) {
        Attendee attendee = new Attendee();
        attendee.cn(originalAttendee.getCn());
        attendee.comment(originalAttendee.getComment());
        attendee.email(originalAttendee.getEmail());
        attendee.setUri(originalAttendee.getUri());
        attendee.setEntity(originalAttendee.getEntity());
        attendee.setPartStat(originalAttendee.getPartStat());
        attendee.setMember(originalAttendee.getMember());
        return attendee;
    }

    /**
     * Changes the calendar settings
     *
     * @param jslobApi The API client to use
     * @param notifyAcceptedDeclinedAsCreator <code>true</code> to receive notifications for participant changes as <b>ORGANIZER</b>
     * @param notifyAcceptedDeclinedAsParticipant <code>true</code> to receive notifications for participant changes as <b>ATTENDEE</b>
     * @param notifyNewModifiedDeleted <code>true</code> to receive notifications for new or deleted events
     * @param deleteInvitationMailAfterAction <code>true</code> to delete iMIP or notification mails after processing
     * @throws JSONException In case of error
     * @throws ApiException In case settings can't be set
     */
    public static void changeCalendarSettings(JSlobApi jslobApi, boolean notifyAcceptedDeclinedAsCreator, boolean notifyAcceptedDeclinedAsParticipant, boolean notifyNewModifiedDeleted, boolean deleteInvitationMailAfterAction) throws JSONException, ApiException {
        JSONObject jsonObject = new JSONObject(4);
        jsonObject.put(NOTIFY_ACCEPTED_DECLINED_AS_CREATOR, Boolean.toString(notifyAcceptedDeclinedAsCreator));
        jsonObject.put(NOTIFY_ACCEPTED_DECLINED_AS_PARTICIPANT, Boolean.toString(notifyAcceptedDeclinedAsParticipant));
        jsonObject.put(NOTIFY_NEW_MODIFIED_DELETED, Boolean.toString(notifyNewModifiedDeleted));
        jsonObject.put(DELETE_INVITATION_MAIL_AFTER_ACTION, Boolean.toString(deleteInvitationMailAfterAction));
        CommonResponse response = jslobApi.setJSlob(jsonObject, "io.ox/calendar", null);
        assertNotNull(response, "Response missing!");
        assertNull(response.getError());
    }

    /**
     * Changes the calendar settings
     *
     * @param jslobApi The API client to use
     * @param values The original values
     * @throws JSONException In case of error
     * @throws ApiException In case settings can't be set
     */
    public static void restoreCalendarSettings(JSlobApi jslobApi, Map<Object, Object> values) throws JSONException, ApiException {
        JSONObject jsonObject = new JSONObject(4);
        jsonObject.put(NOTIFY_ACCEPTED_DECLINED_AS_CREATOR, values.get(NOTIFY_ACCEPTED_DECLINED_AS_CREATOR).toString());
        jsonObject.put(NOTIFY_ACCEPTED_DECLINED_AS_PARTICIPANT, values.get(NOTIFY_ACCEPTED_DECLINED_AS_PARTICIPANT).toString());
        jsonObject.put(NOTIFY_NEW_MODIFIED_DELETED, values.get(NOTIFY_NEW_MODIFIED_DELETED).toString());
        jsonObject.put(DELETE_INVITATION_MAIL_AFTER_ACTION, values.get(DELETE_INVITATION_MAIL_AFTER_ACTION));
        CommonResponse response = jslobApi.setJSlob(jsonObject, "io.ox/calendar", null);
        assertNotNull(response, "Response missing!");
        assertNull(response.getError());
    }

    /**
     * Get the JSLob for <code>io.ox/calendar</code>
     *
     * @param jSlobApi The API client to use
     * @return The JSLob as {@link Map}
     * @throws ApiException In case of error
     */
    @SuppressWarnings("unchecked")
    public static Map<Object, Object> getJSLoabForCalendar(JSlobApi jSlobApi) throws ApiException {
        JSlobsResponse jSlobsResponse = jSlobApi.getJSlobList(Collections.singletonList("io.ox/calendar"), null);
        assertNotNull(jSlobsResponse);
        assertThat("No error expected!", jSlobsResponse.getError(), nullValue());
        JSlobData data = jSlobsResponse.getData().get(0);
        assertNotNull(data);
        return ((Map<Object, Object>) data.getTree());
    }

    /**
     * Enables the automatically processing of incoming iMIP mails
     *
     * @param testContext The context to disable the property in for each user
     * @throws ApiException In case settings can't be set
     */
    public static void setAutoProcessing(TestContext testContext) throws ApiException {
        for (TestUser user : testContext.getUsers()) {
            setAutoProcessing(new JSlobApi(user.getApiClient()), "always");
        }
    }

    /**
     * Set the automatically processing to <code>known</code> of incoming iMIP mails
     *
     * @param testContext The context to disable the property in for each user
     * @throws ApiException In case settings can't be set
     */
    public static void setAutoProcessingKnown(TestContext testContext) throws ApiException {
        for (TestUser user : testContext.getUsers()) {
            setAutoProcessing(new JSlobApi(user.getApiClient()), "known");
        }
    }

    /**
     * Disables the automatically processing of incoming iMIP mails
     *
     * @param testContext The context to disable the property in for each user
     * @throws ApiException In case settings can't be set
     */
    public static void disableAutoProcessing(TestContext testContext) throws ApiException {
        for (TestUser user : testContext.getUsers()) {
            disableAutoProcessing(new JSlobApi(user.getApiClient()));
        }
    }

    /**
     * Disables the automatically processing of incoming iMIP mails
     *
     * @param jslobApi The API client to use
     * @throws ApiException In case settings can't be set
     */
    public static void disableAutoProcessing(JSlobApi jslobApi) throws ApiException {
        setAutoProcessing(jslobApi, "never");
    }

    /**
     * Changes the calendar settings
     *
     * @param jslobApi The API client to use
     * @param value The value to set the property to, <code>always</code>, <code>never</code> or <code>known</code>
     * @throws ApiException In case settings can't be set
     */
    public static void setAutoProcessing(JSlobApi jslobApi, String value) throws ApiException {
        CommonResponse response = jslobApi.setJSlob(Collections.singletonMap("chronos", Collections.singletonMap(AUTO_PROCESS_IMIP, value)), "io.ox/calendar", null);
        assertNotNull(response, "Response missing!");
        assertNull(response.getError());
        JSlobsResponse jSlobsResponse = jslobApi.getJSlobList(Collections.singletonList("io.ox/calendar"), null);
        assertNotNull(jSlobsResponse, "Response missing!");
        assertNull(jSlobsResponse.getError());
        for (JSlobData jSlobData : jSlobsResponse.getData()) {
            Assertions.assertTrue(jSlobData.getTree().toString().toLowerCase().contains((AUTO_PROCESS_IMIP + "=" + value).toLowerCase()));
        }
    }

    /**
     * Searches for the HTML part of the mail, containing the detailed changes
     *
     * @param mail The mail to search in
     * @param content The content to find
     */
    public static void assertContent(MailData mail, String content) {
        assertContent(mail, content, "Didn' find content: " + content);
    }

    /**
     * Searches for the HTML part of the mail, containing the detailed changes
     *
     * @param mail The mail to search in
     * @param content The content to find
     * @param errorMsg The error message to throw when the content isn't found
     */
    public static void assertContent(MailData mail, String content, String errorMsg) {
        assertNotNull(mail);
        assertNotNull(content);
        for (MailAttachment attachment : mail.getAttachments()) {
            if ("text/html".equals(attachment.getContentType())) {
                assertTrue(attachment.getContent().contains(content), errorMsg);
                return;
            }
        }
        Assertions.fail("Unable to find HTML part of the mail");
    }

}
