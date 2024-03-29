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

package com.openexchange.sessiond.redis.token;

import org.slf4j.Logger;

/**
 * {@link TokenSessionTimerRemover}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class TokenSessionTimerRemover implements Runnable {

    private final TokenSessionControl control;

    /**
     * Initializes a new {@link TokenSessionTimerRemover}.
     *
     * @param control The token session control to track
     */
    public TokenSessionTimerRemover(TokenSessionControl control) {
        super();
        this.control = control;
    }

    @Override
    public void run() {
        try {
            TokenSessionContainer.getInstance().removeSession(control);
        } catch (Exception e) {
            Logger logger = org.slf4j.LoggerFactory.getLogger(TokenSessionTimerRemover.class);
            logger.error("Failed to remove elapsed token session", e);
        }
    }

}
