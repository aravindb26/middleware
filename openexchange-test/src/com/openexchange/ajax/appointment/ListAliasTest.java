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

package com.openexchange.ajax.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Date;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.ajax.appointment.action.AllRequest;
import com.openexchange.ajax.appointment.action.AppointmentInsertResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.appointment.action.ListRequest;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.CommonListResponse;
import com.openexchange.ajax.framework.ListIDs;
import com.openexchange.groupware.container.Appointment;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link ListAliasTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class ListAliasTest extends AppointmentTest {

    private AJAXClient client;

    private Appointment appointment;

    /**
     * Initializes a new {@link ListAliasTest}.
     *
     * @param name
     */
    public ListAliasTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getClient();
    }

    @Test
    public void testAll() throws Throwable {
        appointment = new Appointment();
        appointment.setStartDate(new Date());
        appointment.setEndDate(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 2));
        appointment.setIgnoreConflicts(true);
        appointment.setTitle("All Alias Test Appointment");
        appointment.setParentFolderID(client.getValues().getPrivateAppointmentFolder());
        final InsertRequest insertRequest = new InsertRequest(appointment, client.getValues().getTimeZone());
        final AppointmentInsertResponse insertResponse = client.execute(insertRequest);
        insertResponse.fillObject(appointment);

        final AllRequest allRequest = new AllRequest(client.getValues().getPrivateAppointmentFolder(), new int[] { 20, 1 }, new Date(0), new Date(Long.MAX_VALUE), client.getValues().getTimeZone());
        final CommonAllResponse allResponse = client.execute(allRequest);
        final ListIDs ids = allResponse.getListIDs();

        final ListRequest aliasRequest = new ListRequest(ids, "list");
        final CommonListResponse aliasResponse = client.execute(aliasRequest);
        final Object[][] aliasAppointments = aliasResponse.getArray();

        final ListRequest request = new ListRequest(ids, new int[] { 1, 20, 207, 206, 2, 200, 201, 202, 203, 209, 221, 401, 402, 102, 400, 101, 220, 215, 100 });
        final CommonListResponse response = client.execute(request);
        final Object[][] appointments = response.getArray();

        assertEquals(aliasAppointments.length, appointments.length, "Arrays' sizes are not equal.");
        for (int i = 0; i < aliasAppointments.length; i++) {
            final Object[] o1 = aliasAppointments[i];
            final Object[] o2 = appointments[i];
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
