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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * {@link S3OperationClosure} - Executes one or more operations using given S3 client.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @param <R> The result type
 */
public interface S3OperationClosure<R> {

    /**
     * Performs the desired operation(s) using given S3 client.
     *
     * @param s3Client The S3 client to use
     * @return The result
     * @throws SdkClientException If any errors are encountered in the client while making the request or handling the response
     * @throws AmazonServiceException If any errors occurred in Amazon S3 while processing the request
     */
    R perform(AmazonS3Client s3Client) throws SdkClientException, AmazonServiceException;
}
