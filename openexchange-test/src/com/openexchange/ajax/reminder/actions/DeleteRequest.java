
package com.openexchange.ajax.reminder.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.CalendarFields;
import com.openexchange.ajax.fields.ReminderFields;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.groupware.reminder.ReminderObject;

public class DeleteRequest extends AbstractReminderRequest<CommonDeleteResponse> {

    private final boolean failOnError;
    private final List<Long> objectIds = new ArrayList<Long>();
    private final List<Long> lastModifieds = new ArrayList<Long>();
    private final List<Integer> recurrencePositions = new ArrayList<>();

    public DeleteRequest(ReminderObject reminder, boolean failOnError) {
        super();
        this.failOnError = failOnError;
        objectIds.add((long) reminder.getObjectId());
        lastModifieds.add(reminder.getLastModified().getTime());
        recurrencePositions.add(reminder.getRecurrencePosition());
    }

    public DeleteRequest(ReminderObject[] reminders, boolean failOnError) {
        super();
        this.failOnError = failOnError;
        for (ReminderObject reminder : reminders) {
            objectIds.add((long) reminder.getObjectId());
            lastModifieds.add(reminder.getLastModified().getTime());
            recurrencePositions.add(reminder.getRecurrencePosition());
        }
    }

    public DeleteRequest(Long id, Integer recurrencePosition, Long lastModified, boolean failOnError) {
        super();
        this.failOnError = failOnError;
        objectIds.add(id);
        lastModifieds.add(lastModified);
        recurrencePositions.add(recurrencePosition);
    }

    @Override
    public Object getBody() throws IOException, JSONException {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < objectIds.size(); i++) {
            JSONObject json = new JSONObject();
            json.put(CalendarFields.RECURRENCE_POSITION, recurrencePositions.get(i));
            json.put(ReminderFields.ID, objectIds.get(i));
            jsonArray.put(json);
        }
        return jsonArray;
    }

    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    @Override
    public Parameter[] getParameters() throws IOException, JSONException {
        return new Parameter[] { new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_DELETE), new Parameter(AJAXServlet.PARAMETER_TIMESTAMP, String.valueOf(lastModifieds.get(0))) };
    }

    @Override
    public DeleteParser getParser() {
        return new DeleteParser(failOnError);
    }

}
