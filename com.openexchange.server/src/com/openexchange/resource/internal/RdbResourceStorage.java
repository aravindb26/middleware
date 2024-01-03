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

package com.openexchange.resource.internal;

import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Autoboxing.Coll2i;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.resource.ResourcePermissionUtility.DEFAULT_PERMISSIONS;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.LdapExceptionCode;
import com.openexchange.groupware.ldap.LdapUtility;
import com.openexchange.java.Enums;
import com.openexchange.java.Strings;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceGroup;
import com.openexchange.resource.ResourcePermission;
import com.openexchange.resource.SchedulingPrivilege;
import com.openexchange.server.impl.DBPool;

/**
 * This class implements the resource storage using a relational database.
 */
public class RdbResourceStorage implements ExtendedResourceStorage {

    private static final String RPL_TABLE = "#TABLE#";

    private static final String TABLE_ACTIVE = "resource";

    private static final String TABLE_DELETED = "del_resource";

    /**
     * Default constructor.
     *
     * @param context Context.
     */
    public RdbResourceStorage() {
        super();
    }

    @Override
    public ResourceGroup getGroup(final int groupId, final Context context) throws OXException {
        final ResourceGroup[] groups = getGroups(new int[] { groupId }, context);
        if (null == groups || groups.length == 0) {
            throw ResourceExceptionCode.RESOURCEGROUP_NOT_FOUND.create(Integer.valueOf(groupId));
        }
        if (groups.length > 1) {
            throw ResourceExceptionCode.RESOURCEGROUP_CONFLICT.create(Integer.valueOf(groupId));
        }
        return groups[0];
    }

    @Override
    public ResourceGroup[] getGroups(final Context context) throws OXException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement("SELECT `id`,`identifier`,`displayName`,`available` FROM `resource_group` WHERE `cid` = ?");
            stmt.setInt(1, context.getContextId());
            result = stmt.executeQuery();
            if (result.next() == false) {
                return new ResourceGroup[0];
            }

