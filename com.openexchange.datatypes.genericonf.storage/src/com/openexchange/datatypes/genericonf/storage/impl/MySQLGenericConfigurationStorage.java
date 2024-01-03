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

package com.openexchange.datatypes.genericonf.storage.impl;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.database.Databases;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.datatypes.genericonf.storage.GenericConfigStorageExceptionCode;
import com.openexchange.datatypes.genericonf.storage.GenericConfigurationStorageService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.impl.IDGenerator;

/**
 * {@link MySQLGenericConfigurationStorage}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class MySQLGenericConfigurationStorage implements GenericConfigurationStorageService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MySQLGenericConfigurationStorage.class);

    private DBProvider provider;

    public void setDBProvider(final DBProvider provider) {
        this.provider = provider;
    }

    @Override
    public int save(final Context ctx, final Map<String, Object> content) throws OXException {
        return save(null, ctx, content);
    }

    @Override
    public int save(final Connection con, final Context ctx, final Map<String, Object> content) throws OXException {
        return write(con, ctx, new TX<Integer>() {

            @Override
            public Integer perform() throws SQLException {
                final Connection con = getConnection();

                final InsertIterator insertIterator = new InsertIterator();
                insertIterator.prepareStatements(this);

                final int id = IDGenerator.getId(ctx, Types.GENERIC_CONFIGURATION, con);
                final int cid = ctx.getContextId();
                insertIterator.setIds(cid, id);

                Tools.iterate(content, insertIterator);
                insertIterator.close();
                insertIterator.throwException();
                return I(id);
            }

        }).intValue();
    }

    private <R> R write(final Connection optCon, final Context ctx, final TX<R> tx) throws OXException {
        if (optCon != null) {
            try {
                tx.setConnection(optCon);
                return tx.perform();
            } catch (SQLException x) {
                LOG.error("", x);
                throw GenericConfigStorageExceptionCode.SQLException.create(x, x.getMessage());
            } finally {
                tx.close();
            }
        }

        int rollback = 0;
        Connection writeCon = provider.getWriteConnection(ctx);
        try {
            writeCon.setAutoCommit(false);
            rollback = 1;

            R retval = write(writeCon, ctx, tx);

            writeCon.commit();
            rollback = 2;
            return retval;
        } catch (SQLException x) {
            LOG.error("", x);
            throw GenericConfigStorageExceptionCode.SQLException.create(x, x.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(writeCon);
                }
                Databases.autocommit(writeCon);
            }
            provider.releaseWriteConnection(ctx, writeCon);
        }
    }

    @Override
    public void fill(final Context ctx, final int id, final Map<String, Object> content) throws OXException {
        fill(null, ctx, id, content);
    }

    @Override
    public void fill(final Connection optCon, final Context ctx, final int id, final Map<String, Object> content) throws OXException {
        if (optCon != null) {
            loadValues(optCon, ctx, id, content, "genconf_attributes_strings");
            loadValues(optCon, ctx, id, content, "genconf_attributes_bools");
            return;
        }

        Connection readCon = provider.getReadConnection(ctx);
        try {
            loadValues(readCon, ctx, id, content, "genconf_attributes_strings");
            loadValues(readCon, ctx, id, content, "genconf_attributes_bools");
        } finally {
            provider.releaseReadConnection(ctx, readCon);
        }
    }

    private void loadValues(final Connection readCon, final Context ctx, final int id, final Map<String, Object> content, final String tablename) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = readCon.prepareStatement("SELECT name, value FROM " + tablename + " WHERE cid = ? AND id = ?");
            stmt.setInt(1, ctx.getContextId());
            stmt.setInt(2, id);
            rs = stmt.executeQuery();
            while (rs.next()) {
                final String name = rs.getString("name");
                final Object value = rs.getObject("value");

                content.put(name, value);
            }
        } catch (SQLException x) {
            throw GenericConfigStorageExceptionCode.SQLException.create(x, null == stmt ? x.getMessage() : stmt.toString());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public void update(final Context ctx, final int id, final Map<String, Object> content) throws OXException {
        update(null, ctx, id, content);
    }

    @Override
    public void update(final Connection con, final Context ctx, final int id, final Map<String, Object> content) throws OXException {
        final Map<String, Object> original = new HashMap<String, Object>();
        fill(con, ctx, id, original);

        write(con, ctx, new TX<Void>() {

            @Override
            public Void perform() throws SQLException {
                final UpdateIterator updateIterator = new UpdateIterator();
                try {
                    updateIterator.prepareStatements(this);
                    updateIterator.setIds(ctx.getContextId(), id);
                    updateIterator.setOriginal(original);

                    Tools.iterate(content, updateIterator);
                } finally {
                    updateIterator.close();
                }
                updateIterator.throwException();
                return null;
            }
        });

    }

    @Override
    public void delete(final Context ctx, final int id) throws OXException {
        delete(null, ctx, id);
    }

    @Override
    public void delete(final Connection con, final Context ctx, final int id) throws OXException {
        write(con, ctx, new TX<Void>() {

            @Override
            public Void perform() throws SQLException {
                clearTable("genconf_attributes_strings");
                clearTable("genconf_attributes_bools");
                return null;
            }

            private void clearTable(final String tablename) throws SQLException {
                PreparedStatement delete = null;
                delete = prepare("DELETE FROM " + tablename + " WHERE cid = ? AND id = ?");
                delete.setInt(1, ctx.getContextId());
                delete.setInt(2, id);
                delete.executeUpdate();
            }
        });
    }

    @Override
    public void delete(final Connection con, final Context ctx) throws OXException {

        write(con, ctx, new TX<Void>() {

            @Override
            public Void perform() throws SQLException {
                clearTable("genconf_attributes_strings");
                clearTable("genconf_attributes_bools");
                return null;
            }

            private void clearTable(final String tablename) throws SQLException {
                PreparedStatement delete = null;
                delete = prepare("DELETE FROM " + tablename + " WHERE cid = ?");
                delete.setInt(1, ctx.getContextId());
                delete.executeUpdate();
            }
        });

    }

    @Override
    public List<Integer> search(final Context ctx, final Map<String, Object> query) throws OXException {
        return search(null, ctx, query);
    }

    @Override
    public List<Integer> search(Connection optCon, final Context ctx, final Map<String, Object> query) throws OXException {
        if (optCon != null) {
            return doSearch(optCon, ctx, query);
        }

        Connection con = provider.getReadConnection(ctx);
        try {
            return doSearch(optCon, ctx, query);
        } finally {
            provider.releaseReadConnection(ctx, con);
        }
    }

    private List<Integer> doSearch(Connection con, final Context ctx, final Map<String, Object> query) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            SearchIterator whereIterator = new SearchIterator();
            Tools.iterate(query, whereIterator);

            StringBuilder builder = new StringBuilder("SELECT p.id FROM ");
            builder.append(whereIterator.getFrom());
            builder.append(" WHERE ");
            builder.append('(');
            builder.append(whereIterator.getWhere());
            builder.append(") AND p.cid = ?");
            whereIterator.addReplacement(I(ctx.getContextId()));

            stmt = con.prepareStatement(builder.toString());
            whereIterator.setReplacements(stmt);
            rs = stmt.executeQuery();
            if (rs.next() == false) {
                return Collections.emptyList();
            }

            Set<Integer> tmp = new LinkedHashSet<>();
            do {
                tmp.add(I(rs.getInt(1)));
            } while (rs.next());
            return new ArrayList<>(tmp);
        } catch (SQLException e) {
            throw GenericConfigStorageExceptionCode.SQLException.create(e, null == stmt ? e.getMessage() : stmt.toString());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

}
