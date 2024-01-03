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


package com.openexchange.database.internal;

import static com.eaio.util.text.HumanTime.exactly;
import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.exception.ExceptionUtils.dropStackTraceFor;
import static com.openexchange.exception.ExceptionUtils.getLastChainedThrowable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.database.DBPoolingExceptionCodes;
import com.openexchange.exception.OXException;

/**
 * {@link ReachabilityCheck} - Helper class for testing reachability of the config database.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ReachabilityCheck {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ReachabilityCheck.class);

    /**
     * Initializes a new {@link ReachabilityCheck}.
     */
    private ReachabilityCheck() {
        super();
    }

    /**
     * Checks/awaits reachability of the database.
     *
     * @param maxNumAttempts The max. number of attempts or less than/equal to <code>0</code> (zero) for infinite
     * @param configDatabaseService The database service
     * @param writeUrl The database read-write URL
     * @throws OXException If test fails for other reasons than unavailability
     */
    public static void checkReachability(int maxNumAttempts, ConfigDatabaseService configDatabaseService, String writeUrl) throws OXException {
        int connectCounter = 1;
        boolean success = false;
        while (!success) {
            try {
                LOG.info("Trying to connect to config DB end-point: {}", writeUrl);
                Connection con = configDatabaseService.getWritable();
                try {
                    success = test(con);
                } finally {
                    configDatabaseService.backWritableAfterReading(con);
                }
                if (!success) {
                    throw DBPoolingExceptionCodes.NO_CONFIG_DB.create("Test SELECT statement failed");
                }
                LOG.info("Successfully connected to config DB end-point: {}", writeUrl);
            } catch (OXException e) {
                if (!DBPoolingExceptionCodes.NO_CONFIG_DB.equals(e) || (maxNumAttempts > 0 && connectCounter >= maxNumAttempts)) {
                    throw e;
                }
                // Increase wait time until 10 attempts accomplished. Then stay to 15sec + random
                long millis = exponentialBackoffWait(connectCounter > 10 ? 15 : connectCounter++, 1000L);
                if (LOG.isDebugEnabled()) {
                    LOG.warn("Failed to connect to config DB end-point: {}. Waiting {} ({}) for retry attempt...", writeUrl, format(millis), exactly(millis, true), e);
                } else {
                    LOG.warn("Failed to connect to config DB end-point: {}. Waiting {} ({}) for retry attempt...", writeUrl, format(millis), exactly(millis, true), dropStackTraceFor(getLastChainedThrowable(e)));
                }
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(millis, TimeUnit.MILLISECONDS));
            }
        }
    }

    /** The SQL command for checking the connection. */
    private static final String TEST_SELECT = "SELECT 1 AS test";

    /**
     * Tests the connection.
     *
     * @param con The connection to test
     * @return <code>true</code> if passed connection is OK; otherwise <code>false</code>
     * @throws OXException If an SQL error occurs
     */
    private static boolean test(Connection con) throws OXException {
        Statement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.createStatement();
            result = stmt.executeQuery(TEST_SELECT);
            return result.next() ? (result.getInt(1) == 1) : false;
        } catch (SQLException e) {
            throw DBPoolingExceptionCodes.NO_CONFIG_DB.create(e, "Test SELECT statement failed");
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    /**
     * Calculates the time to wait according to exponential back-off strategy.
     * <pre>
     * (retry-count * base-millis) + random-millis
     * </pre>
     *
     * @param retryCount The current number of retries
     * @param baseMillis The base milliseconds
     * @return The time to wait in milliseconds
     */
    private static long exponentialBackoffWait(int retryCount, long baseMillis) {
        return (retryCount * baseMillis) + ((long) (Math.random() * baseMillis));
    }

    /** The decimal format to use when printing milliseconds */
    private static final NumberFormat MILLIS_FORMAT = newNumberFormat();

    /** The accompanying lock for shared decimal format */
    private static final Lock MILLIS_FORMAT_LOCK = new ReentrantLock();

    /**
     * Creates a new {@code DecimalFormat} instance.
     *
     * @return The format instance
     */
    private static NumberFormat newNumberFormat() {
        NumberFormat f = NumberFormat.getInstance(Locale.US);
        if (f instanceof DecimalFormat df) {
            df.applyPattern("#,##0");
        }
        return f;
    }

    private static String format(long millis) {
        if (MILLIS_FORMAT_LOCK.tryLock()) {
            try {
                return MILLIS_FORMAT.format(millis);
            } finally {
                MILLIS_FORMAT_LOCK.unlock();
            }
        }

        // Use thread-specific DecimalFormat instance
        NumberFormat format = newNumberFormat();
        return format.format(millis);
    }

}
