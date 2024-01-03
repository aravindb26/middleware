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

package com.openexchange.caldav;

import static com.openexchange.chronos.common.CalendarUtils.getEventsByUID;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.sortSeriesMasterFirst;
import static com.openexchange.java.Autoboxing.I;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;
import com.google.common.io.BaseEncoding;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.caldav.resources.EventResource;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ExtendedProperties;
import com.openexchange.chronos.ExtendedProperty;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.provider.composition.IDMangling;
import com.openexchange.chronos.service.UpdateResult;
import com.openexchange.dav.AttachmentUtils;
import com.openexchange.dav.DAVOAuthScope;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.tasks.TaskExceptionCode;
import com.openexchange.groupware.tools.mappings.Mapping;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;
import com.openexchange.webdav.protocol.WebdavPath;


/**
 * {@link Tools}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Tools {

    public static final String DEFAULT_ACCOUNT_PREFIX = "cal://0/";

    public static final String EXTENSION_ICS = ".ics";

    /**
     * The OAuth scope token for CalDAV
     */
    public static final String OAUTH_SCOPE = DAVOAuthScope.CALDAV.getScope();

    /** The event fields contributing to a an event resource's schedule tag */
    public static final EventField[] SCHEDULE_TAG_FIELDS = new EventField[] {
        EventField.ID, EventField.SERIES_ID, EventField.RECURRENCE_ID, EventField.SEQUENCE, EventField.START_DATE, EventField.END_DATE, 
        EventField.RECURRENCE_RULE, EventField.RECURRENCE_DATES, EventField.DELETE_EXCEPTION_DATES, EventField.CHANGE_EXCEPTION_DATES
    };
    
    /**
     * Gets the <i>significant</i> events for a collection of arbitrary events from multiple calendar object resources.
     *
     * @param events The events get the significant events from
     * @return The significant events, or an empty list if passed event collection was null or empty
     */
    public static List<Event> getSignificantEvents(List<Event> events) {
        if (null == events || events.isEmpty()) {
            return Collections.emptyList();
        }
        List<Event> significantEvents = new ArrayList<Event>(events.size());
        for (List<Event> eventGroup : getEventsByUID(events, false).values()) {
            Event event = getSignificantEvent(eventGroup);
            if (null != event) {
                significantEvents.add(event);
            }
        }
        return significantEvents;
    }

    /**
     * Gets the <i>significant</i> event of multiple events within the same calendar object resource (with the same unique identifier).
     * This is the event itself for non-recurring events, or the series master event for a recurring event series with change exceptions.
     * In case only change exceptions are contained in the collection, a <i>phantom master</i> event is used implicitly.
     *
     * @param events The events of a calendar object resource to get the significant event from
     * @return The significant event, or <code>null</code> if passed event collection was null or empty
     */
    public static Event getSignificantEvent(List<Event> events) {
        if (null == events || events.isEmpty()) {
            return null;
        }
        if (1 == events.size()) {
            return isSeriesException(events.get(0)) ? new PhantomMaster(events) : events.get(0);
        }
        List<Event> eventGroup = sortSeriesMasterFirst(events);
        return isSeriesMaster(eventGroup.get(0)) ? eventGroup.get(0) : new PhantomMaster(eventGroup);
    }

    public static String encodeFolderId(String folderId) {
        return null == folderId ? null : BaseEncoding.base64Url().omitPadding().encode(folderId.getBytes(Charsets.US_ASCII));
    }

    public static String decodeFolderId(String encodedFolderId) throws IllegalArgumentException {
        return new String(BaseEncoding.base64Url().omitPadding().decode(encodedFolderId), Charsets.US_ASCII);
    }

    /**
     * Checks a client-chosen name for a newly created CalDAV collection for validity.
     * 
     * @param name The collection resource name as supplied by the client
     * @return The resource name, after it was checked for validity
     * @throws OXException - If the name cannot be used
     */
    public static String checkPlaceholderCollectionName(String name) throws OXException {
        try {
            IDMangling.getRelativeFolderId(decodeFolderId(name));
        } catch (OXException | IllegalArgumentException e) {
            LoggerFactory.getLogger(Tools.class).trace("Ignoring non-decoded name: {}", name, e);
            return name;
        }
        LoggerFactory.getLogger(Tools.class).warn("Detected invalid placeholder collection name: {}", name);
        throw OXException.noPermissionForFolder();
    }

    /**
     * Gets a value indicating whether the supplied event represents a <i>phantom master</i>, i.e. a recurring event master the
     * user has no access for that serves as container for detached occurrences.
     *
     * @param event The event to check
     * @return <code>true</code> if the event is a phantom master, <code>false</code>, otherwise
     */
    public static boolean isPhantomMaster(Event event) {
        return (event instanceof PhantomMaster);
    }

    /**
     * Optionally gets an extended property date value from a previously imported iCal component.
     *
     * @param extendedProperties The extended properties to get the extended date property value from
     * @param propertyName The extended property's name
     * @return The extended property's value parsed as UTC date, or <code>null</code> if not set
     */
    public static Date optExtendedPropertyAsDate(ExtendedProperties extendedProperties, String propertyName) {
        ExtendedProperty extendedProperty = optExtendedProperty(extendedProperties, propertyName);
        if (null != extendedProperty && (extendedProperty.getValue() instanceof String) && Strings.isNotEmpty((String) extendedProperty.getValue())) {
            try {
                return parseUTC((String) extendedProperty.getValue());
            } catch (ParseException e) {
                LoggerFactory.getLogger(Tools.class).warn("Error parsing UTC date from iCal property", e);
            }
        }
        return null;
    }

    /**
     * Optionally gets an extended iCal property from a previously imported event component.
     *
     * @param extendedProperties The extended properties to get the extended property from
     * @param propertyName The extended property's name
     * @return The extended property, or <code>null</code> if not set
     */
    public static ExtendedProperty optExtendedProperty(ExtendedProperties extendedProperties, String propertyName) {
        if (null != extendedProperties && 0 < extendedProperties.size()) {
            if (-1 != propertyName.indexOf('*')) {
                Pattern pattern = Pattern.compile(Strings.wildcardToRegex(propertyName));
                for (ExtendedProperty extraProperty : extendedProperties) {
                    if (pattern.matcher(extraProperty.getName()).matches()) {
                        return extraProperty;
                    }
                }
            } else {
                for (ExtendedProperty extraProperty : extendedProperties) {
                    if (propertyName.equals(extraProperty.getName())) {
                        return extraProperty;
                    }
                }
            }
        }
        return null;
    }

    public static AttachmentMetadata getAttachmentMetadata(Attachment attachment, EventResource eventResource, Event event) throws OXException {
        AttachmentMetadata metadata = AttachmentUtils.newAttachmentMetadata();
        metadata.setId(attachment.getManagedId());
        if (null != eventResource) {
            metadata.setModuleId(AttachmentUtils.getModuleId(eventResource.getParent().getFolder().getContentType()));
            metadata.setFolderId(parse(eventResource.getParent().getFolder().getID()));
        }
        if (null != event) {
            metadata.setAttachedId(parse(event.getId()));
        }
        if (null != attachment.getFormatType()) {
            metadata.setFileMIMEType(attachment.getFormatType());
        } else if (null != attachment.getData()) {
            metadata.setFileMIMEType(attachment.getData().getContentType());
        }
        if (null != attachment.getFilename()) {
            metadata.setFilename(attachment.getFilename());
        } else if (null != attachment.getData()) {
            metadata.setFilename(attachment.getData().getName());
        }
        if (0 < attachment.getSize()) {
            metadata.setFilesize(attachment.getSize());
        } else if (null != attachment.getData()) {
            metadata.setFilesize(attachment.getData().getLength());
        }
        return metadata;
    }

    public static Date getLatestModified(Date lastModified1, Date lastModified2) {
        if (null == lastModified1) {
            return lastModified2;
        }
        if (null == lastModified2) {
            return lastModified1;
        }
        return lastModified1.after(lastModified2) ? lastModified1 : lastModified2;
    }

    public static Date getLatestModified(Date lastModified, CommonObject object) {
        return getLatestModified(lastModified, object.getLastModified());
    }

    public static Date getLatestModified(Date lastModified, Event event) {
        return getLatestModified(lastModified, new Date(event.getTimestamp()));
    }

    /**
     * Parses a numerical identifier from a string, wrapping a possible
     * NumberFormatException into an OXException.
     *
     * @param id the id string
     * @return the parsed identifier
     * @throws OXException
     */
    public static int parse(String id) throws OXException {
        if (null == id) {
            throw new OXException(new IllegalArgumentException("id must not be null"));
        }
        if (id.startsWith(DEFAULT_ACCOUNT_PREFIX)) {
            return parse(id.substring(DEFAULT_ACCOUNT_PREFIX.length()));
        }
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new OXException(e);
        }
    }

    /**
     * Gets the resource name from an url, i.e. the path's name without the
     * filename extension.
     *
     * @param url the webdav path
     * @param fileExtension the extension
     * @return the resource name
     */
    public static String extractResourceName(WebdavPath url, String fileExtension) {
        return null != url ? extractResourceName(url.name(), fileExtension) : null;
    }

    /**
     * Gets the resource name from a filename, i.e. the resource name without the
     * filename extension.
     *
     * @param filename the filename
     * @param fileExtension the extension
     * @return the resource name
     */
    public static String extractResourceName(String filename, String fileExtension) {
        String name = filename;
        if (null != fileExtension) {
            String extension = fileExtension.toLowerCase();
            if (false == extension.startsWith(".")) {
                extension = "." + extension;
            }
            if (null != name && extension.length() < name.length() && name.toLowerCase().endsWith(extension)) {
                return name.substring(0, name.length() - extension.length());
            }
        }
        return name;
    }

    public static boolean isDataTruncation(final OXException e) {
        return TaskExceptionCode.TRUNCATED.equals(e);
    }

    public static boolean isIncorrectString(OXException e) {
        return TaskExceptionCode.INCORRECT_STRING.equals(e);
    }

    public static String formatAsUTC(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static Date parseUTC(String value) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(value);
    }

    /**
     * Prepares attachment metadata using the supplied information.
     * 
     * @param fileHolder The file holder to take over as attachment data
     * @param contentType The content type
     * @param fileName The filename
     * @param size The size
     * @return The attachment metadata
     */
    public static Attachment getAttachment(IFileHolder fileHolder, String contentType, String fileName, long size) {
        Attachment attachment = new Attachment();
        attachment.setFilename(fileName);
        attachment.setFormatType(contentType);
        attachment.setSize(size);
        attachment.setData(fileHolder);
        return attachment;
    }

    /**
     * Builds a new list of attachments out of an optionally existing list and another attachment.
     * 
     * @param existingAttachments The already existing attachments list, or <code>null</code> if there is none
     * @param attachment The attachment to add
     * @return The combined attachment list
     */
    public static List<Attachment> addAttachment(List<Attachment> existingAttachments, Attachment attachment) {
        if (null == existingAttachments || existingAttachments.isEmpty()) {
            return Collections.singletonList(attachment);
        }
        List<Attachment> attachments = new ArrayList<Attachment>(existingAttachments.size() + 1);
        attachments.addAll(existingAttachments);
        attachments.add(attachment);
        return attachments;
    }

    /**
     * Builds a new list of attachments out of an optionally existing list and removes an attachment, based on its managed id.
     * 
     * @param existingAttachments The already existing attachments list
     * @param managedId The identifier of the attachment to remove
     * @return The updated attachment list
     */
    public static List<Attachment> removeAttachment(List<Attachment> existingAttachments, int managedId) {
        if (null == existingAttachments || existingAttachments.isEmpty()) {
            return Collections.emptyList();
        }
        List<Attachment> attachments = new ArrayList<Attachment>(existingAttachments.size() - 1);
        for (Attachment existingAttachment : existingAttachments) {
            if (existingAttachment.getManagedId() != managedId) {
                attachments.add(existingAttachment);
            }
        }
        return attachments;
    }

    /**
     * Gets the managed IDs of all added attachments within the supplied update results.
     * 
     * @param updateResults The update results to extract the added attachment ids from
     * @return The added attachment ids, or an empty list of there are none
     */
    public static List<Integer> getAddedAttachmentIDs(List<UpdateResult> updateResults) {
        List<Integer> attachmentIds = new ArrayList<Integer>();
        if (null != updateResults) {
            for (UpdateResult updateResult : updateResults) {
                for (Attachment addedAttachment : updateResult.getAttachmentUpdates().getAddedItems()) {
                    attachmentIds.add(I(addedAttachment.getManagedId()));
                }
            }
        }
        return attachmentIds;
    }

    /**
     * Generates the schedule-tag for events of a calendar object resource, which is a value indicating whether the scheduling object
     * resource has had a <i>consequential</i> change made to it.
     * <p/>
     * The generation is based on a hash calculation considering the values from each object's {@link #SCHEDULE_TAG_FIELDS}.
     * 
     * @param events The events from the object resource to get the schedule tag for
     * @return The schedule tag
     */
    public static String getScheduleTag(List<Event> events) {
        final int prime = 31;
        int result = 1;
        if (null != events && 0 < events.size()) {
            Object[] values = new Object[SCHEDULE_TAG_FIELDS.length];
            for (Event event : CalendarUtils.sortSeriesMasterFirst(events)) {
                for (int i = 0; i < values.length; i++) {
                    Mapping<? extends Object, Event> mapping = EventMapper.getInstance().opt(SCHEDULE_TAG_FIELDS[i]);
                    values[i] = null == mapping ? null : mapping.get(event);
                }
                result = prime * result + Objects.hash(values);
            }
        }
        return BaseEncoding.base32().omitPadding().encode(ByteBuffer.allocate(4).putInt(result).array());
    }

    private Tools() {
    	// prevent instantiation
    }

}
