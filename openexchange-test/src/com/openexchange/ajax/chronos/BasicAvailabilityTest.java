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

package com.openexchange.ajax.chronos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.testing.httpclient.models.AvailabilityData;
import com.openexchange.testing.httpclient.models.Available;
import com.openexchange.testing.httpclient.models.GetAvailabilityResponse;
import com.openexchange.testing.httpclient.modules.ChronosApi;

/**
 * {@link BasicAvailabilityTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
@Disabled // disabled until availability is activated in code again
public class BasicAvailabilityTest extends AbstractChronosTest {

    @SuppressWarnings("hiding")
    private ChronosApi chronosApi;

    /**
     * Tests setting a single available block
     */
    @Test
    public void testSetSingleAvailableBlock() throws Exception {
        AvailabilityData availabilityData = new AvailabilityData();
        Available expected = createAvailable(System.currentTimeMillis(), System.currentTimeMillis() + 7200);
        availabilityData.addAvailableTimesItem(expected);

        chronosApi = new ChronosApi(defaultUserApi.getClient());

        // Set the availability
        chronosApi.setAvailability(availabilityData);

        // Get the availability
        GetAvailabilityResponse availability = chronosApi.getAvailability();
        List<Available> availableBlocks = availability.getData();

        // Assert
        assertNotNull(availableBlocks, "The response payload is null");
        assertFalse(availableBlocks.isEmpty(), "The availables array is empty");
        assertTrue(availableBlocks.size() == 1, "The are more than one available blocks");
        assertAvailable(expected, availableBlocks.get(0));
    }

    /**
     * Tests setting a single available block with recurrence rule
     */
    @Test
    public void testSetSingleAvailableBlockWithRecurrenceRule() throws Exception {
        AvailabilityData availabilityData = new AvailabilityData();
        Available expected = createAvailable(System.currentTimeMillis(), System.currentTimeMillis() + 7200, "FREQ=WEEKLY;BYDAY=TH");
        availabilityData.addAvailableTimesItem(expected);

        chronosApi = new ChronosApi(defaultUserApi.getClient());

        // Set the availability
        chronosApi.setAvailability(availabilityData);

        // Get the availability
        GetAvailabilityResponse availability = chronosApi.getAvailability();
        List<Available> availableBlocks = availability.getData();

        // Assert
        assertNotNull(availableBlocks, "The response payload is null");
        assertFalse(availableBlocks.isEmpty(), "The availables array is empty");
        assertTrue(availableBlocks.size() == 1, "The are more than one available blocks");
        assertAvailable(expected, availableBlocks.get(0));
    }

    /**
     * Tests setting multiple available blocks with recurrence rule
     */
    @Test
    public void testSetMultipleAvailableBlocks() throws Exception {
        AvailabilityData availabilityData = new AvailabilityData();
        availabilityData.addAvailableTimesItem(createAvailable(System.currentTimeMillis() + 36000, System.currentTimeMillis() + 36000 + 7200, "FREQ=WEEKLY;BYDAY=MO"));
        availabilityData.addAvailableTimesItem(createAvailable(System.currentTimeMillis(), System.currentTimeMillis() + 7200, "FREQ=WEEKLY;BYDAY=TH"));

        chronosApi = new ChronosApi(defaultUserApi.getClient());

        // Set the availability
        chronosApi.setAvailability(availabilityData);

        // Get the availability
        GetAvailabilityResponse availability = chronosApi.getAvailability();
        List<Available> availableBlocks = availability.getData();

        // Assert
        assertNotNull(availableBlocks, "The response payload is null");
        assertFalse(availableBlocks.isEmpty(), "The availables array is empty");
        assertAvailable(availabilityData.getAvailableTimes(), availableBlocks);
    }

    /**
     * Creates an {@link Available} block within the specified range
     *
     * @param from The starting point in time
     * @param until The ending point in time
     * @return The {@link Available} block
     */
    private Available createAvailable(long from, long until) {
        Available available = new Available();
        available.setStart(DateTimeUtil.getDateTime(from));
        available.setEnd(DateTimeUtil.getDateTime(until));
        available.setUser(defaultUserApi.getCalUser());
        return available;
    }

    /**
     * Creates an {@link Available} block within the specified range and the specified
     * recurrence rule
     *
     * @param from The starting point in time
     * @param until The ending point in time
     * @param rrule The recurrence rule
     * @return The {@link Available} block
     * @return The {@link Available} block
     */
    private Available createAvailable(long from, long until, String rrule) {
        Available available = createAvailable(from, until);
        available.setRrule(rrule);
        return available;
    }

    /**
     * Asserts that the specified expected {@link List} of {@link Available} blocks
     * is equal to the specified actual {@link List} of {@link Available} blocks
     *
     * @param expected The expected {@link List}
     * @param actual The actual {@link List}
     */
    private void assertAvailable(List<Available> expected, List<Available> actual) {
        assertEquals(expected.size(), actual.size(), "The amount of available blocks does not match");
        for (int index = 0; index < expected.size(); index++) {
            assertAvailable(expected.get(index), actual.get(index));

        }
    }

    /**
     * Asserts that the expected {@link Available} is equal the actual {@link Available}
     *
     * @param expected The expected {@link Available}
     * @param actual The actual {@link Available}
     */
    private void assertAvailable(Available expected, Available actual) {
        assertEquals(expected.getRrule(), actual.getRrule(), "The recurrence rule does not match");
        assertEquals(expected.getStart(), actual.getStart(), "The start time does not match");
        assertEquals(expected.getEnd(), actual.getEnd(), "The start time does not match");
        assertEquals(expected.getUser(), actual.getUser(), "The user id does not match");
    }
}
