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
import java.util.ArrayList;
import java.util.List;
import com.openexchange.group.GroupStorage;
import com.openexchange.tools.oxfolder.deletelistener.CorruptPermission;

/**
 * {@link DetectCorruptPermissions}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class DetectCorruptPermissions {

	/**
	 * Initializes a new {@link DetectCorruptPermissions}
	 */
	private DetectCorruptPermissions() {
		super();
	}

	private static final String SQL_SEL_INVALID_USER_PERMS = "SELECT op.cid, op.fuid, op.permission_id FROM `oxfolder_permissions` as `op` "
			+ "WHERE op.cid = ? AND op.group_flag = 0 AND op.permission_id NOT IN ("
			+ "SELECT `id` FROM `user` AS `u` WHERE u.cid = op.cid)";

	/**
	 * Detects corrupt user permissions existing in folders' permission table
	 *
	 * @param cid
	 *            The context ID
	 * @param readCon
	 *            A readable connection to the database to be checked
	 * @return An array of {@link CorruptPermission} if any; otherwise
	 *         <code>null</code>
	 * @throws SQLException
	 *             If corresponding SQL select statement fails
	 */
	public static CorruptPermission[] detectCorruptUserPermissions(final int cid, final Connection readCon)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = readCon.prepareStatement(SQL_SEL_INVALID_USER_PERMS);
			stmt.setInt(1, cid);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				return null;
			}
			final List<CorruptPermission> l = new ArrayList<CorruptPermission>();
			do {
				l.add(new CorruptPermission(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
			} while (rs.next());
			return l.toArray(new CorruptPermission[l.size()]);
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

    private static final String SQL_SEL_INVALID_GROUP_PERM = "SELECT `cid`, `fuid`, `permission_id` FROM `oxfolder_permissions` AS `op` "
        + "WHERE op.cid = ? AND op.group_flag = 1 AND op.permission_id != " + GroupStorage.GROUP_ZERO_IDENTIFIER + " AND "
        + "op.permission_id != " + GroupStorage.GUEST_GROUP_IDENTIFIER + " AND op.permission_id NOT IN ("
        + "SELECT `id` FROM `groups` AS `g` WHERE g.cid = op.cid)";

	/**
	 * Detects corrupt group permissions existing in folders' permission table
	 *
	 * @param cid
	 *            The context ID
	 * @param readCon
	 *            A readable connection to the database to be checked
	 * @return An array of {@link CorruptPermission} if any; otherwise
	 *         <code>null</code>
	 * @throws SQLException
	 *             If corresponding SQL select statement fails
	 */
	public static CorruptPermission[] detectCorruptGroupPermissions(final int cid, final Connection readCon)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = readCon.prepareStatement(SQL_SEL_INVALID_GROUP_PERM);
			stmt.setInt(1, cid);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				return null;
			}
			final List<CorruptPermission> l = new ArrayList<CorruptPermission>();
			do {
				l.add(new CorruptPermission(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
			} while (rs.next());
			return l.toArray(new CorruptPermission[l.size()]);
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
