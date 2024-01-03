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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.exception.OXException;
import com.openexchange.java.Autoboxing;
import com.openexchange.java.ConcurrentList;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;

/**
 * {@link TestContextPool} - This class will manage the context handling, esp. providing unused contexts and queue related requests
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.3
 */
public class TestContextPool {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TestContextPool.class);

    private static List<TestContext> allTimeContexts = new ConcurrentList<>();

    private static HttpClient GLOBALDB_CLIENT;
    private static String scheme = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);

    private static synchronized HttpClient getInstance() {
        if (GLOBALDB_CLIENT == null) {
            boolean useSSL = scheme.equals("https");
            if (useSSL) {
                try {
                    SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(), NoopHostnameVerifier.INSTANCE);
                    GLOBALDB_CLIENT = HttpClients.custom().setSSLSocketFactory(scsf).build();
                } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                    LOG.error("Not able to create SSLConnectionSocketFactory! Will fall back to non SSL");
                    scheme = "http";
                    GLOBALDB_CLIENT = HttpClients.custom().build();
                }
            } else {
                PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
                connManager.setMaxTotal(50);
                connManager.setDefaultMaxPerRoute(50);

                GLOBALDB_CLIENT = HttpClients.custom().setConnectionManager(connManager).build();
            }
        }
        return GLOBALDB_CLIENT;

    }

    /**
     * Returns an exclusive {@link TestContext} which means this context is currently not used by any other test.<br>
     * <br>
     * <b>Caution: After using the {@link TestContext} make sure it will be returned to pool by using {@link #deleteContext(List)}!</b>
     *
     * @param acquiredByTest The name of the class that acquires the context
     * @return {@link TestContext} to be used for tests.
     * @throws OXException In case context can't be acquired
     */
    public static TestContext acquireContext(String acquiredByTest) throws OXException {
        TestClassConfig config = TestClassConfig.builder().build();
        return acquireContext(acquiredByTest, Optional.of(config));
    }

    /**
     * Returns an exclusive {@link TestContext} which means this context is currently not used by any other test.<br>
     * <br>
     * <b>Caution: After using the {@link TestContext} make sure it will be returned to pool by using {@link #deleteContext(List)}!</b>
     *
     * @param testInfo
     * @param config The optional context config
     * @return {@link TestContext} to be used for tests.
     * @throws OXException In case context can't be acquired
     */
    public static TestContext acquireContext(TestInfo testInfo, Optional<TestClassConfig> config) throws OXException {
        if (testInfo.getTestClass().isPresent()) {
            if (testInfo.getTestMethod().isPresent()) {
                return acquireContext(testInfo.getTestClass().get().getCanonicalName() + "." + testInfo.getTestMethod().get().getName(), config);
            }
            LOG.warn("Using fallback for test method name. TestInfo: {}", testInfo);
            return acquireContext(testInfo.getTestClass().get().getCanonicalName() + "." + UUID.randomUUID().toString(), config);
        }
        LOG.warn("Using fallback for test class name. TestInfo: {}", testInfo);
        return acquireContext(TestContextPool.class.getCanonicalName() + "." + testInfo.getTestMethod().get().getName(), config);
    }

    /**
     * Returns an exclusive {@link TestContext} which means this context is currently not used by any other test.<br>
     * <br>
     * <b>Caution: After using the {@link TestContext} make sure it will be returned to pool by using {@link #deleteContext(List)}!</b>
     *
     * @param acquiredByTest The name of the class that acquires the context
     * @param config The optional context config
     * @return {@link TestContext} to be used for tests.
     * @throws OXException In case context can't be acquired
     */
    public static TestContext acquireContext(String acquiredByTest, Optional<TestClassConfig> config) throws OXException {
        List<TestContext> contextList = acquireOrCreateContexts(acquiredByTest, config, 1);
        assertFalse(contextList == null || contextList.isEmpty(), "Unable to acquire test context due to an empty pool.");
        TestContext context = contextList.get(0);
        LOG.debug("Context '{}' has been acquired by {}.", context.getName(), acquiredByTest);
        return context;
    }

    /**
     * <br>
     * Similar to {@link #acquireContext(String)} but returns any number of contexts instead of only one <br>
     * <br>
     * In addition the contexts will be created if config is provided or has changed
     *
     * @param acquiredByTest The name of the class that acquires the context
     * @param testClassConfig The optional context config
     * @param amount The amount of contexts to acquire
     * @return a list of {@link TestContext} to be used for tests.
     * @throws OXException In case context can't be acquired
     */
    public static List<TestContext> acquireOrCreateContexts(String acquiredByTest, Optional<TestClassConfig> testClassConfig, int amount) throws OXException {
        if (testClassConfig.isPresent() && testClassConfig.get().isEmptyContext()) {
            return createEmptyContexts(acquiredByTest, amount);
        }
        List<TestContext> result;
        if (testClassConfig.isPresent() && testClassConfig.get().optContextConfig().isPresent() && testClassConfig.get().optContextConfig().get().hasChanges()) {
            result = createCustomContexts(testClassConfig.get(), acquiredByTest, amount);
        } else if (! createContextAndUser() && (testClassConfig.isPresent() && testClassConfig.get().optUserConfig().isPresent()) || testClassConfig.get().getNumberOfusersPerContext() > 5) {
            result = createUsersForPreProvisionedContexts(testClassConfig, acquiredByTest);
        } else if (testClassConfig.isPresent() && testClassConfig.get().optUserConfig().isPresent()) {
            result = createContexts(acquiredByTest, testClassConfig, amount);
        } else if (createContextAndUser()) {
            result = createContexts(acquiredByTest, testClassConfig, amount);
        } else {
            result = usePreProvisionedContexts(acquiredByTest, amount);
        }

        checkContexts(acquiredByTest, result);
        return result;
    }

    private static List<TestContext> createContexts(String acquiredByTest, Optional<TestClassConfig> testClassConfig, int amount) throws OXException {
        List<TestContext> result = new ArrayList<TestContext>(amount);
        for (int i = amount; i > 0; i--) {
            TestContext context = ConfigAwareProvisioningService.getService().createContext(acquiredByTest);

            List<TestUser> users = new ArrayList<>();
            for (int j = 0; j < ProvisioningUtils.USER_NAMES_POOL.length; j++) {
                users.add(ConfigAwareProvisioningService.getService()
                                                        .createUser(context.getId(),
                                                                    ProvisioningUtils.USER_NAMES_POOL[j],
                                                                    testClassConfig.get().optUserConfig(),
                                                                    acquiredByTest));
            }
            context = new TestContext(context.getId(), acquiredByTest, context.getAdmin(), acquiredByTest, users);

            result.add(context);
        }
        return result;
    }

    private static void checkContexts(String acquiredByTest, List<TestContext> result) {
        Assertions.assertFalse(result.isEmpty(), "Unable to acquire test context due to an empty pool.");
        result.forEach((context) -> {
            assertTrue(!context.getUsers().isEmpty() && context.getUsers().size() > 0, "User for new context missing.");
        });
        result.forEach((c) -> {
            LOG.info("Context '{}' has been acquired by {}.", Integer.valueOf(c.getId()), acquiredByTest);
        });
    }

    private static boolean createContextAndUser() {
        return Boolean.parseBoolean(AJAXConfig.getProperty(AJAXConfig.Property.CREATE_CONTEXT_AND_USER));
    }

    private static List<TestContext> createUsersForPreProvisionedContexts(Optional<TestClassConfig> testClassConfig, String acquiredByTest) {
        List<TestContext> result = new ArrayList<TestContext>();
        try {
            TestContext context = getPreProvisionedContext(acquiredByTest);
            context.configure(testClassConfig.get());
            result.add(context);
        } catch (Exception e) {
            LOG.error("Unable to get pre-provisioned context.", e);
        }
        return result;
    }

    private static List<TestContext> usePreProvisionedContexts(String acquiredByTest, int amount) {
        List<TestContext> result = new ArrayList<TestContext>(amount);

        for (int i = amount; i > 0; i--) {
            try {
                result.add(getPreProvisionedContext(acquiredByTest));
            } catch (Exception e) {
                LOG.error("Unable to get pre-provisioned context.", e);
            }
        }
        return result;
    }

    private static List<TestContext> createEmptyContexts(String acquiredByTest, int amount) throws OXException {
        List<TestContext> result = new ArrayList<TestContext>(amount);
        for (int i = amount; i > 0; i--) {
            TestContext context = ConfigAwareProvisioningService.getService().createContext(TestContextConfig.builder().build(), acquiredByTest);
            result.add(context);
        }
        return result;
    }

    private static List<TestContext> createCustomContexts(TestClassConfig testClassConfig, String acquiredByTest, int amount) throws OXException {
        List<TestContext> result = new ArrayList<TestContext>(amount);
        for (int i = amount; i > 0; i--) {
            TestContext context = ConfigAwareProvisioningService.getService().createContext(testClassConfig.optContextConfig().get(), acquiredByTest);

            if (testClassConfig.optUserConfig().isPresent()) {
                try {
                    context.configure(testClassConfig);
                } catch (Exception e) {
                    LOG.error("Unable to configure custom context.", e);
                }
            } else {
                List<TestUser> users = new ArrayList<>();
                for (int j = 0; j < ProvisioningUtils.USER_NAMES_POOL.length; j++) {
                    users.add(ConfigAwareProvisioningService.getService().createUser(context.getId(), ProvisioningUtils.USER_NAMES_POOL[j], testClassConfig.optUserConfig(), acquiredByTest));
                }
                context = new TestContext(context.getId(), acquiredByTest, oxAdminMaster, acquiredByTest, users);
            }

            result.add(context);
        }
        return result;
    }

    private static TestContext getPreProvisionedContext(String acquiredByTest) throws Exception {
        int cid = getNextContextId(acquiredByTest);
        if (cid == -1) {
            throw ProvisioningExceptionCode.UNABLE_TO_CREATE_CONTEXT.create("Cannot retrieve next context id from global database");
        }
        User admin_user = ProvisioningUtils.createAdminUser(cid);
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < ProvisioningUtils.USER_NAMES_POOL.length; i++) {
            TestUser userToTestUser = ProvisioningUtils.userToTestUser(ProvisioningUtils.getContextName(cid), ProvisioningUtils.USER_NAMES_POOL[i], "secret", Autoboxing.I(i + 3), Autoboxing.I(cid), acquiredByTest);
            users.add(userToTestUser);
        }

        TestUser adminUser = ProvisioningUtils.userToTestUser(ProvisioningUtils.getContextName(cid), admin_user.getName(), admin_user.getPassword(), Autoboxing.I(2), Autoboxing.I(cid), acquiredByTest);
        return ProvisioningUtils.toTestContext(cid, adminUser, acquiredByTest, users);
    }

    private static int getNextContextId(String acquiredByTest) throws Exception {
        URI uri = new URIBuilder().setScheme(scheme)
                                  .setHost(AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME))
                                  .setPath(AJAXConfig.getProperty(AJAXConfig.Property.BASE_PATH) + "test")
                                  .setPort(scheme.equals("https") ? 443 : Integer.parseInt(AJAXConfig.getProperty(AJAXConfig.Property.SERVER_PORT)))
                                  .addParameter("acquiredBy", acquiredByTest)
                                  .build();

        HttpGet httpGet = new HttpGet(uri);

        HttpResponse response = getInstance().execute(httpGet);
        assertEquals(200, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        String respString = EntityUtils.toString(response.getEntity());
        JSONObject contextObject = new JSONObject(respString);
        return contextObject.getInt("contextId");
    }

    // the admin is not handled to be acquired only by one party
    private static TestUser oxAdminMaster = null;

    public static TestUser getOxAdminMaster() {
        return oxAdminMaster;
    }

    public static void setOxAdminMaster(TestUser oxAdminMaster) {
        TestContextPool.oxAdminMaster = oxAdminMaster;
    }

    // the rest admin user is not handled to be acquired only by one party
    private static TestUser restUser = null;

    public static String getRestAuth() {
        return restUser.getUser() + ":" + restUser.getPassword();
    }

    public static TestUser getRestUser() {
        return restUser;
    }

    public static void setRestUser(TestUser restUser) {
        TestContextPool.restUser = restUser;
    }

    public static synchronized List<TestContext> getAllTimeAvailableContexts() {
        List<TestContext> cloned = new ArrayList<>(allTimeContexts.size());
        for (TestContext current : allTimeContexts) {
            cloned.add((TestContext) deepClone(current));
        }
        return cloned;
    }

    private static Object deepClone(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes given context
     *
     * @param contexts The context to delete
     * @throws OXException In case context can't be deleted
     */
    public static void deleteContext(List<TestContext> contexts) throws OXException {
        for (TestContext context : contexts) {
            deleteContext(context);
        }
    }

    /**
     * Delete given context
     *
     * @param context The context to delete
     * @throws OXException In case context can't be deleted
     */
    public static void deleteContext(TestContext context) throws OXException {
        if (context != null) {
            if (createContextAndUser()) {
                ConfigAwareProvisioningService.getService().deleteContext(context.getId(), context.getUsedBy());
            }
            for (TestUser user : context.getUsers()) {
                user.closeAjaxClients();
            }
        }
    }

}
