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

package com.openexchange.mail.compose.impl.osgi;

import static com.openexchange.osgi.Tools.withRanking;
import java.rmi.Remote;
import java.time.Duration;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.openexchange.auth.Authenticator;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.context.ContextService;
import com.openexchange.conversion.DataSource;
import com.openexchange.crypto.CryptoService;
import com.openexchange.database.AssignmentFactory;
import com.openexchange.database.CreateTableService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.cleanup.CleanUpInfo;
import com.openexchange.database.cleanup.DatabaseCleanUpService;
import com.openexchange.database.cleanup.DefaultCleanUpJob;
import com.openexchange.database.provider.DatabaseServiceDBProvider;
import com.openexchange.deputy.DeputyService;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.DatabaseAccessProvider;
import com.openexchange.filestore.QuotaFileStorageService;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.groupware.filestore.FileLocationHandler;
import com.openexchange.groupware.update.DefaultUpdateTaskProviderService;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.html.HtmlService;
import com.openexchange.image.ImageActionFactory;
import com.openexchange.lock.LockService;
import com.openexchange.login.LoginHandlerService;
import com.openexchange.login.LoginResult;
import com.openexchange.mail.compose.AttachmentStorage;
import com.openexchange.mail.compose.AttachmentStorageService;
import com.openexchange.mail.compose.CompositionSpaceServiceFactory;
import com.openexchange.mail.compose.CompositionSpaceStorageService;
import com.openexchange.mail.compose.impl.CompositionSpaceServiceFactoryImpl;
import com.openexchange.mail.compose.impl.attachment.AttachmentImageDataSource;
import com.openexchange.mail.compose.impl.attachment.AttachmentStorageServiceImpl;
import com.openexchange.mail.compose.impl.attachment.filestore.ContextAssociatedFileStorageAttachmentStorage;
import com.openexchange.mail.compose.impl.attachment.filestore.DedicatedFileStorageAttachmentStorage;
import com.openexchange.mail.compose.impl.attachment.filestore.FileStrorageAttachmentFileLocationHandler;
import com.openexchange.mail.compose.impl.attachment.filestore.FilestorageAttachmentStorageDatabaseAccessProvider;
import com.openexchange.mail.compose.impl.attachment.rdb.RdbAttachmentStorage;
import com.openexchange.mail.compose.impl.cleanup.CompositionSpaceCleanUpTask;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceAddClientToken;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceAddContentEncryptedFlag;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceAddCustomHeaders;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceAddFileStorageIdentifier;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceAddReplyTo;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceAddReplyTo_2;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceCreateTableService;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceCreateTableTask;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceDeleteListener;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceDynamicRowType;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceEnlargeAttachmentNameField;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceEnlargeSubjectField;
import com.openexchange.mail.compose.impl.groupware.CompositionSpaceRestoreAttachmentBinaryDataColumn;
import com.openexchange.mail.compose.impl.rmi.RemoteCompositionSpaceServiceImpl;
import com.openexchange.mail.compose.impl.security.CompositionSpaceKeyStorageServiceImpl;
import com.openexchange.mail.compose.impl.security.FileStorageCompositionSpaceKeyStorage;
import com.openexchange.mail.compose.impl.security.FileStorageKeyStorageFileLocationHandler;
import com.openexchange.mail.compose.impl.security.HazelcastCompositionSpaceKeyStorage;
import com.openexchange.mail.compose.impl.storage.db.RdbCompositionSpaceStorageService;
import com.openexchange.mail.compose.impl.storage.inmemory.InMemoryCompositionSpaceStorageService;
import com.openexchange.mail.compose.impl.storage.security.CryptoCompositionSpaceStorageService;
import com.openexchange.mail.compose.security.CompositionSpaceKeyStorage;
import com.openexchange.mail.compose.security.CompositionSpaceKeyStorageService;
import com.openexchange.mail.json.compose.ComposeHandlerRegistry;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.Tools;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;
import com.openexchange.uploaddir.UploadDirService;
import com.openexchange.user.UserService;

/**
 * {@link CompositionSpaceActivator}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.2
 */
