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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.eatthepath.pushy.apns.ApnsClient;
import com.openexchange.apn.common.APNClient;
import com.openexchange.exception.OXException;

/**
 * {@link AbstractAPNClientImpl} - is an abstract implementation of the {@link APNClient} interface
 *
 * It contains generic config options which are relevant for all {@link APNClient} implementations
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public abstract class AbstractAPNClientImpl implements APNClient {

    private final Optional<String> topic;
    private final boolean production;

    private final Optional<String> clientId;

    private final AtomicReference<ApnsClient> clientReference = new AtomicReference<ApnsClient>();

    /**
     * Initializes a new {@link AbstractAPNClientImpl}.
     *
     * @param optClientId The optional client id
     * @param topic The topic
     * @param isProduction whether to use production servers or not
     */
    protected AbstractAPNClientImpl(Optional<String> optClientId, Optional<String> topic, boolean isProduction) {
        super();
        Objects.requireNonNull(optClientId);
        this.clientId = optClientId;
        this.topic = topic;
        this.production = isProduction;
    }

    /**
     * Whether to use apples production servers or not
     *
     * @return <code>true</code> if productions servers should be used, <code>false</code> otherwise
     */
    protected boolean isProduction() {
        return production;
    }

    /**
     * Gets the topic of the app, which is typically the bundle ID of the app.
     *
     * @return The topic
     */
    @Override
    public Optional<String> optTopic() {
        return topic;
    }

    @Override
    public ApnsClient getApnsClient() throws OXException {
        ApnsClient client = clientReference.get();
        if (null == client) {
            synchronized (this) {
                client = clientReference.get();
                if (null == client) {
                    client = createNewApnsClient();
                    clientReference.set(client);
                }
            }
        }
        return client;
    }

    /**
     * Create a new {@link ApnsClient}
     *
     * @return a new {@link ApnsClient}
     * @throws OXException
     */
    protected abstract ApnsClient createNewApnsClient() throws OXException;

    /**
     * Gets the optional clientId
     *
     * @return The optional clientId
     */
    protected Optional<String> getClientId() {
        return clientId;
    }

}
