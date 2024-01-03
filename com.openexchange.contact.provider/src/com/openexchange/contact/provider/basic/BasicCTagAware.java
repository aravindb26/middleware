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
package com.openexchange.contact.provider.basic;

import com.openexchange.contact.provider.extensions.CTagAware;
import com.openexchange.exception.OXException;

/**
 * {@link BasicCTagAware}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.x
 */
public interface BasicCTagAware extends CTagAware {

    /**
     * Retrieves the CTag (Collection Entity Tag) for the contacts access.
     * <p/>
     * The CTag is like a resource ETag and changes each time something within the contacts access has changed. This allows clients to
     * quickly determine whether it needs to synchronize any changed contents or not.
     *
     * @return the CTag for this basic contacts access
     */
    String getCTag() throws OXException;

}
