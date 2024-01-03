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
package com.openexchange.keystore.k8s.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.WatcherException;

/**
 * {@link AbstractKeyStoreWatcher}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public abstract class AbstractKeyStoreWatcher implements Closeable {

    private final DefaultKubernetesClient client;
    private final String label;
    private final AtomicReference<Watch> secretWatchReference;

    private static final class LoggerHolder {
        public static final Logger LOG = LoggerFactory.getLogger(AbstractKeyStoreWatcher.class);
    }

    /**
     * Initializes a new {@link AbstractKeyStoreWatcher}.
     *
     * @param label The label to watch
     */
    public AbstractKeyStoreWatcher(String label) {
        super();
        client = new DefaultKubernetesClient();
        secretWatchReference = new AtomicReference<Watch>(null);
        this.label = label;
        try {
            start();
        } catch (KubernetesClientException e) {
            // Should not happen in an actual cluster
            LoggerHolder.LOG.warn("Error starting watcher for k8s keystore service, changes in k8s secrets won't be tracked.", e);
        }
    }

    /**
     * Starts the watcher
     */
    private void start() {
        Watch secretWatch = secretWatchReference.get();
        if (secretWatch == null) {
            synchronized (this) {
                secretWatch = secretWatchReference.get();
                if (secretWatch == null) {
                    // @formatter:off
                    secretWatch = client.secrets()
                        .withLabel(label)
                        .watch(new Watcher<Secret>() {
                              @Override
                              public void eventReceived(Action action, Secret secret) {
                                  String id = getIdFromSecret(secret);
                                  LoggerHolder.LOG.debug("Received keystore {} for id {}", action, id);
                                  AbstractKeyStoreWatcher.this.handleChangedKeystore(action, id, secret);
                              }

                              @Override
                              public void onClose(WatcherException cause) {
                                  // Just try a restart
                                  LoggerHolder.LOG.info("Error while watching for secrets: {}", cause.getMessage());
                                  LoggerHolder.LOG.info("Trying to restart secret watcher...");
                                  secretWatchReference.set(null);
                                  start();
                              }
                        });
                    secretWatchReference.set(secretWatch);
                    // @formatter:on
                }
            }
        }
    }

    /**
     * Gets the k8s client
     *
     * @return The client
     */
    protected DefaultKubernetesClient getClient() {
        return client;
    }

    /**
     * Handles changes to the given keystore
     *
     * @param action The action. Is not of type error.
     * @param id The id of the secret
     * @param secret The secret itself
     */
    abstract void handleChangedKeystore(Action action, String id, Secret secret);

    /**
     * Extracts the keystore id from a given {@link Secret}
     *
     * @param secret The secret
     * @return The keystore id
     */
    String getIdFromSecret(Secret secret) {
        return secret.getMetadata().getLabels().get(label);
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
        Watch secretWatch = secretWatchReference.getAndSet(null);
        if (secretWatch != null) {
            secretWatch.close();
        }
    }

}
