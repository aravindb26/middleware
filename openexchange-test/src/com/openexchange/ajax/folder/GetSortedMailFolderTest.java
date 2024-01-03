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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetRequest;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.folder.actions.ListRequest;
import com.openexchange.ajax.folder.actions.ListResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.utils.MailFolderUtility;

/**
 * {@link GetSortedMailFolderTest}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
@Disabled
public class GetSortedMailFolderTest extends AbstractAJAXSession {

    private static final int[] COLUMNS = { FolderObject.OBJECT_ID, FolderObject.FOLDER_NAME, FolderObject.OWN_RIGHTS, FolderObject.PERMISSIONS_BITS };

    /**
     * Initializes a new {@link GetSortedMailFolderTest}.
     *
     * @param name test name.
     */
    public GetSortedMailFolderTest() {
        super();
    }

    @Test
    public void testGetSortedMailFolder() throws Throwable {
        AJAXClient client = getClient();
        ListRequest request = new ListRequest(EnumAPI.OX_OLD, String.valueOf(FolderObject.SYSTEM_PRIVATE_FOLDER_ID));
        ListResponse response = client.execute(request);
        Iterator<FolderObject> iter = response.getFolder();
        FolderObject rootMailFolder = null;
        String primaryMailFolder = MailFolderUtility.prepareFullname(0, MailFolder.ROOT_FOLDER_ID);
        while (iter.hasNext()) {
            final FolderObject fo = iter.next();
            if (fo.containsFullName() && primaryMailFolder.equals(fo.getFullName())) {
                rootMailFolder = fo;
                break;
            }
        }
        assertNotNull(rootMailFolder, "Default email folder not found.");
        assertTrue(rootMailFolder.hasSubfolders(), "Default email folder has no subfolders.");
        request = new ListRequest(EnumAPI.OX_OLD, rootMailFolder.getFullName());
        response = client.execute(request);
        List<FolderObject> l = FolderTools.convert(response.getFolder());
        assertTrue(l.size() > 0, "No folders below virtual primary mail account folder found.");
        int pos = 0;
        final FolderObject inboxFolder = l.get(pos++);
        {
            assertTrue(inboxFolder.getFullName().endsWith("INBOX"), "Default inbox folder not found.");
            GetRequest request2 = new GetRequest(EnumAPI.OX_OLD, inboxFolder.getFullName(), COLUMNS);
            GetResponse response2 = client.execute(request2);
            assertFalse(response2.hasError(), "Getting folder information failed.");
            if (l.size() == 1) {
                request = new ListRequest(EnumAPI.OX_OLD, inboxFolder.getFullName());
                response = client.execute(request);
                l = FolderTools.convert(response.getFolder());
                pos = 0;
            }
        }
        {
            final FolderObject draftsFolder = l.get(pos++);
            assertTrue(draftsFolder.isDefaultFolder(), "Default drafts folder not found.");
            GetRequest request2 = new GetRequest(EnumAPI.OX_OLD, draftsFolder.getFullName(), COLUMNS);
            GetResponse response2 = client.execute(request2);
            assertFalse(response2.hasError(), "Getting folder information failed.");
        }
        {
            final FolderObject sentFolder = l.get(pos++);
            assertTrue(sentFolder.isDefaultFolder(), "Default sent folder not found");
            GetRequest request2 = new GetRequest(EnumAPI.OX_OLD, sentFolder.getFullName(), COLUMNS);
            GetResponse response2 = client.execute(request2);
            assertFalse(response2.hasError(), "Getting folder information failed.");
        }
        {
            final FolderObject spamFolder = l.get(pos++);
            assertTrue(spamFolder.isDefaultFolder(), "Default spam folder not found");
            GetRequest request2 = new GetRequest(EnumAPI.OX_OLD, spamFolder.getFullName(), COLUMNS);
            GetResponse response2 = client.execute(request2);
            assertFalse(response2.hasError(), "Getting folder information failed.");
        }
        {
            final FolderObject trashFolder = l.get(pos++);
            assertTrue(trashFolder.isDefaultFolder(), "Default trash folder not found");
            GetRequest request2 = new GetRequest(EnumAPI.OX_OLD, trashFolder.getFullName(), COLUMNS);
            GetResponse response2 = client.execute(request2);
            assertFalse(response2.hasError(), "Getting folder information failed.");
        }
    }
}
