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

package com.openexchange.contact.provider.folder;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.ContactsAccess;
import com.openexchange.contact.provider.extensions.SyncAware;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.results.SequenceResult;
import com.openexchange.groupware.results.UpdatesResult;

/**
 * {@link FolderSyncAware}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public interface FolderSyncAware extends ContactsAccess, SyncAware {

    /**
     * Gets a list of modified contacts in the specified folder
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     *
     * @param folderId the folder identifier
     * @param from Specifies the lower inclusive limit of the queried range, i.e. only
     *            contacts modified on or after this date should be returned.
     * @return The list of modified contacts
     */
    List<Contact> getModifiedContacts(String folderId, Date from) throws OXException;

    /**
     * Gets a list of deleted contacts in the specified folder.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     * </ul>
     *
     * @param folderId the folder identifier
     * @param from Specifies the lower inclusive limit of the queried range, i.e. only
     *            contacts deleted on or after this date should be returned.
     * @return The list of deleted contacts
     */
    List<Contact> getDeletedContacts(String folderId, Date from) throws OXException;

    /**
     * Gets lists of new and updated as well as deleted contacts since a specific timestamp in certain folders.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_IGNORE} ("changed", "deleted", "count")</li>
     * </ul>
     * 
     * @param folderIds The identifiers of the folder to get the updates from
     * @param since The timestamp since when the updates should be returned
     * @return The updates results, mapped to the corresponding folder ids
     */
    Map<String, UpdatesResult<Contact>> getUpdatedContacts(List<String> folderIds, Date since) throws OXException;

    /**
     * Gets the sequence numbers of certain contacts folders, which is the highest timestamp of all contained items. Distinct object access
     * permissions (e.g. <i>read own</i>) are not considered. Additionally, the actual item count in each of the folders is returned,
     * aiding proper detection of removed items during incremental synchronizations.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_IGNORE} ("count")</li>
     * </ul>
     * 
     * @param folderIds The identifiers of the folders to get the sequence number for
     * @return The sequence number results, mapped to the corresponding folder ids
     */
    Map<String, SequenceResult> getSequenceNumbers(List<String> folderIds) throws OXException;

}
