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

/**
 * {@link PushSubscriptionRestrictions} - Possible restrictions for a push subscription.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class PushSubscriptionRestrictions {

    private int maxNumberOfSuchSubscription;
    private boolean allowSharedToken;

    /**
     * Initializes a new {@link PushSubscriptionRestrictions}.
     */
    public PushSubscriptionRestrictions() {
        super();
    }

    /**
     * Gets the max. number of such subscription.
     * <p>
     * <i>such</i> in terms of subscriptions for the same transport and for the same client of a certain user.
     *
     * @return The max. number of such subscription
     */
    public int getMaxNumberOfSuchSubscription() {
        return maxNumberOfSuchSubscription;
    }

    /**
     * Sets the max. number of such subscription
     *
     * @param maxNumberOfSuchSubscription The max. number of such subscription to set
     */
    public void setMaxNumberOfSuchSubscription(int maxNumberOfSuchSubscription) {
        this.maxNumberOfSuchSubscription = maxNumberOfSuchSubscription;
    }

    /**
     * Gets a value indicating whether the same token can be used for multiple different users or not.
     * 
     * @return <code>true</code> if shared tokens are allowed, <code>false</code> otherwise
     */
    public boolean isAllowSharedToken() {
        return allowSharedToken;
    }

    /**
     * Sets whether the same token can be used by multiple different users or not.
     * 
     * @param allowSharedToken <code>true</code> if shared tokens are allowed, <code>false</code> otherwise
     */
    public void setAllowSharedToken(boolean allowSharedToken) {
        this.allowSharedToken = allowSharedToken;
    }

}
