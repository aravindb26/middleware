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

package com.openexchange.imap.util;

import javax.mail.MessagingException;

/**
 * {@link PartNotFoundException} - Thrown when a certain part could not be found inside a message.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class PartNotFoundException extends MessagingException {

    private static final long serialVersionUID = 582612108891249403L;

    /**
     * Constructs a PartNotFoundException.
     */
    public PartNotFoundException() {
        super();
    }

    /**
     * Constructs a PartNotFoundException with the specified detail message.
     *
     * @param s The detailed error message
     */
    public PartNotFoundException(String s) {
        super(s);
    }

    /**
     * Constructs a PartNotFoundException with the specified detail message and embedded exception.
     * <p>
     * The exception is chained to this exception.
     *
     * @param s The detailed error message
     * @param e The embedded exception
     */
    public PartNotFoundException(String s, Exception e) {
        super(s, e);
    }

}
