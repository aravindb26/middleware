
package com.openexchange.ajax.task;

import java.util.Calendar;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.modules.Module;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;

public abstract class ManagedTaskTest extends AbstractAJAXSession {

    protected int folderID;
    protected Task actual;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderID = ftm.insertFolderOnServer(ftm.generatePublicFolder("Managed task test folder #" + UUID.randomUUID().toString(), Module.TASK.getFolderConstant(), getClient().getValues().getPrivateTaskFolder(), getClient().getValues().getUserId())).getObjectID();
        actual = null;
    }

    public Task generateTask(String title) {
        Task task = new Task();
        task.setParentFolderID(folderID);
        task.setTitle(title);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("next friday at 00:00"));
        task.setStartDate(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        task.setEndDate(calendar.getTime());
        return task;
    }

}
