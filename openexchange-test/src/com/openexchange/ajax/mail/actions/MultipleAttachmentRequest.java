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

import java.io.IOException;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.Mail;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractAJAXParser;

/**
 * {@link MultipleAttachmentRequest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class MultipleAttachmentRequest extends AbstractMailRequest<MultipleAttachmentResponse> {

    class MultipleAttachmentParser extends AbstractAJAXParser<MultipleAttachmentResponse> {

        /**
         * Default constructor.
         */
        MultipleAttachmentParser(final boolean failOnError) {
            super(failOnError);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected MultipleAttachmentResponse createResponse(final Response response) throws JSONException {
            return new MultipleAttachmentResponse(response);
        }

        @Override
        public MultipleAttachmentResponse parse(String body) throws JSONException {
            if (body.startsWith("<!doctype html>")) {
                return super.parse(body);
            }
            Response response = new Response();
            response.setData(body.getBytes());
            return new MultipleAttachmentResponse(response);
        }

        @Override
        public String checkResponse(HttpResponse resp, HttpRequest request) throws ParseException, IOException {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                assertTrue(ContentType.get(resp.getEntity()).getMimeType().equals("application/zip"));
                Header header = resp.getFirstHeader("Content-disposition");
                assertNotNull(header);
                final String disp = header.getValue();
                assertNotNull("No Content-disposition header", disp);
                assertTrue(disp.indexOf("filename=") >= 0, "'filename' parameter not found in Content-disposition");
            }
            return super.checkResponse(resp, request);
        }
    }

    /**
     * Unique identifier
     */
    private final String folderId;

    private final String id;

    private final String[] sequenceIds;

    private final boolean failOnError;

    public MultipleAttachmentRequest(final String folder, final String ID, final String[] sequenceIds) {
        super();
        this.folderId = folder;
        this.id = ID;
        this.sequenceIds = sequenceIds;
        failOnError = true;
    }

    @Override
    public Object getBody() throws JSONException {
        return null;
    }

    @Override
    public Method getMethod() {
        return Method.GET;
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[] { new Parameter(AJAXServlet.PARAMETER_ACTION, Mail.ACTION_ZIP_MATTACH), new Parameter(AJAXServlet.PARAMETER_FOLDERID, folderId), new Parameter(AJAXServlet.PARAMETER_ID, id), new Parameter(Mail.PARAMETER_MAILATTCHMENT, sequenceIds) };
    }

    @Override
    public AbstractAJAXParser<MultipleAttachmentResponse> getParser() {
        return new MultipleAttachmentParser(failOnError);
    }

}
