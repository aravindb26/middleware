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

package com.openexchange.ajax.resource;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.group.GroupStorage;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.test.pool.ProvisioningUtils;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.AllResourcesResponse;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.ResourceData;
import com.openexchange.testing.httpclient.models.ResourcePermission;
import com.openexchange.testing.httpclient.models.ResourcePermission.PrivilegeEnum;
import com.openexchange.testing.httpclient.models.ResourceResponse;
import com.openexchange.testing.httpclient.models.ResourceUpdateResponse;
import com.openexchange.testing.httpclient.modules.ResourcesApi;

/**
 * {@link ResourcePermissionsTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class ResourcePermissionsTest extends AbstractConfigAwareAPIClientSession {

    private static final List<ResourcePermission> DEFAULT_PERMISSIONS = Collections.singletonList(new ResourcePermission()
        .entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.BOOK_DIRECTLY))
    ;

    private ResourcesApi resourceApi;
    private Integer resourceId;

    @Override
    protected Map<String, String> getNeededConfigurations() {
        return Collections.singletonMap("com.openexchange.resource.simplePermissionMode", "false");
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        resourceApi = new ResourcesApi(getApiClient());
        ResourceData resourceData = new ResourceData();
        String name = UUIDs.getUnformattedStringFromRandom();
        resourceData.setDisplayName(name);
        resourceData.setName(name);
        resourceData.setMailaddress(ProvisioningUtils.getMailAddress(name, testContext.getId()));
        ResourceUpdateResponse response = resourceApi.createResource(resourceData);
        assertNull(response.getError(), response.getErrorDesc());
        resourceId = response.getData().getId();
    }

    @Test
    public void testDefaultPermissions() throws Exception {
        /*
         * get all resource ids
         */
        AllResourcesResponse allResourcesResponse = resourceApi.getAllResources();
        assertNull(allResourcesResponse.getError(), allResourcesResponse.getErrorDesc());
        /*
         * get each resource & check that only the default permission is defined
         */
        for (Integer resourceId : allResourcesResponse.getData()) {
            ResourceResponse resourceResponse = resourceApi.getResource(resourceId);
            assertNull(resourceResponse.getError(), resourceResponse.getErrorDesc());
            assertPermissions(DEFAULT_PERMISSIONS, resourceResponse.getData().getPermissions());
        }
    }

    @Test
    public void testUpdatePermissions() throws Exception {
        /*
         * assign a delegate permission for the current user & update resource
         */
        ResourceResponse resourceResponse = resourceApi.getResource(resourceId);
        Long clientTimestamp = resourceResponse.getTimestamp();
        List<ResourcePermission> permissionUpdate = Arrays.asList(
            new ResourcePermission().entity(I(getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        );
        updateResourcePermissions(resourceApi, resourceId, clientTimestamp, permissionUpdate, false);
        /*
         * check that the permissions got applied properly
         */
        resourceResponse = resourceApi.getResource(resourceId);
        clientTimestamp = resourceResponse.getTimestamp();
        assertPermissions(permissionUpdate, resourceResponse.getData().getPermissions());
        /*
         * add another delegate permission & check again
         */
        permissionUpdate = Arrays.asList(
            new ResourcePermission().entity(I(getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(testUser2.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        );
        updateResourcePermissions(resourceApi, resourceId, clientTimestamp, permissionUpdate, false);
        resourceResponse = resourceApi.getResource(resourceId);
        clientTimestamp = resourceResponse.getTimestamp();
        assertPermissions(permissionUpdate, resourceResponse.getData().getPermissions());
        /*
         * remove initial delegate permission & check again
         */
        permissionUpdate = Arrays.asList(
            new ResourcePermission().entity(I(testUser2.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        );
        updateResourcePermissions(resourceApi, resourceId, clientTimestamp, permissionUpdate, false);
        resourceResponse = resourceApi.getResource(resourceId);
        clientTimestamp = resourceResponse.getTimestamp();
        assertPermissions(permissionUpdate, resourceResponse.getData().getPermissions());
    }

    @Test
    public void testRemovePermissions() throws Exception {
        /*
         * assign a delegate permission for the current user & update resource
         */
        ResourceResponse resourceResponse = resourceApi.getResource(resourceId);
        Long clientTimestamp = resourceResponse.getTimestamp();
        List<ResourcePermission> permissionUpdate = Arrays.asList(
            new ResourcePermission().entity(I(getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        );
        updateResourcePermissions(resourceApi, resourceId, clientTimestamp, permissionUpdate, false);
        /*
         * check that the permissions got applied properly
         */
        resourceResponse = resourceApi.getResource(resourceId);
        clientTimestamp = resourceResponse.getTimestamp();
        assertPermissions(permissionUpdate, resourceResponse.getData().getPermissions());
        /*
         * remove permissions & check again
         */
        permissionUpdate = Collections.emptyList();
        updateResourcePermissions(resourceApi, resourceId, clientTimestamp, permissionUpdate, false);
        resourceResponse = resourceApi.getResource(resourceId);
        clientTimestamp = resourceResponse.getTimestamp();
        assertPermissions(DEFAULT_PERMISSIONS, resourceResponse.getData().getPermissions());
    }

    @Test
    public void testInvalidPermissions() throws Exception {
        /*
         * prepare some invalid permission sets
         */
        int nonExistentEntity = 353255325;
        List<List<ResourcePermission>> invalidPermissionLists = new ArrayList<>();
        invalidPermissionLists.add(Arrays.asList(
            new ResourcePermission().entity(I(getUserId())).group(Boolean.TRUE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        ));
        invalidPermissionLists.add(Arrays.asList(
            new ResourcePermission().entity(I(getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.FALSE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        ));
        invalidPermissionLists.add(Arrays.asList(
            new ResourcePermission().entity(I(nonExistentEntity)).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        ));
        invalidPermissionLists.add(Arrays.asList(
            new ResourcePermission().entity(I(getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(nonExistentEntity)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        ));
        invalidPermissionLists.add(Arrays.asList(
            new ResourcePermission().entity(I(getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GUEST_GROUP_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        ));
        invalidPermissionLists.add(Arrays.asList(
            new ResourcePermission().entity(I(GroupStorage.GUEST_GROUP_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        ));
        invalidPermissionLists.add(Arrays.asList(
            new ResourcePermission().entity(I(getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        ));
        /*
         * try and update resource & check response, then verify that resource was not changed
         */
        ResourceResponse resourceResponse = resourceApi.getResource(resourceId);
        Long clientTimestamp = resourceResponse.getTimestamp();
        for (List<ResourcePermission> invalidPermissions : invalidPermissionLists) {
            updateResourcePermissions(resourceApi, resourceId, clientTimestamp, invalidPermissions, true);
            ResourceResponse reloadedResourceResponse = resourceApi.getResource(resourceId);
            assertPermissions(resourceResponse.getData().getPermissions(), reloadedResourceResponse.getData().getPermissions());
        }
    }

    private static CommonResponse updateResourcePermissions(ResourcesApi resourceApi, Integer resourceId, Long clientTimestamp, List<ResourcePermission> permissionUpdate, boolean expectToFail) throws ApiException {
        ResourceData resourceUpdate = new ResourceData().id(resourceId).permissions(permissionUpdate);
        CommonResponse updateResponse = resourceApi.updateResource(resourceId, clientTimestamp, resourceUpdate);
        if (expectToFail) {
            assertNotNull(updateResponse.getError());
        } else {
            assertNull(updateResponse.getError());
        }
        return updateResponse;
    }

    private static void assertPermissions(Collection<ResourcePermission> expectedPermissions, Collection<ResourcePermission> actualPermissions) {
        if (null == expectedPermissions || 0 == expectedPermissions.size()) {
            assertTrue(null == actualPermissions || 0 == actualPermissions.size());
        } else {
            assertNotNull(actualPermissions);
            assertEquals(expectedPermissions.size(), actualPermissions.size());
            for (ResourcePermission expectedPermission : expectedPermissions) {
                ResourcePermission actualPermission = find(actualPermissions, expectedPermission.getEntity());
                assertNotNull(actualPermission);
                assertEquals(expectedPermission.getGroup(), actualPermission.getGroup());
                assertEquals(expectedPermission.getEntity(), actualPermission.getEntity());
                assertEquals(expectedPermission.getPrivilege(), actualPermission.getPrivilege());
            }
        }
    }

    private static ResourcePermission find(Collection<ResourcePermission> permissions, Integer entity) {
        if (null != permissions) {
            for (ResourcePermission permission : permissions) {
                if (entity.equals(permission.getEntity())) {
                    return permission;
                }
            }
        }
        return null;
    }

}
