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

package com.openexchange.ajax;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.openexchange.groupware.attach.AttachmentUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.fields.ResponseFields;
import com.openexchange.ajax.fileholder.InputStreamReadable;
import com.openexchange.ajax.fileholder.Readable;
import com.openexchange.ajax.helper.DownloadUtility;
import com.openexchange.ajax.helper.DownloadUtility.CheckedDownload;
import com.openexchange.ajax.parser.AttachmentParser;
import com.openexchange.ajax.request.AttachmentRequest;
import com.openexchange.ajax.request.ServletRequestAdapter;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.attach.AttachmentExceptionCodes;
import com.openexchange.groupware.attach.AttachmentField;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.attach.Attachments;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.groupware.upload.impl.UploadException;
import com.openexchange.groupware.upload.impl.UploadSizeExceededException;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.java.AllocatingStringWriter;
import com.openexchange.java.Streams;
import com.openexchange.json.OXJSONWriter;
import com.openexchange.session.Session;
import com.openexchange.tools.encoding.Helper;
import com.openexchange.tools.exceptions.OXAborted;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.UploadServletException;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.User;

/**
 * Attachment
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class Attachment extends PermissionServlet {

    private static final String MIME_TEXT_HTML_CHARSET_UTF8 = "text/html; charset=UTF-8";

    private static final String MIME_TEXT_HTML = "text/html";

    private static final String PREFIX_JSON = "json_";

    @Serial
    private static final long serialVersionUID = -5819944675070929520L;

    private static transient final AttachmentParser PARSER = new AttachmentParser();

    public static transient final AttachmentField[] REQUIRED = new AttachmentField[] {
        AttachmentField.FOLDER_ID_LITERAL, AttachmentField.ATTACHED_ID_LITERAL, AttachmentField.MODULE_ID_LITERAL };

    public static transient final AttachmentBase ATTACHMENT_BASE = Attachments.getInstance();
    static {
        ATTACHMENT_BASE.setTransactional(true);
    }

    private static transient final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Attachment.class);


    private volatile long maxUploadSize = -2;

    @Override
    protected boolean hasModulePermission(final ServerSession session) {
        return AttachmentRequest.hasPermission(session.getUserConfiguration());
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        final String action = req.getParameter(PARAMETER_ACTION);
        if (action == null) {
            missingParameter(PARAMETER_ACTION, res, false, null);
            return;
        }

        final ServerSession session;
        final User user;
        try {
            session = ServerSessionAdapter.valueOf(getSessionObject(req));
            user = UserStorage.getInstance().getUser(session.getUserId(), session.getContext());
        } catch (OXException e) {
            handle(res, e, action, getSessionObject(req));
            return;
        }

        final Context ctx = session.getContext();
        final UserConfiguration userConfig = UserConfigurationStorage.getInstance().getUserConfigurationSafe(
            session.getUserId(),
            session.getContext());

        if (ACTION_DOCUMENT.equals(action)) {
            try {
                require(req, PARAMETER_FOLDERID, PARAMETER_ATTACHEDID, PARAMETER_MODULE, PARAMETER_ID);
            } catch (OXException e) {
                handle(res, e, action, session);
                return;
            }
            int folderId, attachedId, moduleId, id;
            final String contentType = req.getParameter(PARAMETER_CONTENT_TYPE);
            try {
                folderId = requireNumber(req, res, action, PARAMETER_FOLDERID, session);
                attachedId = requireNumber(req, res, action, PARAMETER_ATTACHEDID, session);
                moduleId = requireNumber(req, res, action, PARAMETER_MODULE, session);
                id = requireNumber(req, res, action, PARAMETER_ID, session);

            } catch (OXAborted x) {
                return;
            }

            document(
                session,
                req,
                res,
                isIE(req),
                folderId,
                attachedId,
                moduleId,
                id,
                contentType,
                ctx,
                user,
                userConfig);
        } else {
            final OXJSONWriter writer = new OXJSONWriter();
            final AttachmentRequest attRequest = new AttachmentRequest(session, writer);
            if (!attRequest.action(action, new ServletRequestAdapter(req, res))) {
                unknownAction("GET", action, res, false);
            }
            writeResponse(res, writer);
        }
    }

    /**
     * Writes the response with the specified writer
     *
     * @param res The response
     * @param writer The writer
     * @throws IOException if an I/O error is occurred
     */
    private void writeResponse(HttpServletResponse res, OXJSONWriter writer) throws IOException {
        res.setContentType(AJAXServlet.CONTENTTYPE_JSON);
        try {
            writer.getObject().write(res.getWriter());
        } catch (JSONException e) {
            if (e.getCause() instanceof IOException) {
                /*
                 * Throw proper I/O error since a serious socket error could been occurred which prevents further communication. Just
                 * throwing a JSON error possibly hides this fact by trying to write to/read from a broken socket connection.
                 */
                throw (IOException) e.getCause();
            }
            LOG.error("", e);
        }
    }

    private int requireNumber(final HttpServletRequest req, final HttpServletResponse res, final String action, final String parameter, final Session session) {
        final String value = req.getParameter(parameter);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            handle(res, AttachmentExceptionCodes.INVALID_REQUEST_PARAMETER.create(parameter, value), action, session);
            throw new OXAborted();
        }
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        final String action = req.getParameter(PARAMETER_ACTION);
        if (action == null) {
            missingParameter(PARAMETER_ACTION, res, false, null);
            return;
        }

        final ServerSession session;
        try {
            session = ServerSessionAdapter.valueOf(getSessionObject(req));
        } catch (OXException e) {
            handle(res, e, action, getSessionObject(req));
            return;
        }

        final OXJSONWriter writer = new OXJSONWriter();
        final AttachmentRequest attRequest = new AttachmentRequest(session, writer);
        if (!attRequest.action(action, new ServletRequestAdapter(req, res))) {
            unknownAction("PUT", action, res, false);
        }
        writeResponse(res, writer);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {

        res.setContentType(MIME_TEXT_HTML);

        final String action = req.getParameter(PARAMETER_ACTION);
        if (action == null) {
            missingParameter(PARAMETER_ACTION, res, true, "attach");
            return;
        }

        final ServerSession session;
        final User user;
        try {
            session = ServerSessionAdapter.valueOf(getSessionObject(req));
            user = UserStorage.getInstance().getUser(session.getUserId(), session.getContext());
        } catch (OXException e) {
            handle(res, e, action, getSessionObject(req));
            return;
        }

        final Context ctx = session.getContext();
        final UserConfiguration userConfig = UserConfigurationStorage.getInstance().getUserConfigurationSafe(
            session.getUserId(),
            session.getContext());

        try {
            checkSize(req.getContentLength());
            if (ACTION_ATTACH.equals(action)) {
                UploadEvent upload = null;
                try {
                    {
                        long maxSize = getMaxUploadSize();
                        upload = processUpload(req, -1L, maxSize > 0 ? maxSize : -1L, session);
                    }
                    final List<AttachmentMetadata> attachments = new ArrayList<AttachmentMetadata>();
                    final List<UploadFile> uploadFiles = new ArrayList<UploadFile>();

                    long sum = 0;
                    final JSONObject json = new JSONObject();
                    final List<UploadFile> l = upload.getUploadFiles();
                    final int size = l.size();
                    final Iterator<UploadFile> iter = l.iterator();
                    for (int a = 0; a < size; a++) {
                        final UploadFile uploadFile = iter.next();
                        final String fileField = uploadFile.getFieldName();
                        final int index = Integer.parseInt(fileField.substring(5));
                        final String obj = upload.getFormField(PREFIX_JSON + index);
                        if (obj == null || obj.length() == 0) {
                            continue;
                        }
                        json.reset();
                        json.parseJSONString(obj);
                        for (final AttachmentField required : REQUIRED) {
                            if (!json.has(required.getName())) {
                                missingParameter(required.getName(), res, true, action);
                            }
                        }

                        final AttachmentMetadata attachment = PARSER.getAttachmentMetadata(json);
                        AttachmentUtility.assureSize(index, attachments, uploadFiles);

                        attachments.set(index, attachment);
                        uploadFiles.set(index, uploadFile);
                        sum += uploadFile.getSize();
                        // checkSingleSize(uploadFile.getSize(), UserSettingMailStorage.getInstance().getUserSettingMail(
                        // session.getUserId(), session.getContext()));
                        checkSize(sum);
                    }

                    attach(res, attachments, uploadFiles, session, ctx, user, userConfig);
                } finally {
                    if (upload != null) {
                        upload.cleanUp();
                    }
                }
            }
        } catch (OXException x) {
            final Response resp = new Response(getSessionObject(req));
            resp.setException(x);
            try {
                res.setContentType(MIME_TEXT_HTML_CHARSET_UTF8);

                throw new UploadServletException(res, substituteJS(
                        ResponseWriter.getJSON(resp).toString(), "error"),
                        x.getMessage(), x);
            } catch (JSONException e) {
                LOG.error("Giving up", e);
            }

        } catch (JSONException e) {
            LOG.error("", e);
        }
    }

    private void document(Session session, final HttpServletRequest req, final HttpServletResponse res, final boolean ie, final int folderId, final int attachedId, final int moduleId, final int id, final String contentType, final Context ctx, final User user, final UserConfiguration userConfig) {
        Readable documentData = null;
        OutputStream os = null;
        boolean rollback = false;
        Exception exc = null;
        try {
            ATTACHMENT_BASE.startTransaction();
            rollback = true;
            final AttachmentMetadata attachment = ATTACHMENT_BASE.getAttachment(session, folderId, attachedId, moduleId, id, ctx, user, userConfig);

            res.setContentLength((int) attachment.getFilesize());

            documentData = new InputStreamReadable(ATTACHMENT_BASE.getAttachedFile(session, folderId, attachedId, moduleId, id, ctx, user, userConfig));

            if (SAVE_AS_TYPE.equals(contentType)) {
                res.setContentType(contentType);
                res.setHeader("Content-Disposition", "attachment; filename=\"" + Helper.escape(Helper.encodeFilename(
                    attachment.getFilename(),
                    "UTF-8",
                    ie)) + "\"");
            } else {
                final CheckedDownload checkedDownload = DownloadUtility.checkInlineDownload(
                    documentData,
                    attachment.getFilename(),
                    attachment.getFileMIMEType(),
                    req.getHeader("user-agent"),
                    ServerSessionAdapter.valueOf(session));

                res.setHeader("Content-Disposition", checkedDownload.getContentDisposition());
                res.setContentType(checkedDownload.getContentType());
                documentData = checkedDownload.getInputStream();
            }

            /*
             * Handle caching headers
             */
            Tools.updateCachingHeaders(req, res);

            os = res.getOutputStream();

            int buflen = 0xFFFF;
            byte[] buffer = new byte[buflen];
            for (int bytesRead; (bytesRead = documentData.read(buffer, 0, buflen)) > 0;) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            os = null; // No need to close the IS anymore

            ATTACHMENT_BASE.commit();
            rollback = false;
        } catch (Exception e) {
            exc = e;
            return;
        } finally {
            if (rollback) {
                // This is a bit convoluted: In case the contentType is not
                // overridden the returned file will be opened
                // in a new window. To call the JS callback routine from a popup we
                // can use parent.callback_error() but
                // must use window.opener.callback_error()
                rollback(exc, res, ResponseFields.ERROR, session);
            }
            Streams.close(documentData);
            Streams.flush(os);
            try {
                ATTACHMENT_BASE.finish();
            } catch (OXException e) {
                LOG.debug("", e);
            }
        }
    }

    private void rollback(final Throwable t, final HttpServletResponse res, final String action, final Session session) {
        try {
            ATTACHMENT_BASE.rollback();
        } catch (OXException e) {
            LOG.debug("", e);
        }
        if (null != t) {
            if (t instanceof OXException) {
                handle(res, (OXException) t, action, session);
            } else {
                handle(res, new OXException(t), action, session);
            }
        }
    }

    private void attach(final HttpServletResponse res, final List<AttachmentMetadata> attachments, final List<UploadFile> uploadFiles, final ServerSession session, final Context ctx, final User user, final UserConfiguration userConfig) {
        AttachmentUtility.initAttachments(attachments, uploadFiles);
        PrintWriter w = null;
        try {
            ATTACHMENT_BASE.startTransaction();
            // final Iterator<AttachmentMetadata> attIter = attachments.iterator();
            final Iterator<UploadFile> ufIter = uploadFiles.iterator();

            final JSONObject result = new JSONObject();
            final JSONArray arr = new JSONArray();

            long timestamp = 0;

            for (final AttachmentMetadata attachment : attachments) {
                // while (attIter.hasNext()) {
                // final AttachmentMetadata attachment = attIter.next();
                final UploadFile uploadFile = ufIter.next();

                attachment.setId(AttachmentBase.NEW);

                final long modified = ATTACHMENT_BASE.attachToObject(attachment, new BufferedInputStream(new FileInputStream(
                    uploadFile.getTmpFile()), 65536), session, ctx, user, userConfig);
                if (modified > timestamp) {
                    timestamp = modified;
                }
                arr.put(attachment.getId());

            }
            result.put(ResponseFields.DATA, arr);
            result.put(ResponseFields.TIMESTAMP, timestamp);
            w = res.getWriter();
            w.print(substituteJS(result.toString(), ACTION_ATTACH));
            ATTACHMENT_BASE.commit();
        } catch (OXException t) {
            try {
                ATTACHMENT_BASE.rollback();
            } catch (OXException e) {
                LOG.error("", e);
            }
            handle(res, t, ResponseFields.ERROR, session);
            return;
        } catch (JSONException e) {
            try {
                ATTACHMENT_BASE.rollback();
            } catch (OXException x) {
                LOG.error("", e);
            }
            handle(res, AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage()), ResponseFields.ERROR, session);
            return;
        } catch (IOException e) {
            try {
                ATTACHMENT_BASE.rollback();
            } catch (OXException x) {
                LOG.error("", e);
            }
            handle(res, AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage()), ResponseFields.ERROR, session);
            return;
        } finally {
            try {
                ATTACHMENT_BASE.finish();
            } catch (OXException e) {
                LOG.debug("", e);
            }
        }
    }

    private void handle(final HttpServletResponse res, final OXException t, final String action, final Session session) {
        res.setContentType(MIME_TEXT_HTML_CHARSET_UTF8);

        final Response resp = new Response();
        resp.setException(t);
        Writer writer = null;

        try {
            writer = new AllocatingStringWriter();
            ResponseWriter.write(resp, writer, localeFrom(session));
            res.getWriter().write(substituteJS(writer.toString(), action));
        } catch (JSONException e) {
            LOG.error("", t);
        } catch (IOException e) {
            LOG.error("", e);
        }
    }

    private long getMaxUploadSize() {
        long tmp = maxUploadSize;
        if (tmp == -2) {
            synchronized (this) {
                tmp = maxUploadSize;
                if (tmp == -2) {
                    tmp = AttachmentConfig.getMaxUploadSize();
                    maxUploadSize = tmp;
                }
            }
        }
        return tmp;
    }

    private void checkSize(final long size) throws OXException {
        long maxUploadSize = getMaxUploadSize();
        if (maxUploadSize == 0) {
            return;
        }
        if (size > maxUploadSize) {
            throw UploadSizeExceededException.create(size, maxUploadSize, true);
        }
    }

    protected void require(final HttpServletRequest req, final String... parameters) throws OXException {
        for (final String param : parameters) {
            if (req.getParameter(param) == null) {
                throw UploadException.UploadCode.MISSING_PARAM.create(param);
            }
        }
    }

}
