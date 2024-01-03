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

import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.disableAutoProcessing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;
import org.exparity.hamcrest.date.DateMatchers;
import org.hamcrest.MatcherAssert;
import org.jdom2.IllegalDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.google.common.io.BaseEncoding;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.exception.Category;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ActionResponse;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.ConversionDataSource;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailDestinationResponse;
import com.openexchange.testing.httpclient.models.MailListElement;
import com.openexchange.testing.httpclient.models.MailsCleanUpResponse;
import com.openexchange.testing.httpclient.models.UserResponse;
import com.openexchange.testing.httpclient.modules.ChronosApi;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.UserApi;

/**
 * {@link AbstractITipTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.0
 */
public abstract class AbstractITipTest extends AbstractChronosTest {

    protected static final String IMIP_TEMPLATE = // @formatter:off
        "Return-Path: {{FROM}}" + "\r\n" +
        "Delivered-To: {{TO}}" + "\r\n" +
        "From: {{FROM}}" + "\r\n" +
        "To: {{TO}}" + "\r\n" +
        "Subject: {{SUBJECT}}" + "\r\n" +
        "Date: {{DATE}}" + "\r\n" +
        "Message-ID: <{{MESSAGE_ID}}@WINHEXBEEU109.win.mail>" + "\r\n" +
        "Accept-Language: de-DE, en-US" + "\r\n" +
        "Content-Language: de-DE" + "\r\n" +
        "Content-Type: multipart/alternative;" + "\r\n" +
        "   boundary=\"_000_7852067692f94262ac04e48a013322e2WINHEXBEEU109winmail_\"" + "\r\n" +
        "MIME-Version: 1.0" + "\r\n" +
        "X-Spam-Flag: NO" + "\r\n" +
        "" + "\r\n" +
        "--_000_7852067692f94262ac04e48a013322e2WINHEXBEEU109winmail_" + "\r\n" +
        "Content-Type: text/plain; charset=\"iso-8859-1\"" + "\r\n" +
        "Content-Transfer-Encoding: quoted-printable" + "\r\n" +
        "" + "\r\n" +
        "" + "\r\n" +
        "" + "\r\n" +
        "--_000_7852067692f94262ac04e48a013322e2WINHEXBEEU109winmail_" + "\r\n" +
        "Content-Type: text/html; charset=\"iso-8859-1\"" + "\r\n" +
        "Content-Transfer-Encoding: quoted-printable" + "\r\n" +
        "" + "\r\n" +
        "<html>" + "\r\n" +
        "<head>" + "\r\n" +
        "<meta http-equiv=3D\"Content-Type\" content=3D\"text/html; charset=3Diso-8859-=" + "\r\n" +
        "1\">" + "\r\n" +
        "<style type=3D\"text/css\" style=3D\"display:none\"><!-- p { margin-top: 0px; m=" + "\r\n" +
        "argin-bottom: 0px; }--></style>" + "\r\n" +
        "</head>" + "\r\n" +
        "<body dir=3D\"ltr\" style=3D\"font-size:12pt;color:#000000;background-color:#F=" + "\r\n" +
        "FFFFF;font-family:Calibri,Arial,Helvetica,sans-serif;\">" + "\r\n" +
        "<p><br>" + "\r\n" +
        "</p>" + "\r\n" +
        "</body>" + "\r\n" +
        "</html>" + "\r\n" +
        "" + "\r\n" +
        "--_000_7852067692f94262ac04e48a013322e2WINHEXBEEU109winmail_" + "\r\n" +
        "Content-Type: text/calendar; charset=\"utf-8\"; method={{METHOD}}" + "\r\n" +
        "Content-Transfer-Encoding: base64" + "\r\n" +
        "" + "\r\n" +
        "{{ITIP}}" + "\r\n" +
        "" + "\r\n" +
        "--_000_7852067692f94262ac04e48a013322e2WINHEXBEEU109winmail_--" + "\r\n"
    ; // @formatter:on

    protected SessionAwareClient apiClient;

    protected UserResponse userResponseC1;

    protected UserResponse userResponseC2;

    protected SessionAwareClient apiClientC2;

    protected TestUser testUserC2;

    protected TestContext context2;

    protected String folderIdC2;

    protected EventManager eventManagerC2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        apiClient = testUser.getApiClient();
        UserApi api = new UserApi(getApiClient());
        userResponseC1 = api.getUser(String.valueOf(testUser.getUserId()));

