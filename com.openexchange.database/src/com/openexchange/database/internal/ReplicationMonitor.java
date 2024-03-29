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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import com.openexchange.database.Assignment;
import com.openexchange.database.DBPoolingExceptionCodes;
import com.openexchange.database.Databases;
import com.openexchange.database.GeneralDatabaseConnectionListener;
import com.openexchange.exception.OXException;
import com.openexchange.java.IOs;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.pooling.PoolingException;

/**
 * {@link ReplicationMonitor}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ReplicationMonitor {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ReplicationMonitor.class);

    private final ServiceListing<GeneralDatabaseConnectionListener> connectionListeners;

    private final FetchAndSchema TIMEOUT = new TimeoutFetchAndSchema(this, true);
    private final FetchAndSchema NOTIMEOUT = new NotimeoutFetchAndSchema(this, true);
    private final FetchAndSchema TIMEOUT_NOSCHEMA = new TimeoutFetchAndSchema(this, false);
    private final FetchAndSchema NOTIMEOUT_NOSCHEMA = new NotimeoutFetchAndSchema(this, false);

    private final AtomicLong masterConnectionsFetched = new AtomicLong();
    private final AtomicLong slaveConnectionsFetched = new AtomicLong();
    private final AtomicLong masterInsteadOfSlaveFetched = new AtomicLong();

    private final boolean active;
    private final boolean checkWriteCons;

    private long lastLogged = 0;

    public ReplicationMonitor(boolean active, boolean checkWriteCons, ServiceListing<GeneralDatabaseConnectionListener> connectionListeners) {
        super();
        this.active = active;
        this.checkWriteCons = checkWriteCons;
        this.connectionListeners = connectionListeners;
    }

    /**
     * Gets the general connection listeners
     *
     * @return The connection listeners
     */
    public ServiceListing<GeneralDatabaseConnectionListener> getConnectionListeners() {
        return connectionListeners;
    }

    Connection checkFallback(Pools pools, AssignmentImpl assign, boolean noTimeout, boolean write) throws OXException {
        return checkFallback(pools, assign, noTimeout ? NOTIMEOUT : TIMEOUT, write);
    }

    Connection checkFallback(Pools pools, AssignmentImpl assign, boolean noTimeout, boolean write, boolean noSchema) throws OXException {
        FetchAndSchema fetchAndSchema;
        if (noTimeout) {
            fetchAndSchema = noSchema ? NOTIMEOUT_NOSCHEMA : NOTIMEOUT;
        } else {
            fetchAndSchema = noSchema ? TIMEOUT_NOSCHEMA : TIMEOUT;
        }
        return checkFallback(pools, assign, fetchAndSchema, write);
    }

    private Connection checkFallback(Pools pools, AssignmentImpl assign, FetchAndSchema fetch, boolean write) throws OXException {
        Connection retval;
        int tries = 0;
        do {
            tries++;
            try {
                retval = fetch.get(pools, assign, write, false);
                incrementFetched(assign, write);
            } catch (PoolingException e) {
                OXException e1 = createException(assign, write, e);
                // Immediately fail if connection to master is wanted or no fallback is there.
                if (write || assign.getWritePoolId() == assign.getReadPoolId()) {
                    throw e1;
                }
                // Try fallback to master.
                LOG.warn("", e1);
                try {
                    retval = fetch.get(pools, assign, true, true);
                    incrementInstead();
                } catch (PoolingException e2) {
                    throw createException(assign, true, e2);
                }
            }
        } while (null == retval && tries < 10);
        if (null == retval) {
            throw createException(assign, write, null);
        }
        return retval;
    }

    public Connection checkActualAndFallback(final Pools pools, final AssignmentImpl assign, final boolean noTimeout, final boolean write) throws OXException {
        return checkActualAndFallback(pools, assign, noTimeout ? NOTIMEOUT : TIMEOUT, write);
    }

    private Connection checkActualAndFallback(final Pools pools, final AssignmentImpl assign, final FetchAndSchema fetch, final boolean write) throws OXException {
        Connection retval;
        long clientTransaction = 0;
        int tries = 0;
        do {
            tries++;
            try {
                retval = fetch.get(pools, assign, write, false);
                incrementFetched(assign, write);
            } catch (Exception e) {
                final OXException e1;
                if (e instanceof OXException) {
                    e1 = (OXException) e;
                } else {
                    e1 = createException(assign, write, e);
                }
                // Immediately fail if connection to master is wanted or no fallback is there.
                if (write || assign.getWritePoolId() == assign.getReadPoolId()) {
                    throw e1;
                }
                // Try fallback to master.
                LOG.warn(e1.getMessage(), e1);
                try {
                    retval = fetch.get(pools, assign, true, true);
                    incrementInstead();
                } catch (PoolingException e2) {
                    throw createException(assign, true, e2);
                }
            }
            if (!write && assign.isTransactionInitialized()) {
                try {
                    clientTransaction = readTransaction(retval, assign.getContextId());
                } catch (OXException e) {
                    if (10 == tries) {
                        // Consumed all available retry attempts. Do a fall back to the master.
                        LOG.warn("", e);
                        clientTransaction = -1;
                    } else {
                        if (IOs.isEOFException(e)) {
                            // Communications link failure: connection to database was unexpectedly lost
                            LOG.warn("Unexpectedly lost connection to database", e);
                        }
                        try {
                            retval.close();
                        } catch (SQLException e1) {
                            OXException e2 = DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
                            LOG.error("", e2);
                        }
                        retval = null;
                        // Wait with exponential back-off
                        long nanosToWait = TimeUnit.NANOSECONDS.convert((tries * 1000) + ((long) (Math.random() * 1000)), TimeUnit.MILLISECONDS);
                        LockSupport.parkNanos(nanosToWait);
                    }
                }
            }
        } while (null == retval && tries < 10);
        if (null == retval) {
            throw createException(assign, write, null);
        }
        if (!write && assign.isTransactionInitialized() && !isUpToDate(assign.getTransaction(), clientTransaction)) {
            LOG.debug("Slave {} is not actual. Using master {} instead.", I(assign.getReadPoolId()), I(assign.getWritePoolId()));
            final Connection toReturn = retval;
            try {
                retval = fetch.get(pools, assign, true, true);
                incrementInstead();
                try {
                    toReturn.close();
                } catch (SQLException e) {
                    final OXException e1 = DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
                    LOG.error("", e1);
                }
            } catch (PoolingException e) {
                // Use not actual slave if master connection cannot be obtained.
                final OXException e1 = createException(assign, true, e);
                LOG.warn("", e1);
            }
        }
        if (null == retval) {
            throw createException(assign, write, null);
        }
        return retval;
    }

    private static OXException createException(final Assignment assign, final boolean write, final Throwable cause) {
        final int poolId = write ? assign.getWritePoolId() : assign.getReadPoolId();
        return assign.getReadPoolId() == Constants.CONFIGDB_READ_ID ? DBPoolingExceptionCodes.NO_CONFIG_DB.create(cause, cause.getMessage()) : DBPoolingExceptionCodes.NO_CONNECTION.create(cause, I(poolId), cause.getMessage());
    }

    private static boolean isUpToDate(final long masterTransaction, final long slaveTransaction) {
        // TODO handle overflow
        LOG.trace("Replication monitor transaction position is {} on master and {} on slave.", L(masterTransaction), L(slaveTransaction));
        return slaveTransaction >= masterTransaction;
    }

    public void backAndIncrementTransaction(Pools pools, AssignmentImpl assign, Connection con, boolean noTimeout, boolean write, ConnectionState state) {
        // Determine pool identifier
        final int poolId;
        if (write) {
            poolId = assign.getWritePoolId();
            if (!state.isUsedAsRead()) {
                // ConfigDB has no replication monitor and for master fallback connections the counter must not be incremented.
                if (state.isUsedForUpdate()) {
                    // Data on the master has been changed without using a transaction, so we need to increment the counter here.
                    // If a transaction was used the JDBC Connection wrapper incremented the counter in the commit phase.
                    increaseTransactionCounter(assign, con);
                } else {
                    // Initialize counter as early as possible.
                    if (active && poolId != assign.getReadPoolId() && !assign.isTransactionInitialized()) {
                        try {
                            assign.setTransaction(readTransaction(con, assign.getContextId()));
                        } catch (OXException e) {
                            LOG.warn("", e);
                        }
                    }
                    // Warn if a master connection was only used for reading.
                    if (checkWriteCons && !state.isUsedAsRead() && !state.isUpdateCommitted()) {
                        Exception e = new Exception("A writable connection was used but no data has been manipulated.");
                        LOG.warn("A writable connection was used but no data has been manipulated.", e);
                    }
                }
            }
        } else {
            poolId = assign.getReadPoolId();
        }

        // Get associated pool
        final ConnectionPool pool;
        try {
            pool = pools.getPool(poolId);
        } catch (OXException e) {
            LOG.error("", e);
            return;
        }

        // Apply state
        boolean wasHeartbeatEnabled = state.isHeartbeatEnabled();
        if (con instanceof StateAware) {
            StateAware stateAware = (StateAware) con;
            ConnectionState connectionState = stateAware.getConnectionState();
            connectionState.setUpdateCommitted(state.isUpdateCommitted());
            connectionState.setUsedAsRead(state.isUsedAsRead());
            connectionState.setUsedForUpdate(state.isUsedForUpdate());
        }

        // Return connection
        if (noTimeout) {
            pool.backWithoutTimeout(con);
        } else {
            try {
                pool.back(con);
            } catch (PoolingException e) {
                if (!wasHeartbeatEnabled) {
                    Databases.close(con);
                    final OXException e1 = DBPoolingExceptionCodes.RETURN_FAILED.create(e, con.toString());
                    LOG.error("", e1);
                }
            }
        }
    }

    private static long readTransaction(final Connection con, final int ctxId) throws OXException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        final long retval;
        try {
            stmt = con.prepareStatement("SELECT transaction FROM replicationMonitor WHERE cid=?");
            stmt.setInt(1, ctxId);
            result = stmt.executeQuery();
            if (result.next()) {
                retval = result.getLong(1);
            } else {
                throw DBPoolingExceptionCodes.TRANSACTION_MISSING.create(I(ctxId));
            }
        } catch (SQLException e) {
            throw DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(result, stmt);
        }
        return retval;
    }

    private static void increaseCounter(AssignmentImpl assign, Connection con) throws SQLException {
        int contextId = assign.getContextId();
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            // Using Mysql specific functions like LAST_INSERT_ID() do not reveal any performance improvement compared to this transaction.
            // UPDATE replicationMonitor SET transaction=LAST_INSERT_ID(transaction+1) WHERE cid=?
            // Therefore we stick with this simple transaction, UPDATE and SELECT statement for better compatibility.
            stmt = con.prepareStatement("UPDATE replicationMonitor SET transaction=transaction+1 WHERE cid=?");
            stmt.setInt(1, contextId);
            stmt.execute();
            stmt.close();
            stmt = con.prepareStatement("SELECT transaction FROM replicationMonitor WHERE cid=?");
            stmt.setInt(1, contextId);
            result = stmt.executeQuery();
            if (result.next()) {
                assign.setTransaction(result.getLong(1));
            } else {
                LOG.error("Updating transaction for replication monitor failed for context {}.", I(contextId));
            }
        } catch (SQLException e) {
            if ((e.getErrorCode() == 1146) && (e.getSQLState().equalsIgnoreCase("42S02")) && (org.apache.commons.lang.StringUtils.containsIgnoreCase(e.getMessage(), "replicationMonitor"))) {
                return;
            }
            throw e;
        } finally {
            Databases.closeSQLStuff(result, stmt);
        }
    }

    private static void increaseCounterExistingTransaction(AssignmentImpl assign, Connection con) throws SQLException {
        Savepoint save = null;
        try {
            save = con.setSavepoint("replicationMonitor");
            increaseCounter(assign, con);
            con.releaseSavepoint(save);
        } catch (SQLException e) {
            if (1213 != e.getErrorCode()) {
                // In case of a transaction deadlock MySQL already rolled the transaction back. Then the savepoint does not exist anymore.
                Databases.rollback(con, save);
            }
            throw e;
        }
    }

    private void increaseCounterSeparateTransaction(AssignmentImpl assign, Connection con) {
        try {
            con.setAutoCommit(false);
            increaseCounter(assign, con);
            con.commit();
        } catch (SQLException e) {
            Databases.rollback(con);
            if (1146 == e.getErrorCode()) {
                if (lastLogged + 300000 < System.currentTimeMillis()) {
                    lastLogged = System.currentTimeMillis();
                    final OXException e1 = DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
                    LOG.error("", e1);
                }
            } else {
                final OXException e1 = DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
                LOG.error("", e1);
            }
        } finally {
            Databases.autocommit(con);
        }
    }

    private void incrementFetched(final Assignment assign, final boolean write) {
        if (assign.getWritePoolId() == assign.getReadPoolId() || write) {
            masterConnectionsFetched.incrementAndGet();
        } else {
            slaveConnectionsFetched.incrementAndGet();
        }
    }

    private void incrementInstead() {
        masterInsteadOfSlaveFetched.incrementAndGet();
    }

    long getMasterConnectionsFetched() {
        return masterConnectionsFetched.get();
    }

    long getSlaveConnectionsFetched() {
        return slaveConnectionsFetched.get();
    }

    long getMasterInsteadOfSlave() {
        return masterInsteadOfSlaveFetched.get();
    }

    void increaseTransactionCounter(AssignmentImpl assign, Connection con) {
        try {
            if (!active || assign.getWritePoolId() == assign.getReadPoolId() || con.isClosed()) {
                return;
            }
        } catch (SQLException e) {
            final OXException e1 = DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
            LOG.error("", e1);
            return;
        }
        increaseCounterSeparateTransaction(assign, con);
    }

    public void increaseInCurrentTransaction(AssignmentImpl assign, Connection delegate, ConnectionState state) {
        if (!active || assign.getWritePoolId() == assign.getReadPoolId()) {
            return;
        }
        try {
            increaseCounterExistingTransaction(assign, delegate);
            state.setUsedForUpdate(false);
            state.setUpdateCommitted(true);
        } catch (SQLException e) {
            if (1146 == e.getErrorCode()) {
                if (lastLogged + 300000 < System.currentTimeMillis()) {
                    lastLogged = System.currentTimeMillis();
                    final OXException e1 = DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
                    LOG.error("", e1);
                }
            } else {
                final OXException e1 = DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
                LOG.error("", e1);
            }
        }
    }
}