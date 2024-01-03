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

import java.util.concurrent.TimeUnit;
import com.openexchange.config.ConfigTools;
import com.openexchange.config.ConfigurationService;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.cleanup.DatabaseCleanUpService;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.objectusecount.groupware.ObjectUseCountDeleteListener;
import com.openexchange.objectusecount.impl.ObjectUseCountServiceImpl;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.user.UserService;

/**
 * {@link ObjectUseCountActivator}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.1
 */
public class ObjectUseCountActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link ObjectUseCountActivator}.
     */
    public ObjectUseCountActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class, DatabaseService.class, UserService.class, ThreadPoolService.class, IDBasedContactsAccessFactory.class };
    }

    @Override
    protected void startBundle() throws Exception {
        String configValue = getService(ConfigurationService.class).getProperty("com.openexchange.objectusecount.cleanupTimespan", "4W");
        long timespan = ConfigTools.parseTimespan(configValue);
        if (-1 == timespan) {
            timespan = TimeUnit.DAYS.toMillis(28L); // 4 weeks
        }
        if (0 != timespan) {
            track(DatabaseCleanUpService.class, new CleanUpServiceTracker(context, timespan));
            openTrackers();
        }
        registerService(ObjectUseCountService.class, new ObjectUseCountServiceImpl(this));
        registerService(DeleteListener.class, new ObjectUseCountDeleteListener());
    }

}
