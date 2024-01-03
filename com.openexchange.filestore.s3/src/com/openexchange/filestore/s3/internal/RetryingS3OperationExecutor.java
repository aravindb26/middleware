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

package com.openexchange.filestore.s3.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * {@link RetryingS3OperationExecutor} - Executes a given S3 closure while performing a retry on timeout issues.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RetryingS3OperationExecutor<R> {

    private final S3OperationClosure<R> closure;
    private final int maxExecutionCount;

    /**
     * Initializes a new {@link RetryingS3OperationExecutor}.
     *
     * @param closure The closure to invoke
     * @param maxExecutionCount The number of execution attempts
     */
    public RetryingS3OperationExecutor(S3OperationClosure<R> closure, int maxExecutionCount) {
        super();
        this.closure = closure;
        this.maxExecutionCount = maxExecutionCount;
    }

    /**
     * Executes given S3 closure while performing a retry on timeout issues.
     *
     * @param s3Client The S3 client to use
     * @return The result
     * @throws SdkClientException If any errors are encountered in the client while making the request or handling the response
     * @throws AmazonServiceException If any errors occurred in Amazon S3 while processing the request
     */
    public R execute(AmazonS3Client s3Client) throws SdkClientException, AmazonServiceException {
        int executionCount = 1;
        do {
            try {
                return closure.perform(s3Client);
            } catch (AmazonServiceException e) {
                throw e;
            } catch (SdkClientException e) {
                Throwable cause = e.getCause();
                if (cause instanceof org.apache.http.conn.ConnectionPoolTimeoutException && ++executionCount <= maxExecutionCount) {
                    // Retry... wait with exponential back-off
                    int retryCount = executionCount - 1;
                    long nanosToWait = TimeUnit.NANOSECONDS.convert((retryCount * 1000) + ((long) (Math.random() * 1000)), TimeUnit.MILLISECONDS);
                    LockSupport.parkNanos(nanosToWait);
                } else {
                    throw e;
                }
            }
        } while (true);
    }
}
