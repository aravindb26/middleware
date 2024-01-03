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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link DeleteAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(module = AbstractMailAction.MODULE, type = RestrictedAction.Type.WRITE)
public final class DeleteAction extends AbstractMailAction implements EnqueuableAJAXActionService {

    /**
     * Initializes a new {@link DeleteAction}.
     *
     * @param services The service look-up
     */
    public DeleteAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(MailRequest req) throws OXException {
        try {
            // Read in parameters
            boolean hardDelete = AJAXRequestDataTools.parseBoolParameter(req.getParameter(AJAXServlet.PARAMETER_HARDDELETE));
            boolean returnAffectedFolders = AJAXRequestDataTools.parseBoolParameter(req.getParameter("returnAffectedFolders"));
            JSONArray jsonIds = (JSONArray) req.getRequest().requireData();

            int length = jsonIds.length();
            if (length <= 0) {
                return new AJAXRequestResult(JSONArray.EMPTY_ARRAY, "json");
            }

            // Get mail interface
            MailServletInterface mailInterface = getMailInterface(req);

            // Collect affected mail paths
            List<MailPath> l = new ArrayList<MailPath>(length);
            Map<FullnameArgument, FolderInfo> optFolderInfos = returnAffectedFolders ? new LinkedHashMap<FullnameArgument, FolderInfo>(length) : null;
            for (int i = 0; i < length; i++) {
                JSONObject jId = jsonIds.getJSONObject(i);
                FullnameArgument fa = MailFolderUtility.prepareMailFolderParam(jId.getString(AJAXServlet.PARAMETER_FOLDERID));
                l.add(new MailPath(fa.getAccountId(), fa.getFullname(), jId.getString(AJAXServlet.PARAMETER_ID)));
            }

            // Try to batch-delete per folder
            Collections.sort(l, MailPath.COMPARATOR);
            FullnameArgument lastFldArg = l.get(0).getFullnameArgument();
            List<String> arr = new ArrayList<String>(length);
            for (int i = 0; i < length; i++) {
                MailPath current = l.get(i);
                FullnameArgument folderArgument = current.getFullnameArgument();

                // Check if collectable
                if (!lastFldArg.equals(folderArgument)) {
                    // Delete all collected UIDs until here and reset
                    deleteMailsAndPutFolderInfos(arr.toArray(new String[arr.size()]), hardDelete, lastFldArg, optFolderInfos, mailInterface);
                    arr.clear();
                    lastFldArg = folderArgument;
                }
                arr.add(current.getMailID());
            }

            // Delete all collected remaining UIDs
            int size = arr.size();
            if (size > 0) {
                deleteMailsAndPutFolderInfos(arr.toArray(new String[size]), hardDelete, lastFldArg, optFolderInfos, mailInterface);
            }

            JSONObject jResponse = null;
            if (returnAffectedFolders && null != optFolderInfos && !optFolderInfos.isEmpty()) {
                jResponse = new JSONObject(4);
                jResponse.put("conflicts", JSONArray.EMPTY_ARRAY);

                JSONObject jFolders = new JSONObject(optFolderInfos.size());
                for (Map.Entry<FullnameArgument, FolderInfo> infoEntry : optFolderInfos.entrySet()) {
                    FullnameArgument fa = infoEntry.getKey();
                    String id = MailFolderUtility.prepareFullname(fa.getAccountId(), fa.getFullName());
                    FolderInfo folderInfo = infoEntry.getValue();
                    jFolders.put(id, new JSONObject(4).put("total", folderInfo.total).put("unread", folderInfo.unread));
                }
                jResponse.put("folders", jFolders);
            }

            return new AJAXRequestResult(null == jResponse ? JSONArray.EMPTY_ARRAY : jResponse, "json");
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private static void deleteMailsAndPutFolderInfos(String[] uids, boolean hardDelete, FullnameArgument folderArg, Map<FullnameArgument, FolderInfo> optFolderInfos, MailServletInterface mailInterface) throws OXException {
        mailInterface.deleteMessages(folderArg.getPreparedName(), uids, hardDelete);

        if (null != optFolderInfos) {
            int connectedAccount = mailInterface.getAccountID();

            // Add folder
            optFolderInfos.put(folderArg, FolderInfo.getFolderInfo(folderArg.getFullName(), mailInterface.getMailAccess().getFolderStorage()));

            // Check if trash needs to be added, too
            if (!hardDelete) {
                // Add account's trash folder
                FullnameArgument trash = MailFolderUtility.prepareMailFolderParam(mailInterface.getTrashFolder(connectedAccount));
                if (!trash.equals(folderArg)) {
                    optFolderInfos.put(trash, FolderInfo.getFolderInfo(trash.getFullName(), mailInterface.getMailAccess().getFolderStorage()));
                }
            }
        }
    }

    @Override
    public Result isEnqueueable(AJAXRequestData request, ServerSession session) throws OXException {
        try {
            JSONObject jKeyDesc = new JSONObject(6);
            jKeyDesc.put("module", "mail");
            jKeyDesc.put("action", "delete");

            boolean hardDelete = AJAXRequestDataTools.parseBoolParameter(request.getParameter(AJAXServlet.PARAMETER_HARDDELETE));
            jKeyDesc.put(AJAXServlet.PARAMETER_HARDDELETE, hardDelete);
            boolean returnAffectedFolders = AJAXRequestDataTools.parseBoolParameter(request.getParameter("returnAffectedFolders"));
            jKeyDesc.put("returnAffectedFolders", returnAffectedFolders);

            JSONArray jsonIds = (JSONArray) request.requireData();
            int length = jsonIds.length();
            List<MailPath> l = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                JSONObject jId = jsonIds.getJSONObject(i);
                FullnameArgument fa = MailFolderUtility.prepareMailFolderParam(jId.getString(AJAXServlet.PARAMETER_FOLDERID));
                l.add(new MailPath(fa.getAccountId(), fa.getFullname(), jId.getString(AJAXServlet.PARAMETER_ID)));
            }
            Collections.sort(l);
            jKeyDesc.put("ids", getKeyObjectFor(l));

            return EnqueuableAJAXActionService.resultFor(true, new JobKey(session.getUserId(), session.getContextId(), jKeyDesc.toString()), this);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private static Object getKeyObjectFor(List<MailPath> l) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("md5");
            for (MailPath mailPath : l) {
                md5.update((byte) mailPath.getAccountId());
                md5.update(mailPath.getFolder().getBytes(StandardCharsets.UTF_8));
                md5.update(mailPath.getMailID().getBytes(StandardCharsets.UTF_8));
            }
            return Strings.asHex(md5.digest());
        } catch (Exception e) {
            JSONArray jsonIds = new JSONArray(l.size());
            for (MailPath mailPath : l) {
                jsonIds.put(new JSONObject(2).putSafe(AJAXServlet.PARAMETER_FOLDERID, mailPath.getFolderArgument()).putSafe(AJAXServlet.PARAMETER_ID, mailPath.getMailID()));
            }
            return jsonIds;
        }
    }

}
