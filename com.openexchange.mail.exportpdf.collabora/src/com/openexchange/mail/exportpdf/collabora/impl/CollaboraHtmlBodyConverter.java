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

package com.openexchange.mail.exportpdf.collabora.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.DistributedFileManagement;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.java.Streams;
import com.openexchange.mail.exportpdf.MailExportAttachmentInformation;
import com.openexchange.mail.exportpdf.MailExportBodyInformation;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.converter.EmptyMailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverter;
import com.openexchange.mail.exportpdf.converter.ReplacingInputStream;
import com.openexchange.mail.exportpdf.converter.ReplacingInputStream.Replacement;
import com.openexchange.mail.exportpdf.converters.MailExportConverterUtil;
import com.openexchange.mail.exportpdf.converters.images.InlineImageUtils;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link CollaboraHtmlBodyConverter} - converts a HTML email body using "Collabora"
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CollaboraHtmlBodyConverter implements MailExportHtmlBodyConverter {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboraHtmlBodyConverter.class);

    private static final String HTML_CONTENT_TYPE = "text/html";

    private static final String HTML_FILE_EXTENSION = "html";

    private static final Pattern HTML_BODY_PATTERN = Pattern.compile("<body[^>]*+>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    final CollaboraMailExportConverter converter;

    private final ServiceLookup serviceLookup;

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link CollaboraHtmlBodyConverter}.
     *
     * @param converter The {@link CollaboraHtmlBodyConverter} to use for conversion
     * @param serviceLookup The {@link ServiceLookup} to use
     */
    public CollaboraHtmlBodyConverter(CollaboraMailExportConverter converter, ServiceLookup serviceLookup) {
        this.converter = Objects.requireNonNull(converter, "converter must not be null");
        this.serviceLookup = Objects.requireNonNull(serviceLookup, "serviceLookup must not be null");
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Internal method to get the HTML Body, as is, ready to be converted
     *
     * @param mailBody The mail HTML body
     * @param options The options to apply
     * @return A Stream to the HTML content with the CID references resolved
     */
    private InputStream getHtmlBody(MailExportBodyInformation mailBody, MailExportConverterOptions options) {
        return new ByteArrayInputStream(injectHeaderSpace(mailBody.getBody(), options.getMailHeaderLines()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Internal method to get the HTML Body ready to be converted
     * <p>
     * The CID references of the inline images will be replaced with the given {@link Replacement}
     * </p>
     *
     * @param mailBody The mail HTML body
     * @param options the options to apply
     * @param cidReplacement The replacement to apply for inline images
     * @return A Stream to the HTML content with the CID references resolved
     */
    private InputStream getHtmlBody(MailExportBodyInformation mailBody, MailExportConverterOptions options, Replacement cidReplacement) {
        return new ReplacingInputStream(injectHeaderSpace(mailBody.getBody(), options.getMailHeaderLines()), StandardCharsets.UTF_8, InlineImageUtils.CID_REGEX, cidReplacement);
    }

    /**
     * Internal method to get the {@link LeanConfigurationService}
     *
     * @return The {@link LeanConfigurationService}
     * @throws OXException if an error is occurred
     */
    private LeanConfigurationService getLeanConfigurationService() throws OXException {
        return serviceLookup.getServiceSafe(LeanConfigurationService.class);
    }

    /**
     * Applies a space for the header as requested
     *
     * @param contentBody The HTML content to apply the headers to
     * @param amountOfLines The amount of lines to insert for the header space
     * @return The contentBody with the space applied
     */
    private String injectHeaderSpace(String contentBody, int amountOfLines) {
        Matcher matcher = HTML_BODY_PATTERN.matcher(contentBody);
        if (matcher.find()) {
            String body = matcher.group();
            String newBody = body + "<p style=\"background-color: white; max-width=100%\">" + "<br>".repeat(amountOfLines) + "</p>";
            return matcher.replaceFirst(newBody);
        }
        return contentBody;
    }

    /**
     * Internal method to get the {@link Replacement} which will replace the inline image references with actual real content
     *
     * @param session The session
     * @param inlineImages a list with all in-line images
     * @return The {@link Replacement} to use for replacing the inline image references with actual real image content
     * @throws OXException if an error is occurred
     */
    private Replacement getReplacement(Session session, List<InlineImage> inlineImages) throws OXException {
        final ImageReplacementMode replacementMode = ImageReplacementMode.parse(getLeanConfigurationService().getProperty(session.getUserId(), session.getContextId(), CollaboraProperties.IMAGE_REPLACEMENT_MODE));
        switch (replacementMode) {
            case DISTRIBUTED_FILE -> {
                /* just assert that the DistributedFileManagement service is available */
                serviceLookup.getServiceSafe(DistributedFileManagement.class);
                // @formatter:off
                return new DistributedFileImageReplacement(serviceLookup.getServiceSafe(ConfigurationService.class),
                                                           serviceLookup.getServiceSafe(ManagedFileManagement.class))
                           .publishImages(inlineImages);
                // @formatter:on
            }
            case BASE64 -> {
                return new Base64InlineImageReplacement(inlineImages);
            }
            default -> {
                LOG.error("Unknown ImageReplacement mode: {}", replacementMode);
                return ReplacingInputStream.NO_REPLACE;
            }
        }
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

        InputStream htmlBody = null;
        List<InlineImage> inlineImages = new ArrayList<>(mailBody.getInlineImages().size());
        try {
            /* Get inline images, if any */
            for (MailExportAttachmentInformation inlineImageAttachment : mailBody.getInlineImages()) {
                var inlineImage = new InlineImage(inlineImageAttachment);
                inlineImages.add(inlineImage);
            }

            if (inlineImages.isEmpty()) {
                /* we do not have known inline images: Convert the body "as is" */
                htmlBody = getHtmlBody(mailBody, options);
            } else {
                /* inline images will be resolved/replaced with the Replacement strategy configured */
                htmlBody = getHtmlBody(mailBody, options, getReplacement(options.getSession(), inlineImages));
            }

            /* Convert to HTML */
            return converter.convertDocument(htmlBody, HTML_CONTENT_TYPE, options);
        } finally {
            Streams.close(htmlBody);
            Streams.closeAutoCloseables(inlineImages);
        }
    }
}
