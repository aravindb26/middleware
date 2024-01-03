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

package com.openexchange.file.storage.json.actions.files;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;


/**
 * {@link RevertAction}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class RevertAction extends AbstractWriteAction {

    @Override
    public AJAXRequestResult handle(InfostoreRequest request) throws OXException {
        request.require(Param.ID);

        IDBasedFileAccess fileAccess = request.getFileAccess();

        TimedResult<File> versions = fileAccess.getVersions(request.getId());
        List<String> versionIdentifiers = new ArrayList<>(10);

        final SearchIterator<File> results = versions.results();
        try {
            while (results.hasNext()) {
                String version = results.next().getVersion();
                if (version != null && !version.equals("0")) {
                    versionIdentifiers.add(version);
                }
            }
            String[] toDelete = new String[versionIdentifiers.size()];
            for(int i = 0; i < toDelete.length; i++) {
                toDelete[i] = versionIdentifiers.get(i);
            }
            fileAccess.removeVersion(request.getId(), toDelete);
            File fileMetadata = fileAccess.getFileMetadata(request.getId(), FileStorageFileAccess.CURRENT_VERSION);
            return success(fileMetadata.getSequenceNumber());
        } finally {
            SearchIterators.close(results);
        }
    }

}
