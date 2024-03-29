
package com.openexchange.test.common.groupware.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.tasks.Mapper;
import com.openexchange.groupware.tasks.Mapping;
import com.openexchange.groupware.tasks.Task;

public class TaskAsserts {

    /*
     * ASSERTS GO HERE
     */
    public static void assertTaskFieldMatches(int field, Task expectedTask, Task comparedTask) {
        Mapper<?> mapping = Mapping.getMapping(field);
        if (mapping == null) {
            return;
        }

        Object expectedValue = mapping.get(expectedTask);
        Object comparedValue = mapping.get(comparedTask);

        if (expectedValue instanceof Date) {
            assertTrue(TaskAsserts.checkOXDatesAreEqual((Date) expectedValue, (Date) comparedValue), "The following field should be equal in both Tasks: " + "[" + getReadableName(mapping) + "], expected: " + expectedValue + ", but was: " + comparedValue);
        } else {
            assertEquals(expectedValue, comparedValue, "The following field should be equal in both Tasks: " + "[" + getReadableName(mapping) + "]");
        }
    }

    /**
     * @param mapping
     * @return
     */
    private static String getReadableName(Mapper<?> mapping) {
        try {
            return mapping.getDBColumnName();
        } catch (UnsupportedOperationException x) {
            return mapping.getClass().getSimpleName();
        }
    }

    public static void assertTaskFieldDiffers(int field, Task expectedTask, Task comparedTask) {
        Mapper<?> mapping = Mapping.getMapping(field);
        Object expectedValue = mapping.get(expectedTask);
        Object comparedValue = mapping.get(comparedTask);

        assertFalse(expectedValue.equals(comparedValue), "The following field should differ in both Tasks: " + "[" + mapping.getDBColumnName() + "]" + ", value: " + expectedValue);
    }

    /**
     * Compares two Tasks and asserts that all of their fields
     * excepts the listed ones are the same.
     *
     * @param expectedTask
     * @param comparedTask
     * @param excluded
     */
    public static void assertAllTaskFieldsMatchExcept(Task expectedTask, Task comparedTask, Set<Integer> excluded) {
        for (int column : Task.ALL_COLUMNS) {
            if (!excluded.contains(Integer.valueOf(column))) {
                assertTaskFieldMatches(column, expectedTask, comparedTask);
            }
        }
    }

    /**
     * Compares two Tasks and asserts that all listed fields
     * are different.
     *
     * @param expectedTask
     * @param comparedTask
     * @param included
     */
    public static void assertTaskFieldsDiffer(Task expectedTask, Task comparedTask, Set<Integer> included) {
        for (int column : Task.ALL_COLUMNS) {
            if (included.contains(Integer.valueOf(CalendarObject.TITLE))) {
                assertTaskFieldDiffers(column, expectedTask, comparedTask);
            }
        }
    }

    /**
     * Assures that both start and end date are in the future
     * 
     * @param task
     */
    public static void assertTaskIsInTheFuture(Task task) {
        Date now = new Date();
        assertTrue(now.compareTo(task.getStartDate()) < 0, "Start date not in the future");
        assertTrue(now.compareTo(task.getStartDate()) < 0, "End date not in the future");
    }

    /**
     * Assures that both start and end date are in the past
     * 
     * @param task
     */
    public static void assertTaskIsInThePast(Task task) {
        Date now = new Date();
        assertTrue(now.compareTo(task.getStartDate()) > 0, "Start date not in the past");
        assertTrue(now.compareTo(task.getStartDate()) > 0, "End date not in the past");
    }

    /**
     * Assures that the start date is in the past and end date is in the future
     * 
     * @param task
     */
    public static void assertTaskIsOngoing(Task task) {
        Date now = new Date();
        assertTrue(now.compareTo(task.getStartDate()) > 0, "Start date not in the past");
        assertTrue(now.compareTo(task.getStartDate()) < 0, "End date not in the future");
    }

    public static void assertFirstDateOccursLaterThanSecond(Date firstDate, Date secondDate) {
        assertTrue(firstDate.compareTo(secondDate) > 0);
    }

    public static void assertFirstDateOccursEarlierThanSecond(Date firstDate, Date secondDate) {
        assertTrue(firstDate.compareTo(secondDate) < 0);
    }

    /**
     * Compares two dates, but only down to the second. Due to some database
     * optimizations, the OX is not precise below that.
     *
     * @param message
     * @param date1
     * @param date2
     */
    public static boolean checkOXDatesAreEqual(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        cal1.set(Calendar.MILLISECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) && cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY) && cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE);
    }

    public static void assertDateInRecurrence(Date date, CalendarObject calendarObject) {
        fail("NOT IMPLEMENTED");
    }

}
