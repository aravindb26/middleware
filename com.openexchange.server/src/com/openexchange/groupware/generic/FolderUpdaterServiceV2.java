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

package com.openexchange.groupware.generic;

import java.util.Collection;
import com.openexchange.exception.OXException;
import com.openexchange.tools.iterator.SearchIterator;


/**
 * {@link FolderUpdaterServiceV2}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface FolderUpdaterServiceV2<T> extends FolderUpdaterService<T> {

    /**
     * Saves folder modification
     *
     * @param data A search iterator over the data to store
     * @param target The target definition
     * @param errors The optional collection of error
     * @throws OXException If update fails
     */
    void save(SearchIterator<T> data, TargetFolderDefinition target, final Collection<OXException> errors) throws OXException;

}