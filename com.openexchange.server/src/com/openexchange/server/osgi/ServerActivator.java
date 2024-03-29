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

package com.openexchange.server.osgi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.spi.CharsetProvider;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import javax.activation.MailcapCommandMap;
import javax.servlet.ServletException;
import org.json.FileBackedJSONStringProvider;
import org.json.JSONObject;
import org.json.JSONValue;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.admin.rmi.service.OXContextService;
import com.openexchange.ajax.Attachment;
import com.openexchange.ajax.Folder;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.customizer.file.AdditionalFileField;
import com.openexchange.ajax.customizer.file.SanitizedFilename;
import com.openexchange.ajax.customizer.folder.AdditionalFolderField;
import com.openexchange.ajax.customizer.folder.osgi.FolderFieldCollector;
import com.openexchange.ajax.ipcheck.IPCheckService;
import com.openexchange.ajax.requesthandler.AJAXRequestHandler;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.ajax.requesthandler.ResultConverterRegistry;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.auth.Authenticator;
import com.openexchange.auth.mbean.AuthenticatorMBean;
import com.openexchange.auth.mbean.impl.AuthenticatorMBeanImpl;
import com.openexchange.auth.rmi.RemoteAuthenticator;
import com.openexchange.auth.rmi.impl.RemoteAuthenticatorImpl;
import com.openexchange.cache.registry.CacheAvailabilityRegistry;
import com.openexchange.caching.CacheService;
import com.openexchange.caching.events.CacheEventService;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.charset.CustomCharsetProvider;
import com.openexchange.chronos.ical.ICalService;
import com.openexchange.chronos.service.AdministrativeFreeBusyService;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Reloadable;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.configjump.ConfigJumpService;
import com.openexchange.configjump.client.ConfigJump;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.configuration.SystemConfig;
import com.openexchange.contact.vcard.VCardService;
import com.openexchange.contact.vcard.storage.VCardStorageFactory;
import com.openexchange.contactcollector.ContactCollectorService;
import com.openexchange.context.ContextService;
import com.openexchange.conversion.ConversionService;
import com.openexchange.conversion.DataSource;
import com.openexchange.counter.MailCounter;
import com.openexchange.counter.MailIdleCounter;
import com.openexchange.crypto.CryptoService;
import com.openexchange.data.conversion.ical.ICalEmitter;
import com.openexchange.data.conversion.ical.ICalParser;
import com.openexchange.database.AssignmentFactory;
import com.openexchange.database.CreateTableService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.cleanup.DatabaseCleanUpService;
import com.openexchange.database.provider.DBPoolProvider;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.DatabaseServiceDBProvider;
import com.openexchange.databaseold.Database;
import com.openexchange.dataretention.DataRetentionService;
import com.openexchange.deputy.DeputyService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.event.EventFactoryService;
import com.openexchange.event.impl.EventFactoryServiceImpl;
import com.openexchange.event.impl.EventQueue;
import com.openexchange.event.impl.osgi.EventHandlerRegistration;
import com.openexchange.event.impl.osgi.OSGiEventDispatcher;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccountManagerLookupService;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.file.storage.composition.IDBasedFolderAccessFactory;
import com.openexchange.file.storage.parse.FileMetadataParserService;
import com.openexchange.file.storage.registry.FileStorageServiceRegistry;
import com.openexchange.filemanagement.DistributedFileManagement;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.filestore.QuotaFileStorageService;
import com.openexchange.folder.FolderDeleteListenerService;
import com.openexchange.folder.FolderService;
import com.openexchange.folder.internal.FolderDeleteListenerServiceTrackerCustomizer;
import com.openexchange.folder.internal.FolderServiceImpl;
import com.openexchange.folderstorage.FolderI18nNamesService;
import com.openexchange.folderstorage.internal.FolderI18nNamesServiceImpl;
import com.openexchange.folderstorage.osgi.FolderStorageActivator;
import com.openexchange.group.GroupService;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.alias.UserAliasStorage;
import com.openexchange.groupware.alias.impl.AliasCacheDeleteListener;
import com.openexchange.groupware.alias.impl.CachingAliasStorage;
import com.openexchange.groupware.alias.impl.RdbAliasStorage;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.groupware.delete.contextgroup.DeleteContextGroupListener;
import com.openexchange.groupware.impl.id.CreateIDSequenceTable;
import com.openexchange.groupware.infostore.EventFiringInfostoreFacade;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.InfostoreSearchEngine;
import com.openexchange.groupware.infostore.autodelete.InfostoreAutodeleteFileVersionsLoginHandler;
import com.openexchange.groupware.infostore.facade.impl.EventFiringInfostoreFacadeImpl;
import com.openexchange.groupware.infostore.facade.impl.FilteringInfostoreFacadeImpl;
import com.openexchange.groupware.infostore.facade.impl.InfostoreFacadeImpl;
import com.openexchange.groupware.infostore.media.MediaMetadataExtractorService;
import com.openexchange.groupware.notify.hostname.HostnameService;
import com.openexchange.groupware.reminder.ReminderService;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.tree.AvailableFindModules;
import com.openexchange.groupware.settings.tree.JsonMaxSize;
import com.openexchange.groupware.settings.tree.ShardingSubdomains;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.groupware.upgrade.SegmentedUpdateService;
import com.openexchange.groupware.upload.impl.UploadUtility;
import com.openexchange.groupware.userconfiguration.PermissionConfigurationChecker;
import com.openexchange.groupware.userconfiguration.internal.PermissionConfigurationCheckerImpl;
import com.openexchange.groupware.userconfiguration.osgi.CapabilityRegistrationListener;
import com.openexchange.guest.GuestService;
import com.openexchange.html.HtmlService;
import com.openexchange.i18n.I18nService;
import com.openexchange.i18n.I18nServiceRegistry;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.id.IDGeneratorService;
import com.openexchange.imagetransformation.ImageMetadataService;
import com.openexchange.imagetransformation.ImageTransformationService;
import com.openexchange.jslob.ConfigTreeEquivalent;
import com.openexchange.json.FileBackedJSONStringProviderImpl;
import com.openexchange.lock.LockService;
import com.openexchange.lock.impl.LockServiceImpl;
import com.openexchange.log.Slf4jLogger;
import com.openexchange.log.audit.AuditLogService;
import com.openexchange.login.BlockingLoginHandlerService;
import com.openexchange.login.LoginHandlerService;
import com.openexchange.login.internal.LoginNameRecorder;
import com.openexchange.login.listener.AutoLoginAwareLoginListener;
import com.openexchange.login.listener.LoginListener;
import com.openexchange.login.multifactor.MultifactorAutoLoginAwareListener;
import com.openexchange.login.multifactor.MultifactorChecker;
import com.openexchange.mail.MailAuthenticator;
import com.openexchange.mail.MailCounterImpl;
import com.openexchange.mail.MailIdleCounterImpl;
import com.openexchange.mail.MailQuotaProvider;
import com.openexchange.mail.api.AuthenticationFailedHandlerService;
import com.openexchange.mail.api.MailProvider;
import com.openexchange.mail.api.unified.UnifiedViewService;
import com.openexchange.mail.attachment.AttachmentTokenService;
import com.openexchange.mail.authenticity.MailAuthenticityHandlerRegistry;
import com.openexchange.mail.cache.MailAccessCacheEventListener;
import com.openexchange.mail.cache.MailSessionEventHandler;
import com.openexchange.mail.conversion.AttachmentMailPartDataSource;
import com.openexchange.mail.conversion.ICalMailPartDataSource;
import com.openexchange.mail.conversion.VCardMailPartDataSource;
import com.openexchange.mail.json.compose.ComposeHandlerRegistry;
import com.openexchange.mail.json.compose.share.AttachmentStorageRegistry;
import com.openexchange.mail.json.compose.share.internal.EnabledCheckerRegistry;
import com.openexchange.mail.json.compose.share.internal.MessageGeneratorRegistry;
import com.openexchange.mail.json.compose.share.internal.ShareLinkGeneratorRegistry;
import com.openexchange.mail.loginhandler.MailLoginHandler;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.crypto.CryptoMailRecognizer;
import com.openexchange.mail.mime.crypto.CryptoMailRecognizerService;
import com.openexchange.mail.mime.crypto.impl.CompositePGPMailRecognizer;
import com.openexchange.mail.mime.crypto.impl.CryptoMailRecognizerServiceImpl;
import com.openexchange.mail.mime.crypto.impl.PGPInlineMailRecognizer;
import com.openexchange.mail.mime.crypto.impl.PGPMimeMailRecognizer;
import com.openexchange.mail.oauth.MailOAuthService;
import com.openexchange.mail.osgi.AuthenticationFailedHandlerServiceImpl;
import com.openexchange.mail.osgi.MailAuthenticatorServiceTracker;
import com.openexchange.mail.osgi.MailCapabilityServiceTracker;
import com.openexchange.mail.osgi.MailProviderServiceTracker;
import com.openexchange.mail.osgi.MailSessionCacheInvalidator;
import com.openexchange.mail.osgi.MailcapServiceTracker;
import com.openexchange.mail.osgi.TransportProviderServiceTracker;
import com.openexchange.mail.service.MailService;
import com.openexchange.mail.service.impl.MailServiceImpl;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.mail.transport.config.NoReplyConfigFactory;
import com.openexchange.mail.transport.config.impl.DefaultNoReplyConfigFactory;
import com.openexchange.mailaccount.MailAccountDeleteListener;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.mailaccount.internal.CreateMailAccountTables;
import com.openexchange.mailaccount.internal.DeleteListenerServiceTracker;
import com.openexchange.management.ManagementService;
import com.openexchange.management.osgi.HousekeepingManagementTracker;
import com.openexchange.messaging.registry.MessagingServiceRegistry;
import com.openexchange.metadata.MetadataService;
import com.openexchange.mime.MimeTypeMap;
import com.openexchange.multiple.MultipleHandlerFactoryService;
import com.openexchange.multiple.internal.MultipleHandlerServiceTracker;
import com.openexchange.notification.service.FullNameBuilderService;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.provider.resourceserver.OAuthResourceService;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.objectusecount.service.ObjectUseCountServiceTracker;
import com.openexchange.osgi.BundleServiceTracker;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.Tools;
import com.openexchange.password.mechanism.PasswordMechRegistry;
import com.openexchange.pns.PushNotificationService;
import com.openexchange.preview.PreviewService;
import com.openexchange.principalusecount.PrincipalUseCountService;
import com.openexchange.quota.QuotaProvider;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.resource.ResourceService;
import com.openexchange.resource.storage.ResourceStorage;
import com.openexchange.search.SearchService;
import com.openexchange.secret.SecretEncryptionFactoryService;
import com.openexchange.secret.SecretService;
import com.openexchange.secret.osgi.tools.WhiteboardSecretService;
import com.openexchange.server.impl.Starter;
import com.openexchange.server.reloadable.GenericReloadable;
import com.openexchange.server.services.ServerRequestHandlerRegistry;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.serverconfig.ServerConfigService;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.SessionHolder;
import com.openexchange.session.SessionSsoService;
import com.openexchange.session.ThreadLocalSessionHolder;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.snippet.QuotaAwareSnippetService;
import com.openexchange.spamhandler.SpamHandler;
import com.openexchange.spamhandler.osgi.SpamHandlerServiceTracker;
import com.openexchange.startup.ThreadControlService;
import com.openexchange.systemname.SystemNameService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;
import com.openexchange.tools.oxfolder.GABRestorerRMIServiceImpl;
import com.openexchange.tools.oxfolder.OXFolderPathUniqueness;
import com.openexchange.tools.oxfolder.cleanup.OXFolderPathUniquenessDatabaseCleanUpTracker;
import com.openexchange.tools.oxfolder.property.FolderSubscriptionHelper;
import com.openexchange.tools.strings.StringParser;
import com.openexchange.uadetector.UserAgentParser;
import com.openexchange.uploaddir.UploadDirService;
import com.openexchange.uploaddir.impl.UploadDirServiceImpl;
import com.openexchange.user.UserService;
import com.openexchange.user.interceptor.UserServiceInterceptor;
import com.openexchange.user.interceptor.UserServiceInterceptorRegistry;
import com.openexchange.user.internal.FilteringUserService;
import com.openexchange.user.internal.UserServiceImpl;
import com.openexchange.userconf.UserConfigurationService;
import com.openexchange.userconf.UserPermissionService;
import com.openexchange.userconf.internal.UserConfigurationServiceImpl;
import com.openexchange.userconf.internal.UserPermissionServiceImpl;
import com.openexchange.version.VersionService;
import com.openexchange.webdav.FreeBusy;
import com.openexchange.webdav.FreeBusyProperty;
import com.openexchange.webdav.request.analyzer.FreeBusyRequestAnalyzer;
import com.openexchange.xml.jdom.JDOMParser;
import com.openexchange.xml.spring.SpringParser;
import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.LoggerProvider;

