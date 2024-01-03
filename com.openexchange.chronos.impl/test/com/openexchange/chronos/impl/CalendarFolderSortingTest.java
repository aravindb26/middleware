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

package com.openexchange.chronos.impl;

import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.session.SimSession;

/**
 * {@link CalendarFolderSortingTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.3
 */
public class CalendarFolderSortingTest {

    /** Permission bits of the <b>administrator</b> */
    private final static int ADMIN = 272662788;
    /** Permission bits of an <b>author</b> */
    private final static int AUTHOR = 4227332;
    /** Permission bits of an <b>viewer</b> */
    private final static int VIEWER = 257;

    /**
     * Sort folder from type
     * <li> public -> private -> shared </li>
     * and within types from
     * <li> Admin -> write -> read permissions </li>
     * 
     * @throws Exception in case of error
     */
    @Test
    public void calendarFolderSotingTest() throws Exception {
        List<CalendarFolder> folders = new ArrayList<>(9);
        SimSession session = new SimSession(3, 1);
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder9", 9, FolderObject.CALENDAR, FolderObject.SHARED, 4),
                VIEWER));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder", 4, FolderObject.CALENDAR, FolderObject.PRIVATE, 3),
                ADMIN));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder", 8, FolderObject.CALENDAR, FolderObject.SHARED, 4),
                AUTHOR));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder", 1, FolderObject.CALENDAR, FolderObject.PUBLIC, 3),
                ADMIN));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder", 3, FolderObject.CALENDAR, FolderObject.PUBLIC, 3),
                VIEWER));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder", 7, FolderObject.CALENDAR, FolderObject.SHARED, 4),
                ADMIN));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder", 2, FolderObject.CALENDAR, FolderObject.PUBLIC, 3),
                AUTHOR));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder", 5, FolderObject.CALENDAR, FolderObject.PRIVATE, 3),
                AUTHOR));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder", 6, FolderObject.CALENDAR, FolderObject.PRIVATE, 3),
                VIEWER));//@formatter:on
        folders.add(//@formatter:off
            new CalendarFolder(
                session,
                new FolderObject("TestFolder10", 10, FolderObject.CALENDAR, FolderObject.SHARED, 4),
                VIEWER));//@formatter:on
        /*
         * Sort and check order of clearly ordered
         */
        folders.sort(Comparators.CALENDAR_FOLDER_COMPARATOR);
        int i = 0;
        for (; i < folders.size() - 2; i++) {
            CalendarFolder calendarFolder = folders.get(i);
            assertThat(calendarFolder.getId(), is(I(i + 1).toString()));
        }
        /*
         * Folder 9 and 10 are equal in terms of type and permission, check that one of those comes next
         */
        CalendarFolder calendarFolder = folders.get(++i);
        assertThat(calendarFolder.getId(), is(oneOf(I(9).toString(), I(10).toString())));
    }
}
