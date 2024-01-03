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

package com.openexchange.ajax.mail.actions;

import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractColumnsParser;

/**
 * @author <a href="karsten.will@open-xchange.com">Karsten Will</a>
 */
public class AllParser extends AbstractColumnsParser<AllResponse> {

    /**
     * Default constructor.
     */
    protected AllParser(final boolean failOnError, final int[] columns) {
        super(failOnError, columns);
    }

    @Override
    protected AllResponse instantiateResponse(final Response response) {
        return new AllResponse(response);
    }

}
