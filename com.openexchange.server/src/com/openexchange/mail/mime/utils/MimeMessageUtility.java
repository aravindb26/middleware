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

package com.openexchange.mail.mime.utils;

import static com.openexchange.java.Strings.asciiLowerCase;
import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import static com.openexchange.mail.mime.QuotedInternetAddress.toIDN;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParameterList;
import javax.mail.internet.ParseException;
import javax.mail.util.ByteArrayDataSource;
import org.apache.commons.codec.binary.Base64;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.io.LineReaderInputStream;
import org.apache.james.mime4j.io.LineReaderInputStreamAdaptor;
import org.apache.james.mime4j.stream.DefaultFieldBuilder;
import org.apache.james.mime4j.stream.FieldBuilder;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.util.ByteArrayBuffer;
import org.apache.james.mime4j.util.CharsetUtil;
import com.google.common.collect.ImmutableSet;
import com.openexchange.ajax.AJAXUtility;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.image.ImageActionFactory;
import com.openexchange.java.CharsetDetector;
import com.openexchange.java.Charsets;
import com.openexchange.java.ExceptionAwarePipedInputStream;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.config.MailReloadable;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.MailStructure;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.EmptyStringMimeMultipart;
import com.openexchange.mail.mime.HeaderCollection;
import com.openexchange.mail.mime.HeaderName;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.MimeFilter;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.mail.mime.PlainTextAddress;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.converters.DataHandlerWrapper;
import com.openexchange.mail.mime.converters.FileBackedMimeBodyPart;
import com.openexchange.mail.mime.converters.MimeMessageConverter;
import com.openexchange.mail.mime.dataobjects.MimeMailMessage;
import com.openexchange.mail.mime.dataobjects.MimeMailPart;
import com.openexchange.mail.mime.datasource.FileDataSource;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.utils.CP932EmojiMapping;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.threadpool.behavior.AbortBehavior;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;
import com.openexchange.user.User;
import com.sun.mail.imap.protocol.BODYSTRUCTURE;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.MessageRemovedIOException;
import com.sun.mail.util.QDecoderStream;