            List<ResourceGroup> groups = new ArrayList<>();
            do {
                final ResourceGroup group = new ResourceGroup();
                int pos = 1;
                group.setId(result.getInt(pos++));
                group.setIdentifier(result.getString(pos++));
                group.setDisplayName(result.getString(pos++));
                group.setAvailable(result.getBoolean(pos++));
                group.setMember(getMember(con, group.getId(), context));
                groups.add(group);
            } while (result.next());
            return groups.toArray(new ResourceGroup[groups.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    /**
     * Reads multiple resource groups from the database.
     *
     * @param groupId array with unique identifier of the resource groups to read.
     * @return an array with the read resource groups.
     * @throws OXException if an error occurs.
     */
    private ResourceGroup[] getGroups(final int[] groupId, final Context context) throws OXException {
        if (null == groupId || groupId.length == 0) {
            return new ResourceGroup[0];
        }
        final StringBuilder ids = new StringBuilder(16);
        ids.append('(').append(groupId[0]);
        for (int i = 1; i < groupId.length; i++) {
            ids.append(',').append(groupId[i]);
        }
        ids.append(')');

        Connection con = DBPool.pickup(context);
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement(Databases.getIN("SELECT id,identifier,displayName,available FROM resource_group WHERE cid=? AND id IN (", groupId.length));
            int pos = 1;
            stmt.setLong(pos++, context.getContextId());
            for (int id : groupId) {
                stmt.setInt(pos++, id);
            }
            result = stmt.executeQuery();
            if (result.next() == false) {
                return new ResourceGroup[0];
            }

            List<ResourceGroup> groups = new ArrayList<>(groupId.length);
            do {
                final ResourceGroup group = new ResourceGroup();
                pos = 1;
                group.setId(result.getInt(pos++));
                group.setIdentifier(result.getString(pos++));
                group.setDisplayName(result.getString(pos++));
                group.setAvailable(result.getBoolean(pos++));
                group.setMember(getMember(con, group.getId(), context));
                groups.add(group);
            } while (result.next());
            return groups.toArray(new ResourceGroup[groups.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    /**
     * Reads the member of a resource group.
     *
     * @param con readable database connection.
     * @param groupId unique identifier of the resource group.
     * @return an array with all unique identifier of resource that are member of the resource group.
     * @throws SQLException if a database error occurs.
     */
    private int[] getMember(final Connection con, final int groupId, final Context context) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        final List<Integer> member = new ArrayList<>();
        try {
            stmt = con.prepareStatement("SELECT `member` FROM `resource_group_member` WHERE `cid` = ? AND `id` = ?");
            stmt.setLong(1, context.getContextId());
            stmt.setInt(2, groupId);
            result = stmt.executeQuery();
            while (result.next()) {
                member.add(Integer.valueOf(result.getInt(1)));
            }
        } finally {
            closeSQLStuff(result, stmt);
        }
        final int[] retval = new int[member.size()];
        for (int i = 0; i < member.size(); i++) {
            retval[i] = member.get(i).intValue();
        }
        return retval;
    }

    @Override
    public Resource getResource(final int resourceId, final Context context) throws OXException {
        return getResource(resourceId, context, null);
    }

    @Override
    public Resource getResource(int resourceId, Context context, Connection connection) throws OXException {
        Resource[] resources = null == connection ? getResources(new int[] { resourceId }, context) : getResources(connection, new int[] { resourceId }, context);
        if (resources.length == 0) {
            throw ResourceExceptionCode.RESOURCE_NOT_FOUND.create(Integer.valueOf(resourceId));
        }
        if (resources.length > 1) {
            throw ResourceExceptionCode.RESOURCE_CONFLICT.create(Integer.valueOf(resourceId));
        }
        return resources[0];
    }

    /**
     * Reads multiple resources from the database.
     *
     * @param resourceId array with unique identifier of the resources to read.
     * @return an array with the read resources.
     * @throws OXException if an error occurs.
     */
    private Resource[] getResources(final int[] resourceId, final Context context) throws OXException {
        Connection con = DBPool.pickup(context);
        try {
            return getResources(con, resourceId, context);
        } finally {
            DBPool.closeReaderSilent(context, con);
        }
    }

    /**
     * Reads multiple resources from the database.
     *
     * @param connection The database connection to use
     * @param resourceId array with unique identifier of the resources to read.
     * @return an array with the read resources.
     * @throws OXException if an error occurs.
     */
    private Resource[] getResources(Connection connection, final int[] resourceId, final Context context) throws OXException {
        if (null == resourceId || resourceId.length == 0) {
            return new Resource[0];
        }

        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = connection.prepareStatement(Databases.getIN("SELECT id,identifier,displayName,mail,available,description,lastModified FROM resource WHERE cid = ? AND id IN (", resourceId.length));
            int pos = 1;
            stmt.setLong(pos++, context.getContextId()); // cid
            for (int id : resourceId) {
                stmt.setInt(pos++, id);
            }
            result = stmt.executeQuery();
            if (result.next() == false) {
                // No such resources
                return new Resource[0];
            }

            List<Resource> resources = new ArrayList<>(resourceId.length);
            do {
                resources.add(createResourceFromEntry(context.getContextId(), connection, result));
            } while (result.next());
            return resources.toArray(new Resource[resources.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    @Override
    public ResourceGroup[] searchGroups(final String pattern, final Context context) throws OXException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement("SELECT id,identifier,displayName,available FROM resource_group WHERE cid=? AND identifier LIKE ?");
            stmt.setLong(1, context.getContextId());
            stmt.setString(2, pattern.replace('*', '%'));
            result = stmt.executeQuery();
            if (result.next() == false) {
                return new ResourceGroup[0];
            }

            List<ResourceGroup> groups = new ArrayList<>();
            do {
                ResourceGroup group = new ResourceGroup();
                int pos = 1;
                group.setId(result.getInt(pos++));
                group.setIdentifier(result.getString(pos++));
                group.setDisplayName(result.getString(pos++));
                group.setAvailable(result.getBoolean(pos++));
                group.setMember(getMember(con, group.getId(), context));
                groups.add(group);
            } while (result.next());
            return groups.toArray(new ResourceGroup[groups.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public Resource[] searchResources(final String pattern, final Context context) throws OXException {
        if (Strings.isEmpty(pattern)) {
            return new Resource[0];
        }

        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement("SELECT id,identifier,displayName,mail,available,description,lastModified FROM resource WHERE cid = ? AND (identifier LIKE ? OR displayName LIKE ?)");
            stmt.setLong(1, context.getContextId());
            stmt.setString(2, LdapUtility.prepareSearchPattern(pattern));
            stmt.setString(3, LdapUtility.prepareSearchPattern(pattern));
            result = stmt.executeQuery();
            if (result.next() == false) {
                return new Resource[0];
            }

            List<Resource> resources = new ArrayList<>();
            do {
                resources.add(createResourceFromEntry(context.getContextId(), con, result));
            } while (result.next());
            return resources.toArray(new Resource[resources.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public Resource getByIdentifier(String identifier, Context context) throws OXException {
        if (Strings.isEmpty(identifier)) {
            return null;
        }

        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement("SELECT `id`,`identifier`,`displayName`,`mail`,`available`,`description`,`lastModified` FROM resource WHERE cid = ? AND identifier = ?");
            stmt.setLong(1, context.getContextId());
            stmt.setString(2, identifier);
            result = stmt.executeQuery();
            return result.next() == false ? null : createResourceFromEntry(context.getContextId(), con, result);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public Resource getByMail(String mail, Context context) throws OXException {
        if (Strings.isEmpty(mail)) {
            return null;
        }

        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement("SELECT `id`,`identifier`,`displayName`,`mail`,`available`,`description`,`lastModified` FROM resource WHERE cid = ? AND mail = ?");
            stmt.setLong(1, context.getContextId());
            stmt.setString(2, mail);
            result = stmt.executeQuery();
            return result.next() == false ? null : createResourceFromEntry(context.getContextId(), con, result);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    private static final String SQL_SEARCH_RESOURCE_WITH_USECOUNT;
    private static final String SQL_SEARCH_RESOURCE_BY_MAIL_WITH_USECOUNT;

    static {
        StringBuilder b = new StringBuilder();
        b.append("SELECT res.id,res.identifier,res.displayName,res.mail,res.available,res.description,res.lastModified FROM resource AS res ")
            .append("LEFT JOIN principalUseCount AS uc ON res.cid=uc.cid AND res.id=uc.principal AND uc.user=? ").append("WHERE res.cid=? AND (res.identifier LIKE ? OR res.displayName LIKE ?) ")
        .append("ORDER BY uc.value DESC;");

        SQL_SEARCH_RESOURCE_WITH_USECOUNT = b.toString();

        b.setLength(0);
        b.append("SELECT res.id,res.identifier,res.displayName,res.mail,res.available,res.description,res.lastModified FROM resource AS res ")
        .append("LEFT JOIN principalUseCount AS uc ON res.cid=uc.cid AND res.id=uc.principal AND uc.user=? ")
        .append("WHERE res.cid=? AND mail LIKE ? ")
        .append("ORDER BY uc.value DESC;");

        SQL_SEARCH_RESOURCE_BY_MAIL_WITH_USECOUNT = b.toString();
    }

    @Override
    public Resource[] searchResources(final String pattern, final Context context, int userId) throws OXException {
        if (Strings.isEmpty(pattern)) {
            return new Resource[0];
        }
        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement(SQL_SEARCH_RESOURCE_WITH_USECOUNT);
            int index = 1;
            stmt.setInt(index++, userId);
            stmt.setLong(index++, context.getContextId());
            stmt.setString(index++, LdapUtility.prepareSearchPattern(pattern));
            stmt.setString(index, LdapUtility.prepareSearchPattern(pattern));
            result = stmt.executeQuery();
            if (result.next() == false) {
                return new Resource[0];
            }

            List<Resource> resources = new ArrayList<>();
            do {
                resources.add(createResourceFromEntry(context.getContextId(), con, result));
            } while (result.next());
            return resources.toArray(new Resource[resources.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public Resource[] searchResourcesByMail(final String pattern, final Context context, int userId) throws OXException {
        if (Strings.isEmpty(pattern)) {
            return new Resource[0];
        }
        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement(SQL_SEARCH_RESOURCE_BY_MAIL_WITH_USECOUNT);
            int index = 1;
            stmt.setInt(index++, userId);
            stmt.setLong(index++, context.getContextId());
            stmt.setString(index++, LdapUtility.prepareSearchPattern(pattern));
            result = stmt.executeQuery();
            if (result.next() == false) {
                return new Resource[0];
            }

            List<Resource> resources = new ArrayList<>();
            do {
                resources.add(createResourceFromEntry(context.getContextId(), con, result));
            } while (result.next());
            return resources.toArray(new Resource[resources.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public Resource[] searchResourcesByPrivilege(int[] entities, SchedulingPrivilege privilege, Context context) throws OXException {
        if (null == privilege || null == entities || 0 == entities.length) {
            return new Resource[0];
        }
        Connection con = null;
        try {
            con = DBPool.pickup(context);
        } catch (Exception e) {
            throw LdapExceptionCode.NO_CONNECTION.create(e).setPrefix("RES");
        }
        try {
            /*
             * lookup matching resource ids in database
             */
            Set<Integer> resourceIds = new HashSet<>();
            resourceIds.addAll(getResourceIdsWithPrivilegeForEntities(con, context.getContextId(), entities, privilege));
            /*
             * if default permissions are matched, also include ids from resources w/o stored permissions
             */
            if (matchesDefaultPermissions(entities, privilege)) {
                resourceIds.addAll(getResourceIdsWithoutPermissions(con, context.getContextId()));
            }
            /*
             * load & return associated resources
             */
            return getResources(Coll2i(resourceIds), context);
        } catch (SQLException e) {
            throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("RES");
        } finally {
            DBPool.closeReaderSilent(context, con);
        }
    }

    private static boolean matchesDefaultPermissions(int[] entities, SchedulingPrivilege privilege) {
        for (ResourcePermission permission : DEFAULT_PERMISSIONS) {
            if (permission.getSchedulingPrivilege().equals(privilege)) {
                if (com.openexchange.tools.arrays.Arrays.contains(entities, permission.getEntity())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Resource[] searchResourcesByMail(final String pattern, final Context context) throws OXException {
        if (Strings.isEmpty(pattern)) {
            return new Resource[0];
        }
        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement("SELECT id,identifier,displayName,mail,available,description,lastModified FROM resource WHERE cid = ? AND mail LIKE ?");
            stmt.setLong(1, context.getContextId());
            stmt.setString(2, LdapUtility.prepareSearchPattern(pattern));
            result = stmt.executeQuery();
            if (result.next() == false) {
                return new Resource[0];
            }

            List<Resource> resources = new ArrayList<>();
            do {
                resources.add(createResourceFromEntry(context.getContextId(), con, result));
            } while (result.next());
            return resources.toArray(new Resource[resources.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public Resource[] listModified(final Date modifiedSince, final Context context) throws OXException {
        return listModifiedOrDeleted(modifiedSince, context, "SELECT id,identifier,displayName,mail,available,description,lastModified FROM resource WHERE cid = ? AND lastModified > ?");
    }

    @Override
    public Resource[] listDeleted(final Date modifiedSince, final Context context) throws OXException {
        return listModifiedOrDeleted(modifiedSince, context, "SELECT id,identifier,displayName,mail,available,description,lastModified FROM del_resource WHERE cid = ? AND lastModified > ?");
    }

    private Resource[] listModifiedOrDeleted(final Date modifiedSince, final Context context, final String statement) throws OXException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        Connection con = DBPool.pickup(context);
        try {
            stmt = con.prepareStatement(statement);
            stmt.setLong(1, context.getContextId());
            stmt.setLong(2, modifiedSince.getTime());
            result = stmt.executeQuery();
            if (result.next() == false) {
                return new Resource[0];
            }

            List<Resource> resources = new ArrayList<>();
            do {
                resources.add(createResourceFromEntry(context.getContextId(), con, result));
            } while (result.next());
            return resources.toArray(new Resource[resources.size()]);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
    }

    /**
     * Creates a newly allocated {@link Resource resource} from current result set's entry.
     *
     * @param contextId The context identifier
     * @param connection The underlying database connection
     * @param result The result set with its cursor properly set
     * @return A newly allocated {@link Resource resource} from current result set's entry
     * @throws SQLException If an SQL error occurs
     */
    private Resource createResourceFromEntry(int contextId, Connection connection, final ResultSet result) throws SQLException {
        final Resource res = new Resource();
        int pos = 1;
        res.setIdentifier(result.getInt(pos++));// id
        res.setSimpleName(result.getString(pos++));// identifier
        res.setDisplayName(result.getString(pos++));// displayName
        {
            final String mail = result.getString(pos++); // mail
            if (result.wasNull()) {
                res.setMail(null);
            } else {
                res.setMail(mail);
            }
        }
        res.setAvailable(result.getBoolean(pos++));// available
        {
            final String desc = result.getString(pos++);// description
            if (result.wasNull()) {
                res.setDescription(null);
            } else {
                res.setDescription(desc);
            }
        }
        res.setLastModified(result.getLong(pos++));// lastModified
        ResourcePermission[] permissions = selectPermissions(connection, contextId, res.getIdentifier());
        res.setPermissions(null == permissions || 0 == permissions.length ? DEFAULT_PERMISSIONS : permissions);
        return res;
    }

    private static final String SQL_INSERT_RESOURCE = "INSERT INTO `" + RPL_TABLE + "` (`cid`,`id`,`identifier`,`displayName`,`mail`,`available`,`description`,`lastModified`) " + "VALUES (?,?,?,?,?,?,?,?)";

    @Override
    public void insertResource(final Context ctx, final Connection con, final Resource resource, final StorageType type) throws OXException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(Strings.replaceSequenceWith(SQL_INSERT_RESOURCE, RPL_TABLE, StorageType.ACTIVE.equals(type) ? TABLE_ACTIVE : TABLE_DELETED));
            int pos = 1;
            stmt.setInt(pos++, ctx.getContextId()); // cid
            stmt.setInt(pos++, resource.getIdentifier()); // id
            stmt.setString(pos++, resource.getSimpleName()); // identifier
            stmt.setString(pos++, resource.getDisplayName()); // displayName
            if (resource.getMail() == null) {
                stmt.setNull(pos++, Types.VARCHAR); // mail
            } else {
                stmt.setString(pos++, resource.getMail()); // mail
            }
            stmt.setBoolean(pos++, resource.isAvailable()); // available
            if (resource.getDescription() == null) {
                stmt.setNull(pos++, Types.VARCHAR); // description
            } else {
                stmt.setString(pos++, resource.getDescription()); // description
            }
            final long lastModified = System.currentTimeMillis();
            stmt.setLong(pos++, lastModified);// lastModified
            stmt.executeUpdate();
            if (StorageType.ACTIVE.equals(type)) {
                insertPermissions(ctx, con, resource.getIdentifier(), resource.getPermissions());
            }
            resource.setLastModified(lastModified);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e);
        } finally {
            Databases.closeSQLStuff(null, stmt);
        }
    }

    private static final String SQL_UPDATE_RESOURCE = "UPDATE `resource` SET `identifier` = ?, `displayName` = ?, `mail` = ?, `available` = ?, `description` = ?, `lastModified` = ? WHERE `cid` = ? AND `id` = ?";

    @Override
    public void updateResource(final Context ctx, final Connection con, final Resource resource) throws OXException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(SQL_UPDATE_RESOURCE);
            int pos = 1;
            stmt.setString(pos++, resource.getSimpleName()); // identifier
            stmt.setString(pos++, resource.getDisplayName()); // displayName
            if (resource.getMail() == null) {
                stmt.setNull(pos++, Types.VARCHAR); // mail
            } else {
                stmt.setString(pos++, resource.getMail()); // mail
            }
            stmt.setBoolean(pos++, resource.isAvailable()); // available
            if (resource.getDescription() == null) {
                stmt.setNull(pos++, Types.VARCHAR); // description
            } else {
                stmt.setString(pos++, resource.getDescription()); // description
            }
            final long lastModified = System.currentTimeMillis();
            stmt.setLong(pos++, lastModified);// lastModified
            stmt.setInt(pos++, ctx.getContextId()); // cid
            stmt.setInt(pos++, resource.getIdentifier()); // id
            stmt.executeUpdate();
            deletePermissions(ctx, con, resource.getIdentifier());
            insertPermissions(ctx, con, resource.getIdentifier(), resource.getPermissions());
            resource.setLastModified(lastModified);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e);
        } finally {
            Databases.closeSQLStuff(null, stmt);
        }
    }

    private static final String SQL_DELETE_RESOURCE = "DELETE FROM `resource` WHERE `cid` = ? AND `id` = ?";

    @Override
    public void deleteResourceById(final Context ctx, final Connection con, final int resourceId) throws OXException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(SQL_DELETE_RESOURCE);
            int pos = 1;
            stmt.setInt(pos++, ctx.getContextId()); // cid
            stmt.setInt(pos++, resourceId); // id
            stmt.executeUpdate();
            deletePermissions(ctx, con, resourceId);
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e);
        } finally {
            Databases.closeSQLStuff(null, stmt);
        }
    }

    @Override
    public int[] insertPermissions(Context ctx, Connection connection, int resourceId, ResourcePermission[] permissions) throws OXException {
        if (null == permissions || 0 == permissions.length || Arrays.equals(permissions, DEFAULT_PERMISSIONS)) {
            return new int[0];
        }
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("INSERT INTO `resource_permissions` (`cid`,`resource`,`entity`,`group`,`privilege`) VALUES (?,?,?,?,?);");
            stmt.setInt(1, ctx.getContextId());
            stmt.setInt(2, resourceId);
            for (ResourcePermission permission : permissions) {
                stmt.setInt(3, permission.getEntity());
                stmt.setBoolean(4, permission.isGroup());
                stmt.setString(5, permission.getSchedulingPrivilege().name());
                stmt.addBatch();
            }
            return stmt.executeBatch();
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e);
        } finally {
            closeSQLStuff(stmt);
        }
    }

    @Override
    public int deletePermissions(Context ctx, Connection connection, int resourceId) throws OXException {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("DELETE FROM `resource_permissions` WHERE `cid`=? AND `resource`=?;");
            stmt.setInt(1, ctx.getContextId());
            stmt.setInt(2, resourceId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e);
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private static ResourcePermission[] selectPermissions(Connection connection, int contextId, int resourceId) throws SQLException {
        List<ResourcePermission> permissions = new ArrayList<>();
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = connection.prepareStatement("SELECT p.entity,p.group,p.privilege FROM `resource_permissions` AS p WHERE p.cid=? AND p.resource=?;");
            stmt.setInt(1, contextId);
            stmt.setInt(2, resourceId);
            result = stmt.executeQuery();
            while (result.next()) {
                permissions.add(new ResourcePermission(result.getInt(1), result.getBoolean(2), Enums.parse(SchedulingPrivilege.class, result.getString(3), SchedulingPrivilege.NONE)));
            }
        } finally {
            closeSQLStuff(result, stmt);
        }
        return permissions.toArray(new ResourcePermission[permissions.size()]);
    }

    /**
     * Looks up the resources that have a reference to a specific user or group within their resource permissions.
     *
     * @param connection The connection to use
     * @param cid The context identifier
     * @param entity The entity identifier to lookup in the resource permissions table
     * @param group <code>true</code> if the entity refers to a group, <code>false</code>, otherwise
     * @return The resources, or an empty list if there are none
     */
    @Override
    public List<Resource> getResourceIdsWithPermissionsForEntity(Context ctx, Connection connection, int entity, boolean group) throws OXException {
        Set<Integer> resourceIds = new HashSet<>();
        String sql = "SELECT `resource` FROM `resource_permissions` WHERE `cid`=? AND `entity`=? AND `group`=?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ctx.getContextId());
            stmt.setInt(2, entity);
            stmt.setBoolean(3, group);
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    resourceIds.add(I(results.getInt(1)));
                }
            }
        } catch (SQLException e) {
            throw ResourceExceptionCode.SQL_ERROR.create(e);
        }
        int[] ids = Coll2i(resourceIds);
        if (ids == null || ids.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(getResources(connection, ids, ctx));
    }

    /**
     * Looks up the identifiers of those resources that have a specific scheduling privilege assigned to one of the passed entities.
     *
     * @param connection The connection to use
     * @param cid The context identifier
     * @param entities The entity identifiers to lookup in the resource permissions table
     * @param privilege The privilege to match
     * @return The identifiers of the resources, or an empty array if there are none
     */
    private static Set<Integer> getResourceIdsWithPrivilegeForEntities(Connection connection, int cid, int[] entities, SchedulingPrivilege privilege) throws SQLException {
        String sql = new StringBuilder()
            .append("SELECT `resource` FROM `resource_permissions` WHERE `cid`=? AND `entity`")
            .append(Databases.getPlaceholders(entities.length)).append(" AND `privilege`=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, cid);
            for (int entity : entities) {
                stmt.setInt(parameterIndex++, entity);
            }
            stmt.setString(parameterIndex++, privilege.name());
            return executeAndReadIntResults(stmt);
        }
    }

    /**
     * Looks up the identifiers of those resources that have no permission entries stored in the database.
     *
     * @param connection The connection to use
     * @param cid The context identifier
     * @return The identifiers of the resources, or an empty array if there are none
     */
    private static Set<Integer> getResourceIdsWithoutPermissions(Connection connection, int cid) throws SQLException {
        String sql = "SELECT r.id FROM `resource` AS r WHERE r.cid=? AND NOT EXISTS (SELECT 1 FROM `resource_permissions` AS p WHERE p.cid=? AND p.resource=r.id);";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, cid);
            stmt.setInt(2, cid);
            return executeAndReadIntResults(stmt);
        }
    }

    private static Set<Integer> executeAndReadIntResults(PreparedStatement stmt) throws SQLException {
        Set<Integer> values = new HashSet<>();
        try (ResultSet results = stmt.executeQuery()) {
            while (results.next()) {
                values.add(I(results.getInt(1)));
            }
        }
        return values;
    }

}
