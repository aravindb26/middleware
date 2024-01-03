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

package com.openexchange.mail.config;

import static com.openexchange.java.Autoboxing.I;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ConfigurationServices;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.logging.LogMessageBuilder;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.api.IMailProperties;
import com.openexchange.mail.api.MailConfig.LoginSource;
import com.openexchange.mail.api.MailConfig.PasswordSource;
import com.openexchange.mail.api.MailConfig.ServerSource;
import com.openexchange.mail.utils.IpAddressRenderer;
import com.openexchange.net.HostList;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.UserAndContext;
import com.openexchange.tools.net.URIDefaults;

/**
 * {@link MailProperties} - Global mail properties read from properties file.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailProperties implements IMailProperties {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailProperties.class);

    private static volatile MailProperties instance;

    /**
     * Gets the singleton instance of {@link MailProperties}.
     *
     * @return The singleton instance of {@link MailProperties}
     */
    public static MailProperties getInstance() {
        MailProperties tmp = instance;
        if (null == tmp) {
            synchronized (MailProperties.class) {
                tmp = instance;
                if (null == tmp) {
                    tmp = instance = new MailProperties();
                }
            }
        }
        return tmp;
    }

    /**
     * Releases the singleton instance of {@link MailProperties}.
     */
    public static void releaseInstance() {
        if (null != instance) {
            synchronized (MailProperties.class) {
                if (null != instance) {
                    instance = null;
                }
            }
        }
    }

    private static final class PrimaryMailProps {

        static class Params {

            LoginSource loginSource;
            LoginSource secondaryLoginSource;
            PasswordSource passwordSource;
            PasswordSource secondaryPasswordSource;
            ServerSource mailServerSource;
            ServerSource secondaryMailServerSource;
            ServerSource transportServerSource;
            ServerSource secondaryTransportServerSource;
            ConfiguredServer mailServer;
            ConfiguredServer secondaryMailServer;
            ConfiguredServer transportServer;
            ConfiguredServer secondaryTransportServer;
            String masterPassword;
            String secondaryMasterPassword;
            boolean mailStartTls;
            boolean secondaryMailStartTls;
            boolean transportStartTls;
            boolean secondaryTransportStartTls;
            int maxToCcBcc;
            int maxDriveAttachments;
            boolean rateLimitPrimaryOnly;
            int rateLimit;
            String[] phishingHeaders;
            HostList ranges;
            int defaultArchiveDays;
            boolean preferSentDate;
            boolean hidePOP3StorageFolders;
            boolean translateDefaultFolders;
            boolean deleteDraftOnTransport;
            boolean forwardUnquoted;
            long maxMailSize;
            int maxLengthForReferencesHeader;
            int maxForwardCount;
            int mailFetchLimit;

            Params() {
                super();
            }
        }

        // --------------------------------------------------------------------------------

        final LoginSource loginSource;
        final LoginSource secondaryLoginSource;
        final PasswordSource passwordSource;
        final PasswordSource secondaryPasswordSource;
        final ServerSource mailServerSource;
        final ServerSource secondaryMailServerSource;
        final ServerSource transportServerSource;
        final ServerSource secondaryTransportServerSource;
        final ConfiguredServer mailServer;
        final ConfiguredServer secondaryMailServer;
        final ConfiguredServer transportServer;
        final ConfiguredServer secondaryTransportServer;
        final String masterPassword;
        final String secondaryMasterPassword;
        final boolean mailStartTls;
        final boolean secondaryMailStartTls;
        final boolean transportStartTls;
        final boolean secondaryTransportStartTls;
        final int maxToCcBcc;
        final int maxDriveAttachments;
        final boolean rateLimitPrimaryOnly;
        final int rateLimit;
        final HostList ranges;
        final String[] phishingHeaders;
        final int defaultArchiveDays;
        final boolean preferSentDate;
        final boolean hidePOP3StorageFolders;
        final boolean translateDefaultFolders;
        final boolean deleteDraftOnTransport;
        final boolean forwardUnquoted;
        final long maxMailSize;
        final int maxLengthForReferencesHeader;
        final int maxForwardCount;
        final int mailFetchLimit;

        PrimaryMailProps(Params params) {
            super();
            this.loginSource = params.loginSource;
            this.secondaryLoginSource = params.secondaryLoginSource;
            this.passwordSource = params.passwordSource;
            this.secondaryPasswordSource = params.secondaryPasswordSource;
            this.mailServerSource = params.mailServerSource;
            this.secondaryMailServerSource = params.secondaryMailServerSource;
            this.transportServerSource = params.transportServerSource;
            this.secondaryTransportServerSource = params.secondaryTransportServerSource;
            this.mailServer = params.mailServer;
            this.secondaryMailServer = params.secondaryMailServer;
            this.transportServer = params.transportServer;
            this.secondaryTransportServer = params.secondaryTransportServer;
            this.masterPassword = params.masterPassword;
            this.secondaryMasterPassword = params.secondaryMasterPassword;
            this.mailStartTls = params.mailStartTls;
            this.secondaryMailStartTls = params.secondaryMailStartTls;
            this.transportStartTls = params.transportStartTls;
            this.secondaryTransportStartTls = params.secondaryTransportStartTls;
            this.maxToCcBcc = params.maxToCcBcc;
            this.maxDriveAttachments = params.maxDriveAttachments;
            this.rateLimitPrimaryOnly = params.rateLimitPrimaryOnly;
            this.rateLimit = params.rateLimit;
            this.ranges = params.ranges;
            this.phishingHeaders = params.phishingHeaders;
            this.defaultArchiveDays = params.defaultArchiveDays;
            this.preferSentDate = params.preferSentDate;
            this.hidePOP3StorageFolders = params.hidePOP3StorageFolders;
            this.translateDefaultFolders = params.translateDefaultFolders;
            this.deleteDraftOnTransport = params.deleteDraftOnTransport;
            this.forwardUnquoted = params.forwardUnquoted;
            this.maxMailSize = params.maxMailSize;
            this.maxLengthForReferencesHeader = params.maxLengthForReferencesHeader;
            this.maxForwardCount = params.maxForwardCount;
            this.mailFetchLimit = params.mailFetchLimit;
        }

    }

    /** A volatile cache for user-sensitive mail properties */
    private static final Cache<UserAndContext, PrimaryMailProps> VOLATILE_CACHE_PRIMARY_PROPS = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();

    /**
     * Clears the cache.
     */
    public static void invalidateCache() {
        VOLATILE_CACHE_PRIMARY_PROPS.invalidateAll();
    }

    private static PrimaryMailProps getPrimaryMailProps(int userId, int contextId) throws OXException {
        UserAndContext key = UserAndContext.newInstance(userId, contextId);
        PrimaryMailProps primaryMailProps = VOLATILE_CACHE_PRIMARY_PROPS.getIfPresent(key);
        if (null != primaryMailProps) {
            return primaryMailProps;
        }

        Callable<PrimaryMailProps> loader = new Callable<MailProperties.PrimaryMailProps>() {

            @Override
            public PrimaryMailProps call() throws Exception {
                return doGetPrimaryMailProps(userId, contextId);
            }
        };

        try {
            return VOLATILE_CACHE_PRIMARY_PROPS.get(key, loader);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw cause instanceof OXException ? (OXException) cause : new OXException(cause);
        }
    }

    static PrimaryMailProps doGetPrimaryMailProps(int userId, int contextId) throws OXException {
        ConfigViewFactory viewFactory = ServerServiceRegistry.getInstance().getService(ConfigViewFactory.class);
        if (null == viewFactory) {
            throw ServiceExceptionCode.absentService(ConfigViewFactory.class);
        }

        ConfigView view = viewFactory.getView(userId, contextId);

        PrimaryMailProps.Params params = new PrimaryMailProps.Params();

        LogMessageBuilder logMessageBuilder = LOG.isDebugEnabled() ? LogMessageBuilder.createLogMessageBuilder(1024, 16) : LogMessageBuilder.emptyLogMessageBuilder();

        logMessageBuilder.appendln("Primary mail properties successfully loaded for user {} in context {}", Integer.valueOf(userId), Integer.valueOf(contextId));

        {
            final String loginStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.loginSource", view);
            if (loginStr == null) {
                throw MailConfigException.create("Property \"com.openexchange.mail.loginSource\" not set");
            }
            LoginSource loginSource = LoginSource.parse(loginStr.trim());
            if (null == loginSource) {
                throw MailConfigException.create(new StringBuilder(256).append("Unknown value in property \"com.openexchange.mail.loginSource\": ").append(loginStr).toString());
            }
            params.loginSource = loginSource;

            logMessageBuilder.appendln("  Login Source: {}", loginSource);
        }

        {
            final String loginStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.secondary.loginSource", view);
            LoginSource secondaryLoginSource = loginStr == null ? LoginSource.USER_IMAPLOGIN : LoginSource.parse(loginStr.trim());
            if (null == secondaryLoginSource) {
                throw MailConfigException.create(new StringBuilder(256).append("Unknown value in property \"com.openexchange.mail.secondary.loginSource\": ").append(loginStr).toString());
            }
            params.secondaryLoginSource = secondaryLoginSource;

            logMessageBuilder.appendln("  Secondary Login Source: {}", secondaryLoginSource);
        }

        {
            final String pwStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.passwordSource", view);
            if (pwStr == null) {
                throw MailConfigException.create("Property \"com.openexchange.mail.passwordSource\" not set");
            }
            PasswordSource passwordSource = PasswordSource.parse(pwStr.trim());
            if (null == passwordSource) {
                throw MailConfigException.create(new StringBuilder(256).append("Unknown value in property \"com.openexchange.mail.passwordSource\": ").append(pwStr).toString());
            }
            params.passwordSource = passwordSource;

            logMessageBuilder.appendln("  Password Source: {}", passwordSource);
        }

        {
            final String pwStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.secondary.passwordSource", view);
            final PasswordSource passwordSource = pwStr == null ? PasswordSource.SESSION : PasswordSource.parse(pwStr.trim());
            if (null == passwordSource) {
                throw MailConfigException.create(new StringBuilder(256).append("Unknown value in property \"com.openexchange.mail.secondary.passwordSource\": ").append(pwStr).toString());
            }
            params.secondaryPasswordSource = passwordSource;

            logMessageBuilder.appendln("  Secondary Password Source: {}", passwordSource);
        }

        {
            final String mailSrcStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.mailServerSource", view);
            if (mailSrcStr == null) {
                throw MailConfigException.create("Property \"com.openexchange.mail.mailServerSource\" not set");
            }
            ServerSource mailServerSource = ServerSource.parse(mailSrcStr.trim());
            if (null == mailServerSource) {
                throw MailConfigException.create(new StringBuilder(256).append("Unknown value in property \"com.openexchange.mail.mailServerSource\": ").append(mailSrcStr).toString());
            }
            params.mailServerSource = mailServerSource;

            logMessageBuilder.appendln("  Mail Server Source: {}", mailServerSource);
        }

        {
            final String mailSrcStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.secondary.mailServerSource", view);
            ServerSource mailServerSource = mailSrcStr == null ? ServerSource.USER : ServerSource.parse(mailSrcStr.trim());
            if (null == mailServerSource) {
                throw MailConfigException.create(new StringBuilder(256).append("Unknown value in property \"com.openexchange.mail.secondary.mailServerSource\": ").append(mailSrcStr).toString());
            }
            params.secondaryMailServerSource = mailServerSource;

            logMessageBuilder.appendln("  Secondary Mail Server Source: {}", mailServerSource);
        }

        {
            final String transSrcStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.transportServerSource", view);
            if (transSrcStr == null) {
                throw MailConfigException.create("Property \"com.openexchange.mail.transportServerSource\" not set");
            }
            ServerSource transportServerSource = ServerSource.parse(transSrcStr.trim());
            if (null == transportServerSource) {
                throw MailConfigException.create(new StringBuilder(256).append("Unknown value in property \"com.openexchange.mail.transportServerSource\": ").append(transSrcStr).toString());
            }
            params.transportServerSource = transportServerSource;

            logMessageBuilder.appendln("  Transport Server Source: {}", transportServerSource);
        }

        {
            final String transSrcStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.secondary.transportServerSource", view);
            ServerSource transportServerSource = transSrcStr == null ? ServerSource.USER : ServerSource.parse(transSrcStr.trim());
            if (null == transportServerSource) {
                throw MailConfigException.create(new StringBuilder(256).append("Unknown value in property \"com.openexchange.mail.secondary.transportServerSource\": ").append(transSrcStr).toString());
            }
            params.secondaryTransportServerSource = transportServerSource;

            logMessageBuilder.appendln("  Secondary Transport Server Source: {}", transportServerSource);
        }

        {
            ConfiguredServer mailServer = null;
            String tmp = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.mailServer", view);
            if (tmp != null) {
                mailServer = ConfiguredServer.parseFrom(tmp.trim(), URIDefaults.IMAP);

                logMessageBuilder.appendln("  Mail Server: {}", mailServer);
            }
            params.mailServer = mailServer;

        }

        {
            ConfiguredServer secondaryMailServer = null;
            String tmp = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.secondary.mailServer", view);
            if (tmp != null) {
                secondaryMailServer = ConfiguredServer.parseFrom(tmp.trim(), URIDefaults.IMAP);

                logMessageBuilder.appendln("  Secondary Mail Server: {}", secondaryMailServer);
            }
            params.secondaryMailServer = secondaryMailServer;

        }

        {
            ConfiguredServer transportServer = null;
            String tmp = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.transportServer", view);
            if (tmp != null) {
                transportServer = ConfiguredServer.parseFrom(tmp.trim(), URIDefaults.SMTP);

                logMessageBuilder.appendln("  Transport Server: {}", transportServer);
            }
            params.transportServer = transportServer;
        }

        {
            ConfiguredServer secondaryTransportServer = null;
            String tmp = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.secondary.transportServer", view);
            if (tmp != null) {
                secondaryTransportServer = ConfiguredServer.parseFrom(tmp.trim(), URIDefaults.SMTP);

                logMessageBuilder.appendln("  Secondary Transport Server: {}", secondaryTransportServer);
            }
            params.secondaryTransportServer = secondaryTransportServer;
        }

        {
            String masterPassword = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.masterPassword", view);
            if (masterPassword != null) {
                masterPassword = masterPassword.trim();

                logMessageBuilder.appendln("  Master Password: {}", "XXXXXXX");
            }
            params.masterPassword = masterPassword;
        }

        {
            String secondaryMasterPassword = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.secondary.masterPassword", view);
            if (secondaryMasterPassword != null) {
                secondaryMasterPassword = secondaryMasterPassword.trim();

                logMessageBuilder.appendln("  Secondary Master Password: {}", "XXXXXXX");
            }
            params.secondaryMasterPassword = secondaryMasterPassword;
        }

        params.mailStartTls = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.mailStartTls", false, view);
        params.transportStartTls = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.transportStartTls", false, view);

        params.secondaryMailStartTls = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.secondary.mailStartTls", false, view);
        params.secondaryTransportStartTls = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.secondary.transportStartTls", false, view);

        {
            try {
                params.maxToCcBcc = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.mail.maxToCcBcc", 0, view);
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                params.maxToCcBcc = 0;
            }

            logMessageBuilder.appendln("  maxToCcBcc: {}", Integer.valueOf(params.maxToCcBcc));
        }

        {
            try {
                params.maxDriveAttachments = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.mail.maxDriveAttachments", 20, view);
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                params.maxDriveAttachments = 20;
            }

            logMessageBuilder.appendln("  maxDriveAttachments: {}", Integer.valueOf(params.maxDriveAttachments));
        }

        {
            String phishingHdrsStr = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.phishingHeader", view);
            if (null != phishingHdrsStr && phishingHdrsStr.length() > 0) {
                params.phishingHeaders = Strings.splitByComma(phishingHdrsStr);

                logMessageBuilder.appendln("  Phishing Headers: {}", Arrays.toString(params.phishingHeaders));
            } else {
                params.phishingHeaders = null;
            }
        }

        {
            params.rateLimitPrimaryOnly = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.rateLimitPrimaryOnly", true, view);

            logMessageBuilder.appendln("  Rate limit primary only: {}", Boolean.valueOf(params.rateLimitPrimaryOnly));
        }

        {
            try {
                params.rateLimit = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.mail.rateLimit", 0, view);
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                params.rateLimit = 0;
            }

            logMessageBuilder.appendln("  Sent Rate limit: {}", Integer.valueOf(params.rateLimit));
        }

        {
            HostList ranges = HostList.EMPTY;
            String tmp = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.rateLimitDisabledRange", view);
            if (Strings.isNotEmpty(tmp)) {
                ranges = HostList.valueOf(tmp);
            }
            params.ranges = ranges;

            logMessageBuilder.appendln("  White-listed from send rate limit: {}", ranges.toString());
        }

        {
            try {
                params.defaultArchiveDays = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.mail.archive.defaultDays", 90, view);
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                params.defaultArchiveDays = 90;
            }

            logMessageBuilder.appendln("  Default archive days: {}", Integer.valueOf(params.defaultArchiveDays));
        }

        {
            params.preferSentDate = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.preferSentDate", false, view);

            logMessageBuilder.appendln("  Prefer Sent Date: {}", Boolean.valueOf(params.preferSentDate));
        }

        {
            params.hidePOP3StorageFolders = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.hidePOP3StorageFolders", false, view);

            logMessageBuilder.appendln("  Hide POP3 Storage Folder: {}", Boolean.valueOf(params.hidePOP3StorageFolders));
        }

        {
            params.translateDefaultFolders = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.translateDefaultFolders", true, view);

            logMessageBuilder.appendln("  Translate Default Folders: {}", Boolean.valueOf(params.translateDefaultFolders));
        }

        {
            params.deleteDraftOnTransport = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.deleteDraftOnTransport", false, view);

            logMessageBuilder.appendln("  Delete Draft On Transport: {}", Boolean.valueOf(params.deleteDraftOnTransport));
        }

        {
            params.forwardUnquoted = ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.mail.forwardUnquoted", false, view);

            logMessageBuilder.appendln("  Forward Unquoted: {}", Boolean.valueOf(params.forwardUnquoted));
        }

        {
            long maxMailSize;
            String tmp = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.maxMailSize", view);
            if (null == tmp) {
                maxMailSize = -1L;
            } else {
                try {
                    maxMailSize = Long.parseLong(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.debug("", e);
                    maxMailSize = -1L;
                }
            }
            params.maxMailSize = maxMailSize;

            logMessageBuilder.appendln("  Max. Mail Size: {}", Long.valueOf(params.maxMailSize));
        }

        {
            int maxLengthForReferencesHeader;
            String tmp = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.mail.maxLengthForReferencesHeader", view);
            if (null == tmp) {
                maxLengthForReferencesHeader = 998;
            } else {
                try {
                    maxLengthForReferencesHeader = Integer.parseInt(tmp.trim());
                } catch (NumberFormatException e) {
                    LOG.debug("", e);
                    maxLengthForReferencesHeader = 998;
                }
            }
            params.maxLengthForReferencesHeader = maxLengthForReferencesHeader;

            logMessageBuilder.appendln("  Max. Length For \"References\" header: {}", Integer.valueOf(params.maxLengthForReferencesHeader));
        }

        {
            try {
                params.maxForwardCount = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.mail.maxForwardCount", 8, view);
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                params.maxForwardCount = 8;
            }

            logMessageBuilder.appendln("  Max. Forward Count: {}", Integer.valueOf(params.maxForwardCount));
        }

        {
            try {
                params.mailFetchLimit = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.mail.mailFetchLimit", 1000, view);
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                params.mailFetchLimit = 1000;
            }

            logMessageBuilder.appendln("  Mail Fetch Limit: {}", Integer.valueOf(params.mailFetchLimit));
        }

        PrimaryMailProps primaryMailProps = new PrimaryMailProps(params);
        LOG.debug(logMessageBuilder.getMessage(), logMessageBuilder.getArgumentsAsArray());
        return primaryMailProps;
    }


    // -----------------------------------------------------------------------------------------------------

    private final AtomicBoolean loaded;

    /*-
     * Fields for global properties
     */

    private LoginSource loginSource;
    private LoginSource secondaryLoginSource;

    private PasswordSource passwordSource;
    private PasswordSource secondaryPasswordSource;

    private ServerSource mailServerSource;
    private ServerSource secondaryMailServerSource;

    private ServerSource transportServerSource;
    private ServerSource secondaryTransportServerSource;

    private ConfiguredServer mailServer;
    private ConfiguredServer secondaryMailServer;

    private ConfiguredServer transportServer;
    private ConfiguredServer secondaryTransportServer;

    private String masterPassword;
    private String secondaryMasterPassword;

    private int mailFetchLimit;

    private int bodyDisplaySize;

    private int connectTimeout;

    private int readTimeout;

    private boolean userFlagsEnabled;

    private boolean allowNestedDefaultFolderOnAltNamespace;

    private boolean hideInlineImages;

    private String defaultMimeCharset;

    private boolean ignoreSubscription;

    private boolean hidePOP3StorageFolders;

    private boolean preferSentDate;

    private char defaultSeparator;

    private String[] quoteLineColors;

    private Properties javaMailProperties;

    private boolean watcherEnabled;

    private int watcherTime;

    private int watcherFrequency;

    private boolean watcherShallClose;

    private boolean supportSubscription;

    private String[] phishingHeaders;

    private String defaultMailProvider;

    private boolean adminMailLoginEnabled;

    private int mailAccessCacheShrinkerSeconds;

    private int mailAccessCacheIdleSeconds;

    private boolean addClientIPAddress;

    private boolean appendVersionToMailerHeader;

    private IpAddressRenderer ipAddressRenderer;

    private boolean rateLimitPrimaryOnly;

    private int rateLimit;

    private int maxToCcBcc;

    private int maxDriveAttachments;

    private int emailAddressParserWaitMillis;

    private String authProxyDelimiter;
    private String secondaryAuthProxyDelimiter;

    /** Indicates whether MSISDN addresses should be supported or not. */
    private boolean supportMsisdnAddresses;

    private int defaultArchiveDays;

    private HostList ranges;

    private boolean mailStartTls;
    private boolean secondaryMailStartTls;

    private boolean transportStartTls;
    private boolean secondaryTransportStartTls;

    private static final boolean DEFAULT_SUPPORT_APPLE_MAIL_FLAGS = true;
    private boolean supportAppleMailFlags;

    private static final boolean DEFAULT_ORDER_COLOR_FLAGS_NATURALLY = true;
    private boolean orderColorFlagsNaturally; // The natural order by color spectrum

    /**
     * Initializes a new {@link MailProperties}
     */
    private MailProperties() {
        super();
        loaded = new AtomicBoolean();
        defaultSeparator = '/';
        ranges = HostList.EMPTY;
        hideInlineImages = true;
        appendVersionToMailerHeader = true;
        supportAppleMailFlags = DEFAULT_SUPPORT_APPLE_MAIL_FLAGS;
        orderColorFlagsNaturally = DEFAULT_ORDER_COLOR_FLAGS_NATURALLY;
    }

    /**
     * Exclusively loads the global mail properties
     *
     * @throws OXException If loading of global mail properties fails
     */
    public void loadProperties() throws OXException {
        if (!loaded.get()) {
            synchronized (loaded) {
                if (!loaded.get()) {
                    loadProperties0();
                    loaded.set(true);
                    loaded.notifyAll();
                }
            }
        }
    }

    /**
     * Exclusively resets the global mail properties
     */
    public void resetProperties() {
        if (loaded.get()) {
            synchronized (loaded) {
                if (loaded.get()) {
                    resetFields();
                    loaded.set(false);
                }
            }
        }
    }

    /**
     * Waits for loading this properties.
     *
     * @throws InterruptedException If another thread interrupted the current thread before or while the current thread was waiting for
     *             loading the properties.
     */
    @Override
    public void waitForLoading() throws InterruptedException {
        if (!loaded.get()) {
            synchronized (loaded) {
                while (!loaded.get()) {
                    loaded.wait();
                }
            }
        }
    }

    private void resetFields() {
        loginSource = null;
        secondaryLoginSource = null;
        passwordSource = null;
        secondaryPasswordSource = null;
        mailServerSource = null;
        secondaryMailServerSource = null;
        transportServerSource = null;
        secondaryTransportServerSource = null;
        mailServer = null;
        secondaryMailServer = null;
        transportServer = null;
        secondaryTransportServer = null;
        masterPassword = null;
        secondaryMasterPassword = null;
        mailFetchLimit = 0;
        bodyDisplaySize = 10485760; // 10 MB
        connectTimeout = 20000;
        readTimeout = 50000;
        userFlagsEnabled = false;
        allowNestedDefaultFolderOnAltNamespace = false;
        hideInlineImages = true;
        defaultMimeCharset = null;
        ignoreSubscription = false;
        hidePOP3StorageFolders = false;
        preferSentDate = false;
        defaultSeparator = '/';
        quoteLineColors = null;
        javaMailProperties = null;
        watcherEnabled = false;
        watcherTime = 0;
        watcherFrequency = 0;
        watcherShallClose = false;
        supportSubscription = false;
        defaultMailProvider = null;
        adminMailLoginEnabled = false;
        mailAccessCacheShrinkerSeconds = 0;
        mailAccessCacheIdleSeconds = 0;
        addClientIPAddress = false;
        appendVersionToMailerHeader = true;
        ipAddressRenderer = IpAddressRenderer.simpleRenderer();
        rateLimitPrimaryOnly = true;
        rateLimit = 0;
        emailAddressParserWaitMillis = 7000;
        maxToCcBcc = 0;
        maxDriveAttachments = 20;
        authProxyDelimiter = null;
        secondaryAuthProxyDelimiter = null;
        supportMsisdnAddresses = false;
        defaultArchiveDays = 90;
        ranges = HostList.EMPTY;
        mailStartTls = false;
        secondaryMailStartTls = false;
        transportStartTls = false;
        secondaryTransportStartTls = false;
        supportAppleMailFlags = DEFAULT_SUPPORT_APPLE_MAIL_FLAGS;
        orderColorFlagsNaturally = DEFAULT_ORDER_COLOR_FLAGS_NATURALLY;
    }

    private void loadProperties0() throws OXException {
        LogMessageBuilder logBuilder = LOG.isInfoEnabled() ? LogMessageBuilder.createLogMessageBuilder(1024, 16) : LogMessageBuilder.emptyLogMessageBuilder();

        logBuilder.append("{}Loading global mail properties...{}", Strings.getLineSeparator(), Strings.getLineSeparator());

        final ConfigurationService configuration = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);

        {
            final String loginStr = configuration.getProperty("com.openexchange.mail.loginSource");
            if (loginStr == null) {
                throw MailConfigException.create("Property \"com.openexchange.mail.loginSource\" not set");
            }
            final LoginSource loginSource = LoginSource.parse(loginStr.trim());
            if (null == loginSource) {
                throw MailConfigException.create(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.loginSource\": ").append(loginStr).toString());
            }
            this.loginSource = loginSource;
            logBuilder.appendln("\tLogin Source: {}", this.loginSource.toString());
        }

        {
            final String loginStr = configuration.getProperty("com.openexchange.mail.secondary.loginSource");
            final LoginSource secondaryLoginSource = loginStr == null ? LoginSource.USER_IMAPLOGIN : LoginSource.parse(loginStr.trim());
            if (null == loginSource) {
                throw MailConfigException.create(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.secondary.loginSource\": ").append(loginStr).toString());
            }
            this.secondaryLoginSource = secondaryLoginSource;
            logBuilder.appendln("\tSecondary Login Source: {}", this.secondaryLoginSource.toString());
        }

        {
            final String pwStr = configuration.getProperty("com.openexchange.mail.passwordSource");
            if (pwStr == null) {
                throw MailConfigException.create("Property \"com.openexchange.mail.passwordSource\" not set");
            }
            final PasswordSource pwSource = PasswordSource.parse(pwStr.trim());
            if (null == pwSource) {
                throw MailConfigException.create(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.passwordSource\": ").append(pwStr).toString());
            }
            passwordSource = pwSource;
            logBuilder.appendln("\tPassword Source: {}", this.passwordSource.toString());
        }

        {
            final String pwStr = configuration.getProperty("com.openexchange.mail.secondary.passwordSource");
            final PasswordSource pwSource = pwStr == null ? PasswordSource.SESSION : PasswordSource.parse(pwStr.trim());
            if (null == pwSource) {
                throw MailConfigException.create(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.secondary.passwordSource\": ").append(pwStr).toString());
            }
            secondaryPasswordSource = pwSource;
            logBuilder.appendln("\tSecondary Password Source: {}", this.secondaryPasswordSource.toString());
        }

        {
            final String mailSrcStr = configuration.getProperty("com.openexchange.mail.mailServerSource");
            if (mailSrcStr == null) {
                throw MailConfigException.create("Property \"com.openexchange.mail.mailServerSource\" not set");
            }
            final ServerSource mailServerSource = ServerSource.parse(mailSrcStr.trim());
            if (null == mailServerSource) {
                throw MailConfigException.create(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.mailServerSource\": ").append(mailSrcStr).toString());
            }
            this.mailServerSource = mailServerSource;
            logBuilder.appendln("\tMail Server Source: {}", this.mailServerSource.toString());
        }

        {
            final String mailSrcStr = configuration.getProperty("com.openexchange.mail.secondary.mailServerSource");
            final ServerSource mailServerSource = mailSrcStr == null ? ServerSource.USER : ServerSource.parse(mailSrcStr.trim());
            if (null == mailServerSource) {
                throw MailConfigException.create(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.secondary.mailServerSource\": ").append(mailSrcStr).toString());
            }
            this.secondaryMailServerSource = mailServerSource;
            logBuilder.appendln("\tSecondary Mail Server Source: {}", this.secondaryMailServerSource.toString());
        }

        {
            final String transSrcStr = configuration.getProperty("com.openexchange.mail.transportServerSource");
            if (transSrcStr == null) {
                throw MailConfigException.create("Property \"com.openexchange.mail.transportServerSource\" not set");
            }
            final ServerSource transportServerSource = ServerSource.parse(transSrcStr.trim());
            if (null == transportServerSource) {
                throw MailConfigException.create(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.transportServerSource\": ").append(transSrcStr).toString());
            }
            this.transportServerSource = transportServerSource;
            logBuilder.appendln("\tTransport Server Source: {}", this.transportServerSource.toString());
        }

        {
            final String transSrcStr = configuration.getProperty("com.openexchange.mail.secondary.transportServerSource");
            final ServerSource transportServerSource = transSrcStr == null ? ServerSource.USER : ServerSource.parse(transSrcStr.trim());
            if (null == transportServerSource) {
                throw MailConfigException.create(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.secondary.transportServerSource\": ").append(transSrcStr).toString());
            }
            this.secondaryTransportServerSource = transportServerSource;
            logBuilder.appendln("\tSecondary Transport Server Source: {}", this.secondaryTransportServerSource.toString());
        }

        {
            String mailServer = configuration.getProperty("com.openexchange.mail.mailServer");
            if (mailServer != null) {
                mailServer = mailServer.trim();
                this.mailServer = ConfiguredServer.parseFrom(mailServer, URIDefaults.IMAP);
            }
        }

        {
            String secondaryMailServer = configuration.getProperty("com.openexchange.mail.secondary.mailServer");
            if (secondaryMailServer != null) {
                secondaryMailServer = secondaryMailServer.trim();
                this.secondaryMailServer = ConfiguredServer.parseFrom(secondaryMailServer, URIDefaults.IMAP);
            }
        }

        {
            String transportServer = configuration.getProperty("com.openexchange.mail.transportServer");
            if (transportServer != null) {
                transportServer = transportServer.trim();
                this.transportServer = ConfiguredServer.parseFrom(transportServer, URIDefaults.SMTP);
            }
        }

        {
            String transportServer = configuration.getProperty("com.openexchange.mail.secondary.transportServer");
            if (transportServer != null) {
                transportServer = transportServer.trim();
                this.secondaryTransportServer = ConfiguredServer.parseFrom(transportServer, URIDefaults.SMTP);
            }
        }

        {
            masterPassword = configuration.getProperty("com.openexchange.mail.masterPassword");
            if (masterPassword != null) {
                masterPassword = masterPassword.trim();
            }
        }

        {
            secondaryMasterPassword = configuration.getProperty("com.openexchange.mail.secondary.masterPassword");
            if (secondaryMasterPassword != null) {
                secondaryMasterPassword = secondaryMasterPassword.trim();
            }
        }

        {
            final String mailFetchLimitStr = configuration.getProperty("com.openexchange.mail.mailFetchLimit", "1000").trim();
            try {
                mailFetchLimit = Integer.parseInt(mailFetchLimitStr);
                logBuilder.appendln("\tMail Fetch Limit: {}", Integer.valueOf(mailFetchLimit));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                mailFetchLimit = 1000;
                logBuilder.appendln("\tMail Fetch Limit: Non parseable value \"{}\". Setting to fallback: {}", mailFetchLimitStr, Integer.valueOf(mailFetchLimit));
            }
        }

        {
            final String bodyDisplaySizeStr = configuration.getProperty("com.openexchange.mail.bodyDisplaySizeLimit", "10485760").trim();
            try {
                bodyDisplaySize = Integer.parseInt(bodyDisplaySizeStr);
                logBuilder.appendln("\tBody Display Size Limit: {}", Integer.valueOf(bodyDisplaySize));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                bodyDisplaySize = 10485760;
                logBuilder.appendln("\tBody Display Size Limit: Non parseable value \"{}\". Setting to fallback: {}", bodyDisplaySizeStr, Integer.valueOf(bodyDisplaySize));
            }
        }

        {
            final String connectTimeoutStr = configuration.getProperty("com.openexchange.mail.connectTimeout", "20000").trim();
            try {
                connectTimeout = Integer.parseInt(connectTimeoutStr);
                logBuilder.appendln("\tMail Connect Timeout: {}", Integer.valueOf(connectTimeout));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                connectTimeout = 20000;
                logBuilder.appendln("\tMail Connect Timeout: Non parseable value \"{}\". Setting to fallback: {}", connectTimeoutStr, Integer.valueOf(connectTimeout));
            }
        }

        {
            final String readTimeoutStr = configuration.getProperty("com.openexchange.mail.readTimeout", "50000").trim();
            try {
                readTimeout = Integer.parseInt(readTimeoutStr);
                logBuilder.appendln("\tMail Read Timeout: {}", Integer.valueOf(readTimeout));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                readTimeout = 50000;
                logBuilder.appendln("\tMail Read Timeout: Non parseable value \"{}\". Setting to fallback: {}", readTimeoutStr, Integer.valueOf(readTimeout));
            }
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.mail.mailAccessCacheShrinkerSeconds", "3").trim();
            try {
                mailAccessCacheShrinkerSeconds = Integer.parseInt(tmp);
                logBuilder.appendln("\tMail Access Cache shrinker-interval seconds: {}", Integer.valueOf(mailAccessCacheShrinkerSeconds));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                mailAccessCacheShrinkerSeconds = 3;
                logBuilder.appendln("\tMail Access Cache shrinker-interval seconds: Non parseable value \"{}\". Setting to fallback: {}", tmp, Integer.valueOf(mailAccessCacheShrinkerSeconds));
            }
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.mail.mailAccessCacheIdleSeconds", "4").trim();
            try {
                mailAccessCacheIdleSeconds = Integer.parseInt(tmp);
                logBuilder.appendln("\tMail Access Cache idle seconds: {}", Integer.valueOf(mailAccessCacheIdleSeconds));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                mailAccessCacheIdleSeconds = 4;
                logBuilder.appendln("\tMail Access Cache idle seconds: Non parseable value \"{}\". Setting to fallback: {}", tmp, Integer.valueOf(mailAccessCacheIdleSeconds));
            }
        }

        {
            final String userFlagsStr = configuration.getProperty("com.openexchange.mail.userFlagsEnabled", "false").trim();
            userFlagsEnabled = Boolean.parseBoolean(userFlagsStr);
            logBuilder.appendln("\tUser Flags Enabled: {}", Boolean.valueOf(userFlagsEnabled));
        }

        {
            final String allowNestedStr = configuration.getProperty("com.openexchange.mail.allowNestedDefaultFolderOnAltNamespace", "false").trim();
            allowNestedDefaultFolderOnAltNamespace = Boolean.parseBoolean(allowNestedStr);
            logBuilder.appendln("\tAllow Nested Default Folders on AltNamespace: {}", Boolean.valueOf(allowNestedDefaultFolderOnAltNamespace));
        }

        {
            final String hideInlineImagesStr = configuration.getProperty("com.openexchange.mail.hideInlineImages", "true").trim();
            hideInlineImages = Boolean.parseBoolean(hideInlineImagesStr);
            logBuilder.appendln("\tHide Inline Images: {}", Boolean.valueOf(hideInlineImages));
        }

        {
            final String defaultMimeCharsetStr = configuration.getProperty("mail.mime.charset", "UTF-8").trim();
            /*
             * Check validity
             */
            try {
                Charset.forName(defaultMimeCharsetStr);
                defaultMimeCharset = defaultMimeCharsetStr;
                logBuilder.appendln("\tDefault MIME Charset: {}", defaultMimeCharset);
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                LOG.debug("", t);
                defaultMimeCharset = "UTF-8";
                logBuilder.appendln("\tDefault MIME Charset: Unsupported charset \"{}\". Setting to fallback: {}", defaultMimeCharsetStr, defaultMimeCharset);
            }
            /*
             * Add to system properties, too
             */
            System.getProperties().setProperty("mail.mime.charset", defaultMimeCharset);
        }

        {
            final String defaultMailProviderStr = configuration.getProperty("com.openexchange.mail.defaultMailProvider", "imap").trim();
            defaultMailProvider = defaultMailProviderStr;
            logBuilder.appendln("\tDefault Mail Provider: {}", defaultMailProvider);
        }

        {
            final String adminMailLoginEnabledStr = configuration.getProperty("com.openexchange.mail.adminMailLoginEnabled", "false").trim();
            adminMailLoginEnabled = Boolean.parseBoolean(adminMailLoginEnabledStr);
            logBuilder.appendln("\tAdmin Mail Login Enabled: {}", Boolean.valueOf(adminMailLoginEnabled));
        }

        {
            final String ignoreSubsStr = configuration.getProperty("com.openexchange.mail.ignoreSubscription", "false").trim();
            ignoreSubscription = Boolean.parseBoolean(ignoreSubsStr);
            logBuilder.appendln("\tIgnore Folder Subscription: {}", Boolean.valueOf(ignoreSubscription));
        }

        {
            final String preferSentDateStr = configuration.getProperty("com.openexchange.mail.preferSentDate", "false").trim();
            preferSentDate = Boolean.parseBoolean(preferSentDateStr);
            logBuilder.appendln("\tPrefer Sent Date: {}", Boolean.valueOf(preferSentDate));
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.mail.hidePOP3StorageFolders", "false").trim();
            hidePOP3StorageFolders = Boolean.parseBoolean(tmp);
            logBuilder.appendln("\tHide POP3 Storage Folders: {}", Boolean.valueOf(hidePOP3StorageFolders));
        }

        {
            final String supSubsStr = configuration.getProperty("com.openexchange.mail.supportSubscription", "true").trim();
            supportSubscription = Boolean.parseBoolean(supSubsStr);
            logBuilder.appendln("\tSupport Subscription: {}", Boolean.valueOf(supportSubscription));
        }

        {
            String tmp = configuration.getProperty("com.openexchange.mail.appendVersionToMailerHeader", "true").trim();
            appendVersionToMailerHeader = Boolean.parseBoolean(tmp);
            logBuilder.appendln("\tAppend Version To Mailer Header: {}", Boolean.valueOf(appendVersionToMailerHeader));
        }

        {
            String tmp = configuration.getProperty("com.openexchange.mail.addClientIPAddress", "false").trim();
            addClientIPAddress = Boolean.parseBoolean(tmp);
            logBuilder.appendln("\tAdd Client IP Address: {}", Boolean.valueOf(addClientIPAddress));

            if (addClientIPAddress) {
                tmp = configuration.getProperty("com.openexchange.mail.clientIPAddressPattern");
                if (null == tmp) {
                    ipAddressRenderer = IpAddressRenderer.simpleRenderer();
                } else {
                    tmp = tmp.trim();
                    try {
                        ipAddressRenderer = IpAddressRenderer.createRendererFor(tmp);
                        logBuilder.appendln("\tIP Address Pattern: Pattern syntax \"{}\" accepted.", tmp);
                    } catch (Exception e) {
                        LOG.debug("", e);
                        logBuilder.appendln("\tIP Address Pattern: Unsupported pattern syntax \"{}\". Using simple renderer.", tmp);
                    }
                }
            }
        }

        {
            final char defaultSep = configuration.getProperty("com.openexchange.mail.defaultSeparator", "/").trim().charAt(0);
            if (defaultSep <= 32) {
                defaultSeparator = '/';
                logBuilder.appendln("\tDefault Separator: Invalid separator (decimal ascii value={}). Setting to fallback: {}", Integer.valueOf(defaultSep), String.valueOf(defaultSeparator));
            } else {
                defaultSeparator = defaultSep;
                logBuilder.appendln("\tDefault Separator: {}", String.valueOf(defaultSeparator));
            }
        }

        {
            final String quoteColors = configuration.getProperty("com.openexchange.mail.quoteLineColors", "#666666").trim();
            if (Pattern.matches("((#[0-9a-fA-F&&[^,]]{6})(?:\r?\n|\\z|\\s*,\\s*))+", quoteColors)) {
                quoteLineColors = Strings.splitByComma(quoteColors);
                logBuilder.appendln("\tHTML Quote Colors: {}", quoteColors);
            } else {
                quoteLineColors = new String[] { "#666666" };
                logBuilder.appendln("\tHTML Quote Colors: Invalid sequence of colors \"{}\". Setting to fallback: #666666{}", quoteColors);
            }
        }

        {
            final String watcherEnabledStr = configuration.getProperty("com.openexchange.mail.watcherEnabled", "false").trim();
            watcherEnabled = Boolean.parseBoolean(watcherEnabledStr);
            logBuilder.appendln("\tWatcher Enabled: {}", Boolean.valueOf(watcherEnabled));
        }

        {
            final String watcherTimeStr = configuration.getProperty("com.openexchange.mail.watcherTime", "60000").trim();
            try {
                watcherTime = Integer.parseInt(watcherTimeStr);
                logBuilder.appendln("\tWatcher Time: {}", Integer.valueOf(watcherTime));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                watcherTime = 60000;
                logBuilder.appendln("\tWatcher Time: Invalid value \"{}\". Setting to fallback: {}", watcherTimeStr, Integer.valueOf(watcherTime));
            }
        }

        {
            final String watcherFeqStr = configuration.getProperty("com.openexchange.mail.watcherFrequency", "10000").trim();
            try {
                watcherFrequency = Integer.parseInt(watcherFeqStr);
                logBuilder.appendln("\tWatcher Frequency: {}", Integer.valueOf(watcherFrequency));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                watcherFrequency = 10000;
                logBuilder.appendln("\tWatcher Frequency: Invalid value \"{}\". Setting to fallback: {}", watcherFeqStr, Integer.valueOf(watcherFrequency));
            }
        }

        {
            final String watcherShallCloseStr = configuration.getProperty("com.openexchange.mail.watcherShallClose", "false").trim();
            watcherShallClose = Boolean.parseBoolean(watcherShallCloseStr);
            logBuilder.appendln("\tWatcher Shall Close: {}", Boolean.valueOf(watcherShallClose));
        }

        {
            final String phishingHdrsStr = configuration.getProperty("com.openexchange.mail.phishingHeader", "").trim();
            if (null != phishingHdrsStr && phishingHdrsStr.length() > 0) {
                phishingHeaders = Strings.splitByComma(phishingHdrsStr);
            } else {
                phishingHeaders = null;
            }
        }

        {
            final String rateLimitPrimaryOnlyStr = configuration.getProperty("com.openexchange.mail.rateLimitPrimaryOnly", "true").trim();
            rateLimitPrimaryOnly = Boolean.parseBoolean(rateLimitPrimaryOnlyStr);
            logBuilder.appendln("\tRate limit primary only: {}", Boolean.valueOf(rateLimitPrimaryOnly));
        }

        {
            final String emailAddressParserWaitMillisStr = configuration.getProperty("com.openexchange.mail.emailAddressParserWaitMillis", "7000").trim();
            try {
                emailAddressParserWaitMillis = Integer.parseInt(emailAddressParserWaitMillisStr);
                logBuilder.appendln("\tEmail Address Parser Wait Millis: {}", Integer.valueOf(emailAddressParserWaitMillis));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                emailAddressParserWaitMillis = 7000;
                logBuilder.appendln("\tEmail Address Parser Wait Millis: Invalid value \"{}\". Setting to fallback {}", emailAddressParserWaitMillisStr, Integer.valueOf(emailAddressParserWaitMillis));
            }
        }

        {
            final String rateLimitStr = configuration.getProperty("com.openexchange.mail.rateLimit", "0").trim();
            try {
                rateLimit = Integer.parseInt(rateLimitStr);
                logBuilder.appendln("\tSent Rate limit: {}", Integer.valueOf(rateLimit));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                rateLimit = 0;
                logBuilder.appendln("\tSend Rate limit: Invalid value \"{}\". Setting to fallback {}", rateLimitStr, Integer.valueOf(rateLimit));
            }
        }

        {
            HostList ranges = HostList.EMPTY;
            String tmp = configuration.getProperty("com.openexchange.mail.rateLimitDisabledRange", "").trim();
            if (Strings.isNotEmpty(tmp)) {
                ranges = HostList.valueOf(tmp);
            }
            this.ranges = ranges;
            logBuilder.appendln("\tWhite-listed from send rate limit: {}", ranges);
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.mail.archive.defaultDays", "90").trim();
            try {
                defaultArchiveDays = Strings.parseInt(tmp);
                logBuilder.appendln("\tDefault archive days: {}", Integer.valueOf(defaultArchiveDays));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                defaultArchiveDays = 90;
                logBuilder.appendln("\tDefault archive days: Invalid value \"{}\". Setting to fallback {}", tmp, Integer.valueOf(defaultArchiveDays));
            }
        }

        {
            final String maxToCcBccStr = configuration.getProperty("com.openexchange.mail.maxToCcBcc", "0").trim();
            try {
                maxToCcBcc = Integer.parseInt(maxToCcBccStr);
                logBuilder.appendln("\tmaxToCcBcc: {}", Integer.valueOf(maxToCcBcc));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                maxToCcBcc = 0;
                logBuilder.appendln("\tmaxToCcBcc: Invalid value \"{}\". Setting to fallback {}", maxToCcBccStr, Integer.valueOf(maxToCcBcc));

            }
        }

        {
            final String maxDriveAttachmentsStr = configuration.getProperty("com.openexchange.mail.maxDriveAttachments", "20").trim();
            try {
                maxDriveAttachments = Integer.parseInt(maxDriveAttachmentsStr);
                logBuilder.appendln("\tmaxDriveAttachments: {}", Integer.valueOf(maxDriveAttachments));
            } catch (NumberFormatException e) {
                LOG.debug("", e);
                maxDriveAttachments = 20;
                logBuilder.appendln("\tmaxDriveAttachments: Invalid value \"{}\". Setting to fallback {}", maxDriveAttachmentsStr, Integer.valueOf(maxDriveAttachments));

            }
        }

        {
            String javaMailPropertiesStr = configuration.getProperty("com.openexchange.mail.JavaMailProperties");
            if (null != javaMailPropertiesStr) {
                javaMailPropertiesStr = javaMailPropertiesStr.trim();
                try {
                    Properties javaMailProps = configuration.getFile(javaMailPropertiesStr);
                    javaMailProperties = javaMailProps;
                    if (javaMailProperties.isEmpty()) {
                        javaMailProperties = null;
                    }
                } catch (Exception e) {
                    LOG.debug("", e);
                    javaMailProperties = null;
                }
            }
            logBuilder.appendln("\tJavaMail Properties loaded: {}", Boolean.valueOf(javaMailProperties != null));
        }

        {
            authProxyDelimiter = configuration.getProperty("com.openexchange.mail.authProxyDelimiter", "").trim();
            if (Strings.isEmpty(authProxyDelimiter)) {
                authProxyDelimiter = null;
            }
        }

        {
            secondaryAuthProxyDelimiter = configuration.getProperty("com.openexchange.mail.secondary.authProxyDelimiter", "").trim();
            if (Strings.isEmpty(secondaryAuthProxyDelimiter)) {
                secondaryAuthProxyDelimiter = null;
            }
        }

        {
            final String supportMsisdnAddressesStr = configuration.getProperty("com.openexchange.mail.supportMsisdnAddresses", "false").trim();
            supportMsisdnAddresses = Boolean.parseBoolean(supportMsisdnAddressesStr);
            logBuilder.appendln("\tSupports MSISDN addresses: {}", Boolean.valueOf(supportMsisdnAddresses));
        }

        {
            this.mailStartTls = configuration.getBoolProperty("com.openexchange.mail.mailStartTls", false);
            this.transportStartTls = configuration.getBoolProperty("com.openexchange.mail.transportStartTls", false);
        }

        {
            this.secondaryMailStartTls = configuration.getBoolProperty("com.openexchange.mail.secondary.mailStartTls", false);
            this.secondaryTransportStartTls = configuration.getBoolProperty("com.openexchange.mail.secondary.transportStartTls", false);
        }

        {
            supportAppleMailFlags = configuration.getBoolProperty("com.openexchange.mail.supportAppleMailFlags", DEFAULT_SUPPORT_APPLE_MAIL_FLAGS);
            logBuilder.appendln("\tSupport for Apple Mail flags: {}", Boolean.valueOf(supportAppleMailFlags));
        }

        {
            orderColorFlagsNaturally = configuration.getBoolProperty("com.openexchange.mail.orderColorFlagsNaturally", DEFAULT_ORDER_COLOR_FLAGS_NATURALLY);
            logBuilder.appendln("\tOrder color flags naturally (by color spectrum): {}", Boolean.valueOf(orderColorFlagsNaturally));
        }

        logBuilder.append("Global mail properties successfully loaded!");
        LOG.info(logBuilder.getMessage(), logBuilder.getArgumentsAsArray());
    }

    /**
     * Reads the properties from specified property file and returns an appropriate instance of {@link Properties}
     *
     * @param propFile The property file
     * @return The appropriate instance of {@link Properties}
     * @throws OXException If reading property file fails
     */
    protected static Properties readPropertiesFromFile(String propFile) throws OXException {
        final Properties properties = new Properties();
        final FileInputStream fis;
        try {
            fis = new FileInputStream(new File(propFile));
        } catch (FileNotFoundException e) {
            throw MailConfigException.create(
                new StringBuilder(256).append("Properties not found at location: ").append(propFile).toString(),
                e);
        }
        try {
            properties.load(fis);
            return properties;
        } catch (IOException e) {
            throw MailConfigException.create(
                new StringBuilder(256).append("I/O error while reading properties from file \"").append(propFile).append(
                    "\": ").append(e.getMessage()).toString(),
                e);
        } finally {
            Streams.close(fis);
        }
    }

    /**
     * Reads the properties from specified property file and returns an appropriate instance of {@link Properties}
     *
     * @param in The property stream
     * @return The appropriate instance of {@link Properties}
     * @throws OXException If reading property file fails
     */
    protected static Properties readPropertiesFromFile(InputStream in) throws OXException {
        try {
            return ConfigurationServices.loadPropertiesFrom(in);
        } catch (IOException e) {
            throw MailConfigException.create(new StringBuilder(256).append("I/O error: ").append(e.getMessage()).toString(), e);
        } finally {
            Streams.close(in);
        }
    }

    @Override
    public boolean hideInlineImages() {
        return hideInlineImages;
    }

    @Override
    public boolean isAllowNestedDefaultFolderOnAltNamespace() {
        return allowNestedDefaultFolderOnAltNamespace;
    }

    /**
     * Gets the max. allowed size (in bytes) for body for being displayed.
     *
     * @return The max. allowed size (in bytes) for body for being displayed
     */
    public int getBodyDisplaySize() {
        return bodyDisplaySize;
    }

    @Override
    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Gets the default MIME charset.
     *
     * @return The default MIME charset
     */
    public String getDefaultMimeCharset() {
        return defaultMimeCharset;
    }

    /**
     * Gets the default mail provider.
     *
     * @return The default mail provider
     */
    public String getDefaultMailProvider() {
        return defaultMailProvider;
    }

    /**
     * Indicates if admin mail login is enabled; meaning whether admin user's try to login to mail system is permitted or not.
     *
     * @return <code>true</code> if admin mail login is enabled; otherwise <code>false</code>
     */
    public boolean isAdminMailLoginEnabled() {
        return adminMailLoginEnabled;
    }

    /**
     * Gets the default separator character for specified user.
     *
     * @return The default separator character
     */
    public char getDefaultSeparator() {
        return defaultSeparator;
    }

    @Override
    public boolean isIgnoreSubscription() {
        return ignoreSubscription;
    }

    /**
     * Signals whether a mail's sent date (<code>"Date"</code> header) is preferred over its received date when serving the special {@link MailListField#DATE} field.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> to prefer sent date; otherwise <code>false</code> for received date
     */
    public boolean isPreferSentDate(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.preferSentDate;
        } catch (Exception e) {
            LOG.error("Failed to get whether a mail's sent date (<code>\"Date\"</code> header) is preferred over its received date for user {} in context {}. Using default {} instead.", I(userId), I(contextId), Boolean.valueOf(preferSentDate), e);
            return preferSentDate;
        }
    }

    /**
     * Signals whether standard folder names are supposed to be translated.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> to translate; otherwise <code>false</code>
     */
    public boolean isTranslateDefaultFolders(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.translateDefaultFolders;
        } catch (Exception e) {
            LOG.error("Failed to get whether standard folder names are supposed to be translated for user {} in context {}. Using default {} instead.", I(userId), I(contextId), Boolean.valueOf(true), e);
            return true;
        }
    }

    /**
     * Signals whether Draft messages are supposed to be deleted when sent out.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> to delete; otherwise <code>false</code>
     */
    public boolean isDeleteDraftOnTransport(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.deleteDraftOnTransport;
        } catch (Exception e) {
            LOG.error("Failed to get whether Draft messages are supposed to be deleted for user {} in context {}. Using default {} instead.", I(userId), I(contextId), Boolean.valueOf(false), e);
            return false;
        }
    }

    /**
     * Signals whether to forward messages unquoted.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> to forward messages unquoted; otherwise <code>false</code>
     */
    public boolean isForwardUnquoted(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.forwardUnquoted;
        } catch (Exception e) {
            LOG.error("Failed to get whether to forward messages unquoted for user {} in context {}. Using default {} instead.", I(userId), I(contextId), Boolean.valueOf(false), e);
            return false;
        }
    }

    /**
     * Gets max. mail size allowed being transported
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The max. mail size allowed being transported
     */
    public long getMaxMailSize(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.maxMailSize;
        } catch (Exception e) {
            LOG.error("Failed to get max. mail size for user {} in context {}. Using default {} instead.", I(userId), I(contextId), "-1", e);
            return -1L;
        }
    }

    /**
     * Gets max. number of message attachments that are allowed to be forwarded as attachment.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The max. number of message attachments that are allowed to be forwarded as attachment
     */
    public int getMaxForwardCount(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.maxForwardCount;
        } catch (Exception e) {
            LOG.error("Failed to get max. forward count for user {} in context {}. Using default {} instead.", I(userId), I(contextId), "8", e);
            return 8;
        }
    }

    public boolean isHidePOP3StorageFolders(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.hidePOP3StorageFolders;
        } catch (Exception e) {
            LOG.error("Failed to get hide-POP3-storage-folders flag for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return hidePOP3StorageFolders;
        }
    }

    /**
     * Gets the max. length for the special <code>"References"</code> header.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The max. length or <code>0</code> (zero) if there is no limit
     */
    public int getMaxLengthForReferencesHeader(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.maxLengthForReferencesHeader;
        } catch (Exception e) {
            LOG.error("Failed to get max. length for \"References\" header for user {} in context {}. Using default {} instead.", I(userId), I(contextId), "998", e);
            return 998;
        }
    }

    @Override
    public boolean isSupportSubscription() {
        return supportSubscription;
    }

    /**
     * Checks if client's IP address should be added to mail headers on delivery as custom header <code>"X-Originating-IP"</code>.
     *
     * @return <code>true</code> if client's IP address should be added otherwise <code>false</code>
     */
    public boolean isAddClientIPAddress() {
        return addClientIPAddress;
    }

    /**
     * Checks whether the version string is supposed to be appended to <code>"X-Mailer"</code> header.
     *
     * @return <code>true</code> to append version string to <code>"X-Mailer"</code> header; otherwise <code>false</code>
     */
    public boolean isAppendVersionToMailerHeader() {
        return appendVersionToMailerHeader;
    }

    /**
     * Gets the IP address renderer
     * <p>
     * <i>Note</i>: Returns <code>null</code> if {@link #isAddClientIPAddress()} signals <code>false</code>
     *
     * @return The renderer instance
     */
    public IpAddressRenderer getIpAddressRenderer() {
        return ipAddressRenderer;
    }

    /**
     * Gets the JavaMail properties.
     *
     * @return The JavaMail properties
     */
    public Properties getJavaMailProperties() {
        return javaMailProperties;
    }

    /**
     * Gets the login source for specified user to be considered for primary mail account.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The login source
     */
    public LoginSource getLoginSource(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.loginSource;
        } catch (Exception e) {
            LOG.error("Failed to get login source for user {} in context {}. Using default {} instead.", I(userId), I(contextId), loginSource, e);
            return loginSource;
        }
    }

    /**
     * Gets the login source for specified user to be considered for secondary mail account.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The login source
     */
    public LoginSource getSecondaryLoginSource(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryLoginSource;
        } catch (Exception e) {
            LOG.error("Failed to get secondary login source for user {} in context {}. Using default {} instead.", I(userId), I(contextId), secondaryLoginSource, e);
            return secondaryLoginSource;
        }
    }

    /**
     * Gets the password source for specified user to be considered for primary mail account.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The password source
     */
    public PasswordSource getPasswordSource(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.passwordSource;
        } catch (Exception e) {
            LOG.error("Failed to get password source for user {} in context {}. Using default {} instead.", I(userId), I(contextId), passwordSource, e);
            return passwordSource;
        }
    }

    /**
     * Gets the password source for specified user to be considered for secondary mail account.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The password source
     */
    public PasswordSource getSecondaryPasswordSource(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryPasswordSource;
        } catch (Exception e) {
            LOG.error("Failed to get secondary password source for user {} in context {}. Using default {} instead.", I(userId), I(contextId), secondaryPasswordSource, e);
            return secondaryPasswordSource;
        }
    }

    /**
     * Gets the mail server source for specified user to be considered for primary mail account..
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param isGuest If this is a guest account
     * @return The mail server source
     */
    public ServerSource getMailServerSource(int userId, int contextId, boolean isGuest) {
        try {
            if (isGuest) {
                return ServerSource.USER;   // For moment, all guests are going to return as user to avoid conflict with Guard
            }
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.mailServerSource;
        } catch (Exception e) {
            LOG.error("Failed to get mail server source for user {} in context {}. Using default {} instead.", I(userId), I(contextId), mailServerSource, e);
            return mailServerSource;
        }
    }

    /**
     * Gets the mail server source for specified user to be considered for secondary mail account.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param isGuest If this is a guest account
     * @return The mail server source
     */
    public ServerSource getSecondaryMailServerSource(int userId, int contextId, boolean isGuest) {
        try {
            if (isGuest) {
                return ServerSource.USER;   // For moment, all guests are going to return as user to avoid conflict with Guard
            }
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryMailServerSource;
        } catch (Exception e) {
            LOG.error("Failed to get secondary mail server source for user {} in context {}. Using default {} instead.", I(userId), I(contextId), secondaryMailServerSource, e);
            return secondaryMailServerSource;
        }
    }

    /**
     * Gets the transport server source for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param isGuest If this is a guest account
     * @return The transport server source
     */
    public ServerSource getTransportServerSource(int userId, int contextId, boolean isGuest) {
        try {
            if (isGuest) {
                return ServerSource.USER;   // For moment, all guests are going to return as user to avoid conflict with Guard
            }
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.transportServerSource;
        } catch (Exception e) {
            LOG.error("Failed to get transport server source for user {} in context {}. Using default {} instead.", I(userId), I(contextId), transportServerSource, e);
            return transportServerSource;
        }
    }

    /**
     * Gets the transport server source for specified user to be considered for secondary mail account.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param isGuest If this is a guest account
     * @return The transport server source
     */
    public ServerSource getSecondaryTransportServerSource(int userId, int contextId, boolean isGuest) {
        try {
            if (isGuest) {
                return ServerSource.USER;   // For moment, all guests are going to return as user to avoid conflict with Guard
            }
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryTransportServerSource;
        } catch (Exception e) {
            LOG.error("Failed to get secondary transport server source for user {} in context {}. Using default {} instead.", I(userId), I(contextId), secondaryTransportServerSource, e);
            return secondaryTransportServerSource;
        }
    }

    /**
     * Gets the global mail server for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The global mail server
     */
    public ConfiguredServer getMailServer(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.mailServer;
        } catch (Exception e) {
            LOG.error("Failed to get mail server for user {} in context {}. Using default {} instead.", I(userId), I(contextId), mailServer, e);
            return mailServer;
        }
    }

    /**
     * Gets the global secondary mail server for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The global secondary mail server
     */
    public ConfiguredServer getSecondaryMailServer(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryMailServer;
        } catch (Exception e) {
            LOG.error("Failed to get secondary mail server for user {} in context {}. Using default {} instead.", I(userId), I(contextId), secondaryMailServer, e);
            return secondaryMailServer;
        }
    }

    /**
     * Gets the global transport server for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The global transport server
     */
    public ConfiguredServer getTransportServer(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.transportServer;
        } catch (Exception e) {
            LOG.error("Failed to get transport server for user {} in context {}. Using default {} instead.", I(userId), I(contextId), transportServer, e);
            return transportServer;
        }
    }

    /**
     * Gets the global secondary transport server for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The global secondary transport server
     */
    public ConfiguredServer getSecondaryTransportServer(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryTransportServer;
        } catch (Exception e) {
            LOG.error("Failed to get secondary transport server for user {} in context {}. Using default {} instead.", I(userId), I(contextId), secondaryTransportServer, e);
            return secondaryTransportServer;
        }
    }

    /**
     * Checks whether STARTTLS is required for mail access for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> if STARTTLS is required; otherwise <code>false</code>
     */
    public boolean isMailStartTls(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.mailStartTls;
        } catch (Exception e) {
            LOG.error("Failed to get STARTTLS flag for mail access for user {} in context {}. Using default {} instead.", I(userId), I(contextId), Boolean.valueOf(mailStartTls), e);
            return mailStartTls;
        }
    }

    /**
     * Checks whether STARTTLS is required for mail access for specified user against secondary mail accounts.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> if STARTTLS is required; otherwise <code>false</code>
     */
    public boolean isSecondaryMailStartTls(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryMailStartTls;
        } catch (Exception e) {
            LOG.error("Failed to get STARTTLS flag for secondary mail access for user {} in context {}. Using default {} instead.", I(userId), I(contextId), Boolean.valueOf(secondaryMailStartTls), e);
            return secondaryMailStartTls;
        }
    }

    /**
     * Checks whether STARTTLS is required for mail transport for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> if STARTTLS is required; otherwise <code>false</code>
     */
    public boolean isTransportStartTls(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.transportStartTls;
        } catch (Exception e) {
            LOG.error("Failed to get STARTTLS flag for mail transport for user {} in context {}. Using default {} instead.", I(userId), I(contextId), Boolean.valueOf(transportStartTls), e);
            return transportStartTls;
        }
    }

    /**
     * Checks whether STARTTLS is required for mail transport for specified user against secondary transport accounts.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> if STARTTLS is required; otherwise <code>false</code>
     */
    public boolean isSecondaryTransportStartTls(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryTransportStartTls;
        } catch (Exception e) {
            LOG.error("Failed to get STARTTLS flag for secondary mail transport for user {} in context {}. Using default {} instead.", I(userId), I(contextId), Boolean.valueOf(secondaryTransportStartTls), e);
            return secondaryTransportStartTls;
        }
    }

    /**
     * Gets the master password for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The master password
     */
    public String getMasterPassword(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.masterPassword;
        } catch (Exception e) {
            LOG.error("Failed to get master password for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return masterPassword;
        }
    }

    /**
     * Gets the secondary master password for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The master password
     */
    public String getSecondaryMasterPassword(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.secondaryMasterPassword;
        } catch (Exception e) {
            LOG.error("Failed to get secondary master password for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return secondaryMasterPassword;
        }
    }

    /**
     * Gets the max. number of recipient addresses that can be specified for given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The max. number of recipient addresses
     */
    public int getMaxToCcBcc(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.maxToCcBcc;
        } catch (Exception e) {
            LOG.error("Failed to get max. number of recipient addresses for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return maxToCcBcc;
        }
    }

    /**
     * Gets the max. number of Drive attachments that can be attached to a mail
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The max. number of Drive attachments
     */
    public int getMaxDriveAttachments(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.maxDriveAttachments;
        } catch (Exception e) {
            LOG.error("Failed to get max. number of Drive attachments for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return maxDriveAttachments;
        }
    }

    /**
     * Gets the quote line colors.
     *
     * @return The quote line colors
     */
    public String[] getQuoteLineColors() {
        return quoteLineColors;
    }

    /**
     * Gets the phishing headers.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The phishing headers or <code>null</code> if none defined
     */
    public String[] getPhishingHeaders(int userId, int contextId) {
        String[] phishingHeaders;
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            phishingHeaders = primaryMailProps.phishingHeaders;
        } catch (Exception e) {
            LOG.error("Failed to get phishing headers for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            phishingHeaders = this.phishingHeaders;
        }

        if (null == phishingHeaders) {
            return null;
        }
        final String[] retval = new String[phishingHeaders.length];
        System.arraycopy(phishingHeaders, 0, retval, 0, phishingHeaders.length);
        return retval;
    }

    /**
     * Gets the send mail rate limit (how many mails can be sent in
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The send rate limit
     */
    public int getRateLimit(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.rateLimit;
        } catch (Exception e) {
            LOG.error("Failed to get send rate limit for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return rateLimit;
        }
    }

    /**
     * Gets the setting if the rate limit should only affect the primary account or all accounts
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The flag
     */
    public boolean getRateLimitPrimaryOnly(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.rateLimitPrimaryOnly;
        } catch (Exception e) {
            LOG.error("Failed to get rateLimitPrimaryOnly flag for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return rateLimitPrimaryOnly;
        }
    }

    /**
     * Gets the IP ranges for which a rate limit must not be applied
     *
     * @return The IP ranges
     */
    public HostList getDisabledRateLimitRanges(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.ranges;
        } catch (Exception e) {
            LOG.error("Failed to get IP ranges for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return ranges;
        }
    }

    /**
     * Gets the max. number of milliseconds waiting for parsing of email addresses using <code>org.hazlewood.connor.bottema.emailaddress.EmailAddressParser</code>.
     *
     * @return The wait time in milliseconds
     */
    public int getEmailAddressParserWaitMillis() {
        return emailAddressParserWaitMillis;
    }

    /**
     * Gets the proxy authentication delimiter for authenticating against primary account.
     * <p>
     * <b>Note</b>: Applies only to primary mail account
     *
     * @return The proxy authentication delimiter or <code>null</code> if not set
     */
    public final String getAuthProxyDelimiter() {
        return authProxyDelimiter;
    }

    /**
     * Sets the authProxyDelimiter
     *
     * @param authProxyDelimiter The authProxyDelimiter to set
     */
    public void setAuthProxyDelimiter(String authProxyDelimiter) {
        this.authProxyDelimiter = authProxyDelimiter;
    }

    /**
     * Gets the proxy authentication delimiter for authenticating against secondary accounts.
     * <p>
     * <b>Note</b>: Applies only to secondary mail accounts
     *
     * @return The proxy authentication delimiter or <code>null</code> if not set
     */
    public String getSecondaryAuthProxyDelimiter() {
        return secondaryAuthProxyDelimiter;
    }

    /**
     * Signals if MSISDN addresses are supported or not.
     *
     * @return <code>true</code>, if MSISDN addresses are supported; otherwise <code>false</code>
     */
    public boolean isSupportMsisdnAddresses() {
        return supportMsisdnAddresses;
    }

    /**
     * Checks if Apple Mail flags are supported.
     *
     * @return <code>true</code> if Apple Mail flags are supported; otherwise <code>false</code>
     */
    public boolean isSupportAppleMailFlags() {
        return supportAppleMailFlags;
    }

    /**
     * Whether color flags shall be ordered naturally (by color spectrum) or not.
     *
     * @return <code>true</code> to order color flags naturally (by color spectrum); otherwise <code>false</code>
     */
    public boolean isOrderColorFlagsNaturally() {
        return orderColorFlagsNaturally;
    }

    /**
     * Gets the default days when archiving messages.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The default days
     */
    public int getDefaultArchiveDays(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.defaultArchiveDays;
        } catch (Exception e) {
            LOG.error("Failed to get default days when archiving messages for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return defaultArchiveDays;
        }
    }

    /**
     * Gets the mail fetch limit.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The mail fetch limit
     */
    public int getMailFetchLimit(int userId, int contextId) {
        try {
            PrimaryMailProps primaryMailProps = getPrimaryMailProps(userId, contextId);
            return primaryMailProps.mailFetchLimit;
        } catch (Exception e) {
            LOG.error("Failed to get mail fetch limit for user {} in context {}. Using default instead.", I(userId), I(contextId), e);
            return mailFetchLimit;
        }
    }

    @Override
    public int getMailFetchLimit() {
        return mailFetchLimit;
    }

    @Override
    public boolean isUserFlagsEnabled() {
        return userFlagsEnabled;
    }

    /**
     * Indicates if watcher is enabled.
     *
     * @return <code>true</code> if watcher is enabled; otherwise <code>false</code>
     */
    public boolean isWatcherEnabled() {
        return watcherEnabled;
    }

    /**
     * Gets the watcher frequency.
     *
     * @return The watcher frequency
     */
    public int getWatcherFrequency() {
        return watcherFrequency;
    }

    /**
     * Indicates if watcher is allowed to close exceeded connections.
     *
     * @return <code>true</code> if watcher is allowed to close exceeded connections; otherwise <code>false</code>
     */
    public boolean isWatcherShallClose() {
        return watcherShallClose;
    }

    /**
     * Gets the watcher time.
     *
     * @return The watcher time
     */
    public int getWatcherTime() {
        return watcherTime;
    }

    /**
     * Gets the mail access cache shrinker-interval seconds.
     *
     * @return The mail access cache shrinker-interval seconds
     */
    public int getMailAccessCacheShrinkerSeconds() {
        return mailAccessCacheShrinkerSeconds;
    }

    /**
     * Gets the mail access cache idle seconds.
     *
     * @return The mail access cache idle seconds.
     */
    public int getMailAccessCacheIdleSeconds() {
        return mailAccessCacheIdleSeconds;
    }

}
