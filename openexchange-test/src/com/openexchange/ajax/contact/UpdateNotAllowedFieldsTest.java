
package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.contact.action.UpdateRequest;
import com.openexchange.ajax.contact.action.UpdateResponse;
import com.openexchange.ajax.fields.CommonFields;
import com.openexchange.ajax.user.actions.GetRequest;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;

public class UpdateNotAllowedFieldsTest extends AbstractManagedContactTest {

    public UpdateNotAllowedFieldsTest() {
        super();
    }

    @Test
    public void testTryUpdateContextID() throws Exception {
        for (Contact contact : getContactsToUpdate()) {
            Contact changedContextID = new Contact();
            changedContextID.setContextId(3465474);
            Contact updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), contact.getLastModified().getTime(), changedContextID, null);
            assertEquals(contact.getContextId(), updatedContact.getContextId(), "context ID was changed");
            changedContextID.setContextId(0);
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), updatedContact.getLastModified().getTime(), changedContextID, null);
            assertEquals(contact.getContextId(), updatedContact.getContextId(), "context ID was changed");
            changedContextID.setContextId(43654754);
            changedContextID.setParentFolderID(getClient().getValues().getPrivateContactFolder());
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), contact.getLastModified().getTime(), changedContextID, null);
            assertEquals(contact.getContextId(), updatedContact.getContextId(), "context ID was changed");

        }
    }

    @Test
    public void testTryUpdateObjectID() throws Exception {
        for (Contact contact : getContactsToUpdate()) {
            Contact changedObjectID = new Contact();
            changedObjectID.setObjectID(1533523456);
            Contact updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), contact.getLastModified().getTime(), changedObjectID, Category.CATEGORY_PERMISSION_DENIED);
            assertEquals(contact.getObjectID(), updatedContact.getObjectID(), "object ID was changed");
            changedObjectID.setObjectID(0);
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), updatedContact.getLastModified().getTime(), changedObjectID, null);
            assertEquals(contact.getObjectID(), updatedContact.getObjectID(), "object ID was changed");
            changedObjectID.setObjectID(8794);
            changedObjectID.setParentFolderID(getClient().getValues().getPrivateContactFolder());
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), contact.getLastModified().getTime(), changedObjectID, null);
            assertEquals(contact.getObjectID(), updatedContact.getObjectID(), "object ID was changed");
        }
    }

    @Test
    public void testTryUpdateUserID() throws Exception {
        for (Contact contact : getContactsToUpdate()) {
            Contact changedUserID = new Contact();
            changedUserID.setInternalUserId(23235235);
            Contact updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), contact.getLastModified().getTime(), changedUserID, Category.CATEGORY_PERMISSION_DENIED);
            assertEquals(contact.getInternalUserId(), updatedContact.getInternalUserId(), "user ID was changed");
            changedUserID.setInternalUserId(0);
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), updatedContact.getLastModified().getTime(), changedUserID, null);
            assertEquals(contact.getInternalUserId(), updatedContact.getInternalUserId(), "user ID was changed");
            changedUserID.setInternalUserId(45600);
            changedUserID.setParentFolderID(getClient().getValues().getPrivateContactFolder());
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), updatedContact.getLastModified().getTime(), changedUserID, Category.CATEGORY_PERMISSION_DENIED);
            assertEquals(contact.getInternalUserId(), updatedContact.getInternalUserId(), "user ID was changed");
        }
    }

    @Test
    public void testTryUpdateUID() throws Exception {
        for (Contact contact : getContactsToUpdate()) {
            Contact changedUID = new Contact();
            changedUID.setUid(UUID.randomUUID().toString());
            Contact updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), contact.getLastModified().getTime(), changedUID, Category.CATEGORY_PERMISSION_DENIED);
            assertEquals(contact.getUid(), updatedContact.getUid(), "UID was changed");
            changedUID.setUid(null);
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), updatedContact.getLastModified().getTime(), changedUID, null);
            assertEquals(contact.getUid(), updatedContact.getUid(), "UID was changed");
            changedUID.setUid("");
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), updatedContact.getLastModified().getTime(), changedUID, null);
            assertEquals(contact.getUid(), updatedContact.getUid(), "UID was changed");
            changedUID.setUid(UUID.randomUUID().toString());
            changedUID.setParentFolderID(getClient().getValues().getPrivateContactFolder());
            updatedContact = tryToUpdate(contact.getObjectID(), contact.getParentFolderID(), updatedContact.getLastModified().getTime(), changedUID, Category.CATEGORY_PERMISSION_DENIED);
            assertEquals(contact.getUid(), updatedContact.getUid(), "UID was changed");
        }
    }

    private Contact tryToUpdate(int objectID, int folderID, long timestamp, Contact changes, Category expectedExceptionCategory) throws Exception {
        DeltaUpdateRequest updateRequest = new DeltaUpdateRequest(objectID, folderID, timestamp, changes, false);
        UpdateResponse updateResponse = super.getClient().execute(updateRequest);
        assertNotNull(updateResponse, "got no response");
        if (null != expectedExceptionCategory) {
            OXException exception = updateResponse.getException();
            assertNotNull(exception, "got no exception");
            assertEquals(expectedExceptionCategory, exception.getCategory(), "unexpected exception category");
        }
        return cotm.getAction(folderID, objectID);
    }

    private Contact[] getContactsToUpdate() throws Exception {
        return new Contact[] { getClient().execute(new GetRequest(getClient().getValues().getUserId(), getClient().getValues().getTimeZone())).getContact(), cotm.getAction(cotm.newAction(generateContact())) };
    }

    private static class DeltaUpdateRequest extends UpdateRequest {

        private final int objectID;
        private final int folderID;
        private final long timestamp;

        public DeltaUpdateRequest(int objectID, int folderID, long timestamp, Contact changes, boolean failOnErrors) {
            super(folderID, changes, failOnErrors);
            this.objectID = objectID;
            this.folderID = folderID;
            this.timestamp = timestamp;
        }

        @Override
        public Parameter[] getParameters() {
            return new Parameter[] { new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_UPDATE), new Parameter(AJAXServlet.PARAMETER_INFOLDER, String.valueOf(folderID)), new Parameter(AJAXServlet.PARAMETER_ID, String.valueOf(objectID)), new Parameter(AJAXServlet.PARAMETER_TIMESTAMP, String.valueOf(timestamp))
            };
        }

        @Override
        public Object getBody() throws JSONException {
            Contact contact = super.getContact();
            JSONObject jsonObject = convert(contact);
            if (contact.containsContextId()) {
                jsonObject.put("cid", contact.getContextId());
            }
            if (contact.containsUid()) {
                jsonObject.put(CommonFields.UID, null == contact.getUid() ? JSONObject.NULL : contact.getUid());
            }
            return jsonObject;
        }

    }

}
