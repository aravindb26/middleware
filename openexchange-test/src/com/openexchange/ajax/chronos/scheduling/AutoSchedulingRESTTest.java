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

package com.openexchange.ajax.chronos.scheduling;

import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertAttendeePartStat;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.ajax.chronos.itip.ITipUtil.setAutoProcessing;
import static com.openexchange.ajax.chronos.util.DateTimeUtil.parseDateTime;
import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.google.gson.Gson;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.itip.ITipUtil;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.test.common.asset.Asset;
import com.openexchange.test.common.asset.AssetType;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.invoker.Pair;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.ChronosAttachment;
import com.openexchange.testing.httpclient.models.ComposeBody;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailComposeResponse;
import com.openexchange.testing.httpclient.models.MailComposeResponseMessageModel;
import com.openexchange.testing.httpclient.models.MailComposeSendResponse;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.modules.MailComposeApi;
import com.openexchange.testing.restclient.invoker.ApiClient;
import com.openexchange.testing.restclient.models.PushMail;
import com.openexchange.testing.restclient.modules.PushApi;
import okhttp3.Call;
import okhttp3.Response;

/**
 *
 * {@link AutoSchedulingRESTTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class AutoSchedulingRESTTest extends AbstractSchedulingTest {

    private PushApi restApi;

    private String summary;

    private Attendee replyingAttendee;

    /*
     * ============================== Third context ==============================
     */
    protected String folderIdC3;

    protected EventManager eventManagerC3;

    private TestContext context3;

    private TestUser testUserC3;

    private SessionAwareClient apiClientC3;

    @Override
    protected String getScope() {
        return "context";
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        /*
         * Enabled auto-scheduling
         */
        super.setUp(testInfo);
        /*
         * Set properties
         * See com.openexchange.chronos.scheduling.PushedIMipResolveMode
         * and com.openexchange.chronos.scheduling.impl.incoming.IncomingSchedulingMailListener.ON_MAIL_FETCH_PROPERTY
         */
        Map<String, String> map = new HashMap<>(super.getNeededConfigurations());
        map.put("com.openexchange.calendar.pushedIMipResolveMode", "LOGININFO");
        map.put("com.openexchange.calendar.autoProcessIMipOnMailFetch", Boolean.FALSE.toString());
        for (TestContext context : testContextList) {
            changeConfigWithOwnClient(context.getUsers().get(0), map);
        }
        /*
         * Prepare third context
         */
        context3 = testContextList.get(1);
        testUserC3 = context3.acquireUser();
        apiClientC3 = testUserC3.getApiClient();
        folderIdC3 = getDefaultFolder(apiClientC3);
        eventManagerC3 = new EventManager(new com.openexchange.ajax.chronos.UserApi(apiClientC3, testUserC3), folderIdC3);
        eventManagerC3.setIgnoreConflicts(true);

        setAutoProcessing(context3);
        /*
         * Initialize REST API client
         */
        ApiClient restClient = RESTUtilities.createRESTClient(context3.getUsedBy());
        restApi = new PushApi(restClient);

        summary = this.getClass().getName() + " " + UUID.randomUUID().toString();
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(3).build();
    }

    @Test
    public void testSingleEvent() throws Exception {
        EventData preparedEvent = EventFactory.createSingleTwoHourEvent(0, summary);
        replyingAttendee = prepareCommonAttendees(preparedEvent);
        createdEvent = eventManager.createEvent(preparedEvent, true);

        /*
         * Receive mail as attendee
         */
        String iMip = receiveMail();
        /*
         * Prepare push mail
         */
        PushMail mail = new PushMail();
        mail.setUser(testUserC2.getLogin());
        mail.setFolder("INBOX");
        mail.setEvent(PushMail.EventEnum.MESSAGENEW);
        mail.setBody(iMip);

        restApi.pushmail(mail);

        waitForBackgroundProcess();

        /*
         * Load event as attendee
         */
        List<EventData> allEvents = eventManagerC2.getAllEvents(parseDateTime(createdEvent.getStartDate()), parseDateTime(createdEvent.getEndDate()));
        allEvents = allEvents.stream().filter(e -> summary.equals(e.getSummary())).collect(Collectors.toList());
        assertThat(I(allEvents.size()), is(I(1)));
        assertAttendeePartStat(allEvents.get(0).getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION.getStatus());
        assertThat("Event ID not set", allEvents.get(0).getId(), notNullValue());
    }

    @Test
    public void testEventWithAttachment() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);
        EventData preparedEvent = EventFactory.createSingleEventWithAttachment(0, summary, asset);
        replyingAttendee = prepareCommonAttendees(preparedEvent);
        JSONObject expectedEventData = eventManager.createEventWithAttachment(preparedEvent, asset);
        createdEvent = eventManager.getEvent(defaultFolderId, expectedEventData.getString("id"));
        /*
         * Receive mail as attendee
         */
        String iMip = receiveMail();
        /*
         * Prepare push mail
         */
        PushMail mail = new PushMail();
        mail.setUser(testUserC2.getLogin());
        mail.setFolder("INBOX");
        mail.setEvent(PushMail.EventEnum.MESSAGENEW);
        mail.setBody(iMip);

        restApi.pushmail(mail);

        waitForBackgroundProcess();

        /*
         * Load event as attendee, check attachment
         */
        List<EventData> allEvents = eventManagerC2.getAllEvents(parseDateTime(createdEvent.getStartDate()), parseDateTime(createdEvent.getEndDate()));
        allEvents = allEvents.stream().filter(e -> summary.equals(e.getSummary())).collect(Collectors.toList());
        assertThat(I(allEvents.size()), is(I(1)));
        EventData attendeeEvent = allEvents.get(0);
        assertAttendeePartStat(attendeeEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION.getStatus());
        assertThat("Event ID not set", attendeeEvent.getId(), notNullValue());
        assertThat("No attachment set", attendeeEvent.getAttachments(), is(not(empty())));
        ChronosAttachment attachment = attendeeEvent.getAttachments().get(0);
        assertThat("Corrupted attachment", attachment.getFilename(), is(asset.getFilename()));
        assertTrue(null != attachment.getFmtType() && attachment.getFmtType().toLowerCase().startsWith(asset.getAssetType().toString().toLowerCase()), "Corrupted attachment");
        //        assertThat("Corrupted attachment", attachment.getFmtType(), new StringStartsWith(true, asset.getAssetType().toString()));
        assertThat("Corrupted attachment", attachment.getManagedId(), notNullValue());
        assertThat("Corrupted attachment", attachment.getUri(), nullValue());
    }

    @Test
    public void testRecipientNotAttendee() throws Exception {
        EventData preparedEvent = EventFactory.createSingleTwoHourEvent(0, summary);
        replyingAttendee = prepareCommonAttendees(preparedEvent);
        createdEvent = eventManager.createEvent(preparedEvent, true);

        /*
         * Receive mail as non-attendee
         */
        String iMip = receiveMailAsNonAttendee();
        /*
         * Prepare push mail
         */
        PushMail mail = new PushMail();
        mail.setUser(testUserC3.getLogin());
        mail.setFolder("INBOX");
        mail.setEvent(PushMail.EventEnum.MESSAGENEW);
        mail.setBody(iMip);

        restApi.pushmail(mail);

        waitForBackgroundProcess();

        /*
         * Load event as attendee
         */
        List<EventData> allEvents = eventManagerC3.getAllEvents(parseDateTime(createdEvent.getStartDate()), parseDateTime(createdEvent.getEndDate()));
        allEvents = allEvents.stream().filter(e -> summary.equals(e.getSummary())).collect(Collectors.toList());
        assertThat(I(allEvents.size()), is(I(0)));
    }
    /*
     * ============================== HELPERS ==============================
     */

    private String receiveMailAsNonAttendee() throws Exception {
        /*
         * Receive mail
         */
        MailComposeApi mailComposeApi = new MailComposeApi(apiClientC2);
        ComposeBody composeBody = new ComposeBody();
        composeBody.setId(receiveIMip(apiClientC2, userResponseC1.getData().getEmail1(), summary, 0, SchedulingMethod.REQUEST).getId());
        composeBody.setFolderId(ITipUtil.FOLDER_HUMAN_READABLE);
        MailComposeResponse mailCompose = mailComposeApi.postMailCompose("forward", Boolean.FALSE, null, null, Collections.singletonList(composeBody));
        assertNull(mailCompose.getError());

        /*
         * Set to new recipient
         */
        MailComposeResponseMessageModel data = mailCompose.getData();
        ArrayList<String> toList = new ArrayList<>(2);
        toList.add(testUserC3.getUser());
        toList.add(testUserC3.getLogin());
        ArrayList<String> fromList = new ArrayList<>(2);
        fromList.add(userResponseC2.getData().getDisplayName());
        fromList.add(userResponseC2.getData().getEmail1());
        data.setTo(Collections.singletonList(toList));
        data.setFrom(fromList);
        MailComposeSendResponse forwardedMail = mailComposeApi.postMailComposeSend(data.getId(), new Gson().toJson(data), null, null);
        assertNull(forwardedMail.getErrorDesc());

        /*
         * Receive Mail
         */
        MailData iMip = receiveIMip(apiClientC3, userResponseC2.getData().getEmail1(), summary, 0, SchedulingMethod.REQUEST);

        /*
         * Get as raw mail
         */
        return getMailRaw(apiClientC3, iMip.getFolderId(), iMip.getId(), null, null, "noimg", Boolean.FALSE, Boolean.TRUE, null, null, null, null, null, null, null);
    }

    private String receiveMail() throws Exception {
        MailData iMip = receiveIMip(apiClientC2, userResponseC1.getData().getEmail1(), summary, 0, SchedulingMethod.REQUEST);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClientC2, iMip)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(createdEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION.getStatus());
        /*
         * Get as raw mail
         */
        return getMailRaw(apiClientC2, iMip.getFolderId(), iMip.getId(), null, null, "noimg", Boolean.FALSE, Boolean.TRUE, null, null, null, null, null, null, null);
    }

    private String getMailRaw(com.openexchange.testing.httpclient.invoker.ApiClient apiClient, String folder, String id, String messageId, Integer edit, String view, Boolean forceImages, Boolean unseen, Integer maxSize, Integer attachSrc, Boolean estimateLength, Boolean pregeneratePreviews, Boolean noNestedMessage, Boolean decrypt, String cryptoAuth) throws ApiException, JSONException, IOException {
        // create path and map variables
        String localVarPath = "/mail?action=get";

        java.util.List<Pair> localVarQueryParams = new java.util.ArrayList<Pair>();
        java.util.List<Pair> localVarCollectionQueryParams = new java.util.ArrayList<Pair>();
        if (folder != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("folder", folder));
        }

        if (id != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("id", id));
        }

        if (messageId != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("message_id", messageId));
        }

        if (edit != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("edit", edit));
        }

        if (view != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("view", view));
        }

        if (forceImages != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("forceImages", forceImages));
        }

        if (unseen != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("unseen", unseen));
        }

        if (maxSize != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("max_size", maxSize));
        }

        if (attachSrc != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("attach_src", attachSrc));
        }

        if (estimateLength != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("estimate_length", estimateLength));
        }

        if (pregeneratePreviews != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("pregenerate_previews", pregeneratePreviews));
        }

        if (noNestedMessage != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("no_nested_message", noNestedMessage));
        }

        if (decrypt != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("decrypt", decrypt));
        }

        if (cryptoAuth != null) {
            localVarQueryParams.addAll(apiClient.parameterToPair("cryptoAuth", cryptoAuth));
        }

        localVarQueryParams.addAll(apiClient.parameterToPair("src", Boolean.TRUE));
        localVarQueryParams.addAll(apiClient.parameterToPair("sanitize", Boolean.FALSE));

        java.util.Map<String, String> localVarHeaderParams = new java.util.HashMap<String, String>();
        java.util.Map<String, String> localVarCookieParams = new java.util.HashMap<String, String>();
        java.util.Map<String, Object> localVarFormParams = new java.util.HashMap<String, Object>();
        final String[] localVarAccepts = { "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {

        };
        if (localVarFormParams.isEmpty() == false) {
            final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "oauth", "session" };
        Call call = apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, null, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, null);
        Response response = call.execute();
        JSONObject jsonObject = new JSONObject(response.body().string());
        assertThat(jsonObject.optString("errorDesc"), jsonObject.optString("error"), is(emptyOrNullString()));
        String result = jsonObject.getString("data");
        assertThat(result, is(not(emptyOrNullString())));
        return result;
    }

    private void waitForBackgroundProcess() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
    }

}