/**
 * {@link ServerActivator} - The activator for server bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ServerActivator extends HousekeepingActivator {

    private static final class ServiceAdderTrackerCustomizer implements ServiceTrackerCustomizer<FileMetadataParserService, FileMetadataParserService> {

        private final BundleContext context;

        public ServiceAdderTrackerCustomizer(final BundleContext context) {
            super();
            this.context = context;
        }

        @Override
        public void removedService(final ServiceReference<FileMetadataParserService> reference, final FileMetadataParserService service) {
            ServerServiceRegistry.getInstance().removeService(FileMetadataParserService.class);
            context.ungetService(reference);
        }

        @Override
        public void modifiedService(final ServiceReference<FileMetadataParserService> reference, final FileMetadataParserService service) {
            // Nope
        }

        @Override
        public FileMetadataParserService addingService(final ServiceReference<FileMetadataParserService> reference) {
            final FileMetadataParserService service = context.getService(reference);
            ServerServiceRegistry.getInstance().addService(FileMetadataParserService.class, service);
            return service;
        }
    }

    /** The logger */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ServerActivator.class);

    /**
     * Constant for string: "identifier"
     */
    private static final String STR_IDENTIFIER = "identifier";

    private static final Class<?>[] NEEDED_SERVICES_SERVER = {
        // @formatter:off
        ConfigurationService.class, DatabaseService.class, CacheService.class, EventAdmin.class, SessiondService.class, SpringParser.class,
        JDOMParser.class, TimerService.class, ThreadPoolService.class,
        MessagingServiceRegistry.class, HtmlService.class,
        IDBasedFolderAccessFactory.class, IDBasedFileAccessFactory.class, FileStorageServiceRegistry.class, FileStorageAccountManagerLookupService.class,
        CryptoService.class, HttpService.class, SystemNameService.class, ConfigViewFactory.class, StringParser.class, PreviewService.class,
        SecretEncryptionFactoryService.class, SearchService.class, DispatcherPrefixService.class,
        UserAgentParser.class, PasswordMechRegistry.class, LeanConfigurationService.class, VersionService.class };
        // @formatter:on

    private static volatile BundleContext CONTEXT;

    /**
     * Gets the bundle context.
     *
     * @return The bundle context or <code>null</code> if not started, yet
     */
    public static BundleContext getContext() {
        return CONTEXT;
    }

    private final List<ServiceTracker<?, ?>> serviceTrackerList;
    private final List<EventHandlerRegistration> eventHandlerList;
    private final List<BundleActivator> activators;
    private final Starter starter;
    private volatile WhiteboardSecretService secretService;
    private volatile LockServiceImpl lockService;

    /**
     * Initializes a new {@link ServerActivator}
     */
    public ServerActivator() {
        super();
        this.starter = new Starter();
        serviceTrackerList = new ArrayList<ServiceTracker<?, ?>>();
        eventHandlerList = new ArrayList<EventHandlerRegistration>();
        activators = new ArrayList<BundleActivator>(8);
    }

    /**
     * The server bundle will not start unless these services are available.
     */
    @Override
    protected Class<?>[] getNeededServices() {
        return NEEDED_SERVICES_SERVER;
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        LOG.warn("Absent service: {}", clazz.getName());
        if (CacheService.class.equals(clazz)) {
            final CacheAvailabilityRegistry reg = CacheAvailabilityRegistry.getInstance();
            if (null != reg) {
                try {
                    reg.notifyAbsence();
                } catch (OXException e) {
                    LOG.error("", e);
                }
            }
        }
        ServerServiceRegistry.getInstance().removeService(clazz);
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        LOG.info("Re-available service: {}", clazz.getName());
        ServerServiceRegistry.getInstance().addService(clazz, getService(clazz));
        if (CacheService.class.equals(clazz)) {
            final CacheAvailabilityRegistry reg = CacheAvailabilityRegistry.getInstance();
            if (null != reg) {
                try {
                    reg.notifyAvailability();
                } catch (OXException e) {
                    LOG.error("", e);
                }
            }
        }
    }

    @Override
    protected void startBundle() throws Exception {
        final BundleContext context = this.context;
        CONTEXT = context;
        {
            // Set logger
            JSONObject.setLogger(new Slf4jLogger(JSONValue.class));
            // JSON configuration
            final ConfigurationService service = getService(ConfigurationService.class);
            JSONObject.setMaxSize(service.getIntProperty("com.openexchange.json.maxSize", 2500));
            JsonMaxSize jsonMaxSize = new JsonMaxSize(); // --> Statically registered via ConfigTree class
            registerService(ConfigTreeEquivalent.class, jsonMaxSize);

            final AvailableFindModules availableModules = new AvailableFindModules();
            registerService(PreferencesItemService.class, availableModules);
            registerService(ConfigTreeEquivalent.class, availableModules);
        }
        Config.LoggerProvider = LoggerProvider.DISABLED;
        // (Re-)Initialize server service registry with available services
        {
            ServerServiceRegistry registry = ServerServiceRegistry.getInstance();
            registry.clearRegistry();
            for (Class<?> clazz : getNeededServices()) {
                Object service = getService(clazz);
                if (null != service) {
                    registry.addService(clazz, service);
                    if (DatabaseService.class == clazz) {
                        Database.setDatabaseService((DatabaseService) service);
                    }
                }
            }
        }
        LOG.info("starting bundle: com.openexchange.server");
        /*
         * Add service trackers
         */
        // Configuration service load
        final ServiceTracker<ConfigurationService, ConfigurationService> confTracker = new ServiceTracker<ConfigurationService, ConfigurationService>(context, ConfigurationService.class, new ConfigurationCustomizer(context));
        confTracker.open(); // We need this for {@link Starter#start()}
        serviceTrackerList.add(confTracker);

        // AssignmentFactory
        track(AssignmentFactory.class, new RegistryCustomizer<AssignmentFactory>(context, AssignmentFactory.class));

        // I18n service load
        track(I18nServiceRegistry.class, new RegistryCustomizer<I18nServiceRegistry>(context, I18nServiceRegistry.class));
        track(I18nService.class, new I18nServiceListener(context));

        // Deputy service
        track(DeputyService.class, new RegistryCustomizer<DeputyService>(context, DeputyService.class));

        // Audit logger
        track(AuditLogService.class, new RegistryCustomizer<AuditLogService>(context, AuditLogService.class));

        // Full-name builder
        track(ServerConfigService.class, new RegistryCustomizer<ServerConfigService>(context, ServerConfigService.class));
        track(FullNameBuilderService.class, new RegistryCustomizer<FullNameBuilderService>(context, FullNameBuilderService.class));
        track(TranslatorFactory.class, new RegistryCustomizer<TranslatorFactory>(context, TranslatorFactory.class));

        // Mail account delete listener
        track(MailAccountDeleteListener.class, new DeleteListenerServiceTracker(context));

        // Mail provider service tracker
        track(CacheEventService.class, new MailSessionCacheInvalidator(context));
        track(MailProvider.class, new MailProviderServiceTracker(context));
        {
            AuthenticationFailedHandlerServiceImpl authenticationFailedService = new AuthenticationFailedHandlerServiceImpl(context);
            rememberTracker(authenticationFailedService);
            registerService(AuthenticationFailedHandlerService.class, authenticationFailedService);
            ServerServiceRegistry.getInstance().addService(AuthenticationFailedHandlerService.class, authenticationFailedService);
        }
        track(MailcapCommandMap.class, new MailcapServiceTracker(context));
        track(CapabilityService.class, new MailCapabilityServiceTracker(context));
        track(AttachmentTokenService.class, new RegistryCustomizer<AttachmentTokenService>(context, AttachmentTokenService.class));
        // Mail compose stuff
        track(ComposeHandlerRegistry.class, new RegistryCustomizer<ComposeHandlerRegistry>(context, ComposeHandlerRegistry.class));
        track(ShareLinkGeneratorRegistry.class, new RegistryCustomizer<ShareLinkGeneratorRegistry>(context, ShareLinkGeneratorRegistry.class));
        track(MessageGeneratorRegistry.class, new RegistryCustomizer<MessageGeneratorRegistry>(context, MessageGeneratorRegistry.class));
        track(AttachmentStorageRegistry.class, new RegistryCustomizer<AttachmentStorageRegistry>(context, AttachmentStorageRegistry.class));
        track(EnabledCheckerRegistry.class, new RegistryCustomizer<EnabledCheckerRegistry>(context, EnabledCheckerRegistry.class));
        track(MailAuthenticityHandlerRegistry.class, new RegistryCustomizer<MailAuthenticityHandlerRegistry>(context, MailAuthenticityHandlerRegistry.class));
        track(MailAuthenticator.class, new MailAuthenticatorServiceTracker(context));
        // Crypto
        track(CryptoMailRecognizer.class, new CryptoRecognizerTracker(context));
        // Segmented updates
        track(SegmentedUpdateService.class, new RegistryCustomizer<SegmentedUpdateService>(context, SegmentedUpdateService.class));

        // Principal use count stuff
        track(ResourceService.class, new RegistryCustomizer<ResourceService>(context, ResourceService.class));
        track(ResourceStorage.class, new RegistryCustomizer<ResourceStorage>(context, ResourceStorage.class));
        track(GroupStorage.class, new RegistryCustomizer<GroupStorage>(context, GroupStorage.class));
        track(PrincipalUseCountService.class, new RegistryCustomizer<PrincipalUseCountService>(context, PrincipalUseCountService.class));

        // IP checker
        track(IPCheckService.class, new RegistryCustomizer<IPCheckService>(context, IPCheckService.class));

        // OAuth service
        track(OAuthService.class, new RegistryCustomizer<OAuthService>(context, OAuthService.class));
        track(MailOAuthService.class, new RegistryCustomizer<MailOAuthService>(context, MailOAuthService.class));

        // Push notification service (PNS)
        track(PushNotificationService.class, new RegistryCustomizer<PushNotificationService>(context, PushNotificationService.class));

        // Session SSO checker
        track(SessionSsoService.class, new RegistryCustomizer<SessionSsoService>(context, SessionSsoService.class));

        // Image transformation service
        track(ImageTransformationService.class, new RegistryCustomizer<ImageTransformationService>(context, ImageTransformationService.class));
        track(ImageMetadataService.class, new RegistryCustomizer<ImageMetadataService>(context, ImageMetadataService.class));
        track(MediaMetadataExtractorService.class, new RegistryCustomizer<MediaMetadataExtractorService>(context, MediaMetadataExtractorService.class));

        // Transport provider service tracker
        track(TransportProvider.class, new TransportProviderServiceTracker(context));

        // Spam handler provider service tracker
        track(SpamHandler.class, new SpamHandlerServiceTracker(context));

        // CacheEventService
        track(CacheEventService.class, new RegistryCustomizer<CacheEventService>(context, CacheEventService.class));

        // MetadataService
        track(MetadataService.class, new RegistryCustomizer<MetadataService>(context, MetadataService.class));

        // ThreadControlService
        track(ThreadControlService.class, new RegistryCustomizer<ThreadControlService>(context, ThreadControlService.class));

        // AJAX request handler
        track(AJAXRequestHandler.class, new AJAXRequestHandlerCustomizer(context));

        // Reminder Service
        track(ReminderService.class, new RegistryCustomizer<>(context, ReminderService.class));

        // ICal Parser & Emitter
        track(ICalParser.class, new RegistryCustomizer<ICalParser>(context, ICalParser.class));
        track(ICalEmitter.class, new RegistryCustomizer<ICalEmitter>(context, ICalEmitter.class));

        // vCard service & storage
        track(VCardService.class, new RegistryCustomizer<VCardService>(context, VCardService.class));
        track(VCardStorageFactory.class, new RegistryCustomizer<VCardStorageFactory>(context, VCardStorageFactory.class));

        // Data Retention Service
        track(DataRetentionService.class, new RegistryCustomizer<DataRetentionService>(context, DataRetentionService.class));

        // Delete Listener Service Tracker
        track(DeleteListener.class, new DeleteListenerServiceTrackerCustomizer(context));

        // Folder Delete Listener Service Tracker
        track(FolderDeleteListenerService.class, new FolderDeleteListenerServiceTrackerCustomizer(context));

        // Delete Context Group Listener Service Tracker
        track(DeleteContextGroupListener.class, new DeleteContextGroupListenerServiceTracker(context));

        // Distributed files
        track(DistributedFileManagement.class, new DistributedFilesListener());

        // CapabilityService
        track(CapabilityService.class, new CapabilityRegistrationListener(context));

        // Authenticator
        track(Authenticator.class, new RegistryCustomizer<Authenticator>(context, Authenticator.class));
        track(ManagementService.class, new HousekeepingManagementTracker(context, AuthenticatorMBean.class.getName(), AuthenticatorMBean.DOMAIN, new AuthenticatorMBeanImpl()));
        {
            Dictionary<String, Object> props = new Hashtable<String, Object>(2);
            props.put("RMIName", RemoteAuthenticator.RMI_NAME);
            registerService(Remote.class, new RemoteAuthenticatorImpl(), props);
        }

        // Obfuscator
        track(ObfuscatorService.class, new RegistryCustomizer<ObfuscatorService>(context, ObfuscatorService.class));

        /*
         * Register EventHandler
         */
        final OSGiEventDispatcher dispatcher = new OSGiEventDispatcher();
        EventQueue.setNewEventDispatcher(dispatcher);
        eventHandlerList.add(dispatcher);
        eventHandlerList.add(new MailAccessCacheEventListener());
        for (final EventHandlerRegistration ehr : eventHandlerList) {
            ehr.registerService(context);
        }

        track(ManagementService.class, new ManagementServiceTracker(context));
        // TODO:
        /*-
         * serviceTrackerList.add(new ServiceTracker(context, MonitorService.class.getName(),
         *     new BundleServiceTracker&lt;MonitorService&gt;(context, MonitorService.getInstance(), MonitorService.class)));
         */

        // Search for ConfigJumpService
        track(ConfigJumpService.class, new BundleServiceTracker<ConfigJumpService>(context, ConfigJump.getHolder(), ConfigJumpService.class));
        // Search for extensions of the preferences tree interface
        track(PreferencesItemService.class, new PreferencesCustomizer(context));
        // Search for host name service
        track(HostnameService.class, new HostnameServiceCustomizer(context));
        // Conversion service
        track(ConversionService.class, new RegistryCustomizer<ConversionService>(context, ConversionService.class));
        // Contact collector
        track(ContactCollectorService.class, new RegistryCustomizer<ContactCollectorService>(context, ContactCollectorService.class));
        // Login handler
        track(LoginHandlerService.class, new LoginHandlerCustomizer(context));
        track(BlockingLoginHandlerService.class, new BlockingLoginHandlerCustomizer(context));
        // Login listener
        track(Tools.generateServiceFilter(context, LoginListener.class, AutoLoginAwareLoginListener.class), new LoginListenerCustomizer(context));
        // Multiple handler factory services
        track(MultipleHandlerFactoryService.class, new MultipleHandlerServiceTracker(context));

        track(GuestService.class, new RegistryCustomizer<GuestService>(context, GuestService.class));

        // Attachment Plugins
        serviceTrackerList.add(new AttachmentAuthorizationTracker(context));
        serviceTrackerList.add(new AttachmentListenerTracker(context));

        // Folder Fields
        track(AdditionalFolderField.class, new FolderFieldCollector(context, Folder.getAdditionalFields()));

        /*
         * The FileMetadataParserService needs to be tracked by a separate service tracker instead of just adding the service to
         * getNeededServices(), because publishing bundle needs the HttpService which is in turn provided by server
         */
        track(FileMetadataParserService.class, new ServiceAdderTrackerCustomizer(context));

        /*
         * Track ManagedFileManagement
         */
        track(ManagedFileManagement.class, new RegistryCustomizer<ManagedFileManagement>(context, ManagedFileManagement.class));

        /*
         * Track UnifiedViewService
         */
        track(UnifiedViewService.class, new RegistryCustomizer<UnifiedViewService>(context, UnifiedViewService.class));

        /*
         * Track OAuth provider services
         */
        track(OAuthResourceService.class, new RegistryCustomizer<OAuthResourceService>(context, OAuthResourceService.class));

        /*
         * Track QuotaFileStorageService
         */
        track(QuotaFileStorageService.class, new RegistryCustomizer<QuotaFileStorageService>(context, QuotaFileStorageService.class));
        /*
         * Track QuotaAwareSnippetService
         */
        track(QuotaAwareSnippetService.class, new RankingAwareRegistryCustomizer<QuotaAwareSnippetService>(context, QuotaAwareSnippetService.class));
        /*
         * Track GroupService
         */
        track(GroupService.class, new RegistryCustomizer<>(context, GroupService.class));
        /*
         * Track AdministrativeFreeBusyService
         */
        track(AdministrativeFreeBusyService.class, new RegistryCustomizer<AdministrativeFreeBusyService>(context, AdministrativeFreeBusyService.class));
        /*
         * Track OXContextService
         */
        track(OXContextService.class, new RegistryCustomizer<>(context, OXContextService.class));

        trackService(OAuthResourceService.class);

        /*
         * User Alias Service
         */
        CachingAliasStorage aliasStorage;
        {
            // @formatter:off
            String regionName = "UserAlias";
            byte[] ccf = (  "jcs.region." + regionName + "=LTCP\n" +
                            "jcs.region." + regionName + ".cacheattributes=org.apache.jcs.engine.CompositeCacheAttributes\n" +
                            "jcs.region." + regionName + ".cacheattributes.MaxObjects=1000000\n" +
                            "jcs.region." + regionName + ".cacheattributes.MemoryCacheName=org.apache.jcs.engine.memory.lru.LRUMemoryCache\n" +
                            "jcs.region." + regionName + ".cacheattributes.UseMemoryShrinker=true\n" + "jcs.region." + regionName + ".cacheattributes.MaxMemoryIdleTimeSeconds=360\n" +
                            "jcs.region." + regionName + ".cacheattributes.ShrinkerIntervalSeconds=60\n" + "jcs.region." + regionName + ".elementattributes=org.apache.jcs.engine.ElementAttributes\n" +
                            "jcs.region." + regionName + ".elementattributes.IsEternal=false\n" + "jcs.region." + regionName + ".elementattributes.MaxLifeSeconds=-1\n" +
                            "jcs.region." + regionName + ".elementattributes.IdleTime=360\n" + "jcs.region." + regionName + ".elementattributes.IsSpool=false\n" +
                            "jcs.region." + regionName + ".elementattributes.IsRemote=false\n" + "jcs.region." + regionName + ".elementattributes.IsLateral=false\n").getBytes();
            // @formatter:on
            getService(CacheService.class).loadConfiguration(new ByteArrayInputStream(ccf));

            aliasStorage = new CachingAliasStorage(new RdbAliasStorage());
            registerService(DeleteListener.class, new AliasCacheDeleteListener(aliasStorage));
            ServerServiceRegistry.getInstance().addService(UserAliasStorage.class, aliasStorage);
        }

        /*
         * User Service
         */
        UserServiceInterceptorRegistry interceptorRegistry = new UserServiceInterceptorRegistry(context);
        track(UserServiceInterceptor.class, interceptorRegistry);
        registerService(UserServiceInterceptor.class, new com.openexchange.tools.oxfolder.userinterceptor.UserFolderNameInterceptor(this));

        UserService userService = new FilteringUserService(new UserServiceImpl(interceptorRegistry, getService(PasswordMechRegistry.class)), this);
        ServerServiceRegistry.getInstance().addService(UserService.class, userService);

        track(ObjectUseCountService.class, new ObjectUseCountServiceTracker(context));
        track(CalendarService.class, new RegistryCustomizer<CalendarService>(context, CalendarService.class));
        track(ICalService.class, new RegistryCustomizer<ICalService>(context, ICalService.class));
        track(FolderSubscriptionHelper.class, new RegistryCustomizer<FolderSubscriptionHelper>(context, FolderSubscriptionHelper.class));

        CommonResultConverterRegistry resultConverterRegistry = new CommonResultConverterRegistry(context);
        track(ResultConverter.class, resultConverterRegistry);

        track(DatabaseCleanUpService.class, new OXFolderPathUniquenessDatabaseCleanUpTracker(context));

        ConfigViewFactory configViewFactory = getService(ConfigViewFactory.class);
        ConfigurationService configService = getService(ConfigurationService.class);

        // Permission checker
        {
            PermissionConfigurationCheckerImpl checker = new PermissionConfigurationCheckerImpl();
            checker.checkConfig(configService);
            registerService(PermissionConfigurationChecker.class, checker);
        }

        // Start up server the usual way
        starter.start();

        // Open service trackers
        for (final ServiceTracker<?, ?> tracker : serviceTrackerList) {
            tracker.open();
        }
        openTrackers();
        // Register server's services
        registerService(UserService.class, userService);
        registerService(UserAliasStorage.class, aliasStorage);
        registerService(Reloadable.class, ServerConfig.getInstance());
        registerService(Reloadable.class, SystemConfig.getInstance());
        registerService(Reloadable.class, GenericReloadable.getInstance());
        registerService(Reloadable.class, ResponseWriter.getReloadable());
        registerService(CharsetProvider.class, new CustomCharsetProvider());
        registerService(FileBackedJSONStringProvider.class, new FileBackedJSONStringProviderImpl());
        ServerServiceRegistry.getInstance().addService(UserConfigurationService.class, new UserConfigurationServiceImpl());
        registerService(UserConfigurationService.class, ServerServiceRegistry.getInstance().getService(UserConfigurationService.class, true));

        {
            Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(1);
            serviceProperties.put("RMI_NAME", GABRestorerRMIServiceImpl.RMI_NAME);
            registerService(Remote.class, new GABRestorerRMIServiceImpl(), serviceProperties);
        }

        ServerServiceRegistry.getInstance().addService(UserPermissionService.class, new UserPermissionServiceImpl());
        registerService(UserPermissionService.class, ServerServiceRegistry.getInstance().getService(UserPermissionService.class, true));

        ContextService contextService = ServerServiceRegistry.getInstance().getService(ContextService.class, true);
        registerService(ContextService.class, contextService);
        // Register mail stuff
        MailServiceImpl mailService = new MailServiceImpl();
        {
            registerService(MailService.class, mailService);
            final Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(1);
            serviceProperties.put(EventConstants.EVENT_TOPIC, MailSessionEventHandler.getTopics());
            registerService(EventHandler.class, new MailSessionEventHandler(), serviceProperties);
            registerService(MailCounter.class, new MailCounterImpl());
            registerService(MailIdleCounter.class, new MailIdleCounterImpl());
            registerService(MimeTypeMap.class, new MimeTypeMap() {

                @Override
                public String getContentType(final File file) {
                    return MimeType2ExtMap.getContentType(file);
                }

                @Override
                public String getContentType(final String fileName) {
                    return MimeType2ExtMap.getContentType(fileName);
                }

                @Override
                public String getContentTypeByExtension(final String extension) {
                    return MimeType2ExtMap.getContentTypeByExtension(extension);
                }

                @Override
                public List<String> getFileExtensions(final String mime) {
                    return MimeType2ExtMap.getFileExtensions(mime);
                }
            });
        }
        registerService(NoReplyConfigFactory.class, new DefaultNoReplyConfigFactory(contextService, configViewFactory));
        // CryptoMailService
        registerService(CryptoMailRecognizer.class, new CompositePGPMailRecognizer(new PGPMimeMailRecognizer(), new PGPInlineMailRecognizer()));
        registerService(CryptoMailRecognizerService.class, new CryptoMailRecognizerServiceImpl());
        // TODO: Register server's login handler here until its encapsulated in an own bundle
        registerService(LoginHandlerService.class, new MailLoginHandler());
        registerService(LoginHandlerService.class, new LoginNameRecorder(userService));
        // Multifactor Services
        registerService(AutoLoginAwareLoginListener.class, new MultifactorAutoLoginAwareListener());
        ServerServiceRegistry.getInstance().addService(MultifactorChecker.class, new MultifactorChecker(this));
        // registrationList.add(context.registerService(LoginHandlerService.class.getName(), new PasswordCrypter(), null));
        // Register table creation for mail account storage.
        registerService(CreateTableService.class, new CreateMailAccountTables());
        registerService(CreateTableService.class, new CreateIDSequenceTable());
        // Register oxfolder_reservedpath related services
        registerService(CreateTableService.class, new OXFolderPathUniqueness.CreateFolderReservedPathTable());
        registerService(UpdateTaskProviderService.class, () -> Arrays.asList(new OXFolderPathUniqueness.CreateFolderReservedPathUpdateTask()));

        // TODO: Register server's mail account storage here until its encapsulated in an own bundle
        MailAccountStorageService mailAccountStorageService = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class);
        registerService(MailAccountStorageService.class, mailAccountStorageService);
        // TODO: Register server's Unified Mail management here until its encapsulated in an own bundle
        registerService(UnifiedInboxManagement.class, ServerServiceRegistry.getInstance().getService(UnifiedInboxManagement.class));
        // TODO: Register server's Unified Mail management here until its encapsulated in an own bundle
        registerService(QuotaProvider.class, new MailQuotaProvider(mailAccountStorageService, mailService));
        // Register ID generator
        registerService(IDGeneratorService.class, ServerServiceRegistry.getInstance().getService(IDGeneratorService.class));
        /*
         * register an additional file field providing a sanitized filename
         */
        registerService(AdditionalFileField.class, new SanitizedFilename());
        /*
         * Register data sources
         */
        {
            final Dictionary<String, Object> props = new Hashtable<String, Object>(1);
            props.put(STR_IDENTIFIER, "com.openexchange.mail.vcard");
            registerService(DataSource.class, new VCardMailPartDataSource(), props);
        }
        {
            final Dictionary<String, Object> props = new Hashtable<String, Object>(1);
            props.put(STR_IDENTIFIER, "com.openexchange.mail.ical");
            registerService(DataSource.class, new ICalMailPartDataSource().setServiceLookup(this), props);
        }
        {
            final Dictionary<String, Object> props = new Hashtable<String, Object>(1);
            props.put(STR_IDENTIFIER, "com.openexchange.mail.attachment");
            registerService(DataSource.class, new AttachmentMailPartDataSource(), props);
        }
        // {
        // final InlineImageDataSource dataSource = InlineImageDataSource.getInstance();
        // final Dictionary<String, Object> props = new Hashtable<String, Object>(1);
        // props.put(STR_IDENTIFIER, dataSource.getRegistrationName());
        // registerService(DataSource.class, dataSource, props);
        // ImageServlet.addMapping(dataSource.getRegistrationName(), dataSource.getAlias());
        // }
        // {
        // final ContactImageDataSource dataSource = ContactImageDataSource.getInstance();
        // final Dictionary<String, Object> props = new Hashtable<String, Object>(1);
        // props.put(STR_IDENTIFIER, dataSource.getRegistrationName());
        // registerService(DataSource.class, dataSource, props);
        // ImageServlet.addMapping(dataSource.getRegistrationName(), dataSource.getAlias());
        // }
        // {
        // final ManagedFileImageDataSource dataSource = new ManagedFileImageDataSource();
        // final Dictionary<String, Object> props = new Hashtable<String, Object>(1);
        // props.put(STR_IDENTIFIER, dataSource.getRegistrationName());
        // registerService(DataSource.class, dataSource, props);
        // ImageServlet.addMapping(dataSource.getRegistrationName(), dataSource.getAlias());
        // }

        registerService(ResultConverterRegistry.class, resultConverterRegistry);
        ServerServiceRegistry.getInstance().addService(ResultConverterRegistry.class, resultConverterRegistry);

        // Register DBProvider
        registerService(DBProvider.class, new DBPoolProvider());

        // Register Infostore
        registerInfostore();

        // Register AttachmentBase
        registerService(AttachmentBase.class, Attachment.ATTACHMENT_BASE);

        // Register event factory service
        {
            final EventFactoryServiceImpl eventFactoryServiceImpl = new EventFactoryServiceImpl();
            registerService(EventFactoryService.class, eventFactoryServiceImpl);
            ServerServiceRegistry.getInstance().addService(EventFactoryService.class, eventFactoryServiceImpl);
        }

        // Register folder service
        final FolderService folderService = new FolderServiceImpl();
        registerService(FolderService.class, folderService);
        ServerServiceRegistry.getInstance().addService(FolderService.class, folderService);

        // Register folder i18n name service
        FolderI18nNamesServiceImpl folderI18nNamesService = FolderI18nNamesServiceImpl.getInstance();
        registerService(FolderI18nNamesService.class, folderI18nNamesService);
        ServerServiceRegistry.getInstance().addService(FolderI18nNamesService.class, folderI18nNamesService);

        // Register SessionHolder
        registerService(SessionHolder.class, ThreadLocalSessionHolder.getInstance());
        ServerServiceRegistry.getInstance().addService(SessionHolder.class, ThreadLocalSessionHolder.getInstance());

        // Register upload directory service
        registerService(UploadDirService.class, new UploadDirServiceImpl());

        // Fake bundle start
        activators.add(new FolderStorageActivator());
        for (final BundleActivator activator : activators) {
            activator.start(context);
        }

        WhiteboardSecretService secretService = new WhiteboardSecretService(context);
        this.secretService = secretService;
        ServerServiceRegistry.getInstance().addService(SecretService.class, secretService);
        secretService.open();

        // Cache for generic volatile locks
        {
            LockServiceImpl lockService = new LockServiceImpl();
            this.lockService = lockService;
            ServerServiceRegistry.getInstance().addService(LockService.class, lockService);
            registerService(LockService.class, lockService);
        }

        /*
         * Register servlets
         */
        registerServlets(getService(HttpService.class));

        ShardingSubdomains shardingSubdomains = new ShardingSubdomains();
        registerService(PreferencesItemService.class, shardingSubdomains);
        registerService(ConfigTreeEquivalent.class, shardingSubdomains);
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("stopping bundle: com.openexchange.server");
        try {
            /*
             * Fake bundle stop
             */
            for (final BundleActivator activator : activators) {
                activator.stop(context);
            }
            activators.clear();
            /*
             * Unregister server's services
             */
            unregisterServices();
            /*
             * Close service trackers
             */
            for (final ServiceTracker<?, ?> tracker : serviceTrackerList) {
                tracker.close();
            }
            closeTrackers();
            serviceTrackerList.clear();
            ServerRequestHandlerRegistry.getInstance().clearRegistry();
            /*
             * Unregister EventHandler
             */
            for (final EventHandlerRegistration ehr : eventHandlerList) {
                ehr.unregisterService();
            }
            eventHandlerList.clear();
            // Stop all inside the server.
            starter.stop();
            /*
             * Clear service registry
             */
            ServerServiceRegistry.getInstance().clearRegistry();
            WhiteboardSecretService secretService = this.secretService;
            if (null != secretService) {
                this.secretService = null;
                secretService.close();
            }
            LockServiceImpl lockService = this.lockService;
            if (null != lockService) {
                this.lockService = null;
                lockService.dispose();
            }
            LoginServlet.setRampUpServices(null);
            UploadUtility.shutDown();
            super.stopBundle();
        } finally {
            started.set(false);
            CONTEXT = null;
        }
    }

    private void registerInfostore() {
        DBProvider dbProvider = new DatabaseServiceDBProvider(getService(DatabaseService.class));
        InfostoreFacadeImpl infostoreFacade = new InfostoreFacadeImpl(dbProvider);
        infostoreFacade.setTransactional(true);
        infostoreFacade.setSessionHolder(ThreadLocalSessionHolder.getInstance());
        EventFiringInfostoreFacade eventFiringInfostoreFacade = new EventFiringInfostoreFacadeImpl(dbProvider);
        eventFiringInfostoreFacade.setTransactional(true);
        eventFiringInfostoreFacade.setSessionHolder(ThreadLocalSessionHolder.getInstance());
        FilteringInfostoreFacadeImpl filteringInfostoreFacadeImpl = new FilteringInfostoreFacadeImpl(infostoreFacade, this);
        registerService(InfostoreFacade.class, filteringInfostoreFacadeImpl);
        registerService(EventFiringInfostoreFacade.class, eventFiringInfostoreFacade);
        registerService(InfostoreSearchEngine.class, filteringInfostoreFacadeImpl);
        registerService(LoginHandlerService.class, new InfostoreAutodeleteFileVersionsLoginHandler(infostoreFacade));
    }

    private void registerServlets(final HttpService http) throws ServletException, NamespaceException {
        http.registerServlet("/infostore", new com.openexchange.webdav.Infostore(), null, null);
        http.registerServlet("/files", new com.openexchange.webdav.Infostore(), null, null);
        http.registerServlet("/drive", new com.openexchange.webdav.Infostore(), null, null);
        http.registerServlet("/servlet/webdav.infostore", new com.openexchange.webdav.Infostore(), null, null);
        http.registerServlet("/servlet/webdav.drive", new com.openexchange.webdav.Infostore(), null, null);
        // http.registerServlet(prefix+"tasks", new com.openexchange.ajax.Tasks(), null, null);
        // http.registerServlet(prefix+"contacts", new com.openexchange.ajax.Contact(), null, null);
        // http.registerServlet(prefix+"mail", new com.openexchange.ajax.Mail(), null, null);
        LeanConfigurationService leanConfigService = this.getService(LeanConfigurationService.class);
        if (leanConfigService.getBooleanProperty(FreeBusyProperty.ENABLE_INTERNET_FREEBUSY)) {
            registerService(RequestAnalyzer.class, new FreeBusyRequestAnalyzer(() -> getService(DatabaseService.class)));
            http.registerServlet(FreeBusy.SERVLET_PATH, new FreeBusy(this), null, null);
        }

        final String prefix = getService(DispatcherPrefixService.class).getPrefix();
        http.registerServlet(prefix + "mail.attachment", new com.openexchange.ajax.MailAttachment(), null, null);
        // http.registerServlet(prefix+"calendar", new com.openexchange.ajax.Appointment(), null, null);
        // http.registerServlet(prefix+"config", new com.openexchange.ajax.ConfigMenu(), null, null);
        // http.registerServlet(prefix+"attachment", new com.openexchange.ajax.Attachment(), null, null);
        // http.registerServlet(prefix+"reminder", new com.openexchange.ajax.Reminder(), null, null);
        // http.registerServlet(prefix+"group", new com.openexchange.ajax.Group(), null, null);
        // http.registerServlet(prefix+"resource", new com.openexchange.ajax.Resource(), null, null);
        http.registerServlet(prefix + "multiple", new com.openexchange.ajax.Multiple(this, prefix), null, null);
        // http.registerServlet(prefix+"quota", new com.openexchange.ajax.Quota(), null, null);
        http.registerServlet(prefix + "control", new com.openexchange.ajax.ConfigJump(), null, null);
        // http.registerServlet(prefix+"file", new com.openexchange.ajax.AJAXFile(), null, null);
        // http.registerServlet(prefix+"import", new com.openexchange.ajax.ImportServlet(), null, null);
        // http.registerServlet(prefix+"export", new com.openexchange.ajax.ExportServlet(), null, null);
        // http.registerServlet(prefix+"image", new com.openexchange.image.servlet.ImageServlet(), null, null);
        http.registerServlet(prefix + "sync", new com.openexchange.ajax.SyncServlet(), null, null);
    }

}
