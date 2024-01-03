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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.mail.TestMail;
import com.openexchange.ajax.mail.actions.NewMailRequest;
import com.openexchange.exception.OXException;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.MailResponse;

/**
 * {@link MoveTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public class MoveTest extends AbstractMailCategoriesTest {

    @Test
    public void testShouldMoveToAnotherCategory() throws OXException, IOException, JSONException, ApiException {
        getClient().execute(new NewMailRequest(getInboxFolder(), EML, -1, true));
        String origin = values.getInboxFolder();
        TestMail myMail = new TestMail(getFirstMailInFolder(origin));
        String oldID = myMail.getId();
        CommonResponse resp = categoriesApi.moveMails(CAT_1, toBody(myMail.getFolder(), myMail.getId()));
        assertNull(resp.getError(), resp.getErrorDesc());
        MailResponse getResp = mailApi.getMail(origin, oldID, null, null, null, null, null, null, null, null, null, null, null, null);
        assertNull(getResp.getError(), "Should produce no errors when getting moved e-mail");
        assertTrue(getResp.getData().getUser().contains(CAT_1_FLAG), "Move operation failed. Mail does not contain the correct user flag.");
    }

    @Test
    public void testShouldNotMoveToNonExistentCategory() throws OXException, IOException, JSONException, ApiException {
        getClient().execute(new NewMailRequest(getInboxFolder(), EML, -1, true));
        String origin = values.getInboxFolder();
        TestMail myMail = new TestMail(getFirstMailInFolder(origin));
        String oldID = myMail.getId();
        CommonResponse resp = categoriesApi.moveMails("somebadcategoryidentifier", toBody(myMail.getFolder(), myMail.getId()));
        assertNotNull(resp.getError(), "Should produce errors when moving e-mail");
        MailResponse getResp = mailApi.getMail(origin, oldID, null, null, null, null, null, null, null, null, null, null, null, null);
        assertNull(getResp.getError(), "Should produce no errors when getting moved e-mail");
        assertTrue(getResp.getData().getUser().size() == 0, "Move operation failed. Mail does not contain the correct user flag.");

    }

}
