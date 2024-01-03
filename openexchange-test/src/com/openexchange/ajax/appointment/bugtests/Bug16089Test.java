
package com.openexchange.ajax.appointment.bugtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.appointment.action.AppointmentInsertResponse;
import com.openexchange.ajax.appointment.action.GetRequest;
import com.openexchange.ajax.appointment.action.GetResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.participants.ConfirmableParticipant;
import com.openexchange.preferences.ServerUserSetting;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.FolderTestManager;
import org.junit.jupiter.api.TestInfo;

public class Bug16089Test extends AbstractAJAXSession {

    private FolderTestManager manager;


    FolderObject folderObject1;

    Appointment appointment;

    TimeZone timezone;

    Calendar cal;

    public Bug16089Test() {
        super();

    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        manager = new FolderTestManager(getClient());
        timezone = getClient().getValues().getTimeZone();
        cal = Calendar.getInstance(timezone);

        // create a folder
        folderObject1 = new FolderObject();
        folderObject1.setFolderName("Bug16089Testfolder");
        folderObject1.setType(FolderObject.PUBLIC);
        folderObject1.setParentFolderID(FolderObject.SYSTEM_PUBLIC_FOLDER_ID);
        folderObject1.setModule(FolderObject.CALENDAR);
        // create permissions
        final OCLPermission perm1 = new OCLPermission();
        perm1.setEntity(getClient().getValues().getUserId());
        perm1.setGroupPermission(false);
        perm1.setFolderAdmin(true);
        perm1.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
        folderObject1.setPermissionsAsArray(new OCLPermission[] { perm1 });
        manager.insertFolderOnServer(folderObject1);

        appointment = createAppointment();
    }

    @Test
    public void testConfirmation() throws Exception {
        GetResponse getAppointmentResp = getClient().execute(new GetRequest(appointment));
        Appointment testApp = getAppointmentResp.getAppointment(timezone);

        Participant[] participants = testApp.getParticipants();
        boolean found = false;
        for (Participant p : participants) {
            if (p.getIdentifier() == getClient().getValues().getUserId()) {
                found = true;
                ConfirmableParticipant[] confirmations = testApp.getConfirmations();
                for (ConfirmableParticipant c : confirmations) {
                    if (c.getIdentifier() == getClient().getValues().getUserId()) {
                        int ctx = getContextID();
                        int publicConfig = ServerUserSetting.getInstance().getDefaultStatusPublic(ctx, getClient().getValues().getUserId()).intValue();
                        assertEquals(c.getConfirm(), publicConfig, "Confirm status isn't equal with user setting.");
                    }
                }
            }
        }

        if (!found) {
            fail("User not found as Participant");
        }
    }

    private int getContextID() throws IOException, OXException, JSONException {
        return getClient().getValues().getContextId();
    }

    private Appointment createAppointment() throws Exception {
        Calendar cal = (Calendar) this.cal.clone();
        cal.add(Calendar.HOUR_OF_DAY, 1);

        Appointment app = new Appointment();
        app.setTitle("Bug16089Appointment");
        app.setIgnoreConflicts(true);
        app.setStartDate(cal.getTime());
        cal.add(Calendar.HOUR_OF_DAY, 1);
        app.setEndDate(cal.getTime());
        app.setParentFolderID(folderObject1.getObjectID());
        app.setRecurrenceType(Appointment.NO_RECURRENCE);

        InsertRequest insApp = new InsertRequest(app, timezone, false);
        AppointmentInsertResponse execute = getClient().execute(insApp);

        execute.fillAppointment(app);

        return app;
    }

}