        if (testContextList.size() > 1) {
            context2 = testContextList.get(1);
            testUserC2 = context2.acquireUser();
            apiClientC2 = testUserC2.getApiClient();
            UserApi anotherUserApi = new UserApi(apiClientC2);
            userResponseC2 = anotherUserApi.getUser(String.valueOf(apiClientC2.getUserId()));
            // Validate
            if (null == userResponseC1 || null == userResponseC2) {
                throw new IllegalDataException("Need both users for iTIP tests!");
            }
            folderIdC2 = getDefaultFolder(apiClientC2);
            eventManagerC2 = new EventManager(new com.openexchange.ajax.chronos.UserApi(apiClientC2, testUserC2), folderIdC2);
            eventManagerC2.setIgnoreConflicts(true);
        }
        eventManager.setIgnoreConflicts(true);
        setAutoSheduling();
    }

    protected void setAutoSheduling() throws Exception {
        disableAutoProcessing(testContext);
        if (null != context2) {
            disableAutoProcessing(context2);
        }
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).build();
    }

    /*
     * =========================
     * ========SHORTCUTS========
     * =========================
     */

    /**
     * @See {@link ChronosApi#accept(ConversionDataSource, String, String)}
     */
    protected ActionResponse accept(ConversionDataSource body, String comment) throws ApiException {
        return accept(testUser.getApiClient(), body, comment);
    }

    /**
     * @See {@link ChronosApi#accept(ConversionDataSource, String, String)}
     */
    protected ActionResponse accept(ApiClient apiClient, ConversionDataSource body, String comment) throws ApiException {
        ActionResponse response = new ChronosApi(apiClient).accept(body, comment, null);
        validateActionResponse(response);
        return response;
    }

    /**
     * @See {@link ChronosApi#acceptAndIgnoreConflicts(ConversionDataSource, String, String)}
     */
    protected ActionResponse acceptAndIgnoreConflicts(ConversionDataSource body, String comment) throws ApiException {
        ActionResponse response = chronosApi.acceptAndIgnoreConflicts(body, comment, null);
        validateActionResponse(response);
        return response;
    }

    /**
     * @See {@link ChronosApi#tentative(ConversionDataSource, String, String)}
     */
    protected ActionResponse tentative(ConversionDataSource body, String comment) throws ApiException {
        ActionResponse response = chronosApi.tentative(body, comment, null);
        validateActionResponse(response);
        return response;
    }

    /**
     * @See {@link ChronosApi#tentative(ConversionDataSource, String, String)}
     */
    protected ActionResponse tentative(ApiClient apiClient, ConversionDataSource body, String comment) throws ApiException {
        ActionResponse response = new ChronosApi(apiClient).tentative(body, comment, null);
        validateActionResponse(response);
        return response;
    }

    /**
     * @See {@link ChronosApi#decline(String, String, String, ConversionDataSource)}
     */
    protected ActionResponse decline(ConversionDataSource body, String comment) throws ApiException {
        ActionResponse response = chronosApi.decline(body, comment, null);
        validateActionResponse(response);
        return response;
    }

    /**
     * @See {@link ChronosApi#decline(ConversionDataSource, String, String)}
     */
    protected ActionResponse decline(ApiClient apiClient, ConversionDataSource body, String comment) throws ApiException {
        ActionResponse response = new ChronosApi(apiClient).decline(body, comment, null);
        validateActionResponse(response);
        return response;
    }

    protected CalendarResult applyCreate(ApiClient apiClient, ConversionDataSource body) throws ApiException {
        ChronosCalendarResultResponse response = new ChronosApi(apiClient).applyCreate(body, null);
        return checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }

    protected CalendarResult applyChange(ApiClient apiClient, ConversionDataSource body) throws ApiException {
        ChronosCalendarResultResponse response = new ChronosApi(apiClient).applyChange(body, null);
        return checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }

    protected CalendarResult applyRemove(ApiClient apiClient, ConversionDataSource body) throws ApiException {
        ChronosCalendarResultResponse response = new ChronosApi(apiClient).applyRemove(body, null);
        return checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }

    protected CalendarResult applyResponse(ApiClient apiClient, ConversionDataSource body) throws ApiException {
        ChronosCalendarResultResponse response = new ChronosApi(apiClient).applyResponse(body, null);
        return checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }

    /**
     * @See {@link ChronosApi#cancel(ConversionDataSource, String, String)}
     * @param expectData A value indicating whether it is expected that event data is returned or not. If set to <code>false</code> only the timestamp will be checked
     */
    protected ActionResponse cancel(ConversionDataSource body, String comment, boolean expectData) throws ApiException {
        return cancel(testUser.getApiClient(), body, comment, expectData);
    }

    /**
     * @See {@link ChronosApi#cancel(ConversionDataSource, String, String)}
     * @param expectData A value indicating whether it is expected that event data is returned or not. If set to <code>false</code> only the timestamp will be checked
     */
    protected ActionResponse cancel(ApiClient apiClient, ConversionDataSource body, String comment, boolean expectData) throws ApiException {
        ActionResponse response = new ChronosApi(apiClient).cancel(body, comment, null);
        MatcherAssert.assertThat(response.getTimestamp(), is(not(nullValue())));
        MatcherAssert.assertThat("Only timestamp should be returned", new Date(response.getTimestamp().longValue()), DateMatchers.within(3, ChronoUnit.SECONDS, new Date()));
        if (expectData) {
            validateActionResponse(response);
        } else {
            MatcherAssert.assertThat("Only timestamp should be returned", response.getData(), is(empty()));
        }
        return response;
    }

    private void validateActionResponse(ActionResponse response) {
        MatcherAssert.assertThat("Excpected analyze-data", response.getData(), is(not(empty())));
    }

    /**
     * @See {@link ChronosApi#analyze(ConversionDataSource, String)}
     */
    protected AnalyzeResponse analyze(ConversionDataSource body) throws ApiException {
        return chronosApi.analyze(body, null);
    }

    protected static AnalyzeResponse analyze(ApiClient apiClient, MailData mailData) throws Exception {
        ConversionDataSource body = constructBody(mailData);
        return new ChronosApi(apiClient).analyze(body, null);
    }

    /*
     * ============================== HELPERS ==============================
     */

    protected static String generateImip(Map<String, String> replacements) {
        String template = IMIP_TEMPLATE;
        for (Entry<String, String> entry : replacements.entrySet()) {
            template = template.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
        }
        return template;
    }

    protected static String generateImip(String from, String to, String messageId, String subject, Date date, SchedulingMethod method, String iTip) {
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("{{FROM}}", from);
        replacements.put("{{TO}}", to);
        replacements.put("{{MESSAGE_ID}}", Strings.isNotEmpty(messageId) ? messageId : UUID.randomUUID().toString());
        replacements.put("{{SUBJECT}}", subject);
        replacements.put("{{DATE}}", new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(date));
        replacements.put("{{METHOD}}", method.name().toUpperCase());
        replacements.put("{{ITIP}}", BaseEncoding.base64().withSeparator("\r\n", 77).encode(iTip.getBytes(Charsets.UTF_8)));
        return generateImip(replacements);
    }

    protected static MailDestinationResponse sendImip(com.openexchange.testing.httpclient.invoker.ApiClient client, String iMip) throws Exception {
        File tmpFile = null;
        MailDestinationResponse response;
        try {
            tmpFile = File.createTempFile("test", ".tmp");
            try (FileWriter writer = new FileWriter(tmpFile)) {
                writer.write(iMip);
            }
            response = new MailApi(client).sendOrSaveMail(tmpFile, null, null);
        } finally {
            if (null != tmpFile) {
                tmpFile.delete();
            }
        }
        if (null != response.getError() && false == Category.CATEGORY_WARNING.toString().equals(response.getCategories())) {
            assertNull(response.getError(), response.getError());
        }
        return response;
    }

    protected static String randomUID() {
        return UUID.randomUUID().toString();
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

    protected static void deleteMail(MailApi mailApi, MailData data) throws Exception {
        MailListElement mailListElement = new MailListElement();
        mailListElement.setId(data.getId());
        mailListElement.setFolder(data.getFolderId());
        for (int i = 0; i < 10; i++) {
            try {
                MailsCleanUpResponse deleteResponse = mailApi.deleteMails(Collections.singletonList(mailListElement), null, Boolean.TRUE, Boolean.FALSE);
                List<String> notDeleted = checkResponse(deleteResponse.getError(), deleteResponse.getErrorDesc(), deleteResponse.getData());
                if (notDeleted.isEmpty()) {
                    return;
                }
            } catch (Exception | AssertionError e) {
                // try again
            }
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        }
        MailsCleanUpResponse deleteResponse = mailApi.deleteMails(Collections.singletonList(mailListElement), null, Boolean.TRUE, Boolean.FALSE);
        List<String> notDeleted = checkResponse(deleteResponse.getError(), deleteResponse.getErrorDesc(), deleteResponse.getData());
        assertThat(notDeleted, is(empty()));
    }

}
