
package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.json.JSONObject;
import com.openexchange.ajax.contact.action.AdvancedSearchRequest;
import com.openexchange.ajax.framework.CommonSearchResponse;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.test.ContactTestManager;
import com.openexchange.test.common.test.TestClassConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@SuppressWarnings("deprecation")
public class AdvancedSearchTest2 extends AbstractManagedContactTest {

    private static final String BOB_LASTNAME = "Rather complicated last name with timestamp (" + new Date().getTime() + ") that does not appear in other folders";

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        Contact alice = ContactTestManager.generateContact(folderID);
        alice.setGivenName("Alice");

        Contact bob = ContactTestManager.generateContact(folderID);
        bob.setGivenName("Bob");
        bob.setSurName(BOB_LASTNAME);

        Contact charlie = ContactTestManager.generateContact(folderID);
        charlie.setGivenName("Charlie");

        cotm.newAction(alice, bob, charlie);
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().emptyContext(true).build();
    }

    @Test
    public void testSearchOrdering() throws Exception {
        cotm.newAction( // @formatter:off
                ContactTestManager.generateContact(folderID, "Elvis"),
                ContactTestManager.generateContact(folderID, "Feelvis"),
                ContactTestManager.generateContact(folderID, "Gelvis"),
                ContactTestManager.generateContact(folderID, "Geena"),
                ContactTestManager.generateContact(folderID, "Hellvis")
        );// @formatter:on
        ContactField field = ContactField.SUR_NAME;
        JSONObject filter = new JSONObject( // @formatter:off
            "{'filter' : [ 'and', " +
                "['>=' , {'field' : '" + field.getAjaxName() + "'} , 'E'], " +
                "['<' , {'field' : '" + field.getAjaxName() + "'}, 'I'], " +
                "['NOT' , ['=' , {'field' : '" + field.getAjaxName() + "'}, 'Geena']] " +
            "]}"); // @formatter:on

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { field.getNumber() }, field.getNumber(), "asc");
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find at least a result");
        assertEquals(4, resultTable.length, "Should find four results");

        int columnPos = response.getColumnPos(field.getNumber());

        assertTrue(resultTable[0][columnPos].equals("Elvis"), "Result should appear in the right order");
        assertTrue(resultTable[1][columnPos].equals("Feelvis"), "Result should appear in the right order");
        assertTrue(resultTable[2][columnPos].equals("Gelvis"), "Result should appear in the right order");
        assertTrue(resultTable[3][columnPos].equals("Hellvis"), "Result should appear in the right order");

        /* invert it */
        request = new AdvancedSearchRequest(filter, new int[] { field.getNumber() }, field.getNumber(), "desc");
        response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        resultTable = response.getArray();
        assertNotNull(resultTable, "Should find at least a result");
        assertEquals(4, resultTable.length, "Should find four results");

        columnPos = response.getColumnPos(field.getNumber());

        assertTrue(resultTable[0][columnPos].equals("Hellvis"), "Result should appear in the right order");
        assertTrue(resultTable[1][columnPos].equals("Gelvis"), "Result should appear in the right order");
        assertTrue(resultTable[2][columnPos].equals("Feelvis"), "Result should appear in the right order");
        assertTrue(resultTable[3][columnPos].equals("Elvis"), "Result should appear in the right order");

    }
}
