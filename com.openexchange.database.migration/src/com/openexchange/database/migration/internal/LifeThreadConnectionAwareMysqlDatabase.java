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

package com.openexchange.database.migration.internal;

import java.sql.Connection;
import liquibase.database.core.MySQLDatabase;

/**
 * {@link LifeThreadConnectionAwareMysqlDatabase} is a {@link MySQLDatabase} which contains a second connection which can be used by a life thread.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LifeThreadConnectionAwareMysqlDatabase extends MySQLDatabase {

    private Connection lifeThreadCon = null;

    /**
     * Sets the {@link Connection} for the life thread which updates the timestamp of the liquibase lock
     *
     * @param con a Connection
     */
    public void setLifeThreadConnection(Connection con) {
        this.lifeThreadCon = con;
    }

    /**
     * Gets the connection for the life thread
     *
     * @return The life thread connection
     */
    public Connection getLifeThreadConnection() {
        return lifeThreadCon;
    }

}
