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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.framework.ConfigurableResource;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestUserConfig;
import com.openexchange.test.common.test.pool.soap.SoapProvisioningService;

/**
 * {@link TestContext}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v7.8.3
 */
public class TestContext implements Serializable, ConfigurableResource {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -8836508664321761890L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TestContext.class);

    private final String name;
    private final int contextId;

    private final AtomicReference<TestUser> contextAdmin = new AtomicReference<>();
    private final AtomicReference<TestUser> noReplyUser = new AtomicReference<>();

    private String usedByTest;

    /** Overall test users by this context */
    private final LinkedList<TestUser> users = new LinkedList<>();

    /** Pre-provisioned test users */
    private final LinkedList<TestUser> userPool = new LinkedList<>();

    /**
     * Initializes a new {@link TestContext}.
     *
     * @param contextId The context identifier
     * @param name The name
     * @param admin The admin of the context
     * @param usedBy The test class using this the context
     * @param users already existing users for this context
     */
    public TestContext(int contextId, String name, TestUser admin, String usedByTest, List<TestUser> users) {
        this.name = name;
        this.contextId = contextId;
        this.contextAdmin.set(admin);
        this.usedByTest = usedByTest;
        if (users != null && !users.isEmpty()) {
            this.users.addAll(users);
            this.userPool.addAll(users);
        }
    }

    public TestContext(int contextId, String name, TestUser admin, String usedByTest) {
        this(contextId, name, admin, usedByTest, null);
    }

    /**
     * Get the context admin
     *
     * @return The admin as {@link TestUser}
     */
    public TestUser getAdmin() {
        return contextAdmin.get();
    }

    /**
     * Get the context name
     *
     * @return The context name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the test that uses this {@link TestContext}.
     *
     * @return The user of this test context
     */
    public String getUsedBy() {
        return usedByTest;
    }

    /**
     * Set the creator of this {@link TestContext}.
     *
     * @param createdBy
     */
    public void setCreatedBy(String createdBy) {
        this.usedByTest = createdBy;
    }

    @Override
    public void configure(TestClassConfig testConfig) throws Exception {
        if (testConfig.optUserConfig().isPresent() || testConfig.getNumberOfusersPerContext() > ProvisioningUtils.USER_NAMES_POOL.length) {
            for (int i = 0; i < testConfig.getNumberOfusersPerContext(); i++) {
                TestUser createUser = createUser(testConfig.optUserConfig(), usedByTest);
                if (null != createUser) {
                    userPool.addFirst(createUser);
                    users.addFirst(createUser);
                }
            }
        }
    }

    /**
     * Acquire a unique user from this context
     *
     * @return A unique user
     */
    public TestUser acquireUser() {
        // Get pre-provisioned user
        TestUser user = userPool.poll();
        if (null != user) {
            return user;
        }
        // Create new user
        TestUser createUser = createUser(Optional.empty(), usedByTest);
        users.add(createUser);
        return createUser;
    }

    public TestUser acquireUser(String username, Optional<TestUserConfig> userConfig) {
        // Create new user
        TestUser createUser = createUser(username, userConfig, usedByTest);
        users.add(createUser);
        return createUser;
    }

    private TestUser createUser(Optional<TestUserConfig> userConfig, String acquiredBy) {
        try {
            return ConfigAwareProvisioningService.getService().createUser(contextId, getUserNameFromPool(), userConfig, acquiredBy);
        } catch (OXException e) {
            LOG.error("Unable to pre provision test user", e);
        }
        return null;
    }

    private TestUser createUser(String username, Optional<TestUserConfig> userConfig, String acquiredBy) {
        try {
            return ConfigAwareProvisioningService.getService().createUser(contextId, username, userConfig, acquiredBy);
        } catch (OXException e) {
            LOG.error("Unable to pre provision test user", e);
        }
        return null;
    }

    /**
     *
     * Gets the next unused user name from the user name pool.
     *
     * @return The user name. Returns null, if the user name pool can not be read of if there are more users than names in the pool.
     */
    private String getUserNameFromPool() {
        int usedUserSize = users.size();
        String[] pool = ProvisioningUtils.USER_NAMES_POOL;
        if (pool == null) {
            return null;
        }
        int poolSize = pool.length;
        if (usedUserSize < poolSize) {
            try {
                return pool[users.size()];
            } catch (@SuppressWarnings("unused") Exception e) {
                return null;
            }
        }
        return null;
    }

    private final Random rand = new Random(System.currentTimeMillis());

    /**
     * Gets a user from this context. Can be a user that has already
     * been acquired by {@link #acquireUser()}
     *
     * @return A user
     */
    public TestUser getRandomUser() {
        if (userPool.isEmpty()) {
            // Create a new one
            return acquireUser();
        }
        if (1 == userPool.size()) {
            TestUser result = userPool.poll();
            if (result != null) {
                return result;
            }
        }

        int next = rand.nextInt(userPool.size());
        return userPool.remove(next);
    }

    /**
     * Get all created users in this context
     *
     * @return All users
     */
    public List<TestUser> getUsers() {
        return users;
    }

    /**
     * Acquire a resource from this context
     *
     * @return The resource identifier
     */
    public Integer acquireResource() {
        try {
            return SoapProvisioningService.getInstance().createResource(contextId, usedByTest);
        } catch (OXException e) {
            LOG.error("Unable to acquire resource", e);
            Assertions.fail();
        }
        return null;
    }

    /**
     * Acquire a group from this context
     *
     * @param optUsers The optional list of group members
     * @return The group identifier
     */
    public Integer acquireGroup(Optional<List<Integer>> optUsers) {
        try {
            return SoapProvisioningService.getInstance().createGroup(contextId, optUsers, usedByTest);
        } catch (OXException e) {
            LOG.error("Unable to acquire group", e);
            Assertions.fail();
        }
        return null;
    }

    /**
     * Gets the context id.
     *
     * @return The context id.
     */
    public int getId() {
        return contextId;
    }

    /**
     * Acquire the NoReply user from this context
     *
     * @return The no reply user as {@link TestUser}
     */
    public TestUser acquireNoReplyUser() {
        if (noReplyUser.get() == null) {
            synchronized (this) {
                if (noReplyUser.get() == null) {
                    noReplyUser.set(acquireUser());
                    try {
                        SoapProvisioningService.getInstance().changeContexConfig(this.contextId, Collections.singletonMap("com.openexchange.noreply.address", noReplyUser.get().getLogin()), usedByTest);
                    } catch (OXException e) {
                        LOG.error("Unable to change config for no reply address", e);
                        Assertions.fail();
                    }
                }
            }
        }
        return noReplyUser.get();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TestContext other = (TestContext) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equalsIgnoreCase(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        builder.append("TestContext [");
        if (Strings.isNotEmpty(name)) {
            builder.append("name=").append(name).append(", ");
        }
        builder.append("contextId=").append(contextId);
        builder.append(']');
        return builder.toString();
    }

}
