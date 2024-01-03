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

package com.openexchange.xing.exception;

/**
 * {@link XingInvalidFieldException} - Throws if XING API complains about one or more invalid fields.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class XingInvalidFieldException extends XingException {

    private static final long serialVersionUID = -5569197291690418791L;

    /**
     * Initializes a new {@link XingInvalidFieldException}.
     *
     * @param detailMessage The detail message describing the invalid fields
     */
    public XingInvalidFieldException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Initializes a new {@link XingInvalidFieldException}.
     *
     * @param detailMessage The detail message describing the invalid fields
     * @param throwable The cause
     */
    public XingInvalidFieldException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
