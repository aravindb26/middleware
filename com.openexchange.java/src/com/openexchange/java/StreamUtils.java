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


package com.openexchange.java;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@link StreamUtils} - Utilities for Java {@link Stream streams}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class StreamUtils {

    /**
     * Initializes a new {@link StreamUtils}.
     */
    private StreamUtils() {
        super();
    }

    /**
     * Yields a predicate to get distinct values from a stream using a custom function.
     *
     * @param <T> The type from which to extract the key
     * @param <K> The type of the key
     * @param keyExtractor The function to extract the key
     * @return The predicate to get distinct values
     */
    public static <T, K> Predicate<T> distinctByKey(Function<? super T, K> keyExtractor) {
        return new DistinctKeyPredicate<T, K>(keyExtractor);
    }

    // ------------------------------------------------------ Helper classes ---------------------------------------------------------------

    private static final class DistinctKeyPredicate<T, K> implements Predicate<T> {

        private final Map<K, Boolean> map;
        private final Function<? super T, K> keyExtractor;

        DistinctKeyPredicate(Function<? super T, K> keyExtractor) {
            super();
            this.map = new ConcurrentHashMap<>();
            this.keyExtractor = keyExtractor;
        }

        @Override
        public boolean test(T t) {
            return map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
        }
    }

}
