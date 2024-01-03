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

package com.openexchange.rss.util;

import static com.openexchange.rss.httpclient.properties.RssFeedHttpProperties.getHttpClientId;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.java.InetAddresses;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rss.httpclient.properties.RssFeedRedirectStrategy;
import com.openexchange.rss.osgi.Services;
import com.openexchange.tools.stream.CountingInputStream;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.AbstractFeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * {@link TimeoutHttpURLFeedFetcher} - timeout-capable {@link HttpURLFeedFetcher}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since 7.4.1
 */
public class TimeoutHttpURLFeedFetcher extends AbstractFeedFetcher implements Reloadable {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TimeoutHttpURLFeedFetcher.class);

    private static final String MAX_FEED_SIZE_PROPERTY_NAME = "com.openexchange.messaging.rss.feed.size";

    /** The timeout value, in milliseconds, to be used when opening a communications link to the resource */
    protected final int connectTimout;

    /** The read timeout to a specified timeout, in milliseconds */
    protected final int readTimout;

    /**
     * Defines the maximum feed size for an RSS feed in bytes
     */
    private long maximumAllowedSize;

    private FeedFetcherCache feedInfoCache;

    /**
     * Initializes a new {@link TimeoutHttpURLFeedFetcher}.
     *
     * @param connectTimout The timeout value, in milliseconds, to be used when opening a communications link to the resource
     * @param readTimout The read timeout to a specified timeout, in milliseconds
     */
    public TimeoutHttpURLFeedFetcher(int connectTimout, int readTimout) {
        super();
        this.connectTimout = connectTimout;
        this.readTimout = readTimout;
        reloadConfiguration(Services.getService(ConfigurationService.class));
    }

    /**
     * Initializes a new {@link TimeoutHttpURLFeedFetcher}.
     *
     * @param connectTimout The timeout value, in milliseconds, to be used when opening a communications link to the resource
     * @param readTimout The read timeout to a specified timeout, in milliseconds
     * @param feedInfoCache The feed cache
     */
    public TimeoutHttpURLFeedFetcher(int connectTimout, int readTimout, FeedFetcherCache feedInfoCache) {
        this.connectTimout = connectTimout;
        this.readTimout = readTimout;
        setFeedInfoCache(feedInfoCache);
        reloadConfiguration(Services.getService(ConfigurationService.class));
    }

    private HttpClient getHttpClient() throws FetcherException {
        HttpClientService httpClientService = Services.optService(HttpClientService.class);
        if (httpClientService == null) {
            throw new FetcherException("Absent service: " + HttpClientService.class.getName());
        }
        return httpClientService.getHttpClient(getHttpClientId());
    }

    /**
     * Retrieve a feed over HTTP
     *
     * @param feedUrl A non-null URL of a RSS/Atom feed to retrieve
     * @return A {@link com.sun.syndication.feed.synd.SyndFeed} object
     * @throws IllegalArgumentException if the URL is null;
     * @throws IOException if a TCP error occurs
     * @throws FeedException if the feed is not valid
     * @throws FetcherException if a HTTP error occurred
     */
    @Override
    public SyndFeed retrieveFeed(URL feedUrlToRetrieve) throws IllegalArgumentException, IOException, FeedException, FetcherException {
        if (feedUrlToRetrieve == null) {
            throw new IllegalArgumentException("null is not a valid URL");
        }

        boolean originalAddressIsRemote;
        try {
            InetAddress inetAddress = InetAddress.getByName(feedUrlToRetrieve.getHost());
            originalAddressIsRemote = false == InetAddresses.isInternalAddress(inetAddress);
        } catch (UnknownHostException e) {
            // IP address of that host could not be determined
            LOG.warn("Unknown host: {}. Skipping retrieving feed from URL {}", feedUrlToRetrieve.getHost(), feedUrlToRetrieve, e);
            throw new IllegalArgumentException(feedUrlToRetrieve.toExternalForm() + " contains an unknown host", e);
        }

        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(RssFeedRedirectStrategy.ATTRIBUTE_ORIGINAL_ADDR_IS_REMOTE, Boolean.valueOf(originalAddressIsRemote));

        FeedFetcherCache cache = getFeedInfoCache();
        SyndFeedInfo syndFeedInfo = cache == null ? null : cache.getFeedInfo(feedUrlToRetrieve);

        HttpGet request = null;
        HttpResponse httpResponse = null;
        try {
            // Execute HTTP request
            request = new HttpGet(feedUrlToRetrieve.toString());
            setRequestHeaders(request, syndFeedInfo);
            httpResponse = getHttpClient().execute(request, httpContext);
            fireEvent(FetcherEvent.EVENT_TYPE_FEED_POLLED, feedUrlToRetrieve.toExternalForm());

            // Check status code
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            switch (statusCode) {
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new FetcherException("RSS feed \"" + feedUrlToRetrieve + "\" requires authentication");
                case HttpStatus.SC_NOT_FOUND:
                    throw new FetcherException("RSS feed \"" + feedUrlToRetrieve + "\" not found");
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw new FetcherException("RSS feed \"" + feedUrlToRetrieve + "\" has been removed");
            }
            if (statusCode >= 400 && statusCode <= 499) {
                String reasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
                throw new FetcherException("RSS feed \"" + feedUrlToRetrieve + "\" responded with unexpected error" + (Strings.isEmpty(reasonPhrase) ? "" : ": " + reasonPhrase));
            }
            if (statusCode >= 500 && statusCode <= 599) {
                String reasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
                throw new FetcherException("RSS feed \"" + feedUrlToRetrieve + "\" responded with remote server error" + (Strings.isEmpty(reasonPhrase) ? "" : ": " + reasonPhrase));
            }
            handleErrorCodes(statusCode);

            if (syndFeedInfo != null && HttpStatus.SC_NOT_MODIFIED == statusCode) {
                // the feed does not need retrieving
                fireEvent(FetcherEvent.EVENT_TYPE_FEED_UNCHANGED, feedUrlToRetrieve.toExternalForm());
                return syndFeedInfo.getSyndFeed();
            }

            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                throw new FetcherException("RSS feed \"" + feedUrlToRetrieve + "\" provides no content");
            }

            if (maximumAllowedSize > 0) {
                long contentLength = entity.getContentLength();
                Header contentLengthHeader = httpResponse.getFirstHeader("Content-Length");
                String contentLength2 = contentLengthHeader == null ? null : contentLengthHeader.getValue();

                long allowedFeedSize = maximumAllowedSize;
                if (contentLength > allowedFeedSize || (Strings.isNotEmpty(contentLength2) && Long.parseLong(contentLength2) > allowedFeedSize)) {
                    throw new FetcherException("RSS feed \"" + feedUrlToRetrieve + "\" is too big");
                }
            }

            if (cache == null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    return getSyndFeedFromStream(inputStream, entity, feedUrlToRetrieve);
                } finally {
                    Streams.close(inputStream);
                }
            }

            if (syndFeedInfo == null) {
                // this is a feed that hasn't been retrieved
                syndFeedInfo = new SyndFeedInfo();
            }
            retrieveAndCacheFeed(feedUrlToRetrieve, syndFeedInfo, entity, httpResponse);
            return syndFeedInfo.getSyndFeed();
        } finally {
            HttpClients.close(request, httpResponse);
        }
    }

    /**
     * Retrieves and caches a {@link SyndFeed}
     *
     * @param feedUrl The feed {@link URL}
     * @param syndFeedInfo The {@link SyndFeedInfo} to retrieve and cache
     * @param entity The HTTP entity
     * @param httpResponse The HTTP response
     * @throws IllegalArgumentException
     * @throws FeedException
     * @throws IOException if an I/O error occurs
     */
    protected void retrieveAndCacheFeed(URL feedUrl, SyndFeedInfo syndFeedInfo, HttpEntity entity, HttpResponse httpResponse) throws IllegalArgumentException, FeedException, IOException {
        resetFeedInfo(feedUrl, syndFeedInfo, entity, httpResponse);
        FeedFetcherCache cache = getFeedInfoCache();
        // resetting feed info in the cache
        // could be needed for some implementations
        // of FeedFetcherCache (eg, distributed HashTables)
        if (cache != null) {
            cache.setFeedInfo(feedUrl, syndFeedInfo);
        }
    }

    /**
     * Resets the specified {@link SyndFeedInfo}
     *
     * @param orignalUrl The original URL
     * @param syndFeedInfo The {@link SyndFeedInfo} to reset
     * @param entity The HTTP entity
     * @param httpResponse The HTTP response
     * @throws IllegalArgumentException
     * @throws IOException if an I/O error occurs
     * @throws FeedException
     */
    protected void resetFeedInfo(URL orignalUrl, SyndFeedInfo syndFeedInfo, HttpEntity entity, HttpResponse httpResponse) throws IllegalArgumentException, IOException, FeedException {
        // need to always set the URL because this may have changed due to 3xx redirects
        syndFeedInfo.setUrl(orignalUrl);

        // the ID is a persistant value that should stay the same even if the URL for the
        // feed changes (eg, by 3xx redirects)
        syndFeedInfo.setId(orignalUrl.toString());

        // This will be 0 if the server doesn't support or isn't setting the last modified header
        Header lastModifiedHeader = httpResponse.getFirstHeader(HttpHeaders.LAST_MODIFIED);
        syndFeedInfo.setLastModified(lastModifiedHeader == null ? Long.valueOf(0) : Long.valueOf(DateUtils.parseDate(lastModifiedHeader.getValue()).getTime()));

        // This will be null if the server doesn't support or isn't setting the ETag header
        Header eTagHeader = httpResponse.getFirstHeader(HttpHeaders.ETAG);
        syndFeedInfo.setETag(eTagHeader == null ? null : eTagHeader.getValue());

        // get the contents
        InputStream inputStream = null;
        try {
            inputStream = entity.getContent();
            SyndFeed syndFeed = getSyndFeedFromStream(inputStream, entity, orignalUrl);

            Header imHeader = httpResponse.getFirstHeader("IM");
            if (isUsingDeltaEncoding() && (imHeader != null && imHeader.getValue().indexOf("feed") >= 0)) {
                FeedFetcherCache cache = getFeedInfoCache();
                if (cache != null && httpResponse.getStatusLine().getStatusCode() == 226) {
                    // client is setup to use http delta encoding and the server supports it and has returned a delta encoded response
                    // This response only includes new items
                    SyndFeedInfo cachedInfo = cache.getFeedInfo(orignalUrl);
                    if (cachedInfo != null) {
                        SyndFeed cachedFeed = cachedInfo.getSyndFeed();

                        // set the new feed to be the orginal feed plus the new items
                        syndFeed = combineFeeds(cachedFeed, syndFeed);
                    }
                }
            }

            syndFeedInfo.setSyndFeed(syndFeed);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * <p>Set appropriate HTTP headers, including conditional get and gzip encoding headers</p>
     *
     * @param request The HTTP request
     * @param syndFeedInfo The SyndFeedInfo for the feed to be retrieved. May be null
     */
    protected void setRequestHeaders(HttpGet request, SyndFeedInfo syndFeedInfo) {
        if (syndFeedInfo != null) {
            // set the headers to get feed only if modified
            // we support the use of both last modified and eTag headers
            if (syndFeedInfo.getLastModified() != null) {
                Object lastModified = syndFeedInfo.getLastModified();
                if (lastModified instanceof Long) {
                    String ifModifiedSince = DateUtils.formatDate(new Date(((Long) syndFeedInfo.getLastModified()).longValue()));
                    if (Strings.isNotEmpty(ifModifiedSince)) {
                        request.setHeader(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
                    }
                }
            }
            if (syndFeedInfo.getETag() != null) {
                request.addHeader(HttpHeaders.IF_NONE_MATCH, syndFeedInfo.getETag());
            }

        }
        // header to retrieve feed gzipped
        request.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");

        // set the user agent
        request.addHeader("User-Agent", getUserAgent());

        if (isUsingDeltaEncoding()) {
            request.addHeader("A-IM", "feed");
        }
    }

    /**
     * Reads the SyndFeed from the specified input stream and fires an event {@link FetcherEvent#EVENT_TYPE_FEED_RETRIEVED}
     *
     * @param inputStream The {@link InputStream}
     * @param entity The HTTP entity
     * @param feedUrl The URL of the RSS feed
     * @return The {@link SyndFeed}
     * @throws IOException If an I/O error occurs
     * @throws IllegalArgumentException
     * @throws FeedException
     */
    private SyndFeed getSyndFeedFromStream(InputStream inputStream, HttpEntity entity, URL feedUrl) throws IOException, IllegalArgumentException, FeedException {
        SyndFeed feed = readSyndFeedFromStream(inputStream, entity);
        fireEvent(FetcherEvent.EVENT_TYPE_FEED_RETRIEVED, feedUrl.toExternalForm(), feed);
        return feed;
    }

    /**
     * Reads the SyndFeed from the specified input stream
     *
     * @param inputStream The {@link InputStream}
     * @param entity The HTTP entity
     * @return The {@link SyndFeed}
     * @throws IOException If an I/O error occurs
     * @throws IllegalArgumentException
     * @throws FeedException
     */
    private SyndFeed readSyndFeedFromStream(InputStream inputStream, HttpEntity entity) throws IOException, IllegalArgumentException, FeedException {
        XmlReader reader;
        {
            CountingInputStream cis = inputStream instanceof CountingInputStream ? (CountingInputStream) inputStream : new CountingInputStream(inputStream, maximumAllowedSize);
            if (entity.getContentType() != null) {
                reader = new XmlReader(new BufferedInputStream(cis, 65536), entity.getContentType().getValue(), true);
            } else {
                reader = new XmlReader(new BufferedInputStream(cis, 65536), true);
            }
        }

        SyndFeedInput syndFeedInput = new SyndFeedInput();
        syndFeedInput.setPreserveWireFeed(isPreserveWireFeed());

        return syndFeedInput.build(reader);
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        maximumAllowedSize = configService.getIntProperty(MAX_FEED_SIZE_PROPERTY_NAME, 4194304);
    }

    @Override
    public Interests getInterests() {
        return Reloadables.interestsForProperties(MAX_FEED_SIZE_PROPERTY_NAME);
    }

    /**
     * @return The FeedFetcherCache used by this fetcher (Could be null)
     */
    public synchronized FeedFetcherCache getFeedInfoCache() {
        return feedInfoCache;
    }

    /**
     * @param cache The cache to be used by this fetcher (pass null to stop using a cache)
     */
    public synchronized void setFeedInfoCache(FeedFetcherCache cache) {
        feedInfoCache = cache;
    }

}
