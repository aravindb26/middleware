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

package com.openexchange.schedule;

import static com.openexchange.java.Autoboxing.L;
import com.openexchange.server.ServiceLookup;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link TaskScheduler} - Cares about execution of a single periodic task according to given ranges of the week.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public class TaskScheduler {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TaskScheduler.class);

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of {@link TaskScheduler} */
    public static class Builder {

        private Runnable periodicTask;
        private long frequencyMillis;
        private Runnable optStopProcessingListener;
        private RangesOfTheWeek rangesOfTheWeek;
        private Supplier<TimerService> timerServiceSupplier;

        /**
         * Initializes a new {@link Builder}.
         */
        private Builder() {
            super();
        }

        /**
         * Sets the periodic task.
         *
         * @param periodicTask The periodic task to set
         */
        public Builder withPeriodicTask(Runnable periodicTask) {
            this.periodicTask = periodicTask;
            return this;
        }

        /**
         * Sets the frequency milliseconds.
         *
         * @param frequencyMillis The frequency milliseconds to set
         */
        public Builder withFrequencyMillis(long frequencyMillis) {
            this.frequencyMillis = frequencyMillis;
            return this;
        }

        /**
         * Sets the stop processing listener.
         *
         * @param stopProcessingListener The stop processing listener to set
         */
        public Builder withStopProcessingListener(Runnable stopProcessingListener) {
            this.optStopProcessingListener = stopProcessingListener;
            return this;
        }

        /**
         * Sets the ranges of the week.
         *
         * @param rangesOfTheWeek The ranges of the week to set
         */
        public Builder withRangesOfTheWeek(RangesOfTheWeek rangesOfTheWeek) {
            this.rangesOfTheWeek = rangesOfTheWeek;
            return this;
        }

        /**
         * Sets the schedule expression.
         *
         * @param scheduleExpression The schedule expression to set
         */
        public Builder withScheduleExpression(String scheduleExpression) {
            this.rangesOfTheWeek = ScheduleExpressionParser.parse(scheduleExpression);
            return this;
        }

        /**
         * Sets the timer service supplier.
         *
         * @param timerServiceSupplier The timer service supplier to set
         */
        public Builder withTimerServiceSupplier(Supplier<TimerService> timerServiceSupplier) {
            this.timerServiceSupplier = timerServiceSupplier;
            return this;
        }

        /**
         * Sets the timer service supplier to retrieve from given service look-up instance.
         *
         * @param services The service look-up instance to acquire the timer service from
         */
        public Builder withTimerServiceFrom(ServiceLookup services) {
            if (services == null) {
                throw new IllegalArgumentException("Service look-up must not be null");
            }
            return withTimerServiceSupplier(new ServiceLookupSupplier(services));
        }

        /**
         * Creates the instance of {@link TaskScheduler} from this builder's arguments.
         *
         * @return The instance of {@link TaskScheduler}
         */
        public TaskScheduler build() {
            return new TaskScheduler(periodicTask, frequencyMillis, optStopProcessingListener, rangesOfTheWeek, timerServiceSupplier);
        }

    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final Runnable periodicTask;
    private final long frequencyMillis;
    private final Runnable optStopProcessingListener;
    private final RangesOfTheWeek rangesOfTheWeek;
    private final Supplier<TimerService> timerServiceSupplier;

    private final AtomicReference<ScheduledTimerTask> periodicTimerTask;
    private final AtomicReference<ScheduledTimerTask> stopTimerTask;
    private final AtomicReference<ScheduledTimerTask> nextRunTask;

    /**
     * Initializes a new {@link TaskScheduler} with given ranges of the week.
     *
     * @param periodicTask The task that is ought to be periodically executed
     * @param frequencyMillis The frequency in milliseconds for the periodic task
     * @param optStopProcessingListener The task to invoke when stopping periodic task executions; may be <code>null</code>
     * @param rangesOfTheWeek The ranges of the week in which periodic execution is permitted
     * @param timerServiceSupplier The supplier of the timer service
     */
    private TaskScheduler(Runnable periodicTask, long frequencyMillis, Runnable optStopProcessingListener, RangesOfTheWeek rangesOfTheWeek, Supplier<TimerService> timerServiceSupplier) {
        super();
        if (rangesOfTheWeek == null) {
            throw new IllegalArgumentException("Ranges of week must not be null");
        }
        if (timerServiceSupplier == null) {
            throw new IllegalArgumentException("Timer service supplier must not be null");
        }
        if (periodicTask == null) {
            throw new IllegalArgumentException("Periodic task must not be null");
        }
        if (frequencyMillis <= 0) {
            throw new IllegalArgumentException("Frequency millis must not be less than or equal to 0 (zero)");
        }
        this.periodicTask = periodicTask;
        this.frequencyMillis = frequencyMillis;
        this.optStopProcessingListener = optStopProcessingListener;
        this.rangesOfTheWeek = rangesOfTheWeek;
        this.timerServiceSupplier = timerServiceSupplier;

        this.periodicTimerTask = new AtomicReference<>(null);
        this.stopTimerTask = new AtomicReference<>(null);
        this.nextRunTask = new AtomicReference<>(null);
    }

    private static void cancelTimerTasks(AtomicReference<ScheduledTimerTask> timerTaskReference, boolean mayInterruptIfRunning, boolean purge, TimerService optTimerService) {
        ScheduledTimerTask timerTask = timerTaskReference.getAndSet(null);
        if (timerTask != null) {
            timerTask.cancel(mayInterruptIfRunning);
        }

        if (purge && optTimerService != null) {
            optTimerService.purge();
        }
    }

    private void cancelAllTimerTasks(boolean mayInterruptIfRunning, TimerService optTimerService) {
        cancelTimerTasks(periodicTimerTask, mayInterruptIfRunning, false, null);
        cancelTimerTasks(stopTimerTask, mayInterruptIfRunning, false, null);
        cancelTimerTasks(nextRunTask, mayInterruptIfRunning, false, null);
        if (optTimerService != null) {
            optTimerService.purge();
        }
    }

    /**
     * Checks if this task scheduler is already running.
     *
     * @return <code>true</code> if running; otherwise <code>false</code>
     */
    public synchronized boolean isRunning() {
        return periodicTimerTask.get() != null;
    }

    /**
     * Stops this task scheduler.
     */
    public synchronized void stop() {
        cancelAllTimerTasks(true, timerServiceSupplier.get());
    }

    /**
     * (Re-)Starts this task scheduler.
     */
    public synchronized void start() {
        TimerService timerService = timerServiceSupplier.get();
        if (timerService == null) {
            throw new IllegalArgumentException("Timer service must not be null");
        }

        // Cancel all possibly running timer tasks
        cancelAllTimerTasks(false, timerService);

        // Determine start delay
        long startInvocationTimeMillis = System.currentTimeMillis();
        StartAndStopDelay startAndStopDelay = determineStartAndStopDelay(startInvocationTimeMillis, rangesOfTheWeek);

        // Schedule task
        schedule(startAndStopDelay.startDelay, startAndStopDelay.optionalStopDelay, startInvocationTimeMillis);
    }

    /**
     * Schedules the periodic execution of the task.
     * <p>
     * Dependent on whether a stop delay is present, schedule additional one-shot actions for stopping periodic task execution and
     * initiating next slot for periodic task execution.
     *
     * @param startDelay The delay when periodic task execution shall start
     * @param optionalStopDelay The optional delay when periodic task execution shall stop
     * @param startInvocationTimeMillis The time when this instance's {@link #start()} method was invoked (the number of milliseconds since January 1, 1970, 00:00:00 GMT)
     */
    private void schedule(long startDelay, Optional<Long> optionalStopDelay, long startInvocationTimeMillis) {
        if (startDelay < 0) {
            throw new IllegalArgumentException("Start delay must not be less than 0 (zero): " + startDelay);
        }

        TimerService timerService = timerServiceSupplier.get();
        if (optionalStopDelay.isPresent()) {
            long stopDelay = optionalStopDelay.get().longValue();
            if (stopDelay < startDelay) {
                throw new IllegalArgumentException("Stop delay (" + stopDelay + ") must be greater than start delay (" + startDelay + ")");
            }

            // Schedule periodic task execution
            long endTimeMillis = startInvocationTimeMillis + stopDelay;
            Runnable startTask = () -> {
                if (System.currentTimeMillis() < endTimeMillis) {
                    periodicTask.run();
                }
            };
            this.periodicTimerTask.set(timerService.scheduleWithFixedDelay(startTask, startDelay, frequencyMillis, TimeUnit.MILLISECONDS));

            // Schedule one-shot action for stopping periodic task execution
            Runnable stopScheduleTask = () -> {
                cancelTimerTasks(periodicTimerTask, false, false, null);
                cancelTimerTasks(stopTimerTask, false, false, null);
                TimerService timerService1 = timerServiceSupplier.get();
                if (timerService1 != null) {
                    timerService1.purge();
                }

                if (optStopProcessingListener != null) {
                    optStopProcessingListener.run();
                }
            };
            this.stopTimerTask.set(timerService.schedule(stopScheduleTask, stopDelay, TimeUnit.MILLISECONDS));

            // Schedule one-shot action for initiating next slot for periodic task execution
            Runnable nextScheduleRun = () -> {
                try {
                    start();
                } catch (Exception e) {
                    LOG.error("Failed to schedule tasks", e);
                }
            };
            this.nextRunTask.set(timerService.schedule(nextScheduleRun, stopDelay + 60000L, TimeUnit.MILLISECONDS));
        } else {
            // Schedule periodic task execution that runs forever
            this.periodicTimerTask.set(timerService.scheduleWithFixedDelay(periodicTask, 0, frequencyMillis, TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Determines the start and (optional) stop delay.
     *
     * @param startInvocationTimeMillis The time in milliseconds when {@link #start()} has been invoked
     * @param rangesOfTheWeek The ranges of the week representing applicable schedule
     * @return The start and (optional) stop delay
     */
    private static StartAndStopDelay determineStartAndStopDelay(long startInvocationTimeMillis, RangesOfTheWeek rangesOfTheWeek) {
        // With initial settings for the current date and time in the system default time zone
        int dayOfTheWeek;
        TimeOfTheDay time;
        {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(startInvocationTimeMillis);
            dayOfTheWeek = now.get(Calendar.DAY_OF_WEEK);
            time = getTimeOfTheDayFrom(now);
        }

        // Determine next time frame
        DayOfWeekTimeRanges dayOfWeekTimeRanges = rangesOfTheWeek.getDayOfWeekTimeRangesFor(dayOfTheWeek);
        if (dayOfWeekTimeRanges != null) {
            // Today... Check if current time is included in any time range; otherwise determine closest
            TimeRange applicableTimeRange = null;
            Iterator<TimeRange> it = dayOfWeekTimeRanges.getRanges().iterator();
            while (applicableTimeRange == null && it.hasNext()) {
                TimeRange timeRange = it.next();
                if (timeRange.contains(time) || (time.compareTo(timeRange.getStart()) < 0)) {
                    applicableTimeRange = timeRange;
                }
            }

            if (applicableTimeRange != null) {
                // Schedule for closest time range today
                Calendar tmp = Calendar.getInstance();
                tmp.setTimeInMillis(startInvocationTimeMillis);

                long startDelay;
                if (applicableTimeRange.contains(time)) {
                    // Already in a defined time range from today, hence start immediately
                    startDelay = 0L;
                } else {
                    applyTimeOfTheDay(applicableTimeRange.getStart(), tmp);
                    startDelay = tmp.getTimeInMillis() - startInvocationTimeMillis;
                }

                // Determine stop delay
                Optional<Long> optionalStopDelay = determineStopDelay(applicableTimeRange, it, rangesOfTheWeek, dayOfTheWeek, tmp, startInvocationTimeMillis);
                return new StartAndStopDelay(startDelay, optionalStopDelay);
            }

            dayOfWeekTimeRanges = null;
        }

        // Find follow-up day's time range
        int dayDiffer = 0;
        while (dayOfWeekTimeRanges == null) {
            dayOfTheWeek = nextDayOfWeekFor(dayOfTheWeek);
            dayDiffer++;
            dayOfWeekTimeRanges = rangesOfTheWeek.getDayOfWeekTimeRangesFor(dayOfTheWeek);
        }

        // Schedule for first time range of follow-up day
        Iterator<TimeRange> it = dayOfWeekTimeRanges.getRanges().iterator();
        TimeRange firstOnFollowUpDay = it.next();

        Calendar tmp = Calendar.getInstance();
        tmp.setTimeInMillis(startInvocationTimeMillis);
        tmp.add(Calendar.DAY_OF_YEAR, dayDiffer);
        applyTimeOfTheDay(firstOnFollowUpDay.getStart(), tmp);
        long startDelay = tmp.getTimeInMillis() - startInvocationTimeMillis;

        // Determine stop delay
        Optional<Long> optionalStopDelay = determineStopDelay(firstOnFollowUpDay, it, rangesOfTheWeek, dayOfTheWeek, tmp, startInvocationTimeMillis);
        return new StartAndStopDelay(startDelay, optionalStopDelay);
    }

    /**
     * Determines the stop delay.
     *
     * @param applicableTimeRange The time range in which task should be started
     * @param remainingRanges The remaining ranges to consider
     * @param rangesOfTheWeek The ranges of the week representing applicable schedule
     * @param dayOfTheWeek The day of the week of the start date
     * @param calendar The instance of {@code Calendar} pre-initialized with <code>startInvocationTimeMillis</code>
     * @param startInvocationTimeMillis The time in milliseconds when {@link #start()} has been invoked
     * @return The stop delay or <code>null</code>
     */
    private static Optional<Long> determineStopDelay(TimeRange applicableTimeRange, Iterator<TimeRange> remainingRanges, RangesOfTheWeek rangesOfTheWeek, int dayOfTheWeek, Calendar calendar, long startInvocationTimeMillis) {
        // Determine end. First consume remaining time ranges
        TimeRange endTimeRange = applicableTimeRange;
        boolean checkNextDays = true;
        while (remainingRanges.hasNext()) {
            TimeRange nextTimeRange = remainingRanges.next();
            if (endTimeRange.getEnd().isEndBeginningOfOtherDay(nextTimeRange.getStart())) {
                endTimeRange = nextTimeRange;
            } else {
                checkNextDays = false;
            }
        }

        if (!checkNextDays) {
            // End does not fall on next day
            applyTimeOfTheDay(endTimeRange.getEnd(), calendar);
            return Optional.of(L(calendar.getTimeInMillis() - startInvocationTimeMillis));
        }

        // Now, check for follow-up day of week
        boolean infinite = false;
        int dayDiffer = 0;
        {
            int nextDayOfWeek = nextDayOfWeekFor(dayOfTheWeek);
            boolean lookUp = true;
            for (DayOfWeekTimeRanges nextDayOfWeekTimeRanges; lookUp && (nextDayOfWeekTimeRanges = rangesOfTheWeek.getDayOfWeekTimeRangesFor(nextDayOfWeek)) != null;) {
                boolean first = true;
                for (Iterator<TimeRange> nextit = nextDayOfWeekTimeRanges.getRanges().iterator(); lookUp && nextit.hasNext();) {
                    TimeRange nextTimeRange = nextit.next();
                    if (endTimeRange.getEnd().isEndBeginningOfOtherDay(nextTimeRange.getStart())) {
                        if (nextDayOfWeek == dayOfTheWeek) {
                            // Full round-trip
                            infinite = true;
                            lookUp = false;
                        }
                        if (first) {
                            dayDiffer++;
                            first = false;
                        }
                        endTimeRange = nextTimeRange;
                    } else {
                        lookUp = false;
                    }
                }
                nextDayOfWeek = nextDayOfWeekFor(nextDayOfWeek);
            }
        }

        if (infinite) {
            return Optional.empty();
        }

        if (dayDiffer > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, dayDiffer);
        }
        applyTimeOfTheDay(endTimeRange.getEnd(), calendar);
        return Optional.of(L(calendar.getTimeInMillis() - startInvocationTimeMillis));
    }

    /**
     * Gets the time of the day from given instance of {@code Calendar}.
     *
     * @param calendar The instance of {@code Calendar} to extract from
     * @return The time of the day
     */
    private static TimeOfTheDay getTimeOfTheDayFrom(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return new TimeOfTheDay(hour, minute, second);
    }

    /**
     * Applies given time of the day to specified instance of {@code Calendar}.
     * <p>
     * Sets the fields {@link Calendar#HOUR_OF_DAY}, {@link Calendar#MINUTE} as well as {@link Calendar#SECOND} according to time of the day
     * on instance of {@code Calendar}.
     *
     * @param timeOfTheDay The time of the day to apply
     * @param calendar The instance of {@code Calendar} to apply to
     */
    private static void applyTimeOfTheDay(TimeOfTheDay timeOfTheDay, Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, timeOfTheDay.getHour());
        calendar.set(Calendar.MINUTE, timeOfTheDay.getMinute());
        calendar.set(Calendar.SECOND, timeOfTheDay.getSecond());
    }

    /**
     * Gets the next day of week following given day of week.
     * <p>
     * <table>
     * <tr><td>{@link Calendar#SUNDAY}</td><td>-&gt;</td><td align="right">{@link Calendar#MONDAY}</td></tr>
     * <tr><td>{@link Calendar#MONDAY}</td><td>-&gt;</td><td align="right">{@link Calendar#TUESDAY}</td></tr>
     * <tr><td>{@link Calendar#TUESDAY}</td><td>-&gt;</td><td align="right">{@link Calendar#WEDNESDAY}</td></tr>
     * <tr><td>{@link Calendar#WEDNESDAY}</td><td>-&gt;</td><td align="right">{@link Calendar#THURSDAY}</td></tr>
     * <tr><td>{@link Calendar#THURSDAY}</td><td>-&gt;</td><td align="right">{@link Calendar#FRIDAY}</td></tr>
     * <tr><td>{@link Calendar#FRIDAY}</td><td>-&gt;</td><td align="right">{@link Calendar#SATURDAY}</td></tr>
     * <tr><td>{@link Calendar#SATURDAY}</td><td>-&gt;</td><td align="right">{@link Calendar#SUNDAY}</td></tr>
     * </table>
     *
     * @param dayOfWeek The day of week to get the next day for
     * @return The next day of week
     */
    private static int nextDayOfWeekFor(int dayOfWeek) {
        int nextDayOfWeek = dayOfWeek + 1;
        if (nextDayOfWeek > Calendar.SATURDAY) {
            nextDayOfWeek = Calendar.SUNDAY;
        }
        return nextDayOfWeek;
    }

    @Override
    public String toString() {
        return rangesOfTheWeek.toString();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /** Simple tuple for start and optional stop delay */
    private static final class StartAndStopDelay {

        /** The start delay in milliseconds */
        final long startDelay;

        /** The stop delay in milliseconds or <code>null</code> */
        final Optional<Long> optionalStopDelay;

        /**
         * Initializes a new {@link StartAndStopDelay}.
         *
         * @param startDelay The start delay in milliseconds
         * @param optionalStopDelay The stop delay in milliseconds or empty
         */
        StartAndStopDelay(long startDelay, Optional<Long> optionalStopDelay) {
            super();
            this.startDelay = startDelay;
            this.optionalStopDelay = optionalStopDelay;
        }
    }

    /** The supplier offering timer service from a service look-up instance */
    private static final class ServiceLookupSupplier implements Supplier<TimerService> {

        private final ServiceLookup services;

        /**
         * Initializes a new {@link ServiceLookupSupplier}.
         *
         * @param services The service look-up instance
         */
        ServiceLookupSupplier(ServiceLookup services) {
            super();
            this.services = services;
        }

        @Override
        public TimerService get() {
            return services.getOptionalService(TimerService.class);
        }
    }

}
