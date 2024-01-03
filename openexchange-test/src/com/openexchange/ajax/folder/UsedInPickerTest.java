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

import static com.openexchange.java.Autoboxing.B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.manager.FolderApi;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.testing.httpclient.models.FolderContactsExtendedProperties;
import com.openexchange.testing.httpclient.models.FolderContactsExtendedPropertiesUsedInPicker;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FoldersVisibilityData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;

/**
 * {@link UsedInPickerTest}
 * 
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 *
 */
class UsedInPickerTest extends AbstractConfigAwareAPIClientSession {
    
    private FolderManager folderManager;
    
    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderManager = new FolderManager(new FolderApi(getApiClient(), testUser), String.valueOf(EnumAPI.OX_NEW.getTreeId()));
    }
    
    @Test
    public void test_usedInPickerValue() throws Exception {
        String defaultContactFolder = folderManager.getDefaultFolder("addressdata");
        verifyUsedInPicker(defaultContactFolder, true);
        try {
            setUsedInPicker(defaultContactFolder, false);
        } catch (AssertionError error) {
            assertTrue(error.getMessage().startsWith("No folder permission"), "Wrong error message");
        }
    }
    
    @Test
    public void test_usedInPickerProtected() throws Exception {
        FoldersVisibilityData data = folderManager.getAllFolders("addressdata", "1,300", null);
        List<List<Object>> privateFolders = (List<List<Object>>) data.getPrivate();
        String externalContacFolder = null;
        for (List<Object> folder : privateFolders) {
            if (folder.get(1).toString().equals("c.o.contact.provider.test")) {
                externalContacFolder = (String) folder.get(0);
            }
        }
        verifyUsedInPicker(externalContacFolder, true);
        try {
            setUsedInPicker(externalContacFolder, false);
        } catch (AssertionError error) {
            assertTrue(error.getMessage().startsWith("No folder permission"), "Wrong error message");
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void test_usedInPickerDefaultValue() throws Exception {
        FoldersVisibilityData data = folderManager.getAllFolders("addressdata", "1,3301", null);
        List<List<Object>> privateFolders = (List<List<Object>>) data.getPrivate();
        List<List<Object>> publicFolders = (List<List<Object>>) data.getPublic();
        assertFoldersContainUsedInPicker(privateFolders);
        assertFoldersContainUsedInPicker(publicFolders);
    }
    
    private void setUsedInPicker(String folder, boolean value) throws Exception {
        FolderData folderData = folderManager.getFolder(folder);
        FolderContactsExtendedProperties properties = new FolderContactsExtendedProperties();
        FolderContactsExtendedPropertiesUsedInPicker usedInPicker = new FolderContactsExtendedPropertiesUsedInPicker();
        usedInPicker.setValue(B(value));
        properties.setUsedInPicker(usedInPicker);
        folderData.setComOpenexchangeContactsExtendedProperties(properties);
        folderManager.updateFolder(folder, folderData, null); 
    }
    
    private void verifyUsedInPicker(String folder, boolean value) throws Exception {
        FolderData folderData = folderManager.getFolder(folder);
        FolderContactsExtendedProperties properties = folderData.getComOpenexchangeContactsExtendedProperties();
        FolderContactsExtendedPropertiesUsedInPicker usedInPicker = properties.getUsedInPicker();
        assertNotNull(usedInPicker);
        Boolean folderValue = usedInPicker.getValue();
        assertEquals(B(value), folderValue, "Unexpected usedInPicker value");
    }
    
    private void assertFoldersContainUsedInPicker(List<List<Object>> folders) throws Exception{
        for (List<Object> folder : folders) {
            String folderId = (String) folder.get(0);
            if (folderId.startsWith("con://0")) {
                // default contact provider
                verifyUsedInPicker(folderId, true);
            } else {
                // test contact provider
                verifyUsedInPicker(folderId, true);
            }
        }
    }
    
}
