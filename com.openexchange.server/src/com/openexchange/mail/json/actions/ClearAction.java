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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.EnqueuableAJAXActionService;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.requesthandler.jobqueue.JobKey;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ClearAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(module = AbstractMailAction.MODULE, type = RestrictedAction.Type.WRITE)
public final class ClearAction extends AbstractMailAction implements EnqueuableAJAXActionService {

    /**
     * Initializes a new {@link ClearAction}.
     *
     * @param services The service look-up
     */
    public ClearAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(MailRequest req) throws OXException {
        try {
            //final ServerSession session = req.getSession();
            /*
             * Read in parameters
             */
            final JSONArray ja = (JSONArray) req.getRequest().requireData();
            final boolean hardDelete = req.optBool(AJAXServlet.PARAMETER_HARDDELETE, false);
            /*
             * Clear folder sequentially
             */
            final MailServletInterface mailInterface = getMailInterface(req);
            final int length = ja.length();
            final JSONArray ret = new JSONArray();
            for (int i = 0; i < length; i++) {
                final String folderId = ja.getString(i);
                if (!mailInterface.clearFolder(folderId, hardDelete)) {
                    /*
                     * Something went wrong
                     */
                    ret.put(folderId);
                }
            }
            return new AJAXRequestResult(ret, "json");
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public Result isEnqueueable(AJAXRequestData request, ServerSession session) throws OXException {
        try {
            JSONObject jKeyDesc = new JSONObject(6);
            jKeyDesc.put("module", "mail");
            jKeyDesc.put("action", "clear");

            boolean hardDelete = AJAXRequestDataTools.parseBoolParameter(request.getParameter(AJAXServlet.PARAMETER_HARDDELETE));
            jKeyDesc.put(AJAXServlet.PARAMETER_HARDDELETE, hardDelete);

            JSONArray ja = (JSONArray) request.requireData();
            int length = ja.length();
            List<String> l = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                l.add(ja.getString(i));
            }
            Collections.sort(l);
            jKeyDesc.put("ids", getKeyObjectFor(l));

            return EnqueuableAJAXActionService.resultFor(true, new JobKey(session.getUserId(), session.getContextId(), jKeyDesc.toString()), this);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private static Object getKeyObjectFor(List<String> l) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("md5");
            for (String folderId : l) {
                md5.update(folderId.getBytes(StandardCharsets.UTF_8));
            }
            return Strings.asHex(md5.digest());
        } catch (Exception e) {
            JSONArray jsonIds = new JSONArray(l.size());
            for (String folderId : l) {
                jsonIds.put(folderId);
            }
            return jsonIds;
        }
    }

}
