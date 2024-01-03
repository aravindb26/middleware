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

package com.openexchange.deputy.provider.folderservice.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONServices;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.deputy.DeputyExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.server.ServiceLookup;

/**
 * {@link DatabaseFolderUtils}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DatabaseFolderUtils {

    /**
     * Initializes a new {@link DatabaseFolderUtils}.
     */
    private DatabaseFolderUtils() {
        super();
    }

    /**
     * Determines all folders having a permission for given deputy.
     *
     * @param deputyId The deputy identifier
     * @param contentType The content type
     * @param contextId The context identifier
     * @param services The service look-up
     * @return A mapping of folder identifier to metadata
     * @throws OXException If folders cannot be determined
     */
    public static Map<String, Map<String, Object>> getAllFolderIdsHavingDeputy(String deputyId, ContentType contentType, int contextId, ServiceLookup services) throws OXException {
        DatabaseService databaseService = services.getServiceSafe(DatabaseService.class);
        Connection con = databaseService.getReadOnly(contextId);
        try {
            return getAllFolderIdsHavingDeputy(deputyId, contentType, contextId, con);
        } finally {
            databaseService.backReadOnly(contextId, con);
        }
    }

    private static Map<String, Map<String, Object>> getAllFolderIdsHavingDeputy(String deputyId, ContentType contentType, int contextId, Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT meta FROM oxfolder_tree WHERE cid=? AND module=? AND meta LIKE '%\"" + deputyId + "\"%'");
            stmt.setInt(1, contextId);
            stmt.setInt(2, contentType.getModule());
            rs = stmt.executeQuery();
            if (rs.next() == false) {
                return Collections.emptyMap();
            }

            Map<String, Map<String, Object>> retval = new LinkedHashMap<>();
            do {
                Map<String, Object> metadata = JSONServices.parseObject(rs.getString(1)).asMap();
                @SuppressWarnings("unchecked") Map<String, Object> deputyMetadata = (Map<String, Object>) metadata.get(deputyId);
                if (deputyMetadata != null) {
                    retval.put((String) deputyMetadata.get("folderId"), metadata);
                }
            } while (rs.next());
            return retval;
        } catch (SQLException e) {
            throw DeputyExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (JSONException e) {
            throw DeputyExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

}
