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

package com.openexchange.ajax.mail.authenticity;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.mail.authenticity.MailAuthenticityProperty;
import com.openexchange.mail.authenticity.mechanism.dkim.DKIMResult;
import com.openexchange.mail.authenticity.mechanism.dmarc.DMARCResult;
import com.openexchange.mail.authenticity.mechanism.spf.SPFResult;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.AuthenticationResult;
import com.openexchange.testing.httpclient.models.AuthenticationResult.StatusEnum;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailDestinationData;
import com.openexchange.testing.httpclient.models.MailImportResponse;
import com.openexchange.testing.httpclient.models.MailResponse;
import com.openexchange.testing.httpclient.models.MechanismResult;
import com.openexchange.testing.httpclient.modules.ImageApi;
import com.openexchange.testing.httpclient.modules.MailApi;

/**
 * {@link MailAuthenticityTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public class MailAuthenticityTest extends AbstractConfigAwareAPIClientSession {

    private static final String FOLDER = "default0/INBOX";
    private static final String SUBFOLDER = "authenticity";
    private static final String PASS_ALL = "passAll.eml";
    private static final String PISHING = "pishing.eml";
    private static final String NONE = "none.eml";
    private static final String TRUSTED = "trusted.eml";
    private static final String[] MAIL_NAMES = new String[] { PASS_ALL, PISHING, NONE, TRUSTED };
    private MailApi api;
    private final Map<String, MailDestinationData> IMPORTED_EMAILS = new HashMap<>();
    private ImageApi imageApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        // Setup configurations ----------------------------------
        // general config
        CONFIG.put(MailAuthenticityProperty.ENABLED.getFQPropertyName(), Boolean.TRUE.toString());
        CONFIG.put(MailAuthenticityProperty.AUTHSERV_ID.getFQPropertyName(), "open-xchange.authenticity.test");

        // trusted domain config
        CONFIG.put("com.openexchange.mail.authenticity.trusted.config", "support@open-xchange.com:1");

        String testMailDir = AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR) + SUBFOLDER;
        String imgName = "ox.jpg";
        File imgFile = new File(testMailDir, imgName);
        if (imgFile.exists()) {
            CONFIG.put("com.openexchange.mail.authenticity.trusted.image.1", imgFile.getAbsolutePath());
        }
        super.setUpConfiguration();

        // Setup client and import mails ------------------------
        api = new MailApi(getApiClient());
        imageApi = new ImageApi(getApiClient());

        for (String name : MAIL_NAMES) {
            File mail = new File(testMailDir, name);
            Assertions.assertTrue(imgFile.exists());
            MailImportResponse response = api.importMail(FOLDER, mail, null, Boolean.TRUE);
            List<MailDestinationData> data = checkResponse(response);
            // data size should always be 1
            Assertions.assertEquals(1, data.size());
            IMPORTED_EMAILS.put(name, data.get(0));
        }

    }

    @Test
    public void testBasicFunctionality() throws ApiException {
        /*
         * Test pass all
         */
        MailResponse resp = api.getMail(FOLDER, IMPORTED_EMAILS.get(PASS_ALL).getId(), null, I(0), null, Boolean.FALSE, null, null, null, null, null, null, null, null);
        MailData mail = checkResponse(resp);
        AuthenticationResult authenticationResult = mail.getAuthenticity();
        Assertions.assertNotNull(authenticationResult);
        List<MechanismResult> mailAuthenticityMechanismResults = authenticationResult.getUnconsideredResults();
        assertTrue(mailAuthenticityMechanismResults.isEmpty());

        Assertions.assertEquals(SPFResult.PASS.getTechnicalName(), authenticationResult.getSpf().getResult());
        Assertions.assertEquals(DKIMResult.PASS.getTechnicalName(), authenticationResult.getDkim().getResult());
        Assertions.assertEquals(DMARCResult.PASS.getTechnicalName(), authenticationResult.getDmarc().getResult());
        Assertions.assertEquals(StatusEnum.PASS, authenticationResult.getStatus());

        /*
         * Test pishing
         */
        resp = api.getMail(FOLDER, IMPORTED_EMAILS.get(PISHING).getId(), null, I(0), null, Boolean.FALSE, null, null, null, null, null, null, null, null);
        mail = checkResponse(resp);
        authenticationResult = mail.getAuthenticity();
        Assertions.assertNotNull(authenticationResult);
        mailAuthenticityMechanismResults = authenticationResult.getUnconsideredResults();
        assertTrue(mailAuthenticityMechanismResults.isEmpty());
        Assertions.assertEquals(StatusEnum.SUSPICIOUS, authenticationResult.getStatus());

        /*
         * Test none
         */
        resp = api.getMail(FOLDER, IMPORTED_EMAILS.get(NONE).getId(), null, I(0), null, Boolean.FALSE, null, null, null, null, null, null, null, null);
        mail = checkResponse(resp);
        authenticationResult = mail.getAuthenticity();
        Assertions.assertNotNull(authenticationResult);
        mailAuthenticityMechanismResults = authenticationResult.getUnconsideredResults();
        Assertions.assertFalse(mailAuthenticityMechanismResults.isEmpty());

        Assertions.assertEquals(SPFResult.NONE.getTechnicalName(), authenticationResult.getSpf().getResult());
        Assertions.assertEquals(DKIMResult.NONE.getTechnicalName(), authenticationResult.getDkim().getResult());
        Assertions.assertEquals(DMARCResult.NONE.getTechnicalName(), authenticationResult.getDmarc().getResult());
        Assertions.assertEquals(StatusEnum.NEUTRAL, authenticationResult.getStatus());
    }

    @Test
    public void testTrustedDomain() throws ApiException {
        /*
         * Test trusted domain
         */
        MailResponse resp = api.getMailBuilder().withFolder(FOLDER).withId(IMPORTED_EMAILS.get(TRUSTED).getId()).execute();
        MailData mail = checkResponse(resp);
        AuthenticationResult authenticationResult = mail.getAuthenticity();
        Assertions.assertNotNull(authenticationResult);
        Assertions.assertEquals(StatusEnum.TRUSTED, authenticationResult.getStatus());
        Assertions.assertNotNull(authenticationResult.getImage());

        byte[] trustedMailPicture = imageApi.getTrustedMailPicture(authenticationResult.getImage());
        Assertions.assertNotNull(trustedMailPicture);
        Assertions.assertNotEquals(0, trustedMailPicture.length);
    }

    private MailData checkResponse(MailResponse resp) {
        Assertions.assertNull(resp.getError());
        Assertions.assertNotNull(resp.getData());
        return resp.getData();
    }

    private List<MailDestinationData> checkResponse(MailImportResponse response) {
        Assertions.assertNull(response.getError());
        Assertions.assertNotNull(response.getData());
        return response.getData();
    }

    // -------------------------   prepare config --------------------------------------

    private static final Map<String, String> CONFIG = new HashMap<>();

    @Override
    protected Map<String, String> getNeededConfigurations() {
        return CONFIG;
    }

    @Override
    protected String getScope() {
        return "user";
    }

    @Override
    protected String getReloadables() {
        return "ConfigReloader,TrustedMailAuthenticityHandler";
    }

}
