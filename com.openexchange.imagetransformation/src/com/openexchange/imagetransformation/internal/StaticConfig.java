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

package com.openexchange.imagetransformation.internal;

import com.openexchange.imagetransformation.Constants;
import com.openexchange.imagetransformation.ImageTransformationConfig;
import com.openexchange.imagetransformation.Utility;

/**
 * {@link StaticConfig} - The static configuration.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class StaticConfig extends ImageTransformationConfig {

    private static final StaticConfig INSTANCE = new StaticConfig();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static StaticConfig getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link StaticConfig}.
     */
    private StaticConfig() {
        super();
    }

    @Override
    public int getMaxHeight() {
        return Constants.getMaxHeight();
    }

    @Override
    public int getMaxWidth() {
        return Constants.getMaxWidth();
    }

    @Override
    public int getTransformationWaitTimeoutSeconds() {
        return Utility.waitTimeoutSeconds();
    }

    @Override
    public long getTransformationMaxSize() {
        return Utility.maxSize();
    }

    @Override
    public long getTransformationMaxResolution() {
        return Utility.maxResolution();
    }

    @Override
    public float getTransformationPreferThumbnailThreshold() {
        return Utility.preferThumbnailThreshold();
    }

}
