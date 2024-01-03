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

package com.openexchange.database.cleanup.impl;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * {@link DatabaseCleanUpUncaughtExceptionhandler} - The uncaught exception handler for database clean-up.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.8.1
 */
public final class DatabaseCleanUpUncaughtExceptionhandler implements UncaughtExceptionHandler {

    private static final DatabaseCleanUpUncaughtExceptionhandler INSTANCE = new DatabaseCleanUpUncaughtExceptionhandler();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static DatabaseCleanUpUncaughtExceptionhandler getInstance() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------------------------------------- //

    /**
     * Initializes a new {@link DatabaseCleanUpUncaughtExceptionhandler}.
     */
    private DatabaseCleanUpUncaughtExceptionhandler() {
        super();
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseCleanUpUncaughtExceptionhandler.class);
        logger.error("Thread '{}' terminated abruptly with an uncaught RuntimeException or Error.", t.getName(), e);
    }

}
