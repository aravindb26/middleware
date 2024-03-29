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

package com.openexchange.ajax.infostore.actions;

import java.io.IOException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractUploadParser;

/**
 * {@link NewInfostoreParser}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class NewInfostoreParser extends AbstractUploadParser<NewInfostoreResponse> {

    @Override
    public String checkResponse(HttpResponse resp, HttpRequest request) throws ParseException, IOException {
        return super.checkResponse(resp, request);
    }

    private final boolean upload;

    NewInfostoreParser(boolean failOnError, boolean upload) {
        super(failOnError);
        this.upload = upload;
    }

    @Override
    protected Response getResponse(String body) throws JSONException {
        return upload ? super.getResponse(body) : super.getSuperResponse(body);
    }

    @Override
    protected NewInfostoreResponse createResponse(Response response) throws JSONException {
        Assertions.assertNotNull(response);
        return new NewInfostoreResponse(response);
    }
}
