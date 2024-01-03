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

package com.openexchange.demo;

import static com.openexchange.demo.DemoProperty.DB_READ_HOST;
import static com.openexchange.demo.DemoProperty.DB_READ_PASSWORD;
import static com.openexchange.demo.DemoProperty.DB_READ_USER;
import static com.openexchange.demo.DemoProperty.DB_WRITE_HOST;
import static com.openexchange.demo.DemoProperty.DB_WRITE_PASSWORD;
import static com.openexchange.demo.DemoProperty.DB_WRITE_USER;
import static com.openexchange.demo.DemoProperty.FILEITEMSTORE_ENABLED;
import static com.openexchange.demo.DemoProperty.FILEITEMSTORE_PATH;
import static com.openexchange.demo.DemoProperty.FILEITEMSTORE_SIZE;
import static com.openexchange.demo.DemoProperty.FILESTORE_PATH;
import static com.openexchange.demo.DemoProperty.FILESTORE_SIZE;
import static com.openexchange.demo.DemoProperty.GDPRSTORE_ENABLED;
import static com.openexchange.demo.DemoProperty.GDPRSTORE_PATH;
import static com.openexchange.demo.DemoProperty.GDPRSTORE_SIZE;
import static com.openexchange.demo.DemoProperty.GLOBAL_DB;
import static com.openexchange.demo.DemoProperty.GLOBAL_DB_NAME;
import static com.openexchange.demo.DemoProperty.MASTER_ADMIN_PASSWORD;
import static com.openexchange.demo.DemoProperty.MASTER_ADMIN_USERNAME;
import static com.openexchange.demo.DemoProperty.NUMBER_OF_CONTEXT;
import static com.openexchange.demo.DemoProperty.USER_DB_NAME;
import static com.openexchange.demo.DemoProperty.USER_LIST;
import static com.openexchange.demo.DemoProperty.USER_PER_CONTEXT;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.admin.properties.AdminProperties;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.OXGroupInterface;
import com.openexchange.admin.rmi.OXResourceInterface;
import com.openexchange.admin.rmi.OXUserInterface;
import com.openexchange.admin.rmi.OXUtilInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Database;
import com.openexchange.admin.rmi.dataobjects.Filestore;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.Resource;
import com.openexchange.admin.rmi.dataobjects.Server;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.dataobjects.UserModuleAccess;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.java.Strings;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * {@link InitializationPerformer}
 *
 * @author <a href="mailto:nikolaos.tsapanidis@open-xchange.com">Nikolaos Tsapanidis</a>
 * @since v8.0.0
 */
