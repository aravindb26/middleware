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

package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.ContactTest;
import com.openexchange.ajax.contact.action.AllRequest;
import com.openexchange.ajax.contact.action.ListRequest;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.CommonListResponse;
import com.openexchange.ajax.framework.ListIDs;

/**
 * {@link ListAliasTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class ListAliasTest extends ContactTest {

    @Test
    public void testListAlias() throws Exception {
        final AllRequest allRequest = new AllRequest(getClient().getValues().getPrivateContactFolder(), new int[] { 20, 1 });
        final CommonAllResponse allResponse = getClient().execute(allRequest);
        final ListIDs ids = allResponse.getListIDs();

        final ListRequest aliasRequest = new ListRequest(ids, "list");
        final CommonListResponse aliasResponse = getClient().execute(aliasRequest);
        final Object[][] aliasContacts = aliasResponse.getArray();

        final ListRequest request = new ListRequest(ids, new int[] { 20, 1, 5, 2, 500, 501, 502, 505, 523, 525, 526, 527, 542, 555, 102, 602, 592, 101, 551, 552, 543, 547, 548, 549, 556, 569 });
        final CommonListResponse response = getClient().execute(request);
        final Object[][] contacts = response.getArray();

        assertEquals(aliasContacts.length, contacts.length, "Arrays' sizes are not equal.");
        for (int i = 0; i < aliasContacts.length; i++) {
            final Object[] o1 = aliasContacts[i];
            final Object[] o2 = contacts[i];
            assertEquals(o1.length, o2.length, "Objects' sizes are not equal.");
            for (int j = 0; j < o1.length; j++) {
                if ((o1[j] != null || o2[j] != null)) {
                    if (!(o1[j] instanceof JSONArray) && !(o2[j] instanceof JSONArray)) {
                        assertEquals(o1[j], o2[j], "Array[" + i + "][" + j + "] not equal.");
                    } else {
                        compareArrays((JSONArray) o1[j], (JSONArray) o2[j]);
                    }
                }
            }
        }
    }

    private void compareArrays(final JSONArray o1, final JSONArray o2) throws Exception {
        if (o1.length() != o2.length()) {
            fail("Arrays' sizes are not equal.");
        }
        for (int i = 0; i < o1.length(); i++) {
            if ((o1.get(i) != null || o2.get(i) != null)) {
                if (!(o1.get(i) instanceof JSONArray) && !(o2.get(i) instanceof JSONArray)) {
                    assertEquals(o1.get(i).toString(), o2.get(i).toString(), "Array[" + i + "] not equal.");
                } else {
                    compareArrays((JSONArray) o1.get(i), (JSONArray) o2.get(i));
                }
            }
        }
    }

}
