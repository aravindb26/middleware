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

import static com.openexchange.database.Databases.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.config.ConfigurationService;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.database.DBPoolingExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;

/**
 * This class contains methods for handling the server name and identifier.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Server {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Server.class);

    private static final String PROPERTY_NAME = "SERVER_NAME";

    private static final String SELECT = "SELECT * FROM `server` WHERE `name`=?";

    private static final AtomicReference<String> SERVER_NAME_REF = new AtomicReference<String>(null);

    private static final AtomicReference<ConfigDatabaseService> CONFIG_DB_SERVICE_REF = new AtomicReference<ConfigDatabaseService>(null);

    /** The temporarily used server uid string until the database column has been initialized */
    private static final String NOT_SET = "<not set>";

    static void setConfigDatabaseService(final ConfigDatabaseService configDatabaseService) {
        CONFIG_DB_SERVICE_REF.set(configDatabaseService);
    }

    /**
     * Prevent instantiation
     */
    private Server() {
        super();
    }

    private static final record ServerId(int id, String uid) {  }

    private static final AtomicReference<ServerId> SERVER_ID = new AtomicReference<ServerId>(null);
    private static final ServerId UNRESOLVED_SERVER_ID = new ServerId(-1, null);

    /**
     * Gets the identifier of the registered server matching the configured <code>SERVER_NAME</code> property.
     *
     * @return The server identifier
     * @throws OXException If there is no such registered server matching configured <code>SERVER_NAME</code> property
     */
    public static int getServerId() throws OXException {
        return getOrLoadServerId().id();
    }

    /**
     * Gets the unique identifier of the registered server matching the configured <code>SERVER_NAME</code> property.
     *
     * @return The unique server identifier
     * @throws OXException If there is no such registered server matching configured <code>SERVER_NAME</code> property
     */
    public static String getServerUid() throws OXException {
        return getOrLoadServerId().uid();
    }

    /**
     * Initializes the server name using given configuration service.
     *
     * @param service The configuration service to use
     * @throws OXException If <code>SERVER_NAME</code> configuration property is missing
     */
    public static final void start(ConfigurationService service) throws OXException {
        String tmp = service.getProperty(PROPERTY_NAME);
        if (null == tmp || tmp.length() == 0) {
            throw DBPoolingExceptionCodes.NO_SERVER_NAME.create();
        }
        SERVER_NAME_REF.set(tmp);
    }

    /**
     * Gets the configured server name (see <code>SERVER_NAME</code> property in 'system.properties' file)
     *
     * @return The server name
     * @throws OXException If server name is absent
     */
    public static String getServerName() throws OXException {
        String tmp = SERVER_NAME_REF.get();
        if (null == tmp) {
            throw DBPoolingExceptionCodes.NOT_INITIALIZED.create(Server.class.getName());
        }
        return tmp;
    }

    /**
     * Gets or loads the identifier of the registered server matching the configured <code>SERVER_NAME</code> property.
     *
     * @return The server identifier
     * @throws OXException If there is no such registered server matching configured <code>SERVER_NAME</code> property
     */
    private static ServerId getOrLoadServerId() throws OXException {
        // Load if not yet done
        ServerId tmp = SERVER_ID.get();
        if (null == tmp) {
            synchronized (Server.class) {
                tmp = SERVER_ID.get();
                if (null == tmp) {
                    ServerId serverId = loadServerId(getServerName());
                    if (null == serverId || UNRESOLVED_SERVER_ID.equals(serverId)) {
                        throw DBPoolingExceptionCodes.NOT_RESOLVED_SERVER.create(getServerName());
                    }
                    tmp = serverId;
                    if (NOT_SET.equals(tmp.uid())) {
                        LOG.trace("Not remembering {} until initialized completely.", tmp);
                    } else {
                        SERVER_ID.set(tmp);
                        LOG.trace("Got server id: {}", tmp);
                    }
                }
            }
        }
        return tmp;
    }

    /**
     * Resolves specified server name to its registered identifier.
     *
     * @param name The server name; e.g. <code>"oxserver"</code>
     * @return The server identifier, or {@value #UNRESOLVED_SERVER_ID} if no such server is registered with specified name
     * @throws OXException If resolving the server name fails
     */
    private static ServerId loadServerId(String name) throws OXException {
        ConfigDatabaseService myService = CONFIG_DB_SERVICE_REF.get();
        if (null == myService) {
            throw DBPoolingExceptionCodes.NOT_INITIALIZED.create(Server.class.getName());
        }

        Connection con = myService.getReadOnly();
        try {
            return loadServerId(name, con);
        } finally {
            myService.backReadOnly(con);
        }
    }

    private static ServerId loadServerId(String name, Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement(SELECT);
            stmt.setString(1, name);
            result = stmt.executeQuery();
            if (false == result.next()) {
                return UNRESOLVED_SERVER_ID;
            }
            int server_id = -1;
            byte[] uuid = null;
            ResultSetMetaData metaData = result.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                if ("server_id".equals(columnName)) {
                    server_id = result.getInt(i);
                } else if ("uuid".equals(columnName)) {
                    uuid = result.getBytes(i);
                }
            }
            return new ServerId(server_id, null != uuid ? UUIDs.toUUID(uuid).toString() : NOT_SET);
        } catch (SQLException e) {
            throw DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

}