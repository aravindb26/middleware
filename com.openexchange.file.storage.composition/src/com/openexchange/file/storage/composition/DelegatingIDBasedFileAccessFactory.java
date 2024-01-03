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

package com.openexchange.file.storage.composition;

import com.openexchange.session.Session;


/**
 * {@link DelegatingIDBasedFileAccessFactory} - A factory for creating {@link IDBasedFileAccess} instances that delegate to an existing {@link IDBasedFileAccess} instance.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public interface DelegatingIDBasedFileAccessFactory {

    /**
     * Creates the delegating file access instance for given session and file access.
     *
     * @param fileAccess The file access to delegate to
     * @param session The session
     * @return The appropriate delegating file access instance
     */
    DelegatingIDBasedFileAccess createAccess(IDBasedFileAccess fileAccess, Session session);

}
