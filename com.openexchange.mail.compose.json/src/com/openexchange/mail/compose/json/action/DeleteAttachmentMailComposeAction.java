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

package com.openexchange.mail.compose.json.action;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.compose.AttachmentResult;
import com.openexchange.mail.compose.AttachmentResults;
import com.openexchange.mail.compose.CompositionSpace;
import com.openexchange.mail.compose.CompositionSpaceId;
import com.openexchange.mail.compose.CompositionSpaceService;
import com.openexchange.mail.compose.CompositionSpaces;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link DeleteAttachmentMailComposeAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class DeleteAttachmentMailComposeAction extends AbstractMailComposeAction {

    /**
     * Initializes a new {@link DeleteAttachmentMailComposeAction}.
     *
     * @param services The service look-up
     */
    public DeleteAttachmentMailComposeAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException, JSONException {
        // Require composition space identifier
        String sId = requestData.requireParameter("id");
        CompositionSpaceId compositionSpaceId = parseCompositionSpaceId(sId);

        // Check attachment identifier(s)
        List<UUID> attachmentIds;
        {
            // First, check parameter presence
            String sAttachmentId = requestData.getParameter("attachmentId");
            if (Strings.isNotEmpty(sAttachmentId)) {
                String[] ids = Strings.splitByComma(sAttachmentId);
                attachmentIds = new ArrayList<>(ids.length);
                for (String id : ids) {
                    UUID attachmentUuid = CompositionSpaces.parseAttachmentIdIfValid(id);
                    if (attachmentUuid != null) {
                        attachmentIds.add(attachmentUuid);
                    }
                }
            } else {
                // Now, check for body data
                Object data = requestData.getData();
                if (data == null) {
                    throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
                }
                if (!(data instanceof JSONArray)) {
                    throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
                }

                JSONArray body = (JSONArray) data;
                int size = body.length();
                attachmentIds = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    UUID attachmentUuid = CompositionSpaces.parseAttachmentIdIfValid(body.getString(i));
                    if (attachmentUuid != null) {
                        attachmentIds.add(attachmentUuid);
                    }
                }
            }
        }

        if (attachmentIds.isEmpty()) {
            // Trying to delete non-existing attachment... that's ok
            CompositionSpaceService compositionSpaceService = getCompositionSpaceService(compositionSpaceId.getServiceId(), session);
            CompositionSpace compositionSpace = compositionSpaceService.getCompositionSpace(compositionSpaceId.getId());
            AttachmentResult attachmentResult = AttachmentResults.attachmentResultFor(compositionSpace);
            return new AJAXRequestResult(attachmentResult, "compositionSpaceAttachment");
        }

        // Load composition space
        CompositionSpaceService compositionSpaceService = getCompositionSpaceService(compositionSpaceId.getServiceId(), session);
        AttachmentResult attachmentResult = compositionSpaceService.deleteAttachments(compositionSpaceId.getId(), attachmentIds, getClientToken(requestData));

        return new AJAXRequestResult(attachmentResult, "compositionSpaceAttachment").addWarnings(compositionSpaceService.getWarnings());
    }

}
