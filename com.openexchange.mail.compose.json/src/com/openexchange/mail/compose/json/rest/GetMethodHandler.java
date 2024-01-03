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

package com.openexchange.mail.compose.json.rest;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.exception.OXException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link GetMethodHandler} - Serves the REST-like <code>GET</code> request.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GetMethodHandler extends AbstractMailComposeMethodHandler {

    /**
     * Initializes a new {@link GetMethodHandler}.
     */
    public GetMethodHandler() {
        super();
    }

    // GET /mail/compose

    @Override
    protected void modifyByPathInfo(AJAXRequestData requestData, String[] restPathElements, HttpServletRequest req) throws IOException, OXException {
        String action = getAction(restPathElements, req);
        if (action == null) {
            throw AjaxExceptionCodes.BAD_REQUEST.create();
        }
        requestData.setAction(action);

        if ("all".equals(action)) {
            return;
        }

        if ("get".equals(action)) {
            /*-
             *  GET /mail/compose/123
             */
            requestData.putParameter("id", restPathElements[0]);
        } else if ("getAttachment".equals(action)) {
            /*-
             *  GET /mail/compose/123/attachments/1
             */
            if (!"attachments".equals(restPathElements[1])) {
                throw AjaxExceptionCodes.UNKNOWN_ACTION.create(req.getPathInfo());
            }

            requestData.putParameter("id", restPathElements[0]);
            requestData.putParameter("attachmentId", restPathElements[2]);
        } else {
            throw AjaxExceptionCodes.UNKNOWN_ACTION.create(req.getPathInfo());
        }
    }

    @Override
    protected String doGetAction(String[] restPathElements, HttpServletRequest restRequest) {
        if (hasNoPathInfo(restPathElements)) {
            return "all";
        }

        int length = restPathElements.length;
        if (0 == length) {
            /*-
             *  GET /mail/compose
             */
            return "all";
        }

        if (1 == length) {
            /*-
             *  GET /mail/compose/123
             */
            return "get";
        } else if (3 == length) {
            /*-
             *  GET /mail/compose/123/attachments/1
             */
            return "getAttachment";
        }
        return null;
    }

}
