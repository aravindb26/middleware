
package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.action.GetRequest;
import com.openexchange.ajax.contact.action.GetResponse;
import com.openexchange.ajax.contact.action.ListRequest;
import com.openexchange.ajax.framework.CommonListResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.framework.ListIDs;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;

public class ListTest extends AbstractContactTest {

    @Test
    public void testList() throws Exception {
        final Contact contactObj = createContactObject();
        final int id1 = insertContact(contactObj);
        final int id2 = insertContact(contactObj);
        final int id3 = insertContact(contactObj);

        // prevent problems with master/slave
        Thread.sleep(1000);

        final int[][] objectIdAndFolderId = { { id1, contactFolderId }, { id2, contactFolderId }, { id3, contactFolderId } };

        final int cols[] = new int[] { Contact.OBJECT_ID, Contact.SUR_NAME, Contact.DISPLAY_NAME, Contact.FOLDER_ID };

        final Contact[] contactArray = listContact(objectIdAndFolderId, cols);
        assertEquals(3, contactArray.length, "check response array");

        // Check order of returned contacts
        for (int i = 0; i < contactArray.length; i++) {
            final int[] ids = objectIdAndFolderId[i];
            assertTrue(ids[0] == contactArray[i].getObjectID(), "Returned contacts object id differs from the requested one.");
            assertTrue(ids[1] == contactArray[i].getParentFolderID(), "Returned contacts folder id differs from the requested one.");
        }
    }

    @Test
    public void testSortDuration() {
        final int size = 10000;
        final List<Contact> contacts = new ArrayList<Contact>();
        final int[][] objectIdsAndFolderIds = new int[size][2];
        for (int i = 0; i < size; i++) {
            final int objectId = (size - 1) - i;
            final Contact contact = createContactObject();
            contact.setObjectID(objectId);
            contacts.add(contact);

            objectIdsAndFolderIds[i] = new int[] { i, contactFolderId };
        }

        // Sort loaded contacts in the order they were requested
        final List<Contact> sortedContacts = new ArrayList<Contact>(contacts.size());
        for (int i = 0; i < objectIdsAndFolderIds.length; i++) {
            final int[] objectIdsAndFolderId = objectIdsAndFolderIds[i];
            final int objectId = objectIdsAndFolderId[0];
            final int folderId = objectIdsAndFolderId[1];

            for (final Contact contact : contacts) {
                if (contact.getObjectID() == objectId && contact.getParentFolderID() == folderId) {
                    sortedContacts.add(contact);
                    break;
                }
            }
        }
    }

    @Test
    public void testListWithAllFields() throws Exception {
        final Contact contactObject = createCompleteContactObject();

        final int objectId = insertContact(contactObject);

        final int[][] objectIdAndFolderId = { { objectId, contactFolderId } };

        final Contact[] contactArray = listContact(objectIdAndFolderId, CONTACT_FIELDS);

        assertEquals(1, contactArray.length, "check response array");

        final Contact loadContact = contactArray[0];

        contactObject.setObjectID(objectId);
        compareObject(contactObject, loadContact);
    }

    @Test
    public void testListWithNotExistingEntries() throws Exception {
        final Contact contactObject = createCompleteContactObject();
        final int objectId = insertContact(contactObject);
        contactObject.setDisplayName(UUID.randomUUID().toString());
        final int objectId2 = insertContact(contactObject);
        final int cols[] = new int[] { Contact.OBJECT_ID, Contact.SUR_NAME, Contact.DISPLAY_NAME };

        // not existing object last
        final int[][] objectIdAndFolderId1 = { { objectId, contactFolderId }, { objectId + 100, contactFolderId } };
        Contact[] contactArray = listContact(objectIdAndFolderId1, cols);
        assertEquals(1, contactArray.length, "check response array");

        // not existing object first
        final int[][] objectIdAndFolderId2 = { { objectId + 100, contactFolderId }, { objectId, contactFolderId } };
        contactArray = listContact(objectIdAndFolderId2, cols);
        assertEquals(1, contactArray.length, "check response array");

        // not existing object first
        final int[][] objectIdAndFolderId3 = { { objectId + 100, contactFolderId }, { objectId, contactFolderId }, { objectId2, contactFolderId } };
        contactArray = listContact(objectIdAndFolderId3, cols);
        assertEquals(2, contactArray.length, "check response array");

        deleteContact(objectId, contactFolderId, true);
    }

