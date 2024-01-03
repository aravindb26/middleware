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

package com.openexchange.logging;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link LogUtility} - Utility class for logging.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public final class LogUtility {

    /**
     * Initializes a new {@link LogUtility}.
     */
    private LogUtility() {
        super();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class IntArrayString {

        private final int[] integers;
        private final CharSequence optDelimiter;

        IntArrayString(int[] integers, CharSequence optDelimiter) {
            super();
            this.integers = integers;
            this.optDelimiter = optDelimiter;
        }

        @Override
        public String toString() {
            int iMax = integers.length - 1;
            StringBuilder b = new StringBuilder();

            if (optDelimiter != null) {
                for (int i = 0; ; i++) {
                    b.append(integers[i]);
                    if (i == iMax) {
                        return b.toString();
                    }
                    b.append(optDelimiter);
                }
            }

            b.append('[');
            for (int i = 0; ; i++) {
                b.append(integers[i]);
                if (i == iMax) {
                    return b.append(']').toString();
                }
                b.append(", ");
            }
        }
    }

    /**
     * Creates a {@link #toString()} object for given integer array.
     *
     * @param integers The integer array
     * @return The object providing content of given array if {@link #toString()} is invoked
     */
    public static Object toStringObjectFor(int[] integers) {
        return toStringObjectFor(integers, null);
    }

    /**
     * Creates a {@link #toString()} object for given integer array.
     *
     * @param integers The integer array
     * @param optDelimiter The delimiter to be used between each element or <code>null</code> for element-wise output
     * @return The object providing content of given array if {@link #toString()} is invoked
     */
    public static Object toStringObjectFor(int[] integers, CharSequence optDelimiter) {
        if (integers == null) {
            return "null";
        }

        if (integers.length <= 0) {
            return optDelimiter == null ? "[]" : "";
        }

        return new IntArrayString(integers, optDelimiter);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class ObjectArrayString<O> {

        private final O[] objects;
        private final CharSequence optDelimiter;

        ObjectArrayString(O[] objects, CharSequence optDelimiter) {
            super();
            this.objects = objects;
            this.optDelimiter = optDelimiter;
        }

        @Override
        public String toString() {
            return optDelimiter == null ? Arrays.toString(objects) : Arrays.stream(objects).map(String::valueOf).collect(Collectors.joining(", "));
        }
    }

    /**
     * Creates a {@link #toString()} object for given object array.
     *
     * @param <O> The element type
     * @param objects The object array
     * @return The object providing content of given array if {@link #toString()} is invoked
     */
    public static <O> Object toStringObjectFor(O[] objects) {
        return toStringObjectFor(objects, null);
    }

    /**
     * Creates a {@link #toString()} object for given object array.
     *
     * @param <O> The element type
     * @param objects The object array
     * @param optDelimiter The delimiter to be used between each element or <code>null</code> for element-wise output
     * @return The object providing content of given array if {@link #toString()} is invoked
     */
    public static <O> Object toStringObjectFor(O[] objects, CharSequence optDelimiter) {
        if (objects == null) {
            return "null";
        }

        if (objects.length <= 0) {
            return optDelimiter == null ? "[]" : "";
        }

        return new ObjectArrayString<O>(objects, optDelimiter);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class ObjectCollectionString<O> {

        private final Collection<O> objects;
        private final CharSequence optDelimiter;

        ObjectCollectionString(Collection<O> objects, CharSequence optDelimiter) {
            super();
            this.objects = objects;
            this.optDelimiter = optDelimiter;
        }

        @Override
        public String toString() {
            return optDelimiter == null ? objects.toString() : objects.stream().map(String::valueOf).collect(Collectors.joining(", "));
        }
    }

    /**
     * Creates a {@link #toString()} object for given object collection.
     *
     * @param <O> The element type
     * @param objects The object collection
     * @return The object providing content of given collection if {@link #toString()} is invoked
     */
    public static <O> Object toStringObjectFor(Collection<O> objects) {
        return toStringObjectFor(objects, null);
    }

    /**
     * Creates a {@link #toString()} object for given object collection.
     *
     * @param <O> The element type
     * @param objects The object collection
     * @param optDelimiter The delimiter to be used between each element or <code>null</code> for element-wise output
     * @return The object providing content of given collection if {@link #toString()} is invoked
     */
    public static <O> Object toStringObjectFor(Collection<O> objects, CharSequence optDelimiter) {
        if (objects == null) {
            return "null";
        }

        if (objects.isEmpty()) {
            return optDelimiter == null ? "[]" : "";
        }

        return new ObjectCollectionString<O>(objects, optDelimiter);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class ObjectString<O> {

        private final Supplier<O> supplier;

        ObjectString(Supplier<O> supplier) {
            super();
            this.supplier = supplier;
        }

        @Override
        public String toString() {
            O stringForMe = supplier.get();
            return stringForMe == null ? "null" : stringForMe.toString();
        }
    }

    /**
     * Creates a {@link #toString()} object for given supplier.
     *
     * @param <O> The element type
     * @param supplier The supplier
     * @return The object providing content of given supplier if {@link #toString()} is invoked
     */
    public static <O> Object toStringObjectFor(Supplier<O> supplier) {
        if (supplier == null) {
            return "null";
        }

        return new ObjectString<O>(supplier);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class IdentityHashCodeString {

        private final Object object;

        IdentityHashCodeString(Object object) {
            super();
            this.object = object;
        }

        @Override
        public String toString() {
            return object == null ? "null" : Integer.toString(System.identityHashCode(object));
        }
    }

    /**
     * Creates a {@link #toString()} for identity hash code for given object.
     *
     * @param object The object
     * @return The object providing identity hash code if {@link #toString()} is invoked
     */
    public static Object toIdentityHashCodeFor(Object object) {
        if (object == null) {
            return "null";
        }

        return new IdentityHashCodeString(object);
    }
}
