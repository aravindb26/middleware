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

package com.openexchange.chronos;

import java.util.TimeZone;
import java.util.UUID;

/**
 * {@link AlarmTrigger}s contain the information about when an alarm will be triggered.
 * <p>
 * It also tracks whether this trigger was already triggered or not.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public interface AlarmTrigger extends Comparable<AlarmTrigger> {

    /**
     * Gets the isActionSet
     *
     * @return The isActionSet
     */
    boolean containsAction();

    /**
     * Gets the isalarmSet
     *
     * @return The isalarmSet
     */
    boolean containsAlarm();

    /**
     * Gets the isEventIdSet
     *
     * @return The isEventIdSet
     */
    boolean containsEventId();

    boolean containsPushed();

    /**
     * Gets the isRecurrenceSet
     *
     * @return The isRecurrenceSet
     */
    boolean containsRecurrenceId();

    /**
     * Gets the isTimeSet
     *
     * @return The isTimeSet
     */
    boolean containsTime();

    /**
     * Gets the isUserIdSet
     *
     * @return The isUserIdSet
     */
    boolean containsUserId();

    /**
     * Gets the action
     *
     * @return The action
     */
    String getAction();

    /**
     * Gets the alarm
     *
     * @return The alarm
     */
    Integer getAlarm();

    /**
     * Gets the eventId
     *
     * @return The eventId
     */
    String getEventId();

    /**
     * Gets the folder
     *
     * @return The folder
     */
    String getFolder();

    /**
     * Sets the folder
     *
     * @param folder The folder to set
     */
    void setFolder(String folder);

    /**
     * Gets the isFolderSet
     *
     * @return The isFolderSet
     */
    boolean containsFolder();

    /**
     * Removes the folder
     */
    void removeFolder();

    /**
     * Whether the alarm is already pushed or not
     *
     * @return The pushed
     */
    Boolean isPushed();

    /**
     * Gets the recurrence identifier
     *
     * @return The recurrence identifier
     */
    RecurrenceId getRecurrenceId();

    /**
     * Gets the time
     *
     * @return The time
     */
    Long getTime();

    /**
     * Gets the userId
     *
     * @return The userId
     */
    Integer getUserId();

    /**
     * Removes the action
     */
    void removeAction();

    /**
     * Removes the alarm
     */
    void removeAlarm();

    /**
     * Removes the eventId
     */
    void removeEventId();

    /**
     * Removes the pushed
     */
    void removePushed();

    /**
     * Removes the recurrence identifier
     */
    void removeRecurrenceId();

    /**
     * Removes the user id
     */
    void removeTime();

    /**
     * Removes the user id
     */
    void removeUserId();

    /**
     * Sets the action
     *
     * @param action The action to set
     */
    void setAction(String action);

    /**
     * Sets the alarm
     *
     * @param alarm The alarm to set
     */
    void setAlarm(Integer alarm);

    /**
     * Sets the pushed
     *
     * @param pushed A boolean indicating whether the alarm is pushed or not
     */
    void setPushed(Boolean pushed);

    /**
     * Sets the recurrence
     *
     * @param recurrenceId The recurrence identifier to set
     */
    void setRecurrenceId(RecurrenceId recurrenceId);

    /**
     * Sets the time
     *
     * @param time The time to set
     */
    void setTime(Long time);

    /**
     * Sets the userId
     *
     * @param userId The userId to set
     */
    void setUserId(Integer userId);

    /**
     * Sets the eventId
     *
     * @param eventId The eventId to set
     */
    void setEventId(String eventId);

    /**
     * Gets the timezone
     *
     * @return The timezone
     */
    TimeZone getTimezone();

    /**
     * Sets the timezone
     *
     * @param timezone The timezone to set
     */
    void setTimezone(TimeZone timezone);

    /**
     * Removes the timezone
     */
    void removeTimezone();

    /**
     * Checks whether the alarm trigger contains a timezone
     *
     * @return <code>true</code> if a timezone is set, <code>false</code> otherwise
     */
    boolean containsTimezone();

    /**
     * Gets the relatedTime
     *
     * @return The relatedTime
     */
    Long getRelatedTime();

    /**
     * Sets the relatedTime
     *
     * @param relatedTime The relatedTime to set
     */
    void setRelatedTime(Long relatedTime);

    boolean containsRelatedTime();

    void removeRelatedTime();

    /**
     * Gets the processed value
     *
     * @return The processed value
     */
    Long getProcessed();

    /**
     * Sets the relatedTime
     *
     * @param processed The processed value
     */
    void setProcessed(Long processed);

    boolean containsProcessed();

    void removeProcessed();

    /**
     * Gets the claim value
     *
     * @return The claim value
     */
    UUID getClaim();

    /**
     * Sets the claim value
     *
     * @param claim The claim value
     */
    void setClaim(UUID claim);

    boolean containsClaim();

    void removeClaim();

    @Override
    default int compareTo(AlarmTrigger o) {
        long thisTime = this.getTime().longValue();
        long otherTime = o.getTime().longValue();
        return thisTime == otherTime ? 0 : thisTime > otherTime ? 1 : 0;
    }

}
