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

package com.openexchange.ajax.chronos.util;

import static com.openexchange.chronos.provider.composition.IDMangling.getRelativeFolderId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * {@link CalendarAssertUtil}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class CalendarAssertUtil {

    /**
     * Asserts that the expected {@link EventData} is equal the actual {@link EventData}
     *
     * @param expected The expected {@link EventData}
     * @param actual The actual {@link EventData}
     */
    public static void assertEventsEqual(EventData expected, EventData actual) {
        assertEquals(expected.getStartDate(), actual.getStartDate(), "The start date does not match");
        assertEquals(expected.getEndDate(), actual.getEndDate(), "The end date does not match");
        assertEquals(expected.getCreated(), actual.getCreated(), "The created date does not match");
        assertEquals(expected.getUid(), actual.getUid(), "The uid does not match");
        assertEquals(expected.getDescription(), actual.getDescription(), "The description does not match");
        assertEquals(expected.getLastModified(), actual.getLastModified(), "The last modified date does not match");
        assertEquals(expected.getSummary(), actual.getSummary(), "The summary does not match");
        assertEquals(expected.getSequence(), actual.getSequence(), "The sequence number does not match");
        assertEquals(expected.getId(), actual.getId(), "The identifier does not match");
        assertEquals(expected.getPropertyClass(), actual.getPropertyClass(), "The property class does not match");
        assertEquals(expected.getTransp(), actual.getTransp(), "The transparency does not match");
        assertEquals(expected.getColor(), actual.getColor(), "The color does not match");
        assertEquals(expected.getFolder(), actual.getFolder(), "The folder does not match");
        assertEquals(expected.getCreatedBy(), actual.getCreatedBy(), "The created by identifier does not match");
        assertEquals(expected.getRecurrenceId(), actual.getRecurrenceId(), "The recurrence identifier does not match");
        assertEquals(expected.getCalendarUser(), actual.getCalendarUser(), "The calendar user identifier does not match");
        assertEquals(expected.getRrule(), actual.getRrule(), "The recurrence rule does not match");
        assertEquals(expected.getUrl(), actual.getUrl(), "The url does not match");
        assertThat("The attendees list does not match", actual.getAttendees(), is(expected.getAttendees()));
        assertThat("The attachments list does not match", actual.getAttachments(), is(expected.getAttachments()));
        assertThat("The alarms list does not match", actual.getAlarms(), is(expected.getAlarms()));
        assertThat("The organizer does not match", actual.getOrganizer(), is(expected.getOrganizer()));
        assertThat("The delete exception dates do not match", actual.getDeleteExceptionDates(), is(expected.getDeleteExceptionDates()));
        assertThat("The extended properties dates do not match", actual.getExtendedProperties(), is(expected.getExtendedProperties()));
        assertThat("The geo data does not match", actual.getGeo(), is(expected.getGeo()));
    }

    /**
     * Asserts that the expected {@link EventData} is not equal the actual {@link EventData}
     *
     * @param expected The expected {@link EventData}
     * @param actual The actual {@link EventData}
     */
    public static void assertEventsNotEqual(EventData expected, EventData actual) {
        assertNotEquals(expected.getStartDate(), actual.getStartDate(), "The start date does match");
        assertNotEquals(expected.getEndDate(), actual.getEndDate(), "The end date does match");
        assertNotEquals(expected.getCreated(), actual.getCreated(), "The created date does match");
        assertNotEquals(expected.getUid(), actual.getUid(), "The uid does match");
        assertNotEquals(expected.getDescription(), actual.getDescription(), "The description does match");
        assertNotEquals(expected.getLastModified(), actual.getLastModified(), "The last modified date does match");
        assertNotEquals(expected.getSummary(), actual.getSummary(), "The summary does match");
        assertNotEquals(expected.getSequence(), actual.getSequence(), "The sequence number does match");
        assertNotEquals(expected.getId(), actual.getId(), "The identifier does match");
        assertNotEquals(expected.getPropertyClass(), actual.getPropertyClass(), "The property class does match");
        assertNotEquals(expected.getTransp(), actual.getTransp(), "The transparency does match");
        assertNotEquals(expected.getColor(), actual.getColor(), "The color does match");
        assertNotEquals(expected.getFolder(), actual.getFolder(), "The folder does match");
        assertNotEquals(expected.getCreatedBy(), actual.getCreatedBy(), "The created by identifier does match");
        assertNotEquals(expected.getRecurrenceId(), actual.getRecurrenceId(), "The recurrence identifier does match");
        assertNotEquals(expected.getCalendarUser(), actual.getCalendarUser(), "The calendar user identifier does match");
        assertNotEquals(expected.getRrule(), actual.getRrule(), "The recurrence rule does match");
        assertNotEquals(expected.getUrl(), actual.getUrl(), "The url does match");
        assertThat("The attendees list does match", actual.getAttendees(), is(not(expected.getAttendees())));
        assertThat("The attachments list does match", actual.getAttachments(), is(not(expected.getAttachments())));
        assertThat("The alarms list does match", actual.getAlarms(), is(not(expected.getAlarms())));
        assertThat("The organizer does match", actual.getOrganizer(), is(not(expected.getOrganizer())));
        assertThat("The delete exception dates do match", actual.getDeleteExceptionDates(), is(not(expected.getDeleteExceptionDates())));
        assertThat("The extended properties dates do match", actual.getExtendedProperties(), is(not(expected.getExtendedProperties())));
        assertThat("The geo data does match", actual.getGeo(), is(not(expected.getGeo())));
    }

    /** The calendar provider folder prefix */
    public final static String CAL_FOLDER_PREFIX = "cal://";

    /**
     * Asserts that two folder identifier are the same.
     * <p>
     * Will <b>not</b> throw an {@link AssertionError} in case one folder ID starts
     * with the {@value #CAL_FOLDER_PREFIX} prefix
     *
     * @param folderId1 The one folder identifier
     * @param folderId2 The other folder identifier
     * @return <code>true</code> if both folder IDs matches, regardless of mangleing, <code>false</code> otherwise
     */
    public static boolean assertFolderIDEqual(String folderId1, String folderId2) {
        if (Strings.isEmpty(folderId1)) {
            return Strings.isEmpty(folderId2);
        }
        try {
            String id1 = folderId1.startsWith(CAL_FOLDER_PREFIX) ? getRelativeFolderId(folderId1) : folderId1;
            String id2 = folderId2.startsWith(CAL_FOLDER_PREFIX) ? getRelativeFolderId(folderId2) : folderId2;
            return id1.equals(id2);
        } catch (OXException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get a matcher for calendar folder matching
     *
     * @param expectedFolderId The expected calendar folder identifier
     * @return The matcher
     */
    public static Matcher<String> isCalendarFolder(String expectedFolderId) {
        return new CalendarFolderMatcher(expectedFolderId);
    }

    private static class CalendarFolderMatcher extends BaseMatcher<String> {

        private final String expectedFolderId;

        /**
         * Initializes a new {@link CalendarAssertUtil.CalendarFolderMatcher}.
         * 
         * @param expectedFolderId The expected folder identifier
         */
        public CalendarFolderMatcher(String expectedFolderId) {
            super();
            this.expectedFolderId = expectedFolderId;
        }

        @Override
        public boolean matches(Object item) {
            if (item instanceof String actualFolderId) {
                return assertFolderIDEqual(expectedFolderId, actualFolderId);
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expectedFolderId);
        }

    }
}
