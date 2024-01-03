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

package com.openexchange.pubsub;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import com.openexchange.exception.OXException;

/**
 * {@link Channel} - A typed cache.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public interface Channel<D> {

    /**
     * Gets the unique identifier of this service instance in cluster.
     *
     * @return The unique service instance identifier
     */
    UUID getInstanceId();

    /**
     * Starts a new message collection having given optional message as initial element.
     * <p>
     * A message collection is meant to collect messages associated with a certain identifier and to "release" those message later on.
     * Releasing a message collection means to publish its contained messages as a batch operation.
     *
     * @param optionalInitialMessage The optional message that is initially contained
     * @return The identifier of the newly started message collection
     * @throws OXException If starting a message collection fails
     */
    MessageCollectionId startMessageCollection(Optional<D> optionalInitialMessage) throws OXException;

    /**
     * Adds given message to denoted message collection.
     *
     * @param message The message to add
     * @param collectionId The identifier of the message collection
     * @throws OXException If no such collection exists
     */
    void addToMessageCollection(D message, MessageCollectionId collectionId) throws OXException;

    /**
     * Releases denoted message collection.
     * <p>
     * All contained messages are published to channel.
     *
     * @param collectionId The identifier of the message collection
     * @throws OXException If no such collection exists
     */
    void releaseMessageCollection(MessageCollectionId collectionId) throws OXException;

    /**
     * Drops denoted message collection.
     * <p>
     * All contained messages are discarded.
     *
     * @param collectionId The identifier of the message collection
     * @throws OXException If dropping the message collection fails
     */
    void dropMessageCollection(MessageCollectionId collectionId) throws OXException;

    /**
     * Publishes given messages.
     *
     * @param key The key
     * @param messages The messages
     * @throws OXException If publishing messages fails
     * @throws IllegalArgumentException If either of message is <code>null</code>
     */
    default void publish(D message) throws OXException {
        if (message != null) {
            publish(Collections.singleton(message));
        }
    }

    /**
     * Publishes given messages.
     *
     * @param key The key
     * @param messages The messages
     * @throws OXException If publishing messages fails
     * @throws IllegalArgumentException If either of message is <code>null</code>
     */
    void publish(Collection<D> messages) throws OXException;

    /**
     * Subscribes given listener to this channel.
     *
     * @param listener The listener to subscribe
     * @throws OXException If subscription fails
     */
    void subscribe(ChannelListener<D> listener) throws OXException;

    /**
     * Un-Subscribes given listener to this channel.
     *
     * @param listener The listener to un-subscribe
     * @throws OXException If un-subscription fails
     */
    void unsubscribe(ChannelListener<D> listener) throws OXException;
}
