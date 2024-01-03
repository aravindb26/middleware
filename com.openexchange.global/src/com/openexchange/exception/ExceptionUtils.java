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

package com.openexchange.exception;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import com.openexchange.java.Strings;
import com.openexchange.java.util.Pair;
import com.openexchange.log.LogProperties;

/**
 * Utilities for handling <tt>Throwable</tt>s and <tt>Exception</tt>s.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class ExceptionUtils {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ExceptionUtils.class);

    /**
     * Initializes a new {@link ExceptionUtils}.
     */
    private ExceptionUtils() {
        super();
    }

    private static final Consumer<InterruptedException> DEFAULT_INTERRUPTED_HANDLER = e -> {
        UncheckedInterruptedException unchecked = UncheckedInterruptedException.restoreInterruptedStatusAndRethrow(e);
        if (unchecked == null) {
            return;
        }
        throw unchecked;
    };

    /**
     * Checks whether given <tt>Exception</tt> is one that needs to be re-thrown.
     * <p>
     * Useful for typical constructs that would "<i>swallow</i>" exception generically:
     * <pre>
     *   try {
     *     ...
     *   } catch (Exception e) {
     *     ExceptionUtils.handleException(e); // Re-throws critical ones
     *     LOGGER.error("Failed to ...", e);
     *   }
     * </pre>
     *
     * @param e The <tt>Exception</tt> to check
     */
    public static void handleException(Exception e) {
        handleException(e, DEFAULT_INTERRUPTED_HANDLER);
    }

    /**
     * Checks whether given <tt>Exception</tt> is one that needs to be re-thrown.
     * <p>
     * Useful for typical constructs that would "<i>swallow</i>" exception generically:
     * <pre>
     *   try {
     *     ...
     *   } catch (Exception e) {
     *     ExceptionUtils.handleException(e); // Re-throws critical ones
     *     LOGGER.error("Failed to ...", e);
     *   }
     * </pre>
     *
     * @param e The <tt>Exception</tt> to check
     */
    public static void handleException(Exception e, Consumer<InterruptedException> interruptedExceptionHandler) {
        handleThrowable(e);
        if (e instanceof InterruptedException) {
            // Apparently thread has been interrupted.
            interruptedExceptionHandler.accept((InterruptedException) e);
        }
    }

    private static final String MARKER = " ---=== /!\\ ===--- ";

    /**
     * Checks whether the supplied <tt>Throwable</tt> is one that needs to be re-thrown and swallows all others.
     *
     * @param t The <tt>Throwable</tt> to check
     */
    public static void handleThrowable(final Throwable t) {
        if (t instanceof ThreadDeath) {
            logError("Thread death", t);
            throw (ThreadDeath) t;
        }
        if (t instanceof OutOfMemoryError) {
            OutOfMemoryError oom = (OutOfMemoryError) t;
            handleOOM(oom);
            throw oom;
        }
        if (t instanceof VirtualMachineError) {
            logVirtualMachineError(t);
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }

    /**
     * Handles given OutOfMemoryError instance by writing out thread/heap dump if appropriate
     * and placing a prominent log entry.
     * <p>
     * <b><i>Does not re-throw given OutOfMemoryError instance</i></b>
     *
     * @param oom The OutOfMemoryError instance
     */
    public static void handleOOM(final OutOfMemoryError oom) {
        LAST_OOME_REFERENCE.set(new Date());
        String message = oom.getMessage();
        if ("unable to create new native thread".equalsIgnoreCase(message)) {
            if (null == System.getProperties().put("__thread_dump_created", Boolean.TRUE)) {
                boolean error = true;
                try {
                    StringBuilder sb = new StringBuilder(2048);
                    // Dump all the threads to the log
                    Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
                    String ls = Strings.getLineSeparator();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss,SSSZ", Locale.US);
                    String date = dateFormat.format(new Date());
                    sb.append("------ BEGIN THREAD DUMP (").append(date).append(", ").append(threads.size()).append(" threads) ------").append(ls);
                    for (Map.Entry<Thread, StackTraceElement[]> mapEntry : threads.entrySet()) {
                        Thread thread = mapEntry.getKey();
                        sb.append(thread).append(": ").append(thread.getState().name()).append(ls);
                        for (StackTraceElement elem : mapEntry.getValue()) {
                            sb.append('\t').append(elem).append(ls);
                        }
                    }
                    sb.append("------ END THREAD DUMP (").append(date).append(", ").append(threads.size()).append(" threads) ------").append(ls);
                    System.err.print(sb.toString());
                    sb.setLength(0);
                    sb = null; // Might help GC
                    LOG.info("{}    Thread dump written to stderr{}", ls, ls);
                    error = false;
                } finally {
                    if (error) {
                        System.getProperties().remove("__thread_dump_created");
                    }
                }
            }
        } else if ("Java heap space".equalsIgnoreCase(message)) {
            createHeapDump(false);
        }
        logVirtualMachineError(oom);
    }

    /**
     * Creates a heap dump (if <code>"-XX:+HeapDumpOnOutOfMemoryError"</code> JVM option is not already specified)
     *
     * @param force <code>true</code> to create a heap dump even if <code>"-XX:+HeapDumpOnOutOfMemoryError"</code> JVM option is given; otherwise <code>false</code> to only create if that option is absent
     */
    public static void createHeapDump(boolean force) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();

            Pair<Boolean, String> heapDumpArgs = checkHeapDumpArguments();

            // Forced or is HeapDumpOnOutOfMemoryError enabled?
            if ((force || !heapDumpArgs.getFirst().booleanValue()) && null == System.getProperties().put("__heap_dump_created", Boolean.TRUE)) {
                boolean error = true;
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss", Locale.US);
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    // Either "/tmp" or path configured through "-XX:HeapDumpPath" JVM argument
                    String path = null == heapDumpArgs.getSecond() ? "/tmp" : heapDumpArgs.getSecond();
                    String fn = path + "/" + dateFormat.format(new Date()) + "-heap.hprof";
                    String mbeanName = "com.sun.management:type=HotSpotDiagnostic";
                    server.invoke(new ObjectName(mbeanName), "dumpHeap", new Object[] { fn, Boolean.TRUE }, new String[] { String.class.getCanonicalName(), "boolean" });
                    LOG.info("{}    Heap snapshot dumped to file {}{}", Strings.getLineSeparator(), fn, Strings.getLineSeparator());
                    error = false;
                } finally {
                    if (error) {
                        System.getProperties().remove("__heap_dump_created");
                    }
                }
            }
        } catch (Exception e) {
            // Failed for any reason...
        }
    }

    private static void logVirtualMachineError(final Throwable t) {
        logError("The Java Virtual Machine is broken or has run out of resources necessary for it to continue operating.", t);
    }

    private static void logError(String message, Throwable t) {
        Map<String, String> taskProperties = LogProperties.getPropertyMap();
        if (null == taskProperties) {
            LOG.error("{}{}{}", MARKER, message, MARKER, t);
        } else {
            Map<String, String> sorted = new TreeMap<String, String>();
            for (Map.Entry<String, String> entry : taskProperties.entrySet()) {
                String propertyName = entry.getKey();
                String value = entry.getValue();
                if (null != value) {
                    sorted.put(propertyName, value);
                }
            }

            List<Object> args = new ArrayList<>(sorted.size() << 1);
            StringBuilder logBuilder = new StringBuilder(512);
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                if (logBuilder.length() > 0) {
                    logBuilder.append("{}{}={}");
                    args.add(Strings.getLineSeparator());
                    args.add(entry.getKey());
                    args.add(entry.getValue());
                } else {
                    logBuilder.append("{}={}");
                    args.add(entry.getKey());
                    args.add(entry.getValue());
                }
            }
            logBuilder.append("{}{}");
            args.add(Strings.getLineSeparator());
            args.add(Strings.getLineSeparator());

            logBuilder.append("{}{}{}");
            args.add(MARKER);
            args.add(message);
            args.add(MARKER);
            args.add(t);
            LOG.error(logBuilder.toString(), args.toArray(new Object[args.size()]));
        }
    }

    private static Pair<Boolean, String> checkHeapDumpArguments() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        boolean heapDumpOnOOm = false;
        String path = null;
        for (String argument : arguments) {
            if ("-XX:+HeapDumpOnOutOfMemoryError".equals(argument)) {
                heapDumpOnOOm = true;
            } else if (argument.startsWith("-XX:HeapDumpPath=")) {
                path = argument.substring(17).trim();
                File file = new File(path);
                if (!file.exists() || !file.canWrite()) {
                    path = null;
                }
            }
        }
        return new Pair<Boolean, String>(Boolean.valueOf(heapDumpOnOOm), path);
    }

    /**
     * Checks if the exception class occurs in exception chain of given {@link Throwable} instance.
     *
     * @param e The {@link Throwable} instance whose exception chain is supposed to be checked
     * @param clazz The exception class to check for
     * @return <code>true</code> if the exception class occurs in exception chain; otherwise <code>false</code>
     */
    public static boolean isEitherOf(Throwable e, Class<? extends Throwable> clazz) {
        if (null == e || null == clazz) {
            return false;
        }

        if (clazz.isInstance(e)) {
            return true;
        }

        Throwable next = e.getCause();
        return null == next ? false : isEitherOf(next, clazz);
    }

    /**
     * Checks if any of specified exception (classes) occurs in exception chain of given {@link Throwable} instance.
     *
     * @param e The {@link Throwable} instance whose exception chain is supposed to be checked
     * @param classes The exception classes
     * @return <code>true</code> if any of specified exception (classes) occurs in exception chain; otherwise <code>false</code>
     */
    @SafeVarargs
    public static boolean isEitherOf(Throwable e, Class<? extends Throwable>... classes) {
        if (null == e || null == classes || 0 == classes.length) {
            return false;
        }

        for (Class<? extends Throwable> clazz : classes) {
            if (clazz.isInstance(e)) {
                return true;
            }
        }

        Throwable next = e.getCause();
        return null == next ? false : isEitherOf(next, classes);
    }

    /**
     * Checks if any of specified exception (classes) occurs in exception chain of given {@link Throwable} instance.
     *
     * @param e The {@link Throwable} instance whose exception chain is supposed to be checked
     * @param classes The exception classes
     * @return <code>true</code> if any of specified exception (classes) occurs in exception chain; otherwise <code>false</code>
     */
    public static boolean isEitherOf(Throwable e, Collection<Class<? extends Throwable>> classes) {
        if (null == e || null == classes || classes.isEmpty()) {
            return false;
        }

        for (Class<? extends Throwable> clazz : classes) {
            if (clazz.isInstance(e)) {
                return true;
            }
        }

        Throwable next = e.getCause();
        return null == next ? false : isEitherOf(next, classes);
    }

    /**
     * Extracts the first occurrence of given exception class from exception chain of given {@link Throwable} instance.
     *
     * @param e The {@link Throwable} instance whose exception chain is supposed to be traversed
     * @param clazz The exception class to look-up
     * @return The first occurrence or <code>null</code>
     */
    public static <E extends Throwable> E extractFrom(Throwable e, Class<E> clazz) {
        if (null == e || null == clazz) {
            return null;
        }

        if (clazz.isInstance(e)) {
            @SuppressWarnings("unchecked") E exc = (E) e;
            return exc;
        }

        Throwable next = e.getCause();
        return null == next ? null : extractFrom(next, clazz);
    }

    private static final AtomicReference<Date> LAST_OOME_REFERENCE = new AtomicReference<>();

    /**
     * Gets the date when the last <code>OutOfMemoryError</code> occurred.
     *
     * @return The date of last OOME occurrence or <code>null</code>
     */
    public static Date getLastOOM() {
        return LAST_OOME_REFERENCE.get();
    }

    /**
     * Gets the last instance of <code>Throwable</code> from the exception chain of given <code>Throwable</code> instance.
     *
     * @param t The <code>Throwable</code> instance to get last chained instance from
     * @return The last chained <code>Throwable</code> instance
     */
    public static Throwable getLastChainedThrowable(Throwable t) {
        Throwable cause = t == null? null : t.getCause();
        return cause == null ? t : getLastChainedThrowable(cause);
    }

    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    /**
     * Drops the stack trace for given <code>Throwable</code> instance.
     *
     * @param t The <code>Throwable</code> instance for which the stack trace shall be emptied
     * @return The given <code>Throwable</code> instance with empty stack trace
     */
    public static Throwable dropStackTraceFor(Throwable t) {
        if (null != t) {
            // Drop our stack trace
            t.setStackTrace(EMPTY_STACK_TRACE);

            // Drop stack traces for suppressed exceptions, if any
            Throwable[] suppressedOnes = t.getSuppressed();
            if (null != suppressedOnes && suppressedOnes.length > 0) {
                for (Throwable suppressed : suppressedOnes) {
                    dropStackTraceFor(suppressed);
                }
            }

            // Drop stack traces for cause, if any
            dropStackTraceFor(t.getCause());
        }
        return t;
    }

}
