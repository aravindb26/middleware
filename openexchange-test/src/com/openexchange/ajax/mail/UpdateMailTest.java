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

package com.openexchange.ajax.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Date;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.DeleteRequest;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.ajax.mail.actions.AllRequest;
import com.openexchange.ajax.mail.actions.AllResponse;
import com.openexchange.ajax.mail.actions.NewMailRequest;
import com.openexchange.ajax.mail.actions.NewMailResponse;
import com.openexchange.ajax.mail.actions.UpdateMailRequest;
import com.openexchange.ajax.mail.actions.UpdateMailResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.server.impl.OCLPermission;

/**
 * {@link UpdateMailTest}
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class UpdateMailTest extends AbstractMailTest {

    @Test
    public void testShouldBeAbleToAddFlags() throws OXException, IOException, JSONException {
        final String eml = "Message-Id: <4A102517.4650.0059.1@foobar.com>\n" + "Date: Tue, 05 May 2009 11:37:58 -0500\n" + "From: " + getSendAddress() + "\n" + "To: " + getSendAddress() + "\n" + "Subject: Invitation for launch\n" + "Mime-Version: 1.0\n" + "Content-Type: text/plain; charset=\"UTF-8\"\n" + "Content-Transfer-Encoding: 8bit\n" + "\n" + "This is a MIME message. If you are reading this text, you may want to \n" + "consider changing to a mail reader or gateway that understands how to \n" + "properly handle MIME multipart messages.";
        NewMailRequest newMailRequest = new NewMailRequest(getClient().getValues().getInboxFolder(), eml, -1, true);
        NewMailResponse newMailResponse = getClient().execute(newMailRequest);
        String folder = newMailResponse.getFolder();
        String id = newMailResponse.getId();

        UpdateMailRequest updateRequest = new UpdateMailRequest(folder, id);
        final int additionalFlag = MailMessage.FLAG_ANSWERED; //note: doesn't work for 16 (recent) and 64 (user)
        updateRequest.setFlags(additionalFlag);
        updateRequest.updateFlags();
        UpdateMailResponse updateResponse = getClient().execute(updateRequest);
        assertNull(updateResponse.getErrorMessage());

        TestMail updatedMail = getMail(folder, id);
        assertTrue((updatedMail.getFlags() & additionalFlag) == additionalFlag, "Flag should have been changed, but are: " + Integer.toBinaryString(updatedMail.getFlags()));

        updateRequest = new UpdateMailRequest(folder, id);
        updateRequest.setFlags(additionalFlag);
        updateRequest.removeFlags();
        updateResponse = getClient().execute(updateRequest);

        updatedMail = getMail(folder, id);
        assertTrue((updatedMail.getFlags() & additionalFlag) == 0, "Flag should have been changed back again, but are: " + Integer.toBinaryString(updatedMail.getFlags()));
    }

    @Test
    public void testShouldBeAbleToAddFlags2AllMessages() throws OXException, IOException, JSONException {
        String newId = null;
        try {
            /*
             * Create new mail folder
             */
            {
                final FolderObject fo = new FolderObject();
                {
                    final String inboxFolder = getClient().getValues().getInboxFolder();
                    final String name = "TestFolder" + System.currentTimeMillis();
                    final String fullName = inboxFolder + "/" + name;
                    fo.setFullName(fullName);
                    fo.setFolderName(name);
                }
                fo.setModule(FolderObject.MAIL);

                final OCLPermission oclP = new OCLPermission();
                oclP.setEntity(getClient().getValues().getUserId());
                oclP.setGroupPermission(false);
                oclP.setFolderAdmin(true);
                oclP.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
                fo.setPermissionsAsArray(new OCLPermission[] { oclP });

                final InsertRequest request = new InsertRequest(EnumAPI.OUTLOOK, fo);
                final InsertResponse response = getClient().execute(request);

                newId = (String) response.getResponse().getData();
            }
            /*
             * Append mails to new folder
             */
            {
                final String eml = "Message-Id: <4A002517.4650.0059.1@deployfast.com>\n" + "X-Mailer: Novell GroupWise Internet Agent 8.0.0 \n" + "Date: Tue, 05 May 2009 11:37:58 -0500\n" + "From: " + getSendAddress() + "\n" + "To: " + getSendAddress() + "\n" + "Subject: Re: Your order for East Texas Lighthouse\n" + "Mime-Version: 1.0\n" + "Content-Type: text/plain; charset=\"UTF-8\"\n" + "Content-Transfer-Encoding: 8bit\n" + "\n" + "This is a MIME message. If you are reading this text, you may want to \n" + "consider changing to a mail reader or gateway that understands how to \n" + "properly handle MIME multipart messages.";

                for (int i = 0; i < 10; i++) {
                    final NewMailRequest newMailRequest = new NewMailRequest(newId, eml, -1, true);
                    final NewMailResponse newMailResponse = getClient().execute(newMailRequest);
                    final String folder = newMailResponse.getFolder();
                    assertNotNull(folder, "Missing folder in response.");
                    assertNotNull(newMailResponse.getId(), "Missing ID in response.");
                    assertEquals(newId, folder, "Folder ID mismatch in newly appended message.");
                }
            }
            /*
             * Perform batch update call
             */
            final int flag = MailMessage.FLAG_ANSWERED;
            {
                final UpdateMailRequest updateRequest = new UpdateMailRequest(newId);
                final int additionalFlag = flag; // note: doesn't work for 16 (recent) and 64 (user)
                updateRequest.setFlags(additionalFlag);
                updateRequest.updateFlags();
                final UpdateMailResponse updateResponse = getClient().execute(updateRequest);
                assertEquals(newId, updateResponse.getFolder(), "Folder ID mismatch.");
            }
            /*
             * Check
             */
            {
                final AllRequest allRequest = new AllRequest(newId, new int[] { MailListField.ID.getField(), MailListField.FLAGS.getField() }, MailSortField.RECEIVED_DATE.getField(), Order.ASCENDING, true);
                final AllResponse allResponse = getClient().execute(allRequest);
                final Object[][] array = allResponse.getArray();
                for (final Object[] arr : array) {
                    final Integer flags = (Integer) arr[1];
                    assertTrue((flags.intValue() & flag) > 0, "\\Seen flag not set for message " + arr[0] + " in folder " + newId);
                }
            }

        } finally {
            if (null != newId) {
                // Delete folder
                try {
                    final DeleteRequest deleteRequest = new DeleteRequest(EnumAPI.OUTLOOK, newId, new Date());
                    getClient().execute(deleteRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
