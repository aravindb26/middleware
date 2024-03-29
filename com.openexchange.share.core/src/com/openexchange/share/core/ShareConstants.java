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

package com.openexchange.share.core;


/**
 * {@link ShareConstants}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class ShareConstants {

    /**
     * Path of the share servlet, relative to the servlet prefix. Without
     * leading or trailing slashes.
     */
    public static final String SHARE_SERVLET = "share";

    /**
     * The password mechanism identifier used for the share crypto service.
     * Also used in com.openexchange.groupware.update.tasks.RecryptGuestUserPasswords.PASSWORD_MECH_ID, check before changing this!
     */
    public static final String PASSWORD_MECH_ID = "{CRYPTO_SERVICE}";
}
