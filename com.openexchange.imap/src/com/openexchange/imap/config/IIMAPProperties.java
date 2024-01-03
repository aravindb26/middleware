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

import java.util.Map;
import java.util.Set;
import com.openexchange.mail.PreviewMode;
import com.openexchange.mail.api.IMailProperties;
import com.openexchange.mail.api.MailConfig.BoolCapVal;

/**
 * {@link IIMAPProperties} - Properties for IMAP.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface IIMAPProperties extends IMailProperties {

    /**
     * Whether client's IP address should be propagated by a NOOP command.
     *
     * @return <code>true</code> if client's IP address should be propagated by a NOOP command; otherwise <code>false</code>
     */
    boolean isPropagateClientIPAddress();

    /**
     * Whether to use the STARTTLS command (if supported by the server) to switch the connection to a TLS-protected connection
     *
     * @return <code>true</code> to issue STARTTLS command; otherwise <code>false</code>
     */
    boolean isEnableTls();

    /**
     * Whether using the STARTTLS command is enforced
     *
     * @return <code>true</code> to enforce STARTTLS command; otherwise <code>false</code>
     */
    public boolean isRequireTls();

    /**
     * Whether audit log is enabled or not to trace issued IMAP commands.
     *
     * @return <code>true</code> if audit log is enabled; otherwise <code>false</code>
     */
    boolean isAuditLogEnabled();

    /**
     * Whether debug (traffic) log is enabled or not to trace IMAP communication.
     *
     * @return <code>true</code> if debug log is enabled; otherwise <code>false</code>
     */
    boolean isDebugLogEnabled();

    /**
     * Gets the server pattern to check for when debug (traffic) log is enabled to trace IMAP communication.
     *
     * @return The server pattern
     */
    String getDebugServerPattern();

    /**
     * Whether the pre-login capabilities are supposed to be overwritten (completely replaced with the ones advertised after login)
     *
     * @return <code>true</code> to overwrite; otherwise <code>false</code> to extend
     */
    boolean isOverwritePreLoginCapabilities();

    /**
     * Gets the host names to propagate to.
     *
     * @return The host names to propagate to
     */
    Set<String> getPropagateHostNames();

    /**
     * Checks if fast <code>FETCH</code> is enabled.
     *
     * @return <code>true</code> if fast <code>FETCH</code> is enabled; otherwise <code>false</code>
     */
    boolean isFastFetch();

    /**
     * Gets the IMAP authentication encoding.
     *
     * @return The IMAP authentication encoding
     */
    String getImapAuthEnc();

    /**
     * Gets the IMAP temporary down.
     *
     * @return The IMAP temporary down
     */
    int getImapTemporaryDown();

    /**
     * Gets the timeout for failed authentication attempts.
     *
     * @return The timeout for failed authentication attempts
     */
    int getImapFailedAuthTimeout();

    /**
     * Checks if IMAP search is enabled.
     *
     * @return <code>true</code> if IMAP search is enabled; otherwise <code>false</code>
     */
    boolean isImapSearch();

    /**
     * Checks if IMAP search is enabled and should be forced regardless of the mail fetch limit.
     *
     * @return <code>true</code> if IMAP search should be forced; otherwise <code>false</code>
     */
    boolean forceImapSearch();

    /**
     * Checks if IMAP sort is enabled.
     *
     * @return <code>true</code> if IMAP sort is enabled; otherwise <code>false</code>
     */
    boolean isImapSort();

    /**
     * Indicates support for ACLs.
     *
     * @return The support for ACLs
     */
    BoolCapVal getSupportsACLs();

    /**
     * Gets the block size in which large IMAP commands' UIDs/sequence numbers arguments get splitted.
     *
     * @return The block size
     */
    int getBlockSize();

    /**
     * Gets the max. number of connections
     *
     * @return The max. number of connections
     */
    int getMaxNumConnection();

    /**
     * Whether to allow folder caches.
     *
     * @return <code>true</code> if folder caches are allowed; otherwise <code>false</code>
     */
    boolean allowFolderCaches();

    /**
     * Gets the time-to-live in milliseconds for an initialized folder cache.
     *
     * @return The time-to-live in milliseconds for an initialized folder cache
     */
    long getFolderCacheTimeoutMillis();

    /**
     * Checks whether it is allowed to FETCH single headers
     *
     * @return <code>true</code> if allowed; otherwise <code>false</code>
     */
    boolean allowFetchSingleHeaders();

    /**
     * Gets supported SSL protocols
     *
     * @return Supported SSL protocols
     */
    String getSSLProtocols();

    /**
     * Gets the SSL cipher suites that will be enabled for SSL connections. The property value is a whitespace separated list of tokens
     * acceptable to the <code>javax.net.ssl.SSLSocket.setEnabledProtocols</code> method.
     *
     * @return The SSL cipher suites
     */
    String getSSLCipherSuites();

    /**
     * Checks if attachment marker is enabled.
     *
     * @return <code>true</code> if attachment marker is enabled for the underlying IMAP; otherwise <code>false</code>
     */
    boolean isAttachmentMarkerEnabled();

    /**
     * Gets the optional preferred preview mode.
     *
     * @return The preview mode or <code>null</code>
     */
    PreviewMode getPreferredPreviewMode();

    /**
     * Gets the map holding IMAP servers with new ACL Extension.
     *
     * @return The map holding IMAP servers with new ACL Extension
     * @deprecated Should be unnecessary due to new ACL extension detection
     */
    @Deprecated
    Map<String, Boolean> getNewACLExtMap();

    /**
     * Gets the special read timeout when applying mail filter to existent messages in a folder.
     *
     * @return The read timeout in milliseconds or <code>-1</code> if not applicable (and to fall back to generic read timeout setting)
     */
    int getFilterReadTimeout();

    /**
     * Gets the timeout in milliseconds when reading responses from IMAP server after a command has been issued against it.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: This timeout does only apply to subscribed (not provisioned) IMAP accounts; neither primary nor secondary ones.
     * </div>
     *
     * @return The read responses timeout (in milliseconds) or <code>-1</code> if no timeout should be applied when reading responses
     */
    int getReadResponsesTimeout();

    /**
     * Checks whether user flags are supposed to be encoded/decoded using
     * <a href="https://datatracker.ietf.org/doc/html/rfc2060#section-5.1.3">RFC2060's UTF-7 encoding (Mailbox International Naming Convention)</a>.
     * 
     * @return <code>true</code> to use UTF-7 encoding/decoding; otherwise false
     */
    boolean isUseUTF7ForUserFlags();

}
