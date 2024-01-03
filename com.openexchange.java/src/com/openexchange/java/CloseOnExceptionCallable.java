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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * {@link CloseOnExceptionCallable} - The abstract callable that collects instances of <code>AutoCloseable</code> and closes them if the
 * {@link #call()} method is left on exception path.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public abstract class CloseOnExceptionCallable<V> implements Callable<V> {

    /**
     * Executes given callable instance.
     *
     * @param <V> The result type
     * @param callable The callable instance to execute
     * @return The computed result
     * @throws Exception If computing the result fails
     */
    public static <V> V execute(CloseOnExceptionCallable<V> callable) throws Exception {
        return callable == null ? null : callable.call();
    }

    /**
     * Executes given callable instance.
     *
     * @param <V> The result type
     * @param <E> The type of the expected exception
     * @param callable The callable instance to execute
     * @return The computed result
     * @throws E If computing the result fails
     */
    public static <V, E extends Exception> V execute(CloseOnExceptionCallable<V> callable, Function<Exception, E> errorConverter) throws E {
        if (callable == null) {
            return null;
        }

        try {
            return callable.call();
        } catch (Exception e) {
            throw errorConverter.apply(e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final List<AutoCloseable> closeables;

    /**
     * Initializes a new {@link CloseOnExceptionCallable}.
     */
    protected CloseOnExceptionCallable() {
        super();
        closeables = new ArrayList<>(2);
    }

    /**
     * Adds and then returns given closeable.
     *
     * @param <C> The type of the closeable
     * @param closeable The closeable to add
     * @return The given closeable
     */
    protected <C extends AutoCloseable> C addAndReturnCloseable(C closeable) {
        if (closeable != null) {
            closeables.add(closeable);
        }
        return closeable;
    }

    @Override
    public final V call() throws Exception {
        try {
            // Compute the result
            V result = doCall();

            // Assume everything went fine. Clear collected closeables to avoid possible premature closing
            closeables.clear();
            return result;
        } finally {
            Streams.closeAutoCloseables(closeables);
        }
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    protected abstract V doCall() throws Exception;

}
