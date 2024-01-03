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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.util.Date;
import javax.mail.internet.MailDateFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.mail.actions.NewMailRequest;
import com.openexchange.ajax.mail.actions.SearchRequest;
import com.openexchange.ajax.mail.actions.SearchResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.search.Order;
import com.openexchange.mail.MailSortField;

/**
 * {@link SearchTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="karsten.will@open-xchange.com">Karsten Will</a>
 */
public final class SearchTest extends AbstractMailTest {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SearchTest.class);

    /**
     * Tests the <code>action=search</code> request on INBOX folder
     *
     * @throws Throwable
     */
    @Test
    public void testSimpleSearch() throws Throwable {
        /*
         * Insert <numOfMails> mails through a send request
         */
        final int numOfMails = 25;
        LOG.debug("Sending " + numOfMails + " mails to fill emptied INBOX");
        // @formatter:off
        final String eml = "Message-Id: <4A001517.4650.0059.1@foobar.com>\n"
                         + "Date: Tue, 05 May 2009 11:37:58 -0500\n"
                         + "From: " + getSendAddress() + "\n"
                         + "To: " + getSendAddress() + "\n"
                         + "Subject: testSimpleSearch\n"
                         + "Mime-Version: 1.0\n"
                         + "Content-Type: text/plain; charset=\"UTF-8\"\n"
                         + "Content-Transfer-Encoding: 8bit\n" + "\n"
                         + "This is a MIME message. If you are reading this text, you may want to \n"
                         + "consider changing to a mail reader or gateway that understands how to \n"
                         + "properly handle MIME multipart messages.";
        // @formatter:on
        for (int i = 0; i < numOfMails; i++) {
            getClient().execute(new NewMailRequest(getClient().getValues().getInboxFolder(), eml, -1, true));
            LOG.debug("Appended " + (i + 1) + ". mail of " + numOfMails);
        }
        /*
         * Perform search request
         */
        final JSONArray searchExpression = new JSONArray("['=', {'field':'subject'}, 'testSimpleSearch']");
        final JSONObject searchObject = new JSONObject().put("filter", searchExpression);

        final SearchResponse searchR = Executor.execute(getSession(), new SearchRequest(searchObject, getInboxFolder(), COLUMNS_DEFAULT_LIST, MailSortField.RECEIVED_DATE.getField(), Order.DESCENDING, true));
        if (searchR.hasError()) {
            fail(searchR.getException().toString());
        }

        final JSONArray array = (JSONArray) searchR.getData();
        assertEquals(numOfMails, array.length(), "Unexpected number of search results.");
    }

    /**
     * Tests the <code>action=search</code> request on INBOX folder
     *
     * @throws Throwable
     */
    @Test
    public void testComplexSearch() throws Throwable {
        /*
         * Insert <numOfMails> mails through a send request
         */
        final int numOfMails = 25;
        LOG.debug("Sending " + numOfMails + " mails to fill emptied INBOX");
        // @formatter:off
        final String eml = "Message-Id: <4A002517.4650.0059.1@foobar.com>\n"
                         + "Date: Tue, 06 May 2009 11:37:58 -0500\n"
                         + "From: " + getSendAddress() + "\n"
                         + "To: " + getSendAddress() + "\n"
                         + "Subject: testComplexSearch\n"
                         + "Mime-Version: 1.0\n"
                         + "Content-Type: text/plain; charset=\"UTF-8\"\n"
                         + "Content-Transfer-Encoding: 8bit\n" + "\n"
                         + "This is a MIME message. If you are reading this text, you may want to \n"
                         + "consider changing to a mail reader or gateway that understands how to \n"
                         + "properly handle MIME multipart messages.";
        final String eml2 = "Message-Id: <4A002517.4650.0059.1@foobar.com>\n"
                          + "Date: Tue, 06 May 2009 11:37:58 -0500\n"
                          + "From: " + getSendAddress() + "\n"
                          + "To: " + getSendAddress() + "\n"
                          + "Subject: testComplexSearch2\n"
                          + "Mime-Version: 1.0\n"
                          + "Content-Type: text/plain; charset=\"UTF-8\"\n"
                          + "Content-Transfer-Encoding: 8bit\n" + "\n"
                          + "This is a MIME message. If you are reading this text, you may want to \n"
                          + "consider changing to a mail reader or gateway that understands how to \n"
                          + "properly handle MIME multipart messages.";
        // @formatter:on
        for (int i = 0; i < numOfMails; i++) {
            if (i % 2 == 0) {
                getClient().execute(new NewMailRequest(getClient().getValues().getInboxFolder(), eml, -1, true));
            } else {
                getClient().execute(new NewMailRequest(getClient().getValues().getInboxFolder(), eml2, -1, true));
            }
            LOG.debug("Appended " + (i + 1) + ". mail of " + numOfMails);
        }
        /*
         * Perform search request
         */
        final JSONArray searchExpression1 = new JSONArray("['=', {'field':'subject'}, 'testComplexSearch']");
        final JSONArray searchExpression2 = new JSONArray("['=', {'field':'subject'}, 'testComplexSearch2']");
        final JSONArray searchExpression = new JSONArray("['or', " + searchExpression1 + "," + searchExpression2 + "]");

        final JSONObject searchObject = new JSONObject().put("filter", searchExpression);

        final SearchResponse searchR = Executor.execute(getSession(), new SearchRequest(searchObject, getInboxFolder(), COLUMNS_DEFAULT_LIST, MailSortField.RECEIVED_DATE.getField(), Order.DESCENDING, true));
        if (searchR.hasError()) {
            fail(searchR.getException().toString());
        }

        final JSONArray array = (JSONArray) searchR.getData();
        assertEquals(numOfMails, array.length(), "Unexpected number of search results.");
    }

    @Test
    public void testNestedComplexSearch() throws OXException, IOException, JSONException {
        /*
         * Insert <numOfMails> mails through a send request
         */
        final int numOfMails = 25;
        LOG.debug("Sending " + numOfMails + " mails to fill emptied INBOX");

        MailDateFormat mdf = new MailDateFormat();
        mdf.setTimeZone(getTimeZone());
        long now = System.currentTimeMillis();
        Date dnow = new Date(now);
        // @formatter:off
        final String eml = "Message-Id: <4A002517.4650.1059.1@foobar.com>\n"
                         + "Date: " + mdf.format(dnow) + "\n"
                         + "From: " + getSendAddress() + "\n"
                         + "To: " + getSendAddress() + "\n"
                         + "Subject: testNestedComplexSearch\n"
                         + "Mime-Version: 1.0\n"
                         + "Content-Type: text/plain; charset=\"UTF-8\"\n"
                         + "Content-Transfer-Encoding: 8bit\n" + "\n"
                         + "This is a MIME message. If you are reading this text, you may want to \n"
                         + "consider changing to a mail reader or gateway that understands how to \n"
                         + "properly handle MIME multipart messages.";
        // @formatter:on
        for (int i = 0; i < numOfMails; i++) {
            getClient().execute(new NewMailRequest(getClient().getValues().getInboxFolder(), eml, -1, true));
            LOG.debug("Appended " + (i + 1) + ". mail of " + numOfMails);
        }
        /*
         * Perform search request
         */
        Date recDate = DateTimeUtil.incrementDate(getTimeZone(), dnow, -2);
        JSONArray searchExpression = new JSONArray("['and', ['>',{'field':'received_date'}, '" + recDate.getTime() + "'], ['not', ['=', {'field':'flags'},'32']], ['=', {'field':'subject'},'testNestedComplexSearch']]");
        JSONObject searchObject = new JSONObject().put("filter", searchExpression);

        SearchResponse searchR = Executor.execute(getSession(), new SearchRequest(searchObject, getInboxFolder(), COLUMNS_DEFAULT_LIST, MailSortField.RECEIVED_DATE.getField(), Order.DESCENDING, true));
        if (searchR.hasError()) {
            fail(searchR.getException().toString());
        }

        JSONArray array = (JSONArray) searchR.getData();
        assertEquals(numOfMails, array.length(), "Unexpected number of search results.");
    }

}
