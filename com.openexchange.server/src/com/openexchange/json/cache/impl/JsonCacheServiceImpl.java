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

package com.openexchange.json.cache.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.json.JSONException;
import org.json.JSONInputStream;
import org.json.JSONServices;
import org.json.JSONValue;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.database.Databases;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.java.AsciiReader;
import com.openexchange.java.UnsynchronizedPushbackReader;
import com.openexchange.json.cache.JsonCacheService;
import com.openexchange.json.cache.JsonCaches;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link JsonCacheServiceImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class JsonCacheServiceImpl implements JsonCacheService {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link JsonCacheServiceImpl}.
     */
    public JsonCacheServiceImpl(final ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public JSONValue get(final String id, final int userId, final int contextId) throws OXException {
        final JSONValue ret = opt(id, userId, contextId);
        if (null == ret) {
            throw AjaxExceptionCodes.JSON_ERROR.create(id);
        }
        return ret;
    }

    @Override
    public JSONValue opt(final String id, final int userId, final int contextId) throws OXException {
        final Connection con = Database.get(contextId, false);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT json FROM jsonCache WHERE cid=? AND user=? AND id=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setString(3, id);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                return null;
            }
            final UnsynchronizedPushbackReader reader = new UnsynchronizedPushbackReader(rs.getNCharacterStream(1));
            final int read = reader.read();
            // Check for possible JSON
            if (read < 0) {
                return null;
            }
            final char c = (char) read;
            reader.unread(c);
            if ('[' == c || '{' == c) {
                // Either starting JSON object or JSON array
                return JSONServices.parse(reader);
            }
            final String s = AJAXServlet.readFrom(reader);
            if ("null".equals(s)) {
                return null;
            }
            throw AjaxExceptionCodes.JSON_ERROR.create("Not a JSON value.");
        } catch (SQLException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
            Database.back(contextId, false, con);
        }
    }

    @Override
    public void delete(final String id, final int userId, final int contextId) throws OXException {
        final Connection con = Database.get(contextId, true);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("DELETE FROM jsonCache WHERE cid=? AND user=? AND id=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setString(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
            Database.back(contextId, true, con);
        }
    }

    @Override
    public void set(final String id, final JSONValue jsonValue, final long duration, final int userId, final int contextId) throws OXException {
        final Connection con = Database.get(contextId, true);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            /*
             * Check for remove operation (DELETE)
             */
            if (null == jsonValue) {
                stmt = con.prepareStatement("DELETE FROM jsonCache WHERE cid=? AND user=? AND id=?");
                stmt.setInt(1, contextId);
                stmt.setInt(2, userId);
                stmt.setString(3, id);
                stmt.executeUpdate();
                return;
            }
            /*
             * Perform INSERT or UPDATE
             */
            stmt = con.prepareStatement("SELECT 1 FROM jsonCache WHERE cid=? AND user=? AND id=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setString(3, id);
            rs = stmt.executeQuery();
            final boolean update = rs.next();
            Databases.closeSQLStuff(rs, stmt);
            rs = null;
            /*
             * Update or insert
             */
            final long now = System.currentTimeMillis();
            //final String asciiOnly = toJavaNotation(jsonValue.toString());
            if (update) {
                stmt = con.prepareStatement("UPDATE jsonCache SET json=?, size=?, lastUpdate=?, took=? WHERE cid=? AND user=? AND id=?");
                stmt.setNCharacterStream(1, new AsciiReader(new JSONInputStream(jsonValue, "US-ASCII")));
                stmt.setLong(2, jsonValue.length());
                stmt.setLong(3, now);
                if (duration < 0) {
                    stmt.setNull(4, Types.BIGINT);
                } else {
                    stmt.setLong(4, duration);
                }
                stmt.setInt(5, contextId);
                stmt.setInt(6, userId);
                stmt.setString(7, id);
            } else {
                stmt = con.prepareStatement("INSERT INTO jsonCache (cid,user,id,json,size,lastUpdate,took) VALUES (?,?,?,?,?,?,?)");
                stmt.setInt(1, contextId);
                stmt.setInt(2, userId);
                stmt.setString(3, id);
                stmt.setNCharacterStream(4, new AsciiReader(new JSONInputStream(jsonValue, "US-ASCII")));
                stmt.setLong(5, jsonValue.length());
                stmt.setLong(6, now);
                if (duration < 0) {
                    stmt.setNull(7, Types.BIGINT);
                } else {
                    stmt.setLong(7, duration);
                }
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
            Database.back(contextId, true, con);
        }
    }

    @Override
    public boolean setIfDifferent(final String id, final JSONValue jsonValue, final long duration, final int userId, final int contextId) throws OXException {
        if (null == jsonValue) {
            return false;
        }
        final Connection con = Database.get(contextId, true);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            /*
             * Perform INSERT or UPDATE
             */
            stmt = con.prepareStatement("SELECT json FROM jsonCache WHERE cid=? AND user=? AND id=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setString(3, id);
            rs = stmt.executeQuery();
            final boolean update;
            JSONValue prev = null;
            {
                if (rs.next()) {
                    update = true;
                    final UnsynchronizedPushbackReader reader = new UnsynchronizedPushbackReader(rs.getNCharacterStream(1));
                    if (!rs.wasNull()) { // Not NULL
                        final int read = reader.read();
                        // Check for possible JSON
                        if (read >= 0) {
                            try {
                                final char c = (char) read;
                                reader.unread(c);
                                if ('[' == c || '{' == c) {
                                    // Either starting JSON object or JSON array
                                    prev = JSONServices.parse(reader);
                                } else {
                                    final String s = AJAXServlet.readFrom(reader);
                                    if ("null".equals(s)) {
                                        prev = null;
                                    }
                                    throw AjaxExceptionCodes.JSON_ERROR.create("Not a JSON value.");
                                }
                            } catch (JSONException e) {
                                // Read invalid JSON data
                                prev = null;
                            }
                        }
                    }
                } else {
                    prev = null;
                    update = false;
                }
            }
            Databases.closeSQLStuff(rs, stmt);
            stmt = null;
            rs = null;
            /*
             * Update if differ
             */
            if (JsonCaches.areEqual(prev, jsonValue)) {
                return false;
            }
            /*
             * Update or insert
             */
            // final String asciiOnly = toJavaNotation(jsonValue.toString());
            final long now = System.currentTimeMillis();
            if (update) {
                stmt = con.prepareStatement("UPDATE jsonCache SET json=?, size=?, lastUpdate=?, took=? WHERE cid=? AND user=? AND id=?");
                stmt.setNCharacterStream(1, new AsciiReader(new JSONInputStream(jsonValue, "US-ASCII")));
                stmt.setLong(2, jsonValue.length());
                stmt.setLong(3, now);
                if (duration < 0) {
                    stmt.setNull(4, Types.BIGINT);
                } else {
                    stmt.setLong(4, duration);
                }
                stmt.setInt(5, contextId);
                stmt.setInt(6, userId);
                stmt.setString(7, id);
            } else {
                stmt = con.prepareStatement("INSERT INTO jsonCache (cid,user,id,json,size,lastUpdate,took) VALUES (?,?,?,?,?,?,?)");
                stmt.setInt(1, contextId);
                stmt.setInt(2, userId);
                stmt.setString(3, id);
                stmt.setNCharacterStream(4, new AsciiReader(new JSONInputStream(jsonValue, "US-ASCII")));
                stmt.setLong(5, jsonValue.length());
                stmt.setLong(6, now);
                if (duration < 0) {
                    stmt.setNull(7, Types.BIGINT);
                } else {
                    stmt.setLong(7, duration);
                }
            }
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
            Database.back(contextId, true, con);
        }
    }

    @Override
    public boolean lock(final String id, final int userId, final int contextId) throws OXException {
        final Connection con = Database.get(contextId, true);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean transactional = false;
        try {
            Databases.startTransaction(con);
            transactional = true;
            /*
             * Perform INSERT or UPDATE
             */
            stmt = con.prepareStatement("SELECT 1 FROM jsonCache WHERE cid=? AND user=? AND id=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setString(3, id);
            rs = stmt.executeQuery();
            final boolean update = rs.next();
            Databases.closeSQLStuff(rs, stmt);
            rs = null;
            final long now = System.currentTimeMillis();
            if (!update) {
                stmt = con.prepareStatement("INSERT INTO jsonCache (cid,user,id,json,size,inProgress,inProgressSince,lastUpdate) VALUES (?,?,?,?,?,1,?,?)");
                stmt.setInt(1, contextId);
                stmt.setInt(2, userId);
                stmt.setString(3, id);
                stmt.setString(4, "null"); // Dummy
                stmt.setLong(5, 0L);
                stmt.setLong(6, now);
                stmt.setLong(7, now);
                boolean inserted;
                try {
                    inserted = stmt.executeUpdate() > 0;
                } catch (Exception e) {
                    inserted = false;
                }
                if (inserted) {
                    return true;
                }
            }
            Databases.closeSQLStuff(stmt);
            stmt = con.prepareStatement("UPDATE jsonCache SET inProgress=1, inProgressSince=? WHERE cid=? AND user=? AND id=? AND inProgress=0");
            stmt.setLong(1, now);
            stmt.setInt(2, contextId);
            stmt.setInt(3, userId);
            stmt.setString(4, id);
            final boolean updated = (stmt.executeUpdate() > 0);
            con.commit();
            return updated;
        } catch (SQLException e) {
            if (transactional) {
                Databases.rollback(con);
            }
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            if (transactional) {
                Databases.rollback(con);
            }
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
            if (transactional) {
                Databases.autocommit(con);
            }
            Database.back(contextId, true, con);
        }
    }

    @Override
    public void unlock(final String id, final int userId, final int contextId) throws OXException {
        final Connection con = Database.get(contextId, true);
        PreparedStatement stmt = null;
        boolean transactional = false;
        try {
            Databases.startTransaction(con);
            transactional = true;
            stmt = con.prepareStatement("UPDATE jsonCache SET inProgress=0 WHERE cid=? AND user=? AND id=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setString(3, id);
            stmt.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            if (transactional) {
                Databases.rollback(con);
            }
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            if (transactional) {
                Databases.rollback(con);
            }
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
            if (transactional) {
                Databases.autocommit(con);
            }
            Database.back(contextId, true, con);
        }
    }

    private static String toJavaNotation(final String unicode) {
        final int length = unicode.length();
        final StringBuilder sb = new StringBuilder(length << 1);
        for (int i = 0; i < length; ++i) {
            final char a = unicode.charAt(i);
            if (a > 127) {
                final String hexString = Integer.toHexString(a);
                sb.append("\\u");
                if (2 == hexString.length()) {
                    sb.append("00");
                }
                sb.append(hexString);
            } else {
                sb.append(a);
            }
        }
        return sb.toString();
    }

    private static String abbreviate(final String str, final int offset, final int maxWidth) {
        if (str == null) {
            return null;
        }
        final int length = str.length();
        if (length <= maxWidth) {
            return str;
        }
        int off = offset;
        if (off > length) {
            off = length;
        }
        if ((length - off) < (maxWidth - 3)) {
            off = length - (maxWidth - 3);
        }
        if (off <= 4) {
            return str.substring(0, maxWidth - 3) + "...";
        }
        if ((off + (maxWidth - 3)) < length) {
            return "..." + abbreviate(str.substring(off), 0, maxWidth - 3);
        }
        return "..." + str.substring(length - (maxWidth - 3));
    }

}
