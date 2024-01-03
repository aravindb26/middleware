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
T* Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.mail.exportpdf.impl.pdf;

import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.html.HtmlSanitizeOptions;
import com.openexchange.html.HtmlService;
import com.openexchange.i18n.Translator;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.exportpdf.MailExportAttachmentInformation;
import com.openexchange.mail.exportpdf.MailExportContentInformation;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.mail.exportpdf.MailExportProperty;
import com.openexchange.mail.exportpdf.converter.DefaultMailExportConverterOptions;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.MailExportConverters;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverters;
import com.openexchange.mail.exportpdf.converters.header.MailHeader;
import com.openexchange.mail.exportpdf.converters.header.MailHeaderStrings;
import com.openexchange.mail.exportpdf.impl.MailExportConverterRegistry;
import com.openexchange.mail.exportpdf.impl.pdf.attachment.AttachmentWriter;
import com.openexchange.mail.exportpdf.impl.pdf.attachment.ImageAttachmentWriter;
import com.openexchange.mail.exportpdf.impl.pdf.attachment.PDFAttachmentWriter;
import com.openexchange.mail.exportpdf.impl.pdf.attachment.TextAttachmentWriter;
import com.openexchange.mail.exportpdf.impl.pdf.dao.PDFAttachmentMetadata;
import com.openexchange.mail.exportpdf.impl.pdf.dao.PDFMailExportBodyInformation;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFElementRenderer;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFFactory;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFFontUtils;
import com.openexchange.mail.exportpdf.impl.pdf.fonts.FontSet;
import com.openexchange.mail.exportpdf.impl.pdf.renderer.CalculatingContentRenderer;
import com.openexchange.mail.exportpdf.impl.pdf.renderer.RichTextContentRenderer;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.server.ServiceLookup;
import com.openexchange.serverconfig.ServerConfig;
import com.openexchange.serverconfig.ServerConfigService;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSessionAdapter;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.pdfbox.layout.elements.Document;
import rst.pdfbox.layout.elements.PageFormat;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.elements.render.RenderContext;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;

