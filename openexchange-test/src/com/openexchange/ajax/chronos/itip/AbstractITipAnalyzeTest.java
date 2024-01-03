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

import static com.openexchange.ajax.chronos.itip.ITipUtil.convertToAttendee;
import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;
import com.openexchange.ajax.chronos.factory.ConferenceBuilder;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Analysis;
import com.openexchange.testing.httpclient.models.Analysis.ActionsEnum;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.EventData;
import org.hamcrest.MatcherAssert;

/**
 * {@link AbstractITipAnalyzeTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.0
 */
public abstract class AbstractITipAnalyzeTest extends AbstractITipTest {

    protected EventData createdEvent;

    protected Attendee prepareCommonAttendees(EventData event) {
        Attendee replyingAttendee = convertToAttendee(testUserC2, Integer.valueOf(0));
        replyingAttendee.setPartStat(PartStat.NEEDS_ACTION.getStatus());

        Attendee organizer = convertToAttendee(testUser, I(testUser.getUserId()));
        organizer.setPartStat(PartStat.ACCEPTED.getStatus());

        ArrayList<Attendee> attendees = new ArrayList<>(5);
        attendees.add(organizer);
        attendees.add(replyingAttendee);

        event.setAttendees(attendees);
        CalendarUser c = new CalendarUser();
        c.cn(userResponseC1.getData().getDisplayName());
        c.email(userResponseC1.getData().getEmail1());
        c.entity(Integer.valueOf(userResponseC1.getData().getId()));
        event.setOrganizer(c);
        event.setCalendarUser(c);

        return replyingAttendee;
    }

    protected EventData prepareAttendeeConference(EventData eventData) {
        ConferenceBuilder builder = ConferenceBuilder.newBuilder() //@formatter:off
            .setDefaultFeatures()
            .setAttendeeLable()
            .setVideoChatUri()
            .setGroupId();
        eventData.addConferencesItem(builder.build()); //@formatter:on
        return eventData;
    }

    protected EventData prepareModeratorConference(EventData eventData) {
        ConferenceBuilder builder = ConferenceBuilder.newBuilder() //@formatter:off
            .setDefaultFeatures()
            .setModeratorLable()
            .setVideoChatUri();
        eventData.addConferencesItem(builder.build()); //@formatter:on
        return eventData;
    }

    /**
     * Get all events of of a series
     * <p>
     * Note: The {@link #createdEvent} must be a series
     *
     * @return All events of the series
     * @throws ApiException
     */
    protected List<EventData> getAllEventsOfCreatedEvent() throws ApiException {
        return getAllEventsOfEvent(eventManager, createdEvent);
    }

    /**
     * Get all events of of a series
     * <p>
     * Note: The {@link #createdEvent} must be a series
     *
     * @return All events of the series
     * @throws ApiException
     */
    protected List<EventData> getAllEventsOfEvent(EventManager eventManager, EventData event) throws ApiException {
        Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        instance.setTimeInMillis(System.currentTimeMillis());
        instance.add(Calendar.DAY_OF_MONTH, -1);
        Date from = instance.getTime();
        instance.add(Calendar.DAY_OF_MONTH, 7);
        Date until = instance.getTime();
        List<EventData> allEvents = eventManager.getAllEvents(null, from, until, true);
        allEvents = getEventsByUid(allEvents, event.getUid()); // Filter by series uid
        return allEvents;
    }

    protected void analyze(String mailId) throws Exception {
        analyze(mailId, ITipActionSet.APPLY.getConsumer());
    }

    protected void analyze(String mailId, ITipActionSet consumer) throws Exception {
        analyze(mailId, consumer.getConsumer());
    }

    protected void analyze(String mailId, Consumer<Analysis> analysisValidator) throws Exception {
        AnalyzeResponse response = analyze(ITipUtil.constructBody(mailId));
        MatcherAssert.assertThat("Found error: " + response.getErrorDesc(), response.getError(), nullValue());
        MatcherAssert.assertThat("Should have no error", response.getErrorId(), nullValue());

        assertAnalyzeActions(response, analysisValidator);
    }

    protected void assertAnalyzeActions(AnalyzeResponse response, ITipActionSet consumer) {
        assertAnalyzeActions(response, consumer.getConsumer());
    }

