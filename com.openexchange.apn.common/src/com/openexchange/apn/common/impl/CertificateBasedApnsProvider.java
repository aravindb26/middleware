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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.openexchange.apn.common.APNClient;
import com.openexchange.exception.OXException;


/**
 * {@link CertificateBasedApnsProvider} is a {@link APNClient} which uses a certificate to authenticate.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class CertificateBasedApnsProvider extends AbstractAPNClientImpl {

    private final String password;
    private final Object keystore;

    /**
     * Initializes a new {@link CertificateBasedApnsProvider}.
     *
     * @param clientId The client id
     * @param topic The app topic
     * @param isProduction whether to use production servers or not
     * @param keystore The keystore. byte[], File and InputStream are accepted
     * @param password The keystore password
     */
    public CertificateBasedApnsProvider(Optional<String> clientId, // @formatter:off
                                        Optional<String> optTopic,
                                        boolean isProduction,
                                        Object keystore,
                                        String password) { // @formatter:on
        super(clientId, optTopic, isProduction);
        this.keystore = keystore;
        this.password = password;
    }

    @Override
    protected ApnsClient createNewApnsClient() throws OXException {
        try {
            ApnsClientBuilder clientBuilder = new ApnsClientBuilder();
            if ((keystore instanceof File)) {
                File keyStore = (File) keystore;
                clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST).setClientCredentials(keyStore, password);
            } else if ((keystore instanceof InputStream)) {
                InputStream keyStore = (InputStream) keystore;
                clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST).setClientCredentials(keyStore, password);
            } else {
                byte[] keyStore = (byte[]) keystore;
                clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST).setClientCredentials(new ByteArrayInputStream(keyStore), password);
            }
            return clientBuilder.build();
        } catch (FileNotFoundException e) {
            throw OXException.general("No such keystore file for client " + getClientId() + ": " + keystore, e);
        } catch (IOException e) {
            throw OXException.general("There is an I/O or format problem with the keystore data of the keystore for client " + getClientId() + " or specified password is invalid", e);
        }
    }

}
