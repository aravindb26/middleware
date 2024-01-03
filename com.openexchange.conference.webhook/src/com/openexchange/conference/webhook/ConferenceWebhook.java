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

package com.openexchange.conference.webhook;

import static com.openexchange.chronos.common.CalendarUtils.optExtendedParameterValue;
import static com.openexchange.conference.webhook.impl.Utils.PARAMETER_ID;
import static com.openexchange.conference.webhook.impl.Utils.PARAMETER_OWNER;
import static com.openexchange.conference.webhook.impl.Utils.PARAMETER_TYPE;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Strings.getLineSeparator;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.json.JSONException;
import org.json.JSONInputStream;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Conference;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.DataHandlers;
import com.openexchange.conference.webhook.exception.ConferenceWebhookExceptionCodes;
import com.openexchange.conference.webhook.impl.AbstractConferenceInterceptor;
import com.openexchange.conversion.ConversionResult;
import com.openexchange.conversion.ConversionService;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataHandler;
import com.openexchange.conversion.SimpleData;
import com.openexchange.exception.OXException;
import com.openexchange.java.Autoboxing;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;

/**
 * {@link ConferenceWebhook}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.4
 */
public class ConferenceWebhook {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenceWebhook.class);

    private static final String UPDATED = "conference.updated";
    private static final String DELETED = "conference.deleted";

    protected final ServiceLookup services;
    private final ConferenceWebhookConfiguration config;

    /**
     * Initializes a new {@link AbstractConferenceInterceptor}.
     *
     * @param services A service lookup reference
     * @param config The configuration to use
     */
    public ConferenceWebhook(ServiceLookup services, ConferenceWebhookConfiguration config) {
        super();
        this.config = config;
        this.services = services;
    }

    /**
     * Sends a update notification.
     *
     * @param conference The updated conference
     * @param timestamp The timestamp
     */
    public void update(Conference conference, Event update, long timestamp) {
        try {
            post(serialize(UPDATED, conference, update, timestamp));
            LOG.info("Successfully sent {} for event {} and conference {} to webhook target.", UPDATED, update.getId(), I(conference.getId()));
        } catch (JSONException | OXException e) {
            LOG.error("Unable to send conference update to the webhook target.", e);
        }
    }

    /**
     * Sends a delete notification.
     *
     * @param conference The deleted conference
     * @param original The original deleted event
     * @param timestamp The timestamp
     */
    public void delete(Conference conference, Event original, long timestamp) {
        try {
            post(serialize(DELETED, conference, original, timestamp));
            LOG.info("Successfully sent {} for conference {} to webhook target.", DELETED, I(conference.getId()));
        } catch (JSONException | OXException e) {
            LOG.error("Unable to send conference delete to the webhook target.", e);
        }
    }

    private static final String AUTHORIZATION_HEADER = "authorization";

    private void post(JSONObject json) throws OXException {
        try {
            // @formatter:off
            Failsafe.with(
                new RetryPolicy()
                    .withMaxRetries(5)
                    .withBackoff(1000, 10000, TimeUnit.MILLISECONDS)
                    .withJitter(0.25F)
                    .retryOn(f -> f instanceof OXException oxe && ConferenceWebhookExceptionCodes.WEBHOOK_TARGET_SERVER_ERROR.equals(oxe)))
                .onRetry(f -> LOG.info("Error posting event to webhook target API, trying again.", f))
                .run(() -> doPost(json));
            // @formatter:on
        } catch (FailsafeException e) {
            if (e.getCause() instanceof OXException oxe) {
                throw oxe;
            }
            throw e;
        }
    }

    private void doPost(JSONObject json) throws OXException {
        HttpClient client = services.getServiceSafe(HttpClientService.class).getHttpClient("zoom_conference");
        HttpPost post = null;
        HttpResponse response = null;
        try {
            post = new HttpPost(config.getUri());
            post.setEntity(new InputStreamEntity(new JSONInputStream(json, com.openexchange.java.Charsets.UTF_8_NAME), json.toString().length(), ContentType.APPLICATION_JSON));
            post.addHeader(AUTHORIZATION_HEADER, config.getWebhookSecret());

            long start = System.nanoTime();
            LOG.trace(">> POST {}{}   {}", post.getURI(), getLineSeparator(), json);
            response = client.execute(post);
            StatusLine statusLine = response.getStatusLine();
            LOG.trace("<< {}, {} ms elapsed.", statusLine, Autoboxing.L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
            int statusCode = statusLine.getStatusCode();
            if (statusCode < 200 || statusCode > 299) {
                if (500 <= statusCode && statusCode < 600) {
                    throw ConferenceWebhookExceptionCodes.WEBHOOK_TARGET_SERVER_ERROR.create(statusLine.getReasonPhrase());
                }
                throw ConferenceWebhookExceptionCodes.WEBHOOK_TARGET_ERROR.create(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            throw ConferenceWebhookExceptionCodes.IO_ERROR.create(e);
        } finally {
            HttpClients.close(post, response);
        }
    }

    private JSONObject serialize(String action, Conference conference, Event event, long timestamp) throws OXException, JSONException {
        JSONObject payload = new JSONObject();
        payload.putSafe("meetingId", optExtendedParameterValue(conference.getExtendedParameters(), PARAMETER_ID));
        payload.putSafe("owner", optExtendedParameterValue(conference.getExtendedParameters(), PARAMETER_OWNER));
        payload.putSafe("type", optExtendedParameterValue(conference.getExtendedParameters(), PARAMETER_TYPE));
        if (UPDATED.equals(action)) {
            JSONObject eventJson = convertEvent(event);
            payload.putSafe("startDate", eventJson.getJSONObject("startDate"));
            payload.putSafe("endDate", eventJson.getJSONObject("endDate"));
            if (eventJson.hasAndNotNull("summary")) {
                payload.putSafe("summary", eventJson.get("summary"));
            }
            if (eventJson.hasAndNotNull("description")) {
                payload.putSafe("description", eventJson.get("description"));
            }
            payload.putSafe("appointment", eventJson);
        } else if (DELETED.equals(action)) {
            if (event != null) {
                JSONObject eventJson = convertEvent(event);
                payload.putSafe("appointment", eventJson);
            }
        }
        return new JSONObject(2).putSafe("event", action).putSafe("timestamp", L(timestamp)).putSafe("payload", payload);
    }

    private JSONObject convertEvent(Event event) throws OXException {
        ConversionService conversionService = services.getServiceSafe(ConversionService.class);
        DataHandler handler = conversionService.getDataHandler(DataHandlers.EVENT2JSON);
        if (null == handler) {
            throw ServiceExceptionCode.absentService(DataHandler.class);
        }
        ConversionResult result = handler.processData(new SimpleData<Event>(event, null), new DataArguments(), null);
        return (JSONObject) result.getData();
    }

}
