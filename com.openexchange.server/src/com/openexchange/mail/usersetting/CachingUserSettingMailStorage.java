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

package com.openexchange.mail.usersetting;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.tools.sql.DBUtils.closeResources;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.collect.Lists;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.userconfiguration.UserConfigurationCodes;
import com.openexchange.java.Reference;
import com.openexchange.java.Strings;
import com.openexchange.lock.AccessControl;
import com.openexchange.lock.LockService;
import com.openexchange.mail.usersetting.UserSettingMail.Signature;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * CachingUserSettingMailStorage - this storage tries to use a cache for instances of <code>{@link UserSettingMail}</code> and falls back to
 * database-based storage if any cache-related errors occur
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CachingUserSettingMailStorage extends UserSettingMailStorage {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CachingUserSettingMailStorage.class);

    private static final String CACHE_REGION_NAME = "UserSettingMail";

    private final AtomicReference<Cache> cacheReference;

    /**
     * Default constructor
     */
    protected CachingUserSettingMailStorage() {
        super();
        cacheReference = new AtomicReference<>();
        try {
            initCache();
        } catch (OXException e) {
            LOG.error("", e);
        }
    }

    private void initCache() throws OXException {
        cacheReference.set(ServerServiceRegistry.getInstance().getService(CacheService.class, true).getCache(CACHE_REGION_NAME));
    }

    private void releaseCache() {
        cacheReference.set(null);
    }

    private Cache getCache() {
        return cacheReference.get();
    }

    private void logCacheAccessError(OXException e) {
        LOG.error("UserSettingMail could not be put into cache", e);
    }

    /**
     * Saves given user's mail settings to database
     *
     * @param usm the user's mail settings to save
     * @param user the user ID
     * @param ctx the context
     * @param writeConArg - the writable connection; may be <code>null</code>
     * @throws OXException if user's mail settings could not be saved
     */
    @Override
    public void saveUserSettingMail(UserSettingMail usm, int user, Context ctx, Connection writeConArg) throws OXException {
        if (usm.isNoSave()) {
            /*
             * Saving to storage denied
             */
            return;
        }
        try {
            Connection writeCon = writeConArg;
            boolean closeCon = false;
            PreparedStatement stmt = null;
            try {
                if (writeCon == null) {
                    writeCon = DBPool.pickupWriteable(ctx);
                    closeCon = true;
                }

                stmt = getInsertUpdateStmt(usm, user, ctx, writeCon);
                stmt.executeUpdate();
                saveSignatures(usm, user, ctx, writeCon);
            } finally {
                closeResources(null, stmt, closeCon ? writeCon : null, false, ctx);
            }
            usm.setModifiedDuringSession(false);

            Cache cache = getCache();
            if (null != cache) {
                /*
                 * Put clone into cache
                 */
                try {
                    usm.setNoSave(false);
                    final CacheKey key = cache.newCacheKey(ctx.getContextId(), user);
                    if (null != cache.get(key)) {
                        cache.remove(key);
                    }
                    cache.put(key, usm.clone(), false);
                } catch (OXException e) {
                    logCacheAccessError(e);
                }
            }
        } catch (SQLException e) {
            throw UserConfigurationCodes.SQL_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void saveUserSettingMailBits(UserSettingMail usm, int user, Context ctx, Connection writeConArg) throws OXException {
        if (usm.isNoSave()) {
            /*
             * Saving to storage denied
             */
            return;
        }
        try {
            Connection writeCon = writeConArg;
            boolean closeCon = false;
            PreparedStatement stmt = null;
            final ResultSet rs = null;
            try {
                if (writeCon == null) {
                    writeCon = DBPool.pickupWriteable(ctx);
                    closeCon = true;
                }
                stmt = getUpdateStmtBits(usm, user, ctx, writeCon);
                stmt.executeUpdate();
                saveSignatures(usm, user, ctx, writeCon);
            } finally {
                closeResources(rs, stmt, closeCon ? writeCon : null, false, ctx);
            }
            usm.setModifiedDuringSession(false);

            Cache cache = getCache();
            if (null != cache) {
                /*
                 * Put clone into cache
                 */
                try {
                    usm.setNoSave(false);
                    final CacheKey key = cache.newCacheKey(ctx.getContextId(), user);
                    if (null != cache.get(key)) {
                        cache.remove(key);
                    }
                    cache.put(key, usm.clone(), false);
                } catch (OXException e) {
                    logCacheAccessError(e);
                }
            }
        } catch (SQLException e) {
            throw UserConfigurationCodes.SQL_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Deletes the user's mail settings from database
     *
     * @param user the user ID
     * @param ctx the context
     * @param writeConArg the writable connection; may be <code>null</code>
     * @throws OXException - if deletion fails
     */
    @Override
    public void deleteUserSettingMail(int user, Context ctx, Connection writeConArg) throws OXException {
        try {
            Connection writeCon = writeConArg;
            boolean closeWriteCon = false;
            PreparedStatement stmt = null;
            try {
                if (writeCon == null) {
                    writeCon = DBPool.pickupWriteable(ctx);
                    closeWriteCon = true;
                }
                /*
                 * Delete signatures
                 */
                stmt = writeCon.prepareStatement("DELETE FROM user_setting_mail_signature WHERE cid = ? AND user = ?");
                stmt.setInt(1, ctx.getContextId());
                stmt.setInt(2, user);
                stmt.executeUpdate();
                stmt.close();
                /*
                 * Delete user setting
                 */
                stmt = writeCon.prepareStatement("DELETE FROM user_setting_mail WHERE cid = ? AND user = ?");
                stmt.setInt(1, ctx.getContextId());
                stmt.setInt(2, user);
                stmt.executeUpdate();
                stmt.close();
                stmt = null;

                Cache cache = getCache();
                if (null != cache) {
                    /*
                     * Remove from cache
                     */
                    try {
                        cache.remove(cache.newCacheKey(ctx.getContextId(), user));
                    } catch (OXException e) {
                        LOG.error("UserSettingMail could not be removed from cache", e);
                    }
                }
            } finally {
                closeResources(null, stmt, closeWriteCon ? writeCon : null, false, ctx);
                stmt = null;
            }
        } catch (SQLException e) {
            throw UserConfigurationCodes.SQL_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Loads user's mail settings from database
     *
     * @param user the user
     * @param ctx the context
     * @param readConArg the readable connection; may be <code>null</code> to fetch own connection.
     * @throws OXException if loading fails
     */
    @Override
    public UserSettingMail loadUserSettingMail(int user, Context ctx, Connection readConArg) throws OXException {
        try {
            Cache cache = getCache();
            UserSettingMail usm = null == cache ? null : (UserSettingMail) cache.get(cache.newCacheKey(ctx.getContextId(), user));
            if (null != usm) {
                return usm.clone();
            }

            LockService lockService = ServerServiceRegistry.getInstance().getService(LockService.class);
            boolean acquired = false;
            AccessControl accessControl = null == lockService || null == cache ? LockService.EMPTY_ACCESS_CONTROL : lockService.getAccessControlFor(CACHE_REGION_NAME, 1, user, ctx.getContextId());
            try {
                accessControl.acquireGrant();
                acquired = true;

                usm = null == cache ? null : (UserSettingMail) cache.get(cache.newCacheKey(ctx.getContextId(), user));
                if (null != usm) {
                    return usm.clone();
                }

                usm = new UserSettingMail(user, ctx.getContextId());
                Connection readCon = readConArg;
                boolean closeCon = false;
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try {
                    if (readCon == null) {
                        readCon = DBPool.pickup(ctx);
                        closeCon = true;
                    }
                    stmt = readCon.prepareStatement("SELECT bits, send_addr, reply_to_addr, msg_format, display_msg_headers, auto_linebreak, std_trash, std_sent, std_drafts, std_spam, upload_quota, upload_quota_per_file, confirmed_spam, confirmed_ham FROM user_setting_mail WHERE cid = ? AND user = ?");
                    stmt.setInt(1, ctx.getContextId());
                    stmt.setInt(2, user);
                    rs = stmt.executeQuery();
                    if (!rs.next()) {
                        throw UserConfigurationCodes.MAIL_SETTING_NOT_FOUND.create(Integer.valueOf(user), Integer.valueOf(ctx.getContextId()));
                    }

                    usm.parseBits(rs.getInt(1));
                    usm.setSendAddr(rs.getString(2));
                    usm.setReplyToAddr(rs.getString(3));
                    usm.setMsgFormat(rs.getInt(4));
                    setDisplayMsgHeadersString(usm, rs.getString(5));
                    int autoLineBreak = rs.getInt(6);
                    usm.setAutoLinebreak(autoLineBreak >= 0 ? autoLineBreak : 0);
                    usm.setStdTrashName(rs.getString(7));
                    usm.setStdSentName(rs.getString(8));
                    usm.setStdDraftsName(rs.getString(9));
                    usm.setStdSpamName(rs.getString(10));
                    usm.setUploadQuota(rs.getLong(11));
                    usm.setUploadQuotaPerFile(rs.getLong(12));
                    usm.setConfirmedSpam(rs.getString(13));
                    usm.setConfirmedHam(rs.getString(14));
                    loadSignatures(usm, user, ctx, readCon);
                    usm.setModifiedDuringSession(false);

                    applyConfigCascadeSettings(usm, user, ctx);
                    if (null != cache) {
                        /*
                         * Put into cache
                         */
                        usm.setNoSave(false);
                        try {
                            cache.put(cache.newCacheKey(ctx.getContextId(), user), usm, false);
                        } catch (OXException e) {
                            logCacheAccessError(e);
                        }
                    }
                } finally {
                    closeResources(rs, stmt, closeCon ? readCon : null, true, ctx);
                }
                return usm.clone();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw OXException.general("Interrupted while loading user_setting_mail data", e);
            } finally {
                accessControl.release(acquired);
            }
        } catch (SQLException e) {
            throw UserConfigurationCodes.SQL_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public Map<Integer, String> getSenderAddresses(Set<Integer> userIds, Context ctx, Connection connection) throws OXException {
        try {
            // Create resulting map instance
            Map<Integer, String> result = new HashMap<>(userIds.size());

            // Check which user identifiers can be served from cache and which need to be loaded from database
            List<Integer> toLoad;
            {
                Reference<List<Integer>> toLoadReference = new Reference<>(null);
                Cache cache = getCache();
                userIds.stream().filter(Objects::nonNull).forEach(user -> {
                    UserSettingMail usm = null == cache ? null : (UserSettingMail) cache.get(cache.newCacheKey(ctx.getContextId(), i(user)));
                    if (usm == null) {
                        List<Integer> l = toLoadReference.getValue();
                        if (l == null) {
                            l = new ArrayList<>(userIds.size());
                            toLoadReference.setValue(l);
                        }
                        l.add(user);
                    } else {
                        result.put(user, usm.getSendAddr());
                    }
                });

                toLoad = toLoadReference.getValue();
                if (toLoad == null) {
                    return result;
                }
            }

            // Load non-cached ones from database
            Connection readCon = null;
            boolean closeCon = false;
            try {
                readCon = connection;
                if (readCon == null) {
                    readCon = DBPool.pickup(ctx);
                    closeCon = true;
                }

                for (List<Integer> partition : Lists.partition(toLoad, Databases.IN_LIMIT)) {
                    PreparedStatement stmt = null;
                    ResultSet rs = null;
                    try {
                        stmt = readCon.prepareStatement(Databases.getIN("SELECT user, send_addr FROM user_setting_mail WHERE cid = ? AND user IN (", partition.size()));
                        int index = 1;
                        stmt.setInt(index++, ctx.getContextId());
                        for (Integer user : partition) {
                            stmt.setInt(index++, user.intValue());
                        }
                        rs = stmt.executeQuery();
                        while (rs.next()) {
                            result.put(I(rs.getInt(1)), rs.getString(2));
                        }
                    } finally {
                        Databases.closeSQLStuff(rs, stmt);
                    }
                }
            } finally {
                if (closeCon) {
                    DBPool.closeReaderSilent(ctx, readCon);
                }
            }

            return result;
        } catch (SQLException e) {
            throw UserConfigurationCodes.SQL_ERROR.create(e, e.getMessage());
        }
    }

    protected static final String SPAM_ENABLED = "com.openexchange.spamhandler.enabled";

    protected void applyConfigCascadeSettings(UserSettingMail userSettingMail, int userId, Context ctx) throws OXException {
        ConfigViewFactory configViewFactory = ServerServiceRegistry.getInstance().getService(ConfigViewFactory.class);
        if (configViewFactory == null) {
            LOG.warn("Required service ConfigViewFactory absent. Unable to retrieve spam configuration for user {} in context {}.", I(userId), I(ctx.getContextId()));
            return;
        }
        ConfigView configView = configViewFactory.getView(userId, ctx.getContextId());
        if (configView == null) {
            LOG.warn("Required ConfigView not available. Unable to retrieve spam configuration for user {} in context {}.", I(userId), I(ctx.getContextId()));
            return;
        }
        updateSpamSetting(userSettingMail, userId, ctx, configView);
    }

    protected void updateSpamSetting(UserSettingMail userSettingMail, int userId, Context ctx, ConfigView configView) throws OXException {
        ComposedConfigProperty<Boolean> spamEnabledByConfig = configView.property(SPAM_ENABLED, Boolean.class);

        Boolean valie = spamEnabledByConfig.get();
        if (valie == null) {
            LOG.debug("No config for user {} in context {} found. Falling back to user permission bit for spam handling.", I(userId), I(ctx.getContextId()));
            return;
        }
        boolean boolSpamEnabledByConfig = valie.booleanValue();
        if (userSettingMail.isSpamOptionEnabled() != boolSpamEnabledByConfig) {
            userSettingMail.setSpamEnabled(boolSpamEnabledByConfig);
            this.saveUserSettingMail(userSettingMail, userId, ctx);
            LOG.debug("Updated spam configuration for user {} in context {} to '{}' based on ConfigCascade setting.", I(userId), I(ctx.getContextId()), boolSpamEnabledByConfig ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    private static void loadSignatures(UserSettingMail usm, int user, Context ctx, Connection readConArg) throws OXException {
        try {
            Connection readCon = readConArg;
            boolean closeCon = false;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                if (readCon == null) {
                    readCon = DBPool.pickup(ctx);
                    closeCon = true;
                }

                if (!Databases.tableExists(readCon, "user_setting_mail_signature")) {
                    usm.setSignatures(null);
                    return;
                }

                stmt = readCon.prepareStatement("SELECT id, signature FROM user_setting_mail_signature WHERE cid = ? AND user = ?");
                stmt.setInt(1, ctx.getContextId());
                stmt.setInt(2, user);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    usm.setSignatures(null);
                    return;
                }

                Map<String, String> sigMap = new HashMap<>();
                do {
                    sigMap.put(rs.getString(1), rs.getString(2));
                } while (rs.next());
                final int size = sigMap.size();
                final Signature[] signatures = new Signature[size];
                final Iterator<Map.Entry<String, String>> iter = sigMap.entrySet().iterator();
                for (int i = 0; i < size; i++) {
                    final Map.Entry<String, String> e = iter.next();
                    signatures[i] = new Signature(e.getKey(), e.getValue());
                }
                usm.setSignatures(signatures);
            } finally {
                closeResources(rs, stmt, closeCon ? readCon : null, true, ctx);
            }
        } catch (SQLException e) {
            throw UserConfigurationCodes.SQL_ERROR.create(e, e.getMessage());
        }
    }

    private static PreparedStatement getUpdateStmtBits(UserSettingMail usm, int user, Context ctx, Connection writeCon) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = writeCon.prepareStatement("UPDATE user_setting_mail SET bits = ? WHERE cid = ? AND user = ?");
            stmt.setInt(1, usm.getBitsValue());
            stmt.setInt(2, ctx.getContextId());
            stmt.setInt(3, user);
            PreparedStatement retval = stmt;
            stmt = null;
            return retval;
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    private static PreparedStatement getInsertUpdateStmt(UserSettingMail usm, int user, Context ctx, Connection writeCon) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = writeCon.prepareStatement("INSERT INTO user_setting_mail (cid, user, bits, send_addr, reply_to_addr, msg_format, display_msg_headers, auto_linebreak, std_trash, std_sent, std_drafts, std_spam, upload_quota, upload_quota_per_file, confirmed_spam, confirmed_ham) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE bits = ?, send_addr = ?, reply_to_addr = ?, msg_format = ?, display_msg_headers = ?, auto_linebreak = ?, std_trash = ?, std_sent = ?, std_drafts = ?, std_spam = ?, upload_quota = ?, upload_quota_per_file = ?, confirmed_spam = ?, confirmed_ham = ?");

            // Set 'cid' and 'user' columns
            int pos = 1;
            stmt.setInt(pos++, ctx.getContextId());
            stmt.setInt(pos++, user);

            // Set other values; first for "INSERT", then for "ON DUPLICATE KEY UPDATE" part
            String displayMsgHeaders = getDisplayMsgHeadersString(usm);
            for (int i = 2; i-- > 0;) {
                stmt.setInt(pos++, usm.getBitsValue());
                stmt.setString(pos++, usm.getSendAddr() == null ? "" : usm.getSendAddr());
                stmt.setString(pos++, usm.getReplyToAddr() == null ? "" : usm.getReplyToAddr());
                stmt.setInt(pos++, usm.getMsgFormat());
                if (displayMsgHeaders == null) {
                    stmt.setNull(pos++, Types.VARCHAR);
                } else {
                    stmt.setString(pos++, displayMsgHeaders);
                }
                stmt.setInt(pos++, usm.getAutoLinebreak());
                stmt.setString(pos++, usm.getStdTrashName() == null ? MailStrings.TRASH : usm.getStdTrashName());
                stmt.setString(pos++, usm.getStdSentName() == null ? MailStrings.SENT : usm.getStdSentName());
                stmt.setString(pos++, usm.getStdDraftsName() == null ? MailStrings.DRAFTS : usm.getStdDraftsName());
                stmt.setString(pos++, usm.getStdSpamName() == null ? MailStrings.SPAM : usm.getStdSpamName());
                stmt.setLong(pos++, usm.getUploadQuota());
                stmt.setLong(pos++, usm.getUploadQuotaPerFile());
                stmt.setString(pos++, usm.getConfirmedSpam() == null ? MailStrings.CONFIRMED_SPAM : usm.getConfirmedSpam());
                stmt.setString(pos++, usm.getConfirmedHam() == null ? MailStrings.CONFIRMED_HAM : usm.getConfirmedHam());
            }

            // Return compiled statement
            PreparedStatement retval = stmt;
            stmt = null;
            return retval;
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    private static boolean saveSignatures(UserSettingMail usm, int user, Context ctx, Connection writeConArg) throws OXException {
        try {
            Connection writeCon = writeConArg;
            boolean closeCon = false;
            PreparedStatement stmt = null;
            try {
                if (writeCon == null) {
                    writeCon = DBPool.pickupWriteable(ctx);
                    closeCon = true;
                }

                if (!Databases.tableExists(writeCon, "user_setting_mail_signature")) {
                    usm.setSignatures(null);
                    return true;
                }

                // Delete old
                stmt = writeCon.prepareStatement("DELETE FROM user_setting_mail_signature WHERE cid = ? AND user = ?");
                stmt.setInt(1, ctx.getContextId());
                stmt.setInt(2, user);
                stmt.executeUpdate();
                stmt.close();

                // Insert new (if any)
                Signature[] signatures = usm.getSignatures();
                if ((signatures == null) || (signatures.length == 0)) {
                    return true;
                }
                stmt = null;
                for (int i = 0; i < signatures.length; i++) {
                    Signature sig = signatures[i];
                    if (sig != null) {
                        // Initialize statement if not done, yet
                        if (stmt == null) {
                            stmt = writeCon.prepareStatement("INSERT INTO user_setting_mail_signature (cid, user, id, signature) VALUES (?, ?, ?, ?)");
                        }
                        stmt.setInt(1, ctx.getContextId());
                        stmt.setInt(2, user);
                        stmt.setString(3, sig.getId());
                        stmt.setString(4, sig.getSignature());
                        stmt.addBatch();
                    }
                }
                return stmt != null && stmt.executeBatch().length > 0;
            } finally {
                closeResources(null, stmt, closeCon ? writeCon : null, false, ctx);
            }
        } catch (SQLException e) {
            throw UserConfigurationCodes.SQL_ERROR.create(e, e.getMessage());
        }
    }

    private static String getDisplayMsgHeadersString(UserSettingMail usm) {
        final String[] displayMsgHeaders = usm.getDisplayMsgHeaders();
        if ((displayMsgHeaders == null) || (displayMsgHeaders.length == 0)) {
            return null;
        }
        final StringBuilder tmp = new StringBuilder(256);
        tmp.append(displayMsgHeaders[0]);
        for (int i = 1; i < displayMsgHeaders.length; i++) {
            tmp.append(',').append(displayMsgHeaders[i]);
        }
        return tmp.toString();
    }

    private static void setDisplayMsgHeadersString(UserSettingMail usm, String displayMsgHeadersStr) {
        if (displayMsgHeadersStr == null) {
            usm.setDisplayMsgHeaders(null);
            usm.setModifiedDuringSession(true);
            return;
        }
        usm.setDisplayMsgHeaders(Strings.splitByComma(displayMsgHeadersStr));
        usm.setModifiedDuringSession(true);
    }

    @Override
    public void clearStorage() throws OXException {
        final Cache cache = getCache();
        if (null != cache) {
            /*
             * Put clone into cache
             */
            try {
                cache.clear();
            } catch (Exception e) {
                LOG.error("UserSettingMail's cache could not be cleared", e);
            }
        }
    }

    @Override
    public void removeUserSettingMail(int user, Context ctx) throws OXException {
        final Cache cache = getCache();
        if (null != cache) {
            /*
             * Put clone into cache
             */
            try {
                cache.remove(cache.newCacheKey(ctx.getContextId(), user));
            } catch (Exception e) {
                LOG.error("UserSettingMail could not be removed from cache", e);
            }
        }
    }

    @Override
    public void shutdownStorage() {
        try {
            releaseCache();
        } catch (Exception e) {
            LOG.error("", e);
        }
    }

    @Override
    public void handleAbsence() throws OXException {
        releaseCache();
    }

    @Override
    public void handleAvailability() throws OXException {
        initCache();
    }

}
