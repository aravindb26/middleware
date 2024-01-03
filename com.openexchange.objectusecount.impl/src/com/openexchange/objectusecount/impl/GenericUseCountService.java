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
package com.openexchange.objectusecount.impl;

import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.objectusecount.ObjectUseCountExceptionCode;
import com.openexchange.objectusecount.BatchIncrementArguments.ObjectFolderAndModuleForAccount;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link GenericUseCountService}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.6
 */
class GenericUseCountService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GenericUseCountService.class);

    private final ServiceLookup services;

    GenericUseCountService(ServiceLookup services) {
        super();
        this.services = services;
    }

    /**
     * Deletes all use counts for specified account, e.g. if the user removes external account storage
     *
     * @param session The associated session
     * @param module The identifier of the module in which the object resides
     * @param accountId The identifier of the account
     * @throws OXException If deletion fails
     */
    void deleteUseCountsForAccount(Session session, int module, int accountId) throws OXException {
        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }

        Connection con = dbService.getWritable(session.getContextId());
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("DELETE FROM `generic_use_count` WHERE `cid` = ? AND `user` = ? AND `module` = ? AND `account` = ?");
            stmt.setInt(1, session.getContextId());
            stmt.setInt(2, session.getUserId());
            stmt.setInt(3, module);
            stmt.setInt(4, accountId);
            stmt.execute();
            LOG.debug("Deleted generic use counts for user {}, module {}, account {} in context {}.", I(session.getUserId()), I(module), I(accountId), I(session.getContextId()), new Throwable("generic-use-count-trace"));
        } catch (SQLException e) {
            throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
            dbService.backWritable(con);
        }
    }

    /**
     * Increments the use count(s) according to specified arguments
     *
     * @param session The associated session
     * @param module The identifier of the module in which the object resides
     * @param account The identifier of the account in which the object resides
     * @param folderId The identifier of the folder in which the object resides
     * @param objectId The identifier of the object
     * @param con An existing writeable connection to database or <code>null</code> to fetch a new one
     * @throws OXException If incrementing user count(s) fails and arguments signal to throw an error
     */
    void incrementUseCount(Session session, int module, int account, String folderId, String objectId, Connection con) throws OXException {
        if (null == con) {
            incrementUseCount(session, module, account, folderId, objectId);
            return;
        }

        long now = System.currentTimeMillis();
        PreparedStatement stmt = null;
        try {
             stmt = con.prepareStatement("INSERT INTO `generic_use_count` (`cid`,`uuid`,`user`,`module`,`account`,`folder`,`object`,`value`,`lastModified`) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `value`=`value`+1, `lastModified`=?");
             stmt.setInt(1, session.getContextId());
             stmt.setBytes(2, UUIDs.toByteArray(UUID.randomUUID()));
             stmt.setInt(3, session.getUserId());
             stmt.setInt(4, module);
             stmt.setInt(5, account);
             stmt.setString(6, folderId);
             stmt.setString(7, objectId);
             stmt.setInt(8, 1);
             stmt.setLong(9, now);
             stmt.setLong(10, now);
             stmt.execute();
             LOG.debug("Incremented generic use count for user {}, module {}, account {}, folder {}, object {} in context {}.", I(session.getUserId()), I(module), I(account), folderId, objectId, I(session.getContextId()), new Throwable("generic-use-count-trace"));
         } catch (SQLException e) {
             throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Increments the use count(s) according to specified arguments
     *
     * @param session The associated session
     * @param module The identifier of the module in which the object resides
     * @param account The identifier of the account in which the object resides
     * @param folderId The identifier of the folder in which the object resides
     * @param objectId The identifier of the object
     * @throws OXException If incrementing user count(s) fails and arguments signal to throw an error
     */
    void incrementUseCount(Session session, int module, int account, String folderId, String objectId) throws OXException {
        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }
        int contextId = session.getContextId();
        Connection con = dbService.getWritable(contextId);
        try {
            incrementUseCount(session, module, account, folderId, objectId, con);
        } finally {
            dbService.backWritable(contextId, con);
        }
    }

    /**
     * Get use count for objects
     *
     * @param session The associated session
     * @param module The identifier of the module in which the object resides
     * @param accountId The identifier of the account in which the object resides
     * @param folder The identifier of the folder in which the object resides
     * @param objects A list of object identifiers
     * @param con An existing connection to database or <code>null</code> to fetch a new one
     * @return The object's use count
     * @throws OXException If use count cannot be returned
     */
    Map<String, Integer> getUseCount(Session session, int module, int account, String folder, List<String> objects, Connection con) throws OXException {
        if (null == objects || objects.isEmpty()) {
            return Collections.emptyMap();
        }

        if (null == con) {
            return getUseCount(session, module, account, folder, objects);
        }

        Map<String, Integer> useCounts = new HashMap<String, Integer>(objects.size());
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuilder sb = new StringBuilder("SELECT `object`,`value` FROM `generic_use_count` WHERE `cid` = ? AND `user` = ? AND `module` = ? AND `account` = ? AND `folder` = ? AND `object`");
            sb.append(Databases.getPlaceholders(objects.size()));
            stmt = con.prepareStatement(sb.toString());
            int i = 1;
            stmt.setInt(i++, session.getContextId());
            stmt.setInt(i++, session.getUserId());
            stmt.setInt(i++, module);
            stmt.setInt(i++, account);
            stmt.setString(i++, folder);
            for (String object : objects) {
                stmt.setString(i++, object);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                useCounts.put(rs.getString(1), I(rs.getInt(2)));
            }
            return useCounts;
        } catch (SQLException e) {
            throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    /**
     * Get use count for objects
     *
     * @param session The associated see returned
     */
    Map<String, Integer> getUseCount(Session session, int module, int account, String folder, List<String> objects) throws OXException {
        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }
        int contextId = session.getContextId();
        Connection con = dbService.getReadOnly(contextId);
        try {
            return getUseCount(session, module, account, folder, objects, con);
        } finally {
            dbService.backReadOnly(con);
        }
    }

    /**
     * Set use count for object
     *
     * @param session The associated session
     * @param module The identifier of the module in which the object resides
     * @param account The identifier of the account in which the object resides
     * @param folderId The identifier of the folder in which the object resides
     * @param objectId The identifier of the object
     * @param value The value to set
     * @param con An existing writeable connection to database or <code>null</code> to fetch a new one
     * @throws OXException If reset operation fails
     */
    void setUseCount(Session session, int module, int account, String folderId, String objectId, int value, Connection con) throws OXException {
      if (null == con) {
          setUseCount(session, module, account, folderId, objectId, value);
          return;
      }

      int contextId = session.getContextId();
      int userId = session.getUserId();
      long now = System.currentTimeMillis();
      PreparedStatement stmt = null;
      try {
          stmt = con.prepareStatement("INSERT INTO `generic_use_count` (`cid`,`uuid`,`user`,`module`,`account`,`folder`,`object`,`value`,`lastModified`) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `value`=`value`+?, `lastModified`=?");
          stmt.setInt(1, contextId);
          stmt.setBytes(2, UUIDs.toByteArray(UUID.randomUUID()));
          stmt.setInt(3, userId);
          stmt.setInt(4, module);
          stmt.setInt(5, account);
          stmt.setString(6, folderId);
          stmt.setString(7, objectId);
          stmt.setInt(8, value);
          stmt.setLong(9, now);
          stmt.setInt(10, value);
          stmt.setLong(11, now);
          LOG.debug("Set generic use count to {} for user {}, module {}, account {}, folder {}, object {} in context {}", I(value), I(userId), I(module), I(account), folderId, objectId, I(contextId), new Throwable("generic-use-count-trace"));
      } catch (SQLException e) {
          throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
      } finally {
          closeSQLStuff(stmt);
      }
    }

    /**
     * Reset use count for object
     *
     * @param session The associated session
     * @param module The identifier of the module in which the object resides
     * @param account The identifier of the account in which the object resides
     * @param folderId The identifier of the folder in which the object resides
     * @param objectId The identifier of the object
     * @param value The value to set
     * @throws OXException If reset operation fails
     */
    void setUseCount(Session session, int module, int account, String folderId, String objectId, int value) throws OXException {
        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }
        int contextId = session.getContextId();
        Connection con = dbService.getWritable(contextId);
        try {
            setUseCount(session, module, account, folderId, objectId, value, con);
        } finally {
            dbService.backWritable(contextId, con);
        }
    }

    /**
     * Batch-increments all use counts for specified module, account, folder and object
     *
     * @param genericCounts Counts to increment in generic use count
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param con An existing writeable connection to database or <code>null</code> to fetch a new one
     * @throws OXException If batch incremenent fails
     */
    void batchIncrementUseCount(Map<ObjectFolderAndModuleForAccount, Integer> genericCounts, int userId, int contextId, Connection con) throws OXException {
        if (null == con) {
            batchIncrementUseCount(genericCounts, userId, contextId);
            return;
        }

        if (null == genericCounts || genericCounts.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("INSERT INTO `generic_use_count` (`cid`, `uuid`, `user`, `module`, `account`, `folder`, `object`, `value`, `lastModified`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" + " ON DUPLICATE KEY UPDATE `value`=`value` + ?, `lastModified` = ?");
            stmt.setInt(1, contextId);
            stmt.setInt(3, userId);

            Iterator<Entry<ObjectFolderAndModuleForAccount, Integer>> iterator = genericCounts.entrySet().iterator();
            int size = genericCounts.size();
            if (size > 1) {
                for (int i = size; i-- > 0;) {
                    Map.Entry<ObjectFolderAndModuleForAccount, Integer> entry = iterator.next();
                    ObjectFolderAndModuleForAccount key = entry.getKey();
                    int count = entry.getValue().intValue();
                    stmt.setBytes(2, UUIDs.toByteArray(UUID.randomUUID()));
                    stmt.setInt(4, key.getModule());
                    stmt.setInt(5, key.getAccountId());
                    stmt.setString(6, key.getFolderId());
                    stmt.setString(7, key.getObjectId());
                    stmt.setInt(8, count);
                    stmt.setLong(9, now);
                    stmt.setInt(10, count);
                    stmt.setLong(11, now);
                    stmt.addBatch();
                    LOG.debug("Added batch-increment generic use count for user {}, module {}, account {}, folder {}, object {} in context {}.", I(userId), I(key.getModule()), I(key.getAccountId()), key.getFolderId(), key.getObjectId(), I(contextId), new Throwable("generic-use-count-trace"));
                }
                stmt.executeBatch();
            } else {
                Map.Entry<ObjectFolderAndModuleForAccount, Integer> entry = iterator.next();
                ObjectFolderAndModuleForAccount key = entry.getKey();
                int count = entry.getValue().intValue();
                stmt.setBytes(2, UUIDs.toByteArray(UUID.randomUUID()));
                stmt.setInt(4, key.getModule());
                stmt.setInt(5, key.getAccountId());
                stmt.setString(6, key.getFolderId());
                stmt.setString(7, key.getObjectId());
                stmt.setInt(8, count);
                stmt.setLong(9, now);
                stmt.setInt(10, count);
                stmt.setLong(11, now);
                stmt.executeUpdate();
                LOG.debug("Incremented generic use count for user {}, module {}, account {}, folder {}, object {} in context {}.", I(userId), I(key.getModule()), I(key.getAccountId()), key.getFolderId(), key.getObjectId(), I(contextId), new Throwable("generic-use-count-trace"));
            }
        } catch (SQLException e) {
            throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Batch-increments all use counts for specified module, account, folder and object
     *
     * @param genericCounts Counts to increment in generic use count
     * @param userId The user identifier
     * @param contextId The context identifier
     * @throws OXException If batch incremenent fails
     */
    void batchIncrementUseCount(Map<ObjectFolderAndModuleForAccount, Integer> genericCounts, int userId, int contextId) throws OXException {
        if (null == genericCounts || genericCounts.isEmpty()) {
            return;
        }

        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }
        Connection con = dbService.getWritable(contextId);
        try {
            batchIncrementUseCount(genericCounts, userId, contextId, con);
        } finally {
            dbService.backWritable(contextId, con);
        }
    }

}
