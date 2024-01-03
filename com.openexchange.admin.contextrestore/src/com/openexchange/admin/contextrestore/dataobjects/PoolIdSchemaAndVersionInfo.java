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

package com.openexchange.admin.contextrestore.dataobjects;

import java.io.File;
import java.util.Map;

/**
 * {@link PoolIdSchemaAndVersionInfo} - Simple helper class to remember results from parsing of a MySQL dump file for context restoration.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class PoolIdSchemaAndVersionInfo {

    private final int poolId;
    private final int contextId;
    private final String schema;
    private final String fileName;

    private Map<String, File> tempfilemap;
    private UpdateTaskInformation updateTaskInformation;

    /**
     * Initializes a new {@link PoolIdSchemaAndVersionInfo}.
     *
     * @param fileName The name of the MySQL dump file
     * @param contextId The identifier of the context that shall be restored
     * @param poolId The identifier of the database pool that is used by to-restore context
     * @param schema The name of the database schema in which to-restore context resides
     * @param updateTaskInformation Information about update tasks from source database schema
     */
    public PoolIdSchemaAndVersionInfo(final String fileName, final int contextId, int poolId, String schema, UpdateTaskInformation updateTaskInformation) {
        super();
        this.fileName = fileName;
        this.contextId = contextId;
        this.poolId = poolId;
        this.schema = schema;
        this.updateTaskInformation = updateTaskInformation;
    }

    public String getFileName() {
        return fileName;
    }

    public int getContextId() {
        return contextId;
    }

    public final int getPoolId() {
        return poolId;
    }

    public final String getSchema() {
        return schema;
    }

    public Map<String, File> getTempfilemap() {
        return tempfilemap;
    }

    public void setTempfilemap(Map<String, File> tempfilemap) {
        this.tempfilemap = tempfilemap;
    }

    public UpdateTaskInformation getUpdateTaskInformation() {
        return updateTaskInformation;
    }

    public void setUpdateTaskInformation(UpdateTaskInformation updateTaskInformation) {
        this.updateTaskInformation = updateTaskInformation;
    }

}