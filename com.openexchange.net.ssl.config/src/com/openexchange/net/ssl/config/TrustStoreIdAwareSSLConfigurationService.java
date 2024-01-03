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

package com.openexchange.net.ssl.config;


/**
 * {@link TrustStoreIdAwareSSLConfigurationService} is an extension of the {@link SSLConfigurationService} which allows to define the truststore via an id instead of a file
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public interface TrustStoreIdAwareSSLConfigurationService extends SSLConfigurationService {

    /**
     * Returns the id of the custom truststore.
     *
     * Hint: Loaded once per startup and cannot be reloaded as additional initialization is made based on the configuration.
     *
     * @return the id of the custom truststore
     */
    String getCustomTruststoreId();

}
