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

package com.openexchange.admin.plugin.hosting.storage.mysqlStorage;

import com.openexchange.admin.tools.AdminCacheExtended;


/**
 * {@link NumericContextSearcher}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public class NumericContextSearcher extends ContextSearcher {

    /**
     * Initializes a new {@link NumericContextSearcher}.
     *
     * @param cache The cache reference used to acquire/release a connection
     * @param contextId The context identifier to look-for
     */
    public NumericContextSearcher(AdminCacheExtended cache, int contextId) {
        super(cache, "SELECT cid FROM context WHERE cid = " + contextId, null);
    }

}