/**
 * {@link MimeMessageUtility} - Utilities for MIME messages.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MimeMessageUtility {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MimeMessageUtility.class);

    private static final Set<HeaderName> ENCODINGS;

    private static final MailDateFormat MAIL_DATE_FORMAT;

    static {
        final Set<HeaderName> tmp = new HashSet<HeaderName>(4);
        tmp.add(HeaderName.valueOf("iso-8859-1"));
        tmp.add(HeaderName.valueOf("windows-1258"));
        tmp.add(HeaderName.valueOf("UTF-8"));
        tmp.add(HeaderName.valueOf("us-ascii"));
        ENCODINGS = ImmutableSet.copyOf(tmp);
        final MailDateFormat mdf = new MailDateFormat();
        mdf.setTimeZone(TimeZoneUtils.getTimeZone("UTC"));
        MAIL_DATE_FORMAT = mdf;
    }

    private static final ConcurrentMap<String, Future<MailDateFormat>> MDF_MAP = new ConcurrentHashMap<String, Future<MailDateFormat>>();

    /**
     * No instantiation
     */
    private MimeMessageUtility() {
        super();
    }

    private static final AtomicReference<Set<String>> dummyDomains = new AtomicReference<>();

    /**
     * Gets the dummy domains artificially set by IMAP servers; such as <code>"@unspecified-domain"</code> or <code>"@missing-domain"</code>.
     *
     * @return The dummy domains
     */
    public static Set<String> getDummyDomains() {
        Set<String> set = dummyDomains.get();
        if (null == set) {
            synchronized (MimeMessageUtility.class) {
                set = dummyDomains.get();
                if (null == set) {
                    String def = "@unspecified-domain, @missing_domain, @syntax_error";
                    ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return new LinkedHashSet<String>(Arrays.asList(Strings.splitByComma(def)));
                    }

                    String csv = Strings.asciiLowerCase(service.getProperty("com.openexchange.mail.dummyDomains", def).trim());
                    set = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(Strings.splitByComma(csv))));
                    dummyDomains.set(set);
                }
            }
        }
        return set;
    }

    private static final AtomicReference<Set<String>> dummyMailboxNames = new AtomicReference<>();

    /**
     * Gets the dummy mailbox names artificially set by IMAP servers; such as <code>"missing_mailbox"</code>.
     *
     * @return The dummy domains
     */
    public static Set<String> getDummyMailboxNames() {
        Set<String> set = dummyMailboxNames.get();
        if (null == set) {
            synchronized (MimeMessageUtility.class) {
                set = dummyMailboxNames.get();
                if (null == set) {
                    String def = "missing_mailbox";
                    ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return new LinkedHashSet<String>(Arrays.asList(Strings.splitByComma(def)));
                    }

                    String csv = Strings.asciiLowerCase(service.getProperty("com.openexchange.mail.dummyMailboxNames", def).trim());
                    set = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(Strings.splitByComma(csv))));
                    dummyMailboxNames.set(set);
                }
            }
        }
        return set;
    }

    /**
     * Strips possibly dummy domain from specified address string.
     *
     * @param address The address string
     * @return The address string with any dummy domain removed
     */
    public static String stripDummyDomain(String address) {
        if (null == address) {
            return null;
        }

        String retval = null;
        String toCheck = Strings.asciiLowerCase(address);
        for (String dummyDomain : getDummyDomains()) {
            int pos = toCheck.indexOf(dummyDomain);
            if (pos >= 0) {
                retval = address.substring(0, pos);
                break;
            }
        }

        // Contains neither dummy domain nor dummy mailbox name; return unchanged
        if (retval == null) {
            return address;
        }

        toCheck = Strings.asciiLowerCase(retval);
        for (String dummyMailboxName : getDummyMailboxNames()) {
            if (toCheck.equals(dummyMailboxName)) {
                retval = "";
                break;
            }
        }
        return retval;
    }

    /**
     * Prepares given address string by checking for possible mail-safe encodings.
     *
     * @param address The address
     * @return The prepared address
     */
    public static String prepareAddress(String address) {
        String decoded = toIDN(decodeMultiEncodedHeader(address));
        // Check for slash character -- the delimiting character for MSISDN addresses
        int pos = decoded.indexOf('@');
        if (pos < 0) {
            pos = decoded.indexOf('/');
            if (pos > 0) {
                decoded = decoded.substring(0, pos);
            }
        }
        // Check for dummy domains
        return stripDummyDomain(decoded);
    }

    /**
     * Checks whether another attempt to load content of a message and/or part should be performed.
     *
     * @param e The exception to check
     * @return <code>true</code> to retry; otherwise <code>false</code>
     */
    public static boolean shouldRetry(OXException e) {
        if (null == e) {
            return false;
        }
        if (MailExceptionCode.MAIL_NOT_FOUND.equals(e) || MimeMailExceptionCode.FOLDER_CLOSED.equals(e)) {
            return true;
        }
        if (MailExceptionCode.IO_ERROR.equals(e)) {
            final Throwable cause = e.getCause();
            return (cause instanceof IOException) && "no content".equals(asciiLowerCase(cause.getMessage()));
        }
        if (MimeMailExceptionCode.MESSAGING_ERROR.equals(e)) {
            final Throwable cause = e.getCause();
            return (cause instanceof MessagingException) && "failed to fetch headers".equals(asciiLowerCase(cause.getMessage()));
        }
        return false;
    }

    /**
     * Gets the default {@link MailDateFormat}.
     * <p>
     * Note that returned instance of {@link MailDateFormat} is shared, therefore use a surrounding synchronized block to preserve thread
     * safety:
     *
     * <pre>
     * ...
     * final MailDateFormat mdf = getMailDateFormat(session);
     * synchronized(mdf) {
     * mimeMessage.setHeader(&quot;Date&quot;, mdf.format(sendDate));
     * }
     * ...
     * </pre>
     *
     * @return The {@link MailDateFormat} for specified session
     */
    public static MailDateFormat getDefaultMailDateFormat() {
        return MAIL_DATE_FORMAT;
    }

    /**
     * Gets the {@link MailDateFormat} for specified session.
     * <p>
     * Note that returned instance of {@link MailDateFormat} is shared, therefore use a surrounding synchronized block to preserve thread
     * safety:
     *
     * <pre>
     * ...
     * final MailDateFormat mdf = getMailDateFormat(session);
     * synchronized(mdf) {
     * mimeMessage.setHeader(&quot;Date&quot;, mdf.format(sendDate));
     * }
     * ...
     * </pre>
     *
     * @param session The user session
     * @return The {@link MailDateFormat} for specified session
     * @throws OXException If {@link MailDateFormat} cannot be returned
     */
    public static MailDateFormat getMailDateFormat(Session session) throws OXException {
        final User user;
        if (session instanceof ServerSession) {
            user = ((ServerSession) session).getUser();
        } else {
            user = UserStorage.getInstance().getUser(session.getUserId(), session.getContextId());
        }
        return getMailDateFormat(user.getTimeZone());
    }

    /**
     * Gets the {@link MailDateFormat} for specified time zone identifier.
     * <p>
     * Note that returned instance of {@link MailDateFormat} is shared, therefore use a surrounding synchronized block to preserve thread
     * safety:
     *
     * <pre>
     * ...
     * final MailDateFormat mdf = getMailDateFormat(timeZoneId);
     * synchronized(mdf) {
     * mimeMessage.setHeader(&quot;Date&quot;, mdf.format(sendDate));
     * }
     * ...
     * </pre>
     *
     * @param timeZoneId The time zone identifier
     * @return The {@link MailDateFormat} for specified time zone identifier
     * @throws OXException If {@link MailDateFormat} cannot be returned
     */
    public static MailDateFormat getMailDateFormat(String timeZoneId) throws OXException {
        Future<MailDateFormat> future = MDF_MAP.get(timeZoneId);
        if (null == future) {
            final FutureTask<MailDateFormat> ft = new FutureTask<MailDateFormat>(new Callable<MailDateFormat>() {

                @Override
                public MailDateFormat call() throws Exception {
                    final MailDateFormat mdf = new MailDateFormat();
                    mdf.setTimeZone(TimeZoneUtils.getTimeZone(timeZoneId));
                    return mdf;
                }
            });
            future = MDF_MAP.putIfAbsent(timeZoneId, ft);
            if (null == future) {
                future = ft;
                ft.run();
            }
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            throw MailExceptionCode.UNEXPECTED_ERROR.create(cause, cause.getMessage());
        }
    }

    /**
     * Checks if specified headers are empty. The passed headers are considered as all the values for a certain header or <code>null</code>
     * if no headers exist.
     *
     * @param headers The values for a certain header
     * @return <code>true</code> if specified headers are empty; otherwise <code>false</code>
     */
    public static boolean isEmptyHeader(String[] headers) {
        if (null == headers || 0 == headers.length) {
            return true;
        }
        boolean isEmpty = true;
        for (int i = 0; isEmpty && i < headers.length; i++) {
            isEmpty = isEmpty(headers[i]);
        }
        return isEmpty;
    }

    private static boolean isEmpty(String string) {
        return com.openexchange.java.Strings.isEmpty(string);
    }

    private static final Pattern PATTERN_EMBD_IMG = Pattern.compile("(<img[^>]+src=\"cid:)([^\"]+)(\"[^>]*/?>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern PATTERN_EMBD_IMG_ALT = Pattern.compile("(<img[^>]+src=\")([0-9a-z&&[^.\\s>\"]]+\\.[0-9a-z&&[^.\\s>\"]]+)(\"[^>]*/?>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Detects if given HTML content contains inlined images
     * <p>
     * Example:
     *
     * <pre>
     * &lt;img src=&quot;cid:s345asd845@12drg&quot;&gt;
     * </pre>
     *
     * @param htmlContent The HTML content
     * @return <code>true</code> if given HTML content contains inlined images; otherwise <code>false</code>
     */
    public static boolean hasEmbeddedImages(CharSequence htmlContent) {
        return PATTERN_EMBD_IMG.matcher(htmlContent).find() || PATTERN_EMBD_IMG_ALT.matcher(htmlContent).find();
    }

    /**
     * Gathers all occurring content IDs in HTML content and returns them as a list
     * <pre>
     * &lt;img src=&quot;cid:s345asd845@12drg&quot;&gt;
     * </pre>
     *
     * @param htmlContent The HTML content
     * @return an instance of <code>{@link List}</code> containing all occurring content IDs
     */
    public static List<String> getContentIDs(CharSequence htmlContent) {
        final List<String> retval = new LinkedList<String>();
        Matcher m = PATTERN_EMBD_IMG.matcher(htmlContent);
        while (m.find()) {
            retval.add(m.group(2));
        }
        m = PATTERN_EMBD_IMG_ALT.matcher(htmlContent);
        while (m.find()) {
            retval.add(m.group(2));
        }
        return retval;
    }

    /**
     * Checks if given Content-Id value is contained in specified collection.
     *
     * @param contentId The Content-Id value
     * @param contentIds The collection
     * @return <code>true</code> if contained; otherwise <code>false</code>
     */
    public static boolean containsContentId(String contentId, Collection<String> contentIds) {
        boolean contains = false;
        for (Iterator<String> iterator = contentIds.iterator(); !contains && iterator.hasNext();) {
            contains = equalsCID(contentId, iterator.next());
        }
        return contains;
    }

    /**
     * Gets the plain Content-Id value; meaning possible starting <code>'&lt;'</code> and trailing <code>'&gt;'</code> stripped off.
     *
     * @param contentId The Content-Id value to process
     * @return The plain Content-Id value
     */
    public static String getPlainContentId(String contentId) {
        if ((null == contentId) || (0 >= contentId.length())) {
            return contentId;
        }
        String ret = contentId;
        if ('<' == ret.charAt(0)) {
            ret = ret.substring(1);
        }
        final int mlen = ret.length() - 1;
        if (mlen > 0 && '>' == ret.charAt(mlen)) {
            ret = ret.substring(0, mlen);
        }
        return ret;
    }

    /**
     * Compares (case insensitive) the given values of message header "Content-ID". The leading/trailing characters '<code>&lt;</code>' and
     * ' <code>&gt;</code>' are ignored during comparison
     *
     * @param contentId1 The first content ID
     * @param contentId2 The second content ID
     * @return <code>true</code> if both are equal; otherwise <code>false</code>
     */
    public static boolean equalsCID(String contentId1, String contentId2) {
        return equalsCID(contentId1, contentId2, null);
    }

    /**
     * Compares (case insensitive) the given values of message header "Content-ID". The leading/trailing characters '<code>&lt;</code>' and
     * ' <code>&gt;</code>' are ignored during comparison
     *
     * @param contentId1 The first content ID
     * @param contentId2 The second content ID
     * @param ignorableSuffix The optional ignorable suffix; e.g. <code>"@Open-Xchange"</code>
     * @return <code>true</code> if both are equal; otherwise <code>false</code>
     */
    public static boolean equalsCID(String contentId1, String contentId2, String ignorableSuffix) {
        final String cid1 = trimContentId(contentId1, ignorableSuffix);
        final String cid2 = trimContentId(contentId2, ignorableSuffix);
        if (null == cid1) {
            if (null != cid2) {
                return false;
            }
        } else {
            if ((null == cid2) || !cid1.equalsIgnoreCase(cid2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the trimmed Content-ID value
     * <p>
     * <tt>"&lt;1234abcd&gt;"</tt> --&gt; <tt>"1234abcd"</tt>
     *
     * @param contentId The Content-ID value
     * @return The trimmed Content-ID value
     */
    public static String trimContentId(String contentId) {
        return trimContentId(contentId, null);
    }

    private static String trimContentId(String contentId, String ignorableSuffix) {
        if (null == contentId) {
            return null;
        }
        String ret = contentId.trim();
        if (ret.startsWith("<")) {
            ret = ret.substring(1);
            if (ret.endsWith(">")) {
                ret = ret.substring(0, ret.length() - 1);
            }
        }
        if (null != ignorableSuffix && ret.endsWith(ignorableSuffix)) {
            ret = ret.substring(0, ret.lastIndexOf(ignorableSuffix));
        }
        return ret;
    }

    /**
     * URL-decodes given string
     *
     * @param s The string
     * @return The URL-decoded string
     */
    public static String urlDecode(String s) {
        try {
            return AJAXUtility.decodeUrl(replaceURLCodePoints(s), "ISO-8859-1");
        } catch (RuntimeException e) {
            return s;
        }
    }

    private static final Pattern PATTERN_CODE_POINT = Pattern.compile("%u00([a-fA-F0-9]{2})");

    private static String replaceURLCodePoints(String s) {
        Matcher m = PATTERN_CODE_POINT.matcher(s);
        StringBuffer buffer = new StringBuffer(s.length());
        while (m.find()) {
            char[] chars = Character.toChars(Integer.parseInt(m.group(1), 16));
            m.appendReplacement(buffer, com.openexchange.java.Strings.quoteReplacement(new String(chars)));
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

    /** The pattern for matching the <code>src</code> attribute within an &lt;img&gt; tag */
    public static final Pattern PATTERN_SRC = Pattern.compile("<img[^>]*?src=\"([^\"]+)\"[^>]*/?>", Pattern.CASE_INSENSITIVE);

    /**
     * Yields a blank <code>src</code> attribute for passed &lt;img&gt; tag.
     *
     * @param imageTag The &lt;img&gt; tag
     * @return The &lt;img&gt; tag with a blank <code>src</code> attribute
     */
    public static String blankSrc(String imageTag) {
        if (isEmpty(imageTag)) {
            return imageTag;
        }
        final Matcher srcMatcher = PATTERN_SRC.matcher(imageTag);
        if (!srcMatcher.find()) {
            return imageTag;
        }
        final StringBuffer sb = new StringBuffer(imageTag.length());
        int st = srcMatcher.start(1);
        int end = srcMatcher.end(1);
        srcMatcher.appendReplacement(sb, Matcher.quoteReplacement(imageTag.substring(0, st) + imageTag.substring(end)));
        srcMatcher.appendTail(sb);
        return sb.toString();
    }

    private static final String IMAGE_ALIAS_APPENDIX = ImageActionFactory.ALIAS_APPENDIX;

    private static final String FILE_ALIAS_APPENDIX = "file";

    /**
     * Checks if specified &lt;image&gt; tag's <code>src</code> attribute seems to point to an Open-Xchange image end-point.
     *
     * @param imageTag The &lt;image&gt; tag
     * @return <code>true</code> if an image end-point is addressed; otherwise <code>false</code>
     */
    public static boolean isValidImageTag(String imageTag) {
        if (isEmpty(imageTag)) {
            return false;
        }

        String tmp = asciiLowerCase(imageTag);
        String srcStart = "src=\"";
        int pos = tmp.indexOf(srcStart);
        int fromIndex = pos + srcStart.length();
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        return isValidImageSource(tmp, fromIndex);
    }

    /**
     * Checks if specified image URI attribute seems to point to an Open-Xchange image end-point.
     *
     * @param imageUri The image URI to examine
     * @return <code>true</code> if an image end-point is addressed; otherwise <code>false</code>
     */
    public static boolean isValidImageSource(String imageUri) {
        return isValidImageSource(imageUri, 0);
    }

    private static boolean isValidImageSource(String imageUri, int fromIndex) {
        if (isEmpty(imageUri)) {
            return false;
        }

        String tmp = asciiLowerCase(imageUri);
        return (tmp.indexOf('/' + IMAGE_ALIAS_APPENDIX, fromIndex) >= 0 || tmp.indexOf('/' + FILE_ALIAS_APPENDIX, fromIndex) >= 0);
    }

    /**
     * Detects if given HTML content contains references to local image files
     * <ul>
     * <li>Example for an uploaded image file referenced within a composed mail:<br>
     * <code>
     * &nbsp;&nbsp;&lt;img&nbsp;src=&quot;/ajax/file?action=get&amp;session=abcdefg&amp;id=123dfr567zh&quot;&gt;
     * </code></li>
     * <li>Example for a stored image file referenced within a composed mail:<br>
     * <code>
     * &nbsp;&nbsp;&lt;img&nbsp;src=&quot;/ajax/image?uid=12gf356j7&quot;&gt;
     * </code></li>
     * </ul>
     *
     * @param htmlContent The HTML content
     * @return <code>true</code> if given HTML content contains references to local image files; otherwise <code>false</code>
     */
    public static boolean hasReferencedLocalImages(CharSequence htmlContent) {
        final ImageMatcher m = ImageMatcher.matcher(htmlContent);
        if (m.find()) {
            final ManagedFileManagement mfm = ServerServiceRegistry.getInstance().getService(ManagedFileManagement.class);
            do {
                final String mid = m.getManagedFileId();
                if (null != mid) {
                    mfm.contains(mid);
                }
            } while (m.find());
            return true;
        }
        return false;
    }

    /**
     * Determines specified part's real filename if any available.
     *
     * @param part The part whose filename shall be determined
     * @return The part's real filename or <code>null</code> if none present
     */
    public static String getRealFilename(MailPart part) {
        if (part.getFileName() != null) {
            return part.getFileName();
        }
        final String hdr = part.getFirstHeader(MessageHeaders.HDR_CONTENT_DISPOSITION);
        if (hdr == null) {
            return getContentTypeFilename(part);
        }
        try {
            final String retval = new ContentDisposition(hdr).getFilenameParameter();
            if (retval == null) {
                return getContentTypeFilename(part);
            }
            return retval;
        } catch (OXException e) {
            return getContentTypeFilename(part);
        }
    }

    private static final String PARAM_NAME = "name";

    private static String getContentTypeFilename(MailPart part) {
        if (part.containsContentType()) {
            return part.getContentType().getParameter(PARAM_NAME);
        }
        final String hdr = part.getFirstHeader(MessageHeaders.HDR_CONTENT_TYPE);
        if (hdr == null || hdr.length() == 0) {
            return null;
        }
        try {
            return new ContentType(hdr).getParameter(PARAM_NAME);
        } catch (OXException e) {
            LOG.error("", e);
            return null;
        }
    }

    /**
     * Checks if given multipart contains (file) attachments
     *
     * @param mp The multipart to examine
     * @param subtype The multipart's subtype
     * @return <code>true</code> if given multipart contains (file) attachments; otherwise <code>false</code>
     * @throws MessagingException If a messaging error occurs
     * @throws OXException If a mail error occurs
     * @throws IOException If an I/O error occurs
     */
    public static boolean hasAttachments(MailPart mp, String subtype) throws MessagingException, OXException, IOException {
        if (null == mp) {
            return false;
        }

        int count = mp.getEnclosedCount();
        return hasAttachments0(mp, count);
    }

    private static boolean hasAttachments0(MailPart mp, int count) throws MessagingException, OXException, IOException {
        if (count == MailPart.NO_ENCLOSED_PARTS) {
            return hasAttachmentInMetadata(mp);
        }

        boolean found = false;
        ContentType ct = new ContentType();
        for (int i = count; !found && i-- > 0;) {
            MailPart part = mp.getEnclosedMailPart(i);
            if (hasAttachmentInMetadata(part)) {
                return true;
            }
            String[] tmp = part.getHeader(MessageHeaders.HDR_CONTENT_TYPE);
            if (tmp != null && tmp.length > 0) {
                ct.setContentType(unfold(tmp[0]));
                if (ct.startsWith("multipart/")) {
                    found |= hasAttachments(part, ct.getSubType());
                }
            }
        }
        return found;
    }

    private static boolean hasAttachmentInMetadata(MailPart part) {
        if (part.getContentType().getBaseType().toLowerCase().startsWith("application") && part.getContentType().getSubType().toLowerCase().endsWith("-signature")) {
            return false;
        }

        ContentDisposition contentDisposition = part.getContentDisposition();
        if (contentDisposition != null) {
            if (contentDisposition.getFilenameParameter() != null || part.getFileName() != null) {
                return true;
            }
            String disposition = contentDisposition.getDisposition();
            if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                return true;
            }
        }
        return MimeTypes.isConsideredAttachment(part.getContentType().getBaseType() + "/" + part.getContentType().getSubType());
    }

    /**
     * Checks if given part contains (file) attachments
     *
     * @param part The part to examine
     * @return <code>true</code> if given part contains (file) attachments; otherwise <code>false</code>
     * @throws MessagingException If a messaging error occurs
     * @throws OXException If a mail error occurs
     * @throws IOException If an I/O error occurs
     */
    public static boolean hasAttachments(Part part) throws MessagingException, OXException, IOException {
        if (null == part) {
            return false;
        }

        if (part.getContentType().toLowerCase().startsWith("multipart/")) {
            Multipart multipart = getMultipartContentFrom(part);
            if (null != multipart) {
                int count = multipart.getCount();
                return hasAttachments0(multipart, count);
            }
        }
        return hasAttachmentInMetadata(part);
    }

    /**
     * Checks if given part contains (file) attachments
     *
     * @param part The part to examine
     * @param optContentType The optional Content-Type or <code>null</code>
     * @return <code>true</code> if given part contains (file) attachments; otherwise <code>false</code>
     * @throws MessagingException If a messaging error occurs
     * @throws OXException If a mail error occurs
     * @throws IOException If an I/O error occurs
     */
    public static boolean hasAttachments(Part part, String optContentType) throws MessagingException, OXException, IOException {
        if (null == part) {
            return false;
        }

        String contentType = null == optContentType ? part.getContentType() : optContentType;
        if (Strings.asciiLowerCase(contentType).startsWith("multipart/")) {
            Multipart multipart = getMultipartContentFrom(part, contentType);
            if (null != multipart) {
                int count = multipart.getCount();
                return hasAttachments0(multipart, count);
            }
        }
        return hasAttachmentInMetadata(part);
    }

    private static boolean hasAttachments0(Multipart mp, int count) throws MessagingException, OXException, IOException {
        boolean found = false;
        ContentType ct = new ContentType();
        for (int i = count; !found && i-- > 0;) {
            BodyPart part = mp.getBodyPart(i);
            if (hasAttachmentInMetadata(part)) {
                return true;
            }
            String[] tmp = part.getHeader(MessageHeaders.HDR_CONTENT_TYPE);
            if (tmp != null && tmp.length > 0) {
                ct.setContentType(unfold(tmp[0]));
                if (ct.isMimeType(MimeTypes.MIME_MULTIPART_ALL)) {
                    found |= hasAttachments(part);
                }
            }
        }
        return found;
    }

    private static boolean hasAttachmentInMetadata(Part part) throws MessagingException, OXException {
        if (part.getContentType().toLowerCase().startsWith("application") && part.getContentType().toLowerCase().endsWith("-signature")) {
            return false;
        }

        String disposition = part.getDisposition();
        if (disposition != null) {
            ContentDisposition cd = new ContentDisposition(disposition);
            if (cd.getDisposition().equalsIgnoreCase(Part.ATTACHMENT)) {
                return true;
            }
            if (cd.getFilenameParameter() != null) {
                return true;
            }
        }
        return MimeTypes.isConsideredAttachment(part.getContentType());
    }

    /**
     * Checks if given BODYSTRUCTURE item indicates to contain (file) attachments
     *
     * @param bodystructure The BODYSTRUCTURE item
     * @return <code>true</code> if given BODYSTRUCTURE item indicates to contain (file) attachments; otherwise <code>false</code>
     */
    public static boolean hasAttachments(BODYSTRUCTURE bodystructure) {
        // The value determined by this routine will outsmart exact examination
        // See bug 42695 & 42862
        if (bodystructure.isMulti()) {
            return hasAttachments0(bodystructure);
        }

        if (!(bodystructure.type.toLowerCase().startsWith("application") && bodystructure.subtype.toLowerCase().endsWith("-signature"))) {
            if (bodystructure.disposition != null && bodystructure.disposition.equals(Part.ATTACHMENT)) {
                return true;
            }

            if (bodystructure.dParams != null && bodystructure.dParams.get("filename") != null) {
                return true;
            }

            if (bodystructure.disposition == null && bodystructure.cParams != null && bodystructure.cParams.get("name") != null) {
                return true;
            }
        }
        return MimeTypes.isConsideredAttachment(bodystructure.type + "/" + bodystructure.subtype);
    }

    private static boolean hasAttachments0(BODYSTRUCTURE bodystructure) {
        boolean found = false;

        BODYSTRUCTURE[] bodies = bodystructure.bodies;
        if (null != bodies) {
            for (int i = bodies.length; !found && i-- > 0;) {
                found |= hasAttachments(bodies[i]);
            }
        }

        return found;
    }

    /**
     * Parses the content type from given BODYSTRUCTURE item.
     *
     * @param bs The BODYSTRUCTURE item
     * @param msg The optional message for debugging purpose
     * @return The content type
     */
    public static ContentType parseContentType(BODYSTRUCTURE bs, MailMessage msg) {
        ContentType contentType = new ContentType().setPrimaryType(bs.type).setSubType(bs.subtype);
        ParameterList cParams = bs.cParams;
        if (cParams != null) {
            for (Enumeration<?> names = cParams.getNames(); names.hasMoreElements();) {
                String name = names.nextElement().toString();
                String value = cParams.get(name);
                if (!com.openexchange.java.Strings.isEmpty(value)) {
                    try {
                        contentType.setParameterErrorAware(name, MimeMessageUtility.decodeEnvelopeHeader(value));
                    } catch (OXException e) {
                        String uid = msg == null ? null : msg.getMailId();
                        String folder =  msg == null ? null : msg.getFolder();
                        LOG.debug("Ignoring invalid parameter in Content-Type header contained in message {} of folder {}.", uid == null ? "<unknown>" : uid, null == folder ? "<unknown>" : folder, e);
                    }
                }
            }
        }
        return contentType;
    }

    /**
     * Parses the content disposition from given BODYSTRUCTURE item.
     *
     * @param bs The BODYSTRUCTURE item
     * @param msg The optional message for debugging purpose
     * @return The content disposition
     * @throws OXException If parsing fails
     */
    public static ContentDisposition parseContentDisposition(BODYSTRUCTURE bs, MailMessage msg) throws OXException {
        ContentDisposition contentDisposition = new ContentDisposition(bs.disposition);
        ParameterList dParams = bs.dParams;
        if (dParams != null) {
            for (Enumeration<?> names = dParams.getNames(); names.hasMoreElements();) {
                String name = names.nextElement().toString();
                String value = dParams.get(name);
                if (!com.openexchange.java.Strings.isEmpty(value)) {
                    try {
                        contentDisposition.setParameterErrorAware(name, MimeMessageUtility.decodeEnvelopeHeader(value));
                    } catch (OXException e) {
                        String uid =  msg == null ? null : msg.getMailId();
                        String folder =  msg == null ? null : msg.getFolder();
                        LOG.debug("Ignoring invalid parameter in Content-Disposition header contained in message {} of folder {}.", uid == null ? "<unknown>" : uid, null == folder ? "<unknown>" : folder, e);
                    }
                }
            }
        }
        return contentDisposition;
    }

    /**
     * Parses the mail structure from given BODYSTRUCTURE item.
     *
     * @param bs The BODYSTRUCTURE item
     * @param contentType The optional content type
     * @param contentDisposition The optional content disposition
     * @param msg The optional message for debugging purpose
     * @return The mail structure
     * @throws OXException If parsing fails
     */
    public static MailStructure parseMailStructure(BODYSTRUCTURE bs, ContentType contentType, ContentDisposition contentDisposition, MailMessage msg) throws OXException {
        MailStructure.Builder builder = MailStructure.builder();
        builder.withContentDisposition(contentDisposition == null ? parseContentDisposition(bs, msg) : contentDisposition);
        builder.withContentId(bs.id);
        builder.withContentType(contentType == null ? parseContentType(bs, msg) : contentType);
        builder.withSize(bs.size);
        BODYSTRUCTURE[] bodies = bs.bodies;
        if (bodies != null && bodies.length > 0) {
            for (BODYSTRUCTURE subbs : bodies) {
                builder.addBody(parseMailStructure(subbs, null, null, msg));
            }
        }
        return builder.build();
    }

    /**
     * Decodes a "Subject" header obtained from ENVELOPE fetch item.
     *
     * @param subject The subject obtained from ENVELOPE fetch item
     * @return The decoded subject value
     */
    public static String decodeEnvelopeSubject(String subject) {
        if (null == subject) {
            return "";
        }
        /*-
         * Hmm... Why does ENVELOPE FETCH response omit CR?LFs in subject?!
         *
         * Example:
         * Subject: =?UTF-8?Q?Nur_noch_kurze_Zeit:_1_Freimona?=
         *  =?UTF-8?Q?t_f=C3=BCr_3_erfolgreiche_Einladungen?=
         *
         * is transferred as:
         * =?UTF-8?Q?Nur_noch_kurze_Zeit:_1_Freimona?= =?UTF-8?Q?t_f=C3=BCr_3_erfolgreiche_Einladungen?=
         */
        String s = checkNonAscii(subject);

        int length = s.length();
        StringBuilder sb = null;
        int i = 0;
        while (i < length) {
            char c = s.charAt(i);
            if ('\t' == c || ' ' == c) {
                int k = i;
                while ((i + 1) < length && ' ' == s.charAt(i + 1)) {
                    i++;
                }
                if (k != i) {
                    if (null == sb) {
                        sb = new StringBuilder(length);
                        if (k > 0) {
                            sb.append(s, 0, k);
                        }
                    }
                    sb.append(' ');
                } else {
                    if (null != sb) {
                        sb.append(c);
                    }
                }
            } else if ('\r' != c && '\n' != c) {
                if (null != sb) {
                    sb.append(c);
                }
            }
            i++;
        }

        return decodeEnvelopeHeader0(null == sb ? s : sb.toString());
    }

    /**
     * Decodes a string header obtained from ENVELOPE fetch item.
     *
     * @param value The header value
     * @return The decoded header value
     */
    public static String decodeEnvelopeHeader(String value) {
        return decodeEnvelopeHeader0(checkNonAscii(value));
    }

    /**
     * Internal method to decode a string header obtained from ENVELOPE fetch item.
     *
     * @param value The header value
     * @return The decoded header value
     */
    private static String decodeEnvelopeHeader0(String value) {
        /*
         * Passes possibly encoded-word is greater than 75 characters and contains no CR?LF
         */
        boolean hasCRLF = (value.indexOf('\r') < 0) && (value.indexOf('\n') < 0);
        return decodeMultiEncodedHeader0(removeWhitespacesBetweenEncodedWords(value), !hasCRLF);
    }

    private static String removeWhitespacesBetweenEncodedWords(String value) {
        String pattern = "?= =?";
        int i = value.indexOf(pattern);
        if (i >= 0) {
            StringBuilder sb = new StringBuilder(value);
            do {
                sb.deleteCharAt(i + 2);
            } while ((i = sb.indexOf(pattern)) >= 0);
            return sb.toString();
        }

        pattern = "?=\t=?";
        i = value.indexOf(pattern);
        if (i >= 0) {
            StringBuilder sb = new StringBuilder(value);
            do {
                sb.deleteCharAt(i + 2);
            } while ((i = sb.indexOf(pattern)) >= 0);
            return sb.toString();
        }

        return value;
    }

    private static final Pattern ENC_PATTERN = Pattern.compile("=\\?(\\S+?)\\?(\\S+?)\\?(.*?)\\?=");

    /**
     * Decodes a multi-mime-encoded header value using the algorithm specified in RFC 2047, Section 6.1.
     * <p>
     * If the charset-conversion fails for any sequence, an {@link UnsupportedEncodingException} is thrown.
     * <p>
     * If the String is not a RFC 2047 style encoded header, it is returned as-is
     *
     * @param headerValue The possibly encoded header value
     * @return The possibly decoded header value
     */
    public static String decodeMultiEncodedHeader(String headerValue) {
        if ((null == headerValue) || (headerValue.indexOf("=?") < 0)) {
            // In case of null, no "=?" marker
            return unfold(headerValue);
        }
        return decodeMultiEncodedHeader0(checkNonAscii(headerValue), true);
    }

    private static final class WritePartToPipeOutRunnable implements Runnable {

        private final PipedOutputStream pos;
        private final ExceptionAwarePipedInputStream pin;
        private final Part part;

        /**
         * Initializes a new {@link WritePartToPipeOutRunnable}.
         */
        WritePartToPipeOutRunnable(Part part, PipedOutputStream pos, ExceptionAwarePipedInputStream pin) {
            super();
            this.pos = pos;
            this.pin = pin;
            this.part = part;
        }

        @Override
        public void run() {
            try {
                part.writeTo(pos);
            } catch (Exception e) {
                pin.setException(e);
            } finally {
                Streams.close(pos);
            }
        }
    }

    private static final class WriteMailPartToPipeOutRunnable implements Runnable {

        private final PipedOutputStream pos;
        private final ExceptionAwarePipedInputStream pin;
        private final MailPart part;

        /**
         * Initializes a new {@link WritePartToPipeOutRunnable}.
         */
        WriteMailPartToPipeOutRunnable(MailPart part, PipedOutputStream pos, ExceptionAwarePipedInputStream pin) {
            super();
            this.pos = pos;
            this.pin = pin;
            this.part = part;
        }

        @Override
        public void run() {
            try {
                part.writeTo(pos);
            } catch (Exception e) {
                pin.setException(e);
            } finally {
                Streams.close(pos);
            }
        }
    }

    private static final class Base64EncodedValue {

        final String charset;
        final StringBuilder value;

        Base64EncodedValue(String charset, String value) {
            super();
            this.charset = charset;
            this.value = new StringBuilder(value);
        }

        boolean combine(Base64EncodedValue other) {
            if (charset.equals(other.charset)) {
                value.append(other.value);
                return true;
            }

            return false;
        }

        boolean isPadded() {
            int length = value.length();
            return length > 0 && value.charAt(length - 1) == '=';
        }
    }

    private static String decodeMultiEncodedHeader0(String headerValue, boolean unfold) {
        if (headerValue == null) {
            return null;
        }
        String hdrVal = unfold ? unfold(headerValue) : headerValue;
        /*
         * Whether the sequence "=?" exists at all
         */
        if (hdrVal.indexOf("=?") < 0) {
            return hdrVal;
        }
        String decoded;
        do {
            decoded = null;
            Matcher m = ENC_PATTERN.matcher(hdrVal);
            if (m.find()) {
                StringBuilder sb = new StringBuilder(hdrVal.length());
                int lastMatch = 0;
                Base64EncodedValue prev = null;
                do {
                    String encoding = m.group(2);
                    String encodedValue = m.group(3);
                    try {
                        String charset = m.group(1);

                        boolean doEncode = true;
                        int start = m.start();
                        if (lastMatch == 0 || lastMatch == start) {
                            if (lastMatch != start) {
                                sb.append(hdrVal.substring(lastMatch, start));
                            }
                            if ("b".equalsIgnoreCase(encoding)) {
                                Base64EncodedValue ev = new Base64EncodedValue(charset, encodedValue);
                                if (null == prev) {
                                    // Only combineable if not padded
                                    if (!encodedValue.endsWith("=")) {
                                        // Is padded...
                                        prev = ev;
                                        doEncode = false;
                                    }
                                } else {
                                    if (!prev.combine(ev)) {
                                        sb.append(decodeEncodedWord(prev.charset, "B", prev.value.toString()));
                                        prev = ev;
                                    } else if (prev.isPadded()) {
                                        sb.append(decodeEncodedWord(prev.charset, "B", prev.value.toString()));
                                        prev = null;
                                    }
                                    doEncode = false;
                                }
                            } else if (null != prev) {
                                sb.append(decodeEncodedWord(prev.charset, "B", prev.value.toString()));
                                prev = null;
                            }
                        } else {
                            if (null != prev) {
                                sb.append(decodeEncodedWord(prev.charset, "B", prev.value.toString()));
                                prev = null;
                            }
                            sb.append(hdrVal.substring(lastMatch, start));
                        }

                        // Decode encoded-word
                        if (doEncode) {
                            sb.append(decodeEncodedWord(charset, encoding, encodedValue));
                        }

                        // Remember last match position
                        lastMatch = m.end();
                    } catch (UnsupportedTransferEncodingException | ParseException e) {
                        return decodeMultiEncodedHeaderSafe(headerValue);
                    }
                } while (m.find());
                if (null != prev) {
                    try {
                        sb.append(decodeEncodedWord(prev.charset, "B", prev.value.toString()));
                    } catch (UnsupportedTransferEncodingException | ParseException e) {
                        return decodeMultiEncodedHeaderSafe(headerValue);
                    }
                }
                if (lastMatch < hdrVal.length()) {
                    sb.append(hdrVal.substring(lastMatch));
                }
                decoded = sb.toString();
                hdrVal = decoded;
            }
        } while (hdrVal.indexOf("=?") >= 0 && decoded != null);
        if (decoded != null) {
            return decoded;
        }

        // Try to recover from malformed Content-Type value like ``=?windows-1252?q?application/pdf; name="blatt8.pdf"\u00b4\u00b4
        if (!hdrVal.startsWith("=?")) {
            // Encoded word does not start with "=?"
            return hdrVal;
        }
        int start = 2;
        int pos;
        if ((pos = hdrVal.indexOf('?', start)) == -1) {
            // Encoded word does not include charset
            return hdrVal;
        }
        start = pos + 1;
        if ((pos = hdrVal.indexOf('?', start)) == -1) {
            // Encoded word does not include encoding
            return hdrVal;
        }
        start = pos + 1;
        if (hdrVal.indexOf("?=", start) == -1) {
            // Encoded word does not end with "?="
            return hdrVal.substring(start);
        }

        return hdrVal;
    }

    private static String decodeEncodedWord(String charset, String encoding, String encodedValue) throws ParseException, UnsupportedTransferEncodingException {
        if (MessageUtility.isBig5(charset)) {
            String decodeWord = doDecodeEncodedWord(charset, encoding, encodedValue);
            if (MessageUtility.containsUnknown(decodeWord)) {
                decodeWord = doDecodeEncodedWord("Big5-HKSCS", encoding, encodedValue);
            }
            return decodeWord;
        } else if (MessageUtility.isGB2312(charset)) {
            String decodeWord = doDecodeEncodedWord(charset, encoding, encodedValue);
            if (MessageUtility.containsUnknown(decodeWord)) {
                decodeWord = doDecodeEncodedWord("GB18030", encoding, encodedValue);
            }
            return decodeWord;
        } else if (MessageUtility.isShiftJis(charset)) {
            String decodeWord = doDecodeEncodedWord("MS932", encoding, encodedValue);
            if (MessageUtility.containsUnknown(decodeWord)) {
                decodeWord = CP932EmojiMapping.getInstance().replaceIn(doDecodeEncodedWord("MS932", encoding, encodedValue));
            }
            return decodeWord;
        } else if (MessageUtility.isISO2022JP(charset)) {
            String decodeWord = doDecodeEncodedWord("ISO-2022-JP", encoding, encodedValue);
            if (MessageUtility.containsUnknown(decodeWord)) {
                decodeWord = doDecodeEncodedWord("x-windows-iso2022jp", encoding, encodedValue);
            }
            return decodeWord;
        } else {
            return doDecodeEncodedWord(charset, encoding, encodedValue);
        }
    }

    private static String doDecodeEncodedWord(String charset2, String encoding, String encodedValue) throws ParseException, UnsupportedTransferEncodingException {
        String charset = MimeUtility.javaCharset(charset2);
        if (encodedValue.length() <= 0) {
            return "";
        }

        // Extract the bytes
        byte[] asciiBytes = Charsets.toAsciiBytes(encodedValue);
        int count = asciiBytes.length;

        // Get the appropriate decoder
        InputStream is;
        if (encoding.equalsIgnoreCase("B")) {
            is = new BASE64DecoderStream(Streams.newByteArrayInputStream(asciiBytes));
        } else if (encoding.equalsIgnoreCase("Q")) {
            is = new QDecoderStream(Streams.newByteArrayInputStream(asciiBytes));
        } else {
            throw new UnsupportedTransferEncodingException("unknown encoding: " + encoding);
        }

        // Read decoded bytes
        byte[] bytes;
        try {
            bytes = new byte[count];
            // count is set to the actual number of decoded bytes
            count = is.read(bytes, 0, count);

            if (count <= 0) {
                return "";
            }
        } catch (IOException ioex) {
            // Shouldn't happen.
            throw new ParseException(ioex.toString(), ioex);
        } finally {
            Streams.close(is);
        }

        // Return decoded value
        try {
            return new String(bytes, 0, count, charset);
        } catch (java.io.UnsupportedEncodingException uec) {
            LOG.debug("Unsupported character-encoding in encoded-word: {}", encodedValue, uec);
            String decoded = detectCharsetAndDecodeElseNull(bytes, count);
            return null == decoded ? encodedValue : decoded;
        } catch (IllegalArgumentException iex) {
            /*-
             * An unknown charset of the form ISO-XXX-XXX, will cause
             * the JDK to throw an IllegalArgumentException ... Since the
             * JDK will attempt to create a classname using this string,
             * but valid classnames must not contain the character '-',
             * and this results in an IllegalArgumentException, rather than
             * the expected UnsupportedEncodingException. Yikes
             */
            LOG.debug("Unsupported character-encoding in encoded-word: {}", encodedValue, iex);
            String decoded = detectCharsetAndDecodeElseNull(bytes, count);
            return null == decoded ? encodedValue : decoded;
        }
    }

    private static String detectCharsetAndDecodeElseNull(byte[] bytes, int count) {
        String detectedCharset = CharsetDetector.detectCharset(Streams.newByteArrayInputStream(bytes, 0, count));
        try {
            return new String(bytes, 0, count, Charsets.forName(detectedCharset));
        } catch (java.nio.charset.UnsupportedCharsetException uce) {
            /*
             * Even detected charset is unknown... giving up
             */
            LOG.warn("Unknown character-encoding: {}", detectedCharset, uce);
            return null;
        }
    }

    /**
     * Checks if given raw header contains non-ascii characters.
     *
     * @param rawHeader The raw header
     * @return The proper unicode string
     */
    public static String checkNonAscii(String rawHeader) {
        return rawHeader;
    }

    /**
     * Checks whether the specified string's characters are ASCII 7 bit
     *
     * @param s The string to check
     * @return <code>true</code> if string's characters are ASCII 7 bit; otherwise <code>false</code>
     */
    private static boolean isAscii(String s) {
        final int length = s.length();
        boolean isAscci = true;
        for (int i = length; isAscci && i-- > 0;) {
            isAscci &= (s.charAt(i) < 128);
        }
        return isAscci;
    }

    /**
     * Decodes a multi-mime-encoded header value using the algorithm specified in RFC 2047, Section 6.1 in a safe manner.
     *
     * @param headerValue The possibly encoded header value
     * @return The possibly decoded header value
     */
    private static String decodeMultiEncodedHeaderSafe(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        final String hdrVal = unfold(headerValue);
        final Matcher m = ENC_PATTERN.matcher(hdrVal);
        if (m.find()) {
            final StringBuilder sb = new StringBuilder(hdrVal.length());
            StringBuilder tmp = null;
            int lastMatch = 0;
            do {
                try {
                    sb.append(hdrVal.substring(lastMatch, m.start()));
                    if ("Q".equalsIgnoreCase(m.group(2))) {
                        final String charset = m.group(1);
                        final String preparedEWord = prepareQEncodedValue(m.group(3), charset);
                        if (null == tmp) {
                            tmp = new StringBuilder(preparedEWord.length() + 16);
                        } else {
                            tmp.setLength(0);
                        }
                        tmp.append("=?").append(charset).append('?').append('Q').append('?').append(preparedEWord).append("?=");
                        sb.append(MimeUtility.decodeWord(tmp.toString()));
                    } else if ("B".equalsIgnoreCase(m.group(2))) {
                        try { // NOSONARLINT
                            sb.append(MimeUtility.decodeWord(m.group()));
                        } catch (ParseException e) {
                            /*
                             * Retry with another library
                             */
                            sb.append(new String(Base64.decodeBase64(Charsets.toAsciiBytes(m.group(3))), Charsets.forName(m.group(1))));
                        }
                    } else {
                        sb.append(MimeUtility.decodeWord(m.group()));
                    }
                    lastMatch = m.end();
                } catch (UnsupportedEncodingException e) {
                    LOG.debug("Unsupported character-encoding in encoded-word: {}", m.group(), e);
                    sb.append(m.group());
                    lastMatch = m.end();
                } catch (ParseException e) {
                    LOG.debug("String is not an encoded-word as per RFC 2047: {}", m.group(), e);
                    sb.append(m.group());
                    lastMatch = m.end();
                }
            } while (m.find());
            sb.append(hdrVal.substring(lastMatch));
            return sb.toString();
        }
        return hdrVal;
    }

    /**
     * Prepares specified encoded word, thus even corrupt headers are properly handled.<br>
     * Here is an example of such a corrupt header:
     *
     * <pre>
     * =?windows-1258?Q?foo_bar@mail.foobar.com, _Foo_B=E4r_=28fb@somewhere,
     * =@unspecified-domain, =?windows-1258?Q?de=29@mail.foobar.com,
     * _Jane_Doe@mail.foobar.com, ?=
     * </pre>
     *
     * @param eword The possibly corrupt encoded word
     * @param charset The charset
     * @return The prepared encoded word which won't cause a {@link ParseException parse error} during decoding
     */
    private static String prepareQEncodedValue(String eword, String charset) {
        final int len = eword.length();
        int pos = eword.indexOf('=');
        if (pos == -1) {
            return eword;
        }
        final StringBuilder sb = new StringBuilder(len);
        int prev = 0;
        do {
            final int pos1 = pos + 1;
            final int pos2 = pos + 2;
            if (pos2 < len) {
                final char c1 = eword.charAt(pos1);
                final char c2 = eword.charAt(pos2);
                final int nextPos;
                if (isHex(c1) && isHex(c2)) {
                    final int pos3 = pos + 3;
                    if (pos3 > len) {
                        nextPos = len;
                    } else {
                        nextPos = pos3;
                    }
                    sb.append(eword.substring(prev, nextPos));
                } else {
                    nextPos = pos1;
                    if (ENCODINGS.contains(HeaderName.valueOf(charset))) {
                        sb.append(eword.substring(prev, pos)).append("=3D");
                    } else {
                        sb.append(eword.substring(prev, pos)).append(qencode('=', charset));
                    }
                }
                prev = nextPos;
                pos = nextPos < len ? eword.indexOf('=', nextPos) : -1;
            } else {
                if (prev < len) {
                    sb.append(eword.substring(prev));
                    prev = len;
                }
                pos = -1;
            }
        } while (pos != -1);
        if (prev < len) {
            sb.append(eword.substring(prev));
        }
        return sb.toString();
    }

    private static boolean isHex(char c) {
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
    }

    private static String qencode(char toEncode, String charset) {
        if (!CharsetDetector.isValid(charset)) {
            LOG.warn("Illegal or unsupported charset name: {}", charset);
            return String.valueOf(toEncode);
        }
        final StringBuilder retval = new StringBuilder(4);
        try {
            final byte[] bytes = String.valueOf(toEncode).getBytes(charset);
            for (byte b : bytes) {
                retval.append('=').append(Integer.toHexString(b & 0xFF).toUpperCase(Locale.ENGLISH));
            }
        } catch (java.io.UnsupportedEncodingException e) {
            // Cannot occur
            LOG.error("", e);
        }
        return retval.toString();
    }

    /**
     * Get the decoded filename associated with specified mail part.
     * <p>
     * Returns the value of the "filename" parameter from the "Content-Disposition" header field. If its not available, returns the value of
     * the "name" parameter from the "Content-Type" header field. Returns <code>null</code> if both are absent.
     *
     * @param mailPart The mail part whose filename shall be returned
     * @return The mail part's decoded filename or <code>null</code>.
     */
    public static String getFileName(MailPart mailPart) {
        // First look-up content-disposition
        String fileName = mailPart.getContentDisposition().getFilenameParameter();
        if (isEmpty(fileName)) {
            // Then look-up content-type
            fileName = mailPart.getContentType().getNameParameter();
            return decodeMultiEncodedHeader(fileName);
        }
        fileName = decodeMultiEncodedHeader(fileName);
        if (fileName.indexOf('.') < 0) {
            // No file extension given. Check if "name" parameter from Content-Type provides a better "alternative" value
            String name = mailPart.getContentType().getNameParameter();
            if (Strings.isNotEmpty(name) && name.indexOf('.') > 0) {
                fileName = decodeMultiEncodedHeader(name);
            }
        }
        return fileName;
    }

    /**
     * Parse the given sequence of addresses into InternetAddress objects by invoking
     * <code>{@link InternetAddress#parse(String, boolean)}</code>. If <code>strict</code> is false, simple email addresses separated by
     * spaces are also allowed. If <code>strict</code> is true, many (but not all) of the RFC822 syntax rules are enforced. In particular,
     * even if <code>strict</code> is true, addresses composed of simple names (with no "@domain" part) are allowed. Such "illegal"
     * addresses are not uncommon in real messages.
     * <p>
     * Non-strict parsing is typically used when parsing a list of mail addresses entered by a human. Strict parsing is typically used when
     * parsing address headers in mail messages.
     * <p>
     * Additionally the personal parts are MIME encoded using default MIME charset.
     *
     * @param addresslist - comma separated address strings
     * @param strict - <code>true</code> to enforce RFC822 syntax; otherwise <code>false</code>
     * @return An array of <code>InternetAddress</code> objects
     */
    public static InternetAddress[] parseAddressList(String addresslist, boolean strict) {
        try {
            return parseAddressList(addresslist, strict, false);
        } catch (AddressException e) {
            /*
             * Cannot occur
             */
            throw new IllegalStateException("Unexpected exception.", e);
        }
    }

    private static final Pattern SPLITS = Pattern.compile(" *, *");

    /**
     * Split address list
     *
     * @param addrs The address list
     * @return The splitted addresses
     */
    public static List<String> splitAddrs(String addrs) {
        if (isEmpty(addrs)) {
            return Collections.<String> emptyList();
        }
        final String[] sa = SPLITS.split(addrs, 0);
        final List<String> ret = new ArrayList<String>(sa.length);
        final StringBuilder tmp = new StringBuilder(24);
        for (String string : sa) {
            if (string.isEmpty()) {
                continue;
            }
            final String trim = string.trim();
            if (trim.charAt(0) == '"') {
                tmp.setLength(0);
                tmp.append(trim).append(", ");
            } else if (trim.indexOf("\" <") >= 0) {
                tmp.append(trim);
                ret.add(tmp.toString());
                tmp.setLength(0);
            } else if (tmp.length() > 0) {
                tmp.append(string).append(", ");
            } else {
                ret.add(string);
            }
        }
        if (tmp.length() > 0) {
            final String str = tmp.toString();
            ret.add(str.endsWith(", ") ? str.substring(0, str.length() - 2) : str);
        }
        return ret;
    }

    /**
     * Parse the given sequence of addresses into InternetAddress objects by invoking
     * <code>{@link InternetAddress#parse(String, boolean)}</code>. If <code>strict</code> is false, simple email addresses separated by
     * spaces are also allowed. If <code>strict</code> is true, many (but not all) of the RFC822 syntax rules are enforced. In particular,
     * even if <code>strict</code> is true, addresses composed of simple names (with no "@domain" part) are allowed. Such "illegal"
     * addresses are not uncommon in real messages.
     * <p>
     * Non-strict parsing is typically used when parsing a list of mail addresses entered by a human. Strict parsing is typically used when
     * parsing address headers in mail messages.
     * <p>
     * Additionally the personal parts are MIME encoded using default MIME charset.
     *
     * @param addresslist - comma separated address strings
     * @param strict - <code>true</code> to enforce RFC822 syntax; otherwise <code>false</code>
     * @param failOnError - <code>true</code> to fail if parsing fails; otherwise <code>false</code> to get a plain-text representation
     * @return An array of <code>InternetAddress</code> objects
     * @throws AddressException If parsing fails and <code>failOnError</code> is <code>true</code>
     */
    public static InternetAddress[] parseAddressList(String addresslist, boolean strict, boolean failOnError) throws AddressException {
        if (null == addresslist) {
            return new InternetAddress[0];
        }
        final String al = replaceWithComma(unfold(addresslist));
        InternetAddress[] addrs = null;
        try {
            addrs = QuotedInternetAddress.parse(al, strict);
        } catch (AddressException e) {
            // Retry with single parse
            final List<String> sAddrs = splitAddrs(al);
            if (null == sAddrs || sAddrs.isEmpty()) {
                return new InternetAddress[0];
            }
            try {
                final List<InternetAddress> addrList = new ArrayList<InternetAddress>(sAddrs.size());
                for (String sAddr : sAddrs) {
                    final QuotedInternetAddress tmp = new QuotedInternetAddress(sAddr, strict);
                    LOG.trace(tmp.toString());
                    addrList.add(tmp);
                }
                // Hm... single parse did not fail, throw original exception instead
                return addrList.toArray(new InternetAddress[0]);
            } catch (AddressException e1) {
                if (failOnError) {
                    for (String sAddr : sAddrs) {
                        final QuotedInternetAddress tmp = new QuotedInternetAddress(sAddr, strict);
                        LOG.trace(tmp.toString());
                    }
                    // Hm... single parse did not fail, throw original exception instead
                    throw e;
                }
                LOG.debug("Internet addresses could not be properly parsed, using plain addresses' string representation instead.", e);
                addrs = PlainTextAddress.getAddresses(splitAddrs(al).toArray(Strings.getEmptyStrings()));
            }
        }
        try {
            for (InternetAddress addr : addrs) {
                if (null != addr) {
                    addr.setPersonal(addr.getPersonal(), MailProperties.getInstance().getDefaultMimeCharset());
                }
            }
        } catch (UnsupportedEncodingException e) {
            /*
             * Cannot occur since default charset is checked on global mail configuration initialization
             */
            LOG.error("", e);
        }
        return addrs;
    }

    /**
     * Parses a crafted/assembled address from a client.
     *
     * @param address The crafted/assembled address to parse
     * @return The parsed instance of <code>InternetAddress</code>
     * @throws OXException If parsing fails
     */
    public static InternetAddress parseCraftedAddress(String address) throws OXException {
        try {
            if (Strings.isEmpty(address)) {
                throw new AddressException("Address is empty");
            }

            int pos = address.lastIndexOf('<');
            if (pos <= 0) {
                return new QuotedInternetAddress(address);
            }

            String addr = address.substring(pos).trim();
            if (addr.endsWith(">")) {
                addr = addr.substring(1, addr.length() - 1);
            }

            String personal = address.substring(0, pos).trim();
            if (personal.startsWith("\"") && personal.endsWith("\"")) {
                if (personal.length() < 3) {
                    return new QuotedInternetAddress(addr);
                }

                personal = personal.substring(1, personal.length() - 1);
            }

            return new QuotedInternetAddress(addr, personal, "UTF-8");
        } catch (AddressException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (UnsupportedEncodingException e) {
            throw MailExceptionCode.ENCODING_ERROR.create("UTF-8");
        }
    }

    private static volatile Boolean checkReplaceWithComma;

    private static boolean checkReplaceWithComma() {
        Boolean b = checkReplaceWithComma;
        if (null == b) {
            synchronized (MimeMessageUtility.class) {
                b = checkReplaceWithComma;
                if (null == b) {
                    final boolean fallback = false;
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return fallback;
                    }
                    b = Boolean.valueOf(service.getBoolProperty("com.openexchange.mail.replaceWithComma", fallback));
                    checkReplaceWithComma = b;
                }
            }
        }
        return b.booleanValue();
    }

    static {
        MailReloadable.getInstance().addReloadable(new Reloadable() {

            @Override
            public void reloadConfiguration(ConfigurationService configService) {
                checkReplaceWithComma = null;
            }

            @Override
            public Interests getInterests() {
                return Reloadables.interestsForProperties("com.openexchange.mail.replaceWithComma");
            }
        });
    }

    private static final Pattern PATTERN_REPLACE = Pattern.compile("(\\.\\w+>?)(\\s*);(\\s*)");

    /**
     * Sanitizes address list
     *
     * @param addressList The address list
     * @return The sanitized address list
     */
    public static String replaceWithComma(String addressList) {
        if (null == addressList) {
            return null;
        }
        if (!checkReplaceWithComma()) {
            return addressList;
        }
        final Matcher m = PATTERN_REPLACE.matcher(addressList);
        if (!m.find()) {
            return addressList;
        }
        final StringBuilder sb = new StringBuilder(addressList.length());
        int lastMatch = 0;
        do {
            sb.append(addressList.substring(lastMatch, m.start()));
            sb.append(m.group(1)).append(m.group(2)).append(',').append(m.group(3));
            lastMatch = m.end();
        } while (m.find());
        sb.append(addressList.substring(lastMatch));
        return sb.toString();
    }

    /**
     * Returns a literal replacement <code>String</code> for the specified <code>String</code>. This method produces a <code>String</code>
     * that will work as a literal replacement <code>s</code> in the <code>appendReplacement</code> method of the {@link Matcher} class. The
     * <code>String</code> produced will match the sequence of characters in <code>s</code> treated as a literal sequence. Slashes ('\') and
     * dollar signs ('$') will be given no special meaning.
     *
     * @param s The string to be literalized
     * @return A literal string replacement
     */
    public static String quoteReplacement(String s) {
        if (isEmpty(s) || ((s.indexOf('\\') == -1) && (s.indexOf('$') == -1))) {
            return s;
        }
        final int length = s.length();
        final StringBuilder sb = new StringBuilder(length << 1);
        for (int i = 0; i < length; i++) {
            final char c = s.charAt(i);
            if (c == '\\') {
                sb.append('\\');
                sb.append('\\');
            } else if (c == '$') {
                sb.append('\\');
                sb.append('$');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final String RFC822 = "()<>@,;:\\\".[]";

    /**
     * Quotes given personal part of an Internet address according to RFC 822 syntax if needed; otherwise the personal is returned
     * unchanged.
     * <p>
     * This method guarantees that the resulting string can be used to build an Internet address according to RFC 822 syntax so that the
     * <code>{@link InternetAddress#parse(String)}</code> constructor won't throw an instance of <code>{@link AddressException}</code>.
     *
     * <pre>
     * final String quotedPersonal = quotePersonal(&quot;Doe, Jane&quot;);
     *
     * final String buildAddr = quotedPersonal + &quot; &lt;someone@somewhere.com&gt;&quot;
     * System.out.println(buildAddr);
     * // Plain Address: &quot;=?UTF-8?Q?Doe=2C_Jan=C3=A9?=&quot; &lt;someone@somewhere.com&gt;
     *
     * final InternetAddress ia = new InternetAddress(buildAddr);
     * System.out.println(ia.toUnicodeString());
     * // Unicode Address: &quot;Doe, Jane&quot; &lt;someone@somewhere.com&gt;
     * </pre>
     *
     * @param personal The personal's string representation
     * @return The properly quoted personal for building an Internet address according to RFC 822 syntax
     */
    public static String quotePersonal(String personal) {
        return quotePhrase(personal, true);
    }

    /**
     * Quotes given phrase if needed.
     *
     * @param phrase The phrase
     * @param encode <code>true</code> to encode phrase according to RFC 822 syntax if needed; otherwise <code>false</code>
     * @return The quoted phrase
     */
    public static String quotePhrase(String phrase, boolean encode) {
        return quotePhrase(phrase, encode, true);
    }

    private static final Pattern P_REPL1 = Pattern.compile("\\\\");
    private static final Pattern P_REPL2 = Pattern.compile("\"");

    /**
     * Quotes given phrase if needed.
     *
     * @param phrase The phrase
     * @param encode <code>true</code> to encode phrase according to RFC 822 syntax if needed; otherwise <code>false</code>
     * @param allowNonAscii Whether non-ascci characters need quoting or not
     * @return The quoted phrase
     */
    public static String quotePhrase(String phrase, boolean encode, boolean allowNonAscii) {
        if (null == phrase) {
            return phrase;
        }
        final int len = phrase.length();
        if (len == 0) {
            return phrase;
        }
        if ('"' == phrase.charAt(0) && '"' == phrase.charAt(len - 1)) {
            /*
             * Already quoted
             */
            return phrase;
        }
        boolean needQuoting = false;
        for (int i = 0; !needQuoting && i < len; i++) {
            final char c = phrase.charAt(i);
            needQuoting = (c == '"' || c == '\\' || (c < 32 && c != '\r' && c != '\n' && c != '\t') || (!allowNonAscii && c >= 127) || RFC822.indexOf(c) >= 0);
        }
        try {
            if (!needQuoting) {
                return encode ? MimeUtility.encodeWord(phrase) : phrase;
            }
            final String replaced = P_REPL2.matcher(P_REPL1.matcher(phrase).replaceAll("\\\\\\\\")).replaceAll("\\\\\\\"");
            return new StringBuilder(len + 2).append('"').append(encode ? MimeUtility.encodeWord(replaced) : replaced).append('"').toString();
        } catch (UnsupportedEncodingException e) {
            LOG.error("Unsupported encoding in a message detected and monitored", e);
            mailInterfaceMonitor.addUnsupportedEncodingExceptions(e.getMessage());
            return phrase;
        }
    }

    private static final int USED_CT = 14;

    /**
     * Folds specified <code>Content-Type</code> value.
     *
     * @param contentDisposition The <code>Content-Type</code> value
     * @return The folded <code>Content-Type</code> value
     */
    public static String foldContentType(String contentType) {
        return fold(USED_CT, contentType);
    }

    private static final int USED_CD = 21;

    /**
     * Folds specified <code>Content-Disposition</code> value.
     *
     * @param contentDisposition The <code>Content-Disposition</code> value
     * @return The folded <code>Content-Disposition</code> value
     */
    public static String foldContentDisposition(String contentDisposition) {
        return fold(USED_CD, contentDisposition);
    }

    /**
     * Set the folded value for given header name. Replaces all existing header values with this new value.<br>
     * The header value gets folded at linear whitespace so that each line is no longer than 76 characters.
     * <p>
     * Note that RFC 822 headers must contain only US-ASCII characters, so a header that
     * contains non US-ASCII characters must have been encoded by the
     * caller as per the rules of RFC 2047.
     *
     * @param headerName The header name
     * @param headerValue The header value to set
     * @param mimeMessage The MIME message
     * @throws MessagingException If setting the header fails
     */
    public static void setFoldedHeader(String headerName, String headerValue, MimeMessage mimeMessage) throws MessagingException {
        if (null != headerName && null != headerValue && null != mimeMessage) {
            mimeMessage.setHeader(headerName, fold(headerName.length() + 2, headerValue));
        }
    }

    /**
     * Folds a string at linear whitespace so that each line is no longer than 76 characters, if possible. If there are more than 76
     * non-whitespace characters consecutively, the string is folded at the first whitespace after that sequence. The parameter
     * <tt>used</tt> indicates how many characters have been used in the current line; it is usually the length of the header name.
     * <p>
     * Note that line breaks in the string aren't escaped; they probably should be.
     *
     * @param used The characters used in line so far; typically the length of the header name plus 2 (for <code>": "</code> part)
     * @param foldMe The string to fold
     * @return The folded string
     */
    public static String fold(int used, String foldMe) {
        return MimeUtility.fold(used, foldMe);
    }

    /**
     * Folds a string at linear whitespace so that each line is no longer than 76 characters, if that string contains a whitespace.
     * Otherwise a forced folding is performed.
     *
     * @param used The characters used in line so far; typically the length of the header name plus 2 (for <code>": "</code> part)
     * @param foldMe The string to fold
     * @return The folded string
     * @see #fold(int, String)
     */
    public static String forceFold(int used, String foldMe) {
        int usedInLine = used < 0 ? 0 : used;
        int length = foldMe.length();
        if (usedInLine + length <= 76) {
            return foldMe;
        }

        if (foldMe.indexOf(' ') > 0) { // NOSONARLINT
            // Contains a whitespace
            return MimeUtility.fold(usedInLine, foldMe);
        }

        StringBuilder sb = new StringBuilder(length + 16);
        int start = 0;
        while (start < length) {
            int end = Math.min(start + 76 - usedInLine, length);
            if (start == 0) {
                usedInLine = 1;
            }
            if (sb.length() > 0) {
                sb.append("\r\n ");
            }
            sb.append(foldMe.substring(start, end));
            start = end;
        }
        return sb.toString();
    }

    private static final Pattern PATTERN_UNFOLD = Pattern.compile("(\\?=)(\\s*)(=\\?)");

    /**
     * Unfolds a folded header. Any line breaks that aren't escaped and are followed by whitespace are removed.
     *
     * @param headerLine The header line to unfold
     * @return The unfolded string
     */
    public static String unfold(String headerLine) {
        if (null == headerLine) {
            return null;
        }

        boolean hasEncodingMark = headerLine.indexOf("?=", 0) >= 0;
        String s = hasEncodingMark ? PATTERN_UNFOLD.matcher(headerLine).replaceAll("$1$3") : headerLine;

        int i;
        if ((i = headerLine.indexOf('\r', 0)) < 0 && (i = headerLine.indexOf('\n', 0)) < 0) {
            return s;
        }
        /*-
         * Check folded encoded-words as per RFC 2047:
         *
         * An 'encoded-word' may not be more than 75 characters long, including
         * 'charset', 'encoding', 'encoded-text', and delimiters.  If it is
         * desirable to encode more text than will fit in an 'encoded-word' of
         * 75 characters, multiple 'encoded-word's (separated by CRLF SPACE) may
         * be used.
         *
         * In this case the SPACE character is not part of the header and should
         * be discarded.
         */
        if (!hasEncodingMark) {
            s = headerLine;
        } else {
            s = unfoldEncodedWords(headerLine);
            if ((i = s.indexOf('\r')) < 0 && (i = s.indexOf('\n')) < 0) {
                return s;
            }
        }
        StringBuilder sb = null;
        do {
            final int start = i;
            final int len = s.length();
            i++; // skip CR or NL
            if ((i < len) && (s.charAt(i - 1) == '\r') && (s.charAt(i) == '\n')) {
                i++; // skip LF
            }
            if (start == 0 || s.charAt(start - 1) != '\\') {
                char c;
                /*
                 * If next line starts with whitespace, skip all of it
                 */
                if ((i < len) && (((c = s.charAt(i)) == ' ') || (c == '\t'))) {
                    i++; // skip whitespace
                    while ((i < len) && (((c = s.charAt(i)) == ' ') || (c == '\t'))) {
                        i++;
                    }
                    if (sb == null) {
                        sb = new StringBuilder(s.length());
                    }
                    if (start != 0) {
                        sb.append(s.substring(0, start));
                        sb.append(' ');
                    }
                    s = s.substring(i);
                } else {
                    /*
                     * It's not a continuation line, just leave it in
                     */
                    if (sb == null) {
                        sb = new StringBuilder(s.length());
                    }
                    sb.append(s.substring(0, i));
                    s = s.substring(i);
                }
            } else {
                /*
                 * There's a backslash at "start - 1", strip it out, but leave in the line break
                 */
                if (sb == null) {
                    sb = new StringBuilder(s.length());
                }
                sb.append(s.substring(0, start - 1));
                sb.append(s.substring(start, i));
                s = s.substring(i);
            }
        } while ((i = s.indexOf('\r')) >= 0 || (i = s.indexOf('\n')) >= 0);
        sb.append(s);
        return sb.toString();
    }

    private static final Pattern PAT_ENC_WORDS;

    static {
        final String regex = "(\\?=)" + "(?:\r?\n(?:\t| +))" + "(=\\?)"; // NOSONARLINT
        PAT_ENC_WORDS = Pattern.compile(regex);
        // PAT_ENC_WORDS = Pattern.compile(regexEncodedWord + "(?:\r?\n(?:\t| +))" + regexEncodedWord);
    }

    /**
     * Unfolds encoded-words as per RFC 2047. When unfolding a non-encoded-word the preceding space character should not be stripped out,
     * but should when unfolding encoded-words.
     * <p>
     * &quot;...<br>
     * An 'encoded-word' may not be more than 75 characters long, including 'charset', 'encoding', 'encoded-text', and delimiters. If it is
     * desirable to encode more text than will fit in an 'encoded-word' of 75 characters, multiple 'encoded-word's (separated by CRLF SPACE)
     * may be used.&quot;
     * <p>
     *
     * <pre>
     * Subject: =?UTF-8?Q?Kombatibilit=C3=A4t?=\r\n =?UTF-8?Q?sliste?=
     * </pre>
     *
     * Should be unfolded to:
     *
     * <pre>
     * Subject: =?UTF-8?Q?Kombatibilit=C3=A4t?==?UTF-8?Q?sliste?=
     * </pre>
     *
     * @param encodedWords The possibly folded encoded-words
     * @return The unfolded encoded-words
     */
    private static String unfoldEncodedWords(String encodedWords) {
        return PAT_ENC_WORDS.matcher(encodedWords).replaceAll("$1$2");
    }

    private static final int BUFSIZE = 8192; // 8K

    /**
     * Gets the matching header out of RFC 822 data input stream.
     *
     * @param headerName The header name
     * @param inputStream The input stream
     * @param closeStream <code>true</code> to close the stream on finish; otherwise <code>false</code>
     * @return The value of first appeared matching header
     * @throws IOException If reading input stream fails
     */
    public static String extractHeader(String headerName, InputStream inputStream, boolean closeStream) throws IOException {
        boolean close = closeStream;
        final UnsynchronizedByteArrayOutputStream buffer = new UnsynchronizedByteArrayOutputStream(BUFSIZE);
        try {
            /*
             * Gather bytes until empty line, EOF or matching header found
             */
            int start = 0;
            int i = -1;
            boolean firstColonFound = false;
            boolean found = false;
            while ((i = inputStream.read()) != -1) {
                int count = 0;
                while ((i == '\r') || (i == '\n')) {
                    if (!found) {
                        buffer.write(i);
                    }
                    if ((i == '\n')) {
                        if (found) {
                            i = inputStream.read();
                            if ((i != ' ') && (i != '\t')) {
                                /*
                                 * All read
                                 */
                                return Charsets.toAsciiString(buffer.toByteArray());
                            }
                            /*
                             * Write previously ignored CRLF
                             */
                            buffer.write('\r');
                            buffer.write('\n');
                            /*
                             * Continue collecting header value
                             */
                            buffer.write(i);
                        }
                        if (++count >= 2) {
                            /*
                             * End of headers
                             */
                            return null;
                        }
                        i = inputStream.read();
                        if ((i != ' ') && (i != '\t')) {
                            /*
                             * No header continuation; start of a new header
                             */
                            start = buffer.size();
                            firstColonFound = false;
                        }
                        buffer.write(i);
                    }
                    i = inputStream.read();
                }
                buffer.write(i);
                if (!firstColonFound && (i == ':')) {
                    /*
                     * Found the first delimiting colon in header line
                     */
                    firstColonFound = true;
                    if ((Charsets.toAsciiString(buffer.toByteArray(start, buffer.size() - start - 1)).equalsIgnoreCase(headerName))) {
                        /*
                         * Matching header
                         */
                        buffer.reset();
                        found = true;
                    }
                }
            }
            return null;
        } catch (IOException e) {
            // Close on error
            close = true;
            throw e;
        } finally {
            if (close) {
                Streams.close(inputStream);
            }
            Streams.close(buffer);
        }
    }

    /**
     * Writes specified part's headers to given output stream.
     *
     * @param p The part
     * @param os The output stream
     * @throws OXException If an I/O error occurs
     */
    public static void writeHeaders(MailPart p, OutputStream os) throws OXException {
        if (p instanceof MimeMailMessage) {
            writeHeaders(((MimeMailMessage) p).getMimeMessage(), os);
            return;
        }
        if (p instanceof MimeMailPart) {
            writeHeaders(((MimeMailPart) p).getPart(), os);
            return;
        }
        try {
            final LineOutputStream los;
            if (os instanceof LineOutputStream) {
                los = (LineOutputStream) os;
            } else {
                los = new LineOutputStream(os);
            }
            /*
             * Write headers
             */
            final StringBuilder sb = new StringBuilder(256);
            for (Iterator<Entry<String, String>> it = p.getHeadersIterator(); it.hasNext();) {
                final Entry<String, String> entry = it.next();
                sb.setLength(0);
                sb.append(entry.getKey()).append(": ");
                sb.append(fold(sb.length(), entry.getValue()));
                los.writeln(sb);
            }
            /*
             * The CRLF separator between header and content
             */
            los.writeln();
            os.flush();
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Writes specified part's headers to given output stream.
     *
     * @param p The part
     * @param os The output stream
     * @throws OXException If an error occurs
     */
    public static void writeHeaders(Part p, OutputStream os) throws OXException {
        if (p instanceof MimePart) {
            writeHeaders((MimePart) p, os);
            return;
        }
        try {
            final LineOutputStream los;
            if (os instanceof LineOutputStream) {
                los = (LineOutputStream) os;
            } else {
                los = new LineOutputStream(os);
            }
            /*
             * Write headers
             */
            Enumeration<Header> headers = p.getAllHeaders();
            final StringBuilder sb = new StringBuilder(256);
            while (headers.hasMoreElements()) {
                final Header header = headers.nextElement();
                sb.setLength(0);
                sb.append(header.getName()).append(": ");
                sb.append(fold(sb.length(), header.getValue()));
                los.writeln(sb);
            }
            /*
             * The CRLF separator between header and content
             */
            los.writeln();
            os.flush();
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Writes specified part's headers to given output stream.
     *
     * @param p The part
     * @param os The output stream
     * @throws OXException If an error occurs
     */
    public static void writeHeaders(MimePart p, OutputStream os) throws OXException {
        try {
            final LineOutputStream los;
            if (os instanceof LineOutputStream) {
                los = (LineOutputStream) os;
            } else {
                los = new LineOutputStream(os);
            }
            /*
             * Write headers
             */
            for (Enumeration<String> hdrLines = p.getNonMatchingHeaderLines(null); hdrLines.hasMoreElements();) {
                los.writeln(hdrLines.nextElement());
            }
            /*
             * The CRLF separator between header and content
             */
            los.writeln();
            os.flush();
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    private static final class LineOutputStream extends FilterOutputStream { // NOSONARLINT

        private static final byte[] NEWLINE = { (byte) '\r', (byte) '\n' };

        /**
         * Initializes a new {@link LineOutputStream}.
         *
         * @param out The backing output stream
         */
        LineOutputStream(OutputStream out) {
            super(out);
        }

        protected void writeln(CharSequence s) throws IOException {
            out.write(getBytes(s));
            out.write(NEWLINE);
        }

        protected void writeln() throws IOException {
            out.write(NEWLINE);
        }

        private static byte[] getBytes(CharSequence s) {
            if (null == s) {
                return new byte[0];
            }
            final int len = s.length();
            final byte[] bytes = new byte[len];
            for (int i = 0; i < len; i++) {
                bytes[i] = (byte) s.charAt(i);
            }
            return bytes;
        }
    }

    /**
     * Gets the denoted (single) header.
     *
     * @param name The header name
     * @param defaultValue The default value to return if absent
     * @param part The part to look-up
     * @return The header value or <code>defaultValue</code>
     * @throws MessagingException If returning header fails
     */
    public static String getHeader(String name, String defaultValue, Part part) throws MessagingException {
        if (null == name || null == part) {
            return defaultValue;
        }
        final String[] header = part.getHeader(name);
        if (null == header || 0 == header.length) {
            return defaultValue;
        }
        return isEmpty(header[0]) ? defaultValue : header[0];
    }

    /**
     * Checks if given part is considered as an inline part, that is if:<br>
     * <ul>
     * <li>Part's disposition is equal to <code>&quot;inline&quot;</code> OR </li>
     * <li>Part has no (file) name</li>
     * </ul>
     *
     * @param part The part to check
     * @return <code>true</code> if inline; otherwise <code>false</code>
     * @throws MessagingException If check fails
     */
    public static boolean isInline(Part part) throws MessagingException {
        if (null == part) {
            return false;
        }
        final String disposition = asciiLowerCase(getHeader("Content-Disposition", null, part));
        if (null != disposition) {
            return disposition.startsWith("inline") || disposition.indexOf("filename=") < 0;
        }
        // Check name
        final String type = asciiLowerCase(getHeader("Content-Type", "", part));
        return type.indexOf("name=") < 0;
    }

    private static final String X_ORIGINAL_HEADERS = "x-original-headers";

    /**
     * Drops invalid "X-Original-Headers" header from RFC822 source file.
     *
     * @param file The file
     * @param newTempFile The new file (to write cleansed content to)
     * @return The resulting file
     */
    public static File dropInvalidHeaders(File file, File newTempFile) {
        InputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file), 65536);
            out = new BufferedOutputStream(new FileOutputStream(newTempFile), 65536);
            {
                @SuppressWarnings("resource") final LineReaderInputStream instream = new LineReaderInputStreamAdaptor(in, -1);
                int lineCount = 0;
                final ByteArrayBuffer linebuf = new ByteArrayBuffer(64);
                final FieldBuilder fieldBuilder = new DefaultFieldBuilder(-1);
                boolean endOfHeader = false;
                while (!endOfHeader) {
                    fieldBuilder.reset();
                    for (;;) {
                        // If there's still data stuck in the line buffer
                        // copy it to the field buffer
                        int len = linebuf.length();
                        if (len > 0) {
                            fieldBuilder.append(linebuf);
                        }
                        linebuf.clear();
                        if (instream.readLine(linebuf) == -1) {
                            endOfHeader = true;
                            break;
                        }
                        len = linebuf.length();
                        if (len > 0 && linebuf.byteAt(len - 1) == '\n') {
                            len--;
                        }
                        if (len > 0 && linebuf.byteAt(len - 1) == '\r') {
                            len--;
                        }
                        if (len == 0) {
                            // empty line detected
                            endOfHeader = true;
                            break;
                        }
                        lineCount++;
                        if (lineCount > 1) {
                            final int ch = linebuf.byteAt(0);
                            if (ch != CharsetUtil.SP && ch != CharsetUtil.HT) {
                                // new header detected
                                break;
                            }
                        }
                    }
                    final RawField rawfield = fieldBuilder.build();
                    if (rawfield != null && !X_ORIGINAL_HEADERS.equalsIgnoreCase(rawfield.getName())) {
                        final ByteArrayBuffer buffer = fieldBuilder.getRaw();
                        out.write(buffer.buffer(), 0, buffer.length());
                    }
                } // End of Headers
            }
            // Write rest
            final int l = 2048;
            final byte[] buf = new byte[l];
            for (int read; (read = in.read(buf, 0, l)) > 0;) {
                out.write(buf, 0, read);
            }
            out.flush();
            return newTempFile;
        } catch (IOException | MimeException e) {
            LOG.debug("An error occurred", e);
            return file;
        } finally {
            Streams.close(in, out);
        }
    }

    private static final String HDR_CONTENT_TYPE = MessageHeaders.HDR_CONTENT_TYPE;
    private static final String PRIMARY_TEXT = "text/";

    /**
     * Gets the appropriate charset encoding for specified mail part.
     *
     * @param mailPart The part
     * @param contentType The part's Content-Type
     * @return The appropriate charset
     * @throws OXException If detecting charset fails
     */
    public static String getCharset(MailPart mailPart, ContentType contentType) throws OXException {
        if (null == mailPart) {
            return null;
        }
        final String charset;
        if (mailPart.containsHeader(HDR_CONTENT_TYPE)) {
            String cs = contentType.getCharsetParameter();
            if (!CharsetDetector.isValid(cs)) {
                if (null != cs) {
                    mailInterfaceMonitor.addUnsupportedEncodingExceptions(cs);
                }
                if (contentType.startsWith(PRIMARY_TEXT)) {
                    cs = CharsetDetector.detectCharset(mailPart.getInputStream());
                    if ("US-ASCII".equalsIgnoreCase(cs)) {
                        cs = "ISO-8859-1";
                    }
                } else {
                    cs = MailProperties.getInstance().getDefaultMimeCharset();
                }
            }
            charset = cs;
        } else {
            if (contentType.startsWith(PRIMARY_TEXT)) {
                final InputStream inputStream = mailPart.getInputStream();
                if (null == inputStream) {
                    charset = MailProperties.getInstance().getDefaultMimeCharset();
                } else {
                    charset = CharsetDetector.detectCharset(inputStream);
                }
            } else {
                charset = MailProperties.getInstance().getDefaultMimeCharset();
            }
        }
        return charset;
    }

    /**
     * Reads the textual content from specified part.
     *
     * @param mailPart The mail part
     * @param contentType The content type
     * @return The textual part or <code>null</code> if part does not exist
     * @throws OXException If reading content fails
     * @throws IOException If reading content fails with an I/O error
     */
    public static String readContent(MailPart mailPart, ContentType contentType) throws OXException, IOException {
        return readContent(mailPart, contentType, false);
    }

    /**
     * Reads the textual content from specified part.
     *
     * @param mailPart The mail part
     * @param contentType The content type
     * @param errorOnNoContent Whether to throw an error if read attempt causes a <i>"No content"</i> I/O exception
     * @return The textual part or <code>null</code> if part does not exist
     * @throws OXException If reading content fails
     * @throws IOException If reading content fails with an I/O error
     */
    public static String readContent(MailPart mailPart, ContentType contentType, boolean errorOnNoContent) throws OXException, IOException {
        return readContent(mailPart, contentType, errorOnNoContent, MailProperties.getInstance().getBodyDisplaySize());
    }

    /**
     * Reads the textual content from specified part.
     *
     * @param mailPart The mail part
     * @param contentType The content type
     * @param errorOnNoContent Whether to throw an error if read attempt causes a <i>"No content"</i> I/O exception
     * @param maxSize The maximum size to read
     * @return The textual part or <code>null</code> if part does not exist
     * @throws OXException If reading content fails
     * @throws IOException If reading content fails with an I/O error
     */
    public static String readContent(MailPart mailPart, ContentType contentType, boolean errorOnNoContent, long maxSize) throws OXException, IOException {
        if (null == mailPart) {
            return null;
        }
        /*
         * Read content
         */
        final String charset = getCharset(mailPart, contentType);
        try {
            if (contentType.startsWith("text/htm")) {
                final String html = MessageUtility.readMailPart(mailPart, charset, errorOnNoContent, maxSize);
                return MessageUtility.simpleHtmlDuplicateRemoval(html);
            }
            return MessageUtility.readMailPart(mailPart, charset, errorOnNoContent, maxSize);
        } catch (java.io.CharConversionException e) {
            // Obviously charset was wrong or bogus implementation of character conversion
            final String fallback = "ISO-8859-1";
            LOG.warn("Character conversion exception while reading content with charset \"{}\". Using fallback charset \"{}\" instead.", charset, fallback, e);
            return MessageUtility.readMailPart(mailPart, fallback, errorOnNoContent, maxSize);
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                LOG.warn("Mail part removed in the meantime.", e);
                return null;
            }
            throw e;
        }
    }

    /**
     * Constructs a MimeMessage by reading and parsing the data from the specified MIME input stream.
     *
     * @param is The MIME input stream
     * @param retainDate <code>true</code> to retain the date (align received date to sent date); otherwise <code>false</code>
     * @return The new {@link MimeMessage} instance
     * @throws OXException If a new {@link MimeMessage} instance cannot be returned
     */
    public static MimeMessage newMimeMessage(InputStream is, boolean retainDate) throws OXException {
        return newMimeMessage(is, null, retainDate);
    }

    /**
     * Constructs a MimeMessage by reading and parsing the data from the specified MIME input stream.
     *
     * @param is The MIME input stream
     * @param optReceivedDate The optional received date or <code>null</code>
     * @return The new {@link MimeMessage} instance
     * @throws OXException If a new {@link MimeMessage} instance cannot be returned
     */
    public static MimeMessage parseMimeMessageFrom(InputStream is, Date optReceivedDate) throws OXException {
        return newMimeMessage(is, optReceivedDate, false);
    }

    /**
     * Constructs a MimeMessage by reading and parsing the data from the specified MIME input stream.
     *
     * @param is The MIME input stream
     * @param optReceivedDate The optional received date or <code>null</code>
     * @return The new {@link MimeMessage} instance
     * @throws OXException If a new {@link MimeMessage} instance cannot be returned
     */
    public static MimeMessage newMimeMessage(InputStream is, Date optReceivedDate) throws OXException {
        return newMimeMessage(is, optReceivedDate, false);
    }

    private static MimeMessage newMimeMessage(InputStream is, Date optReceivedDate, boolean retainDate) throws OXException {
        InputStream msgSrc = is;
        boolean closeSink = true;
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        try {
            sink.write(msgSrc);
            msgSrc = null;

            File tempFile = sink.getTempFile();
            MimeMessage tmp;
            if (null == tempFile) {
                tmp = new InMemoryMimeMessage(MimeDefaultSession.getDefaultSession(), sink.getStream(), optReceivedDate, retainDate);
            } else {
                tmp = new FileBackedMimeMessage(MimeDefaultSession.getDefaultSession(), tempFile, optReceivedDate, retainDate);
            }
            closeSink = false;
            return tmp;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (closeSink) {
                sink.close();
            }
        }
    }

    /**
     * Constructs a MimeBodyPart by reading and parsing the data from the specified MIME input stream.
     *
     * @param is The MIME input stream
     * @return The new {@link MimeBodyPart} instance
     * @throws OXException If a new {@link MimeMessage} instance cannot be returned
     */
    public static MimeBodyPart newMimeBodyPart(InputStream is) throws OXException {
        InputStream msgSrc = is;
        boolean closeSink = true;
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        try {
            sink.write(msgSrc);
            msgSrc = null;

            File tempFile = sink.getTempFile();
            MimeBodyPart tmp;
            if (null == tempFile) {
                tmp = new MimeBodyPart(sink.getStream());
            } else {
                tmp = new FileBackedMimeBodyPart(tempFile);
            }
            closeSink = false;
            return tmp;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (closeSink) {
                sink.close();
            }
        }
    }

    /**
     * Constructs a DataSource by reading and parsing the data from the specified MIME input stream.
     *
     * @param is The MIME input stream
     * @param contentType The Content-Type to set
     * @return The new {@link DataSource} instance
     * @throws OXException If a new {@link DataSource} instance cannot be returned
     */
    public static DataSource newDataSource(InputStream is, String contentType) throws OXException {
        InputStream msgSrc = is;
        boolean closeSink = true;
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        try {
            sink.write(msgSrc);
            msgSrc = null;

            File tempFile = sink.getTempFile();
            DataSource tmp;
            if (null == tempFile) {
                tmp = new ByteArrayDataSource(sink.getBuffer().toByteArray(), contentType);
            } else {
                tmp = new FileDataSource(tempFile, contentType);
            }
            closeSink = false;
            return tmp;
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (closeSink) {
                sink.close();
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructs a MimeMessage by reading and parsing the data from the specified MIME input stream.
     *
     * @param original The original MIME message
     * @param optReceivedDate The optional received date or <code>null</code>
     * @return The new {@link MimeMessage} instance
     * @throws OXException If a new {@link MimeMessage} instance cannot be returned
     */
    public static MimeMessage cloneMessage(Message original, Date optReceivedDate) throws OXException {
        boolean closeSink = true;
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        try {
            original.writeTo(sink.asOutputStream());

            File tempFile = sink.getTempFile();
            MimeMessage tmp;
            if (null == tempFile) {
                tmp = new InMemoryMimeMessage(MimeDefaultSession.getDefaultSession(), sink.getStream(), optReceivedDate, false);
            } else {
                tmp = new FileBackedMimeMessage(MimeDefaultSession.getDefaultSession(), tempFile, optReceivedDate, false);
            }
            closeSink = false;
            return tmp;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (closeSink) {
                sink.close();
            }
        }
    }

    /**
     * Constructs a MimeBodyPart by reading and parsing the data from the specified MIME input stream.
     *
     * @param is The MIME input stream
     * @return The new {@link MimeBodyPart} instance
     * @throws OXException If a new {@link MimeMessage} instance cannot be returned
     */
    public static MimeBodyPart clonePart(Part part) throws OXException {
        boolean closeSink = true;
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        try {
            part.writeTo(sink.asOutputStream());

            File tempFile = sink.getTempFile();
            MimeBodyPart tmp;
            if (null == tempFile) {
                tmp = new MimeBodyPart(sink.getStream());
            } else {
                tmp = new FileBackedMimeBodyPart(tempFile);
            }
            closeSink = false;
            return tmp;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (closeSink) {
                sink.close();
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the part's source
     *
     * @return The part's source
     * @throws IOException If an error occurs fetching the data to be written
     * @throws MessagingException If an error occurs writing to the stream or if an error is generated by the javax.activation layer
     */
    public static String getSource(Part part) throws IOException, MessagingException {
        return new String(getSourceBytes(part), Charsets.ISO_8859_1);
    }

    /**
     * Gets a newly allocated byte array containing the part's source bytes
     *
     * @return The part's source bytes
     * @throws IOException If an error occurs fetching the data to be written
     * @throws MessagingException If an error occurs writing to the stream or if an error is generated by the javax.activation layer
     */
    public static byte[] getSourceBytes(Part part) throws IOException, MessagingException {
        final ByteArrayOutputStream out = Streams.newByteArrayOutputStream(4096);
        part.writeTo(out);
        return out.toByteArray();
    }

    /**
     * Gets the stream of specified part's raw data.
     *
     * @param part Either a message or a body part
     * @return The stream of specified part's raw data (with the optional empty starting line omitted)
     * @throws IOException If an I/O error occurs
     */
    public static InputStream getStreamFromPart(Part part) throws IOException {
        if (null == part) {
            return null;
        }

        // Initialize pipes
        PipedOutputStream pos = new PipedOutputStream();
        ExceptionAwarePipedInputStream pin = new ExceptionAwarePipedInputStream(pos, 65536);

        // Write part to piped output stream using a separate thread
        ThreadPoolService threadPool = ThreadPools.getThreadPool();
        if (null == threadPool) {
            new Thread(new WritePartToPipeOutRunnable(part, pos, pin), "getStreamFromPart").start();
        } else {
            Runnable r = new WritePartToPipeOutRunnable(part, pos, pin);
            try {
                threadPool.submit(ThreadPools.task(r), AbortBehavior.getInstance());
            } catch (RejectedExecutionException e) {
                new Thread(r, "getStreamFromPart").start();
            }
        }

        // Return piped input stream getting filled with bytes from separate thread
        return pin;
    }

    /**
     * Gets the stream of specified part's raw data.
     *
     * @param part Either a message or a body part
     * @return The stream of specified part's raw data (with the optional empty starting line omitted)
     * @throws OXException If an I/O error occurs
     */
    public static InputStream getStreamFromMailPart(MailPart part) throws OXException {
        if (null == part) {
            return null;
        }
        try {
            // Initialize pipes
            PipedOutputStream pos = new PipedOutputStream();
            ExceptionAwarePipedInputStream pin = new ExceptionAwarePipedInputStream(pos, 65536);

            // Write part to piped output stream using a separate thread
            ThreadPoolService threadPool = ThreadPools.getThreadPool();
            if (null == threadPool) {
                new Thread(new WriteMailPartToPipeOutRunnable(part, pos, pin), "getStreamFromMailPart").start();
            } else {
                WriteMailPartToPipeOutRunnable r = new WriteMailPartToPipeOutRunnable(part, pos, pin);
                try {
                    threadPool.submit(ThreadPools.task(r), AbortBehavior.getInstance());
                } catch (RejectedExecutionException e) {
                    new Thread(r, "getStreamFromMailPart").start();
                }
            }

            // Return piped input stream getting filled with bytes from separate thread
            return pin;
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the multipart content from specified part.
     *
     * @param part The part
     * @return The multipart or <code>null</code>
     * @throws MessagingException If a messaging error occurs
     * @throws IOException If an I/O error occurs
     */
    public static Multipart getMultipartContentFrom(Part part) throws MessagingException, IOException {
        if (null == part) {
            return null;
        }
        final String contentType = getHeader("Content-Type", null, part);
        if (null == contentType || !asciiLowerCase(contentType).startsWith("multipart/")) {
            return null;
        }
        return getMultipartContentFrom(part, contentType);
    }

    /**
     * Gets the multipart content from specified part.
     *
     * @param part The part
     * @param contentType The <code>Content-Type</code> header value
     * @return The multipart or <code>null</code>
     * @throws MessagingException If a messaging error occurs
     * @throws IOException If an I/O error occurs
     */
    public static Multipart getMultipartContentFrom(Part part, String contentType) throws MessagingException, IOException {
        if (null == part) {
            return null;
        }
        if (null == contentType || !asciiLowerCase(contentType).startsWith("multipart/")) {
            return null;
        }
        return multipartFrom(part, contentType);
    }

    /**
     * Gets the multipart content from specified part known to hold a multipart content (w/o checking Content-Type header).
     *
     * @param part The part
     * @return The multipart or <code>null</code>
     * @throws MessagingException If a messaging error occurs
     * @throws IOException If an I/O error occurs
     */
    public static Multipart multipartFrom(Part part) throws MessagingException, IOException {
        return multipartFrom(part, null);
    }

    private static Multipart multipartFrom(Part part, String contentType) throws MessagingException, IOException {
        try {
            final Object content = part.getContent();
            if (content instanceof Multipart) {
                return (Multipart) content;
            }
            if (content instanceof InputStream) {
                return new MimeMultipart(new MessageDataSource((InputStream) content, null == contentType ? getHeader("Content-Type", null, part) : contentType));
            }
            if (content instanceof Message) {
                return getMultipartContentFrom((Message) content);
            }
            if (content instanceof String) {
                String stringContent = (String) content;
                if (Strings.isEmpty(stringContent)) {
                    return new EmptyStringMimeMultipart(new MessageDataSource(new byte[0], null == contentType ? getHeader("Content-Type", null, part) : contentType));
                }
                return new MimeMultipart(new MessageDataSource(Streams.newByteArrayInputStream(stringContent.getBytes(Charsets.ISO_8859_1)), null == contentType ? getHeader("Content-Type", null, part) : contentType));
            }

            LOG.warn("Unable to retrieve multipart content from part with Content-Type={}. Content signals to be {}.", null == contentType ? getHeader("Content-Type", null, part) : contentType, null == content ? "null" : content.getClass().getName());
            return null;
        } catch (MessageRemovedIOException e) {
            String message = e.getMessage();
            throw new MessageRemovedException(null == message ? "Message has been removed in the meantime" : message, e);
        }
    }

    /**
     * Detects the charset of specified part.
     *
     * @param p The part whose charset shall be detected
     * @return The detected part's charset
     * @throws MessagingException If an error occurs in part's getter methods
     */
    public static String detectPartCharset(Part p) throws MessagingException {
        if (null == p) {
            return null;
        }
        try {
            return CharsetDetector.detectCharset(p.getInputStream());
        } catch (IOException e) {
            /*
             * Try to get data from raw input stream
             */
            final InputStream rawIn;
            if (p instanceof MimeBodyPart) {
                rawIn = ((MimeBodyPart) p).getRawInputStream();
            } else if (p instanceof MimeMessage) {
                rawIn = ((MimeMessage) p).getRawInputStream();
            } else {
                /*
                 * Neither a MimeBodyPart nor a MimeMessage
                 */
                LOG.error("", e);
                return CharsetDetector.getFallback();
            }
            return CharsetDetector.detectCharset(rawIn);
        }
    }

    /**
     * Creates a new MIME message from given message
     *
     * @param msg The message to copy from
     * @return The new MIME message
     * @throws MessagingException If a messaging error occurs
     * @throws IOException If an I/O error occurs
     */
    public static MimeMessage mimeMessageFrom(Message msg) throws MessagingException, IOException {
        boolean closeSink = true;
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        try {
            msg.writeTo(sink.asOutputStream());
            File tempFile = sink.getTempFile();
            MimeMessage tmp;
            if (null == tempFile) {
                tmp = new InMemoryMimeMessage(MimeDefaultSession.getDefaultSession(), sink.getStream(), msg.getReceivedDate(), false);
            } else {
                tmp = new FileBackedMimeMessage(MimeDefaultSession.getDefaultSession(), tempFile, msg.getReceivedDate(), false);
            }
            closeSink = false;
            return tmp;
        } catch (OXException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        } catch (com.sun.mail.util.MessageRemovedIOException e) {
            throw new MessageRemovedException(e.getMessage());
        } catch (IOException e) {
            if (e.getCause() instanceof MessageRemovedException) {
                throw (MessageRemovedException) e.getCause();
            }
            throw e;
        } finally {
            if (closeSink) {
                sink.close();
            }
        }
    }

    /**
     * Creates a new MIME message from given mail message
     *
     * @param msg The mail message to copy from
     * @return The new MIME message
     * @throws MessagingException If a messaging error occurs
     * @throws IOException If an I/O error occurs
     */
    public static MimeMessage mimeMessageFrom(MailMessage msg) throws MessagingException, IOException {
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        boolean closeSink = true;
        try {
            msg.writeTo(sink.asOutputStream());
            File tempFile = sink.getTempFile();
            MimeMessage tmp;
            if (null == tempFile) {
                tmp = new InMemoryMimeMessage(MimeDefaultSession.getDefaultSession(), sink.getStream(), msg.getReceivedDate(), false);
            } else {
                tmp = new FileBackedMimeMessage(MimeDefaultSession.getDefaultSession(), tempFile, msg.getReceivedDate(), false);
            }
            closeSink = false;
            return tmp;
        } catch (OXException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        } catch (com.sun.mail.util.MessageRemovedIOException e) {
            throw new MessageRemovedException(e.getMessage());
        } catch (IOException e) {
            if (e.getCause() instanceof MessageRemovedException) {
                throw (MessageRemovedException) e.getCause();
            }
            throw e;
        } finally {
            if (closeSink) {
                sink.close();
            }
        }
    }

    private static final class FileBackedMimeMessage extends com.openexchange.mail.mime.converters.FileBackedMimeMessage {

        private final boolean retainDate;

        FileBackedMimeMessage(javax.mail.Session session, File tempFile, Date receivedDate, boolean retainDate) throws MessagingException, IOException {
            super(session, tempFile, receivedDate);
            this.retainDate = retainDate;
        }

        @Override
        public Date getReceivedDate() throws MessagingException {
            Date optReceivedDate = super.getReceivedDate();
            if (null != optReceivedDate) {
                return optReceivedDate;
            }

            return retainDate ? super.getSentDate() : super.getReceivedDate();
        }
    } // End of class FileBackedMimeMessage

    private static final class InMemoryMimeMessage extends MimeMessage {

        private final Date optReceivedDate;
        private final boolean retainDate;

        InMemoryMimeMessage(javax.mail.Session session, InputStream is, Date optReceivedDate, boolean retainDate) throws MessagingException {
            super(session, is);
            this.optReceivedDate = optReceivedDate;
            this.retainDate = retainDate;
        }

        @Override
        public Date getReceivedDate() throws MessagingException {
            if (null != optReceivedDate) {
                return optReceivedDate;
            }

            return retainDate ? super.getSentDate() : super.getReceivedDate();
        }
    } // End of class InMemoryMimeMessage

    /**
     * The special poison address that denies a message being sent via
     * {@link MailTransport#sendMailMessage(com.openexchange.mail.dataobjects.compose.ComposedMailMessage, com.openexchange.mail.dataobjects.compose.ComposeType, Address[])}
     * .
     */
    public static final InternetAddress POISON_ADDRESS = new InternetAddress() {

        private static final long serialVersionUID = -6860515616722560896L;

        {
            address = "poison@unknown-domain.invalid";
            personal = "Poison";
            encodedPersonal = "Poison";
        }

        @Override
        public String toString() {
            return "poison";
        }

        @Override
        public String getType() {
            return "rfc822";
        }

        @Override
        public boolean equals(Object address) {
            return (this == address);
        }
    };

    /**
     * Performs {@link MimeMessage#saveChanges() saveChanges()} on specified message with sanitizing for a possibly corrupt/wrong Content-Type header.
     * <p>
     * Aligns <i>Message-Id</i> header to given host name.
     *
     * @param mimeMessage The MIME message
     * @param hostName The host name
     * @param keepMessageIdIfPresent Whether to keep a possibly available <i>Message-ID</i> header or to generate a new (unique) one
     * @throws OXException If operation fails
     */
    public static void saveChanges(MimeMessage mimeMessage, String hostName, boolean keepMessageIdIfPresent) throws OXException {
        try {
            String name = "Message-ID";
            String prevMessageId = keepMessageIdIfPresent ? mimeMessage.getHeader(name, null) : null;
            saveChanges(mimeMessage);
            if (null != prevMessageId) {
                mimeMessage.setHeader(name, prevMessageId);
            } else if (null != hostName) {
                // Change Message-Id header appropriately
                String messageId = mimeMessage.getHeader(name, null);
                if (null != messageId) {
                    /*
                     * Somewhat of: <744810669.1.1314981157714.JavaMail.username@host.com>
                     */
                    int pos = messageId.indexOf('@');
                    if (pos > 0) {
                        StringBuilder mid = new StringBuilder(messageId.substring(0, pos + 1)).append(hostName);
                        if (messageId.charAt(0) == '<') {
                            mid.append('>');
                        }
                        mimeMessage.setHeader(name, mid.toString());
                    } else {
                        mimeMessage.setHeader(name, messageId + hostName);
                    }
                }
            }
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Performs {@link MimeMessage#saveChanges() saveChanges()} on specified message with sanitizing for a possibly corrupt/wrong
     * Content-Type header.
     *
     * @param mimeMessage The message
     * @throws OXException If an error occurs
     */
    public static void saveChanges(MimeMessage mimeMessage) throws OXException {
        saveChanges(mimeMessage, true);
    }

    private static void saveChanges(MimeMessage mimeMessage, boolean trySanitizeMultipart) throws OXException {
        if (null == mimeMessage) {
            return;
        }
        try {
            doSaveChanges(mimeMessage, trySanitizeMultipart);
        } catch (MessagingException e) {
            throw MailExceptionCode.MESSAGING_ERROR.create(e, e.getMessage());
        }
    }

    private static void doSaveChanges(MimeMessage mimeMessage, boolean trySanitizeMultipart) throws OXException, MessagingException {
        try {
            mimeMessage.saveChanges();
        } catch (javax.mail.internet.ParseException e) {
            /*-
             * Probably parsing of a Content-Type header failed.
             *
             * Try to sanitize parameter list headers
             */
            sanitizeContentTypeHeaders(mimeMessage, new ContentType());
            /*
             * ... and retry
             */
            mimeMessage.saveChanges();
        } catch (javax.mail.MessagingException e) {
            if (!trySanitizeMultipart) {
                throw MimeMailException.handleMessagingException(e);
            }
            // Check for DCH error
            final String msg = asciiLowerCase(e.getMessage());
            if (null != msg && msg.startsWith("mime part of type \"multipart/")) {
                sanitizeMultipartContent(mimeMessage);
                saveChanges(mimeMessage, false);
            } else {
                throw MimeMailException.handleMessagingException(e);
            }
        }
    }

    private static boolean sanitizeMultipartContent(MimePart part) throws OXException {
        try {
            final String sContentType = asciiLowerCase(part.getHeader("Content-Type", null));
            if (null != sContentType && sContentType.startsWith("multipart/")) {
                final Object o = part.getContent();
                if (o instanceof MimeMultipart) {
                    final MimeMultipart multipart = (MimeMultipart) o;
                    final int count = multipart.getCount();
                    for (int i = 0; i < count; i++) {
                        if (!sanitizeMultipartContent((MimePart) multipart.getBodyPart(i))) {
                            return false;
                        }
                    }
                    return true;
                }
                // Not an instance of MimeMultipart.
                // Try to sanitize
                if (o instanceof InputStream) {
                    final MimeMultipart multipart = new MimeMultipart(new MessageDataSource((InputStream) o, sContentType));
                    part.setContent(multipart);
                    return true;
                }
                return false;
            }
            return true;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    private static void sanitizeContentTypeHeaders(Part part, ContentType sanitizer) throws OXException {
        final DataHandler dh;
        try {
            dh = part.getDataHandler();
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
        if (dh == null) {
            return;
        }
        try {
            final String type = dh.getContentType();
            sanitizer.setContentType(type);
            try {
                /*
                 * Try to parse with JavaMail Content-Type implementation
                 */
                new javax.mail.internet.ContentType(type);
            } catch (javax.mail.internet.ParseException e) {
                /*
                 * Sanitize Content-Type header
                 */
                final String cts = sanitizer.toString(true);
                try {
                    new javax.mail.internet.ContentType(cts);
                } catch (javax.mail.internet.ParseException pe) {
                    /*
                     * Still not parseable
                     */
                    throw MailExceptionCode.INVALID_CONTENT_TYPE.create(e, type);
                }
                part.setDataHandler(new DataHandlerWrapper(dh, cts));
                part.setHeader("Content-Type", cts);
            }
            /*
             * Check for recursive invocation
             */
            if (sanitizer.startsWith("multipart/")) {
                final Object o = dh.getContent();
                if (o instanceof MimeMultipart) {
                    final MimeMultipart mm = (MimeMultipart) o;
                    final int count = mm.getCount();
                    for (int i = 0; i < count; i++) {
                        sanitizeContentTypeHeaders(mm.getBodyPart(i), sanitizer);
                    }
                }
            }
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the first header denoted by specified header name and decodes its value to a unicode string if necessary.
     *
     * <pre>
     * &quot;=?UTF-8?Q?=C3=BCber?=&quot; is decoded to &quot;&amp;uumlber&quot;
     * </pre>
     *
     * @param name The header name
     * @param message The message providing the header
     * @return The decoded header
     * @throws MessagingException If a messaging error occurs
     */
    public static String getSubject(Message message) throws MessagingException {
        final String[] valueArr = message.getHeader(MessageHeaders.HDR_SUBJECT);
        return decodeSubject(valueArr);
    }

    /**
     * Gets the first header denoted by specified header name and decodes its value to a unicode string if necessary.
     *
     * <pre>
     * &quot;=?UTF-8?Q?=C3=BCber?=&quot; is decoded to &quot;&amp;uumlber&quot;
     * </pre>
     *
     * @param name The header name
     * @param message The message providing the header
     * @return The decoded header
     */
    public static String getSubject(MailMessage message) {
        final String[] valueArr = message.getHeader(MessageHeaders.HDR_SUBJECT);
        return decodeSubject(valueArr);
    }

    private static String decodeSubject(String[] valueArr) {
        if (null == valueArr || valueArr.length == 0) {
            return null;
        }
        return decodeEnvelopeSubject(valueArr[0]);
    }

    /**
     * Parses the value of header <code>X-Priority</code>.
     *
     * @param priorityStr The header value
     * @param mailMessage The mail message to fill
     */
    public static void parsePriority(String priorityStr, MailMessage mailMessage) {
        mailMessage.setPriority(parsePriority(priorityStr));
    }

    /**
     * Parses the value of header <code>X-Priority</code>.
     *
     * @param priorityStr The header value
     */
    public static int parsePriority(String priorityStr) {
        int priority = MailMessage.PRIORITY_NORMAL;
        if (null != priorityStr) {
            final String[] tmp = priorityStr.split(" +");
            try {
                priority = Integer.parseInt(tmp[0]);
            } catch (NumberFormatException nfe) {
                LOG.debug("Assuming priority NORMAL due to strange X-Priority header: {}", priorityStr);
                priority = MailMessage.PRIORITY_NORMAL;
            }
        }
        return priority;
    }

    /**
     * Parses the value of header <code>Importance</code>.
     *
     * @param importance The header value
     * @param mailMessage The mail message to fill
     */
    public static void parseImportance(String importance, MailMessage mailMessage) {
        mailMessage.setPriority(parseImportance(importance));
    }

    /**
     * Parses the value of header <code>Importance</code>.
     *
     * @param importance The header value
     */
    public static int parseImportance(String importance) {
        int priority = MailMessage.PRIORITY_NORMAL;
        if (null != importance) {
            final String imp = importance.trim();
            if ("Low".equalsIgnoreCase(imp)) {
                priority = MailMessage.PRIORITY_LOWEST;
            } else if ("Medium".equalsIgnoreCase(imp) || "Normal".equalsIgnoreCase(imp)) {
                priority = MailMessage.PRIORITY_NORMAL;
            } else if ("High".equalsIgnoreCase(imp)) {
                priority = MailMessage.PRIORITY_HIGHEST;
            } else {
                LOG.debug("Assuming priority NORMAL due to strange Importance header: {}", importance);
                priority = MailMessage.PRIORITY_NORMAL;
            }
        }
        return priority;
    }

    /**
     * Parses specified {@link Flags flags} to given {@link MailMessage mail}.
     *
     * @param flags The flags to parse
     * @param examineHasAttachmentUserFlags Whether has-attachment user flags should be considered
     * @param mailMessage The mail to apply the flags to
     */
    public static void parseFlags(Flags flags, boolean examineHasAttachmentUserFlags, MailMessage mailMessage) {
        MimeMessageConverter.parseFlags(flags, examineHasAttachmentUserFlags, mailMessage);
    }

    /**
     * Gets the address headers denoted by specified header name in a safe manner.
     * <p>
     * If strict parsing of address headers yields a {@link AddressException}, then a plain-text version is generated to display broken
     * address header as it is.
     *
     * @param name The address header name
     * @param message The message providing the address header
     * @return The parsed address headers as an array of {@link InternetAddress} instances
     * @throws MessagingException If a messaging error occurs
     */
    public static InternetAddress[] getAddressHeader(String name, Message message) throws MessagingException {
        return getAddresses(message.getHeader(name));
    }

    /**
     * Gets the address headers denoted by specified header name in a safe manner.
     * <p>
     * If strict parsing of address headers yields a {@link AddressException}, then a plain-text version is generated to display broken
     * address header as it is.
     *
     * @param name The address header name
     * @param message The message providing the address header
     * @return The parsed address headers as an array of {@link InternetAddress} instances
     */
    public static InternetAddress[] getAddressHeader(String name, MailMessage message) {
        return getAddresses(message.getHeader(name));
    }

    private static InternetAddress[] getAddresses(String[] addressArray) {
        if (null == addressArray || addressArray.length == 0) {
            return null;
        }
        final String addresses;
        if (addressArray.length > 1) {
            final StringBuilder sb = new StringBuilder(addressArray[0]);
            for (int i = 1; i < addressArray.length; i++) {
                sb.append(',').append(addressArray[i]);
            }
            addresses = sb.toString();
        } else {
            addresses = addressArray[0];
        }
        try {
            return QuotedInternetAddress.parseHeader(addresses, true);
        } catch (AddressException e) {
            return getAddressHeaderNonStrict(addresses, addressArray);
        }
    }

    private static InternetAddress[] getAddressHeaderNonStrict(String addressStrings, String[] addressArray) {
        try {
            final InternetAddress[] addresses = QuotedInternetAddress.parseHeader(addressStrings, false);
            final List<InternetAddress> addressList = new ArrayList<InternetAddress>(addresses.length);
            for (InternetAddress internetAddress : addresses) {
                addAddress(internetAddress, addressList);
            }
            return addressList.toArray(new InternetAddress[addressList.size()]);
        } catch (AddressException e) {
            LOG.debug("Internet addresses could not be properly parsed. Using plain addresses' string representation instead.", e);
            return getAddressesOnParseError(addressArray);
        }
    }

    private static void addAddress(InternetAddress internetAddress, final List<InternetAddress> addressList) {
        try {
            addressList.add(new QuotedInternetAddress(internetAddress.toString()));
        } catch (AddressException e) {
            addressList.add(internetAddress);
        }
    }

    /**
     * Gets the address header from given address header value.
     *
     * @param addresses The address header value
     * @return The parsed addresses
     */
    public static InternetAddress[] getAddressHeader(String addresses) {
        try {
            return QuotedInternetAddress.parseHeader(addresses, true);
        } catch (AddressException e) {
            LOG.debug("Internet addresses could not be properly parsed. Using plain addresses' string representation instead.", e);
            return PlainTextAddress.parseAddresses(addresses);
        }
    }

    private static InternetAddress[] getAddressesOnParseError(String[] addrs) {
        List<InternetAddress> list = new LinkedList<InternetAddress>();
        for (int i = 0; i < addrs.length; i++) {
            String addr = addrs[i];
            if (addr != null) {
                addr = addr.trim();
                if ("<>".equals(addr)) {
                    // Treat as empty address
                    addr = "";
                }
            }
            InternetAddress[] plainAddresses = PlainTextAddress.parseAddresses(addr);
            if (null != plainAddresses && plainAddresses.length > 0) {
                list.addAll(Arrays.asList(plainAddresses));
            }
        }
        return list.toArray(new InternetAddress[list.size()]);
    }

    /**
     * Returns the value of the RFC 822 "Date" field. This is the date on which this message was sent. Returns <code>null</code> if this
     * field is unavailable or its value is absent.
     *
     * @param part The mail part
     * @return The sent Date
     */
    public static Date getSentDate(MailPart part) {
        final String dateString = part.getHeader("Date", null);
        return parseDate(dateString);
    }

    /**
     * Returns the value of the RFC 822 "Date" field. This is the date on which this message was sent. Returns <code>null</code> if this
     * field is unavailable or its value is absent.
     *
     * @param mimeMessage The MIME message
     * @return The sent Date
     * @throws MessagingException If sent date cannot be returned
     */
    public static Date getSentDate(MimeMessage mimeMessage) throws MessagingException {
        String dateString = mimeMessage.getHeader("Date", null);
        return parseDate(dateString);
    }

    private static Date parseDate(String dateString ) {
        Date result = null;
        if (dateString != null) {
            try {
                MailDateFormat mailDateFormat = getDefaultMailDateFormat();
                synchronized (mailDateFormat) {
                    result =  mailDateFormat.parse(dateString);
                }
            } catch (java.text.ParseException pex) {
            }
        }
        return result;
    }

    /**
     * Gets the Content-Type for given part.
     *
     * @param part The part
     * @return The parsed Content-Type
     * @throws OXException If parsing fails
     */
    public static ContentType getContentType(Part part) throws OXException {
        try {
            final String[] tmp = part.getHeader(MessageHeaders.HDR_CONTENT_TYPE);
            return (tmp != null) && (tmp.length > 0) ? new ContentType(tmp[0]) : new ContentType(MimeTypes.MIME_DEFAULT);
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the multipart from passed message.
     *
     * @param message The message
     * @param contentType The message's Content-Type
     * @return The appropriate multipart
     * @throws OXException If content cannot be presented as a multipart
     */
    public static Multipart multipartFor(MimeMessage message, ContentType contentType) throws OXException {
        return multipartFor(message, contentType, true);
    }

    private static Multipart multipartFor(MimeMessage message, ContentType contentType, boolean reparse) throws OXException {
        try {
            return multipartFor(message.getContent(), contentType);
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (javax.mail.internet.ParseException e) {
            if (!reparse) {
                throw MimeMailException.handleMessagingException(e);
            }
            // Sanitize parameterized headers
            try {
                final String sContentType = message.getHeader(MessageHeaders.HDR_CONTENT_TYPE, null);
                message.setHeader(MessageHeaders.HDR_CONTENT_TYPE, new ContentType(sContentType).toString(true));
                saveChanges(message);
            } catch (Exception x) {
                // Content-Type cannot be sanitized
                org.slf4j.LoggerFactory.getLogger(MimeFilter.class).debug("Content-Type cannot be sanitized.", x);
                throw MimeMailException.handleMessagingException(e);
            }
            return multipartFor(message, contentType, false);
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the multipart from passed content object.
     *
     * @param content The content object
     * @param contentType The content type
     * @return The appropriate multipart
     * @throws OXException If content cannot be presented as a multipart
     */
    private static Multipart multipartFor(Object content, ContentType contentType) throws OXException {
        if (null == content) {
            return null;
        }
        if (content instanceof Multipart) {
            return (Multipart) content;
        }
        if (content instanceof InputStream) {
            try {
                return new MimeMultipart(new MessageDataSource((InputStream) content, contentType));
            } catch (MessagingException e) {
                throw MimeMailException.handleMessagingException(e);
            } catch (IOException e) {
                if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                    throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
                }
                throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
            }
        }
        if (content instanceof String) {
            try {
                return new MimeMultipart(new MessageDataSource(Streams.newByteArrayInputStream(((String) content).getBytes(Charsets.ISO_8859_1)), contentType));
            } catch (MessagingException e) {
                throw MimeMailException.handleMessagingException(e);
            } catch (IOException e) {
                if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                    throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
                }
                throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
            }
        }
        throw MailExceptionCode.MESSAGING_ERROR.create("Content is not of type javax.mail.Multipart, but " + content.getClass().getName());
    }

    /**
     * Converts specified flags bit mask to an instance of {@link Flags}.
     *
     * @param flags The flags bit mask
     * @return The corresponding instance of {@link Flags}
     */
    public static Flags convertMailFlags(int flags) {
        final Flags flagsObj = new Flags();
        if ((flags & MailMessage.FLAG_ANSWERED) > 0) {
            flagsObj.add(Flags.Flag.ANSWERED);
        }
        if ((flags & MailMessage.FLAG_DELETED) > 0) {
            flagsObj.add(Flags.Flag.DELETED);
        }
        if ((flags & MailMessage.FLAG_DRAFT) > 0) {
            flagsObj.add(Flags.Flag.DRAFT);
        }
        if ((flags & MailMessage.FLAG_FLAGGED) > 0) {
            flagsObj.add(Flags.Flag.FLAGGED);
        }
        if ((flags & MailMessage.FLAG_RECENT) > 0) {
            flagsObj.add(Flags.Flag.RECENT);
        }
        if ((flags & MailMessage.FLAG_SEEN) > 0) {
            flagsObj.add(Flags.Flag.SEEN);
        }
        if ((flags & MailMessage.FLAG_USER) > 0) {
            flagsObj.add(Flags.Flag.USER);
        }
        if ((flags & MailMessage.FLAG_SPAM) > 0) {
            flagsObj.add(MailMessage.USER_SPAM);
        }
        if ((flags & MailMessage.FLAG_FORWARDED) > 0) {
            flagsObj.add(MailMessage.USER_FORWARDED);
        }
        if ((flags & MailMessage.FLAG_READ_ACK) > 0) {
            flagsObj.add(MailMessage.USER_READ_ACK);
        }
        return flagsObj;
    }

    private static final Pattern PATTERN_PARSE_HEADER = Pattern.compile("(\\S+):\\p{Blank}?(.*)(?:(?:\r?\n)|(?:$))");

    /**
     * Parses given message source's headers into a {@link HeaderCollection collection} until EOF or 2 subsequent CRLFs occur.
     *
     * @param messageSrc The message source
     * @return The parsed headers as a {@link HeaderCollection collection}.
     */
    public static HeaderCollection loadHeaders(String messageSrc) {
        /*
         * Determine position of double line break
         */
        final int len = messageSrc.length();
        int i;
        NextRead: for (i = 0; i < len; ++i) {
            char c = messageSrc.charAt(i);
            final int prevPos = i;
            int count = 0;
            while ((c == '\r') || (c == '\n')) {
                if ((c == '\n') && (++count >= 2)) {
                    i = prevPos;
                    break NextRead;
                }
                if (++i >= len) {
                    i = prevPos;
                    break NextRead;
                }
                c = messageSrc.charAt(i);
            }
        }
        /*
         * Parse single headers
         */
        final Matcher m = PATTERN_PARSE_HEADER.matcher(unfold(messageSrc.substring(0, i)));
        final HeaderCollection headers = new HeaderCollection();
        while (m.find()) {
            final String value = m.group(2);
            if (value == null || isEmpty(value)) {
                headers.addHeader(m.group(1), "");
            } else {
                headers.addHeader(m.group(1), value);
            }
        }
        return headers;
    }

}
