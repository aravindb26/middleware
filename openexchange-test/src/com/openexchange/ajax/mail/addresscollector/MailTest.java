
package com.openexchange.ajax.mail.addresscollector;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.config.actions.SetRequest;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.folder.Create;
import com.openexchange.ajax.folder.actions.DeleteRequest;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.framework.CommonInsertResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.mail.AbstractMailTest;
import com.openexchange.ajax.mail.actions.SendRequest;
import com.openexchange.ajax.mail.contenttypes.MailContentType;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.mail.MailJSONField;

/**
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herr furth</a>
 *
 */

@Disabled
public class MailTest extends AbstractMailTest {

    public MailTest() {
        super();
    }

    @Test
    public void testAddressCollection() throws Throwable {
        FolderObject folder = null;
        try {
            //Create Collection Folder and set config.
            folder = createContactFolder();

            //Clear mailfolder
            clearFolder(getInboxFolder());
            clearFolder(getSentFolder());
            clearFolder(getTrashFolder());

            //Send Mail
            sendMail();

            //Check Contacts
            checkContacts(folder.getObjectID());

            //Send Mail again
            sendMail();

            //No changes in Collection Folder
            checkContacts(folder.getObjectID());

        } finally {
            if (folder != null) {
                deleteContactFolder(folder);
            }

            clearFolder(getInboxFolder());
            clearFolder(getSentFolder());
            clearFolder(getTrashFolder());
        }
    }

    private void sendMail() throws OXException, JSONException, IOException {
        final JSONObject mail = new JSONObject();
        mail.put(MailJSONField.FROM.getKey(), getSendAddress());
        mail.put(MailJSONField.RECIPIENT_TO.getKey(), getSendAddress());
        mail.put(MailJSONField.RECIPIENT_CC.getKey(), "");
        mail.put(MailJSONField.RECIPIENT_BCC.getKey(), "");
        mail.put(MailJSONField.SUBJECT.getKey(), "Mail Test for Contact Collection.");
        mail.put(MailJSONField.PRIORITY.getKey(), "3");

        final JSONObject bodyObject = new JSONObject();
        bodyObject.put(MailJSONField.CONTENT_TYPE.getKey(), MailContentType.ALTERNATIVE.toString());
        bodyObject.put(MailJSONField.CONTENT.getKey(), "Bodytext for Contact Collection Mail Test");

        final JSONArray attachments = new JSONArray();
        attachments.put(bodyObject);

        mail.put(MailJSONField.ATTACHMENTS.getKey(), attachments);

        //Send mail
        Executor.execute(getClient(), new SendRequest(mail.toString(), null));
    }

    private void checkContacts(final int folderId) throws Exception {
        final Contact[] contacts = cotm.searchAction("*", folderId);
        assertEquals(1, contacts.length, "Number of collected Contacts not correct.");
        assertEquals(getSendAddress(), contacts[0].getEmail1(), "Email does not match.");
    }

    private FolderObject createContactFolder() throws OXException, IOException, JSONException {
        final FolderObject folder = Create.createPrivateFolder("ContactCollectionFolder " + UUID.randomUUID().toString(), FolderObject.CONTACT, getClient().getValues().getUserId());
        folder.setParentFolderID(getClient().getValues().getPrivateContactFolder());
        final CommonInsertResponse response = Executor.execute(getClient(), new InsertRequest(EnumAPI.OX_OLD, folder));
        folder.setObjectID(response.getId());
        folder.setLastModified(response.getTimestamp());

        getClient().execute(new SetRequest(Tree.ContactCollectEnabled, B(true)));
        getClient().execute(new SetRequest(Tree.ContactCollectFolder, I(folder.getObjectID())));
        return folder;
    }

    private void deleteContactFolder(final FolderObject folder) throws OXException, IOException, JSONException {
        Executor.execute(getClient(), new SetRequest(Tree.ContactCollectEnabled, B(false)));
        Executor.execute(getClient(), new DeleteRequest(EnumAPI.OX_OLD, folder.getObjectID(), folder.getLastModified()));
    }

}
