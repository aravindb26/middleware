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

package com.openexchange.imagetransformation.java.settings;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.imagetransformation.Constants;
import com.openexchange.imagetransformation.ImageTransformationConfig;

/**
 * {@link ImageTransformationMaxHeight} - The setting for max. height image transformation setting.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ImageTransformationMaxHeight extends AbstractImageTransformationSetting<Integer> {

    /**
     * Initializes a new {@link ImageTransformationMaxHeight}.
     */
    public ImageTransformationMaxHeight() {
        super();
    }

    @Override
    protected String getSettingName() {
        return "maxheight";
    }

    @Override
    protected Integer getStaticSetting() {
        return I(Constants.getMaxHeight());
    }

    @Override
    protected Integer getUserSensitiveSetting(ImageTransformationConfig imageTransformationConfig) {
        return I(imageTransformationConfig.getMaxHeight());
    }

}
