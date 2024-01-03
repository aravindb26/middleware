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

package com.openexchange.ajax.chronos.factory;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import com.google.common.collect.ImmutableList;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.java.Strings;
import com.openexchange.test.common.asset.Asset;
import com.openexchange.testing.httpclient.models.Alarm;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.ChronosAttachment;
import com.openexchange.testing.httpclient.models.DateTimeData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventData.TranspEnum;

/**
 * {@link EventFactory}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class EventFactory {

    /**
     * {@link RecurringFrequency} - The recurring frequency of a series
     * as described in <a href="https://tools.ietf.org/html/rfc5545#section-3.3.10">RFC-5545, Section 3.3.10</a>
     */
    public enum RecurringFrequency {
        SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY;
    }

    /**
     * {@link Weekday} - The weekday as described in <a href="https://tools.ietf.org/html/rfc5545#section-3.3.10">RFC-5545, Section 3.3.10</a>
     */
    public enum Weekday {
        SU, MO, TU, WE, TH, FR, SA;
    }

    /**
     * Creates a single event for the specified user with in the specified interval.
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param startDate The start date
     * @param endDate The end date
     * @return The {@link EventData}
     */
    public static EventData createSingleEvent(int userId, String summary, DateTimeData startDate, DateTimeData endDate) {
        return createSingleEvent(userId, summary, null, startDate, endDate, null);
    }

    /**
     * Creates a single event for the specified user with in the specified interval.
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param description The description of the event
     * @param startDate The start date
     * @param endDate The end date
     * @param folderId The folder id
     * @return The {@link EventData}
     */
    public static EventData createSingleEvent(int userId, String summary, String description, DateTimeData startDate, DateTimeData endDate, String folderId) {
        Attendee attendee = AttendeeFactory.createIndividual(I(userId));

        EventData singleEvent = new EventData();
        singleEvent.setPropertyClass("PUBLIC");
        ArrayList<Attendee> atts = new ArrayList<>();
        atts.add(attendee);
        singleEvent.setAttendees(atts);
        singleEvent.setStartDate(startDate);
        singleEvent.setEndDate(endDate);
        singleEvent.setTransp(TranspEnum.OPAQUE);
        singleEvent.setSummary(summary);
        singleEvent.setDescription(description);
        singleEvent.setFolder(folderId);

        return singleEvent;
    }

    /**
     * Creates a simple two hour event with the specified frequency and the specified weekday limited by the until value
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param until The until value for the recurrence rule
     * @param folderId The folder identifier
     * @param freq The recurring frequency
     * @param weekday The weekday the event is occuring
     * @return The series {@link EventData}
     */
    public static EventData createSeriesEvent(int userId, String summary, DateTimeData until, String folderId, RecurringFrequency freq, Weekday weekday) {
        EventData seriesEvent = createSingleTwoHourEvent(userId, summary, folderId);
        seriesEvent.setRrule(RRuleFactory.getFrequencyWithUntilLimit(freq, until, weekday));
        return seriesEvent;
    }

    /**
     * Creates a simple two hour event with the specified amount of occurrences and recurring frequency
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param occurences The number of occurences
     * @param folderId The folder identifier
     * @param freq The recurring frequency
     * @return The series {@link EventData}
     */
    public static EventData createSeriesEvent(int userId, String summary, int occurences, String folderId, RecurringFrequency freq) {
        EventData seriesEvent = createSingleTwoHourEvent(userId, summary, folderId);
        seriesEvent.setRrule(RRuleFactory.getFrequencyWithOccurenceLimit(freq, occurences));
        return seriesEvent;
    }

    /**
     * Creates a simple daily two hour event with the specified amount of occurrences
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @return The series {@link EventData}
     */
    public static EventData createSeriesEvent(int userId, String summary, int occurences, String folderId) {
        return createSeriesEvent(userId, summary, occurences, folderId, RecurringFrequency.DAILY);
    }

    /**
     * Creates a simple daily event series with the specified amount of occurrences
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param startDate The start date
     * @param endDate The end date
     * @param occurences The number of occurences
     * @return The series {@link EventData}
     */
    public static EventData createSeriesEvent(int userId, String summary, DateTimeData startDate, DateTimeData endDate, int occurences) {
        return createSeriesEvent(userId, summary, startDate, endDate, occurences, null);
    }

    /**
     * Creates a simple daily event series with the specified amount of occurrences
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param startDate The start date
     * @param endDate The end date
     * @param occurences The number of occurrences
     * @return The series {@link EventData}
     */
    public static EventData createSeriesEvent(int userId, String summary, DateTimeData startDate, DateTimeData endDate, int occurences, String folderId) {
        EventData seriesEvent = createSingleEvent(userId, summary, null, startDate, endDate, folderId);
        seriesEvent.setRrule(RRuleFactory.getFrequencyWithOccurenceLimit(RecurringFrequency.DAILY, occurences));
        return seriesEvent;
    }

    /**
     * Creates a series event
     *
     * @param userId The calendar user
     * @param summary The event summary
     * @param startDate The event start date
     * @param endDate The event end date
     * @param until The until value for the recurrence rule
     * @param freq The {@link RecurringFrequency} for the recurrence rule
     * @param weekday The {@link Weekday} for the recurrence rule
     * @param folderId The folde rid
     * @return The series {@link EventData}
     */
    public static EventData createSeriesEvent(int userId, String summary, DateTimeData startDate, DateTimeData endDate, DateTimeData until, RecurringFrequency freq, Weekday weekday, String folderId) {
        EventData seriesEvent = createSingleEvent(userId, summary, null, startDate, endDate, folderId);
        seriesEvent.setRrule(RRuleFactory.getFrequencyWithUntilLimit(freq, until, weekday));
        return seriesEvent;
    }

    /**
     * Creates a series event with the specified start and end date and with the specified recurrence rule
     *
     * @param userId The calendar user
     * @param summary The event summary
     * @param startDate The event start date
     * @param endDate The event end date
     * @param rRule The recurrence rule
     * @param folderId The folde rid
     * @return The series {@link EventData}
     */
    public static EventData createSeriesEvent(int userId, String summary, DateTimeData startDate, DateTimeData endDate, String rRule, String folderId) {
        EventData seriesEvent = createSingleEvent(userId, summary, null, startDate, endDate, folderId);
        seriesEvent.setRrule(rRule);
        return seriesEvent;
    }

    /**
     * Creates a series event with the specified start and end date, the specified recurrence rule and delete exception dates
     *
     * @param userId The calendar user
     * @param summary The event summary
     * @param startDate The event start date
     * @param endDate The event end date
     * @param rRule The recurrence rule
     * @param folderId The folde rid
     * @param deleteExceptions The list of change delete dates
     * @return The series {@link EventData}
     */
    public static EventData createSeriesEventWithDeleteExceptions(int userId, String summary, DateTimeData startDate, DateTimeData endDate, String rRule, String folderId, List<String> deleteExceptions) {
        EventData seriesEvent = createSingleEvent(userId, summary, null, startDate, endDate, folderId);
        seriesEvent.setRrule(rRule);
        seriesEvent.setDeleteExceptionDates(deleteExceptions);
        return seriesEvent;
    }

    /**
     * Creates a single event with the specified start and end time and with the specified attachment.
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param startDate The start date
     * @param endDate The end date
     * @param asset The {@link Asset} to attach
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithAttachment(int userId, String summary, Asset asset) {
        return createSingleEventWithAttachments(userId, summary, ImmutableList.of(asset), null);
    }

    /**
     * Creates a single event with the specified start and end time and with the specified attachment from OX drive.
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param eventFolder The folder where the event will be created
     * @param attachmentFileIds the drive file ids of the attachments
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithAttachment(int userId, String summary, String eventFolder, String attachmentFileId) {
        return createSingleEventWithAttachments(userId, summary, eventFolder, ImmutableList.of(attachmentFileId));
    }

    /**
     * Creates a single event with the specified start and end time and with the specified attachment from OX drive.
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param eventFolder The folder where the event will be created
     * @param attachmentFileId the drive file id of the attachment
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithAttachments(int userId, String summary, String eventFolder, List<String> attachmentFileIds) {
        EventData eventData = createSingleTwoHourEvent(userId, summary, eventFolder);
        attachFiles(eventData, attachmentFileIds);
        return eventData;
    }

    /**
     * Creates a single event with the specified start and end time and with the specified assets
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param eventFolder The folder where the event will be created
     * @param assets the assets to attach
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithAttachments(int userId, String summary, List<Asset> assets, String eventFolder) {
        EventData eventData = createSingleTwoHourEvent(userId, summary, eventFolder);
        attachAssets(eventData, assets);
        return eventData;
    }

    /**
     * Creates a single event with the specified start and end time and with the specified assets and files from OX drive
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param assets the assets to attach
     * @param fileIds the drive file ids of the attachments
     * @param eventFolder The folder where the event will be created
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithAttachments(int userId, String summary, List<Asset> assets, List<String> fileIds, String eventFolder) {
        EventData eventData = createSingleTwoHourEvent(userId, summary, eventFolder);
        attachAssets(eventData, assets);
        attachFiles(eventData, fileIds);
        return eventData;
    }

    /**
     * Attaches the specified assets to the event
     *
     * @param eventData the event
     * @param assets the assets to attach
     */
    private static void attachAssets(EventData eventData, List<Asset> assets) {
        for (Asset asset : assets) {
            eventData.addAttachmentsItem(createAttachment(asset));
        }
    }

    /**
     * Attaches the specified drive files to the event
     *
     * @param eventData The event
     * @param fileIds The files
     */
    private static void attachFiles(EventData eventData, List<String> fileIds) {
        for (String id : fileIds) {
            eventData.addAttachmentsItem(createDriveAttachment(id));
        }
    }

    /**
     * Creates a single event with the specified start and end time and with the specified attachment.
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param startDate The start date
     * @param endDate The end date
     * @param asset The {@link Asset} to attach
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithAttachment(int userId, String summary, Asset asset, String folderId) {
        return createSingleEventWithAttachments(userId, summary, Collections.singletonList(asset), folderId);
    }

    /**
     * Creates a single event with an alarm
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param alarm The alarm
     * @param folderId The folder where the event will be created
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithSingleAlarm(int userId, String summary, Alarm alarm, String folderId) {
        EventData eventData = createSingleTwoHourEvent(userId, summary, folderId);
        eventData.setAlarms(Collections.singletonList(alarm));
        return eventData;
    }

    /**
     * Creates a single event with an alarm and the specified start and end time
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param startDate The start date
     * @param endDate The end date
     * @param alarm The alarm
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithSingleAlarm(int userId, String summary, DateTimeData startDate, DateTimeData endDate, Alarm alarm) {
        return createSingleEventWithSingleAlarm(userId, summary, startDate, endDate, alarm, null);
    }

    /**
     * Creates a single event with an alarm and the specified start and end time
     *
     * @param userId The user identifier
     * @param summary The summary of the event
     * @param startDate The start date
     * @param endDate The end date
     * @param alarm The alarm
     * @param folderId The folder where the event will be created
     * @return The {@link EventData}
     */
    public static EventData createSingleEventWithSingleAlarm(int userId, String summary, DateTimeData startDate, DateTimeData endDate, Alarm alarm, String folder) {
        EventData eventData = createSingleEvent(userId, summary, null, startDate, endDate, folder);
        eventData.setAlarms(Collections.singletonList(alarm));
        return eventData;
    }

    /**
     * Creates a {@link ChronosAttachment} out of the specified {@link Asset}
     *
     * @param asset The {@link Asset}
     * @return The {@link ChronosAttachment}
     */
    public static ChronosAttachment createAttachment(Asset asset) {
        ChronosAttachment attachment = new ChronosAttachment();
        attachment.setFilename(asset.getFilename());
        attachment.setFmtType(asset.getAssetType().name());
        attachment.setUri("cid:file_0");

        return attachment;
    }

    /**
     * Creates a {@link ChronosAttachment} out of the specified file in OX drive
     *
     * @param fileId the file id
     * @return The attachment
     */
    public static ChronosAttachment createDriveAttachment(String fileId) {
        ChronosAttachment attachment = new ChronosAttachment();
        attachment.setUri("drive://" + fileId);

        return attachment;
    }

    /**
     * Creates a single event two-hour event
     *
     * @param userId The user identifier
     * @param summary The event's summary
     * @param folder The folder to set
     * @return The {@link EventData}
     */
    public static EventData createSingleTwoHourEvent(int userId, String summary, String folder) {
        return createSingleTwoHourEvent(userId, summary, null, folder);
    }

    /**
     * Creates a single event two-hour event
     *
     * @param userId The user identifier
     * @param summary The event's summary
     * @param folder The folder to set
     * @return The {@link EventData}
     */
    public static EventData createSingleTwoHourEvent(int userId, String summary, String description, String folder) {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        end.setTimeInMillis(end.getTimeInMillis() + TimeUnit.HOURS.toMillis(2));

        EventData result = createSingleEvent(userId, summary, description, DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end), null);
        result.setFolder(folder);
        return result;
    }

    /**
     * Creates a single event two-hour event
     *
     * @param userId The user identifier
     * @param summary The event's summary
     * @return The {@link EventData}
     */
    public static EventData createSingleTwoHourEvent(int userId, String summary) {
        return createSingleTwoHourEvent(userId, summary, null);
    }

    /**
     * Prepares a new event with necessary attributes set. Attributes for an delta event are:
     * <li>event ID</li>
     * <li>folder ID, if any is set</li>
     * <li>recurrence ID, if any is set</li>
     * <li>attendees, if any are set</li>
     * <li>conferences, if any are set</li>
     *
     * @param eventData The event do base an delta event on
     * @return An new event
     */
    public static EventData deltaOf(EventData eventData) {
        return prepareDeltaEvent(eventData);
    }

    /**
     * Prepares a new event with necessary attributes set. Attributes for an delta event are:
     * <li>event ID</li>
     * <li>folder ID, if any is set</li>
     * <li>recurrence ID, if any is set</li>
     * <li>attendees, if any are set</li>
     * <li>conferences, if any are set</li>
     *
     * @param eventData The event do base an delta event on
     * @return An new event
     */
    public static EventData prepareDeltaEvent(EventData eventData) {
        EventData delta = new EventData();
        delta.setId(eventData.getId());
        if (Strings.isNotEmpty(eventData.getFolder())) {
            delta.setFolder(eventData.getFolder());
        }
        if (null != eventData.getRecurrenceId()) {
            delta.setRecurrenceId(eventData.getRecurrenceId());
        }
        if (null != eventData.getAttendees()) {
            delta.setAttendees(new ArrayList<>(eventData.getAttendees()));
        }
        if (null != eventData.getConferences()) {
            delta.setConferences(new ArrayList<>(eventData.getConferences()));
        }
        return delta;
    }
}
