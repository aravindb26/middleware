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

package com.openexchange.test.common.test.pool;

/**
 * {@link UserModuleAccessFields}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.0.0
 */
public enum UserModuleAccessFields {

    /** Calendar access */
    CALENDAR,
    /** Contact access */
    CONTACTS,
    /** Delegation if task */
    DELEGATETASK,
    /** Public folder edition */
    EDITPUBLICFOLDERS,
    /** iCAL access */
    ICAL,
    /** infostore access */
    INFOSTORE,
    /** Read/Create shared folder access */
    READCREATESHAREDFOLDERS,
    /***/
    SYNCML,
    /** Task access */
    TASKS,
    /** VCard access */
    VCARD,
    /** WebDAV access */
    WEBDAV,
    /** WebDAC XML access */
    @Deprecated
    WEBDAVXML,
    /** Mail access */
    WEBMAIL,
    /** Edit group permission */
    EDITGROUP,
    /** Edit resource permission */
    EDITRESOURCE,
    /** Edit password permission */
    EDITPASSWORD,
    /** Collect mail permission */
    COLLECTEMAILADDRESSES,
    /** Multiple mail accounts permission */
    MULTIPLEMAILACCOUNTS,
    /** Subscription access */
    SUBSCRIPTION,
    /** Publication access */
    @Deprecated(since = "7.10.2")
    PUBLICATION,
    /** Active sync access */
    ACTIVESYNC,
    /** USM access */
    USM,
    /** If Global addressbook is accessible */
    GLOBALADDRESSBOOKDISABLED,
    /** Public folder editable permission */
    PUBLICFOLDEREDITABLE,
    /** OLOX 2.0 permission */
    @Deprecated
    OLOX20,

}
