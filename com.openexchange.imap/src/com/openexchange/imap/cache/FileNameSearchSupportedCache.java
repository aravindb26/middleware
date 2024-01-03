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

package com.openexchange.imap.cache;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import javax.mail.MessagingException;
import org.jctools.maps.NonBlockingHashMap;
import com.openexchange.exception.OXException;
import com.openexchange.imap.IMAPCapabilities;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.mime.MimeMailException;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link FileNameSearchSupportedCache} - A cache to check if file name search is supported by an IMAP server.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FileNameSearchSupportedCache {

    private static final AtomicReference<ConcurrentMap<InetSocketAddress, Future<Boolean>>> MAP_REF = new AtomicReference<>();

    /**
     * Initializes a new {@link FileNameSearchSupportedCache}.
     */
    private FileNameSearchSupportedCache() {
        super();
    }

    /**
     * Initializes this cache.
     */
    public static void init() {
        initElseGet();
    }

    private static ConcurrentMap<InetSocketAddress, Future<Boolean>> initElseGet() {
        ConcurrentMap<InetSocketAddress, Future<Boolean>> map = new NonBlockingHashMap<>();
        ConcurrentMap<InetSocketAddress, Future<Boolean>> witness = MAP_REF.compareAndExchange(null, map);
        return witness == null ? map : witness;
    }

    /**
     * Tear-down for this cache.
     */
    public static void tearDown() {
        ConcurrentMap<InetSocketAddress, Future<Boolean>> map = MAP_REF.getAndSet(null);
        if (map != null) {
            map.clear();
        }
    }

    /**
     * Clears this cache.
     */
    public static void clear() {
        ConcurrentMap<InetSocketAddress, Future<Boolean>> map = MAP_REF.get();
        if (map != null) {
            map.clear();
        }
    }

    /**
     * Checks if file name search is supported by given IMAP server.
     * <p>
     * Only call this method if it is ensured that IMAP server does not advertise <code>"SEARCH=X-MIMEPART"</code> capability.
     *
     * @param imapConfig The IMAP configuration
     * @param imapStore The connected IMAP store
     * @return <code>true</code> if file name search is supported; otherwise <code>false</code>
     * @throws OXException If a mail error occurs
     */
    public static boolean isFileNameSearchSupported(IMAPConfig imapConfig, IMAPStore imapStore) throws OXException {
        ConcurrentMap<InetSocketAddress, Future<Boolean>> map = MAP_REF.get();
        Future<Boolean> f = map.get(imapConfig.getImapServerSocketAddress());
        if (null == f) {
            final FutureTask<Boolean> ft = new FutureTask<Boolean>(new FileNameSearchSupportedCallable(imapStore));
            f = map.putIfAbsent(imapConfig.getImapServerSocketAddress(), ft);
            if (null == f) {
                f = ft;
                ft.run();
            }
        }
        try {
            return f.get().booleanValue();
        } catch (InterruptedException e) {
            // Keep interrupted status
            Thread.currentThread().interrupt();
            throw MailExceptionCode.INTERRUPT_ERROR.create(e, e.getMessage());
        } catch (CancellationException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof MessagingException) {
                throw MimeMailException.handleMessagingException((MessagingException) cause, imapConfig);
            }
            if (cause instanceof RuntimeException) {
                throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Not unchecked", cause);
        }
    }

    private static final class FileNameSearchSupportedCallable implements Callable<Boolean> {

        private final IMAPStore imapStore;

        FileNameSearchSupportedCallable(IMAPStore imapStore) {
            super();
            this.imapStore = imapStore;
        }

        @Override
        public Boolean call() throws Exception {
            if (imapStore.getCapabilities().containsKey(IMAPCapabilities.CAP_SEARCH_FILENAME)) {
                // "SEARCH=X-MIMEPART" capability already announced by IMAP server
                return Boolean.TRUE;
            }

            // Probe for it...
            return Boolean.valueOf(IMAPCommandsCollection.supportsFileNameSearch(imapStore));
        }
    }

}
