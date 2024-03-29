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

package com.openexchange.client.onboarding.mail;

import com.openexchange.exception.OXException;
import com.openexchange.mail.transport.config.TransportAuthSupportAware;
import com.openexchange.mail.transport.config.TransportConfig;

/**
 * {@link TransportConfigTransportSettings}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class TransportConfigTransportSettings implements TransportSettings {

    private final TransportConfig transportConfig;

    /**
     * Initializes a new {@link TransportConfigTransportSettings}.
     */
    public TransportConfigTransportSettings(TransportConfig transportConfig) {
        super();
        this.transportConfig = transportConfig;
    }

    @Override
    public String getLogin() {
        return transportConfig.getLogin();
    }

    @Override
    public String getPassword() {
        return transportConfig.getPassword();
    }

    @Override
    public int getPort() {
        return transportConfig.getPort();
    }

    @Override
    public String getServer() {
        return transportConfig.getServer();
    }

    @Override
    public boolean isSecure() {
        return transportConfig.isSecure();
    }

    @Override
    public boolean isRequireTls() {
        return transportConfig.isRequireTls();
    }

    @Override
    public boolean needsAuthentication() throws OXException {
        return (!(transportConfig instanceof TransportAuthSupportAware) || ((TransportAuthSupportAware) transportConfig).isAuthSupported());
    }

}
