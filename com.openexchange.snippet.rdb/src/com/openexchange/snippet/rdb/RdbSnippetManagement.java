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

package com.openexchange.snippet.rdb;

import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.snippet.rdb.Services.getService;
import static com.openexchange.snippet.utils.SnippetUtils.sanitizeContent;
import static com.openexchange.snippet.utils.SnippetUtils.sanitizeHtmlContent;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.datatypes.genericonf.storage.GenericConfigurationStorageService;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.id.IDGeneratorService;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.quota.AccountQuota;
import com.openexchange.quota.Quota;
import com.openexchange.quota.QuotaExceptionCodes;
import com.openexchange.quota.QuotaProvider;
import com.openexchange.quota.QuotaType;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.snippet.Attachment;
import com.openexchange.snippet.DefaultAttachment;
import com.openexchange.snippet.DefaultSnippet;
import com.openexchange.snippet.GetSwitch;
import com.openexchange.snippet.Property;
import com.openexchange.snippet.ReferenceType;
import com.openexchange.snippet.Snippet;
import com.openexchange.snippet.SnippetExceptionCodes;
import com.openexchange.snippet.SnippetManagement;
import com.openexchange.snippet.utils.SnippetUtils;
import com.openexchange.tools.session.ServerSession;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;

/**
 * {@link RdbSnippetManagement}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class RdbSnippetManagement implements SnippetManagement {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RdbSnippetManagement.class);

    static DatabaseService getDatabaseService() {
        return getService(DatabaseService.class);
    }

    private static GenericConfigurationStorageService getStorageService() {
        return getService(GenericConfigurationStorageService.class);
    }

    private static IDGeneratorService getIdGeneratorService() {
        return getService(IDGeneratorService.class);
    }

    private static String extractFilename(final Attachment attachment) {
        if (null == attachment) {
            return null;
        }
        try {
            final String sContentDisposition = attachment.getContentDisposition();
            String fn = null == sContentDisposition ? null : new ContentDisposition(sContentDisposition).getFilenameParameter();
            if (fn == null) {
                final String sContentType = attachment.getContentType();
                fn = null == sContentType ? null : new ContentType(sContentType).getNameParameter();
            }
            if (fn == null) {
                DefaultAttachment da = (DefaultAttachment) attachment;
                fn = da.getFilename();
            }
            return fn;
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------------

    private final int contextId;
    private final int userId;
    private final Session session;
    private final boolean supportsAttachments;
    private final QuotaProvider quotaProvider;

    /**
     * Initializes a new {@link RdbSnippetManagement}.
     */
    public RdbSnippetManagement(Session session, QuotaProvider quotaProvider, ServiceLookup services) {
        super();
        this.session = session;
        this.userId = session.getUserId();
        this.contextId = session.getContextId();
        this.quotaProvider = quotaProvider;

        ConfigViewFactory factory = services.getOptionalService(ConfigViewFactory.class);
        boolean defaultSupportsAttachments = false;
        if (null == factory) {
            supportsAttachments = defaultSupportsAttachments;
        } else {
            boolean supportsAttachments;
            try {
                ComposedConfigProperty<Boolean> property = factory.getView(userId, contextId).property("com.openexchange.snippet.rdb.supportsAttachments", boolean.class);
                Boolean value = property.get();
                supportsAttachments = value != null ? value.booleanValue() : defaultSupportsAttachments;
            } catch (Exception e) {
                LOG.error("", e);
                supportsAttachments = defaultSupportsAttachments;
            }
            this.supportsAttachments = supportsAttachments;
        }
    }

    private static Context getContext(final Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getContext();
        }
        return getService(ContextService.class).getContext(session.getContextId());
    }

    private static Context getContext(final int contextId) throws OXException {
        return getService(ContextService.class).getContext(contextId);
    }

    private AccountQuota getQuota() throws OXException {
        return null == quotaProvider ? null : quotaProvider.getFor(session, "0");
    }

    @Override
    public List<Snippet> getSnippets(final String... types) throws OXException {
        final DatabaseService databaseService = getDatabaseService();
        final int contextId = this.contextId;
        final Connection con = databaseService.getReadOnly(contextId);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            final StringBuilder sql = new StringBuilder("SELECT id FROM snippet WHERE cid=? AND (user=? OR shared>0) AND refType=").append(ReferenceType.GENCONF.getType());
            if (null != types && types.length > 0) {
                sql.append(" AND (");
                sql.append("type=?");
                for (int i = 1; i < types.length; i++) {
                    sql.append(" OR type=?");
                }
                sql.append(')');
            }
            stmt = con.prepareStatement(sql.toString());
            int pos = 0;
            stmt.setInt(++pos, contextId);
            stmt.setInt(++pos, userId);
            if (null != types && types.length > 0) {
                for (final String type : types) {
                    stmt.setString(++pos, type);
                }
            }
            rs = stmt.executeQuery();
            if (!rs.next()) {
                return Collections.emptyList();
            }
            // Collect identifiers
            final TIntList ids = new TIntArrayList(8);
            do {
                ids.add(Integer.parseInt(rs.getString(1)));
            } while (rs.next());
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            // Load by identifiers
            final List<Snippet> list = new ArrayList<Snippet>(ids.size());
            final AtomicReference<OXException> error = new AtomicReference<OXException>();
            ids.forEach(new TIntProcedure() {

                @Override
                public boolean execute(final int id) {
                    try {
                        list.add(getSnippet0(Integer.toString(id), con));
                        return true;
                    } catch (OXException e) {
                        error.set(e);
                        return false;
                    }
                }
            });
            final OXException e = error.get();
            if (null != e) {
                throw e;
            }
            return list;
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(rs, stmt);
            databaseService.backReadOnly(contextId, con);
        }
    }

    @Override
    public int getOwnSnippetsCount() throws OXException {
        final DatabaseService databaseService = getDatabaseService();
        final int contextId = this.contextId;
        final Connection con = databaseService.getReadOnly(contextId);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            final StringBuilder sql = new StringBuilder("SELECT COUNT(id) FROM snippet WHERE cid=? AND user=? AND refType=").append(ReferenceType.GENCONF.getType());
            stmt = con.prepareStatement(sql.toString());
            int pos = 0;
            stmt.setInt(++pos, contextId);
            stmt.setInt(++pos, userId);
            rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(rs, stmt);
            databaseService.backReadOnly(contextId, con);
        }
    }

    @Override
    public List<Snippet> getOwnSnippets() throws OXException {
        final DatabaseService databaseService = getDatabaseService();
        final int contextId = this.contextId;
        final Connection con = databaseService.getReadOnly(contextId);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            final StringBuilder sql = new StringBuilder("SELECT id FROM snippet WHERE cid=? AND user=? AND refType=").append(ReferenceType.GENCONF.getType());
            stmt = con.prepareStatement(sql.toString());
            int pos = 0;
            stmt.setInt(++pos, contextId);
            stmt.setInt(++pos, userId);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                return Collections.emptyList();
            }
            final TIntList ids = new TIntArrayList(8);
            do {
                ids.add(Integer.parseInt(rs.getString(1)));
            } while (rs.next());
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            final List<Snippet> list = new ArrayList<Snippet>(ids.size());
            final AtomicReference<OXException> error = new AtomicReference<OXException>();
            ids.forEach(new TIntProcedure() {

                @Override
                public boolean execute(final int id) {
                    try {
                        list.add(getSnippet0(Integer.toString(id), con));
                        return true;
                    } catch (OXException e) {
                        error.set(e);
                        return false;
                    }
                }
            });
            final OXException e = error.get();
            if (null != e) {
                throw e;
            }
            return list;
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(rs, stmt);
            databaseService.backReadOnly(contextId, con);
        }
    }

    @Override
    public Snippet getSnippet(final String identifier) throws OXException {
        final DatabaseService databaseService = getDatabaseService();
        final int contextId = this.contextId;
        final Connection con = databaseService.getReadOnly(contextId);
        try {
            return getSnippet0(identifier, con);
        } finally {
            databaseService.backReadOnly(contextId, con);
        }
    }

    Snippet getSnippet0(final String identifier, final Connection con) throws OXException {
        if (null == identifier) {
            return null;
        }
        if (null == con) {
            return getSnippet(identifier);
        }
        final int id = Integer.parseInt(identifier);
        final int contextId = this.contextId;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT accountId, displayName, module, type, shared, refId, user FROM snippet WHERE cid=? AND id=? AND refType=" + ReferenceType.GENCONF.getType());
            int pos = 0;
            stmt.setInt(++pos, contextId);
            stmt.setString(++pos, Integer.toString(id));
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw SnippetExceptionCodes.SNIPPET_NOT_FOUND.create(Integer.valueOf(id));
            }
            final DefaultSnippet snippet = new DefaultSnippet().setId(identifier).setCreatedBy(rs.getInt(7));
            {
                final int accountId = rs.getInt(1);
                if (!rs.wasNull()) {
                    snippet.setAccountId(accountId);
                }
            }
            snippet.setDisplayName(rs.getString(2));
            snippet.setModule(rs.getString(3));
            snippet.setType(rs.getString(4));
            snippet.setShared(rs.getInt(5) > 0);
            final int confId = Integer.parseInt(rs.getString(6));
            closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;
            /*
             * Load unnamed properties
             */
            final Context context = getContext(session);
            {
                final GenericConfigurationStorageService storageService = getStorageService();
                final Map<String, Object> configuration = new HashMap<String, Object>(8);
                storageService.fill(con, context, confId, configuration);
                snippet.putUnnamedProperties(configuration);
            }
            /*
             * Load content
             */
            stmt = con.prepareStatement("SELECT content FROM snippetContent WHERE cid=? AND user=? AND id=?");
            pos = 0;
            stmt.setInt(++pos, contextId);
            stmt.setInt(++pos, userId);
            stmt.setInt(++pos, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                snippet.setContent(rs.getString(1));
            }
            closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;
            /*
             * Load JSON data
             */
            stmt = con.prepareStatement("SELECT json FROM snippetMisc WHERE cid=? AND user=? AND id=?");
            pos = 0;
            stmt.setInt(++pos, contextId);
            stmt.setInt(++pos, userId);
            stmt.setInt(++pos, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                snippet.setMisc(rs.getString(1));
            }
            closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;
            /*
             * Load attachments
             */
            if (supportsAttachments) {
                stmt = con.prepareStatement("SELECT referenceId, fileName, mimeType, disposition FROM snippetAttachment WHERE cid=? AND user=? AND id=?");
                pos = 0;
                stmt.setInt(++pos, contextId);
                stmt.setInt(++pos, userId);
                stmt.setInt(++pos, id);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    final List<Attachment> attachments = new LinkedList<Attachment>();
                    do {
                        final String referenceId = rs.getString(1);
                        if (!rs.wasNull()) {
                            String filename = rs.getString(2);
                            String mimeType = rs.getString(3);
                            String disposition = rs.getString(4);
                            final DefaultAttachment attachment = new DefaultAttachment();
                            attachment.setId(referenceId);
                            attachment.setContentType(null != mimeType ? mimeType : (null != filename ? MimeType2ExtMap.getContentType(filename) : "application/octet-stream"));
                            if (null == disposition) {
                                disposition = "attachment";
                                if (null != filename) {
                                    disposition += "; filename=\"" + filename + "\"";
                                }
                            } else if (null != filename && disposition.indexOf("filename=") < 0) {
                                disposition += "; filename=\"" + filename + "\"";
                            }
                            attachment.setContentDisposition(disposition);
                            attachment.setStreamProvider(new BlobStreamProvider(referenceId, contextId));
                            attachments.add(attachment);

                            Optional<String> optionalImageId = SnippetUtils.getImageId(snippet.getMisc());
                            if (optionalImageId.isPresent()) {
                                String imageId = optionalImageId.get();
                                if (Strings.isNotEmpty(imageId)) {
                                    ManagedFileManagement mfm = Services.getService(ManagedFileManagement.class);
                                    if (!mfm.contains(imageId)) {
                                        ManagedFile mf = mfm.createManagedFile(imageId, attachment.getInputStream());
                                        mf.setContentDisposition(attachment.getContentDisposition());
                                        mf.setContentType(attachment.getContentType());
                                    }
                                }
                            }
                        }
                    } while (rs.next());
                    closeSQLStuff(rs, stmt);
                    rs = null;
                    stmt = null;
                    snippet.setAttachments(attachments);
                }
            }
            /*
             * Finally return snippet
             */
            return snippet;
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (IOException e) {
            throw SnippetExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public String createSnippet(final Snippet snippet) throws OXException {
        AccountQuota quota = getQuota();
        if (null != quota && quota.hasQuota(QuotaType.AMOUNT)) {
            Quota amountQuota = quota.getQuota(QuotaType.AMOUNT);
            if (amountQuota.isExceeded() || amountQuota.willExceed(1)) {
                throw QuotaExceptionCodes.QUOTA_EXCEEDED_SNIPPETS.create(L(amountQuota.getUsage()), L(amountQuota.getLimit()));
            }
        }

        String contentSubType = determineContentSubtype(snippet.getMisc());

        DatabaseService databaseService = getDatabaseService();
        int contextId = this.contextId;
        Connection con = databaseService.getWritable(contextId);

        List<Closeable> closeables = new LinkedList<Closeable>();
        PreparedStatement stmt = null;
        int rollback = 0;
        try {
            con.setAutoCommit(false); // BEGIN;
            rollback = 1;
            /*-
             * Obtain identifier
             *
             * Yes, please use "com.openexchange.snippet.mime" since both implementations use shared table 'snippet'.
             */
            final int id = getIdGeneratorService().getId("com.openexchange.snippet.mime", contextId);
            // Store attachments
            if (supportsAttachments) {
                final List<Attachment> attachments = snippet.getAttachments();
                updateAttachments(id, attachments, false, userId, contextId, con, closeables);
            }
            // Store content
            {
                String content = snippet.getContent();
                if (content == null) {
                    // Set to empty string
                    content = "";
                } else if (contentSubType.indexOf("htm") >= 0) {
                    content = sanitizeHtmlContent(content);
                } else {
                    content = sanitizeContent(content);
                }
                if (null != content) {
                    stmt = con.prepareStatement("INSERT INTO snippetContent (cid, user, id, content) VALUES (?, ?, ?, ?)");
                    stmt.setInt(1, contextId);
                    stmt.setInt(2, userId);
                    stmt.setInt(3, id);
                    stmt.setString(4, content);
                    stmt.executeUpdate();
                    Databases.closeSQLStuff(stmt);
                    stmt = null;
                }
            }
            // Store JSON object
            {
                final Object misc = snippet.getMisc();
                if (null != misc) {
                    stmt = con.prepareStatement("INSERT INTO snippetMisc (cid, user, id, json) VALUES (?, ?, ?, ?)");
                    stmt.setInt(1, contextId);
                    stmt.setInt(2, userId);
                    stmt.setInt(3, id);
                    stmt.setString(4, misc.toString());
                    stmt.executeUpdate();
                    Databases.closeSQLStuff(stmt);
                    stmt = null;
                }
            }
            // Store unnamed properties
            final int confId;
            {
                final Map<String, Object> unnamedProperties = snippet.getUnnamedProperties();
                if (null == unnamedProperties || unnamedProperties.isEmpty()) {
                    confId = -1;
                } else {
                    final GenericConfigurationStorageService storageService = getStorageService();
                    final Map<String, Object> configuration = new HashMap<String, Object>(unnamedProperties);
                    confId = storageService.save(con, getContext(session), configuration);
                }
            }
            // Store snippet
            stmt = con.prepareStatement("INSERT INTO snippet (cid, user, id, accountId, displayName, module, type, shared, lastModified, refId, refType) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + ReferenceType.GENCONF.getType() + ")");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setString(3, Integer.toString(id));
            {
                final int accountId = snippet.getAccountId();
                if (accountId >= 0) {
                    stmt.setInt(4, accountId);
                } else {
                    stmt.setNull(4, Types.INTEGER);
                }
            }
            stmt.setString(5, snippet.getDisplayName());
            stmt.setString(6, snippet.getModule());
            stmt.setString(7, snippet.getType());
            stmt.setInt(8, snippet.isShared() ? 1 : 0);
            stmt.setLong(9, System.currentTimeMillis());
            stmt.setString(10, Integer.toString(confId));
            stmt.executeUpdate();
            /*
             * Commit & return identifier
             */
            con.commit(); // COMMIT
            rollback = 2;
            return Integer.toString(id);
        } catch (DataTruncation e) {
            throw SnippetExceptionCodes.DATA_TRUNCATION_ERROR.create(e);
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Streams.close(closeables);
            Databases.closeSQLStuff(stmt);
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            databaseService.backReadOnly(contextId, con);
        }
    }

    @Override
    public String updateSnippet(final String identifier, final Snippet snippet, final Set<Property> properties, final Collection<Attachment> addAttachments, final Collection<Attachment> removeAttachments) throws OXException {
        if (null == identifier) {
            return identifier;
        }

        int id = Integer.parseInt(identifier);
        DatabaseService databaseService = getDatabaseService();
        int contextId = this.contextId;
        Connection con = databaseService.getWritable(contextId);

        List<Closeable> closeables = new LinkedList<Closeable>();
        PreparedStatement stmt = null;
        int rollback = 0;
        try {
            con.setAutoCommit(false); // BEGIN;
            rollback = 1;
            /*
             * Update snippet if necessary
             */
            final UpdateSnippetBuilder updateBuilder = new UpdateSnippetBuilder();
            for (final Property property : properties) {
                property.doSwitch(updateBuilder);
            }
            final Set<Property> modifiableProperties = updateBuilder.getModifiableProperties();
            if (null != modifiableProperties && !modifiableProperties.isEmpty()) {
                stmt = con.prepareStatement(updateBuilder.getUpdateStatement());
                final GetSwitch getter = new GetSwitch(snippet);
                int pos = 0;
                for (final Property modifiableProperty : modifiableProperties) {
                    final Object value = modifiableProperty.doSwitch(getter);
                    if (null != value) {
                        stmt.setObject(++pos, value);
                    }
                }
                stmt.setLong(++pos, contextId);
                stmt.setLong(++pos, userId);
                stmt.setString(++pos, Integer.toString(id));
                LOG.debug(
                    "Trying to perform SQL update query for attributes {} :\n{}",
                    modifiableProperties,
                    stmt.toString().substring(stmt.toString().indexOf(':') + 1));
                stmt.executeUpdate();
                closeSQLStuff(stmt);
                stmt = null;
            }
            /*
             * Update content
             */
            if (properties.contains(Property.CONTENT)) {
                String content = snippet.getContent();
                if (null == content) {
                    content = "";
                }
                stmt = con.prepareStatement("UPDATE snippetContent SET content=? WHERE cid=? AND user=? AND id=?");
                int pos = 0;
                stmt.setString(++pos, sanitizeContent(content));
                stmt.setLong(++pos, contextId);
                stmt.setLong(++pos, userId);
                stmt.setLong(++pos, id);
                stmt.executeUpdate();
                closeSQLStuff(stmt);
                stmt = null;
            }
            /*
             * Update unnamed properties
             */
            final Context context = getContext(session);
            if (properties.contains(Property.PROPERTIES)) {
                final int confId;
                {
                    ResultSet rs = null;
                    try {
                        stmt = con.prepareStatement("SELECT refId FROM snippet WHERE cid=? AND user=? AND id=? AND refType=" + ReferenceType.GENCONF.getType());
                        int pos = 0;
                        stmt.setLong(++pos, contextId);
                        stmt.setLong(++pos, userId);
                        stmt.setString(++pos, Integer.toString(id));
                        rs = stmt.executeQuery();
                        confId = rs.next() ? Integer.parseInt(rs.getString(1)) : -1;
                    } finally {
                        closeSQLStuff(rs, stmt);
                        stmt = null;
                        rs = null;
                    }
                }
                if (confId > 0) {
                    final Map<String, Object> unnamedProperties = snippet.getUnnamedProperties();
                    final GenericConfigurationStorageService storageService = getStorageService();
                    if (null == unnamedProperties || unnamedProperties.isEmpty()) {
                        storageService.delete(con, context, confId);
                    } else {
                        storageService.update(con, context, confId, unnamedProperties);
                    }
                }
            }
            /*
             * Update misc
             */
            if (properties.contains(Property.MISC)) {
                final Object misc = snippet.getMisc();
                if (null == misc) {
                    stmt = con.prepareStatement("DELETE FROM snippetMisc WHERE cid=? AND user=? AND id=?");
                    int pos = 0;
                    stmt.setLong(++pos, contextId);
                    stmt.setLong(++pos, userId);
                    stmt.setLong(++pos, id);
                    stmt.executeUpdate();
                } else {
                    stmt = con.prepareStatement("UPDATE snippetMisc SET json=? WHERE cid=? AND user=? AND id=?");
                    int pos = 0;
                    stmt.setString(++pos, misc.toString());
                    stmt.setLong(++pos, contextId);
                    stmt.setLong(++pos, userId);
                    stmt.setLong(++pos, id);
                    stmt.executeUpdate();
                }
                closeSQLStuff(stmt);
                stmt = null;
            }
            /*
             * Update attachments
             */
            if (supportsAttachments) {
                if (null != removeAttachments && !removeAttachments.isEmpty()) {
                    removeAttachments(id, removeAttachments, userId, contextId, con);
                }
                if (null != addAttachments && !addAttachments.isEmpty()) {
                    updateAttachments(id, addAttachments, false, userId, contextId, con, closeables);
                }
            }
            /*
             * Commit & return
             */
            con.commit(); // COMMIT
            rollback = 2;
            return identifier;
        } catch (DataTruncation e) {
            throw SnippetExceptionCodes.DATA_TRUNCATION_ERROR.create(e);
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Streams.close(closeables);
            closeSQLStuff(stmt);
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            databaseService.backReadOnly(contextId, con);
        }
    }

    private static void updateAttachments(int id, Collection<Attachment> attachments, boolean deleteExisting,int userId, int contextId, Connection con, List<Closeable> closeables) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (deleteExisting) {
                // Check existing ones...
                stmt = con.prepareStatement("SELECT referenceId FROM snippetAttachment WHERE cid=? AND user=? AND id=?");
                int pos = 0;
                stmt.setLong(++pos, contextId);
                stmt.setLong(++pos, userId);
                stmt.setLong(++pos, id);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    List<String> referenceIds = new LinkedList<String>();
                    do {
                        referenceIds.add(rs.getString(1));
                    } while (rs.next());
                    closeSQLStuff(rs, stmt);
                    rs = null;
                    stmt = null;

                    // ... and delete from "snippetAttachmentBinary" table
                    for (String referenceId : referenceIds) {
                        stmt = con.prepareStatement("DELETE FROM snippetAttachmentBinary WHERE cid=? AND referenceId=?");
                        pos = 0;
                        stmt.setLong(++pos, contextId);
                        stmt.setString(++pos, referenceId);
                        stmt.executeUpdate();
                        closeSQLStuff(stmt);
                        stmt = null;
                    }

                    // Delete from table, too
                    stmt = con.prepareStatement("DELETE FROM snippetAttachment WHERE cid=? AND user=? AND id=?");
                    pos = 0;
                    stmt.setLong(++pos, contextId);
                    stmt.setLong(++pos, userId);
                    stmt.setLong(++pos, id);
                    stmt.executeUpdate();
                    closeSQLStuff(stmt);
                    stmt = null;
                } else {
                    closeSQLStuff(rs, stmt);
                    rs = null;
                    stmt = null;
                }
            }

            // Check passed ones
            if (null != attachments && !attachments.isEmpty()) {
                for (Attachment attachment : attachments) {
                    String referenceId = null == attachment.getId() ? UUIDs.getUnformattedStringFromRandom() : attachment.getId();
                    stmt = con.prepareStatement("INSERT INTO snippetAttachmentBinary (cid, referenceId, data) VALUES (?, ?, ?)");
                    stmt.setInt(1, contextId);
                    stmt.setString(2, referenceId);
                    InputStream in = attachment.getInputStream();
                    closeables.add(in);
                    stmt.setBinaryStream(3, in);
                    stmt.executeUpdate();
                    closeSQLStuff(stmt);
                    stmt = null;

                    stmt = con.prepareStatement("INSERT INTO snippetAttachment (cid, user, id, referenceId, fileName, mimeType, disposition) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    stmt.setInt(1, contextId);
                    stmt.setInt(2, userId);
                    stmt.setInt(3, id);
                    stmt.setString(4, referenceId);
                    String fileName = extractFilename(attachment);
                    if (null == fileName) {
                        stmt.setNull(5, Types.VARCHAR);
                    } else {
                        stmt.setString(5, fileName);
                    }
                    String mimeType = attachment.getContentType();
                    if (null == mimeType) {
                        stmt.setNull(6, Types.VARCHAR);
                    } else {
                        stmt.setString(6, mimeType);
                    }
                    String disposition = attachment.getContentDisposition();
                    if (null == disposition) {
                        stmt.setNull(7, Types.VARCHAR);
                    } else {
                        stmt.setString(7, disposition);
                    }
                    stmt.executeUpdate();
                    closeSQLStuff(stmt);
                    stmt = null;
                }
            }
        } catch (DataTruncation e) {
            throw SnippetExceptionCodes.DATA_TRUNCATION_ERROR.create(e);
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (IOException e) {
            throw SnippetExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    private static void removeAttachments(int id, Collection<Attachment> attachments, int userId, int contextId, Connection con) throws OXException {
        PreparedStatement stmt = null;
        try {
            for (final Attachment attachment : attachments) {
                String referenceId = attachment.getId();

                // Delete binary content
                stmt = con.prepareStatement("DELETE FROM snippetAttachmentBinary WHERE cid=? AND referenceId=?");
                int pos = 0;
                stmt.setLong(++pos, contextId);
                stmt.setString(++pos, referenceId);
                stmt.executeUpdate();
                closeSQLStuff(stmt);
                stmt = null;

                // Delete from table, too
                stmt = con.prepareStatement("DELETE FROM snippetAttachment WHERE cid=? AND user=? AND id=? AND referenceId=?");
                pos = 0;
                stmt.setLong(++pos, contextId);
                stmt.setLong(++pos, userId);
                stmt.setLong(++pos, id);
                stmt.setString(++pos, referenceId);
                stmt.executeUpdate();
                closeSQLStuff(stmt);
                stmt = null;
            }
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    @Override
    public void deleteSnippet(final String identifier) throws OXException {
        if (null == identifier) {
            return;
        }
        final int id = Integer.parseInt(identifier);
        final DatabaseService databaseService = getDatabaseService();
        final int contextId = this.contextId;
        final Connection con = databaseService.getWritable(contextId);
        int rollback = 0;
        try {
            con.setAutoCommit(false); // BEGIN;
            rollback = 1;
            deleteSnippet(id, userId, contextId, con);
            con.commit(); // COMMIT
            rollback = 2;
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            databaseService.backReadOnly(contextId, con);
        }
    }

    /**
     * Deletes specified snippet.
     *
     * @param contextId The context identifier
     * @param con The connection to use
     * @throws OXException If delete attempt fails
     */
    public static void deleteForContext(int contextId, Connection con) throws OXException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("DELETE FROM snippetAttachmentBinary WHERE cid=?");
            stmt.setLong(1, contextId);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;

            stmt = con.prepareStatement("DELETE FROM snippetAttachment WHERE cid=?");
            stmt.setLong(1, contextId);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;

            stmt = con.prepareStatement("DELETE FROM snippetContent WHERE cid=?");
            stmt.setLong(1, contextId);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;

            stmt = con.prepareStatement("DELETE FROM snippetMisc WHERE cid=?");
            stmt.setLong(1, contextId);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;

            stmt = con.prepareStatement("DELETE FROM snippet WHERE cid=?");
            stmt.setLong(1, contextId);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    /**
     * Deletes specified snippet.
     *
     * @param id The snippet identifier
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param con The connection to use
     * @throws OXException If delete attempt fails
     */
    public static void deleteSnippet(int id, int userId, int contextId, Connection con) throws OXException {
        PreparedStatement stmt = null;
        try {
            // Delete attachments
            {
                ResultSet rs = null;
                try {
                    stmt = con.prepareStatement("SELECT referenceId FROM snippetAttachment WHERE cid=? AND user=? AND id=?");
                    int pos = 0;
                    stmt.setLong(++pos, contextId);
                    stmt.setLong(++pos, userId);
                    stmt.setLong(++pos, id);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        List<String> attachIds = new LinkedList<String>();
                        do {
                            attachIds.add(rs.getString(1));
                        } while (rs.next());
                        Databases.closeSQLStuff(rs, stmt);
                        rs = null;
                        stmt = null;

                        for (String attachId : attachIds) {
                            stmt = con.prepareStatement("DELETE FROM snippetAttachmentBinary WHERE cid=? AND referenceId=?");
                            pos = 0;
                            stmt.setLong(++pos, contextId);
                            stmt.setString(++pos, attachId);
                            stmt.executeUpdate();
                            Databases.closeSQLStuff(stmt);
                            stmt = null;
                        }

                        stmt = con.prepareStatement("DELETE FROM snippetAttachment WHERE cid=? AND user=? AND id=?");
                        pos = 0;
                        stmt.setLong(++pos, contextId);
                        stmt.setLong(++pos, userId);
                        stmt.setLong(++pos, id);
                        stmt.executeUpdate();
                        Databases.closeSQLStuff(stmt);
                        stmt = null;
                    }
                } finally {
                    Databases.closeSQLStuff(rs, stmt);
                }
            }

            // Delete content
            stmt = con.prepareStatement("DELETE FROM snippetContent WHERE cid=? AND user=? AND id=?");
            int pos = 0;
            stmt.setLong(++pos, contextId);
            stmt.setLong(++pos, userId);
            stmt.setLong(++pos, id);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;

            // Delete JSON object
            stmt = con.prepareStatement("DELETE FROM snippetMisc WHERE cid=? AND user=? AND id=?");
            pos = 0;
            stmt.setLong(++pos, contextId);
            stmt.setLong(++pos, userId);
            stmt.setLong(++pos, id);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;

            // Delete unnamed properties
            final int confId;
            {
                ResultSet rs = null;
                try {
                    stmt = con.prepareStatement("SELECT refId FROM snippet WHERE cid=? AND user=? AND id=? AND refType=" + ReferenceType.GENCONF.getType());
                    pos = 0;
                    stmt.setLong(++pos, contextId);
                    stmt.setLong(++pos, userId);
                    stmt.setString(++pos, Integer.toString(id));
                    rs = stmt.executeQuery();
                    confId = rs.next() ? Integer.parseInt(rs.getString(1)) : -1;
                } finally {
                    closeSQLStuff(rs, stmt);
                    stmt = null;
                    rs = null;
                }
            }
            if (confId > 0) {
                GenericConfigurationStorageService storageService = getStorageService();
                storageService.delete(con, getContext(contextId), confId);
            }

            // Delete snippet
            stmt = con.prepareStatement("DELETE FROM snippet WHERE cid=? AND user=? AND id=? AND refType=" + ReferenceType.GENCONF.getType());
            pos = 0;
            stmt.setLong(++pos, contextId);
            stmt.setLong(++pos, userId);
            stmt.setString(++pos, Integer.toString(id));
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;
        } catch (SQLException e) {
            throw SnippetExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * {@link BlobStreamProvider} - BLOB stream provider.
     *
     * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
     */
    public static final class BlobStreamProvider implements DefaultAttachment.InputStreamProvider {

        private final int contextId;
        private final String referenceId;

        BlobStreamProvider(String referenceId, int contextId) {
            super();
            this.contextId = contextId;
            this.referenceId = referenceId;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            DatabaseService databaseService = getDatabaseService();

            Connection con = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            boolean closeStuff = true;
            try {
                con = databaseService.getReadOnly(contextId);
                stmt = con.prepareStatement("SELECT data FROM snippetAttachmentBinary WHERE cid=? AND referenceId=?");
                stmt.setInt(1, contextId);
                stmt.setString(2, referenceId);
                rs = stmt.executeQuery();

                if (false == rs.next()) {
                    throw new IOException("No such attachment binary for reference identifier " + referenceId + " in context " + contextId);
                }

                InputStream stream = new ClosingInputStream(rs.getBinaryStream(1), rs, stmt, con, contextId, databaseService);
                closeStuff = false; // Avoid preliminary closing
                return stream;
            } catch (OXException e) {
                throw new IOException("Loading file from database failed.", e);
            } catch (SQLException e) {
                throw new IOException("Loading file from database failed.", e);
            } finally {
                if (closeStuff) {
                    Databases.closeSQLStuff(rs, stmt);
                    if (null != con) {
                        databaseService.backReadOnly(contextId, con);
                    }
                }
            }
        }
    }

    private static final class ClosingInputStream extends FilterInputStream {

        private final int contextId;
        private final Connection con;
        private final PreparedStatement stmt;
        private final ResultSet rs;
        private final DatabaseService databaseService;
        private boolean closed;

        /**
         * Initializes a new {@link RdbSnippetManagement.ClosingInputStream}.
         */
        ClosingInputStream(InputStream in, ResultSet rs, PreparedStatement stmt, Connection con, int contextId, DatabaseService databaseService) {
            super(in);
            this.rs = rs;
            this.stmt = stmt;
            this.con = con;
            this.contextId = contextId;
            this.databaseService = databaseService;
            closed = false;
        }

        @Override
        public synchronized void close() throws IOException {
            if (closed) {
                // Already closed
                return;
            }

            super.close();
            Databases.closeSQLStuff(rs, stmt);
            if (null != con) {
                databaseService.backReadOnly(contextId, con);
            }
            closed = true;
        }
    }

    private static String determineContentSubtype(final Object misc) throws OXException {
        if (misc == null) {
            return "plain";
        }
        final String ct = SnippetUtils.parseContentTypeFromMisc(misc);
        return Strings.asciiLowerCase(new ContentType(ct).getSubType());
    }
}
