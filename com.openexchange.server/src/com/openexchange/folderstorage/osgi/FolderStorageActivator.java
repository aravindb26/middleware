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

package com.openexchange.folderstorage.osgi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.ajax.customizer.AdditionalFieldsUtils;
import com.openexchange.ajax.customizer.folder.AdditionalFolderField;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.config.Interests;
import com.openexchange.config.admin.HideAdminService;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentTypeDiscoveryService;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.addressbook.osgi.AddressbookFolderStorageActivator;
import com.openexchange.folderstorage.cache.osgi.CacheFolderStorageActivator;
import com.openexchange.folderstorage.calendar.osgi.CalendarFolderStorageActivator;
import com.openexchange.folderstorage.database.osgi.DatabaseFolderStorageActivator;
import com.openexchange.folderstorage.filestorage.osgi.FileStorageFolderStorageActivator;
import com.openexchange.folderstorage.internal.ConfiguredDefaultPermissions;
import com.openexchange.folderstorage.internal.ContentTypeRegistry;
import com.openexchange.folderstorage.internal.FilteringFolderService;
import com.openexchange.folderstorage.internal.FolderServiceImpl;
import com.openexchange.folderstorage.mail.osgi.MailFolderStorageActivator;
import com.openexchange.folderstorage.messaging.osgi.MessagingFolderStorageActivator;
import com.openexchange.folderstorage.outlook.osgi.OutlookFolderStorageActivator;
import com.openexchange.folderstorage.virtual.osgi.VirtualFolderStorageActivator;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.osgi.Tools;
import com.openexchange.principalusecount.PrincipalUseCountService;
import com.openexchange.session.UserAndContext;
import com.openexchange.share.ShareService;
import com.openexchange.share.groupware.ModuleSupport;
import com.openexchange.share.notification.ShareNotificationService;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link FolderStorageActivator} - {@link BundleActivator Activator} for folder storage framework.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FolderStorageActivator implements BundleActivator {

    private static final class DisplayNameFolderField implements AdditionalFolderField {

        private final Cache<UserAndContext, String> cache;

        protected DisplayNameFolderField() {
            super();
            cache = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();
        }

        @Override
        public Object renderJSON(AJAXRequestData requestData, final Object value) {
            return value == null ? JSONObject.NULL : value;
        }

        @Override
        public Object getValue(final Folder folder, final ServerSession session) {
            final int createdBy = folder.getCreatedBy();
            if (createdBy <= 0) {
                return JSONObject.NULL;
            }

            Context context = session.getContext();
            UserAndContext key = UserAndContext.newInstance(createdBy, context.getContextId());
            String displayName = cache.getIfPresent(key);
            try {
                if (null != displayName) {
                    return displayName;
                }
                displayName = UserStorage.getInstance().getUser(createdBy, context).getDisplayName();
                if (displayName != null) {
                    cache.put(key, displayName);
                }
                return displayName;
            } catch (OXException e) {
                return null;
            }
        }

        @Override
        public String getColumnName() {
            return "com.openexchange.folderstorage.displayName";
        }

        @Override
        public int getColumnID() {
            return 3030;
        }

        @Override
        public List<Object> getValues(final List<Folder> folder, final ServerSession session) {
            return AdditionalFieldsUtils.bulk(this, folder, session);
        }

    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FolderStorageActivator.class);

    private List<ServiceRegistration<?>> serviceRegistrations;

    private List<ServiceTracker<?, ?>> serviceTrackers;

    private List<BundleActivator> activators;

    /**
     * Initializes a new {@link FolderStorageActivator}.
     */
    public FolderStorageActivator() {
        super();
    }

    private static final Class<?>[] TRACKED_SERVICES = new Class<?>[] {
        ShareService.class,
        ShareNotificationService.class,
        ModuleSupport.class,
        UserService.class,
        DatabaseService.class,
        UserPermissionService.class,
        ObjectUseCountService.class,
        HideAdminService.class,
        PrincipalUseCountService.class,
        ConfigViewFactory.class,
        LeanConfigurationService.class
    };

    @Override
    public void start(final BundleContext context) throws Exception {
        try {
            // Register error component
            // Register services
            serviceRegistrations = new ArrayList<ServiceRegistration<?>>(4);
            // Register folder service
            serviceRegistrations.add(context.registerService(FolderService.class.getName(), new FilteringFolderService(new FolderServiceImpl()), null));
            serviceRegistrations.add(context.registerService(
                ContentTypeDiscoveryService.class.getName(),
                ContentTypeRegistry.getInstance(),
                null));
            serviceRegistrations.add(context.registerService(AdditionalFolderField.class.getName(), new DisplayNameFolderField(), null));
            {
                ForcedReloadable reloadable = new ForcedReloadable() {

                    @Override
                    public void reloadConfiguration(ConfigurationService configService) {
                        ConfiguredDefaultPermissions.getInstance().invalidateCache();
                    }

                    @Override
                    public Interests getInterests() {
                        return null;
                    }
                };
                serviceRegistrations.add(context.registerService(ForcedReloadable.class, reloadable, null));
            }
            // Register service trackers
            serviceTrackers = new ArrayList<ServiceTracker<?, ?>>(2);
            serviceTrackers.add(new ServiceTracker<FolderStorage, FolderStorage>(
                context,
                FolderStorage.class.getName(),
                new FolderStorageTracker(context)));

            FolderStorageServices services = FolderStorageServices.init(context, TRACKED_SERVICES);
            serviceTrackers.add(new ServiceTracker<>(context, Tools.generateServiceFilter(context, TRACKED_SERVICES), services));
            for (final ServiceTracker<?, ?> serviceTracker : serviceTrackers) {
                serviceTracker.open();
            }

            // Start other activators
            activators = new ArrayList<BundleActivator>(9);
            activators.add(new DatabaseFolderStorageActivator()); // Database impl
            activators.add(new MailFolderStorageActivator()); // Mail impl
            activators.add(new MessagingFolderStorageActivator()); // Messaging impl
            activators.add(new FileStorageFolderStorageActivator()); // File storage impl
            activators.add(new CalendarFolderStorageActivator()); // Calendar storage impl
            activators.add(new AddressbookFolderStorageActivator()); // Contacts storage impl
            activators.add(new CacheFolderStorageActivator()); // Cache impl
            activators.add(new OutlookFolderStorageActivator()); // MS Outlook storage activator
            activators.add(new VirtualFolderStorageActivator()); // Virtual storage activator
            BundleActivator activator = null;
            for (final Iterator<BundleActivator> iter = activators.iterator(); iter.hasNext();) {
                try {
                    if (isBundleResolved(context)) {
                        if (null != activator) {
                            logFailedStartup(activator);
                        }
                        return;
                    }
                } catch (IllegalStateException e) {
                    if (null != activator) {
                        logFailedStartup(activator);
                    }
                    return;
                }
                activator = iter.next();
                activator.start(context);
            }

            LOG.info("Bundle \"com.openexchange.folderstorage\" successfully started!");
        } catch (Exception e) {
            LOG.error("", e);
            throw e;
        }
    }

    private static boolean isBundleResolved(final BundleContext context) {
        return Bundle.RESOLVED == context.getBundle().getState();
    }

    private static void logFailedStartup(final BundleActivator activator) {
        final StringBuilder sb = new StringBuilder(32);
        sb.append("Failed start of folder storage bundle \"");
        sb.append(activator.getClass().getName());
        sb.append("\"!");
        LOG.error(sb.toString(), new Throwable());
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        try {
            // Drop activators
            if (null != activators) {
                for (final BundleActivator activator : activators) {
                    activator.stop(context);
                }
                activators.clear();
                activators = null;
            }
            // Drop service trackers
            if (null != serviceTrackers) {
                for (final ServiceTracker<?, ?> serviceTracker : serviceTrackers) {
                    serviceTracker.close();
                }
                serviceTrackers.clear();
                serviceTrackers = null;
            }
            // Unregister previously registered services
            if (null != serviceRegistrations) {
                for (final ServiceRegistration<?> serviceRegistration : serviceRegistrations) {
                    serviceRegistration.unregister();
                }
                serviceRegistrations.clear();
                serviceRegistrations = null;
            }
            // Unregister previously registered component

            LOG.info("Bundle \"com.openexchange.folderstorage\" successfully stopped!");
        } catch (Exception e) {
            LOG.error("", e);
            throw e;
        }
    }

}
