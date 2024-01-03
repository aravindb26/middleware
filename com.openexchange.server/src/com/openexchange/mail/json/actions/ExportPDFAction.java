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

package com.openexchange.mail.json.actions;

import static com.openexchange.java.Autoboxing.B;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.EnqueuableAJAXActionService;
import com.openexchange.ajax.requesthandler.crypto.CryptographicServiceAuthenticationFactory;
import com.openexchange.ajax.requesthandler.jobqueue.JobKey;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.exportpdf.DefaultMailExportOptions;
import com.openexchange.mail.exportpdf.DefaultMailExportOptions.Builder;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.mail.exportpdf.MailExportResult;
import com.openexchange.mail.exportpdf.MailExportService;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ExportPDFAction}
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 */
public class ExportPDFAction extends AbstractMailAction implements EnqueuableAJAXActionService {

    private static final String PARAMETER_DECRYPT = "decrypt";

    private final ServiceLookup services;

    /**
     * Initialises a new {@link ExportPDFAction}.
     */
    public ExportPDFAction(ServiceLookup services) {
        super(services);
        this.services = services;
    }

    @Override
    protected AJAXRequestResult perform(MailRequest req) throws OXException, JSONException {
        MailExportService service = services.getServiceSafe(MailExportService.class);
        MailExportResult result = service.exportMail(req.getSession(), getMailExportOptions(req));
        return new AJAXRequestResult(writeResult(result));
    }

    @Override
    public Result isEnqueueable(AJAXRequestData request, ServerSession session) throws OXException {
        try {
            JSONObject jKeyDesc = new JSONObject(5);
            jKeyDesc.put("module", "mail");
            jKeyDesc.put("action", "export_PDF");
            jKeyDesc.put("folderId", MailFolderUtility.prepareMailFolderParam(request.getParameter(AJAXServlet.PARAMETER_FOLDERID)));
            jKeyDesc.put("id", request.requireParameter(AJAXServlet.PARAMETER_ID));
            jKeyDesc.put("driveFolderId", ((JSONObject) request.requireData()).getString(FolderChildFields.FOLDER_ID));
            return EnqueuableAJAXActionService.resultFor(true, new JobKey(session.getUserId(), session.getContextId(), jKeyDesc.toString()), this);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the mail export options from the specified mail request
     *
     * @param req The mail request
     * @return The mail export options
     * @throws OXException if an error is occurred
     * @throws JSONException
     */
    private MailExportOptions getMailExportOptions(MailRequest req) throws OXException, JSONException {
        Object data = req.getRequest().requireData();
        if (!(data instanceof JSONObject)) {
            throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
        }
        JSONObject object = (JSONObject) data;

        FullnameArgument fullName = MailFolderUtility.prepareMailFolderParam(req.getParameter(AJAXServlet.PARAMETER_FOLDERID));
        String uid = req.getRequest().requireParameter(AJAXServlet.PARAMETER_ID);
        String destFolder = object.optString(FolderChildFields.FOLDER_ID);
        if (Strings.isEmpty(destFolder)) {
            throw AjaxExceptionCodes.MISSING_FIELD.create(FolderChildFields.FOLDER_ID);
        }

        boolean appendAttachmentPreviews = object.optBoolean("appendAttachmentPreviews");
        boolean embedAttachmentPreviews = object.optBoolean("embedAttachmentPreviews");
        boolean embedRawAttachments = object.optBoolean("embedRawAttachments");
        boolean embedNonConvertibleNonInlineAttachments = object.optBoolean("embedNonConvertibleAttachments");
        boolean preferRichText = object.optBoolean("preferRichText");
        Boolean includeExternalImages;
        try {
            includeExternalImages = B(object.getBoolean("includeExternalImages"));
        } catch (@SuppressWarnings("unused") JSONException e) {
            includeExternalImages = null;
        }

        Builder builder = new DefaultMailExportOptions.Builder().withAccountId(fullName.getAccountId()).withDestinationFolderId(destFolder).withMailFolderId(fullName.getFullname()).withMailId(uid);

        setAuthToken(req, builder);
        setPageFormat(req.getSession(), object, builder);

        builder.withIncludeExternalImages(includeExternalImages);
        builder.withAppendAttachmentPreviews(appendAttachmentPreviews);
        builder.withEmbedAttachmentPreviews(embedAttachmentPreviews);
        builder.withEmbedRawAttachments(embedRawAttachments);
        builder.withEmbedNonConvertibleAttachments(embedNonConvertibleNonInlineAttachments);
        builder.withRichText(preferRichText);
        return builder.build();
    }

    /**
     * Gets the auth token if <code>decrypt</code> is present
     *
     * @param req The mail request
     * @return The auth token or <code>null</code>
     * @throws OXException if an error is occurred
     */
    private void setAuthToken(MailRequest req, Builder builder) throws OXException {
        CryptographicServiceAuthenticationFactory authFactory = services.getOptionalService(CryptographicServiceAuthenticationFactory.class);
        boolean decrypt = AJAXRequestDataTools.parseBoolParameter(PARAMETER_DECRYPT, req.getRequest());
        String authToken = null;
        if (decrypt && authFactory != null) {
            authToken = authFactory.createAuthenticationFrom(req.getRequest());
        }
        builder.withDecryptFlag(decrypt).withDecryptionToken(authToken);
    }

    /**
     * Sets the page format
     *
     * @param req The session
     * @param object The payload
     * @param builder The builder
     */
    private void setPageFormat(ServerSession session, JSONObject object, Builder builder) {
        String pageFormat = object.optString("pageFormat");
        if (Strings.isNotEmpty(pageFormat)) {
            builder.withPageFormat(pageFormat);
            return;
        }
        String countryCode = session.getUser().getLocale().getCountry();
        if (countryCode.equalsIgnoreCase("us") || countryCode.equalsIgnoreCase("ca")) {
            builder.withPageFormat("letter");
        }
    }

    /**
     * Writes the result as a JSON object
     *
     * @param result The result to write
     * @return The result as JSON object
     * @throws JSONException if a JSON error is occurred
     */
    private JSONObject writeResult(MailExportResult result) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", result.getFileId());
        if (!result.getWarnings().isEmpty()) {
            object.put("warnings", result.getWarnings());
        }
        return object;
    }
}
