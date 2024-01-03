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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * {@link TestUserConfig} is a test configuration for users. All created users for this test will use this config.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class TestUserConfig {

    private final Optional<Long> quota;
    private final Optional<ClientTimeouts> timeouts;
	private Optional<List<String>> aliases;
	private Optional<List<String>> fakeAliases;

    /**
     * Initializes a new {@link TestUserConfig}.
     */
    TestUserConfig(Long quota, List<String> aliases, List<String> fakeAliases, ClientTimeouts timeouts) {
        super();
        this.quota = Optional.ofNullable(quota);
        this.timeouts = Optional.ofNullable(timeouts);
        this.aliases = Optional.ofNullable(aliases);
        this.fakeAliases = Optional.ofNullable(fakeAliases);
    }

    /**
     * Gets the optional quota
     *
     * @return The optional quota
     */
    public Optional<Long> optQuota() {
        return quota;
    }
    
    /**
     * Gets the optional aliases
     *
     * @return The optional aliases
     */
    public Optional<List<String>> optAliases() {
        return aliases;
    }
    
    /**
     * Gets the optional fake aliases
     *
     * @return The optional fake aliases
     */
    public Optional<List<String>> optFakeAliases() {
        return fakeAliases;
    }

    /**
     * Gets the optional timeouts
     *
     * @return The optional timeouts
     */
    public Optional<ClientTimeouts> optTimeouts() {
        return timeouts;
    }

    /**
     * Creates a builder
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link ClientTimeouts} holds timeout configurations for the ok-http client
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8.0.0
     */
    public static class ClientTimeouts {

        private final Optional<Duration> conTimeout;
        private final Optional<Duration> readTimeout;

        /**
         * Initializes a new {@link ClientTimeouts}.
         *
         * @param conTimeout The connection timeout
         * @param readTimeout The read timeout
         */
        public ClientTimeouts(Duration conTimeout, Duration readTimeout) {
            this.conTimeout = Optional.ofNullable(conTimeout);
            this.readTimeout = Optional.ofNullable(readTimeout);
        }

        /**
         * Gets the optional connection timeout
         *
         * @return The optional connection timeout
         */
        public Optional<Duration> optConTimeout() {
            return conTimeout;
        }

        /**
         * Gets the optional read timeout
         *
         * @return The optional read timeout
         */
        public Optional<Duration> optReadTimeout() {
            return readTimeout;
        }

    }

    /**
     * {@link Builder} is a builder for the {@link TestUserConfig}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8.0.0
     */
    public static class Builder {

        private Duration conTimeout = null;
        private Duration readTimeout = null;
        private Long quota = null;
		private List<String> aliases = null;
		private List<String> fakeAliases = null;

        /**
         * Sets the connection timeout
         *
         * @param timeout the timeout
         * @return this
         */
        public Builder withConTimeout(Duration timeout) {
            this.conTimeout = timeout;
            return this;
        }

        /**
         * Sets the read timeout
         *
         * @param timeout the timeout
         * @return this
         */
        public Builder withReadTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }

        /**
         * Sets the user quota
         *
         * @param quota the quota
         * @return this
         */
        public Builder withQuota(Long quota) {
            this.quota = quota;
            return this;
        }
        
		/**
		 * Sets the users aliases
		 *
		 * @param aliases the aliases
		 * @return this
		 */
		public Builder withAliases(List<String> aliases) {
			this.aliases = aliases;
			return this;
		}
		
		/**
		 * Sets the users fake aliases
		 *
		 * @param aliases the fake aliases
		 * @return this
		 */
		public Builder withFakeAliases(List<String> fakeAliases) {
			this.fakeAliases = fakeAliases;
			return this;
		}

        /**
         * Builds the {@link TestUserConfig}
         *
         * @return The {@link TestUserConfig}
         */
        public TestUserConfig build() {
            return new TestUserConfig(quota, aliases, fakeAliases, conTimeout == null && readTimeout == null ? null : new ClientTimeouts(conTimeout, readTimeout));
        }

    }
}
