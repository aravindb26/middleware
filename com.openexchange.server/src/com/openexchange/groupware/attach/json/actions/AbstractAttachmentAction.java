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

package com.openexchange.groupware.attach.json.actions;

import static com.openexchange.ajax.AJAXServlet.PARAMETER_FOLDERID;
import static com.openexchange.groupware.attach.Attachments.adjustFolderId;
import static com.openexchange.java.Autoboxing.I;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.user.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.Attachment;
import com.openexchange.ajax.fields.ResponseFields;
import com.openexchange.ajax.parser.AttachmentParser;
import com.openexchange.ajax.parser.AttachmentParser.UnknownColumnException;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.oauth.OAuthConstants;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.calendar.contentType.CalendarContentType;
import com.openexchange.folderstorage.database.contentType.ContactsContentType;
import com.openexchange.folderstorage.database.contentType.TaskContentType;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.attach.AttachmentExceptionCodes;
import com.openexchange.groupware.attach.AttachmentField;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.upload.impl.UploadException;
import com.openexchange.groupware.upload.impl.UploadSizeExceededException;
import com.openexchange.java.Strings;
import com.openexchange.oauth.provider.resourceserver.OAuthAccess;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractAttachmentAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractAttachmentAction implements AJAXActionService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractAttachmentAction.class);

    private static final AttachmentParser PARSER = new AttachmentParser();

    protected static final AttachmentBase ATTACHMENT_BASE = Attachment.ATTACHMENT_BASE;

    protected static final List<Integer> ACCEPTED_OAUTH_MODULES = ImmutableList.of(I(FolderObject.CALENDAR), I(FolderObject.CONTACT), I(FolderObject.TASK));

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final AtomicLong maxUploadSize;

    protected final ServiceLookup serviceLookup;

    /**
     * Initializes a new {@link AbstractAttachmentAction}.
     */
    protected AbstractAttachmentAction(final ServiceLookup serviceLookup) {
        super();
        maxUploadSize = new AtomicLong(-2L);
        this.serviceLookup = serviceLookup;
    }

    /**
     * Performs the attachment request.
     * 
     * @param requestData The request data
     * @param session The associated session
     * @return The request result
     */
    protected abstract AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException;

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        /*
         * extract & check if requested folder identifier can be unmangled for internal handling
         */
        String requestedFolderId = requestData.getParameter(PARAMETER_FOLDERID);
        Optional<String> adjustedFolderId = adjustFolderId(requestedFolderId);
        if (false == adjustedFolderId.isPresent()) {
            /*
             * default handling possible
             */
            return doPerform(requestData, session);
        }
        /*
         * composite folder id from default account detected, exchange with relative id for internal handling & re-adjust id in results
         */
        try {
            requestData.putParameter(PARAMETER_FOLDERID, adjustedFolderId.get());
            AJAXRequestResult requestResult = doPerform(requestData, session);
            return exchangeFolderIds(requestData, requestResult, adjustedFolderId.get(), requestedFolderId);
        } finally {
            requestData.putParameter(PARAMETER_FOLDERID, requestedFolderId);
        }
    }

    protected int requireNumber(final AJAXRequestData req, final String parameter) throws OXException {
        String value = req.getParameter(parameter);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            throw AttachmentExceptionCodes.INVALID_REQUEST_PARAMETER.create(nfe, parameter, value);
        }
    }

    protected static void require(final AJAXRequestData req, final String... parameters) throws OXException {
        for (String param : parameters) {
            if (req.getParameter(param) == null) {
                throw UploadException.UploadCode.MISSING_PARAM.create(param);
            }
        }
    }

    private static final String CALLBACK = "callback";

    /**
     * Checks current size of uploaded data against possible quota restrictions.
     *
     * @param size The size
     * @param requestData The associated request data
     * @throws OXException If any quota restrictions are exceeded
     */
    protected void checkSize(final long size, final AJAXRequestData requestData) throws OXException {
        if (maxUploadSize.get() == -2) {
            final long configuredSize = AttachmentConfig.getMaxUploadSize();
            long cur;
            do {
                cur = maxUploadSize.get();
            } while (!maxUploadSize.compareAndSet(cur, configuredSize));
        }

        long maxUploadSize = this.maxUploadSize.get();
        if (maxUploadSize == 0) {
            return;
        }

        if (size > maxUploadSize) {
            if (!requestData.containsParameter(CALLBACK)) {
                requestData.putParameter(CALLBACK, "error");
            }
            throw UploadSizeExceededException.create(size, maxUploadSize, true);
        }
    }

    protected void rollback() {
        try {
            Attachment.ATTACHMENT_BASE.rollback();
        } catch (Exception e) {
            LOG.debug("Rollback failed.", e);
        }
    }

    /**
     * Returns the value of the specified parameter as an integer array
     *
     * @param requestData The request data
     * @param name The name of the parameter
     * @return An integer list or <code>null</code> if the parameter is absent
     * @throws OXException if an invalid value is specified
     */
    protected List<Integer> optIntegerList(AJAXRequestData requestData, String name) throws OXException {
        String value = requestData.getParameter(name);
        if (value == null) {
            return null;
        }
        String[] array = Strings.splitByComma(value);
        List<Integer> retList = new LinkedList<>();
        for (String s : array) {
            try {
                retList.add(Integer.valueOf(s));
            } catch (NumberFormatException e) {
                throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(e, name, name);
            }
        }
        return retList;
    }

    /**
     * Parses the requested attachment fields based on the {@link AJAXServlet#PARAMETER_COLUMNS} from the request.
     * 
     * @param requestData The request to retrieve the fields from
     * @return The parsed attachment fields
     */
    protected static AttachmentField[] getColumns(AJAXRequestData requestData) throws UnknownColumnException {
        return PARSER.getColumns(requestData.getParameterValues(AJAXServlet.PARAMETER_COLUMNS));
    }

    /**
     * Gets the folder ids from the specified request
     *
     * @param requestData The request data
     * @return an integer array with the folder ids
     * @throws OXException if an invalid parameter is supplied
     * @throws JSONException if a JSON error is occurred
     */
    static int[] getIds(AJAXRequestData requestData) throws OXException, JSONException {
        JSONArray idsArray = (JSONArray) requestData.requireData();
        int[] ids = new int[idsArray.length()];
        for (int i = 0; i < idsArray.length(); i++) {
            try {
                ids[i] = idsArray.getInt(i);
            } catch (JSONException e) {
                String string = idsArray.getString(i);
                try {
                    ids[i] = Integer.parseInt(string);
                } catch (NumberFormatException e1) {
                    throw AjaxExceptionCodes.INVALID_PARAMETER.create(string);
                }
            }
        }
        return ids;
    }

    /**
     * Returns the {@link AJAXServlet#PARAMETER_FOLDERID} request parameter
     *
     * @param requestData The request data
     * @return the folder id
     * @throws OXException if the parameter is not set
     */
    int getFolderId(AJAXRequestData requestData) throws OXException {
        return requireNumber(requestData, AJAXServlet.PARAMETER_FOLDERID);
    }

    /**
     * Returns the {@link AJAXServlet#PARAMETER_ATTACHEDID} request parameter
     *
     * @param requestData The request data
     * @return the attached id
     * @throws OXException if the parameter is not set
     */
    int getAttachedId(AJAXRequestData requestData) throws OXException {
        return requireNumber(requestData, AJAXServlet.PARAMETER_ATTACHEDID);
    }

    /**
     * Returns the {@link AJAXServlet#PARAMETER_MODULE} request parameter
     *
     * @param requestData The request data
     * @return the module id
     * @throws OXException if the parameter is not set
     */
    int getModuleId(AJAXRequestData requestData) throws OXException {
        return requireNumber(requestData, AJAXServlet.PARAMETER_MODULE);
    }

    AttachmentField getSort(AJAXRequestData requestData) {
        if (null != requestData.getParameter(AJAXServlet.PARAMETER_SORT)) {
            return AttachmentField.get(Integer.parseInt(requestData.getParameter(AJAXServlet.PARAMETER_SORT)));
        }
        return null;
    }
    
    int getOrder(AJAXRequestData requestData) {
        if ("DESC".equalsIgnoreCase(requestData.getParameter(AJAXServlet.PARAMETER_ORDER))) {
            return AttachmentBase.DESC;
        }
        return AttachmentBase.ASC;
    }

    TimedResult<AttachmentMetadata> getTimedResult(ServerSession session, int folderId, int attachedId, int moduleId, AttachmentField[] fields, AttachmentField sort, int order) throws OXException {
        Context ctx = session.getContext();
        User user = session.getUser();
        UserConfiguration userConfig = session.getUserConfiguration();
        if (sort != null) {
            return ATTACHMENT_BASE.getAttachments(session, folderId, attachedId, moduleId, fields, sort, order, ctx, user, userConfig);
        }
        return ATTACHMENT_BASE.getAttachments(session, folderId, attachedId, moduleId, ctx, user, userConfig);
    }

    /**
     * Parses attachment metadata from the supplied JSON object.
     * 
     * @param json The JSON object to parses attachment metadata from
     * @return The parsed metadata
     */
    protected static AttachmentMetadata parseAttachmentMetadata(JSONObject json) {
        String fieldName = AttachmentField.FOLDER_ID_LITERAL.getName();
        if (null != json && json.hasAndNotNull(fieldName)) {
            Optional<String> adjustedFolderId = adjustFolderId(json.optString(fieldName, null));
            if (adjustedFolderId.isPresent()) {
                return PARSER.getAttachmentMetadata(new JSONObject(json).putSafe(fieldName, adjustedFolderId.get()));
            }
        }
        return PARSER.getAttachmentMetadata(json);
    }

    private static AJAXRequestResult exchangeFolderIds(AJAXRequestData requestData, AJAXRequestResult result, String folderId, String newFolderId) {
        Object resultObject = result.getResultObject();
        if (null == resultObject || false == (resultObject instanceof JSONObject)) {
            return result;
        }
        JSONArray dataArray = ((JSONObject) resultObject).optJSONArray(ResponseFields.DATA);
        if (null != dataArray && 0 < dataArray.length()) {
            try {
                AttachmentField[] columns = getColumns(requestData);
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONArray jsonArray = dataArray.optJSONArray(i);
                    if (null != jsonArray && jsonArray.length() == columns.length) {
                        for (int j = 0; j < jsonArray.length(); j++) {
                            if (AttachmentField.FOLDER_ID_LITERAL == columns[j] && Objects.equals(folderId, jsonArray.optString(j, null))) {
                                jsonArray.put(j, newFolderId);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unexpected error adjusting composite folder identifiers, falling back to plain result", e);
                return result;
            }
        }
        JSONObject dataObject = ((JSONObject) resultObject).optJSONObject(ResponseFields.DATA);
        if (null != dataObject) {
            String fieldName = AttachmentField.FOLDER_ID_LITERAL.getName();
            if (dataObject.hasAndNotNull(fieldName) && Objects.equals(folderId, dataObject.optString(fieldName, null))) {
                dataObject.putSafe(fieldName, newFolderId);
            }
        }
        return result;
    }

    //////////////////////////////////////// OAUTH STUFF //////////////////////////////////////////

    /**
     * Returns the {@link ContentType} for the specified module
     * 
     * @param moduleId The module id
     * @return The content type
     */
    protected ContentType getContentType(int moduleId) {
        return switch (moduleId) {
            case Types.APPOINTMENT -> CalendarContentType.getInstance();
            case Types.CONTACT -> ContactsContentType.getInstance();
            case Types.TASK -> TaskContentType.getInstance();
            default -> null;
        };
    }

    /**
     * Check whether the given request is made via OAuth.
     *
     * @param request The request
     * @return <code>true</code> if so
     */
    protected static boolean isOAuthRequest(AJAXRequestData request) {
        return request.containsProperty(OAuthConstants.PARAM_OAUTH_ACCESS);
    }

    /**
     * Gets the OAuth oauthAccess if the given request is made via OAuth.
     *
     * @param request The request
     * @return The oauthAccess
     */
    protected static OAuthAccess getOAuthAccess(AJAXRequestData request) {
        return request.getProperty(OAuthConstants.PARAM_OAUTH_ACCESS);
    }
}
