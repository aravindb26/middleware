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

package com.openexchange.mail.compose;

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.session.Session;

/**
 * {@link CompositionSpaceServiceFactoryRegistry} - A registry for composition space service factories.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
@SingletonService
public interface CompositionSpaceServiceFactoryRegistry {

    /**
     * Gets the highest-ranked composition space service factory for session-associated user.
     *
     * @param session The session providing user information
     * @return The highest-ranked composition space service factory
     * @throws OXException If highest-ranked composition space service factory cannot be returned
     */
    CompositionSpaceServiceFactory getHighestRankedFactoryFor(Session session) throws OXException;

    /**
     * Gets the composition space service factories for session-associated user.
     *
     * @param session The session providing user information
     * @return The composition space service factories ordered by ranking
     * @throws OXException If composition space service factories cannot be returned
     */
    List<CompositionSpaceServiceFactory> getFactoriesFor(Session session) throws OXException;

    /**
     * Gets the composition space service factory associated with given identifier for session-associated user.
     *
     * @param serviceId The service identifier
     * @param session The session providing user information
     * @return The composition space service factory
     * @throws OXException If composition space service factory cannot be returned
     */
    CompositionSpaceServiceFactory getFactoryFor(String serviceId, Session session) throws OXException;

}
