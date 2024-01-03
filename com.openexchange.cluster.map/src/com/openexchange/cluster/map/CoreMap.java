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

package com.openexchange.cluster.map;

/**
 * {@link CoreMap} - An enumeration for known core map names.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum CoreMap {

    /**
     * The name for distributed files.
     */
    DISTRIBUTED_FILES("distFiles"),
    /**
     * The name for cluster task locks.
     */
    CLUSTER_TASK_LOCKS("clsTaskLocks"),
    /**
     * The name for composition space key storage.
     */
    COMPOSITION_SPACE_KEY_STORAGE("csKeys"),
    /**
     * The name for multi-factor SMS tokens.
     */
    MULTIFACTOR_SMS_TOKENS("mfSmsTokens"),
    /**
     * The name for multi-factor U2F tokens.
     */
    MULTIFACTOR_U2F_TOKENS("mfU2fTokens"),
    /**
     * The name for OAuth auth code.
     */
    OAUTH_PROVIDER_AUTH_CODE("oauthAuthCode"),
    /**
     * The name for OIDC auth infos.
     */
    OIDC_AUTH_INFOS("oidcAuthInfos"),
    /**
     * The name for OIDC logout infos.
     */
    OIDC_LOGOUT_INFOS("oidcLogoutInfos"),
    /**
     * The name for OIDC session infos.
     */
    OIDC_SESSION_INFOS("oidcSessionInfos"),
    /**
     * The name for Dovecot push locks.
     */
    DOVECOT_PUSH_LOCKS("dovecotPushLocks"),
    /**
     * The name for IMAP-IDLE push locks.
     */
    IMAPIDLE_PUSH_LOCKS("imapidlePushLocks"),
    /**
     * The name for permanent listener registry.
     */
    PUSH_PERMANENT_LISTENER_REGISTRY("permListenerReg"),
    /**
     * The name for push credentials.
     */
    PUSH_CREDENTIALS("pushCreds"),
    /**
     * The name for SAML authn request infos.
     */
    SAML_AUTHN_REQUEST_INFOS("samlAuthnRequestInfos"),
    /**
     * The name for SAML logout request infos.
     */
    SAML_LOGOUT_REQUEST_INFOS("samlLogoutRequestInfos"),
    /**
     * The name for SAML authn response identifiers.
     */
    SAML_AUTHN_RESPONSE_IDS("samlAuthnResponseIds"),
    /**
     * The name for session reservations.
     */
    SESSION_RESERVATIONS("sessionRes"),
    /**
     * The name for session tokens.
     */
    SESSION_TOKEN_MAP("sessionTokens"),
    /**
     * The name for session id to token mapping.
     */
    SESSION_SESSIONID_TO_TOKEN("sessionId2Token"),
    /**
     * The name for SMS bucket map.
     */
    SMS_BUCKET("smsBucket"),
    /**
     * The name for Web Socket remote distributor.
     */
    WEBSOCKET_REMOTE_DISTRIBUTOR("wsRemDistributor"),
    /**
     * The name for OAuth call-back registry.
     */
    OAUTH_CALLBACK_REGISTRY("oauthCbReg"),
    /**
     * The name for permanent push rescheduler.
     */
    PUSH_RESCHEDULER("pushRescheduler"),
    ;

    private final ApplicationName applicationName;
    private final MapName name;

    /**
     * Initializes a new {@link CoreMap}.
     *
     * @param name The map name
     */
    private CoreMap(String name) {
        this(() -> name);
    }

    /**
     * Initializes a new {@link CoreMap}.
     *
     * @param name The map name
     */
    private CoreMap(MapName name) {
        applicationName = () -> "ox-map";
        this.name = name;
    }

    /**
     * Gets the map name.
     *
     * @return The map name
     */
    public MapName getMapName() {
        return name;
    }

    /**
     * Gets the application name.
     *
     * @return The application name
     */
    public ApplicationName getApplicationName() {
        return applicationName;
    }

}
