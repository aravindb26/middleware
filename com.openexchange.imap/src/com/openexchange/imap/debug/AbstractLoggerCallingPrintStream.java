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

package com.openexchange.imap.debug;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import com.openexchange.java.Strings;

/**
 * {@link AbstractLoggerCallingPrintStream} - A print stream writing passed bytes to an instance of <code>org.slf4j.Logger</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.4
 */
public abstract class AbstractLoggerCallingPrintStream extends PrintStream {

    private org.slf4j.Logger logger;
    private final StringBuffer buf;

    /**
     * Initializes a new {@link AbstractLoggerCallingPrintStream}.
     */
    protected AbstractLoggerCallingPrintStream() {
        super(System.out); // hopefully nothing will actually reach stdout
        buf = new StringBuffer();
    }

    /**
     * Creates the logger to use.
     *
     * @return The logger or empty if creation failed
     */
    protected abstract Optional<org.slf4j.Logger> createLogger();

    /**
     * Outputs buffer's content to given logger.
     *
     * @param buf The buffer
     * @param logger The logger
     */
    protected void logBufferContent(StringBuffer buf, org.slf4j.Logger logger) {
        logger.info("{}", buf);
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public synchronized void flush() {
        if (buf.length() > 0) {
            org.slf4j.Logger logger = this.logger;
            if (logger == null) {
                Optional<org.slf4j.Logger> optLogger = createLogger();
                if (optLogger.isPresent()) {
                    logger = optLogger.get();
                    this.logger = logger;
                }
            }
            if (logger != null) {
                logBufferContent(buf, logger);
            }
            buf.setLength(0);
        }
    }

    @Override
    public void print(boolean b) {
        print(Boolean.toString(b));
    }

    @Override
    public void print(char c) {
        print(Character.toString(c));
    }

    @Override
    public void print(char[] s) {
        print(new String(s));
    }

    @Override
    public void print(double d) {
        print(Double.toString(d));
    }

    @Override
    public void print(float f) {
        print(Float.toString(f));
    }

    @Override
    public void print(int i) {
        print(Integer.toString(i));
    }

    @Override
    public void print(long l) {
        print(Long.toString(l));
    }

    @Override
    public void print(Object obj) {
        print(obj == null ? "null" : obj.toString());
    }

    @Override
    public void print(String s) {
        buf.append(s);
    }

    @Override
    public void println() {
        flush();
    }

    @Override
    public void println(boolean x) {
        println(Boolean.toString(x));
    }

    @Override
    public void println(char x) {
        println(Character.toString(x));
    }

    @Override
    public void println(char[] x) {
        println(new String(x));
    }

    @Override
    public void println(double x) {
        println(Double.toString(x));
    }

    @Override
    public void println(float x) {
        println(Float.toString(x));
    }

    @Override
    public void println(int x) {
        println(Integer.toString(x));
    }

    @Override
    public void println(long x) {
        println(Long.toString(x));
    }

    @Override
    public void println(Object x) {
        println(x == null ? "null" : x.toString());
    }

    @Override
    public void println(String x) {
        buf.append(x);
        flush();
    }

    @Override
    public void write(byte[] aBuf, int off, int len) {
        String str = new String(aBuf, off, len, StandardCharsets.UTF_8);
        String[] lines = Strings.splitByCRLF(str);
        for (int i = 0; i < lines.length - 1; i++) {
            println(lines[i]);
        }
        String lastLine = lines[lines.length - 1];
        if (lastLine.endsWith("\n")) {
            println(lastLine);
        } else {
            print(lastLine);
        }
    }

    @Override
    public void write(int b) {
        print(new String(new byte[] { (byte) b }, StandardCharsets.UTF_8));
    }

}
