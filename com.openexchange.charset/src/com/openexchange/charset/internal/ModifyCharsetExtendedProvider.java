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

package com.openexchange.charset.internal;

import java.lang.reflect.Field;
import java.nio.charset.spi.CharsetProvider;
import java.util.NoSuchElementException;

/**
 * {@link ModifyCharsetExtendedProvider} - Modifies the <code>charsetExtendedProvider</code> field in {@link java.nio.charset.Charset}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ModifyCharsetExtendedProvider {

    /**
     * The result for <code>ModifyCharsetExtendedProvider.modifyCharsetExtendedProvider()</code>.
     */
    public static class Result {

        private final CharsetProvider backupCharsetProvider;
        private final CollectionCharsetProvider collectionCharsetProvider;

        /**
         * Initializes a new {@link Result}.
         */
        Result(CharsetProvider backupCharsetProvider, CollectionCharsetProvider collectionCharsetProvider) {
            super();
            this.backupCharsetProvider = backupCharsetProvider;
            this.collectionCharsetProvider = collectionCharsetProvider;
        }

        /**
         * Gets the backup charset provider.
         *
         * @return The backup charset provider
         */
        public CharsetProvider getBackupCharsetProvider() {
            return backupCharsetProvider;
        }

        /**
         * Gets the collection charset provider.
         *
         * @return The collection charset provider
         */
        public CollectionCharsetProvider getCollectionCharsetProvider() {
            return collectionCharsetProvider;
        }

    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static volatile Field extendedProvidersField;

    /**
     * Initializes a new {@link ModifyCharsetExtendedProvider}.
     */
    private ModifyCharsetExtendedProvider() {
        super();
    }

    /**
     * Modifies field <code>java.nio.charset.Charset.extendedProvider</code>
     *
     * @throws NoSuchFieldException If field "extendedProviders" does not exist
     * @throws IllegalAccessException If field "extendedProviders" is not accessible
     * @return An array of {@link CharsetProvider} of length <code>2</code>; the first index is occupied by replaced {@link CharsetProvider}
     *         instance, the second with new instance
     */
    public static Result modifyCharsetExtendedProvider() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        /*
         * Force initialization of Charset.extendedProvider. Otherwise target field "extendedProvider" is not initialized.
         */
        java.nio.charset.Charset.isSupported("X-Unknown-Charset");
        /*
         * Modify java.nio.charset.Charset class
         */
        Class<?> extendedProviderHolderClass = null;
        Class<?>[] declaredClasses = java.nio.charset.Charset.class.getDeclaredClasses();
        for (int i = 0; null == extendedProviderHolderClass && i < declaredClasses.length; i++) {
            Class<?> subclass = declaredClasses[i];
            if (subclass.getCanonicalName().endsWith("ExtendedProviderHolder")) {
                extendedProviderHolderClass = subclass;
            }
        }
        if (null == extendedProviderHolderClass) {
            throw new ClassNotFoundException("java.nio.charset.Charset.ExtendedProviderHolder");
        }
        Field extendedProvidersField = extendedProviderHolderClass.getDeclaredField("extendedProviders");
        extendedProvidersField.setAccessible(true);
        ModifyCharsetExtendedProvider.extendedProvidersField = extendedProvidersField;
        CharsetProvider[] charsetProviders = (CharsetProvider[]) extendedProvidersField.get(null);
        if (null == charsetProviders || 0 == charsetProviders.length) {
            throw new NoSuchElementException("java.nio.charset.Charset.ExtendedProviderHolder.extendedProviders[0]");
        }
        /*
         * replace first charset provider with an adjusted collection charset provider instance
         */
        CharsetProvider backupCharsetProvider = charsetProviders[0];
        CollectionCharsetProvider collectionCharsetProvider = new CollectionCharsetProvider(backupCharsetProvider);
        charsetProviders[0] = collectionCharsetProvider;
        /*
         * return both instances as result
         */
        return new Result(backupCharsetProvider, collectionCharsetProvider);
    }

    /**
     * Restores field <code>java.nio.charset.Charset.extendedProvider</code>
     *
     * @param provider The {@link CharsetProvider} instance to restore to
     * @throws IllegalAccessException If field "extendedProviders" is not accessible
     */
    public static void restoreCharsetExtendedProvider(final CharsetProvider provider) throws IllegalAccessException {
        /*
         * Restore java.nio.charset.Charset class
         */
        Field extendedProvidersField = ModifyCharsetExtendedProvider.extendedProvidersField;
        if (null != extendedProvidersField) {
            /*
             * Assign previously remembered charset provider
             */
            CharsetProvider[] charsetProviders = (CharsetProvider[]) extendedProvidersField.get(null);
            if (null == charsetProviders || 0 == charsetProviders.length) {
                throw new NoSuchElementException("java.nio.charset.Charset.ExtendedProviderHolder.extendedProviders[0]");
            }
            charsetProviders[0] = provider;
            ModifyCharsetExtendedProvider.extendedProvidersField = null;
        }
    }

}
