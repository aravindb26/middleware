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
package com.openexchange.rest.services;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.Response.Status.Family;

/**
 * {@link CustomStatus} defines a custom {@link StatusType}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public final class CustomStatus implements StatusType {

    private final int code;
    private final Object msg;
    private final Status defaultStatus;

    /**
     * Initializes a new {@link CustomStatus}.
     *
     * @param code The status code
     * @param msg the reason phrase
     */
    public CustomStatus(int code, Object msg) {
        super();
        this.code = code;
        this.msg = msg;
        defaultStatus = Status.fromStatusCode(code);
    }

    @Override
    public int getStatusCode() {
        return code;
    }

    @Override
    public Family getFamily() {
        if (defaultStatus == null) {
            return Family.familyOf(code);
        }

        return defaultStatus.getFamily();
    }

    @Override
    public String getReasonPhrase() {
        if (msg == null) {
            if (defaultStatus == null) {
                return "";
            }

            return defaultStatus.getReasonPhrase();
        }

        return msg.toString();
    }

}