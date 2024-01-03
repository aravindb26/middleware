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

package com.openexchange.chronos.scheduling.common;

import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.mapping.DefaultEventUpdate;
import com.openexchange.chronos.scheduling.ITipEventUpdate;
import com.openexchange.groupware.tools.mappings.common.CollectionUpdate;
import com.openexchange.groupware.tools.mappings.common.ItemUpdate;

/**
 * {@link ITipEventUpdateImpl}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class ITipEventUpdateImpl extends DefaultEventUpdate implements ITipEventUpdate {

    // @formatter:off
    private final static Set<AttendeeField> IGNOREES = ImmutableSet.of(
        AttendeeField.EXTENDED_PARAMETERS,
        AttendeeField.CN,
        AttendeeField.CU_TYPE,
        AttendeeField.ENTITY,
        AttendeeField.ROLE,
        AttendeeField.RSVP,
        AttendeeField.HIDDEN,
        AttendeeField.TRANSP,
//        AttendeeField.SEQUENCE,
        AttendeeField.TIMESTAMP); // @formatter:on

    private final static EventField[] IGNORED_FIELDS = new EventField[] { EventField.EXTENDED_PROPERTIES };

    /**
     * 
     * Initializes a new {@link ITipEventUpdateImpl}.
     *
     * @param originalEvent The original event
     * @param updatedEvent The event with updated data
     * @param considerUnset <code>true</code> to consider unset fields in the updated event as removed
     * @param ignoredFields A set of fields to ignore for the diff
     */
    public ITipEventUpdateImpl(Event originalEvent, Event updatedEvent, boolean considerUnset, EventField... ignoredFields) {
        super(originalEvent, updatedEvent, considerUnset, IGNOREES, chooseFields(ignoredFields));

        /*
         * Super implementation checks if organizer is equals, for iTIP a matches is sufficient
         */
        if (updatedFields.contains(EventField.ORGANIZER)) {
            if (null == originalEvent || CalendarUtils.matches(originalEvent.getOrganizer(), updatedEvent.getOrganizer())) {
                updatedFields.remove(EventField.ORGANIZER);
            }
        }
    }

    private static EventField[] chooseFields(EventField... ignoredFields) {
        if (ignoredFields == null || ignoredFields.length == 0) {
            return IGNORED_FIELDS;
        }
        if (Arrays.stream(ignoredFields).anyMatch(x -> x == EventField.EXTENDED_PROPERTIES)) {
            return ignoredFields;
        }
        EventField[] fields = new EventField[ignoredFields.length + 1];
        System.arraycopy(ignoredFields, 0, fields, 0, ignoredFields.length);
        fields[fields.length - 1] = EventField.EXTENDED_PROPERTIES;
        return fields;
    }

    @Override
    public boolean containsAllChangesOf(EventField[] fields) {
        return getUpdatedFields().containsAll(Arrays.asList(fields));
    }

    @Override
    public boolean containsAnyChangesBeside(EventField[] fields) {
        Set<EventField> temp = new HashSet<>(getUpdatedFields());
        temp.removeAll(Arrays.asList(fields));
        return !temp.isEmpty();
    }

    @Override
    public boolean containsExactTheseChanges(EventField[] fields) {
        return containsAllChangesOf(fields) && containsOnlyChangeOf(fields);
    }

    @Override
    public boolean containsOnlyChangeOf(EventField[] fields) {
        return !containsAnyChangesBeside(fields);
    }

    @Override
    public boolean isAboutStateChangesOnly() {
        // First, let's see if any fields besides the state tracking fields have changed
        Set<EventField> differing = new HashSet<>(getUpdatedFields());
        differing.remove(EventField.ATTENDEES);
        if (!differing.isEmpty()) {
            return false;
        }

        return isAboutStateChanges();
    }

    @Override
    public boolean isAboutStateChangesOnly(EventField[] relevant) {
        // First, let's see if any fields besides the state tracking fields have changed
        Set<EventField> differing = new HashSet<>(getUpdatedFields());
        differing.remove(EventField.ATTENDEES);
        if (differing.removeAll(Arrays.asList(relevant))) {
            return false; // There is at least one relevant change left.
        }

        return isAboutStateChanges();
    }

    @Override
    public boolean isAboutStateChanges() {
        // Hm, okay, so now let's see if any participants were added or removed. That also means this mail is not only about state changes.
        if (!isNullOrEmpty(getAttendeeUpdates().getAddedItems())) {
            return false;
        }

        if (!isNullOrEmpty(getAttendeeUpdates().getRemovedItems())) {
            return false;
        }

        List<? extends ItemUpdate<Attendee, AttendeeField>> updatedItems = getAttendeeUpdates().getUpdatedItems();
        for (ItemUpdate<Attendee, AttendeeField> updatedItem : updatedItems) {
            Set<AttendeeField> temp = new HashSet<>(updatedItem.getUpdatedFields());
            temp.remove(AttendeeField.PARTSTAT);
            temp.remove(AttendeeField.TIMESTAMP);
            temp.remove(AttendeeField.SEQUENCE);
            temp.remove(AttendeeField.COMMENT);
            temp.remove(AttendeeField.HIDDEN);
            temp.remove(AttendeeField.TRANSP);
            if (!temp.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isAboutDetailChangesOnly() {
        HashSet<EventField> differing = new HashSet<>(getUpdatedFields());
        differing.remove(EventField.ATTENDEES);

        // If any other field than the participants fields as changed and the participant fields were not changed, we're done, as no state changes could have occurred
        if (!differing.isEmpty() && !containsAnyChangeOf(new EventField[] { EventField.ATTENDEES })) {
            return true;
        }

        // Hm, okay, so now let's see if any participants state has changed. That means, that something other than a detail field has changed
        if (getAttendeeUpdates() != null && !getAttendeeUpdates().isEmpty()) {
            if (getAttendeeUpdates().getUpdatedItems() != null && !getAttendeeUpdates().getUpdatedItems().isEmpty()) {
                for (ItemUpdate<Attendee, AttendeeField> item : getAttendeeUpdates().getUpdatedItems()) {
                    if (item.getUpdatedFields().contains(AttendeeField.PARTSTAT)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean isAboutCertainParticipantsStateChangeOnly(String identifier) {
        if (!isAboutStateChangesOnly()) {
            return false;
        }
        if (getAttendeeUpdates() == null || getAttendeeUpdates().isEmpty()) {
            return false;
        }

        CollectionUpdate<Attendee, AttendeeField> attendeeUpdates = getAttendeeUpdates();
        if (attendeeUpdates.getAddedItems() != null && !attendeeUpdates.getAddedItems().isEmpty()) {
            return false;
        }
        if (attendeeUpdates.getRemovedItems() != null && !attendeeUpdates.getRemovedItems().isEmpty()) {
            return false;
        }
        if (attendeeUpdates.getUpdatedItems() == null || attendeeUpdates.getUpdatedItems().size() != 1) {
            return false;
        }

        ItemUpdate<Attendee, AttendeeField> attendeeUpdate = attendeeUpdates.getUpdatedItems().get(0);
        if (!(attendeeUpdate.getOriginal().getEMail().equals(identifier) || Integer.toString(attendeeUpdate.getOriginal().getEntity()).equals(identifier))) {
            return false;
        }
        Set<AttendeeField> updatedFields = new HashSet<>(attendeeUpdate.getUpdatedFields());
        updatedFields.remove(AttendeeField.PARTSTAT);
        updatedFields.remove(AttendeeField.TIMESTAMP);
        if (!updatedFields.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isAboutCertainParticipantsRemoval(int userId) {
        if (getAttendeeUpdates() == null || getAttendeeUpdates().isEmpty()) {
            return false;
        }
        CollectionUpdate<Attendee, AttendeeField> attendeeUpdates = getAttendeeUpdates();
        if (attendeeUpdates.getAddedItems() != null && !attendeeUpdates.getAddedItems().isEmpty()) {
            return false;
        }
        if (attendeeUpdates.getUpdatedItems() != null && !attendeeUpdates.getUpdatedItems().isEmpty()) {
            return false;
        }
        if (attendeeUpdates.getRemovedItems() == null || attendeeUpdates.getRemovedItems().size() != 1) {
            return false;
        }
        if (attendeeUpdates.getRemovedItems().get(0).getEntity() != userId) {
            return false;
        }
        return true;
    }

}
