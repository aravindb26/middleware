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

package com.openexchange.conference.webhook.impl;

import static com.openexchange.chronos.common.CalendarUtils.optExtendedParameterValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.chronos.Conference;
import com.openexchange.chronos.Event;
import com.openexchange.java.Strings;

/**
 * {@link Utils}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.4
 */
public class Utils {

    /** The name of the type property parameter used in handled conferences */
    public static final String PARAMETER_TYPE = "X-OX-TYPE";

    /** The name of the id property parameter used in handled conferences */
    public static final String PARAMETER_ID = "X-OX-ID";

    /** The name of the owner property parameter used in handled conferences */
    public static final String PARAMETER_OWNER = "X-OX-OWNER";

    /** The conference type for "zoom" conferences */
    public static final String TYPE_ZOOM = "zoom";

    /** The conference type for "jitsi" conferences */
    public static final String TYPE_JITSI = "jitsi";

    /**
     * Gets conferences of an event that are of a certain type, as per parameter {@value #PARAMETER_TYPE}.
     * 
     * @param event The event to get the conferences from
     * @param types The types to consider when collecting the conferences
     * @return The conferences, or an empty list if there are none
     */
    static List<Conference> getConferences(Event event, String... types) {
        List<Conference> conferences = event.getConferences();
        if (null == conferences || conferences.isEmpty() || null == types || 0 == types.length) {
            return Collections.emptyList();
        }
        List<Conference> matchingConferences = new ArrayList<Conference>(conferences.size());
        for (Conference conference : event.getConferences()) {
            String conferenceType = optExtendedParameterValue(conference.getExtendedParameters(), PARAMETER_TYPE);
            if (Strings.isNotEmpty(conferenceType)) {
                for (String possibleType : types) {
                    if (conferenceType.equals(possibleType)) {
                        matchingConferences.add(conference);
                        break;
                    }
                }
            }
        }
        return matchingConferences;
    }

    /**
     * Checks, if the two given conferences match according to their zoom id
     *
     * @param conf1 The first conference
     * @param conf2 The second conference
     * @return true if the zoom ids match, false otherwise
     */
    static boolean matches(Conference conf1, Conference conf2) {
        if (conf1 == null || conf2 == null) {
            return false;
        }

        String param1 = optExtendedParameterValue(conf1.getExtendedParameters(), PARAMETER_ID);
        return null != param1 && param1.equals(optExtendedParameterValue(conf2.getExtendedParameters(), PARAMETER_ID));
    }

}
