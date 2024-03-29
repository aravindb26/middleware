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

package com.openexchange.folderstorage;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.openexchange.groupware.EntityInfo;

/**
 * {@link Folder} - A folder.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface Folder extends Serializable, Cloneable {

    /**
     * Gets the entity which created this folder.
     *
     * @return The entity which created this folder or <code>-1</code>
     */
    int getCreatedBy();

    /**
     * Sets the entity which created this folder.
     *
     * @param createdBy The entity which created this folder
     */
    void setCreatedBy(int createdBy);

    /**
     * Gets the entity which lastly modified this folder.
     *
     * @return The entity which lastly modified this folder or <code>-1</code>
     */
    int getModifiedBy();

    /**
     * Sets the entity which lastly modified this folder.
     *
     * @param modifiedBy The entity which lastly modified this folder
     */
    void setModifiedBy(int modifiedBy);

    /**
     * Gets the creation date.
     *
     * @return The creation date
     */
    Date getCreationDate();

    /**
     * Sets the creation date.
     *
     * @param creationDate The creation date
     */
    void setCreationDate(Date creationDate);

    /**
     * Gets the last-modified date.
     *
     * @return The last-modified date
     */
    Date getLastModified();

    /**
     * Sets the last-modified date.
     *
     * @param lastModified The last-modified date
     */
    void setLastModified(Date lastModified);

    /**
     * Indicates if this folder is cacheable.
     *
     * @return <code>true</code> if this folder is cacheable; otherwise <code>false</code>
     * @see #isGlobalID()
     */
    boolean isCacheable();

    /**
     * Indicates if this folder is virtual.
     *
     * @return <code>true</code> if this folder is virtual; otherwise <code>false</code>
     */
    boolean isVirtual();

    /**
     * Indicates whether this folder is globally unique, meaning not bound to a certain user.
     *
     * @return <code>true</code> if this folder is globally unique; otherwise <code>false</code>
     */
    boolean isGlobalID();

    /**
     * Gets the tree ID.
     *
     * @return The tree ID or <code>null</code> if not available
     */
    String getTreeID();

    /**
     * Sets the tree ID.
     *
     * @param id The tree ID to set
     */
    void setTreeID(String id);

    /**
     * Gets the ID.
     *
     * @return The ID or <code>null</code> if not available
     */
    String getID();

    /**
     * Sets the ID.
     *
     * @param id The ID to set
     */
    void setID(String id);

    /**
     * Gets the new ID.
     *
     * @return The new ID or <code>null</code> if not available
     */
    String getNewID();

    /**
     * Sets the new ID.
     *
     * @param newId The new ID to set
     */
    void setNewID(String newId);

    /**
     * Gets the parent ID.
     *
     * @return The parent ID or <code>null</code> if not available
     */
    String getParentID();

    /**
     * Sets the parent ID.
     *
     * @param parentId The parent ID to set
     */
    void setParentID(String parentId);

    /**
     * Gets the ID of the account the folder belongs to.
     *
     * @return The account ID. Will be <code>null</code> if the folder does not belong to any account
     * (i.e. if its module doesn't support multiple accounts), is a virtual folder or an account-
     * agnostic system folder.
     */
    String getAccountID();

    /**
     * Sets the ID of the account the folder belongs to.
     *
     * @param accountId The ID to set
     */
    void setAccountID(String accountId);

    /**
     * Gets the subfolder IDs.
     * <p>
     * <b>Note</b>: This method is allowed to return <code>null</code>. A returned <code>null</code> value indicates that:
     * <ul>
     * <li>this folder surely has subfolders</li>
     * <li>this folder's subfolders may be located in more than one folder storages</li>
     * </ul>
     * <p>
     * To obtain full list of subfolders use the {@link FolderStorage#getSubfolders(String)} method on each appropriate folder storage.
     *
     * @return The subfolder IDs or <code>null</code>
     */
    String[] getSubfolderIDs();

    /**
     * Sets the subfolder IDs.
     * <p>
     * <b>Note</b>: This method allows to pass a <code>null</code> value. A passed <code>null</code> value indicates that:
     * <ul>
     * <li>this folder surely has subfolders</li>
     * <li>this folder's subfolders may be located in more than one folder storages</li>
     * </ul>
     *
     * @param subfolderIds The subfolder IDs to set or <code>null</code>
     */
    void setSubfolderIDs(String[] subfolderIds);

    /**
     * Gets the name.
     *
     * @return The name or <code>null</code> if not available
     */
    String getName();

    /**
     * Sets the name.
     *
     * @param name The name to set
     * @see #getLocalizedName(Locale)
     */
    void setName(String name);

    /**
     * Gets the locale-sensitive name.
     *
     * @param locale The locale
     * @return The locale-sensitive name or <code>null</code> if not available
     */
    String getLocalizedName(Locale locale);

    /**
     * Gets the permissions.
     *
     * @return The permissions or <code>null</code> if not available
     */
    Permission[] getPermissions();

    /**
     * Sets the permissions.
     *
     * @param permissions The permissions to set
     */
    void setPermissions(Permission[] permissions);

    /**
     * Gets the content type.
     *
     * @return The content type or <code>null</code> if not available
     */
    ContentType getContentType();

    /**
     * Sets the content type.
     *
     * @param contentType The content type to set
     */
    void setContentType(ContentType contentType);

    /**
     * Gets the type.
     *
     * @return The type or <code>null</code> if not available
     */
    Type getType();

    /**
     * Sets the type.
     *
     * @param type The type to set
     */
    void setType(Type type);

    /**
     * Indicates if this folder is subscribed.
     *
     * @return <code>true</code> if this folder is subscribed; otherwise <code>false</code>
     */
    boolean isSubscribed();

    /**
     * Sets if this folder is subscribed.
     *
     * @param subscribed <code>true</code> if this folder is subscribed; otherwise <code>false</code>
     */
    void setSubscribed(boolean subscribed);

    /**
     * Indicates if this folder has subscribed subfolders.
     *
     * @return <code>true</code> if this folder has subscribed subfolders; otherwise <code>false</code>
     */
    boolean hasSubscribedSubfolders();

    /**
     * Sets whether this folder has subscribed subfolders.
     *
     * @param subscribedSubfolders <code>true</code> if this folder has subscribed subfolders; otherwise <code>false</code>
     */
    void setSubscribedSubfolders(boolean subscribedSubfolders);

    /**
     * Gets the summary.
     *
     * @return The summary
     */
    String getSummary();

    /**
     * Sets the summary.
     *
     * @param summary The summary
     */
    void setSummary(String summary);

    /**
     * Gets the total number of elements held by this folder.
     *
     * @return The total number of elements or <code>-1</code> if not supported
     */
    int getTotal();

    /**
     * Sets the total number of elements held by this folder.
     *
     * @param total The total number of elements
     */
    void setTotal(int total);

    /**
     * Gets the number of elements held by this folder which are marked as new.
     *
     * @return The number of elements held by this folder which are marked as new or <code>-1</code> if not supported
     */
    int getNew();

    /**
     * Sets the number of elements held by this folder which are marked as new.
     *
     * @param nu The number of elements held by this folder which are marked as new
     */
    void setNew(int nu);

    /**
     * Gets the number of elements held by this folder which are marked as unread.
     *
     * @return The number of elements held by this folder which are marked as unread or <code>-1</code> if not supported
     */
    int getUnread();

    /**
     * Sets the number of elements held by this folder which are marked as unread.
     *
     * @param unread The number of elements held by this folder which are marked as unread
     */
    void setUnread(int unread);

    /**
     * Gets the number of elements held by this folder which are marked as deleted.
     *
     * @return The number of elements held by this folder which are marked as deleted or <code>-1</code> if not supported
     */
    int getDeleted();

    /**
     * Sets the number of elements held by this folder which are marked as deleted.
     *
     * @param deleted The number of elements held by this folder which are marked as deleted
     */
    void setDeleted(int deleted);

    /**
     * Indicates if this folder is a default folder.
     *
     * @return <code>true</code> if this folder is a default folder; otherwise <code>false</code>
     */
    boolean isDefault();

    /**
     * Sets if this folder is a default folder.
     *
     * @param deefault <code>true</code> if this folder is a default folder; otherwise <code>false</code>
     */
    void setDefault(boolean deefault);

    /**
     * Gets the default type.
     *
     * @return The default type or <code>0</code> if none available
     */
    int getDefaultType();

    /**
     * Sets the default type.
     *
     * @param defaultType The default type
     */
    void setDefaultType(int defaultType);

    /**
     * Gets the capabilities.
     *
     * @return The capabilities or <code>-1</code> if not supported
     */
    int getCapabilities();

    /**
     * Sets the capabilities.
     *
     * @param capabilities The capabilities
     */
    void setCapabilities(int capabilities);

    /**
     * Gets the bits
     *
     * @return The bits
     */
    int getBits();

    /**
     * Sets the bits
     *
     * @param bits The bits to set
     */
    void setBits(final int bits);

    /**
     * Sets dynamic metadata
     */
    void setMeta(Map<String, Object> meta);

    /**
     * Gets dynamic metadata
     */
    Map<String, Object> getMeta();

    /**
     * Gets an optional set of supported capabilities.
     *
     * @return The supported capabilities or <code>null</code>
     */
    Set<String> getSupportedCapabilities();

    /**
     * Sets the set of supported capabilities.
     *
     * @param capabilitites The capabilities to set
     */
    void setSupportedCapabilities(Set<String> capabilities);

    /**
     * Gets the folder's origin path
     */
    FolderPath getOriginPath();

    /**
     * Sets the folder's origin path
     *
     * @param originPath The origin path to set
     */
    void setOriginPath(FolderPath originPath);

    EntityInfo getCreatedFrom();

    void setCreatedFrom(EntityInfo createdFrom);

    EntityInfo getModifiedFrom();

    void setModifiedFrom(EntityInfo modifiedFrom);

    /**
     * Creates and returns a copy of this object.
     *
     * @return A clone of this instance.
     */
    Object clone();

    /**
     * Whether the folder is used for sync or not. Defaults to {@link UsedForSync#DEFAULT}
     *
     * @return <code>true</code> if the folder is used for sync, <code>false</code> otherwise
     */
    default UsedForSync getUsedForSync() {
        return UsedForSync.DEFAULT;
    }

    /**
     * Sets the used for sync value. Defaults to no-op
     *
     * @param usedForSync The {@link UsedForSync} value
     */
    default void setUsedForSync(UsedForSync usedForSync) {
        // empty
    }

    /**
     * Gets the localized name if it is available, otherwise the name is returned as fallback.
     *
     * @param locale The locale
     * @return The locale-sensitive name or the name if not available
     */
    default String getName(Locale locale) {
        String localizedName = getLocalizedName(locale);
        return localizedName != null ? localizedName : getName();
    }

}
