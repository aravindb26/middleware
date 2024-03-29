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

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.mail.actions.NewMailRequest;

/**
 * {@link TrainTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
@Disabled
public class TrainTest extends AbstractMailCategoriesTest {

    @Test
    public void testTrain() throws Exception {
        getClient().execute(new NewMailRequest(getInboxFolder(), EML, -1, true));
        String origin = values.getInboxFolder();
        train(CAT_1, getSendAddress(), FALSE, TRUE);

        List<List<Object>> mails = getAllMails(origin, CAT_GENERAL);
        assertTrue(mails.size() == 1, "General category should still contain the old email!");
        mails = getAllMails(origin, CAT_1);
        assertTrue(mails.size() == 0, "Category 1 should contain no email!");
        getClient().execute(new NewMailRequest(null, EML, -1, true));
        mails = getAllMails(origin, CAT_GENERAL);
        assertTrue(mails.size() == 1, "General category should still contain only the old email!");
        mails = getAllMails(origin, CAT_1);
        assertTrue(mails.size() == 1, "Category 1 should now contain the new mail!");
    }

    @Test
    public void testReorganize() throws Exception {
        getClient().execute(new NewMailRequest(getInboxFolder(), EML, -1, true));
        String origin = values.getInboxFolder();
        train(CAT_1, getSendAddress(), TRUE, FALSE);
        List<List<Object>> mails = getAllMails(origin, CAT_GENERAL);
        assertTrue(mails.size() == 0, "General category should contain no mails now!");
        mails = getAllMails(origin, CAT_1);
        assertTrue(mails.size() == 1, "Category 1 should contain the mail now!");
    }

    @Test
    public void testDuplicateTrain() throws Exception {
        getClient().execute(new NewMailRequest(getInboxFolder(), EML, -1, true));
        String origin = values.getInboxFolder();
        train(CAT_1, getSendAddress(), FALSE, TRUE);
        train(CAT_2, getSendAddress(), FALSE, TRUE);
        List<List<Object>> mails = getAllMails(origin, CAT_GENERAL);
        assertTrue(mails.size() == 1, "General category should still contain the old email!");
        mails = getAllMails(origin, CAT_1);
        assertTrue(mails.size() == 0, "Category 1 should contain no email!");
        mails = getAllMails(origin, CAT_2);
        assertTrue(mails.size() == 0, "Category 2 should contain no email!");
        getClient().execute(new NewMailRequest(null, EML, -1, true));
        mails = getAllMails(origin, CAT_GENERAL);
        assertTrue(mails.size() == 1, "General category should still contain only the old email!");
        mails = getAllMails(origin, CAT_1);
        assertTrue(mails.size() == 0, "Category 1 should contain no email!");
        mails = getAllMails(origin, CAT_2);
        assertTrue(mails.size() == 1, "Category 2 should contain the new mail!");
    }
}
