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

package com.openexchange.policy.retry;

import com.openexchange.java.CryptoUtil;

/**
 * {@link ExponentialBackOffRetryPolicy}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ExponentialBackOffRetryPolicy extends AbstractRetryPolicy {

    private final double multiplier = 1.5;
    private double randomFactor = 0.5;
    private double interval = 0.5;

    /**
     * Initialises a new {@link ExponentialBackOffRetryPolicy} with a default amount of 10 retries
     */
    public ExponentialBackOffRetryPolicy() {
        this(10);
    }

    /**
     * Initialises a new {@link ExponentialBackOffRetryPolicy}.
     *
     * @param maxTries The amount of maximum retries
     */
    public ExponentialBackOffRetryPolicy(int maxTries) {
        super(maxTries, 0L);
        randomFactor = CryptoUtil.getSecureRandom().nextDouble();
    }

    @Override
    protected long getSleepTime() {
        double max = interval * multiplier;
        double min = max - interval;
        interval = interval * multiplier;
        double factor = (randomFactor * (max - min)) * 1000;
        return Math.round(factor);
    }
}