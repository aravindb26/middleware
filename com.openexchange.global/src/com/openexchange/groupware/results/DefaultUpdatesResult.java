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

package com.openexchange.groupware.results;

import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.List;
import java.util.function.Function;

/**
 * {@link DefaultUpdatesResult}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class DefaultUpdatesResult<T> extends DefaultSequenceResult implements UpdatesResult<T> {

    private final List<T> newAndModifiedObjects;
    private final List<T> deletedObjects;
    private final boolean truncated;

    /**
     * Initializes a new {@link DefaultUpdatesResult}.
     *
     * @param newAndModifiedObjects The list of new/modified events
     * @param deletedObjects The list of deleted events
     * @param timestamp The maximum timestamp of all events in the update result
     * @param truncated <code>true</code> if the result is truncated, <code>false</code>, otherwise
     * @param count The total number of events in the underlying folder view (independently of the client supplied timestamp)
     */
    public DefaultUpdatesResult(List<T> newAndModifiedObjects, List<T> deletedObjects, long timestamp, boolean truncated, long count) {
        super(timestamp, count);
        this.newAndModifiedObjects = newAndModifiedObjects;
        this.deletedObjects = deletedObjects;
        this.truncated = truncated;
    }

    /**
     * Initializes a new {@link DefaultUpdatesResult}.
     *
     * @param newAndModifiedObjects The list of new/modified events
     * @param deletedObjects The list of deleted events
     * @param timestampFunction A function that yields the timestamp of an object
     * @param count The total number of events in the underlying folder view (independently of the client supplied timestamp)
     */
    public DefaultUpdatesResult(List<T> newAndModifiedObjects, List<T> deletedObjects, Function<T, Long> timestampFunction, long count) {
        this(newAndModifiedObjects, deletedObjects, Math.max(getMaximumTimestamp(newAndModifiedObjects, timestampFunction), getMaximumTimestamp(deletedObjects, timestampFunction)), false, count);
    }

    @Override
    public List<T> getNewAndModifiedObjects() {
        return newAndModifiedObjects;
    }

    @Override
    public List<T> getDeletedObjects() {
        return deletedObjects;
    }

    @Override
    public boolean isTruncated() {
        return truncated;
    }

    @Override
    public boolean isEmpty() {
        return isNullOrEmpty(deletedObjects) && isNullOrEmpty(newAndModifiedObjects);
    }

    @Override
    public String toString() {
        return "DefaultUpdatesResult [newAndModifiedObjects=" + newAndModifiedObjects + ", deletedObjects=" + deletedObjects + "]";
    }

    private static <T> long getMaximumTimestamp(List<T> objects, Function<T, Long> timestampFunction) {
        long timestamp = 0L;
        if (null != objects) {
            for (T object : objects) {
                Long value = timestampFunction.apply(object);
                if (null != value) {
                    timestamp = Math.max(timestamp, value.longValue());
                }
            }
        }
        return timestamp;
    }

}
