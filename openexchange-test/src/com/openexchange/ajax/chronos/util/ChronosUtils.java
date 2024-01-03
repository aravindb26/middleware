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

import java.util.List;
import com.openexchange.testing.httpclient.models.Attendee;

/**
 * {@link ChronosUtils}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class ChronosUtils {

    /**
     * Try to find an attendee with the given ID
     *
     * @param attendees The list of attendees to search in
     * @param entity The entity ID of the attendee to find
     * @return The {@link Attendee} or <code>null</code>
     */
    public static Attendee find(List<Attendee> attendees, int entity) {
        return find(attendees, Integer.valueOf(entity));
    }

    /**
     * Try to find an attendee with the given ID
     *
     * @param attendees The list of attendees to search in
     * @param entity The entity ID of the attendee to find
     * @return The {@link Attendee} or <code>null</code>
     */
    public static Attendee find(List<Attendee> attendees, Integer entity) {
        if (null != attendees) {
            for (Attendee a : attendees) {
                if (null != a.getEntity() && a.getEntity().equals(entity)) {
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * Try to find an attendee with the given email
     *
     * @param attendees The list of attendees to search in
     * @param email The email of the attendee to find
     * @return The {@link Attendee} or <code>null</code>
     */
    public static Attendee find(List<Attendee> attendees, String email) {
        if (null != attendees) {
            for (Attendee attendee : attendees) {
                String uri = attendee.getUri();
                if (null != uri && uri.toLowerCase().contains(email.toLowerCase())) {
                    return attendee;
                }
            }
        }
        return null;
    }

}
