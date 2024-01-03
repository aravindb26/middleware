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

package com.openexchange.version.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.version.Version;
import com.openexchange.version.VersionService;

/**
 * Stores the version of the Middleware.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a> JavaDoc
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a> - Refactoring
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a> Refactoring
 */
public class VersionServiceImpl implements VersionService {

    private static final Logger LOG = LoggerFactory.getLogger(VersionServiceImpl.class);
    private static final String VERSION_FILENAME = "/opt/open-xchange/version.txt";
    private static final String BACKUP_VERSION_FILENAME = "/version.txt";

    private final Version serverVersion;

    /**
     * Initializes a new {@link VersionServiceImpl}.
     */
    public VersionServiceImpl(){
        super();
        Version version;
        try (FileInputStream fis = new FileInputStream(VERSION_FILENAME)) {
            version = Version.parse(IOUtils.toString(fis, "UTF-8"));
        } catch (IOException | IllegalArgumentException e) {
            version = optBackupVersion().orElse(Version.builder().build());
            LOG.warn("Unable to parse version from {}, falling back to {}.", VERSION_FILENAME, version, e);
        }
        this.serverVersion = version;
    }

    /**
     * Parses {@value #BACKUP_VERSION_FILENAME} as a backup version if possible.
     *
     * @return The optional backup version
     */
    private Optional<Version> optBackupVersion(){
        try (InputStream fis = getClass().getResourceAsStream(BACKUP_VERSION_FILENAME)){
            if (fis == null) {
                return Optional.empty();
            }
            return Optional.of(Version.parse(IOUtils.toString(fis, "UTF-8").substring(9)));
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("Unable to parse backup version.", e);
            return Optional.empty();
        }
    }

    @Override
    public Version getVersion() {
        return this.serverVersion;
    }

}

