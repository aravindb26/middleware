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
package com.openexchange.apn.common.impl;

/**
 * {@link PushClientConfigFields} contains config fields and their default of a apn push client config
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public enum PushClientConfigFields {

    /**
     * Configures the apps's topic, which is typically the bundle ID of the app.
     * Default: no default
     */
    topic(),

    /**
     * Indicates which APNS service is used when sending push notifications to iOS devices.
     * A value of "true" will use the production service, a value of "false" the sandbox service.
     * Default: true
     */
    production(Boolean.TRUE),

    /**
     * Specifies the authentication type to use for the APNS HTTP/2 push.
     * Allows the values "certificate" and "jwt".
     * - "certificates" signals to connect to APNs using provider certificates while
     * - "jwt" signals to connect to APNs using provider authentication JSON Web Token (JWT)
     * Default: "certificate"
     */
    authtype("certificate"),

    /**
     * Specifies the path to the local keystore file (PKCS #12) containing the APNS HTTP/2 certificate and keys for the iOS application.
     * Default: no default
     */
    keystore(),

    /**
     * Specifies id of the keystore containing the APNS HTTP/2 certificate and keys for the iOS application.
     * Default: no default
     */
    keystoreId(),

    /**
     * Specifies the password used when creating the referenced keystore containing the certificate of the iOS application.
     * Note that blank or null passwords are in violation of the PKCS #12 specifications.
     * Default: no default
     */
    password(),

    /**
     * Specifies the private key file used to connect to APNs using provider authentication JSON Web Token (JWT).
     * Default: no default
     */
    privatekey(),

    /**
     * Specifies the id of the private key used to connect to APNs using provider authentication JSON Web Token (JWT).
     * Default: no default
     */
    privatekeyId(),

    /**
     * Specifies the key identifier used to connect to APNs using provider authentication JSON Web Token (JWT).
     * Default: no default
     */
    keyid(),

    /**
     * Specifies the team identifier used to connect to APNs using provider authentication JSON Web Token (JWT).
     * Default: no default
     */
    teamid(),

    ;

    private final Object defaultValue;

    /**
     * Initializes a new {@link PushClientConfigFields}.
     *
     * @param defaultValue The default value
     */
    private PushClientConfigFields(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Initializes a new {@link PushClientConfigFields} with null as the default.
     */
    private PushClientConfigFields() {
        this.defaultValue = null;
    }

    /**
     * Gets the defaultValue
     *
     * @return The defaultValue
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

}
