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

import static com.openexchange.java.Autoboxing.b;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import com.openexchange.apn.common.APNClient;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.keystore.KeyStoreService;
import com.openexchange.push.clients.PushClientFactory;
import com.openexchange.push.clients.PushClientsExceptionCodes;

/**
 * {@link APNClientFactory} is a {@link PushClientFactory} which converts the yml based configuration of a apn client into a {@link APNClient}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class APNClientFactory implements PushClientFactory<APNClient> {

    private final KeyStoreService keyStoreService;

    /**
     * Initializes a new {@link APNClientFactory}.
     *
     * @param keyStoreService
     */
    public APNClientFactory(KeyStoreService keyStoreService) {
        super();
        this.keyStoreService = keyStoreService;
    }

    @Override
    public String getType() {
        return "apn";
    }

    @Override
    public APNClient create(Map<String, Object> config) throws OXException {
        String typeStr = getValue(config, PushClientConfigFields.authtype);
        AuthType authType = AuthType.authTypeFor(typeStr);
        switch (authType) {
            case CERTIFICATE:
                return toCertOptions(config);
            case JWT:
                return toJWTOptions(config);
            case UNKNOWN:
            default:
                throw PushClientsExceptionCodes.INVALID_CONFIGURATION.create("Unknown authentication type '" + typeStr + "'");
        }
    }

    /**
     * Converts the config into a certificate based {@link APNClient}
     *
     * @param config The config to convert
     * @return The certificate based {@link APNClient}
     * @throws OXException in case the {@link APNClient} couldn't be created
     */
    private APNClient toCertOptions(Map<String, Object> config) throws OXException {
        // Get fields from config map
        String keystoreId = getValue(config, PushClientConfigFields.keystoreId);
        String keystorePath = getValue(config, PushClientConfigFields.keystore);
        if (Strings.isEmpty(keystorePath) && Strings.isEmpty(keystoreId)) {
            throw PushClientsExceptionCodes.INVALID_CONFIGURATION.create("Missing keystore with id '" + keystoreId + "'");
        }
        String topic = getValue(config, PushClientConfigFields.topic);
        String password = getValue(config, PushClientConfigFields.password);
        Boolean production = getBooleanValue(config, PushClientConfigFields.production);

        Optional<byte[]> optKeyStore = keyStoreService.optSecret(keystoreId);
        if (optKeyStore.isEmpty() && Strings.isEmpty(keystorePath)) {
            throw PushClientsExceptionCodes.INVALID_CONFIGURATION.create("Missing keystore with id '" + keystoreId + "'");
        }
        // @formatter:off
        return APNClientBuilders.certificate(topic)
                                        .withPassword(password)
                                        .withKeystore(optKeyStore.map(bytes -> (Object) bytes)
                                                                 .orElseGet(() -> new File(keystorePath)))
                                        .withProduction(b(production))
                                        .build();
        // @formatter:on
    }

    /**
     * Converts the config into a jwt based {@link APNClient}
     *
     * @param config The config to convert
     * @return The jwt based {@link APNClient}
     * @throws OXException in case the {@link APNClient} couldn't be created
     */
    private APNClient toJWTOptions(Map<String, Object> config) throws OXException {
        String privateKeyId = getValue(config, PushClientConfigFields.privatekeyId);
        String privateKeyFile = getValue(config, PushClientConfigFields.privatekey);
        String keyId = getValue(config, PushClientConfigFields.keyid);
        String teamId = getValue(config, PushClientConfigFields.teamid);
        String topic = getValue(config, PushClientConfigFields.topic);
        Boolean production = getBooleanValue(config, PushClientConfigFields.production);

        // Try loading via KeyStoreService first
        Optional<byte[]> optKeyStore = keyStoreService.optSecret(privateKeyId);

        try {
            if(optKeyStore.isEmpty() && Strings.isEmpty(privateKeyFile)) {
                throw PushClientsExceptionCodes.INVALID_CONFIGURATION.create("Missing private key with id '" + privateKeyId + "'");
            }
            Object privateKey = optKeyStore.isPresent() ? optKeyStore.get() : Files.readAllBytes(new File(privateKeyFile).toPath());
            // @formatter:off
            return APNClientBuilders.jwt(topic)
                                            .withPrivateKey(privateKey)
                                            .withKeyId(keyId)
                                            .withTeamId(teamId)
                                            .withProduction(b(production))
                                            .build();
            // @formatter:on
        } catch (IOException e) {
            throw PushClientsExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets a string value from the given config map
     *
     * @param config The config
     * @param field The field to get
     * @return The string value
     */
    private String getValue(Map<String, Object> config, PushClientConfigFields field) {
        return (String) config.getOrDefault(field.name(), field.getDefaultValue());
    }

    /**
     * Gets a boolean value from the given config map
     *
     * @param config The config
     * @param field The field to get
     * @return The boolean value
     */
    private Boolean getBooleanValue(Map<String, Object> config, PushClientConfigFields field) {
        return (Boolean) config.getOrDefault(field.name(), field.getDefaultValue());
    }

}
