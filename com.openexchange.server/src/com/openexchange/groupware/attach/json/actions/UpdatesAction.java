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

import org.json.JSONValue;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.parser.AttachmentParser.UnknownColumnException;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.writer.AttachmentWriter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.AttachmentField;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.json.OXJSONWriter;
import com.openexchange.oauth.provider.resourceserver.OAuthAccess;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.RestrictedActionUtil;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link UpdatesAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(hasCustomRestrictedAccessCheck = true)
public final class UpdatesAction extends AbstractAttachmentAction {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UpdatesAction.class);

    /**
     * Initializes a new {@link UpdatesAction}.
     */
    public UpdatesAction(final ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    public AJAXRequestResult doPerform(final AJAXRequestData requestData, final ServerSession session) throws OXException {
        try {
            require(
                requestData,
                AJAXServlet.PARAMETER_FOLDERID,
                AJAXServlet.PARAMETER_MODULE,
                AJAXServlet.PARAMETER_ATTACHEDID,
                AJAXServlet.PARAMETER_TIMESTAMP);
            final int folderId = requireNumber(requestData, AJAXServlet.PARAMETER_FOLDERID);
            final int attachedId = requireNumber(requestData, AJAXServlet.PARAMETER_ATTACHEDID);
            final int moduleId = requireNumber(requestData, AJAXServlet.PARAMETER_MODULE);

            long timestamp = -1;
            String timestampStr = requestData.getParameter(AJAXServlet.PARAMETER_TIMESTAMP);
            if (timestampStr == null) {
                throw AjaxExceptionCodes.MISSING_PARAMETER.create(AJAXServlet.PARAMETER_TIMESTAMP);
            }
            try {
                timestamp = Long.parseLong(timestampStr.trim());
            } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
                throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(AJAXServlet.PARAMETER_TIMESTAMP, timestampStr);
            }

            final AttachmentField[] columns = getColumns(requestData);
            final String delete = requestData.getParameter(AJAXServlet.PARAMETER_IGNORE);
            final String timeZoneId = requestData.getParameter(AJAXServlet.PARAMETER_TIMEZONE);

            return new AJAXRequestResult(updates(
                session,
                folderId,
                attachedId,
                moduleId,
                timestamp,
                "deleted".equals(delete),
                columns,
                getSort(requestData),
                getOrder(requestData),
                timeZoneId), "apiResponse");
        } catch (RuntimeException | UnknownColumnException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }
    
    @RestrictedAccessCheck
    public boolean accessAllowed(AJAXRequestData request, ServerSession session, Scope scope) throws OXException {
        return RestrictedActionUtil.mayReadWithScope(getContentType(requireNumber(request, AJAXServlet.PARAMETER_MODULE)), scope);
    }

    private JSONValue updates(final ServerSession session, final int folderId, final int attachedId, final int moduleId, final long ts, final boolean ignoreDeleted, final AttachmentField[] fields, final AttachmentField sort, final int order, final String timeZoneId) throws OXException {
        SearchIterator<AttachmentMetadata> iter = null;
        SearchIterator<AttachmentMetadata> iter2 = null;

        try {
            ATTACHMENT_BASE.startTransaction();

            final Context ctx = session.getContext();
            final User user = session.getUser();
            final UserConfiguration userConfig = session.getUserConfiguration();

            Delta<AttachmentMetadata> delta;
            if (sort != null) {
                delta =
                    ATTACHMENT_BASE.getDelta(session, folderId, attachedId, moduleId, ts, ignoreDeleted, fields, sort, order, ctx, user, userConfig);
            } else {
                delta = ATTACHMENT_BASE.getDelta(session, folderId, attachedId, moduleId, ts, ignoreDeleted, ctx, user, userConfig);
            }
            iter = delta.results();
            iter2 = delta.getDeleted();

            final OXJSONWriter w = new OXJSONWriter();
            final AttachmentWriter aWriter = new AttachmentWriter(w);
            aWriter.timedResult(delta.sequenceNumber());
            aWriter.writeDelta(
                iter,
                iter2,
                fields,
                ignoreDeleted,
                null == timeZoneId ? TimeZoneUtils.getTimeZone(user.getTimeZone()) : TimeZoneUtils.getTimeZone(timeZoneId));
            aWriter.endTimedResult();
            // w.flush();
            ATTACHMENT_BASE.commit();

            return w.getObject();
        } catch (Throwable t) {
            rollback();
            if (t instanceof OXException) {
                throw (OXException) t;
            }
            throw new OXException(t);
        } finally {
            try {
                ATTACHMENT_BASE.finish();
            } catch (OXException e) {
                LOG.error("", e);
            }
            SearchIterators.close(iter);
            SearchIterators.close(iter2);
        }
    }
}
