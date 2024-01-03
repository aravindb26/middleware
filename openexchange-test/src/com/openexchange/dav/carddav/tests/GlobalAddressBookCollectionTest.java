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
package com.openexchange.dav.carddav.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import com.openexchange.config.cascade.ConfigViewScope;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.UserAgents;
import com.openexchange.folderstorage.GlobalAddressBookProperties;
import com.openexchange.groupware.i18n.FolderStrings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link GlobalAddressBookCollectionTest}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 */
public class GlobalAddressBookCollectionTest extends CardDAVTest {
    
    private Map<String, String> CONFIG = new HashMap<String, String>();
    
    public GlobalAddressBookCollectionTest() {
        super();
    }
    
    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testGlobalAddressBook(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.GLOBAL_ADDRESS_BOOK_ID);
        super.setUpConfiguration();
        discoverContactsCollection(FolderStrings.SYSTEM_LDAP_FOLDER_NAME);
    }
    
    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testInternalUsers(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.INTERNAL_USERS_ID);
        super.setUpConfiguration();
        discoverContactsCollection(FolderStrings.INTERNAL_USERS_NAME);
    }
    
    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAllUsers(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.ALL_USERS_ID);
        super.setUpConfiguration();
        discoverContactsCollection(FolderStrings.ALL_USERS_NAME);
    }
    
    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCustom(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String customFolderName = "Family Members";
        CONFIG.put(GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName(), GlobalAddressBookProperties.CUSTOM);
        CONFIG.put(GlobalAddressBookProperties.CUSTOM_GAB_FOLDER_NAME.getFQPropertyName(), customFolderName);
        super.setUpConfiguration();
        discoverContactsCollection(customFolderName);
    }
    
    @Override
    protected String getScope() {
        return ConfigViewScope.CONTEXT.getScopeName();
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        return CONFIG;
    }
    
    private void discoverContactsCollection(String collectionName) throws Exception {
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.DISPLAYNAME);
        PropFindMethod propFind = new PropFindMethod(super.webDAVClient.getBaseURI() + Config.getPathPrefix() + "/carddav/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_1);
        super.webDAVClient.setUserAgent(UserAgents.EM_CLIENT_FOR_APP_SUITE);
        boolean found = false;
        for (MultiStatusResponse response : super.webDAVClient.doPropFind(propFind)) {
            String displayName = super.extractTextContent(DavPropertyName.DISPLAYNAME, response);
            if (collectionName.equals(displayName)) {
                found = true;
                break;
            }
        }
        assertTrue(found, String.format("No folder collection with name '%s' found below /carddav/", collectionName));
    }

}
