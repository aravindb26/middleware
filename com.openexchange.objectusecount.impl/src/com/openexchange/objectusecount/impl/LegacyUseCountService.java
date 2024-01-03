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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.objectusecount.ObjectUseCountExceptionCode;
import com.openexchange.objectusecount.BatchIncrementArguments.ObjectAndFolder;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;

/**
 * {@link LegacyUseCountService}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.6
 */
class LegacyUseCountService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LegacyUseCountService.class);

    private final ServiceLookup services;

    LegacyUseCountService(ServiceLookup services) {
        super();
        this.services = services;
    }

    /**
     * Get use count for object
     *
     * @param session The associated session
     * @param folderId The identifier of the folder in which the object resides
     * @param objectId The identifier of the object
     * @param con An existing connection to database or <code>null</code> to fetch a new one
     * @return The object's use count
     * @throws OXException If use count cannot be returned
     */
    int getUseCount(Session session, int folderId, int objectId, Connection con) throws OXException {
        if (null == con) {
            return getUseCount(session, folderId, objectId);
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `value` FROM `object_use_count` WHERE `cid` = ? AND `user` = ? AND `folder` = ? AND `object` = ?");
            stmt.setInt(1, session.getContextId());
            stmt.setInt(2, session.getUserId());
            stmt.setInt(3, folderId);
            stmt.setInt(4, objectId);
            rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    /**
     * Get use count for object
     *
     * @param session The associated session
     * @param folderId The identifier of the folder in which the object resides
     * @param objectId The identifier of the object
     * @return The object's use count
     * @throws OXException If use count cannot be returned
     */
    int getUseCount(Session session, int folderId, int objectId) throws OXException {
        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }

        Connection con = dbService.getReadOnly(session.getContextId());
        try {
            return getUseCount(session, folderId, objectId, con);
        } finally {
            dbService.backReadOnly(session.getContextId(), con);
        }
    }

    /**
     * Increment use count for specified objects
     *
     * @param contact2folder The objects to increment mapped by folder
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param con A writeable connection to database or <code>null</code> to fetch a new one
     * @throws OXException If incrementing use counts fails
     */
    void incrementUseCount(TIntIntMap contact2folder, int userId, int contextId, Connection con) throws OXException {
        if (null == contact2folder || contact2folder.isEmpty()) {
            return;
        }

        if (null == con) {
            incrementUseCount(contact2folder, userId, contextId);
            return;
        }

        long now = System.currentTimeMillis();
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("INSERT INTO `object_use_count` (`cid`, `user`, `folder`, `object`, `value`, `lastModified`) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value`=`value`+1, `lastModified`=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);

            TIntIntIterator iterator = contact2folder.iterator();
            int size = contact2folder.size();
            if (size > 1) {
                for (int i = size; i-- > 0;) {
                    iterator.advance();
                    int folderId = iterator.value();
                    int objectId = iterator.key();
                    stmt.setInt(3, folderId);
                    stmt.setInt(4, objectId);
                    stmt.setInt(5, 1);
                    stmt.setLong(6, now);
                    stmt.setLong(7, now);
                    stmt.addBatch();
                    LOG.debug("Added batch-increment legacy use count for user {}, folder {}, object {} in context {}.", I(userId), I(folderId), I(objectId), I(contextId), new Throwable("legacy-use-count-trace"));
                }
                stmt.executeBatch();
            } else {
                iterator.advance();
                int folderId = iterator.value();
                int objectId = iterator.key();
                stmt.setInt(3, folderId);
                stmt.setInt(4, objectId);
                stmt.setInt(5, 1);
                stmt.setLong(6, now);
                stmt.setLong(7, now);
                stmt.executeUpdate();
                LOG.debug("Incremented legacy use count for user {}, folder {}, object {} in context {}.", I(userId), I(folderId), I(objectId), I(contextId), new Throwable("legacy-use-count-trace"));
            }
        } catch (SQLException e) {
            throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Increment use count for specified objects
     *
     * @param object2folder The objects to increment mapped by folder
     * @param userId The user identifier
     * @param contextId The context identifier
     * @throws OXException If incrementing use counts fails
     */
    void incrementUseCount(TIntIntMap object2folder, int userId, int contextId) throws OXException {
        if (null == object2folder || object2folder.isEmpty()) {
            return;
        }

        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }
        Connection con = dbService.getWritable(contextId);
        try {
            incrementUseCount(object2folder, userId, contextId, con);
        } finally {
            dbService.backWritable(contextId, con);
        }
    }

    /**
     * Set use count for specified object
     *
     * @param folderId The folder identifier
     * @param objectId The object identifier
     * @param value The value to set
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param con A writeable connection to database or <code>null</code> to fetch a new one
     * @throws OXException If setting use counts fails
     */
    void setUseCount(int folderId, int objectId, int value, int userId, int contextId, Connection con) throws OXException {
        if (null == con) {
            setUseCount(folderId, objectId, value, userId, contextId);
            return;
        }

        long now = System.currentTimeMillis();
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("INSERT INTO `object_use_count` (`cid`, `user`, `folder`, `object`, `value`, `lastModified`) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value`=?, `lastModified`=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setInt(3, folderId);
            stmt.setInt(4, objectId);
            stmt.setInt(5, value);
            stmt.setLong(6, now);
            stmt.setInt(7, value);
            stmt.setLong(8, now);
            stmt.executeUpdate();
            LOG.debug("Set legacy use count to {} for user {}, folder {}, object {} in context {}", I(value), I(userId), I(folderId), I(objectId), I(contextId), new Throwable("legacy-use-count-trace"));
        } catch (SQLException e) {
            throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Set use count for specified object
     *
     * @param folderId The folder identifier
     * @param objectId The object identifier
     * @param value The value to set
     * @param userId The user identifier
     * @param contextId The context identifier
     * @throws OXException If setting use counts fails
     */
    void setUseCount(int folderId, int objectId, int value, int userId, int contextId) throws OXException {
        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }
        Connection con = dbService.getWritable(contextId);
        try {
            setUseCount(folderId, objectId, value, userId, contextId, con);
        } finally {
            dbService.backWritable(contextId, con);
        }
    }

    /**
     * Batch-increments use count for specified objects
     *
     * @param counts Values to add to use count mapped by object containing folder and object identifiers
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param con A writeable connection to database or <code>null</code> to fetch a new one
     * @throws OXException If batch-incrementing use counts fails
     */
    void batchIncrementUseCount(Map<ObjectAndFolder, Integer> counts, int userId, int contextId, Connection con) throws OXException {
        if (null == con) {
            batchIncrementUseCount(counts, userId, contextId);
            return;
        }

        if (null == counts || counts.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("INSERT INTO `object_use_count` (`cid`, `user`, `folder`, `object`, `value`, `lastModified`) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value`=`value` + ?, `lastModified` = ?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);

            Iterator<Entry<ObjectAndFolder, Integer>> iterator = counts.entrySet().iterator();
            int size = counts.size();
            if (size > 1) {
                for (int i = size; i-- > 0;) {
                    Map.Entry<ObjectAndFolder, Integer> entry = iterator.next();
                    ObjectAndFolder key = entry.getKey();
                    int count = entry.getValue().intValue();
                    stmt.setInt(3, key.getFolderId());
                    stmt.setInt(4, key.getObjectId());
                    stmt.setInt(5, count);
                    stmt.setLong(6, now);
                    stmt.setInt(7, count);
                    stmt.setLong(8, now);
                    stmt.addBatch();
                    LOG.debug("Added batch-increment legacy use count for user {}, folder {}, object {} in context {}.", I(userId), I(key.getFolderId()), I(key.getObjectId()), I(contextId), new Throwable("legacy-use-count-trace"));
                }
                stmt.executeBatch();
            } else {
                Map.Entry<ObjectAndFolder, Integer> entry = iterator.next();
                ObjectAndFolder key = entry.getKey();
                int count = entry.getValue().intValue();
                stmt.setInt(3, key.getFolderId());
                stmt.setInt(4, key.getObjectId());
                stmt.setInt(5, count);
                stmt.setLong(6, now);
                stmt.setInt(7, count);
                stmt.setLong(8, now);
                stmt.executeUpdate();
                LOG.debug("Incremented legacy use count for user {}, folder {}, object {} in context {}.", I(userId), I(key.getFolderId()), I(key.getObjectId()), I(contextId), new Throwable("legacy-use-count-trace"));
            }
        } catch (SQLException e) {
            throw ObjectUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    /**
     * Batch-increments use count for specified objects
     *
     * @param counts Values to add to use count mapped by object containing folder and object identifiers
     * @param userId The user identifier
     * @param contextId The context identifier
     * @throws OXException If batch-incrementing use counts fails
     */
    void batchIncrementUseCount(Map<ObjectAndFolder, Integer> counts, int userId, int contextId) throws OXException {
        if (null == counts || counts.isEmpty()) {
            return;
        }

        DatabaseService dbService = services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }
        Connection con = dbService.getWritable(contextId);
        try {
            batchIncrementUseCount(counts, userId, contextId, con);
        } finally {
            dbService.backWritable(contextId, con);
        }
    }

}
