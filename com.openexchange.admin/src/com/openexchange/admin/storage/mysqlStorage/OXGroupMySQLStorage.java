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

package com.openexchange.admin.storage.mysqlStorage;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.PoolException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.services.AdminServiceRegistry;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;
import com.openexchange.admin.storage.sqlStorage.OXGroupSQLStorage;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.admin.tools.PropertyHandler;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFinishedListenerRegistry;
import com.openexchange.groupware.delete.DeleteRegistry;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.tools.oxfolder.OXFolderAdminHelper;

/**
 * @author d7
 *
 */
public class OXGroupMySQLStorage extends OXGroupSQLStorage implements OXMySQLDefaultValues {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OXGroupMySQLStorage.class);

    private final AdminCache cache;
    private final PropertyHandler prop;

    /**
     * Initializes a new {@link OXGroupMySQLStorage}.
     */
    public OXGroupMySQLStorage() {
        super();
        this.cache = ClientAdminThread.cache;
        this.prop = cache.getProperties();
    }

    private void changeLastModifiedOnGroup(int ctxId, int groupId, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("UPDATE `groups` SET `lastModified`=? WHERE `cid`=? AND `id`=?;");
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setInt(2, ctxId);
            stmt.setInt(3, groupId);
            stmt.executeUpdate();
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    private void createRecoveryData(int ctxId, Connection con, int groupId) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement("SELECT `identifier`,`displayName` FROM `groups` WHERE `id`=? AND `cid`=?");
            stmt.setInt(1, groupId);
            stmt.setInt(2, ctxId);
            result = stmt.executeQuery();

            stmt.close();

            stmt = con.prepareStatement("INSERT into `del_groups` (`id`,`cid`,`lastModified`,`identifier`,`displayName`) VALUES (?,?,?,?,?)");
            stmt.setInt(1, groupId);
            stmt.setInt(2, ctxId);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, "");
            stmt.setString(5, "");
            stmt.executeUpdate();
        } finally {
            Databases.closeSQLStuff(result, stmt);
        }
    }

    private Group get(final Context ctx, final Group grp, final Connection con) throws StorageException {
        PreparedStatement prep_list = null;
        ResultSet rs = null;
        final int context_ID = i(ctx.getId());
        try {
            prep_list = con.prepareStatement("SELECT `cid`,`identifier`,`displayName` FROM `groups` WHERE groups.cid = ? AND groups.id = ?");
            prep_list.setInt(1, context_ID);
            prep_list.setInt(2, i(grp.getId()));
            rs = prep_list.executeQuery();

            while (rs.next()) {
                final String ident = rs.getString("identifier");
                final String disp = rs.getString("displayName");
                grp.setName(ident);
                grp.setDisplayname(disp);
            }
            final Integer[] members = getMembers(ctx, i(grp.getId()), con);
            if (members != null) {
                grp.setMembers(members);
            }
        } catch (SQLException sql) {
            log.error("SQL Error", sql);
            throw new StorageException(sql.toString());
        } finally {
            Databases.closeSQLStuff(rs, prep_list);
        }
        return grp;
    }

    private Integer[] getMembers(Context ctx, int groupId, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        final int ctxId = i(ctx.getId());
        try {
            stmt = con.prepareStatement("SELECT `member` FROM `groups_member` WHERE `cid`=? AND `id`=?");
            stmt.setInt(1, ctxId);
            stmt.setInt(2, groupId);
            ResultSet result = stmt.executeQuery();
            List<Integer> retval = new ArrayList<Integer>();
            while (result.next()) {
                retval.add(I(result.getInt("member")));
            }
            return retval.toArray(new Integer[retval.size()]);
        } catch (SQLException sql) {
            log.error("SQL Error", sql);
            throw new StorageException(sql.toString());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    private void pushConnectionforContext(Connection con, int ctxId) {
        pushConnectionforContext(con, ctxId, false);
    }

    private void pushConnectionforContext(Connection con, int ctxId, boolean afterReading) {
        if (null != con) {
            try {
                if (afterReading) {
                    cache.pushConnectionForContextAfterReading(ctxId, con);
                } else {
                    cache.pushConnectionForContext(ctxId, con);
                }
            } catch (PoolException e) {
                log.error("Error pushing ox connection to pool!", e);
            }
        }
    }

    @Override
    public void addMember(Context ctx, int groupId, User[] members) throws StorageException {
        final int ctxId = i(ctx.getId());
        final Connection con;
        try {
            con = cache.getConnectionForContext(ctxId);
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        PreparedStatement stmt = null;
        try {
            con.setAutoCommit(false);
            stmt = con.prepareStatement("INSERT INTO `groups_member` VALUES (?,?,?);");
            stmt.setInt(1, ctxId);
            stmt.setInt(2, groupId);
            for (final User member : members) {
                stmt.setInt(3, i(member.getId()));
                stmt.addBatch();
            }
            stmt.executeBatch();
            // set last modified on group
            changeLastModifiedOnGroup(ctxId, groupId, con);
            Integer[] memberIds = new Integer[members.length];
            for (int i = 0; i < members.length; i++) {
                memberIds[i] = members[i].getId();
            }
            changeLastModifiedOfGroupMembers(ctx, con, memberIds);
            // let the groupware api know that the group has changed
            OXFolderAdminHelper.propagateGroupModification(groupId, con, con, ctxId);
            con.commit();
        } catch (DataTruncation dt) {
            log.error(AdminCache.DATA_TRUNCATION_ERROR_MSG, dt);
            Databases.rollback(con);
            throw AdminCache.parseDataTruncation(dt);
        } catch (SQLException sql) {
            log.error("SQL Error", sql);
            Databases.rollback(con);
            throw new StorageException(sql.toString());
        } finally {
            Databases.closeSQLStuff(stmt);
            pushConnectionforContext(con, ctxId);
        }
    }

    @Override
    public void change(final Context ctx, final Group grp) throws StorageException {
        final int ctxId = i(ctx.getId());
        final Connection con;
        try {
            con = cache.getConnectionForContext(ctxId);
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        PreparedStatement stmt = null;
        try {
            con.setAutoCommit(false);
            final int groupId = i(grp.getId());
            final String identifier = grp.getName();
            if (null != identifier) {
                stmt = con.prepareStatement("UPDATE `groups` SET `identifier`=? WHERE `cid`=? AND `id`=?");
                stmt.setString(1, identifier);
                stmt.setInt(2, ctxId);
                stmt.setInt(3, groupId);
                stmt.executeUpdate();
                stmt.close();
            }
            final String displayName = grp.getDisplayname();
            if (null != displayName) {
                stmt = con.prepareStatement("UPDATE `groups` SET `displayName`=? WHERE `cid`=? AND `id`=?");
                stmt.setString(1, displayName);
                stmt.setInt(2, ctxId);
                stmt.setInt(3, groupId);
                stmt.executeUpdate();
                stmt.close();
            }
            // check for members and add them after deleting old ones, cause we overwrite the members in this method (change)
            final Integer[] memberIds = grp.getMembers();
            if (memberIds != null) {
                // first delete all old members
                stmt = con.prepareStatement("DELETE FROM `groups_member` WHERE `cid`=? AND `id`=?");
                stmt.setInt(1, ctxId);
                stmt.setInt(2, groupId);
                stmt.executeUpdate();
                stmt.close();
                stmt = con.prepareStatement("INSERT INTO `groups_member` (`cid`,`id`,`member`) VALUES (?,?,?)");
                for (final Integer memberId : memberIds) {
                    stmt.setInt(1, ctxId);
                    stmt.setInt(2, groupId);
                    stmt.setInt(3, i(memberId));
                    stmt.addBatch();
                }
                stmt.executeBatch();
                stmt.close();
                changeLastModifiedOfGroupMembers(ctx, con, memberIds);
            } else if (grp.isMembersset()) {
                changeLastModifiedOfGroupMembers(ctx, con, getMembers(ctx, groupId, con));
                stmt = con.prepareStatement("DELETE FROM `groups_member` WHERE `cid`=? AND `id`=?");
                stmt.setInt(1, ctxId);
                stmt.setInt(2, groupId);
                stmt.executeUpdate();
                stmt.close();
            }
            // set last modified
            changeLastModifiedOnGroup(ctxId, groupId, con);
            con.commit();
            log.info("Group {} changed!", I(groupId));
        } catch (DataTruncation dt) {
            log.error(AdminCache.DATA_TRUNCATION_ERROR_MSG, dt);
            Databases.rollback(con);
            throw AdminCache.parseDataTruncation(dt);
        } catch (SQLException e) {
            log.error("SQL Error", e);
            Databases.rollback(con);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(stmt);
            pushConnectionforContext(con, ctxId);
        }
    }

    private void changeLastModifiedOfGroupMembers(Context ctx, Connection con, Integer[] memberIds) throws StorageException {
        final OXUserMySQLStorage oxu = new OXUserMySQLStorage();
        for (Integer memberId : memberIds) {
            oxu.changeLastModified(i(memberId), ctx, con);
        }
    }

    @Override
    public int create(Context ctx, Group group) throws StorageException {
        final int ctxId = i(ctx.getId());
        final Connection con;
        try {
            con = cache.getConnectionForContext(ctxId);
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        PreparedStatement stmt = null;
        final int groupId;
        try {
            con.setAutoCommit(false);
            String identifier = group.getName();
            String displayName = group.getDisplayname();
            groupId = IDGenerator.getId(ctxId, com.openexchange.groupware.Types.PRINCIPAL, con);
            con.commit();
            stmt = con.prepareStatement("INSERT INTO `groups` (`cid`,`id`,`identifier`,`displayName`,`lastModified`) VALUES (?,?,?,?,?)");
            stmt.setInt(1, ctxId);
            stmt.setInt(2, groupId);
            stmt.setString(3, identifier);
            stmt.setString(4, displayName);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
            stmt.close();
            // check for members and add them
            if (group.getMembers() != null && group.getMembers().length > 0) {
                stmt = con.prepareStatement("INSERT INTO `groups_member` (`cid`,`id`,`member`) VALUES (?,?,?)");
                stmt.setInt(1, ctxId);
                stmt.setInt(2, groupId);
                for (Integer memberId : group.getMembers()) {
                    stmt.setInt(3, i(memberId));
                    stmt.addBatch();
                }
                stmt.executeBatch();
                stmt.close();
                changeLastModifiedOfGroupMembers(ctx, con, group.getMembers());
            }
            con.commit();
            log.info("Group {} created!", I(groupId));
        } catch (DataTruncation dt) {
            log.error(AdminCache.DATA_TRUNCATION_ERROR_MSG, dt);
            Databases.rollback(con);
            throw AdminCache.parseDataTruncation(dt);
        } catch (SQLException sql) {
            log.error("SQL Error", sql);
            Databases.rollback(con);
            throw new StorageException(sql.toString());
        } finally {
            Databases.closeSQLStuff(stmt);
            pushConnectionforContext(con, ctxId);
        }
        return groupId;
    }

    @Override
    public void delete(Context ctx, Group[] groups) throws StorageException {
        final int ctxId = i(ctx.getId());
        final Connection con;
        try {
            con = cache.getConnectionForContext(ctxId);
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        try {
            final OXToolStorageInterface tool = OXToolStorageInterface.getInstance();
            tool.existsGroup(ctx, groups);
            com.openexchange.groupware.contexts.Context gwContext = ContextStorage.getInstance().getContext(ctxId);
            con.setAutoCommit(false);
            for (Group group : groups) {
                int groupId = i(group.getId());
                // let the groupware api know that the group will be deleted
                OXFolderAdminHelper.propagateGroupModification(groupId, con, con, ctxId);
                DeleteRegistry.getInstance().fireDeleteEvent(DeleteEvent.createDeleteEventForGroupDeletion(this, groupId, gwContext), con, con);

                final List<Integer> members = new LinkedList<Integer>();
                {
                    PreparedStatement stmt3 = null;
                    ResultSet rs3 = null;
                    try {
                        stmt3 = con.prepareStatement("SELECT `member` FROM `groups_member` WHERE `cid`=? AND `id`=?");
                        stmt3.setInt(1, ctxId);
                        stmt3.setInt(2, groupId);
                        rs3 = stmt3.executeQuery();
                        while (rs3.next()) {
                            members.add(Integer.valueOf(rs3.getInt(1)));
                        }
                    } finally {
                        Databases.closeSQLStuff(rs3, stmt3);
                    }
                }

                changeLastModifiedOfGroupMembers(ctx, con, getMembers(ctx, groupId, con));
                stmt1 = con.prepareStatement("DELETE FROM `groups_member` WHERE `cid`=? AND `id`=?");
                stmt1.setInt(1, ctxId);
                stmt1.setInt(2, groupId);
                stmt1.executeUpdate();
                stmt1.close();

                createRecoveryData(ctxId, con, groupId);

                stmt2 = con.prepareStatement("DELETE FROM `groups` WHERE `cid`=? AND `id`=?");
                stmt2.setInt(1, ctxId);
                stmt2.setInt(2, groupId);
                stmt2.executeUpdate();
                stmt2.close();

                // JCS
                final CacheService cacheService = AdminServiceRegistry.getInstance().getService(CacheService.class);
                if (null != cacheService) {
                    try {
                        final int contextId = ctx.getId().intValue();
                        for (final Integer userId : members) {
                            final CacheKey key = cacheService.newCacheKey(contextId, userId.intValue());
                            Cache cache = cacheService.getCache("User");
                            cache.remove(key);
                            cache = cacheService.getCache("UserPermissionBits");
                            cache.remove(key);
                            cache = cacheService.getCache("UserConfiguration");
                            cache.remove(key);
                            cache = cacheService.getCache("UserSettingMail");
                            cache.remove(key);
                        }
                    } catch (OXException e) {
                        log.error("", e);
                    }
                }
                // End of JCS
            }
            con.commit();

            for (Group group : groups) {
                try {
                    DeleteEvent delev = DeleteEvent.createDeleteEventForGroupDeletion(this, i(group.getId()), gwContext);
                    DeleteFinishedListenerRegistry.getInstance().fireDeleteEvent(delev);
                } catch (Exception e) {
                    log.warn("Failed to trigger delete finished listeners", e);
                }
                log.info("Group {} deleted!", group.getId());
            }
        } catch (SQLException e) {
            log.error("SQL Error", e);
            Databases.rollback(con);
            throw new StorageException(e.toString());
        } catch (OXException e) {
            log.error("Internal Error", e);
            Databases.rollback(con);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(stmt1);
            Databases.closeSQLStuff(stmt2);
            pushConnectionforContext(con, ctxId);
        }
    }

    @Override
    public void deleteAllRecoveryData(Context ctx, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        final int ctxId = i(ctx.getId());
        try {
            stmt = con.prepareStatement("DELETE FROM `del_groups` WHERE `cid`=?");
            stmt.setInt(1, ctxId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL Error", e);
            Databases.rollback(con);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public void deleteRecoveryData(Context ctx, int groupId, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        final int ctxId = i(ctx.getId());
        try {
            stmt = con.prepareStatement("DELETE FROM `del_groups` WHERE `id`=? AND `cid`=?");
            stmt.setInt(1, groupId);
            stmt.setInt(2, ctxId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL Error", e);
            Databases.rollback(con);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public Group get(Context ctx, Group group) throws StorageException {
        final Connection con;
        try {
            con = cache.getConnectionForContext(i(ctx.getId()));
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        try {
            return get(ctx, group, con);
        } finally {
            pushConnectionforContext(con, i(ctx.getId()), true);
        }
    }

    @Override
    public Group[] getGroupsForUser(Context ctx, User user) throws StorageException {
        final Connection con;
        try {
            con = cache.getConnectionForContext(i(ctx.getId()));
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("SELECT id FROM `groups_member` WHERE `cid`=? AND `member`=?");
            stmt.setInt(1, i(ctx.getId()));
            stmt.setInt(2, i(user.getId()));
            ResultSet result = stmt.executeQuery();
            List<Group> groups = new ArrayList<Group>();
            while (result.next()) {
                groups.add(get(ctx, new Group(I(result.getInt(1))), con));
            }
            return groups.toArray(new Group[groups.size()]);
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(stmt);
            pushConnectionforContext(con, i(ctx.getId()), true);
        }
    }

    @Override
    public User[] getMembers(Context ctx, int groupId) throws StorageException {
        final int ctxId = i(ctx.getId());
        final Connection con;
        try {
            con = cache.getConnectionForContext(ctxId);
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        try {
            final Integer[] memberIds = getMembers(ctx, groupId, con);
            User[] members = new User[memberIds.length];
            for (int i = 0; i < memberIds.length; i++) {
                members[i] = new User(i(memberIds[i]));
            }
            return members;
        } finally {
            pushConnectionforContext(con, ctxId, true);
        }
    }

    @Override
    public Group[] list(Context ctx, String pattern) throws StorageException {
        String sqlPattern = null;
        if (pattern != null) {
            sqlPattern = pattern.replace('*', '%');
        }
        final int ctxId = i(ctx.getId());
        final Connection con;
        try {
            con = cache.getConnectionForContext(ctxId);
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement("SELECT `cid`,`id`,`identifier`,`displayName` FROM `groups` WHERE `cid`=? AND (`identifier` LIKE ? OR `displayName` LIKE ?)");
            stmt.setInt(1, ctxId);
            stmt.setString(2, sqlPattern);
            stmt.setString(3, sqlPattern);
            result = stmt.executeQuery();
            List<Group> groups = new ArrayList<Group>();
            while (result.next()) {
                Group group = new Group(I(result.getInt(2)), result.getString(3), result.getString(4));
                Integer[] memberIds = getMembers(ctx, i(group.getId()), con);
                if (memberIds != null) {
                    group.setMembers(memberIds);
                }
                groups.add(group);
            }
            return groups.toArray(new Group[groups.size()]);
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(result);
            Databases.closeSQLStuff(stmt);
            pushConnectionforContext(con, ctxId, true);
        }
    }

    @Override
    public void removeMember(Context ctx, int groupId, User[] members) throws StorageException {
        final int ctxId = i(ctx.getId());
        final Connection con;
        try {
            con = cache.getConnectionForContext(ctxId);
        } catch (PoolException e) {
            log.error("Pool Error", e);
            throw new StorageException(e);
        }
        PreparedStatement stmt = null;
        try {
            con.setAutoCommit(false);
            stmt = con.prepareStatement("DELETE FROM `groups_member` WHERE `cid`=? AND `id`=? AND `member`=?");
            stmt.setInt(1, ctxId);
            stmt.setInt(2, groupId);
            for (User member : members) {
                stmt.setInt(3, i(member.getId()));
                stmt.addBatch();
            }
            stmt.executeBatch();
            // set last modified
            changeLastModifiedOnGroup(ctxId, groupId, con);
            Integer[] memberIds = new Integer[members.length];
            for (int i = 0; i < members.length; i++) {
                memberIds[i] = members[i].getId();
            }
            changeLastModifiedOfGroupMembers(ctx, con, memberIds);
            // let the groupware api know that the group has changed
            OXFolderAdminHelper.propagateGroupModification(groupId, con, con, ctxId);
            con.commit();
        } catch (SQLException e) {
            log.error("SQL Error", e);
            Databases.rollback(con);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(stmt);
            pushConnectionforContext(con, ctxId);
        }
    }
}
