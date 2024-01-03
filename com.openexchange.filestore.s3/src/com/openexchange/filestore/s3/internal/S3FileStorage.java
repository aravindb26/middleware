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

import static com.openexchange.filestore.s3.internal.AbortIfNotConsumedInputStream.closeContentStream;
import static com.openexchange.filestore.s3.internal.S3ExceptionCode.wrap;
import static com.openexchange.java.Autoboxing.L;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.conditions.BooleanCondition;
import com.amazonaws.auth.policy.conditions.StringCondition;
import com.amazonaws.auth.policy.conditions.StringCondition.StringComparisonType;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.MultiObjectDeleteException.DeleteError;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorage;
import com.openexchange.filestore.FileStorageCodes;
import com.openexchange.filestore.s3.internal.client.S3FileStorageClient;
import com.openexchange.filestore.utils.TempFileHelper;
import com.openexchange.java.Sets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link S3FileStorage}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class S3FileStorage implements FileStorage {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(S3FileStorage.class);

    /** The delimiter character (<code>'/'</code>) to separate the prefix from the keys. */
    private static final String DELIMITER = "/";

    /** The minimum part size required for a multipart upload */
    private static final int MINIMUM_MULTIPART_SIZE = 5242880; /* 5 MB */

    private final URI uri;
    private final String prefix;
    private final String bucketName;
    private final S3FileStorageClient client;

    /**
     * Initializes a new {@link S3FileStorage}.
     *
     * @param uri The URI that fully qualifies this file storage
     * @param prefix The prefix to use; e.g. <code>"1337ctxstore"</code>
     * @param bucketName The bucket name to use
     * @param client The file storage client
     */
    public S3FileStorage(URI uri, String prefix, String bucketName, S3FileStorageClient client) {
        super();
        BucketNameUtils.validateBucketName(bucketName);
        if (Strings.isEmpty(prefix) || prefix.indexOf(DELIMITER) >= 0) {
            throw new IllegalArgumentException(prefix);
        }
        this.uri = uri;
        this.prefix = prefix;
        this.bucketName = bucketName;
        this.client = client;
        LOG.debug("S3 file storage initialized for \"{}/{}{}\"", bucketName, prefix, DELIMITER);
    }

    /**
     * Executes with retry the given closure with configured retry count.
     *
     * @param <R> The result type
     * @param closure The closure to invoke
     * @param client The S3 file storage client to use
     * @return The result
     * @throws SdkClientException If any errors are encountered in the client while making the request or handling the response
     * @throws AmazonServiceException If any errors occurred in Amazon S3 while processing the request
     */
    private void withVoidRetry(S3VoidOperationClosure closure) throws SdkClientException, AmazonServiceException {
        int retryCount = client.getNumOfRetryAttemptyOnConnectionPoolTimeout();
        new RetryingS3OperationExecutor<Void>(closure, retryCount <= 0 ? 1 : retryCount + 1).execute(client.getSdkClient());
    }

    /**
     * Executes with retry the given closure with configured retry count.
     *
     * @param <R> The result type
     * @param closure The closure to invoke
     * @param client The S3 file storage client to use
     * @return The result
     * @throws SdkClientException If any errors are encountered in the client while making the request or handling the response
     * @throws AmazonServiceException If any errors occurred in Amazon S3 while processing the request
     */
    private <R> R withRetry(S3OperationClosure<R> closure) throws SdkClientException, AmazonServiceException {
        int retryCount = client.getNumOfRetryAttemptyOnConnectionPoolTimeout();
        return new RetryingS3OperationExecutor<R>(closure, retryCount).execute(client.getSdkClient());
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public boolean isSpooling() {
        return true;
    }

    @Override
    public String saveNewFile(InputStream input) throws OXException {
        /*
         * perform chunked upload as needed
         */
        String key = generateKey(true);
        S3ChunkedUpload chunkedUpload = null;
        S3UploadChunk chunk = null;
        File tmpFile = null;
        try {
            /*
             * spool to file
             */
            if (!(input instanceof FileInputStream)) {
                Optional<File> optionalTempFile = TempFileHelper.getInstance().newTempFile();
                if (optionalTempFile.isPresent()) {
                    tmpFile = optionalTempFile.get();
                    input = Streams.transferToFileAndCreateStream(input, tmpFile);
                }
            }
            /*
             * proceed
             */
            chunkedUpload = new S3ChunkedUpload(input, client.getEncryptionConfig().isClientEncryptionEnabled(), client.getChunkSize());
            chunk = chunkedUpload.next();
            if (false == chunkedUpload.hasNext()) {
                /*
                 * whole file fits into buffer (this includes a zero byte file), upload directly
                 */
                uploadSingle(key, chunk);
            } else {
                /*
                 * upload in multipart chunks to provide the correct content length
                 */
                InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key).withObjectMetadata(prepareMetadataForSSE(new ObjectMetadata()));
                String uploadID = withRetry(c -> c.initiateMultipartUpload(initiateMultipartUploadRequest).getUploadId());
                boolean completed = false;
                try {
                    Thread currentThread = Thread.currentThread();
                    List<PartETag> partETags = new ArrayList<PartETag>();
                    int partNumber = 1;
                    /*
                     * upload n-1 parts
                     */
                    do {
                        partETags.add(uploadPart(key, uploadID, partNumber++, chunk, false).getPartETag());
                        if (currentThread.isInterrupted()) {
                            throw OXException.general("Upload to S3 aborted");
                        }
                        chunk = chunkedUpload.next();
                    } while (chunkedUpload.hasNext());
                    /*
                     * upload last part & complete upload
                     */
                    partETags.add(uploadPart(key, uploadID, partNumber++, chunk, true).getPartETag());
                    withRetry(c -> c.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, key, uploadID, partETags)));
                    completed = true;
                } finally {
                    if (false == completed) {
                        try {
                            withVoidRetry(c -> c.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, key, uploadID)));
                        } catch (AmazonClientException e) {
                            LOG.warn("Error aborting multipart upload", e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw FileStorageCodes.IOERROR.create(e, e.getMessage());
        } finally {
            Streams.close(chunk, chunkedUpload, input);
            TempFileHelper.deleteQuietly(tmpFile);
        }
        return removePrefix(key);
    }

    @Override
    public InputStream getFile(String name) throws OXException {
        String key = addPrefix(name);
        S3ObjectInputStream objectContent = null;
        try {
            objectContent = getObject(key).getObjectContent();
            InputStream wrapper = wrapperWithoutRangeSupport(objectContent, key);
            objectContent = null; // Avoid premature closing
            return wrapper;
        } catch (AmazonClientException e) {
            throw wrap(e, key);
        } finally {
            closeContentStream(objectContent);
        }
    }

    private InputStream wrapperWithoutRangeSupport(S3ObjectInputStream objectContent, String key) {
        return new ResumableAbortIfNotConsumedInputStream(objectContent, bucketName, key, client.getSdkClient());
    }

    @Override
    public InputStream getFile(String name, long offset, long length) throws OXException {
        // Check validity of given offset/length arguments
        long fileSize = getFileSize(name);
        if (offset >= fileSize || (length >= 0 && length > fileSize - offset)) {
            throw FileStorageCodes.INVALID_RANGE.create(L(offset), L(length), name, L(fileSize));
        }

        // Check for 0 (zero) requested bytes
        if (length == 0) {
            return Streams.EMPTY_INPUT_STREAM;
        }

        // Initialize appropriate Get-Object request
        String key = addPrefix(name);
        GetObjectRequest request = new GetObjectRequest(bucketName, key);
        long rangeEnd = (length > 0 ? (offset + length) : fileSize) - 1;
        request.setRange(offset, rangeEnd);

        // Return content stream
        S3ObjectInputStream objectContent = null;
        try {
            objectContent = withRetry(c -> c.getObject(request).getObjectContent());
            long[] range = new long[] { offset, rangeEnd };
            InputStream wrapper = wrapperWithRangeSupport(objectContent, range, key);
            objectContent = null; // Avoid premature closing
            return wrapper;
        } catch (AmazonClientException e) {
            if ((e instanceof AmazonServiceException) && HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE == ((AmazonServiceException) e).getStatusCode()) {
                throw FileStorageCodes.INVALID_RANGE.create(e, L(offset), L(length), name, L(fileSize));
            }
            throw wrap(e, key);
        } finally {
            closeContentStream(objectContent);
        }
    }

    private InputStream wrapperWithRangeSupport(S3ObjectInputStream objectContent, long[] range, String key) {
        return new RangeAcceptingResumableAbortIfNotConsumedInputStream(objectContent, range, bucketName, key, client.getSdkClient());
    }

    @Override
    public SortedSet<String> getFileList() throws OXException {
        SortedSet<String> files = new TreeSet<String>();
        /*
         * results may be paginated - repeat listing objects as long as result is truncated
         */
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withDelimiter(DELIMITER).withPrefix(new StringBuilder(prefix).append(DELIMITER).toString());
        ObjectListing objectListing;
        do {
            objectListing = withRetry(c -> c.listObjects(listObjectsRequest));
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                files.add(removePrefix(objectSummary.getKey()));
            }
            listObjectsRequest.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());
        return files;
    }

    @Override
    public long getFileSize(final String name) throws OXException {
        return getContentLength(getMetadata(addPrefix(name)));
    }

    @Override
    public String getMimeType(String name) throws OXException {
        //TODO: makes no sense at storage layer
        return getMetadata(addPrefix(name)).getContentType();
    }

    @Override
    public boolean deleteFile(String name) throws OXException {
        String key = addPrefix(name);
        try {
            withVoidRetry(c -> c.deleteObject(bucketName, key));
            return true;
        } catch (AmazonClientException e) {
            throw wrap(e, key);
        }
    }

    /**
     * The max. number of keys that are allowed being passed by a multiple delete objects request.
     * <p>
     * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html">See here</a>
     */
    private static final int MAX_NUMBER_OF_KEYS_TO_DELETE = 1000;

    @Override
    public Set<String> deleteFiles(String[] names) throws OXException {
        if (null == names || 0 >= names.length) {
            return Collections.emptySet();
        }

        Set<String> notDeleted = new HashSet<String>();
        for (String[] partition : Arrays.partition(names, MAX_NUMBER_OF_KEYS_TO_DELETE)) {
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName).withKeys(addPrefix(partition));
            try {
                withRetry(c -> c.deleteObjects(deleteRequest));
            } catch (MultiObjectDeleteException e) {
                List<DeleteError> errors = e.getErrors();
                if (null != errors && !errors.isEmpty()) {
                    for (DeleteError error : errors) {
                        notDeleted.add(removePrefix(error.getKey()));
                    }
                }
            } catch (AmazonClientException e) {
                throw wrap(e);
            }
        }
        return notDeleted;
    }

    @Override
    public void remove() throws OXException {
        try {
            /*
             * try and delete all contained files repeatedly
             */
            final int RETRY_COUNT = 10;
            for (int i = 0; i < RETRY_COUNT; i++) {
                try {
                    SortedSet<String> fileList = getFileList();
                    if (null == fileList || fileList.isEmpty()) {
                        return; // no more files found
                    }

                    for (Set<String> partition : Sets.partition(fileList, MAX_NUMBER_OF_KEYS_TO_DELETE)) {
                        withRetry(c -> c.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(addPrefix(partition))));
                    }
                } catch (MultiObjectDeleteException e) {
                    if (i < RETRY_COUNT - 1) {
                        LOG.warn("Not all files in bucket deleted yet, trying again.", e);
                    } else {
                        throw FileStorageCodes.NOT_ELIMINATED.create(e, "Not all files in bucket deleted after " + i + " tries, giving up.");
                    }
                }
            }
        } catch (OXException e) {
            throw FileStorageCodes.NOT_ELIMINATED.create(e);
        } catch (AmazonClientException e) {
            throw FileStorageCodes.NOT_ELIMINATED.create(wrap(e));
        }
    }

    @Override
    public void recreateStateFile() throws OXException {
        // no
    }

    @Override
    public boolean stateFileIsCorrect() throws OXException {
        return true;
    }


    @Override
    public long appendToFile(InputStream file, String name, long offset) throws OXException {
        /*
         * Get the content length of the object to add the data to
         */
        long contentLength = getContentLength(getMetadata(addPrefix(name)));
        if (contentLength != offset) {
            throw FileStorageCodes.INVALID_OFFSET.create(Long.valueOf(offset), name, Long.valueOf(contentLength));
        }

        File tmpFile = null;
        try {
            /*
             * spool to file
             */
            long t0 = System.nanoTime();
            if (!(file instanceof FileInputStream)) {
                Optional<File> optionalTempFile = TempFileHelper.getInstance().newTempFile();
                if (optionalTempFile.isPresent()) {
                    tmpFile = optionalTempFile.get();
                    file = Streams.transferToFileAndCreateStream(file, tmpFile);
                }
            }
            LOG.trace("Spooling to disk done in {} ms", L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));

            /*-
             * Append the data: CopyPart/UploadPartCopy is preferred, because it does not require to download the S3 object for the
             * append operation. But: UploadPartCopy might not be available/possible in all situations
             */
            return isCopyPartAvailable(contentLength) ? appendToFileWithCopyPart(file, name) : appendToFileWithSpooling(file, name);
        } catch (IOException e) {
            throw FileStorageCodes.IOERROR.create(e, e.getMessage());
        }
        finally {
            Streams.close(file);
            TempFileHelper.deleteQuietly(tmpFile);
        }
    }

    /**
     * Set the new file length of an existing S3 object  (shortens an object)
     * <p>
     * This method uses a <i>UploadPartCopy</i> ({@link CopyPartRequest}) within a multipart upload request in order to create a new temporary S3 object with shortened data.
     * Afterwards the temporary object replaces the original.
     * </p>
     * <p>
     * Due to S3 restrictions, a UploadPartCopy is only possible if the source file, is larger than 5 MB.
     * </p>
     *
     * @param length The new length of object
     * @param name The name of the object
     * @throws OXException
     */
    private void setFileLengthWithCopyPart(long length, String name) throws OXException {
        /*
         * Initialize the multipart upload
         */
        String key = addPrefix(name);
        String tmpKey = generateKey(true);
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, tmpKey).withObjectMetadata(prepareMetadataForSSE(new ObjectMetadata()));
        String uploadID = withRetry(c -> c.initiateMultipartUpload(initiateMultipartUploadRequest).getUploadId());
        boolean uploadCompleted = false;

        try {
            /*
             * Copy the object in parts to a temporary S3 object with the desired length
             */
            long partSize = 5 * 1024 * 1024L;
            long bytePosition = 0;
            int partNum = 1;
            List<PartETag> parts = new ArrayList<PartETag>();
            while (bytePosition < length) {

                // The last part might be smaller than partSize, so check to make sure
                // that lastByte isn't beyond the end of the object.
                long lastByte = Math.min(bytePosition + partSize - 1, length - 1);

                // Copy this part.
                //@formatter:off
                CopyPartRequest copyRequest = new CopyPartRequest()
                    .withSourceBucketName(bucketName)
                    .withSourceKey(key)
                    .withDestinationBucketName(bucketName)
                    .withDestinationKey(tmpKey)
                    .withUploadId(uploadID)
                    .withFirstByte(L(bytePosition))
                    .withLastByte(L(lastByte))
                    .withPartNumber(partNum++);
                //@formatter:on
                CopyPartResult copyPartResult = withRetry(c -> c.copyPart(copyRequest));
                parts.add(copyPartResult.getPartETag());
                bytePosition += partSize;
            }

            /*
             * complete the request
             */
            withRetry(c -> c.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, tmpKey, uploadID, parts)));
            uploadCompleted = true;

            /*
             * replace the original
             */
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, tmpKey, bucketName, key);
            ObjectMetadata metadata = prepareMetadataForSSE(getMetadata(tmpKey).clone());
            copyObjectRequest.setNewObjectMetadata(metadata);
            withRetry(c -> c.copyObject(copyObjectRequest));
            withVoidRetry(c -> c.deleteObject(bucketName, tmpKey));
        } catch (AmazonClientException e) {
            throw wrap(e, key);
        } finally {
            if (uploadCompleted == false) {
                try {
                    withVoidRetry(c -> c.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, tmpKey, uploadID)));
                } catch (AmazonClientException e) {
                    LOG.warn("Error aborting multipart upload", e);
                }
            }
        }
    }

    /**
     * Set the new file length of an existing S3 object  (shortens an object)
     * <p>
     * This method downloads the existing S3 object and uploads it again, up to the desired new length.
     * </p>
     *
     * @param file The data to append as {@link InputStream}
     * @param name The name of the object to append the data to
     * @return The new size of the modified object
     * @throws OXException
     */
    private void setFileLengthWithSpooling(long length, String name) throws OXException {
        /*
         * copy previous file to temporary file
         */
        String key = addPrefix(name);
        String tempKey = generateKey(true);
        try {
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, key, bucketName, tempKey);
            ObjectMetadata metadata = prepareMetadataForSSE(getMetadata(key).clone());
            copyObjectRequest.setNewObjectMetadata(metadata);
            withRetry(c -> c.copyObject(copyObjectRequest));
            /*
             * upload $length bytes from previous file to new current file
             */
            metadata = new ObjectMetadata();
            metadata.setContentLength(length);
            metadata = prepareMetadataForSSE(metadata);
            InputStream inputStream = getFile(tempKey, 0, length);
            try {
                ObjectMetadata meta = metadata;
                withRetry(c -> c.putObject(bucketName, key, inputStream, meta));
            } finally {
                Streams.close(inputStream);
            }
        } catch (AmazonClientException e) {
            throw wrap(e, key);
        } finally {
            try {
                withVoidRetry(c -> c.deleteObject(bucketName, tempKey));
            } catch (AmazonClientException e) {
                LOG.warn("Error cleaning up temporary file", e);
            }
        }
    }

    @Override
    public void setFileLength(long length, String name) throws OXException {
        /*
         * Get the current length of the object
         */
        long contentLength = getContentLength(getMetadata(addPrefix(name)));
        if (contentLength == length) {
            /* the file already has the desired length */
            return;
        }

        /**
         * Set the new file length: CopyPart/UploadPartCopy is preferred, because it does not require
         * to download the S3 object. But: UploadPartCopy might not be available/possible in all situations
         */
        if (isCopyPartAvailable(contentLength)) {
            setFileLengthWithCopyPart(length, name);
        } else {
            setFileLengthWithSpooling(length, name);
        }
    }

    /**
     * Ensures the configured bucket exists, creating it dynamically if needed.
     *
     * @throws OXException If initialization fails
     */
    public void ensureBucket() throws OXException {
        boolean bucketExists = false;
        try {
            bucketExists = withRetry(c -> Boolean.valueOf(c.doesBucketExist(bucketName))).booleanValue();
        } catch (AmazonClientException e) {
            throw S3ExceptionCode.wrap(e);
        } catch (RuntimeException e) {
            throw S3ExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }

        if (false == bucketExists) {
            String region = client.getSdkClient().getRegionName();
            try {
                withRetry(c -> c.createBucket(new CreateBucketRequest(bucketName, Region.fromValue(region))));
                if (client.getEncryptionConfig().isServerSideEncryptionEnabled()) {
                    withVoidRetry(c -> c.setBucketPolicy(new SetBucketPolicyRequest(bucketName, getSSEOnlyBucketPolicy(bucketName))));
                }
            } catch (AmazonS3Exception e) {
                if ("InvalidLocationConstraint".equals(e.getErrorCode())) {
                    // Failed to create such a bucket
                    throw S3ExceptionCode.BUCKET_CREATION_FAILED.create(bucketName, region);
                }
                throw S3ExceptionCode.wrap(e);
            } catch (AmazonClientException e) {
                throw S3ExceptionCode.wrap(e);
            } catch (RuntimeException e) {
                throw S3ExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        }
    }

    /**
     * Gets the bucket policy for a server side encryption only bucket.
     *
     * @param bucketName The name of the bucket
     * @return The encryption only policy
     */
    private static String getSSEOnlyBucketPolicy(String bucketName) {
        Policy bucketPolicy = new Policy().withStatements(
            new Statement(Statement.Effect.Deny)
                .withId("DenyIncorrectEncryptionHeader")
                .withPrincipals(Principal.AllUsers)
                .withActions(S3Actions.PutObject)
                .withResources(new Resource("arn:aws:s3:::" + bucketName + "/*"))
                .withConditions(new StringCondition(StringComparisonType.StringNotEquals, "s3:x-amz-server-side-encryption", "AES256")),
            new Statement(Statement.Effect.Deny)
                .withId("DenyUnEncryptedObjectUploads")
                .withPrincipals(Principal.AllUsers)
                .withActions(S3Actions.PutObject)
                .withResources(new Resource("arn:aws:s3:::" + bucketName + "/*"))
                .withConditions(new BooleanCondition("s3:x-amz-server-side-encryption", true))
                );
        return bucketPolicy.toJson();
    }

    /**
     * Gets metadata for an existing file.
     *
     * @param key The (full) key for the new file; no additional prefix will be prepended implicitly
     * @return The upload ID for the multipart upload
     * @throws OXException
     */
    private ObjectMetadata getMetadata(String key) throws OXException {
        try {
            return withRetry(c -> c.getObjectMetadata(bucketName, key));
        } catch (AmazonClientException e) {
            throw wrap(e, key);
        }
    }

    /**
     * Gets a stored S3 object.
     *
     * @param key The key of the file
     * @return The S3 object
     * @throws OXException
     */
    private S3Object getObject(String key) throws OXException {
        try {
            return withRetry(c -> c.getObject(bucketName, key));
        } catch (AmazonClientException e) {
            throw wrap(e, key);
        }
    }

    /**
     * Creates a new arbitrary key (an unformatted string representation of a new random UUID), optionally prepended with the configured
     * prefix and delimiter.
     *
     * @param withPrefix <code>true</code> to prepend the prefix, <code>false</code>, otherwise
     *
     * @return A new UID string, optionally with prefix and delimiter, e.g. <code>[prefix]/067e61623b6f4ae2a1712470b63dff00</code>.
     */
    private String generateKey(boolean withPrefix) {
        String uuid = UUIDs.getUnformattedString(UUID.randomUUID());
        return withPrefix ? new StringBuilder(prefix).append(DELIMITER).append(uuid).toString() : uuid;
    }

    /**
     * Prepends the configured prefix and delimiter character sequence to the supplied name.
     *
     * @param name The name to prepend the prefix
     * @return The name with prefix
     */
    private String addPrefix(String name) {
        return addPrefix0(name, null);
    }

    /**
     * Prepends the configured prefix and delimiter character sequence to the supplied name.
     *
     * @param name The name to prepend the prefix
     * @param optBuilder The string builder to use or <code>null</code>
     * @return The name with prefix
     */
    private String addPrefix0(String name, StringBuilder optBuilder) {
        StringBuilder sb = optBuilder;
        if (sb == null) {
            sb = new StringBuilder(64);
        } else {
            if (sb.length() > 0) {
                sb.setLength(0);
            }
        }
        return sb.append(prefix).append(DELIMITER).append(name).toString();
    }

    /**
     * Prepends the configured prefix and delimiter character sequence to the supplied names.
     *
     * @param names The names to prepend the prefix
     * @return The names with prefix in an array
     */
    private String[] addPrefix(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return Strings.getEmptyStrings();
        }

        String[] keys = new String[names.size()];
        StringBuilder sb = new StringBuilder(64);
        int i = 0;
        for (String name : names) {
            keys[i++] = addPrefix0(name, sb);
        }
        return keys;
    }

    /**
     * Prepends the configured prefix and delimiter character sequence to the supplied names.
     *
     * @param names The names to prepend the prefix
     * @return The names with prefix in an array
     */
    private String[] addPrefix(String[] names) {
        if (names == null || names.length <= 0) {
            return Strings.getEmptyStrings();
        }

        String[] keys = new String[names.length];
        StringBuilder sb = new StringBuilder(64);
        for (int i = names.length; i-- > 0;) {
            keys[i] = addPrefix0(names[i], sb);
        }
        return keys;
    }

    /**
     * Strips the prefix and delimiter character sequence to the supplied key.
     *
     * @param key The key to strip the prefix from
     * @return The key without prefix
     */
    private String removePrefix(String key) {
        int idx = prefix.length() + DELIMITER.length();
        if (idx > key.length() || false == key.startsWith(new StringBuilder(prefix).append(DELIMITER).toString())) {
            throw new IllegalArgumentException(key);
        }
        return key.substring(idx);
    }

    /**
     * Puts a single upload chunk to amazon s3.
     *
     * @param key The object key
     * @param chunk The chunk to store
     * @return The put object result passed from the client
     */
    private PutObjectResult uploadSingle(String key, S3UploadChunk chunk) throws OXException {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(chunk.getSize());
            metadata.setContentMD5(chunk.getMD5Digest());
            prepareMetadataForSSE(metadata);
            InputStream data = chunk.getData();
            try {
                return withRetry(c -> c.putObject(bucketName, key, data, metadata));
            } finally {
                Streams.close(data);
            }
        } finally {
            Streams.close(chunk);
        }
    }

    private ObjectMetadata prepareMetadataForSSE(ObjectMetadata metadata) {
        if (client.getEncryptionConfig().isServerSideEncryptionEnabled()) {
            metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        }
        return metadata;
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param key The key to store
     * @param uploadID the upload ID
     * @param partNumber The part number of the chunk
     * @param chunk the chunk to store
     * @param lastPart <code>true</code> if this is the last part, <code>false</code>, otherwise
     * @return The put object result passed from the client
     */
    private UploadPartResult uploadPart(String key, String uploadID, int partNumber, S3UploadChunk chunk, boolean lastPart) throws OXException {
        try {
            UploadPartRequest request = new UploadPartRequest()
                                                .withBucketName(bucketName)
                                                .withKey(key)
                                                .withUploadId(uploadID)
                                                .withInputStream(chunk.getData())
                                                .withPartSize(chunk.getSize())
                                                .withPartNumber(partNumber + 1)
                                                .withLastPart(lastPart);
            String md5Digest = chunk.getMD5Digest();
            if (null != md5Digest) {
                request.withMD5Digest(md5Digest);
            }
            return withRetry(c -> c.uploadPart(request));
        } finally {
            Streams.close(chunk);
        }
    }

    /**
     * Extracts the effective content length from the supplied S3 object metadata, which is the length of the unencrypted content if specified, or the plain content length, otherwise.
     *
     * @param metadata The metadata to get the content length from
     * @return The length of the unencrypted content if specified, or the plain content length, otherwise
     */
    private static long getContentLength(ObjectMetadata metadata) throws OXException {
        String unencryptedContentLength = metadata.getUserMetaDataOf(com.amazonaws.services.s3.Headers.UNENCRYPTED_CONTENT_LENGTH);
        if (Strings.isNotEmpty(unencryptedContentLength)) {
            try {
                return Long.parseLong(unencryptedContentLength);
            } catch (NumberFormatException e) {
                throw FileStorageCodes.NO_NUMBER.create(e);
            }
        }
        return metadata.getContentLength();
    }

    /**
     * Checks if "UploadPart" request is possible to use
     *
     * @param name The name of the object to check
     * @param contentLength The content length of the object
     * @return <code>True</code> if the "UploadPart" request can be used, <code>False</code> otherwise
     */
    private boolean isCopyPartAvailable(long contentLength) {
        return (client.useUploadPartCopy() && contentLength >= MINIMUM_MULTIPART_SIZE);
    }

    /**
     * Appends data to an existing S3 object
     * <p>
     * This method downloads the existing S3 object to a local file where the data is appended.
     * Afterwards the temporary object is upload again to a temporary S3 object which replaces the original one.
     * </p>
     *
     * @param file The data to append as {@link InputStream}
     * @param name The name of the object to append the data to
     * @return The new size of the modified object
     * @throws OXException
     */
    private long appendToFileWithSpooling(InputStream file, String name) throws OXException {
        /*
         * get existing object
         */
        String tempName = null;
        String key = addPrefix(name);
        S3Object s3Object = null;
        SequenceInputStream inputStream = null;
        try {
            s3Object = getObject(key);
            /*
             * append both streams at temporary location
             */
            long t0 = System.nanoTime();
            inputStream = new SequenceInputStream(s3Object.getObjectContent(), file);
            tempName = saveNewFile(inputStream);
            LOG.trace("Spooling append done in {} ms", L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
        } finally {
            Streams.close(inputStream, s3Object);
        }
        /*
         * replace old file, cleanup
         */
        try {
            String tempKey = addPrefix(tempName);
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, tempKey, bucketName, key);
            ObjectMetadata metadata = prepareMetadataForSSE(getMetadata(tempKey).clone());
            copyObjectRequest.setNewObjectMetadata(metadata);
            withRetry(c -> c.copyObject(copyObjectRequest));
            withVoidRetry(c -> c.deleteObject(bucketName, tempKey));
            return getContentLength(getMetadata(key));
        } catch (AmazonClientException e) {
            throw wrap(e, key);
        }
    }

    /**
     * Appends data to an existing S3 object.
     * <p>
     * This method uses a <i>UploadPartCopy</i> ({@link CopyPartRequest}) within a multipart upload request in order to create a new temporary S3 object with the appended data.
     * Afterwards the temporary object replaces the original.
     * </p>
     * <p>
     * Due to S3 restrictions, a UploadPartCopy is only possible if the source file, where to append the data, is larger than 5 MB.
     * </p>
     *
     * @param file The data to append as {@link InputStream}
     * @param name The name of the object to append the data to
     * @return The new size of the modified object
     * @throws OXException
     */
    private long appendToFileWithCopyPart(InputStream file, String name) throws OXException {
        S3ChunkedUpload chunkedUpload = null;
        String key = addPrefix(name);
        try {
            /*
             * Initialize the multipart upload
             */
            String tmpKey = generateKey(true);
            long t0 = System.nanoTime();
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, tmpKey).withObjectMetadata(prepareMetadataForSSE(new ObjectMetadata()));
            String uploadID = withRetry(c -> c.initiateMultipartUpload(initiateMultipartUploadRequest).getUploadId());
            LOG.trace("Initialized multipart upload done in {} ms", L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
            boolean uploadCompleted = false;

            try {
                int partNumber = 1;
                List<PartETag> parts = new ArrayList<PartETag>();

                /*
                 * UploadPartCopy: Copy from the existing object into an S3 tmp Object
                 */
                t0 = System.nanoTime();
                //@formatter:off
                CopyPartRequest copyPartRequest = new CopyPartRequest()
                    .withUploadId(uploadID)
                    .withPartNumber(partNumber++)
                    .withSourceBucketName(bucketName)
                    .withSourceKey(key)
                    .withDestinationBucketName(bucketName)
                    .withDestinationKey(tmpKey);
                //@formatter:on
                CopyPartResult copyPartResult = withRetry(c -> c.copyPart(copyPartRequest));
                if (copyPartResult == null) {
                    /* cannot perform partial copy because of some not satisfied constrains; fallback to spooling */
                    return appendToFileWithSpooling(file, name);
                }
                parts.add(copyPartResult.getPartETag());
                LOG.trace("CopyPartRequest done in {} ms", L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));

                /**
                 * Append the new data to the tmp object in chunks
                 */
                Thread currentThread = Thread.currentThread();
                t0 = System.nanoTime();
                chunkedUpload = new S3ChunkedUpload(file, client.getEncryptionConfig().isClientEncryptionEnabled(), client.getChunkSize());
                while (chunkedUpload.hasNext()) {
                    try (S3UploadChunk chunk = chunkedUpload.next()) {
                        boolean lastChunk = chunkedUpload.hasNext() == false;
                        parts.add(uploadPart(tmpKey, uploadID, partNumber++, chunk, lastChunk).getPartETag());
                        if (currentThread.isInterrupted()) {
                            throw OXException.general("Upload to S3 aborted");
                        }
                    }
                }
                LOG.trace("Chunked upload done in {} ms", L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));

                /*
                 * Finish the multipart upload
                 */
                t0 = System.nanoTime();
                withRetry(c -> c.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, tmpKey, uploadID, parts)));
                uploadCompleted = true;
                LOG.trace("Completing multipart upload done in {} ms", L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));

                /**
                 * Replace the old file , cleanup
                 */
                try {
                    t0 = System.nanoTime();
                    CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, tmpKey, bucketName, key);
                    ObjectMetadata metadata = prepareMetadataForSSE(getMetadata(tmpKey).clone());
                    copyObjectRequest.setNewObjectMetadata(metadata);
                    withRetry(c -> c.copyObject(copyObjectRequest));
                    withVoidRetry(c -> c.deleteObject(bucketName, tmpKey));
                    LOG.trace("Replacing file and cleanup done in {} ms", L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
                    return getContentLength(getMetadata(key));
                } catch (AmazonClientException e) {
                    throw wrap(e, key);
                }
            } catch (IOException e) {
                throw FileStorageCodes.IOERROR.create(e, e.getMessage());
            } finally {
                if (false == uploadCompleted) {
                    try {
                        withVoidRetry(c -> c.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, tmpKey, uploadID)));
                    } catch (AmazonClientException e) {
                        LOG.warn("Error aborting multipart upload", e);
                    }
                }
            }
        } finally {
            Streams.close(chunkedUpload);
        }
    }
}
