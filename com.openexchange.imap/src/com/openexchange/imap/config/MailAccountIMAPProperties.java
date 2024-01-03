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

package com.openexchange.imap.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.openexchange.java.CharsetDetector;
import com.openexchange.java.Strings;
import com.openexchange.mail.PreviewMode;
import com.openexchange.mail.api.MailConfig.BoolCapVal;
import com.openexchange.mail.config.MailAccountProperties;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;

/**
 * {@link MailAccountIMAPProperties} - IMAP properties read from mail account with fallback to properties read from properties file.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailAccountIMAPProperties extends MailAccountProperties implements IIMAPProperties {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailAccountIMAPProperties.class);

    private static final int PRIMARY = Account.DEFAULT_ID;

    private final int mailAccountId;

    /**
     * Initializes a new {@link MailAccountIMAPProperties}.
     *
     * @param mailAccount The mail account
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public MailAccountIMAPProperties(MailAccount mailAccount, int userId, int contextId) {
        super(mailAccount, userId, contextId);
        mailAccountId = mailAccount.getId();
    }

    @Override
    public int getBlockSize() {
        String blockSizeStr = getAccountProperty("com.openexchange.imap.blockSize");
        if (null != blockSizeStr) {
            try {
                return Integer.parseInt(blockSizeStr.trim());
            } catch (NumberFormatException e) {
                LOG.error("Block Size: Invalid value.", e);
                return IMAPProperties.getInstance().getBlockSize();
            }
        }

        if (mailAccountId == PRIMARY) {
            blockSizeStr = lookUpProperty("com.openexchange.imap.primary.blockSize");
            if (null != blockSizeStr) {
                try {
                    return Integer.parseInt(blockSizeStr.trim());
                } catch (NumberFormatException e) {
                    LOG.error("Block Size: Invalid value.", e);
                    return IMAPProperties.getInstance().getBlockSize();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.blockSize", IMAPProperties.getInstance().getBlockSize());
    }

    @Override
    public PreviewMode getPreferredPreviewMode() {
        PreviewMode fallbackMode = IMAPProperties.getInstance().getPreferredPreviewMode();

        String modeStr = getAccountProperty("com.openexchange.imap.preferredPreviewMode");
        if (null != modeStr) {
            PreviewMode parsedMode = PreviewMode.previewModeFor(modeStr);
            return parsedMode == null ? fallbackMode : parsedMode;
        }

        if (mailAccountId == PRIMARY) {
            modeStr = lookUpProperty("com.openexchange.imap.primary.preferredPreviewMode");
            if (null != modeStr) {
                PreviewMode parsedMode = PreviewMode.previewModeFor(modeStr);
                return parsedMode == null ? fallbackMode : parsedMode;
            }
        }

        modeStr = lookUpProperty("com.openexchange.imap.preferredPreviewMode", fallbackMode == null ? "" : fallbackMode.getCapabilityName());
        if (null != modeStr) {
            PreviewMode parsedMode = PreviewMode.previewModeFor(modeStr);
            return parsedMode == null ? fallbackMode : parsedMode;
        }
        return fallbackMode;
    }

    @Override
    public int getMaxNumConnection() {
        String tmp = getAccountProperty("com.openexchange.imap.maxNumConnections");
        if (null != tmp) {
            try {
                return Integer.parseInt(tmp.trim());
            } catch (NumberFormatException e) {
                LOG.error("Max. Number of connections: Invalid value.", e);
                return IMAPProperties.getInstance().getMaxNumConnection();
            }
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.maxNumConnections");
            if (null != tmp) {
                try {
                    return Integer.parseInt(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.error("Max. Number of connections: Invalid value.", e);
                    return IMAPProperties.getInstance().getMaxNumConnection();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.maxNumConnections", IMAPProperties.getInstance().getMaxNumConnection());
    }

    @Override
    public String getImapAuthEnc() {
        String imapAuthEncStr = getAccountProperty("com.openexchange.imap.imapAuthEnc");
        if (null != imapAuthEncStr) {
            imapAuthEncStr = imapAuthEncStr.trim();
            if (CharsetDetector.isValid(imapAuthEncStr)) {
                return imapAuthEncStr;
            }
            final String fallback = IMAPProperties.getInstance().getImapAuthEnc();
            LOG.error("Authentication Encoding: Unsupported charset \"{}\". Setting to fallback: {}", imapAuthEncStr, fallback);
            return fallback;
        }

        if (mailAccountId == PRIMARY) {
            imapAuthEncStr = lookUpProperty("com.openexchange.imap.primary.imapAuthEnc");
            if (null != imapAuthEncStr) {
                imapAuthEncStr = imapAuthEncStr.trim();
                if (CharsetDetector.isValid(imapAuthEncStr)) {
                    return imapAuthEncStr;
                }
                final String fallback = IMAPProperties.getInstance().getImapAuthEnc();
                LOG.error("Authentication Encoding: Unsupported charset \"{}\". Setting to fallback: {}", imapAuthEncStr, fallback);
                return fallback;
            }
        }

        return lookUpProperty("com.openexchange.imap.imapAuthEnc", IMAPProperties.getInstance().getImapAuthEnc());
    }

    @Override
    public int getConnectTimeout() {
        String tmp = getAccountProperty("com.openexchange.imap.imapConnectionTimeout");
        if (null != tmp) {
            try {
                return Integer.parseInt(tmp.trim());
            } catch (NumberFormatException e) {
                LOG.error("IMAP Connect Timeout: Invalid value.", e);
                return IMAPProperties.getInstance().getConnectTimeout();
            }
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.imapConnectionTimeout");
            if (null != tmp) {
                try {
                    return Integer.parseInt(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.error("IMAP Connect Timeout: Invalid value.", e);
                    return IMAPProperties.getInstance().getConnectTimeout();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.imapConnectionTimeout", IMAPProperties.getInstance().getConnectTimeout());
    }

    @Override
    public int getImapTemporaryDown() {
        String tmp = getAccountProperty("com.openexchange.imap.imapTemporaryDown");
        if (null != tmp) {
            try {
                return Integer.parseInt(tmp.trim());
            } catch (NumberFormatException e) {
                LOG.error("IMAP Temporary Down: Invalid value.", e);
                return IMAPProperties.getInstance().getImapTemporaryDown();
            }
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.imapTemporaryDown");
            if (null != tmp) {
                try {
                    return Integer.parseInt(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.error("IMAP Temporary Down: Invalid value.", e);
                    return IMAPProperties.getInstance().getImapTemporaryDown();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.imapTemporaryDown", IMAPProperties.getInstance().getImapTemporaryDown());
    }

    @Override
    public int getImapFailedAuthTimeout() {
        String tmp = getAccountProperty("com.openexchange.imap.failedAuthTimeout");
        if (null != tmp) {
            try {
                return Integer.parseInt(tmp.trim());
            } catch (NumberFormatException e) {
                LOG.error("IMAP Failed Auth Timeout: Invalid value.", e);
                return IMAPProperties.getInstance().getImapFailedAuthTimeout();
            }
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.failedAuthTimeout");
            if (null != tmp) {
                try {
                    return Integer.parseInt(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.error("IMAP Failed Auth Timeout: Invalid value.", e);
                    return IMAPProperties.getInstance().getImapFailedAuthTimeout();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.failedAuthTimeout", IMAPProperties.getInstance().getImapFailedAuthTimeout());
    }

    @Override
    public int getReadTimeout() {
        String tmp = getAccountProperty("com.openexchange.imap.imapTimeout");
        if (null != tmp) {
            try {
                return Integer.parseInt(tmp.trim());
            } catch (NumberFormatException e) {
                LOG.error("IMAP Read Timeout: Invalid value.", e);
                return IMAPProperties.getInstance().getReadTimeout();
            }
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.imapTimeout");
            if (null != tmp) {
                try {
                    return Integer.parseInt(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.error("IMAP Read Timeout: Invalid value.", e);
                    return IMAPProperties.getInstance().getReadTimeout();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.imapTimeout", IMAPProperties.getInstance().getReadTimeout());
    }

    @Override
    public int getReadResponsesTimeout() {
        String tmp = getAccountProperty("com.openexchange.imap.readResponsesTimeout");
        if (null != tmp) {
            try {
                return Integer.parseInt(tmp.trim());
            } catch (NumberFormatException e) {
                LOG.error("IMAP Read Responses Timeout: Invalid value.", e);
                return IMAPProperties.getInstance().getReadResponsesTimeout();
            }
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.readResponsesTimeout");
            if (null != tmp) {
                try {
                    return Integer.parseInt(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.error("IMAP Read Responses Timeout: Invalid value.", e);
                    return IMAPProperties.getInstance().getReadResponsesTimeout();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.readResponsesTimeout", IMAPProperties.getInstance().getReadResponsesTimeout());
    }

    @Override
    public int getFilterReadTimeout() {
        String tmp = getAccountProperty("com.openexchange.imap.filterCommandTimeout");
        if (null != tmp) {
            try {
                return Integer.parseInt(tmp.trim());
            } catch (NumberFormatException e) {
                LOG.error("IMAP Filter Read Timeout: Invalid value.", e);
                return IMAPProperties.getInstance().getFilterReadTimeout();
            }
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.filterCommandTimeout");
            if (null != tmp) {
                try {
                    return Integer.parseInt(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.error("IMAP Filter Read Timeout: Invalid value.", e);
                    return IMAPProperties.getInstance().getFilterReadTimeout();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.filterCommandTimeout", IMAPProperties.getInstance().getFilterReadTimeout());
    }

    @Override
    public Map<String, Boolean> getNewACLExtMap() {
        return IMAPProperties.getInstance().getNewACLExtMap();
    }

    @Override
    public BoolCapVal getSupportsACLs() {
        String tmp = getAccountProperty("com.openexchange.imap.imapSupportsACL");
        if (null != tmp) {
            return BoolCapVal.parseBoolCapVal(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.imapSupportsACL");
            if (null != tmp) {
                return BoolCapVal.parseBoolCapVal(tmp.trim());
            }
        }

        tmp = lookUpProperty("com.openexchange.imap.imapSupportsACL");
        return null == tmp ? IMAPProperties.getInstance().getSupportsACLs() : BoolCapVal.parseBoolCapVal(tmp.trim());
    }

    @Override
    public boolean isFastFetch() {
        String tmp = getAccountProperty("com.openexchange.imap.imapFastFetch");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.imapFastFetch");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.imapFastFetch", IMAPProperties.getInstance().isFastFetch());
    }

    @Override
    public boolean isPropagateClientIPAddress() {
        String tmp = getAccountProperty("com.openexchange.imap.propagateClientIPAddress");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.propagateClientIPAddress");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.propagateClientIPAddress", IMAPProperties.getInstance().isPropagateClientIPAddress());
    }

    @Override
    public boolean isEnableTls() {
        String tmp = getAccountProperty("com.openexchange.imap.enableTls");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.enableTls");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.enableTls", IMAPProperties.getInstance().isEnableTls());
    }

    @Override
    public boolean isUseUTF7ForUserFlags() {
        String tmp = getAccountProperty("com.openexchange.imap.useUTF7ForUserFlags");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.useUTF7ForUserFlags");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.useUTF7ForUserFlags", IMAPProperties.getInstance().isUseUTF7ForUserFlags());
    }

    @Override
    public boolean isRequireTls() {
        String tmp = getAccountProperty("com.openexchange.imap.requireTls");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }
        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.requireTls");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }
        return lookUpBoolProperty("com.openexchange.imap.requireTls", IMAPProperties.getInstance().isRequireTls());
    }

    @Override
    public boolean isAuditLogEnabled() {
        String tmp = getAccountProperty("com.openexchange.imap.auditLog.enabled");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.auditLog.enabled");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.auditLog.enabled", IMAPProperties.getInstance().isAuditLogEnabled());
    }

    @Override
    public boolean isDebugLogEnabled() {
        String tmp = getAccountProperty("com.openexchange.imap.debugLog.enabled");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.debugLog.enabled");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.debugLog.enabled", IMAPProperties.getInstance().isDebugLogEnabled());
    }

    @Override
    public String getDebugServerPattern() {
        String tmp = getAccountProperty("com.openexchange.imap.debugLog.serverPattern");
        if (null != tmp) {
            return tmp.trim();
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.debugLog.serverPattern");
            if (null != tmp) {
                return tmp.trim();
            }
        }

        return lookUpProperty("com.openexchange.imap.debugLog.serverPattern", IMAPProperties.getInstance().getDebugServerPattern());
    }

    @Override
    public boolean isOverwritePreLoginCapabilities() {
        String tmp = getAccountProperty("com.openexchange.imap.overwritePreLoginCapabilities");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.overwritePreLoginCapabilities");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.overwritePreLoginCapabilities", IMAPProperties.getInstance().isOverwritePreLoginCapabilities());
    }

    @Override
    public Set<String> getPropagateHostNames() {
        String tmp = getAccountProperty("com.openexchange.imap.propagateHostNames");
        if (null != tmp) {
            return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(Strings.splitByComma(tmp.trim()))));
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.propagateHostNames");
            if (null != tmp) {
                return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(Strings.splitByComma(tmp.trim()))));
            }
        }

        tmp = lookUpProperty("com.openexchange.imap.propagateHostNames");
        if (null != tmp) {
            return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(Strings.splitByComma(tmp.trim()))));
        }

        return IMAPProperties.getInstance().getPropagateHostNames();
    }

    @Override
    public boolean isImapSearch() {
        String tmp = getAccountProperty("com.openexchange.imap.imapSearch");
        if (null != tmp) {
            return "force-imap".equalsIgnoreCase(tmp) || Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.imapSearch");
            if (null != tmp) {
                return "force-imap".equalsIgnoreCase(tmp) || Boolean.parseBoolean(tmp.trim());
            }
        }

        tmp = lookUpProperty("com.openexchange.imap.imapSearch");
        if (null != tmp) {
            return "force-imap".equalsIgnoreCase(tmp) || Boolean.parseBoolean(tmp.trim());
        }

        return IMAPProperties.getInstance().isImapSearch();
    }

    @Override
    public boolean forceImapSearch() {
        return IMAPProperties.getInstance().forceImapSearch();
    }

    @Override
    public boolean isImapSort() {
        String tmp = getAccountProperty("com.openexchange.imap.imapSort");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.imapSort");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.imapSort", IMAPProperties.getInstance().isImapSort());
    }

    @Override
    public boolean allowFolderCaches() {
        String tmp = getAccountProperty("com.openexchange.imap.allowFolderCaches");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.allowFolderCaches");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.allowFolderCaches", IMAPProperties.getInstance().allowFolderCaches());
    }

    @Override
    public long getFolderCacheTimeoutMillis() {
        String folderCacheTimeoutStr = getAccountProperty("com.openexchange.imap.folderCacheTimeoutMillis");
        if (null != folderCacheTimeoutStr) {
            try {
                long timeout = Long.parseLong(folderCacheTimeoutStr.trim());
                if (timeout <= 0) {
                    timeout = com.openexchange.imap.cache.ListLsubCache.DEFAULT_TIMEOUT;
                }
                return timeout;
            } catch (NumberFormatException e) {
                LOG.error("Folder Cache Timeout: Invalid value.", e);
                return IMAPProperties.getInstance().getFolderCacheTimeoutMillis();
            }
        }

        if (mailAccountId == PRIMARY) {
            folderCacheTimeoutStr = lookUpProperty("com.openexchange.imap.primary.folderCacheTimeoutMillis");
            if (null != folderCacheTimeoutStr) {
                try {
                    long timeout = Long.parseLong(folderCacheTimeoutStr.trim());
                    if (timeout <= 0) {
                        timeout = com.openexchange.imap.cache.ListLsubCache.DEFAULT_TIMEOUT;
                    }
                    return timeout;
                } catch (NumberFormatException e) {
                    LOG.error("Block Size: Invalid value.", e);
                    return IMAPProperties.getInstance().getFolderCacheTimeoutMillis();
                }
            }
        }

        return lookUpIntProperty("com.openexchange.imap.folderCacheTimeoutMillis", (int) IMAPProperties.getInstance().getFolderCacheTimeoutMillis());
    }

    @Override
    public boolean allowFetchSingleHeaders() {
        String tmp = getAccountProperty("com.openexchange.imap.allowFetchSingleHeaders");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.allowFetchSingleHeaders");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        return lookUpBoolProperty("com.openexchange.imap.allowFetchSingleHeaders", IMAPProperties.getInstance().allowFetchSingleHeaders());
    }

    @Override
    public String getSSLProtocols() {
        String tmp = getAccountProperty("com.openexchange.imap.ssl.protocols");
        if (null != tmp) {
            return tmp.trim();
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.ssl.protocols");
            if (null != tmp) {
                return tmp.trim();
            }
        }

        return lookUpProperty("com.openexchange.imap.ssl.protocols", IMAPProperties.getInstance().getSSLProtocols());
    }

    @Override
    public String getSSLCipherSuites() {
        String tmp = getAccountProperty("com.openexchange.imap.ssl.ciphersuites");
        if (null != tmp) {
            return tmp.trim();
        }

        if (mailAccountId == PRIMARY) {
            tmp = lookUpProperty("com.openexchange.imap.primary.ssl.ciphersuites");
            if (null != tmp) {
                return tmp.trim();
            }
        }

        return lookUpProperty("com.openexchange.imap.ssl.ciphersuites", IMAPProperties.getInstance().getSSLCipherSuites());
    }

    @Override
    public boolean isAttachmentMarkerEnabled() {
        if (false == isUserFlagsEnabled()) {
            return false;
        }

        String tmp = getAccountProperty("com.openexchange.imap.attachmentMarker.enabled");
        if (null != tmp) {
            return Boolean.parseBoolean(tmp.trim());
        }

        if (mailAccountId == PRIMARY) { // only for primary account
            tmp = lookUpProperty("com.openexchange.imap.attachmentMarker.enabled");
            if (null != tmp) {
                return Boolean.parseBoolean(tmp.trim());
            }
        }

        // Not applicable for non-primary account
        return false;
    }

}
