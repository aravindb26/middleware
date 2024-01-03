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
 * {@link DefaultLockedAlarmTrigger} - Represents a locked alarm trigger.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public class DefaultLockedAlarmTrigger implements LockedAlarmTrigger {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultLockedAlarmTrigger.class);

    /**
     * Creates an unlocked alarm trigger.
     *
     * @param trigger The backing trigger
     * @return The created unlocked alarm trigger
     */
    public static DefaultLockedAlarmTrigger createUnlockedAlarmTriggerFor(AlarmTrigger trigger) {
        return new DefaultLockedAlarmTrigger(trigger, AcquiredAlarmTriggerLock.DUMMY, null);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final AlarmTrigger trigger;
    private final AcquiredAlarmTriggerLock lock;

    /**
     * Initializes a new {@link DefaultLockedAlarmTrigger}.
     *
     * @param trigger The backing trigger
     * @param lock The acquired lock
     * @param claim The claim for the lock
     */
    public DefaultLockedAlarmTrigger(AlarmTrigger trigger, AcquiredAlarmTriggerLock lock, UUID claim) {
        super();
        this.trigger = trigger;
        this.lock = lock;
        this.setClaim(claim); // NOSONARLINT
    }

    @Override
    public void unlock() {
        try {
            lock.unlock();
        } catch (Exception e) {
            LOG.warn("Failed to orderly unlock locked alarm trigger {}", this, e);
        }
    }

    @Override
    public boolean containsAction() {
        return trigger.containsAction();
    }

    @Override
    public boolean containsAlarm() {
        return trigger.containsAlarm();
    }

    @Override
    public boolean containsEventId() {
        return trigger.containsEventId();
    }

    @Override
    public boolean containsPushed() {
        return trigger.containsPushed();
    }

    @Override
    public boolean containsRecurrenceId() {
        return trigger.containsRecurrenceId();
    }

    @Override
    public boolean containsTime() {
        return trigger.containsTime();
    }

    @Override
    public boolean containsUserId() {
        return trigger.containsUserId();
    }

    @Override
    public String getAction() {
        return trigger.getAction();
    }

    @Override
    public Integer getAlarm() {
        return trigger.getAlarm();
    }

    @Override
    public String getEventId() {
        return trigger.getEventId();
    }

    @Override
    public String getFolder() {
        return trigger.getFolder();
    }

    @Override
    public void setFolder(String folder) {
        trigger.setFolder(folder);
    }

    @Override
    public boolean containsFolder() {
        return trigger.containsFolder();
    }

    @Override
    public void removeFolder() {
        trigger.removeFolder();
    }

    @Override
    public Boolean isPushed() {
        return trigger.isPushed();
    }

    @Override
    public RecurrenceId getRecurrenceId() {
        return trigger.getRecurrenceId();
    }

    @Override
    public Long getTime() {
        return trigger.getTime();
    }

    @Override
    public Integer getUserId() {
        return trigger.getUserId();
    }

    @Override
    public void removeAction() {
        trigger.removeAction();
    }

    @Override
    public void removeAlarm() {
        trigger.removeAlarm();
    }

    @Override
    public void removeEventId() {
        trigger.removeEventId();
    }

    @Override
    public void removePushed() {
        trigger.removePushed();
    }

    @Override
    public void removeRecurrenceId() {
        trigger.removeRecurrenceId();
    }

    @Override
    public void removeTime() {
        trigger.removeTime();
    }

    @Override
    public void removeUserId() {
        trigger.removeUserId();
    }

    @Override
    public void setAction(String action) {
        trigger.setAction(action);
    }

    @Override
    public void setAlarm(Integer alarm) {
        trigger.setAlarm(alarm);
    }

    @Override
    public void setPushed(Boolean pushed) {
        trigger.setPushed(pushed);
    }

    @Override
    public void setRecurrenceId(RecurrenceId recurrenceId) {
        trigger.setRecurrenceId(recurrenceId);
    }

    @Override
    public void setTime(Long time) {
        trigger.setTime(time);
    }

    @Override
    public void setUserId(Integer userId) {
        trigger.setUserId(userId);
    }

    @Override
    public void setEventId(String eventId) {
        trigger.setEventId(eventId);
    }

    @Override
    public TimeZone getTimezone() {
        return trigger.getTimezone();
    }

    @Override
    public void setTimezone(TimeZone timezone) {
        trigger.setTimezone(timezone);
    }

    @Override
    public void removeTimezone() {
        trigger.removeTimezone();
    }

    @Override
    public boolean containsTimezone() {
        return trigger.containsTimezone();
    }

    @Override
    public Long getRelatedTime() {
        return trigger.getRelatedTime();
    }

    @Override
    public void setRelatedTime(Long relatedTime) {
        trigger.setRelatedTime(relatedTime);
    }

    @Override
    public boolean containsRelatedTime() {
        return trigger.containsRelatedTime();
    }

    @Override
    public void removeRelatedTime() {
        trigger.removeRelatedTime();
    }

    @Override
    public Long getProcessed() {
        return trigger.getProcessed();
    }

    @Override
    public void setProcessed(Long processed) {
        trigger.setProcessed(processed);
    }

    @Override
    public boolean containsProcessed() {
        return trigger.containsProcessed();
    }

    @Override
    public void removeProcessed() {
        trigger.removeProcessed();
    }

    @Override
    public UUID getClaim() {
        return trigger.getClaim();
    }

    @Override
    public void setClaim(UUID claim) {
        trigger.setClaim(claim);
    }

    @Override
    public boolean containsClaim() {
        return trigger.containsClaim();
    }

    @Override
    public void removeClaim() {
        trigger.removeClaim();
    }

    @Override
    public String toString() {
        return trigger.toString();
    }

}
