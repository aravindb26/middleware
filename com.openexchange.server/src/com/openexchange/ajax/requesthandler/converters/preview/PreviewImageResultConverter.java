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

package com.openexchange.ajax.requesthandler.converters.preview;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tika.util.TikaUtils.detectMimeType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.FileUtils;
import com.openexchange.ajax.container.ByteArrayFileHolder;
import com.openexchange.ajax.container.FileHolder;
import com.openexchange.ajax.container.ModifyableFileHolder;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.ajax.container.TmpFileFileHolder;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.requesthandler.cache.CachedResource;
import com.openexchange.ajax.requesthandler.cache.ResourceCache;
import com.openexchange.ajax.requesthandler.cache.ResourceCaches;
import com.openexchange.conversion.DataProperties;
import com.openexchange.conversion.SimpleData;
import com.openexchange.exception.OXException;
import com.openexchange.java.InterruptibleInputStream;
import com.openexchange.java.Reference;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.preview.ContentTypeChecker;
import com.openexchange.preview.PreviewDocument;
import com.openexchange.preview.PreviewExceptionCodes;
import com.openexchange.preview.PreviewOutput;
import com.openexchange.preview.PreviewService;
import com.openexchange.preview.RemoteInternalPreviewService;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link PreviewImageResultConverter}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class PreviewImageResultConverter extends AbstractPreviewResultConverter {

    /** The logger constant */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PreviewImageResultConverter.class);

    // ----------------------------------------------------------------------------------------------------------------------//

    static PreviewDocument getPreviewDocument(IFileHolder fileHolder, InputStream stream, AJAXRequestData requestData, String previewLanguage, PreviewOutput previewOutput, ServerSession session, PreviewService previewService) throws OXException {
        try {
            // Prepare properties for preview generation
            DataProperties dataProperties = new DataProperties(12);
            String mimeType = getContentType(fileHolder, previewService instanceof ContentTypeChecker ? (ContentTypeChecker) previewService : null);
            dataProperties.put(DataProperties.PROPERTY_CONTENT_TYPE, mimeType);
            dataProperties.put(DataProperties.PROPERTY_DISPOSITION, fileHolder.getDisposition());
            dataProperties.put(DataProperties.PROPERTY_NAME, fileHolder.getName());
            dataProperties.put(DataProperties.PROPERTY_SIZE, Long.toString(fileHolder.getLength()));
            dataProperties.put("PreviewType", requestData.getModule().equals("files") ? "DetailView" : "Thumbnail");
            dataProperties.put("PreviewWidth", requestData.getParameter("width"));
            dataProperties.put("PreviewHeight", requestData.getParameter("height"));
            dataProperties.put("PreviewDelivery", requestData.getParameter("delivery"));
            dataProperties.put("PreviewScaleType", requestData.getParameter("scaleType"));
            dataProperties.put("PreviewLanguage", previewLanguage);

            // Generate preview
            return previewService.getPreviewFor(new SimpleData<InputStream>(stream, dataProperties), previewOutput, session, 1);
        } catch (RuntimeException rte) {
            throw PreviewExceptionCodes.ERROR.create(rte, rte.getMessage());
        }
    }

    private static final class PreviewDocumentCallable extends AbstractTask<PreviewDocument> {

        private final AJAXRequestData requestData;
        private final IFileHolder fileHolder;
        private final String previewLanguage;
        private final PreviewOutput previewOutput;
        private final ServerSession session;
        private final InputStream stream;
        private final PreviewService previewService;

        PreviewDocumentCallable(IFileHolder fileHolder, InputStream stream, AJAXRequestData requestData, String previewLanguage, PreviewOutput previewOutput, ServerSession session, PreviewService previewService) {
            super();
            this.fileHolder = fileHolder;
            this.stream = stream;
            this.requestData = requestData;
            this.previewLanguage = previewLanguage;
            this.previewOutput = previewOutput;
            this.session = session;
            this.previewService = previewService;
        }

        @Override
        public PreviewDocument call() throws OXException {
            return getPreviewDocument(fileHolder, stream, requestData, previewLanguage, previewOutput, session, previewService);
        }

    }

    // ----------------------------------------------------------------------------------------------------------------------//

    /**
     * Initializes a new {@link PreviewImageResultConverter}.
     */
    public PreviewImageResultConverter() {
        super();
    }

    @Override
    public String getOutputFormat() {
        return "preview_image";
    }

    @Override
    public Quality getQuality() {
        return Quality.GOOD;
    }

    @Override
    public PreviewOutput getOutput() {
        return PreviewOutput.IMAGE;
    }

    @Override
    public void convert(final AJAXRequestData requestData, final AJAXRequestResult result, final ServerSession session, final Converter converter) throws OXException {
        try {
            // Check cache first
            final ResourceCache resourceCache;
            {
                final ResourceCache tmp = ResourceCaches.getResourceCache();
                resourceCache = null == tmp ? null : (tmp.isEnabledFor(session.getContextId(), session.getUserId()) ? tmp : null);
            }

            // Get eTag from result that provides the IFileHolder
            final String eTag = result.getHeader("ETag");
            final boolean isValidEtag = Strings.isNotEmpty(eTag);
            final String previewLanguage = getUserLanguage(session);
            if (null != resourceCache && isValidEtag && AJAXRequestDataTools.parseBoolParameter("cache", requestData, true)) {
                final String cacheKey = ResourceCaches.generatePreviewCacheKey(eTag, requestData, previewLanguage);
                final CachedResource cachedPreview = resourceCache.get(cacheKey, 0, session.getContextId());
                if (null != cachedPreview) {
                    requestData.setFormat("file");

                    // Determine MIME type
                    String contentType = cachedPreview.getFileType();
                    if (null == contentType) {
                        contentType = "image/jpeg";
                    }

                    // Create appropriate IFileHolder
                    IFileHolder responseFileHolder;
                    {
                        InputStream inputStream = cachedPreview.getInputStream();
                        if (null == inputStream) {
                            final ByteArrayFileHolder bafh = new ByteArrayFileHolder(cachedPreview.getBytes());
                            bafh.setContentType(contentType);
                            bafh.setName(cachedPreview.getFileName());
                            responseFileHolder = bafh;
                        } else {
                            responseFileHolder = new FileHolder(inputStream, cachedPreview.getSize(), contentType, cachedPreview.getFileName());
                        }
                    }

                    // Apply result
                    result.setResultObject(responseFileHolder, "file");
                    LOG.debug("Returned preview for file {} with MIME type {} from cache using ETag {} for user {} in context {}", cachedPreview.getFileName(), contentType, eTag, I(session.getUserId()), I(session.getContextId()));
                    return;
                }
            }

            // No cached preview available -- get the preview document from appropriate 'PreviewService'
            PreviewDocument previewDocument = null;
            {
                InputStream stream = null;
                IFileHolder fileHolder = null;
                Future<PreviewDocument> submittedTask = null;
                try {
                    final Object resultObject = result.getResultObject();
                    if (!(resultObject instanceof IFileHolder)) {
                        throw AjaxExceptionCodes.UNEXPECTED_RESULT.create(IFileHolder.class.getSimpleName(), null == resultObject ? "null" : resultObject.getClass().getSimpleName());
                    }
                    fileHolder = (IFileHolder) resultObject;

                    // Check file holder's content
                    stream = fileHolder.getStream();
                    {
                        if (0 == fileHolder.getLength()) {
                            Streams.close(stream, fileHolder);
                            stream = null;
                            setDefaulThumbnail(requestData, result);
                            return;
                        }
                        final Reference<InputStream> ref = new Reference<InputStream>();
                        if (streamIsEof(stream, ref)) {
                            Streams.close(stream, fileHolder);
                            stream = null;
                            setDefaulThumbnail(requestData, result);
                            return;
                        }
                        stream = ref.getValue();
                    }

                    // Obtain preview either using running or separate thread
                    PreviewService previewService = ServerServiceRegistry.getInstance().getService(PreviewService.class);

                    // Name-wise MIME type detection
                    String mimeType = MimeType2ExtMap.getContentType(fileHolder.getName(), null);
                    if (null == mimeType) {
                        // Unknown. Then detect MIME type by content.
                        ThresholdFileHolder tfh = new ThresholdFileHolder();
                        boolean error = true;
                        try {
                            tfh.write(stream).setContentInfo(fileHolder);
                            fileHolder = tfh;
                            mimeType = detectMimeType(fileHolder.getStream());
                            stream = fileHolder.getStream();
                            error = false;
                        } finally {
                            if (error) {
                                Streams.close(tfh);
                            }
                        }
                    }
                    ModifyableFileHolder mfh = new ModifyableFileHolder(fileHolder);
                    mfh.setContentType(mimeType);
                    fileHolder = mfh;

                    boolean useCurrentThread = true;
                    {
                        // Check if we deal with an instance of RemoteInternalPreviewService. In that case we need to limit the processing time...
                        RemoteInternalPreviewService remoteInternalPreviewService = getRemoteInternalPreviewServiceWithMime(previewService, mimeType, getOutput(), session);
                        if (null != remoteInternalPreviewService) {
                            long timeToWaitMillis = remoteInternalPreviewService.getTimeToWaitMillis();
                            if (timeToWaitMillis > 0) {
                             // Perform with separate thread
                                useCurrentThread = false;
                                InterruptibleInputStream iis = new InterruptibleInputStream(stream);
                                try {
                                    PreviewDocumentCallable task = new PreviewDocumentCallable(fileHolder, iis, requestData, previewLanguage, getOutput(), session, previewService);
                                    submittedTask = ThreadPools.getThreadPool().submit(task, CallerRunsBehavior.<PreviewDocument> getInstance());
                                    previewDocument = submittedTask.get(timeToWaitMillis, TimeUnit.MILLISECONDS);
                                } catch (TimeoutException e) {
                                    // Preview image has not been generated in time
                                    iis.interrupt();
                                    submittedTask.cancel(true);
                                    throw PreviewExceptionCodes.THUMBNAIL_NOT_AVAILABLE.create("Thumbnail has not been generated in time.");
                                } catch (InterruptedException e) {
                                    // Keep interrupted state
                                    Thread.currentThread().interrupt();
                                    throw PreviewExceptionCodes.ERROR.create(e, e.getMessage());
                                } catch (ExecutionException e) {
                                    // Failed to generate preview image
                                    throw ThreadPools.launderThrowable(e, OXException.class);
                                }
                            }
                        }
                    }

                    if (useCurrentThread) {
                        // Perform with this thread
                        previewDocument = getPreviewDocument(fileHolder, stream, requestData, previewLanguage, getOutput(), session, previewService);
                    }
                } catch (RuntimeException rte) {
                    throw PreviewExceptionCodes.ERROR.create(rte, rte.getMessage());
                } finally {
                    Streams.close(stream, fileHolder);
                }
            }

            // Check result
            if (null == previewDocument) {
                // No thumbnail available
                Optional<String> optName = optFileHolderNameFromResult(result);
                throw PreviewExceptionCodes.THUMBNAIL_NOT_AVAILABLE.create("PreviewDocument is null" + (optName.isPresent() ? " (source=" + optName.get() + ")" : ""));
            }

            // Check thumbnail stream
            requestData.setFormat("file");
            InputStream thumbnail = previewDocument.getThumbnail();
            if (null == thumbnail) {
                // No thumbnail data available
                Optional<String> optName = optFileHolderNameFromResult(result);
                throw PreviewExceptionCodes.THUMBNAIL_NOT_AVAILABLE.create("PreviewDocument's thumbnail input stream is null" + (optName.isPresent() ? " (source=" + optName.get() + ")" : ""));
            }

            // Prepare response
            preventTransformations(requestData, previewDocument);

            // (Asynchronously) Put to cache if ETag is available
            final String contentType = previewDocument.getMetaData().get("content-type");
            final String fileName = previewDocument.getMetaData().get("resourcename");
            if (null != resourceCache && isValidEtag && AJAXRequestDataTools.parseBoolParameter("cache", requestData, true)) {
                ThresholdFileHolder tmpFileHolder = null;
                try {
                    // Create file holder possibly writing data to temp. file
                    tmpFileHolder = new ThresholdFileHolder();
                    tmpFileHolder.write(thumbnail);
                    thumbnail = null; // Already closed
                    tmpFileHolder.setContentType(contentType);
                    tmpFileHolder.setName(fileName);

                    // Specify task
                    final String cacheKey = ResourceCaches.generatePreviewCacheKey(eTag, requestData);
                    Optional<AbstractTask<Void>> optTask = resourceCacheSaveTask(tmpFileHolder, fileName, contentType, cacheKey, resourceCache, session);
                    if (optTask.isPresent()) {
                        // Acquire thread pool service & execute task
                        AbstractTask<Void> task = optTask.get();
                        final ThreadPoolService threadPool = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
                        if (null == threadPool) {
                            try {
                                ThreadPools.execute(task);
                            } catch (Exception ex) {
                                throw (ex instanceof OXException ? (OXException) ex : AjaxExceptionCodes.UNEXPECTED_ERROR.create(ex, ex.getMessage()));
                            }
                        } else {
                            threadPool.submit(task);
                        }
                    }

                    // Set response object
                    result.setResultObject(tmpFileHolder, "file");
                    tmpFileHolder = null; // Avoid premature closing
                } finally {
                    Streams.close(tmpFileHolder);
                }
            } else {
                // Set response object
                result.setResultObject(new FileHolder(thumbnail, -1, contentType, fileName), "file");
            }
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private static Optional<AbstractTask<Void>> resourceCacheSaveTask(ThresholdFileHolder fileHolder, String fileName, String contentType, String cacheKey, ResourceCache resourceCache, ServerSession session) {
        File tempFile = fileHolder.getTempFile();
        if (tempFile == null) {
            // Data is held in memory
            ByteArrayOutputStream buf = fileHolder.getBuffer();
            return Optional.of(new AbstractTask<Void>() {
                @Override
                public Void call() {
                    try {
                        final CachedResource preview = new CachedResource(Streams.asInputStream(buf), fileName, contentType, buf.size());
                        resourceCache.save(cacheKey, preview, 0, session.getContextId());
                    } catch (Exception e) {
                        LOG.warn("Could not cache preview.", e);
                    }

                    return null;
                }
            });
        }

        // Copy file to avoid premature deletion through finishing the processing of parental request
        File newFile = null;
        try {
            newFile = TmpFileFileHolder.newTempFile(false);
            FileUtils.copyFile(tempFile, newFile);

            // Yield appropriate task using file's content
            AbstractTask<Void> task = newFileBackedTask(newFile, fileName, contentType, cacheKey, resourceCache, session);
            newFile = null; // Avoid premature deletion
            return Optional.of(task);
        } catch (Exception e) {
            LOG.warn("Could not cache preview.", e);
            return Optional.empty();
        } finally {
            FileUtils.deleteQuietly(newFile);
        }
    }

    private static AbstractTask<Void> newFileBackedTask(File newFile, String fileName, String contentType, String cacheKey, ResourceCache resourceCache, ServerSession session) {
        return new AbstractTask<Void>() {

            @Override
            public Void call() {
                InputStream data = null;
                try {
                    data = new FileInputStream(newFile);
                    final CachedResource preview = new CachedResource(data, fileName, contentType, newFile.length());
                    resourceCache.save(cacheKey, preview, 0, session.getContextId());
                } catch (Exception e) {
                    LOG.warn("Could not cache preview.", e);
                } finally {
                    Streams.close(data);
                    FileUtils.deleteQuietly(newFile);
                }

                return null;
            }
        };
    }

}
