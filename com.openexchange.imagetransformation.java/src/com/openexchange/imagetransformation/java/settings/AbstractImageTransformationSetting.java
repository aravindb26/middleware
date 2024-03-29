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

import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.ReadOnlyValue;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.imagetransformation.ImageTransformationConfig;
import com.openexchange.imagetransformation.java.osgi.Services;
import com.openexchange.jslob.ConfigTreeEquivalent;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 * {@link AbstractImageTransformationSetting} - The abstract image setting.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 * @param <V> The type of the setting
 */
public abstract class AbstractImageTransformationSetting<V> implements PreferencesItemService, ConfigTreeEquivalent {

    /**
     * Initializes a new {@link AbstractImageTransformationSetting}.
     */
    protected AbstractImageTransformationSetting() {
        super();
    }

    /**
     * Gets the name of this setting; e.g. <code>"maxwidth"</code>.
     *
     * @return The name of this setting
     */
    protected abstract String getSettingName();

    /**
     * Gets the static setting.
     *
     * @return The setting
     */
    protected abstract V getStaticSetting();

    /**
     * Gets the setting from given user-sensitive image transformation configuration.
     *
     * @param imageTransformationConfig The image transformation configuration
     * @return The setting
     */
    protected abstract V getUserSensitiveSetting(ImageTransformationConfig imageTransformationConfig);

    @Override
    public String[] getPath() {
        return new String[] { "modules", "image", "transformation", getSettingName() };
    }

    @Override
    public IValueHandler getSharedValue() {
        return new ReadOnlyValue() {

            @Override
            public boolean isAvailable(UserConfiguration userConfig) {
                return true;
            }

            @Override
            public void getValue(Session session, Context ctx, User user, UserConfiguration userConfig, Setting setting) throws OXException {
                ConfigViewFactory factory = Services.optService(ConfigViewFactory.class);
                if (factory == null){
                    setting.setSingleValue(getStaticSetting());
                    return;
                }

                ImageTransformationConfig imageTransformationConfig = ImageTransformationConfig.getConfig(session.getUserId(), session.getContextId(), factory);
                setting.setSingleValue(getUserSensitiveSetting(imageTransformationConfig));
            }
        };
    }

    @Override
    public String getConfigTreePath() {
        return "modules/image/transformation/" + getSettingName();
    }

    @Override
    public String getJslobPath() {
        return "io.ox/image//transformation/" + getSettingName();
    }

}
