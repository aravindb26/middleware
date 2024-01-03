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

package com.openexchange.osgi;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.osgi.util.RankedService;

/**
 * {@link DefaultServiceProvider}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class DefaultServiceProvider<S> implements ServiceProvider<S> {

    private final TreeSet<RankedService<S>> set;
    private final AtomicReference<S> highestRankedService;

    /**
     * Initializes a new {@link DefaultServiceProvider}.
     */
    public DefaultServiceProvider() {
        super();
        set = new TreeSet<RankedService<S>>();
        highestRankedService = new AtomicReference<S>(null);
    }

    @Override
    public S getService() {
        return highestRankedService.get();
    }

    @Override
    public synchronized void addService(S service, int ranking) {
        set.add(new RankedService<S>(service, ranking));
        highestRankedService.set(set.first().service);
    }

    /**
     * Removes the given service from this instance.
     *
     * @param service The service to remove
     * @return <code>true</code> if this provider is considered as empty after removal; otherwise <code>false</code> if still services left
     */
    public synchronized boolean removeService(Object service) {
        if (service == null) {
            return false;
        }

        boolean removed = false;
        for (Iterator<RankedService<S>> it = set.iterator(); !removed && it.hasNext();) {
            RankedService<S> rankedService = it.next();
            if (service.equals(rankedService.service)) {
                it.remove();
                removed = true;
            }
        }
        boolean empty = set.isEmpty();
        if (removed) {
            highestRankedService.set(empty ? null : set.first().service);
        }
        return empty;
    }

}