    protected void assertAnalyzeActions(AnalyzeResponse response, Consumer<Analysis> analysisValidator) {
        List<Analysis> data = response.getData();
        MatcherAssert.assertThat("No Analyze", data, notNullValue());
        MatcherAssert.assertThat("Only one event should have been analyzed", Integer.valueOf(data.size()), is(Integer.valueOf(1)));

        if (null == analysisValidator) {
            ITipActionSet.ACTIONS.getConsumer().accept(data.get(0));
        } else {
            analysisValidator.accept(data.get(0));
        }
    }

    /**
     * Enum containing pre-defined sets of possible actions
     */
    public enum ITipActionSet {

        /** Validates that the response does contain all common actions */
        ALL((Analysis t) -> {
            assertTrue(t.getActions().contains(ActionsEnum.ACCEPT) || t.getActions().contains(ActionsEnum.ACCEPT_AND_IGNORE_CONFLICTS), "Missing action!");
            assertTrue(t.getActions().contains(ActionsEnum.TENTATIVE), "Missing action!");
            assertTrue(t.getActions().contains(ActionsEnum.DECLINE), "Missing action!");
            assertTrue(t.getActions().stream().anyMatch(a -> a.toString().startsWith("apply_")), "Missing action!");
        }),
        /** Validates that the response does only contain actions for the participant status of the user */
        ACTIONS((Analysis t) -> {
            assertTrue(t.getActions().contains(ActionsEnum.ACCEPT) || t.getActions().contains(ActionsEnum.ACCEPT_AND_IGNORE_CONFLICTS), "Missing action!");
            assertTrue(t.getActions().contains(ActionsEnum.TENTATIVE), "Missing action!");
            assertTrue(t.getActions().contains(ActionsEnum.DECLINE), "Missing action!");

            assertFalse(t.getActions().stream().anyMatch(a -> a.toString().startsWith("apply_")), "Unwanted action!");
        }),
        /** Validates that the response does only contain the APPLY action */
        APPLY((Analysis t) -> {
            String actual = String.valueOf(t.getActions());
            assertFalse(t.getActions().contains(ActionsEnum.ACCEPT) || t.getActions().contains(ActionsEnum.ACCEPT_AND_IGNORE_CONFLICTS), "Unwanted action 'accept*'! Actual: " + actual);
            assertFalse(t.getActions().contains(ActionsEnum.TENTATIVE), "Unwanted action 'tentative'! Actual: " + actual);
            assertFalse(t.getActions().contains(ActionsEnum.DECLINE), "Unwanted action 'decline'! Actual: " + actual);
            assertTrue(t.getActions().stream().anyMatch(a -> a.toString().startsWith("apply_")), "Missing action 'apply_*! Actual: " + actual);
        }),
        /** Validates that the response doesn't contain any action */
        EMPTY((Analysis t) -> {
            assertTrue(t.getActions() == null || t.getActions().isEmpty(), "There should be no action, but was " + t.getActions());
        }),
        /** Validates that the response does contain the party crasher action */
        PARTY_CRASHER((Analysis t) -> {
            assertTrue(t.getActions().size() == 2, "There should be two actions! But was " + t.getActions());
            assertTrue(t.getActions().contains(ActionsEnum.ACCEPT_PARTY_CRASHER), "Unwanted action!");
            assertTrue(t.getActions().contains(ActionsEnum.IGNORE), "Unwanted action!");
        }),
        /** Validates that the response does contain the cancel action */
        CANCEL((Analysis t) -> {
            assertTrue(t.getActions().size() >= 1, "There should be at least one action! But was " + t.getActions());
            assertTrue(t.getActions().contains(ActionsEnum.APPLY_REMOVE), "Unwanted action!");
        }),
        /** Validates that the response does contain the cancel action */
        IGNORE((Analysis t) -> {
            assertTrue(t.getActions().size() == 1, "There should be only one action! But was " + t.getActions());
            assertTrue(t.getActions().contains(ActionsEnum.IGNORE), "Unwanted action!");
        }),
        ;

        private final Consumer<Analysis> consumer;

        ITipActionSet(Consumer<Analysis> consumer) {
            this.consumer = consumer;
        }

        /**
         * Gets a consumer that checks of the desired iTIP actions are present or absent within the analysis
         *
         * @return The consumer
         */
        public Consumer<Analysis> getConsumer() {
            return consumer;
        }

    }
}
