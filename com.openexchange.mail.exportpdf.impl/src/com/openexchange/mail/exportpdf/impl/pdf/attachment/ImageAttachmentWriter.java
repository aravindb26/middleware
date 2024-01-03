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

package com.openexchange.mail.exportpdf.impl.pdf.attachment;

import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.mail.exportpdf.InternalConverterProperties;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.mail.exportpdf.MailExportProperty;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFFactory;
import com.openexchange.session.Session;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.pdfbox.layout.elements.Orientation;
import rst.pdfbox.layout.elements.PageFormat;
import rst.pdfbox.layout.elements.render.RenderContext;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * {@link ImageAttachmentWriter}
 */
public class ImageAttachmentWriter extends AbstractAttachmentWriter {

    private static final Logger LOG = LoggerFactory.getLogger(ImageAttachmentWriter.class);
    private final PageFormat pageFormat;

    /**
     * Initialises a new {@link ImageAttachmentWriter}.
     *
     * @param pageFormat the page format
     */
    public ImageAttachmentWriter(Session session, LeanConfigurationService leanConfigService, PageFormat pageFormat) {
        super(session, leanConfigService, InternalConverterProperties.IMAGE_FILE_EXTENSIONS);
        this.pageFormat = pageFormat;
    }

    @Override
    public void writeAttachment(MailExportConversionResult result, MailExportOptions options, RenderContext renderContext) {
        if (result.getStatus().equals(Status.UNAVAILABLE)) {
            return;
        }
        try (InputStream stream = result.getInputStream()) {
            if (checkNullStream(stream, result.getTitle())) {
                return;
            }
            PDDocument pdDocument = renderContext.getPdDocument();
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdDocument, IOUtils.toByteArray(stream), result.getTitle().orElse("Image-" + UUID.randomUUID()));
            Orientation orientation = getPageOrientation(pdImage);
            PDPage attachmentPage = PDFFactory.createPage(pageFormat, orientation);
            pdDocument.addPage(attachmentPage);

            float yPosition = attachmentPage.getMediaBox().getUpperRightY() - pageFormat.getMarginTop();
            PDRectangle box = attachmentPage.getMediaBox();
            drawImage(result, pdDocument, pdImage, attachmentPage, yPosition, box);
        } catch (Exception e) {
            result.addWarnings(List.of(MailExportExceptionCode.UNEXPECTED_ERROR.create(e)));
            LOG.debug("", e);
        }
    }

    /**
     * Draws the image to the specified document
     * 
     * @param result The conversion result
     * @param pdDocument The document
     * @param pdImage The image to draw
     * @param attachmentPage The attachment page to draw the image
     * @param yPosition the y position
     * @param box The content box
     */
    private void drawImage(MailExportConversionResult result, PDDocument pdDocument, PDImageXObject pdImage, PDPage attachmentPage, float yPosition, PDRectangle box) {
        try (PDPageContentStream contents = PDFFactory.createPageContentStream(pdDocument, attachmentPage)) {
            float scaledWidth = pdImage.getWidth();
            float scaledHeight = pdImage.getHeight();
            if (pdImage.getWidth() > (box.getWidth() - (pageFormat.getMarginLeft() + pageFormat.getMarginRight()))) {
                scaledWidth = box.getWidth() - (pageFormat.getMarginLeft() + pageFormat.getMarginRight());
                scaledHeight = ((scaledWidth * (pdImage.getHeight() - (pageFormat.getMarginBottom() + pageFormat.getMarginTop()))) / pdImage.getWidth());
            }
            if (scaledHeight > (box.getHeight() - (pageFormat.getMarginBottom() + pageFormat.getMarginTop()))) {
                scaledHeight = box.getHeight() - (pageFormat.getMarginBottom() + pageFormat.getMarginTop());
                scaledWidth = (scaledHeight * pdImage.getWidth()) / pdImage.getHeight();
            }
            contents.drawImage(pdImage, pageFormat.getMarginLeft(), yPosition - scaledHeight, scaledWidth, scaledHeight);
        } catch (Exception e) {
            result.addWarnings(List.of(MailExportExceptionCode.UNEXPECTED_ERROR.create(e)));
            LOG.debug("", e);
        }
    }

    /**
     * Retrieves the page orientation
     *
     * @param pdImage the image to determine the page orientation
     * @return The page orientation
     */
    private Orientation getPageOrientation(PDImageXObject pdImage) {
        if (!leanConfigService.getBooleanProperty(session.getUserId(), session.getContextId(), MailExportProperty.autoPageOrientation)) {
            return Orientation.Portrait;
        }
        return pdImage.getWidth() > pdImage.getHeight() ? Orientation.Landscape : Orientation.Portrait;
    }
}
