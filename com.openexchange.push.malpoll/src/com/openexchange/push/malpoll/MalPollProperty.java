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
package com.openexchange.push.malpoll;

import static com.openexchange.java.Autoboxing.L;
import com.openexchange.config.lean.Property;

/**
 * {@link MALPollProperty}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public enum MalPollProperty implements Property {

    /**
     * Define the amount of time in milliseconds when to periodically check for new mails.
     */
    period(L(300000)),

    /**
     * Define the folder to look-up for new mails in each mailbox.
     */
    folder("INBOX"),

    /**
     * Whether a global timer is set or a timer per user.
       Or in other words: Do you want a global heartbeat or a heartbeat per user?
     */
    global(Boolean.TRUE),

    /**
     * Whether the tasks executed by global timer are executed concurrently
       or by calling timer's thread.
       Note:  
       This property only has effect if [[com.openexchange.push.malpoll.global]]
       is set to <code>true</code>.
     */
    concurrentGlobal(Boolean.TRUE),

    ;
    
    private static final String PREFIX = "com.openexchange.push.malpoll.";

    private final Object defaultValue;

    private MalPollProperty(Object defaultValue) {
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
