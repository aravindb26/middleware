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

package com.openexchange.chronos.scheduling.impl;

import static com.openexchange.tools.arrays.Arrays.isNullOrEmpty;
import com.openexchange.chronos.scheduling.MessageStatus;
import com.openexchange.tools.arrays.Arrays;

/**
 * 
 * {@link ITipMailFlag} - iMIP specific user flags for mails
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public enum ITipMailFlag {

    /** The mail has been processed by this server */
    PROCESSED("Processed"),

    /** The mails wasn't processed and further shall be ignored by the scheduling stack */
    IGNORED("Ignore"),

    /** The mail has been analyzed and couldn't be processed automatically */
    ANALYZED("Analyzed")

    ;

    /** The prefix used for the IMAP flag, starting with <code>$</code> */
    private final static String IMAP_FLAG_PREFIX = "$ITip";

    private final String flag;

    /**
     * Initializes a new {@link ITipMailFlag}.
     */
    private ITipMailFlag(String value) {
        this.flag = IMAP_FLAG_PREFIX + value;
    }

    /**
     * Get the state as IMAP flag
     * <p>
     * See also {@link com.openexchange.mail.dataobjects.MailMessage}
     *
     * @return The flag
     */
    public String getFlag() {
        return flag;
    }

    /**
     * Converts the flag to a state
     *
     * @return The {@link MessageStatus} this flag represents or <code>null</code>
     */
    public MessageStatus convert() {
        switch (this) {
            case IGNORED:
                return MessageStatus.IGNORED;
            case ANALYZED:
                return MessageStatus.NEEDS_USER_INTERACTION;
            case PROCESSED:
                return MessageStatus.APPLIED;
            default:
                return null;
        }
    }

    /**
     * Get the fitting iTIP mail flag from a bunch of user flags
     *
     * @param flags The flags to search in
     * @return The fitting {@link ITipMailFlag} or null
     */
    public static ITipMailFlag fromUserFlags(String[] flags) {
        if (isNullOrEmpty(flags)) {
            return null;
        }
        for (ITipMailFlag mailFlag : values()) {
            if (Arrays.contains(flags, mailFlag.flag)) {
                return mailFlag;
            }
        }
        return null;
    }
}
