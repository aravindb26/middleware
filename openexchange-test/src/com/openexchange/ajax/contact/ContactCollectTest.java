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

package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.mail.actions.GetRequest;
import com.openexchange.ajax.mail.actions.ImportMailRequest;
import com.openexchange.ajax.mail.actions.ImportMailResponse;
import com.openexchange.ajax.mail.actions.SendRequest;
import com.openexchange.ajax.mail.contenttypes.MailContentType;
import com.openexchange.java.util.ImmutablePair;
import com.openexchange.java.util.TimeZones;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.utils.DateUtils;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ContactsResponse;
import com.openexchange.testing.httpclient.modules.ImportApi;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link ContactCollectTest}
 * 
 * This test requires bundle com.openexchange.contact.provider.test
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 */
public class ContactCollectTest extends ContactProviderTest {

    /*
     * Internal contact storage
     */
    final String RECIPIENT_A = "thomas.kessler@fc.de";
    /*
     * No contact with such address
     */
    final String RECIPIENT_B = "jonas.hector@fc.de";
    /*
     * External contact provider
     */
    final String RECIPIENT_C = "john.doe@example.org";

    // @formatter:off
    final String IMPORT_VCARD =
        "BEGIN:VCARD\n"
      + "VERSION:3.0\n"
      + "EMAIL;TYPE=work:thomas.kessler@fc.de\n"
      + "FN:Thomas Kessler\n"
      + "N:;Thomas Kessler\n"
      + "END:VCARD\n";
    // @formatter:on

    final String MAIL_ADDRESS_COLUMN = "555";

    final String COLLECT_ON_ACCESS = "{\"contactCollectOnMailAccess\": true}";
    final String COLLECT_ON_TRANSPORT = "{\"contactCollectOnMailTransport\": true}";

