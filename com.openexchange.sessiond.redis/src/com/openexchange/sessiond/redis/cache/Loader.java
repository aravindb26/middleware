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

package com.openexchange.sessiond.redis.cache;

import java.util.concurrent.Callable;
import com.openexchange.sessiond.redis.SessionImpl;

/**
 * {@link Loader} - A session loader.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public abstract class Loader implements Callable<SessionImpl> {

    private boolean loaded;

    /**
     * Initializes a new {@link Loader}.
     */
    protected Loader() {
        super();
        loaded = false;
    }

    @Override
    public final SessionImpl call() throws Exception {
        SessionImpl session = loadSession();
        loaded = true;
        session.setLastChecked(System.currentTimeMillis()); // Since newly added
        return session;
    }

    /**
     * Checks whether session has been loaded.
     *
     * @return <code>true</code> if loaded; otherwise <code>false</code>
     */
    public final boolean isLoaded() {
        return loaded;
    }

    /**
     * Loads the session.
     *
     * @return The loaded session
     * @throws Exception If loading session fails
     */
    protected abstract SessionImpl loadSession() throws Exception;

}
