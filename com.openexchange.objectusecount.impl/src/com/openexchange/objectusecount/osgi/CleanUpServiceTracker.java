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
package com.openexchange.objectusecount.osgi;

import java.time.Duration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.database.cleanup.CleanUpInfo;
import com.openexchange.database.cleanup.DatabaseCleanUpService;
import com.openexchange.database.cleanup.DefaultCleanUpJob;
import com.openexchange.exception.OXException;
import com.openexchange.objectusecount.cleanup.GenericUseCountCleanUpExecution;
import com.openexchange.objectusecount.cleanup.ObjectUseCountCleanUpExecution;


/**
 * {@link CleanUpServiceTracker}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.6
 */
public class CleanUpServiceTracker implements ServiceTrackerCustomizer<DatabaseCleanUpService, DatabaseCleanUpService> {

    private final static Logger LOG = LoggerFactory.getLogger(CleanUpServiceTracker.class);

    private final BundleContext context;
    private CleanUpInfo objectUseCountCleanUpJobInfo;
    private CleanUpInfo genericUseCountCleanUpJobInfo;
    private long timespan;

    public CleanUpServiceTracker(BundleContext context, long timespan) {
        super();
        this.context = context;
        this.timespan = timespan;
    }

    @Override
    public DatabaseCleanUpService addingService(ServiceReference<DatabaseCleanUpService> reference) {
        DatabaseCleanUpService service = context.getService(reference);

        try {
            objectUseCountCleanUpJobInfo = service.scheduleCleanUpJob(DefaultCleanUpJob.builder() //@formatter:off
                .withId(ObjectUseCountCleanUpExecution.class)
                .withDelay(Duration.ofDays(1))
                .withInitialDelay(Duration.ofMinutes(30))
                .withRunsExclusive(true)
                .withPreferNoConnectionTimeout(true)
                .withExecution(new ObjectUseCountCleanUpExecution(timespan))
                .build());  //@formatter:on
        } catch (OXException e) {
            LOG.error("Could not register clean-up job for object_use_count table.", e);
        }

        try {
            genericUseCountCleanUpJobInfo = service.scheduleCleanUpJob(DefaultCleanUpJob.builder() //@formatter:off
                .withId(GenericUseCountCleanUpExecution.class)
                .withDelay(Duration.ofDays(1))
                .withInitialDelay(Duration.ofMinutes(60))
                .withRunsExclusive(true)
                .withPreferNoConnectionTimeout(true)
                .withExecution(new GenericUseCountCleanUpExecution(timespan))
                .build());  //@formatter:on
        } catch (OXException e) {
            LOG.error("Could not register clean-up job for generic_use_count table.", e);
        }

        return service;
    }

    @Override
    public void modifiedService(ServiceReference<DatabaseCleanUpService> reference, DatabaseCleanUpService service) {
        // nothing to do
    }

    @Override
    public void removedService(ServiceReference<DatabaseCleanUpService> reference, DatabaseCleanUpService service) {
        if (null != objectUseCountCleanUpJobInfo) {
            try {
                objectUseCountCleanUpJobInfo.cancel(true);
            } catch (Exception e) {
                LOG.error("Could not stop clean-up job for object_use_count table.", e);
            }
            objectUseCountCleanUpJobInfo = null;
        }

        if (null != genericUseCountCleanUpJobInfo) {
            try {
                genericUseCountCleanUpJobInfo.cancel(true);
            } catch (Exception e) {
                LOG.error("Could not stop clean-up job for generic_use_count table.", e);
            }
            genericUseCountCleanUpJobInfo = null;
        }

        context.ungetService(reference);
    }

}
