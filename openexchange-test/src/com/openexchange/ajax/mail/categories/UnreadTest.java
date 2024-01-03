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
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.mail.MailTestManager;
import com.openexchange.ajax.mail.actions.NewMailRequest;

/**
 * {@link UnreadTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public class UnreadTest extends AbstractMailCategoriesTest {

    @Test
    public void testUnreadCount() throws Exception {
        MailTestManager manager = new MailTestManager(getClient(), false);
        getClient().execute(new NewMailRequest(getInboxFolder(), EML, -1, true));
        assertTrue(manager.getUnreadCount(CAT_GENERAL) == 1, "Unread count is not 1.");
        assertTrue(manager.getUnreadCount(CAT_1) == 0, "Unread count is not 0.");
        assertTrue(manager.getUnreadCount(CAT_2) == 0, "Unread count is not 0.");
    }

}
