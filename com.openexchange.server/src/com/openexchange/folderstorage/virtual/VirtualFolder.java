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

package com.openexchange.folderstorage.virtual;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderField;
import com.openexchange.folderstorage.FolderPath;
import com.openexchange.folderstorage.FolderProperty;
import com.openexchange.folderstorage.ParameterizedFolder;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.Type;
import com.openexchange.groupware.EntityInfo;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.i18n.tools.StringHelper;

/**
 * {@link VirtualFolder} - A virtual folder backed by a real folder.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class VirtualFolder implements ParameterizedFolder {

    private static final long serialVersionUID = -7353105231203614064L;

    private Folder realFolder;

    private Date lastModified;

    private int modifiedBy;

    private String treeId;

    private String accountId;

    private String name;

    private String parent;

    private Permission[] permissions;

    private String[] subfolders;
    private boolean b_subfolders;

    private Boolean subscribed;

    private Boolean subscribedSubfolders;

    private String newId;

    private Map<FolderField, FolderProperty> properties;

    /**
     * Initializes a {@link VirtualFolder} with specified real folder.
     *
     * @param source The real folder which is mapped by this virtual folder
     */
    public VirtualFolder(final Folder source) {
        super();
        realFolder = source;
        modifiedBy = -1;
        properties = new HashMap<FolderField, FolderProperty>(4);
    }

    @Override
    public String toString() {
        return new StringBuilder(32).append("{ name=").append(getName()).append(", id=").append(getID()).append('}').toString();
    }

    @Override
    public Object clone() {
        try {
            final VirtualFolder clone = (VirtualFolder) super.clone();
            clone.realFolder = (Folder) (realFolder == null ? null : realFolder.clone());
            clone.lastModified = cloneDate(lastModified);
            if (permissions != null) {
                final Permission[] thisPermissions = permissions;
                final Permission[] clonePermissions = new Permission[thisPermissions.length];
                for (int i = 0; i < thisPermissions.length; i++) {
                    clonePermissions[i] = (Permission) thisPermissions[i].clone();
                }
                clone.permissions = clonePermissions;
            }
            if (subfolders != null) {
                final String[] thisSub = subfolders;
                final String[] cloneSub = new String[thisSub.length];
                for (int i = 0; i < cloneSub.length; i++) {
                    cloneSub[i] = thisSub[i];
                }
                clone.subfolders = cloneSub;
            }
            if (properties != null) {
                final Map<FolderField, FolderProperty> cloneProps = new HashMap<FolderField, FolderProperty>(properties);
                clone.properties = cloneProps;
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    @Override
    public String getNewID() {
        return newId;
    }

    @Override
    public void setNewID(final String newId) {
        this.newId = newId;
    }

    @Override
    public String getAccountID() {
        if (accountId == null) {
            if (realFolder == null) {
                return "unknown";
            } else {
                return realFolder.getAccountID();
            }
        }

        return accountId;
    }

    @Override
    public void setAccountID(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public int getCreatedBy() {
        return null == realFolder ? -1 : realFolder.getCreatedBy();
    }

    @Override
    public Date getCreationDate() {
        return null == realFolder ? null : realFolder.getCreationDate();
    }

    @Override
    public Date getLastModified() {
        return lastModified == null ? (null == realFolder ? null : realFolder.getLastModified()) : cloneDate(lastModified);
    }

    @Override
    public int getModifiedBy() {
        return -1 == modifiedBy ? (null == realFolder ? -1 : realFolder.getModifiedBy()) : modifiedBy;
    }

    @Override
    public void setCreatedBy(final int createdBy) {
        // Nothing to do
    }

    @Override
    public void setCreationDate(final Date creationDate) {
        // Nothing to do
    }

    @Override
    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified == null ? null : new Date(lastModified.getTime());
    }

    @Override
    public void setModifiedBy(final int modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public ContentType getContentType() {
        return realFolder.getContentType();
    }

    @Override
    public String getID() {
        return realFolder.getID();
    }

    @Override
    public String getLocalizedName(final Locale locale) {
        final Locale loc = null == locale ? LocaleTools.DEFAULT_LOCALE : locale;
        if (null == name) {
            return realFolder.getLocalizedName(loc);
        }
        return StringHelper.valueOf(loc).getString(name);
    }

    @Override
    public String getName() {
        return null == name ? realFolder.getName() : name;
    }

    @Override
    public String getParentID() {
        return null == parent ? realFolder.getParentID() : parent;
    }

    /**
     * Gets either real folder's permissions or virtual folder's individual permissions (if set)
     *
     * <pre>
     * return permissions == null ? realFolder.getPermissions() : permissions;
     * </pre>
     *
     * @return The permissions for this virtual folder
     */
    @Override
    public Permission[] getPermissions() {
        /*
         * If no permissions applied return real folder's permissions
         */
        return null == permissions ? realFolder.getPermissions() : permissions;
    }

    @Override
    public String[] getSubfolderIDs() {
        return b_subfolders ? subfolders : realFolder.getSubfolderIDs();
    }

    @Override
    public String getTreeID() {
        return treeId;
    }

    @Override
    public Type getType() {
        return realFolder.getType();
    }

    @Override
    public void setContentType(final ContentType contentType) {
        // Nothing to do
    }

    @Override
    public void setID(final String id) {
        // Nothing to do
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public void setParentID(final String parentId) {
        parent = parentId;
    }

    @Override
    public void setPermissions(final Permission[] permissions) {
        this.permissions = permissions;
    }

    @Override
    public void setSubfolderIDs(final String[] subfolderIds) {
        subfolders = subfolderIds;
        b_subfolders = true;
    }

    @Override
    public void setTreeID(final String id) {
        treeId = id;
    }

    @Override
    public void setType(final Type type) {
        // Nothing to do
    }

    @Override
    public boolean isSubscribed() {
        return null == subscribed ? realFolder.isSubscribed() : subscribed.booleanValue();
    }

    @Override
    public void setSubscribed(final boolean subscribed) {
        this.subscribed = Boolean.valueOf(subscribed);
    }

    @Override
    public boolean hasSubscribedSubfolders() {
        return null == subscribedSubfolders ? realFolder.hasSubscribedSubfolders() : subscribedSubfolders.booleanValue();
        /*-
         *
        if (null == subscribedSubfolders) {
            return null == subfolders || subfolders.length > 0;
        }
        return subscribedSubfolders.booleanValue();
        */
    }

    @Override
    public void setSubscribedSubfolders(final boolean subscribedSubfolders) {
        this.subscribedSubfolders = Boolean.valueOf(subscribedSubfolders);
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public boolean isGlobalID() {
        return realFolder.isGlobalID();
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    public int getCapabilities() {
        return realFolder.getCapabilities();
    }

    @Override
    public int getDeleted() {
        return realFolder.getDeleted();
    }

    @Override
    public int getNew() {
        return realFolder.getNew();
    }

    @Override
    public String getSummary() {
        return realFolder.getSummary();
    }

    @Override
    public int getTotal() {
        return realFolder.getTotal();
    }

    @Override
    public int getUnread() {
        return realFolder.getUnread();
    }

    @Override
    public boolean isDefault() {
        return realFolder.isDefault();
    }

    @Override
    public void setCapabilities(final int capabilities) {
        // Nothing to do
    }

    @Override
    public void setDefault(final boolean deefault) {
        // Nothing to do
    }

    @Override
    public void setDeleted(final int deleted) {
        // Nothing to do
    }

    @Override
    public void setNew(final int nu) {
        // Nothing to do
    }

    @Override
    public void setSummary(final String summary) {
        // Nothing to do
    }

    @Override
    public void setTotal(final int total) {
        // Nothing to do
    }

    @Override
    public void setUnread(final int unread) {
        // Nothing to do
    }

    @Override
    public int getDefaultType() {
        return realFolder.getDefaultType();
    }

    @Override
    public void setDefaultType(final int defaultType) {
        // Nothing to do
    }

    @Override
    public int getBits() {
        return realFolder.getBits();
    }

    @Override
    public void setBits(final int bits) {
        // Nothing to do
    }

    private static Date cloneDate(final Date d) {
        if (null == d) {
            return null;
        }
        return new Date(d.getTime());
    }

    @Override
    public void setProperty(final FolderField name, final Object value) {
        if (null == value) {
            properties.remove(name);
        } else {
            properties.put(name, new FolderProperty(name.getName(), value));
        }
    }

    @Override
    public Map<FolderField, FolderProperty> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public void setMeta(Map<String, Object> meta) {
        realFolder.setMeta(meta);
    }

    @Override
    public Map<String, Object> getMeta() {
        return realFolder.getMeta();
    }

    @Override
    public Set<String> getSupportedCapabilities() {
        return realFolder.getSupportedCapabilities();
    }

    @Override
    public void setSupportedCapabilities(Set<String> capabilities) {
        realFolder.setSupportedCapabilities(capabilities);
    }

    @Override
    public FolderPath getOriginPath() {
        return realFolder.getOriginPath();
    }

    @Override
    public void setOriginPath(FolderPath originPath) {
        realFolder.setOriginPath(originPath);
    }

    @Override
    public EntityInfo getCreatedFrom() {
        return realFolder.getCreatedFrom();
    }

    @Override
    public void setCreatedFrom(EntityInfo createdFrom) {
        realFolder.setCreatedFrom(createdFrom);
    }

    @Override
    public EntityInfo getModifiedFrom() {
        return realFolder.getModifiedFrom();
    }

    @Override
    public void setModifiedFrom(EntityInfo modifiedFrom) {
        realFolder.setModifiedFrom(modifiedFrom);
    }

}
