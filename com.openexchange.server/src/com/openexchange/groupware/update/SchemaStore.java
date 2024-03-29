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

package com.openexchange.groupware.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import com.openexchange.caching.CacheService;
import com.openexchange.database.Databases;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.update.internal.SchemaExceptionCodes;
import com.openexchange.groupware.update.internal.SchemaStoreImpl;

/**
 * Abstract class defining the interface for reading the schema version information.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public abstract class SchemaStore {

    /**
     * Factory method.
     *
     * @return an implementation for this interface.
     */
    public static SchemaStore getInstance() {
        return SchemaStoreImpl.getInstance();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link SchemaStore}.
     */
    protected SchemaStore() {
        super();
    }

    /**
     * Gets the schema information for specified schema name on given database referenced by given pool identifier.
     *
     * @param poolId The pool identifier referencing the database
     * @param schemaName The schema name
     * @param con The connection to use
     * @return The schema information
     * @throws OXException If schema information cannot be returned
     */
    protected abstract SchemaUpdateState getSchema(int poolId, String schemaName, Connection con) throws OXException;

    /**
     * Marks given schema as locked due to a start of an update process.
     *
     * @param schema the schema
     * @param background <code>false</code> if blocking tasks are executed.
     * @throws OXException if the schema cannot be locked
     */
    public abstract void lockSchema(Schema schema, boolean background) throws OXException;

    /**
     * Marks given schema as unlocked to release this schema from an update process.
     *
     * @param schema the schema
     * @param background <code>false</code> if blocking tasks finished.
     * @throws OXException if the schema cannot be unlocked
     */
    public abstract void unlockSchema(Schema schema, boolean background) throws OXException;
    
    /**
     * Tries to refresh the schema lock (resetting time stamp to current time).
     *
     * @param schema The schema whose lock is supposed to be refreshed
     * @param background <code>false</code> if blocking tasks are in progress; otherwise <code>true</code>.
     * @return <code>true</code> if the lock was successfully refreshed; otherwise <code>false</code>
     * @throws OXException If refresh fails
     */
    public abstract boolean tryRefreshSchemaLock(Schema schema, boolean background) throws OXException;

    /**
     * Gets the idle time in milliseconds for either blocking or background tasks.
     *
     * @param background <code>true</code> to retrieve idle time for background tasks; otherwise <code>false</code> for blocking ones
     * @return The idle time
     */
    public abstract long getIdleMillis(boolean background);

    /**
     * Gets the schema information for specified context (implicitly referencing schema and database).
     *
     * @param ctx The context
     * @return The schema information
     * @throws OXException If schema information cannot be returned
     */
    public final Schema getSchema(final Context ctx) throws OXException {
        return getSchema(ctx.getContextId());
    }

    /**
     * Gets the schema information for specified context identifier (implicitly referencing schema and database).
     *
     * @param contextId The context identifier
     * @return The schema information
     * @throws OXException If schema information cannot be returned
     */
    public final SchemaUpdateState getSchema(final int contextId) throws OXException {
        // This method is used when doing a normal login. In this case fetching the Connection runs through replication monitor and
        // initializes the transaction counter from the master. This allows redirecting subsequent reads after normal login to the master
        // if the slave is not actual. See bugs 19817 and 27460.
        Connection con = Database.get(contextId, true);
        try {
            return getSchema(Database.resolvePool(contextId, true), Database.getSchema(contextId), con);
        } finally {
            // In fact the transaction counter is initialized when returning the connection to the pool ;-)
            Database.backAfterReading(contextId, con);
        }
    }

    /**
     * Gets the schema information for specified schema name on given database referenced by given pool identifier.
     *
     * @param poolId The pool identifier referencing the database
     * @param schemaName The schema name
     * @return The schema information
     * @throws OXException If schema information cannot be returned
     */
    public final SchemaUpdateState getSchema(int poolId, String schemaName) throws OXException {
        // This method is used when creating a context through the administration daemon. In this case we did not write yet the information
        // into the ConfigDB in which database and schema the context is located. Therefore we are not able to initialize the transaction
        // counter for the replication monitor.
        Connection con = Database.get(poolId, schemaName);
        try {
            return getSchema(poolId, schemaName, con);
        } finally {
            Database.back(poolId, con);
        }
    }

    /**
     * Gets the collection of executed update tasks.
     *
     * @param poolId The pool identifier referencing the database
     * @param schemaName The schema name
     * @return The collection of executed update tasks
     * @throws OXException If collection of executed update tasks cannot be returned
     */
    public abstract ExecutedTask[] getExecutedTasks(int poolId, String schemaName) throws OXException;

    /**
     * Adds specified executed update task to referenced database schema.
     *
     * @param taskName The unique name of the task
     * @param success The flag indicating successful execution or not
     * @param poolId The pool identifier referencing the database
     * @param schema The schema name
     * @throws OXException If adding executed task fails
     */
    public final void addExecutedTask(String taskName, boolean success, int poolId, String schema) throws OXException {
        Connection con = Database.get(poolId, schema);
        int rollback = 0;
        try {
            con.setAutoCommit(false);
            rollback = 1;

            addExecutedTask(con, taskName, success, poolId, schema);

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw SchemaExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            Database.back(poolId, con);
        }
    }

    /**
     * Adds specified executed update task to referenced database schema using given read-write connection.
     *
     * @param con The read-write connection to use (in transaction mode)
     * @param taskName The unique name of the task
     * @param success The flag indicating successful execution or not
     * @param poolId The pool identifier referencing the database
     * @param schema The schema name
     * @throws OXException If adding executed task fails
     */
    public abstract void addExecutedTask(Connection con, String taskName, boolean success, int poolId, String schema) throws OXException;

    /**
     * Adds specified executed update tasks to referenced database schema.
     *
     * @param contextId The context identifier
     * @param taskNames The unique names of the tasks
     * @param success The flag indicating successful execution or not
     * @param poolId The pool identifier referencing the database
     * @param schema The schema name
     * @throws OXException If adding executed task fails
     */
    @Deprecated
    public final void addExecutedTasks(int contextId, Collection<String> taskNames, boolean success, int poolId, String schema) throws OXException {
        final Connection con = Database.get(contextId, true);
        int rollback = 0;
        try {
            con.setAutoCommit(false);
            rollback = 1;

            addExecutedTasks(con, taskNames, success, poolId, schema);

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw SchemaExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            Database.back(contextId, true, con);
        }
    }

    /**
     * Adds specified executed update tasks to referenced database schema.
     *
     * @param con The read-write connection to use (in transaction mode)
     * @param taskNames The unique names of the tasks
     * @param success The flag indicating successful execution or not
     * @param poolId The pool identifier referencing the database
     * @param schema The schema name
     * @throws OXException If adding executed task fails
     */
    public abstract void addExecutedTasks(Connection con, Collection<String> taskNames, boolean success, int poolId, String schema) throws OXException;

    /**
     * Sets the cache service to use.
     *
     * @param cacheService The cache service
     */
    public abstract void setCacheService(CacheService cacheService);

    /**
     * Removes the cache service.
     */
    public abstract void removeCacheService();

    /**
     * Invalidates given schema from cache.
     *
     * @param schema The schema
     */
    public abstract void invalidateCache(Schema schema);

}
