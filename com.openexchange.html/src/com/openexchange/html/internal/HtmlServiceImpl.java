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

package com.openexchange.html.internal;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Strings.isEmpty;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.Serializer;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;
import org.owasp.esapi.codecs.HTMLEntityCodec;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.html.HtmlExceptionCodes;
import com.openexchange.html.HtmlSanitizeOptions;
import com.openexchange.html.HtmlSanitizeResult;
import com.openexchange.html.HtmlService;
import com.openexchange.html.HtmlServices;
import com.openexchange.html.internal.emoji.EmojiRegistry;
import com.openexchange.html.internal.filtering.FilterMaps;
import com.openexchange.html.internal.html2text.control.Html2TextControl;
import com.openexchange.html.internal.html2text.control.Html2TextControlTask;
import com.openexchange.html.internal.html2text.control.Html2TextTask;
import com.openexchange.html.internal.image.DroppingImageHandler;
import com.openexchange.html.internal.image.ImageProcessor;
import com.openexchange.html.internal.image.ProxyRegistryImageHandler;
import com.openexchange.html.internal.jericho.JerichoParser;
import com.openexchange.html.internal.jericho.handler.FilterJerichoHandler;
import com.openexchange.html.internal.jericho.handler.UrlReplacerJerichoHandler;
import com.openexchange.html.internal.jsoup.HtmlToPlainText;
import com.openexchange.html.internal.jsoup.InterruptedConversionException;
import com.openexchange.html.internal.jsoup.InterruptedParsingException;
import com.openexchange.html.internal.jsoup.JsoupParser;
import com.openexchange.html.internal.jsoup.handler.CleaningJsoupHandler;
import com.openexchange.html.internal.jsoup.handler.CssOnlyCleaningJsoupHandler;
import com.openexchange.html.internal.jsoup.handler.UrlReplacerJsoupHandler;
import com.openexchange.html.internal.parser.HtmlParser;
import com.openexchange.html.internal.parser.handler.HTMLFilterHandler;
import com.openexchange.html.internal.parser.handler.HTMLImageFilterHandler;
import com.openexchange.html.services.ServiceRegistry;
import com.openexchange.html.whitelist.Whitelist;
import com.openexchange.java.AllocatingStringWriter;
import com.openexchange.java.Charsets;
import com.openexchange.java.InterruptibleCharSequence;
import com.openexchange.java.InterruptibleCharSequence.InterruptedRuntimeException;
import com.openexchange.java.Streams;
import com.openexchange.java.StringBuilderStringer;
import com.openexchange.java.Stringer;
import com.openexchange.java.Strings;
import com.openexchange.proxy.ProxyRegistry;
import com.openexchange.tika.util.TikaUtils;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import gnu.inet.encoding.IDNAException;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.JerichoRenderer;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

