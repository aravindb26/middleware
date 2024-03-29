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

package com.openexchange.ajax.attach.actions;

import org.json.JSONException;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.java.Strings;

/**
 * 
 * {@link DetachParser}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.4
 */
public class DetachParser extends AbstractAJAXParser<DetachResponse> {

    public DetachParser(boolean failOnError) {
        super(failOnError);
    }

    @Override
    protected DetachResponse createResponse(Response response) throws JSONException {
        final DetachResponse retval = new DetachResponse(response);
        final String data = (String) response.getData();
        if (Strings.isEmpty(data)) { // parent object deleted in the meantime
            return retval;
        }
        final int objectId = Integer.parseInt(data);
        if (isFailOnError()) {
            assertTrue(objectId > 0, "Problem while inserting object.");
        }
        retval.setId(objectId);
        return retval;
    }
}
