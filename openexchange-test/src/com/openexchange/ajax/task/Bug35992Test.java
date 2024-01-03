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

package com.openexchange.ajax.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.TimeZone;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.task.actions.InsertRequest;
import com.openexchange.ajax.task.actions.InsertResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.groupware.tasks.TaskExceptionCode;
import org.junit.jupiter.api.TestInfo;

/**
 * Verifies that external participants can not be created with empty email address.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @since 7.6.2
 */
public final class Bug35992Test extends AbstractAJAXSession {

    private AJAXClient client1;
    private TimeZone timeZone;
    private Task task;

    public Bug35992Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client1 = getClient();
        timeZone = client1.getValues().getTimeZone();
        task = new Task();
        task.setParentFolderID(client1.getValues().getPrivateTaskFolder());
        task.setTitle("Test for bug 35992");
        ExternalUserParticipant participant = new ExternalUserParticipant("");
        participant.setDisplayName("");
        task.addParticipant(participant);
    }

    @Test
    public void testForExternalParticipantWithEmpyEMail() throws OXException, IOException, JSONException {
        InsertResponse response = client1.execute(new InsertRequest(task, timeZone, false));
        if (!response.hasError()) {
            response.fillTask(task);
        }
        assertTrue(response.hasError(), "Creating task with external participants having an empty email address should not be possible.");
        assertTrue(TaskExceptionCode.EXTERNAL_WITHOUT_MAIL.create().similarTo(response.getException()), "Did not get expected exception about external participant with empty email address.");
    }
}
