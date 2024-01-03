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

package com.openexchange.push.dovecot;

import com.openexchange.config.lean.LeanConfigurationService;

/**
 * {@link DovecotPushConfiguration}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.4
 */
public class DovecotPushConfiguration {

    private final LeanConfigurationService configService;

    /**
     * Initializes a new {@link DovecotPushConfiguration}.
     *
     * @param configService The configuration service to use
     */
    public DovecotPushConfiguration(LeanConfigurationService configService) {
        super();
        this.configService = configService;
    }

    public String getClusterLockMech() {
        return configService.getProperty(DovecotPushProperty.clusterLock);
    }

    /**
     * Whether to use stateless implementation.
     *
     * @param services The service look-up to obtain required services from
     * @return <code>true</code> for stateless implementation; otherwise <code>false</code>
     */
    public boolean useStatelessImpl() {
        return configService.getBooleanProperty(DovecotPushProperty.stateless);
    }

    /**
     * Checks whether to prefer Doveadm to issue METADATA commands.
     *
     * @param optionalServices The optional service look-up
     * @return <code>true</code> to prefer Doveadm; otherwise <code>false</code>
     */
    public boolean preferDoveadmForMetadata() {
        return configService.getBooleanProperty(DovecotPushProperty.preferDoveadmForMetadata);
    }

}
