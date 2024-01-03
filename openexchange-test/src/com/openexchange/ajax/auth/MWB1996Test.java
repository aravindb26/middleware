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

package com.openexchange.ajax.auth;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.rmi.RemoteException;

/*
 * Copyright &#169; 2004, 2010 Oracle and/or its affiliates. All  Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 * Neither the name of Oracle and/or its affiliates. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. Oracle and/or its affiliates. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.openexchange.admin.rmi.OXContextGroupInterface;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.advertisement.RemoteAdvertisementService;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.ajax.requesthandler.converters.preview.cache.rmi.ResourceCacheRMIService;
import com.openexchange.auth.Credentials;
import com.openexchange.chronos.rmi.ChronosRMIService;
import com.openexchange.consistency.rmi.ConsistencyRMIService;
import com.openexchange.contact.storage.rdb.rmi.ContactStorageRMIService;
import com.openexchange.context.rmi.ContextRMIService;
import com.openexchange.database.migration.rmi.DBMigrationRMIService;
import com.openexchange.exception.OXException;
import com.openexchange.external.account.ExternalAccountRMIService;
import com.openexchange.gab.GABMode;
import com.openexchange.gab.GABRestorerRMIService;
import com.openexchange.gdpr.dataexport.rmi.DataExportRMIService;
import com.openexchange.groupware.infostore.rmi.FileChecksumsRMIService;
import com.openexchange.groupware.update.UpdateTaskRMIService;
import com.openexchange.logging.rmi.LogbackConfigurationRMIService;
import com.openexchange.mail.compose.rmi.RemoteCompositionSpaceService;
import com.openexchange.push.rmi.PushRMIService;
import com.openexchange.sessiond.rmi.SessiondRMIService;
import com.openexchange.share.impl.rmi.ShareRMIService;
import com.openexchange.test.common.test.pool.ProvisioningUtils;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.FoldersVisibilityData;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.modules.FoldersApi;

public class MWB1996Test extends AbstractConfigAwareAPIClientSession {

    private static final Credentials WRONG_AUTH = new Credentials("this_user", "does_not_exist");

    private static final Credentials CORRECT_MASTER_AUTH = new Credentials(TestContextPool.getOxAdminMaster().getLogin(), TestContextPool.getOxAdminMaster().getPassword());
    private static final Credentials CORRECT_CONTEXT_ADMIN_AUTH = new Credentials(ProvisioningUtils.CTX_ADMIN, ProvisioningUtils.CTX_SECRET);

    @Disabled("Disabled because RMI interfaces are not exposed in test setup!")
    @Test
    public void test() throws Exception {
        String hostname = this.testUser.getAjaxClient().getHostname();
        Registry registry = LocateRegistry.getRegistry(hostname);

        OXContextInterface contextInterface = (OXContextInterface) registry.lookup("OXContext_V2");
        com.openexchange.admin.rmi.dataobjects.Credentials master = new com.openexchange.admin.rmi.dataobjects.Credentials(CORRECT_MASTER_AUTH.getLogin(), CORRECT_MASTER_AUTH.getPassword());
        Context[] allContexts = contextInterface.list(Integer.toString(this.testContext.getId()), master);
        assertEquals(1, allContexts.length, "Found wrong number of contexts!");

        int executionMarker = 0;
        try {
            LogbackConfigurationRMIService logbackConfigurationRMIServiceStub = (LogbackConfigurationRMIService) registry.lookup("LogbackConfigurationRMIService");
            logbackConfigurationRMIServiceStub.getRootAppenderStats();
            executionMarker = executionMarker + 1;
            logbackConfigurationRMIServiceStub.clearFilters(CORRECT_MASTER_AUTH.getLogin(), CORRECT_MASTER_AUTH.getPassword());
            executionMarker = executionMarker + 2;
            logbackConfigurationRMIServiceStub.clearFilters("", "");
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 4;
            }
        }
        assertEquals(7, executionMarker);

        executionMarker = 0;
        try {
            DBMigrationRMIService dbMigrationRMIService = (DBMigrationRMIService) registry.lookup("DBMigrationRMIService");
            executionMarker = executionMarker + 1;
            dbMigrationRMIService.forceMigration(hostname, WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        OXContextGroupInterface contextGroup = (OXContextGroupInterface) registry.lookup("OXContextGroup");
        try {
            contextGroup.deleteContextGroup("jibbet net", CORRECT_MASTER_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof StorageException storageException) {
                String string = storageException.toString();
                assertTrue(string.contains("The global database is not available for the context group with identifier 'jibbet net'"));
                executionMarker = executionMarker + 1;
            }
        }
        try {
            contextGroup.deleteContextGroup(hostname, WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            RemoteAdvertisementService remoteAdvertisementService = (RemoteAdvertisementService) registry.lookup("RemoteAdvertisementService");
            remoteAdvertisementService.removeConfigurations(hostname, false, false, CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 1;
            remoteAdvertisementService.removeConfigurations(hostname, false, false, WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof OXException oxException) {
                String string = oxException.toString();
                assertTrue(string.contains("Authentication failed"));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            ExternalAccountRMIService externalAccountRMIService = (ExternalAccountRMIService) registry.lookup("ExternalAccountRMIService");
            externalAccountRMIService.list(this.testContext.getId());
            executionMarker = executionMarker + 1;
            externalAccountRMIService.delete(getUserId(), getUserId(), getUserId(), null, WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            RemoteCompositionSpaceService remoteCompositionSpaceService = (RemoteCompositionSpaceService) registry.lookup("RemoteCompositionSpaceService");
            remoteCompositionSpaceService.deleteOrphanedReferences(Collections.singletonList(3), CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 1;
            remoteCompositionSpaceService.deleteOrphanedReferences(Collections.singletonList(3), WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        //        Socket Monitoring per default disabled per configuration
        //        executionMarker = 0;
        //        try {
        //            SocketLoggerRMIService socketLoggerRMIService = (SocketLoggerRMIService) registry.lookup("SocketLoggerRMIService");
        //            Set<String> blacklistedLoggers = socketLoggerRMIService.getBlacklistedLoggers();
        //            executionMarker = executionMarker + 1;
        //            socketLoggerRMIService.registerLoggerFor("", CORRECT_AUTH);
        //            executionMarker = executionMarker + 2;
        //            socketLoggerRMIService.registerLoggerFor("", WRONG_AUTH);
        //            executionMarker = executionMarker + 4;
        //            fail();
        //        } catch (Exception e) {
        //            if (e instanceof RemoteException remoteException) {
        //                String string = remoteException.toString();
        //                assertTrue(string.contains("User unauthorized."));
        //                executionMarker = executionMarker + 8;
        //            }
        //        }
        //        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            GABRestorerRMIService gabRestorerRMIService = (GABRestorerRMIService) registry.lookup("GABRestorerRMIService");
            gabRestorerRMIService.restoreDefaultPermissions(this.testContext.getId(), GABMode.GLOBAL, CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 1;
            gabRestorerRMIService.restoreDefaultPermissions(this.testContext.getId(), GABMode.GLOBAL, WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            SessiondRMIService sessiondRMIService = (SessiondRMIService) registry.lookup("SessiondRMIService");
            sessiondRMIService.clearSessionStorage(CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 1;
            sessiondRMIService.clearSessionStorage(WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            UserApi defaultUserApi = new UserApi(getApiClient(), testUser);
            String defaultFolder = getDefaultFolder(defaultUserApi.getFoldersApi());

            EventManager eManager = new EventManager(defaultUserApi, defaultFolder);
            Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
            Calendar start = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
            start.setTimeInMillis(System.currentTimeMillis());
            end.setTimeInMillis(System.currentTimeMillis() + 100000);

            EventData event = EventFactory.createSingleEvent(this.getUserId(), "Summary", DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end));
            EventData createdEvent = eManager.createEvent(event);

            ChronosRMIService chronosRMIService = (ChronosRMIService) registry.lookup("ChronosRMIService");
            chronosRMIService.setEventOrganizer(this.testContext.getId(), Integer.parseInt(createdEvent.getId()), this.getUserId(), CORRECT_CONTEXT_ADMIN_AUTH);
            executionMarker = executionMarker + 1;
            chronosRMIService.setEventOrganizer(this.testContext.getId(), Integer.parseInt(createdEvent.getId()), this.getUserId(), WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            int contactFolder = getClient().getValues().getPrivateContactFolder();
            ContactStorageRMIService contactStorageRMIService = (ContactStorageRMIService) registry.lookup("ContactStorageRMIService");
            contactStorageRMIService.deduplicateContacts(this.testContext.getId(), contactFolder, 0, true, CORRECT_CONTEXT_ADMIN_AUTH);
            executionMarker = executionMarker + 1;
            contactStorageRMIService.deduplicateContacts(this.testContext.getId(), contactFolder, 0, true, WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            DataExportRMIService dataExportRMIService = (DataExportRMIService) registry.lookup("DataExportRMIService");
            dataExportRMIService.getDataExportTasks();
            executionMarker = executionMarker + 1;
            dataExportRMIService.cancelDataExportTasks(this.testContext.getId(), CORRECT_CONTEXT_ADMIN_AUTH);
            executionMarker = executionMarker + 2;
            dataExportRMIService.cancelDataExportTasks(this.testContext.getId(), WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 4;
            }
        }
        assertEquals(7, executionMarker);

        executionMarker = 0;
        try {
            UpdateTaskRMIService updateTaskRMIService = (UpdateTaskRMIService) registry.lookup("UpdateTaskRMIService");
            updateTaskRMIService.getNamespaceAware();
            executionMarker = executionMarker + 1;
            updateTaskRMIService.runAllUpdates(false, CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 2;
            updateTaskRMIService.runAllUpdates(false, WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 4;
            }
        }
        assertEquals(7, executionMarker);

        executionMarker = 0;
        try {
            ConsistencyRMIService consistencyRMIService = (ConsistencyRMIService) registry.lookup("ConsistencyRMIService");
            consistencyRMIService.listAllMissingFiles();
            executionMarker = executionMarker + 1;
            consistencyRMIService.checkOrRepairConfigDB(false, CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 2;
            consistencyRMIService.checkOrRepairConfigDB(false, WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 4;
            }
        }
        assertEquals(7, executionMarker);

        executionMarker = 0;
        try {
            ContextRMIService contextRMIService = (ContextRMIService) registry.lookup("ContextRMIService");
            contextRMIService.checkLogin2ContextMapping(CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 1;

            contextRMIService.checkLogin2ContextMapping(this.testContext.getId(), CORRECT_CONTEXT_ADMIN_AUTH);
            executionMarker = executionMarker + 2;

            contextRMIService.checkLogin2ContextMapping(this.testContext.getId(), WRONG_AUTH);

            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 4;
            }
        }
        assertEquals(7, executionMarker);

        executionMarker = 0;
        try {
            FileChecksumsRMIService fileChecksumsRMIService = (FileChecksumsRMIService) registry.lookup("FileChecksumsRMIService");
            fileChecksumsRMIService.listAllFilesWithoutChecksum();
            executionMarker = executionMarker + 1;

            fileChecksumsRMIService.calculateMissingChecksumsInContext(this.testContext.getId(), CORRECT_CONTEXT_ADMIN_AUTH);
            fileChecksumsRMIService.calculateAllMissingChecksums(CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 2;

            fileChecksumsRMIService.calculateAllMissingChecksums(WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 4;
            }
        }
        assertEquals(7, executionMarker);

        executionMarker = 0;
        try {
            ResourceCacheRMIService resourceCacheRMIService = (ResourceCacheRMIService) registry.lookup("ResourceCacheRMIService");
            resourceCacheRMIService.clear(CORRECT_MASTER_AUTH);
            resourceCacheRMIService.clearFor(this.testContext.getId(), CORRECT_CONTEXT_ADMIN_AUTH);
            executionMarker = executionMarker + 1;

            resourceCacheRMIService.clear(WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 2;
            }
        }
        assertEquals(3, executionMarker);

        executionMarker = 0;
        try {
            ShareRMIService shareRMIService = (ShareRMIService) registry.lookup("ShareRMIService");
            shareRMIService.listShares(this.testContext.getId());
            executionMarker = executionMarker + 1;

            shareRMIService.removeShares(this.testContext.getId(), CORRECT_CONTEXT_ADMIN_AUTH);
            executionMarker = executionMarker + 2;

            shareRMIService.removeShares(this.testContext.getId(), WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 4;
            }
        }
        assertEquals(7, executionMarker);

        executionMarker = 0;
        try {
            PushRMIService pushRMIService = (PushRMIService) registry.lookup("PushRMIService");
            pushRMIService.listPushUsers();
            executionMarker = executionMarker + 1;

            pushRMIService.unregisterPermanentListenerFor(this.testUser.getUserId(), this.testContext.getId(), "test client", CORRECT_MASTER_AUTH);
            executionMarker = executionMarker + 2;

            pushRMIService.unregisterPermanentListenerFor(this.testUser.getUserId(), this.testContext.getId(), "test client", WRONG_AUTH);
            fail();
        } catch (Exception e) {
            if (e instanceof RemoteException remoteException) {
                String string = remoteException.toString();
                assertTrue(string.contains("User unauthorized."));
                executionMarker = executionMarker + 4;
            }
        }
        assertEquals(7, executionMarker);
    }

    protected String getDefaultFolder(FoldersApi foldersApi) throws Exception {
        ArrayList<ArrayList<?>> privateList = getPrivateFolderList(foldersApi, "event", "1,308", "0");
        if (privateList.size() == 1) {
            return (String) privateList.get(0).get(0);
        }
        for (ArrayList<?> folder : privateList) {
            if (folder.get(1) != null && ((Boolean) folder.get(1)).booleanValue()) {
                return (String) folder.get(0);
            }
        }
        throw new Exception("Unable to find default calendar folder!");
    }

    protected ArrayList<ArrayList<?>> getPrivateFolderList(FoldersApi foldersApi, String module, String columns, String tree) throws Exception {
        FoldersVisibilityResponse resp = foldersApi.getVisibleFolders(module, columns, tree, null, Boolean.TRUE);
        FoldersVisibilityData visibilityData = checkResponse(resp.getError(), resp.getErrorDesc(), resp.getCategories(), resp.getData());
        Object privateFolders = visibilityData.getPrivate();
        return (ArrayList<ArrayList<?>>) privateFolders;
    }
}
