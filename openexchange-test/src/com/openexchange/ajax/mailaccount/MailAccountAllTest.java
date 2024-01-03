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

package com.openexchange.ajax.mailaccount;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.mailaccount.actions.MailAccountAllRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountAllResponse;
import com.openexchange.exception.OXException;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.MailAccountDescription;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MailAccountAllTest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class MailAccountAllTest extends AbstractMailAccountTest {

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        createMailAccount();
    }

    @Test
    public void testAllShouldNotIncludePassword() throws OXException, IOException, JSONException {
        int[] fields = new int[] { Attribute.ID_LITERAL.getId(), Attribute.PASSWORD_LITERAL.getId() };
        MailAccountAllResponse response = getClient().execute(new MailAccountAllRequest(fields));

        List<MailAccountDescription> descriptions = response.getDescriptions();
        assertFalse(descriptions.isEmpty());

        boolean found = false;
        for (MailAccountDescription description : descriptions) {
            if (description.getId() == mailAccountDescription.getId()) {
                assertTrue(null == description.getPassword(), "Password was not null");
                found = true;
            }
        }
        assertTrue(found, "Did not find mail account in response");
    }
}
