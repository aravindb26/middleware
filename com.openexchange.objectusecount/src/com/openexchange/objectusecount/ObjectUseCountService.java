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

package com.openexchange.objectusecount;

import static com.openexchange.objectusecount.FolderId2ObjectIdsMapping.singletonMappingFor;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;

/**
 * {@link ObjectUseCountService}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.1
 */
public interface ObjectUseCountService {

    /** The identifier for contact module */
    static final int CONTACT_MODULE = 3;

    /** The identifier for default account */
    static final int DEFAULT_ACCOUNT = 0;

    /**
     * Get use count for object in specified folder.
     *
     * @param session The associated session
     * @param moduleId The identifier of the module in which the object resides
     * @param accountId The identifier of the account in which the object resides
     * @param folderId The identifier of the folder in which the object resides
     * @param objectId The identifier of the object
     * @return The object's use count
     * @throws OXException If use count cannot be returned
     */
    default int getUseCount(Session session, int moduleId, int accountId, String folderId, String objectId) throws OXException {
        Map<String, Integer> objectId2Count = getUseCounts(session, moduleId, accountId, singletonMappingFor(folderId, objectId)).get(folderId);
        if (objectId2Count == null) {
            return 0;
        }
        Integer value = objectId2Count.get(objectId);
        return null != value ? value.intValue() : 0;
    }

    /**
     * Get use count for objects in specified folders.
     *
     * @param session The associated session
     * @param moduleId The identifier of the module in which the objects reside
     * @param accountId The identifier of the account in which the objects reside
     * @param folderId2objectIds The object identifiers to retrieve use counts for mapped by folder identifier
     * @return The objects' use counts
     * @throws OXException If use counts cannot be returned
     */
    Map<String, Map<String, Integer>> getUseCounts(Session session, int moduleId, int accountId, FolderId2ObjectIdsMapping folderId2objectIds) throws OXException;

    /**
     * Sets use count according to specified arguments
     *
     * @param session The associated session
     * @param arguments The arguments determining how/what to set
     * @throws OXException If setting user count(s) fails and arguments signal to throw an error
     */
    void setUseCount(Session session, SetArguments arguments) throws OXException;

    /**
     * Increment use count for specified arguments
     *
     * @param session The associated session
     * @param arguments The arguments determining how/what to update
     * @throws OXException If incrementing user count(s) fails and arguments signal to throw an error
     */
    void incrementUseCount(Session session, IncrementArguments arguments) throws OXException;

    /**
     * Deletes all use counts for specified account, e.g. if the user removes external account storage
     *
     * @param session The associated session
     * @param moduleId The identifier of the module in which the object resides
     * @param accountId The identifier of the account
     * @throws OXException If deletion fails
     */
    void deleteUseCountsForAccount(Session session, int moduleId, int accountId) throws OXException;

}
