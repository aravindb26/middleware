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

package com.openexchange.file.storage.mail.osgi;

import org.slf4j.Logger;
import com.openexchange.ajax.customizer.file.AdditionalFileField;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.mail.MailDriveFileStorageService;
import com.openexchange.file.storage.mail.MailMetadataField;
import com.openexchange.file.storage.mail.filter.MailDriveFileFilter;
import com.openexchange.file.storage.mail.settings.AbstractMailDriveSetting;
import com.openexchange.file.storage.mail.settings.AllAttachmentsFolder;
import com.openexchange.file.storage.mail.settings.ReceivedAttachmentsFolder;
import com.openexchange.file.storage.mail.settings.SentAttachmentsFolder;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.jslob.ConfigTreeEquivalent;
import com.openexchange.mime.MimeTypeMap;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.user.UserService;

/**
 * {@link MailDriveActivator} - Activator for Mail Drive bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.2
 */
public final class MailDriveActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link MailDriveActivator}.
     */
    public MailDriveActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { MimeTypeMap.class, ConfigViewFactory.class, UserService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        Logger logger = org.slf4j.LoggerFactory.getLogger(MailDriveActivator.class);
        try {
            Services.setServices(this);

            track(MailDriveFileFilter.class, new MailDriveFileFilterTracker(context));
            openTrackers();

            MailDriveFileStorageService service = MailDriveFileStorageService.newInstance();

            registerService(FileStorageService.class, service);

            AbstractMailDriveSetting setting = new AllAttachmentsFolder(service);
            registerService(PreferencesItemService.class, setting);
            registerService(ConfigTreeEquivalent.class, setting);

            setting = new ReceivedAttachmentsFolder(service);
            registerService(PreferencesItemService.class, setting);
            registerService(ConfigTreeEquivalent.class, setting);

            setting = new SentAttachmentsFolder(service);
            registerService(PreferencesItemService.class, setting);
            registerService(ConfigTreeEquivalent.class, setting);
            /*
             * register an additional file field providing additional mail metadata
             */
            registerService(AdditionalFileField.class, new MailMetadataField());
        } catch (Exception e) {
            logger.error("", e);
            throw e;
        }
    }

    @Override
    public <S> void registerService(final Class<S> clazz, final S service) {
        super.registerService(clazz, service);
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        Services.setServices(null);
    }

}
