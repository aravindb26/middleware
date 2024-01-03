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

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.mail.TestMail;
import com.openexchange.ajax.mail.actions.NewMailRequest;
import com.openexchange.testing.httpclient.models.MailCategoriesMoveBody;

/**
 * {@link AllRequestCategoryParameterTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public class AllRequestCategoryParameterTest extends AbstractMailCategoriesTest {

    @Test
    public void testAllRequest() throws Exception {
        getClient().execute(new NewMailRequest(getInboxFolder(), EML, -1, true));
        String origin = values.getInboxFolder();

        List<List<Object>> mails = getAllMails(origin, CAT_GENERAL);
        assertTrue(mails.size() == 1, "General category should contain the old mail.");
        mails = getAllMails(origin, CAT_1);
        assertTrue(mails.size() == 0, "Category 1 should contain no email.");

        TestMail mail = new TestMail(getFirstMailInFolder(origin));
        MailCategoriesMoveBody body = new MailCategoriesMoveBody();
        body.folderId(mail.getFolder()).id(mail.getId());
        categoriesApi.moveMails(CAT_1, Collections.singletonList(body));

        mails = getAllMails(origin, CAT_GENERAL);

        assertTrue(mails.size() == 0, "General category should contain the old mail.");
        mails = getAllMails(origin, CAT_1);
        assertTrue(mails.size() == 1, "Category 1 should contain no email.");
    }

}
