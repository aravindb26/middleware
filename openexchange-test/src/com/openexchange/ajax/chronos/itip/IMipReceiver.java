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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.l;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailResponse;
import com.openexchange.testing.httpclient.models.MailsResponse;
import com.openexchange.testing.httpclient.modules.MailApi;

/**
 * {@link IMipReceiver}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class IMipReceiver {

    private final SessionAwareClient apiClient;
    private final String mailFolder;
    
    private String fromToMatch;
    private String subjectToMatch;
    private Integer sequenceToMatch;
    private String uidToMatch;
    private RecurrenceId recurrenceIdToMatch;
    private SchedulingMethod methodToMatch;
    private ParticipationStatus partStatToMatch;
    private Long dtStampToMatch;
    private boolean isParallelExecution;

    protected MailData result;

    /**
     * Initializes a new {@link IMipReceiver} which uses {@link ITipUtil#FOLDER_MACHINE_READABLE} as folder to lookup
     *
     * @param apiClient The API client to use
     */
    public IMipReceiver(SessionAwareClient apiClient) {
        this(apiClient, ITipUtil.FOLDER_HUMAN_READABLE);
    }
    /**
     * Initializes a new {@link IMipReceiver}
     *
     * @param apiClient The API client to use
     * @param mailFolder The mail folder to lookup the mail in
     */
    public IMipReceiver(SessionAwareClient apiClient, String mailFolder) {
        super();
        this.apiClient = apiClient;
        this.mailFolder = mailFolder;
    }

    /**
     * Set the from address to match
     *
     * @param fromToMatch The from address of the mail
     * @return This instance for chaining
     */
    public IMipReceiver from(String fromToMatch) {
        this.fromToMatch = fromToMatch;
        return this;
    }

    /**
     * Set the subject of the mail to match
     *
     * @param subjectToMatch The subject
     * @return This instance for chaining
     */
    public IMipReceiver subject(String subjectToMatch) {
        this.subjectToMatch = subjectToMatch;
        return this;
    }

    /**
     * Set the UID of the event in the mails iCAL attachment to match
     *
     * @param uidToMatch The event UID
     * @return This instance for chaining
     */
    public IMipReceiver uid(String uidToMatch) {
        this.uidToMatch = uidToMatch;
        return this;
    }

    /**
     * Set the recurrence ID of the event in the mails iCAL attachment to match
     *
     * @param recurrenceIdToMatch The recurrence ID of the event
     * @return This instance for chaining
     */
    public IMipReceiver recurrenceId(RecurrenceId recurrenceIdToMatch) {
        this.recurrenceIdToMatch = recurrenceIdToMatch;
        return this;
    }

    /**
     * Set the scheduling method of the event in the mails iCAL attachment to match
     *
     * @param methodToMatch The method
     * @return This instance for chaining
     */
    public IMipReceiver method(SchedulingMethod methodToMatch) {
        this.methodToMatch = methodToMatch;
        return this;
    }

    /**
     * Set the participant status of the sending attendee of the event in the mails iCAL attachment to match
     *
     * @param partStatToMatch The participant status
     * @return This instance for chaining
     */
    public IMipReceiver partStat(ParticipationStatus partStatToMatch) {
        this.partStatToMatch = partStatToMatch;
        return this;
    }

    /**
     * Set the sequence of the event in the mails iCAL attachment to match
     *
     * @param sequenceToMatch The sequence
     * @return This instance for chaining
     */
    public IMipReceiver sequence(Integer sequenceToMatch) {
        this.sequenceToMatch = sequenceToMatch;
        return this;
    }

    /**
     * Set the DTSTAMP of the event in the mails iCAL attachment to match
     *
     * @param dtStampToMatch The DTSTAMP
     * @return This instance for chaining
     */
    public IMipReceiver dtstamp(Long dtStampToMatch) {
        this.dtStampToMatch = dtStampToMatch;
        return this;
    }

    /**
     * Set the DTSTAMP, sequence, subject and UID of the event in the mails iCAL attachment to match
     *
     * @param event The event data to get the information from
     * @return This instance for chaining
     */
    public IMipReceiver event(EventData event) {
        return dtstamp(event.getLastModified()).sequence(event.getSequence()).subject(event.getSummary()).uid(event.getUid());
    }

    /**
     * Set a value whether the mail receiving should be done in parallel execution
     *
     * @param isParallelExecution <code>true</code> if receiving the mail should create multiple threads try receiving the mail
     * @return This instance for chaining
     */
    public IMipReceiver isParallelExecution(boolean isParallelExecution) {
        this.isParallelExecution = isParallelExecution;
        return this;
    }

    /**
     * Receives the desired iMIP mail based on the given attributes
     *
     * @return The iMIP mail
     * @throws Exception In case the mail can't be found
     */
    public MailData receive() throws Exception {
        if (isParallelExecution) {
            /*
             * Run in parallel
             */
            Runnable runnable = new ReceiveMailRunnable();
            Thread[] insertThreads = new Thread[10];
            for (int i = 0; i < insertThreads.length; i++) {
                insertThreads[i] = new Thread(runnable);
                insertThreads[i].start();
            }
            for (int i = 0; i < insertThreads.length; i++) {
                insertThreads[i].join();
            }
        } else {
            new ReceiveMailRunnable().run();
        }

        if (null != result) {
            return result;
        }
        throw new AssertionError("No mail with " + subjectToMatch + " from " + fromToMatch + " received");
    }

    /**
     * {@link ReceiveMailRunnable}
     *
     * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
     * @since v7.10.6
     */
    private final class ReceiveMailRunnable implements Runnable {

        /**
         * Initializes a new {@link ReceiveMailRunnable}.
         */
        public ReceiveMailRunnable() {
            super();
        }

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    MailData mailData = lookupMail();
                    if (null != mailData) {
                        result = mailData;
                        return;
                    }
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                } catch (Exception e) {
                    Assertions.fail(e.getMessage());
                }
            }
        }
    }

    /**
     * Tries to lookup the desired mail with the specified attributes set in this class
     *
     * @return The mail or <code>null</code> if not found
     * @throws Exception In case of error
     */
    MailData lookupMail() throws Exception {
        MailApi mailApi = new MailApi(apiClient);
        MailsResponse mailsResponse = mailApi.getAllMails(mailFolder, "600,601,607,610", null, null, null, "610", "desc", null, null, I(10), null);
        assertNull(mailsResponse.getError(), mailsResponse.getErrorDesc());
        assertNotNull(mailsResponse.getData());
        MailData matchingMailData = null;
        Long matchingMailDataTimestamp = null;
        for (List<Object> mail : mailsResponse.getData()) {
            String subject = mail.get(2).toString();
            if (Strings.isEmpty(subject) || false == subject.contains(subjectToMatch)) {
                continue;
            }
            MailResponse mailResponse = mailApi.getMail(mail.get(1).toString(), mail.get(0).toString(), null, null, "noimg", Boolean.FALSE, Boolean.TRUE, null, null, null, null, null, null, null);
            assertNull(mailsResponse.getError(), mailResponse.getError());
            assertNotNull(mailResponse.getData());
            MailData mailData = mailResponse.getData();
            if (null == extractMatchingAddress(mailData.getFrom(), fromToMatch)) {
                continue;
            }
            if (null == methodToMatch) {
                return mailData;
            }
            ImportedCalendar calendar = null;
            try {
                calendar = ITipUtil.parseICalAttachment(apiClient, mailData, methodToMatch);
            } catch (AssertionError e) {
                continue;
            }
            if (null == calendar) {
                continue;
            }
            Event matchingEvent = extractMatchingEvent(calendar.getEvents(), sequenceToMatch, uidToMatch, recurrenceIdToMatch, dtStampToMatch);
            if (null == matchingEvent) {
                continue;
            }
            if (null != partStatToMatch && null != matchingEvent.getAttendees()) {
                boolean found = false;
                for (Attendee attendee : matchingEvent.getAttendees()) {
                    if (partStatToMatch.matches(attendee.getPartStat())) {
                        found = true;
                        break;
                    }
                }
                if (false == found) {
                    continue;
                }
            }
            if (null == matchingMailDataTimestamp || matchingMailDataTimestamp.longValue() < matchingEvent.getTimestamp()) {
                matchingMailData = mailData;
                matchingMailDataTimestamp = Long.valueOf(matchingEvent.getTimestamp());
            }
        }
        return matchingMailData;
    }

    private static Event extractMatchingEvent(List<Event> events, Integer sequenceToMatch, String uidToMatch, RecurrenceId recurrenceIdToMatch, Long dtStampToMatch) {
        if (null != events) {
            for (Event event : events) {
                if (null != sequenceToMatch && event.getSequence() != sequenceToMatch.intValue()) {
                    continue;
                }
                if (null != uidToMatch && false == uidToMatch.equals(event.getUid())) {
                    continue;
                }
                if (null != recurrenceIdToMatch && false == recurrenceIdToMatch.matches(event.getRecurrenceId())) {
                    continue;
                }
                if (null != dtStampToMatch && TimeUnit.MILLISECONDS.toSeconds(l(dtStampToMatch)) != TimeUnit.MILLISECONDS.toSeconds(event.getDtStamp())) {
                    continue;
                }
                return event;
            }
        }
        return null;
    }

    private static List<String> extractMatchingAddress(List<List<String>> addresses, String email) {
        if (null != addresses) {
            for (List<String> address : addresses) {
                assertEquals(2, address.size());
                if (null != address.get(1) && address.get(1).contains(email)) {
                    return address;
                }
            }
        }
        return null;
    }

}
