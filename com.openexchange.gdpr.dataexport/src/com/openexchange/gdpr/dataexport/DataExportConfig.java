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

package com.openexchange.gdpr.dataexport;

import java.time.DayOfWeek;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.openexchange.java.Strings;
import com.openexchange.schedule.RangesOfTheWeek;
import com.openexchange.schedule.ScheduleExpressionParser;

/**
 * {@link DataExportConfig} - The configuration for data export.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class DataExportConfig {

    /**
     * Gets a new builder instance.
     *
     * @return The new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for a configuration instance. */
    public static class Builder {

        private boolean active;
        private RangesOfTheWeek rangesOfTheWeek;
        private int numberOfConcurrentTasks;
        private int maxFailCountForWorkItem;
        private long checkForTasksFrequency;
        private long checkForAbortedTasksFrequency;
        private long maxProcessingTimeMillis;
        private long maxTimeToLiveMillis;
        private long expirationTimeMillis;
        private long defaultMaxFileSize;
        private boolean replaceUnicodeWithAscii;
        private boolean allowPausingRunningTasks;
        private boolean useZip64;

        /**
         * Initializes a new {@link DataExportConfig.Builder}.
         */
        private Builder() {
            super();
            active = true;
            numberOfConcurrentTasks = DataExportConstants.DEFAULT_NUMBER_OF_CONCURRENT_TASKS;
            maxFailCountForWorkItem = DataExportConstants.DEFAULT_MAX_FAIL_COUNT_FOR_WORK_ITEM;
            checkForTasksFrequency = DataExportConstants.DEFAULT_CHECK_FOR_TASKS_FREQUENCY;
            checkForAbortedTasksFrequency = DataExportConstants.DEFAULT_CHECK_FOR_ABORTED_TASKS_FREQUENCY;
            expirationTimeMillis = DataExportConstants.DEFAULT_EXPIRATION_TIME;
            maxProcessingTimeMillis = -1L;
            maxTimeToLiveMillis = DataExportConstants.DEFAULT_MAX_TIME_TO_LIVE;
            defaultMaxFileSize = DataExportConstants.DFAULT_MAX_FILE_SIZE;
            allowPausingRunningTasks = false;
            useZip64 = true;
        }

        /**
         * Sets whether processing of data export tasks should be enabled/active on this node.
         *
         * @param active <code>true</code> to activate processing of data export tasks; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withActive(boolean active) {
            this.active = active;
            return this;
        }

        /**
         * Sets whether pausing of running data export tasks is allowed.
         *
         * @param allowPausingRunningTasks <code>true</code> to allow pausing; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withAllowPausingRunningTasks(boolean allowPausingRunningTasks) {
            this.allowPausingRunningTasks = allowPausingRunningTasks;
            return this;
        }

        /**
         * Sets the number of concurrent tasks that are allowed being executed by this node.
         *
         * @param numberOfConcurrentTasks The number of concurrent tasks
         * @return This builder
         * @throws IllegalArgumentException If number is less than/equal to 0 (zero)
         */
        public Builder withNumberOfConcurrentTasks(int numberOfConcurrentTasks) {
            if (numberOfConcurrentTasks <= 0) {
                throw new IllegalArgumentException("numberOfConcurrentTasks must not be less than or equal to 0 (zero).");
            }
            this.numberOfConcurrentTasks = numberOfConcurrentTasks;
            return this;
        }

        /**
         * Sets the max. fail count for attempts to export items from a certain provider.
         *
         * @param maxFailCountForWorkItem The max. fail count
         * @return This builder
         * @throws IllegalArgumentException If max. fail count is less than/equal to 0 (zero)
         */
        public Builder withMaxFailCountForWorkItem(int maxFailCountForWorkItem) {
            if (maxFailCountForWorkItem <= 0) {
                throw new IllegalArgumentException("maxFailCountForWorkItem must not be less than or equal to 0 (zero).");
            }
            this.maxFailCountForWorkItem = maxFailCountForWorkItem;
            return this;
        }

        /**
         * Sets the frequency in milliseconds when to check for further tasks to process.
         *
         * @param checkForTasksFrequency The frequency to set
         * @return This builder
         * @throws IllegalArgumentException If frequency is less than/equal to 0 (zero)
         */
        public Builder withCheckForTasksFrequency(long checkForTasksFrequency) {
            if (checkForTasksFrequency <= 0) {
                throw new IllegalArgumentException("checkForTasksFrequency must not be less than or equal to 0 (zero).");
            }
            this.checkForTasksFrequency = checkForTasksFrequency;
            return this;
        }

        /**
         * Sets the frequency in milliseconds when to check for aborted tasks.
         *
         * @param checkForTasksFrequency The frequency to set
         * @return This builder
         * @throws IllegalArgumentException If frequency is less than/equal to 0 (zero)
         */
        public Builder withCheckForAbortedTasksFrequency(long checkForAbortedTasksFrequency) {
            if (checkForAbortedTasksFrequency <= 0) {
                throw new IllegalArgumentException("checkForAbortedTasksFrequency must not be less than or equal to 0 (zero).");
            }
            this.checkForAbortedTasksFrequency = checkForAbortedTasksFrequency;
            return this;
        }

        /**
         * Sets the max. time-to-live for completed tasks in milliseconds
         *
         * @param maxTimeToLiveMillis The max. time-to-live in milliseconds
         * @return This builder
         * @throws IllegalArgumentException If max. time-to-live in milliseconds is less than/equal to 0 (zero)
         */
        public Builder withMaxTimeToLiveMillis(long maxTimeToLiveMillis) {
            if (maxTimeToLiveMillis <= 0) {
                throw new IllegalArgumentException("Max. time-to-live in milliseconds must not be less than/equal to 0 (zero");
            }
            this.maxTimeToLiveMillis = maxTimeToLiveMillis;
            return this;
        }

        /**
         * Sets the max. processing time in milliseconds.
         *
         * @param maxProcessingTimeMillis The max. processing time in milliseconds or <code>-1</code> for infinite
         * @return This builder
         */
        public Builder withMaxProcessingTimeMillis(long maxProcessingTimeMillis) {
            this.maxProcessingTimeMillis = maxProcessingTimeMillis <= 0 ? -1 : maxProcessingTimeMillis;
            return this;
        }

        /**
         * Sets the expiration time in milliseconds.
         *
         * @param expirationTimeMillis The expiration time in milliseconds
         * @return This builder
         * @throws IllegalArgumentException If expiration time in milliseconds is less than/equal to 0 (zero)
         */
        public Builder withExpirationTimeMillis(long expirationTimeMillis) {
            if (expirationTimeMillis <= 0) {
                throw new IllegalArgumentException("Expiration time in milliseconds must not be less than/equal to 0 (zero");
            }
            this.expirationTimeMillis = expirationTimeMillis;
            return this;
        }

        /**
         * Sets the default max. file size for resulting files.
         *
         * @param defaultMaxFileSize The default max. file size
         * @return This builder
         */
        public Builder withDefaultMaxFileSize(long defaultMaxFileSize) {
            this.defaultMaxFileSize = defaultMaxFileSize;
            return this;
        }

        /**
         * Sets whether to replace Unicode characters of ZIP archive entry names with somewhat reasonable ASCII7-only characters.
         *
         * @param replaceUnicodeWithAscii <code>true</code> to replace Unicode with somewhat reasonable ASCII7-only; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withReplaceUnicodeWithAscii(boolean replaceUnicodeWithAscii) {
            this.replaceUnicodeWithAscii = replaceUnicodeWithAscii;
            return this;
        }

        /**
         * Sets whether to use ZIP64 format when creating ZIP archives.
         *
         * @param useZip64 <code>true</code> to use ZIP64 format; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withUseZip64(boolean useZip64) {
            this.useZip64 = useZip64;
            return this;
        }

        /**
         * Parses the given configuration; e.g. <code>"Mon 0:12-6:45; Tue-Thu 0-7:15; Fri 0-6,22:30-24; Sat,Sun 0-8"</code>.
         *
         * @param config The configuration to parse
         * @return This builder
         * @throws IllegalArgumentException If schedule information is invalid
         */
        public Builder parse(String config) {
            if (Strings.isEmpty(config)) {
                return this;
            }

            this.rangesOfTheWeek = ScheduleExpressionParser.parse(config);
            return this;
        }

        /**
         * Creates the <code>DataExportConfig</code> instance from this builder's arguments.
         *
         * @return The <code>DataExportConfig</code> instance
         */
        public DataExportConfig build() {
            return new DataExportConfig(active, rangesOfTheWeek, defaultMaxFileSize, numberOfConcurrentTasks, checkForTasksFrequency, checkForAbortedTasksFrequency, maxProcessingTimeMillis, maxTimeToLiveMillis, expirationTimeMillis, maxFailCountForWorkItem, replaceUnicodeWithAscii, allowPausingRunningTasks, useZip64);
        }

    } // End of Builder class

    // -----------------------------------------------------------------------------------------------------------------------------

    private final boolean active;
    private final RangesOfTheWeek rangesOfTheWeek;
    private final int numberOfConcurrentTasks;
    private final long checkForTasksFrequency;
    private final long checkForAbortedTasksFrequency;
    private final long maxProcessingTimeMillis;
    private final long maxTimeToLiveMillis;
    private final long expirationTimeMillis;
    private final long defaultMaxFileSize;
    private final int maxFailCountForWorkItem;
    private final boolean replaceUnicodeWithAscii;
    private final boolean allowPausingRunningTasks;
    private final boolean useZip64;

    /**
     * Initializes a new {@link DataExportConfig}.
     *
     * @param active Whether processing of data export tasks should be enabled/active on this node
     * @param rangesOfTheWeek The ranges of week defining the allowed schedule
     * @param defaultMaxFileSize The default max. file size
     * @param numberOfConcurrentTasks The number of concurrent tasks
     * @param checkForTasksFrequency The frequency when to check for due data export tasks
     * @param checkForAbortedTasksFrequency The frequency when to check for aborted data export tasks
     * @param maxProcessingTimeMillis The max. processing time in milliseconds
     * @param maxTimeToLiveMillis The max. time-to-live in milliseconds
     * @param expirationTimeMillis The max. expiration time in milliseconds
     * @param maxFailCountForWorkItem The max. fail count for a work item
     * @param replaceUnicodeWithAscii Whether to replace unicode characters with appropriate ASCII representation
     * @param allowPausingRunningTasks Whether to allow pausing tasks if schedule is elapsed
     * @param useZip64 Whether to use ZIP64 format for created ZIP archives
     */
    private DataExportConfig(boolean active, RangesOfTheWeek rangesOfTheWeek, long defaultMaxFileSize, int numberOfConcurrentTasks,
            long checkForTasksFrequency, long checkForAbortedTasksFrequency, long maxProcessingTimeMillis, long maxTimeToLiveMillis,
            long expirationTimeMillis, int maxFailCountForWorkItem, boolean replaceUnicodeWithAscii, boolean allowPausingRunningTasks,
            boolean useZip64) {
        super();
        this.active = active;
        this.defaultMaxFileSize = defaultMaxFileSize;
        this.numberOfConcurrentTasks = numberOfConcurrentTasks;
        this.maxProcessingTimeMillis = maxProcessingTimeMillis;
        this.maxTimeToLiveMillis = maxTimeToLiveMillis;
        this.expirationTimeMillis = expirationTimeMillis;
        this.maxFailCountForWorkItem = maxFailCountForWorkItem;
        this.replaceUnicodeWithAscii = replaceUnicodeWithAscii;
        this.useZip64 = useZip64;
        this.rangesOfTheWeek = rangesOfTheWeek == null ? RangesOfTheWeek.EMPTY_SCHEDULE : rangesOfTheWeek;
        this.checkForTasksFrequency = checkForTasksFrequency;
        this.checkForAbortedTasksFrequency = checkForAbortedTasksFrequency;
        this.allowPausingRunningTasks = allowPausingRunningTasks;
    }

    /**
     * Whether to use ZIP64 format when creating ZIP archives.
     *
     * @return <code>true</code> to use ZIP64 format; otherwise <code>false</code>
     */
    public boolean isUseZip64() {
        return useZip64;
    }

    /**
     * Checks whether to replace Unicode characters of ZIP archive entry names with somewhat reasonable ASCII7-only characters.
     * <p>
     * For instance, the string <code>"r&eacute;sum&eacute;"</code> is converted to <code>"resume"</code>.
     *
     * @return <code>true</code> to replace Unicode with somewhat reasonable ASCII7-only; otherwise <code>false</code>
     */
    public boolean isReplaceUnicodeWithAscii() {
        return replaceUnicodeWithAscii;
    }

    /**
     * Checks whether pausing of running data export tasks is allowed.
     *
     * @return <code>true</code> if pausing is allowed; otherwise <code>false</code>
     */
    public boolean isAllowPausingRunningTasks() {
        return allowPausingRunningTasks;
    }

    /**
     * Gets the max. fail count for attempts to export items from a certain provider.
     *
     * @return The max. fail count
     */
    public int getMaxFailCountForWorkItem() {
        return maxFailCountForWorkItem;
    }

    /**
     * Checks whether processing of data export tasks should be enabled/active on this node.
     *
     * @return <code>true</code> if processing is activated; otherwise <code>false</code>
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Gets the default max. file size to assume for result files.
     *
     * @return The default max. file size
     */
    public long getDefaultMaxFileSize() {
        return defaultMaxFileSize;
    }

    /**
     * Gets the expiration time in milliseconds.
     * <p>
     * If a task's time stamp has not been touch for this amount of milliseconds, it is considered as expired.
     *
     * @return The expiration time in milliseconds or <code>-1</code> for no expiration time
     */
    public long getExpirationTimeMillis() {
        return expirationTimeMillis;
    }

    /**
     * Gets the max. processing time in milliseconds
     *
     * @return The max. processing time in milliseconds or <code>-1</code> form infinite
     */
    public long getMaxProcessingTimeMillis() {
        return maxProcessingTimeMillis;
    }

    /**
     * Gets the max. time-to-live for completed tasks in milliseconds
     *
     * @return The max. time-to-live in milliseconds
     */
    public long getMaxTimeToLiveMillis() {
        return maxTimeToLiveMillis;
    }

    /**
     * Gets the frequency in milliseconds when to check for further tasks to process.
     *
     * @return The frequency
     */
    public long getCheckForTasksFrequency() {
        return checkForTasksFrequency;
    }

    /**
     * Gets the frequency in milliseconds when to check for aborted tasks.
     *
     * @return The frequency
     */
    public long getCheckForAbortedTasksFrequency() {
        return checkForAbortedTasksFrequency;
    }

    /**
     * Gets the number of concurrent tasks that are allowed being executed by this node.
     *
     * @return The number of concurrent tasks
     */
    public int getNumberOfConcurrentTasks() {
        return numberOfConcurrentTasks;
    }

    /**
     * Gets the ranges of the week.
     *
     * @return The ranges of the week.
     */
    public RangesOfTheWeek getRangesOfTheWeek() {
        return rangesOfTheWeek;
    }

    // -----------------------------------------------------------------------------------------------------------------------------

    private static final Set<String> DAY_OF_WEEK_MONDAY = ImmutableSet.of("mo", "mon", "monday");
    private static final Set<String> DAY_OF_WEEK_TUESDAY = ImmutableSet.of("tu", "tue", "tuesday");
    private static final Set<String> DAY_OF_WEEK_WEDNESDAY = ImmutableSet.of("we", "wed", "wednesday");
    private static final Set<String> DAY_OF_WEEK_THURSDAY = ImmutableSet.of("th", "thu", "thursday");
    private static final Set<String> DAY_OF_WEEK_FRIDAY = ImmutableSet.of("fr", "fri", "friday");
    private static final Set<String> DAY_OF_WEEK_SATURDAY = ImmutableSet.of("sa", "sat", "satday");
    private static final Set<String> DAY_OF_WEEK_SUNDAY = ImmutableSet.of("su", "sun", "sunday");

    /**
     * Parses specified day of week to associated calendar constant.
     *
     * @param day The day of week to parse
     * @return The calendar constant
     * @throws IllegalArgumentException If given day of week cannot be parsed to a calendar constant
     * @see DayOfWeek#SUNDAY
     * @see DayOfWeek#MONDAY
     * @see DayOfWeek#TUESDAY
     * @see DayOfWeek#WEDNESDAY
     * @see DayOfWeek#THURSDAY
     * @see DayOfWeek#FRIDAY
     * @see DayOfWeek#SATURDAY
     */
    public static DayOfWeek getDayOfWeekFor(String day) {
        if (day == null) {
            return null;
        }
        String toCheck = Strings.asciiLowerCase(day.trim());
        if (DAY_OF_WEEK_MONDAY.contains(toCheck)) {
            return DayOfWeek.MONDAY;
        }
        if (DAY_OF_WEEK_TUESDAY.contains(toCheck)) {
            return DayOfWeek.TUESDAY;
        }
        if (DAY_OF_WEEK_WEDNESDAY.contains(toCheck)) {
            return DayOfWeek.WEDNESDAY;
        }
        if (DAY_OF_WEEK_THURSDAY.contains(toCheck)) {
            return DayOfWeek.THURSDAY;
        }
        if (DAY_OF_WEEK_FRIDAY.contains(toCheck)) {
            return DayOfWeek.FRIDAY;
        }
        if (DAY_OF_WEEK_SATURDAY.contains(toCheck)) {
            return DayOfWeek.SATURDAY;
        }
        if (DAY_OF_WEEK_SUNDAY.contains(toCheck)) {
            return DayOfWeek.SUNDAY;
        }
        throw new IllegalArgumentException("Cannot be parsed to a day of week: " + day);
    }

}