    private JSlobApi jSlobApi;
    private ImportApi importApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        jSlobApi = new JSlobApi(testUser.getApiClient());
        importApi = new ImportApi(testUser.getApiClient());
        importVCARD(IMPORT_VCARD);
    }

    @Test
    public void testContactCollectOnMailTransport() throws Exception {
        List<String> recipientList = Arrays.asList(RECIPIENT_A, RECIPIENT_B, RECIPIENT_C);

        // contactCollectOnMailTransport: false
        for (String recipient : recipientList) {
            sendMailTo(recipient);
        }
        Thread.sleep(1000); // wait for contact collector
        assertMailAddressesHaveNotBeenCollected(recipientList);

        // contactCollectOnMailTransport: true
        jSlobApi.setJSlob(COLLECT_ON_TRANSPORT, "io.ox/mail", null);
        for (String recipient : recipientList) {
            sendMailTo(recipient);
        }
        Thread.sleep(1000); // wait for contact collector
        assertMailAddressesHaveNotBeenCollected(Arrays.asList(RECIPIENT_A, RECIPIENT_C));
        assertMailAddressHasBeenCollected(RECIPIENT_B);
    }

    @Test
    public void testContactCollectOnMailAccess() throws Exception {
        List<String> recipientList = Arrays.asList(RECIPIENT_A, RECIPIENT_B, RECIPIENT_C);

        // contactCollectOnMailAccess: false
        for (String recipient : recipientList) {
            readMail(importMail(recipient));
        }
        Thread.sleep(1000); // wait for contact collector
        assertMailAddressesHaveNotBeenCollected(Arrays.asList(RECIPIENT_A, RECIPIENT_B, RECIPIENT_C));

        // contactCollectOnMailAccess: true
        jSlobApi.setJSlob(COLLECT_ON_ACCESS, "io.ox/mail", null);
        for (String recipient : recipientList) {
            readMail(importMail(recipient));
        }
        Thread.sleep(1000); // wait for contact collector
        assertMailAddressesHaveNotBeenCollected(Arrays.asList(RECIPIENT_A, RECIPIENT_C));
        assertMailAddressHasBeenCollected(RECIPIENT_B);
    }

    /////////////////////////////////// Test Helpers ////////////////////////////////

    private String getCollectedAddressesFolderId() throws ApiException {
        ArrayList<ArrayList<Object>> folders = folderManager.listFolders("1", "1,300", Boolean.FALSE);
        Optional<ArrayList<Object>> collectedAddressesFolder = folders.stream().filter(folder -> folder.get(1).equals("Collected addresses")).findFirst();
        return (String) collectedAddressesFolder.get().get(0);
    }

    private void sendMailTo(String recipientTo) throws Exception {
        SendRequest request = new SendRequest(createJSONMail(recipientTo).toString());
        Executor.execute(testUser.getAjaxClient(), request);
    }

    private void importVCARD(String vcard) throws Exception {
        File file = File.createTempFile("tmp", null);
        FileUtils.writeStringToFile(file, vcard, Charset.defaultCharset());
        importApi.importVCard(String.valueOf(testUser.getAjaxClient().getValues().getPrivateContactFolder()), file, null);
    }

    private JSONObject createJSONMail(String recipientTo) throws Exception {
        final JSONObject mail = new JSONObject();
        mail.put(MailJSONField.FROM.getKey(), testUser.getAjaxClient().getValues().getSendAddress());
        mail.put(MailJSONField.RECIPIENT_TO.getKey(), recipientTo);
        mail.put(MailJSONField.SUBJECT.getKey(), UUID.randomUUID());
        JSONObject mailBody = new JSONObject();
        mailBody.put(MailJSONField.CONTENT_TYPE.getKey(), MailContentType.PLAIN);
        mailBody.put(MailJSONField.CONTENT.getKey(), "Deutscher Meister 1.FC Koeln");
        final JSONArray attachments = new JSONArray();
        attachments.put(mailBody);
        mail.put(MailJSONField.ATTACHMENTS.getKey(), attachments);
        return mail;
    }

    private String createMailSource(String from) {
        // @formatter:off
        String mail = 
            "From: " + from + "\n"
          + "To: " + from + "\n"
          + "Date: " + DateUtils.toStringRFC822(new Date(), TimeZones.UTC) + "\n"
          + "Subject: " + UUID.randomUUID() + "\n"
          + "Mime-Version: 1.0" + "\n" 
          + "Content-Type: text/plain; charset=UTF-8" + "\n"
          + "Content-Transfer-Encoding: 8bit" + "\n"
          + "\n"
          + "Lorem ipsum";
        // @formatter:on
        return mail;
    }
    
    private ImmutablePair<String, String> importMail(String recipientTo) throws Exception {
        String mail = createMailSource(recipientTo);
        ByteArrayInputStream mailStream = new ByteArrayInputStream(mail.getBytes(com.openexchange.java.Charsets.UTF_8));
        ImportMailRequest request = new ImportMailRequest("default0/INBOX", 0, true, true, mailStream);
        ImportMailResponse response = Executor.execute(testUser.getAjaxClient(), request);
        return ImmutablePair.newInstance(response.getIds()[0][0], response.getIds()[0][1]);
    }

    private void readMail(ImmutablePair<String, String> mail) throws Exception {
        GetRequest request = new GetRequest(mail.getFirst(), mail.getSecond());
        Executor.execute(testUser.getAjaxClient(), request);
    }
    
    private void assertMailAddressesHaveNotBeenCollected(List<String> mailAddresses) throws ApiException {
        ArrayList<String> collectedMailAddresses = getCollectedMailAddresses();
        for (String mailAddress : mailAddresses) {
            assertFalse(collectedMailAddresses.contains(mailAddress), mailAddress + " has been collected");
        }
    }

    private void assertMailAddressesHaveBeenCollected(List<String> mailAddresses) throws ApiException, InterruptedException {
        ArrayList<String> collectedMailAddresses;
        int counter;
        boolean collected;
        for (String mailAddress : mailAddresses) {
            counter = 0;
            collected = false;
            while (!collected && counter < 30) { // max wait 30 seconds for contact collection
                collectedMailAddresses = getCollectedMailAddresses();
                if (collectedMailAddresses.contains(mailAddress)) {
                    collected = true;
                }
                Thread.sleep(1000);
                counter++;
            }
            if (!collected) {
                fail(mailAddress + " has not been collected");
            }
        }
    }

    private void assertMailAddressHasBeenCollected(String mailAddress) throws ApiException, InterruptedException {
        assertMailAddressesHaveBeenCollected(Arrays.asList(mailAddress));
    }

    private ArrayList<String> getCollectedMailAddresses() throws ApiException {
        ContactsResponse contactResponse = addressbooksApi.getAllContactsFromAddressbook(getCollectedAddressesFolderId(), MAIL_ADDRESS_COLUMN, null, null, null);
        ArrayList<ArrayList<Object>> collectedContacts = (ArrayList<ArrayList<Object>>) contactResponse.getData();
        ArrayList<String> collectedMailAddresses = new ArrayList<String>();
        for (ArrayList<Object> contact : collectedContacts) {
            String address = (String) contact.get(0);
            collectedMailAddresses.add(address);
        }
        return collectedMailAddresses;
    }

}
