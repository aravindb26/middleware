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

import static com.openexchange.admin.storage.mysqlStorage.AdminMySQLStorageUtil.leaseConnectionForContext;
import static com.openexchange.admin.storage.mysqlStorage.AdminMySQLStorageUtil.releaseWriteContextConnection;
import static com.openexchange.admin.storage.mysqlStorage.AdminMySQLStorageUtil.releaseWriteContextConnectionAfterReading;
import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Resource;
import com.openexchange.admin.rmi.dataobjects.ResourcePermission;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.storage.sqlStorage.OXResourceSQLStorage;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFinishedListenerRegistry;
import com.openexchange.groupware.delete.DeleteRegistry;
import com.openexchange.groupware.impl.IDGenerator;

/**
 * @author d7
 * @author cutmasta
 */
public class OXResourceMySQLStorage extends OXResourceSQLStorage implements OXMySQLDefaultValues {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OXResourceMySQLStorage.class);

    private final AdminCache cache;

    /**
     * Initialises a new {@link OXResourceMySQLStorage}.
     */
    public OXResourceMySQLStorage() {
        super();
        this.cache = ClientAdminThread.cache;
    }

    @Override
    public void change(Context ctx, Resource res) throws StorageException {
        int contextId = ctx.getId().intValue();
        int resourceId = res.getId().intValue();
        Connection con = null;
        PreparedStatement editres = null;
        boolean rollback = false;
        try {

            con = leaseConnectionForContext(contextId, cache);
            con.setAutoCommit(false);
            rollback = true;

            int edited_the_resource = 0;

            // update status of resource availability
            if (null != res.getAvailable()) {
                editres = con.prepareStatement("UPDATE resource SET available = ? WHERE cid = ? AND id = ?");
                editres.setBoolean(1, res.getAvailable().booleanValue());
                editres.setInt(2, contextId);
                editres.setInt(3, resourceId);
                editres.executeUpdate();
                editres.close();
                edited_the_resource++;
            }

            // update description of resource
            if (null == res.getDescription() && res.isDescriptionset()) {
                editres = con.prepareStatement("UPDATE resource SET description = ? WHERE cid = ? AND id = ?");
                editres.setNull(1, java.sql.Types.VARCHAR);
                editres.setInt(2, contextId);
                editres.setInt(3, resourceId);
                editres.executeUpdate();
                editres.close();
                edited_the_resource++;
            } else if (null != res.getDescription()) {
                editres = con.prepareStatement("UPDATE resource SET description = ? WHERE cid = ? AND id = ?");
                editres.setString(1, res.getDescription());
                editres.setInt(2, contextId);
                editres.setInt(3, resourceId);
                editres.executeUpdate();
                editres.close();
                edited_the_resource++;
            }

            // update mail of resource
            String mail = res.getEmail();
            if (null != mail) {
                editres = con.prepareStatement("UPDATE resource SET mail = ? WHERE cid = ? AND id = ?");
                editres.setString(1, mail);
                editres.setInt(2, contextId);
                editres.setInt(3, resourceId);
                editres.executeUpdate();
                editres.close();
                edited_the_resource++;
            }

            // Update displayName of resource
            String displayname = res.getDisplayname();
            if (null != displayname) {
                editres = con.prepareStatement("UPDATE resource SET displayName = ? WHERE cid = ? AND id = ?");
                editres.setString(1, displayname);
                editres.setInt(2, contextId);
                editres.setInt(3, resourceId);
                editres.executeUpdate();
                editres.close();
                edited_the_resource++;
            }

            // Check for possibly updating the name of resource
            String resourceName = res.getName();
            if (null == resourceName) {
                // Load the name of the resource for logging purpose
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try {
                    stmt = con.prepareStatement("SELECT identifier FROM resource WHERE cid = ? AND id = ?");
                    stmt.setInt(1, contextId);
                    stmt.setInt(2, resourceId);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        resourceName = rs.getString(1);
                    }
                } finally {
                    Databases.closeSQLStuff(rs, stmt);
                }
            } else {
                // Change the name of the resource
                editres = con.prepareStatement("UPDATE resource SET identifier = ? WHERE cid = ? AND id = ?");
                editres.setString(1, resourceName);
                editres.setInt(2, contextId);
                editres.setInt(3, resourceId);
                editres.executeUpdate();
                editres.close();
                edited_the_resource++;
            }

            if (res.isPermissionsSet()) {
                edited_the_resource += changePermissionsForResource(con, contextId, resourceId, res.getPermissions());
            }

            // Update last-modified time stamp if any modification performed
            if (edited_the_resource > 0) {
                changeLastModified(resourceId, ctx, con);
            }

            con.commit();
            rollback = false;

            log.info("Resource {} changed!", resourceName == null ? "" : resourceName);
        } catch (DataTruncation dt) {
            log.error(AdminCache.DATA_TRUNCATION_ERROR_MSG, dt);
            throw AdminCache.parseDataTruncation(dt);
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            if (rollback) {
                Databases.rollback(con);
            }
            Databases.closeSQLStuff(editres);
            releaseWriteContextConnection(con, ctx, cache);
        }
    }

    @Override
    public int create(Context ctx, Resource res) throws StorageException {
        int contextId = ctx.getId().intValue();
        Connection con = null;
        PreparedStatement prep_insert = null;
        boolean rollback = false;
        try {

            con = leaseConnectionForContext(contextId, cache);
            con.setAutoCommit(false);
            rollback = true;

            String identifier = res.getName();
            String displayName = res.getDisplayname();

            int available;
            if (null != res.getAvailable()) {
                available = res.getAvailable().booleanValue() ? 1 : 0;
            } else {
                // This is the default, so if this attribute of the object has never been
                // touched, we set this to true;
                available = 1;
            }

            String description = res.getDescription();
            String mail = res.getEmail();

            int resID = IDGenerator.getId(contextId, com.openexchange.groupware.Types.PRINCIPAL, con);

            prep_insert = con.prepareStatement("INSERT INTO resource (cid,id,identifier,displayName,available,description,lastModified,mail)VALUES (?,?,?,?,?,?,?,?);");
            prep_insert.setInt(1, contextId);
            prep_insert.setInt(2, resID);
            if (identifier != null) {
                prep_insert.setString(3, identifier);
            } else {
                prep_insert.setNull(3, Types.VARCHAR);
            }
            if (displayName != null) {
                prep_insert.setString(4, displayName);
            } else {
                prep_insert.setNull(4, Types.VARCHAR);
            }
            prep_insert.setInt(5, available);
            if (description != null) {
                prep_insert.setString(6, description);
            } else {
                prep_insert.setNull(6, Types.VARCHAR);
            }
            prep_insert.setLong(7, System.currentTimeMillis());
            if (mail != null) {
                prep_insert.setString(8, mail);
            } else {
                prep_insert.setNull(8, Types.VARCHAR);
            }

            prep_insert.executeUpdate();

            if (res.isPermissionsSet()) {
                createPermissionsForResource(con, contextId, resID, res.getPermissions());
            }

            con.commit();
            rollback = false;
            log.info("Resource {} created!", I(resID));
            return resID;
        } catch (DataTruncation dt) {
            log.error(AdminCache.DATA_TRUNCATION_ERROR_MSG, dt);
            throw AdminCache.parseDataTruncation(dt);
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            if (rollback) {
                Databases.rollback(con);
            }
            Databases.closeSQLStuff(prep_insert);
            releaseWriteContextConnection(con, ctx, cache);
        }
    }

    @Override
    public void delete(Context ctx, int resource_id) throws StorageException {
        int contextId = ctx.getId().intValue();
        Connection con = null;
        PreparedStatement prep_del = null;
        PreparedStatement stmt_perm = null;
        boolean rollback = false;
        try {
            con = leaseConnectionForContext(contextId, cache);
            con.setAutoCommit(false);
            rollback = true;

            DeleteEvent delev = DeleteEvent.createDeleteEventForResourceDeletion(this, resource_id, ContextStorage.getInstance().getContext(contextId));
            DeleteRegistry.getInstance().fireDeleteEvent(delev, con, con);

            createRecoveryData(resource_id, ctx, con);

            prep_del = con.prepareStatement("DELETE FROM resource WHERE cid=? AND id=?;");
            prep_del.setInt(1, contextId);
            prep_del.setInt(2, resource_id);
            prep_del.executeUpdate();

            stmt_perm = con.prepareStatement("DELETE FROM `resource_permissions` WHERE `cid` = ? AND `resource` = ?");
            stmt_perm.setInt(1, contextId);
            stmt_perm.setInt(2, resource_id);
            stmt_perm.executeUpdate();

            con.commit();
            rollback = false;

            try {
                DeleteFinishedListenerRegistry.getInstance().fireDeleteEvent(delev);
            } catch (Exception e) {
                log.warn("Failed to trigger delete finished listeners", e);
            }

            log.info("Resource {} deleted!", I(resource_id));
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } catch (OXException e) {
            log.error("Internal Error", e);
            throw new StorageException(e.toString());
        } finally {
            if (rollback) {
                Databases.rollback(con);
            }
            Databases.closeSQLStuff(prep_del, stmt_perm);
            releaseWriteContextConnection(con, ctx, cache);
        }
    }

    @Override
    public void delete(Context ctx, Resource resource) throws StorageException {
        delete(ctx, i(resource.getId()));
    }

    @Override
    public Resource getData(Context ctx, Resource resource) throws StorageException {
        int contextId = ctx.getId().intValue();
        Connection con = null;
        PreparedStatement prep_list = null;
        try {

            con = leaseConnectionForContext(contextId, cache);

            prep_list = con.prepareStatement("SELECT `cid`,`id`,`identifier`,`displayName`,`available`,`description`,`mail` FROM `resource` WHERE resource.cid = ? AND resource.id = ?");
            prep_list.setInt(1, contextId);
            prep_list.setInt(2, resource.getId().intValue());
            ResultSet rs = prep_list.executeQuery();

            if (!rs.next()) {
                throw new StorageException("No such resource");
            }
            int id = rs.getInt("id");
            String ident = rs.getString("identifier");
            String mail = rs.getString("mail");
            String disp = rs.getString("displayName");
            Boolean aval = Boolean.valueOf(rs.getBoolean("available"));
            String desc = rs.getString("description");

            Resource retval = (Resource) resource.clone();

            retval.setId(I(id));
            if (null != mail) {
                retval.setEmail(mail);
            }
            if (null != disp) {
                retval.setDisplayname(disp);
            }

            if (null != ident) {
                retval.setName(ident);
            }

            if (null != desc) {
                retval.setDescription(desc);
            }

            if (null != aval) {
                retval.setAvailable(aval);
            }

            applyPermissionsForResource(resource, con, contextId, id);

            return retval;

        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } catch (CloneNotSupportedException e) {
            log.error("", e);
            throw new StorageException(e);
        } finally {
            Databases.closeSQLStuff(prep_list);
            releaseWriteContextConnectionAfterReading(con, contextId, cache);
        }
    }

    @Override
    public Resource[] list(Context ctx, String pattern) throws StorageException {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement prep_list = null;
        String patterntemp = pattern.replace('*', '%');
        int contextId = ctx.getId().intValue();
        try {
            ArrayList<Resource> list = new ArrayList<Resource>();
            con = leaseConnectionForContext(contextId, cache);

            prep_list = con.prepareStatement("SELECT resource.mail,resource.cid,resource.id,resource.identifier,resource.displayName,resource.available,resource.description FROM resource WHERE resource.cid = ? AND (resource.identifier like ? OR resource.displayName = ?)");
            prep_list.setInt(1, contextId);
            prep_list.setString(2, patterntemp);
            prep_list.setString(3, patterntemp);
            rs = prep_list.executeQuery();
            while (rs.next()) {
                Resource res = new Resource();

                int id = rs.getInt("id");
                String ident = rs.getString("identifier");
                String mail = rs.getString("mail");
                String disp = rs.getString("displayName");
                Boolean aval = Boolean.valueOf(rs.getBoolean("available"));
                String desc = rs.getString("description");

                res.setId(I(id));
                if (null != mail) {
                    res.setEmail(mail);
                }
                if (null != disp) {
                    res.setDisplayname(disp);
                }

                if (null != ident) {
                    res.setName(ident);
                }

                if (null != desc) {
                    res.setDescription(desc);
                }

                if (null != aval) {
                    res.setAvailable(aval);
                }

                applyPermissionsForResource(res, con, contextId, id);

                list.add(res);
            }

            Resource[] retval = new Resource[list.size()];
            for (int i = 0; i < list.size(); i++) {
                retval[i] = list.get(i);
            }
            return retval;
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(rs, prep_list);
            releaseWriteContextConnectionAfterReading(con, contextId, cache);
        }
    }

    @Override
    public void changeLastModified(int resource_id, Context ctx, Connection write_ox_con) throws StorageException {
        PreparedStatement prep_edit_user = null;
        try {
            prep_edit_user = write_ox_con.prepareStatement("UPDATE resource SET lastModified=? WHERE cid=? AND id=?");
            prep_edit_user.setLong(1, System.currentTimeMillis());
            prep_edit_user.setInt(2, ctx.getId().intValue());
            prep_edit_user.setInt(3, resource_id);
            prep_edit_user.executeUpdate();
            prep_edit_user.close();
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(prep_edit_user);
        }
    }

    @Override
    public void createRecoveryData(int resource_id, Context ctx, Connection con) throws StorageException {
        // insert into del_resource table
        int context_id = ctx.getId().intValue();
        PreparedStatement del_st = null;
        try {
            del_st = con.prepareStatement("INSERT into del_resource (id,cid,lastModified) VALUES (?,?,?)");
            del_st.setInt(1, resource_id);
            del_st.setInt(2, context_id);
            del_st.setLong(3, System.currentTimeMillis());
            del_st.executeUpdate();
        } catch (DataTruncation dt) {
            log.error(AdminCache.DATA_TRUNCATION_ERROR_MSG, dt);
            throw AdminCache.parseDataTruncation(dt);
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(del_st);
        }
    }

    @Override
    public void deleteAllRecoveryData(Context ctx, Connection con) throws StorageException {
        // delete from del_resource table
        int context_id = ctx.getId().intValue();
        PreparedStatement del_st = null;
        try {
            del_st = con.prepareStatement("DELETE from del_resource WHERE cid = ?");
            del_st.setInt(1, context_id);
            del_st.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(del_st);
        }
    }

    @Override
    public void deleteRecoveryData(int resource_id, Context ctx, Connection con) throws StorageException {
        // delete from del_resource table
        int context_id = ctx.getId().intValue();
        PreparedStatement del_st = null;
        try {
            del_st = con.prepareStatement("DELETE from del_resource WHERE id = ? AND cid = ?");
            del_st.setInt(1, resource_id);
            del_st.setInt(2, context_id);
            del_st.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL Error", e);
            throw new StorageException(e.toString());
        } finally {
            Databases.closeSQLStuff(del_st);
        }
    }

    /**
     * Creates permissions for a resource
     *
     * @param con The database connection
     * @param contextId The context identifier
     * @param resourceId The resource identifier
     * @param permissions The permissions to create
     * @throws SQLException If an error occures while creating the permissions
     */
    private void createPermissionsForResource(Connection con, int contextId, int resourceId, List<ResourcePermission> permissions) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("INSERT INTO `resource_permissions` (`cid`,`resource`,`entity`,`group`,`privilege`) VALUES (?,?,?,?,UPPER(?))");
            stmt.setInt(1, contextId);
            stmt.setInt(2, resourceId);
            for (ResourcePermission perm : permissions) {
                stmt.setInt(3, perm.entity().intValue());
                stmt.setBoolean(4, perm.group().booleanValue());
                stmt.setString(5, perm.privilege());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    /**
     * Handles permission changes in given resource
     *
     * @param con The database connection
     * @param contextId The context identifier
     * @param resourceId The resource to change
     * @param permissions The list of the permissions
     * @return Count of changes for the resource
     * @throws SQLException If an error occures while applying the permissions
     */
    private int changePermissionsForResource(Connection con, int contextId, int resourceId, List<ResourcePermission> permissions) throws SQLException {
        int edited_the_resource = 0;
        if (null != permissions && false == permissions.isEmpty()) {
            PreparedStatement insert_stmt = null;
            PreparedStatement delete_stmt = null;
            try {
                StringBuilder delSQL = new StringBuilder("DELETE FROM `resource_permissions` WHERE `cid` = ? AND `resource` = ? AND `entity` NOT IN (");
                delSQL.append(permissions.stream().map(p -> String.valueOf(p.entity())).collect(Collectors.joining(","))).append(")");
                delete_stmt = con.prepareStatement(delSQL.toString());
                delete_stmt.setInt(1, contextId);
                delete_stmt.setInt(2, resourceId);

                insert_stmt = con.prepareStatement("INSERT INTO `resource_permissions` (`cid`,`resource`,`entity`,`group`,`privilege`) VALUES (?,?,?,?,UPPER(?)) ON DUPLICATE KEY UPDATE `entity`=?, `group`=?, `privilege`=UPPER(?)");
                insert_stmt.setInt(1, contextId);
                insert_stmt.setInt(2, resourceId);

                for (ResourcePermission perm : permissions) {
                    insert_stmt.setInt(3, i(perm.entity()));
                    insert_stmt.setBoolean(4, b(perm.group()));
                    insert_stmt.setString(5, perm.privilege());
                    insert_stmt.setInt(6, i(perm.entity()));
                    insert_stmt.setBoolean(7, b(perm.group()));
                    insert_stmt.setString(8, perm.privilege());
                    insert_stmt.addBatch();
                    edited_the_resource++;
                }
                insert_stmt.executeBatch();
                edited_the_resource += delete_stmt.executeUpdate();
            } finally {
                Databases.closeSQLStuff(insert_stmt, delete_stmt);
            }
        } else {
            PreparedStatement delete_stmt = null;
            try {
                delete_stmt = con.prepareStatement("DELETE FROM `resource_permissions` WHERE `cid` = ? AND `resource` = ?");
                delete_stmt.setInt(1, contextId);
                delete_stmt.setInt(2, resourceId);
                edited_the_resource += delete_stmt.executeUpdate();
            } finally {
                Databases.closeSQLStuff(delete_stmt);
            }
        }
        return edited_the_resource;
    }

    /**
     * Loads all permissions for a resource from database
     *
     * @param resource The resource
     * @param con The database connection
     * @param contextId The context identifier
     * @param resourceId The resource identifier
     * @throws SQLException If an error occures while getting the permissions
     */
    private void applyPermissionsForResource(Resource resource, Connection con, int contextId, int resourceId) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `entity`,`group`,`privilege` FROM `resource_permissions` WHERE `cid` = ? AND `resource` = ?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, resourceId);
            rs = stmt.executeQuery();
            List<ResourcePermission> permissions = new ArrayList<>();
            while (rs.next()) {
                int entity = rs.getInt(1);
                boolean group = rs.getBoolean(2);
                String privilege = rs.getString(3);
                ResourcePermission perm = new ResourcePermission(I(entity), B(group), privilege);
                permissions.add(perm);
            }
            resource.setPermissions(permissions);
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }
}
