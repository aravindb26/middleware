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
 * {@link DefaultAlarmTrigger}s contain the information about when an alarm will be triggered.
 * <p>
 * It also tracks whether this trigger was already triggered or not.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public class DefaultAlarmTrigger implements AlarmTrigger {

    private String action;
    private Integer alarm;
    private String eventId;
    private String folder;
    private RecurrenceId recurrenceId;
    private Long time;
    private Long relatedTime;
    private Long processed;
    private UUID claim;
    private Integer userId;
    private Boolean pushed;
    private TimeZone timezone;

    private boolean isActionSet=false;
    private boolean isAlarmSet=false;
    private boolean isEventIdSet=false;
    private boolean isRecurrenceIdSet = false;
    private boolean isTimeSet=false;
    private boolean isUserIdSet=false;
    private boolean isPushedSet = false;
    private boolean isFolderSet = false;
    private boolean isTimeZoneSet = false;
    private boolean isRelatedTimeSet = false;
    private boolean isProcessedSet = false;
    private boolean isClaimSet = false;

    /**
     * Initializes a new {@link DefaultAlarmTrigger}.
     */
    public DefaultAlarmTrigger() {
        super();
    }

    /**
     * Gets the isActionSet
     *
     * @return The isActionSet
     */
    @Override
    public boolean containsAction() {
        return isActionSet;
    }

    /**
     * Gets the isalarmSet
     *
     * @return The isalarmSet
     */
    @Override
    public boolean containsAlarm() {
        return isAlarmSet;
    }

    /**
     * Gets the isEventIdSet
     *
     * @return The isEventIdSet
     */
    @Override
    public boolean containsEventId() {
        return isEventIdSet;
    }

    @Override
    public boolean containsPushed() {
        return isPushedSet;
    }

    /**
     * Gets the isRecurrenceSet
     *
     * @return The isRecurrenceSet
     */
    @Override
    public boolean containsRecurrenceId() {
        return isRecurrenceIdSet;
    }

    /**
     * Gets the isTimeSet
     *
     * @return The isTimeSet
     */
    @Override
    public boolean containsTime() {
        return isTimeSet;
    }

    /**
     * Gets the isUserIdSet
     *
     * @return The isUserIdSet
     */
    @Override
    public boolean containsUserId() {
        return isUserIdSet;
    }

    /**
     * Gets the action
     *
     * @return The action
     */
    @Override
    public String getAction() {
        return action;
    }

    /**
     * Gets the alarm
     *
     * @return The alarm
     */
    @Override
    public Integer getAlarm() {
        return alarm;
    }

    /**
     * Gets the eventId
     *
     * @return The eventId
     */
    @Override
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the folder
     *
     * @return The folder
     */
    @Override
    public String getFolder() {
        return folder;
    }

    /**
     * Sets the folder
     *
     * @param folder The folder to set
     */
    @Override
    public void setFolder(String folder) {
        this.folder = folder;
        this.isFolderSet = true;
    }

    /**
     * Gets the isFolderSet
     *
     * @return The isFolderSet
     */
    @Override
    public boolean containsFolder() {
        return isFolderSet;
    }

    /**
     * Removes the folder
     */
    @Override
    public void removeFolder() {
        this.folder = null;
        this.isFolderSet = false;
    }

    /**
     * Whether the alarm is already pushed or not
     *
     * @return The pushed
     */
    @Override
    public Boolean isPushed() {
        return pushed;
    }

    /**
     * Gets the recurrence identifier
     *
     * @return The recurrence identifier
     */
    @Override
    public RecurrenceId getRecurrenceId() {
        return recurrenceId;
    }

    /**
     * Gets the time
     *
     * @return The time
     */
    @Override
    public Long getTime() {
        return time;
    }

    /**
     * Gets the userId
     *
     * @return The userId
     */
    @Override
    public Integer getUserId() {
        return userId;
    }

    /**
     * Removes the action
     */
    @Override
    public void removeAction() {
        this.action=null;
        this.isActionSet=false;
    }

    /**
     * Removes the alarm
     */
    @Override
    public void removeAlarm() {
        this.alarm=null;
        this.isAlarmSet=false;
    }

    /**
     * Removes the eventId
     */
    @Override
    public void removeEventId() {
        this.eventId=null;
        this.isEventIdSet=false;
    }

    /**
     * Removes the pushed
     */
    @Override
    public void removePushed() {
        this.pushed=null;
        this.isPushedSet = false;
    }

    /**
     * Removes the recurrence identifier
     */
    @Override
    public void removeRecurrenceId() {
        this.recurrenceId=null;
        this.isRecurrenceIdSet = false;
    }

    /**
     * Removes the user id
     */
    @Override
    public void removeTime() {
        this.time=null;
        this.isTimeSet=false;
    }

    /**
     * Removes the user id
     */
    @Override
    public void removeUserId() {
        this.userId=null;
        this.isUserIdSet=false;
    }

    /**
     * Sets the action
     *
     * @param action The action to set
     */
    @Override
    public void setAction(String action) {
        this.action = action;
        this.isActionSet=true;
    }

    /**
     * Sets the alarm
     *
     * @param alarm The alarm to set
     */
    @Override
    public void setAlarm(Integer alarm) {
        this.alarm = alarm;
        this.isAlarmSet=true;
    }

    /**
     * Sets the pushed
     *
     * @param pushed A boolean indicating whether the alarm is pushed or not
     */
    @Override
    public void setPushed(Boolean pushed) {
        this.pushed = pushed;
        this.isPushedSet = true;
    }

    /**
     * Sets the recurrence
     *
     * @param recurrenceId The recurrence identifier to set
     */
    @Override
    public void setRecurrenceId(RecurrenceId recurrenceId) {
        this.recurrenceId = recurrenceId;
        this.isRecurrenceIdSet = true;
    }

    /**
     * Sets the time
     *
     * @param time The time to set
     */
    @Override
    public void setTime(Long time) {
        this.time = time;
        this.isTimeSet=true;
    }

    /**
     * Sets the userId
     *
     * @param userId The userId to set
     */
    @Override
    public void setUserId(Integer userId) {
        this.userId = userId;
        this.isUserIdSet=true;
    }

    /**
     * Sets the eventId
     *
     * @param eventId The eventId to set
     */
    @Override
    public void setEventId(String eventId) {
        this.eventId = eventId;
        this.isEventIdSet=true;
    }

    /**
     * Gets the timezone
     *
     * @return The timezone
     */
    @Override
    public TimeZone getTimezone() {
        return timezone;
    }

    /**
     * Sets the timezone
     *
     * @param timezone The timezone to set
     */
    @Override
    public void setTimezone(TimeZone timezone) {
        this.timezone = timezone;
        this.isTimeZoneSet = true;
    }

    /**
     * Removes the timezone
     */
    @Override
    public void removeTimezone(){
        this.timezone = null;
        this.isTimeZoneSet = false;
    }

    /**
     * Checks whether the {@link DefaultAlarmTrigger} contains a timezone
     *
     * @return <code>true</code> if a timezone is set, <code>false</code> otherwise
     */
    @Override
    public boolean containsTimezone(){
        return isTimeZoneSet;
    }

    /**
     * Gets the relatedTime
     *
     * @return The relatedTime
     */
    @Override
    public Long getRelatedTime() {
        return relatedTime;
    }

    /**
     * Sets the relatedTime
     *
     * @param relatedTime The relatedTime to set
     */
    @Override
    public void setRelatedTime(Long relatedTime) {
        this.relatedTime = relatedTime;
        this.isRelatedTimeSet = true;
    }

    @Override
    public boolean containsRelatedTime(){
        return isRelatedTimeSet;
    }

    @Override
    public void removeRelatedTime(){
        this.relatedTime = null;
        this.isRelatedTimeSet = false;
    }

    /**
     * Gets the processed value
     *
     * @return The processed value
     */
    @Override
    public Long getProcessed() {
        return processed;
    }

    /**
     * Sets the relatedTime
     *
     * @param processed The processed value
     */
    @Override
    public void setProcessed(Long processed) {
        this.processed = processed;
        this.isProcessedSet = true;
    }

    @Override
    public boolean containsProcessed(){
        return isProcessedSet;
    }

    @Override
    public void removeProcessed(){
        this.processed = null;
        this.isProcessedSet = false;
    }

    @Override
    public UUID getClaim() {
        return claim;
    }

    @Override
    public void setClaim(UUID claim) {
        this.claim = claim;
        this.isClaimSet = true;
    }

    @Override
    public boolean containsClaim() {
        return isClaimSet;
    }

    @Override
    public void removeClaim() {
        claim = null;
        this.isClaimSet = false;
    }

    @Override
    public String toString() {
        return "AlarmTrigger [action=" + action + ", time=" + time + ", alarm=" + alarm + ", userId=" + userId + ", eventId=" + eventId + ", recurrenceId=" + recurrenceId + "]";
    }

}
