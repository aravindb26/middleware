package com.openexchange.database.cleanup.impl;

import java.util.concurrent.ConcurrentMap;
import com.openexchange.database.cleanup.CleanUpInfo;
import com.openexchange.database.cleanup.CleanUpJobId;
import com.openexchange.timer.ScheduledTimerTask;

/**
 * {@link CustomJobCleanUpInfo} - The clean-up info implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class CustomJobCleanUpInfo implements CleanUpInfo {

    private final ScheduledTimerTask timerTask;
    private final CleanUpJobId jobId;
    private final ConcurrentMap<CleanUpJobId, ?> submittedJobs;

    /**
     * Initializes a new {@link CustomJobCleanUpInfo}.
     *
     * @param jobId The cleanup job's identifier
     * @param timerTask The wrapped timer task
     * @param submittedJobs The in-memory registry for submitted jobs
     */
    CustomJobCleanUpInfo(CleanUpJobId jobId, ScheduledTimerTask timerTask, ConcurrentMap<CleanUpJobId, ?> submittedJobs) {
        super();
        this.jobId = jobId;
        this.timerTask = timerTask;
        this.submittedJobs = submittedJobs;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean canceled = timerTask.cancel(mayInterruptIfRunning);
        submittedJobs.remove(jobId);
        return canceled;
    }

    @Override
    public CleanUpJobId getJobId() {
        return jobId;
    }

}