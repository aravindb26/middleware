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

/**
 * {@link ExceptionCatchingRunnable} - A simple utility class to make sure we don't let exception slip away and stop execution.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public interface ExceptionCatchingRunnable extends Runnable {

    /** The logger constant */
    static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExceptionCatchingRunnable.class);

    @Override
    default void run() {
        try {
            runMayThrow();
        } catch (Exception e) {
            LOGGER.error("Error while executing task {}", getClass().getName(), e);
        }
    }

    /**
     * Runs this operation.
     *
     * @throws Exception If operation fails
     */
    void runMayThrow() throws Exception;

}
