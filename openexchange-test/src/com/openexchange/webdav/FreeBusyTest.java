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

package com.openexchange.webdav;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.scheduling.RESTUtilities;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.java.Charsets;
import com.openexchange.java.util.TimeZones;
import com.openexchange.test.Host;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventData.TranspEnum;
import com.openexchange.testing.httpclient.models.ResourceResponse;
import com.openexchange.testing.httpclient.modules.ResourcesApi;
import com.openexchange.testing.restclient.invoker.ApiClient;
import com.openexchange.testing.restclient.modules.InternetFreeBusyApi;

/**
 * {@link FreeBusyTest}
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Ottersbach</a>
 * @since v7.10.4
 */

@Execution(ExecutionMode.SAME_THREAD)
public class FreeBusyTest extends AbstractChronosTest {

    private final Map<String, String> CONFIG = new HashMap<String, String>();

    private static final int DEFAULT_WEEKS_FUTURE = 4;

    private static final int DEFAULT_WEEKS_PAST = 1;

    private static final int MAX_RETRY = 10;

    private Date testDate;

    private EventData busyDate;

    private EventData freeDate;

    private String userName;

    private String server;

    private int contextid;

    private InternetFreeBusyApi api;

    private String testResourceName;

    private TestUser restUser;

    private String hostname;

    private String protocol;

    private Integer port;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        /*
         * Initialize internet free busy API with generated REST API client
         */
        ApiClient restClient = RESTUtilities.createRESTClient(testContext.getUsedBy());
        restClient.setVerifyingSsl(false);
        restClient.setBasePath(getBasePath());
        restUser = TestContextPool.getRestUser();
        restClient.setUsername(restUser.getUser());
        restClient.setPassword(restUser.getPassword());
        String authorizationHeaderValue = "Basic " + Base64.encodeBase64String((restUser.getUser() + ":" + restUser.getPassword()).getBytes(Charsets.UTF_8));
        restClient.addDefaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
        api = new InternetFreeBusyApi(restClient);
        /*
         * Prepare config
         */
        CONFIG.put(FreeBusyProperty.PUBLISH_INTERNET_FREEBUSY.getFQPropertyName(), "true");
        /*
         * Create test data
         */
        userName = testUser.getUser();
        server = testUser.getContext();
        contextid = testUser.getContextId();

