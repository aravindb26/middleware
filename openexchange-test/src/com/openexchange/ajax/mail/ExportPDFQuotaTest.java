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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.DestinationBody;
import com.openexchange.testing.httpclient.models.ExportPDFResponse;
import com.openexchange.testing.httpclient.models.MailDestinationData;
import com.openexchange.testing.httpclient.models.MailImportResponse;
import com.openexchange.testing.httpclient.models.MailListElement;
import com.openexchange.testing.httpclient.modules.MailApi;

/**
 * {@link ExportPDFQuotaTest}
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class ExportPDFQuotaTest extends AbstractMailTest {

    private static final Long VERY_LIMITITED_QUOTA = Long.valueOf(1);
    private static final String FOLDER = "default0/INBOX";

    private String testMailDir;
    private MailApi mailApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        testMailDir = AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR);
        mailApi = new MailApi(testUser.getApiClient());
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContextConfig(
            TestContextConfig.builder()
            .withMaxQuota(VERY_LIMITITED_QUOTA)
            .withConfig(Map.of("com.openexchange.capability.mail_export_pdf", "true",
                               "com.openexchange.mail.exportpdf.gotenberg.enabled", "true",
                               "com.openexchange.mail.exportpdf.collabora.enabled", "true",
                               "com.openexchange.mail.exportpdf.pdfa.collabora.enabled", "true"))
            .build())
      .build();
    }

    /**
     * Imports the specified mail
     *
     * @param mailFile The mail file
     * @throws ApiException
     */
    public String importMail(String mailFile) throws ApiException {
        File emlFile = new File(testMailDir, mailFile);
        MailImportResponse importResponse = mailApi.importMail(FOLDER, emlFile, null, Boolean.TRUE);
        assertNull(importResponse.getError());
        assertNull(importResponse.getErrorDesc());
        assertNotNull(importResponse.getData());
        List<MailDestinationData> data = importResponse.getData();
        assertFalse("data should be present", data.isEmpty());
        MailListElement element = new MailListElement();
        element.folder(FOLDER).id(data.get(0).getId());
        return element.getId();
    }


    @Test
    public void testExportPDFQuotaExceeded() throws Exception {
        String mailId = importMail("mailExportWithManyAttachments.eml");
        TestMail myMail = getMail(FOLDER, mailId);
        String driveFolder = Integer.toString(testUser.getAjaxClient().getValues().getPrivateInfostoreFolder());
        DestinationBody destinationBody = new DestinationBody();
        destinationBody.setFolderId(driveFolder);
        destinationBody.appendAttachmentPreviews(B(true));
        destinationBody.embedNonConvertibleAttachments(B(true));
        destinationBody.embedAttachmentPreviews(B(true));
        destinationBody.embedRawAttachments(B(true));
        destinationBody.preferRichText(B(true));
        destinationBody.pageFormat("a4");
        destinationBody.includeExternalImages(B(true));
        ExportPDFResponse exportResponse = mailApi.exportPDF(FOLDER, myMail.getId(), destinationBody, B(false), Boolean.FALSE, null);
        assertNotNull(exportResponse.getError());
        assertEquals(FileStorageExceptionCodes.QUOTA_REACHED.create().getErrorCode(), exportResponse.getCode());
    }
}
