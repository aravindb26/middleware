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

package com.openexchange.chronos.scheduling;

import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;

/**
 * {@link ITipSequence}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ITipSequence implements Comparable<ITipSequence> {

    /**
     * Initializes a new {@link ITipSequence} using the values of {@link EventField#SEQUENCE} and {@link EventField#DTSTAMP} from the
     * passed event. The timestamp will be used in <i>seconds</i> precision only, due to its representation in iCalendar format.
     * 
     * @param event The event to initialize the ITip sequence from
     * @return The ITip sequence
     */
    public static ITipSequence of(Event event) {
        return new ITipSequence(event.getSequence(), event.getDtStamp());
    }

    /**
     * Initializes a new {@link ITipSequence} using the values of {@link AttendeeField#SEQUENCE} and {@link AttendeeField#TIMESTAMP} from
     * the passed attendee. The timestamp will be used in <i>seconds</i> precision only, due to its representation in iCalendar format.
     * 
     * @param attendee The attendee to initialize the ITip sequence from
     * @return The ITip sequence
     */
    public static ITipSequence of(Attendee attendee) {
        return new ITipSequence(attendee.getSequence(), attendee.getTimestamp());
    }

    private final int sequence;
    private final long dtStamp;

    /**
     * Initializes a new {@link ITipSequence}.
     * 
     * @param sequence The sequence number
     * @param dtStamp The timestamp (will be used in <i>seconds</i> precision, due to its representation in iCalendar format)
     */
    private ITipSequence(int sequence, long dtStamp) {
        super();
        this.sequence = sequence;
        this.dtStamp = 1000 * (dtStamp / 1000);
    }

    /**
     * Gets the sequence number.
     * 
     * @return The sequence number
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Gets the timestamp (in <i>seconds</i> precision, due to its representation in iCalendar format).
     * 
     * @return The timestamp
     */
    public long getDtStamp() {
        return dtStamp;
    }

    /**
     * Gets a value indicating whether this ITip sequence is <i>after</i> (i.e. marks a newer revision of calendar data) the passed ITip
     * sequence. The test is performed based on the sequence number, using the timestamp as tie-breaker if both sequence number match.
     * 
     * @param o The ITip sequence to check against
     * @return <code>true</code> if this sequence is <i>after</i> the other one
     */
    public boolean after(ITipSequence o) {
        return 0 < compareTo(o);
    }

    /**
     * Gets a value indicating whether this ITip sequence is <i>after</i> (i.e. marks a newer revision of calendar data) or equal to the
     * passed ITip sequence. The test is performed based on the sequence number, using the timestamp as tie-breaker if both sequence number
     * match.
     * 
     * @param o The ITip sequence to check against
     * @return <code>true</code> if this sequence is <i>after</i> or <i>equal to</i> the other one
     */
    public boolean afterOrEquals(ITipSequence o) {
        return 0 <= compareTo(o);
    }

    /**
     * Gets a value indicating whether this ITip sequence is <i>before</i> (i.e. marks an older revision of calendar data) the passed ITip
     * sequence. The test is performed based on the sequence number, using the timestamp as tie-breaker if both sequence number match.
     * 
     * @param o The ITip sequence to check against
     * @return <code>true</code> if this sequence is <i>before</i> the other one
     */
    public boolean before(ITipSequence o) {
        return 0 > compareTo(o);
    }

    /**
     * Gets a value indicating whether this ITip sequence is <i>before</i> (i.e. marks an older revision of calendar data) or equal to the
     * passed ITip sequence. The test is performed based on the sequence number, using the timestamp as tie-breaker if both sequence number
     * match.
     * 
     * @param o The ITip sequence to check against
     * @return <code>true</code> if this sequence is <i>before</i> or <i>equal to</i> the other one
     */
    public boolean beforeOrEquals(ITipSequence o) {
        return 0 >= compareTo(o);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (dtStamp ^ (dtStamp >>> 32));
        result = prime * result + sequence;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ITipSequence other = (ITipSequence) obj;
        if (dtStamp != other.dtStamp) {
            return false;
        }
        if (sequence != other.sequence) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(ITipSequence o) {
        if (null == o) {
            return 1;
        }
        int result = Integer.compare(sequence, o.sequence);
        return 0 != result ? result : Long.compare(dtStamp, o.dtStamp);
    }

    @Override
    public String toString() {
        return "ITipSequence [sequence=" + sequence + ", dtStamp=" + dtStamp + "]";
    }

}
