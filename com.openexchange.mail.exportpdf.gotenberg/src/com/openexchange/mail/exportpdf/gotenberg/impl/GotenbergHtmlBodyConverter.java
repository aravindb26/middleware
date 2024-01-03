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

package com.openexchange.mail.exportpdf.gotenberg.impl;

import static com.openexchange.java.Autoboxing.F;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.openexchange.exception.OXException;
import com.openexchange.gotenberg.client.GotenbergClient.HTMLDocument;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.mail.exportpdf.MailExportAttachmentInformation;
import com.openexchange.mail.exportpdf.MailExportBodyInformation;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.converter.EmptyMailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverter;
import com.openexchange.mail.exportpdf.converters.MailExportConverterUtil;
import com.openexchange.mail.exportpdf.converters.images.InlineImageUtils;

/**
 * {@link GotenbergHtmlBodyConverter} - converts a HTML email body using "Gotenberg"
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class GotenbergHtmlBodyConverter implements MailExportHtmlBodyConverter {

    /* The HTML content type */
    private static final String HTML_CONTENT_TYPE = "text/html";

    /* The HTML file extension */
    private static final String HTML_FILE_EXTENSION = "html";

    /* The template for injection header space */
    private static final String HTML_HEADER_SPACE_TEMPLATE = """
               <div style="background-color: white !important; height: %smm !important; !important; width: 100%% !important;">
                   <!-- A mail export header placeholder -->
               </div>
        """;

    /* The pattern for matching a HTML document's starting body tag */
    private static final Pattern HTML_BODY_PATTERN = Pattern.compile("<body[^>]*+>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    //---------------------------------------------------------------------------------------------------------------------------------------

    private final GotenbergMailExportConverter converter;

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link GotenbergHtmlBodyConverter}.
     *
     * @param converter The {@link GotenbergHtmlBodyConverter} to use for conversion
     */
    public GotenbergHtmlBodyConverter(GotenbergMailExportConverter converter) {
        this.converter = Objects.requireNonNull(converter, "converter must not be null");
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Injects space for the mail headers into the given HTML
     *
     * @param contentBody The HTML content to injection the space into
     * @param options The options defining the amount of space to inject
     * @return The HTML content with the space injected
     */
    private String injectMailHeaderSpace(String contentBody, MailExportConverterOptions options) {
        Matcher matcher = HTML_BODY_PATTERN.matcher(contentBody);
        if (!matcher.find()) {
            return contentBody;
        }
        String body = matcher.group();
        String newBody = body + HTML_HEADER_SPACE_TEMPLATE.formatted(F(options.getTopMarginOffset()).toString());
        return matcher.replaceFirst(newBody);
    }

    /**
     * Gets the HTML body from the {@link MailExportBodyInformation}
     *
     * @param mailBody The HTML body to convert
     * @param options The {@link MailExportConverterOptions} to apply
     * @return The HTML body to convert
     */
    private String getHTMLBody(MailExportBodyInformation mailBody, MailExportConverterOptions options) {
        //inject space for the mail headers as requested by the given options
        String html = injectMailHeaderSpace(mailBody.getBody(), options);

        /*
         * Replace the inline image CID-URLS with unique file names:
         *
         * The inline image sources in the email's HTML body are defined as "cid"-URLs referencing the attachment by CID.
         * Gotenberg would treat the CID-URL-schema as an unresolvable URL, thus we need to resolve these URLs to
         * relative filenames so that Gotenberg is able to resolve the images by their names as included in the conversion request.
         *
         * i.e. The "cid:"-schema needs to be removed.
         * e.g.: <img src="cid:part1.Rye9jGtH.NyrGlVaf@context1.ox.test">
         * becomes
         * <img src="part1.Rye9jGtH.NyrGlVaf@context1.ox.test">
         */
        Matcher matcher = InlineImageUtils.CID_REGEX.matcher(html);
        return matcher.replaceAll(match -> "src=\"%s\"".formatted(match.group(1) /* cid */));
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public MailExportConversionResult convertHtmlBody(MailExportBodyInformation mailBody, MailExportConverterOptions options) throws OXException {
        /* Check if the converter is enabled */
        if (!converter.isEnabled(options.getSession())) {
            return new EmptyMailExportConversionResult(Status.DISABLED);
        }
        /* Check if the converter is configured to support HTML */
        if (!MailExportConverterUtil.handlesContentType(HTML_CONTENT_TYPE, converter.getEnabledFileExtensions(options.getSession()))
            && !MailExportConverterUtil.handlesFileExtension(HTML_FILE_EXTENSION, converter.getEnabledFileExtensions(options.getSession()))) {
            return new EmptyMailExportConversionResult(Status.TYPE_NOT_SUPPORTED);
        }

        /* Get inline images, if any */
        List<MailExportAttachmentInformation> inlineImageAttachments = mailBody.getInlineImages();
        List<com.openexchange.gotenberg.client.GotenbergClient.Document> inlineImages = new ArrayList<>(inlineImageAttachments.size());
        for (var image : inlineImageAttachments) {
            inlineImages.add(new GotenbergInlineImageDocument(image.getMailPart()));
        }

        String htmlBody = getHTMLBody(mailBody, options);
        HTMLDocument html = () -> Streams.newByteArrayInputStream(htmlBody.getBytes(Charsets.UTF_8));
        return converter.convertHTML(options, html, inlineImages);
    }
}