public class CompositionSpaceActivator extends HousekeepingActivator {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CompositionSpaceActivator.class);
    }

    private InMemoryCompositionSpaceStorageService inmemoryStorage;
    private RdbCompositionSpaceStorageService rdbStorage;
    private CleanUpInfo cleanUpInfo;

    /**
     * Initializes a new {@link CompositionSpaceActivator}.
     */
    public CompositionSpaceActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class, QuotaFileStorageService.class, CapabilityService.class, HtmlService.class,
            ConfigurationService.class, ContextService.class, UserService.class, ComposeHandlerRegistry.class, ObfuscatorService.class,
            ConfigViewFactory.class, CryptoService.class, MailAccountStorageService.class, ThreadPoolService.class, TimerService.class,
            SessiondService.class, DatabaseCleanUpService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class[] { Authenticator.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        final BundleContext context = this.context;

        final HazelcastCompositionSpaceKeyStorage hzCompositionSpaceKeyStorage = new HazelcastCompositionSpaceKeyStorage(this);

        ServiceTracker<HazelcastInstance, HazelcastInstance> hzTracker = new ServiceTracker<HazelcastInstance, HazelcastInstance>(context, HazelcastInstance.class, new ServiceTrackerCustomizer<HazelcastInstance, HazelcastInstance>() {

            @Override
            public synchronized HazelcastInstance addingService(ServiceReference<HazelcastInstance> reference) {
                HazelcastInstance hazelcastInstance = context.getService(reference);
                String mapName = discoverMapName(hazelcastInstance.getConfig());
                if (null == mapName) {
                    LoggerHolder.LOG.warn("No distributed composition space key map found in Hazelcast configuration. Hazelcast will not be used to store AES keys!");
                    context.ungetService(reference);
                    return null;
                }

                CompositionSpaceActivator.this.addService(HazelcastInstance.class, hazelcastInstance);
                hzCompositionSpaceKeyStorage.setHazelcastResources(hazelcastInstance, mapName);
                return hazelcastInstance;
            }

            @Override
            public void modifiedService(ServiceReference<HazelcastInstance> reference, HazelcastInstance hazelcastInstance) {
                // Ignore
            }

            @Override
            public synchronized void removedService(ServiceReference<HazelcastInstance> reference, HazelcastInstance hazelcastInstance) {
                hzCompositionSpaceKeyStorage.unsetHazelcastResources(true);
                CompositionSpaceActivator.this.removeService(HazelcastInstance.class);
                context.ungetService(reference);
            }

            /**
             * Discovers the map name from the supplied Hazelcast configuration.
             *
             * @param config The config object
             * @return The sessions map name
             * @throws IllegalStateException If no such map is available
             */
            private String discoverMapName(Config config) throws IllegalStateException {
                Map<String, MapConfig> mapConfigs = config.getMapConfigs();
                if (null != mapConfigs) {
                    for (String mapName : mapConfigs.keySet()) {
                        if (mapName.startsWith("cskeys-")) {
                            return mapName;
                        }
                    }
                }
                return null;
            }

        });
        rememberTracker(hzTracker);

        CompositionSpaceKeyStorageServiceImpl keyStorageService = new CompositionSpaceKeyStorageServiceImpl(this, context);
        rememberTracker(keyStorageService);

        AttachmentStorageServiceImpl attachmentStorageService = new AttachmentStorageServiceImpl(keyStorageService, this, context);
        rememberTracker(attachmentStorageService);

        trackService(LockService.class);
        trackService(UnifiedInboxManagement.class);
        trackService(UploadDirService.class);
        trackService(AssignmentFactory.class);
        trackService(DeputyService.class);

        openTrackers();

        registerService(CompositionSpaceKeyStorageService.class, keyStorageService);
        registerService(CompositionSpaceKeyStorage.class, hzCompositionSpaceKeyStorage, withRanking(0));
        registerService(CompositionSpaceKeyStorage.class, FileStorageCompositionSpaceKeyStorage.initInstance(this), withRanking(1));
        registerService(FileLocationHandler.class, new FileStorageKeyStorageFileLocationHandler(this));

        registerService(AttachmentStorageService.class, attachmentStorageService);

        registerService(AttachmentStorage.class, new DedicatedFileStorageAttachmentStorage(this), withRanking(2));
        registerService(AttachmentStorage.class, new ContextAssociatedFileStorageAttachmentStorage(this), withRanking(1));
        registerService(FileLocationHandler.class, new FileStrorageAttachmentFileLocationHandler());
        registerService(AttachmentStorage.class, new RdbAttachmentStorage(this), withRanking(0));
        registerService(DatabaseAccessProvider.class, new FilestorageAttachmentStorageDatabaseAccessProvider(this));

        {
            AttachmentImageDataSource attachmentImageDataSource = AttachmentImageDataSource.getInstance();
            attachmentImageDataSource.setService(attachmentStorageService);

            Dictionary<String, Object> attachmentImageProps = new Hashtable<String, Object>(1);
            attachmentImageProps.put("identifier", attachmentImageDataSource.getRegistrationName());
            registerService(DataSource.class, attachmentImageDataSource, attachmentImageProps);
            ImageActionFactory.addMapping(attachmentImageDataSource.getRegistrationName(), attachmentImageDataSource.getAlias());
        }

        ConfigurationService configurationService = getService(ConfigurationService.class);

        CompositionSpaceStorageService storageService;
        {
            DatabaseServiceDBProvider dbProvider = new DatabaseServiceDBProvider(getService(DatabaseService.class));
            RdbCompositionSpaceStorageService rdbStorage = new RdbCompositionSpaceStorageService(dbProvider, attachmentStorageService, this);
            this.rdbStorage = rdbStorage;
            boolean useInMemoryStorage = configurationService.getBoolProperty("com.openexchange.mail.compose.useInMemoryStorage", false);
            if (useInMemoryStorage) {
                long delayDuration = configurationService.getIntProperty("com.openexchange.mail.compose.delayDuration", 60000);
                long maxDelayDuration = configurationService.getIntProperty("com.openexchange.mail.compose.maxDelayDuration", 300000);
                InMemoryCompositionSpaceStorageService inmemoryStorage = new InMemoryCompositionSpaceStorageService(delayDuration, maxDelayDuration, rdbStorage);
                inmemoryStorage.start();
                this.inmemoryStorage = inmemoryStorage;
                storageService = inmemoryStorage;
            } else {
                storageService = rdbStorage;
            }

            // Set non-crypto composition space storage
            attachmentStorageService.setCompositionSpaceStorageService(storageService);

            storageService = new CryptoCompositionSpaceStorageService(storageService, keyStorageService, this);
        }
        registerService(CompositionSpaceStorageService.class, storageService);

        CompositionSpaceServiceFactoryImpl serviceFactoryImpl = new CompositionSpaceServiceFactoryImpl(storageService, attachmentStorageService, keyStorageService, this);
        registerService(CompositionSpaceServiceFactory.class, serviceFactoryImpl, Tools.withRanking(serviceFactoryImpl.getRanking()));

        cleanUpInfo = getServiceSafe(DatabaseCleanUpService.class).scheduleCleanUpJob(DefaultCleanUpJob.builder() //@formatter:off
                .withId(CompositionSpaceCleanUpTask.class)
                .withDelay(Duration.ofMinutes(60))
                .withInitialDelay(Duration.ofMinutes(5))
                .withRunsExclusive(true)
                .withExecution(new CompositionSpaceCleanUpTask(this))
                .build()); //@formatter:on

        {
            LoginHandlerService loginHandler = new LoginHandlerService() {

                @Override
                public void handleLogout(LoginResult logout) throws OXException {
                    // Ignore
                }

                @Override
                public void handleLogin(LoginResult login) throws OXException {
                    Session session = login.getSession();
                    if (null != session) {
                        AttachmentStorage attachmentStorage = attachmentStorageService.getAttachmentStorageFor(session);
                        attachmentStorage.deleteUnreferencedAttachments(session);
                    }
                }
            };
            registerService(LoginHandlerService.class, loginHandler);
        }

        // Register Groupware stuff.
        registerService(CreateTableService.class, new CompositionSpaceCreateTableService());
        registerService(UpdateTaskProviderService.class, new DefaultUpdateTaskProviderService(
            new CompositionSpaceCreateTableTask(),
            new CompositionSpaceAddContentEncryptedFlag(),
            new CompositionSpaceAddFileStorageIdentifier(),
            new CompositionSpaceEnlargeSubjectField(),
            new CompositionSpaceAddCustomHeaders(),
            new CompositionSpaceEnlargeAttachmentNameField(),
            new CompositionSpaceAddReplyTo(),
            new CompositionSpaceRestoreAttachmentBinaryDataColumn(),
            new CompositionSpaceDynamicRowType(),
            new CompositionSpaceAddReplyTo_2(),
            new CompositionSpaceAddClientToken()
        ));
        registerService(DeleteListener.class, new CompositionSpaceDeleteListener(this));

        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(1);
        serviceProperties.put("RMI_NAME", RemoteCompositionSpaceServiceImpl.RMI_NAME);
        registerService(Remote.class, new RemoteCompositionSpaceServiceImpl(this), serviceProperties);
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        InMemoryCompositionSpaceStorageService inmemoryStorage = this.inmemoryStorage;
        if (null != inmemoryStorage) {
            this.inmemoryStorage = null;
            inmemoryStorage.close();
        }
        RdbCompositionSpaceStorageService rdbStorage = this.rdbStorage;
        if (null != rdbStorage) {
            this.rdbStorage = null;
            try {
                rdbStorage.signalStop();
            } catch (Exception e) {
                LoggerHolder.LOG.error("Failed to stop database-backed composition space storage", e);
            }
        }
        FileStorageCompositionSpaceKeyStorage.unsetInstance();
        CleanUpInfo cleanUpInfo = this.cleanUpInfo;
        if (null != cleanUpInfo) {
            this.cleanUpInfo = null;
            cleanUpInfo.cancel(true);
        }
        super.stopBundle();
    }

    @Override
    public <S> boolean addService(Class<S> clazz, S service) {
        return super.addService(clazz, service);
    }

    @Override
    public <S> boolean removeService(Class<? extends S> clazz) {
        return super.removeService(clazz);
    }

}
