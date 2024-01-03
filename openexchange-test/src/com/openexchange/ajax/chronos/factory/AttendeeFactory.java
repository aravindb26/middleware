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

package com.openexchange.ajax.chronos.factory;

import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.CalendarUser;

/**
 * {@link AttendeeFactory}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class AttendeeFactory {

    /**
     * Creates a new {@link Attendee} object with the specified user identifier, email address and {@link CuTypeEnum}
     *
     * @param userId The user identifier
     * @param cuType the {@link CuTypeEnum}
     * @return The new {@link Attendee}
     */
    public static Attendee createAttendee(int userId, CuTypeEnum cuType) {
        return createAttendee(I(userId), cuType);
    }

    /**
     * Creates a new {@link Attendee} object with the specified user identifier, email address and {@link CuTypeEnum}
     *
     * @param userId The user identifier
     * @param cuType the {@link CuTypeEnum}
     * @return The new {@link Attendee}
     */
    public static Attendee createAttendee(Integer userId, CuTypeEnum cuType) {
        assertThat(userId, is(notNullValue()));
        Attendee attendee = new Attendee();
        attendee.entity(userId);
        attendee.cuType(cuType);
        attendee.setMember(null); //set member explicitly to null
        return attendee;
    }

    /**
     * Create internal {@link Attendee} of type {@link CuTypeEnum#INDIVIDUAL}
     *
     * @param user The user
     * @return The {@link Attendee}
     */
    public static Attendee createIndividual(TestUser user) {
        return createAttendee(I(user.getUserId()), CuTypeEnum.INDIVIDUAL);
    }

    /**
     * Create internal {@link Attendee}s of type {@link CuTypeEnum#INDIVIDUAL}
     *
     * @param users The users
     * @return The {@link Attendee}s
     */
    public static List<Attendee> createIndividuals(TestUser... users) {
        if (null == users || users.length == 0) {
            return Collections.emptyList();
        }
        ArrayList<Attendee> attendees = new ArrayList<>(users.length);
        for (TestUser user : users) {
            attendees.add(createAttendee(I(user.getUserId()), CuTypeEnum.INDIVIDUAL));
        }
        return attendees;
    }

    /**
     * Creates an internal {@link Attendee} of type {@link CuTypeEnum#INDIVIDUAL}
     *
     * @param userIds The user identifiers
     * @return The {@link Attendee}s
     */
    public static List<Attendee> createIndividuals(Integer... userIds) {
        if (null == userIds || userIds.length == 0) {
            return Collections.emptyList();
        }
        ArrayList<Attendee> attendees = new ArrayList<>(userIds.length);
        for (Integer userId : userIds) {
            attendees.add(createAttendee(userId, CuTypeEnum.INDIVIDUAL));
        }
        return attendees;
    }

    /**
     * Creates an internal {@link Attendee} of type {@link CuTypeEnum#INDIVIDUAL}
     *
     * @param userId The user identifier
     * @return The new {@link Attendee}
     */
    public static Attendee createIndividual(int userId) {
        return createAttendee(userId, CuTypeEnum.INDIVIDUAL);
    }

    /**
     * Creates an internal {@link Attendee} of type {@link CuTypeEnum#INDIVIDUAL}
     *
     * @param userId The user identifier
     * @return The new {@link Attendee}
     */
    public static Attendee createIndividual(Integer userId) {
        return createAttendee(userId, CuTypeEnum.INDIVIDUAL);
    }

    /**
     * Create external {@link Attendee}s of type {@link CuTypeEnum#INDIVIDUAL}
     *
     * @param users The users
     * @return The {@link Attendee}s
     */
    public static List<Attendee> createAsExternals(TestUser... users) {
        if (null == users || users.length == 0) {
            return Collections.emptyList();
        }
        ArrayList<Attendee> attendees = new ArrayList<>(users.length);
        for (TestUser user : users) {
            attendees.add(createIndividual(user.getLogin()));
        }
        return attendees;
    }

    /**
     * Create external {@link Attendee}s of type {@link CuTypeEnum#INDIVIDUAL}
     *
     * @param user The user
     * @return The {@link Attendee}
     */
    public static Attendee createAsExternal(TestUser user) {
        assertThat(user, is(notNullValue()));
        assertThat(user.getLogin(), is(not(emptyOrNullString())));
        Attendee attendee = new Attendee();
        attendee.cuType(CuTypeEnum.INDIVIDUAL);
        attendee.setUri("mailto:" + user.getLogin());
        attendee.cn(user.getUser());
        attendee.email(user.getLogin());
        return createIndividual(user.getLogin());
    }

    /**
     * Creates an external {@link Attendee} of type {@link CuTypeEnum#INDIVIDUAL}
     *
     * @param emailAddress The e-mail address
     * @return The new {@link Attendee}
     */
    public static Attendee createIndividual(String emailAddress) {
        assertThat(emailAddress, is(not(emptyOrNullString())));
        Attendee attendee = new Attendee();
        attendee.cuType(CuTypeEnum.INDIVIDUAL);
        attendee.setUri("mailto:" + emailAddress);
        return attendee;
    }

    /**
     * Converts an {@link TestUser} to an organizer.
     * 
     * @param user The user to convert
     * @return THe organizer as {@link CalendarUser} object
     */
    public static CalendarUser createOrganizerFrom(TestUser user) {
        assertThat(user, is(notNullValue()));
        CalendarUser c = new CalendarUser();
        c.cn(user.getUser());
        c.email(user.getLogin());
        c.entity(I(user.getUserId()));
        c.uri("mailto:" + user.getLogin());
        return c;
    }

    /**
     * Converts an {@link Attendee} to an organizer.
     * 
     * @param attendee The attendee to convert
     * @return THe organizer as {@link CalendarUser} object
     */
    public static CalendarUser createOrganizerFrom(Attendee attendee) {
        assertThat(attendee, is(notNullValue()));
        CalendarUser c = new CalendarUser();
        c.cn(attendee.getCn());
        c.email(attendee.getEmail());
        c.entity(attendee.getEntity());
        c.uri(attendee.getUri());
        return c;
    }

}
