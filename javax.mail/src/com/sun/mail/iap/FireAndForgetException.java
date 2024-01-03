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

package com.sun.mail.iap;


/**
 * {@link FireAndForgetException} - A special exception that signals IMAP responses were completely ignored and not waited for after issuing an IMAP command.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public class FireAndForgetException extends Exception {

    private static final long serialVersionUID = 2244567125591635500L;

    /**
     * Initializes a new {@link FireAndForgetException}.
     *
     * @param command The issued IMAP command
     * @param  args The arguments
     * @param host The IMAP host
     * @param user The IMAP user
     */
    public FireAndForgetException(String command, Argument args, String host, String user) {
        super(buildErrorMessage(command, args, host, user));
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static String buildErrorMessage(String command, Argument args, String host, String user) {
        StringBuilder sb = new StringBuilder("Reading responses for command '");
        sb.append(command);
        if (args != null) {
            sb.append(' ').append(args.toString());
        }
        sb.append("' from IMAP server '").append(host).append("' has been ignored");
        if (user != null) {
            sb.append(" for user '").append(user).append('\'');
        }
        return sb.toString();
    }

}
