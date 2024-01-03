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

package com.openexchange.log.audit.slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import com.openexchange.exception.OXException;
import com.openexchange.log.audit.Attribute;
import com.openexchange.log.audit.AuditLogFilter;
import com.openexchange.log.audit.AuditLogService;
import com.openexchange.log.LogProperties;
import com.openexchange.osgi.ServiceListing;


/**
 * {@link Slf4jAuditLogService}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.2
 */
public class Slf4jAuditLogService implements AuditLogService, Runnable {

    /**
     * Initializes a new SLF4J audit log service.
     *
     * @param configuration The associated configuration
     * @param filters The filter listing
     * @return The new service instance
     * @throws OXException If service instance cannot be created
     */
    public static Slf4jAuditLogService initInstance(Configuration configuration, ServiceListing<AuditLogFilter> filters) {
        Slf4jAuditLogService service = new Slf4jAuditLogService(configuration, filters);
        service.startUp();
        return service;
    }

    // --------------------------------------------------------------------------------------------------------------------------

    private static final Slf4jLogEntry POISON = new Slf4jLogEntry("poison", new Attribute[0]);

    /** The logger to use */
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Slf4jAuditLogService.class);

    /** The date formatter */
    private final DateFormatter dateFormatter;

    /** The log level */
    private final Slf4jLogLevel level;

    /** The attribute delimiter */
    private final String delimiter;

    /** Whether to include attribute names */
    private final boolean includeAttributeNames;

    /** The tracked filter s*/
    private final ServiceListing<AuditLogFilter> filters;

    /** The work queue for log entries */
    private final BlockingQueue<Slf4jLogEntry> entries;

    /** The active flag */
    private final AtomicBoolean active;

    /** The shutting-down flag */
    private boolean stopped; // Protected by synchronized

    /**
     * Initializes a new {@link Slf4jAuditLogService}.
     *
     * @throws OXException If initialization fails
     */
    private Slf4jAuditLogService(Configuration configuration, ServiceListing<AuditLogFilter> filters) {
        super();
        this.filters = filters;
        dateFormatter = configuration.getDateFormatter();
        level = configuration.getLevel();
        delimiter = null == configuration.getDelimiter() ? "" : configuration.getDelimiter();
        includeAttributeNames = configuration.isIncludeAttributeNames();

        active = new AtomicBoolean(false);
        stopped = false;
        entries = new LinkedBlockingQueue<Slf4jLogEntry>();
    }

    /**
     * Starts-up this service.
     */
    private synchronized boolean startUp() {
        if (stopped) {
            // Stopped...
            return false;
        }

        if (active.compareAndSet(false, true)) {
            Thread thread = new Thread(this, Slf4jAuditLogService.class.getSimpleName());
            thread.start();
        }

        return true;
    }

    /**
     * Shuts-down this service.
     */
    public synchronized void shutDown() {
        if (active.compareAndSet(true, false)) {
            entries.offer(POISON);
        }
        stopped = true;
    }

    @Override
    public void log(String eventId, Attribute<?>... attributes) {
        boolean stillRunning = true;
        if (false == active.get()) {
            stillRunning = startUp();
        }

        if (stillRunning) {
            entries.offer(new Slf4jLogEntry(eventId, attributes));
        } else {
            // Worker thread inactive... Log with running thread
            List<AuditLogFilter> filters = this.filters.getServiceList();
            doLog(eventId, attributes, filters);
        }
    }

    @Override
    public void run() {
        try {
            List<Slf4jLogEntry> list = new ArrayList<Slf4jLogEntry>(128);
            while (active.get()) {
                // Blocking take from queue
                {
                    Slf4jLogEntry next;
                    try {
                        next = entries.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    // Poisoned?
                    if (POISON == next) {
                        return;
                    }
                    list.add(next);
                }

                // Drain more entries (if any)
                entries.drainTo(list);

                // Poisoned?
                boolean quit = list.remove(POISON);
                if (quit) {
                    return;
                }

                // Determine currently available filters
                List<AuditLogFilter> filters = this.filters.getServiceList();
                if (filters.isEmpty()) {
                    filters = null;
                }

                // Iterate and process log entries, clear list afterwards
                for (Slf4jLogEntry slf4jLogEntry : list) {
                    doLog(slf4jLogEntry.entryId, slf4jLogEntry.attributes, filters);
                }
                list.clear();
            }
        } finally {
            // Going to leave...
            active.set(false);
        }
    }

    /**
     * Logs the specified event identifier and attributes (in given order).
     *
     * @param eventId The event identifier
     * @param attributes The associated attributes
     * @param filters The filters or <code>null</code>
     */
    protected void doLog(String eventId, Attribute<?>[] attributes, List<AuditLogFilter> filters) {
        if (null != filters) {
            for (AuditLogFilter filter : filters) {
                if (false == filter.accept(eventId, attributes)) {
                    return;
                }
            }
        }

        String message = compileMessage(eventId, attributes);
        try {
            LogProperties.addAuditProperty();
            if (null != message) {
                switch (level) {
                    case DEBUG:
                        logger.debug(message);
                        break;
                    case ERROR:
                        logger.error(message);
                        break;
                    case INFO:
                        logger.info(message);
                        break;
                    case TRACE:
                        logger.trace(message);
                        break;
                    case WARN:
                        logger.warn(message);
                        break;
                    default:
                        logger.info(message);
                        break;
                }
            }
        } finally {
            LogProperties.removeAuditProperty();
        }

    }

    /**
     * Compiles the log message.
     *
     * @param eventId The event identifier
     * @param attributes The associated attributes
     * @return The compiled log message
     */
    @SuppressWarnings("null")
    private String compileMessage(String eventId, Attribute<?>[] attributes) {
        int length = null == attributes ? 0 : attributes.length;
        if (length == 0) {
            return eventId;
        }

        StringBuilder sb = new StringBuilder(length << 5);
        sb.append(eventId);

        for (Attribute<?> attribute : attributes) {
            if (null == attribute) {
                // An associated attribute is null. Discard the log event.
                return null;
            }

            // Delimiter
            sb.append(delimiter);

            // Attribute name
            if (includeAttributeNames) {
                sb.append(attribute.getName()).append('=');
            }

            // Attribute value
            if (attribute.isDate()) {
                sb.append(dateFormatter.format((Date) attribute.getValue()));
            } else {
                Object value = attribute.getValue();
                sb.append(null == value ? "null" : value.toString());
            }
        }
        return sb.toString();
    }

}
