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
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.RestrictedActionUtil;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;
import org.json.JSONException;

import java.util.Date;

/**
 * {@link DetachAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(hasCustomRestrictedAccessCheck = true)
public final class DetachAction extends AbstractAttachmentAction {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DetachAction.class);

    /**
     * Initializes a new {@link DetachAction}.
     */
    public DetachAction(ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    public AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            require(requestData, AJAXServlet.PARAMETER_FOLDERID, AJAXServlet.PARAMETER_MODULE, AJAXServlet.PARAMETER_ATTACHEDID);
            int[] ids = getIds(requestData);
            Date timestamp = detach(session, getFolderId(requestData), getAttachedId(requestData), getModuleId(requestData), ids);
            return new AJAXRequestResult("", timestamp, "string");
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private Date detach(ServerSession session, int folderId, int attachedId, int moduleId, int[] ids) throws OXException {
        long timestamp = 0;
        try {
            ATTACHMENT_BASE.startTransaction();

            Context ctx = session.getContext();
            User user = session.getUser();
            UserConfiguration userConfig = session.getUserConfiguration();
            timestamp = ATTACHMENT_BASE.detachFromObject(folderId, attachedId, moduleId, ids, session, ctx, user, userConfig);

            ATTACHMENT_BASE.commit();
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
        }
        return new Date(timestamp);
    }

    @RestrictedAccessCheck
    public boolean accessAllowed(AJAXRequestData request, ServerSession session, Scope scope) throws OXException {
        return RestrictedActionUtil.mayWriteWithScope(getContentType(requireNumber(request, AJAXServlet.PARAMETER_MODULE)), scope);
    }
}
