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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.itip.ITipAssertion;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.testing.httpclient.models.AnalysisChange;
import com.openexchange.testing.httpclient.models.AnalysisChange.TypeEnum;
import com.openexchange.testing.httpclient.models.AnalysisChangeCurrentEvent;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactUpdateResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.modules.ContactsApi;

/**
 * 
 * {@link AutoProcessKnownTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class AutoProcessKnownTest extends AbstractKnownProcessingTest {

    @Test
    public void testAutoProcessForKnown() throws Exception {
        /*
         * Add organizer to attendees address book
         */
        ContactsApi contactsApi = new ContactsApi(apiClientC2);
        ContactData cd = new ContactData();
        cd.setFirstName(testUser.getUser());
        cd.setLastName(testUser.getContext());
        cd.setEmail1(testUser.getLogin());
        cd.setFolderId(getDefaultContactFolder());
        cd.setMarkAsDistributionlist(Boolean.FALSE);
        ContactUpdateResponse contactUpdateResponse = contactsApi.createContact(cd);
        assertNull(contactUpdateResponse.getErrorDesc(), contactUpdateResponse.getError());
        /*
         * Create event
         */
        String summary = "testAutoProcessForKnown" + UUID.randomUUID().toString();
        EventData eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);
        Attendee replyingAttendee = prepareCommonAttendees(eventData);
        createdEvent = eventManager.createEvent(eventData);
        /*
         * Check that mail is within attendees INBOX, should already be processed
         */
        MailData iMip = receiveIMip(apiClientC2, testUser.getLogin(), summary, 0, SchedulingMethod.REQUEST);
        AnalyzeResponse response = analyze(apiClientC2, iMip);
        assertAnalyzeActions(response, ITipActionSet.ACTIONS);
        AnalysisChange change = assertSingleChange(response);
        /*
         * Check that participant status isn't set
         */
        AnalysisChangeCurrentEvent currentEvent = change.getCurrentEvent();
        assertThat("No event found", currentEvent, notNullValue());
        assertAttendeePartStat(currentEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION.getStatus());
        assertThat("Event ID not set", currentEvent.getId(), notNullValue());
        assertThat(change.getType(), is(TypeEnum.CREATE));
    }

    @Test
    public void testAutoProcessForUnknown() throws Exception {
        /*
         * Create event, organizer isn't known to attendee
         */
        String summary = "testAutoProcessForUnknown" + UUID.randomUUID().toString();
        EventData eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);
        prepareCommonAttendees(eventData);
        createdEvent = eventManager.createEvent(eventData);
        /*
         * Check that mail is within attendees INBOX, shouldn't be processed
         */
        MailData iMip = receiveIMip(apiClientC2, testUser.getLogin(), summary, 0, SchedulingMethod.REQUEST);
        AnalyzeResponse response = analyze(apiClientC2, iMip);
        assertAnalyzeActions(response, ITipActionSet.ALL);
        AnalysisChange change = ITipAssertion.assertChanges(response, 1, 0);
        /*
         * Check that the event wasn't created
         */
        AnalysisChangeCurrentEvent currentEvent = change.getCurrentEvent();
        assertThat("Event shouldn't be created", currentEvent, nullValue());
        assertThat(change.getType(), is(TypeEnum.CREATE));
    }
}
