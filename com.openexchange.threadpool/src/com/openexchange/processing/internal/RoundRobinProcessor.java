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

package com.openexchange.processing.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.MDC;
import com.openexchange.exception.OXException;
import com.openexchange.processing.Processor;
import com.openexchange.processing.ProcessorTask;
import com.openexchange.processing.internal.watcher.ProcessorTaskInfo;
import com.openexchange.processing.internal.watcher.ProcessorTaskWatcher;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.timer.TimerService;

/**
 * {@link RoundRobinProcessor} - A processor that manages its tasks using round-robin behavior.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.8.1
 */
public class RoundRobinProcessor implements Processor {

    /** The logger */
    static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RoundRobinProcessor.class);

    private final String name;
    private final ExecutorService pool;
    private final int numThreads;
    final BlockingDeque<TaskManager> roundRobinQueue;
    final Map<TaskKey, TaskManager> taskManagers;
    final AtomicLong numberOfExecutingTasks;
    final AtomicInteger numberOfActiveSelectors;
    final AtomicBoolean stopped;

    /**
     * Initializes a new {@link RoundRobinProcessor}.
     */
    public RoundRobinProcessor(String name, int numThreads) {
        super();
        if (numThreads <= 0) {
            throw new IllegalArgumentException("numThreads must not be equal to/less than zero");
        }
        this.numThreads = numThreads;
        this.name = name;

        // Initialize fixed thread pool
        ProcessorThreadPoolExecutor newPool = new ProcessorThreadPoolExecutor(name, numThreads, false);
        newPool.prestartAllCoreThreads();
        pool = newPool;
        taskManagers = new HashMap<>(256);
        roundRobinQueue = new LinkedBlockingDeque<TaskManager>();
        numberOfExecutingTasks = new AtomicLong(0);

        // Start selector threads
        stopped = new AtomicBoolean(false);
        numberOfActiveSelectors = new AtomicInteger();
        for (int i = numThreads; i-- > 0;) {
            newPool.execute(new Selector());
            numberOfActiveSelectors.incrementAndGet();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getNumberOfBufferedTasks() throws OXException {
        synchronized (taskManagers) {
            long count = 0;
            for (TaskManager taskManager : taskManagers.values()) {
                count+= taskManager.size();
            }
            return count;
        }
    }

    @Override
    public long getNumberOfExecutingTasks() throws OXException {
        return numberOfExecutingTasks.get();
    }

    /**
     * Checks whether to consider task managers map as empty.
     * <p>
     * Must only be accessed synchronized.
     *
     * @return <code>true</code> if empty; otherwise <code>false</code>
     */
    protected boolean considerTaskManagersEmpty() {
        if (taskManagers.isEmpty()) {
            return true;
        }

        for (TaskManager taskManager : taskManagers.values()) {
            if (false == taskManager.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the next task from specified task manager
     *
     * @param manager The task manager
     * @return The next task or <code>null</code>
     */
    protected ProcessorTask getNextTaskFrom(TaskManager manager) {
        return manager.remove();
    }

    /**
     * Checks whether specified task is allowed to be added to this processor.
     *
     * @param task The task to check
     * @return <code>true</code> if allowed/granted; otherwise <code>false</code>
     */
    protected boolean allowNewTask(ProcessorTask task) {
        return true;
    }

    /**
     * Handles if a task could not be offered to processor
     *
     * @param task The task that could not be offered
     */
    protected void handleFailedTaskOffer(ProcessorTask task) {
        // Nothing
    }

    /**
     * Checks if a new <code>Selector</code> is supposed to be created
     *
     * @return <code>true</code> if a new <code>Selector</code> is supposed to be created; otherwise <code>false</code>
     * @throws RejectedExecutionException If the possibly needed <code>Selector</code> cannot be accepted for execution
     */
    protected void scheduleNewSelectorIfNeeded() {
        // Check number of currently running Selector instances
        int num;
        do {
            num = numberOfActiveSelectors.get();
            if (num >= numThreads) {
                return;
            }
        } while (!numberOfActiveSelectors.compareAndSet(num, num + 1));

        // Start a new Selector
        pool.execute(new Selector());
    }

    private void haltThreadsAndShutDownPool() {
        for (int i = numThreads; i-- > 0;) {
            roundRobinQueue.offerFirst(TaskManager.POISON);
        }

        try {
            pool.shutdownNow();
        } catch (Exception x) {
            // Ignore
        }
    }

    @Override
    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            // Already stopped
            return;
        }

        haltThreadsAndShutDownPool();
    }

    @Override
    public void stopWhenEmpty() throws InterruptedException {
        if (!stopped.compareAndSet(false, true)) {
            // Already stopped
            return;
        }

        synchronized (taskManagers) {
            while (false == considerTaskManagersEmpty()) {
                taskManagers.wait();
            }
        }

        haltThreadsAndShutDownPool();
    }

    @Override
    public boolean execute(Object optKey, ProcessorTask task) {
        if (stopped.get()) {
            return false;
        }

        // Acquire grant
        if (!allowNewTask(task)) {
            return false;
        }

        // Check number of currently running Selector instances
        try {
            scheduleNewSelectorIfNeeded();
        } catch (RejectedExecutionException x) {
            handleFailedTaskOffer(task);
            return false;
        }

        // Determine the key to use
        TaskKey key = new TaskKey(null == optKey ? Thread.currentThread() : optKey);

        // Schedule task
        TaskManager newManager = null;
        synchronized (taskManagers) {
            // Stopped meanwhile...?
            if (stopped.get()) {
                handleFailedTaskOffer(task);
                return false;
            }

            // Add task to either new or existing TaskManager instance
            TaskManager existingManager = taskManagers.get(key);
            try {
                if (existingManager == null) {
                    // None present, yet. Create a new executer.
                    newManager = new DefaultTaskManager(task, key);
                    taskManagers.put(key, newManager);
                } else {
                    // Use existing one
                    existingManager.add(task);
                }
            } catch (RuntimeException e) {
                // Adding to manager failed
                handleFailedTaskOffer(task);
                throw e;
            }
        }

        // Add to round-robin queue in case task manager was newly created
        if (null != newManager) {
            try {
                roundRobinQueue.offerLast(newManager);
            } catch (RuntimeException e) {
                // Adding to manager failed
                handleFailedTaskOffer(task);
                throw e;
            }
        }

        // Successfully added task
        return true;
    }

    // ----------------------------------------------------------------------------------------------- //

    private final class SelectorAdder implements Runnable {

        SelectorAdder() {
            super();
        }

        @Override
        public void run() {
            try {
                // Check number of currently running Selector instances
                scheduleNewSelectorIfNeeded();
            } catch (Exception e) {
                LOGGER.warn("Failed to accept new Selector for execution", e);
            }
        }
    }

    /**
     * The Selector waiting for incoming processing tasks.
     */
    public final class Selector implements Runnable {

        Selector() {
            super();
        }

        @Override
        public void run() {
            // Remember associated worker thread
            Thread currentThread = Thread.currentThread();

            if (stopped.get()) {
                // Stopped...
                LOGGER.info("Processor selector '{}' terminated", currentThread.getName());
                return;
            }

            boolean decrementCount = true;
            try {
                // Perform processing until aborted
                boolean proceed = true;
                while (proceed) {
                    try {
                        // Await next slot
                        TaskManager manager = roundRobinQueue.takeFirst();

                        // Check slot for POISON
                        if (TaskManager.POISON == manager) {
                            // Poisoned...
                            LOGGER.info("Processor selector '{}' terminated", currentThread.getName());
                            return;
                        }

                        // Acquire next task from slot
                        ProcessorTask task;
                        boolean addToQueue = true;
                        synchronized (taskManagers) {
                            task = getNextTaskFrom(manager);
                            if (null == task || manager.isEmpty()) {
                                // TaskManager has no next task. Or it is empty.
                                taskManagers.remove(manager.getExecuterKey());
                                addToQueue = false;
                            }
                            taskManagers.notifyAll();
                        }

                        // Check task
                        if (null != task) {
                            // Re-add slot to round-robin queue for next processing (if supposed to do so)
                            if (addToQueue) {
                                roundRobinQueue.offerLast(manager);
                            }

                            // Perform task
                            numberOfExecutingTasks.incrementAndGet();
                            try {
                                executeTask(task);
                            } finally {
                                numberOfExecutingTasks.decrementAndGet();
                            }

                            // Check (& possibly clear) interrupted status after task execution
                            if (Thread.interrupted()) {
                                // Check status
                                if (stopped.get()) {
                                    // Stopped...
                                    LOGGER.info("Processor selector '{}' terminated", currentThread.getName());
                                    return;
                                }

                                // Otherwise orderly terminate this Selector & re-schedule another Selector
                                proceed = false;
                                LOGGER.info("Processor selector '{}' terminated. Going to schedule a new selector for further processing.", currentThread.getName());

                                // Ensure counter is decremented prior to one-shot task becoming active
                                numberOfActiveSelectors.decrementAndGet();
                                decrementCount = false;

                                TimerService optService = ThreadPools.getTimerService();
                                if (null != optService) {
                                    optService.schedule(new SelectorAdder(), 250, TimeUnit.MILLISECONDS);
                                }

                                // Leave...
                                return;
                            }
                        }

                        // Check status
                        if (stopped.get()) {
                            // Stopped...
                            LOGGER.info("Processor selector '{}' terminated", currentThread.getName());
                            return;
                        }
                    } catch (InterruptedException e) {
                        // Handle in outer try-catch clause
                        throw e;
                    } catch (Exception | StackOverflowError e) {
                        LOGGER.info("Processing failed.", e);
                    }
                }
            } catch (InterruptedException e) {
                // Keep interrupted status
                currentThread.interrupt();
                LOGGER.info("Processor selector '{}' interrupted", currentThread.getName(), e);
            } finally {
                // Decrement count
                if (decrementCount) {
                    numberOfActiveSelectors.decrementAndGet();
                }
            }

            LOGGER.info("Processor selector '{}' terminated", currentThread.getName());
        }

        private void executeTask(ProcessorTask task) {
            Map<String, String> mdc = task.getSubmitterMdc();
            if (mdc == null || mdc.isEmpty()) {
                run(task);
                return;
            }

            for (Map.Entry<String, String> mdcEntry : mdc.entrySet()) {
                MDC.put(mdcEntry.getKey(), mdcEntry.getValue());
            }
            try {
                run(task);
            } finally {
                MDC.clear();
            }
        }

        private void run(ProcessorTask task) {
            long timeoutMillis = task.getTimeoutMillis();
            if (timeoutMillis <= 0) {
                // Timeout is less than or equal to 0 (zero). Hence, no watching needed.
                task.run();
                return;
            }

            // Add to watcher & run task
            ProcessorTaskInfo info = new ProcessorTaskInfo(Thread.currentThread(), timeoutMillis);
            ProcessorTaskWatcher.getInstance().add(info);
            try {
                task.run();
            } finally {
                ProcessorTaskWatcher.getInstance().remove(info);
            }
        }
    } // End of class Selector

}
