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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.action.ContactUpdatesResponse;
import com.openexchange.ajax.contact.action.GetRequest;
import com.openexchange.ajax.contact.action.GetResponse;
import com.openexchange.ajax.contact.action.InsertRequest;
import com.openexchange.ajax.contact.action.InsertResponse;
import com.openexchange.ajax.contact.action.UpdatesRequest;
import com.openexchange.ajax.fields.ContactFields;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.Order;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug13960Test}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class Bug13960Test extends AbstractAJAXSession {

    private static final int[] COLUMNS = { Contact.OBJECT_ID, Contact.DEFAULT_ADDRESS, Contact.FILE_AS, Contact.NUMBER_OF_IMAGES };

    private TimeZone timeZone;
    private Contact contact;

    public Bug13960Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        timeZone = getClient().getValues().getTimeZone();
        contact = new Contact();
        contact.setParentFolderID(getClient().getValues().getPrivateContactFolder());
        InsertResponse response = getClient().execute(new InsertRequest(contact));
        response.fillObject(contact);
    }

    @Test
    public void testJSONValues() throws Throwable {
        {
            GetRequest request = new GetRequest(contact, timeZone);
            GetResponse response = getClient().execute(request);
            JSONObject json = (JSONObject) response.getData();
            assertFalse(json.has(ContactFields.DEFAULT_ADDRESS), "'Default address' should not be contained if not set.");
            assertFalse(json.has(ContactFields.FILE_AS), "'File as should' not be contained if not set.");
            assertTrue(json.has(ContactFields.NUMBER_OF_IMAGES), "'Number of images' should be contained always.");
            assertEquals(0, json.getInt(ContactFields.NUMBER_OF_IMAGES), "'Number of images' should be zero.");
        }
        {
            UpdatesRequest request = new UpdatesRequest(contact.getParentFolderID(), COLUMNS, 0, Order.ASCENDING, new Date(contact.getLastModified().getTime() - 1));
            ContactUpdatesResponse response = getClient().execute(request);
            int row = 0;
            while (row < response.size()) {
                if (response.getValue(row, Contact.OBJECT_ID).equals(Integer.toString(contact.getObjectID()))) {
                    break;
                }
                row++;
            }
            JSONArray array = ((JSONArray) response.getData()).getJSONArray(row);
            int defaultAddressPos = response.getColumnPos(Contact.DEFAULT_ADDRESS);
            assertEquals(JSONObject.NULL, array.get(defaultAddressPos), "Default address should not be contained if not set.");
            int fileAsPos = response.getColumnPos(Contact.FILE_AS);
            assertEquals(JSONObject.NULL, array.get(fileAsPos));
            int numberOfImagesPos = response.getColumnPos(Contact.NUMBER_OF_IMAGES);
            assertEquals(0, array.getInt(numberOfImagesPos), "'Number of images' should be zero.");
        }
    }
}
