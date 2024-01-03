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

package com.openexchange.http.requestwatcher.internal;

import static com.eaio.util.text.HumanTime.exactly;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.http.requestwatcher.osgi.services.RequestRegistryEntry;
import com.openexchange.http.requestwatcher.osgi.services.RequestTrace;
import com.openexchange.http.requestwatcher.osgi.services.RequestWatcherService;
import com.openexchange.java.Strings;
import com.openexchange.log.LogProperties;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessiond.SessiondServiceExtended;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link RequestWatcherServiceImpl}
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RequestWatcherServiceImpl implements RequestWatcherService {

    /** The logger. */
    protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RequestWatcherServiceImpl.class);

    /** The request number */
    private static final AtomicLong NUMBER = new AtomicLong();

    /** The attribute name for Grizzly's request number */
    private static final String ATTRIBUTE_REQUEST_NUMBER = "grizzly.reqnum";

    // --------------------------------------------------------------------------------------------------------------------------

    /** Navigable set, entries ordered by age (youngest first), weakly consistent iterator */
    private final ConcurrentSkipListSet<RequestRegistryEntry> requestRegistry;

    /** The watcher task */
    private final AtomicReference<ScheduledTimerTask> requestWatcherTask;

    /**
     * Initializes a new {@link RequestWatcherServiceImpl}
     *
     * @param configService The configuration service used for initialization
     * @param timerService The timer service used for initialization
     * @throws OXException If initialization fails
     */
    public RequestWatcherServiceImpl(ConfigurationService configService, TimerService timerService) throws OXException {
        super();
        // Create set
        ConcurrentSkipListSet<RequestRegistryEntry> requestRegistry = new ConcurrentSkipListSet<RequestRegistryEntry>();
        this.requestRegistry = requestRegistry;

        // Create ScheduledTimerTask to watch requests
        int watcherFrequency = configService.getIntProperty("com.openexchange.requestwatcher.frequency", 30_000);
        int requestMaxAge = configService.getIntProperty("com.openexchange.requestwatcher.maxRequestAge", 60_000);
        int requestExpiredAge = configService.getIntProperty("com.openexchange.requestwatcher.expiredRequestAge", 360_000);
        int downloadUploadExpiredAge = configService.getIntProperty("com.openexchange.requestwatcher.expiredDownloadUploadAge", 43_200_000);
        int interruptedThreshold = configService.getIntProperty("com.openexchange.requestwatcher.interruptedThreshold", 1);
        if (requestMaxAge > requestExpiredAge) {
            throw OXException.general("Request expiration age (" + requestExpiredAge + ") must not be less than request max. age (" + requestMaxAge + ")");
        }
        if (requestMaxAge > downloadUploadExpiredAge) {
            throw OXException.general("Request expiration age for down-/uploads (" + downloadUploadExpiredAge + ") must not be less than request max. age (" + requestMaxAge + ")");
        }
        if (interruptedThreshold < 0) {
            interruptedThreshold = 0;
        }
        Watcher task = new Watcher(this.requestRegistry, requestMaxAge, requestExpiredAge, downloadUploadExpiredAge, interruptedThreshold);
        ScheduledTimerTask requestWatcherTask = timerService.scheduleAtFixedRate(task, requestMaxAge, watcherFrequency);
        this.requestWatcherTask = new AtomicReference<>(requestWatcherTask);
    }

    @Override
    public RequestRegistryEntry registerRequest(HttpServletRequest request, HttpServletResponse response, Thread thread, Map<String, String> propertyMap) {
        RequestRegistryEntry registryEntry = new RequestRegistryEntry(getRequestNumber(request), request, thread, propertyMap);
        requestRegistry.add(registryEntry);
        return registryEntry;
    }

    /**
     * Gets the request number for given HTTP request.
     *
     * @param request The HTTP request
     * @return The request number
     */
    private static long getRequestNumber(HttpServletRequest request) {
        Long requestNumber = (Long) request.getAttribute(ATTRIBUTE_REQUEST_NUMBER);
        return requestNumber == null ? NUMBER.incrementAndGet() : requestNumber.longValue();
    }

    @Override
    public boolean unregisterRequest(RequestRegistryEntry registryEntry) {
        return requestRegistry.remove(registryEntry);
    }

    @Override
    public boolean stopWatching() {
        ScheduledTimerTask requestWatcherTask = this.requestWatcherTask.getAndSet(null);
        return null == requestWatcherTask || requestWatcherTask.cancel();
    }

    // ----------------------------------------------------------------------------------------------------------------------- //

    /** The result when handling an entry */
    private static enum Result {

        /**
         * Exceeded entry has been logged.
         */
        LOGGED,
        /**
         * Expired entry has been interrupted.
         */
        INTERRUPTED,
        /**
         * Expired entry has been hard-killed.
         */
        STOPPED;
    }

    private static final class Watcher implements Runnable {

        private static final String LINE_SEPARATOR = Strings.getLineSeparator();

        private final ConcurrentSkipListSet<RequestRegistryEntry> requestRegistry;
        private final int requestMaxAge;
        private final int requestExpiredAge;
        private final int downloadUploadExpiredAge;
        private final int interruptedThreshold;
        private final Lock runLock;

        /**
         * Initializes a new {@link Watcher}.
         */
        Watcher(ConcurrentSkipListSet<RequestRegistryEntry> requestRegistry, int requestMaxAge, int requestExpiredAge, int downloadUploadExpiredAge, int interruptedThreshold) {
            super();
            this.runLock = new ReentrantLock();
            this.interruptedThreshold = interruptedThreshold;
            this.requestRegistry = requestRegistry;
            this.requestMaxAge = requestMaxAge;
            this.requestExpiredAge = requestExpiredAge;
            this.downloadUploadExpiredAge = downloadUploadExpiredAge;
        }

        /**
         * Performs a request watcher run:
         * <ol>
         * <li>Start at the tail of the <code>java.util.NavigableSet</code> to get the oldest request first.
         * <li>Then proceed to the younger requests.
         * <li>Stop processing at the first yet valid request.
         * </ol>
         */
        @Override
        public void run() {
            if (!runLock.tryLock()) {
                // Lock NOT acquired --> Another run is being performed. Leave...
                return;
            }

            // Locked...
            try {
                Thread runner = Thread.currentThread();
                boolean debugEnabled = LOG.isDebugEnabled();
                StringBuilder sb = new StringBuilder(256);
                List<Object> args = new ArrayList<>();
                boolean stillOldRequestsLeft = true;
                for (Iterator<RequestRegistryEntry> descendingEntryIterator = requestRegistry.descendingIterator(); !runner.isInterrupted() && stillOldRequestsLeft && descendingEntryIterator.hasNext();) {
                    // Debug logging
                    if (debugEnabled) {
                        sb.setLength(0);
                        args.clear();
                        for (RequestRegistryEntry entry : requestRegistry) {
                            sb.append("{}RegisteredThreads:{}    age: ").append(entry.getAge()).append(" ms").append(", thread: ").append(entry.getThreadInfo());
                            args.add(LINE_SEPARATOR);
                            args.add(LINE_SEPARATOR);
                        }
                        final String entries = sb.toString();
                        if (!entries.isEmpty()) {
                            LOG.debug(sb.toString(), args.toArray(new Object[args.size()])); // NOSONARLINT
                        }
                    }

                    // Check entry's age
                    RequestRegistryEntry entry = descendingEntryIterator.next();
                    if (entry.getAge() > requestMaxAge) {
                        sb.setLength(0);
                        args.clear();
                        switch (handleEntry(entry, sb, args)) {
                            case INTERRUPTED:
                                entry.markInterrupted();
                                break;
                            case STOPPED:
                                requestRegistry.remove(entry);
                                break;
                            case LOGGED:
                                // fall-through
                            default:
                                // Nothing
                                break;

                        }
                    } else {
                        stillOldRequestsLeft = false;
                    }
                }
            } catch (Exception e) {
                LOG.error("Request watcher run failed", e);
            } finally {
                runLock.unlock();
            }
        }

        private Result handleEntry(RequestRegistryEntry entry, StringBuilder logBuilder, List<Object> args) {
            // Age info
            AgeInfo ageInfo = newAgeInfo(entry.getAge(), requestMaxAge);

            // Get trace for associated thread's trace
            Throwable trace = new RequestTrace(ageInfo.sAge, ageInfo.sMaxAge, entry.getThread().getName());
            StackTraceElement[] stackTrace = entry.getStackTrace();
            trace.setStackTrace(stackTrace);

            // Examine trace if request is a download or upload
            boolean downloadOrUploadRequest = isDownloadOrUploadRequest(stackTrace);

            // Determine effective expiration age and...
            int effectiveExpirationAge = downloadOrUploadRequest ? downloadUploadExpiredAge : requestExpiredAge;

            // ... check if request shall be interrupted (if not already done) or even killed
            Result result = determineResult(entry, effectiveExpirationAge);

            // Special handling for down-/upload
            if (downloadOrUploadRequest) {
                // No logging for down-/upload
                if (result == Result.INTERRUPTED) {
                    entry.getThread().interrupt();
                } else if (result == Result.STOPPED) {
                    entry.getThread().stop();
                }
                return result;
            }

            // Prepare logging
            try {
                logBuilder.append('#').append(entry.getAndIncrementExceededCounter()).append(' ').append(entry.isInterrupted() ? "Interrupted request" : "Request").append(" with age ").append(ageInfo.sAge).append("ms (").append(exactly(entry.getAge(), true)).append(") exceeds max. age of ").append(ageInfo.sMaxAge).append("ms (").append(exactly(requestMaxAge, true)).append(").");
            } catch (Exception e) {
                LOG.trace("", e);
                logBuilder.append('#').append(entry.getAndIncrementExceededCounter()).append(' ').append(entry.isInterrupted() ? "Interrupted request" : "Request").append(" with age ").append(ageInfo.sAge).append("ms exceeds max. age of ").append(ageInfo.sMaxAge).append("ms.");
            }

            // Special logging in case of interrupt or kill
            if (result == Result.INTERRUPTED) {
                logBuilder.append("{}Going to interrupt request's thread since expiration threshold {}ms ({}) is exceeded!");
                args.add(LINE_SEPARATOR);
                args.add(formatDecimal(effectiveExpirationAge));
                args.add(exactly(effectiveExpirationAge, true));
            } else if (result == Result.STOPPED) {
                logBuilder.append("{}Going to hard-kill request's thread since already interrupted, but still running!");
                args.add(LINE_SEPARATOR);
            }

            // Append log properties from the ThreadLocal to logBuilder
            appendLogProperties(entry, logBuilder, args);

            // Add stack trace of tracked request
            args.add(trace);
            LOG.info(logBuilder.toString(), args.toArray(new Object[args.size()])); // NOSONARLINT

            // Check if request's thread is supposed to be interrupted or killed
            if (result == Result.INTERRUPTED) {
                entry.getThread().interrupt();
            } else if (result == Result.STOPPED) {
                entry.getThread().stop();
            }
            return result;
        }

        private Result determineResult(RequestRegistryEntry entry, int effectiveExpirationAge) {
            if (entry.isInterrupted()) {
                // Already interrupted but still running
                if (interruptedThreshold <= 0) {
                    return Result.LOGGED;
                }

                int expiredCount = entry.incrementAndGetExpiredCounter();
                return expiredCount >= interruptedThreshold ? Result.STOPPED : Result.LOGGED;
            }

            return shallInterrupt(entry, effectiveExpirationAge) ? Result.INTERRUPTED : Result.LOGGED;
        }

        /**
         * Checks if the request associated with given entry shall be interrupted.
         *
         * @param entry The entry providing information about tracked request
         * @param expiredAge The expired age to check against
         * @param downloadOrUploadRequest <code>true</code> for download/upload request; otherwise <code>false</code>
         * @return <code>true</code> if request shall be interrupted; otherwise <code>false</code>
         */
        private static boolean shallInterrupt(RequestRegistryEntry entry, int expiredAge) {
            if (entry.getAge() > expiredAge) {
                // Expiration time elapsed
                return true;
            }

            // Check request's log properties for inactive session
            Map<String, String> propertyMap = entry.getPropertyMap();
            if (null != propertyMap && isInvalidSession(propertyMap.get(LOG_PROPERTY_SESSION_ID))) { // NOSONARLINT
                return true;
            }

            return false;
        }

        /**
         * The log property for session identifier: <code>"com.openexchange.session.sessionId"</code>
         */
        private static final String LOG_PROPERTY_SESSION_ID = LogProperties.Name.SESSION_SESSION_ID.getName();

        private static void appendLogProperties(RequestRegistryEntry entry, StringBuilder logBuilder, List<Object> args) {
            Map<String, String> propertyMap = entry.getPropertyMap();
            if (null != propertyMap) {
                // Sort the properties for readability and add them to the logBuilder (if any)
                Iterator<Map.Entry<String, String>> it = new TreeMap<String, String>(propertyMap).entrySet().iterator();
                if (it.hasNext()) {
                    logBuilder.append(" Request's properties:{}");
                    args.add(LINE_SEPARATOR);
                    String indention = "  ";
                    Map.Entry<String, String> propertyEntry = it.next();
                    logBuilder.append(indention).append(propertyEntry.getKey()).append('=').append(propertyEntry.getValue());
                    while (it.hasNext()) {
                        propertyEntry = it.next();
                        logBuilder.append("{}").append(indention).append(propertyEntry.getKey()).append('=').append(propertyEntry.getValue());
                        args.add(LINE_SEPARATOR);
                    }
                }
            }
        }

        /**
         * Checks if specified session identifier refers to an invalid session (e.g. there is no such active session).
         *
         * @param sessionId The session identifier to check for
         * @return <code>true</code> if session identifier refers to an invalid session; otherwise <code>false</code>
         */
        private static boolean isInvalidSession(String sessionId) {
            if (sessionId == null) {
                // Cannot check for null session identifier. Signal as valid then...
                return false;
            }

            SessiondService sessiondService = SessiondService.SERVICE_REFERENCE.get();
            return sessiondService instanceof SessiondServiceExtended && !((SessiondServiceExtended) sessiondService).isActive(sessionId);
        }

        private static boolean isDownloadOrUploadRequest(StackTraceElement[] trace) {
            for (StackTraceElement ste : trace) {
                String className = ste.getClassName();
                if (null != className) {
                    if (className.startsWith("org.apache.commons.fileupload.MultipartStream$ItemInputStream")) {
                        // A long-running file upload
                        return true;
                    }
                    if (className.startsWith("com.openexchange.ajax.requesthandler.responseRenderers.actions.OutputBinaryContentAction")) {
                        // A long-running file download
                        return true;
                    }
                }
            }
            return false;
        }

    } // End of class Watcher

    /**
     * Creates a new age info for given arguments.
     *
     * @param age The current age
     * @param requestMaxAge The age threshold
     * @return The age info
     */
    protected static AgeInfo newAgeInfo(long age, int requestMaxAge) {
        return new AgeInfo(formatDecimal(age), formatDecimal(requestMaxAge));
    }

    private static String formatDecimal(long number) {
        StringBuilder builder = new StringBuilder(Long.toString(number));
        for (int offset = builder.length() - 3; offset > 0; offset -= 3) {
            builder.insert(offset, ',');
        }
        return builder.toString();
    }

    private static final class AgeInfo {

        final String sAge;
        final String sMaxAge;

        AgeInfo(String sAge, String sMaxAge) {
            super();
            this.sAge = sAge;
            this.sMaxAge = sMaxAge;
        }
    }

}
