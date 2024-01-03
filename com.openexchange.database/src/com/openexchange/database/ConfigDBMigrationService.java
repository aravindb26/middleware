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

package com.openexchange.database;

import com.openexchange.database.migration.DBMigrationState;
import com.openexchange.exception.OXException;
import liquibase.changelog.ChangeSet;
import liquibase.resource.ResourceAccessor;

import java.util.List;

/**
 * {@link ConfigDBMigrationService} - A helper service for performing configdb migrations
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public interface ConfigDBMigrationService {

    /**
     * Schedules pending migrations for the config database.
     *
     * @param changeSetLocation The change set location
     * @return The scheduled migration
     * @throws OXException if the migration fails
     */
    DBMigrationState scheduleMigrations(String changeSetLocation, ResourceAccessor resourceAccessor) throws OXException;

    /**
     * Returns a list of the currently not executed change sets of the given changelog for the database.
     *
     * @param changeSetLocation The change set location
     * @return List with the currently not executed liquibase change sets
     * @throws OXException if listing the change sets fails
     */
    List<ChangeSet> listUnrunDBChangeSets(String changeSetLocation, ResourceAccessor resourceAccessor) throws OXException;

}
