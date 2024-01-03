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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.java.Strings;

/**
 * {@link LogMessageBuilder} - A helper class when composing bigger log messages with log arguments.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public abstract class LogMessageBuilder {

    /** The constant for empty arguments array */
    private static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * Gets the empty log message builder.
     *
     * @return The empty builder
     */
    public static LogMessageBuilder emptyLogMessageBuilder() {
        return EMPTY;
    }

    /**
     * Creates a new log message builder with given capacities.
     *
     * @param messageCapacity The capacity for log message
     * @param argsCapacity The capacity for log arguments
     * @return The newly created builder
     */
    public static LogMessageBuilder createLogMessageBuilder(int messageCapacity, int argsCapacity) {
        return new LogMessageBuilderImpl(messageCapacity, argsCapacity);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link LogMessageBuilder}.
     */
    protected LogMessageBuilder() {
        super();
    }

    /**
     * Appends given log message with without any log arguments.
     *
     * @param message The log message
     * @return This builder
     */
    public LogMessageBuilder append(String message) {
        return append(message, EMPTY_ARGS);
    }

    /**
     * Appends given log message with accompanying log arguments and a trailing line separator.
     * <pre>
     *   [message] + LF
     * </pre>
     *
     * @param message The log message
     * @param args The log arguments
     */
    public LogMessageBuilder appendln(String message, Object... args) {
        Object[] newArgs;
        if (args == null || args.length <= 0) {
            newArgs = new Object[] { Strings.getLineSeparator() };
        } else {
            newArgs = new Object[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = Strings.getLineSeparator();
        }
        return append(message + "{}", newArgs);
    }

    /**
     * Appends given log message with accompanying log arguments and a leading line separator.
     * <pre>
     *   LF + [message]
     * </pre>
     *
     * @param message The log message
     * @param args The log arguments
     */
    public LogMessageBuilder lfappend(String message, Object... args) {
        Object[] newArgs;
        if (args == null || args.length <= 0) {
            newArgs = new Object[] { Strings.getLineSeparator() };
        } else {
            newArgs = new Object[args.length + 1];
            newArgs[0] = Strings.getLineSeparator();
            System.arraycopy(args, 0, newArgs, 1, args.length);
        }
        return append("{}" + message, newArgs);
    }

    /**
     * Appends given log message with accompanying log arguments and a leading as well as a trailing line separator.
     * <pre>
     *   LF + [message] + LF
     * </pre>
     *
     * @param message The log message
     * @param args The log arguments
     */
    public LogMessageBuilder lfappendln(String message, Object... args) {
        Object[] newArgs;
        if (args == null || args.length <= 0) {
            newArgs = new Object[] { Strings.getLineSeparator(), Strings.getLineSeparator() };
        } else {
            newArgs = new Object[args.length + 2];
            newArgs[0] = Strings.getLineSeparator();
            System.arraycopy(args, 0, newArgs, 1, args.length);
            newArgs[newArgs.length - 1] = Strings.getLineSeparator();
        }
        return append("{}" + message + "{}", newArgs);
    }

    /**
     * Adds given log arguments.
     *
     * @param args The log arguments to add
     */
    public LogMessageBuilder add(Object... args) {
        return append("", args);
    }

    /**
     * Gets the gathered log arguments as array.
     *
     * @return The log arguments as array
     */
    public Object[] getArgumentsAsArray() {
        List<Object> args = getArguments();
        int size = args.size();
        return size <= 0 ? EMPTY_ARGS : args.toArray(new Object[size]);
    }

    /**
     * Appends given log message with accompanying log arguments.
     *
     * @param message The log message
     * @param args The log arguments
     * @return This builder
     */
    public abstract LogMessageBuilder append(String message, Object... args);

    /**
     * Gets the compiles log message.
     *
     * @return The log message
     */
    public abstract String getMessage();

    /**
     * Gets the gathered log arguments.
     *
     * @return The log arguments
     */
    public abstract List<Object> getArguments();

    /**
     * Resets this builder for being re-used.
     *
     * @return This builder in reset state
     */
    public abstract LogMessageBuilder reset();

    @Override
    public String toString() {
        return getMessage();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * The log message builder implementation.
     */
    private static class LogMessageBuilderImpl extends LogMessageBuilder {

        private final StringBuilder logMessageBuilder;
        private final List<Object> args;

        /**
         * Initializes a new {@link LogMessageBuilderImpl}.
         *
         * @param messageCapacity The capacity for log message
         * @param argsCapacity The capacity for log arguments
         */
        LogMessageBuilderImpl(int messageCapacity, int argsCapacity) {
            super();
            logMessageBuilder = new StringBuilder(messageCapacity);
            args = new ArrayList<>(argsCapacity);
        }

        @Override
        public LogMessageBuilder append(String message, Object... args) {
            logMessageBuilder.append(message);
            if (args != null) {
                for (Object arg : args) {
                    this.args.add(arg);
                }
            }
            return this;
        }

        @Override
        public String getMessage() {
            return logMessageBuilder.toString();
        }

        @Override
        public List<Object> getArguments() {
            return args;
        }

        @Override
        public LogMessageBuilder reset() {
            logMessageBuilder.setLength(0);
            args.clear();
            return this;
        }
    } // End of class LogMessageBuilderImpl

    /** The constant for empty log message builder */
    private static final LogMessageBuilder EMPTY = new LogMessageBuilder() {

        @Override
        public String getMessage() {
            return "";
        }

        @Override
        public Object[] getArgumentsAsArray() {
            return EMPTY_ARGS;
        }

        @Override
        public List<Object> getArguments() {
            return Collections.emptyList();
        }

        @Override
        public LogMessageBuilder append(String message, Object... args) {
            return this;
        }

        @Override
        public LogMessageBuilder reset() {
            return this;
        }
    };

}
