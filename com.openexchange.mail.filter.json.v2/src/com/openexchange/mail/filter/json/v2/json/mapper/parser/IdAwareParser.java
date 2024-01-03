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

package com.openexchange.mail.filter.json.v2.json.mapper.parser;

import java.util.Set;
import com.openexchange.exception.OXException;

/**
 * {@link IdAwareParser}
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.4
 */
public interface IdAwareParser {

    /**
     * Checks whether this command is supported or not
     *
     * @param capabilities The capabilities of the user
     * @param id The id of the command
     * @return <code>true</code> of this command is supported, <code>false</code> otherwise
     * @throws OXException
     */
    boolean isCommandSupported(Set<String> capabilities, String id) throws OXException;

}
