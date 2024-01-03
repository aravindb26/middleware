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
package com.openexchange.segmenter.client.impl;

import java.util.Objects;
import com.openexchange.segmenter.client.Site;

/**
 * {@link SiteImpl} is a simple pojo {@link Site}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
class SiteImpl implements Site {

    private final String id;
    private final float availability;

    /**
     * Initializes a new {@link SiteImpl}.
     *
     * @param id The site identifier
     * @param availability The site's availability
     */
    SiteImpl(String id, float availability) {
        super();
        this.id = id;
        this.availability = availability;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public float getAvailability() {
        return availability;
    }

    @Override
    public boolean matches(Site other) {
        return null != other && Objects.equals(id, other.getId());
    }

    @Override
    public String toString() {
        return "SiteImpl [id=" + id + ", availability=" + availability + "]";
    }

}
