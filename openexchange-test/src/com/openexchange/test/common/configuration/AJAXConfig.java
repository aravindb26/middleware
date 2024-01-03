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

package com.openexchange.test.common.configuration;

import java.io.File;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.tools.conf.AbstractConfig;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class AJAXConfig extends AbstractConfig {

    private static final TestConfig.Property KEY = TestConfig.Property.AJAX_PROPS;

    private static volatile AJAXConfig singleton;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPropertyFileName() throws OXException {
        final String fileName = TestConfig.getProperty(KEY);
        if (null == fileName) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create(KEY.getPropertyName());
        }
        return fileName;
    }

    /**
     * Reads the configuration.
     * @throws OXException if reading configuration fails.
     */
    public static void init() throws OXException {
        TestConfig.init();
        if (null == singleton) {
            synchronized (AJAXConfig.class) {
                if (null == singleton) {
                    singleton = new AJAXConfig();
                    String propertyFileName = singleton.getPropertyFileName();
                    //Check for custom ajax properties
                    String customPropertyFileName = propertyFileName.replace(".properties", "-custom.properties");
                    final File customPropFile = new File(customPropertyFileName);
                    if (customPropFile.exists() && customPropFile.canRead()) {
                        singleton.loadPropertiesInternal(customPropertyFileName);
                    } else {
                        singleton.loadPropertiesInternal(propertyFileName);
                    }
                }
            }
        }
    }

    public static String getProperty(final Property key) {
        String property = tryToFindEnviromentVariable(key);
        if (property == null) {
            property = singleton.getPropertyInternal(key.getPropertyName());
        }
        return property != null ? property : key.defaultValue;
    }

    public static String getProperty(final Property key, final String fallBack) {
        String property;
        try {
            property = tryToFindEnviromentVariable(key);
            if (property == null) {
                property = singleton.getPropertyInternal(key.getPropertyName(), fallBack);
            }
        } catch (Exception e) {
            return fallBack;
        }
        return property;
    }

    private static String tryToFindEnviromentVariable(Property key) {
        String envVar;
        try {
            envVar = System.getenv(key.getEnvVarName());
        } catch (@SuppressWarnings("unused") Exception e) {
            envVar = null;
        }
        return envVar;
    }

    /**
     * Enumeration of all properties in the ajax.properties file.
     * FIXME only required for unittests. do clean up their setup
     */
    public static enum Property {
        /**
         * http or https.
         */
        PROTOCOL("protocol", "http"),
        /**
         * Server host.
         */
        SERVER_HOSTNAME("serverhostname", "localhost"),
        /**
         * Server port
         */
        SERVER_PORT("serverPort", "80"),
        /**
         * Mock server host.
         */
        MOCK_HOSTNAME("mockhostname", "localhost"),
        /**
         * Mock server port.
         */
        MOCK_PORT("mockport", "80"),
        /**
         * Dav host.
         */
        DAV_HOST("davhost", "localhost"),
        /**
         * The host for RMI calls
         */
        RMI_HOST("rmihost", "localhost"),
        /**
         * Server host that routes traffic to only one node/pod
         */
        SINGLENODE_HOSTNAME("singlenodehostname", "localhost"),
        /**
         * Server host that is configured for OIDC authentication
         */
        OIDC_HOSTNAME("oidchostname", "localhost"),
        /**
         * Executor sleeps this amount of time after every request to prevent Apache problems
         */
        SLEEP("sleep", "0"),
        /**
         * Whether SP3 or SP4 data
         */
        IS_SP3("isSP3", "false"),
        /**
         * Echo header; see property "com.openexchange.servlet.echoHeaderName" in file 'server.properties'
         */
        ECHO_HEADER("echo_header", ""),
        /**
         * Directory which contains test files
         */
        TEST_DIR("testMailDir", "testData/"),
        /**
         * Mail server host
         */
        MAIL_HOST("mailHost", "localhost"),
        /**
         * Mail server port
         */
        MAIL_PORT("mailPort", "143"),
        /**
         * Mail transport server host
         */
        MAIL_TRANSPORT_HOST("mailTransportHost", "localhost"),
        /**
         * Mail transport server port
         */
        MAIL_TRANSPORT_PORT("mailTransportPort", "25"),
        /**
         * Indicates if provisioning should be on the fly or pre-provisioned data should be used (if available)<br>
         * <br>
         * If set to <code>true</code> each and every context and user will be created for the test
         */
        CREATE_CONTEXT_AND_USER("createContextAndUser", "true"),
        /**
         * Path prefix
         */
        PATH_PREFIX("pathPrefix", ""),
        /**
         * The token endpoint of an oauth authentication server (e.g. keycloak)
         */
        OAUTH_TOKEN_ENDPOINT("oauthTokenEndpoint", ""),
        /**
         * The client id configured in the oauth authentication server
         */
        OAUTH_CLIENT_ID("oauthClientID", ""),
        /**
         * The protocol to use for oauth requests
         */
        OAUTH_PROTOCOL("oauthProtocol", "https"),
        /**
         * The client secret configured in the oauth authentication server
         */
        OAUTH_CLIENT_PASSWORD("oauthClientPassword", ""),
        /**
         * The context admin login.
         */
        CONTEXT_ADMIN_USER("contextAdminUser", "oxadmin"),
        /**
         * The context admin password.
         */
        CONTEXT_ADMIN_PASSWORD("contextAdminPassword", "secret"),
        /**
         * The default password for new users.
         */
        USER_PASSWORD("userPassword", "secret"),
        /**
         * A comma separated list with names for the users of a context.
         */
        USER_NAMES("userNames", "anton,berta,caesar,dora,emil"),
        /**
         * Configures the default connection timeout for the ok http client
         */
        CONNECTION_TIMOUT("connectionTimout", "30000"),
        /**
         * Configures the default read timeout for the ok http client
         */
        READ_TIMOUT("readTimout", "60000"),
        /**
         * Specifies the URL prefix. Defaults to /appsuite/api/
         */
        BASE_PATH("basePath", "/api/"),
        /**
         * Specifies the OIDC prefix. Defaults to /oidc
         */
        OIDC_PATH("oidcPath", "/oidc")
        ;

        /**
         * Name of the property in the ajax.properties file.
         */
        private final String propertyName;

        private static final String ENV_VAR_PREFIX = "ajax_properties__%1s";

        /**
         * Default value of the ajax.property.
         */
        private final String defaultValue;

        /**
         * Default constructor.
         * @param propertyName Name of the property in the ajax.properties
         * file.
         */
        private Property(final String propertyName, final String defaultValue) {
            this.propertyName = propertyName;
            this.defaultValue = defaultValue;

        }

        /**
         * @return the propertyName
         */
        public String getPropertyName() {
            return propertyName;
        }

        public String getEnvVarName() {
            return String.format(ENV_VAR_PREFIX, this.name());
        }
    }
}
