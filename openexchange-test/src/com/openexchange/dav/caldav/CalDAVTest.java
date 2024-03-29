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

package com.openexchange.dav.caldav;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.google.common.io.BaseEncoding;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.dav.Config;
import com.openexchange.dav.Headers;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.WebDAVClient;
import com.openexchange.dav.WebDAVTest;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.dav.caldav.methods.MkCalendarMethod;
import com.openexchange.dav.caldav.reports.CalendarMultiGetReportInfo;
import com.openexchange.dav.reports.SyncCollectionResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.java.Charsets;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.test.PermissionTools;
import net.fortuna.ical4j.model.component.Available;
import net.fortuna.ical4j.model.component.VAvailability;

/**
 * {@link CalDAVTest} - Common base class for CalDAV tests
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class CalDAVTest extends WebDAVTest {

    @SuppressWarnings("hiding")
    protected static final int TIMEOUT = 10000;

    private int folderId;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderId = this.getAJAXClient().getValues().getPrivateAppointmentFolder();
        catm.setFailOnError(true);
    }

    /**
     * Gets the personal calendar folder id
     *
     * @return
     */
    protected String getDefaultFolderID() {
        return Integer.toString(folderId);
    }

    protected String encodeFolderID(String folderID) {
        return BaseEncoding.base64Url().omitPadding().encode(CalendarUtils.prependDefaultAccount(folderID).getBytes(Charsets.US_ASCII));
    }

    protected FolderObject createFolder(String folderName) {
        return createFolder(ftm.getFolderFromServer(folderId), folderName);
    }

    /**
     * Gets the underlying {@link CalendarTestManager} instance.
     *
     * @return
     */
    protected CalendarTestManager getManager() {
        return catm;
    }

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.MACOS_10_7_3;
    }

    protected void delete(Appointment appointment) {
        getManager().delete(appointment);
    }

    protected void delete(Task task) {
        ttm.deleteTaskOnServer(task);
    }

    @Override
    protected String fetchSyncToken(String folderID) throws Exception {
        return super.fetchSyncToken(Config.getPathPrefix() + "/caldav/" + encodeFolderID(folderID));
    }

    protected String fetchSyncToken() throws Exception {
        return fetchSyncToken(getDefaultFolderID());
    }

    @Override
    protected SyncCollectionResponse syncCollection(SyncToken syncToken, String folderID) throws Exception {
        return super.syncCollection(syncToken, "/caldav/" + encodeFolderID(folderID));
    }

    protected SyncCollectionResponse syncCollection(SyncToken syncToken) throws Exception {
        return this.syncCollection(syncToken, getDefaultFolderID());
    }

    protected List<ICalResource> calendarMultiget(Collection<String> hrefs) throws Exception {
        return calendarMultiget(getDefaultFolderID(), hrefs);
    }

    protected List<ICalResource> calendarMultiget(String folderID, Collection<String> hrefs) throws Exception {
        List<ICalResource> calendarData = new ArrayList<ICalResource>();
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        props.add(PropertyNames.CALENDAR_DATA);
        ReportInfo reportInfo = new CalendarMultiGetReportInfo(hrefs.toArray(new String[hrefs.size()]), props);
        MultiStatusResponse[] responses = webDAVClient.doReport(reportInfo, getBaseUri() + Config.getPathPrefix() + "/caldav/" + encodeFolderID(folderID) + "/");
        for (MultiStatusResponse response : responses) {
            if (response.getProperties(StatusCodes.SC_OK).contains(PropertyNames.GETETAG)) {
                String href = response.getHref();
                assertNotNull(href, "got no href from response");
                String data = this.extractTextContent(PropertyNames.CALENDAR_DATA, response);
                assertNotNull(data, "got no address data from response");
                String eTag = this.extractTextContent(PropertyNames.GETETAG, response);
                assertNotNull(eTag, "got no etag data from response");
                calendarData.add(new ICalResource(data, href, eTag));
            }
        }
        return calendarData;
    }

    protected int putICal(String resourceName, String iCal) throws Exception {
        return putICal(getDefaultFolderID(), resourceName, iCal);
    }

    protected int putICal(String folderID, String resourceName, String iCal) throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Headers.IF_NONE_MATCH, "*");

        return putICal(folderID, resourceName, iCal, headers);
    }

    protected int putICal(String folderID, String resourceName, String iCal, Map<String, String> headers) throws Exception {
        return putICal(webDAVClient, encodeFolderID(folderID), resourceName, iCal, headers);
    }

    protected static int putICal(WebDAVClient client, String folderID, String resourceName, String iCal, Map<String, String> headers) throws Exception {
        PutMethod put = null;
        try {
            String href = Config.getPathPrefix() + "/caldav/" + folderID + "/" + urlEncode(resourceName) + ".ics";
            put = new PutMethod(getBaseUri() + href);
            for (String key : headers.keySet()) {
                put.addRequestHeader(key, headers.get(key));
            }
            put.setRequestEntity(new StringRequestEntity(iCal, "text/calendar", null));
            return client.executeMethod(put);
        } finally {
            release(put);
        }
    }

    /**
     * Puts the specified iCal (containing one or multiple VAvailability components) via the
     * PROPATCH method to the server
     *
     * @param iCal The ical as string
     * @return The response code of the operation
     */
    protected int propPatchICal(String iCal) throws Exception {
        String resource = "schedule-inbox";
        // Set props
        DavProperty<String> set = new DefaultDavProperty<>(DavPropertyName.create("calendar-availability", PropertyNames.NS_CALENDARSERVER), iCal);
        DavPropertySet setProps = new DavPropertySet();
        setProps.add(set);

        // Unset props
        DavPropertyNameSet unsetProps = new DavPropertyNameSet();

        // Execute
        return propPatchICal(resource, setProps, unsetProps, Collections.<String, String> emptyMap());
    }

    /**
     * Sets and unsets the specified DAV properties from the specified resource using the PROPATCH method
     *
     * @param resource The resource name
     * @param setProps The properties to set
     * @param unsetProps The properties to unset
     * @param headers The headers
     * @return The response code of the operation
     */
    protected int propPatchICal(String resource, DavPropertySet setProps, DavPropertyNameSet unsetProps, Map<String, String> headers) throws Exception {
        PropPatchMethod propPatch = null;
        try {
            String href = Config.getPathPrefix() + "/caldav/" + resource + "/";
            propPatch = new PropPatchMethod(getBaseUri() + href, setProps, unsetProps);
            for (String key : headers.keySet()) {
                propPatch.addRequestHeader(key, headers.get(key));
            }
            return webDAVClient.executeMethod(propPatch);
        } finally {
            release(propPatch);
        }
    }

    /**
     *
     * @param property
     * @return
     * @throws Exception
     */
    protected List<ICalResource> propFind(String property) throws Exception {
        String resource = "schedule-inbox";

        DavPropertyNameSet queryProps = new DavPropertyNameSet();
        queryProps.add(DavPropertyName.create(property, PropertyNames.NS_CALENDARSERVER));

        return propFind(resource, queryProps);
    }

    /**
     *
     * @return
     * @throws Exception
     */
    protected List<ICalResource> propFind(String resource, DavPropertyNameSet queryProps) throws Exception {
        PropFindMethod propFind = null;
        try {
            String href = Config.getPathPrefix() + "/caldav/" + resource + "/";
            propFind = new PropFindMethod(getBaseUri() + href, queryProps, DavConstants.DEPTH_0);

            Assertions.assertEquals(207, webDAVClient.executeMethod(propFind), "response code wrong");
            MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
            assertNotNull(multiStatus, "got no response body");

            List<ICalResource> resources = new ArrayList<ICalResource>();
            MultiStatusResponse[] responses = multiStatus.getResponses();
            for (MultiStatusResponse response : responses) {
                String data = extractTextContent(PropertyNames.CALENDAR_AVAILABILITY, response);
                resources.add(new ICalResource(data));
            }
            return resources;
        } finally {
            release(propFind);
        }
    }

    protected int move(ICalResource iCalResource, String targetFolderID) throws Exception {
        DeleteMethod ds;
        MoveMethod move = null;
        try {
            String targetHref = Config.getPathPrefix() + "/caldav/" + encodeFolderID(targetFolderID) + "/" + iCalResource.getHref().substring(1 + iCalResource.getHref().lastIndexOf('/'));
            move = new MoveMethod(getBaseUri() + iCalResource.getHref(), getBaseUri() + targetHref, false);
            if (null != iCalResource.getETag()) {
                move.addRequestHeader(Headers.IF_MATCH, iCalResource.getETag());
            }
            int status = webDAVClient.executeMethod(move);
            if (StatusCodes.SC_CREATED == status) {
                iCalResource.setHref(targetHref);
            }
            return status;
        } finally {
            release(move);
        }
    }

    protected void mkCalendar(String targetResourceName, DavPropertySet setProperties) throws Exception {
        MkCalendarMethod mkCalendar = null;
        try {
            String targetHref = Config.getPathPrefix() + "/caldav/" + targetResourceName + '/';
            mkCalendar = new MkCalendarMethod(getBaseUri() + targetHref, setProperties);
            Assertions.assertEquals(StatusCodes.SC_CREATED, webDAVClient.executeMethod(mkCalendar), "response code wrong");
        } finally {
            release(mkCalendar);
        }
    }

    protected ICalResource get(String resourceName) throws Exception {
        return get(getDefaultFolderID(), resourceName, null, null);
    }

    protected ICalResource get(String folderID, String resourceName) throws Exception {
        return get(folderID, resourceName, null, null);
    }

    protected ICalResource get(String folderID, String resourceName, String ifMatchEtag) throws Exception {
        return get(folderID, resourceName, null, ifMatchEtag);
    }

    protected ICalResource get(String folderID, String resourceName, String ifNoneMatchEtag, String ifMatchEtag) throws Exception {
        return get(webDAVClient, encodeFolderID(folderID), resourceName, ifNoneMatchEtag, ifMatchEtag);
    }

    public static ICalResource get(WebDAVClient client, String folderID, String resourceName, String ifNoneMatchEtag, String ifMatchEtag) throws Exception {
        GetMethod get = null;
        try {
            String href = Config.getPathPrefix() + "/caldav/" + folderID + "/" + urlEncode(resourceName) + ".ics";
            get = new GetMethod(getBaseUri() + href);
            if (null != ifNoneMatchEtag) {
                get.addRequestHeader(Headers.IF_NONE_MATCH, ifNoneMatchEtag);
            }
            if (null != ifMatchEtag) {
                get.addRequestHeader(Headers.IF_MATCH, ifMatchEtag);
            }
            Assertions.assertEquals(StatusCodes.SC_OK, client.executeMethod(get), "response code wrong");
            byte[] responseBody = get.getResponseBody();
            assertNotNull(responseBody, "got no response body");
            ICalResource iCalResource = new ICalResource(new String(responseBody, Charsets.UTF_8), href, get.getResponseHeader("ETag").getValue());
            Header scheduleTag = get.getResponseHeader("Schedule-Tag");
            if (null != scheduleTag) {
                iCalResource.setScheduleTag(scheduleTag.getValue());
            }
            return iCalResource;
        } finally {
            release(get);
        }
    }

    public static String urlEncode(String name) throws URISyntaxException {
        return new URI(null, name, null).toString();
    }

    protected int putICalUpdate(String resourceName, String iCal, String ifMatchEtag) throws Exception {
        return this.putICalUpdate(getDefaultFolderID(), resourceName, iCal, ifMatchEtag);
    }

    protected int putICalUpdate(String folderID, String resourceName, String iCal, String ifMatchEtag) throws Exception {
    	return putICalUpdate(folderID, resourceName, iCal, ifMatchEtag, null);
    }

    protected int putICalUpdate(String folderID, String resourceName, String iCal, String ifMatchEtag, String ifMatchScheduleTag) throws Exception {
        PutMethod put = null;
        try {
            String href = Config.getPathPrefix() + "/caldav/" + encodeFolderID(folderID) + "/" + urlEncode(resourceName) + ".ics";
            put = new PutMethod(getBaseUri() + href);
            if (null != ifMatchEtag) {
                put.addRequestHeader(Headers.IF_MATCH, ifMatchEtag);
            }
            if (null != ifMatchScheduleTag) {
                put.addRequestHeader("If-Schedule-Tag-Match", ifMatchScheduleTag);
            }
            put.setRequestEntity(new StringRequestEntity(iCal, "text/calendar", null));
            return webDAVClient.executeMethod(put);
        } finally {
            release(put);
        }
    }

    protected int delete(String folderID, String resourceName, String ifMatchEtag, String ifMatchScheduleTag) throws Exception {
        return delete(webDAVClient, encodeFolderID(folderID), resourceName, ifMatchEtag, ifMatchScheduleTag);
    }

    protected static int delete(WebDAVClient client, String collectioName, String resourceName, String ifMatchEtag, String ifMatchScheduleTag) throws Exception {
        DeleteMethod delete = null;
        try {
            String href = Config.getPathPrefix() + "/caldav/" + collectioName + "/" + urlEncode(resourceName) + ".ics";
            delete = new DeleteMethod(getBaseUri() + href);
            if (null != ifMatchEtag) {
                delete.addRequestHeader(Headers.IF_MATCH, ifMatchEtag);
            }
            if (null != ifMatchScheduleTag) {
                delete.addRequestHeader("If-Schedule-Tag-Match", ifMatchScheduleTag);
            }
            return client.executeMethod(delete);
        } finally {
            release(delete);
        }
    }

    public int putICalUpdate(ICalResource iCalResource) throws Exception {
        return putICalUpdate(webDAVClient, iCalResource);
    }
    public int putICalUpdate(WebDAVClient webDAVClient, ICalResource iCalResource) throws Exception {
        PutMethod put = null;
        try {
            put = new PutMethod(getBaseUri() + iCalResource.getHref());
            if (null != iCalResource.getETag()) {
                put.addRequestHeader(Headers.IF_MATCH, iCalResource.getETag());
            }
            put.setRequestEntity(new StringRequestEntity(iCalResource.toString(), "text/calendar", null));
            return  webDAVClient.executeMethod(put);
        } finally {
            release(put);
        }
    }

    protected Appointment getAppointment(String folderID, String uid) throws OXException {
        Appointment[] appointments = catm.all(parse(folderID), new Date(0), new Date(100000000000000L), new int[] { Appointment.OBJECT_ID, Appointment.RECURRENCE_ID, Appointment.FOLDER_ID, Appointment.UID });
        for (Appointment appointment : appointments) {
            if (uid.equals(appointment.getUid())) {
                if (0 >= appointment.getRecurrenceID() || appointment.getRecurrenceID() == appointment.getObjectID()) {
                    return catm.get(appointment);
                }
            }
        }
        return null;
    }

    protected List<Appointment> getAppointments(String folderID, String uid) {
        List<Appointment> matchingAppointments = new ArrayList<Appointment>();
        Appointment[] appointments = catm.all(parse(folderID), new Date(0), new Date(100000000000000L), Appointment.ALL_COLUMNS);
        for (Appointment appointment : appointments) {
            if (uid.equals(appointment.getUid())) {
                matchingAppointments.add(appointment);
            }
        }
        return matchingAppointments;
    }

    protected List<Appointment> getChangeExceptions(Appointment appointment) throws OXException {
        List<Appointment> exceptions = catm.getChangeExceptions(appointment.getParentFolderID(), appointment.getObjectID(), new int[] { Task.OBJECT_ID, Task.FOLDER_ID, Task.UID });
        if (null != exceptions && 0 < exceptions.size()) {
            for (int i = 0; i < exceptions.size(); i++) {
                exceptions.set(i, catm.get(exceptions.get(i)));
            }
        }
        return exceptions;
    }

    protected Task getTask(String folderID, String uid) {
        Task[] tasks = ttm.getAllTasksOnServer(parse(folderID), new int[] { Task.OBJECT_ID, Task.FOLDER_ID, Task.UID });
        for (Task task : tasks) {
            if (uid.equals(task.getUid())) {
                return ttm.getTaskFromServer(parse(folderID), task.getObjectID());
            }
        }
        return null;
    }

    /**
     * Remembers the supplied appointment for deletion after the test is
     * finished in the <code>tearDown()</code> method.
     *
     * @param appointment
     */
    protected void rememberForCleanUp(Appointment appointment) {
        if (null != appointment) {
            this.getManager().getCreatedEntities().add(appointment);
        }
    }

    protected Appointment getAppointment(String uid) throws OXException {
        return getAppointment(getDefaultFolderID(), uid);
    }

    protected static int parse(String id) {
        return Integer.parseInt(id);
    }

    protected static String format(Date date, TimeZone timeZone) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmm'00'");
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(date);
    }

    protected static String format(Date date, String timeZoneID) {
        return format(date, TimeZone.getTimeZone(timeZoneID));
    }

    protected static Appointment generateAppointment(Date start, Date end, String uid, String summary, String location) {
        Appointment appointment = new Appointment();
        appointment.setTitle(summary);
        appointment.setLocation(location);
        appointment.setStartDate(start);
        appointment.setEndDate(end);
        appointment.setUid(uid);
        return appointment;
    }

    protected static Task generateTask(Date start, Date end, String uid, String summary) {
        Task task = new Task();
        task.setStartDate(start);
        task.setEndDate(end);
        task.setUid(uid);
        task.setTitle(summary);
        return task;
    }

    protected static String generateICal(Date start, Date end, String uid, String summary, String location) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BEGIN:VCALENDAR").append("\r\n").append("VERSION:2.0").append("\r\n").append("PRODID:-//Apple Inc.//iCal 5.0.2//EN").append("\r\n").append("CALSCALE:GREGORIAN").append("\r\n").append("BEGIN:VTIMEZONE").append("\r\n").append("TZID:Europe/Amsterdam").append("\r\n").append("BEGIN:DAYLIGHT").append("\r\n").append("TZOFFSETFROM:+0100").append("\r\n").append("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU").append("\r\n").append("DTSTART:19810329T020000").append("\r\n").append("TZNAME:CEST").append("\r\n").append("TZOFFSETTO:+0200").append("\r\n").append("END:DAYLIGHT").append("\r\n").append("BEGIN:STANDARD").append("\r\n").append("TZOFFSETFROM:+0200").append("\r\n").append("RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU").append("\r\n").append("DTSTART:19961027T030000").append("\r\n").append("TZNAME:CET").append("\r\n").append("TZOFFSETTO:+0100").append("\r\n").append("END:STANDARD").append("\r\n").append("END:VTIMEZONE").append("\r\n").append("BEGIN:VEVENT").append("\r\n").append("CREATED:").append(formatAsUTC(new Date())).append("\r\n");
        if (null != uid) {
            stringBuilder.append("UID:").append(uid).append("\r\n");
        }
        if (null != end) {
            stringBuilder.append("DTEND;TZID=Europe/Amsterdam:").append(format(end, "Europe/Amsterdam")).append("\r\n");
        }
        stringBuilder.append("TRANSP:OPAQUE").append("\r\n");
        if (null != summary) {
            stringBuilder.append("SUMMARY:").append(summary).append("\r\n");
        }
        if (null != location) {
            stringBuilder.append("LOCATION:").append(location).append("\r\n");
        }
        if (null != start) {
            stringBuilder.append("DTSTART;TZID=Europe/Amsterdam:").append(format(start, "Europe/Amsterdam")).append("\r\n");
        }
        stringBuilder.append("DTSTAMP:").append(formatAsUTC(new Date())).append("\r\n").append("SEQUENCE:0").append("\r\n").append("END:VEVENT").append("\r\n").append("END:VCALENDAR").append("\r\n");

        return stringBuilder.toString();
    }

    protected static String generateVTodo(Date start, Date due, String uid, String summary, String location) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BEGIN:VCALENDAR").append("\r\n").append("VERSION:2.0").append("\r\n").append("PRODID:-//Apple Inc.//iCal 5.0.2//EN").append("\r\n").append("CALSCALE:GREGORIAN").append("\r\n").append("BEGIN:VTIMEZONE").append("\r\n").append("TZID:Europe/Amsterdam").append("\r\n").append("BEGIN:DAYLIGHT").append("\r\n").append("TZOFFSETFROM:+0100").append("\r\n").append("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU").append("\r\n").append("DTSTART:19810329T020000").append("\r\n").append("TZNAME:CEST").append("\r\n").append("TZOFFSETTO:+0200").append("\r\n").append("END:DAYLIGHT").append("\r\n").append("BEGIN:STANDARD").append("\r\n").append("TZOFFSETFROM:+0200").append("\r\n").append("RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU").append("\r\n").append("DTSTART:19961027T030000").append("\r\n").append("TZNAME:CET").append("\r\n").append("TZOFFSETTO:+0100").append("\r\n").append("END:STANDARD").append("\r\n").append("END:VTIMEZONE").append("\r\n").append("BEGIN:VTODO").append("\r\n").append("CREATED:").append(formatAsUTC(new Date())).append("\r\n");
        if (null != uid) {
            stringBuilder.append("UID:").append(uid).append("\r\n");
        }
        if (null != due) {
            stringBuilder.append("DUE;TZID=Europe/Amsterdam:").append(format(due, "Europe/Amsterdam")).append("\r\n");
        }
        stringBuilder.append("TRANSP:OPAQUE").append("\r\n");
        if (null != summary) {
            stringBuilder.append("SUMMARY:").append(summary).append("\r\n");
        }
        if (null != location) {
            stringBuilder.append("LOCATION:").append(location).append("\r\n");
        }
        if (null != start) {
            stringBuilder.append("DTSTART;TZID=Europe/Amsterdam:").append(format(start, "Europe/Amsterdam")).append("\r\n");
        }
        stringBuilder.append("DTSTAMP:").append(formatAsUTC(new Date())).append("\r\n").append("SEQUENCE:0").append("\r\n").append("END:VTODO").append("\r\n").append("END:VCALENDAR").append("\r\n");

        return stringBuilder.toString();
    }

    /**
     * Generates a {@link VAvailability} with one {@link Available} block
     *
     * @param start The start of the available block
     * @param end The end of the available block
     * @param uid The uid
     * @param summary The summary
     * @param location The location
     * @return The {@link VAvailability} as {@link String} iCal
     */
    protected static String generateVAvailability(Date start, Date end, String uid, String summary, String location) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR").append("\r\n").append("VERSION:2.0").append("\r\n");
        sb.append("PRODID:-//Apple Inc.//Mac OS X 10.12.6//EN").append("\r\n").append("CALSCALE:GREGORIAN").append("\r\n");
        sb.append("BEGIN:VTIMEZONE").append("\r\n").append("TZID:Europe/Berlin").append("\r\n");
        sb.append("BEGIN:DAYLIGHT").append("\r\n").append("TZOFFSETFROM:+0100").append("\r\n");
        sb.append("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU").append("\r\n");
        sb.append("DTSTART:19810329T020000").append("\r\n").append("TZNAME:GMT+2").append("\r\n").append("TZOFFSETTO:+0200").append("\r\n");
        sb.append("END:DAYLIGHT").append("\r\n").append("BEGIN:STANDARD").append("\r\n").append("TZOFFSETFROM:+0200").append("\r\n");
        sb.append("RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU").append("\r\n").append("DTSTART:19961027T030000").append("\r\n");
        sb.append("TZNAME:CET").append("\r\n").append("TZOFFSETTO:+0100").append("\r\n").append("END:STANDARD").append("\r\n");
        sb.append("END:VTIMEZONE").append("\r\n");
        sb.append("BEGIN:VAVAILABILITY").append("\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("DTSTAMP:").append(formatAsUTC(new Date())).append("\r\n").append("\r\n");
        sb.append("SUMMARY:").append(summary).append("\r\n");
        sb.append("LOCATION:").append(location).append("\r\n");
        sb.append("BEGIN:AVAILABLE").append("\r\n");
        sb.append("DTSTART;TZID=Europe/Berlin:").append(format(start, "Europe/Berlin")).append("\r\n");
        sb.append("RRULE:FREQ=WEEKLY;BYDAY=WE").append("\r\n");
        sb.append("DTEND;TZID=Europe/Berlin:").append(format(end, "Europe/Berlin")).append("\r\n");
        sb.append("END:AVAILABLE").append("\r\n");
        sb.append("END:VAVAILABILITY").append("\r\n");
        sb.append("END:VCALENDAR").append("\r\n");

        return sb.toString();
    }

    public static void assertAppointmentEquals(Appointment appointment, Date expectedStart, Date expectedEnd, String expectedUid, String expectedTitle, String expectedLocation) {
        assertNotNull(appointment, "appointment is null");
        Assertions.assertEquals(expectedStart, appointment.getStartDate(), "start date wrong");
        Assertions.assertEquals(expectedEnd, appointment.getEndDate(), "end date wrong");
        Assertions.assertEquals(expectedUid, appointment.getUid(), "uid wrong");
        Assertions.assertEquals(expectedTitle, appointment.getTitle(), "title wrong");
        Assertions.assertEquals(expectedLocation, appointment.getLocation(), "location wrong");
    }

    public static ICalResource assertContains(String uid, Collection<ICalResource> iCalResources) {
        ICalResource match = null;
        for (ICalResource iCalResource : iCalResources) {
            if (null != iCalResource.getVEvent()) {
                if (uid.equals(iCalResource.getVEvent().getUID())) {
                    assertNull(match, "duplicate match for UID '" + uid + "'");
                    match = iCalResource;
                }
            } else if (null != iCalResource.getVTodo()) {
                if (uid.equals(iCalResource.getVTodo().getUID())) {
                    assertNull(match, "duplicate match for UID '" + uid + "'");
                    match = iCalResource;
                }
            }
        }
        assertNotNull(match, "no iCal resource with UID '" + uid + "' found");
        return match;
    }

    protected Appointment create(Appointment appointment) {
        return create(getDefaultFolderID(), appointment);
    }

    protected Appointment create(String folderID, Appointment appointment) {
        appointment.setParentFolderID(parse(folderID));
        appointment.setIgnoreConflicts(true);
        return getManager().insert(appointment);
    }

    protected Task create(String folderID, Task task) {
        task.setParentFolderID(parse(folderID));
        return ttm.insertTaskOnServer(task);
    }

    protected Task update(Task task) {
        return ttm.updateTaskOnServer(task);
    }

    protected Appointment update(Appointment appointment) {
        appointment.setIgnoreConflicts(true);
        getManager().update(appointment);
        return appointment;
    }

    protected FolderObject createPublicFolder() throws OXException, IOException, JSONException {
        return createPublicFolder(randomUID());
    }

    protected FolderObject createPublicFolder(String name) throws OXException, IOException, JSONException {
        FolderObject folder = new FolderObject();
        folder.setModule(FolderObject.CALENDAR);
        folder.setParentFolderID(FolderObject.SYSTEM_PUBLIC_FOLDER_ID);
        folder.setPermissions(PermissionTools.P(Integer.valueOf(getClient().getValues().getUserId()), PermissionTools.ADMIN));
        folder.setFolderName(name);
        folder = ftm.insertFolderOnServer(folder);
        folder.setLastModified(new Date());
        return folder;
    }

    protected static void assertDummyAlarm(Component component) {
        List<Component> vAlarms = component.getVAlarms();
        Assertions.assertEquals(1, vAlarms.size(), "Expected exactly one VAlarm.");
        Component vAlarm = vAlarms.get(0);
        Assertions.assertEquals("19760401T005545Z", vAlarm.getProperty("TRIGGER").getValue(), "Expected dummy trigger.");
        Assertions.assertEquals("TRUE", vAlarm.getProperty("X-APPLE-LOCAL-DEFAULT-ALARM").getValue(), "Expected dummy property.");
        Assertions.assertEquals("TRUE", vAlarm.getProperty("X-APPLE-DEFAULT-ALARM").getValue(), "Expected dummy property.");
    }

    protected static void assertAcknowledgedOrDummyAlarm(Component component, String expectedAcknowledged) {
        List<Component> vAlarms = component.getVAlarms();
        Assertions.assertEquals(1, vAlarms.size(), "Expected exactly one VAlarm.");
        Component vAlarm = vAlarms.get(0);
        if ("19760401T005545Z".equals(vAlarm.getPropertyValue("TRIGGER"))) {
            Assertions.assertEquals("19760401T005545Z", vAlarm.getProperty("TRIGGER").getValue(), "Expected dummy trigger");
            Assertions.assertEquals("TRUE", vAlarm.getProperty("X-APPLE-LOCAL-DEFAULT-ALARM").getValue(), "Expected dummy property");
            Assertions.assertEquals("TRUE", vAlarm.getProperty("X-APPLE-DEFAULT-ALARM").getValue(), "Expected dummy property");
        } else {
            Assertions.assertEquals(expectedAcknowledged, vAlarm.getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
        }
    }

}