/**
 * {@link PDFInputStreamConverter} - Creates a PDF out of an e-mail
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class PDFInputStreamConverter implements AutoCloseable {

    static final Logger LOG = LoggerFactory.getLogger(PDFInputStreamConverter.class);

    private static final String DEFAULT_TITLE = "export";

    private final List<AttachmentWriter> attachmentWriters;

    private final ServiceLookup services;
    private final ServerConfigService serverConfigService;
    private final LeanConfigurationService leanConfigService;
    private final MailExportConverterRegistry registry;

    private final DefaultMailExportConverterOptions converterOptions;
    private final Session session;
    private final MailExportOptions options;
    private final Translator translator;

    private final List<OXException> warnings;
    private final LinkedList<AutoCloseable> closeables;
    private final PageFormat pageFormat;
    private FontSet font;
    private FontSet alternativeFont;

    /**
     * Initialises a new {@link PDFInputStreamConverter}.
     *
     * @param services The service lookup reference to use
     * @param serverConfigService The server config service
     * @param leanConfigService The lean configuration service
     * @param translatorFactory The translator factory
     * @param registry The mail export converter registry
     * @param session The session
     * @param options The mail export options
     * @throws OXException if the page format cannot be initialised
     */
    public PDFInputStreamConverter(ServiceLookup services, ServerConfigService serverConfigService, LeanConfigurationService leanConfigService, TranslatorFactory translatorFactory, MailExportConverterRegistry registry, Session session, MailExportOptions options) throws OXException {
        super();
        this.services = services;
        this.serverConfigService = serverConfigService;
        this.leanConfigService = leanConfigService;
        this.registry = registry;
        this.session = session;
        this.options = options;

        this.warnings = new LinkedList<>();
        this.closeables = new LinkedList<>();

        this.pageFormat = PDFFactory.createPageFormat(session, leanConfigService, options.getPageFormat());
        this.attachmentWriters = initAttachmentWriters();
        this.converterOptions = initConverterOptions();

        this.translator = translatorFactory.translatorFor(ServerSessionAdapter.valueOf(session).getUser().getLocale());
    }

    /**
     * Initialises the internal attachment writers
     *
     * @return a list with the internal attachment writers
     */
    private List<AttachmentWriter> initAttachmentWriters() {
        //@formatter:off
        return List.of(
            new ImageAttachmentWriter(session, leanConfigService, pageFormat),
            new PDFAttachmentWriter(session, leanConfigService),
            new TextAttachmentWriter(session, leanConfigService));
        //@formatter:on
    }

    /**
     * Initialises the converter options
     *
     * @return The initialised converter options
     */
    private DefaultMailExportConverterOptions initConverterOptions() {
        DefaultMailExportConverterOptions tmpOptions = new DefaultMailExportConverterOptions();
        tmpOptions.setSession(session);
        tmpOptions.setWidth(PDFFactory.unitsToMillimeter(pageFormat.getMediaBox().getWidth()));
        tmpOptions.setHeight(PDFFactory.unitsToMillimeter(pageFormat.getMediaBox().getHeight()));
        tmpOptions.setTopMargin(PDFFactory.unitsToMillimeter(pageFormat.getMarginTop()));
        tmpOptions.setLeftMargin(PDFFactory.unitsToMillimeter(pageFormat.getMarginLeft()));
        tmpOptions.setRightMargin(PDFFactory.unitsToMillimeter(pageFormat.getMarginRight()));
        tmpOptions.setBottomMargin(PDFFactory.unitsToMillimeter(pageFormat.getMarginBottom()));
        return tmpOptions;
    }

    /**
     * Converts the mail message and its contents to a {@link PDFExportResult}
     *
     * @param mailAccess The mail access
     * @return The result
     * @throws OXException if an error is occurred
     */
    public PDFExportResult convert(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException {
        try {
            MailExportContentInformation mailInfo = getMailInfo(mailAccess);
            RenderContext renderContext = writeHeadersAndBody(mailInfo);
            writeAttachments(mailInfo, renderContext);
            writePDFAMetadata(renderContext, mailInfo);

            return getResult(saveDocument(renderContext), mailInfo);
        } catch (IOException e) {
            throw MailExportExceptionCode.IO_ERROR.create(e.getMessage(), e);
        }
    }

    /**
     * Writes the headers and the body
     *
     * @param mailInfo The mail information
     * @return The render context
     * @throws IOException if an I/O error is occurred
     * @throws OXException if any other error is occurred
     */
    private RenderContext writeHeadersAndBody(MailExportContentInformation mailInfo) throws OXException, IOException {
        if ((!options.preferRichText() && mailInfo.getTextBody() != null) || /* plain text is preferred */
            (mailInfo.getBody() == null && mailInfo.getTextBody() != null)) { /* only plain text is available */
            RenderContext renderContext = createRenderContext();
            writeHeadersAndAttachmentMetadata(mailInfo, renderContext);
            FontSet fontSet = getAlternativeFont(renderContext.getPdDocument());
            PDFElementRenderer.renderText(mailInfo.getTextBody(), renderContext, fontSet.getPlainFont(), getIntProperty(MailExportProperty.bodyFontSize));
            return renderContext;
        }

        calculateOffsets(mailInfo);
        if (null == mailInfo.getBody()) {
            RenderContext renderContext = createRenderContext();
            writeHeadersAndAttachmentMetadata(mailInfo, renderContext);
            return renderContext;
        }
        MailExportConversionResult result = convertHtmlBody(mailInfo);
        if (result.getWarnings() != null) {
            warnings.addAll(result.getWarnings());

        }
        try (InputStream stream = result.getInputStream()) {
            if (null == stream) {
                LOG.debug("No stream found for mail body");
                RenderContext renderContext = createRenderContext();
                writeHeadersAndAttachmentMetadata(mailInfo, renderContext);
                return renderContext;
            }

            PDDocument pdf = PDFFactory.createDocument(stream);
            closeables.addFirst(pdf);

            if (pdf.getNumberOfPages() == 0) {
                RenderContext renderContext = createRenderContext();
                writeHeadersAndAttachmentMetadata(mailInfo, renderContext);
                return renderContext;
            }
            return writeHeadersAndAttachmentMetadata(mailInfo, pdf);
        }
    }

    /**
     * Creates a new PDF document and a new render context. Bundles everything under {@link RenderContext}
     *
     * @return The new {@link RenderContext}
     * @throws IOException if an I/O error is occurred
     */
    private RenderContext createRenderContext() throws IOException {
        PDDocument pdDocument = new PDDocument(MemoryUsageSetting.setupMixed(ThresholdFileHolder.DEFAULT_IN_MEMORY_THRESHOLD));
        closeables.addFirst(pdDocument);
        return createRenderContext(pdDocument);
    }

    /**
     * Creates a render context with the specified options and document
     *
     * @param document The document
     * @return The render context
     * @throws IOException if an I/O error is occurred
     */
    private RenderContext createRenderContext(PDDocument document) throws IOException {
        RenderContext renderContext = new RenderContext(new Document(pageFormat), document);
        closeables.addFirst(renderContext);
        return renderContext;
    }

    /**
     * Gets the value of a {@link MailHeader}
     *
     * @param mailHeader The {@link MailHeader} to get the value for
     * @param mailInfo The related {@link MailExportContentInformation} to extract the mail header from
     * @return The value of the given header
     * @throws OXException if an error is occurred
     */
    private String getMailHeaderValue(MailHeader mailHeader, MailExportContentInformation mailInfo) throws OXException {
        return mailHeader.getFormatter().orElse((ses, serv, val) -> val.getHeaders().get(mailHeader.getMailHeaderName())).format(ServerSessionAdapter.valueOf(session), services, mailInfo);
    }

    /**
     * Creates a temporary {@link PDDocument} and calculates the offsets for the headers
     *
     * @param mailInfo the mail info
     * @throws OXException if an error is occurred
     * @throws IOException if an I/O error is occurred
     */
    private void calculateOffsets(MailExportContentInformation mailInfo) throws OXException, IOException {
        int fontSize = getIntProperty(MailExportProperty.headersFontSize);
        PDDocument document = new PDDocument();
        closeables.addFirst(document);
        FontSet fontSet = getFont(document);
        List<PDFAttachmentMetadata> attachments = collectAttachmentMetadata(mailInfo);
        CalculatingContentRenderer renderer = new CalculatingContentRenderer(translator, converterOptions);
        renderer.setFontSize(fontSize);
        renderer.setMaxWidth(determineParagraphMaxWidth(mailInfo, attachments, fontSize, fontSet));
        renderer.render(document, fontSet, pageFormat, attachments, header -> getMailHeaderValue(header, mailInfo));

        //Taking over the calculated result offsets after "rendering"
        converterOptions.setMailHeaderLines(renderer.getMailHeaderLines());
        converterOptions.setTopMarginOffset(renderer.getTopMarginOffset());
        converterOptions.setHeaderPageCount(renderer.getHeaderPageCount());
    }

    /**
     * Determines the paragraph max width for the headers
     *
     * @param mailInfo The mail info
     * @param attachmentMetadata The attachment data
     * @param fontSize The font size
     * @param fontSet The font set
     * @return The maximum width
     * @throws OXException if an error is occurred
     * @throws IOException if an I/O error is occurred
     */
    private float determineParagraphMaxWidth(MailExportContentInformation mailInfo, List<PDFAttachmentMetadata> attachmentMetadata, int fontSize, FontSet fontSet) throws OXException, IOException {
        float maxWidth = 0;
        if (handleAttachments(options) && mailInfo.getAttachmentInformation().isEmpty()) {
            maxWidth = PDFElementRenderer.getTextWidth(attachmentMetadata.size() + " " + translator.translate(MailHeaderStrings.ATTACHMENTS), fontSize, fontSet.getPlainFont());
        }

        for (MailHeader mailHeader : MailHeader.values()) {
            String value = getMailHeaderValue(mailHeader, mailInfo);
            if (Strings.isEmpty(value)) {
                continue;
            }
            maxWidth = Math.max(PDFElementRenderer.getTextWidth(translator.translate(mailHeader.getDisplayName()), fontSize, fontSet.getPlainFont()), maxWidth);
        }
        return maxWidth;
    }

    /**
     * Writes the headers and the attachment metadata
     *
     * @param mailInfo The mail information
     * @param mailBody The mail body as a PDF document
     * @return renderContext The render context
     * @throws IOException if an I/O error is occurred
     * @throws OXException if any other error is occurred
     */
    private RenderContext writeHeadersAndAttachmentMetadata(MailExportContentInformation mailInfo, PDDocument mailBody) throws OXException, IOException {
        PDDocument document = new PDDocument();
        closeables.addFirst(document);
        int fontSize = getIntProperty(MailExportProperty.headersFontSize);
        FontSet fontSet = getFont(document);
        List<PDFAttachmentMetadata> attachments = collectAttachmentMetadata(mailInfo);

        RichTextContentRenderer renderer = new RichTextContentRenderer(translator, converterOptions, mailBody);
        renderer.setFontSize(fontSize);
        renderer.setMaxWidth(determineParagraphMaxWidth(mailInfo, attachments, fontSize, fontSet));
        renderer.render(document, fontSet, pageFormat, attachments, header -> getMailHeaderValue(header, mailInfo));
        return renderer.getRenderContext();
    }

    /**
     * Writes the headers and the attachment metadata
     *
     * @param mailInfo The mail information
     * @param renderContext The render context
     * @throws OXException if an error is occurred
     * @throws IOException if an I/O error is occurred
     */
    private void writeHeadersAndAttachmentMetadata(MailExportContentInformation mailInfo, RenderContext renderContext) throws OXException, IOException {
        FontSet fontSet = getFont(renderContext.getPdDocument());
        int fontSize = getIntProperty(MailExportProperty.headersFontSize);
        float leftMargin = pageFormat.getMarginLeft();
        float rightMargin = pageFormat.getMarginRight();
        PDRectangle rect = renderContext.getCurrentPage().getMediaBox();
        List<PDFAttachmentMetadata> attachmentMetadata = collectAttachmentMetadata(mailInfo);
        float maxWidth = determineParagraphMaxWidth(mailInfo, attachmentMetadata, fontSize, fontSet);
        float spaceWidth = PDFElementRenderer.getTextWidth(" ", fontSize, fontSet.getPlainFont());
        float indentationSpace = maxWidth + (PDFFormatConstants.EXTRA_SPACE * spaceWidth);
        // Write headers
        float paragraphMaxWidth = rect.getWidth() - leftMargin - rightMargin;
        for (MailHeader mailHeader : MailHeader.values()) {
            String value = getMailHeaderValue(mailHeader, mailInfo);
            if (Strings.isEmpty(value)) {
                continue;
            }
            Paragraph paragraph = PDFFactory.createParagraph(paragraphMaxWidth);
            paragraph.add(PDFFactory.createIndentation(translator.translate(mailHeader.getDisplayName()) + ":", indentationSpace, fontSize, fontSet.getBoldFont()));
            renderHeaderSafe(renderContext, paragraph, value, fontSize, fontSet);
        }

        // Write attachment metadata
        if (attachmentMetadata.isEmpty()) {
            PDFElementRenderer.renderElement(renderContext, PDFFactory.createNewLine(fontSize, fontSet));
            PDFElementRenderer.renderElement(renderContext, PDFFactory.createHorizontalLine());
            return;
        }
        Paragraph paragraph = PDFFactory.createParagraph(paragraphMaxWidth);
        paragraph.add(PDFFactory.createIndentation(attachmentMetadata.size() + " " + translator.translate(MailHeaderStrings.ATTACHMENTS) + ":", indentationSpace, fontSize, fontSet.getBoldFont()));
        renderAttachmentMetadataSafe(renderContext, paragraph, null, attachmentMetadata, fontSize, fontSet, 2);

        PDFElementRenderer.renderElement(renderContext, PDFFactory.createHorizontalLine());

    }

    /**
     *
     * Renders the corresponding element and handles the case of unknown glyphs by replacing them.
     *
     * @param renderContext The render context
     * @param paragraph The paragraph
     * @param value The value
     * @param fontSize The font size
     * @param fontSet The font set
     * @throws IOException if an I/O error is occurred
     */
    private void renderHeaderSafe(RenderContext renderContext, Paragraph paragraph, String value, int fontSize, FontSet fontSet) throws IOException {
        paragraph.addText(value, fontSize, fontSet.getPlainFont());
        try {
            PDFElementRenderer.renderElement(renderContext, paragraph);
        } catch (IllegalArgumentException e) {
            String fixedText = PDFElementRenderer.replaceUnknownCharacter(e, value);
            if (fixedText.equals(value)) {
                throw e;
            }
            paragraph.removeLast();
            renderHeaderSafe(renderContext, paragraph, fixedText, fontSize, fontSet);
        }

    }

    /**
     * @param renderContext The render context
     * @param paragraph The paragraph
     * @param values The values
     * @param attachmentMetadata The attachment metadata
     * @param fontSize The font size
     * @param fontSet The font set
     * @param overHead defines how many of the entries per metadata entries of the text array must be deleted again in case of an error. addText() adds different numbers of entries to the text array of the paragraph.
     * @throws IOException if an I/O error is occurred
     */
    private void renderAttachmentMetadataSafe(RenderContext renderContext, Paragraph paragraph, List<String> values, List<PDFAttachmentMetadata> attachmentMetadata, int fontSize, FontSet fontSet, int overHead) throws IOException {
        List<String> displayTexts = new LinkedList<>();
        if (values == null) {
            for (PDFAttachmentMetadata m : attachmentMetadata) {
                String text = m.displayText().substring(m.displayText().indexOf(":") + 1) + "\n";
                paragraph.addText(text, fontSize, fontSet.getPlainFont());
                displayTexts.add(text);
            }
        } else {
            for (String s : values) {
                paragraph.addText(s, fontSize, fontSet.getPlainFont());
                displayTexts.add(s);
            }
        }
        try {
            PDFElementRenderer.renderElement(renderContext, paragraph);
        } catch (IllegalArgumentException e) {
            List<String> fixedTexts = new LinkedList<>();
            boolean somethingChanged = false;
            for (String text : displayTexts) {
                String fixedText = PDFElementRenderer.replaceUnknownCharacter(e, text);
                fixedTexts.add(fixedText);
                if (!fixedText.equals(text)) {
                    somethingChanged = true;
                }
            }
            if (somethingChanged) {
                for (int i = 0; i < fixedTexts.size() * overHead; i++) {
                    paragraph.removeLast();
                }
                renderAttachmentMetadataSafe(renderContext, paragraph, fixedTexts, attachmentMetadata, fontSize, fontSet, 2);
            } else {
                throw e;
            }
        }

    }

    /**
     * Writes the attachments to the PDF document
     *
     * @param mailInfo The mail info
     * @param renderContext the render context
     * @throws OXException if an error is occurred
     * @throws IOException if an I/O error is occurred
     */
    private void writeAttachments(MailExportContentInformation mailInfo, RenderContext renderContext) throws OXException, IOException {
        // Trigger conversions of attachment previews as needed
        List<MailExportConversionResult> previewResults = new LinkedList<>();
        if (generatePreviews()) {
            for (MailExportAttachmentInformation attachmentInfo : mailInfo.getAttachmentInformation()) {
                MailExportConversionResult previewResult = convertPreviews(attachmentInfo);
                if (previewResult.getWarnings() != null) {
                    warnings.addAll(previewResult.getWarnings());
                }
                closeables.addFirst(previewResult);
                previewResults.add(previewResult);
            }
        }

        if (options.appendAttachmentPreviews()) {
            for (MailExportConversionResult result : previewResults) {
                attachmentWriters.forEach(c -> {
                    try {
                        if (c.handles(result) && result.getStatus().equals(Status.SUCCESS)) {
                            c.writeAttachment(result, options, renderContext);
                        }
                    } catch (OXException e) {
                        LOG.debug("", e);
                    }
                });
            }
        }

        Map<String, PDComplexFileSpecification> attachments = new HashMap<>();
        if (options.embedAttachmentPreviews()) {
            for (MailExportConversionResult result : previewResults) {
                PDComplexFileSpecification embedAttachment = embedAttachment(result, renderContext.getPdDocument(), PDFAFRelationship.Unspecified);
                if (embedAttachment != null && result.getStatus().equals(Status.SUCCESS)) {
                    attachments.put(UUID.randomUUID().toString(), embedAttachment);
                }
            }
        }
        if (options.embedRawAttachments()) {
            for (MailExportAttachmentInformation attInfo : mailInfo.getAttachmentInformation()) {
                attachments.put(attInfo.getFileName(), embedAttachment(attInfo, renderContext.getPdDocument(), PDFAFRelationship.Unspecified));
            }
        }
        if (options.embedNonConvertibleAttachments() && !options.appendAttachmentPreviews()) {
            MailExportConverters converters = registry.getConverters();
            for (MailExportAttachmentInformation attInfo : mailInfo.getAttachmentInformation()) {
                if (!converters.handles(attInfo.getMailPart(), converterOptions)) {
                    attachments.put(attInfo.getFileName(), embedAttachment(attInfo, renderContext.getPdDocument(), PDFAFRelationship.Unspecified));
                }
            }
            for (MailExportConversionResult result : previewResults) {
                if (result.getStatus().equals(Status.ATTACHMENT_CORRUPT)) {
                    attachments.put(result.getTitle().orElse(UUID.randomUUID().toString()), embedAttachment(result, renderContext.getPdDocument(), PDFAFRelationship.Unspecified));
                }
            }
        }

        if (!attachments.isEmpty()) {
            embedAttachments(renderContext, attachments);
        }
    }

    /**
     * Converts the specified attachment
     *
     * @param attachmentInfo The attachment info
     * @return The conversion result
     * @throws OXException if an error is occurred
     */
    private MailExportConversionResult convertPreviews(MailExportAttachmentInformation attachmentInfo) throws OXException {
        MailExportConverters converters = registry.getConverters();
        return converters.convert(attachmentInfo.getMailPart(), converterOptions);
    }

    /**
     * Converts the HTML body
     *
     * @param mailInfo The body info
     * @return The conversion result
     * @throws OXException if an error is occurred
     */
    private MailExportConversionResult convertHtmlBody(MailExportContentInformation mailInfo) throws OXException {
        MailExportHtmlBodyConverters bodyConverters = registry.getBodyConverters();
        String htmlContent = mailInfo.getBody();
        Boolean includeExternal = options.includeExternalImages();
        if (includeExternal != null) {
            HtmlSanitizeOptions.Builder optionsBuilder = HtmlSanitizeOptions.builder().setSession(session);
            HtmlSanitizeOptions sanitiseOptions = optionsBuilder.setDropExternalImages(!b(includeExternal)).build();
            HtmlService service = services.getService(HtmlService.class);
            htmlContent = service.sanitize(htmlContent, sanitiseOptions).getContent();
        }
        var bodyInfo = new PDFMailExportBodyInformation().setRichTextBody(htmlContent).addImages(mailInfo.getInlineImages());
        return bodyConverters.convertHtmlBody(bodyInfo, converterOptions);
    }

    /**
     * Retrieves the mail message
     *
     * @param mailAccess The mail access
     * @return The mail message
     * @throws OXException if no mail was found
     */
    private MailMessage getMailMessage(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException {
        MailMessage mailMessage = mailAccess.getMessageStorage().getMessage(options.getMailFolderId(), options.getMailId(), false);
        if (mailMessage == null) {
            throw MailExceptionCode.MAIL_NOT_FOUND.create(options.getMailId(), options.getMailFolderId());
        }
        return mailMessage;
    }

    /**
     * Collects the attachment metadata for later processing
     *
     * @param contentHolder The content holder with the attachments
     * @return a list with the attachment metadata
     * @throws OXException if an error is occurred
     */
    private List<PDFAttachmentMetadata> collectAttachmentMetadata(MailExportContentInformation contentHolder) throws OXException {
        if (!handleAttachments(options) && contentHolder.getAttachmentInformation().isEmpty()) {
            return Collections.emptyList();
        }
        List<PDFAttachmentMetadata> map = new LinkedList<>();
        collectAttachmentMetadata(contentHolder.getAttachmentInformation(), map, PDFAttachmentTag.APPENDED_PREVIEW);
        //        collectAttachmentMetadata(contentHolder.getEmbedAttachments(), map, PDFAttachmentTag.EMBEDDED_PREVIEW);
        //        collectAttachmentMetadata(contentHolder.getRawAttachments(), map, PDFAttachmentTag.EMBEDDED_RAW);
        //        collectAttachmentMetadata(contentHolder.getNonConvertibleAttachments(), map, PDFAttachmentTag.EMBEDDED_NON_CONVERTIBLE);
        return map;
    }

    /**
     * Collects the attachment metadata from the specified results
     *
     * @param attachmentInformation The attachment information
     * @param collector The collector map
     * @param tag The tag for appended or embedded attachments
     * @throws OXException if an error is occurred
     */
    private void collectAttachmentMetadata(List<MailExportAttachmentInformation> attachmentInformation, List<PDFAttachmentMetadata> collector, PDFAttachmentTag tag) throws OXException {
        for (MailExportAttachmentInformation info : attachmentInformation) {
            String title = tag + ":" + (info.getFileName().isEmpty() ? "Attachment-" + UUID.randomUUID() : info.getFileName());
            String tmpText = title + " (" + Strings.humanReadableByteCount(info.getMailPart().getSize(), true) + ")";
            collector.add(new PDFAttachmentMetadata(tmpText, tag));
        }
    }

    /**
     * Embeds the specified result into the specified document
     *
     * @param result The result to embed
     * @param document The document to embed the file into
     * @param relation The relation between the attachment and the main PDF content
     * @return The file specification of the embedded attachment
     * @throws IOException if an I/O error is occurred
     * @throws OXException if an error is occurred
     */
    private PDComplexFileSpecification embedAttachment(MailExportConversionResult result, PDDocument document, PDFAFRelationship relation) throws IOException, OXException {
        if (!Status.SUCCESS.equals(result.getStatus())) {
            return null;
        }
        try (InputStream inputStream = result.getInputStream()) {
            return embedAttachment(inputStream, result.getContentType(), result.getTitle().orElseGet(() -> "attachment-" + UUID.randomUUID()), document, relation);
        }
    }

    /**
     * Embeds the specified attachment into the specified document
     *
     * @param info The attachment information
     * @param document The document to embed the file into
     * @param relation The relation between the attachment and the main PDF content
     * @return The file specification of the embedded attachment
     * @throws IOException if an I/O error is occurred
     */
    private PDComplexFileSpecification embedAttachment(MailExportAttachmentInformation info, PDDocument document, PDFAFRelationship relation) throws IOException, OXException {
        try (InputStream inputStream = info.getMailPart().getInputStream()) {
            return embedAttachment(inputStream, info.getBaseContentType(), info.getFileName(), document, relation);
        }
    }

    /**
     * Embeds the specified stream into the specified document
     *
     * @param stream The stream with the data to embed
     * @param contentType The stream's content type
     * @param filename The stream's filename
     * @param document The document to embed the file into
     * @param relation The relation between the attachment and the main PDF content
     * @return The file specification of the embedded attachment
     * @throws IOException if an I/O error is occurred
     * @throws OXException if an error is occurred
     */
    private PDComplexFileSpecification embedAttachment(InputStream stream, String contentType, String filename, PDDocument document, PDFAFRelationship relation) throws IOException, OXException {
        closeables.addFirst(stream);

        /* Setting the mod. date to the current time. A mod. date is required for PDF/A compliance */
        Calendar modDate = Calendar.getInstance(ServerSessionAdapter.valueOf(session).getUser().getLocale());
        PDEmbeddedFile ef = PDFFactory.createEmbeddedFile(document, stream, contentType, modDate);

        PDComplexFileSpecification fs = new PDComplexFileSpecification();
        fs.setFile(filename);
        /* Setting the Unicode filename as it is required for PDF/A compliance */
        fs.setFileUnicode(filename);
        fs.setEmbeddedFile(ef);
        /* PDF/A compliance requires to set relationship between attached file and the original PDF */
        COSDictionary dict = fs.getCOSObject();
        dict.setName(PDFAFRelationship.AF_RELATIONSHIP_KEY_NAME, relation.toString());

        return fs;
    }

    /**
     * Embeds the attachments to the specified document
     *
     * @param renderContext the render context
     * @param attachments The attachments to embed
     */
    private static void embedAttachments(RenderContext renderContext, Map<String, PDComplexFileSpecification> attachments) {
        if (attachments.isEmpty()) {
            return;
        }
        PDEmbeddedFilesNameTreeNode treeNode = new PDEmbeddedFilesNameTreeNode();
        treeNode.setNames(attachments);

        List<PDEmbeddedFilesNameTreeNode> kids = new LinkedList<>();
        kids.add(treeNode);

        PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();
        efTree.setKids(kids);

        PDDocumentNameDictionary names = new PDDocumentNameDictionary(renderContext.getPdDocument().getDocumentCatalog());
        names.setEmbeddedFiles(efTree);
        renderContext.getPdDocument().getDocumentCatalog().setNames(names);

        /* Associated files */
        PDDocumentCatalog catalog = renderContext.getPdDocument().getDocumentCatalog();
        COSArray associatedFiles = catalog.getCOSObject().containsKey("AF") ? (COSArray) catalog.getCOSObject().getItem("AF") : new COSArray();
        for (var attachmentEntry : attachments.entrySet()) {
            /* Adding AF entry for each embedded file as required by PDF/A compliance */
            PDComplexFileSpecification file = attachmentEntry.getValue();
            associatedFiles.add(file);
        }
        catalog.getCOSObject().setItem("AF", associatedFiles);
    }

    /**
     * Writes the PDF/A metadata to the specified document
     *
     * @param renderContext the render context
     * @param contentHolder the content holder
     * @throws OXException if an error is occurred
     */
    private void writePDFAMetadata(RenderContext renderContext, MailExportContentInformation contentHolder) throws OXException {
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        try {
            PDDocument pdDocument = renderContext.getPdDocument();
            PDDocumentInformation documentInformation = pdDocument.getDocumentInformation();
            documentInformation.setAuthor(contentHolder.getFrom());
            documentInformation.setTitle(contentHolder.getSubject() == null ? DEFAULT_TITLE : contentHolder.getSubject());
            documentInformation.setProducer(getProducer(options.getHostname(), session));
            if (contentHolder.getReceivedDate() != null) {
                documentInformation.setCreationDate(getCalendar(contentHolder.getReceivedDate()));
            }

            PDFAIdentificationSchema id = xmp.createAndAddPFAIdentificationSchema();
            id.setPart(I(3));
            id.setConformance("B");

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                XmpSerializer serializer = new XmpSerializer();
                serializer.serialize(xmp, outputStream, true);

                PDMetadata metadata = new PDMetadata(pdDocument);
                metadata.importXMPMetadata(outputStream.toByteArray());
                pdDocument.getDocumentCatalog().setMetadata(metadata);
            }

            // sRGB output intent
            pdDocument.getDocumentCatalog().addOutputIntent(PDFFactory.loadColourProfile(pdDocument));
        } catch (BadFieldValueException e) {
            throw new IllegalArgumentException(e); // Should never happen
        } catch (TransformerException e) {
            throw MailExportExceptionCode.UNEXPECTED_ERROR.create(e.getMessage(), e);
        } catch (IOException e) {
            throw MailExportExceptionCode.IO_ERROR.create(e.getMessage(), e);
        }
    }

    /**
     * Gets the calendar from the specified date
     *
     * @param date The date to convert to calendar
     * @return The calendar
     */
    private Calendar getCalendar(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    /**
     * Gets the configured product name
     *
     * @param hostName The host name
     * @param session The session
     * @return The product name
     * @throws OXException If product name cannot be returned
     */
    private String getProducer(String hostName, Session session) throws OXException {
        ServerConfig serverConfig = serverConfigService.getServerConfig(hostName, session);
        return serverConfig.getProductName() + " " + serverConfig.getServerVersion();
    }

    /**
     * Saves the document into a new threshold file holder.
     *
     * @return The document as a file holder
     * @throws IOException if an I/O error is occurred
     */
    private static ThresholdFileHolder saveDocument(RenderContext renderContext) throws IOException {
        PDDocument document = renderContext.getPdDocument();
        if (null == document) {
            return null;
        }

        ThresholdFileHolder fileHolder = null;
        boolean error = true;
        try {
            fileHolder = new ThresholdFileHolder();
            fileHolder.setName(document.getDocumentInformation().getTitle());
            fileHolder.setContentType("application/pdf");

            // Render context must be closed before saving the document
            renderContext.close();
            document.save(fileHolder.asOutputStream());
            ThresholdFileHolder result = fileHolder;
            error = false;
            return result;
        } finally {
            if (error) {
                Streams.close(fileHolder);
            }
        }
    }

    /**
     * Wraps the exported PDF document into an export results.
     *
     * @param document The file holder for the exported PDF document
     * @param mailInfo The basic information extracted from the mail
     */
    private PDFExportResult getResult(ThresholdFileHolder document, MailExportContentInformation mailInfo) {
        return new PDFExportResult(document, null) {

            @Override
            public String getTitle() {
                return mailInfo.getSubject();
            }

            @Override
            public List<OXException> getWarnings() {
                return warnings;
            }

            @Override
            public Date getDate() {
                return null != mailInfo.getReceivedDate() ? mailInfo.getReceivedDate() : mailInfo.getSentDate();
            }
        };
    }

    /**
     * Parses a mail message and tracks all relevant information for further processing.
     *
     * @param mailAccess The mail access
     * @return The parsed mail information
     * @throws OXException If parsing fails
     */
    private MailExportContentInformation getMailInfo(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException {
        PDFExportMailMessageHandler messageHandler = new PDFExportMailMessageHandler(mailAccess.getSession());
        MailMessage mailMessage = getMailMessage(mailAccess);
        new MailMessageParser().parseMailMessage(mailMessage, messageHandler);
        return messageHandler.getInformation();
    }

    /**
     * Returns the specified integer property
     *
     * @param property The integer property
     * @return The value of the integer property
     */
    private int getIntProperty(MailExportProperty property) {
        return leanConfigService.getIntProperty(session.getUserId(), session.getContextId(), property);
    }

    /**
     * Decides whether previews shall be generated
     *
     * @return <code>true</code> if previews shall be generated; <code>false</code> otherwise
     */
    private boolean generatePreviews() {
        return options.appendAttachmentPreviews() || options.embedAttachmentPreviews();
    }

    /**
     * Returns the main {@link FontSet} to use and embeds it into the given document
     *
     * @param document The document to embed the font to
     * @return The main font
     * @throws OXException if an error is occurred
     */
    private FontSet getFont(PDDocument document) throws OXException {
        if (font == null) {
            font = PDFFontUtils.getFont(document);
        }
        return font;
    }

    /**
     * Returns the alternative {@link FontSet} to use and embeds it into the given document
     *
     * @param document The document to embed the font to
     * @return The alternative font
     * @throws OXException if an error is occurred
     */
    private FontSet getAlternativeFont(PDDocument document) throws OXException {
        if (alternativeFont == null) {
            alternativeFont = PDFFontUtils.getAlternativeFont(document);
        }
        return alternativeFont;
    }

    /**
     * Decides according to the specified options whether the attachments shall be handled
     *
     * @param options The options
     * @return <code>true</code> if the attachments shall be handled; <code>false</code> otherwise
     */
    private boolean handleAttachments(MailExportOptions options) {
        return options.appendAttachmentPreviews() || options.embedAttachmentPreviews() || options.embedRawAttachments() || options.embedNonConvertibleAttachments();
    }

    @Override
    public void close() {
        attachmentWriters.forEach(Streams::close);
        Streams.closeAutoCloseables(closeables);
    }
}
