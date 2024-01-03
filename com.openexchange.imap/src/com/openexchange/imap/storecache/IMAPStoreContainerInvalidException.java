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

package com.openexchange.imap.storecache;


/**
 * {@link IMAPStoreContainerInvalidException} - Special exception thrown when
 * {@link IMAPStoreContainer#getStore(javax.mail.Session, String, String, com.openexchange.session.Session) getStore()} method is invoked,
 * but {@code IMAPStoreContainer} instance has been invalidated.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.4
 */
public class IMAPStoreContainerInvalidException extends Exception {

    private static final long serialVersionUID = -8486026072974077164L;

    /**
     * Initializes a new {@link IMAPStoreContainerInvalidException}.
     */
    public IMAPStoreContainerInvalidException() {
        super("IMAP store container is invalid");
    }

}
