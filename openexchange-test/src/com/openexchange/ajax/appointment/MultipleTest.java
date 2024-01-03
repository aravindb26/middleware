
package com.openexchange.ajax.appointment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.ajax.appointment.action.AppointmentInsertResponse;
import com.openexchange.ajax.appointment.action.DeleteRequest;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.framework.AJAXRequest;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.ajax.framework.CommonInsertResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.framework.MultipleRequest;
import com.openexchange.ajax.framework.MultipleResponse;
import com.openexchange.groupware.container.Appointment;

public class MultipleTest extends AppointmentTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testMultipleInsert() throws Exception {
        final Appointment appointmentObj = createAppointmentObject("testMultipleInsert");
        appointmentObj.setIgnoreConflicts(true);

        final InsertRequest insertRequest1 = new InsertRequest(appointmentObj, timeZone, true);
        final InsertRequest insertRequest2 = new InsertRequest(appointmentObj, timeZone, true);
        final InsertRequest insertRequest3 = new InsertRequest(appointmentObj, timeZone, true);

        final MultipleRequest<AppointmentInsertResponse> multipleInsertRequest = MultipleRequest.create(new AJAXRequest[] { insertRequest1, insertRequest2, insertRequest3 });
        final MultipleResponse<AppointmentInsertResponse> multipleInsertResponse = Executor.execute(getClient(), multipleInsertRequest);

        assertFalse(multipleInsertResponse.getResponse(0).hasError(), "first insert request has errors: ");
        assertFalse(multipleInsertResponse.getResponse(1).hasError(), "second insert request has errors: ");
        assertFalse(multipleInsertResponse.getResponse(2).hasError(), "third insert request has errors: ");

        final int objectId1 = ((CommonInsertResponse) multipleInsertResponse.getResponse(0)).getId();
        final int objectId2 = ((CommonInsertResponse) multipleInsertResponse.getResponse(1)).getId();
        final int objectId3 = ((CommonInsertResponse) multipleInsertResponse.getResponse(2)).getId();

        final Appointment loadAppointment = catm.get(appointmentFolderId, objectId3);
        final Date modified = loadAppointment.getLastModified();

        final DeleteRequest deleteRequest1 = new DeleteRequest(objectId1, appointmentFolderId, modified);
        final DeleteRequest deleteRequest2 = new DeleteRequest(objectId2, appointmentFolderId, modified);
        final DeleteRequest deleteRequest3 = new DeleteRequest(objectId3, appointmentFolderId, modified);

        final MultipleRequest<CommonDeleteResponse> multipleDeleteRequest = MultipleRequest.create(new AJAXRequest[] { deleteRequest1, deleteRequest2, deleteRequest3 });
        final MultipleResponse<CommonDeleteResponse> multipleDeleteResponse = Executor.execute(getClient(), multipleDeleteRequest);

        assertFalse(multipleDeleteResponse.getResponse(0).hasError(), "first delete request has errors: ");
        assertFalse(multipleDeleteResponse.getResponse(1).hasError(), "second delete request has errors: ");
        assertFalse(multipleDeleteResponse.getResponse(2).hasError(), "third delete request has errors: ");
    }
}