        Integer resId = testContext.acquireResource();
        ResourceResponse resource = new ResourcesApi(getApiClient(Host.SINGLENODE)).getResource(resId);
        assertNull(resource.getError());
        testResourceName = resource.getData().getName();
        this.testDate = new Date();
        this.busyDate = createEvent(TranspEnum.OPAQUE, FreeBusyTest.class.getName() + "_Busy", 10, 11);
        this.freeDate = createEvent(TranspEnum.TRANSPARENT, FreeBusyTest.class.getName() + "_Free", 11, 12);
    }

    protected String getBasePath() {
        if (hostname == null) {
            this.hostname = AJAXConfig.getProperty(AJAXConfig.Property.SINGLENODE_HOSTNAME);
        }

        if (protocol == null) {
            this.protocol = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
            if (this.protocol == null) {
                this.protocol = "http";
            }
        }

        if (port == null) {
            this.port = Integer.valueOf(AJAXConfig.getProperty(AJAXConfig.Property.SERVER_PORT));
        }

        return this.protocol + "://" + this.hostname + (protocol.equals("https") ? ":443" : String.format(":%d", this.port));
    }

    // -------------------------------Positive tests--------------------------------------------------------------

    /**
     *
     * Tests whether valid iCalendar data is returned,
     * if only contentId, user name and server name are given.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_MinimumValidData_Status200() throws Exception, ApiException {
        super.setUpConfiguration();
        waitForServlet();
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, null, null, null);
        validateICal(icalResponse, userName, server, DEFAULT_WEEKS_PAST, DEFAULT_WEEKS_FUTURE, false);
    }

    /**
     *
     * Tests whether valid iCalendar data is returned,
     * if contentId, resource name and server name are given.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_Resource_Status200() throws Exception, ApiException {
        super.setUpConfiguration();
        waitForServlet();
        String icalResponse = api.getFreeBusy(I(contextid), testResourceName, server, null, null, null);
        validateICal(icalResponse, testResourceName, server, DEFAULT_WEEKS_PAST, DEFAULT_WEEKS_FUTURE, false);
    }

    /**
     *
     * Tests whether valid iCalendar data is returned for a resource, if the value simple is set to true.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_ResourceSimple_Status200() throws Exception, ApiException {
        super.setUpConfiguration();
        waitForServlet();
        String icalResponse = api.getFreeBusy(I(contextid), testResourceName, server, null, null, B(true));
        validateICal(icalResponse, testResourceName, server, DEFAULT_WEEKS_PAST, DEFAULT_WEEKS_FUTURE, true);
    }

    /**
     *
     * Tests whether valid iCalendar data is returned, if all parameters are given.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_MaximumValidData_Status200() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        int weeksPast = 12;
        int weeksFuture = 26;
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, I(weeksPast), I(weeksFuture), B(false));
        validateICal(icalResponse, userName, server, weeksPast, weeksFuture, false);
    }

    /**
     *
     * Tests whether valid iCalendar data is returned, if the value simple is set to true.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_Simple_Status200() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, null, null, B(true));
        validateICal(icalResponse, userName, server, DEFAULT_WEEKS_PAST, DEFAULT_WEEKS_FUTURE, true);
    }

    /**
     *
     * Tests whether valid iCalendar data is returned, if the requested time range into future is greater than the configured maximum.
     * In this case the time range up to the configured time range is expected.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_PropertyWeeksFuture_Status200() throws Exception {
        int maxTimerangeFuture = 1;
        CONFIG.put(FreeBusyProperty.INTERNET_FREEBUSY_MAXIMUM_TIMERANGE_FUTURE.getFQPropertyName(), I(maxTimerangeFuture).toString());
        super.setUpConfiguration();
        waitForServlet();
        int weeksFuture = 2;
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, null, I(weeksFuture), null);
        validateICal(icalResponse, userName, server, DEFAULT_WEEKS_PAST, maxTimerangeFuture, false);
    }

    /**
     *
     * Tests whether valid iCalendar data is returned, if the requested time range into past is greater than the configured maximum.
     * In this case the time range down to the configured time range is expected.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_PropertyWeeksPast_Status200() throws Exception {
        int maxTimerangePast = 1;
        CONFIG.put(FreeBusyProperty.INTERNET_FREEBUSY_MAXIMUM_TIMERANGE_PAST.getFQPropertyName(), I(maxTimerangePast).toString());
        super.setUpConfiguration();
        waitForServlet();
        int weeksPast = 2;
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, I(weeksPast), null, null);
        validateICal(icalResponse, userName, server, maxTimerangePast, DEFAULT_WEEKS_FUTURE, false);
    }

    /**
     *
     * Tests the edge case where the maximum time range into the past property is set to 0.
     * In this case no past free busy times should be returned.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_PropertyWeeksPast0_Status200() throws Exception {
        int maxTimerangePast = 0;
        CONFIG.put(FreeBusyProperty.INTERNET_FREEBUSY_MAXIMUM_TIMERANGE_PAST.getFQPropertyName(), I(maxTimerangePast).toString());
        super.setUpConfiguration();
        waitForServlet();
        int weeksPast = 2;
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, I(weeksPast), null, null);
        validateICal(icalResponse, userName, server, maxTimerangePast, DEFAULT_WEEKS_FUTURE, false);
    }

    /**
     *
     * Tests the edge case where the time range into the past parameter is set to 0.
     * In this case no past free busy times should be returned.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_WeeksPast0_Status200() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        int weeksPast = 0;
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, I(weeksPast), null, null);
        validateICal(icalResponse, userName, server, weeksPast, DEFAULT_WEEKS_FUTURE, false);
    }

    /**
     *
     * Tests the edge case where the maximum time range into the future property is set to 0.
     * In this case no future free busy times should be returned.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_PropertyWeeksFuture0_Status200() throws Exception {
        int maxTimerangeFuture = 0;
        CONFIG.put(FreeBusyProperty.INTERNET_FREEBUSY_MAXIMUM_TIMERANGE_FUTURE.getFQPropertyName(), I(maxTimerangeFuture).toString());
        super.setUpConfiguration();
        waitForServlet();
        int weeksFuture = 2;
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, null, I(weeksFuture), null);
        validateICal(icalResponse, userName, server, DEFAULT_WEEKS_PAST, maxTimerangeFuture, false);
    }

    /**
     *
     * Tests the edge case where the time range into the future parameter is set to 0.
     * In this case no future free busy times should be returned.
     *
     * @throws Exception if changing the configuration fails
     * @throws ApiException if an error occurred by getting the free busy data
     */
    @Test
    public void testGetFreeBusy_WeeksFuture0_Status200() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        int weeksFuture = 0;
        String icalResponse = api.getFreeBusy(I(contextid), userName, server, null, I(weeksFuture), null);
        validateICal(icalResponse, userName, server, DEFAULT_WEEKS_PAST, weeksFuture, false);
    }

    // -------------------------------Depend on property tests--------------------------------------------------------------

    /**
     *
     * Tests whether getting free busy data failed with code 404,
     * if free busy data is not published for the requested user.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_PublishDisabled_Status404() throws Exception {
        CONFIG.put(FreeBusyProperty.PUBLISH_INTERNET_FREEBUSY.getFQPropertyName(), "false");
        super.setUpConfiguration();
        try {
            api.getFreeBusy(I(contextid), userName, server, null, null, null);
            fail("Expected error code 404");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    // -------------------------------Invalid input tests--------------------------------------------------------------

    /**
     *
     * Tests whether getting free busy data failed with code 404,
     * if context id is null and therefore missing.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_ContextIdNull_Status404() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusyBuilder().withUserName(userName).withServer(server).skipParamCheck().execute();
            fail("Expected error code 404");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(404, e.getCode(), "Expected 404 error: " + e.getResponseBody());
        }
    }

    /**
     *
     * Tests whether getting free busy data failed with code 404,
     * if context id is negative.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_ContextIdNegative_Status404() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusy(I(-1), userName, server, null, null, null);
            fail("Expected error code 404");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(404, e.getCode(), "Expected 404 error: " + e.getResponseBody());
        }
    }

    /**
     *
     * Tests whether getting free busy data failed with code 404,
     * if user name is null.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_UsernameNull_Status404() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusyBuilder().withContextId(I(contextid)).withServer(server).skipParamCheck().execute();
            fail("Expected error code 404");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    /**
     *
     * Tests whether getting free busy data failed with code 404,
     * if server name is null.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_ServerNull_Status404() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusyBuilder().withContextId(I(contextid)).withUserName(userName).skipParamCheck().execute();
            fail("Expected error code 404");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    /**
     *
     * Tests whether getting free busy data failed with code 404,
     * if context id does not fit to user and server name.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_WrongContextId_Status404() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusy(I(contextid + 1), userName, server, null, null, null);
            fail("Expected error code 404");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    /**
     *
     * Tests whether getting free busy data failed with code 404,
     * if the user name is not existing in the context.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_WrongUserName_Status404() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusy(I(contextid), userName + "Test", server, null, null, null);
            fail("Expected error code 404");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    /**
     *
     * Tests whether getting free busy data failed with code 404,
     * if the server name does not fit to context id and user name.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_WrongServer_Status404() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusy(I(contextid), userName, server + ".test", null, null, null);
            fail("Expected error code 404");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    /**
     *
     * Tests whether getting free busy data failed with code 400,
     * if the time range into past value is negative.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_WeeksStartNegative_Status400() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusy(I(contextid), userName, server, I(-1), null, null);
            fail("Expected error code 400");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    /**
     *
     * Tests whether getting free busy data failed with code 400,
     * if the time range into future value is negative.
     *
     * @throws Exception if changing the configuration fails
     */
    @Test
    public void testGetFreeBusy_WeeksEndNegative_Status400() throws Exception {
        super.setUpConfiguration();
        waitForServlet();
        try {
            api.getFreeBusy(I(contextid), userName, server, null, I(-1), null);
            fail("Expected error code 400");
        } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    // -------------------------------Helper methods--------------------------------------------------------------

    private EventData createEvent(TranspEnum visibility, String name, int start, int end) throws ApiException {
        EventData event = new EventData();
        event.setPropertyClass("PUBLIC");
        Attendee attendeeUser = new Attendee();
        attendeeUser.entity(getApiClient(Host.SINGLENODE).getUserId());
        attendeeUser.cuType(CuTypeEnum.INDIVIDUAL);
        Attendee attendeeRessouce = new Attendee();
        attendeeRessouce.setUri(CalendarUtils.getURI(testResourceName + "@" + server));
        attendeeRessouce.cuType(CuTypeEnum.RESOURCE);
        List<Attendee> attendees = new ArrayList<>();
        attendees.add(attendeeUser);
        attendees.add(attendeeRessouce);
        event.setAttendees(attendees);
        event.setTransp(visibility);
        event.setSummary(name);
        Date startDate = CalendarUtils.add(new Date(), Calendar.HOUR_OF_DAY, start);
        event.setStartDate(DateTimeUtil.getDateTime(TimeZones.UTC.getID(), startDate.getTime()));
        Date endDate = CalendarUtils.add(new Date(), Calendar.HOUR_OF_DAY, end);
        event.setEndDate(DateTimeUtil.getDateTime(TimeZones.UTC.getID(), endDate.getTime()));

        return eventManager.createEvent(event);
    }

    private void validateICal(String contentStr, String userName, String serverName, int weeksPast, int weeksFuture, boolean simple) throws ParseException {
        String content = removeLinebreaksAndWhiteSpace(contentStr);
        assertTrue(content.startsWith("BEGIN:VCALENDAR") && content.contains("END:VCALENDAR"), "Is no VCalendar");
        assertTrue(content.contains("BEGIN:VFREEBUSY") && content.contains("END:VFREEBUSY"), "Contains no VFreeBusy");
        String startDate = this.busyDate.getStartDate().getValue();
        String endDate = this.busyDate.getEndDate().getValue();
        if (weeksFuture > 0) {
            if (simple) {
                assertTrue(content.contains("FREEBUSY:"), "Contains no simple free busy data.");

                assertTrue(content.contains(startDate) && content.contains(endDate), "Does not contain the busy data");
            } else {
                assertTrue(content.contains("FREEBUSY;FBTYPE=BUSY:"), "Contains no extended free busy data.");
                assertTrue(content.contains(startDate) && content.contains(endDate), "Does not contain the busy data");
                assertTrue(content.contains("FREEBUSY;FBTYPE=FREE:"), "Contains no extended free busy data.");
                startDate = this.freeDate.getStartDate().getValue();
                endDate = this.freeDate.getEndDate().getValue();
                assertTrue(content.contains(startDate) && content.contains(endDate), "Does not contain the free data");
            }
        }

        Date expectedStart = CalendarUtils.add(this.testDate, Calendar.WEEK_OF_YEAR, -weeksPast);
        int startIndex = content.indexOf("DTSTART") + 8;
        String dtStart = content.substring(startIndex, startIndex + 17);
        Date actualStart = DateTimeUtil.parseZuluDateTime(dtStart);
        assertTrue(Math.abs(expectedStart.getTime() - actualStart.getTime()) < 5000, "Contains wrong start time");

        Date expectedEnd = CalendarUtils.add(this.testDate, Calendar.WEEK_OF_YEAR, weeksFuture);
        int endIndex = content.indexOf("DTEND") + 6;
        String dtEnd = content.substring(endIndex, endIndex + 17);
        Date actualEnd = DateTimeUtil.parseZuluDateTime(dtEnd);
        assertTrue(Math.abs(expectedEnd.getTime() - actualEnd.getTime()) < 5000, "Contains wrong end time");

        assertTrue(content.contains(userName), "Does not contain the user name");
        assertTrue(content.contains(serverName), "Does not contain the server name");
    }

    /**
     * Prepares the ical string. Removes linebreaks and leading and trailing whitespaces.
     *
     * @param s The string to prepare
     * @return Thre resulting string
     */
    private static final String removeLinebreaksAndWhiteSpace(String s) {
        List<String> lines = Arrays.asList(s.split("\\r?\\n"));
        StringBuilder str = new StringBuilder();
        lines.stream().map(tmp -> tmp.trim()).forEach(tmp -> str.append(tmp));
        return str.toString();
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        return CONFIG;
    }

    @Override
    protected String getScope() {
        return "context";
    }

    private void waitForServlet() throws InterruptedException {
        int counter = 0;
        boolean isAvailable = false;
        com.openexchange.testing.restclient.invoker.ApiException error = null;
        while (!isAvailable && counter < MAX_RETRY) {
            try {
                api.getFreeBusy(I(contextid), userName, server, null, null, null);
                isAvailable = true;
            } catch (com.openexchange.testing.restclient.invoker.ApiException e) {
                // servlet is not available. waiting ...
                Thread.sleep(1000);
                counter++;
                error = e;
            }
        }
        if (!isAvailable) {
            fail("Servlet is not available: " + (error == null ? "Unknown error" : error.getMessage()));
        }
    }

}
