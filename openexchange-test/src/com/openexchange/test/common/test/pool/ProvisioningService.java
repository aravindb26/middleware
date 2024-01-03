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

package com.openexchange.test.common.test.pool;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.TestUserConfig;

/**
 * {@link ProvisioningService}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v8.0.0
 */
public interface ProvisioningService {

    /**
     * Creates a context
     *
     * @param createdBy The class name that created the context
     * @return The context as {@link TestContext}
     * @throws OXException If context can't be created
     */
    TestContext createContext(String createdBy) throws OXException;

    /**
     * Creates a context
     *
     * @param config The configuration to pass for context creation
     * @param createdBy The class name that created the context
     * @return The context as {@link TestContext}
     * @throws OXException If context can't be created
     */
    TestContext createContext(TestContextConfig config, String createdBy) throws OXException;

    /**
     * Changes a context
     *
     * @param cid The context identifier
     * @param changedBy The class name that changed the context
     * @param config The configuration
     * @throws OXException If context can't be changed
     */
    void changeContext(int cid, TestContextConfig config, String changedBy) throws OXException;

    /**
     * Delete a context
     *
     * @param cid The context identifier
     * @param deletedBy The class name that deleted the context
     * @throws OXException If context can't be deleted
     */
    void deleteContext(int cid, String deletedBy) throws OXException;

    /**
     * Creates a user in the given context
     *
     * @param cid The context identifier of the context the user shall be created in
     * @param createdBy The class name that created the user
     * @return The user as {@link TestUser}
     * @throws OXException If user can't be created
     */
    TestUser createUser(int cid, String createdBy) throws OXException;

    /**
     * Creates a user in the given context
     *
     * @param cid The context identifier of the context the user shall be created in
     * @param userLogin The login name of the user.
     * @param userConfig The optional user configuration
     * @param createdBy The class name that created the user
     * @return The user as {@link TestUser}
     * @throws OXException If user can't be created
     */
    TestUser createUser(int cid, String userLogin, Optional<TestUserConfig> userConfig, String createdBy) throws OXException;

    /**
     * Changes an existing user
     *
     * @param cid The context ID
     * @param userID The user ID
     * @param config The optional user configuration
     * @param changedBy The class name that changed the user
     * @throws OXException If user can't be changed
     */
    void changeUser(int cid, int userID, Optional<Map<String, String>> config, String changedBy) throws OXException;

    /**
     * Changes an existing user
     *
     * @param cid The context ID
     * @param userID The user ID
     * @param changedBy The class name that changed the user
     * @throws OXException If user can't be changed
     */
    void deleteUser(int cid, int userID, String changedBy) throws OXException;

    /**
     * Get the module access of the user
     *
     * @param testUser The user
     * @return The module access
     * @throws OXException If the access can't be get
     */
    default UserModuleAccess getModuleAccess(TestUser testUser) throws OXException {
        return getModuleAccess(testUser.getContextId(), testUser.getUserId());
    }

    /**
     * Get the module access of the user
     *
     * @param contextId The context id
     * @param userId the user id
     * @return The module access
     * @throws OXException If the access can't be get
     */
    UserModuleAccess getModuleAccess(int contextId, int userId) throws OXException;

    /**
     * Updates user capabilities
     *
     * @param cid The context id
     * @param userID The user id
     * @param capsToAdd Capabilities to add
     * @param capsToRemove Capabilities to remove
     * @param capsToDrop Capabilities to drop
     * @param changedBy The class name that changed the capabilities
     * @throws OXException If user can't be updated
     */
    void changeCapability(int cid, int userID, Set<String> capsToAdd, Set<String> capsToRemove, Set<String> capsToDrop, String changedBy) throws OXException;

    /**
     * Updates user module access
     *
     * @param testUser The test user to change the access for
     * @param userModuleAccess The module accesses to set
     * @param readCreateSharedFolders Enable/ disable readCreateSharedFolders module access
     * @param editPublicFolders Enable/ disable editPublicFolders module access
     * @param changedBy The class name that changed the module access
     * @throws OXException If user module access can't be changed
     */
    default void changeModuleAccess(TestUser testUser, UserModuleAccess userModuleAccess, String changedBy) throws OXException {
        changeModuleAccess(testUser.getContextId(), testUser.getUserId(), userModuleAccess, changedBy);
    }

    /**
     * Updates user module access
     *
     * @param cid The context id
     * @param userID the user id
     * @param userModuleAccess The module accesses to set
     * @param readCreateSharedFolders Enable/ disable readCreateSharedFolders module access
     * @param editPublicFolders Enable/ disable editPublicFolders module access
     * @param changedBy The class name that changed the module access
     * @throws OXException If user module access can't be changed
     */
    void changeModuleAccess(int cid, int userID, UserModuleAccess userModuleAccess, String changedBy) throws OXException;

    /**
     * Creates a group in the given context
     *
     * @param cid The context identifier of the context the group shall be created in
     * @param optUserIds Optional users to add to the group
     * @param createdBy The class name that created the group
     * @return The group identifier
     * @throws OXException If group can't be created
     */
    Integer createGroup(int cid, Optional<List<Integer>> optUserIds, String createdBy) throws OXException;

    /**
     * Creates a resource in the given context
     *
     * @param cid The context identifier of the context the resource shall be created in
     * @param createdBy The class name that created the resource
     * @return The resource identifier
     * @throws OXException If group can't be created
     */
    Integer createResource(int cid, String createdBy) throws OXException;

    /**
     * Creates a resource in the given context
     *
     * @param cid The context identifier of the context the resource shall be created in
     * @param resourceId The resource ID
     * @return The resource as {@link TestUser}
     * @throws OXException If group can't be created
     */
    TestUser getResource(int cid, int resourceId) throws OXException;

    /**
     * Registers a oauth client with the given name
     *
     * @param name The client name
     * @param registeredBy The class name that registered the client
     * @return The returned client
     * @throws OXException
     */
    Client registerOAuthClient(String name, String registeredBy) throws OXException;

    /**
     * Registers the given oauth client
     *
     * @param client The client to register
     * @param registeredBy The class name that registered the client
     * @return The returned client
     * @throws OXException
     */
    Client registerOAuthClient(Client client, String registeredBy) throws OXException;

    /**
     * Updates the given oauth client
     *
     * @param client The client to update
     * @param updatedBy The class name that updated the client
     * @return The returned client
     * @throws OXException
     */
    Client updateOAuthClient(Client client, String updatedBy) throws OXException;

    /**
     * Unregisters the client with the given id
     *
     * @param clientId The client id
     * @param unregisteredBy The class name that unregistered the client
     * @throws OXException
     */
    void unregisterOAuthClient(String clientId, String unregisteredBy) throws OXException;
}
