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

import java.util.UUID;
import org.json.JSONException;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.upload.StreamedUpload;
import com.openexchange.mail.compose.Attachment.ContentDisposition;
import com.openexchange.mail.compose.AttachmentResult;
import com.openexchange.mail.compose.CompositionSpaceId;
import com.openexchange.mail.compose.CompositionSpaceService;
import com.openexchange.mail.compose.UploadLimits;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link ReplaceAttachmentMailComposeAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class ReplaceAttachmentMailComposeAction extends AbstractMailComposeAction {

    /**
     * Initializes a new {@link ReplaceAttachmentMailComposeAction}.
     *
     * @param services The service look-up
     */
    public ReplaceAttachmentMailComposeAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException, JSONException {
        // Require composition space identifier
        String sId = requestData.requireParameter("id");
        CompositionSpaceId compositionSpaceId = parseCompositionSpaceId(sId);

        // Require attachment identifier
        String sAttachmentId = requestData.requireParameter("attachmentId");
        UUID attachmentUuid = parseAttachmentId(sAttachmentId);

        // Acquire composition space service
        CompositionSpaceService compositionSpaceService = getCompositionSpaceService(compositionSpaceId.getServiceId(), session);

        // Determine upload quotas
        UploadLimits uploadLimits = compositionSpaceService.getAttachmentUploadLimits(compositionSpaceId.getId());
        boolean hasFileUploads = hasUploads(uploadLimits, requestData);

        StreamedUpload upload = requestData.getStreamedUpload();
        if (null == upload) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
        }

        String disposition = upload.getFormField("contentDisposition");
        if (null == disposition) {
            disposition = ContentDisposition.ATTACHMENT.getId();
        }

        if (!hasFileUploads) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
        }

        // File upload available...
        AttachmentResult attachmentResult = validateAttachmentResult(compositionSpaceService.replaceAttachmentInCompositionSpace(
            compositionSpaceId.getId(), attachmentUuid, upload.getUploadFiles(), disposition, getClientToken(requestData)));
        return new AJAXRequestResult(attachmentResult, "compositionSpaceAttachment").addWarnings(compositionSpaceService.getWarnings());
    }

}
