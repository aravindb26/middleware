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


package com.sun.mail.imap;

import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;

/**
 * {@link IMAPCommandAndResultHandler} - Handles an IMAP command, its associated arguments and the resulting response
 * (if not yet intercepted by an instance of <code>com.sun.mail.iap.ResponseInterceptor</code>).
 * <p>
 * Inject instances via <code>"mail.imap.commandAndResultHandler"</code> property; either single instances or a collection.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public interface IMAPCommandAndResultHandler {

    /**
     * Handles given IMAP command, its associated arguments and the resulting response.
     *
     * @param command The IMAP command; e.g. <code>"SORT"</code> or <code>"UID FETCH"</code>
     * @param args The arguments for the IMAP command
     * @param responses The parsed responses from IMAP server
     * @throws ProtocolException If handling fails
     */
    void handle(String command, Argument args, Response[] responses) throws ProtocolException;
}
