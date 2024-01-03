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

package com.openexchange.charset.osgi;

import java.nio.charset.spi.CharsetProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.charset.CharsetService;
import com.openexchange.charset.internal.CharsetServiceImpl;
import com.openexchange.charset.internal.CharsetServiceUtility;
import com.openexchange.charset.internal.CollectionCharsetProvider;
import com.openexchange.charset.internal.ModifyCharsetExtendedProvider;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link CharsetActivator} - Activator for com.openexchange.charset bundle
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CharsetActivator extends HousekeepingActivator implements ServiceTrackerCustomizer<CharsetProvider, CharsetProvider> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CharsetActivator.class);

    private CollectionCharsetProvider collectionCharsetProvider;

    private CharsetProvider backupExtendedCharsetProvider;

    /**
     * Default constructor
     */
    public CharsetActivator() {
        super();
    }

    @Override
    public synchronized CharsetProvider addingService(final ServiceReference<CharsetProvider> reference) {
        CharsetProvider charsetProvider = context.getService(reference);

        CollectionCharsetProvider collectionCharsetProvider = this.collectionCharsetProvider;
        if (null != collectionCharsetProvider) {
            collectionCharsetProvider.addCharsetProvider(charsetProvider);
            LOG.info("New charset provider detected and added: {}", charsetProvider.getClass().getName());
        }

        return charsetProvider;
    }

    @Override
    public void modifiedService(final ServiceReference<CharsetProvider> reference, final CharsetProvider charsetProvider) {
        // Nope
    }

    @Override
    public synchronized void removedService(final ServiceReference<CharsetProvider> reference, final CharsetProvider charsetProvider) {
        CollectionCharsetProvider collectionCharsetProvider = this.collectionCharsetProvider;
        if (null != collectionCharsetProvider) {
            collectionCharsetProvider.removeCharsetProvider(charsetProvider);
            LOG.info("Charset provider removed: {}", charsetProvider.getClass().getName());
        }
        context.ungetService(reference);
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {};
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        LOG.info("starting bundle: com.openexchange.charset");
        try {
            {
                final ModifyCharsetExtendedProvider.Result result = ModifyCharsetExtendedProvider.modifyCharsetExtendedProvider();
                backupExtendedCharsetProvider = result.getBackupCharsetProvider();
                collectionCharsetProvider = result.getCollectionCharsetProvider();
            }
            LOG.info("Extended charset provider replaced with collection charset provider");
            /*
             * Initialize a service tracker to track bundle chars providers
             */
            track(CharsetProvider.class, this);
            openTrackers();
            registerService(CharsetService.class, CharsetServiceImpl.newInstance(CharsetServiceUtility.getStandardProvider(), backupExtendedCharsetProvider));
            LOG.info("Charset bundle successfully started");
        } catch (Throwable t) {
            LOG.error("", t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        }
    }

    @Override
    public synchronized void stopBundle() {
        LOG.info("stopping bundle: com.openexchange.charset");
        try {
            /*
             * Restore original
             */
            if (null != backupExtendedCharsetProvider) {
                ModifyCharsetExtendedProvider.restoreCharsetExtendedProvider(backupExtendedCharsetProvider);
                backupExtendedCharsetProvider = null;
            }
            collectionCharsetProvider = null;
            LOG.info("Collection charset provider replaced with former extended charset provider. Charset bundle successfully stopped");
            super.stopBundle();
        } catch (Throwable t) {
            LOG.error("", t);
        } finally {
            collectionCharsetProvider = null;
        }
    }

}
