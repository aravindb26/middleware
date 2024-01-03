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

package com.openexchange.rest.client.v2.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Collections;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONServices;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.rest.client.exception.RESTExceptionCodes;
import com.openexchange.rest.client.v2.RESTResponse;

/**
 * {@link JsonRESTResponseBodyParser}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public class JsonRESTResponseBodyParser implements RESTResponseBodyParser {

    private final Set<String> contentTypes;

    /**
     * Initializes a new {@link JsonRESTResponseBodyParser}.
     */
    public JsonRESTResponseBodyParser() {
        super();
        contentTypes = Collections.singleton("application/json");
    }

    @Override
    public void parse(HttpResponse httpResponse, RESTResponse restResponse) throws OXException {
        InputStream content = null;
        PushbackInputStream pis = null;
        try {
            content = httpResponse.getEntity().getContent();
            pis = Streams.getNonEmpty(Streams.bufferedInputStreamFor(content));
            if (pis == null) {
                // Stream was empty
                throw RESTExceptionCodes.JSON_ERROR.create("Missing content");
            }

            char c = (char) pis.read();
            pis.unread(c);
            switch (c) {
                case '{':
                    restResponse.setResponseBody(JSONServices.parseObject(pis));
                    return;
                case '[':
                    restResponse.setResponseBody(JSONServices.parseArray(pis));
                    return;
                default:
                    throw RESTExceptionCodes.JSON_ERROR.create("Unexpected start token detected '" + c + "'");
            }
        } catch (IOException e) {
            throw RESTExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (JSONException e) {
            throw RESTExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } finally {
            Streams.close(pis, content);
        }
    }

    @Override
    public Set<String> getContentTypes() {
        return contentTypes;
    }
}
