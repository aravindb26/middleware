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

package com.openexchange.importexport.actions.importer;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.importexport.Format;
import com.openexchange.importexport.Importer;
import com.openexchange.importexport.exceptions.ImportExportExceptionCodes;
import com.openexchange.importexport.importers.ICalImporter;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.RestrictedActionUtil;
import com.openexchange.tools.session.ServerSession;

@RestrictedAction(hasCustomRestrictedAccessCheck = true)
public class ICalImportAction extends AbstractImportAction {

    /**
     * Initializes a new {@link ICalImportAction}.
     * 
     * @param services
     */
    public ICalImportAction(ServiceLookup services) {
        super(services);
    }

    private Importer importer;

    @Override
    public Format getFormat() {
        return Format.ICAL;
    }

    @Override
    public Importer getImporter() {
        if (importer == null) {
            importer = new ICalImporter(services);
        }
        return importer;
    }

    @RestrictedAccessCheck
    public boolean accessAllowed(AJAXRequestData request, ServerSession session, Scope scope) throws OXException {
        String folderId = request.getParameter(AJAXServlet.PARAMETER_FOLDERID);
        if (folderId == null) {
            throw ImportExportExceptionCodes.NEED_FOLDER.create();
        }

        FolderService folderService = services.getService(FolderService.class);
        UserizedFolder userizedFolder = folderService.getFolder(FolderStorage.REAL_TREE_ID, folderId, session, null);
        return RestrictedActionUtil.mayWriteWithScope(userizedFolder.getContentType(), scope);
    }

}
