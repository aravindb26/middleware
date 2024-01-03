/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the OX Software GmbH. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2016-2020 OX Software GmbH
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.test.common.test;

import java.util.Map;
import java.util.Optional;
import com.openexchange.test.common.test.pool.UserModuleAccess;

/**
 * {@link TestContextConfig} is a test configuration for contexts. The context will be created with this configuration
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class TestContextConfig {

    public static final TestContextConfig EMPTY_CONFIG = new TestContextConfig(null, null, null, null);

    private final Optional<Map<String, String>> config;
    private final Optional<String> taxonomyType;
    private final Optional<UserModuleAccess> access;
    private final Optional<Long> maxQuota;

    /**
     * Initializes a new {@link TestContextConfig}.
     *
     * @param config
     * @param access
     * @param taxonomyType
     * @param maxQuota
     */
    TestContextConfig(Map<String, String> config, UserModuleAccess access, String taxonomyType, Long maxQuota) {
        super();
        this.config = Optional.ofNullable(config);
        this.access = Optional.ofNullable(access);
        this.taxonomyType = Optional.ofNullable(taxonomyType);
        this.maxQuota = Optional.ofNullable(maxQuota);
    }

    /**
     * Gets the optional config as a property-value-map
     *
     * @return The optional config
     */
    public Optional<Map<String, String>> optConfig() {
        return config;
    }

    /**
     * Gets the optional user module access
     *
     * @return The optional {@link UserModuleAccess}
     */
    public Optional<UserModuleAccess> optAccess() {
        return access;
    }

    /**
     * Gets the optional taxonomyType
     *
     * @return The taxonomyType
     */
    public Optional<String> optTaxonomyType() {
        return taxonomyType;
    }

    /**
     * Gets the optional max quota restriction
     *
     * @return The optional quota restriction
     */
    public Optional<Long> optMaxQuota(){
        return maxQuota;
    }

    /**
     * Creates a builder
     *
     * @return The {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if the context needs to be adjusted. Or in other words if any custom configuration is made
     *
     * @return <code>true</code> if any custom configuration is made, <code>false</code> otherwise
     */
    public boolean hasChanges() {
        return access.isPresent() || config.isPresent();
    }

    /**
     * {@link Builder} is a builder for {@link TestContextConfig}s
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8.0.0
     */
    public static class Builder {

        private Map<String, String> config = null;
        private UserModuleAccess access = null;
        private String taxonomyType;
        private Long maxQuota = null;

        /**
         * Sets the config for the context
         *
         * @param config The config
         * @return this
         */
        public Builder withConfig(Map<String, String> config) {
            this.config = config;
            return this;
        }

        /**
         * Sets the {@link UserModuleAccess} for the context
         *
         * @param access
         * @return this
         */
        public Builder withUserModuleAccess(UserModuleAccess access) {
            this.access = access;
            return this;
        }

        /**
         * Sets the taxonomy type for the context
         *
         * @param taxonomyType
         * @return this
         */
        public Builder withTaxonomyType(String taxonomyType) {
            this.taxonomyType = taxonomyType;
            return this;
        }

        /**
         * Sets the max. quota for the context
         *
         * @param maxQuota The may. quota to set
         * @return this
         */
        public Builder withMaxQuota(Long maxQuota) {
            this.maxQuota = maxQuota;
            return this;
        }

        /**
         * Builds the {@link TestContextConfig}
         *
         * @return The {@link TestContextConfig}
         */
        public TestContextConfig build() {
            return new TestContextConfig(config, access, taxonomyType, maxQuota);
        }
    }

}
