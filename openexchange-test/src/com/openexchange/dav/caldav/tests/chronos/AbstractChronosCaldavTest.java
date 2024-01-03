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

package com.openexchange.dav.caldav.tests.chronos;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
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
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.google.common.io.BaseEncoding;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.oauth.provider.AbstractOAuthTest;
import com.openexchange.ajax.oauth.provider.OAuthSession;
import com.openexchange.ajax.oauth.provider.protocol.Grant;
import com.openexchange.ajax.oauth.provider.protocol.OAuthParams;
import com.openexchange.ajax.oauth.provider.protocol.Protocol;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.dav.Config;
import com.openexchange.dav.Headers;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.WebDAVClient;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.dav.caldav.methods.MkCalendarMethod;
import com.openexchange.dav.caldav.reports.CalendarMultiGetReportInfo;
import com.openexchange.dav.reports.SyncCollectionReportInfo;
import com.openexchange.dav.reports.SyncCollectionResponse;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.Client;
import com.openexchange.testing.httpclient.models.EventId;
import net.fortuna.ical4j.model.component.Available;
import net.fortuna.ical4j.model.component.VAvailability;

/**
 * {@link AbstractChronosCaldavTest} - Common base class for CalDAV tests
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public abstract class AbstractChronosCaldavTest extends AbstractChronosTest {

    protected static final int TIMEOUT = 10000;

    @SuppressWarnings("hiding")
    protected String folderId;

    public String authMethod;

    private static final boolean AUTODISCOVER_AUTH = true;

    protected static final String AUTH_METHOD_BASIC = "Basic Auth";

    protected static final String AUTH_METHOD_OAUTH = "OAuth";

    protected Client oAuthClientApp;

    protected WebDAVClient webDAVClient;

    protected static Grant oAuthGrant;

    @SuppressWarnings("unused")
    protected static Iterable<Object[]> availableAuthMethods() {
        if (!AUTODISCOVER_AUTH) {
            List<Object[]> authMethods = new ArrayList<>(2);
            authMethods.add(new Object[] { AUTH_METHOD_OAUTH });
            authMethods.add(new Object[] { AUTH_METHOD_BASIC });

            return authMethods;
        }
        List<Object[]> authMethods = new ArrayList<Object[]>(2);
        PropFindMethod propFind = null;
        try {
            AJAXConfig.init();
            DavPropertyNameSet props = new DavPropertyNameSet();
            props.add(PropertyNames.CURRENT_USER_PRINCIPAL);
            propFind = new PropFindMethod(Config.getBaseUri() + Config.getPathPrefix() + "/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
            propFind.setRequestHeader("User-Agent", UserAgents.IOS_15_0_1);
            if (HttpServletResponse.SC_UNAUTHORIZED == new HttpClient().executeMethod(propFind)) {
                for (Header header : propFind.getResponseHeaders("WWW-Authenticate")) {
                    if (header.getValue().startsWith("Bearer")) {
                        authMethods.add(new Object[] { AUTH_METHOD_OAUTH });
                    } else if (header.getValue().startsWith("Basic")) {
                        authMethods.add(new Object[] { AUTH_METHOD_BASIC });
                    }
                }
            }
        } catch (OXException | IOException e) {
            fail(e.getMessage());
        } finally {
            release(propFind);
        }
        return authMethods;
    }

    protected boolean testOAuth() {
        return AUTH_METHOD_OAUTH.equals(authMethod);
    }

    @AfterEach
    public void unregisterOAuthClient() {
        if (oAuthClientApp != null) {
            try {
                AbstractOAuthTest.unregisterTestClient(oAuthClientApp);
            } catch (Exception e) {
                e.printStackTrace();
            }
            oAuthClientApp = null;
            oAuthGrant = null;
        }
    }

    public void prepareOAuthClient() throws Exception {
        /*
         * Lazy initialization - static (BeforeClass) is not possible because the testOAuth()
         * depends on the configuration of the concrete subclass (via parameterized testing).
         *
         */
        if (testOAuth() && oAuthClientApp == null && oAuthGrant == null) {
            oAuthClientApp = AbstractOAuthTest.registerTestClient(testUser.getCreatedBy());
            try (CloseableHttpClient client = OAuthSession.newOAuthHttpClient(testUser.getCreatedBy())) {
                String state = UUIDs.getUnformattedStringFromRandom();
                OAuthParams params = getOAuthParams(oAuthClientApp, state);
                oAuthGrant = Protocol.obtainAccess(client, params, testUser.getLogin(), testUser.getPassword());
            }
        }
    }

    protected static void release(HttpMethodBase method) {
        if (null != method) {
            method.releaseConnection();
        }
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        webDAVClient = new WebDAVClient(testUser, getDefaultUserAgent(), oAuthGrant);
        prepareOAuthClient();        folderId = getDefaultFolder();
        changeTimezone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Gets the personal calendar folder id
     *
     * @return
     */
    protected String getDefaultFolderID() {
        return folderId;
    }

    protected String encodeFolderID(String folderID) {
        return BaseEncoding.base64Url().omitPadding().encode(CalendarUtils.prependDefaultAccount(folderID).getBytes(Charsets.US_ASCII));
    }

    protected String getDefaultUserAgent() {
        return UserAgents.MACOS_10_7_3;
    }

    private String fetchSyncTokenInternal(String relativeUrl) throws Exception {
        PropFindMethod propFind = null;
        try {
            DavPropertyNameSet props = new DavPropertyNameSet();
            props.add(PropertyNames.SYNC_TOKEN);
            propFind = new PropFindMethod(getBaseUri() + relativeUrl, DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
            MultiStatusResponse response = assertSingleResponse(webDAVClient.doPropFind(propFind, StatusCodes.SC_MULTISTATUS));
            return this.extractTextContent(PropertyNames.SYNC_TOKEN, response);
        } finally {
            release(propFind);
        }
    }

    public static MultiStatusResponse assertSingleResponse(MultiStatusResponse[] responses) {
        assertNotNull(responses, "got no multistatus responses");
        assertTrue(0 < responses.length, "got zero multistatus responses");
        assertTrue(1 == responses.length, "got more than one multistatus responses");
        final MultiStatusResponse response = responses[0];
        assertNotNull(response, "no multistatus response");
        return response;
    }

    protected String extractTextContent(final DavPropertyName propertyName, final MultiStatusResponse response) {
        assertNotEmpty(propertyName, response);
        final Object value = response.getProperties(StatusCodes.SC_OK).get(propertyName).getValue();
        assertTrue(value instanceof String, "value is not a string in " + propertyName);
        return (String) value;
    }

    public static void assertNotEmpty(DavPropertyName propertyName, MultiStatusResponse response) {
        assertIsPresent(propertyName, response);
        final Object value = response.getProperties(StatusCodes.SC_OK).get(propertyName).getValue();
        assertNotNull(value, "no value for " + propertyName);
    }

    public static void assertIsPresent(DavPropertyName propertyName, MultiStatusResponse response) {
        final DavProperty<?> property = response.getProperties(StatusCodes.SC_OK).get(propertyName);
        assertNotNull(property, "property " + propertyName + " not found");
    }

    protected static String getBaseUri() throws OXException {
        return CalDAVTest.getBaseUri();
    }

    protected static String getDavHostname() throws OXException {
        return CalDAVTest.getDavHostname();
    }

    protected static String getHostname() throws OXException {
        return CalDAVTest.getHostname();
    }

    protected static String getProtocol() throws OXException {
        return CalDAVTest.getProtocol();
    }

    protected String fetchSyncToken(String folderID) throws Exception {
        return fetchSyncTokenInternal("/caldav/" + folderID);
    }

    protected String fetchSyncToken() throws Exception {
        return fetchSyncTokenInternal(getCaldavFolder());
    }

    protected SyncCollectionResponse syncCollection(SyncToken syncToken, String folderID) throws Exception {
        return syncCollectionInternal(syncToken, "/caldav/" + folderID);
    }

    protected SyncCollectionResponse syncCollectionInternal(SyncToken syncToken, String relativeUrl) throws Exception {
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        SyncCollectionReportInfo reportInfo = new SyncCollectionReportInfo(syncToken.getToken(), props);
        SyncCollectionResponse syncCollectionResponse = webDAVClient.doReport(reportInfo, getBaseUri() + relativeUrl);
        syncToken.setToken(syncCollectionResponse.getSyncToken());
        return syncCollectionResponse;
    }

    protected SyncCollectionResponse syncCollection(SyncToken syncToken) throws Exception {
        return this.syncCollection(syncToken, getCaldavFolder());
    }

    protected List<ICalResource> calendarMultiget(Collection<String> hrefs) throws Exception {
        return calendarMultiget(getCaldavFolder(), hrefs);
    }

    protected List<ICalResource> calendarMultiget(String folderID, Collection<String> hrefs) throws Exception {
        List<ICalResource> calendarData = new ArrayList<ICalResource>();
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        props.add(PropertyNames.CALENDAR_DATA);
        ReportInfo reportInfo = new CalendarMultiGetReportInfo(hrefs.toArray(new String[hrefs.size()]), props);
        MultiStatusResponse[] responses = webDAVClient.doReport(reportInfo, getBaseUri() + "/caldav/" + folderID + "/");
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

        return putICal(getCaldavFolder(), resourceName, iCal);
    }
    
    public void rememberEvent(String id) {
        EventId eventId = new EventId();
        eventId.setFolder(getDefaultFolderID());
        eventId.setId(id);
        rememberEventId(eventId);
    }
    
    private String getCaldavFolder(){
        String defaultFolderID = getDefaultFolderID();
        if (defaultFolderID.indexOf('/')!=-1){
            defaultFolderID = defaultFolderID.substring(defaultFolderID.lastIndexOf("/")+1, defaultFolderID.length());
        }
        return defaultFolderID;
    }
    
    protected String getCaldavFolder(String folderId) {
        String result = folderId;
        if (result.indexOf('/') != -1) {
            result = result.substring(result.lastIndexOf("/") + 1, result.length());
        }
        return result;
    }

    protected int putICal(String folderID, String resourceName, String iCal) throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Headers.IF_NONE_MATCH, "*");

        return putICal(folderID, resourceName, iCal, headers);
    }

    protected int putICal(String folderID, String resourceName, String iCal, Map<String, String> headers) throws Exception {
        PutMethod put = null;
        try {
            String href = "/caldav/" + folderID + "/" + urlEncode(resourceName) + ".ics";
            put = new PutMethod(getBaseUri() + href);
            for (String key : headers.keySet()) {
                put.addRequestHeader(key, headers.get(key));
            }
            put.setRequestEntity(new StringRequestEntity(iCal, "text/calendar", null));
            return webDAVClient.executeMethod(put);
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
            String href = "/caldav/" + resource + "/";
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
            String href = "/caldav/" + resource + "/";
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
        MoveMethod move = null;
        try {
            String targetHref = "/caldav/" + targetFolderID + "/" + iCalResource.getHref().substring(1 + iCalResource.getHref().lastIndexOf('/'));
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
            String targetHref = "/caldav/" + targetResourceName + '/';
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

    protected static ICalResource get(WebDAVClient client, String folderID, String resourceName, String ifNoneMatchEtag, String ifMatchEtag) throws Exception {
        return CalDAVTest.get(client, folderID, resourceName, ifNoneMatchEtag, ifMatchEtag);
    }

    private static String urlEncode(String name) throws URISyntaxException {
        return CalDAVTest.urlEncode(name);
    }

    protected int putICalUpdate(String resourceName, String iCal, String ifMatchEtag) throws Exception {
        return this.putICalUpdate(getCaldavFolder(), resourceName, iCal, ifMatchEtag);
    }

    protected int putICalUpdate(String folderID, String resourceName, String iCal, String ifMatchEtag) throws Exception {
        PutMethod put = null;
        try {
            String href = "/caldav/" + folderID + "/" + urlEncode(resourceName) + ".ics";
            put = new PutMethod(getBaseUri() + href);
            if (null != ifMatchEtag) {
                put.addRequestHeader(Headers.IF_MATCH, ifMatchEtag);
            }
            put.setRequestEntity(new StringRequestEntity(iCal, "text/calendar", null));
            return webDAVClient.executeMethod(put);
        } finally {
            release(put);
        }
    }
    
    protected int putICalUpdate(WebDAVClient client, String folderID, String resourceName, String iCal, String ifMatchEtag) throws Exception {
        PutMethod put = null;
        try {
            String href = "/caldav/" + folderID + "/" + urlEncode(resourceName) + ".ics";
            put = new PutMethod(getBaseUri() + href);
            if (null != ifMatchEtag) {
                put.addRequestHeader(Headers.IF_MATCH, ifMatchEtag);
            }
            put.setRequestEntity(new StringRequestEntity(iCal, "text/calendar", null));
            return client.executeMethod(put);
        } finally {
            release(put);
        }
    }
    

    protected int putICalUpdate(ICalResource iCalResource) throws Exception {
        PutMethod put = null;
        try {
            put = new PutMethod(getBaseUri() + iCalResource.getHref());
            if (null != iCalResource.getETag()) {
                put.addRequestHeader(Headers.IF_MATCH, iCalResource.getETag());
            }
            put.setRequestEntity(new StringRequestEntity(iCalResource.toString(), "text/calendar", null));
            return webDAVClient.executeMethod(put);
        } finally {
            release(put);
        }
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

    protected static String formatAsUTC(final Date date) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    protected static String formatAsDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
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

    public static ICalResource assertContains(String uid, Collection<ICalResource> iCalResources) {
        ICalResource match = null;
        for (ICalResource iCalResource : iCalResources) {
            if (uid.equals(iCalResource.getVEvent().getUID())) {
                assertNull(match, "duplicate match for UID '" + uid + "'");
                match = iCalResource;
            }
        }
        assertNotNull(match, "no iCal resource with UID '" + uid + "' found");
        return match;
    }

    protected static String randomUID() {
        return UUID.randomUUID().toString();
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
