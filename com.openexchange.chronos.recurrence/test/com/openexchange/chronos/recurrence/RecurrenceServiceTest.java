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

package com.openexchange.chronos.recurrence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import org.dmfs.rfc5545.DateTime;
import org.junit.After;
import org.junit.Before;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.chronos.recurrence.service.RecurrenceServiceImpl;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.time.TimeTools;

/**
 * {@link RecurrenceServiceTest}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public abstract class RecurrenceServiceTest {

    protected RecurrenceService service;
    protected String timeZone;

    public RecurrenceServiceTest() {}

    public RecurrenceServiceTest(String timeZone) {
        this.timeZone = timeZone;
    }

    @Before
    public void setUp() {
        service = new RecurrenceServiceImpl(new TestRecurrenceConfig());
    }

    @After
    public void tearDown() {}

    protected void compareInstanceWithMaster(Event master, Event instance, Date start, Date end) {
        assertNotNull("Master must not be null.", master);
        assertNotNull("Instance must not be null", instance);
        Event clone = clone(master);

        instance = clone(instance);
        instance.removeRecurrenceId();
        instance.removeRecurrenceRule();

        clone.removeId();
        clone.removeRecurrenceRule();
        clone.removeDeleteExceptionDates();
        clone.setStartDate(DT(start, clone.getStartDate().getTimeZone(), clone.getStartDate().isAllDay()));
        clone.setEndDate(DT(end, clone.getEndDate().getTimeZone(), clone.getEndDate().isAllDay()));

        String equals = equals(clone, instance);
        assertNull(equals);
    }

    protected void compareChangeExceptionWithMaster(Event master, Event instance, Date recurrenceId, Date start, Date end) {
        assertNotNull("Master must not be null.", master);
        assertNotNull("Instance must not be null", instance);
        Event clone = clone(master);

        clone.removeId();
        clone.removeRecurrenceRule();
        clone.removeDeleteExceptionDates();
        clone.setRecurrenceId(new DefaultRecurrenceId(DT(recurrenceId, master.getStartDate().getTimeZone(), master.getStartDate().isAllDay())));
        clone.setStartDate(DT(start, clone.getStartDate().getTimeZone(), clone.getStartDate().isAllDay()));
        clone.setEndDate(DT(end, clone.getEndDate().getTimeZone(), clone.getEndDate().isAllDay()));

        String equals = equals(clone, instance);
        assertNull(equals);
    }

    protected void compareFullTimeChangeExceptionWithMaster(Event master, Event instance, DateTime recurrenceId, DateTime start, DateTime end) {
        assertNotNull("Master must not be null.", master);
        assertNotNull("Instance must not be null", instance);
        Event clone = clone(master);

        clone.removeId();
        clone.removeRecurrenceRule();
        clone.removeDeleteExceptionDates();
        clone.setRecurrenceId(new DefaultRecurrenceId(recurrenceId));
        clone.setStartDate(start);
        clone.setEndDate(end);

        String equals = equals(clone, instance);
        assertNull(equals);
    }

    protected void compareChangeExceptionWithFullTimeMaster(Event master, Event instance, DateTime recurrenceId, DateTime start, DateTime end) {
        assertNotNull("Master must not be null.", master);
        assertNotNull("Instance must not be null", instance);
        Event clone = clone(master);

        clone.removeId();
        clone.removeRecurrenceRule();
        clone.removeDeleteExceptionDates();
        clone.setRecurrenceId(new DefaultRecurrenceId(recurrenceId));
        clone.setStartDate(start);
        clone.setEndDate(end);

        String equals = equals(clone, instance);
        assertNull(equals);
    }

    protected String getUntilZulu(Calendar c) {
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        String month = Integer.toString(c.get(Calendar.MONTH) + 1);
        String year = Integer.toString(c.get(Calendar.YEAR));
        String dayOfMonth = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
        if (dayOfMonth.length() == 1) {
            dayOfMonth = "0" + dayOfMonth;
        }
        String hourOfDay = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        if (hourOfDay.length() == 1) {
            hourOfDay = "0" + hourOfDay;
        }
        String minute = Integer.toString(c.get(Calendar.MINUTE));
        if (minute.length() == 1) {
            minute = "0" + minute;
        }
        String second = Integer.toString(c.get(Calendar.SECOND));
        if (second.length() == 1) {
            second = "0" + second;
        }
        return "" + year + month + dayOfMonth + "T" + hourOfDay + minute + second + "Z";
    }

    protected Calendar getCal(String date) {
        Calendar retval = GregorianCalendar.getInstance(TimeZone.getTimeZone(timeZone));
        retval.setTime(TimeTools.D(date, TimeZone.getTimeZone(timeZone)));
        return retval;
    }

    protected Event getInstance(Event master, Date recurrenceId, Date start, Date end) {
        Event instance = clone(master);
        instance.removeId();
        instance.removeRecurrenceRule();
        instance.removeDeleteExceptionDates();
        instance.setRecurrenceId(new DefaultRecurrenceId(DT(recurrenceId, master.getStartDate().getTimeZone(), master.getStartDate().isAllDay())));
        instance.setStartDate(DT(start, instance.getStartDate().getTimeZone(), instance.getStartDate().isAllDay()));
        instance.setEndDate(DT(end, instance.getEndDate().getTimeZone(), instance.getEndDate().isAllDay()));
        return instance;
    }

    protected String equals(Event event, Event other) {
        if (event == other) {
            return null;
        }
        if (event == null) {
            return null == other ? null : "One event is nul.";
        }
        if (event.getAttachments() == null) {
            if (other.getAttachments() != null) {
                return "One Attachments List is null";
            }
        } else if (!event.getAttachments().equals(other.getAttachments())) {
            return "Different attachments";
        }
        if (event.getAttendees() == null) {
            if (other.getAttendees() != null) {
                return "One Attendee List is null";
            }
        } else if (!event.getAttendees().equals(other.getAttendees())) {
            return "Different Attendees";
        }
        if (event.getCategories() == null) {
            if (other.getCategories() != null) {
                return "One Categories List is null";
            }
        } else if (!event.getCategories().equals(other.getCategories())) {
            return "Different Categories";
        }
        if (event.getClassification() != other.getClassification()) {
            return "Different Classification: " + event.getClassification() + " VS " + other.getClassification();
        }
        if (event.getColor() == null) {
            if (other.getColor() != null) {
                return "One color is null";
            }
        } else if (!event.getColor().equals(other.getColor())) {
            return "Different Color: " + event.getColor() + " VS " + other.getColor();
        }
        if (event.getCreated() == null) {
            if (other.getCreated() != null) {
                return "One created is null";
            }
        } else if (!event.getCreated().equals(other.getCreated())) {
            return "Different Created: " + event.getCreated() + " VS " + other.getCreated();
        }
        if (event.getCreatedBy() != other.getCreatedBy()) {
            return "Different CreatedBy: " + event.getCreatedBy() + " VS " + other.getCreatedBy();
        }
        if (event.getDeleteExceptionDates() == null) {
            if (other.getDeleteExceptionDates() != null) {
                return "One DeleteExceptionDates is null";
            }
        } else if (!event.getDeleteExceptionDates().equals(other.getDeleteExceptionDates())) {
            return "Different DeleteExceptionDates";
        }
        if (event.getDescription() == null) {
            if (other.getDescription() != null) {
                return "One Description is null";
            }
        } else if (!event.getDescription().equals(other.getDescription())) {
            return "Different Description: " + event.getDescription() + " VS " + other.getDescription();
        }
        if (event.getEndDate() == null) {
            if (other.getEndDate() != null) {
                return "One End Date is null";
            }
        } else if (!event.getEndDate().equals(other.getEndDate())) {
            return "Different End Date: " + event.getEndDate() + " VS " + other.getEndDate();
        }
        if (event.getFilename() == null) {
            if (other.getFilename() != null) {
                return "One Filename is null";
            }
        } else if (!event.getFilename().equals(other.getFilename())) {
            return "Different Filename: " + event.getFilename() + " VS " + other.getFilename();
        }
        if (event.getId() != other.getId()) {
            return "Different ID: " + event.getId() + " VS " + other.getId();
        }
        if (event.getLastModified() == null) {
            if (other.getLastModified() != null) {
                return "One Last Modified is null";
            }
        } else if (!event.getLastModified().equals(other.getLastModified())) {
            return "Different Last Modified: " + event.getLastModified() + " VS " + other.getLastModified();
        }
        if (event.getLocation() == null) {
            if (other.getLocation() != null) {
                return "One Location is null";
            }
        } else if (!event.getLocation().equals(other.getLocation())) {
            return "Different Location: " + event.getLocation() + " VS " + other.getLocation();
        }
        if (event.getModifiedBy() != other.getModifiedBy()) {
            return "Different Modified By: " + event.getModifiedBy() + " VS " + other.getModifiedBy();
        }
        if (event.getOrganizer() == null) {
            if (other.getOrganizer() != null) {
                return "One Organizer is null";
            }
        } else if (!event.getOrganizer().equals(other.getOrganizer())) {
            return "Different Organizer: " + event.getOrganizer() + " VS " + other.getOrganizer();
        }
        if (event.getFolderId() != other.getFolderId()) {
            return "Different Folder Id: " + event.getFolderId() + " VS " + other.getFolderId();
        }
        if (event.getRecurrenceId() == null) {
            if (other.getRecurrenceId() != null) {
                return "One recurrence Id is null";
            }
        } else if (!event.getRecurrenceId().equals(other.getRecurrenceId())) {
            return "Different Recurrence id: " + event.getRecurrenceId() + " VS " + other.getRecurrenceId();
        }
        if (event.getRecurrenceRule() == null) {
            if (other.getRecurrenceRule() != null) {
                return "One Recurrence Rule is null";
            }
        } else if (!event.getRecurrenceRule().equals(other.getRecurrenceRule())) {
            return "Different Recurrence Rule: " + event.getRecurrenceRule() + " VS " + other.getRecurrenceRule();
        }
        if (event.getSequence() != other.getSequence()) {
            return "Different Sequence: " + event.getSequence() + " VS " + other.getSequence();
        }
        if (event.getSeriesId() != other.getSeriesId()) {
            return "Different Series Id: " + event.getSeriesId() + " VS " + other.getSeriesId();
        }
        if (event.getStartDate() == null) {
            if (other.getStartDate() != null) {
                return "One start date is null";
            }
        } else if (!event.getStartDate().equals(other.getStartDate())) {
            return "Different Start Date: " + event.getStartDate() + " VS " + other.getStartDate();
        }
        if (event.getStatus() != other.getStatus()) {
            return "Different Status: " + event.getStatus() + " VS " + other.getStatus();
        }
        if (event.getSummary() == null) {
            if (other.getSummary() != null) {
                return "One summary is null";
            }
        } else if (!event.getSummary().equals(other.getSummary())) {
            return "Different Summary: " + event.getSummary() + " VS " + other.getSummary();
        }
        if (event.getTransp() == null) {
            if (other.getTransp() != null) {
                return "One transp is null";
            }
        } else if (!event.getTransp().equals(other.getTransp())) {
            return "Different Transparency: " + event.getTransp() + " VS " + other.getTransp();
        }
        if (event.getUid() == null) {
            if (other.getUid() != null) {
                return "One Uid is null";
            }
        } else if (!event.getUid().equals(other.getUid())) {
            return "Different Uid: " + event.getUid() + " VS " + other.getUid();
        }
        return null;
    }

    protected Event clone(Event event) {
        Event clone = new Event();
        if (event.containsAttachments()) {
            clone.setAttachments(cloneList(event.getAttachments()));
        }
        if (event.containsAttendees()) {
            clone.setAttendees(cloneList(event.getAttendees()));
        }
        if (event.containsAlarms()) {
            clone.setAlarms(cloneList(event.getAlarms()));
        }
        if (event.containsCategories()) {
            clone.setCategories(cloneList(event.getCategories()));
        }
        if (event.containsClassification()) {
            clone.setClassification(event.getClassification());
        }
        if (event.containsColor()) {
            clone.setColor(event.getColor());
        }
        if (event.containsCreated()) {
            clone.setCreated(event.getCreated());
        }
        if (event.containsCreatedBy()) {
            clone.setCreatedBy(event.getCreatedBy());
        }
        if (event.containsDeleteExceptionDates()) {
            clone.setDeleteExceptionDates(cloneSet(event.getDeleteExceptionDates()));
        }
        if (event.containsDescription()) {
            clone.setDescription(event.getDescription());
        }
        if (event.containsEndDate()) {
            clone.setEndDate(event.getEndDate());
        }
        if (event.containsFilename()) {
            clone.setFilename(event.getFilename());
        }
        if (event.containsFolderId()) {
            clone.setFolderId(event.getFolderId());
        }
        if (event.containsId()) {
            clone.setId(event.getId());
        }
        if (event.containsLastModified()) {
            clone.setLastModified(event.getLastModified());
        }
        if (event.containsLocation()) {
            clone.setLocation(event.getLocation());
        }
        if (event.containsModifiedBy()) {
            clone.setModifiedBy(event.getModifiedBy());
        }
        if (event.containsOrganizer()) {
            clone.setOrganizer(event.getOrganizer());
        }
        if (event.containsRecurrenceId()) {
            clone.setRecurrenceId(event.getRecurrenceId());
        }
        if (event.containsRecurrenceRule()) {
            clone.setRecurrenceRule(event.getRecurrenceRule());
        }
        if (event.containsSequence()) {
            clone.setSequence(event.getSequence());
        }
        if (event.containsSeriesId()) {
            clone.setSeriesId(event.getSeriesId());
        }
        if (event.containsStartDate()) {
            clone.setStartDate(event.getStartDate());
        }
        if (event.containsStatus()) {
            clone.setStatus(event.getStatus());
        }
        if (event.containsSummary()) {
            clone.setSummary(event.getSummary());
        }
        if (event.containsTransp()) {
            clone.setTransp(event.getTransp());
        }
        if (event.containsUid()) {
            clone.setUid(event.getUid());
        }
        return clone;
    }

    private <T> List<T> cloneList(List<T> list) {
        if (null == list) {
            return null;
        }
        List<T> retval = new ArrayList<T>();
        retval.addAll(list);
        return retval;
    }

    private <T> SortedSet<T> cloneSet(SortedSet<T> list) {
        if (null == list) {
            return null;
        }
        SortedSet<T> retval = new TreeSet<T>();
        retval.addAll(list);
        return retval;
    }

    protected static DateTime DT(String value, TimeZone timeZone, boolean allDay) {
        return DT(TimeTools.D(value, timeZone), timeZone, allDay);
    }

    protected static DateTime DT(Date date, TimeZone timeZone, boolean allDay) {
        if (allDay) {
            return new DateTime(date.getTime()).toAllDay();
        } else {
            return new DateTime(timeZone, date.getTime());
        }
    }

    protected static void setStartAndEndDates(Event event, Date startDate, Date endDate, boolean allDay, TimeZone timeZone) {
        if (allDay) {
            event.setStartDate(new DateTime(startDate.getTime()).toAllDay());
            event.setEndDate(new DateTime(endDate.getTime()).toAllDay());
        } else {
            event.setStartDate(new DateTime(timeZone, startDate.getTime()));
            event.setEndDate(new DateTime(timeZone, endDate.getTime()));
        }
    }

    protected static void setStartAndEndDates(Event event, String start, String end, boolean allDay, TimeZone timeZone) {
        event.setStartDate(DT(start, timeZone, allDay));
        event.setEndDate(DT(end, timeZone, allDay));
    }

}
