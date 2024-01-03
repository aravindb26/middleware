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

package com.openexchange.filestore.s3.internal.config.keystore;

import java.util.Map;
import java.util.Optional;
import com.openexchange.config.lean.Property;

/**
 * {@link KeystoreProviderConfig}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class KeystoreProviderConfig {

    private final String id;
    private final Optional<Property> optKeyStoreIdProperty;
    private final Optional<Property> optKeyStorePathProperty;
    private final Optional<Property> optKeyStorePasswordProperty;

    private final Optional<Property> optKeyStoreTypeProperty;
    private final Optional<String> optKeyStoreType;

    private final Optional<KeyStoreChangeListener> optChangeListener;
    private final Optional<Map<String, String>> optOptionals;
    private final boolean isReloadManually;

    /**
     * Initializes a new {@link KeystoreProviderConfig}.
     *
     * @param id
     * @param optKeystoreIdProperty
     * @param optKeystorePathProperty
     * @param optPasswordProperty
     * @param optTypeProperty
     * @param keystoreType
     * @param changeListener
     * @param optOptionals
     * @param isReloadManually
     */
    KeystoreProviderConfig(String id, // @formatter:off
                           Property optKeystoreIdProperty,
                           Property optKeystorePathProperty,
                           Property optPasswordProperty,
                           Property optTypeProperty,
                           String keystoreType,
                           KeyStoreChangeListener changeListener,
                           Map<String, String> optOptionals,
                           boolean isReloadManually) { // @formatter:on
        super();
        this.id = id;
        this.optKeyStoreIdProperty = Optional.ofNullable(optKeystoreIdProperty);
        this.optKeyStorePathProperty = Optional.ofNullable(optKeystorePathProperty);
        this.optKeyStorePasswordProperty = Optional.ofNullable(optPasswordProperty);
        this.optKeyStoreTypeProperty = Optional.ofNullable(optTypeProperty);
        this.optKeyStoreType = Optional.ofNullable(keystoreType);
        this.optChangeListener = Optional.ofNullable(changeListener);
        this.optOptionals = Optional.ofNullable(optOptionals);
        this.isReloadManually = isReloadManually;
    }

    /**
     * Gets the id
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the keystore id property if available
     *
     * @return The optional keystore id property
     */
    public Optional<Property> optKeyStoreIdProperty() {
        return optKeyStoreIdProperty;
    }

    /**
     * Gets the keystore path property if available
     *
     * @return The optional keystore path property
     */
    public Optional<Property> optKeyStorePathProperty() {
        return optKeyStorePathProperty;
    }

    /**
     * Gets the keystore password property if available
     *
     * @return The optional keystore password property
     */
    public Optional<Property> optKeyStorePasswordProperty() {
        return optKeyStorePasswordProperty;
    }

    /**
     * Gets the keystore store type property if available
     *
     * @return The optional keystore store type property
     */
    public Optional<Property> optKeyStoreTypeProperty() {
        return optKeyStoreTypeProperty;
    }

    /**
     * Gets the keystore type if available
     *
     * @return The optional keystore type
     */
    public Optional<String> optKeyStoreType() {
        return optKeyStoreType;
    }

    /**
     * Gets the change listener if available
     *
     * @return The optional change lister
     */
    public Optional<KeyStoreChangeListener> optChangeListener() {
        return optChangeListener;
    }

    /**
     * Gets the property optionals if available
     *
     * @return The optional optionals
     */
    public Optional<Map<String, String>> optOptionals() {
        return optOptionals;
    }

    /**
     * Whether to reload the keystore manually or not
     *
     * @return <code>true</code> to reload manually, <code>false</code> otherwise
     */
    public boolean isReloadManually() {
        return isReloadManually;
    }

    /**
     * Creates a {@link Builder} for {@link KeystoreProviderConfig}s
     *
     * @param id The keystore id
     * @return The {@link Builder}
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * {@link Builder} - A builder
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8.0.0
     */
    public static class Builder {

        private final String id;
        private Property keyStoreIdProperty = null;
        private Property keyStorePathProperty = null;
        private Property keyStorePasswordProperty = null;
        private Property keyStoreTypeProperty = null;
        private String keyStoreType = null;

        private KeyStoreChangeListener changeListener = null;
        private Map<String, String> optionals = null;
        private boolean isReloadManually = false;

        /**
         * Initializes a new {@link KeystoreProviderConfig.Builder}.
         */
        Builder(String id) {
            super();
            this.id = id;
        }

        /**
         * Sets the property name which holds the keystore id.
         * This id is used to retrieve the keystore from the {@link KeyStoreService}
         *
         * @param keyStoreIdProperty The keyStoreId property
         * @return this
         */
        public Builder withKeyStoreIdProperty(Property keyStoreIdProperty) {
            this.keyStoreIdProperty = keyStoreIdProperty;
            return this;
        }

        /**
         * Sets the property name which holds the path to a local keystore
         *
         * @param keyStorePathProperty The keyStorePath property
         * @return this
         */
        public Builder withKeyStorePathProperty(Property keyStorePathProperty) {
            this.keyStorePathProperty = keyStorePathProperty;
            return this;
        }

        /**
         * Sets the property name which holds the keystore password
         *
         * @param keyStorePasswordProperty The kestore password property
         * @return this
         */
        public Builder withKeyStorePasswordProperty(Property keyStorePasswordProperty) {
            this.keyStorePasswordProperty = keyStorePasswordProperty;
            return this;
        }

        /**
         * Sets the property name which hold the keystore type
         *
         * @param keyStoreTypeProperty The keystore type
         * @return this
         */
        public Builder withKeyStoreTypeProperty(Property keyStoreTypeProperty) {
            this.keyStoreTypeProperty = keyStoreTypeProperty;
            return this;
        }

        /**
         * Sets the keystore type.
         * In case the type is not configurable you can set the type this way.
         *
         * @param keyStoreType The keystore type
         * @return this
         */
        public Builder withKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
            return this;
        }

        /**
         * Sets the {@link KeyStoreChangeListener} for the {@link KeystoreProviderConfig}
         * The listener will be notified in case the keystore changed
         *
         * @param changeListener The change listener
         * @return this
         */
        public Builder withChangeListener(KeyStoreChangeListener changeListener) {
            this.changeListener = changeListener;
            return this;
        }

        /**
         * Sets the optionals for the properties.
         * In case the properties use optionals you can provide mapping here
         *
         *
         * @param optionals The optionals map
         * @return this
         */
        public Builder withOptionals(Map<String, String> optionals) {
            this.optionals = optionals;
            return this;
        }

        /**
         * Normally reload is handled by the {@link KeystoreProviderConfig} implementation itself
         * and the {@link KeyStoreChangeListener} is notified. If for any reason you want to handle
         * this reload yourself you can use this method to deactivate the automatic reload
         *
         * @return this
         */
        public Builder reloadManually() {
            this.isReloadManually = true;
            return this;
        }

        /**
         * Builds the {@link KeystoreProviderConfig}
         *
         * @return the {@link KeystoreProviderConfig}
         */
        public KeystoreProviderConfig build() {
            // @formatter:off
            return new KeystoreProviderConfig(id,
                                              keyStoreIdProperty,
                                              keyStorePathProperty,
                                              keyStorePasswordProperty,
                                              keyStoreTypeProperty,
                                              keyStoreType,
                                              changeListener,
                                              optionals,
                                              isReloadManually);
            // @formatter:on
        }

    }

}
