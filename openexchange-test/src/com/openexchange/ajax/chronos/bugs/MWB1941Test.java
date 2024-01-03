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

package com.openexchange.ajax.chronos.bugs;

import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.UserData;

/**
 * {@link MWB1941Test}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.8
 */
public class MWB1941Test extends AbstractChronosTest {

    @Test
    public void testDeleteUser() throws Exception {
        /*
         * Create event with users
         */
        EventData eventData = EventFactory.createSingleTwoHourEvent(getUserId(), "MWB-1941");
        eventData.addAttendeesItem(AttendeeFactory.createIndividual(testUser2.getUserId()));
        eventData.addAttendeesItem(AttendeeFactory.createIndividual(testContext.acquireUser().getUserId()));

        EventData createdEvent = eventManager.createEvent(eventData);
        assertThat(I(createdEvent.getAttendees().size()), is(I(3)));

        /*
         * Remove calendar access from admin
         */
        ConfigAwareProvisioningService.getService().changeModuleAccess(admin, //@formatter:off
            UserModuleAccess.Builder
            .newInstance()
            .calendar(Boolean.FALSE)
            .build(),
            this.getClass().getSimpleName());  //@formatter:on

        /*
         * Now delete user that created the event and automatically reassign to admin without access
         */
        ConfigAwareProvisioningService.getService().deleteUser(testUser.getContextId(), testUser.getUserId(), "MWB-1941");

        /*
         * Check that event is still accessible and context admin is part of the event
         */
        EventManager eventManager2 = getEventManager2(testUser2);
        EventData event = eventManager2.getEvent(null, createdEvent.getId());
        assertThat(createdEvent.getUid(), is(event.getUid()));
        assertThat(I(event.getAttendees().size()), is(I(3)));
        if (null != event.getOrganizer().getEntity() && 0 < event.getOrganizer().getEntity().intValue()) {
            assertTrue(I(admin.getUserId()).equals(event.getOrganizer().getEntity()));
        } else {
            UserData adminUserData = new com.openexchange.testing.httpclient.modules.UserApi(testUser2.getApiClient()).getUser(String.valueOf(admin.getUserId())).getData();
            assertEquals(adminUserData.getEmail1(), CalendarUtils.extractEMailAddress(event.getOrganizer().getUri()));
        }
    }

    private EventManager getEventManager2(TestUser user) throws ApiException, Exception {
        SessionAwareClient client = user.getApiClient();
        UserApi userApi = new UserApi(client, user);
        String defaultFolderId2 = getDefaultFolder(userApi.getFoldersApi());
        return new EventManager(userApi, defaultFolderId2);
    }

}
