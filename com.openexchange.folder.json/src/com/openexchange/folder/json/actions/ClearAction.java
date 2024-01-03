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

package com.openexchange.folder.json.actions;

import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.EnqueuableAJAXActionService;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.requesthandler.jobqueue.JobKey;
import com.openexchange.exception.OXException;
import com.openexchange.folder.json.services.ServiceRegistry;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.RestrictedActionUtil;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ClearAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(type = RestrictedAction.Type.WRITE, hasCustomRestrictedAccessCheck = true)
public final class ClearAction extends AbstractFolderAction implements EnqueuableAJAXActionService {

    private static final String PARAM_TREE = "tree";
    public static final String ACTION = AJAXServlet.ACTION_CLEAR;

    /**
     * Initializes a new {@link ClearAction}.
     */
    public ClearAction() {
        super();
    }

    @Override
    protected AJAXRequestResult doPerform(final AJAXRequestData request, final ServerSession session) throws OXException, JSONException {
        /*
         * Parse parameters
         */
        String treeId = request.getParameter(PARAM_TREE);
        if (null == treeId) {
            /*
             * Fallback to default tree identifier
             */
            treeId = getDefaultTreeIdentifier();
        }
        /*
         * Compose JSON array with id
         */
        final JSONArray jsonArray = (JSONArray) request.requireData();
        final int len = jsonArray.length();
        /*
         * Delete
         */
        final List<OXException> warnings = new LinkedList<>();
        final JSONArray responseArray = new JSONArray();
        final FolderService folderService = ServiceRegistry.getInstance().getService(FolderService.class, true);
        for (int i = 0; i < len; i++) {
            final String folderId = jsonArray.getString(i);
            try {
                folderService.clearFolder(treeId, folderId, session);
            } catch (OXException e) {
                final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClearAction.class);
                log.error("", e);
                responseArray.put(folderId);
                e.setCategory(com.openexchange.exception.Category.CATEGORY_WARNING);
                warnings.add(e);
            }
        }
        /*
         * Return appropriate result
         */
        return new AJAXRequestResult(responseArray).addWarnings(warnings);
    }

    @RestrictedAccessCheck
    public static boolean accessAllowed(final AJAXRequestData request, final ServerSession session, final Scope scope) throws OXException {
        final JSONArray jsonArray = (JSONArray) request.requireData();
        final int len = jsonArray.length();
        String treeId = request.getParameter("tree");
        if (null == treeId) {
            treeId = getDefaultTreeIdentifier();
        }

        final FolderService folderService = ServiceRegistry.getInstance().getService(FolderService.class, true);
        try {
            for (int i = 0; i < len; i++) {
                final String folderId = jsonArray.getString(i);
                UserizedFolder folder = folderService.getFolder(treeId, folderId, session, getDecorator(request));
                if (!RestrictedActionUtil.mayWriteWithScope(folder.getContentType(), scope)) {
                    return false;
                }
            }

            return true;
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public Result isEnqueueable(AJAXRequestData request, ServerSession session) throws OXException {
        String treeId = request.getParameter(PARAM_TREE);
        if (null == treeId) {
            treeId = getDefaultTreeIdentifier();
        }

        JSONArray jsonArray = (JSONArray) request.requireData();
        int hash = jsonArray.toString().hashCode();

        try {
            JSONObject jKeyDesc = new JSONObject(4);
            jKeyDesc.put("module", "folder");
            jKeyDesc.put("action", ACTION);
            jKeyDesc.put(PARAM_TREE, treeId);
            jKeyDesc.put("body", hash);

            return EnqueuableAJAXActionService.resultFor(true, new JobKey(session.getUserId(), session.getContextId(), jKeyDesc.toString()), this);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

}
