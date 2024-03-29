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

import static com.openexchange.test.common.groupware.calendar.TimeTools.D;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.UserValues;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.FolderTestManager;
import com.openexchange.test.TestManager;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MoveTestNew} This test describes the current status of the calendar implementation. It does not cover any user stories or expected
 * behaviours. This is just to ensure, no unintended side effects occur during calendar changes. It's subject to change, if the behaviour
 * should change.
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class MoveTestNew extends AbstractAppointmentTest {

    private AJAXClient clientC;

    private UserValues valuesC;

    private CalendarTestManager ctmB, ctmC;

    private FolderTestManager ftmB, ftmC;

    private FolderObject folderA, folderA1, folderB, folderB1, folderB2, folderC, folderC1;

    private final Set<TestManager> tm = new HashSet<TestManager>();

    private int idA, idB, idC;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        clientC = testContext.acquireUser().getAjaxClient();
        valuesC = clientC.getValues();

        idA = getClient().getValues().getUserId();
        idB = testUser2.getAjaxClient().getValues().getUserId();
        idC = valuesC.getUserId();

        ctmB = new CalendarTestManager(testUser2.getAjaxClient());
        ctmC = new CalendarTestManager(clientC);

        tm.add(ctmB);
        tm.add(ctmC);

        ftmB = new FolderTestManager(testUser2.getAjaxClient());
        ftmC = new FolderTestManager(clientC);

        tm.add(ftmB);
        tm.add(ftmC);

        for (TestManager manager : tm) {
            manager.setFailOnError(true);
        }

        folderA = ftm.getFolderFromServer(getClient().getValues().getPrivateAppointmentFolder());
        folderA1 = createPrivateFolder("SubfolderA1" + UUID.randomUUID().toString(), ftm, getClient());
        folderA1 = ftm.insertFolderOnServer(folderA1);

        //
        folderB = ftmB.getFolderFromServer(testUser2.getAjaxClient().getValues().getPrivateAppointmentFolder());
        addAuthorPermissions(folderB, getClient().getValues().getUserId(), ftmB);
        folderB1 = createPrivateFolder("SubfolderB1" + UUID.randomUUID().toString(), ftmB, testUser2.getAjaxClient(), getClient());
        folderB1 = ftmB.insertFolderOnServer(folderB1);
        folderB2 = createPrivateFolder("SubfolderB2" + UUID.randomUUID().toString(), ftmB, testUser2.getAjaxClient(), getClient());
        folderB2 = ftmB.insertFolderOnServer(folderB2);

        folderC = ftmC.getFolderFromServer(valuesC.getPrivateAppointmentFolder());
        addAuthorPermissions(folderC, getClient().getValues().getUserId(), ftmC);
        folderC1 = createPrivateFolder("SubfolderC1" + UUID.randomUUID().toString(), ftmC, clientC, getClient());
        folderC1 = ftmC.insertFolderOnServer(folderC1);
    }

    private void addAuthorPermissions(FolderObject folder, int userId, FolderTestManager actor) {
        OCLPermission authorPermissions = getAuthorPermissions(userId, folder.getObjectID());
        List<OCLPermission> newPermissions = new ArrayList<OCLPermission>();
        for (OCLPermission ocl : folder.getPermissions()) {
            if (ocl.getEntity() != userId) {
                newPermissions.add(ocl);
            }
        }
        newPermissions.add(authorPermissions);
        FolderObject folderUpdate = new FolderObject(folder.getObjectID());
        folderUpdate.setPermissions(newPermissions);
        folderUpdate.setLastModified(new Date(Long.MAX_VALUE));
        actor.updateFolderOnServer(folderUpdate);
    }

    @Test
    public void testOwnPrivateToSubfolder() throws Exception {
        Appointment app = generateAppointment("testOwnPrivateToSubfolder", folderA);
        catm.insert(app);
        Appointment loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(getClient().getValues().getUserId(), loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderA, folderA1, catm);
        loaded = get(app, folderA1, catm);
        assertEquals(folderA1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(getClient().getValues().getUserId(), loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOwnPrivateToSubfolderWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOwnPrivateToSubfolderWithParticipants", folderA, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB, ctmB);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderA, folderA1, catm);
        loaded = get(app, folderA1, catm);
        assertEquals(folderA1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB, ctmB);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");
    }

    @Test
    public void testOwnPrivateToOtherPrivate() throws Exception {
        Appointment app = generateAppointment("testOwnPrivateToOtherPrivate", folderA);
        catm.insert(app);
        Appointment loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(getClient().getValues().getUserId(), loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderA, folderB, catm);
        loaded = get(app, folderB, ctmB);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOwnPrivateToOtherPrivateWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOwnPrivateToOtherPrivateWithParticipants", folderA, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB, ctmB);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderA, folderB, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    @Test
    public void testOwnPrivateToOtherSubfolder() throws Exception {
        Appointment app = generateAppointment("testOwnPrivateToOtherSubfolder", folderA);
        catm.insert(app);
        Appointment loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(getClient().getValues().getUserId(), loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderA, folderB1, catm);

        loaded = get(app, folderB1, ctmB);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOwnPrivateToOtherSubfolderWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOwnPrivateToOtherSubfolderWithParticipants", folderA, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB, ctmB);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderA, folderB1, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    @Test
    public void testOtherPrivateToOwnPrivate() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOwnPrivate", folderB);
        catm.insert(app);
        Appointment loaded = get(app, folderB, catm);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB, folderA, catm);
        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idA, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherPrivateToOwnPrivateWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOwnPrivateWithParticipants", folderB, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB, catm);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB, folderA, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    @Test
    public void testOtherPrivateToOtherSubfolder() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOtherSubfolder", folderB, idB);
        catm.insert(app);
        Appointment loaded = get(app, folderB, catm);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB, folderB1, catm);
        loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherPrivateToOtherSubfolderWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOtherSubfolderWithParticipants", folderB, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB, catm);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB, ctmB);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB, folderB1, catm);
        loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB1, ctmB);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

    }

    @Test
    public void testOtherPrivateToThirdPartyPrivateWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOtherSubfolder", folderB, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB, catm);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB, ctmB);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB, folderC, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    @Test
    public void testOtherSubfolderToOwnPrivate() throws Exception {
        Appointment app = generateAppointment("testOtherSubfolderToOwnPrivate", folderB1);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB1, folderA, catm);
        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idA, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherSubfolderToOwnSubfolder() throws Exception {
        Appointment app = generateAppointment("testOtherSubfolderToOwnPrivate", folderB1);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB1, folderA1, catm);
        loaded = get(app, folderA1, catm);
        assertEquals(folderA1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idA, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherSubfolderToOwnPrivateWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOwnPrivateWithParticipants", folderB1, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB1, folderA, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    @Test
    public void testOtherSubfolderToOwnSubfolderWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOwnPrivateWithParticipants", folderB1, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB1, folderA1, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    @Test
    public void testOtherSubfolderToOtherSubfolder() throws Exception {
        Appointment app = generateAppointment("testOtherSubfolderToOwnPrivate", folderB1);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        loaded = get(app, folderB1, ctmB);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB1, folderB2, catm);
        loaded = get(app, folderB2, catm);
        assertEquals(folderB2.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        loaded = get(app, folderB2, ctmB);
        assertEquals(folderB2.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherSubfolderToOtherSubfolderWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherSubfolderToOtherSubfolderWithParticipants", folderB1, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB1, ctmB);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB1, folderB2, catm);
        loaded = get(app, folderB2, catm);
        assertEquals(folderB2.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB2, ctmB);
        assertEquals(folderB2.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");
    }

    @Test
    public void testOtherPrivateToThirdPartySubfolder() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToThirdPartySubfolder", folderB, idB);
        catm.insert(app);
        Appointment loaded = get(app, folderB, catm);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB, folderC1, catm);
        loaded = get(app, folderC1, catm);
        assertEquals(folderC1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idC, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherSubfolderToThirdPartySubfolder() throws Exception {
        Appointment app = generateAppointment("testOtherSubfolderToThirdPartySubfolder", folderB1);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        loaded = get(app, folderB1, ctmB);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB1, folderC1, catm);
        loaded = get(app, folderC1, catm);
        assertEquals(folderC1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idC, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherPrivateToThirdPartySubfolderWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToThirdPartySubfolderWithParticipants", folderB, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB, catm);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB, ctmB);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB, folderC1, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    @Test
    public void testOtherSubfolderToThirdPartyPrivate() throws Exception {
        Appointment app = generateAppointment("testOtherSubfolderToThirdPartyPrivate", folderB1);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        loaded = get(app, folderB1, ctmB);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB1, folderC, catm);
        loaded = get(app, folderC, catm);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idC, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherPrivateToThirdPartyPrivate() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOtherSubfolder", folderB, idB);
        catm.insert(app);
        Appointment loaded = get(app, folderB, catm);
        assertEquals(folderB.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idB, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");

        move(app, folderB, folderC, catm);
        loaded = get(app, folderC, catm);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(1, loaded.getParticipants().length, "Wrong amount of participants.");
        assertEquals(idC, loaded.getParticipants()[0].getIdentifier(), "Wrong participant.");
    }

    @Test
    public void testOtherSubfolderToThirdPartySubfolderWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherPrivateToOtherSubfolder", folderB1, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB1, ctmB);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB1, folderC1, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    @Test
    public void testOtherSubfolderToThirdPartyPrivateWithParticipants() throws Exception {
        Appointment app = generateAppointment("testOtherSubfolderToThirdPartyPrivateWithParticipants", folderB1, getClient().getValues().getUserId(), idB, idC);
        catm.insert(app);
        Appointment loaded = get(app, folderB1, catm);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderA, catm);
        assertEquals(folderA.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderB1, ctmB);
        assertEquals(folderB1.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        loaded = get(app, folderC, ctmC);
        assertEquals(folderC.getObjectID(), loaded.getParentFolderID(), "Wrong folder id.");
        assertEquals(3, loaded.getParticipants().length, "Wrong amount of participants.");

        move(app, folderB1, folderC, catm);
        assertNotNull(catm.getLastResponse().getException(), "No exception");
    }

    private Appointment get(Appointment app, FolderObject inFolder, CalendarTestManager actor) throws Exception {
        return actor.get(inFolder.getObjectID(), app.getObjectID(), true);
    }

    private void move(Appointment app, FolderObject from, FolderObject to, CalendarTestManager ctm) {
        Appointment appointmentUpdate = new Appointment();
        appointmentUpdate.setObjectID(app.getObjectID());
        appointmentUpdate.setParentFolderID(to.getObjectID());
        appointmentUpdate.setLastModified(app.getLastModified());
        ctm.update(from.getObjectID(), appointmentUpdate);
    }

    private FolderObject createPrivateFolder(String name, FolderTestManager ftm, AJAXClient... client) throws Exception {
        FolderObject folder = ftm.generatePrivateFolder(name, FolderObject.CALENDAR, client[0].getValues().getPrivateAppointmentFolder(), client[0].getValues().getUserId());

        if (client.length > 1) {
            for (int i = 1; i < client.length; i++) {
                folder.addPermission(getAuthorPermissions(client[i].getValues().getUserId(), folder.getObjectID()));
            }
        }

        return folder;
    }

    private OCLPermission getAuthorPermissions(int userId, int folderId) {
        OCLPermission permissions = new OCLPermission();
        permissions.setEntity(userId);
        permissions.setGroupPermission(false);
        permissions.setFolderAdmin(false);
        permissions.setFuid(folderId);
        permissions.setAllPermission(OCLPermission.CREATE_SUB_FOLDERS, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);

        return permissions;
    }

    private Appointment generateAppointment(String title, FolderObject folder, int... userIds) {
        Appointment retval = new Appointment();
        retval.setTitle(title);
        retval.setStartDate(D("01.09.2014 08:00"));
        retval.setEndDate(D("01.09.2014 09:00"));
        retval.setParentFolderID(folder.getObjectID());
        retval.setIgnoreConflicts(true);
        if (userIds != null && userIds.length > 0) {
            for (int userId : userIds) {
                retval.addParticipant(new UserParticipant(userId));
            }
        }

        return retval;
    }
}
