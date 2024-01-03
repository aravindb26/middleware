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

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.hamcrest.MatcherAssert;
import com.openexchange.testing.httpclient.models.Attendee;

/**
 * {@link PartStat} - Participant status
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public enum PartStat {

    /** ACCEPTED */
    ACCEPTED("ACCEPTED"),

    /** TENTATIVE */
    TENTATIVE("TENTATIVE"),

    /** DECLINED */
    DECLINED("DECLINED"),

    /** NEEDS_ACTION */
    NEEDS_ACTION("NEEDS-ACTION");

    protected final String status;

    private PartStat(String status) {
        this.status = status;
    }

    /**
     * Gets the status
     *
     * @return The status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Asset that the participant status represented by this object matches the given input
     *
     * @param attendee The attendee to check
     * @return The attendee to check
     */
    public Attendee assertStatus(Attendee attendee) {
        MatcherAssert.assertThat("Attendee not available", attendee, is(notNullValue()));
        assertStatus(attendee.getPartStat());
        return attendee;
    }

    /**
     * Asset that the participant status represented by this object matches the given input
     *
     * @param partStat The participant status to check
     */
    public void assertStatus(String partStat) {
        MatcherAssert.assertThat("Participant status is not set.", partStat, is(notNullValue()));
        MatcherAssert.assertThat("Participant status is not correct.", partStat, equalToIgnoringCase(getStatus()));
    }

}
