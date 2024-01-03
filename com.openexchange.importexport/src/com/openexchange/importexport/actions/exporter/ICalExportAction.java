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

package com.openexchange.importexport.actions.exporter;

import static com.openexchange.java.Autoboxing.I;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.DispatcherNotes;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.importexport.Format;
import com.openexchange.importexport.exporters.ICalExporter;
import com.openexchange.importexport.json.ExportRequest;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.RestrictedActionUtil;
import com.openexchange.tools.session.ServerSession;

@DispatcherNotes(defaultFormat = "file")
@RestrictedAction(hasCustomRestrictedAccessCheck = true)
public class ICalExportAction extends AbstractExportAction {

    private static final List<Integer> ACCEPTED_OAUTH_MODULES = ImmutableList.of(I(FolderObject.CALENDAR), I(FolderObject.TASK));

    private final ServiceLookup services;

    /**
     * Initializes a new {@link ICalExportAction}.
     * 
     * @param services The service look-up
     */
    public ICalExportAction(ServiceLookup services) {
        super(new ICalExporter(), Format.ICAL);
        this.services = services;
    }

    @RestrictedAccessCheck
    public boolean accessAllowed(final AJAXRequestData request, final ServerSession session, Scope scope) throws OXException {
        ExportRequest exportRequest = new ExportRequest(request, session);
        Map<String, List<String>> batchIds = exportRequest.getBatchIds();
        if (false == isBatchExport(batchIds)) {
            return RestrictedActionUtil.mayReadWithScope(getContentTypeForUserizedFolder(exportRequest.getFolder(), session), scope);
        }
        return RestrictedActionUtil.mayReadWithScope(collectContentTypes(batchIds, session), scope);
    }

    /**
     * Collects the content types from the specified batch id map
     * 
     * @param batchIds the map
     * @param session The session
     * @return The content types
     * @throws OXException if an error is occurred
     */
    private Set<ContentType> collectContentTypes(Map<String, List<String>> batchIds, ServerSession session) throws OXException {
        short items = 0;
        Set<ContentType> contentTypes = new HashSet<>(4);
        for (String folderId : batchIds.keySet()) {
            if (false == contentTypes.add(getContentTypeForUserizedFolder(folderId, session))) {
                continue;
            }
            items++;
            if (items == ACCEPTED_OAUTH_MODULES.size()) {
                break;
            }
        }
        return contentTypes;
    }

    /**
     * Determines whether the specified map is empty or null
     * which implies that this is NOT a batch export
     *
     * @param batchIds The batch ids map
     * @return <code>true</code> if it is a batch export; <code>false</code> otherwise
     */
    private boolean isBatchExport(Map<String, List<String>> batchIds) {
        return null != batchIds && false == batchIds.isEmpty();
    }

    /**
     * 
     * Gets the content type for a given folder id of a userized folder.
     *
     * @param folderId The folder id.
     * @param session The server session.
     * @return The content type.
     * @throws OXException If folder cannot be found.
     */
    protected ContentType getContentTypeForUserizedFolder(String folderId, final ServerSession session) throws OXException {
        FolderService folderService = services.getService(FolderService.class);
        UserizedFolder userizedFolder = folderService.getFolder(FolderStorage.REAL_TREE_ID, folderId, session, null);
        return userizedFolder.getContentType();
    }
}
