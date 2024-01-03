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

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.parser.AttachmentParser.UnknownColumnException;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.writer.AttachmentWriter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentField;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.json.OXJSONWriter;
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
import org.json.JSONException;
import org.json.JSONValue;

import java.util.TimeZone;

/**
 * {@link ListAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(hasCustomRestrictedAccessCheck = true)
public final class ListAction extends AbstractAttachmentAction {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ListAction.class);

    /**
     * Initializes a new {@link ListAction}.
     */
    public ListAction(ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    public AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            require(requestData, AJAXServlet.PARAMETER_FOLDERID, AJAXServlet.PARAMETER_MODULE, AJAXServlet.PARAMETER_ATTACHEDID);
            int[] ids = getIds(requestData);
            AttachmentField[] columns = getColumns(requestData);
            String timeZoneId = requestData.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            JSONValue jsonValue = list(session, getFolderId(requestData), getAttachedId(requestData), getModuleId(requestData), ids, columns, timeZoneId);
            return new AJAXRequestResult(jsonValue, "apiResponse");
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException | UnknownColumnException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private JSONValue list(ServerSession session, int folderId, int attachedId, int moduleId, int[] ids, AttachmentField[] fields, String timeZoneId) throws OXException {
        SearchIterator<AttachmentMetadata> iter = null;
        User user = session.getUser();
        TimeZone tz = TimeZoneUtils.getTimeZone(null == timeZoneId ? user.getTimeZone() : timeZoneId);
        try {
            ATTACHMENT_BASE.startTransaction();

            Context ctx = session.getContext();
            UserConfiguration userConfig = session.getUserConfiguration();
            TimedResult<AttachmentMetadata> result = ATTACHMENT_BASE.getAttachments(session, folderId, attachedId, moduleId, ids, fields, ctx, user, userConfig);

            iter = result.results();

            OXJSONWriter w = new OXJSONWriter();
            AttachmentWriter aWriter = new AttachmentWriter(w);
            aWriter.timedResult(result.sequenceNumber());
            aWriter.writeAttachments(iter, fields, tz);
            aWriter.endTimedResult();

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
        }
    }

    @RestrictedAccessCheck
    public boolean accessAllowed(AJAXRequestData request, ServerSession session, Scope scope) throws OXException {
        return RestrictedActionUtil.mayReadWithScope(getContentType(requireNumber(request, AJAXServlet.PARAMETER_MODULE)), scope);
    }
}
