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

package com.openexchange.subscribe.folders;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.ajax.customizer.folder.AdditionalFolderField;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.Folder;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.java.Strings;
import com.openexchange.subscribe.AbstractSubscribeService;
import com.openexchange.tools.id.IDMangler;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link HasSubscriptions}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class HasSubscriptions implements AdditionalFolderField {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HasSubscriptions.class);

    private static final Set<String> ID_BLACKLIST = Set.of(
                        /* String.valueOf(FolderObject.SYSTEM_GLOBAL_FOLDER_ID), */ // finally dropped
                        String.valueOf(FolderObject.SYSTEM_LDAP_FOLDER_ID),
                        String.valueOf(FolderObject.SYSTEM_PRIVATE_FOLDER_ID),
                        String.valueOf(FolderObject.SYSTEM_PUBLIC_FOLDER_ID),
                        String.valueOf(FolderObject.SYSTEM_SHARED_FOLDER_ID),
                        String.valueOf(FolderObject.SYSTEM_INFOSTORE_FOLDER_ID),
                        String.valueOf(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID),
                        String.valueOf(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID),
                        String.valueOf(FolderObject.SYSTEM_ROOT_FOLDER_ID));

    @Override
    public int getColumnID() {
        return 3020;
    }

    @Override
    public String getColumnName() {
        return "com.openexchange.subscribe.subscriptionFlag";
    }

    @Override
    public Object getValue(final Folder folder, final ServerSession session) {
        return getValues(Arrays.asList(folder), session).get(0);
    }

    @Override
    public List<Object> getValues(final List<Folder> folders, final ServerSession session) {
        UserPermissionBits permissionBits = session.getUserPermissionBits();
        if (null == permissionBits || !permissionBits.isPublication()) {
            return allFalse(folders.size());
        }

        List<IdAndFolder> idAndFolders = new ArrayList<>(folders.size());
        for (final Folder f : folders) {
            String folderId = f.getID();
            int fuid = getNumericContactFolderIdIfPossible(folderId);
            idAndFolders.add(new IdAndFolder(fuid < 0 ? folderId : Integer.toString(fuid), f));
        }
        return doGetValues(idAndFolders, session);
    }

    private List<Object> doGetValues(List<IdAndFolder> folders, final ServerSession session) {
        int numberOfFolders = folders.size();
        List<String> folderIdsToQuery = null;
        Map<String, Boolean> hasSubscriptions = new HashMap<>(numberOfFolders);
        for (IdAndFolder iaf : folders) {
            String folderId = iaf.folderId;
            ContentType contentType = iaf.folder.getContentType();
            if (null != contentType && FolderObject.MAIL != contentType.getModule() && !ID_BLACKLIST.contains(folderId)) {
                if (folderIdsToQuery == null) {
                    folderIdsToQuery = new ArrayList<>(numberOfFolders);
                }
                folderIdsToQuery.add(folderId);
            } else {
                hasSubscriptions.put(folderId, Boolean.FALSE);
            }
        }
        if (folderIdsToQuery == null) {
            // No folders to query available.. thus all false
            return allFalse(numberOfFolders);
        }

        try {
            hasSubscriptions.putAll(AbstractSubscribeService.STORAGE.get().hasSubscriptions(session.getContext(), folderIdsToQuery));
            final List<Object> retval = new ArrayList<>(numberOfFolders);
            for (IdAndFolder iaf : folders) {
                Boolean subscriptionFlag = hasSubscriptions.get(iaf.folderId);
                if (subscriptionFlag != null) {
                    retval.add(subscriptionFlag);
                } else {
                    LOG.warn("Missing subscription flag for folder {} ({}) in context {}. Assuming \"false\" for that folder.", iaf.folderId, iaf.folder.getID(), I(session.getContextId()));
                    retval.add(Boolean.FALSE);
                }
            }
            return retval;
        } catch (OXException e) {
            LOG.warn("Failed to query subscription flag for folders in context {}:{}{}{}Assuming \"false\" for all folders.", I(session.getContextId()), Strings.getLineSeparator(), folderIdsToQuery, Strings.getLineSeparator(), e);
        }
        return allFalse(numberOfFolders);
    }

    private static List<Object> allFalse(final int size) {
        final List<Object> retval = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            retval.add(Boolean.FALSE);
        }
        return retval;
    }

    @Override
    public Object renderJSON(AJAXRequestData requestData, final Object value) {
        return value;
    }

    private static int getNumericContactFolderIdIfPossible(String folderId) {
        int iFolderId = Strings.parsePositiveInt(folderId);
        if (iFolderId >= 0) {
            return iFolderId;
        }

        List<String> folderIdComponents = IDMangler.unmangle(folderId);
        if (folderIdComponents.size() != 3) {
            // Not of expected number of components
            return -1;
        }
        if (!ContactsAccount.ID_PREFIX.equals(folderIdComponents.get(0))) {
            // Does not start with "con"
            return -1;
        }
        if (ContactsAccount.DEFAULT_ACCOUNT.getAccountId() != Strings.parsePositiveInt(folderIdComponents.get(1))) {
            // Account identifier NaN or not default account
            return -1;
        }
        int fuid = Strings.parsePositiveInt(folderIdComponents.get(2));
        return fuid >= 0 ? fuid : -1;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static class IdAndFolder {

        final String folderId;
        final Folder folder;

        IdAndFolder(String folderId, Folder folder) {
            super();
            this.folderId = folderId;
            this.folder = folder;
        }
    }

}
