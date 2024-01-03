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

package com.openexchange.marker;

import java.io.Closeable;

/**
 * {@link KnownThreadLocalValue} - Known names for thread-local values.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public enum KnownThreadLocalValue {

    /**
     * A collection of message queues that are supposed to be drained when thread is finished.
     * <p>
     * Value is of type: <code>java.util.Map&lt;com.openexchange.cache.v2.ChannelKey, java.util.Queue&lt;com.openexchange.cache.v2.MessageCollectionId&gt;&gt;</code>.
     */
    CACHE_MESSAGE_COLLECTIONS("com.openexchange.cache.v2.messageCollections"),
    /**
     * A map managing temporary files associated with a  thread.
     * <p>
     * Value is of type: <code>java.util.Map&lt;java.io.File, java.lang.Object&gt;</code>.
     */
    TEMPORARY_FILES_ON_DISK("com.openexchange.disk.temporaryFiles"),
    /**
     * A queue holding {@link Closeable} instances associated with a  thread.
     * <p>
     * Value is of type: <code>java.util.Queue&lt;java.io.Closeable&gt;</code>.
     */
    CLOSABLES("com.openexchange.io.closables"),
    ;

    private final String identifier;

    private KnownThreadLocalValue(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the identifier
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

}
