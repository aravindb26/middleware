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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.MDC;
import com.openexchange.exception.OXException;
import com.openexchange.processing.Processor;
import com.openexchange.processing.ProcessorTask;
import com.openexchange.processing.internal.watcher.ProcessorTaskInfo;
import com.openexchange.processing.internal.watcher.ProcessorTaskWatcher;


/**
 * {@link StripedProcessor} - A processor that also divides tasks to execute in slots to prevent from a single source obtaining all
 * available threads at once.
 * <p>
 * In contrast to <code>RoundRobinProcessor</code> this processor supports a core and max. pool size while preferring to use more threads
 * up to max. pool size rather than putting tasks into <code>ThreadPoolExecutor</code>'s queue;<br>
 * see <code>IncreaseThreadsBeforeQueueingThreadPoolExecutor</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class StripedProcessor implements Processor {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(StripedProcessor.class);

    /** Special task instance signaling abortion for queue consumer thread */
    private static final ProcessorTask POISON_TASK = new ProcessorTask() {

        @Override
        public void run() {
            // Nothing
        }

        @Override
        public Map<String, String> getSubmitterMdc() {
            return null;
        }
    };

    private final int maxTasks;
    private final Lock lock;
    private final String name;
    private final ExecutorService pool;
    private final BlockingDeque<TaskManager> roundRobinQueue;
    private final Map<TaskKey, TaskManager> taskManagers;
    private final AtomicBoolean stopped;
    private final AtomicInteger numberOfExecutingTasks;
    private final AtomicInteger numberOfPendingTasks;
    private Thread queueConsumer;

    /**
     * Initializes a new {@link StripedProcessor}.
     *
     * @param name The name of this processor
     * @param numThreads The core number of threads
     * @param maxNumThreads The maximum number of threads
     * @param maxTasks The maximum number of tasks or <code>-1</code>
     */
    public StripedProcessor(String name, int numThreads, int maxNumThreads, int maxTasks) {
        super();
        if (numThreads <= 0) {
            throw new IllegalArgumentException("numThreads must not be equal to/less than zero");
        }
        if (maxNumThreads <= 0) {
            throw new IllegalArgumentException("maxNumThreads must not be equal to/less than zero");
        }
        if (maxNumThreads < numThreads) {
            throw new IllegalArgumentException("maxNumThreads must not be less than numThreads");
        }
        this.name = name;
        this.maxTasks = maxTasks;
        this.lock = new ReentrantLock();
        this.pool = new IncreaseThreadsBeforeQueueingThreadPoolExecutor(name, numThreads, maxNumThreads, false);
        this.taskManagers = new HashMap<>(256);
        this.roundRobinQueue = new LinkedBlockingDeque<TaskManager>();
        this.numberOfExecutingTasks = new AtomicInteger(0);
        this.numberOfPendingTasks = new AtomicInteger(0);
        this.stopped = new AtomicBoolean(false);
    }

    /**
     * Gets the thread pool.
     *
     * @return The thread pool
     */
    public ExecutorService getPool() {
        return pool;
    }

    @Override
    public String getName() {
        return name;
    }

    private void startCheckerIfNeeded() { // Only invoked if lock is held
        if (queueConsumer != null) {
            // Nothing to do
            return;
        }

        Thread newConsumer = new Thread(new QueueConsumer(60_000L, this), name + "QueueConsumer");
        newConsumer.start();
        this.queueConsumer = newConsumer;
    }

    private void stopChecker(boolean offerPoison) { // Only invoked if lock is held
        if (queueConsumer == null) {
            // Nothing to do
            return;
        }

        if (offerPoison) {
            roundRobinQueue.offerFirst(TaskManager.POISON);
        }
        queueConsumer.interrupt();
        queueConsumer = null;
    }

    /**
     * Submits given task for being executed.
     *
     * @param task The task
     */
    public void submitTaskForExecution(ProcessorTask task) {
        pool.execute(new WrappedProcessorTask(task, numberOfExecutingTasks, numberOfPendingTasks, name));
    }

    /**
     * Awaits next available processor task.
     *
     * @param waitMillis The time in milliseconds to wait for a next task to become available
     * @return The next task to execute, <code>null</code> if there is none or {@link #POISON_TASK} if aborted
     */
    public ProcessorTask awaitProcessorTask(long waitMillis) {
        // Await slot
        TaskManager manager;
        try {
            manager = roundRobinQueue.pollFirst(waitMillis, TimeUnit.MILLISECONDS);
            if (TaskManager.POISON == manager) {
                // Likely by calling stopChecker().
                return POISON_TASK;
            }
        } catch (InterruptedException e) {
            // Interrupted
            return POISON_TASK;
        }

        // Acquire task...
        AcquireResult acquireResult;
        if (manager == null || (acquireResult = acquireNextTaskFrom(manager)) == null) {
            // Timed out while waiting or task manager is empty
            lock.lock();
            try {
                if (roundRobinQueue.isEmpty()) {
                    // No more elements in queue. Stop checker thread.
                    stopChecker(false);
                    return POISON_TASK;
                }
            } finally {
                lock.unlock();
            }
            return null;
        }

        // Re-add slot to round-robin queue for next processing (if supposed to do so)
        if (acquireResult.addToQueue()) {
            roundRobinQueue.offerLast(manager);
        }

        // Return task
        return acquireResult.processorTask();
    }

    private AcquireResult acquireNextTaskFrom(TaskManager manager) {
        boolean addToQueue = true;
        synchronized (taskManagers) {
            ProcessorTask task = manager.remove();
            if (null == task || manager.isEmpty()) {
                // TaskManager has no next task. Or it is empty.
                taskManagers.remove(manager.getExecuterKey());
                addToQueue = false;
            }
            return task == null ? null : new AcquireResult(task, addToQueue);
        }
    }

    @Override
    public boolean execute(Object optKey, ProcessorTask task) {
        // Check against max. number of tasks
        if (maxTasks > 0) {
            int numPendingTasks;
            do {
                numPendingTasks = numberOfPendingTasks.get();
                if (numPendingTasks >= maxTasks) {
                    return false;
                }
            } while (numberOfPendingTasks.compareAndSet(numPendingTasks, numPendingTasks + 1) == false);
        } else {
            numberOfPendingTasks.incrementAndGet();
        }

        // Schedule task
        TaskManager newManager = null;
        synchronized (taskManagers) {
            // Add task to appropriate slot
            boolean error = true;
            try {
                // Stopped meanwhile...?
                if (stopped.get()) {
                    return false;
                }

                // Determine the key to use
                TaskKey key = new TaskKey(null == optKey ? Thread.currentThread() : optKey);

                // Add task to either new or existing TaskManager instance
                TaskManager existingManager = taskManagers.get(key);
                if (existingManager == null) {
                    // None present, yet. Create a new executer.
                    newManager = new DefaultTaskManager(task, key);
                    taskManagers.put(key, newManager);
                } else {
                    // Use existing one
                    existingManager.add(task);
                }

                error = false;
            } finally {
                // Adding to slot failed
                if (error && maxTasks > 0) {
                    numberOfPendingTasks.decrementAndGet();
                }
            }
        }

        // Add to round-robin queue in case task manager was newly created
        if (null != newManager) {
            lock.lock();
            try {
                roundRobinQueue.offerLast(newManager);
                startCheckerIfNeeded();
            } catch (RuntimeException e) {
                // Adding to queue failed
                throw e;
            } finally {
                lock.unlock();
            }
        }

        // Successfully added task
        return true;
    }

    @Override
    public long getNumberOfBufferedTasks() throws OXException {
        return numberOfPendingTasks.get();
    }

    @Override
    public long getNumberOfExecutingTasks() throws OXException {
        return numberOfExecutingTasks.get();
    }

    @Override
    public void stopWhenEmpty() throws InterruptedException {
        stopped.set(true);
        while (roundRobinQueue.isEmpty() == false) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
        lock.lock();
        try {
            stopChecker(true);
        } finally {
            lock.unlock();
        }
        try {
            pool.shutdownNow();
        } catch (Exception x) {
            // Ignore
        }
    }

    @Override
    public void stop() {
        stopped.set(true);
        lock.lock();
        try {
            stopChecker(true);
        } finally {
            lock.unlock();
        }
        try {
            pool.shutdownNow();
        } catch (Exception x) {
            // Ignore
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * {@link QueueConsumer} - Responsible for obtaining new tasks from queue and submitting them into thread pool.
     */
    private static final class QueueConsumer implements Runnable {

        private final StripedProcessor stripedProcessor;
        private final long waitMillis;

        /**
         * Initializes a new {@link QueueConsumer}.
         *
         * @param waitMillis The time in milliseconds to wait for new tasks
         * @param stripedProcessor The associated processor
         */
        QueueConsumer(long waitMillis, StripedProcessor stripedProcessor) {
            super();
            this.stripedProcessor = stripedProcessor;
            this.waitMillis = waitMillis;
        }

        @Override
        public void run() {
            try {
                LOG.info("Started queue consumer for {}", stripedProcessor.getName());
                while (true) {
                    ProcessorTask task = stripedProcessor.awaitProcessorTask(waitMillis);
                    if (task == POISON_TASK) {
                        LOG.info("Stopped queue consumer for {}", stripedProcessor.getName());
                        return;
                    }

                    // Otherwise submit task for execution (if not null)
                    if (task != null) {
                        stripedProcessor.submitTaskForExecution(task);
                    }
                }
            } catch (Exception e) {
                LOG.error("Aborted queue consumer for {}", stripedProcessor.getName(), e);
            }
        }
    }

    private static final class WrappedProcessorTask implements Runnable {

        private final ProcessorTask task;
        private final AtomicInteger numberOfExecutingTasks;
        private final AtomicInteger numberOfPendingTasks;
        private final String name;

        /**
         * Initializes a new {@link WrappedProcessorTask}.
         *
         * @param task The wrapped task
         * @param numberOfExecutingTasks The counter for currently executed tasks
         * @param numberOfPendingTasks The counter for currently pending/queued tasks
         */
        WrappedProcessorTask(ProcessorTask task, AtomicInteger numberOfExecutingTasks, AtomicInteger numberOfPendingTasks, String name) {
            super();
            this.task = task;
            this.numberOfExecutingTasks = numberOfExecutingTasks;
            this.numberOfPendingTasks = numberOfPendingTasks;
            this.name = name;
        }

        @Override
        public void run() {
            // Perform task
            numberOfPendingTasks.decrementAndGet();
            numberOfExecutingTasks.incrementAndGet();
            try {
                executeTask();
            } finally {
                numberOfExecutingTasks.decrementAndGet();
            }
        }

        private void executeTask() {
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
    }

    private static record AcquireResult(ProcessorTask processorTask, boolean addToQueue) {
        // Nothing
    }

}
