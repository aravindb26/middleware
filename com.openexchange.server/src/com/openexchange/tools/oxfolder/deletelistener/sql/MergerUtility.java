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

package com.openexchange.tools.oxfolder.deletelistener.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.tools.oxfolder.deletelistener.Permission;

/**
 * {@link MergerUtility}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class MergerUtility {

	/**
	 * Initializes a new {@link MergerUtility}
	 */
	private MergerUtility() {
		super();
	}

	private static final String SQL_REASSIGN_UPDATE_PERM = "UPDATE oxfolder_permissions "
			+ "SET fp = ?, orp = ?, owp = ?, odp = ?, admin_flag = ?, permission_id = ?, group_flag = 1 "
			+ "WHERE cid = ? AND permission_id = ? AND fuid = ?";

	/**
	 * Updates the permission assigned to specified <code>entity</code> with
	 * given <code>mergedPerm</code> and reassigns it to specified
	 * <code>setToEntity</code>.
	 *
	 * @param mergedPerm
	 *            The merged permission holding appropriate permission settings
	 * @param entity
	 *            The entity currently holding the permission
	 * @param setToEntity
	 *            The entity to which the permission shall be reassigned
	 * @param fuid
	 *            The folder ID
	 * @param cid
	 *            The context ID
	 * @param writeCon
	 *            A connection with write capability
	 * @throws SQLException
	 *             If a SQL error occurs
	 */
	public static void updatePermission(final Permission mergedPerm, final int entity, final int setToEntity,
			final int fuid, final int cid, final Connection writeCon) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = writeCon.prepareStatement(SQL_REASSIGN_UPDATE_PERM);
			stmt.setInt(1, mergedPerm.fp);
			stmt.setInt(2, mergedPerm.orp);
			stmt.setInt(3, mergedPerm.owp);
			stmt.setInt(4, mergedPerm.odp);
			stmt.setInt(5, mergedPerm.admin ? 1 : 0);
			stmt.setInt(6, setToEntity);
			stmt.setInt(7, cid);
			stmt.setInt(8, entity);
			stmt.setInt(9, fuid);
			stmt.executeUpdate();
		} finally {
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					System.err.println("Warning: Statement could not be properly closed");
					e.printStackTrace(System.err);
				}
			}
		}
	}

	private static final String SQL_SEL_PERM = "SELECT fp, orp, owp, odp, admin_flag "
			+ "FROM oxfolder_permissions WHERE cid = ? AND permission_id = ? AND fuid = ?";

	/**
	 * Generates a merged {@link Permission permission} object dependent on the
	 * fact if specified fallback entity already holds a permission on
	 * corresponding folder. If it does, value of <code>delete</code> is turned
	 * to hold <code>true</code> to indicate need for deletion, otherwise is
	 * left to <code>false</code>.
	 *
	 * @param entity
	 *            The entity's ID
	 * @param fallbackEntity
	 *            The fallback entity's ID
	 * @param fuid
	 *            The folder ID
	 * @param cid
	 *            The context ID
	 * @param readCon
	 *            A connection with read capability
	 * @param delete
	 *            The array to store whether a entity's permission has to be
	 *            deleted or not
	 * @return The merged permission assigned to specified fallback entity
	 * @throws SQLException
	 *             If a SQL error occurs
	 * @throws IllegalStateException
	 *             If neither a permission can be found for given entity nor fallback entity
	 */
	public static Permission getMergedPermission(final int entity, final int fallbackEntity, final int fuid,
			final int cid, final Connection readCon, final boolean[] delete) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = readCon.prepareStatement(SQL_SEL_PERM);
			stmt.setInt(1, cid);
			stmt.setInt(2, fallbackEntity);
			stmt.setInt(3, fuid);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				/*
				 * Merged permission is entity's permission since no permission
				 * is defined for fallback entity
				 */
				rs.close();
				stmt.close();
				stmt = readCon.prepareStatement(SQL_SEL_PERM);
				stmt.setInt(1, cid);
				stmt.setInt(2, entity);
				stmt.setInt(3, fuid);
				rs = stmt.executeQuery();
				if (!rs.next()) {
					/*
					 * No permission?!
					 */
				    throw new IllegalStateException(new StringBuilder(64).append("Entity ").append(entity).append(
                    " has no permission on folder ").append(fuid).append(" in context ").append(cid).toString());
				}
				delete[0] = false;
				return new Permission(fallbackEntity, fuid, rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs
						.getInt(5) > 0);
			}
			final Permission fallbackPerm = new Permission(fallbackEntity, fuid, rs.getInt(1), rs.getInt(2), rs
					.getInt(3), rs.getInt(4), rs.getInt(5) > 0);
			rs.close();
			stmt.close();
			stmt = readCon.prepareStatement(SQL_SEL_PERM);
			stmt.setInt(1, cid);
			stmt.setInt(2, entity);
			stmt.setInt(3, fuid);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				return fallbackPerm;
			}
			final Permission entityPerm = new Permission(entity, fuid, rs.getInt(1), rs.getInt(2), rs.getInt(3), rs
					.getInt(4), rs.getInt(5) > 0);
			/*
			 * Merge
			 */
			final Permission mergedPerm = new Permission(fallbackEntity, fuid,
					Math.max(fallbackPerm.fp, entityPerm.fp), Math.max(fallbackPerm.orp, entityPerm.orp), Math.max(
							fallbackPerm.owp, entityPerm.owp), Math.max(fallbackPerm.odp, entityPerm.odp),
					(fallbackPerm.admin || entityPerm.admin));
			delete[0] = true;
			return mergedPerm;
		} finally {
			if (null != rs) {
				try {
					rs.close();
					rs = null;
				} catch (SQLException e) {
					System.err.println("Warning: Result set could not be properly closed");
					e.printStackTrace(System.err);
				}
			}
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					System.err.println("Warning: Statement could not be properly closed");
					e.printStackTrace(System.err);
				}
			}
		}
	}

	private static final String SQL_DELETE_PERM = "DELETE FROM oxfolder_permissions "
			+ "WHERE cid = ? AND permission_id = ? AND fuid = ?";

	/**
	 * Deletes specified permission
	 *
	 * @param entity
	 *            The entity ID
	 * @param fuid
	 *            The folder ID
	 * @param cid
	 *            The context ID
	 * @param con
	 *            A connection to MySQL database
	 * @throws SQLException
	 *             If deletion fails due to a SQL error
	 */
	public static void deletePermission(final int entity, final int fuid, final int cid, final Connection con)
			throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement(SQL_DELETE_PERM);
			stmt.setInt(1, cid);
			stmt.setInt(2, entity);
			stmt.setInt(3, fuid);
			stmt.executeUpdate();
		} finally {
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					System.err.println("Warning: Statement could not be properly closed");
					e.printStackTrace(System.err);
				}
			}
		}
	}

	private static final String SQL_SELECT_ADMIN = "SELECT user FROM user_setting_admin WHERE cid = ?";

	/**
	 * Determines the ID of the user who is defined as admin for given context
	 * or <code>-1</code> if none found
	 *
	 * @param cid
	 *            The context ID
	 * @param con
	 *            A readable connection
	 * @return The ID of context admin or <code>-1</code> if none found
	 * @throws SQLException
	 *             If a SQL error occurs
	 */
	public static int getContextAdminID(final int cid, final Connection con) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.prepareStatement(SQL_SELECT_ADMIN);
			stmt.setInt(1, cid);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				return -1;
			}
			return rs.getInt(1);
		} finally {
			if (null != rs) {
				try {
					rs.close();
					rs = null;
				} catch (SQLException e) {
					System.err.println("Warning: Result set could not be properly closed");
					e.printStackTrace(System.err);
				}
			}
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					System.err.println("Warning: Statement could not be properly closed");
					e.printStackTrace(System.err);
				}
			}
		}
	}
}
