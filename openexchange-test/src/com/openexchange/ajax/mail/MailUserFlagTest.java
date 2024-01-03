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

import static com.openexchange.ajax.framework.ClientCommons.checkResponse;
import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.openexchange.ajax.framework.AbstractClientSession;
import com.openexchange.ajax.mail.contenttypes.MailContentType;
import com.openexchange.mail.MailJSONField;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailDestinationResponse;
import com.openexchange.testing.httpclient.models.MailResponse;
import com.openexchange.testing.httpclient.models.MailUpdateBody;
import com.openexchange.testing.httpclient.models.MailsResponse;
import com.openexchange.testing.httpclient.modules.MailApi;

/**
 * {@link MailUserFlagTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class MailUserFlagTest extends AbstractClientSession {

    /**
     * Columns: received_date, id, folder_id, subject
     */
    private static final String COLUMNS = "610,600,601,607";
    private static final String SUBJECT = "Just a test";
    private static final String INBOX = "default0/INBOX";
    private static final String PILE_OF_POO = "\uD83D\uDCA9";
    private MailApi mailApi;
    private String mailId;

    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        mailApi = new MailApi(testUser.getApiClient());
        /*
         * Create mail in INBOX
         */
        String mail = createEMail(testUser.getLogin(), SUBJECT, "Here is a little text for me.");
        String callback = mailApi.sendMailBuilder().withJson0(mail).execute();
        assertTrue(callback.contains("callback_new"));
        mailId = getMailId(mailApi, SUBJECT);
        MailResponse response = mailApi.getMailBuilder().withFolder(INBOX).withId(mailId).execute();
        MailData mailData = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        List<String> userFlags = mailData.getUser();
        assertTrue(null == userFlags || userFlags.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings =
    { "my_fancy_flag", PILE_OF_POO, "\u00dc\u00d6\u00c4", "\u00f1\u00f5", "\ud83d\udcb8", "$cf_foo" })
    public void testSingleUserFlag(String flag) throws Exception {
        /*
         * Update mail with any user flag, use e.g. emoji to test en-/de-coding from UTF-7 based user flags
         */
        setFlags(List.of(flag));
        /*
         * Get mail and check that user flags are set
         */
        List<String> userFlags = getUserFlags();
        assertTrue(null != userFlags && 1 == userFlags.size());
        assertTrue(flag.equals(userFlags.get(0)));
    }

    @Test
    public void testMultipleUserFlags() throws Exception {
        {
            /*
             * Update mail with any user flags, add duplicate flag
             */
            List<String> flags = List.of("my_fancy_flag", "myFancyFlag", "myfancyflag");
            setFlags(flags);
            /*
             * Get mail and check that user flags are set, flags ignore case so only expect two flags
             */
            List<String> userFlags = getUserFlags();
            assertTrue(null != userFlags && 2 == userFlags.size());
            for (String userFlag : userFlags) {
                assertTrue(flags.contains(userFlag));
            }
        }
        {
            /*
             * Update mail with another user flags
             */
            setFlags(List.of(PILE_OF_POO));
            /*
             * Get mail and check that user flag was added, other flags should still be there
             */
            List<String> userFlags = getUserFlags();
            assertTrue(null != userFlags && 3 == userFlags.size());
            List<String> flags = List.of("my_fancy_flag", "myFancyFlag", "myfancyflag", PILE_OF_POO);
            for (String userFlag : userFlags) {
                assertTrue(flags.contains(userFlag));
            }
        }
    }

    @Test
    public void testClearUserFlags() throws Exception {
        {
            /*
             * Update mail with any user flags
             */
            List<String> flags = List.of("my_fancy_flag", "myFancyFlag", PILE_OF_POO);
            setFlags(flags);
            /*
             * Get mail and check that user flags are set
             */
            List<String> userFlags = getUserFlags();
            assertTrue(null != userFlags && 3 == userFlags.size());
            for (String userFlag : userFlags) {
                assertTrue(flags.contains(userFlag));
            }
        }
        {
            /*
             * Remove one user flag
             */
            clearFlags(List.of("my_fancy_flag"));
            /*
             * Check that other flags are still persisted
             */
            List<String> userFlags = getUserFlags();
            assertTrue(null != userFlags && 2 == userFlags.size());
            List<String> flags = List.of("myFancyFlag", PILE_OF_POO);
            for (String userFlag : userFlags) {
                assertTrue(flags.contains(userFlag));
            }
        }
        {
            /*
             * Call with no flags to clear
             */
            clearFlags(List.of(""));
            /*
             * Check that flags are still persisted
             */
            List<String> userFlags = getUserFlags();
            assertTrue(null != userFlags && 2 == userFlags.size());
            List<String> flags = List.of("myFancyFlag", PILE_OF_POO);
            for (String userFlag : userFlags) {
                assertTrue(flags.contains(userFlag));
            }
        }
        {
            /*
             * Remove all flags
             */
            clearFlags(List.of("myFancyFlag", PILE_OF_POO));
            /*
             * Check that all flags has been removed
             */
            List<String> userFlags = getUserFlags();
            assertTrue(null == userFlags || userFlags.isEmpty());
        }
    }

    @Test
    public void testSearchUserFlags() throws Exception {
        {
            /*
             * Update mail with any user flags
             */
            List<String> flags = List.of("my_fancy_flag", "myFancyFlag", PILE_OF_POO);
            setFlags(flags);
            /*
             * Get mail and check that user flags are set
             */
            List<String> userFlags = getUserFlags();
            assertTrue(null != userFlags && 3 == userFlags.size());
            for (String userFlag : userFlags) {
                assertTrue(flags.contains(userFlag));
            }
        }
        {
            /*
             * Create second mail
             */
            String mail = createEMail(testUser.getLogin(), "Subject 2", "Here is a little text for me.");
            String callback = mailApi.sendMailBuilder().withJson0(mail).execute();
            assertTrue(callback.contains("callback_new"));
            mailId = getMailId(mailApi, "Subject 2");
            List<String> userFlags = getUserFlags();
            assertTrue(null == userFlags || userFlags.isEmpty());
        }
        {
            /*
             * Search for dedicated user flag
             */
            List<List<Object>> searchResult = searchForMails(getSingleSearchTerm(PILE_OF_POO));
            assertTrue(searchResult.size() == 1);
            assertTrue(SUBJECT.equals(searchResult.get(0).get(3)));// Indices based on COLUMNS
        }
        {
            /*
             * Search for non existing user flag
             */
            List<List<Object>> searchResult = searchForMails(getSingleSearchTerm("pile_of_poo"));
            assertTrue(searchResult.isEmpty());
        }
    }

    /*
     * ============================== HELPERS ==============================
     */

    /**
     * Set the given user flags
     *
     * @param flags The user flags
     * @throws ApiException In case of error
     */
    private void setFlags(List<String> flags) throws ApiException {
        MailUpdateBody body = new MailUpdateBody();
        body.setUserFlags(flags);
        MailDestinationResponse mdResponse = mailApi.updateMailBuilder().withFolder(INBOX).withId(mailId).withMailUpdateBody(body).execute();
        checkResponse(mdResponse.getError(), mdResponse.getErrorDesc());
    }

    /**
     * Removes the given user flags
     *
     * @param flags The user flags
     * @throws ApiException In case of error
     */
    private void clearFlags(List<String> flags) throws ApiException {
        MailUpdateBody body = new MailUpdateBody();
        body.setClearUserFlags(flags);
        MailDestinationResponse mdResponse = mailApi.updateMailBuilder().withFolder(INBOX).withId(mailId).withMailUpdateBody(body).execute();
        checkResponse(mdResponse.getError(), mdResponse.getErrorDesc());
    }

    /**
     * Get the user flags from created mail in {@link #setUp(TestInfo)}
     *
     * @return The user flags of the mail as {@link List}
     * @throws ApiException In case of error
     */
    private List<String> getUserFlags() throws ApiException {
        return getUserFlags(mailApi, mailId);
    }

    /**
     * Get the user flags from requested mail
     *
     * @param mailApi The mail API to use
     * @param mailId The identifier of the mail to get the user flags from
     * @return The user flags of the mail as {@link List}
     * @throws ApiException In case of error
     */
    private static List<String> getUserFlags(MailApi mailApi, String mailId) throws ApiException {
        MailResponse response = mailApi.getMailBuilder().withFolder(INBOX).withId(mailId).execute();
        MailData mailData = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        return mailData.getUser();
    }

    /**
     * Search for mails based on the given search term
     *
     * @param searchTerm The search term to use
     * @return The mails
     * @throws Exception in case of error
     */
    private List<List<Object>> searchForMails(String searchTerm) throws Exception {
        MailsResponse searchResponse = mailApi.searchMails(INBOX, COLUMNS, searchTerm, null, null, null, "600", "desc");
        return checkResponse(searchResponse.getError(), searchResponse.getErrorDesc(), searchResponse.getData());
    }

    /**
     * Creates a JSON containing information to build a new mail
     *
     * @param recipient The recipient of the mail
     * @param subject The subject of the mail
     * @param text The text of the mail, used as-is
     * @return The JSON formatted mail as {@link String}
     * @throws Exception In case JSON can't be build
     */
    private String createEMail(String recipient, String subject, String text) throws Exception {
        return createEMail(recipient, subject, MailContentType.ALTERNATIVE.toString(), text);
    }

    /**
     * Creates a JSON containing information to build a new mail
     *
     * @param recipient The recipient of the mail
     * @param subject The subject of the mail
     * @param contentType The content type of the mail
     * @param text The text of the mail, used as-is
     * @return The JSON formatted mail as {@link String}
     * @throws Exception In case JSON can't be build
     */
    private String createEMail(String recipient, String subject, String contentType, String text) throws Exception {
        JSONObject mail = new JSONObject();
        mail.put(MailJSONField.FROM.getKey(), testUser.getLogin());
        mail.put(MailJSONField.RECIPIENT_TO.getKey(), recipient);
        mail.put(MailJSONField.RECIPIENT_CC.getKey(), "");
        mail.put(MailJSONField.RECIPIENT_BCC.getKey(), "");
        mail.put(MailJSONField.SUBJECT.getKey(), subject);
        mail.put(MailJSONField.PRIORITY.getKey(), "3");

        JSONObject mailBody = new JSONObject();
        mailBody.put(MailJSONField.CONTENT_TYPE.getKey(), contentType);
        mailBody.put(MailJSONField.CONTENT.getKey(), text);

        JSONArray attachments = new JSONArray();
        attachments.put(mailBody);
        mail.put(MailJSONField.ATTACHMENTS.getKey(), attachments);
        return mail.toString();
    }

    /**
     * Get the mail fitting to the supplied subject in the INBOX
     *
     * @param mailApi The {@link MailApi} to use
     * @param subject The subject to find
     * @return The mail
     * @throws Exception If mail is not found
     */
    private static String getMailId(MailApi mailApi, String subject) throws Exception {
        int maxRetries = 5;
        String mailId = null;
        while (maxRetries > 0 && mailId == null) {
            maxRetries--;
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            MailsResponse mailsResponse = mailApi.getAllMails(INBOX, COLUMNS, null, Boolean.FALSE, Boolean.FALSE, "600", "desc", null, null, I(100), null);
            List<List<Object>> data = checkResponse(mailsResponse.getError(), mailsResponse.getErrorDesc(), mailsResponse.getData());
            if (data.isEmpty()) {
                continue;
            }
            for (List<Object> singleMailData : data) {
                // Indices based on COLUMNS
                if (subject.equals(singleMailData.get(3))) {
                    mailId = singleMailData.get(1).toString();
                    break;
                }
            }
        }
        assertThat("No mail found", mailId, notNullValue());
        return mailId;
    }

    /**
     * Generates a search term for a single user flag to search
     *
     * @param userFlag The user flag to search
     * @return The search term as {@link String}
     * @throws Exception In case JSON for search term can't be build
     */
    private static String getSingleSearchTerm(String userFlag) throws Exception {
        JSONArray filter = new JSONArray(3);
        filter.add(0, "=");
        JSONObject field = new JSONObject(1);
        field.put("field", "user_flags");
        filter.add(1, field);
        filter.add(2, userFlag);
        JSONObject search = new JSONObject(1);
        search.put("filter", filter);
        return search.toString();
    }

}
