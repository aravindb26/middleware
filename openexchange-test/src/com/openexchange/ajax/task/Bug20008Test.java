
package com.openexchange.ajax.task;

import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.math.BigDecimal;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.task.actions.GetRequest;
import com.openexchange.ajax.task.actions.GetResponse;
import com.openexchange.ajax.task.actions.InsertRequest;
import com.openexchange.ajax.task.actions.InsertResponse;
import com.openexchange.ajax.task.actions.UpdateRequest;
import com.openexchange.ajax.task.actions.UpdateResponse;
import com.openexchange.groupware.tasks.Task;
import org.junit.jupiter.api.TestInfo;

public class Bug20008Test extends AbstractAJAXSession {

    private AJAXClient client;
    private Task task;
    private TimeZone tz;

    public Bug20008Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getClient();
        tz = client.getValues().getTimeZone();
        task = new Task();
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("Test for bug 20008");
        task.setActualDuration(L(2));
        task.setActualCosts(new BigDecimal("2.0"));
        task.setTargetDuration(L(10));
        task.setTargetCosts(new BigDecimal("10.0"));
        InsertRequest request = new InsertRequest(task, tz);
        InsertResponse response = client.execute(request);
        response.fillTask(task);
    }

    @Test
    public void testUpdate() throws Throwable {
        task.setActualDuration(null);
        task.setTargetDuration(null);
        task.setActualCosts(null);
        task.setTargetCosts(null);
        UpdateRequest req = new UpdateRequest(task, tz, false);
        try {
            UpdateResponse response = client.execute(req);
            task.setLastModified(response.getTimestamp());
        } catch (Exception e) {
            fail("Deleting task attributes actualDuration, targetDuration, actualCosts, targetCosts failed!");
        }
        GetRequest request = new GetRequest(task);
        GetResponse response = client.execute(request);
        task.setLastModified(response.getTimestamp());
        Task test = response.getTask(tz);
        assertNull(test.getActualDuration(), "Actual duration should not be set.");
        assertNull(test.getTargetDuration(), "Target duration should not be set.");
        assertNull(test.getActualCosts(), "Actual costs should not be set.");
        assertNull(test.getTargetCosts(), "Target costs should not be set.");
    }
}
