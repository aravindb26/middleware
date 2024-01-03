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

package com.openexchange.contactcollector;

import java.util.Collection;
import javax.mail.internet.InternetAddress;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;

/**
 * {@link ContactCollectorService}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public interface ContactCollectorService {

    /**
     * Remembers specified addresses using asynchronous execution.
     *
     * @param addresses The addresses to remember
     * @param incrementUseCount Whether use-count is supposed to be incremented
     * @param session The user-session
     */
    default void memorizeAddresses(Collection<InternetAddress> addresses, boolean incrementUseCount, Session session) {
        try {
            memorizeAddresses(addresses, Args.builder(session).withIncrementUseCount(incrementUseCount).build());
        } catch (OXException e) {
            // Cannot occur since asynchronously invocation is passed
        }
    }

    /**
     * Remembers specified addresses according to given arguments.
     *
     * @param addresses The addresses to remember
     * @param args The arguments for contact collector invocation
     * @throws OXException If exception is supposed to be thrown (synchronous execution); see {@link Args#isAsync()}
     */
    void memorizeAddresses(Collection<InternetAddress> addresses, Args args) throws OXException;

}
