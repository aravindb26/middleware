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

package com.openexchange.pns;

import java.util.Date;
import java.util.List;

/**
 * {@link PushSubscriptionDescription} - A push subscription description.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class PushSubscriptionDescription {

    /**
     * Creates the appropriate instance of <code>PushSubscriptionDescription</code> from given push subscription.
     *
     * @param pushSubscription The push subscription to create from
     * @return The resulting instance of <code>PushSubscriptionDescription</code
     */
    public static PushSubscriptionDescription fromPushSubscription(PushSubscription pushSubscription) {
        if (pushSubscription == null) {
            return null;
        }

        PushSubscriptionDescription desc = new PushSubscriptionDescription();
        desc.setContextId(pushSubscription.getContextId());
        desc.setUserId(pushSubscription.getUserId());
        desc.setClient(pushSubscription.getClient());
        desc.setToken(pushSubscription.getToken());
        desc.setTopics(pushSubscription.getTopics());
        desc.setTransportId(pushSubscription.getTransportId());
        desc.setExpires(pushSubscription.getExpires());
        return desc;
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    private int contextId;
    private int userId;
    private String client;
    private List<String> topics;
    private String transportId;
    private Token token;
    private Date expires;

    /**
     * Initializes a new {@link PushSubscriptionDescription}.
     */
    public PushSubscriptionDescription() {
        super();
    }

    /**
     * Gets the context identifier.
     *
     * @return The context identifier
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Sets the context identifier.
     *
     * @param contextId The context identifier to set
     * @return This instance
     */
    public PushSubscriptionDescription setContextId(int contextId) {
        this.contextId = contextId;
        return this;
    }

    /**
     * Gets the user identifier.
     *
     * @return The user identifier
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier.
     *
     * @param userId The user identifier to set
     * @return This instance
     */
    public PushSubscriptionDescription setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Gets the client identifier.
     *
     * @return The client identifier
     */
    public String getClient() {
        return client;
    }

    /**
     * Sets the client identifier.
     *
     * @param client The client identifier to set
     * @return This instance
     */
    public PushSubscriptionDescription setClient(String client) {
        this.client = client;
        return this;
    }

    /**
     * Gets the topics.
     *
     * @return The topics
     */
    public List<String> getTopics() {
        return topics;
    }

    /**
     * Sets the topics.
     *
     * @param topics The topics to set
     * @return This instance
     */
    public PushSubscriptionDescription setTopics(List<String> topics) {
        this.topics = topics;
        return this;
    }

    /**
     * Gets the transport identifier.
     *
     * @return The transport identifier
     */
    public String getTransportId() {
        return transportId;
    }

    /**
     * Sets the transport identifier.
     *
     * @param transportId The transport identifier to set
     * @return This instance
     */
    public PushSubscriptionDescription setTransportId(String transportId) {
        this.transportId = transportId;
        return this;
    }

    /**
     * Gets the token.
     *
     * @return The token
     */
    public Token getToken() {
        return token;
    }

    /**
     * Sets the token.
     *
     * @param token The token to set
     * @return This instance
     */
    public PushSubscriptionDescription setToken(Token token) {
        this.token = token;
        return this;
    }

    /**
     * Gets the expiration date.
     *
     * @return The expiration date or <code>null</code>
     */
    public Date getExpires() {
        return expires;
    }

    /**
     * Sets the expiration date
     *
     * @param expires The expiration date to set
     * @return This instance
     */
    public PushSubscriptionDescription setExpires(Date expires) {
        this.expires = expires;
        return this;
    }

}
