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

package com.openexchange.log;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link LogMessageAndArguments} - A (modifiable) pair of log message/line and associated arguments.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class LogMessageAndArguments {

    private final StringBuilder logMessage;
    private final List<Object> logArguments;
    private Throwable error;

    /**
     * Initializes a new {@link LogMessageAndArguments}.
     *
     * @param logMessage The log message/line
     * @param logArguments The associated log arguments
     */
    public LogMessageAndArguments(CharSequence logMessage, List<Object> logArguments) {
        this(logMessage, logArguments, null);
    }

    /**
     * Initializes a new {@link LogMessageAndArguments}.
     *
     * @param logMessage The log message/line
     * @param logArguments The associated log arguments
     */
    public LogMessageAndArguments(CharSequence logMessage, List<Object> logArguments, Throwable error) {
        super();
        this.logMessage = new StringBuilder(logMessage);
        this.logArguments = logArguments == null ? new ArrayList<>(0) : new ArrayList<>(logArguments);
        this.error = error;
    }

    /**
     * Resets this instance for being re-used.
     */
    public void reset() {
        error = null;
        logMessage.setLength(0);
        logArguments.clear();
    }

    /**
     * Sets the error
     *
     * @param error The error to set
     */
    public void setError(Throwable error) {
        this.error = error;
    }

    /**
     * Gets the error
     *
     * @return The error
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Gets the log message/line for being passed to e.g. {@link org.slf4j.Logger#info(String, Object...)}.
     *
     * @return The log message/line
     */
    public String getLogMessage() {
        return logMessage.toString();
    }

    /**
     * Gets the log arguments for being passed to e.g. {@link org.slf4j.Logger#info(String, Object...)}.
     *
     * @return The log arguments
     */
    public Object[] getLogArguments() {
        if (error == null) {
            return logArguments.toArray(new Object[logArguments.size()]);
        }

        Object[] args = new Object[logArguments.size() + 1];
        int i = 0;
        for (Object obj : logArguments) {
            args[i++] = obj;
        }
        args[i] = error;
        return args;
    }

    /**
     * Prepends given text to log message.
     *
     * @param arg The argument
     */
    public void prependText(String text) {
        logMessage.insert(0, text == null ? "null" : text);
    }

    /**
     * Appends given text to log message.
     *
     * @param arg The argument
     */
    public void appendText(String text) {
        logMessage.append(text == null ? "null" : text);
    }

    /**
     * Prepends given argument.
     *
     * @param arg The argument
     */
    public void prependArgument(Object arg) {
        logMessage.insert(0, "{}");
        logArguments.add(0, arg == null ? "null" : arg);
    }

    /**
     * Appends given argument.
     *
     * @param arg The argument
     */
    public void appendArgument(Object arg) {
        logMessage.append("{}");
        logArguments.add(arg == null ? "null" : arg);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(32);
        builder.append('(');
        if (logMessage != null) {
            builder.append("logMessage=").append(logMessage).append(", ");
        }
        if (logArguments != null) {
            builder.append("logArguments=").append(logArguments).append(", ");
        }
        if (error != null) {
            builder.append("error=").append(error.getMessage());
        }
        builder.append(')');
        return builder.toString();
    }

}
