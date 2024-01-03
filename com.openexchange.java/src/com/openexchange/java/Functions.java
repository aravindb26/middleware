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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link Functions} - Utility class for functions.
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public final class Functions {

    /**
     * Initializes a new {@link Functions}.
     */
    private Functions() {
        super();
    }

    /**
     * {@link OXFunction} Represents an exception aware function that accepts one argument and produces a result.
     *
     * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
     * @since v7.10.5
     * @param <T> The type of the input to the function
     * @param <R> The type of the result of the function
     * @param <E> The type of exception
     */
    @FunctionalInterface
    public interface OXFunction<T, R, E extends Exception> {

        /**
         * Applies this function to the given argument.
         *
         * @param t The function argument
         * @return The function result
         * @throws E In case the result can't be formulated
         */
        R apply(T t) throws E;

        /**
         * Applies this function to the given argument. In case of error
         * the exception will be given to the consumer
         *
         * @param t The function argument
         * @param log The consumer to log the exception
         * @return The function result or an empty optional
         */
        default Optional<R> consumeError(T t, Consumer<Exception> log) {
            try {
                return Optional.ofNullable(apply(t));
            } catch (Exception e) {
                log.accept(e);
            }
            return Optional.empty();
        }
    }

    /**
     * {@link OXBiFunction} Represents an exception aware function that accepts two arguments and produces a result.
     *
     * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
     * @since v7.10.5
     * @param <T> The type of the input to the function
     * @param <U> the type of the second argument to the function
     * @param <R> The type of the result of the function
     * @param <E> The type of exception
     */
    @FunctionalInterface
    public interface OXBiFunction<T, U, R, E extends Exception> {

        /**
         * Applies this function to the given argument.
         *
         * @param t The function argument
         * @param u The second function argument
         * @return The function result
         * @throws E In case the result can't be formulated
         */
        R apply(T t, U u) throws E;

        /**
         * Applies this function to the given argument. In case of error
         * the exception will be given to the consumer
         *
         * @param t The function argument
         * @param u The second function argument
         * @param log The consumer to log the exception
         * @return The function result or an empty optional
         */
        default Optional<R> consumeError(T t, U u, Consumer<Exception> log) {
            try {
                return Optional.ofNullable(apply(t, u));
            } catch (Exception e) {
                log.accept(e);
            }
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the function constant yielding a new {@link ArrayList} regardless of function argument.
     *
     * @param <T> The type of the input to the function
     * @param <E> The type of the elements of the list
     * @return The function  yielding a new {@link ArrayList}
     */
    @SuppressWarnings("unchecked")
    public static <T, E> Function<T, List<E>> getNewArrayListFuntion() {
        return FUNCTION_NEW_ARRAYLIST;
    }

    @SuppressWarnings("rawtypes")
    private static final Function FUNCTION_NEW_ARRAYLIST = new NewArrayListFunction<>();

    private static class NewArrayListFunction<T, E> implements Function<T, List<E>> {

        @Override
        public List<E> apply(T t) {
            return new ArrayList<>();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the function constant yielding a new {@link LinkedList} regardless of function argument.
     *
     * @param <T> The type of the input to the function
     * @param <E> The type of the elements of the list
     * @return The function  yielding a new {@link LinkedList}
     */
    @SuppressWarnings("unchecked")
    public static <T, E> Function<T, List<E>> getNewLinkedListFuntion() {
        return FUNCTION_NEW_LINKEDLIST;
    }

    @SuppressWarnings("rawtypes")
    private static final Function FUNCTION_NEW_LINKEDLIST = new NewLinkedListFunction<>();

    private static class NewLinkedListFunction<T, E> implements Function<T, List<E>> {

        @Override
        public List<E> apply(T t) {
            return new LinkedList<>();
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the function constant yielding a new {@link HashMap} regardless of function argument.
     *
     * @param <T> The type of the input to the function
     * @param <K> The type of the keys of the map
     * @param <V> The type of the values of the map
     * @return The function  yielding a new {@link LinkedList}
     */
    @SuppressWarnings("unchecked")
    public static <T, K, V> Function<T, Map<K, V>> getNewHashMapFuntion() {
        return FUNCTION_NEW_HASHMAP;
    }

    @SuppressWarnings("rawtypes")
    private static final Function FUNCTION_NEW_HASHMAP = new NewHashMapFunction<>();

    private static class NewHashMapFunction<T, K, V> implements Function<T, Map<K, V>> {

        @Override
        public Map<K, V> apply(T t) {
            return new HashMap<>();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the function constant yielding a new {@link LinkedHashMap} regardless of function argument.
     *
     * @param <T> The type of the input to the function
     * @param <K> The type of the keys of the map
     * @param <V> The type of the values of the map
     * @return The function  yielding a new {@link LinkedList}
     */
    @SuppressWarnings("unchecked")
    public static <T, K, V> Function<T, Map<K, V>> getNewLinkedHashMapFuntion() {
        return FUNCTION_NEW_LINKEDHASHMAP;
    }

    @SuppressWarnings("rawtypes")
    private static final Function FUNCTION_NEW_LINKEDHASHMAP = new NewLinkedHashMapFunction<>();

    private static class NewLinkedHashMapFunction<T, K, V> implements Function<T, Map<K, V>> {

        @Override
        public Map<K, V> apply(T t) {
            return new LinkedHashMap<>();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the function constant yielding a new {@link LinkedHashSet} regardless of function argument.
     *
     * @param <T> The type of the input to the function
     * @param <E> The type of the elements of the set
     * @return The function  yielding a new {@link LinkedList}
     */
    @SuppressWarnings("unchecked")
    public static <T, E> Function<T, Set<E>> getNewLinkedHashSetFuntion() {
        return FUNCTION_NEW_LINKEDHASHSET;
    }

    @SuppressWarnings("rawtypes")
    private static final Function FUNCTION_NEW_LINKEDHASHSET = new NewLinkedHashSetFunction<>();

    private static class NewLinkedHashSetFunction<T, E> implements Function<T, Set<E>> {

        @Override
        public Set<E> apply(T t) {
            return new LinkedHashSet<>();
        }
    }

}
