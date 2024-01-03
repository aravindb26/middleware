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

import org.json.JSONException;
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
import com.openexchange.groupware.results.TimedResult;
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
 * {@link AllAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(hasCustomRestrictedAccessCheck = true)
public final class AllAction extends AbstractAttachmentAction {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllAction.class);

    /**
     * Initializes a new {@link AllAction}.
     */
    public AllAction(ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    public AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            require(requestData, AJAXServlet.PARAMETER_FOLDERID, AJAXServlet.PARAMETER_MODULE, AJAXServlet.PARAMETER_ATTACHEDID);
            int folderId = requireNumber(requestData, AJAXServlet.PARAMETER_FOLDERID);
            int attachedId = requireNumber(requestData, AJAXServlet.PARAMETER_ATTACHEDID);
            int moduleId = requireNumber(requestData, AJAXServlet.PARAMETER_MODULE);
            AttachmentField[] columns = getColumns(requestData);
            JSONValue jsonValue = all(session, folderId, attachedId, moduleId, columns, getSort(requestData), getOrder(requestData));
            return new AJAXRequestResult(jsonValue, "apiResponse");
        } catch (RuntimeException | UnknownColumnException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private JSONValue all(ServerSession session, int folderId, int attachedId, int moduleId, AttachmentField[] fields, AttachmentField sort, int order) throws OXException {
        SearchIterator<AttachmentMetadata> iter = null;
        boolean rollback = false;
        try {
            ATTACHMENT_BASE.startTransaction();
            rollback = true;

            TimedResult<AttachmentMetadata> result = getTimedResult(session, folderId, attachedId, moduleId, fields, sort, order);
            iter = result.results();
            User user = session.getUser();
            OXJSONWriter w = new OXJSONWriter();
            AttachmentWriter aWriter = new AttachmentWriter(w);
            aWriter.timedResult(result.sequenceNumber());
            aWriter.writeAttachments(iter, fields, TimeZoneUtils.getTimeZone(user.getTimeZone()));
            aWriter.endTimedResult();
            // w.flush();
            ATTACHMENT_BASE.commit();
            rollback = false;
            return w.getObject();
        } catch (JSONException e) {
            throw new OXException(e);
        } finally {
            if (rollback) {
                rollback();
            }
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
