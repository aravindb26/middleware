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

import static com.openexchange.test.common.test.pool.UserModuleAccessFields.ACTIVESYNC;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.CALENDAR;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.COLLECTEMAILADDRESSES;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.CONTACTS;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.DELEGATETASK;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.EDITGROUP;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.EDITPASSWORD;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.EDITPUBLICFOLDERS;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.EDITRESOURCE;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.GLOBALADDRESSBOOKDISABLED;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.ICAL;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.INFOSTORE;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.MULTIPLEMAILACCOUNTS;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.OLOX20;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.PUBLICATION;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.PUBLICFOLDEREDITABLE;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.READCREATESHAREDFOLDERS;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.SUBSCRIPTION;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.SYNCML;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.TASKS;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.USM;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.VCARD;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.WEBDAV;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.WEBDAVXML;
import static com.openexchange.test.common.test.pool.UserModuleAccessFields.WEBMAIL;
import java.util.EnumMap;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.DefaultMapper;
import com.openexchange.groupware.tools.mappings.DefaultMapping;
import com.openexchange.groupware.tools.mappings.Mapping;

/**
 * {@link UserModuleAccessMapper}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.0.0
 */
@SuppressWarnings("deprecation")
public class UserModuleAccessMapper extends DefaultMapper<UserModuleAccess, UserModuleAccessFields> {

    /**
     * Initializes a new {@link UserModuleAccessMapper}.
     *
     */
    private UserModuleAccessMapper() {
        super();
    }

    private final static UserModuleAccessMapper INSTANCE = new UserModuleAccessMapper();

    /**
     * Get this mapper
     *
     * @return The mapper
     */
    public static UserModuleAccessMapper getInstance() {
        return INSTANCE;
    }

    @Override
    public UserModuleAccess newInstance() {
        return new UserModuleAccess();
    }

    @Override
    public UserModuleAccessFields[] newArray(int size) {
        return new UserModuleAccessFields[size];
    }

    @Override
    public EnumMap<UserModuleAccessFields, Mapping<Boolean, UserModuleAccess>> getMappings() {
        return MAP;
    }

    private static final EnumMap<UserModuleAccessFields, Mapping<Boolean, UserModuleAccess>> MAP = new EnumMap<>(UserModuleAccessFields.class);

    static {
        MAP.put(CALENDAR, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getCalendar();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setCalendar(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getCalendar();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setCalendar(null);
            }
        });
        MAP.put(CONTACTS, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getContacts();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setContacts(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getContacts();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setContacts(null);
            }
        });
        MAP.put(DELEGATETASK, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getDelegateTask();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setDelegateTask(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getDelegateTask();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setDelegateTask(null);
            }
        });
        MAP.put(EDITPUBLICFOLDERS, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getEditPublicFolders();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setEditPublicFolders(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getEditPublicFolders();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setEditPublicFolders(null);
            }
        });
        MAP.put(ICAL, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getIcal();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setIcal(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getIcal();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setIcal(null);
            }
        });
        MAP.put(INFOSTORE, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getInfostore();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setInfostore(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getInfostore();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setInfostore(null);
            }
        });
        MAP.put(READCREATESHAREDFOLDERS, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getReadCreateSharedFolders();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setReadCreateSharedFolders(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getReadCreateSharedFolders();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setReadCreateSharedFolders(null);
            }
        });
        MAP.put(SYNCML, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getSyncml();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setSyncml(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getSyncml();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setSyncml(null);
            }
        });
        MAP.put(TASKS, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getTasks();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setTasks(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getTasks();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setTasks(null);
            }
        });
        MAP.put(VCARD, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getVcard();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setVcard(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getVcard();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setVcard(null);
            }
        });
        MAP.put(WEBDAV, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getWebdav();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setWebdav(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getWebdav();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setWebdav(null);
            }
        });
        MAP.put(WEBDAVXML, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getWebdavXml();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setWebdavXml(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getWebdavXml();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setWebdavXml(null);
            }
        });
        MAP.put(WEBMAIL, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getWebmail();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setWebmail(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getWebmail();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setWebmail(null);
            }
        });
        MAP.put(EDITGROUP, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getEditGroup();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setEditGroup(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getEditGroup();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setEditGroup(null);
            }
        });
        MAP.put(EDITRESOURCE, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getEditResource();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setEditResource(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getEditResource();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setEditResource(null);
            }
        });
        MAP.put(EDITPASSWORD, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.getEditPassword();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setEditPassword(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.getEditPassword();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setEditPassword(null);
            }
        });
        MAP.put(COLLECTEMAILADDRESSES, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isCollectEmailAddresses();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setCollectEmailAddresses(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isCollectEmailAddresses();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setCollectEmailAddresses(null);
            }
        });
        MAP.put(MULTIPLEMAILACCOUNTS, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isMultipleMailAccounts();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setMultipleMailAccounts(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isMultipleMailAccounts();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setMultipleMailAccounts(null);
            }
        });
        MAP.put(SUBSCRIPTION, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isSubscription();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setSubscription(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isSubscription();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setSubscription(null);
            }
        });
        MAP.put(PUBLICATION, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isPublication();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setPublication(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isPublication();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setPublication(null);
            }
        });
        MAP.put(ACTIVESYNC, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isActiveSync();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setActiveSync(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isActiveSync();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setActiveSync(null);
            }
        });
        MAP.put(USM, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isUSM();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setUSM(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isUSM();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setUSM(null);
            }
        });
        MAP.put(GLOBALADDRESSBOOKDISABLED, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isGlobalAddressBookDisabled();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setGlobalAddressBookDisabled(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isGlobalAddressBookDisabled();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setGlobalAddressBookDisabled(null);
            }
        });
        MAP.put(PUBLICFOLDEREDITABLE, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isPublicFolderEditable();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setPublicFolderEditable(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isPublicFolderEditable();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setPublicFolderEditable(null);
            }
        });
        MAP.put(OLOX20, new DefaultMapping<Boolean, UserModuleAccess>() {

            @Override
            public boolean isSet(UserModuleAccess object) {
                return null != object.isOLOX20();
            }

            @Override
            public void set(UserModuleAccess object, Boolean value) throws OXException {
                object.setOLOX20(value);
            }

            @Override
            public Boolean get(UserModuleAccess object) {
                return object.isOLOX20();
            }

            @Override
            public void remove(UserModuleAccess object) {
                object.setOLOX20(null);
            }
        });
    }

}
