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
package com.openexchange.push.dovecot;

import com.openexchange.config.lean.Property;

/**
 * {@link DovecotPushProperty}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public enum DovecotPushProperty implements Property {

    /**
     * Specifies what system to use to manage a cluster-lock
       Possible values
       * "db" for database-based locking
       * "hz" for Hazelcast-based locking (default)
       * "none" for no cluster lock mechanism
       Only applicable if property "com.openexchange.push.dovecot.stateless" is set to "false"
     */
    clusterLock("hz"),

    /**
     * Whether to use stateless implementation.
     */
    stateless(Boolean.TRUE),

    /**
     * Whether to prefer Doveadm to issue METADATA commands.
     */
    preferDoveadmForMetadata(Boolean.FALSE),
    ;

    private static final String PREFIX = "com.openexchange.push.dovecot.";

    private final Object defaultValue;

    private DovecotPushProperty(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + name();
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
