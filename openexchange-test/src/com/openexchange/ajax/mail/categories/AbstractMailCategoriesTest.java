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

package com.openexchange.ajax.mail.categories;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.capabilities.actions.AllRequest;
import com.openexchange.ajax.capabilities.actions.AllResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.framework.UserValues;
import com.openexchange.ajax.mail.actions.GetRequest;
import com.openexchange.config.cascade.ConfigViewScope;
import com.openexchange.exception.OXException;
import com.openexchange.mail.categories.MailCategoriesConstants;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.MailCategoriesMoveBody;
import com.openexchange.testing.httpclient.models.MailCategoriesTrainBody;
import com.openexchange.testing.httpclient.models.MailsResponse;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.MailCategoriesApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractMailCategoriesTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public abstract class AbstractMailCategoriesTest extends AbstractConfigAwareAPIClientSession {

    protected static final String COLUMNS = "102, 600, 601, 602, 603, 604, 605, 606, 607, 608, 610, 611, 614, 652, 656";

    protected static final String CAT_GENERAL = "general";
    protected static final String CAT_1 = "social";
    protected static final String CAT_1_FLAG = "$social";
    protected static final String CAT_2 = "promotion";
    protected static final String CAT_2_FLAG = "$promotion";

    protected UserValues values;
    protected String EML;
    protected MailApi mailApi;
    protected MailCategoriesApi categoriesApi;


    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);
        setUpConfiguration();
        AllResponse response = getClient().execute(new AllRequest());
        assumeTrue(response.getCapabilities().contains("mail_categories"), "User does not have the mail_categories capability. Probably the mailserver does not support imap4flags.");

        values = getClient().getValues();
        EML = "Date: Mon, 19 Nov 2012 21:36:51 +0100 (CET)\n" + "From: " + getSendAddress() + "\n" + "To: " + getSendAddress() + "\n" + "Message-ID: <1508703313.17483.1353357411049>\n" + "Subject: Test mail\n" + "MIME-Version: 1.0\n" + "Content-Type: multipart/alternative; \n" + "    boundary=\"----=_Part_17482_1388684087.1353357411002\"\n" + "\n" + "------=_Part_17482_1388684087.1353357411002\n" + "MIME-Version: 1.0\n" + "Content-Type: text/plain; charset=UTF-8\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "Test\n" + "------=_Part_17482_1388684087.1353357411002\n" + "MIME-Version: 1.0\n" + "Content-Type: text/html; charset=UTF-8\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">" + " <head>\n" + "    <meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\"/>\n" + " </head><body style=\"font-family: verdana,geneva; font-size: 10pt; \">\n" + " \n" + "  <div>\n" + "   Test\n" + "  </div>\n" + " \n" + "</body></html>\n" + "------=_Part_17482_1388684087.1353357411002--\n";
        mailApi = new MailApi(testUser.getApiClient());
        categoriesApi = new MailCategoriesApi(testUser.getApiClient());
    }


    protected List<MailCategoriesMoveBody> toBody(String folderId, String id) {
        return Collections.singletonList(new MailCategoriesMoveBody().folderId(folderId).id(id));
    }

    protected void train(String category, String address, Boolean applyForExisting, Boolean createRule) throws ApiException {
        CommonResponse resp = categoriesApi.train(category, new MailCategoriesTrainBody().addFromItem(address), applyForExisting, createRule);
        assertNull(resp.getError(), resp.getErrorDesc());
    }

    protected List<List<Object>> getAllMails(String folderId, String category) throws ApiException {
        MailsResponse allMailsResp = mailApi.getAllMails(folderId, COLUMNS, null, null, null, "610", "DESC", null, null, null, category);
        assertNull(allMailsResp.getError(), allMailsResp.getErrorDesc());
        return allMailsResp.getData();
    }

    @Override
    protected String getReloadables() {
        return "CapabilityReloadable";
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        Map<String, String> configs = new HashMap<>();
        configs.put("com.openexchange.mail.categories", Boolean.TRUE.toString());
        configs.put(MailCategoriesConstants.MAIL_CATEGORIES_SWITCH, Boolean.TRUE.toString());
        configs.put("com.openexchange.mail.categories.general.name.fallback", "General");
        configs.put(MailCategoriesConstants.MAIL_CATEGORIES_IDENTIFIERS, "promotion, social");
        configs.put(MailCategoriesConstants.MAIL_USER_CATEGORIES_IDENTIFIERS, "uc1");
        configs.put("com.openexchange.mail.categories.promotion.flag", "$promotion");
        configs.put("com.openexchange.mail.categories.promotion.force", Boolean.FALSE.toString());
        configs.put("com.openexchange.mail.categories.promotion.active", Boolean.TRUE.toString());
        configs.put("com.openexchange.mail.categories.promotion.fallback", "Offers");
        configs.put("com.openexchange.mail.categories.social.flag", "$social");
        configs.put("com.openexchange.mail.categories.social.force", Boolean.TRUE.toString());
        configs.put("com.openexchange.mail.categories.social.active", Boolean.FALSE.toString());
        configs.put("com.openexchange.mail.categories.social.fallback", "Social");
        configs.put("com.openexchange.mail.categories.uc1.flag", "$uc1");
        configs.put("com.openexchange.mail.categories.uc1.active", Boolean.TRUE.toString());
        configs.put("com.openexchange.mail.categories.uc1.fallback", "Family");
        return configs;
    }

    @Override
    protected String getScope() {
        return ConfigViewScope.USER.getScopeName();
    }

    /**
     * Performs a hard delete on specified folder
     *
     * @param folder
     *            The folder
     */
    protected final void clearFolder(final String folder) throws OXException, IOException, JSONException {
        Executor.execute(getClient().getSession(), new com.openexchange.ajax.mail.actions.ClearRequest(folder).setHardDelete(true));
    }

    /**
     * @return User's default send address
     */
    protected String getSendAddress() throws OXException, IOException, JSONException {
        return getSendAddress(getClient());
    }

    protected String getSendAddress(final AJAXClient client) throws OXException, IOException, JSONException {
        return client.getValues().getSendAddress();
    }

    protected String getInboxFolder() throws OXException, IOException, JSONException {
        return getClient().getValues().getInboxFolder();
    }

    protected JSONObject getFirstMailInFolder(final String inboxFolder) throws OXException, IOException, JSONException {
        final CommonAllResponse response = getClient().execute(new com.openexchange.ajax.mail.actions.AllRequest(inboxFolder, new int[] { 600 }, -1, null, true));
        final JSONArray arr = (JSONArray) response.getData();
        final JSONArray mailFields = arr.getJSONArray(0);
        final String id = mailFields.getString(0);
        final AbstractAJAXResponse response2 = getClient().execute(new GetRequest(inboxFolder, id));
        return (JSONObject) response2.getData();
    }
}
