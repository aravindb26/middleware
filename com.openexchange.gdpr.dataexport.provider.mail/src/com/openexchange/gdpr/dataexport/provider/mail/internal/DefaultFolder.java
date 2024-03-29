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

package com.openexchange.gdpr.dataexport.provider.mail.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.collect.ImmutableSet;
import com.openexchange.exception.OXException;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailFolderStorageInfoSupport;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailFolderInfo;

/**
 * {@link DefaultFolder}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class DefaultFolder implements Folder {

    static AtomicReference<Set<String>> FULL_NAMES_TO_IGNORE = new AtomicReference<>(Collections.emptySet());

    /**
     * Sets the full names to ignore.
     *
     * @param fullNamesToIgnore The set containing the full names to ignore
     */
    public static void setFullNamesToIgnore(Set<String> fullNamesToIgnore) {
        if (fullNamesToIgnore == null) {
            return;
        }

        FULL_NAMES_TO_IGNORE.set(ImmutableSet.copyOf(fullNamesToIgnore));
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final FolderAccess folderAccess;
    private final String fullname;
    private final boolean trash;
    private final String name;
    private final boolean subscribed;
    private final boolean holdsMessages;
    private final boolean holdsFolders;
    private final boolean rootFolder;
    private final boolean shared;
    private final boolean publik;

    /**
     * Initializes a new {@link DefaultFolder}.
     *
     * @param folderInfo The folder info
     * @param mailOperationExecutor The mail access reference
     */
    public DefaultFolder(MailFolderInfo folderInfo, MailOperationExecutor mailOperationExecutor) {
        super();
        String fullname = folderInfo.getFullname();
        folderAccess = new MailAccessReferenceAccess(fullname, mailOperationExecutor);
        this.fullname = fullname;
        trash = folderInfo.isTrash();
        name = folderInfo.getName();
        subscribed = folderInfo.isSubscribed();
        holdsFolders = folderInfo.isHoldsFolders();
        holdsMessages = folderInfo.isHoldsMessages();
        rootFolder = folderInfo.isRootFolder();
        shared = folderInfo.isShared();
        publik = folderInfo.isPublic();
    }

    /**
     * Initializes a new {@link DefaultFolder}.
     *
     * @param folder The folder
     * @param mailOperationExecutor The mail access reference
     */
    public DefaultFolder(MailFolder folder, MailOperationExecutor mailOperationExecutor) {
        super();
        String fullname = folder.getFullname();
        folderAccess = new MailAccessReferenceAccess(fullname, mailOperationExecutor);
        this.fullname = fullname;
        trash = folder.isTrash();
        name = folder.getName();
        subscribed = folder.isSubscribed();
        holdsFolders = folder.isHoldsFolders();
        holdsMessages = folder.isHoldsMessages();
        rootFolder = folder.isRootFolder();
        shared = folder.isShared();
        publik = folder.isPublic();
    }

    @Override
    public String getFullname() {
        return fullname;
    }

    @Override
    public boolean isTrash() {
        return trash;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isSubscribed() {
        return subscribed;
    }

    @Override
    public boolean isHoldsMessages() {
        return holdsMessages;
    }

    @Override
    public boolean isHoldsFolders() {
        return holdsFolders;
    }

    @Override
    public boolean isRootFolder() {
        return rootFolder;
    }

    @Override
    public boolean isShared() {
        return shared;
    }

    @Override
    public boolean isPublic() {
        return publik;
    }

    @Override
    public List<Folder> getChildren(boolean subscribedOnly) throws OXException {
        return folderAccess.getChildren(subscribedOnly);
    }

    // ------------------------------------------------------ Helpers ----------------------------------------------------------------------

    private static class MailAccessReferenceAccess implements FolderAccess {

        private final MailOperationExecutor mailOperationExecutor;
        private final String fullname;

        MailAccessReferenceAccess(String fullname, MailOperationExecutor mailOperationExecutor) {
            super();
            this.mailOperationExecutor = mailOperationExecutor;
            this.fullname = fullname;
        }

        @Override
        public List<Folder> getChildren(boolean subscribedOnly) throws OXException {
            return mailOperationExecutor.executeWithRetryOnConnectionLoss(mailAccess -> {
                IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
                IMailFolderStorageInfoSupport infoSupport = folderStorage.supports(IMailFolderStorageInfoSupport.class);
                if (null != infoSupport && infoSupport.isInfoSupported()) {
                    return getChildrenByInfoSupport(subscribedOnly, infoSupport);
                }
                return getChildrenByFolderStorage(subscribedOnly, folderStorage);
            });
        }

        private List<Folder> getChildrenByInfoSupport(boolean subscribedOnly, IMailFolderStorageInfoSupport infoSupport) throws OXException {
            List<MailFolderInfo> children = infoSupport.getFolderInfos(fullname, subscribedOnly);
            if (children == null) {
                return Collections.emptyList();
            }

            int size = children.size();
            if (size <= 0) {
                return Collections.emptyList();
            }

            Set<String> fullNamesToIgnore = FULL_NAMES_TO_IGNORE.get();
            boolean ignoreFullNamesToIgnore = fullNamesToIgnore.isEmpty();

            List<Folder> retval = new ArrayList<>(size);
            for (MailFolderInfo mailFolderInfo : children) {
                if (ignoreFullNamesToIgnore || !fullNamesToIgnore.contains(mailFolderInfo.getFullname())) {
                    retval.add(new DefaultFolder(mailFolderInfo, mailOperationExecutor));
                }
            }
            return retval;
        }

        private List<Folder> getChildrenByFolderStorage(boolean subscribedOnly, IMailFolderStorage folderStorage) throws OXException {
            MailFolder[] children = folderStorage.getSubfolders(fullname, !subscribedOnly);
            if (children == null || children.length <= 0) {
                return Collections.emptyList();
            }

            Set<String> fullNamesToIgnore = FULL_NAMES_TO_IGNORE.get();
            boolean ignoreFullNamesToIgnore = fullNamesToIgnore.isEmpty();

            List<Folder> retval = new ArrayList<>(children.length);
            for (MailFolder mailFolder : children) {
                if (ignoreFullNamesToIgnore || !fullNamesToIgnore.contains(mailFolder.getFullname())) {
                    retval.add(new DefaultFolder(mailFolder, mailOperationExecutor));
                }
            }
            return retval;
        }
    }

}
