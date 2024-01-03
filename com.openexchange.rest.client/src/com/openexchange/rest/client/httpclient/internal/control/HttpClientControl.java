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

package com.openexchange.rest.client.httpclient.internal.control;

import java.time.Duration;
import org.slf4j.Logger;
import com.openexchange.java.AbstractOperationsWatcher;

/**
 * {@link HttpClientControl} - A registry for threads connecting an <code>HttpClient</code> instance.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class HttpClientControl extends AbstractOperationsWatcher<HttpClientInfo> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpClientControl.class);

    private static final HttpClientControl INSTANCE = new HttpClientControl();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static HttpClientControl getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link HttpClientControl}.
     */
    private HttpClientControl() {
        super("HttpClientControl", Duration.ofMinutes(5));
    }

    @Override
    protected HttpClientInfo getPoisonElement() {
        return HttpClientInfo.POISON;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected void handleExpiredOperation(HttpClientInfo info) throws Exception {
        info.interrupt();
    }

}
