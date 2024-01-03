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

package com.openexchange.carddav.resources;

import static com.openexchange.carddav.Tools.getFoldersHash;
import static com.openexchange.carddav.Tools.getSupportedCapabilities;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.carddav.GroupwareCarddavFactory;
import com.openexchange.dav.DAVProtocol;
import com.openexchange.dav.PreconditionException;
import com.openexchange.dav.reports.SyncStatus;
import com.openexchange.dav.resources.SyncToken;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.java.Reference;
import com.openexchange.java.Strings;
import com.openexchange.login.Interface;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;

/**
 * {@link AggregatedCollection} - CardDAV collection aggregating the contents
 * of all visible folders.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class AggregatedCollection extends CardDAVCollection {

    private static final String AGGREGATED_DISPLAY_NAME = "All Contacts";

    private final List<UserizedFolder> folders;

    /**
     * Initializes a new {@link AggregatedCollection}.
     *
     * @param factory The factory
     * @param url The WebDAV path
     * @param folders The folders aggregated in this collection
     */
    public AggregatedCollection(GroupwareCarddavFactory factory, WebdavPath url, List<UserizedFolder> folders) throws OXException {
        super(factory, url, factory.getState().getDefaultFolder(), factory.getState().getDefaultFolder().getOwnPermission(), getSupportedCapabilities(folders));
        this.folders = folders;
    }

    @Override
    protected List<UserizedFolder> getFolders() {
        return folders;
    }

    @Override
    public String getDisplayName() {
        return AGGREGATED_DISPLAY_NAME;
    }

    @Override
    public String getPushTopic() {
        return "ox:" + Interface.CARDDAV.toString().toLowerCase() + ":contacts" ;
    }

    @Override
    public String getSyncToken() throws WebdavProtocolException {
        /*
         * determine sync token & enrich with additional folders hash
         */
        return addFoldersHash(fetchSyncToken(), getFoldersHash(getFolders())).toString();
    }

    @Override
    protected SyncStatus<WebdavResource> getSyncStatus(SyncToken clientToken) throws OXException {
        /*
         * extract folders hash from sync-token
         */
        Reference<String> clientFoldersHash = new Reference<String>(null);
        SyncToken syncToken = removeFoldersHash(clientToken, clientFoldersHash);
        /*
         * re-check hash of aggregated folders to detect changes
         */
        String foldersHash = getFoldersHash(getFolders());
        if (0L < syncToken.getTimestamp() && false == Objects.equals(clientFoldersHash.getValue(), foldersHash)) {
            String msg = "Mismatching folders hash of aggregated collection [clientToken: " + syncToken + ", currentHash: " + foldersHash + "]";
            OXException cause = OXException.general(msg).setCategory(Category.CATEGORY_CONFLICT);
            throw new PreconditionException(cause, DAVProtocol.DAV_NS.getURI(), "valid-sync-token", getUrl(), HttpServletResponse.SC_FORBIDDEN);
        }
        /*
         * get sync status & enrich next sync token with hash for aggregated folders
         */
        SyncStatus<WebdavResource> syncStatus = super.getSyncStatus(syncToken);
        syncStatus.setToken(addFoldersHash(SyncToken.parse(syncStatus.getToken()), foldersHash).toString());
        return syncStatus;
    }

    private static SyncToken removeFoldersHash(SyncToken syncToken, Reference<String> foldersHash) {
        if (null == syncToken) {
            return syncToken;
        }
        String additional = syncToken.getAdditional();
        if (Strings.isEmpty(additional)) {
            return syncToken;
        }
        int index = additional.indexOf(':');
        if (-1 == index) {
            foldersHash.setValue(additional); // folders hash, only
            return new SyncToken(syncToken.getTimestamp(), null, syncToken.getFlags());
        }
        foldersHash.setValue(additional.substring(0, index)); // folders hash plus something else
        return new SyncToken(syncToken.getTimestamp(), additional.substring(index + 1), syncToken.getFlags());
    }

    private static SyncToken addFoldersHash(SyncToken syncToken, String foldersHash) {
        if (null == syncToken || Strings.isEmpty(foldersHash)) {
            return syncToken;
        }
        String additional = null != syncToken.getAdditional() ? foldersHash + ':' + syncToken.getAdditional() : foldersHash;
        return new SyncToken(syncToken.getTimestamp(), additional, syncToken.getFlags());
    }

}
