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

package com.openexchange.snippet.utils;

import static com.openexchange.java.Strings.isEmpty;
import static com.openexchange.java.Autoboxing.L;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.internet.MimeUtility;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import com.google.common.collect.ImmutableSet;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.config.cascade.ConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.conversion.ConversionService;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataProperties;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.html.HtmlServices;
import com.openexchange.image.ImageDataSource;
import com.openexchange.image.ImageLocation;
import com.openexchange.image.ImageUtility;
import com.openexchange.java.HTMLDetector;
import com.openexchange.java.InetAddresses;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.utils.ImageMatcher;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.net.HostList;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;
import com.openexchange.snippet.Attachment;
import com.openexchange.snippet.DefaultAttachment;
import com.openexchange.snippet.DefaultAttachment.InputStreamProvider;
import com.openexchange.snippet.DefaultSnippet;
import com.openexchange.snippet.SnippetExceptionCodes;
import com.openexchange.snippet.utils.internal.Services;
import com.openexchange.tools.net.URITools;
import com.openexchange.version.VersionService;

/**
 * {@link SnippetProcessor}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class SnippetProcessor {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SnippetProcessor.class);

    private static class ThresholdFileHolderInputStreamProvider implements InputStreamProvider {

        private final ThresholdFileHolder fileHolder;

        /**
         * Initializes a new {@link ThresholdFileHolderInputStreamProvider}.
         *
         * @param fileHolder The threshold file holder
         */
        ThresholdFileHolderInputStreamProvider(ThresholdFileHolder fileHolder) {
            super();
            this.fileHolder = fileHolder;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                return fileHolder.getStream();
            } catch (OXException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                throw new IOException(null == cause ? e : cause);
            }
        }
    }

    private static final String LOCAL_HOST_NAME;
    private static final String LOCAL_HOST_ADDRESS;

    static {
        // Host name initialization
        String localHostName;
        String localHostAddress;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            localHostName = localHost.getCanonicalHostName();
            localHostAddress = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            localHostName = "localhost";
            localHostAddress = "127.0.0.1";
        }
        LOCAL_HOST_NAME = localHostName;
        LOCAL_HOST_ADDRESS = localHostAddress;
    }

    // --------------------------------------------------------------------------------------------------------------------------

    private final Session session;

    /**
     * Initializes a new {@link SnippetProcessor}.
     *
     * @param session The session
     */
    public SnippetProcessor(Session session) {
        super();
        this.session = session;
    }

    private static HttpClient getHttpClient() throws OXException {
        return Services.getServiceLookup().getServiceSafe(HttpClientService.class).getHttpClient("icalfeed");
    }

    private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SRC_PATTERN = Pattern.compile("(?:src=\"([^\"]+)\")|(?:src='([^']+)')", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Set<String> ALLOWED_PROTOCOLS = ImmutableSet.of("http", "https", "ftp", "ftps");

    /** A set containing denied host names and IP addresses */
    public static final HostList DENIED_HOSTS = HostList.of("localhost", "127.0.0.1", LOCAL_HOST_ADDRESS, LOCAL_HOST_NAME);

    private static final Set<String> PROCOLS_HTTP = ImmutableSet.<String> builderWithExpectedSize(2).add("http").add("https").build();

    /**
     * Process the images in the snippet, extracts them and convert them to attachments.
     *
     * @param snippet The snippet to process
     * @throws OXException If processing images fails
     */
    public void processExternalImages(DefaultSnippet snippet) throws OXException {
        String content = snippet.getContent();
        if (isEmpty(content)) {
            return;
        }

        boolean isSignature = "signature".equalsIgnoreCase(snippet.getType());

        MaxImageProps maxImageProps = getMaxImageProps();
        long maxImageSize = maxImageProps.maxImageSize;
        int maxImageLimit = maxImageProps.maxImageLimit;

        Matcher m = IMG_PATTERN.matcher(content);
        if (false == m.find()) {
            return;
        }

        List<Attachment> attachments = new LinkedList<Attachment>();
        StringBuffer sb = new StringBuffer(content.length());
        int count = 0;
        do {
            String imgTag = m.group();

            Matcher srcMatcher = SRC_PATTERN.matcher(imgTag);
            if (srcMatcher.find()) {
                // Get the 'src' attribute's value
                String src = srcMatcher.group(1);
                if (src == null) {
                    src = srcMatcher.group(2);
                }

                // Check for valid URL
                URL u;
                try {
                    u = new URL(src);
                } catch (Exception e) {
                    // No... it's not
                    throw SnippetExceptionCodes.UNEXPECTED_ERROR.create(e, "Invalid image URL: " + src);
                }

                // Check max. number of images
                count++;
                if (maxImageLimit > 0 && count > maxImageLimit) {
                    throw SnippetExceptionCodes.MAXIMUM_IMAGES_COUNT.create(Integer.valueOf(maxImageLimit));
                }

                URLConnection con = null;
                try {
                    if (PROCOLS_HTTP.contains(Strings.asciiLowerCase(u.getProtocol()))) {
                        if (SnippetProcessor.DENIED_HOSTS.contains(u.getHost()) || InetAddresses.isInternalAddress(InetAddresses.forString(u.getHost()))) {
                            throw SnippetExceptionCodes.UNEXPECTED_ERROR.create("Invalid image URL: " + src);
                        }
                    } else {
                        con = URITools.getTerminalConnection(src, Optional.of(VALIDATOR), Optional.of(DECORATOR));
                    }
                } catch (OXException e) {
                    // No... it's not
                    throw e;
                } catch (Exception e) {
                    // No... it's not
                    throw SnippetExceptionCodes.UNEXPECTED_ERROR.create(e, "Invalid image URL: " + src);
                }

                // Get content identifier for URL resource
                String contentId = con == null ? loadHttpImage(u, count, maxImageSize, attachments, isSignature) : loadImage(con, count, maxImageSize, attachments, isSignature);

                if (null == contentId) {
                    // No valid image data accessible through URL. Drop <img> tag
                    m.appendReplacement(sb, "");
                } else {
                    // Replace <img> tag
                    int start = srcMatcher.start();
                    int end = srcMatcher.end();
                    String iTag = new StringBuilder(imgTag.length()).append(imgTag.substring(0, start)).append("src=\"cid:").append(contentId).append("\"").append(imgTag.substring(end)).toString();
                    m.appendReplacement(sb, Strings.quoteReplacement(iTag));
                }
            } else {
                m.appendReplacement(sb, Strings.quoteReplacement(imgTag));
            }

        } while (m.find());
        m.appendTail(sb);

        // Set "new" content
        snippet.setContent(sb.toString());

        // Add attachments
        for (Attachment attachment : attachments) {
            snippet.addAttachment(attachment);
        }
    }

    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 3000;

    private static Function<URLConnection, Optional<OXException>> DECORATOR = con -> {
        try {
            con.setConnectTimeout(CONNECT_TIMEOUT);
            con.setReadTimeout(READ_TIMEOUT);
            if (con instanceof HttpURLConnection) {
                HttpURLConnection httpCon = (HttpURLConnection) con;
                httpCon.setRequestMethod("GET");
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.of(SnippetExceptionCodes.IO_ERROR.create(e, e.getMessage()));
        }
    };

    /**
     * Validates the given URL according to white-listed protocols and blacklisted hosts.
     *
     * @param url The URL to validate
     * @return An optional OXException
     */
    private static Function<URL, Optional<OXException>> VALIDATOR = (url) -> {
        String protocol = url.getProtocol();
        if (protocol == null || !ALLOWED_PROTOCOLS.contains(Strings.asciiLowerCase(protocol))) {
            return Optional.of(SnippetExceptionCodes.UNEXPECTED_ERROR.create("Invalid image URL: " + url.toString()));
        }

        String host = Strings.asciiLowerCase(url.getHost());
        if (host == null || DENIED_HOSTS.contains(host)) {
            return Optional.of(SnippetExceptionCodes.UNEXPECTED_ERROR.create("Invalid image URL: " + url.toString()));
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(url.getHost());
            if (InetAddresses.isInternalAddress(inetAddress)) {
                return Optional.of(SnippetExceptionCodes.UNEXPECTED_ERROR.create("Invalid image URL: " + url.toString()));
            }
        } catch (UnknownHostException e) {
            return Optional.of(SnippetExceptionCodes.UNEXPECTED_ERROR.create("Invalid image URL: " + url.toString()));
        }
        return Optional.empty();
    };

    private String loadImage(URLConnection connectedCon, int count, long maxImageSize, List<Attachment> attachments, boolean isSignature) throws OXException {
        ThresholdFileHolder fileHolder = null;
        InputStream in = null;
        try {
            // Generic URLConnection handling
            in = connectedCon.getInputStream();

            int contentLength = connectedCon.getContentLength();
            if (contentLength > 0 && contentLength > maxImageSize) {
                throw SnippetExceptionCodes.MAXIMUM_IMAGE_SIZE.create(FileUtils.byteCountToDisplaySize(maxImageSize), Long.valueOf(maxImageSize));
            }

            String contentType = connectedCon.getHeaderField("content-type");
            if (Strings.isNotEmpty(contentType) && !Strings.asciiLowerCase(contentType).startsWith("image/")) {
                throw SnippetExceptionCodes.INVALID_IMAGE_DATA.create();
            }
            contentType = Strings.isEmpty(contentType) ? "image/jpeg" : contentType;
            String ext = MimeType2ExtMap.getFileExtension(contentType);

            fileHolder = new ThresholdFileHolder();
            fileHolder.write(in);
            fileHolder.setContentType(contentType);
            fileHolder.setName("image" + count + "." + ext);

            if (false == checkImageData(fileHolder)) {
                // No valid image data
                throw SnippetExceptionCodes.INVALID_IMAGE_DATA.create();
            }

            String id = UUIDs.getUnformattedString(UUID.randomUUID());
            String contentId = processLocalImage(fileHolder, id, true, attachments, isSignature);
            fileHolder = null; // Null'ify to avoid preliminary closing
            return contentId;
        } catch (IOException e) {
            throw SnippetExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            Streams.close(in, fileHolder);
        }
    }

    private String loadHttpImage(URL url, int count, long maxImageSize, List<Attachment> attachments, boolean isSignature) throws OXException {
        ThresholdFileHolder fileHolder = null;
        InputStream in = null;
        HttpGet request = null;
        HttpResponse response = null;
        try {
            HttpClient httpClient = getHttpClient();
            request = new HttpGet(url.toURI());
            response = httpClient.execute(request);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }

            long contentLength = entity.getContentLength();
            if (contentLength > 0 && contentLength > maxImageSize) {
                throw SnippetExceptionCodes.MAXIMUM_IMAGE_SIZE.create(FileUtils.byteCountToDisplaySize(maxImageSize), Long.valueOf(maxImageSize));
            }

            String contentType;
            {
                Header contentTypeHdr = entity.getContentType();
                contentType = contentTypeHdr == null ? null : contentTypeHdr.getValue();
            }
            if (contentType != null && Strings.isNotEmpty(contentType) && !Strings.asciiLowerCase(contentType.trim()).startsWith("image/")) {
                throw SnippetExceptionCodes.INVALID_IMAGE_DATA.create();
            }
            contentType = Strings.isEmpty(contentType) ? "image/jpeg" : contentType;
            String ext = MimeType2ExtMap.getFileExtension(contentType);

            fileHolder = new ThresholdFileHolder();
            in = entity.getContent();
            fileHolder.write(in);
            in = null; // Already closed here
            fileHolder.setContentType(contentType);
            fileHolder.setName("image" + count + "." + ext);

            if (false == checkImageData(fileHolder)) {
                // No valid image data
                throw SnippetExceptionCodes.INVALID_IMAGE_DATA.create();
            }

            String id = UUIDs.getUnformattedString(UUID.randomUUID());
            String contentId = processLocalImage(fileHolder, id, true, attachments, isSignature);
            fileHolder = null; // Null'ify to avoid preliminary closing
            return contentId;
        } catch (IOException e) {
            throw SnippetExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (URISyntaxException e) {
            throw SnippetExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Streams.close(in, fileHolder);
            HttpClients.close(request, response);
        }
    }

    private boolean checkImageData(ThresholdFileHolder fileHolder) throws IOException, OXException {
        if (HTMLDetector.containsHTMLTags(fileHolder.getStream(), false, HtmlServices.getGlobalEventHandlerIdentifiers())) {
            return false;
        }

        String contentType = com.openexchange.java.ImageTypeDetector.getMimeType(fileHolder.getStream());
        if (!Strings.asciiLowerCase(contentType).startsWith("image/")) {
            return false;
        }

        return isValidImage(fileHolder.getStream());
    }

    private static boolean isValidImage(InputStream data) {
        try {
            java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(data);
            return (bimg != null && bimg.getHeight() > 0 && bimg.getWidth() > 0);
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------ //

    private static final Pattern PATTERN_SRC_ATTRIBUTE = Pattern.compile("(?i)src=\"[^\"]*\"");
    private static final Pattern PATTERN_ID_ATTRIBUTE = Pattern.compile("(?i)id=\"[^\"]*@" + VersionService.NAME + "\"");

    /**
     * Process the images in the snippet, extracts them and convert them to attachments.
     *
     * @param snippet The snippet to process
     * @return The identifiers of extracted managed files
     * @throws OXException If processing images fails
     */
    public List<String> processImages(DefaultSnippet snippet) throws OXException {
        return processImages0(snippet, new LinkedList<Attachment>(), true);
    }

    /**
     * Process the images in the snippet, extracts them and convert them to attachments.
     *
     * @param snippet The snippet to process
     * @param attachments The list to add attachments to
     * @return The identifiers of extracted managed files
     * @throws OXException If processing images fails
     */
    public List<String> processImages(DefaultSnippet snippet, List<Attachment> attachments) throws OXException {
        return processImages0(snippet, attachments, false);
    }

    private static final Pattern PATTERN_SRC = MimeMessageUtility.PATTERN_SRC;

    private List<String> processImages0(DefaultSnippet snippet, List<Attachment> attachments, boolean addAttachments) throws OXException {
        String content = snippet.getContent();
        if (isEmpty(content)) {
            return Collections.emptyList();
        }

        long maxRawSnippetSize = getMaxSnippetSize();
        if (content.length() > maxRawSnippetSize) {
            throw SnippetExceptionCodes.MAXIMUM_RAW_SNIPPET_SIZE.create(L(maxRawSnippetSize), L(content.length()));
        }

        ImageMatcher m = ImageMatcher.matcher(content);
        if (m.find() == false) {
            // Found no image in snippet's content
            LOG.debug("Found no <img> tag in snippet's HTML content. Leaving as-is...");

            // Add attachments
            if (addAttachments) {
                for (Attachment attachment : attachments) {
                    snippet.addAttachment(attachment);
                }
            }

            return Collections.emptyList();
        }

        // Found <img> tag in snippet's HTML content
        LOG.debug("Found one or more <img> tag in snippet's HTML content. Starting processing...");

        // Acquire needed service
        ManagedFileManagement mfm = Services.getService(ManagedFileManagement.class);
        if (mfm == null) {
            throw ServiceExceptionCode.absentService(ManagedFileManagement.class);
        }

        // Check image restriction settings
        long maxImageSize;
        int maxImageLimit;
        {
            MaxImageProps maxImageProps = getMaxImageProps();
            maxImageSize = maxImageProps.maxImageSize;
            maxImageLimit = maxImageProps.maxImageLimit;
        }

        // Is snippet a signature?
        boolean isSignature = "signature".equalsIgnoreCase(snippet.getType());

        // Some variables needed for processing
        StringBuffer sb = new StringBuffer(content.length());
        List<String> managedFiles = new LinkedList<>();
        Set<String> trackedIds = new HashSet<String>(2);
        int count = 0;
        do {
            String imageTag = m.group();
            if (MimeMessageUtility.isValidImageTag(imageTag)) {
                LOG.debug("Found valid <img> tag in snippet's HTML content: {}", imageTag);

                // Get associated managed file for <img> tag
                ManagedFile mf;
                try {
                    String id = m.getManagedFileId();
                    if (null != id) {
                        if (false == mfm.contains(id)) {
                            LOG.debug("No such managed file for <img> tag (tag is maintained as-is): {}", imageTag);
                            m.appendLiteralReplacement(sb, MimeMessageUtility.blankSrc(imageTag));
                            continue;
                        }
                        mf = mfm.getByID(id);
                        LOG.debug("Obtained managed file {} for <img> tag: {}", mf.getID(), imageTag);
                    } else {
                        LOG.debug("<img> tag carries no managed-file identifier (therefore creating artifical managed file): {}", imageTag);
                        Optional<ManagedFile> optionalFile = toManagedFile(imageTag, mfm);
                        if (false == optionalFile.isPresent()) {
                            LOG.debug("<img> tag could not be converted to a managed file (tag is maintained as-is): {}", imageTag);
                            m.appendLiteralReplacement(sb, MimeMessageUtility.blankSrc(imageTag));
                            continue;
                        }
                        mf = optionalFile.get();
                        LOG.debug("Created artifical managed file {} for <img> tag: {}", mf.getID(), imageTag);
                    }
                } catch (OXException e) {
                    LOG.warn("Managed file could not be loaded/created for <img> tag (replacing tag with empty <img> tag): {}", imageTag, e);
                    // Anyway, replace image tag
                    m.appendLiteralReplacement(sb, MimeMessageUtility.blankSrc(imageTag));
                    continue;
                }

                String id = mf.getID();
                managedFiles.add(id);
                LOG.debug("Going to add managed file {} to snippet's attachments", id);

                if (++count > maxImageLimit) {
                    LOG.debug("<img> tag could not be added to snippet since max. number ({}) of images is exceeded: {}", Integer.valueOf(maxImageLimit), imageTag);
                    throw SnippetExceptionCodes.MAXIMUM_IMAGES_COUNT.create(Integer.valueOf(maxImageLimit));
                }

                if (mf.getSize() > maxImageSize) {
                    LOG.debug("<img> tag could not be added to snippet since max. image size ({}) is exceeded: {}", FileUtils.byteCountToDisplaySize(maxImageSize), imageTag);
                    throw SnippetExceptionCodes.MAXIMUM_IMAGE_SIZE.create(FileUtils.byteCountToDisplaySize(maxImageSize), Long.valueOf(maxImageSize));
                }

                // Add as atatchment to snippet
                boolean appendBodyPart = trackedIds.add(id);
                String contentId = processLocalImage(mf, id, appendBodyPart, attachments, isSignature);
                LOG.debug("Added managed file {} to snippet's attachments", id);

                // Replace "src" attribute
                String iTag = PATTERN_SRC_ATTRIBUTE.matcher(imageTag).replaceFirst(Strings.quoteReplacement("src=\"cid:" + contentId + "\""));
                iTag = PATTERN_ID_ATTRIBUTE.matcher(iTag).replaceFirst("");
                m.appendLiteralReplacement(sb, iTag);
                LOG.debug("Replaced \"src\" attribute of <img> tag in snippet's HTML content: {}", iTag);
            } else {
                // Re-append as-is
                LOG.debug("Found invalid <img> tag in snippet's HTML content (tag is maintained as-is): {}", imageTag);
                m.appendLiteralReplacement(sb, imageTag);
            }
        } while (m.find());
        m.appendTail(sb);

        // Set "new" content
        snippet.setContent(sb.toString());

        // Add attachments
        if (addAttachments) {
            for (Attachment attachment : attachments) {
                snippet.addAttachment(attachment);
            }
        }

        return managedFiles;
    }

    private final Optional<ManagedFile> toManagedFile(String imageTag, ManagedFileManagement mfm) throws OXException {
        ConversionService conversionService = Services.optService(ConversionService.class);
        if (conversionService == null) {
            // No such service
            return Optional.empty();
        }

        ImageLocation imageLocation = null;
        Matcher srcMatcher = PATTERN_SRC.matcher(imageTag);
        if (srcMatcher.find()) {
            String imageUri = Strings.replaceSequenceWith(srcMatcher.group(1), "&amp;", '&');
            if (MimeMessageUtility.isValidImageSource(imageUri)) {
                try {
                    imageLocation = ImageUtility.parseImageLocationFrom(imageUri);
                } catch (IllegalArgumentException e) {
                    // Nothing
                }
            }
        }

        if (null == imageLocation) {
            // Could not yield image location
            return Optional.empty();
        }

        ImageDataSource dataSource = (ImageDataSource) conversionService.getDataSource(imageLocation.getRegistrationName());
        if (null == dataSource) {
            // No such data source
            return Optional.empty();
        }

        InputStream in = null;
        try {
            Data<InputStream> data = dataSource.getData(InputStream.class, dataSource.generateDataArgumentsFrom(imageLocation), session);
            in = data.getData();

            DataProperties dataProperties = data.getDataProperties();
            String fileName = dataProperties == null ? null : dataProperties.get(DataProperties.PROPERTY_NAME);
            String contentType = dataProperties == null ? null : dataProperties.get(DataProperties.PROPERTY_CONTENT_TYPE);

            ManagedFile managedFile = mfm.createManagedFile(in, false);
            if (Strings.isNotEmpty(fileName)) {
                managedFile.setFileName(fileName);
            }
            if (Strings.isNotEmpty(contentType)) {
                managedFile.setContentType(contentType);
            }
            return Optional.of(managedFile);
        } finally {
            Streams.close(in);
        }
    }

    /**
     * Processes a local image and returns its content identifier
     *
     * @param mf The uploaded file
     * @param id The uploaded file's ID
     * @param appendBodyPart Whether to actually append the part to the snippet
     * @param attachments The attachment list
     * @param addFileName Whether to add file name information to snippet's image (adding a file name might imply a non-inline attachment)
     * @return The content id
     * @throws OXException If managed file' content cannot be read
     */
    private final String processLocalImage(ManagedFile mf, String id, boolean appendBodyPart, List<Attachment> attachments, boolean addFileName) throws OXException {
        if (false == appendBodyPart) {
            return id;
        }

        // Determine filename
        String fileName = mf.getFileName();
        if (null == fileName) {
            /*
             * Generate dummy file name
             */
            List<String> exts = MimeType2ExtMap.getFileExtensions(mf.getContentType().toLowerCase(Locale.ENGLISH));
            StringBuilder sb = new StringBuilder("image.");
            if (exts == null) {
                sb.append("dat");
            } else {
                sb.append(exts.get(0));
            }
            fileName = sb.toString();
        } else {
            /*
             * Encode image's file name for being mail-safe
             */
            try {
                fileName = MimeUtility.encodeText(fileName, MailProperties.getInstance().getDefaultMimeCharset(), "Q");
            } catch (UnsupportedEncodingException e) {
                fileName = mf.getFileName();
            }
        }

        // Transfer content to a ThresholdFileHolder instance
        ThresholdFileHolder fileHolder = null;
        try {
            fileHolder = new ThresholdFileHolder();
            fileHolder.write(mf.getInputStream());
            fileHolder.setName(fileName);
            fileHolder.setContentType(mf.getContentType());
            fileHolder.setDisposition("inline");
            String identifier = processLocalImage(fileHolder, id, appendBodyPart, attachments, addFileName);
            fileHolder = null; // Null'ify to avoid preliminary closing
            return identifier;
        } finally {
            Streams.close(fileHolder);
        }
    }

    /**
     * Processes a local image and returns its content identifier
     *
     * @param fileHolder The file holder containing the binary content and file meta-data
     * @param id The uploaded file's ID
     * @param appendBodyPart Whether to actually append the part to the snippet
     * @param attachments The attachment list
     * @param addFileName Whether to add file name information to snippet's image (adding a file name might imply a non-inline attachment)
     * @return The content id
     */
    private final String processLocalImage(ThresholdFileHolder fileHolder, String id, boolean appendBodyPart, List<Attachment> attachments, boolean addFileName) {
        if (appendBodyPart) {
            // Determine filename
            String fileName = fileHolder.getName();
            if (null == fileName) {
                /*
                 * Generate dummy file name
                 */
                List<String> exts = MimeType2ExtMap.getFileExtensions(fileHolder.getContentType());
                StringBuilder sb = new StringBuilder("image.");
                if (exts == null) {
                    sb.append("dat");
                } else {
                    sb.append(exts.get(0));
                }
                fileName = sb.toString();
            } else {
                /*
                 * Encode image's file name for being mail-safe
                 */
                try {
                    fileName = MimeUtility.encodeText(fileName, MailProperties.getInstance().getDefaultMimeCharset(), "Q");
                } catch (UnsupportedEncodingException e) {
                    fileName = fileHolder.getName();
                }
            }

            // Create appropriate attachment for inline image
            DefaultAttachment att = new DefaultAttachment();
            {
                ContentDisposition cd = new ContentDisposition();
                cd.setInline();
                if (!addFileName) {
                    cd.setFilenameParameter(fileName);
                }
                att.setContentDisposition(cd.toString());
            }
            att.setContentType(fileHolder.getContentType());
            att.setContentId(new StringBuilder(32).append('<').append(id).append('>').toString());
            att.setId(id);
            att.setSize(fileHolder.getLength());
            att.setStreamProvider(new ThresholdFileHolderInputStreamProvider(fileHolder));
            if (!addFileName) {
                att.setFilename(fileName);
            }
            attachments.add(att);
        }
        return id;
    }

    private MaxImageProps getMaxImageProps() throws OXException {
        long maxImageSize;
        int maxImageLimit;
        {
            ConfigViewFactory configViewFactory = Services.getService(ConfigViewFactory.class);
            ConfigView configView = configViewFactory.getView(session.getUserId(), session.getContextId());
            ConfigProperty<Integer> maxImageLimitConf = configView.property("com.openexchange.mail.signature.maxImageLimit", Integer.class);

            Integer value = maxImageLimitConf.get();
            if (value == null) {
                // Defaults to 3 images
                maxImageLimit = 3;
            } else {
                maxImageLimit = value.intValue();
            }

            ConfigProperty<Double> misConf = configView.property("com.openexchange.mail.signature.maxImageSize", Double.class);
            final double mis;
            Double dValue = misConf.get();
            if (dValue != null) {
                mis = dValue.doubleValue();
            } else {
                // Defaults to 1 MB
                mis = (1d);
            }
            maxImageSize = (long) (Math.pow(1024, 2) * mis);
        }
        return new MaxImageProps(maxImageSize, maxImageLimit);
    }

    private static final Property MAX_SNIPPET_SIZE = DefaultProperty.valueOf("com.openexchange.mail.signature.maxRawSnippetSize", L(51200l));

    /**
     * Gets the maximum allowed snippet size without attachments like images
     *
     * @return the maximum allowed snippet size
     */
    private long getMaxSnippetSize() {
        LeanConfigurationService lean = Services.getService(LeanConfigurationService.class);
        return lean.getLongProperty(session.getUserId(), session.getContextId(), MAX_SNIPPET_SIZE);
    }

    // --------------------------------------------------------------------------------------------------------------------------- //

    private static class MaxImageProps {

        final long maxImageSize;
        final int maxImageLimit;

        MaxImageProps(long maxImageSize, int maxImageLimit) {
            super();
            this.maxImageSize = maxImageSize;
            this.maxImageLimit = maxImageLimit;
        }
    }

}
