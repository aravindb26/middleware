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

import com.openexchange.config.lean.Property;

/**
 * {@link SegmenterProperties} contains properties relevant to configure the segmenter service.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public enum SegmenterProperties implements Property {

    /**
     * Contains the base URL for the segmenter service; e.g. <code>https://my.segmenter.domain</code>. As long as no value is defined,
     * a non-sharded environment is assumed where all segments are served by the <i>local</i> site.
     */
    BASE_URL("baseUrl"),

    /**
     * Holds the identifier of the local site where this node is deployed, defaults to <code>default</code> unless overridden.
     */
    LOCAL_SITE_ID("localSiteId", "default"),

    ;

    private final String fqn;
    private final Object defaultValue;

    private static final String PREFIX = "com.openexchange.segmenter.";

    /**
     * Initializes a new {@link SegmenterProperties} with a null default.
     *
     * @param name The name of the property
     */
    private SegmenterProperties(String name) {
        this(name, null);
    }

    /**
     * Initializes a new {@link SegmenterProperties}.
     *
     * @param name The name of the property
     * @param defaultValue The default value of the property
     */
    private SegmenterProperties(String name, Object defaultValue) {
        this.fqn = PREFIX + name;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
