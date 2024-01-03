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

package com.openexchange.processing;

import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.MDC;
import com.openexchange.exception.OXException;

/**
 * {@link ProcessorTasks} - Utility class for processor tasks.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ProcessorTasks {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ProcessorTasks.class);
    }

    /**
     * Initializes a new {@link ProcessorTasks}.
     */
    private ProcessorTasks() {
        super();
    }

    /**
     * Gets the appropriate processor task for given <code>Runnable</code> instance.
     *
     * @param task The <code>Runnable</code> instance to wrap by a processor task
     * @return The processor task
     */
    public static ProcessorTask processorTaskFor(Runnable task) {
        return processorTaskFor(task, 0);
    }

    /**
     * Gets the appropriate processor task for given <code>Runnable</code> instance.
     *
     * @param task The <code>Runnable</code> instance to wrap by a processor task
     * @param timeoutMillis The timeout in milliseconds or <code>0</code>
     * @return The processor task
     */
    public static ProcessorTask processorTaskFor(Runnable task, long timeoutMillis) {
        if (task == null) {
            return null;
        }
        return (task instanceof ProcessorTask) ? (ProcessorTask) task : new RunnableProcessorTask(task, MDC.getCopyOfContextMap(), timeoutMillis <= 0 ? 0 : timeoutMillis);
    }

    /**
     * Gets the appropriate processor task for given <code>Callable</code> instance.
     *
     * @param task The <code>Callable</code> instance to wrap by a processor task
     * @return The processor task
     */
    public static ProcessorTask processorTaskFor(Callable<? extends Void> task) {
        return processorTaskFor(task, 0);
    }

    /**
     * Gets the appropriate processor task for given <code>Callable</code> instance.
     *
     * @param task The <code>Callable</code> instance to wrap by a processor task
     * @param timeoutMillis The timeout in milliseconds or <code>0</code>
     * @return The processor task
     */
    public static ProcessorTask processorTaskFor(Callable<? extends Void> task, long timeoutMillis) {
        if (task == null) {
            return null;
        }
        return (task instanceof ProcessorTask) ? (ProcessorTask) task : new CallableProcessorTask(task, MDC.getCopyOfContextMap(), timeoutMillis <= 0 ? 0 : timeoutMillis);
    }

    private static final class RunnableProcessorTask implements ProcessorTask {

        private final Runnable task;
        private final Map<String, String> mdc;
        private final long timeoutMillis;

        RunnableProcessorTask(Runnable task, Map<String, String> mdc, long timeoutMillis) {
            super();
            this.task = task;
            this.mdc = mdc;
            this.timeoutMillis = timeoutMillis;
        }

        @Override
        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        @Override
        public void run() {
            task.run();
        }

        @Override
        public Map<String, String> getSubmitterMdc() {
            return mdc;
        }
    } // End of class RunnableProcessorTask

    private static final class CallableProcessorTask implements ProcessorTask {

        private final Callable<? extends Void> task;
        private final Map<String, String> mdc;
        private final long timeoutMillis;

        CallableProcessorTask(Callable<? extends Void> task, Map<String, String> mdc, long timeoutMillis) {
            super();
            this.task = task;
            this.mdc = mdc;
            this.timeoutMillis = timeoutMillis;
        }

        @Override
        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        @Override
        public void run() {
            try {
                task.call();
            } catch (Exception e) {
                LoggerHolder.LOG.error("Failed processor task: {}", task.getClass().getName(), e);
            }
        }

        @Override
        public Map<String, String> getSubmitterMdc() {
            return mdc;
        }
    } // End of class CallableProcessorTask

    /**
     * Gets the dummy caller-runs processor.
     *
     * @return The caller-runs processor
     */
    public static Processor getCallerRunsProcessor() {
        return CallerRunsProcessor.getInstance();
    }

    private static class CallerRunsProcessor implements Processor {

        private static final CallerRunsProcessor INSTANCE = new CallerRunsProcessor();

        static CallerRunsProcessor getInstance() {
            return INSTANCE;
        }

        @Override
        public String getName() {
            return "CallerRunsProcessor";
        }

        @Override
        public boolean execute(Object optKey, ProcessorTask task) {
            task.run();
            return true;
        }

        @Override
        public long getNumberOfBufferedTasks() throws OXException {
            return 0;
        }

        @Override
        public long getNumberOfExecutingTasks() throws OXException {
            return 0;
        }

        @Override
        public void stopWhenEmpty() throws InterruptedException {
            // Nothing
        }

        @Override
        public void stop() {
            // Nothing
        }
    } // End of class CallerRunsProcessor

}
