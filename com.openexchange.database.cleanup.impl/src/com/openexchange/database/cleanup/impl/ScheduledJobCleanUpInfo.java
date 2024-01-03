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

package com.openexchange.database.cleanup.impl;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.database.cleanup.CleanUpInfo;
import com.openexchange.database.cleanup.CleanUpJobId;


/**
 * {@link ScheduledJobCleanUpInfo} - The clean-up info for a scheduled/general clean-up job.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public class ScheduledJobCleanUpInfo implements CleanUpInfo {

    private final CleanUpJobId jobId;
    private final AtomicReference<CleanUpJobRunnable> currentRunnable;
    private final ConcurrentMap<CleanUpJobId, ?> submittedJobs;

    /**
     * Initializes a new {@link ScheduledJobCleanUpInfo}.
     *
     * @param jobId The identifier of the associated clean-up job
     * @param currentRunnable The reference for the current runnable executing the job
     * @param submittedJobs The collection of submitted clean-up jobs to remove from in case job gets canceled
     */
    public ScheduledJobCleanUpInfo(CleanUpJobId jobId, AtomicReference<CleanUpJobRunnable> currentRunnable, ConcurrentMap<CleanUpJobId, ?> submittedJobs) {
        super();
        this.jobId = jobId;
        this.currentRunnable = currentRunnable;
        this.submittedJobs = submittedJobs;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        submittedJobs.remove(jobId);
        CleanUpJobRunnable runnable = currentRunnable.get();
        return runnable != null ? runnable.interrupt(true) : false;
    }

    @Override
    public CleanUpJobId getJobId() {
        return jobId;
    }

}
