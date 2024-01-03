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

package com.openexchange.admin.rmi.service;

import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.exception.OXException;

/**
 * This class defines the Open-Xchange API Version 2 for creating and manipulating OX Contexts.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface OXContextService {

    /**
     * Delete a context.<br>
     * Note: Deleting a context will delete all data which the context include (all users, groups, appointments, ... )
     *
     * @param ctx A new Context object, this should not have been used before or a one returned from a previous call to this API.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     *
     * @throws OXException If deletion fails
     */
    void delete(Context ctx) throws OXException;

}
