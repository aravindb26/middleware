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

package com.openexchange.ajax.mail;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.invoker.ApiResponse;
import com.openexchange.testing.httpclient.models.DestinationBody;
import com.openexchange.testing.httpclient.models.ExportPDFResponse;
import com.openexchange.testing.httpclient.models.ExportPDFResponseData;
import com.openexchange.testing.httpclient.models.InfoItemListElement;
import com.openexchange.testing.httpclient.models.MailAttachment;
import com.openexchange.testing.httpclient.models.MailDestinationData;
import com.openexchange.testing.httpclient.models.MailImportResponse;
import com.openexchange.testing.httpclient.models.MailListElement;
import com.openexchange.testing.httpclient.models.MailResponse;
import com.openexchange.testing.httpclient.modules.InfostoreApi;
import com.openexchange.testing.httpclient.modules.MailApi;

/**
 * {@link MailExportTest}
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ExportPDFTest extends AbstractMailTest {

    private String testMailDir;
    private MailApi mailApi;
    private InfostoreApi infostoreApi;

    private List<MailListElement> mailIds;
    private List<InfoItemListElement> fileIds;

    private static final String FOLDER = "default0/INBOX";

    public ExportPDFTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        mailIds = new LinkedList<>();
        fileIds = new LinkedList<>();
        testMailDir = AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR);
        mailApi = new MailApi(testUser.getApiClient());
        infostoreApi = new InfostoreApi(testUser.getApiClient());
    }

    @AfterEach
    public void cleanUp() throws ApiException {
        infostoreApi.deleteInfoItems(L(Long.MAX_VALUE), fileIds, B(true), null);
        mailApi.deleteMails(mailIds, L(Long.MAX_VALUE), B(true), B(false));
    }

    /**
     * Tests mail export without attachments from mail with attachments
     */
    @Test
    public void testExportPDF() throws Exception {
        byte[] data = cycle("testmail_png.eml");
        List<EmbeddedAttachment> embeddedFileNames = extractEmbeddedFiles(data);
        assertTrue(embeddedFileNames.isEmpty());
    }

    /**
     * Tests mail export with appended previews of the attachments
     */
    @Test
    public void testExportPDFWithAppendedPreviewAttachments() throws Exception {
        byte[] data = cycle("mailExportWithManyAttachments.eml", new TestMailExportOptions.Builder().withAppendPreviews(true).withPageFormat("a4").withRichText(false).build());
        List<EmbeddedAttachment> embeddedAttachments = extractEmbeddedFiles(data);
        assertTrue(embeddedAttachments.isEmpty());
    }

    /**
     * Tests mail export with embedded raw attachments
     *
     * @throws Exception
     */
    @Test
    public void testExportPDFWithEmbeddedRawAttachments() throws Exception {
        byte[] data = cycle("mailExportWithManyAttachments.eml", new TestMailExportOptions.Builder().withEmbedRaw(true).build());
        MailResponse mailResponse = mailApi.getMail(FOLDER, mailIds.get(0).getId(), null, null, null, null, null, null, null, null, null, null, null, null);
        List<MailAttachment> attachments = mailResponse.getData().getAttachments();
        List<EmbeddedAttachment> embeddedAttachments = extractEmbeddedFiles(data);
        assertEquals(attachments.size() - 1, embeddedAttachments.size()); // Subtract 1 to omit the actual body part
    }

    /**
     * Tests mail export with embedded preview attachments
     *
     * @throws Exception
     */
    @Test
    public void testExportPDFWithEmbeddedPreviewAttachments() throws Exception {
        byte[] data = cycle("mailExportWithManyAttachments.eml", new TestMailExportOptions.Builder().withEmbedPreviews(true).build());
        List<EmbeddedAttachment> embeddedAttachments = extractEmbeddedFiles(data);
        assertEquals(6, embeddedAttachments.size()); // Only six attachments are previewable
    }

    /**
     * Tests mail export with non-convertible embedded attachments
     *
     * @throws Exception
     */
    @Test
    public void testExportPDFWithNonConvertibleEmbeddedAttachments() throws Exception {
        byte[] data = cycle("mailExportWithManyAttachments.eml", new TestMailExportOptions.Builder().withEmbedNonConvertible(true).build());
        List<EmbeddedAttachment> embeddedAttachments = extractEmbeddedFiles(data);
        assertEquals(1, embeddedAttachments.size()); // Only one attachments is non convertible
    }

    /**
     * Tests mail export on non-existing mail
     */
    @Test
    public void testExportPDFMailNotFound() throws Exception {
        String driveFolder = Integer.toString(testUser.getAjaxClient().getValues().getPrivateInfostoreFolder());
        DestinationBody destinationBody = new DestinationBody();
        destinationBody.setFolderId(driveFolder);
        ExportPDFResponse exportResponse = mailApi.exportPDF(FOLDER, Integer.MAX_VALUE + "/" + Integer.MAX_VALUE, destinationBody, B(false), Boolean.FALSE, null);
        assertNotNull(exportResponse.getError());
        assertEquals(MailExceptionCode.MAIL_NOT_FOUND.create().getErrorCode(), exportResponse.getCode());
    }

    /**
     * Test mail export on non-existing drive folder
     */
    @Test
    public void testExportPDFDestinationFolderNotExists() throws Exception {
        DestinationBody destinationBody = new DestinationBody();
        destinationBody.setFolderId(Integer.MAX_VALUE + "/" + Integer.MAX_VALUE);
        ExportPDFResponse exportResponse = mailApi.exportPDF(FOLDER, Integer.MAX_VALUE + "/" + Integer.MAX_VALUE, destinationBody, B(false), Boolean.FALSE, null);
        assertNotNull(exportResponse.getError());
        assertEquals(FileStorageExceptionCodes.FOLDER_NOT_FOUND.create().getErrorCode(), exportResponse.getCode());
    }

    @Test
    public void testExportPDFWithEmbeddedPreviewOfCorruptDocumnt() throws Exception {
        byte[] data = cycle("corrupt_docx_no_magic_bytes.eml", new TestMailExportOptions.Builder().withEmbedPreviews(true).build());
        List<EmbeddedAttachment> embeddedAttachments = extractEmbeddedFiles(data);
        assertEquals(0, embeddedAttachments.size()); // No attachment should be present
    }

    @Test
    public void testExportPDFWithEmbeddedPreviewOfCorruptImage() throws Exception {
        byte[] data = cycle("corrupt_image_no_magic_bytes_regular_eof.eml", new TestMailExportOptions.Builder().withEmbedPreviews(true).build());
        List<EmbeddedAttachment> embeddedAttachments = extractEmbeddedFiles(data);
        assertEquals(0, embeddedAttachments.size()); // No attachment should be present
    }
    
    @Ignore
    public void testExportPDFWithEmbeddedPreviewAndNonConvertibleOfCorruptImage() throws Exception {
        byte[] data = cycle("corrupt_image_no_magic_bytes_regular_eof.eml", new TestMailExportOptions.Builder().withEmbedPreviews(true).withEmbedNonConvertible(true).build());
        List<EmbeddedAttachment> embeddedAttachments = extractEmbeddedFiles(data);
        assertEquals(1, embeddedAttachments.size()); // The corrupt attachment should be present
    }

    ////////////////////////////////////////////////// HELPERS /////////////////////////////////////////////

    private record EmbeddedAttachment(String filename, PDEmbeddedFile file) {
    }

    /**
     * Gets the mailApi
     *
     * @return The mailApi
     */
    protected MailApi getMailApi() {
        return mailApi;
    }

    /**
     * Extracts the embedded files (if any)
     *
     * @param data The pdf data
     * @return The embedded files`
     */
    private List<EmbeddedAttachment> extractEmbeddedFiles(byte[] data) throws Exception {
        try (PDDocument document = PDDocument.load(data)) {
            PDDocumentNameDictionary namesDictionary = new PDDocumentNameDictionary(document.getDocumentCatalog());
            PDEmbeddedFilesNameTreeNode efTree = namesDictionary.getEmbeddedFiles();
            if (efTree == null) {
                return extractFromAnnotations(document);
            }
            Map<String, PDComplexFileSpecification> names = efTree.getNames();
            if (names != null) {
                return extractFiles(names);
            }
            List<PDNameTreeNode<PDComplexFileSpecification>> kids = efTree.getKids();
            for (PDNameTreeNode<PDComplexFileSpecification> node : kids) {
                names = node.getNames();
                return extractFiles(names);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Extracts embedded files from annotations
     *
     * @param embeddedAttachments the container
     * @param document the document
     */
    private static List<EmbeddedAttachment> extractFromAnnotations(PDDocument document) throws IOException {
        List<EmbeddedAttachment> embeddedAttachments = new LinkedList<>();
        for (PDPage page : document.getPages()) {
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (!(annotation instanceof PDAnnotationFileAttachment annotationFileAttachment)) {
                    continue;
                }
                PDComplexFileSpecification fileSpec = (PDComplexFileSpecification) annotationFileAttachment.getFile();
                PDEmbeddedFile embeddedFile = getEmbeddedFile(fileSpec);
                embeddedAttachments.add(new EmbeddedAttachment(embeddedFile.getFile().getFile(), embeddedFile));
            }
        }
        return embeddedAttachments;
    }

    /**
     * Extracts the files from the specified specifications
     *
     * @param names The specifications
     * @return The files
     */
    private static List<EmbeddedAttachment> extractFiles(Map<String, PDComplexFileSpecification> names) {
        List<EmbeddedAttachment> embeddedAttachments = new LinkedList<>();
        for (Map.Entry<String, PDComplexFileSpecification> entry : names.entrySet()) {
            PDComplexFileSpecification fileSpec = entry.getValue();
            String filename = fileSpec.getFile();
            PDEmbeddedFile embeddedFile = getEmbeddedFile(fileSpec);
            embeddedAttachments.add(new EmbeddedAttachment(filename, embeddedFile));
        }
        return embeddedAttachments;
    }

    /**
     * Returns the embedded file according to OS
     *
     * @param fileSpec the file specification
     * @return the embedded file
     */
    private static PDEmbeddedFile getEmbeddedFile(PDComplexFileSpecification fileSpec) {
        if (fileSpec == null) {
            return null;
        }
        PDEmbeddedFile embeddedFile = fileSpec.getEmbeddedFileUnicode();
        if (embeddedFile == null) {
            embeddedFile = fileSpec.getEmbeddedFileDos();
        }
        if (embeddedFile == null) {
            embeddedFile = fileSpec.getEmbeddedFileMac();
        }
        if (embeddedFile == null) {
            embeddedFile = fileSpec.getEmbeddedFileUnix();
        }
        if (embeddedFile == null) {
            embeddedFile = fileSpec.getEmbeddedFile();
        }
        return embeddedFile;
    }

    /**
     * Imports an e-mail, exports it as pdf and fetches that pdf
     *
     * @param mailFile The mail file to cycle
     * @return the pdf export
     * @throws Exception if an error is occurred
     */
    private byte[] cycle(String mailFile) throws Exception {
        return cycle(mailFile, new TestMailExportOptions.Builder().build());
    }

    /**
     * Imports an e-mail, exports it as pdf and fetches that pdf
     *
     * @param mailFile The mail file to cycle
     * @return the pdf export
     * @throws Exception if an error is occurred
     */
    private byte[] cycle(String mailFile, TestMailExportOptions options) throws Exception {
        String mailId = importMail(mailFile);
        String fileId = exportPDF(mailId, options);
        File exportedFile = getExportedFile(fileId);
        byte[] data = Files.readAllBytes(Paths.get(exportedFile.getPath()));
        assertNotNull(data);
        return data;
    }

    /**
     * Imports the specified mail
     *
     * @param mailFile The mail file
     */
    private String importMail(String mailFile) throws ApiException {
        File emlFile = new File(testMailDir, mailFile);
        MailImportResponse importResponse = mailApi.importMail(FOLDER, emlFile, null, Boolean.TRUE);
        Assertions.assertNull(importResponse.getErrorDesc(), importResponse.getError());
        assertNotNull(importResponse.getData());
        List<MailDestinationData> data = importResponse.getData();
        if (data.isEmpty()) {
            fail("Mail was not imported");
        }
        MailListElement element = new MailListElement();
        element.folder(FOLDER).id(data.get(0).getId());
        mailIds.add(element);
        return element.getId();
    }

    /**
     * Exports the specified email
     *
     * @param mailId The mail id
     * @param options The text mail export options
     * @return The file id in file store
     */
    private String exportPDF(String mailId, TestMailExportOptions options) throws Exception {
        TestMail myMail = getMail(FOLDER, mailId);
        String driveFolder = Integer.toString(testUser.getAjaxClient().getValues().getPrivateInfostoreFolder());
        DestinationBody destinationBody = new DestinationBody();
        destinationBody.setFolderId(driveFolder);
        destinationBody.appendAttachmentPreviews(B(options.isAppendPreviews()));
        destinationBody.embedNonConvertibleAttachments(B(options.isEmbedNonConvertible()));
        destinationBody.embedAttachmentPreviews(B(options.isEmbedPreviews()));
        destinationBody.embedRawAttachments(B(options.isEmbedRaw()));
        destinationBody.preferRichText(B(options.isPreferRichtText()));
        destinationBody.pageFormat(options.getPageFormat());
        destinationBody.includeExternalImages(B(options.getIncludeExternalImages()));
        ExportPDFResponse exportResponse = mailApi.exportPDF(FOLDER, myMail.getId(), destinationBody, B(false), Boolean.FALSE, null);
        ExportPDFResponseData responseData = exportResponse.getData();
        assertNotNull(responseData, "No response.");

        InfoItemListElement element = new InfoItemListElement();
        element.id(responseData.getId());
        fileIds.add(element);
        return responseData.getId();
    }

    /**
     * Gets the exported file denoted by the specified id
     *
     * @param fileId The file id
     * @return The file
     */
    private File getExportedFile(String fileId) throws ApiException {
        ApiResponse<File> exportedFile = infostoreApi.getInfoItemDocumentWithHttpInfo(fileId.split("/")[0], fileId, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertNotNull(exportedFile.getData(), "No file at expected location");
        String contentType = exportedFile.getHeaders().get("content-type").get(0);
        String mimeType = contentType.split(";")[0];
        assertEquals("application/pdf", mimeType, "Mime type doesnt match.");
        return exportedFile.getData();
    }

    /**
     * POJO class that represents a document with specific formatting options.
     */
    private static class TestMailExportOptions {

        private final boolean appendPreviews;
        private final boolean embedPreviews;
        private final boolean embedRaw;
        private final boolean embedNonConvertible;
        private final boolean preferRichText;
        private final String pageFormat;
        private final boolean includeExternalImages;

        /**
         * Private constructor to be used by the builder.
         *
         * @param builder the builder that contains the formatting options
         */
        private TestMailExportOptions(Builder builder) {
            this.appendPreviews = builder.appendPreviews;
            this.embedPreviews = builder.embedPreviews;
            this.embedRaw = builder.embedRaw;
            this.embedNonConvertible = builder.embedNonConvertible;
            this.preferRichText = builder.preferRichText;
            this.pageFormat = builder.pageFormat;
            this.includeExternalImages = builder.includeExternalImages;
        }

        /**
         * Returns whether to append previews of the documents.
         *
         * @return true if previews should be appended, false otherwise
         */
        public boolean isAppendPreviews() {
            return appendPreviews;
        }

        /**
         * Returns whether to embed previews of the documents.
         *
         * @return true if previews should be embedded, false otherwise
         */
        public boolean isEmbedPreviews() {
            return embedPreviews;
        }

        /**
         * Returns whether to embed the raw data of the documents.
         *
         * @return true if raw data should be embedded, false otherwise
         */
        public boolean isEmbedRaw() {
            return embedRaw;
        }

        /**
         * Returns whether to embed non-convertible documents.
         *
         * @return true if non-convertible documents should be embedded, false otherwise
         */
        public boolean isEmbedNonConvertible() {
            return embedNonConvertible;
        }

        /**
         * Returns whether to prefer rich text for the mail body
         *
         * @return true if rich text for the mail body if preferred, false otherwise
         */
        public boolean isPreferRichtText() {
            return preferRichText;
        }

        /**
         * Returns the page format of the documents.
         *
         * @return the page format of the documents
         */
        public String getPageFormat() {
            return pageFormat;
        }

        /**
         * Returns whether external images should be included.
         *
         * @return true if external images should be included, false otherwise
         */
        public boolean getIncludeExternalImages() {
            return includeExternalImages;
        }

        /**
         * Builder class for the TestMailExportOptions POJO.
         */
        private static class Builder {

            private boolean appendPreviews;
            private boolean embedPreviews;
            private boolean embedRaw;
            private boolean embedNonConvertible;
            private boolean preferRichText;
            private String pageFormat;
            private boolean includeExternalImages;

            /**
             * Sets the append previews option.
             *
             * @param appendPreviews the value to set for append previews
             * @return the builder object
             */
            public Builder withAppendPreviews(boolean appendPreviews) {
                this.appendPreviews = appendPreviews;
                return this;
            }

            /**
             * Sets the embed previews option.
             *
             * @param embedPreviews the value to set for embed previews
             * @return the builder object
             */
            public Builder withEmbedPreviews(boolean embedPreviews) {
                this.embedPreviews = embedPreviews;
                return this;
            }

            /**
             * Sets the embed raw option.
             *
             * @param embedRaw the value to set for embed raw
             * @return the builder object
             */
            public Builder withEmbedRaw(boolean embedRaw) {
                this.embedRaw = embedRaw;
                return this;
            }

            /**
             * Sets the embed non-convertible option.
             *
             * @param embedNonConvertible the value to set for embed non-convertible
             * @return the builder object
             */
            public Builder withEmbedNonConvertible(boolean embedNonConvertible) {
                this.embedNonConvertible = embedNonConvertible;
                return this;
            }

            /**
             * Sets the page format option.
             *
             * @param pageFormat the value to set for page format
             * @return the builder object
             */
            public Builder withPageFormat(String pageFormat) {
                this.pageFormat = pageFormat;
                return this;
            }

            /**
             * Sets the prefer rich text option
             *
             * @param preferRichText the value to set for prefer rich text
             * @return the builder object
             */
            public Builder withRichText(boolean preferRichText) {
                this.preferRichText = preferRichText;
                return this;
            }

            /**
             * Sets the include external images option
             *
             * @param includeExternalImages the value to set for including external images
             * @return the builder object
             */
            public Builder withIncludeExternalImages(boolean includeExternalImages) {
                this.includeExternalImages = includeExternalImages;
                return this;
            }

            /**
             * Builds the TestMailExportOptions POJO using the options set in the builder.
             *
             * @return a new instance of TestMailExportOptions
             */
            public TestMailExportOptions build() {
                return new TestMailExportOptions(this);
            }
        }
    }
}
