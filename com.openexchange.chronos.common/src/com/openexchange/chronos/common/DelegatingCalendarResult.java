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

package com.openexchange.chronos.common;

import java.util.List;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CreateResult;
import com.openexchange.chronos.service.DeleteResult;
import com.openexchange.chronos.service.UpdateResult;
import com.openexchange.session.Session;

/**
 * {@link DelegatingCalendarResult}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public abstract class DelegatingCalendarResult implements CalendarResult {

    protected final CalendarResult delegate;

    /**
     * Initializes a new {@link DelegatingCalendarResult}.
     *
     * @param delegate The underlying calendar result delegate
     */
    protected DelegatingCalendarResult(CalendarResult delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public long getTimestamp() {
        return delegate.getTimestamp();
    }

    @Override
    public Session getSession() {
        return delegate.getSession();
    }

    @Override
    public int getCalendarUser() {
        return delegate.getCalendarUser();
    }

    @Override
    public String getFolderID() {
        return delegate.getFolderID();
    }

    @Override
    public List<DeleteResult> getDeletions() {
        return delegate.getDeletions();
    }

    @Override
    public List<UpdateResult> getUpdates() {
        return delegate.getUpdates();
    }

    @Override
    public List<CreateResult> getCreations() {
        return delegate.getCreations();
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
