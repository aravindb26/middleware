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
package com.openexchange.ajax.folder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.manager.FolderApi;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.config.cascade.ConfigViewScope;
import com.openexchange.folderstorage.GlobalAddressBookProperties;
import com.openexchange.groupware.contact.ContactUtil;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link GlobalAddressBookFolderNameTest}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class GlobalAddressBookFolderNameTest extends AbstractConfigAwareAPIClientSession {

    private static final String GLOBAL_ADDRESS_BOOK_ID = ContactUtil.DEFAULT_ACCOUNT_PREFIX + "/" + FolderObject.SYSTEM_LDAP_FOLDER_ID;
    private Map<String, String> CONFIG = new HashMap<String, String>();
    private FolderManager folderManager;
    private JSlobApi jslobApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderManager = new FolderManager(new FolderApi(getApiClient(), testUser), String.valueOf(EnumAPI.OX_NEW.getTreeId()));
        jslobApi = new JSlobApi(getApiClient());
    }

    @Test
    public void test_default() throws Exception {
        FolderData folderData = folderManager.getFolder(GLOBAL_ADDRESS_BOOK_ID);
        assertEquals(FolderStrings.ALL_USERS_NAME, folderData.getTitle());
    }

    @Test
    public void test_internal_users() throws Exception {
        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.INTERNAL_USERS_ID);
        super.setUpConfiguration();
        FolderData folderData = folderManager.getFolder(GLOBAL_ADDRESS_BOOK_ID);
        assertEquals(FolderStrings.INTERNAL_USERS_NAME, folderData.getTitle());
    }

    @Test
    public void test_all_users() throws Exception {
        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.ALL_USERS_ID);
        super.setUpConfiguration();
        FolderData folderData = folderManager.getFolder(GLOBAL_ADDRESS_BOOK_ID);
        assertEquals(FolderStrings.ALL_USERS_NAME, folderData.getTitle());
    }

    @Test
    public void test_custom_default() throws Exception {        
        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.CUSTOM);
        CONFIG.put(GlobalAddressBookProperties.CUSTOM_GAB_FOLDER_NAME.getFQPropertyName(), "Family Members");
        super.setUpConfiguration();
        FolderData folderData = folderManager.getFolder(GLOBAL_ADDRESS_BOOK_ID);
        assertEquals("Family Members", folderData.getTitle());
    }

    @Test
    public void test_custom_localized() throws Exception {        
        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.CUSTOM);
        CONFIG.put(GlobalAddressBookProperties.CUSTOM_GAB_FOLDER_NAME.getFQPropertyName(), "Family Members");
        CONFIG.put(GlobalAddressBookProperties.CUSTOM_GAB_FOLDER_NAME.getFQPropertyName() + ".de_DE", "Familienmitglieder");
        super.setUpConfiguration();
        jslobApi.setJSlob(Collections.singletonMap("language", "de_DE"), "io.ox/core", null);
        FolderData folderData = folderManager.getFolder(GLOBAL_ADDRESS_BOOK_ID);
        assertEquals("Familienmitglieder", folderData.getTitle());
    }

    @Test
    public void test_custom_fallback() throws Exception {
        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.CUSTOM);
        CONFIG.put(GlobalAddressBookProperties.CUSTOM_GAB_FOLDER_NAME.getFQPropertyName(), "Family Members");
        super.setUpConfiguration();
        jslobApi.setJSlob(Collections.singletonMap("language", "fr_FR"), "io.ox/core", null);
        FolderData folderData = folderManager.getFolder(GLOBAL_ADDRESS_BOOK_ID);
        assertEquals("Family Members", folderData.getTitle());
    }

    @Test
    public void test_unknown_indentifier() throws Exception {
        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), "unknown_indentifier");
        super.setUpConfiguration();
        FolderData folderData = folderManager.getFolder(GLOBAL_ADDRESS_BOOK_ID);
        assertEquals(FolderStrings.ALL_USERS_NAME, folderData.getTitle());
    }

    @Override
    protected String getScope() {
        return ConfigViewScope.CONTEXT.getScopeName();
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        return CONFIG;
    }
}
