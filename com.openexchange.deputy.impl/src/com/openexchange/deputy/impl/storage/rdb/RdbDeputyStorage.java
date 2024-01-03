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

package com.openexchange.deputy.impl.storage.rdb;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONServices;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.deputy.DefaultDeputyInfo;
import com.openexchange.deputy.DeputyExceptionCode;
import com.openexchange.deputy.DeputyInfo;
import com.openexchange.deputy.impl.storage.DeputyStorage;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;


/**
 * {@link RdbDeputyStorage} - The database-backed implementation of deputy storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class RdbDeputyStorage implements DeputyStorage {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link RdbDeputyStorage}.
     *
     * @param services The service look-up
     */
    public RdbDeputyStorage(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public DeputyInfo store(int entityId, boolean group, boolean sendOnBehalfOf, Collection<String> moduleIds, Session session) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        Connection con = databaseService.getWritable(session.getContextId());
        try {
            return store(entityId, group, sendOnBehalfOf, moduleIds, session, con);
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            databaseService.backWritable(session.getContextId(), con);
        }
    }

    private static DeputyInfo store(int entityId, boolean group, boolean sendOnBehalfOf, Collection<String> moduleIds, Session session, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        try {
            UUID uuid = UUID.randomUUID();
            stmt = con.prepareStatement("INSERT INTO `deputy` (`uuid`, `cid`, `user`, `entity`, `groupFlag`, `sendOnBehalfOf`, `moduleIds`) VALUES (?, ?, ?, ?, ?, ?, ?)");
            stmt.setBytes(1, UUIDs.toByteArray(uuid));
            stmt.setInt(2, session.getContextId());
            stmt.setInt(3, session.getUserId());
            stmt.setInt(4, entityId);
            stmt.setInt(5, group ? 1 : 0);
            stmt.setInt(6, sendOnBehalfOf ? 1 : 0);
            stmt.setString(7, new JSONArray(moduleIds).toString());
            stmt.executeUpdate();
            return DefaultDeputyInfo.builder()
                .withUserId(session.getUserId())
                .withDeputyId(UUIDs.getUnformattedString(uuid))
                .withEntityId(entityId)
                .withGroup(group)
                .withModuleIds(moduleIds)
                .withSendOnBehalfOf(sendOnBehalfOf)
                .build();
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public DeputyInfo update(String deputyId, boolean sendOnBehalfOf, Collection<String> moduleIds, Session session) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        Connection con = databaseService.getWritable(session.getContextId());
        try {
            boolean updated = update(deputyId, sendOnBehalfOf, moduleIds, session, con);
            if (updated == false) {
                throw DeputyExceptionCode.NO_SUCH_DEPUTY.create(deputyId);
            }
            return get(deputyId, session, con);
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (JSONException e) {
            throw DeputyExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            databaseService.backWritable(session.getContextId(), con);
        }
    }

    private static boolean update(String deputyId, boolean sendOnBehalfOf, Collection<String> moduleIds, Session session, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("UPDATE `deputy` SET `sendOnBehalfOf`=?, `moduleIds`=? WHERE `uuid`=? AND `cid`=? AND `user`=?");
            stmt.setInt(1, sendOnBehalfOf ? 1 : 0);
            stmt.setString(2, new JSONArray(moduleIds).toString());
            stmt.setBytes(3, UUIDs.toByteArray(UUIDs.fromUnformattedString(deputyId)));
            stmt.setInt(4, session.getContextId());
            stmt.setInt(5, session.getUserId());
            return stmt.executeUpdate() > 0;
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public void delete(String deputyId, Session session) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        boolean deleted = false;
        Connection con = databaseService.getWritable(session.getContextId());
        try {
            deleted = delete(deputyId, session, con);
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (deleted) {
                databaseService.backWritable(session.getContextId(), con);
            } else {
                databaseService.backWritableAfterReading(session.getContextId(), con);
            }
        }
    }

    private static boolean delete(String deputyId, Session session, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("DELETE FROM `deputy` WHERE `uuid`=? AND `cid`=? AND `user`=?");
            stmt.setBytes(1, UUIDs.toByteArray(UUIDs.fromUnformattedString(deputyId)));
            stmt.setInt(2, session.getContextId());
            stmt.setInt(3, session.getUserId());
            return stmt.executeUpdate() > 0;
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public List<DeputyInfo> list(Session session) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        ListResult result;
        Connection con = databaseService.getReadOnly(session.getContextId());
        try {
            result = list(session, con);
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (JSONException e) {
            throw DeputyExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            databaseService.backReadOnly(session.getContextId(), con);
        }

        deleteSafely(result.uuidsToDrop, session, databaseService);
        return result.deputies;
    }

    private static ListResult list(Session session, Connection con) throws SQLException, JSONException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `uuid`, `entity`, `groupFlag`, `sendOnBehalfOf`, `moduleIds` FROM `deputy` WHERE `cid`=? AND `user`=?");
            stmt.setInt(1, session.getContextId());
            stmt.setInt(2, session.getUserId());
            rs = stmt.executeQuery();
            if (rs.next() == false) {
                return EMPTY_LIST_RESULT;
            }

            List<DeputyInfo> deputies = new LinkedList<>();
            List<byte[]> uuidsToDrop = null;
            do {
                byte[] uuidBytes = rs.getBytes(1);
                JSONArray jModuleIds = JSONServices.parseArray(rs.getString(5));
                if (jModuleIds.length() > 0) {
                    deputies.add(DefaultDeputyInfo.builder()
                        .withUserId(session.getUserId())
                        .withDeputyId(UUIDs.getUnformattedString(UUIDs.toUUID(uuidBytes)))
                        .withEntityId(rs.getInt(2))
                        .withGroup(rs.getInt(3) > 0)
                        .withSendOnBehalfOf(rs.getInt(4) > 0)
                        .withModuleIds(jModuleIds.stream().map(Object::toString).collect(Collectors.toList()))
                        .build());
                } else {
                    // No modules...
                    if (uuidsToDrop == null) {
                        uuidsToDrop = new LinkedList<>();
                    }
                    uuidsToDrop.add(uuidBytes);
                }
            } while (rs.next());
            return new ListResult(deputies, uuidsToDrop);
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    private static void deleteSafely(List<byte[]> uuidsToDrop, Session session, DatabaseService databaseService) throws OXException {
        if (uuidsToDrop == null || uuidsToDrop.isEmpty()) {
            return;
        }

        boolean deleted = false;
        Connection con = databaseService.getWritable(session.getContextId());
        try {
            deleted = deleteSafely(uuidsToDrop, session, con);
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (deleted) {
                databaseService.backWritable(session.getContextId(), con);
            } else {
                databaseService.backWritableAfterReading(session.getContextId(), con);
            }
        }
    }

    private static boolean deleteSafely(List<byte[]> uuidsToDrop, Session session, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("DELETE FROM `deputy` WHERE `uuid`=? AND `cid`=? AND `user`=?");
            stmt.setInt(2, session.getContextId());
            stmt.setInt(3, session.getUserId());
            for (byte[] uuidBytes : uuidsToDrop) {
                stmt.setBytes(1, uuidBytes);
                stmt.addBatch();
            }
            int[] modifiedRows = stmt.executeBatch();
            for (int modified : modifiedRows) {
                if (modified > 0) {
                    return true;
                }
            }
            return false;
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public Map<Integer, List<DeputyInfo>> listReverse(Session session) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        Connection con = databaseService.getReadOnly(session.getContextId());
        try {
            return listReverse(session, con);
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (JSONException e) {
            throw DeputyExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            databaseService.backReadOnly(session.getContextId(), con);
        }
    }

    private Map<Integer, List<DeputyInfo>> listReverse(Session session, Connection con) throws SQLException, JSONException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `uuid`, `entity`, `groupFlag`, `sendOnBehalfOf`, `moduleIds`, `user` FROM `deputy` LEFT JOIN `groups_member` ON deputy.cid = groups_member.cid AND deputy.entity = groups_member.id WHERE deputy.cid=? AND (deputy.entity=? OR groups_member.member=?)");
            stmt.setInt(1, session.getContextId());
            stmt.setInt(2, session.getUserId());
            stmt.setInt(3, session.getUserId());
            rs = stmt.executeQuery();
            if (rs.next() == false) {
                return Collections.emptyMap();
            }

            Map<Integer, List<DeputyInfo>> grantee2DeputyPermission = new LinkedHashMap<>();
            do {
                Integer grantee = I(rs.getInt(6));
                List<DeputyInfo> infos = grantee2DeputyPermission.get(grantee);
                if (infos == null) {
                    infos = new ArrayList<DeputyInfo>(2);
                    grantee2DeputyPermission.put(grantee, infos);
                }
                infos.add(DefaultDeputyInfo.builder()
                    .withUserId(grantee.intValue())
                    .withDeputyId(UUIDs.getUnformattedString(UUIDs.toUUID(rs.getBytes(1))))
                    .withEntityId(rs.getInt(2))
                    .withGroup(rs.getInt(3) > 0)
                    .withSendOnBehalfOf(rs.getInt(4) > 0)
                    .withModuleIds(JSONServices.parseArray(rs.getString(5)).stream().map(Object::toString).collect(Collectors.toList()))
                    .build());
            } while (rs.next());
            return grantee2DeputyPermission;
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public List<DeputyInfo> listReverse(int granteeId, Session session) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        Connection con = databaseService.getReadOnly(session.getContextId());
        try {
            return listReverse(granteeId, session, con);
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (JSONException e) {
            throw DeputyExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            databaseService.backReadOnly(session.getContextId(), con);
        }
    }

    private List<DeputyInfo> listReverse(int granteeId, Session session, Connection con) throws SQLException, JSONException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `uuid`, `entity`, `groupFlag`, `sendOnBehalfOf`, `moduleIds` FROM `deputy` LEFT JOIN `groups_member` ON deputy.cid = groups_member.cid AND deputy.entity = groups_member.id WHERE deputy.cid=? AND deputy.user=? AND (deputy.entity=? OR groups_member.member=?)");
            stmt.setInt(1, session.getContextId());
            stmt.setInt(2, granteeId);
            stmt.setInt(3, session.getUserId());
            stmt.setInt(4, session.getUserId());
            rs = stmt.executeQuery();
            if (rs.next() == false) {
                return Collections.emptyList();
            }

            List<DeputyInfo> infos = new ArrayList<DeputyInfo>(4);
            do {
                infos.add(DefaultDeputyInfo.builder()
                    .withUserId(granteeId)
                    .withDeputyId(UUIDs.getUnformattedString(UUIDs.toUUID(rs.getBytes(1))))
                    .withEntityId(rs.getInt(2))
                    .withGroup(rs.getInt(3) > 0)
                    .withSendOnBehalfOf(rs.getInt(4) > 0)
                    .withModuleIds(JSONServices.parseArray(rs.getString(5)).stream().map(Object::toString).collect(Collectors.toList()))
                    .build());
            } while (rs.next());
            return infos;
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public DeputyInfo get(String deputyId, Session session) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        Connection con = databaseService.getReadOnly(session.getContextId());
        try {
            DeputyInfo deputyInfo = get(deputyId, session, con);
            if (deputyInfo == null) {
                throw DeputyExceptionCode.NO_SUCH_DEPUTY.create(deputyId);
            }
            return deputyInfo;
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (JSONException e) {
            throw DeputyExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            databaseService.backReadOnly(session.getContextId(), con);
        }
    }

    private DeputyInfo get(String deputyId, Session session, Connection con) throws SQLException, JSONException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `entity`, `groupFlag`, `sendOnBehalfOf`, `moduleIds` FROM `deputy` WHERE `uuid`=? AND `cid`=? AND `user`=?");
            stmt.setBytes(1, UUIDs.toByteArray(UUIDs.fromUnformattedString(deputyId)));
            stmt.setInt(2, session.getContextId());
            stmt.setInt(3, session.getUserId());
            rs = stmt.executeQuery();
            if (rs.next() == false) {
                return null;
            }
            return DefaultDeputyInfo.builder()
                .withUserId(session.getUserId())
                .withDeputyId(deputyId)
                .withEntityId(rs.getInt(1))
                .withGroup(rs.getInt(2) > 0)
                .withSendOnBehalfOf(rs.getInt(3) > 0)
                .withModuleIds(JSONServices.parseArray(rs.getString(4)).stream().map(Object::toString).collect(Collectors.toList()))
                .build();
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public boolean exists(String deputyId, int contextId) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        Connection con = databaseService.getReadOnly(contextId);
        try {
            return exists(deputyId, contextId, con);
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            databaseService.backReadOnly(contextId, con);
        }
    }

    private static boolean exists(String deputyId, int contextId, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT 1 FROM `deputy` WHERE `uuid`=? AND `cid`=?");
            stmt.setBytes(1, UUIDs.toByteArray(UUIDs.fromUnformattedString(deputyId)));
            stmt.setInt(2, contextId);
            rs = stmt.executeQuery();
            return rs.next();
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final ListResult EMPTY_LIST_RESULT = new ListResult(Collections.emptyList(), null);

    private static class ListResult {

        final List<DeputyInfo> deputies;
        final List<byte[]> uuidsToDrop;

        ListResult(List<DeputyInfo> deputies, List<byte[]> uuidsToDrop) {
            super();
            this.deputies = deputies;
            this.uuidsToDrop = uuidsToDrop;
        }
    }
}
