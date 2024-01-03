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

package com.openexchange.mail.compose;

import java.util.Optional;
import com.openexchange.mail.MailPath;

/**
 * {@link CompositionSpaceInfo} - A composition space info.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public interface CompositionSpaceInfo {

    /**
     * Gets the identifier
     *
     * @return The identifier
     */
    CompositionSpaceId getId();

    /**
     * Gets the optional mail path for this message representation.
     *
     * @return The optional mail path
     */
    Optional<MailPath> getMailPath();

    /**
     * Gets the last-modified time stamp, which is the number of milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     * @return The last-modified time stamp
     */
    long getLastModified();

}