public class InitializationPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(InitializationPerformer.class);

    private final LeanConfigurationService leanConfigService;
    private final AtomicReference<InitializationState> currentState;
    private final int totalNumberOfContexts;
    private final DataGenerator dataGenerator;
    private final KubernetesClient kubeClient;
    private final Map<String, String> map;

    /**
     * Initializes a new {@link InitializationPerformer}.
     *
     * @param leanConfigService The config service to use
     */
    public InitializationPerformer(LeanConfigurationService leanConfigService) {
        super();
        this.leanConfigService = leanConfigService;
        int numberOfContexts = leanConfigService.getIntProperty(NUMBER_OF_CONTEXT);
        this.totalNumberOfContexts = numberOfContexts;
        currentState = new AtomicReference<InitializationState>(InitializationState.builder(numberOfContexts).build());
        this.dataGenerator = new DataGenerator(leanConfigService);
        this.kubeClient = new DefaultKubernetesClient();
        this.map = new HashMap<>();
    }

    /**
     * Gets the current initialization state.
     *
     * @return The current initialization state
     */
    public InitializationState getInitializationState() {
        return currentState.get();
    }

    /**
     * Performs initialization of the demo system.
     *
     * @param provisioningInterfaces Provides access to needed provisioning interfaces
     */
    public void init(ProvisioningInterfaces provisioningInterfaces) {
        OXUtilInterface utilInterface = provisioningInterfaces.getUtilInterface();
        //@formatter:off
        Credentials adminMasterCreds = new Credentials(
                leanConfigService.getProperty(MASTER_ADMIN_USERNAME),
                leanConfigService.getProperty(MASTER_ADMIN_PASSWORD));
        //@formatter:on

        try {
            if (utilInterface.listAllServer(adminMasterCreds).length == 0) {
                LOG.info("Starting demo system initialization...");

                registerServer(utilInterface, adminMasterCreds);
                registerFilestore(utilInterface, adminMasterCreds);
                registerDatabase(utilInterface, adminMasterCreds);

                String releaseName = System.getenv("HELM_RELEASE_NAME");
                String namespace = kubeClient.getNamespace();
                String configMapName = "core-mw-demo";

                //@formatter:off
                ConfigMap configMap = kubeClient.configMaps().createOrReplace(
                        new ConfigMapBuilder()
                                .withNewMetadata()
                                .withName((Strings.isNotEmpty(releaseName)) ? releaseName + "-" + configMapName : namespace + "-" + configMapName)
                                .withLabels(Collections.singletonMap("app.kubernetes.io/managed-by", "Helm"))
                                .endMetadata()
                                .addToData(map)
                                .build());
                //@formatter:on
                LOG.info("Created ConfigMap {} with data {}", configMap.getMetadata().getName(), configMap.getData());

                if (leanConfigService.getIntProperty(NUMBER_OF_CONTEXT) > 0) {
                    createDemoData(provisioningInterfaces);
                }

                //@formatter:off
                currentState.set(InitializationState.builder(currentState.get())
                        .withFinished(true)
                        .withStateMessage("Finished demo system initialization.")
                        .build());
                //@formatter:on
                LOG.info("Finished demo system initialization.");
            } else {
                //@formatter:off
                currentState.set(InitializationState.builder(currentState.get())
                        .withFinished(true)
                        .withStateMessage("Skipping demo system initialization since there is already a server registered.")
                        .build());
                //@formatter:on
                LOG.info("Skipping demo system initialization since there is already a server registered.");
            }
        } catch (Exception e) {
            LOG.error("Error during demo system initialization", e);
        }
    }

    /**
     * Registers a {@link Server}
     *
     * @param utilInterface The {@link OXUtilInterface}
     * @param creds The credentials
     * @throws Exception
     */
    private void registerServer(OXUtilInterface utilInterface, Credentials creds) throws Exception {
        Server server = new Server();
        server.setName(leanConfigService.getProperty(DefaultProperty.valueOf(AdminProperties.Prop.SERVER_NAME, "oxserver")));
        try {
            server = utilInterface.registerServer(server, creds);
            currentState.set(InitializationState.builder(currentState.get()).withServerRegistered(true).build());
            map.put("SERVER_ID", server.getId().toString());
            LOG.info("Successfully registered server {}.", server.getName());
        } catch (Exception e) {
            currentState.set(InitializationState.builder(currentState.get()).withStateMessage("Error while registering server: " + e.getMessage()).build());
            LOG.error("Error while registering server", e);
            throw e;
        }
    }

    /**
     * Registers {@link Filestore}
     *
     * @param utilInterface The {@link OXUtilInterface}
     * @param creds The credentials
     * @throws Exception
     */
    private void registerFilestore(OXUtilInterface utilInterface, Credentials creds) throws Exception {
        //@formatter:off
        Filestore filestore = dataGenerator.createFilestore(
                leanConfigService.getProperty(FILESTORE_PATH),
                L(leanConfigService.getLongProperty(FILESTORE_SIZE)),
                I(5000));
        //@formatter:on

        try {
            filestore = utilInterface.registerFilestore(filestore, creds);
            map.put("FILESTORE_ID", filestore.getId().toString());
            LOG.info("Successfully registered filestore with id {}.", filestore.getId().toString());

            if (leanConfigService.getBooleanProperty(GDPRSTORE_ENABLED)) {
                //@formatter:off
                Filestore gdprstore = dataGenerator.createFilestore(
                        leanConfigService.getProperty(GDPRSTORE_PATH),
                        L(leanConfigService.getLongProperty(GDPRSTORE_SIZE)),
                        I(0));
                //@formatter:on

                gdprstore = utilInterface.registerFilestore(gdprstore, creds);
                map.put("GDPRSTORE_ID", gdprstore.getId().toString());
                LOG.info("Successfully registered filestore with id {}.", gdprstore.getId().toString());
            }
            if (leanConfigService.getBooleanProperty(FILEITEMSTORE_ENABLED)) {
                //@formatter:off
                Filestore fileitemstore = dataGenerator.createFilestore(
                        leanConfigService.getProperty(FILEITEMSTORE_PATH),
                        L(leanConfigService.getLongProperty(FILEITEMSTORE_SIZE)),
                        I(0));
                //@formatter:on
                fileitemstore = utilInterface.registerFilestore(fileitemstore, creds);
                map.put("FILEITEMSTORE_ID", fileitemstore.getId().toString());
                LOG.info("Successfully registered filestore with id {}.", fileitemstore.getId().toString());
            }

            currentState.set(InitializationState.builder(currentState.get()).withFilestoreRegistered(true).build());
        } catch (Exception e) {
            currentState.set(InitializationState.builder(currentState.get()).withStateMessage("Error while registering filestore: " + e.getMessage()).build());
            LOG.error("Error while registering filestore", e);
            throw e;
        }
    }

    /**
     * Register {@link Database}
     *
     * @param utilInterface The {@link OXUtilInterface}
     * @param creds The credentials
     * @throws Exception
     */
    private void registerDatabase(OXUtilInterface utilInterface, Credentials creds) throws Exception {
        String writeHost = leanConfigService.getProperty(DB_WRITE_HOST);
        String writeUser = leanConfigService.getProperty(DB_WRITE_USER);
        String writePassword = leanConfigService.getProperty(DB_WRITE_PASSWORD);

        String configuredReadHost = leanConfigService.getProperty(DB_READ_HOST);
        String configuredReadUser = leanConfigService.getProperty(DB_READ_USER);
        String configuredReadPassword = leanConfigService.getProperty(DB_READ_PASSWORD);
        String readHost = configuredReadHost.isEmpty() ? writeHost : configuredReadHost;
        String readUser = configuredReadUser.isEmpty() ? writeUser : configuredReadUser;
        String readPassword = configuredReadPassword.isEmpty() ? writePassword : configuredReadPassword;

        boolean registerGlobablDb = leanConfigService.getBooleanProperty(GLOBAL_DB);

        try {
            // register user db
            Database userdbWrite = dataGenerator.createDatabase(leanConfigService.getProperty(USER_DB_NAME), writeHost, writeUser, writePassword, true, I(5000));
            userdbWrite = utilInterface.registerDatabase(userdbWrite, Boolean.FALSE, null, creds);
            map.put("USER_DB_WRITE_ID", userdbWrite.getId().toString());
            LOG.info("Successfully registered database with id {}", userdbWrite.getId().toString());

            // check for master/slave setup
            if (!writeHost.equals(readHost)) {
                Database userdbRead = dataGenerator.createDatabase(leanConfigService.getProperty(USER_DB_NAME) + "_slave", readHost, readUser, readPassword, false, I(5000));
                userdbRead.setMasterId(userdbWrite.getId());
                userdbRead = utilInterface.registerDatabase(userdbRead, Boolean.FALSE, null, creds);
                map.put("USER_DB_READ_ID", userdbRead.getId().toString());
                LOG.info("Successfully registered (read only) database with id {}", userdbRead.getId().toString());
            }

            // check if global db should be registered
            if (registerGlobablDb) {
                Database globabldbWrite = dataGenerator.createDatabase(leanConfigService.getProperty(GLOBAL_DB_NAME), writeHost, writeUser, writePassword, true, I(0));
                globabldbWrite = utilInterface.registerDatabase(globabldbWrite, Boolean.FALSE, null, creds);
                map.put("GLOBAL_DB_ID", globabldbWrite.getId().toString());
                LOG.info("Successfully registered global database with id {}", globabldbWrite.getId().toString());

                // check for master/slave setup
                if (!writeHost.equals(readHost)) {
                    Database globaldbRead = dataGenerator.createDatabase(leanConfigService.getProperty(GLOBAL_DB_NAME) + "_slave", readHost, readUser, readPassword, false, I(0));
                    globaldbRead.setMasterId(globabldbWrite.getId());
                    utilInterface.registerDatabase(globaldbRead, Boolean.FALSE, null, creds);
                    LOG.info("Successfully registered (read only) global database with id {}", globaldbRead.getId().toString());
                }
            }
            currentState.set(InitializationState.builder(currentState.get()).withDatabaseRegistered(true).build());
        } catch (Exception e) {
            currentState.set(InitializationState.builder(currentState.get()).withStateMessage("Error while registering database: " + e.getMessage()).build());
            LOG.error("Error while registering database", e);
            throw e;
        }
    }

    /**
     * Creates demo data
     * <p>
     * Registers:
     * <ul>
     * <li>{@link Context}</li>
     * <li>{@link User}</li>
     * <li>{@link Group}</li>
     * <li>{@link Resource}</li>
     * </ul>
     *
     * @param provisioningInterfaces The {@link ProvisioningInterfaces}
     * @throws Exception
     */
    private void createDemoData(ProvisioningInterfaces provisioningInterfaces) throws Exception {
        OXContextInterface contextInterface = provisioningInterfaces.getContextInterface();
        OXUserInterface userInterface = provisioningInterfaces.getUserInterface();
        OXResourceInterface resourceInterface = provisioningInterfaces.getResourceInterface();
        OXGroupInterface groupInterface = provisioningInterfaces.getGroupInterface();

        int userPerContext = Math.max(leanConfigService.getIntProperty(USER_PER_CONTEXT), 1);

        //@formatter:off
        String[] users = Arrays.stream(Strings.splitByComma(leanConfigService.getProperty(USER_LIST)))
                .filter(s -> s != null && s.length() > 0)
                .toArray(String[]::new);

        Credentials adminMasterCreds = new Credentials(leanConfigService.getProperty(MASTER_ADMIN_USERNAME),
                leanConfigService.getProperty(MASTER_ADMIN_PASSWORD));
        //@formatter:on

        UserModuleAccess userAccess = new UserModuleAccess();
        userAccess.enableAll();

        for (int cid = 1; cid <= totalNumberOfContexts; cid++) {
            User oxadmin = dataGenerator.createAdmin(cid);
            Context context = dataGenerator.createContext(cid);

            createDemoContext(contextInterface, context, oxadmin, userAccess, adminMasterCreds);

            List<User> userList = new ArrayList<>();
            User user = null;

            // check if random user should be created
            if (users.length == 0) {
                for (int uid = 1; uid <= userPerContext; uid++) {
                    user = dataGenerator.createUser(cid, Optional.empty());
                    userList.add(user);
                }
            } else {
                for (String username : users) {
                    user = dataGenerator.createUser(cid, Optional.of(username));
                    userList.add(user);
                }
            }

            Credentials oxadminCreds = new Credentials(oxadmin.getName(), oxadmin.getPassword());
            createDemoUser(userInterface, context, userList, oxadminCreds);
            createDemoGroup(groupInterface, context, userList.size(), oxadminCreds);
            createDemoResource(resourceInterface, context, cid, oxadminCreds);

            currentState.set(InitializationState.builder(currentState.get()).withNumberOfCreatedContexts(cid).build());
        }
    }

    /**
     * Creates a demo context
     *
     * @param contextInterface The {@link OXContextInterface}
     * @param context The context
     * @param oxadmin The oxadmin
     * @param access The {@link UserModuleAccess}
     * @param creds The {@link Credentials}
     * @throws Exception
     */
    private void createDemoContext(OXContextInterface contextInterface, Context context, User oxadmin, UserModuleAccess access, Credentials creds) throws Exception {
        try {
            contextInterface.create(context, oxadmin, access, creds);
        } catch (Exception e) {
            currentState.set(InitializationState.builder(currentState.get()).withStateMessage("Error while creating context: " + e.getMessage()).build());
            LOG.error("Error while creating context", e);
            throw e;
        }
    }

    /**
     * Creates a demo user
     *
     * @param userInterface The {@link OXUserInterface}
     * @param context The context
     * @param userList List of users within the context
     * @param creds The {@link Credentials}
     * @throws Exception
     */
    private void createDemoUser(OXUserInterface userInterface, Context context, List<User> userList, Credentials creds) throws Exception {
        for (User u : userList) {
            try {
                userInterface.create(context, u, creds);
            } catch (Exception e) {
                currentState.set(InitializationState.builder(currentState.get()).withStateMessage("Error while creating user: " + e.getMessage()).build());
                LOG.error("Error while creating user", e);
                throw e;
            }
        }
    }

    /**
     * Creates a demo group
     *
     * @param groupInterface The {@link OXGroupInterface}
     * @param context The context
     * @param userCount The amount of user within a context
     * @param creds The {@link Credentials}
     * @throws Exception
     */
    private void createDemoGroup(OXGroupInterface groupInterface, Context context, int userCount, Credentials creds) throws Exception {
        try {
            groupInterface.create(context, dataGenerator.createGroup(userCount), creds);
        } catch (Exception e) {
            currentState.set(InitializationState.builder(currentState.get()).withStateMessage("Error while creating group: " + e.getMessage()).build());
            LOG.error("Error while creating group", e);
            throw e;
        }
    }

    /**
     * Creates a demo resource
     *
     * @param resourceInterface The {@link OXResourceInterface}
     * @param context The context
     * @param cid The context identifier
     * @param creds The {@link Credentials}
     * @throws Exception
     */
    private void createDemoResource(OXResourceInterface resourceInterface, Context context, int cid, Credentials creds) throws Exception {
        try {
            resourceInterface.create(context, dataGenerator.createResource(cid), creds);
        } catch (Exception e) {
            currentState.set(InitializationState.builder(currentState.get()).withStateMessage("Error while creating resource: " + e.getMessage()).build());
            LOG.error("Error while creating resource", e);
            throw e;
        }

    }

}
