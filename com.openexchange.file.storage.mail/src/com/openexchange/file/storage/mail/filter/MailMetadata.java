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

package com.openexchange.file.storage.mail.filter;

import javax.mail.internet.InternetAddress;

/**
 * {@link MailMetadata} - The meta-data for a mail drive file.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public interface MailMetadata {

    /**
     * Gets the original subject.
     *
     * @return The original subject
     */
    String getOriginalSubject();

    /**
     * Gets the original UID.
     *
     * @return The original UID
     */
    Long getOriginalgUid();

    /**
     * Gets the original folder.
     *
     * @return The original folder
     */
    String getOriginalFolder();

    /**
     * Gets the "From" headers.
     *
     * @return The "From" headers
     */
    InternetAddress[] getFromHeaders();

    /**
     * Gets the "To" headers.
     *
     * @return The "To" headers
     */
    InternetAddress[] getToHeaders();

}
