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

package com.openexchange.chronos.common;

import com.openexchange.chronos.service.SequenceResult;

/**
 * {@link DefaultSequenceResult}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class DefaultSequenceResult implements SequenceResult {

    private final long timestamp;
    private final long count;

    /**
     * Initializes a new {@link DefaultSequenceResult}.
     *
     * @param timestamp The maximum timestamp as sequence number
     * @param count The total number of events in the underlying folder view
     */
    public DefaultSequenceResult(long timestamp, long count) {
        super();
        this.timestamp = timestamp;
        this.count = count;
    }

    @Override
    public long getTotalCount() {
        return count;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "DefaultSequenceResult [timestamp=" + timestamp + ", totalCount=" + count + "]";
    }

}