/**
 * {@link HtmlServiceImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HtmlServiceImpl implements HtmlService {

    /** The logger */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HtmlServiceImpl.class);

    private static final String CHARSET_UTF_8 = "UTF-8";

    private static final String TAG_E_HEAD = "</head>";

    private static final String TAG_S_HEAD = "<head>";

    private final static char[] IMMUNE_EMPTY = new char[0];
    private final static char[] IMMUNE_HTML = { ',', '.', '-', '_', ' ' };
    private final static char[] IMMUNE_HTMLATTR = { ',', '.', '-', '_' };

    /*-
     * Member stuff
     */

    private final TIntObjectMap<String> htmlCharMap;
    private final Map<String, Character> htmlEntityMap;
    private final String lineSeparator;
    private final HTMLEntityCodec htmlCodec;
    private final DefaultWhitelist fullWhitelist;
    private final DefaultWhitelist htmlOnlyWhitelist;
    private final int htmlParseTimeoutSec;
    private volatile Thread html2TextControlRunner;

    /**
     * Initializes a new {@link HtmlServiceImpl}.
     *
     * @param htmlCharMap The HTML entity to string map
     * @param htmlEntityMap The string to HTML entity map
     */
    public HtmlServiceImpl(final Map<Character, String> htmlCharMap, final Map<String, Character> htmlEntityMap) {
        super();
        lineSeparator = Strings.getLineSeparator();
        {
            TIntObjectMap<String> tmp = new TIntObjectHashMap<String>(htmlCharMap.size());
            for (Map.Entry<Character, String> entry : htmlCharMap.entrySet()) {
                tmp.put(entry.getKey().charValue(), entry.getValue());
            }
            this.htmlCharMap = tmp;
        }
        this.htmlEntityMap = htmlEntityMap;
        htmlCodec = new HTMLEntityCodec();

        DefaultWhitelist.Builder builder = DefaultWhitelist.builder();
        builder.setHtmlWhitelistMap(FilterMaps.getStaticHTMLMap());
        builder.setStyleWhitelistMap(FilterMaps.getStaticStyleMap());
        fullWhitelist = builder.build();

        builder = DefaultWhitelist.builder();
        builder.setHtmlWhitelistMap(FilterMaps.getStaticHTMLMap());
        htmlOnlyWhitelist = builder.build();

        ConfigurationService service = ServiceRegistry.getInstance().getService(ConfigurationService.class);
        int defaultValue = 10;
        htmlParseTimeoutSec = null == service ? defaultValue : service.getIntProperty("com.openexchange.html.parse.timeout", defaultValue);
    }

    /**
     * Stops this HTML service instance.
     */
    public void stop() {
        if (htmlParseTimeoutSec > 0) {
            Html2TextControl.getInstance().add(Html2TextTask.POISON);
        }
        Thread html2TextControlRunner = this.html2TextControlRunner;
        if (null != html2TextControlRunner) {
            this.html2TextControlRunner = null;
            html2TextControlRunner.interrupt();
        }
    }

    @Override
    public String replaceImages(final String content, final String sessionId) {
        if (null == content) {
            return null;
        }
        ProxyRegistry proxyRegistry = ProxyRegistryProvider.getInstance().getProxyRegistry();
        if (null == proxyRegistry) {
            LOG.warn("Missing ProxyRegistry service. Replacing image URL skipped.");
            return content;
        }
        ProxyRegistryImageHandler handler = new ProxyRegistryImageHandler(sessionId, proxyRegistry);
        return ImageProcessor.getInstance().replaceImages(content, handler);
    }

    @Override
    public String formatHrefLinks(final String content) {
        try {
            final Matcher m = PATTERN_LINK_WITH_GROUP.matcher(content);
            final StringBuilder targetBuilder = new StringBuilder(content.length());
            final StringBuilder sb = new StringBuilder(256);
            int lastMatch = 0;
            while (m.find()) {
                targetBuilder.append(content.substring(lastMatch, m.start()));
                final String url = m.group(1);
                sb.setLength(0);
                if ((url == null) || (isSrcAttr(content, m.start(1)))) {
                    targetBuilder.append(checkTarget(m.group(), sb));
                } else {
                    appendLink(url, sb);
                    targetBuilder.append(sb.toString());
                }
                lastMatch = m.end();
            }
            targetBuilder.append(content.substring(lastMatch));
            return targetBuilder.toString();
        } catch (Exception e) {
            LOG.error("", e);
        } catch (StackOverflowError error) {
            LOG.error(StackOverflowError.class.getName(), error);
        }
        return content;
    }

    private static final String STR_IMG_SRC = "src=";

    private static boolean isSrcAttr(final String line, final int urlStart) {
        return (urlStart >= 5) && ((STR_IMG_SRC.equalsIgnoreCase(line.substring(urlStart - 5, urlStart - 1))) || (STR_IMG_SRC.equalsIgnoreCase(line.substring(urlStart - 4, urlStart))));
    }

    private static final Pattern PATTERN_TARGET = Pattern.compile("(<a[^>]*?target=\"?)([^\\s\">]+)(\"?.*</a>)", Pattern.CASE_INSENSITIVE);

    private static final String STR_BLANK = "_blank";

    private static String checkTarget(final String anchorTag, final StringBuilder sb) {
        final Matcher m = PATTERN_TARGET.matcher(anchorTag);
        if (m.matches()) {
            if (!STR_BLANK.equalsIgnoreCase(m.group(2))) {
                return sb.append(m.group(1)).append(STR_BLANK).append(m.group(3)).toString();
            }
            return anchorTag;
        }
        /*
         * No target specified
         */
        final int pos = anchorTag.indexOf('>');
        if (pos == -1) {
            return anchorTag;
        }
        return sb.append(anchorTag.substring(0, pos)).append(" target=\"").append(STR_BLANK).append('"').append(anchorTag.substring(pos)).toString();
    }

    @Override
    public String formatURLs(final String content, final String comment) {
        try {
            Matcher m = PATTERN_URL.matcher(content);
            if (m.find() == false) {
                return content;
            }

            StringBuilder targetBuilder = new StringBuilder(content.length());
            StringBuilder sb = new StringBuilder(256);
            int lastMatch = 0;
            do {
                String url = m.group();
                if (HtmlServices.isSafe(url, null)) {
                    final int startOpeningPos = m.start();
                    targetBuilder.append(content.substring(lastMatch, startOpeningPos));
                    sb.setLength(0);
                    appendLink(url, sb);
                    targetBuilder.append("<!--").append(comment).append(' ').append(sb.toString()).append("-->");
                    lastMatch = m.end();
                }
            } while (m.find());
            targetBuilder.append(content.substring(lastMatch));
            return targetBuilder.toString();
        } catch (Exception e) {
            LOG.error("", e);
        } catch (StackOverflowError error) {
            LOG.error(StackOverflowError.class.getName(), error);
        }
        return content;
    }

    private static void appendLink(final String url, final StringBuilder builder) {
        try {
            final int mlen = url.length() - 1;
            if ((mlen > 0) && (')' == url.charAt(mlen))) { // Ends with a parenthesis
                /*
                 * Keep starting parenthesis if present
                 */
                if ('(' == url.charAt(0)) { // Starts with a parenthesis
                    builder.append('(');
                    appendAnchor(url.substring(1, mlen), builder);
                } else {
                    appendAnchor(url.substring(0, mlen), builder);
                }
                /*
                 * Append closing parenthesis
                 */
                builder.append(')');
            } else if ((mlen >= 0) && ('(' == url.charAt(0))) { // Starts with a parenthesis, but does not end with a parenthesis
                /*
                 * Append opening parenthesis
                 */
                builder.append('(');
                appendAnchor(url.substring(1), builder);
            } else {
                appendAnchor(url, builder);
            }
        } catch (Exception e) {
            /*
             * Append as-is
             */
            LOG.warn("", e);
            builder.append(url);
        }
    }

    private static void appendAnchor(final String url, final StringBuilder builder) throws IDNAException {
        try {
            final String checkedUrl = checkURL(url);
            builder.append("<a href=\"");
            if (url.startsWith("www") || url.startsWith("news")) {
                builder.append("http://");
            }
            builder.append(checkedUrl).append("\" target=\"_blank\">").append(url).append("</a>");
        } catch (MalformedURLException e) {
            LOG.debug("Malformed URL", e);
            // Append as-is
            builder.append(url);
        }
    }

    /**
     * Checks if specified URL needs to be converted to its ASCII form.
     *
     * @param url The URL to check
     * @return The checked URL
     * @throws MalformedURLException If URL is malformed
     * @throws IDNAException If conversion fails
     */
    public static String checkURL(final String url) throws MalformedURLException, IDNAException {
        String urlStr = url;
        /*
         * Get the host part of URL. Ensure scheme is present before creating a java.net.URL instance
         */
        final String host = new URL(urlStr.startsWith("www.") || urlStr.startsWith("news.") ? new StringBuilder("http://").append(urlStr).toString() : urlStr).getHost();
        if (null != host && !isAscii(host)) {
            final String encodedHost = gnu.inet.encoding.IDNA.toASCII(host);
            urlStr = Pattern.compile(Pattern.quote(host)).matcher(urlStr).replaceFirst(com.openexchange.java.Strings.quoteReplacement(encodedHost));
        }
        /*
         * Still contains any non-ascii character?
         */
        final int len = urlStr.length();
        StringBuilder tmp = null;
        int lastpos = 0;
        int i;
        for (i = 0; i < len; i++) {
            final char c = urlStr.charAt(i);
            if (c >= 128) {
                if (null == tmp) {
                    tmp = new StringBuilder(len + 16);
                }
                tmp.append(urlStr.substring(lastpos, i)).append('%').append(Integer.toHexString(c).toUpperCase(Locale.ENGLISH));
                lastpos = i + 1;
            }
        }
        /*
         * Return
         */
        if (null == tmp) {
            return urlStr;
        }
        return (lastpos < len) ? tmp.append(urlStr.substring(lastpos)).toString() : tmp.toString();
    }

    /**
     * Checks whether the specified string's characters are ASCII 7 bit
     *
     * @param s The string to check
     * @return <code>true</code> if string's characters are ASCII 7 bit; otherwise <code>false</code>
     */
    private static boolean isAscii(final String s) {
        final int length = s.length();
        boolean isAscci = true;
        for (int i = 0; isAscci && (i < length); i++) {
            isAscci = (s.charAt(i) < 128);
        }
        return isAscci;
    }

    @Override
    public String filterWhitelist(final String htmlContent) {
        final HTMLFilterHandler handler = new HTMLFilterHandler(this, htmlContent.length());
        HtmlParser.parse(htmlContent, handler);
        return handler.getHTML();
    }

    @Override
    public String filterWhitelist(final String htmlContent, final String configName) {
        String confName = configName;
        if (!confName.endsWith(".properties")) {
            confName += ".properties";
        }
        final String definition = getConfiguration().getText(confName);
        if (definition == null) {
            // Apparently, the file was not found, so we'll just fall back to the default whitelist
            return filterWhitelist(htmlContent);
        }
        final HTMLFilterHandler handler = new HTMLFilterHandler(this, htmlContent.length(), definition);
        HtmlParser.parse(htmlContent, handler);
        return handler.getHTML();
    }

    protected ConfigurationService getConfiguration() {
        return ServiceRegistry.getInstance().getService(ConfigurationService.class);
    }

    @Override
    public String filterExternalImages(final String htmlContent, final boolean[] modified) {
        final HTMLImageFilterHandler handler = new HTMLImageFilterHandler(this, htmlContent.length());
        HtmlParser.parse(htmlContent, handler);
        modified[0] |= handler.isImageURLFound();
        return handler.getHTML();
    }

    @Override
    public String encodeForHTML(final char[] candidates, final String input) {
        if (input == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(input.length() << 1);
        Arrays.sort(candidates);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (Arrays.binarySearch(candidates, c) >= 0) {
                sb.append(htmlCodec.encodeCharacter(IMMUNE_EMPTY, Character.valueOf(c)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public String encodeForHTML(final String input) {
        if (input == null) {
            return null;
        }
        return htmlCodec.encode(IMMUNE_HTML, input);
    }

    @Override
    public String encodeForHTMLAttribute(final String input) {
        if (input == null) {
            return null;
        }
        return htmlCodec.encode(IMMUNE_HTMLATTR, input);
    }

    /**
     * Encode a String so that it can be safely used in a specific context.
     *
     * @param immune
     * @param input the String to encode
     * @return the encoded String
     */
    public String encodeForHTMLAttribute(final char[] immune, final String input) {
        if (input == null) {
            return null;
        }
        return htmlCodec.encode(immune, input);
    }

    @Override
    public Whitelist getWhitelist(boolean withCss) {
        return withCss ? fullWhitelist : htmlOnlyWhitelist;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String sanitize(final String htmlContent, final String optConfigName, final boolean dropExternalImages, final boolean[] modified, final String cssPrefix) throws OXException {
        return sanitize(htmlContent, optConfigName, dropExternalImages, modified, cssPrefix, -1).getContent();
    }

    private String normalize(String str) {
        boolean needsCheck = false;
        int length = str.length();
        for (int i = 0; !needsCheck && i < length; i++) {
            needsCheck = str.charAt(i) >= 128;
        }

        if (!needsCheck) {
            return str;
        }

        StringBuilder tmp = new StringBuilder(length);
        OneCharSequence helper = null;
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (c < 128) {
                tmp.append(c);
            } else {
                if (null == helper) {
                    helper = new OneCharSequence(c);
                } else {
                    helper.setCharacter(c);
                }
                tmp.append(Normalizer.normalize(helper, Form.NFKC));
            }
        }
        return tmp.toString();
    }

    @Override
    public HtmlSanitizeResult sanitize(String htmlContent, String optConfigName, boolean dropExternalImages, boolean[] modified, String cssPrefix, int maxContentSize) throws OXException {
        return sanitize(htmlContent, HtmlSanitizeOptions.builder().setCssPrefix(cssPrefix).setDropExternalImages(dropExternalImages).setMaxContentSize(maxContentSize).setModified(modified).setOptConfigName(optConfigName).build());
    }

    @Override
    public HtmlSanitizeResult sanitize(String htmlContent, HtmlSanitizeOptions options) throws OXException {
        HtmlSanitizeResult htmlSanitizeResult = new HtmlSanitizeResult(htmlContent);
        if (isEmpty(htmlContent)) {
            return htmlSanitizeResult;
        }

        // First, check size
        checkSize(htmlContent);

        try {
            String html = htmlContent;

            // Check if input is a full HTML document or a fragment of HTML to parse
            boolean hasBody = html.indexOf("<body") >= 0 || html.indexOf("<BODY") >= 0;

            boolean useJericho = options.isSanitize() && (HtmlSanitizeOptions.ParserPreference.JERICHO == options.getParserPreference() || HtmlServices.useJericho());
            if (useJericho) {
                // Normalize the string
                {
                    Matcher matcher = PATTERN_URL.matcher(html);
                    if (matcher.find()) {
                        StringBuffer sb = new StringBuffer(html.length());
                        do {
                            matcher.appendReplacement(sb, Matcher.quoteReplacement(normalize(matcher.group())));
                        } while (matcher.find());
                        matcher.appendTail(sb);
                        html = sb.toString();
                    }
                }

                html = removeComments(html, hasBody, options);

                // Perform one-shot sanitizing
                html = replacePercentTags(html);
                html = replaceHexEntities(html);
                html = processDownlevelRevealedConditionalComments(html);
                html = dropWeirdXmlNamespaceDeclarations(html);
                html = dropDoubleAccents(html);
                html = dropSlashedTags(html);
                html = dropExtraChar(html);

                // Repetitive sanitizing until no further replacement/changes performed
                final boolean[] sanitized = new boolean[] { true };
                while (sanitized[0]) {
                    sanitized[0] = false;
                    // Start sanitizing round
                    html = SaneScriptTags.saneScriptTags(html, sanitized);
                }

                // // CSS- and tag-wise sanitizing -- Initialize the handler
                FilterJerichoHandler handler = getHandlerFor(html.length(), options.getOptConfigName());
                handler.setDropExternalImages(options.isDropExternalImages()).setCssPrefix(options.getCssPrefix()).setMaxContentSize(options.getMaxContentSize()).setSuppressLinks(options.isSuppressLinks());

                // Drop external images using regular expression
                boolean[] modified = options.getModified();
                if (options.isDropExternalImages()) {
                    DroppingImageHandler imageHandler = new DroppingImageHandler();
                    html = ImageProcessor.getInstance().replaceImages(html, imageHandler);
                    if (null != modified) {
                        modified[0] |= imageHandler.isModified();
                    }
                }

                // Parse the HTML content
                JerichoParser.getInstance().parse(html, handler, true);

                // Check if modified by handler
                if (options.isDropExternalImages() && null != modified) {
                    modified[0] |= handler.isImageURLFound();
                }

                // Get HTML content
                html = handler.getHTML();
                htmlSanitizeResult.setTruncated(handler.isMaxContentSizeExceeded());
            } else {
                if (options.isSanitize()) {
                    boolean[] sanitized = new boolean[] { true };
                    while (sanitized[0]) {
                        sanitized[0] = false;
                        // Start sanitizing round
                        html = SaneScriptTags.saneScriptTags(html, sanitized);
                    }

                    CleaningJsoupHandler handler = getJsoupHandlerFor(options.getOptConfigName());
                    handler.setDropExternalImages(options.isDropExternalImages()).setCssPrefix(options.getCssPrefix()).setMaxContentSize(options.getMaxContentSize());
                    handler.setSuppressLinks(options.isSuppressLinks()).setReplaceBodyWithDiv(options.isReplaceBodyWithDiv());

                    boolean[] modified = options.getModified();

                    // Parse the HTML content
                    JsoupParser.getInstance().parse(html, handler, false, false);

                    // Check if modified by handler
                    if (options.isDropExternalImages() && null != modified) {
                        modified[0] |= handler.isImageURLFound();
                    }

                    // Get HTML content
                    if (options.isReplaceBodyWithDiv()) {
                        html = handler.getHtml();
                        if (false == startsWith("<!doctype html>", html, true)) {
                            html = "<!doctype html>\n" + html;
                        }
                        htmlSanitizeResult.setTruncated(handler.isMaxContentSizeExceeded());
                        htmlSanitizeResult.setBodyReplacedWithDiv(true);
                    } else {
                        Document document = handler.getDocument();
                        handlePrettyPrint(options, document);
                        html = hasBody || document.body() == null ? document.outerHtml() : document.body().html();
                        htmlSanitizeResult.setTruncated(handler.isMaxContentSizeExceeded());
                    }
                } else {
                    CssOnlyCleaningJsoupHandler handler = new CssOnlyCleaningJsoupHandler();
                    handler.setDropExternalImages(options.isDropExternalImages()).setCssPrefix(options.getCssPrefix()).setMaxContentSize(options.getMaxContentSize());
                    handler.setSuppressLinks(options.isSuppressLinks()).setReplaceBodyWithDiv(options.isReplaceBodyWithDiv());

                    boolean[] modified = options.getModified();

                    // Parse the HTML content
                    JsoupParser.getInstance().parse(html, handler, false, false);

                    // Check if modified by handler
                    if (options.isDropExternalImages() && null != modified) {
                        modified[0] |= handler.isImageURLFound();
                    }

                    // Get HTML content
                    if (options.isReplaceBodyWithDiv()) {
                        html = handler.getHtml();
                        if (false == startsWith("<!doctype html>", html, true)) {
                            html = "<!doctype html>\n" + html;
                        }
                        htmlSanitizeResult.setTruncated(handler.isMaxContentSizeExceeded());
                        htmlSanitizeResult.setBodyReplacedWithDiv(true);
                    } else {
                        Document document = handler.getDocument();
                        handlePrettyPrint(options, document);
                        html = hasBody || document.body() == null ? document.outerHtml() : document.body().html();
                        htmlSanitizeResult.setTruncated(handler.isMaxContentSizeExceeded());
                    }
                }
            }

            // Replace HTML entities
            html = keepUnicodeForEntities(html);
            htmlSanitizeResult.setContent(html);

            /*-
             *
            System.out.println(" ---------------------------------- ");
            System.out.println("Was:\n" + htmlContent);
            System.out.println(" >>>>>>>>>>>>>>>>>>>>>>>> ");
            System.out.println("Now:\n" + htmlSanitizeResult.getContent());
            */

            return htmlSanitizeResult;
        } catch (RuntimeException e) {
            LOG.warn("HTML content will be returned un-sanitized.", e);
            return htmlSanitizeResult;
        }
    }

    private void checkSize(String html) throws OXException {
        int maxLength = HtmlServices.htmlThreshold();
        if (maxLength > 0 && html.length() > maxLength) {
            LOG.info("HTML content is too big: max. '{}', but is '{}'.", I(maxLength), I(html.length()));
            throw HtmlExceptionCodes.TOO_BIG.create(I(maxLength), I(html.length()));
        }
    }

    private static void handlePrettyPrint(HtmlSanitizeOptions options, Document document) {
        if (false == options.isPrettyPrint()) {
            document.outputSettings(new Document.OutputSettings().prettyPrint(false));
        }
    }

    private static String removeComments(String html, boolean hasBody, HtmlSanitizeOptions options) {
        Document document = Jsoup.parse(html);
        final Set<Node> removedNodes = new HashSet<>(16, 0.9F);
        document.traverse(new NodeVisitor() {

            @Override
            public void tail(Node node, int depth) {
                // Ignore
            }

            @Override
            public void head(Node node, int depth) {
                if (node instanceof Comment) {
                    removedNodes.add(node);
                }
            }
        });
        for (Node node : removedNodes) {
            node.remove();
        }
        handlePrettyPrint(options, document);
        return hasBody ? document.outerHtml() : document.body().html();
    }

    private FilterJerichoHandler getHandlerFor(int initialCapacity, String optionalConfigName) {
        if (null == optionalConfigName) {
            return new FilterJerichoHandler(initialCapacity, this);
        }
        String definition = getConfiguration().getText(optionalConfigName.endsWith(".properties") ? optionalConfigName : optionalConfigName + ".properties");
        return null == definition ? new FilterJerichoHandler(initialCapacity, this) : new FilterJerichoHandler(initialCapacity, definition, this);
    }

    private CleaningJsoupHandler getJsoupHandlerFor(String optionalConfigName) {
        if (null == optionalConfigName) {
            return new CleaningJsoupHandler();
        }
        String definition = getConfiguration().getText(optionalConfigName.endsWith(".properties") ? optionalConfigName : optionalConfigName + ".properties");
        return null == definition ? new CleaningJsoupHandler() : new CleaningJsoupHandler(definition);
    }

    private static final Pattern PATTERN_FIX_START_TAG = Pattern.compile("(<[a-zA-Z_0-9-]+)/+([a-zA-Z_0-9-][^>]+)");

    private static String dropSlashedTags(final String html) {
        if (null == html) {
            return html;
        }
        final Matcher m = PATTERN_FIX_START_TAG.matcher(html);
        if (!m.find()) {
            /*
             * No slashed tags found
             */
            return html;
        }
        final StringBuffer sb = new StringBuffer(html.length());
        do {
            m.appendReplacement(sb, "$1 $2");
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern PATTERN_EXTRA_CHAR = Pattern.compile("(<[a-zA-Z_0-9-]++)(/\\s*[^>]|[^\\s>/][^\\s>a-zA-Z_0-9]*)");
    //                                                                                    ^^^^^^^^^                            Attempts to catch "<img/ ..." constructs
    //                                                                                                      ^^^^^^^^^^^^^^^^^  Attempts to catch all other bad constructs; e.g. "<a!/...", "<a~..."

    /**
     * Attempts to drop any illegal, extraneous character that is trailing a valid tag start sequence;<br>
     * e.g. "&lt;a&lt; href=..." or "&lt;a~ href=..."
     *
     * @param html The HTML content to process
     * @return The processed HTML content
     */
    private static String dropExtraChar(String html) {
        if (null == html) {
            return html;
        }

        String tmp = html;
        Matcher m = PATTERN_EXTRA_CHAR.matcher(tmp);
        if (!m.find()) {
            /*
             * No extra LT found
             */
            return html;
        }

        StringBuffer sb = new StringBuffer(tmp.length());
        do {
            sb.setLength(0);
            String extraneous;
            do {
                extraneous = m.group(2);
                if (extraneous.startsWith("/")) {
                    m.appendReplacement(sb, "$1" + extraneous.substring(1));
                } else {
                    if (m.end() >= tmp.length()) {
                        m.appendReplacement(sb, "$1");
                    } else {
                        boolean appendWS = !Strings.isWhitespace(tmp.charAt(m.end()));
                        m.appendReplacement(sb, appendWS ? "$1 " : "$1");
                    }
                }
            } while (m.find());
            m.appendTail(sb);

            tmp = sb.toString();
            m = PATTERN_EXTRA_CHAR.matcher(tmp);
        } while (m.find());
        return tmp;
    }

    private static final Pattern PATTERN_TAG = Pattern.compile("<\\w+?[^>]*>");
    private static final Pattern PATTERN_DOUBLE_ACCENTS = Pattern.compile(Pattern.quote("\u0060\u0060") + "|" + Pattern.quote("\u00b4\u00b4"));
    private static final Pattern PATTERN_ACCENT1 = Pattern.compile(Pattern.quote("\u0060"));
    private static final Pattern PATTERN_ACCENT2 = Pattern.compile(Pattern.quote("\u00b4"));

    private static String dropDoubleAccents(final String html) {
        if (null == html || (html.indexOf('\u0060') < 0 && html.indexOf('\u00b4') < 0)) {
            return html;
        }
        final Matcher m = PATTERN_TAG.matcher(html);
        if (!m.find()) {
            /*
             * No conditional comments found
             */
            return html;
        }
        int lastMatch = 0;
        final StringBuilder sb = new StringBuilder(html.length());
        do {
            sb.append(html.substring(lastMatch, m.start()));
            final String match = m.group();
            if (!isEmpty(match)) {
                if (match.indexOf('\u0060') < 0 && match.indexOf('\u00b4') < 0) {
                    sb.append(match);
                } else {
                    sb.append(PATTERN_DOUBLE_ACCENTS.matcher(match).replaceAll(""));
                }
            }
            lastMatch = m.end();
        } while (m.find());
        sb.append(html.substring(lastMatch));
        String ret = PATTERN_ACCENT1.matcher(sb.toString()).replaceAll("&#96;");
        ret = PATTERN_ACCENT2.matcher(ret).replaceAll("&#180;");
        return ret;
    }

    @Override
    public String extractText(String htmlContent) throws OXException {
        if (Strings.isEmpty(htmlContent)) {
            return htmlContent;
        }

        try {
            BoilerpipeExtractor extractor = CommonExtractors.KEEP_EVERYTHING_EXTRACTOR;
            return extractor.getText(htmlContent);
        } catch (BoilerpipeProcessingException e) {
            throw new OXException(e);
        }
    }

    @Override
    public String extractText(Reader htmlInput) throws OXException {
        if (null == htmlInput) {
            return null;
        }

        try {
            BoilerpipeExtractor extractor = CommonExtractors.KEEP_EVERYTHING_EXTRACTOR;
            return extractor.getText(htmlInput);
        } catch (BoilerpipeProcessingException e) {
            throw new OXException(e);
        }
    }

    @Override
    public String html2text(final String htmlContent, final boolean appendHref) {
        boolean preferJericho = true;

        int timeout = htmlParseTimeoutSec;
        if (timeout <= 0) {
            return doHtml2Text(htmlContent, appendHref, preferJericho);
        }

        // Ensure control thread is running
        Thread html2TextControlRunner = this.html2TextControlRunner;
        if (null == html2TextControlRunner) {
            synchronized (this) {
                html2TextControlRunner = this.html2TextControlRunner;
                if (null == html2TextControlRunner) {
                    html2TextControlRunner = new Thread(new Html2TextControlTask(), "Html2TextControl");
                    html2TextControlRunner.start();
                    this.html2TextControlRunner = html2TextControlRunner;
                }
            }
        }

        // Run as a monitored task
        return new Html2TextTask(htmlContent, appendHref, preferJericho, timeout, this).call();
    }

    /**
     * Converts specified HTML content to plain text.
     *
     * @param htmlContent The <b>validated</b> HTML content
     * @param appendHref <code>true</code> to append URLs contained in <i>href</i>s and <i>src</i>s; otherwise <code>false</code>.<br>
     *            Example: <code>&lt;a&nbsp;href=\"www.somewhere.com\"&gt;Link&lt;a&gt;</code> would be
     *            <code>Link&nbsp;[www.somewhere.com]</code>
     * @param preferJericho Whether to prefer Jericho for HTML-to-text conversion or JSoup
     * @return The plain text representation of specified HTML content
     */
    public String doHtml2Text(final String htmlContent, final boolean appendHref, final boolean preferJericho) {
        if (isEmpty(htmlContent)) {
            return htmlContent;
        }

        try {
            String prepared = htmlContent;

            // Keep considerable content inside <head> element
            {
                Source source = new Source(InterruptibleCharSequence.valueOf(prepared));
                source.fullSequentialParse();
                OutputDocument outputDocument = new OutputDocument(source);

                Element headElement = source.getFirstElement(HTMLElementName.HEAD);
                if (null != headElement) {
                    // Check for other content that should not reside in <head>
                    List<Element> allElements = headElement.getChildElements();
                    if (null != allElements) {
                        Set<String> elementsAllowedInHead = HtmlServices.getElementsAllowedInHead();
                        boolean any = false;
                        for (Iterator<Element> it = allElements.iterator(); false == any && it.hasNext();) {
                            Element element = it.next();
                            if (!elementsAllowedInHead.contains(Strings.asciiLowerCase(element.getName()))) {
                                any = true;
                            }
                        }

                        if (any) {
                            for (Element element : allElements) {
                                if (elementsAllowedInHead.contains(Strings.asciiLowerCase(element.getName()))) {
                                    outputDocument.remove(element);
                                }
                            }

                            StartTag headStart = headElement.getStartTag();
                            if (null != headStart) {
                                outputDocument.remove(headStart);
                            }
                            EndTag headEnd = headElement.getEndTag();
                            if (null != headEnd) {
                                outputDocument.remove(headEnd);
                            }
                            prepared = outputDocument.toString().trim();
                        }
                    }
                }
            }

            prepared = prepareSignatureStart(InterruptibleCharSequence.valueOf(prepared));
            prepared = prepareHrTag(InterruptibleCharSequence.valueOf(prepared));
            prepared = prepareAnchorTag(InterruptibleCharSequence.valueOf(prepared));
            prepared = insertBlockquoteMarker(InterruptibleCharSequence.valueOf(prepared));
            prepared = insertSpaceMarker(InterruptibleCharSequence.valueOf(prepared));

            String text;
            if (preferJericho || HtmlServices.useJericho()) {
                JerichoRenderer renderer = new JerichoRenderer(new Segment(new Source(InterruptibleCharSequence.valueOf(prepared)), 0, prepared.length())) {

                    @Override
                    public String renderHyperlinkURL(final StartTag startTag) {
                        /*
                         * The default returns the 'href' content in angle brackets, i.e.
                         * <a href="http://www.example.com">Link</a> is transformed into
                         * <http://www.example.com>. Some web mailers tend to remove text
                         * within angle brackets in plain text modes, which leads to missing
                         * links in plain text replies/forwards. We override this behavior
                         * here, to return the href content as is as described in the JavaDoc
                         * of Renderer().renderHyperlinkURL(StartTag).
                         */
                        final String href = startTag.getAttributeValue("href");
                        if (href == null || href.startsWith("javascript:")) {
                            return null;
                        }
                        try {
                            URI uri = new URI(href);
                            if (!uri.isAbsolute()) {
                                return null;
                            }
                        } catch (URISyntaxException ex) {
                            LOG.debug("Invalid URI", ex);
                            return null;
                        }
                        return href;
                    }
                };
                renderer.setConvertNonBreakingSpaces(true).setMaxLineLength(9999).setIncludeHyperlinkURLs(appendHref);
                text = quoteText(renderer.toString());
            } else {
                Document document = Jsoup.parse(prepared);
                document.outputSettings().prettyPrint(false);
                text = HtmlToPlainText.getPlainText(document, 9999, appendHref);
                text = quoteText(text);
            }

            // Drop heading whitespaces
            //text = PATTERN_HEADING_WS.matcher(InterruptibleCharSequence.valueOf(text)).replaceAll("$1");
            // ... but keep enforced ones
            text = whitespaceText(InterruptibleCharSequence.valueOf(text));
            return text;
        } catch (InterruptedRuntimeException | InterruptedParsingException | InterruptedConversionException ipe) {
            LOG.warn("Timeout during html2text conversion.", ipe);
            // Retry with Tika framework
            return tikaHtml2Text(htmlContent);
        } catch (Exception ioe) {
            // Unfortunately it may happen...
            LOG.warn("Error during html2text conversion.", ioe);
            // Retry with Tika framework
            return tikaHtml2Text(htmlContent);
        } catch (StackOverflowError soe) {
            // Unfortunately it may happen...
            LOG.warn("Stack-overflow during html2text conversion.", soe);
            // Retry with Tika framework
            return tikaHtml2Text(htmlContent);
        }
    }

    private String tikaHtml2Text(String htmlContent) {
        try (ByteArrayInputStream newByteArrayInputStream = Streams.newByteArrayInputStream(htmlContent.getBytes(Charsets.UTF_8))) {
            return TikaUtils.html2text(newByteArrayInputStream);
        } catch (IOException e) {
            LOG.error("An error occurred while converting HTML2text by using Tika.", e);
        }
        return htmlContent;
    }

    private static final Pattern PATTERN_ANCHOR = Pattern.compile("<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static String prepareAnchorTag(final CharSequence htmlContent) {
        final Matcher m = PATTERN_ANCHOR.matcher(htmlContent);
        if (!m.find()) {
            return htmlContent.toString();
        }
        final StringBuffer sb = new StringBuffer(htmlContent.length());
        do {
            final String href = m.group(1);
            if (href.equals(m.group(2))) { // href attribute equals anchor's text
                m.appendReplacement(sb, com.openexchange.java.Strings.quoteReplacement(href));
            }
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern PATTERN_HR = Pattern.compile("<hr[^>]*>(.*?</hr>)?", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static String prepareHrTag(final CharSequence htmlContent) {
        final Matcher m = PATTERN_HR.matcher(htmlContent);
        if (!m.find()) {
            return htmlContent.toString();
        }
        final StringBuffer sb = new StringBuffer(htmlContent.length());
        final String repl = "<br>---------------------------------------------<br>";
        do {
            final String tail = m.group(1);
            if (null == tail || "</hr>".equals(tail)) {
                m.appendReplacement(sb, com.openexchange.java.Strings.quoteReplacement(repl));
            } else {
                m.appendReplacement(sb, com.openexchange.java.Strings.quoteReplacement(repl + tail.substring(0, tail.length() - 5)));
            }
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern PATTERN_SIGNATURE_START = Pattern.compile("(?:\r?\n|^)([ \t]*)-- (\r?\n)");

    private static String prepareSignatureStart(final CharSequence htmlContent) {
        final Matcher m = PATTERN_SIGNATURE_START.matcher(htmlContent);
        if (!m.find()) {
            return htmlContent.toString();
        }
        final StringBuffer sb = new StringBuffer(htmlContent.length());
        m.appendReplacement(sb, "$1--&#160;$2");
        m.appendTail(sb);
        return sb.toString();
    }

    private static final String SPACE_MARKER = "--?space?--";

    private static final Pattern PATTERN_SPACE_MARKER = Pattern.compile(Pattern.quote(SPACE_MARKER));

    private static String whitespaceText(final CharSequence text) {
        return PATTERN_SPACE_MARKER.matcher(text).replaceAll(" ");
    }

    private static final Pattern PATTERN_HTML_MANDATORY_SPACE = Pattern.compile("&#160;|&nbsp;");

    private static String insertSpaceMarker(final CharSequence html) {
        return PATTERN_HTML_MANDATORY_SPACE.matcher(html).replaceAll(SPACE_MARKER);
    }

    private static final String CRLF = "\r\n";

    private static final String SPECIAL = "=?";

    private static final String END = "--";

    private static final String BLOCKQUOTE_MARKER = SPECIAL + UUID.randomUUID().toString();

    private static final String BLOCKQUOTE_MARKER_END = BLOCKQUOTE_MARKER + END;

    private static final Pattern PATTERN_MARKER = Pattern.compile(Pattern.quote(BLOCKQUOTE_MARKER) + "(?:" + Pattern.quote(END) + ")?");

    private static String quoteText(final String text) {
        if (text.indexOf(SPECIAL) < 0) {
            return text;
        }
        final String marker = BLOCKQUOTE_MARKER;
        final int len = marker.length();
        final String[] lines = Strings.splitByLineSeparator(text);
        final StringBuilder sb = new StringBuilder(text.length());
        int quote = 0;
        String prefix = "";
        for (String line : lines) {
            final int pos = line.indexOf(marker);
            if (pos >= 0) {
                if (pos > 0) {
                    sb.append(prefix).append(line.substring(0, pos));
                }
                final int endPos = len + pos;
                if (line.length() >= endPos && line.startsWith(END, endPos)) { // Marker for blockquote end
                    quote--;
                    line = line.substring(endPos + 2).trim();
                } else {
                    quote++;
                    line = line.substring(endPos).trim();
                }
                prefix = getPrefixFor(quote);
                if (isEmpty(line)) {
                    continue;
                }
            }
            sb.append(prefix).append(line).append(CRLF);
        }
        final String retval = sb.toString();
        if (retval.indexOf(SPECIAL) < 0) {
            return retval;
        }
        return PATTERN_MARKER.matcher(InterruptibleCharSequence.valueOf(retval)).replaceAll("");
    }

    private static String getPrefixFor(final int quote) {
        if (quote <= 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < quote; i++) {
            sb.append("> ");
        }
        return sb.toString();
    }

    private static final Pattern PATTERN_BLOCKQUOTE_START = Pattern.compile("(<blockquote.*?>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_BLOCKQUOTE_END = Pattern.compile("(</blockquote>)", Pattern.CASE_INSENSITIVE);

    private static String insertBlockquoteMarker(final CharSequence html) {
        return PATTERN_BLOCKQUOTE_END.matcher(PATTERN_BLOCKQUOTE_START.matcher(html).replaceAll("$1" + BLOCKQUOTE_MARKER)).replaceAll("$1" + BLOCKQUOTE_MARKER_END);
    }

    private static final String HTML_BR = "<br>"; // + Strings.getLineSeparator();

    /**
     * {@inheritDoc}
     */
    @Override
    public HtmlSanitizeResult htmlFormat(final String plainText, final boolean withQuote, final String commentId, int maxContentSize) {
        String content = escape(plainText, withQuote, commentId);
        content = Strings.replaceSequenceWith(content, "\r\n", HTML_BR);
        content = Strings.replaceSequenceWith(content, "\n", HTML_BR);

        HtmlSanitizeResult htmlSanitizeResult = new HtmlSanitizeResult(content);

        int maxSize = maxContentSize;
        if (!(maxSize >= 10000) && !(maxSize <= 0)) {
            maxSize = 10000;
        }

        if ((maxSize > 0) && (maxSize < content.length())) {
            int endOfSentence = content.indexOf('.', maxSize) + 1;
            content = content.substring(0, endOfSentence);
            htmlSanitizeResult.setTruncated(true);
        }
        content = keepUnicodeForEntities(content);
        htmlSanitizeResult.setContent(content);

        return htmlSanitizeResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String htmlFormat(final String plainText, final boolean withQuote, final String commentId) {
        return htmlFormat(plainText, withQuote, commentId, -1).getContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String htmlFormat(final String plainText, final boolean withQuote) {
        return htmlFormat(plainText, withQuote, null);
    }

    private String escape(final String s, final boolean withQuote, final String commentId) {
        StringBuilder sb = new StringBuilder(s.length());
        if (null == commentId) {
            escapePlain(s, withQuote, sb);
            return sb.toString();
        }
        /*
         * Specify pattern & matcher
         */
        Pattern p = Pattern.compile(sb.append(Pattern.quote("<!--" + commentId + ' ')).append("(.+?)").append(Pattern.quote("-->")).toString(), Pattern.DOTALL);
        sb.setLength(0);
        Matcher m = p.matcher(s);
        if (!m.find()) {
            escapePlain(s, withQuote, sb);
            return sb.toString();
        }

        int lastMatch = 0;
        do {
            escapePlain(s.substring(lastMatch, m.start()), withQuote, sb);
            sb.append(m.group(1));
            lastMatch = m.end();
        } while (m.find());
        escapePlain(s.substring(lastMatch), withQuote, sb);
        return sb.toString();
    }

    private void escapePlain(String s, boolean withQuote, StringBuilder htmlBuilder) {
        int length = s.length();
        TIntObjectMap<String> htmlChar2EntityMap = htmlCharMap;

        for (int i = 0, k = length; k-- > 0;) {
            char c = s.charAt(i++);
            if (withQuote && '"' == c) {
                // Append quote character as-is
                htmlBuilder.append(c);
            } else {
                String optEntity = htmlChar2EntityMap.get(c);
                if (optEntity != null) {
                    // HTML entity
                    htmlBuilder.append('&').append(optEntity).append(';');
                } else if (c <= 127) {
                    // ASCII character
                    htmlBuilder.append(c);
                } else if (k <= 0) {
                    // No next character to check for a possible Unicode surrogate pair
                    appendNonAsciiChar(c, htmlBuilder);
                } else {
                    char nc = s.charAt(i);
                    if (false == Character.isSurrogatePair(c, nc)) {
                        // Not a Unicode surrogate pair.
                        appendNonAsciiChar(c, htmlBuilder);
                    } else {
                        // Surrogate pair
                        int codePoint = Character.toCodePoint(c, nc);
                        if (EmojiRegistry.getInstance().isEmoji(codePoint)) {
                            // Keep unicode for Emoji
                            htmlBuilder.appendCodePoint(codePoint);
                        } else {
                            htmlBuilder.append("&#").append(codePoint).append(';');
                        }
                        k--;
                        i++;
                    }
                }
            }
        }
    }

    private void appendNonAsciiChar(char c, StringBuilder sb) {
        if (EmojiRegistry.getInstance().isEmoji(c)) {
            // Keep unicode for Emoji
            sb.append(c);
        } else {
            sb.append("&#").append((int) c).append(';');
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String htmlFormat(final String plainText) {
        return htmlFormat(plainText, true);
    }

    private static final String REGEX_URL_SOLE = "\\b(?:https?://|ftp://|mailto:|news\\.|www\\.|tel:)[-\\p{L}\\p{Sc}0-9+&@#/%?=~_()|!:,.;\\[\\]]*[-\\p{L}\\p{Sc}0-9+&@#/%=~_()|]";

    /**
     * The regular expression to match URLs inside text:<br>
     * <code>\b(?:https?://|ftp://|mailto:|news\\.|www\.|tel:)[-\p{L}\p{Sc}0-9+&@#/%?=~_()|!:,.;]*[-\p{L}\p{Sc}0-9+&@#/%=~_()|]</code>
     * <p>
     * Parentheses, if present, are allowed in the URL -- The leading one is <b>not</b> absorbed.
     */
    public static final Pattern PATTERN_URL_SOLE = Pattern.compile(REGEX_URL_SOLE);

    private static final String REGEX_URL = "\\(?" + REGEX_URL_SOLE;

    /**
     * The regular expression to match URLs inside text:<br>
     * <code>\(?\b(?:https?://|ftp://|mailto:|news\\.|www\.)[-\p{L}\p{Sc}0-9+&@#/%?=~_()|!:,.;]*[-\p{L}\p{Sc}0-9+&@#/%=~_()|]</code>
     * <p>
     * Parentheses, if present, are allowed in the URL -- The leading one is absorbed, too.
     *
     * <pre>
     * String s = matcher.group();
     * int mlen = s.length() - 1;
     * if (mlen &gt; 0 &amp;&amp; '(' == s.charAt(0) &amp;&amp; ')' == s.charAt(mlen)) {
     * s = s.substring(1, mlen);
     * }
     * </pre>
     */
    public static final Pattern PATTERN_URL = Pattern.compile(REGEX_URL);

    public Pattern getURLPattern() {
        return PATTERN_URL;
    }

    private static final String REGEX_ANCHOR = "<a\\s+href[^>]+>.*?</a>";

    private static final Pattern PATTERN_LINK = Pattern.compile(REGEX_ANCHOR + '|' + REGEX_URL);

    public Pattern getLinkPattern() {
        return PATTERN_LINK;
    }

    private static final Pattern PATTERN_LINK_WITH_GROUP = Pattern.compile(REGEX_ANCHOR + "|(" + REGEX_URL + ')');

    public Pattern getLinkWithGroupPattern() {
        return PATTERN_LINK_WITH_GROUP;
    }

    /**
     * Maps specified HTML entity - e.g. <code>&amp;uuml;</code> - to corresponding unicode character.
     *
     * @param entity The HTML entity
     * @return The corresponding unicode character or <code>null</code>
     */
    @Override
    public Character getHTMLEntity(final String entity) {
        if (null == entity) {
            return null;
        }
        String key = entity;
        if (key.charAt(0) == '&') {
            key = key.substring(1);
        }
        {
            final int lastPos = key.length() - 1;
            if (key.charAt(lastPos) == ';') {
                key = key.substring(0, lastPos);
            }
        }
        return htmlEntityMap.get(key);
    }

    private static final Pattern PAT_ENTITIES = Pattern.compile("&(?:#([0-9]+)|#x([0-9a-fA-F]+)|([a-zA-Z]+));");

    @Override
    public String replaceHTMLEntities(final String content) {
        final Matcher m = PAT_ENTITIES.matcher(content);
        final MatcherReplacer mr = new MatcherReplacer(m, content);
        final Stringer sb = new StringBuilderStringer(new StringBuilder(content.length()));
        while (m.find()) {
            /*
             * Try decimal syntax; e.g. &#39; (single-quote)
             */
            int numEntity = numOf(m.group(1), 10);
            if (numEntity >= 0) {
                /*
                 * Detected decimal value
                 */
                mr.appendLiteralReplacement(sb, String.valueOf((char) numEntity));
            } else {
                /*
                 * Try hexadecimal syntax; e.g. &#xFC;
                 */
                numEntity = numOf(m.group(2), 16);
                if (numEntity >= 0) {
                    /*
                     * Detected hexadecimal value
                     */
                    mr.appendLiteralReplacement(sb, String.valueOf((char) numEntity));
                } else {
                    /*
                     * No numeric entity syntax, assume a non-numeric entity like &quot; or &nbsp;
                     */
                    final Character entity = getHTMLEntity(m.group(3));
                    if (null != entity) {
                        mr.appendLiteralReplacement(sb, entity.toString());
                    }
                }
            }
        }
        mr.appendTail(sb);
        return sb.toString();
    }

    private static int numOf(final String possibleNum, final int radix) {
        if (null == possibleNum) {
            return -1;
        }
        try {
            return Integer.parseInt(possibleNum, radix);
        } catch (NumberFormatException e) {
            LOG.trace("", e);
            return -1;
        }
    }

    @Override
    public String prettyPrint(final String htmlContent) {
        if (Strings.isEmpty(htmlContent)) {
            return htmlContent;
        }
        try {
            /*
             * Clean...
             */
            final TagNode htmlNode = newHtmlCleaner().clean(htmlContent);
            if (null == htmlNode) {
                return htmlContent;
            }
            /*
             * Serialize
             */
            final AllocatingStringWriter writer = new AllocatingStringWriter(htmlContent.length());
            newSerializer().write(htmlNode, writer, "UTF-8");
            return writer.toString();
        } catch (UnsupportedEncodingException e) {
            // Cannot occur
            LOG.error("Unsupported encoding", e);
            return htmlContent;
        } catch (IOException e) {
            // Cannot occur
            LOG.error("I/O error", e);
            return htmlContent;
        } catch (RuntimeException rte) {
            /*
             * HtmlCleaner failed horribly...
             */
            LOG.warn("HtmlCleaner library failed to pretty-print HTML content", rte);
            return htmlContent;
        }
    }

    private static final Pattern PATTERN_BODY_START = Pattern.compile(Pattern.quote("<body"), Pattern.CASE_INSENSITIVE);

    @Override
    public String checkBaseTag(final String htmlContent, final boolean externalImagesAllowed) {
        if (Strings.isEmpty(htmlContent)) {
            return htmlContent;
        }
        /*
         * The <base> tag must be between the document's <head> tags. Also, there must be no more than one base element per document.
         */
        final Matcher m1 = PATTERN_BODY_START.matcher(htmlContent);
        return checkBaseTag(htmlContent, m1.find() ? m1.start() : htmlContent.length());
    }

    private static final Pattern PATTERN_BASE_TAG = Pattern.compile("<base[^>]*href=\\s*(?:\"|')(\\S*?)(?:\"|')[^>]*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern BACKGROUND_PATTERN = Pattern.compile("(<[a-zA-Z]+[^>]*?)(?:(?:background=([^\\s>]*))|(?:background=\"([^\"]*)\"))([^>]*/?>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern HREF_PATTERN = Pattern.compile("(<[a-zA-Z]+[^>]*?)(?:(?:href=\\\"([^\\\"]*)\\\")|(?:href=([^\\s>]*)))([^>]*/?>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static String checkBaseTag(final String htmlContent, final int end) {
        Matcher m = PATTERN_BASE_TAG.matcher(htmlContent);
        if (!m.find() || m.end() >= end) {
            return htmlContent;
        }
        /*
         * Find bases
         */
        String base = m.group(1);
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        /*
         * Convert to absolute URIs
         */
        String html = htmlContent.substring(0, m.start()) + htmlContent.substring(m.end());
        html = sanitizeAttributes(html);
        m = ImageProcessor.getInstance().getImgPattern().matcher(html);
        MatcherReplacer mr = new MatcherReplacer(m, html);
        final Stringer sb = new StringBuilderStringer(new StringBuilder(html.length()));
        if (m.find()) {
            /*
             * Replace images
             */
            do {
                final String imgTag = m.group();
                final int pos = imgTag.indexOf("src=");
                final int epos;
                if (pos >= 0) {
                    String href;
                    final char c = imgTag.charAt(pos + 4);
                    if ('"' == c) {
                        epos = imgTag.indexOf('"', pos + 5);
                        href = imgTag.substring(pos + 5, epos);
                    } else if ('\'' == c) {
                        epos = imgTag.indexOf('\'', pos + 5);
                        href = imgTag.substring(pos + 5, epos);
                    } else {
                        epos = imgTag.indexOf('>', pos + 4);
                        href = imgTag.substring(pos + 4, epos);
                    }
                    if (!href.startsWith("cid") && !href.startsWith("http") && !href.startsWith("mailto")) {
                        if (!href.startsWith("/")) {
                            href = '/' + href;
                        }
                        final String replacement = imgTag.substring(0, pos) + "src=\"" + base + href + "\"" + imgTag.substring(epos);
                        mr.appendLiteralReplacement(sb, replacement);
                    }
                }
            } while (m.find());
        }
        mr.appendTail(sb);
        html = sb.toString();
        sb.setLength(0);
        m = BACKGROUND_PATTERN.matcher(html);
        mr = new MatcherReplacer(m, html);
        if (m.find()) {
            /*
             * Replace images
             */
            do {
                final String hrefTag = m.group();
                final int pos = hrefTag.indexOf("background=");
                final int epos;
                if (pos >= 0) {
                    String href;
                    final char c = hrefTag.charAt(pos + 11);
                    if ('"' == c) {
                        epos = hrefTag.indexOf('"', pos + 12);
                        href = hrefTag.substring(pos + 12, epos);
                    } else if ('\'' == c) {
                        epos = hrefTag.indexOf('\'', pos + 12);
                        href = hrefTag.substring(pos + 12, epos);
                    } else {
                        epos = hrefTag.indexOf('>', pos + 11);
                        href = hrefTag.substring(pos + 11, epos);
                    }
                    if (!href.startsWith("cid") && !href.startsWith("http") && !href.startsWith("mailto")) {
                        if (!href.startsWith("/")) {
                            href = '/' + href;
                        }
                        final String replacement = hrefTag.substring(0, pos) + "background=\"" + base + href + "\"" + hrefTag.substring(epos);
                        mr.appendLiteralReplacement(sb, replacement);
                    }
                }
            } while (m.find());
        }
        mr.appendTail(sb);
        html = sb.toString();
        sb.setLength(0);
        m = HREF_PATTERN.matcher(html);
        mr = new MatcherReplacer(m, html);
        if (m.find()) {
            /*
             * Replace images
             */
            do {
                final String hrefTag = m.group();
                final int pos = hrefTag.indexOf("href=");
                final int epos;
                if (pos >= 0) {
                    String href;
                    final char c = hrefTag.charAt(pos + 5);
                    if ('"' == c) {
                        epos = hrefTag.indexOf('"', pos + 6);
                        href = hrefTag.substring(pos + 6, epos);
                    } else if ('\'' == c) {
                        epos = hrefTag.indexOf('\'', pos + 6);
                        href = hrefTag.substring(pos + 6, epos);
                    } else {
                        epos = hrefTag.indexOf('>', pos + 5);
                        href = hrefTag.substring(pos + 5, epos);
                    }
                    if (!href.startsWith("cid") && !href.startsWith("http") && !href.startsWith("mailto")) {
                        if (!href.startsWith("/")) {
                            href = '/' + href;
                        }
                        final String replacement = hrefTag.substring(0, pos) + "href=\"" + base + href + "\"" + hrefTag.substring(epos);
                        mr.appendLiteralReplacement(sb, replacement);
                    }
                }
            } while (m.find());
        }
        mr.appendTail(sb);
        html = sb.toString();
        return html;
    }

    private final static Pattern SRC_ATTRIBUTE_PATTERN = Pattern.compile("(src= *\"[^\"]*\")|(src= *'[^']*')", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static String sanitizeAttributes(String html) {
        Matcher m = SRC_ATTRIBUTE_PATTERN.matcher(html);
        String retval = html;
        if (m.find()) {
            /*
             * Replace < > with &lt; &gt;
             */
            do {
                final String attribute = m.group();
                if (attribute.contains("<") && attribute.contains(">")) {
                    String replace = attribute;
                    replace = replace.replace("<", "&lt;");
                    replace = replace.replace(">", "&gt;");
                    retval = html.replace(attribute, replace);
                }

            } while (m.find());
        }
        return retval;
    }

    @Override
    public String dropScriptTagsInHeader(final String htmlContent) {
        if (null == htmlContent || htmlContent.indexOf("<script") < 0) {
            return htmlContent;
        }
        final Matcher m1 = PATTERN_BODY_START.matcher(htmlContent);
        return dropScriptTagsInHeader(htmlContent, m1.find() ? m1.start() : htmlContent.length());
    }

    private static final Pattern PATTERN_SCRIPT_TAG = Pattern.compile(
        "<script[^>]*>" + ".*?" + "</script>",
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static String dropScriptTagsInHeader(final String htmlContent, final int end) {
        final Matcher m = PATTERN_SCRIPT_TAG.matcher(htmlContent);
        if (!m.find() || m.end() >= end) {
            return htmlContent;
        }
        final Stringer sb = new StringBuilderStringer(new StringBuilder(htmlContent.length()));
        final MatcherReplacer mr = new MatcherReplacer(m, htmlContent);
        do {
            mr.appendLiteralReplacement(sb, "");
        } while (m.find() && m.end() < end);
        mr.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern PATTERN_STYLESHEET_FILE = Pattern.compile(
        "<link.*?(type=['\"]text/css['\"].*?href=['\"](.*?)['\"]|href=['\"](.*?)['\"].*?type=['\"]text/css['\"]).*?/>",
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_STYLESHEET = Pattern.compile("<style.*?>(.*?)</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public String getCSSFromHTMLHeader(final String htmlContent) {
        final StringBuilder css = new StringBuilder(htmlContent.length() >> 1);
        final Matcher mStyle = PATTERN_STYLESHEET.matcher(htmlContent);
        while (mStyle.find()) {
            css.append(mStyle.group(1));
        }
        final Matcher mStyleFile = PATTERN_STYLESHEET_FILE.matcher(htmlContent);
        while (mStyleFile.find()) {
            final String cssFile = mStyleFile.group(2);

            CloseableHttpClient client = null;
            HttpGet get = null;
            try {
                client = HttpClientBuilder.create().setRetryHandler(new DefaultHttpRequestRetryHandler(0, false)).build();
                get = new HttpGet(cssFile);
                get.setConfig(RequestConfig.custom().setSocketTimeout(3000).setConnectTimeout(3000).setCookieSpec(CookieSpecs.DEFAULT).build());
                HttpResponse resp = client.execute(get);
                if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    // throw new OXException(); //TODO: set exceptioncode
                } else {
                    final byte[] responseBody = EntityUtils.toByteArray(resp.getEntity());
                    if (null != responseBody && responseBody.length > 0) {
                        try {
                            final Charset charSet = ContentType.getOrDefault(resp.getEntity()).getCharset();
                            css.append(new String(responseBody, null == charSet ? Charsets.ISO_8859_1 : charSet));
                        } catch (UnsupportedCharsetException e) {
                            css.append(new String(responseBody, Charsets.ISO_8859_1));
                        }
                    }
                }
            } catch (IOException e) {
                // throw new OXException(); //TODO: set exceptioncode
            } finally {
                if (get != null) {
                    try {
                        get.releaseConnection();
                    } catch (Exception e) {
                        /* ignore */ }
                }
                Streams.close(client);
            }
        }
        return css.toString();
    }

    private static final Pattern HTML_START = Pattern.compile("<html.*?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern HEAD_START = Pattern.compile("<head.*?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern BODY_START = Pattern.compile("<body.*?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public String documentizeContent(final String htmlContent, final String charset) {
        if (null == htmlContent) {
            return htmlContent;
        }
        final String lineSeparator = this.lineSeparator;
        final StringBuilder sb = new StringBuilder(htmlContent.length() + 512);
        if (isEmpty(htmlContent)) {
            sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">").append(lineSeparator);
            sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">").append(lineSeparator);
            sb.append("<head>").append(lineSeparator);
            sb.append("    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=").append(charset).append("\" />").append(lineSeparator);
            sb.append("</head>").append(lineSeparator);
            sb.append("<body>").append(lineSeparator);
            sb.append(htmlContent);
            sb.append("</body>").append(lineSeparator);
            sb.append("</html>");
            return sb.toString();
        }
        // Check for <html> tag
        boolean closeHtml = false;
        if (!HTML_START.matcher(htmlContent).find()) {
            sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">").append(lineSeparator);
            sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">").append(lineSeparator);
            closeHtml = true;
        }
        // Check for <head> tag
        if (!HEAD_START.matcher(htmlContent).find()) {
            sb.append("<head>").append(lineSeparator);
            sb.append("    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=").append(charset).append("\" />").append(lineSeparator);
            sb.append("</head>").append(lineSeparator);
        }
        // Check for <body> tag
        boolean closeBody = false;
        if (!BODY_START.matcher(htmlContent).find()) {
            sb.append("<body>").append(lineSeparator);
            closeBody = true;
        }
        sb.append(htmlContent);
        if (closeBody) {
            sb.append("</body>").append(lineSeparator);
        }
        if (closeHtml) {
            sb.append("</html>").append(lineSeparator);
        }
        return sb.toString();
    }

    @Override
    public String getWellFormedHTMLDocument(String htmlContent) throws OXException {
        if (null == htmlContent || 0 == htmlContent.length()) {
            return htmlContent;
        }

        Document document = Jsoup.parse(htmlContent);

        {
            DocumentType docType = null;
            List<Node> nodes = document.childNodes();
            for (Iterator<Node> it = nodes.iterator(); null == docType && it.hasNext();) {
                Node node = it.next();
                if (node instanceof DocumentType) {
                    docType = (DocumentType) node;
                }
            }
            if (null == docType) {
                docType = new DocumentType("html", "", "");
                document.insertChildren(0, docType);
            }
        }

        {
            Elements heads = document.getElementsByTag("head");
            if (false == heads.isEmpty()) {
                org.jsoup.nodes.Element head = heads.get(0);
                org.jsoup.nodes.Element meta = null;
                List<Node> nodes = head.childNodes();
                for (Iterator<Node> it = nodes.iterator(); null == meta && it.hasNext();) {
                    Node node = it.next();
                    if (node instanceof org.jsoup.nodes.Element) {
                        org.jsoup.nodes.Element e = (org.jsoup.nodes.Element) node;
                        if ("meta".equals(e.tagName())) {
                            org.jsoup.nodes.Attributes attributes = e.attributes();
                            if ("Content-Type".equalsIgnoreCase(attributes.get("http-equiv"))) {
                                meta = e;
                            }
                        }
                    }
                }
                if (null == meta) {
                    meta = new org.jsoup.nodes.Element("meta");
                    meta.attr("http-equiv", "Content-Type");
                    meta.attr("content", "text/html; charset=UTF-8");
                    head.insertChildren(0, meta);
                }
            }
        }

        return document.outerHtml();
    }

    @Override
    public String getConformHTML(final String htmlContent, final String charset) throws OXException {
        return getConformHTML(htmlContent, charset, true);
    }

    @Override
    public String getConformHTML(final String htmlContent, final String charset, final boolean replaceUrls) throws OXException {
        if (null == htmlContent || 0 == htmlContent.length()) {
            // Nothing to do...
            return htmlContent;
        }

        // Drop superfluous <div> tags from sanitizing
        String html = dropSuperfluousDivTags(htmlContent);

        // Validate
        html = validate(html);

        // Check for meta tag in validated HTML content which indicates documents content type. Add if missing.
        int headTagLen = TAG_S_HEAD.length();
        int start = html.indexOf(TAG_S_HEAD) + headTagLen;
        if (start >= headTagLen) {
            int end = html.indexOf(TAG_E_HEAD);
            if (!occursWithin(html, start, end, true, "http-equiv=\"content-type\"", "http-equiv=content-type", "charset=\"UTF-8\"", "charset=UTF-8")) {
                StringBuilder sb = new StringBuilder(html);
                String cs = null == charset ? CHARSET_UTF_8 : charset;

                // Append in reverse order
                sb.insert(start, Strings.getLineSeparator());
                sb.insert(start, "\">").append(Strings.getLineSeparator()).append(' ');
                sb.insert(start, cs);

                if (html.indexOf("<!DOCTYPE html>") >= 0) {
                    sb.insert(start, "<meta charset=\"");
                } else {
                    sb.insert(start, "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=");
                }

                sb.insert(start, "    ");
                sb.insert(start, Strings.getLineSeparator());
                html = sb.toString();
            }
        }
        html = processDownlevelRevealedConditionalComments(html);

        // Check URLs
        if (replaceUrls) {
            boolean useJericho = HtmlServices.useJericho();
            if (useJericho) {
                UrlReplacerJerichoHandler handler = new UrlReplacerJerichoHandler(html.length());
                JerichoParser.getInstance().parse(html, handler, false);
                html = handler.getHTML();
            } else {
                UrlReplacerJsoupHandler handler = new UrlReplacerJsoupHandler();
                JsoupParser.getInstance().parse(html, handler, false, true);
                html = handler.getDocument().toString();
            }
        }

        return html;
    }

    /**
     * Drops superfluous <code>&lt;div&gt;</code> tags from given HTML content that were yielded from previous sanitizing.
     * <p>
     * E.g.
     * <p>
     * <code>"&lt;div id="ox-7bf62dbb34"&gt;&lt;p&gt;Some text&lt;/p&gt;&lt;/div&gt;"</code>
     * &nbsp;&nbsp;---&gt;&nbsp;&nbsp;&nbsp;
     * <code>"&lt;p&gt;Some text&lt;/p&gt;"</code>
     *
     * @param htmlContent The HTML content to process
     * @return The processed HTML content
     */
    private static String dropSuperfluousDivTags(final String htmlContent) {
        Source source = new Source(htmlContent);
        source.fullSequentialParse();
        OutputDocument outputDocument = new OutputDocument(source);

        List<Element> divElements = source.getAllElements(HTMLElementName.DIV);
        if (null != divElements && !divElements.isEmpty()) {
            // Iterate <div> tags
            for (Element divElement : divElements) {
                Attributes attributes = divElement.getAttributes();
                if (null != attributes && attributes.size() == 1) {
                    Attribute idAttribute = attributes.get("id");
                    if (null != idAttribute) {
                        String idValue = idAttribute.getValue();
                        if (null != idValue && idValue.startsWith("ox-")) {
                            outputDocument.replace(divElement, divElement.getContent());
                        }
                    }
                }
            }
        }

        return outputDocument.toString();
    }

    private static boolean occursWithin(String str, int start, int end, boolean ignorecase, String... searchStrings) {
        String source = ignorecase ? Strings.asciiLowerCase(str) : str;

        int index;
        for (String searchString : searchStrings) {
            String searchMe = ignorecase ? Strings.asciiLowerCase(searchString) : searchString;
            if (((index = source.indexOf(searchMe, start)) >= start) && ((index + searchMe.length()) < end)) {
                return true;
            }
        }

        return false;
    }

    private static final Pattern PATTERN_XML_NS_DECLARATION = Pattern.compile("<\\?xml:namespace[^>]*>", Pattern.CASE_INSENSITIVE);

    private static String dropWeirdXmlNamespaceDeclarations(String htmlContent) {
        // <?xml:namespace prefix = "o" ns =  "urn:schemas-microsoft-com:office:office" />
        if (null == htmlContent) {
            return htmlContent;
        }

        if (htmlContent.indexOf("<?xml:") < 0 && htmlContent.indexOf("<?XML:") < 0) {
            return htmlContent;
        }

        Matcher m = PATTERN_XML_NS_DECLARATION.matcher(htmlContent);
        if (false == m.find()) {
            return htmlContent;
        }

        StringBuffer sb = new StringBuffer(htmlContent.length());
        do {
            m.appendReplacement(sb, "");
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern PATTERN_CC = Pattern.compile("(<!(?:--)?\\[if)([^\\]>]+\\]?(?:--!?)?>)(.*?)((?:<!\\[endif\\])?(?:--)?>)", Pattern.DOTALL);
    private static final Pattern PATTERN_CC2 = Pattern.compile("(<!(?:--)?\\[if)([^\\]>]+\\]?(?:--!?)?>)(.*?)(<!\\[endif\\](?:--)?>)", Pattern.DOTALL);

    private static final String CC_START_IF = "<!-- [if";

    private static final String CC_END_IF = " -->";

    private static final String CC_ENDIF = "<!-- <![endif] -->";

    private static final String CC_UNCOMMENTED_ENDIF = "<![endif] -->";

    /**
     * Processes detected downlevel-revealed <a href="http://en.wikipedia.org/wiki/Conditional_comment">conditional comments</a> through
     * adding dashes before and after each <code>if</code> statement tag to complete them as a valid HTML comment and leaves center code
     * open to rendering on non-IE browsers:
     *
     * <pre>
     * &lt;![if !IE]&gt;
     * &lt;link rel=&quot;stylesheet&quot; type=&quot;text/css&quot; href=&quot;non-ie.css&quot;&gt;
     * &lt;![endif]&gt;
     * </pre>
     *
     * is turned to
     *
     * <pre>
     * &lt;!--[if !IE]&gt;--&gt;
     * &lt;link rel=&quot;stylesheet&quot; type=&quot;text/css&quot; href=&quot;non-ie.css&quot;&gt;
     * &lt;!--&lt;![endif]--&gt;
     * </pre>
     *
     * @param htmlContent The HTML content possibly containing downlevel-revealed conditional comments
     * @return The HTML content whose downlevel-revealed conditional comments contain valid HTML for non-IE browsers
     */
    private static String processDownlevelRevealedConditionalComments(final String htmlContent) {
        final String ret = processDownlevelRevealedConditionalComments0(htmlContent, PATTERN_CC2);
        return processDownlevelRevealedConditionalComments0(ret, PATTERN_CC);
    }

    private static String processDownlevelRevealedConditionalComments0(final String htmlContent, final Pattern p) {
        final Matcher m = p.matcher(htmlContent);
        if (!m.find()) {
            /*
             * No conditional comments found
             */
            return htmlContent;
        }

        int lastMatch = 0;
        final StringBuilder sb = new StringBuilder(htmlContent.length() + 128);
        do {
            sb.append(htmlContent.substring(lastMatch, m.start()));
            String condition = m.group(2);
            if (condition.indexOf(']') < 0) {
                // Need to insert the missing closing bracket ']' character
                if (condition.endsWith("--!>")) {
                    condition = condition.substring(0, condition.length() - 4) + "]--!>";
                } else if (condition.endsWith("-->")) {
                    condition = condition.substring(0, condition.length() - 3) + "]-->";
                } else {
                    condition = condition.substring(0, condition.length() - 1) + "]>";
                }
            }
            if (isValidCondition(condition)) {
                boolean isDownlevelRevealed = false;
                if (m.group(1).startsWith("<![if")) {
                    isDownlevelRevealed = true;
                }
                //check for downlevel hidden comments and leave them be
                sb.append(CC_START_IF).append(condition);
                final String wrappedContent = m.group(3);
                if (!wrappedContent.startsWith("-->", 0) && !condition.endsWith("-->") && isDownlevelRevealed) {
                    sb.append(CC_END_IF);
                }
                sb.append(wrappedContent);
                if (wrappedContent.endsWith("<!--")) {
                    sb.append(m.group(4));
                } else if (isDownlevelRevealed) {
                    sb.append(CC_ENDIF);
                } else {
                    sb.append(CC_UNCOMMENTED_ENDIF);
                }
            }
            lastMatch = m.end();
        } while (m.find());
        sb.append(htmlContent.substring(lastMatch));
        return sb.toString();
    }

    private static final Pattern PAT_VALID_COND = Pattern.compile("[a-zA-Z_0-9 -!|()]+");

    private static boolean isValidCondition(final String condition) {
        if (isEmpty(condition)) {
            return false;
        }
        return PAT_VALID_COND.matcher(condition.substring(0, condition.indexOf(']'))).matches();
    }

    /**
     * Validates specified HTML content with <a href="http://tidy.sourceforge.net/">tidy html</a> library and falls back using <a
     * href="http://htmlcleaner.sourceforge.net/">HtmlCleaner</a> if any error occurs.
     *
     * @param htmlContent The HTML content
     * @return The validated HTML content
     */
    private static String validate(final String htmlContent) {
        return validateWithHtmlCleaner(replaceHexEntities(htmlContent));
    }

    private static final Pattern PAT_HEX_PERCENT_TAG = Pattern.compile("<%tag[^>]*>", Pattern.CASE_INSENSITIVE);

    private static String replacePercentTags(final String htmlContent) {
        final Matcher m = PAT_HEX_PERCENT_TAG.matcher(htmlContent);
        if (!m.find()) {
            return htmlContent;
        }
        final StringBuffer sb = new StringBuffer(htmlContent.length());
        do {
            m.appendReplacement(sb, "");
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern PAT_HEX_ENTITIES = Pattern.compile("&#x([0-9a-fA-F]+);");

    private static String replaceHexEntities(final String htmlContent) {
        final Matcher m = PAT_HEX_ENTITIES.matcher(htmlContent);
        if (!m.find()) {
            return htmlContent;
        }
        final MatcherReplacer mr = new MatcherReplacer(m, htmlContent);
        final Stringer builder = new StringBuilderStringer(new StringBuilder(htmlContent.length()));
        final StringBuilder tmp = new StringBuilder(8).append("&#");
        do {
            try {
                tmp.setLength(2);
                tmp.append(Integer.parseInt(m.group(1), 16)).append(';');
                mr.appendLiteralReplacement(builder, tmp.toString());
            } catch (NumberFormatException e) {
                tmp.setLength(0);
                tmp.append("&amp;#x").append(m.group(1)).append("&#59;");
                mr.appendLiteralReplacement(builder, tmp.toString());
                tmp.setLength(0);
                tmp.append("&#");
            }
        } while (m.find());
        mr.appendTail(builder);
        return builder.toString();
    }

    private static final Pattern PAT_SPECIAL_ENTITIES = Pattern.compile("&#([0-9a-fA-F]{5,}+);&#([0-9a-fA-F]{5,}+);");

    private static String replaceSpecialEntities(final String htmlContent) {
        final Matcher m = PAT_SPECIAL_ENTITIES.matcher(htmlContent);
        if (!m.find()) {
            return htmlContent;
        }
        final MatcherReplacer mr = new MatcherReplacer(m, htmlContent);
        final Stringer builder = new StringBuilderStringer(new StringBuilder(htmlContent.length()));
        final StringBuilder tmp = new StringBuilder(16);
        do {
            try {
                tmp.setLength(0);
                // Check for valid surrogate pair
                final char c1 = (char) Integer.parseInt(m.group(1), 10);
                final char c2 = (char) Integer.parseInt(m.group(2), 10);
                if (Character.isSurrogatePair(c1, c2)) {
                    final int codePoint = Character.toCodePoint(c1, c2);
                    tmp.setLength(0);
                    tmp.append("&#").append(codePoint).append(';');
                    mr.appendLiteralReplacement(builder, tmp.toString());
                }
            } catch (NumberFormatException e) {
                tmp.setLength(0);
                tmp.append("&amp;#x").append(m.group(1)).append("&#59;");
                tmp.append("&amp;#x").append(m.group(2)).append("&#59;");
                mr.appendLiteralReplacement(builder, tmp.toString());
                tmp.setLength(0);
            }
        } while (m.find());
        mr.appendTail(builder);
        return builder.toString();
    }

    // ----------------------------- JSoup stuff ----------------------------- //

    /**
     * The white-list of permitted HTML elements for <a href="http://jsoup.org/">jsoup</a> library.
     */
    // private static final Whitelist WHITELIST = Whitelist.relaxed();

    /**
     * Pre-process specified HTML content with <a href="http://jsoup.org/">jsoup</a> library.
     *
     * @param htmlContent The HTML content
     * @return The safe HTML content according to JSoup processing
     */
    private static String preprocessWithJSoup(final String htmlContent) {
        return Jsoup.parse(htmlContent).toString();
    }

    // -------------------------- HtmlCleaner stuff -------------------------- //

    /**
     * Generates new {@link CleanerProperties} instance.
     *
     * @return The new {@link CleanerProperties}
     */
    private static CleanerProperties newCleanerProperties() {
        final CleanerProperties props = new CleanerProperties();
        props.setOmitDoctypeDeclaration(true);
        props.setOmitXmlDeclaration(true);
        props.setPruneTags("script");
        props.setTranslateSpecialEntities(true);
        props.setTransSpecialEntitiesToNCR(true);
        props.setTransResCharsToNCR(true);
        props.setRecognizeUnicodeChars(false);
        props.setUseEmptyElementTags(false);
        props.setIgnoreQuestAndExclam(false);
        props.setUseCdataForScriptAndStyle(false);
        props.setIgnoreQuestAndExclam(true);
        props.setAddNewlineToHeadAndBody(true);
        props.setCharset("UTF-8");
        return props;
    }

    /**
     * Creates a new {@link HtmlCleaner} instance.
     *
     * @return The instance
     */
    private static HtmlCleaner newHtmlCleaner() {
        return new HtmlCleaner(newCleanerProperties());
    }

    /**
     * Creates a new {@link Serializer} instance.
     *
     * @return The instance
     */
    private static Serializer newSerializer() {
        return new SimpleHtmlSerializer(newCleanerProperties());
    }

    private static final String DOCTYPE_DECL = "<!DOCTYPE html>" + Strings.getLineSeparator();

    // @formatter:off
    private static final List<String[]> SA_HTMLE = Collections.unmodifiableList(Arrays.asList(
        new String[] {"&#169;","&copy;"},
        new String[] {"&#174;","&reg;"},
        new String[] {"&#8482;","&trade;"},
        new String[] {"&#9829;","&hearts;"},
        new String[] {"&#9824;","&spades;"},
        new String[] {"&#9827;","&clubs;"},
        new String[] {"&#9830;","&diams;"}
        ));

    private static final List<String> S_HTMLE = Collections.unmodifiableList(Arrays.asList(
        "\u00a9",
        "\u00ae",
        "\u2122",
        "\u2665",
        "\u2660",
        "\u2663",
        "\u2666"
        ));
    // @formatter:on

    private static String keepUnicodeForEntities(final String html) {
        StringBuilder ret = new StringBuilder(html);

        int size = SA_HTMLE.size();
        for (int i = 0; i < size; i++) {
            checkAndReplace(SA_HTMLE.get(i), S_HTMLE.get(i), ret);
        }

        return ret.toString();
    }

    private static void checkAndReplace(String[] entities, String replacement, StringBuilder html) {
        for (String entity : entities) {
            for (int index = 0; (index = html.indexOf(entity, index)) >= 0;) {
                html.replace(index, index + entity.length(), replacement);
            }
        }
    }

    private static final Pattern END_TAG_FORBIDDEN_PATTERN = Pattern.compile("<(area|base|basefont|br|col|frame|hr|img|input|isindex|link|meta|param)([ \\t\\n\\x0B\\f\\r])([^>]*)/>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static String obeyForbiddenEndTags(String html) {
        Matcher m = END_TAG_FORBIDDEN_PATTERN.matcher(html);
        if (false == m.find()) {
            return html;
        }
        StringBuffer sb = new StringBuffer(html.length());
        StringBuilder tmp = new StringBuilder(64);
        do {
            tmp.setLength(0);
            tmp.append('<').append(m.group(1));
            String appendix = m.group(3);
            if (Strings.isNotEmpty(appendix)) {
                tmp.append(m.group(2)).append(appendix);
            }
            tmp.append('>');
            m.appendReplacement(sb, Matcher.quoteReplacement(tmp.toString()));
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    protected static String validateWithHtmlCleaner(final String htmlContent) {
        try {
            /*-
             * http://stackoverflow.com/questions/238036/java-html-parsing
             *
             * Clean...
             */
            boolean preprocessWithJSoup = false;
            String preprocessed = preprocessWithJSoup ? preprocessWithJSoup(htmlContent) : htmlContent;
            preprocessed = replaceSpecialEntities(preprocessed);
            final TagNode htmlNode = newHtmlCleaner().clean(preprocessed);
            if (null == htmlNode) {
                LOG.warn("HtmlCleaner library failed to pretty-print HTML content");
                return htmlContent;
            }

            // Serialize
            final UnsynchronizedStringWriter writer = new UnsynchronizedStringWriter(htmlContent.length());
            newSerializer().write(htmlNode, writer, "UTF-8");
            final StringBuilder buffer = writer.getBuffer();

            // Insert DOCTYPE if absent
            if (buffer.indexOf("<!DOCTYPE") < 0) {
                buffer.insert(0, DOCTYPE_DECL);
            }

            // Keep Unicode representation of 'copy' and 'reg' intact
            String result = keepUnicodeForEntities(buffer.toString());

            // Obey forbidden end tags
            result = obeyForbiddenEndTags(result);
            return result;
        } catch (UnsupportedEncodingException e) {
            // Cannot occur
            LOG.error("HtmlCleaner library failed to pretty-print HTML content with an unsupported encoding", e);
            return htmlContent;
        } catch (IOException e) {
            // Cannot occur
            LOG.error("HtmlCleaner library failed to pretty-print HTML content with I/O error", e);
            return htmlContent;
        } catch (RuntimeException rte) {
            /*
             * HtmlCleaner failed horribly...
             */
            LOG.warn("HtmlCleaner library failed to pretty-print HTML content", rte);
            return htmlContent;
        }
    }

    /**
     * Writes given string to specified file
     *
     * @param str The string to write
     * @param file The file to write to
     */
    protected static void writeTo(String str, String file) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, false);
            fos.write(str.getBytes(Charsets.UTF_8));
            fos.flush();
        } catch (Exception x) {
            // Ignore
        } finally {
            Streams.close(fos);
        }
    }

    private static boolean startsWith(String prefix, String toCheck, boolean ignoreHeadingWhitespaces) {
        if (null == toCheck) {
            return false;
        }

        int len = toCheck.length();
        if (len <= 0) {
            return false;
        }

        if (!ignoreHeadingWhitespaces) {
            return toCheck.startsWith(prefix);
        }

        int i = 0;
        while (i < len && Strings.isWhitespace(toCheck.charAt(i))) {
            i++;
        }
        if (i >= len) {
            return false;
        }
        return toCheck.startsWith(prefix, i);
    }

}
