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
package com.openexchange.push.imapidle;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import com.openexchange.config.lean.Property;

/**
 * {@link ImapIdleProperty}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public enum ImapIdleProperty implements Property {

    /**
     * The full name
     */
    folder("folder", "INBOX"),

    /**
     * The delay
     */
    delay("delay", L(5000)),

    /**
     * The cluster lock
     */
    clusterLock("clusterLock", "hz"),

    /**
     * Check if session is valid
     */
    clusterLockValidateSessionExistence("clusterLock.validateSessionExistence", Boolean.FALSE),

    /**
     * The account identifier
     */
    accountId("accountId", I(0)),

    /**
     * The push mode
     */
    pushMode("pushMode", "always"),

    /**
     * Whether existence of expired IMAP IDLE listeners should happen periodically or through waiting take.
     */
    checkPeriodic("checkPeriodic", Boolean.FALSE),

    /**
     * Whether to support permanent listeners
     */
    supportsPermanentListeners("supportsPermanentListeners", Boolean.FALSE),

    ;

    private static final String PREFIX = "com.openexchange.push.imapidle.";

    private final String propertyName;
    private final Object defaultValue;

    private ImapIdleProperty(String propertyName, Object defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + propertyName;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
