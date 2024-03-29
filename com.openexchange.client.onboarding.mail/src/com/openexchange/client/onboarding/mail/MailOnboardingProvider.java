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

package com.openexchange.client.onboarding.mail;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import com.openexchange.client.onboarding.AvailabilityResult;
import com.openexchange.client.onboarding.BuiltInProvider;
import com.openexchange.client.onboarding.Device;
import com.openexchange.client.onboarding.DisplayResult;
import com.openexchange.client.onboarding.OnboardingExceptionCodes;
import com.openexchange.client.onboarding.OnboardingRequest;
import com.openexchange.client.onboarding.OnboardingType;
import com.openexchange.client.onboarding.OnboardingUtility;
import com.openexchange.client.onboarding.Result;
import com.openexchange.client.onboarding.ResultReply;
import com.openexchange.client.onboarding.Scenario;
import com.openexchange.client.onboarding.mail.custom.CustomLoginSource;
import com.openexchange.client.onboarding.plist.OnboardingPlistProvider;
import com.openexchange.client.onboarding.plist.PlistProviderNames;
import com.openexchange.client.onboarding.plist.PlistResult;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.userconfiguration.Permission;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.service.MailService;
import com.openexchange.mail.transport.config.TransportConfig;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.plist.PListDict;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.serverconfig.ServerConfigService;
import com.openexchange.session.Session;
import com.openexchange.session.Sessions;
import com.openexchange.session.UserAndContext;
import com.openexchange.sessiond.SessionMatcher;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.user.User;
import com.openexchange.user.UserService;

