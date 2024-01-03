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

package com.openexchange.mail.utils;

import java.util.List;
import com.openexchange.net.NetUtility;

/**
 * {@link NetUtils} - Network utilities.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class NetUtils {

    /**
     * Initializes a new {@link NetUtils}.
     */
    private NetUtils() {
        super();
    }

    /**
     * Gets a listing of available protocols; e.g. <code>"SSLv3 TLSv1 TLSv1.1 TLSv1.2"</code>
     *
     * @return The protocols' listing
     */
    public static String getProtocolsListing() {
        return NetUtility.getProtocolsListing();
    }

    /**
     * Gets an array containing available protocols.
     *
     * @return The array containing available protocols
     */
    public static String[] getProtocolsArray() {
        return NetUtility.getProtocolsArray();
    }

    /**
     * Gets an unmodifiable list containing all the installed providers.
     * <p>
     * The order of the providers in the {@link List} is their alphabetical order.
     *
     * @return an unmodifiable list containing all the installed provider
     */
    public static List<String> getProtocols() {
        return NetUtility.getProtocols();
    }

}
