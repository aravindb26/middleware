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

package com.openexchange.ajax.folder.tree;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.groupware.container.FolderObject;

/**
 * {@link RootNode}
 * Represents the root of the oxfolder tree.
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class RootNode extends AbstractFolderNode {

    public RootNode(AJAXClient client) {
        super(null, client);
    }

    @Override
    public List<FolderNode> getChildren() {
        List<FolderNode> folders = new ArrayList<FolderNode>();
        for (FolderObject folder : getManager().listRootFoldersOnServer()) {
            folders.add(load(folder));
        }
        return folders;
    }

    @Override
    public FolderNode getParent() {
        return null;
    }

    @Override
    protected FolderNode node(FolderObject folderObject, AJAXClient client) {
        return new RegularFolderNode(folderObject, client);
    }

    @Override
    public boolean isRoot() {
        return true;
    }

}
