/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package javax.mail.util;


import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loader utility class for {@link StreamProvider}.
 */
public final class StreamProviderLoader {

    /**
     * Initializes a new {@link StreamProviderLoader}.
     */
    private StreamProviderLoader() {
        super();
    }

    private static final AtomicReference<StreamProvider> STREAM_PROVIDER_REFERENCE = new AtomicReference<StreamProvider>(null);

    /**
     * Creates a stream provider object. The provider is loaded using the
     * {@link ServiceLoader#load(Class)} method. If there are no available
     * service providers, this method throws an IllegalStateException.
     * Users are recommended to cache the result of this method.
     *
     * @return a stream provider
     */
    public static StreamProvider provider() {
        StreamProvider streamProvider = STREAM_PROVIDER_REFERENCE.get();
        if (streamProvider == null) {
            synchronized (StreamProvider.class) {
                streamProvider = STREAM_PROVIDER_REFERENCE.get();
                if (streamProvider == null) {
                    streamProvider = doProvider();
                    STREAM_PROVIDER_REFERENCE.set(streamProvider);
                }
            }
        }
        return streamProvider;
    }

    private static StreamProvider doProvider() {
        try {
            if (System.getSecurityManager() != null) {
                return AccessController.doPrivileged(new PrivilegedAction<StreamProvider>() {
                    @Override
                    public StreamProvider run() {
                        return FactoryFinder.find(StreamProvider.class);
                    }
                });
            } else {
                return FactoryFinder.find(StreamProvider.class);
            }
        } catch (IllegalStateException e) {
            return new com.sun.mail.util.MailStreamProvider();
        }
    }

}
