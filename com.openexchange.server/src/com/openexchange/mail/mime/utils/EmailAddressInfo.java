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

package com.openexchange.mail.mime.utils;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import com.openexchange.java.AbstractOperationsWatcher;
import com.openexchange.java.InterruptibleCharSequence;

/**
 * {@link EmailAddressInfo} - The email address information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.14.0
 */
public class EmailAddressInfo extends AbstractOperationsWatcher.Operation {

    /** The constant dummy instance */
    public static final EmailAddressInfo POISON = new EmailAddressInfo() {

        @Override
        public int compareTo(Delayed o) {
            return -1;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }
    };

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final InterruptibleCharSequence addresses;

    /**
     * Dummy constructor
     */
    private EmailAddressInfo() {
        super();
        addresses = null;
    }

    /**
     * Initializes a new {@link EmailAddressInfo}.
     *
     * @param parserThread The thread parsing the email addresses
     * @param addresses The addresses to parse
     * @param timeoutMillis The timeout in milliseconds
     */
    public EmailAddressInfo(Thread parserThread, InterruptibleCharSequence addresses, int timeoutMillis) {
        super(parserThread, timeoutMillis);
        this.addresses = addresses;
    }

    /**
     * Gets the email addresses that are parsed.
     *
     * @return The email addresses
     */
    public InterruptibleCharSequence getAddresses() {
        return addresses;
    }

}
