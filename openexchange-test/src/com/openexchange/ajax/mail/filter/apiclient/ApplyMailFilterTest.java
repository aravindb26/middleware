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

package com.openexchange.ajax.mail.filter.apiclient;

import static com.openexchange.java.Autoboxing.I;
import static java.lang.Boolean.FALSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.google.common.collect.ImmutableList;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.MailDestinationData;
import com.openexchange.testing.httpclient.models.MailFilterAction;
import com.openexchange.testing.httpclient.models.MailFilterApplyResponse;
import com.openexchange.testing.httpclient.models.MailFilterCreationResponse;
import com.openexchange.testing.httpclient.models.MailFilterRulev2;
import com.openexchange.testing.httpclient.models.MailFilterTestv2;
import com.openexchange.testing.httpclient.models.MailImportResponse;
import com.openexchange.testing.httpclient.models.MailsResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.models.Result.ResultEnum;

/**
 * {@link ApplyMailFilterTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public class ApplyMailFilterTest extends AbstractMailFilterTest {

    /**
     * Initializes a new {@link ApplyMailFilterTest}.
     */
    public ApplyMailFilterTest() {
        super();
    }

    /**
     * Tests the apply mail-filter action
     */
    @Test
    public void testApplyMailfilterAction() throws ApiException {
        // Create a new mail folder
        String folderId = createFolder();

        // Add two mails to this folder (one matches the sieve rule)
        String testMailDir = AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR);
        importMail("mailfilter1.eml", testMailDir, folderId); // person1 to person2
        String matchingMailId = importMail("mailfilter2.eml", testMailDir, folderId); // person1 to person3

        // Add a new sieve rule and remember its id
        Integer ruleId = addNewRule(getRule());

        // Check amount of mails are 2
        getAllMailsAssertAmount(folderId, 2);

        // Apply the rule to the new folder
        applyPredefinedRule(folderId, ruleId);

        // Check one mail is properly marked as deleted
        MailsResponse allMails = getAllMailsAssertAmount(folderId, 1);
        Assertions.assertNotEquals(matchingMailId, allMails.getData().get(0).get(0));
    }

    /**
     * Tests the apply mail-filter action with an illegal redirect action
     */
    @Test
    public void testApplyMailfilterActionWithIllegalAction() throws ApiException {
        // Create a new mail folder
        String folderId = createFolder();

        // Add a new sieve rule and remember its id
        Integer ruleId = addNewRule(getIllegalRule());

        // Apply the rule to the new folder
        MailFilterApplyResponse applyPredefinedRule = mailfilterapi.applyPredefinedRule(ruleId, null, folderId);

        // Check result contains an error
        Assertions.assertNotNull(applyPredefinedRule.getErrorDesc(), applyPredefinedRule.getError());
        assertEquals(applyPredefinedRule.getCode(), "MAIL_FILTER-0036");
    }

    /////////////////////////////////// HELPERS //////////////////////////////////

    /**
     * Creates a new folder
     *
     * @return The folder identifier
     * @throws ApiException if an error is occurred
     */
    private String createFolder() throws ApiException {
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setTitle(ApplyMailFilterTest.class.getSimpleName() + "_" + System.currentTimeMillis());
        folder.setModule("mail");

        NewFolderBody body = new NewFolderBody();
        body.setFolder(folder);

        FolderUpdateResponse createFolder = folderApi.createFolder("default0/INBOX", body, "0", null, null, null);
        assertNull(createFolder.getError(), createFolder.getErrorDesc());
        return rememberFolder(createFolder.getData());
    }

    /**
     * Imports the specified .eml asset from the specified asset directory
     * to the folder with the specified identifier
     *
     * @param asset The asset name
     * @param assetDir The asset directory
     * @param folderId The folder identifier
     * @return The imported mail identifier
     * @throws ApiException if an error is occurred
     */
    private String importMail(String asset, String assetDir, String folderId) throws ApiException {
        File f = new File(assetDir, asset);
        Assertions.assertTrue(f.exists());

        MailImportResponse importResponse = mailApi.importMail(folderId, f, null, Boolean.TRUE);
        Assertions.assertNull(importResponse.getErrorDesc(), importResponse.getError());

        List<MailDestinationData> data = importResponse.getData();
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.size());

        return data.get(0).getId();
    }

    /**
     * Gets all mails from the specified folder and asserts that the expected amount
     * is equals the actual amount
     *
     * @param folderId The folder identifier
     * @param expectedAmount The expected amount
     * @return The MailsResponse
     * @throws ApiException if an API error is occurred
     */
    private MailsResponse getAllMailsAssertAmount(String folderId, int expectedAmount) throws ApiException {
        MailsResponse allMails = mailApi.getAllMails(folderId, "600", null, FALSE, FALSE, null, null, I(0), I(5), I(5), null);
        Assertions.assertNull(allMails.getErrorDesc(), allMails.getError());
        Assertions.assertNotNull(allMails.getData());
        Assertions.assertEquals(expectedAmount, allMails.getData().size(), "Unexpected amount of mails returned.");

        return allMails;
    }

    /**
     * Applies the predefined rule to the specified folder
     *
     * @param folderId The folder id to apply the rule
     * @param ruleId The rule id to apply to the folder
     * @throws ApiException if an error is occurred
     */
    private void applyPredefinedRule(String folderId, Integer ruleId) throws ApiException {
        MailFilterApplyResponse applyPredefinedRule = mailfilterapi.applyPredefinedRule(ruleId, null, folderId);

        // Check result is ok
        Assertions.assertNull(applyPredefinedRule.getErrorDesc(), applyPredefinedRule.getError());
        Assertions.assertNotNull(applyPredefinedRule.getData());
        applyPredefinedRule.getData().stream().forEach(result -> assertNull(result.getErrors(), "Expected no error but found: " + result.getErrors()));

        Assertions.assertEquals(1, applyPredefinedRule.getData().size(), "Invalid size of results: " + applyPredefinedRule.getData().toString());
        applyPredefinedRule.getData().stream().forEach(m -> assertEquals(ResultEnum.OK, m.getResult()));
    }

    /**
     * Adds a new sieve rule
     *
     * @param rule The rule to add
     * @return The rule's id
     * @throws ApiException if an error is occurred
     */
    private Integer addNewRule(MailFilterRulev2 rule) throws ApiException {
        MailFilterCreationResponse response = mailfilterapi.createRuleV2(rule, null);
        Assertions.assertNull(response.getErrorDesc(), response.getError());
        return rememberSieveRule(response.getData());
    }

    /**
     * Creates a rule
     *
     * @return The created rule
     */
    private MailFilterRulev2 getRule() {
        MailFilterTestv2 test = new MailFilterTestv2();
        test.id("to")
            .header("to")
            .comparison("contains")
            .values(List.of("person3@invalid.com"));

        MailFilterAction action = new MailFilterAction();
        action.setId("discard");

        return new MailFilterRulev2().test(test).actioncmds(ImmutableList.of(action));
    }

    /**
     * Creates a rule with a redirect action
     *
     * @return The created rule
     */
    private MailFilterRulev2 getIllegalRule() {
        MailFilterTestv2 test = new MailFilterTestv2();
        test.id("to")
            .header("to")
            .comparison("contains")
            .values(List.of("person3@invalid.com"));

        MailFilterAction action = new MailFilterAction();
        action.setId("redirect");
        action.to("person3@invalid.com");

        return new MailFilterRulev2().test(test).actioncmds(ImmutableList.of(action));
    }
}
