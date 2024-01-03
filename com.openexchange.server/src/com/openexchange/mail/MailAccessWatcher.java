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

package com.openexchange.mail;

import java.util.LinkedList;
import java.util.List;
import com.openexchange.java.Strings;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.watcher.MailAccessDelayElement;
import com.openexchange.mail.watcher.MailAccessDelayQueue;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link MailAccessWatcher} - Keeps track of connected instances of {@link MailAccess} and allows a forced close if connection time exceeds
 * allowed time
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailAccessWatcher {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailAccessWatcher.class);

    private static final MailAccessDelayQueue MAIL_ACCESSES = new MailAccessDelayQueue();

    private static boolean initialized = false;

    private static volatile ScheduledTimerTask watcherTask;

    /**
     * Initializes and starts mail connection watcher if not done, yet
     */
    static synchronized void init() {
        if (initialized) {
            return;
        }
        if (MailProperties.getInstance().isWatcherEnabled()) {
            /*
             * Start task
             */
            final TimerService timer = ServerServiceRegistry.getInstance().getService(TimerService.class);
            if (null != timer) {
                final int frequencyMillis = MailProperties.getInstance().getWatcherFrequency();
                watcherTask = timer.scheduleWithFixedDelay(new WatcherTask(MAIL_ACCESSES, LOG), 1000, frequencyMillis);
            }
            LOG.info("Mail connection watcher successfully established and ready for tracing");
        }
        initialized = true;
    }

    /**
     * Stops mail connection watcher if currently running
     */
    static synchronized void stop() {
        if (!initialized) {
            return;
        }
        if (MailProperties.getInstance().isWatcherEnabled()) {
            final ScheduledTimerTask watcherTask = MailAccessWatcher.watcherTask;
            if (null != watcherTask) {
                watcherTask.cancel(false);
                MailAccessWatcher.watcherTask = null;
                final TimerService timer = ServerServiceRegistry.getInstance().getService(TimerService.class);
                if (null != timer) {
                    timer.purge();
                }
            }
            LOG.info("Mail connection watcher successfully stopped");
        }
        MAIL_ACCESSES.clear();
        initialized = false;
    }

    /**
     * Prevent instantiation
     */
    private MailAccessWatcher() {
        super();
    }

    /**
     * Adds specified mail access to this watcher's tracing if not already added before. If already present its time stamp is updated.
     * <p>
     * Watcher is established if not running, yet
     *
     * @param mailAccess The mail access to add
     */
    public static void addMailAccess(MailAccess<?, ?> mailAccess) {
        /*
         * Insert or update time stamp
         */
        MAIL_ACCESSES.offer(new MailAccessDelayElement(mailAccess, System.currentTimeMillis()));
    }

    /**
     * Touches specified mail access.
     *
     * @param mailAccess The mail access to touch
     */
    public static boolean touchMailAccess(MailAccess<?, ?> mailAccess) {
        return MAIL_ACCESSES.touch(new MailAccessDelayElement(mailAccess, System.currentTimeMillis()));
    }

    /**
     * Removes specified mail access from this watcher's tracing
     *
     * @param mailAccess The mail access to remove
     */
    public static void removeMailAccess(MailAccess<?, ?> mailAccess) {
        MAIL_ACCESSES.remove(new MailAccessDelayElement(mailAccess, 0L));
    }

    /**
     * Gets the number of currently tracked mail accesses.
     *
     * @return The number of currently tracked mail accesses
     */
    public static int getNumberOfMailAccesses() {
        return MAIL_ACCESSES.size();
    }

    /**
     * Gets the number of currently tracked idling mail accesses.
     *
     * @return The number of currently tracked idling mail accesses
     */
    public static int getNumberOfIdlingMailAccesses() {
        int count = 0;
        for (MailAccessDelayElement element : MAIL_ACCESSES) {
            final MailAccess<?, ?> mailAccess = element.mailAccess;
            if (mailAccess.isConnectedUnsafe() && mailAccess.isWaiting()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Helper class.
     */
    private static class WatcherTask implements Runnable {

        private final MailAccessDelayQueue queue;
        private final org.slf4j.Logger logger;
        private final String lineSeparator;
        private final MailAccessDelayQueue.ElementFilter filter;

        public WatcherTask(MailAccessDelayQueue mailAccesses, org.slf4j.Logger logger) {
            super();
            queue = mailAccesses;
            this.logger = logger;
            final String lineSeparator = Strings.getLineSeparator();
            this.lineSeparator = lineSeparator;
            // Specify filter expression
            filter = new MailAccessDelayQueue.ElementFilter() {

                @Override
                public boolean accept(MailAccessDelayElement element) {
                    final MailAccess<?, ?> mailAccess = element.mailAccess;
                    if (!mailAccess.isConnected()) {
                        return true;
                    }
                    if (mailAccess.isWaiting()) {
                        logger.trace("Idling/waiting mail connection:{}{}", lineSeparator, mailAccess.getTrace());
                        return false;
                    }
                    // Connected and not idle...
                    return true;
                }
            };
        }

        @Override
        public void run() {
            try {
                MailAccessDelayElement expired = queue.poll(filter);
                if (null == expired) {
                    // nothing
                    return;
                }
                final StringBuilder sb = new StringBuilder(512);
                final long now = System.currentTimeMillis();
                if (MailProperties.getInstance().isWatcherShallClose()) {
                    final List<MailAccess<?, ?>> exceededAcesses = new LinkedList<MailAccess<?, ?>>();
                    do {
                        final MailAccess<?, ?> mailAccess = expired.mailAccess;
                        if (mailAccess.isConnected()) {
                            final long val = expired.stamp;
                            final long duration = (now - val);
                            sb.setLength(0);
                            sb.append("UNCLOSED MAIL CONNECTION AFTER ");
                            sb.append(duration).append("msec:");
                            mailAccess.logTrace(sb, logger);
                            exceededAcesses.add(mailAccess);
                        }
                    } while ((expired = queue.poll(filter)) != null);
                    /*
                     * Close exceeded accesses if allowed to
                     */
                    for (MailAccess<?, ?> mailAccess : exceededAcesses) {
                        sb.setLength(0);
                        sb.append("CLOSING MAIL CONNECTION BY WATCHER:").append(lineSeparator).append(mailAccess.toString());
                        mailAccess.close(false);
                        sb.append(lineSeparator).append("    DONE");
                        logger.info(sb.toString());
                    }
                } else {
                    do {
                        final MailAccess<?, ?> mailAccess = expired.mailAccess;
                        if (mailAccess.isConnected()) {
                            final long val = expired.stamp;
                            final long duration = (now - val);
                            sb.setLength(0);
                            sb.append("UNCLOSED MAIL CONNECTION AFTER ");
                            sb.append(duration).append("msec:");
                            mailAccess.logTrace(sb, logger);
                        }
                    } while ((expired = queue.poll(filter)) != null);
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    } // End of WatcherTask class

}
