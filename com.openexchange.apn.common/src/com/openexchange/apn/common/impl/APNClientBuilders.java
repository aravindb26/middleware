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
package com.openexchange.apn.common.impl;

import java.util.Optional;
import com.openexchange.apn.common.APNClient;

/**
 * {@link APNClientBuilders} is a factory for {@link APNClient} objects.
 * It supports two builder pattern for certificate based and private key based providers respectively.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class APNClientBuilders {

    /**
     * Creates a new {@link JWTBuilder} which helps to create private key based {@link APNClient}
     *
     * @param topic The topic of the {@link APNClient}
     * @return The {@link JWTBuilder}
     */
    public static JWTBuilder jwt(String topic) {
        return new JWTBuilder(topic);
    }

    /**
     * Creates a new {@link CertificateBuilder} which helps to create certificate based {@link APNClient}
     *
     * @param topic The topic of the {@link APNClient}
     * @return The {@link CertificateBuilder}
     */
    public static CertificateBuilder certificate(String topic) {
        return new CertificateBuilder(topic);
    }

    /**
     * {@link APNClientBuilder}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8.0.0
     */
    @SuppressWarnings("unchecked")
    private static abstract class APNClientBuilder<B extends APNClientBuilder<B>> {


        final String topic;
        boolean isProduction = true;

        String clientId;

        /**
         * Initializes a new {@link APNClientBuilder}.
         *
         * @param topic The app topic
         */
        public APNClientBuilder(String topic) {
            this.topic = topic;
        }

        /**
         * Sets the production
         *
         * @param isProduction whether or not to use production servers
         * @return this
         */
        public B withProduction(boolean isProduction) {
            this.isProduction = isProduction;
            return (B) this;

        }

        /**
         * Sets the client id
         *
         * @param clientId The client id to set
         * @return this
         */
        public B withClientId(String clientId) {
            this.clientId = clientId;
            return (B) this;
        }

    }

    /**
     * {@link JWTBuilder} is a builder which creates {@link APNClient} based on a private key
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8.0.0
     */
    public static class JWTBuilder extends APNClientBuilder<JWTBuilder> {

        private Object privateKey;
        private String keyId;
        private String teamId;

        /**
         * Initializes a new {@link JWTBuilder}.
         *
         * @param topic The app topic
         */
        public JWTBuilder(String topic) {
            super(topic);
        }

        /**
         * Sets the private key
         *
         * byte[], File and InputStreams are all acceptable
         *
         * @param privateKey The private key
         * @return this
         */
        public JWTBuilder withPrivateKey(Object privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        /**
         * Sets the key id
         *
         * @param keyId The key id to set
         * @return this
         */
        public JWTBuilder withKeyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        /**
         * Sets the team id
         *
         * @param teamId The team id to set
         * @return this
         */
        public JWTBuilder withTeamId(String teamId) {
            this.teamId = teamId;
            return this;
        }

        /**
         * Builds the new {@link APNClient}
         *
         * @return The new {@link APNClient}
         */
        public APNClient build() {
            // @formatter:off
            return new JWTBasedApnsClientProvider(Optional.ofNullable(clientId),
                                      Optional.ofNullable(topic),
                                      isProduction,
                                      privateKey,
                                      keyId,
                                      teamId);
            // @formatter:on
        }
    }

    /**
     * {@link CertificateBuilder} is a builder which creates {@link APNClient} based on a certificate
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8.0.0
     */
    public static class CertificateBuilder extends APNClientBuilder<CertificateBuilder> {

        private Object keystore;
        private String password;

        /**
         * Initializes a new {@link CertificateBuilder}.
         *
         * @param topic The app topic
         */
        public CertificateBuilder(String topic) {
            super(topic);
        }

        /**
         * Sets the keystore
         *
         * byte[], File and InputStreams are all acceptable
         *
         * @param keystore The keystore to set
         * @return this
         */
        public CertificateBuilder withKeystore(Object keystore) {
            this.keystore = keystore;
            return this;
        }

        /**
         * Sets the keystore password
         *
         * @param password The password to set
         * @return this
         */
        public CertificateBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Builds a new {@link APNClient}
         *
         * @return the new {@link APNClient}
         */
        public APNClient build() {
            // @formatter:off
            return new CertificateBasedApnsProvider(Optional.ofNullable(clientId),
                                              Optional.ofNullable(topic),
                                              isProduction,
                                              keystore,
                                              password);
            // @formatter:on
        }
    }

}
