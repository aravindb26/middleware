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


package com.openexchange.groupware.infostore.database.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.infostore.InfostoreExceptionCodes;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link GeometricFunctionChecker} - Checks availability of the MySQL functions for populating and parsing spatial columns.
 * <p>
 * <ul>
 * <li><code>"ST_GeomFromText"</code> for populating spatial columns.
 * <li><code>"ST_AsText"</code> for parsing spatial columns.
 * </ul>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class GeometricFunctionChecker {

    /**
     * Initializes a new {@link GeometricFunctionChecker}.
     */
    private GeometricFunctionChecker() {
        super();
    }

    private static final Cache<String, String> CACHE_AS_TEXT = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();

    /**
     * Gets the name of function for converting to Well-Known Text (WKT) format from internal geometry format.
     * <p>
     * Example:
     * <pre>
     *   SELECT ST_AsText(0x000000000101000000000000000000F03F000000000000F03F) AS test;
     *
     *   Output:
     *   +------------+
     *   | test       |
     *   +------------+
     *   | POINT(1 1) |
     *   +------------+
     *   1 row in set (0,01 sec)
     * </pre>
     *
     * @param contextId The context identifier
     * @param defaulName The default function name to return in case database does not support the function or an error occurs
     * @return The name of the function or given default name
     * @see #getAsTextName(int)
     */
    public static String getAsTextName(int contextId, String defaulName) {
        try {
            Optional<String> optName = getAsTextName(contextId);
            return optName.orElse(defaulName);
        } catch (OXException e) {
            return defaulName;
        }
    }

    /**
     * Gets the name of function for converting to Well-Known Text (WKT) format from internal geometry format.
     * <p>
     * Example:
     * <pre>
     *   SELECT ST_AsText(0x000000000101000000000000000000F03F000000000000F03F) AS test;
     *
     *   Output:
     *   +------------+
     *   | test       |
     *   +------------+
     *   | POINT(1 1) |
     *   +------------+
     *   1 row in set (0,01 sec)
     * </pre>
     *
     * @param contextId The context identifier
     * @return The name of the function or empty no such function exists
     * @throws OXException If the function name cannot be determined
     */
    public static Optional<String> getAsTextName(int contextId) throws OXException {
        // Determine schema name
        String schemaName = getSchemaName(contextId);

        // Check for existence of GeomFromText function
        String functionName = CACHE_AS_TEXT.getIfPresent(schemaName);
        if (functionName == null) {
            Callable<String> loader = () -> {
                // Check for function name
                DatabaseService databaseService = ServerServiceRegistry.getInstance().getService(DatabaseService.class);
                if (databaseService == null) {
                    throw ServiceExceptionCode.absentService(DatabaseService.class);
                }

                Connection connection = databaseService.getReadOnly(contextId);
                try {
                    if (Databases.functionExists(connection, "ST_AsText", "0x000000000101000000000000000000F03F000000000000F03F", true)) {
                        return "ST_AsText";
                    }
                    if (Databases.functionExists(connection, "AsText", "0x000000000101000000000000000000F03F000000000000F03F", true)) {
                        return "AsText";
                    }
                    // No such function
                    return "";
                } finally {
                    databaseService.backReadOnly(contextId, connection);
                }
            };
            functionName = getFrom(schemaName, loader, CACHE_AS_TEXT);
        }
        return Strings.isEmpty(functionName) ? Optional.empty() : Optional.of(functionName);
    }

    private static final Cache<String, String> CACHE_GEOM_FROM_TEXT = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();

    /**
     * Gets the name of function for converting to internal geometry format from Well-Known Text (WKT) format.
     * <p>
     * Example:
     * <pre>
     *   INSERT INTO geom VALUES (ST_GeomFromText('POINT(1 1)'));
     * </pre>
     *
     * @param contextId The context identifier
     * @param defaulName The default function name to return in case database does not support the function or an error occurs
     * @return The name of the function or given default name
     * @see #getGeomFromTextName(int)
     */
    public static String getGeomFromTextName(int contextId, String defaulName) {
        try {
            Optional<String> optName = getGeomFromTextName(contextId);
            return optName.orElse(defaulName);
        } catch (OXException e) {
            return defaulName;
        }
    }

    /**
     * Gets the name of function for converting to internal geometry format from Well-Known Text (WKT) format.
     * <p>
     * Example:
     * <pre>
     *   INSERT INTO geom VALUES (ST_GeomFromText('POINT(1 1)'));
     * </pre>
     *
     * @param contextId The context identifier
     * @return The name of the function or empty no such function exists
     * @throws OXException If the function name cannot be determined
     */
    public static Optional<String> getGeomFromTextName(int contextId) throws OXException {
        // Determine schema name
        String schemaName = getSchemaName(contextId);

        // Check for existence of GeomFromText function
        String functionName = CACHE_GEOM_FROM_TEXT.getIfPresent(schemaName);
        if (functionName == null) {
            Callable<String> loader = () -> {
                // Check for function name
                DatabaseService databaseService = ServerServiceRegistry.getInstance().getService(DatabaseService.class);
                if (databaseService == null) {
                    throw ServiceExceptionCode.absentService(DatabaseService.class);
                }

                Connection connection = databaseService.getReadOnly(contextId);
                try {
                    if (Databases.functionExists(connection, "ST_GeomFromText", "POINT(1 1)", false)) {
                        return "ST_GeomFromText";
                    }
                    if (Databases.functionExists(connection, "GeomFromText", "POINT(1 1)", false)) {
                        return "GeomFromText";
                    }
                    // No such function
                    return "";
                } finally {
                    databaseService.backReadOnly(contextId, connection);
                }
            };
            functionName = getFrom(schemaName, loader, CACHE_GEOM_FROM_TEXT);
        }
        return Strings.isEmpty(functionName) ? Optional.empty() : Optional.of(functionName);
    }

    private static final Cache<Integer, String> CACHE_SCHEMA_NAMES = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();

    private static String getSchemaName(int contextId) throws OXException {
        String schemaName = CACHE_SCHEMA_NAMES.getIfPresent(Integer.valueOf(contextId));
        if (schemaName == null) {
            Callable<String> loader = () -> {
                DatabaseService databaseService = ServerServiceRegistry.getInstance().getService(DatabaseService.class);
                if (databaseService == null) {
                    throw ServiceExceptionCode.absentService(DatabaseService.class);
                }

                Connection connection = databaseService.getReadOnly(contextId);
                try {
                    String schemaName1 = connection.getCatalog();
                    if (null == schemaName1) {
                        schemaName1 = databaseService.getSchemaName(contextId);
                        if (null == schemaName1) {
                            throw InfostoreExceptionCodes.SQL_PROBLEM.create("No schema name for connection");
                        }
                    }
                    return schemaName1;
                } finally {
                    databaseService.backReadOnly(contextId, connection);
                }
            };
            schemaName = getFrom(Integer.valueOf(contextId), loader, CACHE_SCHEMA_NAMES);
        }
        return schemaName;
    }

    private static <K, V> V getFrom(K key, Callable<? extends V> loader, Cache<K, V> cache) throws OXException {
        try {
            return cache.get(key, loader);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                throw (OXException) cause;
            }
            if (cause instanceof SQLException) {
                throw InfostoreExceptionCodes.SQL_PROBLEM.create(cause, cause.getMessage());
            }
            throw InfostoreExceptionCodes.UNEXPECTED_ERROR.create(cause, cause.getMessage());
        } catch (UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            throw InfostoreExceptionCodes.UNEXPECTED_ERROR.create(cause, cause.getMessage());
        }
    }

}
