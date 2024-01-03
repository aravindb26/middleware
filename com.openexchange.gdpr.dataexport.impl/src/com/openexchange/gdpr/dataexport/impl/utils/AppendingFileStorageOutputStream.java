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

package com.openexchange.gdpr.dataexport.impl.utils;

import static com.openexchange.groupware.upload.impl.UploadUtility.getSize;
import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.filestore.FileStorage;
import com.openexchange.java.Streams;

/**
 * {@link AppendingFileStorageOutputStream}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class AppendingFileStorageOutputStream extends OutputStream {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AppendingFileStorageOutputStream.class);
    }

    /** The default in-memory threshold of 500 KB. */
    public static final int DEFAULT_IN_MEMORY_THRESHOLD = 500 * 1024; // 500KB

    private final FileStorage fileStorage;
    private String fileStorageLocation;
    private final byte buf[];
    private int bufpos;
    private long bytesWritten;

    /**
     * Initializes a new {@link AppendingFileStorageOutputStream} with an internal buffer size of 500 KB.
     *
     * @param fileStorage The file storage to write to
     */
    public AppendingFileStorageOutputStream(FileStorage fileStorage) {
        this(DEFAULT_IN_MEMORY_THRESHOLD, fileStorage);
    }

    /**
     * Initializes a new {@link AppendingFileStorageOutputStream}.
     *
     * @param size The buffer size
     * @param fileStorage The file storage to write to
     */
    public AppendingFileStorageOutputStream(int size, FileStorage fileStorage) {
        super();
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.fileStorage = fileStorage;
        buf = new byte[size];
        fileStorageLocation = null;
    }

    /** Flush the internal buffer to file storage location */
    private void flushBufferToFileStorage() throws IOException {
        if (bufpos > 0) {
            boolean retry = true;
            int retryCount = 0;
            int maxRetries = 5;
            do {
                try {
                    if (fileStorageLocation == null) {
                        fileStorageLocation = fileStorage.saveNewFile(Streams.newByteArrayInputStream(buf, 0, bufpos));
                        bytesWritten = bufpos;
                    } else {
                        fileStorage.appendToFile(Streams.newByteArrayInputStream(buf, 0, bufpos), fileStorageLocation, bytesWritten);
                        bytesWritten += bufpos;
                    }
                    bufpos = 0;
                    retry = false;
                } catch (Exception e) {
                    if (retryCount++ >= maxRetries || (ExceptionUtils.isEitherOf(e, IOException.class) == false)) {
                        LoggerHolder.LOG.error("Failed flushing buffer ({}) to {} file during data export after {} attempts.", getSize(bufpos, 2, false, true), fileStorageLocation == null ? "new" : "existent", I(retryCount), e);
                        IOException ioe = ExceptionUtils.extractFrom(e, IOException.class);
                        throw ioe != null ? ioe : new IOException(e);
                    }

                    // A timeout while connecting to an HTTP server or waiting for an available connection from an HttpConnectionManager
                    LoggerHolder.LOG.info("Could not flush buffer ({}) to {} file during data export on {}. attempt due to an I/O error (\"{}\"). Retrying...", getSize(bufpos, 2, false, true), fileStorageLocation == null ? "new" : "existent", I(retryCount), e.getMessage());

                    // Retry using exponential back-off...
                    exponentialBackoffWait(retryCount, 1000L);
                }
            } while (retry);
        }
    }

    /**
     * Performs a wait according to exponential back-off strategy.
     * <pre>
     * (retry-count * base-millis) + random-millis
     * </pre>
     *
     * @param retryCount The current number of retries
     * @param baseMillis The base milliseconds
     */
    private static void exponentialBackoffWait(int retryCount, long baseMillis) {
        long nanosToWait = TimeUnit.NANOSECONDS.convert((retryCount * baseMillis) + ((long) (Math.random() * baseMillis)), TimeUnit.MILLISECONDS);
        LockSupport.parkNanos(nanosToWait);
    }

    /**
     * Gets the file storage location
     *
     * @return The file storage location
     */
    public synchronized Optional<String> getFileStorageLocation() {
        return Optional.ofNullable(fileStorageLocation);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (bufpos >= buf.length) {
            flushBufferToFileStorage();
        }
        buf[bufpos++] = (byte)b;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        int toTransfer = len;
        int offset = off;
        while (toTransfer > 0) {
            int bufcapacity = buf.length - bufpos;
            if (bufcapacity <= 0) {
                flushBufferToFileStorage();
            } else {
                int toCopy = (bufcapacity <= toTransfer) ? bufcapacity : toTransfer;
                System.arraycopy(b, offset, buf, bufpos, toCopy);
                bufpos += toCopy;
                offset += toCopy;
                toTransfer -= toCopy;
            }
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        flushBufferToFileStorage();
    }

    @Override
    public synchronized void close() throws IOException {
        flushBufferToFileStorage();
        super.close();
    }

}