    // Node 2652    @Test
    @Test
    public void testLastModifiedUTC() throws Exception {
        final int cols[] = new int[] { Contact.OBJECT_ID, Contact.FOLDER_ID, Contact.LAST_MODIFIED_UTC };

        final Contact contactObj = createContactObject();
        final int objectId = insertContact(contactObj);
        try {
            final ListRequest listRequest = new ListRequest(ListIDs.l(new int[] { contactFolderId, objectId }), cols, true);
            final CommonListResponse response = Executor.execute(getClient(), listRequest);
            final JSONArray arr = (JSONArray) response.getResponse().getData();

            assertNotNull(arr);
            final int size = arr.length();
            assertTrue(size > 0);
            for (int i = 0; i < size; i++) {
                final JSONArray objectData = arr.optJSONArray(i);
                assertNotNull(objectData);
                assertNotNull(objectData.opt(2));
            }
        } finally {
            deleteContact(objectId, contactFolderId, true);
        }
    }

    protected Contact createContactWithDistList(final Contact contactEntry) throws Exception {
        final Contact contactObj = new Contact();
        String string = UUID.randomUUID().toString();
        contactObj.setSurName(string);
        contactObj.setParentFolderID(contactFolderId);
        contactObj.setDisplayName(string);

        final DistributionListEntryObject[] entry = new DistributionListEntryObject[1];
        entry[0] = new DistributionListEntryObject(contactEntry.getDisplayName(), contactEntry.getEmail1(), DistributionListEntryObject.EMAILFIELD1);
        entry[0].setEntryID(contactEntry.getId(true));

        contactObj.setDistributionList(entry);

        insertContact(contactObj);
        return contactObj;
    }

    @Test
    public void testBug42726_emptyList_markedAsDistList() throws Exception {
        // SETUP
        Contact contactEntry = createContactObject();
        contactEntry.setEmail1("internalcontact@x.de");
        final int contactId = insertContact(contactEntry);
        int distListObjectId = 0;
        try {
            contactEntry.setObjectID(contactId);

            Contact distList = createContactWithDistList(contactEntry);
            distListObjectId = distList.getObjectID();

            GetRequest getRequest = new GetRequest(contactFolderId, distListObjectId, tz, false);
            GetResponse getResponse = getClient().execute(getRequest);

            Boolean markAsDistList = new Boolean(((JSONObject) getResponse.getData()).getBoolean("mark_as_distributionlist"));
            assertTrue(markAsDistList.booleanValue());

            long numberOfDistListMembers = ((JSONObject) getResponse.getData()).getLong("number_of_distribution_list");
            assertTrue(numberOfDistListMembers == 1);
            JSONArray distListArray = ((JSONObject) getResponse.getData()).getJSONArray("distribution_list");
            assertTrue(distListArray.length() == numberOfDistListMembers);

            // UPDATE
            distList.setDistributionList(new DistributionListEntryObject[0]);
            updateContact(distList, contactFolderId);

            // ASSERT
            GetRequest emptyGetRequest = new GetRequest(contactFolderId, distListObjectId, tz, false);
            GetResponse emptyGetResponse = getClient().execute(emptyGetRequest);
            boolean emptyMarkAsDistList = ((JSONObject) emptyGetResponse.getData()).getBoolean("mark_as_distributionlist");
            assertTrue(emptyMarkAsDistList, "Contact not marked as list");

            long emptyNumberOfDistListMembers = ((JSONObject) emptyGetResponse.getData()).getLong("number_of_distribution_list");
            assertTrue(emptyNumberOfDistListMembers == 0, "Empty list has wrong number of members");
        } finally {
            deleteContact(contactId, contactFolderId, true);
            deleteContact(distListObjectId, contactFolderId, true);
        }
    }
}
