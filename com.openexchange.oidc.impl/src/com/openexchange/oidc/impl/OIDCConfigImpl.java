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
package com.openexchange.oidc.impl;

import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.oidc.OIDCConfig;
import com.openexchange.oidc.OIDCProperty;
import com.openexchange.server.ServiceLookup;


/**
 * Default implementation of the OpenID feature configuration.
 *
 * @author <a href="mailto:vitali.sjablow@open-xchange.com">Vitali Sjablow</a>
 * @since v7.10.0
 */
public class OIDCConfigImpl implements OIDCConfig{

    private final LeanConfigurationService leanConfigurationService;

    public OIDCConfigImpl(LeanConfigurationService leanConfigurationService) {
        this.leanConfigurationService = leanConfigurationService;
    }

    public OIDCConfigImpl(ServiceLookup serviceLookup) {
        this.leanConfigurationService = serviceLookup.getService(LeanConfigurationService.class);
    }

    @Override
    public boolean isEnabled() {
        return this.leanConfigurationService.getBooleanProperty(OIDCProperty.enabled);
    }

    @Override
    public boolean startDefaultBackend() {
        return this.leanConfigurationService.getBooleanProperty(OIDCProperty.startDefaultBackend);
    }

    @Override
    public boolean isPasswordGrantEnabled() {
        return this.leanConfigurationService.getBooleanProperty(OIDCProperty.enablePasswordGrant);
    }
}
