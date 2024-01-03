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

package com.openexchange.test.common.test;

import java.util.Optional;

/**
 *
 * {@link TestClassConfig}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class TestClassConfig {

    final int numberOfContexts;
    final int numberOfusersPerContext;
    final String userAgent;
    final Optional<TestUserConfig> userConfig;
    final Optional<TestContextConfig> contextConfig;
    final boolean emptyContext;

    /**
     * Initializes a new {@link TestClassConfig}.
     *
     * @param numberOfContexts The amount of context to prepare for the test
     * @param numberOfusersPerContext The amount of users to prepare for each context
     * @param userAgent The client's user agent to set
     */
    public TestClassConfig(int numberOfContexts, int numberOfusersPerContext, TestUserConfig userConfig, TestContextConfig contextConfig, String userAgent, boolean emptyContext) {
        super();
        this.numberOfContexts = numberOfContexts;
        this.numberOfusersPerContext = 2 > numberOfusersPerContext ? 2 : numberOfusersPerContext;
        this.userConfig = Optional.ofNullable(userConfig);
        this.contextConfig = Optional.ofNullable(contextConfig);
        this.userAgent = userAgent;
        this.emptyContext = emptyContext;
    }

    /**
     * The amount of context to prepare for the test
     *
     * @return the amount
     */
    public int getNumberOfContexts() {
        return numberOfContexts;
    }

    /**
     * The amount of users to prepare for each context
     *
     * @return the amount
     */
    public int getNumberOfusersPerContext() {
        return numberOfusersPerContext;
    }

    /**
     * Gets the optional user config
     *
     * @return The user config
     */
    public Optional<TestUserConfig> optUserConfig() {
        return userConfig;
    }

    /**
     * Gets the optional context config
     *
     * @return The context config
     */
    public Optional<TestContextConfig> optContextConfig() {
        return contextConfig;
    }

    /**
     * The client's user agent
     *
     * @return The client's user agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * The client's user agent
     *
     * @return The client's user agent
     */
    public boolean isEmptyContext() {
        return emptyContext;
    }

    /**
     * Builder for a {@link TestClassConfig}
     *
     * @return The builder
     */
    public static TestConfigBuilder builder() {
        return new TestConfigBuilder();
    }

    /**
     *
     * {@link TestConfigBuilder}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v7.10.5
     */
    public static class TestConfigBuilder {

        private int numberOfContexts = 1;
        private int numberOfusersPerContext = 1;
        private TestUserConfig userConfig = null;
        private TestContextConfig contextConfig = null;
        private String userAgent = null;
        private boolean emptyContext = false;

        /**
         * Initializes a new {@link TestClassConfig}.
         */
        TestConfigBuilder() {}

        /**
         * The amount of context to prepare for the test
         *
         * @param x The amount
         * @return This builder for chaining
         */
        public TestConfigBuilder withContexts(int x) {
            numberOfContexts = x;
            return this;
        }

        /**
         * The amount of context to prepare for the test
         *
         * @param x The amount
         * @return This builder for chaining
         */
        public TestConfigBuilder emptyContext(boolean x) {
            emptyContext = x;
            return this;
        }

        /**
         * The amount of users to prepare for each context
         *
         * @param x The amount
         * @return This builder for chaining
         */
        public TestConfigBuilder withUserPerContext(int x) {
            numberOfusersPerContext = x;
            return this;
        }

        /**
         * Sets the {@link TestUserConfig}
         */
        public TestConfigBuilder withUserConfig(TestUserConfig userConfig) {
            this.userConfig = userConfig;
            return this;
        }

        /**
         * Sets the {@link TestContextConfig}
         */
        public TestConfigBuilder withContextConfig(TestContextConfig contextConfig) {
            this.contextConfig = contextConfig;
            return this;
        }

        /**
         * Sets the client's user agent
         *
         * @param client The client's user agent
         * @return this
         */
        public TestConfigBuilder withUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Build the configuration
         *
         * @return The {@link TestClassConfig}
         */
        public TestClassConfig build() {
            // @formatter:off
            return new TestClassConfig(numberOfContexts,
                                       numberOfusersPerContext,
                                       userConfig,
                                       contextConfig,
                                       userAgent,
                                       emptyContext);
            // @formatter:on
        }

    }

}
