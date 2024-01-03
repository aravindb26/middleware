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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.openexchange.apn.common.APNClient;
import com.openexchange.exception.OXException;


/**
 * {@link JWTBasedApnsClientProvider} is a {@link APNClient} which uses a json web token (jwt) to authenticate.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class JWTBasedApnsClientProvider extends AbstractAPNClientImpl {

    private final Object privateKey;
    private final String keyId;
    private final String teamId;

    /**
     * Initializes a new {@link JWTBasedApnsClientProvider}.
     *
     * @param clientId The client id
     * @param topic The app topic
     * @param isProduction whether to use production servers or not
     * @param privateKey The private key. byte[], File and InputStream are accepted
     * @param keyId The key id
     * @param teamId The team id
     */
    public JWTBasedApnsClientProvider(Optional<String> clientId, // @formatter:off
                                      Optional<String> optTopic,
                                      boolean isProduction,
                                      Object privateKey,
                                      String keyId,
                                      String teamId) { // @formatter:on
        super(clientId, optTopic, isProduction);
        // Check parameter are not null
        Objects.requireNonNull(privateKey);
        Objects.requireNonNull(keyId);
        Objects.requireNonNull(teamId);

        this.privateKey = privateKey;
        this.keyId = keyId;
        this.teamId = teamId;
    }

    @Override
    protected ApnsClient createNewApnsClient() throws OXException {
        try {
            ApnsClientBuilder clientBuilder = new ApnsClientBuilder();
            if ((privateKey instanceof File)) {
                clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST).setSigningKey(ApnsSigningKey.loadFromPkcs8File((File) privateKey, teamId, keyId));
            } else if ((privateKey instanceof InputStream)) {
                clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST).setSigningKey(ApnsSigningKey.loadFromInputStream((InputStream) privateKey, teamId, keyId));
            } else {
                clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST).setSigningKey(ApnsSigningKey.loadFromInputStream(new ByteArrayInputStream((byte[]) privateKey), teamId, keyId));
            }
            return clientBuilder.build();
        } catch (NoSuchAlgorithmException e) {
            throw OXException.general("The algorithm used to check the integrity of the keystore for client " + getClientId() + " cannot be found", e);
        } catch (IOException e) {
            throw OXException.general("There is an I/O or format problem with the keystore data of the keystore for client " + getClientId() + " or specified password is invalid", e);
        } catch (InvalidKeyException e) {
            throw OXException.general("Invalid private key specified for client " + getClientId(), e);
        }
    }

}