/**
 * {@link MailOnboardingProvider}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class MailOnboardingProvider implements OnboardingPlistProvider {

    private static final Logger LOG = Logger.getLogger(MailOnboardingProvider.class.getName());

    private final ServiceListing<CustomLoginSource> customLoginSources;
    private final ServiceLookup services;
    private final String identifier;
    private final Set<Device> supportedDevices;
    private final Set<OnboardingType> supportedTypes;

    /**
     * Initializes a new {@link MailOnboardingProvider}.
     */
    public MailOnboardingProvider(ServiceListing<CustomLoginSource> customLoginSources, ServiceLookup services) {
        super();
        this.customLoginSources = customLoginSources;
        this.services = services;
        identifier = BuiltInProvider.MAIL.getId();
        supportedDevices = EnumSet.allOf(Device.class);
        supportedTypes = EnumSet.of(OnboardingType.PLIST, OnboardingType.MANUAL);
    }

    private CustomLoginSource getHighestRankedCustomLoginSource() {
        List<CustomLoginSource> loginSources = customLoginSources.getServiceList();
        return loginSources.isEmpty() ? null : loginSources.get(0);
    }

    @Override
    public String getDescription() {
        return "Configures IMAP/SMTP.";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public AvailabilityResult isAvailable(Session session) throws OXException {
        if (!OnboardingUtility.hasCapability(Permission.WEBMAIL.getCapabilityName(), session)) {
            return new AvailabilityResult(false, Permission.WEBMAIL.getCapabilityName());
        }

        MailAccountStorageService service = services.getService(MailAccountStorageService.class);
        if (service==null){
            throw ServiceExceptionCode.absentService(MailAccountStorageService.class);
        }
        MailAccount mailAccount = service.getDefaultMailAccount(session.getUserId(), session.getContextId());
        boolean available = (mailAccount.getMailProtocol().startsWith("imap") && mailAccount.getTransportProtocol().startsWith("smtp"));
        return new AvailabilityResult(available);
    }

    @Override
    public AvailabilityResult isAvailable(int userId, int contextId) throws OXException {
        if (!OnboardingUtility.hasCapability(Permission.WEBMAIL.getCapabilityName(), userId, contextId)) {
            return new AvailabilityResult(false, Permission.WEBMAIL.getCapabilityName());
        }

        MailAccountStorageService service = services.getService(MailAccountStorageService.class);
        if (service==null){
            throw ServiceExceptionCode.absentService(MailAccountStorageService.class);
        }
        MailAccount mailAccount = service.getDefaultMailAccount(userId, contextId);
        boolean available = (mailAccount.getMailProtocol().startsWith("imap") && mailAccount.getTransportProtocol().startsWith("smtp"));
        return new AvailabilityResult(available);
    }

    @Override
    public String getId() {
        return identifier;
    }

    @Override
    public Set<OnboardingType> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public Set<Device> getSupportedDevices() {
        return Collections.unmodifiableSet(supportedDevices);
    }

    @Override
    public Result execute(OnboardingRequest request, Result previousResult, Session session) throws OXException {
        Device device = request.getDevice();
        if (!supportedDevices.contains(device)) {
            throw OnboardingExceptionCodes.UNSUPPORTED_DEVICE.create(identifier, device.getId());
        }

        Scenario scenario = request.getScenario();
        if (!Device.getActionsFor(request.getClientDevice(), device, scenario.getType(), session).contains(request.getAction())) {
            throw OnboardingExceptionCodes.UNSUPPORTED_ACTION.create(request.getAction().getId());
        }

        switch (scenario.getType()) {
            case LINK:
                throw OnboardingExceptionCodes.UNSUPPORTED_TYPE.create(identifier, scenario.getType().getId());
            case MANUAL:
                return doExecuteManual(previousResult, session);
            case PLIST:
                return doExecutePlist(request, previousResult, session);
            default:
                throw OnboardingExceptionCodes.UNSUPPORTED_TYPE.create(identifier, scenario.getType().getId());
        }
    }

    private Configurations getEffectiveConfigurations(UserAndContext userAndContext, Session optSession, MailSettings optMailSettings) throws OXException {
        int userId = userAndContext.getUserId();
        int contextId = userAndContext.getContextId();
        MailAccount account = null;

        // Determine IMAP settings
        MailSettings mailSettings = optMailSettings;
        if (mailSettings == null) {
            if (optSession == null) {
                MailAccountStorageService service = services.getService(MailAccountStorageService.class);
                if (service == null){
                    throw ServiceExceptionCode.absentService(MailAccountStorageService.class);
                }
                account = service.getDefaultMailAccount(userId, contextId);
                mailSettings = new MailAccountMailSettings(account);
            } else {
                MailConfig mailConfig = services.getService(MailService.class).getMailConfig(optSession, Account.DEFAULT_ID);
                mailSettings = new MailConfigMailSettings(mailConfig);
            }
        }

        // IMAP
        Configuration imapConfiguration;
        {
            Configuration.Builder configBuilder = Configuration.builder();

            // Host
            {
                String imapServer = OnboardingUtility.getValueFromProperty("com.openexchange.client.onboarding.mail.imap.host", null, userId, contextId);
                if (null == imapServer) {
                    imapServer = mailSettings.getServer();
                }
                configBuilder.withHost(imapServer);
            }

            // Port
            {
                Integer imapPort = OnboardingUtility.getIntFromProperty("com.openexchange.client.onboarding.mail.imap.port", null, userId, contextId);
                if (null == imapPort) {
                    imapPort = Integer.valueOf(mailSettings.getPort());
                }
                configBuilder.withPort(imapPort.intValue());
            }

            // Secure (SSL/TLS)
            {
                Boolean imapSecure = OnboardingUtility.getBoolFromProperty("com.openexchange.client.onboarding.mail.imap.secure", null, userId, contextId);
                if (null == imapSecure) {
                    imapSecure = Boolean.valueOf(mailSettings.isSecure());
                }
                configBuilder.withSecure(imapSecure.booleanValue());
            }

            // STARTTLS required
            {
                Boolean imapRequireTls = OnboardingUtility.getBoolFromProperty("com.openexchange.client.onboarding.mail.imap.requireTls", null, userId, contextId);
                if (null == imapRequireTls) {
                    imapRequireTls = Boolean.valueOf(mailSettings.isRequireTls());
                }
                configBuilder.withRequireTls(imapRequireTls.booleanValue());
            }

            // Login
            {
                String imapLogin;
                Boolean customSource = OnboardingUtility.getBoolFromProperty("com.openexchange.client.onboarding.mail.imap.login.customsource", Boolean.FALSE, userId, contextId);
                if (customSource.booleanValue()) {
                    CustomLoginSource customLoginSource = getHighestRankedCustomLoginSource();
                    if (customLoginSource == null) {
                        LOG.warning("Unable to find any CustomLoginSource services! Falling back to imap config.");
                        imapLogin = mailSettings.getLogin();
                    } else {
                        imapLogin = customLoginSource.getImapLogin(optSession, userId, contextId);
                    }
                } else {
                    imapLogin = mailSettings.getLogin();
                }
                configBuilder.withLogin(imapLogin);
            }

            // Password
            {
                String imapPassword = mailSettings.getPassword();
                configBuilder.withPassword(imapPassword);
            }
            imapConfiguration = configBuilder.build();
        }

        // Determine SMTP settings
        TransportSettings transportSettings;
        if (optSession == null) {
            if (account == null) {
                MailAccountStorageService service = services.getService(MailAccountStorageService.class);
                if (service == null){
                    throw ServiceExceptionCode.absentService(MailAccountStorageService.class);
                }
                account = service.getDefaultMailAccount(userId, contextId);
            }
            transportSettings = new TransportAccountTransportSettings(account, services.getServiceSafe(ConfigurationService.class));
        } else {
            TransportConfig smtpConfig = services.getServiceSafe(MailService.class).getTransportConfig(optSession, Account.DEFAULT_ID);
            TransportConfig.getTransportConfig(smtpConfig, optSession, Account.DEFAULT_ID);
            transportSettings = new TransportConfigTransportSettings(smtpConfig);
        }

        // SMTP
        Configuration smtpConfiguration;
        {
            Configuration.Builder configBuilder = Configuration.builder();

            // Host
            {
                String smtpServer = OnboardingUtility.getValueFromProperty("com.openexchange.client.onboarding.mail.smtp.host", null, userId, contextId);
                if (null == smtpServer) {
                    smtpServer = transportSettings.getServer();
                }
                configBuilder.withHost(smtpServer);
            }

            // Port
            {
                Integer smtpPort = OnboardingUtility.getIntFromProperty("com.openexchange.client.onboarding.mail.smtp.port", null, userId, contextId);
                if (null == smtpPort) {
                    smtpPort = Integer.valueOf(transportSettings.getPort());
                }
                configBuilder.withPort(smtpPort.intValue());
            }

            // Secure (SSL/TLS)
            {
                Boolean smtpSecure = OnboardingUtility.getBoolFromProperty("com.openexchange.client.onboarding.mail.smtp.secure", null, userId, contextId);
                if (null == smtpSecure) {
                    smtpSecure = Boolean.valueOf(transportSettings.isSecure());
                }
                configBuilder.withSecure(smtpSecure.booleanValue());
            }

            // STARTTLS required
            {
                Boolean smtpRequireTls = OnboardingUtility.getBoolFromProperty("com.openexchange.client.onboarding.mail.smtp.requireTls", null, userId, contextId);
                if (null == smtpRequireTls) {
                    smtpRequireTls = Boolean.valueOf(transportSettings.isRequireTls());
                }
                configBuilder.withRequireTls(smtpRequireTls.booleanValue());
            }

            // Password
            {
                String smtpPassword = transportSettings.getPassword();
                configBuilder.withPassword(smtpPassword);
            }

            // Login
            {
                String smtpLogin;
                Boolean customSource = OnboardingUtility.getBoolFromProperty("com.openexchange.client.onboarding.mail.smtp.login.customsource", Boolean.FALSE, userId, contextId);
                if (customSource.booleanValue()) {
                    CustomLoginSource customLoginSource = getHighestRankedCustomLoginSource();
                    if (customLoginSource == null) {
                        LOG.warning("Unable to find any CustomLoginSource services! Falling back to smtp config.");
                        smtpLogin = transportSettings.getLogin();
                    } else {
                        smtpLogin = customLoginSource.getSmtpLogin(optSession, userId, contextId);
                    }
                } else {
                    smtpLogin = transportSettings.getLogin();
                    if (Strings.isEmpty(smtpLogin)) {
                        smtpLogin = imapConfiguration.login;
                    }
                }
                configBuilder.withLogin(smtpLogin);
            }

            // Authentication needed?
            {
                if (!transportSettings.needsAuthentication()) {
                    configBuilder.withNeedsAuthentication(false);
                }
            }

            smtpConfiguration = configBuilder.build();
        }

        // Return configurations
        return new Configurations(imapConfiguration, smtpConfiguration);
    }

    private Configurations getEffectiveConfigurations(int userId, int contextId) throws OXException {
        Optional<Session> optionalSession = Sessions.getValidatedSessionForCurrentThread(userId, contextId);
        if (!optionalSession.isPresent()) {
            return getEffectiveConfigurations(UserAndContext.newInstance(userId, contextId), null, null);
        }

        // Check if session is suitable to obtain the MailConfig instance
        Session session = optionalSession.get();
        MailConfig mailConfig = null;
        try {
            mailConfig = services.getService(MailService.class).getMailConfig(session, Account.DEFAULT_ID);
        } catch (OXException e) {
            if (!MailExceptionCode.MISSING_CONNECT_PARAM.equals(e) || session.getPassword() != null) {
                throw e;
            }

            SessiondService sessiondService = services.getOptionalService(SessiondService.class);
            if (null == sessiondService) {
                throw e;
            }

            // Need to find a session with a password; otherwise no MailConfig can be obtained
            SessionMatcher matcher = new SessionMatcher() {

                @Override
                public Set<Flag> flags() {
                    return SessionMatcher.ONLY_SHORT_TERM;
                }

                @Override
                public boolean accepts(Session session) {
                    return session.getPassword() != null;
                }
            };
            Session sessionWithPassword = sessiondService.findFirstMatchingSessionForUser(userId, contextId, matcher);
            if (null == sessionWithPassword) {
                return getEffectiveConfigurations(UserAndContext.newInstance(userId, contextId), null, null);
            }
            session = sessionWithPassword;
        }

        return getEffectiveConfigurations(UserAndContext.newInstance(userId, contextId), session, new MailConfigMailSettings(mailConfig));
    }

    private Result doExecutePlist(OnboardingRequest request, Result previousResult, Session session) throws OXException {
        return plistResult(request, previousResult, session);
    }

    private Result doExecuteManual(Result previousResult, Session session) throws OXException {
        return displayResult(previousResult, session);
    }

    // --------------------------------------------------------------------------------------------------------------------------

    private final static String IMAP_LOGIN_FIELD = "imapLogin";
    private final static String IMAP_SERVER_FIELD = "imapServer";
    private final static String IMAP_PORT_FIELD = "imapPort";
    private final static String IMAP_SECURE_FIELD = "imapSecure";
    private final static String SMTP_LOGIN_FIELD = "smtpLogin";
    private final static String SMTP_SERVER_FIELD = "smtpServer";
    private final static String SMTP_PORT_FIELD = "smtpPort";
    private final static String SMTP_SECURE_FIELD = "smtpSecure";

    private Result displayResult(Result previousResult, Session session) throws OXException {
        Configurations configurations;
        {
            MailConfig mailConfig = services.getService(MailService.class).getMailConfig(session, Account.DEFAULT_ID);
            MailSettings mailSettings = new MailConfigMailSettings(mailConfig);
            configurations = getEffectiveConfigurations(UserAndContext.newInstance(session), session, mailSettings);
        }

        Map<String, Object> configuration = null == previousResult ? new HashMap<String, Object>(8) : ((DisplayResult) previousResult).getConfiguration();

        configuration.put(IMAP_LOGIN_FIELD, configurations.imapConfig.login);
        configuration.put(IMAP_SERVER_FIELD, configurations.imapConfig.host);
        configuration.put(IMAP_PORT_FIELD, Integer.valueOf(configurations.imapConfig.port));
        configuration.put(IMAP_SECURE_FIELD, Boolean.valueOf(configurations.imapConfig.secure));

        boolean needsAuthentication = configurations.smtpConfig.needsAuthentication;
        if (needsAuthentication) {
            configuration.put(SMTP_LOGIN_FIELD, configurations.smtpConfig.login);
        } else {
            //String none = StringHelper.valueOf(getUser(session).getLocale()).getString("None");
            //configuration.put(SMTP_LOGIN_FIELD, none);
            configuration.put(SMTP_LOGIN_FIELD, configurations.smtpConfig.login);
        }
        configuration.put(SMTP_SERVER_FIELD, configurations.smtpConfig.host);
        configuration.put(SMTP_PORT_FIELD, Integer.valueOf(configurations.smtpConfig.port));
        configuration.put(SMTP_SECURE_FIELD, Boolean.valueOf(configurations.smtpConfig.secure));

        return new DisplayResult(configuration, ResultReply.NEUTRAL);
    }



    // --------------------------------------------- PLIST utils --------------------------------------------------------------

    private UserSettingMail getUserSettingMail(int userId, int contextId) throws OXException {

        return UserSettingMailStorage.getInstance().getUserSettingMail(userId, contextId);
    }

    private User getUser(int userId, int contextId) throws OXException {
        UserService service = services.getService(UserService.class);
        if (service == null) {
            throw OnboardingExceptionCodes.UNEXPECTED_ERROR.create("UserService not available");
        }
        return service.getUser(userId, contextId);
    }

    private Result plistResult(OnboardingRequest request, Result previousResult, Session session) throws OXException {
        PListDict previousPListDict = previousResult == null ? null : ((PlistResult) previousResult).getPListDict();
        PListDict pListDict = getPlist(previousPListDict, request.getScenario(), request.getHostData().getHost(), session.getUserId(), session.getContextId());
        return new PlistResult(pListDict, ResultReply.NEUTRAL);
    }

    @Override
    public PListDict getPlist(PListDict optPrevPListDict, Scenario scenario, String hostName, int userId, int contextId) throws OXException {
        Configurations configurations = getEffectiveConfigurations(userId, contextId);

        String defaultName = StringHelper.valueOf(OnboardingUtility.getLocaleFor(userId, contextId)).getString(PlistProviderNames.PROFILE_NAME_MAIL);
        String scenarioDisplayName = getPListPayloadName(defaultName, services.getOptionalService(ServerConfigService.class), hostName, userId, contextId);

        // Get the PListDict to contribute to
        PListDict pListDict;
        if (null == optPrevPListDict) {
            pListDict = new PListDict();
            pListDict.setPayloadIdentifier(OnboardingUtility.reverseDomainNameString(hostName) + "." + scenario.getId() + "." + OnboardingUtility.dropWhitespacesAndLowerCaseFor(configurations.imapConfig.login));
            pListDict.setPayloadType("Configuration");
            pListDict.setPayloadUUID(OnboardingUtility.craftScenarioUUIDFrom(scenario.getId(), userId, contextId).toString());
            pListDict.setPayloadVersion(1);
            pListDict.setPayloadDisplayName(scenarioDisplayName);
        } else {
            pListDict = optPrevPListDict;
        }

        // Generate content
        PListDict payloadContent = new PListDict();
        payloadContent.setPayloadType("com.apple.mail.managed");
        payloadContent.setPayloadUUID(OnboardingUtility.craftProviderUUIDFrom(identifier, userId, contextId).toString());
        payloadContent.setPayloadIdentifier(OnboardingUtility.reverseDomainNameString(hostName) + ".mail");
        payloadContent.setPayloadVersion(1);

        // A user-visible description of the email account, shown in the Mail and Settings applications.
        payloadContent.addStringValue("EmailAccountDescription", OnboardingUtility.getProductName(hostName, userId, contextId) + " Mail");

        // The full user name for the account. This is the user name in sent messages, etc.
        payloadContent.addStringValue("EmailAccountName", getUser(userId, contextId).getDisplayName());

        // Allowed values are EmailTypePOP and EmailTypeIMAP. Defines the protocol to be used for that account.
        payloadContent.addStringValue("EmailAccountType", "EmailTypeIMAP");

        // Designates the full email address for the account. If not present in the payload, the device prompts for this string during profile installation.
        payloadContent.addStringValue("EmailAddress", getUserSettingMail(userId, contextId).getSendAddr());

        // -------------------------------------------------- IncomingMailServer --------------------------------------------------------

        // Designates the authentication scheme for incoming mail. Allowed values are EmailAuthPassword and EmailAuthNone.
        payloadContent.addStringValue("IncomingMailServerAuthentication", "EmailAuthPassword");

        // Designates the incoming mail server host name (or IP address).
        payloadContent.addStringValue("IncomingMailServerHostName", configurations.imapConfig.host);

        // Designates the incoming mail server port number. If no port number is specified, the default port for a given protocol is used.
        payloadContent.addIntegerValue("IncomingMailServerPortNumber", configurations.imapConfig.port);

        // Designates whether the incoming mail server uses SSL for authentication. Default false.
        payloadContent.addBooleanValue("IncomingMailServerUseSSL", configurations.imapConfig.secure || configurations.imapConfig.requireTls);

        // Designates the user name for the email account, usually the same as the email address up to the @ character.
        // If not present in the payload, and the account is set up to require authentication for incoming email, the device will prompt for this string during profile installation.
        payloadContent.addStringValue("IncomingMailServerUsername", configurations.imapConfig.login);

        // -------------------------------------------------- OutgoingMailServer --------------------------------------------------------

        // Designates the authentication scheme for outgoing mail. Allowed values are EmailAuthPassword and EmailAuthNone.
        boolean needsAuthentication = configurations.smtpConfig.needsAuthentication;
        payloadContent.addStringValue("OutgoingMailServerAuthentication", needsAuthentication ? "EmailAuthPassword" : "EmailAuthNone");

        // Designates the outgoing mail server host name (or IP address).
        payloadContent.addStringValue("OutgoingMailServerHostName", configurations.smtpConfig.host);

        // Designates the outgoing mail server port number. If no port number is specified, ports 25, 587 and 465 are used, in this order.
        payloadContent.addIntegerValue("OutgoingMailServerPortNumber", configurations.smtpConfig.port);

        // Designates whether the outgoing mail server uses SSL for authentication. Default false.
        payloadContent.addBooleanValue("OutgoingMailServerUseSSL", configurations.smtpConfig.secure || configurations.smtpConfig.requireTls);

        // Designates the user name for the email account, usually the same as the email address up to the @ character.
        // If not present in the payload, and the account is set up to require authentication for outgoing email, the device prompts for this string during profile installation.
        payloadContent.addStringValue("OutgoingMailServerUsername", configurations.smtpConfig.login);

        if (needsAuthentication) {
            if (Strings.isNotEmpty(configurations.smtpConfig.password) && Strings.isNotEmpty(configurations.imapConfig.password) && configurations.smtpConfig.password.equals(configurations.imapConfig.password)) {
                payloadContent.addBooleanValue("OutgoingPasswordSameAsIncomingPassword", true);
            }
        } else {
            payloadContent.addBooleanValue("OutgoingPasswordSameAsIncomingPassword", true);
        }

        // Further options (currently not used)

        // PreventMove - Boolean - Optional. Default false.
        // If true, messages may not be moved out of this email account into another account. Also prevents forwarding or replying from a different account than the message was originated from.
        // Availability: Available only in iOS 5.0 and later.

        // PreventAppSheet - Boolean - Optional. Default false.
        // If true, this account is not available for sending mail in any app other than the Apple Mail app.
        // Availability: Available only in iOS 5.0 and later.

        // SMIMEEnabled - Boolean - Optional. Default false.
        // If true, this account supports S/MIME.
        // Availability: Available only in iOS 5.0 and later.

        // SMIMESigningCertificateUUID - String - Optional.
        // The PayloadUUID of the identity certificate used to sign messages sent from this account.
        // Availability: Available only in iOS 5.0 and later.

        // SMIMEEncryptionCertificateUUID - String - Optional.
        // The PayloadUUID of the identity certificate used to decrypt messages sent to this account.
        // Availability: Available only in iOS 5.0 and later.

        // SMIMEEnablePerMessageSwitch _ Boolean - Optional.
        // If set to true, enable the per-message signing and encryption switch. Defaults to true.
        // Availability: Available only in iOS 8.0 and later.

        // disableMailRecentsSyncing - Boolean - Default false.
        // If true, this account is excluded from address Recents syncing. This defaults to false.
        // Availability: Available only in iOS 6.0 and later.

        // allowMailDrop - Boolean - Default false.
        // If true, this account is allowed to use Mail Drop. The default is false.
        // Availability: Available only in iOS 9.0 and later.

        // disableMailDrop - Boolean - Default false.
        // If true, this account is excluded from using Mail Drop. The default is false.

        // Add payload content dictionary to top-level dictionary
        pListDict.addPayloadContent(payloadContent);
        return pListDict;
    }

}
